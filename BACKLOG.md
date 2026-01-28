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

### ⏳ Git: Commit
- [ ] Commit da Entrega 4 ← ATUAL

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
