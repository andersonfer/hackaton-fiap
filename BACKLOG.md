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

### ✅ Entrega 5b: Concorrencia On-Demand
- [x] ConfiguracaoExecutor (ThreadPoolTaskExecutor elastico)
- [x] OuvinteMensagemVideo delega ao executor (ack imediato)
- [x] AgendadorReprocessamento (scheduler para videos travados)
- [x] Metodo buscarPorStatusEAtualizadoAntesDe no repositorio
- [x] Testes unitarios do AgendadorReprocessamento (4 testes)
- [x] application.yml: concorrencia=1, config reprocessamento

### ✅ Entrega 5c: Interface Grafica (Frontend)
- [x] Tela de Login (login.html)
- [x] Tela Meus Videos (index.html) - upload, listagem, download
- [x] CSS (estilo.css) - design responsivo
- [x] Endpoint de envio em lote (POST /api/videos/enviar-lote)
- [x] Liberacao de recursos estaticos no Spring Security
- [x] Upload de ate 5 videos por vez com drag-and-drop
- [x] Auto-refresh da lista de videos (polling 5s)
- [x] Download de ZIP com autenticacao via JWT

### ⏳ Entrega 6: CI/CD + Documentacao
- [ ] GitHub Actions pipeline (build, test, docker)
- [ ] README completo com instrucoes
- [ ] Documentacao da arquitetura (diagrama)

### ⏳ Entrega 7: Kubernetes (OPCIONAL)
- [ ] Manifests k8s
- [ ] PersistentVolume
- [ ] Deploy MySQL/RabbitMQ
- [ ] Script para minikube

### ⏳ Entrega 8: Correcao de Seguranca (OPCIONAL)
- [ ] Validar propriedade do video no download (acesso cross-user)
  - Arquivo: BaixarVideo.java e/ou VideoControlador.java
  - Verificar se video.usuarioId == usuarioAutenticado.id antes de permitir download
  - Habilitar teste: VideoControladorTest.deveNegarAcessoAoBaixarVideoDeOutroUsuario()
