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

### 🔄 Git: Inicializar Repositorio
- [x] Criar .gitignore
- [ ] Primeiro commit ← ATUAL

### ⏳ Entrega 2: Persistencia + Listagem
- [ ] MySQL no Docker Compose
- [ ] Entidades JPA e repositorios
- [ ] GET /api/videos (listagem)
- [ ] Testes de integracao

### ⏳ Entrega 3: Processamento Assincrono
- [ ] RabbitMQ no Docker Compose
- [ ] PublicadorMensagemVideo
- [ ] OuvinteMensagemVideo
- [ ] Retorno assincrono no envio
- [ ] Testes de integracao

### ⏳ Entrega 4: Autenticacao JWT
- [ ] Entidade Usuario (pre-cadastrado via SQL)
- [ ] POST /api/autenticacao/login
- [ ] Spring Security + filtro JWT
- [ ] Filtrar videos por usuario
- [ ] Testes de autenticacao

### ⏳ Entrega 5: Kubernetes
- [ ] Manifests k8s
- [ ] PersistentVolume
- [ ] Deploy MySQL/RabbitMQ
- [ ] Script para minikube

### ⏳ Entrega 6: CI/CD + Documentacao
- [ ] GitHub Actions pipeline
- [ ] README completo
- [ ] Documento de arquitetura
- [ ] Video de apresentacao
