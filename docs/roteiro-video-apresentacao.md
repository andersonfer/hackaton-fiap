# Roteiro do Vídeo de Apresentação — FIAP X Video Processor

## Context
O vídeo de apresentação é um dos entregáveis obrigatórios do Hackathon SOAT10. Deve ter no máximo 10 minutos e cobrir três tópicos: documentação, arquitetura escolhida e o projeto funcionando. O roteiro abaixo mapeia cada trecho do vídeo a um requisito do PDF, garantindo cobertura completa dos critérios de avaliação.

---

## Requisitos a demonstrar

| Requisito | Como demonstrar |
|-----------|----------------|
| Processar múltiplos vídeos simultâneos | Upload de 3 vídeos + painel RabbitMQ |
| Não perder requisições em picos | Explicar fila RabbitMQ + mostrar mensagens enfileiradas |
| Protegido por usuário e senha | Demo registro + login + JWT no header |
| Listagem de status dos vídeos | Dashboard com PENDENTE → PROCESSANDO → CONCLUIDO |
| Notificação de erro | Upload de arquivo inválido → status FALHA na tela |
| Persistência de dados | Mostrar MySQL no docker-compose |
| Arquitetura escalável | Diagrama + Docker Compose + Clean Architecture |
| Versionado no GitHub | Abrir repositório no browser |
| Testes de qualidade | Mostrar arquivos de teste + resultado do CI |
| CI/CD | Mostrar GitHub Actions pipeline executado |

---

## Roteiro — 10 minutos

### PARTE 1 — Introdução [0:00–1:00] (1 min)

**O que falar:**
> "Olá, este vídeo apresenta o projeto FIAP X — um sistema de processamento de vídeos desenvolvido para o Hackathon SOAT10. O sistema recebe vídeos, extrai frames com FFmpeg e disponibiliza um ZIP para download, de forma assíncrona, segura e escalável."

**O que mostrar na tela:**
- README.md aberto no GitHub (visão geral)
- Ou slide simples com o nome do projeto e stack

---

### PARTE 2 — Documentação e Arquitetura [1:00–3:00] (2 min)

**O que falar:**
> "A arquitetura segue Clean Architecture em camadas: Domínio, Aplicação, Infraestrutura e Interfaces. O processamento é assíncrono via RabbitMQ — o upload retorna imediatamente com status PENDENTE, enquanto até 5 workers processam vídeos em paralelo."

**O que mostrar na tela:**
1. Diagrama ASCII do README (seção de arquitetura) — ≈30s
2. Estrutura de pastas no VS Code/terminal mostrando as 4 camadas — ≈30s
3. docker-compose.yml destacando os 3 serviços: app, mysql, rabbitmq — ≈30s
4. Fluxo no README: POST → RabbitMQ → FFmpeg → ZIP — ≈30s

**Requisitos cobertos:** arquitetura escalável, documentação

---

### PARTE 3 — Demo do Projeto Funcionando [3:00–9:00] (6 min)

#### 3.1 — Autenticação [3:00–4:30] (1:30 min)

**O que fazer:**
1. Abrir http://localhost:8080 — mostrar tela de login
2. Clicar em "Registrar", preencher e-mail e senha, confirmar
3. Mostrar mensagem "Conta criada com sucesso"
4. Fazer login com as credenciais criadas
5. Chegar no dashboard
6. Tentar acessar em aba anônima sem login → redireciona para login

**O que falar:**
> "O sistema exige cadastro e autenticação via JWT. Sem token válido, o acesso ao dashboard é bloqueado."

**Requisitos cobertos:** protegido por usuário e senha

---

#### 3.2 — Upload e Processamento Paralelo [4:30–7:00] (2:30 min)

**O que fazer:**
1. No dashboard, selecionar **3 vídeos .mp4** de uma vez e enviar
2. Mostrar os 3 aparecendo na lista com status **PENDENTE**
3. Abrir http://localhost:15672 em outra aba (fiapx/fiapx123)
4. Mostrar fila `fila.video.processamento` com mensagens enfileiradas
5. Voltar para o dashboard e aguardar atualização automática
6. Mostrar status evoluindo: PENDENTE → PROCESSANDO → CONCLUIDO nos 3 vídeos

**O que falar:**
> "O upload é não-bloqueante: retorna imediatamente e a fila RabbitMQ garante que nenhuma requisição seja perdida, mesmo em picos. Até 5 workers processam vídeos em paralelo."

**Requisitos cobertos:** processamento múltiplo, não perder requisições, listagem de status

---

#### 3.3 — Download do ZIP [7:00–7:45] (45s)

**O que fazer:**
1. Clicar no botão de download de um vídeo com status CONCLUIDO
2. Mostrar o arquivo .zip sendo baixado
3. Extrair o ZIP e mostrar as imagens .png (frames)

**O que falar:**
> "Após o processamento, o ZIP com os frames fica disponível para download diretamente pelo dashboard."

**Requisitos cobertos:** funcionalidade principal do sistema

---

#### 3.4 — Tratamento de Erro e Isolamento [7:45–9:00] (1:15 min)

**O que fazer:**
1. Tentar fazer upload de um arquivo `.jpg` → mostrar mensagem de erro
2. Abrir aba anônima, registrar Usuário 2, fazer login
3. Mostrar lista vazia (não vê vídeos do Usuário 1)

**O que falar:**
> "Arquivos inválidos são rejeitados com mensagem de erro. Cada usuário enxerga apenas seus próprios vídeos — isolamento garantido pela autenticação JWT."

**Requisitos cobertos:** notificação de erro, isolamento entre usuários

---

### PARTE 4 — Qualidade e CI/CD [9:00–10:00] (1 min)

**O que fazer:**
1. Abrir o repositório no GitHub
2. Mostrar aba "Actions" com o último pipeline executado (verde)
3. Expandir o job para mostrar etapas: build → test → docker build
4. Mostrar rapidamente a pasta src/test no código (mencionar 14 arquivos de teste)

**O que falar:**
> "O projeto tem 14 arquivos de testes — unitários, de integração e e2e. O pipeline de CI/CD no GitHub Actions executa todos os testes automaticamente a cada push para a branch main."

**Requisitos cobertos:** testes de qualidade, CI/CD, versionado no GitHub

---

## Checklist antes de gravar

- [ ] `docker-compose up --build` no ar e estável
- [ ] 3 arquivos .mp4 curtos (5-10s) prontos na área de trabalho
- [ ] 1 arquivo .jpg pronto para o teste de erro
- [ ] Painel RabbitMQ acessível em http://localhost:15672
- [ ] GitHub Actions com pelo menos 1 execução verde recente
- [ ] Resolução de tela adequada para gravação (zoom no browser se necessário)
- [ ] Banco limpo (sem vídeos de testes anteriores) para demo limpa

## Dica de gravação
Grave a Parte 3.2 (processamento paralelo) por último ou use vídeos muito curtos (5s), pois o processamento precisa ser rápido o suficiente para aparecer no tempo da gravação. Alternativamente, deixe vídeos pré-processados na lista e grave apenas o status final.
