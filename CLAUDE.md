# Prompt para Desenvolvimento do Projeto FIAP X - Sistema de Processamento de Vídeos

## Contexto do Projeto

Estou desenvolvendo um projeto para o Hackathon SOAT10 da FIAP. O sistema é um **processador de vídeos** que extrai frames e gera um arquivo ZIP para download.

### Projeto Base (referência)

O projeto original era em Go e foi removido do repositório (apenas o PDF com a descrição do desafio foi mantido em `docs/`). O projeto base fazia o seguinte de forma síncrona:
- Recebe upload de vídeo via HTTP
- Usa FFmpeg para extrair frames (1 frame/segundo): `ffmpeg -i video.mp4 -vf "fps=1" frame_%04d.png`
- Compacta os frames em um ZIP
- Retorna o ZIP para download

**Problemas do projeto base que preciso resolver:**
- Processamento síncrono (usuário espera bloqueado)
- Sem autenticação
- Sem persistência (dados em memória/filesystem)
- Não escala horizontalmente
- Perde requisições se o servidor cair

### Requisitos Funcionais

1. Processar múltiplos vídeos simultaneamente (2-3 em paralelo)
2. Não perder requisições em caso de picos ou falhas
3. Sistema protegido por usuário e senha (JWT)
4. Listagem de status dos vídeos por usuário
5. Em caso de erro, o status deve refletir a falha (sem notificação por e-mail)

### Requisitos Técnicos

- Persistência em banco de dados (MySQL)
- Arquitetura escalável (preparada para k8s)
- Versionado no GitHub
- Testes automatizados (unitários + integração)
- CI/CD com GitHub Actions

---

## Decisões Técnicas

### Stack Definida
- **Linguagem**: Java 17+ com Spring Boot 3.x
- **Arquitetura**: Monolito com Clean Architecture
- **Banco de Dados**: MySQL 8
- **Mensageria**: RabbitMQ (para processamento assíncrono)
- **Containerização**: Docker + Kubernetes (minikube para desenvolvimento)
- **CI/CD**: GitHub Actions
- **Processamento de Vídeo**: FFmpeg executado via ProcessBuilder
- **Autenticação**: JWT simples (Spring Security)
- **Monitoramento**: Não será implementado nesta versão

### Convenção de Nomenclatura
**IMPORTANTE**: Todos os nomes de classes, variáveis, métodos e endpoints devem estar em **português brasileiro**. Exemplos:
- Classes: `EnviarVideo`, `ProcessadorVideoGateway`, `ArmazenamentoArquivoLocal`
- Variáveis: `caminhoArquivo`, `nomeOriginal`, `mensagemErro`
- Endpoints: `/api/videos/enviar`, `/api/videos/{id}/baixar`, `/api/autenticacao/registrar`
- Enums: `PENDENTE`, `PROCESSANDO`, `CONCLUIDO`, `FALHA`

### Estrutura do Projeto (Clean Architecture)

```
fiapx-video-processor/
├── docs/
│   └── Hack_SOAT10.pdf         # Descrição original do desafio (referência)
├── src/main/java/br/com/fiapx/
│   ├── dominio/                   # Camada de Domínio
│   │   ├── entidade/
│   │   │   ├── Usuario.java
│   │   │   └── Video.java
│   │   ├── enums/
│   │   │   └── StatusVideo.java   # PENDENTE, PROCESSANDO, CONCLUIDO, FALHA
│   │   ├── excecao/
│   │   │   └── (exceções de domínio)
│   │   └── repositorio/           # Interfaces dos repositórios
│   │       ├── UsuarioRepositorio.java
│   │       └── VideoRepositorio.java
│   │
│   ├── aplicacao/                 # Camada de Aplicação (Casos de Uso)
│   │   ├── casosdeuso/
│   │   │   ├── EnviarVideo.java
│   │   │   ├── ProcessarVideo.java
│   │   │   ├── ListarVideos.java
│   │   │   ├── BaixarVideo.java
│   │   │   └── AutenticarUsuario.java
│   │   ├── dto/
│   │   │   └── (DTOs de entrada/saída dos casos de uso)
│   │   └── gateway/               # Interfaces para serviços externos
│   │       ├── ProcessadorVideoGateway.java
│   │       ├── ArmazenamentoArquivoGateway.java
│   │       └── FilaMensagemGateway.java
│   │
│   ├── infraestrutura/            # Camada de Infraestrutura
│   │   ├── persistencia/
│   │   │   ├── entidade/          # Entidades JPA
│   │   │   ├── repositorio/       # Implementações Spring Data
│   │   │   └── mapeador/          # Conversores Dominio <-> JPA
│   │   ├── mensageria/
│   │   │   ├── ConfiguracaoRabbitMQ.java
│   │   │   ├── PublicadorMensagemVideo.java
│   │   │   └── OuvinteMensagemVideo.java
│   │   ├── processador/
│   │   │   └── ProcessadorVideoFFmpeg.java
│   │   ├── armazenamento/
│   │   │   └── ArmazenamentoArquivoLocal.java
│   │   └── seguranca/
│   │       ├── ServicoJwt.java
│   │       ├── ConfiguracaoSeguranca.java
│   │       └── FiltroAutenticacaoJwt.java
│   │
│   └── interfaces/                # Camada de Interface (API REST)
│       ├── controlador/
│       │   ├── AutenticacaoControlador.java
│       │   └── VideoControlador.java
│       ├── dto/
│       │   ├── requisicao/
│       │   └── resposta/
│       └── excecao/
│           └── TratadorExcecaoGlobal.java
│
├── src/main/resources/
│   ├── application.yml
│   └── application-docker.yml
│
├── src/test/java/br/com/fiapx/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
│
├── docker/
│   └── Dockerfile                 # Imagem Java + FFmpeg
├── docker-compose.yml             # App + MySQL + RabbitMQ
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── mysql-deployment.yaml
│   ├── rabbitmq-deployment.yaml
│   ├── app-deployment.yaml
│   ├── app-service.yaml
│   └── persistent-volume.yaml
├── .github/
│   └── workflows/
│       └── ci.yml
├── pom.xml
└── README.md
```

### Modelo de Dados

```sql
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nome_original VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    caminho_arquivo VARCHAR(500),
    caminho_zip VARCHAR(500),
    mensagem_erro TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
```

### Endpoints da API

```
# Autenticação
POST   /api/autenticacao/login      - Login (retorna JWT)

# Vídeos (requer autenticação)
POST   /api/videos/enviar           - Envio de vídeo (multipart/form-data)
GET    /api/videos                  - Lista vídeos do usuário autenticado
GET    /api/videos/{id}/baixar      - Download do ZIP
```

**Nota**: Usuários serão pré-cadastrados via script SQL (registro via API não é requisito do hackathon).

### Fluxo de Processamento Assíncrono

```
1. POST /api/videos/enviar
   ├── Valida JWT e extrai usuário
   ├── Salva arquivo no filesystem
   ├── Cria registro no banco (status=PENDENTE)
   ├── Publica mensagem na fila RabbitMQ
   └── Retorna {id, status: "PENDENTE"} imediatamente

2. OuvinteMensagemVideo (consome fila, concurrency=3)
   ├── Atualiza status para PROCESSANDO
   ├── Executa FFmpeg para extrair frames
   ├── Cria arquivo ZIP
   ├── Atualiza status para CONCLUIDO (ou FALHA com mensagem de erro)
   └── Remove arquivo de vídeo original
```

---

## Plano de Entregas Incrementais

**IMPORTANTE**: Cada entrega deve ser um software funcional. Os testes devem ser escritos junto com cada funcionalidade (não deixar para o final).

### Entrega 1: MVP Síncrono (~3-4 dias)

**Objetivo**: API funcional que processa vídeos de forma síncrona

**Tarefas**:
1. Criar projeto Spring Boot com estrutura Clean Architecture
2. Implementar `POST /api/videos/enviar` (síncrono)
3. Implementar `GET /api/videos/{id}/download`
4. Criar Dockerfile com Java + FFmpeg
5. Docker Compose apenas com a aplicação
6. Testes unitários dos casos de uso

**Critério de aceite**: 
```bash
curl -X POST -F "video=@video.mp4" http://localhost:8080/api/videos/enviar
# Retorna JSON com ID e link para download

curl -O http://localhost:8080/api/videos/1/download
# Baixa o arquivo ZIP
```

---

### Entrega 2: Persistência + Listagem (~2-3 dias)

**Objetivo**: Rastreabilidade dos vídeos processados

**Tarefas**:
1. Adicionar MySQL ao Docker Compose
2. Criar entidades JPA e repositórios
3. Implementar `GET /api/videos` (listagem)
4. Implementar `GET /api/videos/{id}` (detalhes)
5. Testes de integração dos repositórios

**Critério de aceite**:
```bash
curl http://localhost:8080/api/videos
# [{"id":1,"nomeOriginal":"video.mp4","status":"CONCLUIDO","createdAt":"..."}]
```

---

### Entrega 3: Processamento Assíncrono (~3-4 dias)

**Objetivo**: Resiliência - não perder requisições em picos ou falhas

**Tarefas**:
1. Adicionar RabbitMQ ao Docker Compose
2. Implementar PublicadorMensagemVideo
3. Implementar OuvinteMensagemVideo (concurrency=3)
4. Envio retorna imediatamente com status PENDENTE
5. Testes de integração da mensageria

**Critério de aceite**:
```bash
curl -X POST -F "video=@video.mp4" http://localhost:8080/api/videos/enviar
# {"id":1,"status":"PENDENTE"}

# Aguarda processamento...
curl http://localhost:8080/api/videos/1
# {"id":1,"status":"CONCLUIDO","zipPath":"..."}
```

---

### Entrega 4: Autenticação JWT (~2-3 dias)

**Objetivo**: Multiusuário com isolamento de dados

**Tarefas**:
1. Criar entidade Usuario e repositório
2. Implementar `POST /api/auth/register`
3. Implementar `POST /api/auth/login`
4. Configurar Spring Security com filtro JWT
5. Filtrar vídeos por usuário autenticado
6. Testes dos endpoints de autenticação

**Critério de aceite**:
```bash
# Registro
curl -X POST -H "Content-Type: application/json" \
  -d '{"email":"teste@email.com","senha":"123456"}' \
  http://localhost:8080/api/auth/register

# Login
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"teste@email.com","senha":"123456"}' \
  http://localhost:8080/api/auth/login | jq -r '.token')

# Envio autenticado
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F "video=@video.mp4" http://localhost:8080/api/videos/enviar

# Listagem só mostra vídeos do usuário
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/videos
```

---

### Entrega 5: Kubernetes (~2-3 dias)

**Objetivo**: Infraestrutura escalável

**Tarefas**:
1. Criar manifests k8s (Deployment, Service, ConfigMap, Secret)
2. Configurar PersistentVolume para arquivos
3. Deploy do MySQL no k8s
4. Deploy do RabbitMQ no k8s
5. Script para subir ambiente completo no minikube
6. Documentar comandos de deploy

**Critério de aceite**:
```bash
minikube start
kubectl apply -f k8s/
# Sistema rodando no cluster local
```

---

### Entrega 6: CI/CD + Documentação (~2-3 dias)

**Objetivo**: Pipeline automatizado e documentação para apresentação

**Tarefas**:
1. GitHub Actions: build → test → docker build → push
2. README completo com instruções
3. Documento de arquitetura (diagrama C4 ou similar)
4. Gravar vídeo de apresentação (máx 10 min)

---

## Instruções para Implementação

### Começando a Entrega 1

1. **Crie o projeto Spring Boot** com as dependências:
   - spring-boot-starter-web
   - spring-boot-starter-validation
   - spring-boot-starter-test

   **Nota sobre Java 17**: Não usar Lombok. Utilizar **Records** para DTOs e objetos imutáveis. Para entidades JPA (que precisam ser mutáveis), escrever getters/setters manualmente.

2. **Crie a estrutura de pastas** conforme Clean Architecture descrita acima

3. **Implemente na ordem**:
   - Dominio: `Video.java`, `StatusVideo.java`
   - Aplicacao: `EnviarVideo.java`, `ProcessadorVideoGateway.java`, `ArmazenamentoArquivoGateway.java`
   - Infraestrutura: `ProcessadorVideoFFmpeg.java`, `ArmazenamentoArquivoLocal.java`
   - Interfaces: `VideoControlador.java`

4. **Dockerfile** (Java 17 + FFmpeg):
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

5. **Escreva testes** para cada use case implementado

### Comandos úteis para testar

```bash
# Build e run local
./mvnw clean package -DskipTests
docker-compose up --build

# Testar envio
curl -X POST -F "video=@sample.mp4" http://localhost:8080/api/videos/enviar

# Ver logs do processamento
docker-compose logs -f app
```

---

## Observações Importantes

1. **Prazo**: 23/02/2025 (entrega formatada até ~21/02)
2. **Incremental**: Se não der tempo de terminar tudo, cada entrega é um software funcional
3. **Testes junto com código**: Não deixar testes para o final
4. **Simplicidade**: Prefira soluções simples que funcionam a soluções complexas incompletas
5. **Sem frontend**: Toda interação via curl/API REST
6. **Git**: Commits e pushes são responsabilidade do usuário, não do Claude Code. Apenas avise quando for um bom momento para commitar (ex: após completar uma tarefa do backlog).

---

## Gerenciamento do Backlog

**IMPORTANTE**: Mantenha um arquivo `BACKLOG.md` na raiz do projeto para acompanhamento do progresso.

### Estrutura do BACKLOG.md

O backlog deve ser simples e direto, contendo apenas as entregas e suas tarefas com checkboxes. Sem seções de status geral, datas, notas ou impedimentos.

```markdown
# Backlog - FIAP X Video Processor

### 🔄 Entrega 1: MVP Síncrono
- [x] Criar projeto Spring Boot
- [x] Estrutura Clean Architecture
- [ ] Implementar POST /api/videos/enviar ← ATUAL
- [ ] Implementar GET /api/videos/{id}/download
- [ ] Dockerfile (Java + FFmpeg)
- [ ] Docker Compose básico
- [ ] Testes unitários dos casos de uso

### ⏳ Entrega 2: Persistência + Listagem
- [ ] MySQL no Docker Compose
- [ ] Entidades JPA e repositórios
- [ ] GET /api/videos (listagem)
- [ ] GET /api/videos/{id} (detalhes)
- [ ] Testes de integração

### ⏳ Entrega 3: Processamento Assíncrono
- [ ] RabbitMQ no Docker Compose
- [ ] PublicadorMensagemVideo
- [ ] OuvinteMensagemVideo
- [ ] Retorno assíncrono no envio
- [ ] Testes de integração

### ⏳ Entrega 4: Autenticação JWT
- [ ] Entidade Usuario
- [ ] POST /api/auth/register
- [ ] POST /api/auth/login
- [ ] Spring Security + filtro JWT
- [ ] Filtrar vídeos por usuário
- [ ] Testes de autenticação

### ⏳ Entrega 5: Kubernetes
- [ ] Manifests k8s
- [ ] PersistentVolume
- [ ] Deploy MySQL/RabbitMQ
- [ ] Script para minikube
- [ ] Documentação de deploy

### ⏳ Entrega 6: CI/CD + Documentação
- [ ] GitHub Actions pipeline
- [ ] README completo
- [ ] Documento de arquitetura
- [ ] Vídeo de apresentação
```

### Regras para Manutenção do Backlog

1. **Atualize o backlog** sempre que completar uma tarefa
2. **Marque a tarefa atual** com "← ATUAL" para fácil identificação
3. **Use os ícones**:
   - ✅ Entrega completa
   - 🔄 Entrega em andamento
   - ⏳ Entrega pendente

### Ao Iniciar uma Sessão

Sempre que eu iniciar uma nova sessão de desenvolvimento, leia o `BACKLOG.md` para entender o estado atual e me informe qual é o próximo passo

---

## Comando Inicial

Comece criando o projeto Spring Boot com a estrutura de pastas da Entrega 1. Crie também o arquivo `BACKLOG.md` inicial. Implemente o fluxo de envio síncrono primeiro, com testes, e depois evoluímos para as próximas entregas.
