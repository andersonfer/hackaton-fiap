# FIAP X - Sistema de Processamento de Videos

Sistema que recebe upload de videos, extrai frames em paralelo via FFmpeg e disponibiliza os frames em um arquivo ZIP para download.

---

## Arquitetura

### Visao Geral

```
                        ┌─────────────────────────────────────────────┐
                        │              FIAP X Application              │
                        │                                               │
  Usuario               │  ┌──────────┐    ┌──────────────────────┐   │
  (Browser) ──HTTP──►   │  │  Spring  │    │    Casos de Uso      │   │
                        │  │ Security │──► │  EnviarVideo         │   │
                        │  │  (JWT)   │    │  ProcessarVideo      │   │
                        │  └──────────┘    │  ListarVideos        │   │
                        │                  │  BaixarVideo         │   │
                        │  ┌──────────┐    │  AutenticarUsuario   │   │
                        │  │   REST   │    │  RegistrarUsuario    │   │
                        │  │   API    │◄───└──────────────────────┘   │
                        │  └──────────┘             │                  │
                        │                           │                  │
                        └───────────────────────────┼──────────────────┘
                                                    │
                    ┌───────────────────────────────┼───────────────────┐
                    │                               │                   │
                    ▼                               ▼                   ▼
             ┌─────────────┐               ┌──────────────┐    ┌──────────────┐
             │  MySQL 8    │               │  RabbitMQ 3  │    │  Filesystem  │
             │             │               │              │    │  (videos,    │
             │  usuarios   │               │  fila.video  │    │  frames,     │
             │  videos     │               │  .processam. │    │  zips)       │
             └─────────────┘               └──────┬───────┘    └──────────────┘
                                                  │
                                    ┌─────────────▼─────────────┐
                                    │   OuvinteMensagemVideo    │
                                    │   (concorrencia=5)        │
                                    │                           │
                                    │   ProcessarVideo ──► FFmpeg
                                    └───────────────────────────┘
```

### Fluxo de Processamento Assincrono

```
POST /api/videos/enviar
  │
  ├─ Valida JWT (extrai usuarioId)
  ├─ Salva arquivo no filesystem
  ├─ Cria registro no banco (status=PENDENTE)
  ├─ Publica mensagem na fila RabbitMQ
  └─ Retorna { id, status: "PENDENTE" } imediatamente

RabbitMQ (fila.video.processamento)
  │
  └─ OuvinteMensagemVideo (ate 5 consumidores paralelos)
       ├─ Atualiza status -> PROCESSANDO
       ├─ FFmpeg extrai frames (1 frame/segundo)
       ├─ Compacta frames em ZIP
       ├─ Atualiza status -> CONCLUIDO (ou FALHA + mensagem de erro)
       └─ Remove arquivo de video original
```

### Arquitetura de Software: Clean Architecture

```
interfaces/          ← Controllers REST, DTOs de entrada/saida
    └─ controlador/
    └─ dto/
    └─ excecao/

aplicacao/           ← Casos de uso (regras de negocio da aplicacao)
    └─ casosdeuso/
    └─ gateway/      ← Interfaces para servicos externos

dominio/             ← Entidades e regras de dominio puras (sem dependencias externas)
    └─ entidade/
    └─ enums/
    └─ excecao/
    └─ repositorio/  ← Interfaces de repositorio

infraestrutura/      ← Implementacoes: JPA, RabbitMQ, FFmpeg, JWT, filesystem
    └─ persistencia/
    └─ mensageria/
    └─ processador/
    └─ armazenamento/
    └─ seguranca/
```

### Decisoes Tecnicas

| Aspecto | Escolha | Justificativa |
|---|---|---|
| Linguagem | Java 17 + Spring Boot 3.2 | Ecossistema maduro, suporte a Records para DTOs imutaveis |
| Banco de dados | MySQL 8 | ACID, suporte JPA/Hibernate, amplamente adotado |
| Mensageria | RabbitMQ 3 | Filas persistentes, garantia de entrega, controle de concorrencia via prefetch |
| Autenticacao | JWT + Spring Security | Stateless, escalavel horizontalmente |
| Processamento de video | FFmpeg via ProcessBuilder | Padrao da industria para manipulacao de video |
| Armazenamento | Filesystem local com volume Docker | Simples para a escala atual; substituivel por S3 sem mudar casos de uso |
| Containerizacao | Docker + Docker Compose | Ambiente reproducivel, pronto para evolucao a Kubernetes |
| CI/CD | GitHub Actions | Integrado ao repositorio, sem custo adicional |
| Arquitetura | Clean Architecture (monolito) | Separacao clara de responsabilidades, testabilidade, facil evolucao para microsservicos |

---

## Requisitos

- Docker e Docker Compose instalados
- Porta 8080 disponivel (aplicacao)
- Porta 3306 disponivel (MySQL)
- Porta 5672 / 15672 disponivel (RabbitMQ)

---

## Executando o Projeto

### Subir o ambiente completo

```bash
docker-compose up --build
```

Aguarde todos os servicos ficarem saudaveis (MySQL e RabbitMQ tem healthcheck configurado).

A aplicacao estara disponivel em: **http://localhost:8080**

### Interface Web

Acesse http://localhost:8080/login.html para usar a interface grafica.

- Crie uma conta via "Registrar"
- Faca login
- Envie videos (ate 20 por vez, drag-and-drop suportado)
- Acompanhe o status em tempo real (auto-refresh a cada 5s)
- Baixe o ZIP com os frames quando o status for "Concluido"
- Uma notificacao aparece automaticamente quando algum video falha

### Parar o ambiente

```bash
docker-compose down
```

Para remover os volumes (dados persistidos):

```bash
docker-compose down -v
```

---

## API REST

Todos os endpoints de video requerem o header `Authorization: Bearer <token>`.

### Autenticacao

```bash
# Registrar usuario
curl -X POST http://localhost:8080/api/autenticacao/registrar \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@email.com","senha":"123456"}'

# Login (retorna JWT)
TOKEN=$(curl -s -X POST http://localhost:8080/api/autenticacao/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@email.com","senha":"123456"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

### Videos

```bash
# Enviar um video
curl -X POST http://localhost:8080/api/videos/enviar \
  -H "Authorization: Bearer $TOKEN" \
  -F "video=@meu-video.mp4"

# Enviar multiplos videos (ate 20)
curl -X POST http://localhost:8080/api/videos/enviar-lote \
  -H "Authorization: Bearer $TOKEN" \
  -F "videos=@video1.mp4" \
  -F "videos=@video2.mp4"

# Listar videos do usuario autenticado
curl http://localhost:8080/api/videos \
  -H "Authorization: Bearer $TOKEN"

# Baixar ZIP de frames
curl -O -J http://localhost:8080/api/videos/{id}/baixar \
  -H "Authorization: Bearer $TOKEN"
```

### Exemplo de resposta - listagem de videos

```json
[
  {
    "id": 1,
    "nomeOriginal": "aula.mp4",
    "status": "CONCLUIDO",
    "urlDownload": "/api/videos/1/baixar",
    "mensagemErro": null,
    "criadoEm": "2025-02-20T10:30:00"
  }
]
```

Valores possiveis para `status`: `PENDENTE`, `PROCESSANDO`, `CONCLUIDO`, `FALHA`

---

## Banco de Dados

O script de criacao das tabelas esta em `docker/init.sql` e e executado automaticamente pelo MySQL na primeira inicializacao.

### Schema

```sql
-- Usuarios do sistema
CREATE TABLE usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
);

-- Videos enviados por cada usuario
CREATE TABLE videos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT       NOT NULL,
    nome_original   VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    caminho_arquivo VARCHAR(500),
    caminho_zip     VARCHAR(500),
    mensagem_erro   TEXT,
    criado_em       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_videos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
```

---

## Testes

```bash
# Executar todos os testes (unit + integracao)
./mvnw clean verify

# Apenas testes unitarios
./mvnw test

# Script de testes E2E via curl (requer o ambiente rodando)
./scripts/e2e-tests.sh
```

Os testes de integracao usam **Testcontainers** (requer Docker disponivel na maquina).

---

## CI/CD

O pipeline do GitHub Actions (`.github/workflows/ci.yml`) executa automaticamente a cada push ou pull request para a branch `main`:

1. Build com Maven
2. Execucao de todos os testes (`mvn clean verify`)
3. Upload dos relatorios de teste como artefato
4. Build da imagem Docker

---

## Estrutura do Projeto

```
fiapx-video-processor/
├── docker/
│   └── init.sql                    # Script SQL de criacao das tabelas
├── docker-compose.yml              # App + MySQL + RabbitMQ
├── Dockerfile                      # Imagem Java 17 + FFmpeg
├── scripts/
│   ├── e2e-tests.sh                # Testes end-to-end via curl
│   └── test-parallel.sh            # Teste de processamento paralelo
├── src/
│   ├── main/
│   │   ├── java/br/com/fiapx/
│   │   │   ├── dominio/            # Entidades, enums, excecoes, interfaces de repositorio
│   │   │   ├── aplicacao/          # Casos de uso e interfaces de gateway
│   │   │   ├── infraestrutura/     # JPA, RabbitMQ, FFmpeg, JWT, filesystem
│   │   │   └── interfaces/         # Controllers REST, DTOs, tratador de excecoes
│   │   └── resources/
│   │       ├── application.yml     # Configuracao da aplicacao
│   │       └── static/             # Frontend (HTML, CSS, JS)
│   └── test/                       # Testes unitarios e de integracao
├── .github/workflows/ci.yml        # Pipeline CI/CD
└── pom.xml
```
