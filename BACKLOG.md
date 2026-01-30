# Backlog - FIAP X Video Processor

### ✅ Entrega 1: MVP Sincrono
- [x] Criar projeto Spring Boot
- [x] Estrutura Clean Architecture
- [x] Dominio: Video.java, StatusVideo.java
- [x] Aplicacao: EnviarVideo, BaixarVideo, gateways
- [x] Infraestrutura: ProcessadorVideoFFmpeg, ArmazenamentoArquivoLocal
- [x] Interfaces: VideoControlador (enviar + baixar)
- [x] Dockerfile (Java + FFmpeg)
- [x] Docker Compose basico
- [x] Testes unitarios dos casos de uso

### ✅ Entrega 2: Persistencia + Listagem
- [x] MySQL no Docker Compose
- [x] Entidades JPA e repositorios
- [x] GET /api/videos (listagem)
- [x] Testes de integracao

### ✅ Entrega 3: Processamento Assincrono
- [x] RabbitMQ no Docker Compose
- [x] PublicadorMensagemVideo
- [x] OuvinteMensagemVideo
- [x] Retorno assincrono no envio
- [x] Testes de integracao

### ✅ Entrega 4: Autenticacao JWT
- [x] Entidade Usuario (pre-cadastrado via SQL)
- [x] POST /api/autenticacao/login
- [x] POST /api/autenticacao/registrar (cadastro de usuario)
- [x] Spring Security + filtro JWT
- [x] Filtrar videos por usuario
- [x] Testes de autenticacao

### ✅ Entrega 5: Testes E2E + Refinamento
- [x] Testes E2E dos endpoints (fluxo completo)
- [x] Script de testes E2E via curl (scripts/e2e-tests.sh)
- [x] Video de teste (test-data/sample.mp4)
- [x] Script SQL de inicializacao (docker/init.sql)
- [x] Mapear regras de negocio e testar edge cases
  - ProcessarVideoTest (6 testes)
  - BaixarVideoTest (3 testes)
  - ListarVideosTest (4 testes)
  - GAP documentado: Acesso cross-user no download (teste @Disabled)

### ✅ Entrega 6: Concorrencia On-Demand
- [x] ConfiguracaoExecutor (ThreadPoolTaskExecutor elastico)
- [x] OuvinteMensagemVideo delega ao executor (ack imediato)
- [x] AgendadorReprocessamento (scheduler para videos travados)
- [x] Metodo buscarPorStatusEAtualizadoAntesDe no repositorio
- [x] Testes unitarios do AgendadorReprocessamento (4 testes)
- [x] application.yml: concorrencia=1, config reprocessamento

### ✅ Entrega 7: Interface Grafica (Frontend)
- [x] Tela de Login (login.html)
- [x] Tela Meus Videos (index.html) - upload, listagem, download
- [x] CSS (estilo.css) - design responsivo
- [x] Endpoint de envio em lote (POST /api/videos/enviar-lote)
- [x] Liberacao de recursos estaticos no Spring Security
- [x] Upload de ate 5 videos por vez com drag-and-drop
- [x] Auto-refresh da lista de videos (polling 5s)
- [x] Download de ZIP com autenticacao via JWT

### ✅ Entrega 8: CI/CD
- [x] GitHub Actions pipeline (build, test, docker)
- [x] Simplificar fluxo para PRs diretos na main (remover branch develop)
- [x] Branch protection na main (status check build, enforce admins, linear history)

### ⏳ Entrega 9: Refatorar Excecoes ← ATUAL
- [ ] Mover `CredenciaisInvalidasException` para `dominio/excecao/`
- [ ] Criar `ProcessamentoVideoException` para erros de processamento FFmpeg
- [ ] Criar `ArmazenamentoException` para erros de I/O no armazenamento
- [ ] Substituir `RuntimeException` generico nos pontos identificados
  - ProcessarVideo.java (video nao encontrado, falha processamento)
  - ProcessadorVideoFFmpeg.java (erro ffmpeg, erro zip)
  - ArmazenamentoArquivoLocal.java (erro salvar, ler, deletar)
- [ ] Adicionar handlers no `TratadorGlobalExcecoes`
- [ ] Atualizar testes afetados

### ⏳ Entrega 10: Rever Separacao entre Camadas
- [ ] Remover `@Service` de `AutenticarUsuario` e `RegistrarUsuario`
- [ ] Registrar ambos como `@Bean` em `ConfiguracaoCasosDeUso`
- [ ] Criar `GeradorTokenGateway` (interface na aplicacao) + implementacao na infraestrutura
- [ ] Criar `CodificadorSenhaGateway` (interface na aplicacao) + implementacao na infraestrutura
- [ ] Atualizar testes afetados

### ⏳ Entrega 11: Separar Validacao nos Casos de Uso
- [ ] Adicionar validacao de entrada em `EnviarVideo` (usuarioId, nomeArquivo, conteudo nao nulos)
- [ ] Adicionar validacao de entrada em `RegistrarUsuario` (email formato, senha tamanho minimo)
- [ ] Adicionar validacao de entrada em `AutenticarUsuario` (email e senha nao vazios)
- [ ] Mover validacao de limite de arquivos do `VideoControlador` para caso de uso
- [ ] Criar excecao `ValidacaoException` em `dominio/excecao/`
- [ ] Adicionar testes para cada validacao

### ⏳ Entrega 12: Documentacao
- [ ] README completo com instrucoes
- [ ] Documentacao da arquitetura (diagrama)

### ⏳ Entrega 13: Testes E2E de UI com Selenium
- [ ] Adicionar dependencias (selenium-java, webdrivermanager) no pom.xml
- [ ] Criar classe base de teste Selenium com setup/teardown do WebDriver
- [ ] Teste E2E: fluxo de login (login valido, credenciais invalidas)
- [ ] Teste E2E: fluxo de registro de usuario
- [ ] Teste E2E: upload de video via UI (drag-and-drop ou file input)
- [ ] Teste E2E: listagem de videos e verificacao de status
- [ ] Teste E2E: download de ZIP via UI
- [ ] Teste E2E: fluxo completo (login → upload → aguardar processamento → download)
- [ ] Configurar execucao headless para CI

### ⏳ Entrega 14: Migrar Testes E2E de UI para Playwright (OPCIONAL)
- [ ] Substituir Selenium por Playwright Java (com.microsoft.playwright)
- [ ] Reescrever testes aproveitando auto-wait e locators modernos
- [ ] Configurar trace viewer para debug de falhas
- [ ] Avaliar ganho de performance e estabilidade vs Selenium

### ⏳ Entrega 15: Kubernetes (OPCIONAL)
- [ ] Manifests k8s
- [ ] PersistentVolume
- [ ] Deploy MySQL/RabbitMQ
- [ ] Script para minikube

### ⏳ Entrega 16: Correcao de Seguranca (OPCIONAL)
- [ ] Validar propriedade do video no download (acesso cross-user)
  - Arquivo: BaixarVideo.java e/ou VideoControlador.java
  - Verificar se video.usuarioId == usuarioAutenticado.id antes de permitir download
  - Habilitar teste: VideoControladorTest.deveNegarAcessoAoBaixarVideoDeOutroUsuario()

### ⏳ Entrega 17: Cancelamento de Processamento (OPCIONAL)
- [ ] Novo status CANCELADO no enum StatusVideo
- [ ] Remover shell intermediario (sh -c) do ProcessadorVideoFFmpeg para destroy() confiavel
- [ ] Armazenar referencia ao Process por videoId (ConcurrentHashMap)
- [ ] Trocar waitFor() por loop com polling e verificacao de interrupcao
- [ ] Metodo cancelar(videoId) no ProcessadorVideoFFmpeg e na interface gateway
- [ ] Caso de uso CancelarVideo (orquestra cancelamento, cleanup de arquivos, atualiza status)
- [ ] Endpoint POST /api/videos/{id}/cancelar no VideoControlador
- [ ] Nao reprocessar videos com status CANCELADO no AgendadorReprocessamento
- [ ] Testes unitarios do cancelamento
