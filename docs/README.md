# ⚽ DaChamp — Gerenciamento de Campeonatos Esportivos

**Projeto: DaChamp**

Sistema web completo para criação e gerenciamento de campeonatos esportivos, com suporte a pontos corridos e mata-mata, controle de partidas em tempo real e estatísticas automáticas.

---

## 1. Tema Escolhido

Um **sistema de gerenciamento de campeonatos esportivos** onde:

- **Gerenciadores** criam campeonatos (pontos corridos ou mata-mata), adicionam times, iniciam campeonatos, controlam partidas em tempo real (timer, gols), e visualizam estatísticas.
- **Atletas** criam times, adicionam membros (com número de camisa), recebem notificações por e-mail ao serem adicionados a times ou campeonatos, e visualizam estatísticas.

### Funcionalidades principais:
- Cadastro de usuários (gerenciador/atleta) com autenticação JWT
- Criação de times com capitão e membros
- Criação de campeonatos (pontos corridos com turno/returno ou mata-mata com chaveamento)
- Geração automática de rodadas e chaveamentos (com opção de sorteio)
- Controle de partida em tempo real (iniciar, pausar, retomar, +30s/-30s, registrar gols, finalizar)
- Geração automática de estatísticas (classificação, artilharia) ao finalizar partidas
- Notificações por e-mail

---

## 2. Justificativa dos Bancos de Dados

O projeto implementa **Polyglot Persistence** — cada banco foi escolhido pelo tipo de dado que armazena:

### 🐘 PostgreSQL (RDB — Banco Relacional)
**O que armazena:** Usuários, Times, Membros de Times, Campeonatos, Partidas, Gols.

**Justificativa:** Estes dados possuem **relações fortes e integridade referencial** — um gol pertence a uma partida, que pertence a um campeonato, que tem times, que têm membros. O modelo relacional garante consistência (foreign keys, constraints, transações ACID). Queries complexas como "todos os gols de um atleta em um campeonato" são naturais em SQL com JOINs.

### 🍃 MongoDB (DB1 — Document Store)
**O que armazena:** Estatísticas de partidas e classificação de campeonatos.

**Justificativa:** Estatísticas são **documentos flexíveis e denormalizados** que variam por esporte e crescem ao longo do tempo. Um documento de estatísticas de partida contém listas aninhadas de gols, stats por time (que podem ter campos diferentes por esporte), e um snapshot completo do resultado. O modelo de documentos do MongoDB é ideal para armazenar dados semi-estruturados que são lidos como um todo (não precisam de JOINs) e que podem evoluir sem migrações de schema.

### 🔴 Redis (DB2 — Key-Value / In-Memory Store)
**O que armazena:** Estado do timer de partidas ao vivo, cache de classificação, sessões.

**Justificativa:** O timer de uma partida ao vivo precisa de **leitura/escrita com latência mínima** — o gerenciador interage a cada segundo. Redis, como banco in-memory, oferece operações O(1) para ler/atualizar o estado do timer (tempo decorrido, status). Também é usado como cache de classificação (evita recalcular a tabela a cada request) e pode servir como blacklist de tokens JWT. É um tipo de banco NoSQL diferente do MongoDB (key-value vs document store).

### Arquitetura:
```
Frontend (React/TS) ←→ Backend (Spring Boot) ←→ PostgreSQL (dados relacionais)
                                                ←→ MongoDB (estatísticas/docs)
                                                ←→ Redis (timer/cache/sessão)
```

---

## 3. Definição do Backend

**Tecnologia:** Java 21 + Spring Boot 3.2

**Justificativa:** Spring Boot oferece integração nativa com os 3 bancos via Spring Data (JPA para PostgreSQL, Spring Data MongoDB, Spring Data Redis), além de Spring Security (JWT), Spring Mail (notificações), e WebSocket (timer em tempo real).

### Serviços do Backend (organizado por domínio):

| Serviço | Responsabilidade | Banco Principal |
|---------|-----------------|-----------------|
| `AuthService` | Registro, login, JWT, CRUD de usuários | PostgreSQL |
| `TeamService` | CRUD de times, gerenciamento de membros | PostgreSQL |
| `ChampionshipService` | CRUD de campeonatos, geração de rodadas/chaveamento, início | PostgreSQL |
| `MatchService` | Controle de partidas, registro de gols | PostgreSQL + Redis |
| `StatisticsService` | Geração e consulta de estatísticas | MongoDB |
| `RedisService` | Timer de partida, cache de classificação | Redis |
| `EmailService` | Envio de notificações por e-mail | — | (TODO)

### Endpoints da API (principais):

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Cadastro de usuário |
| POST | `/api/auth/login` | Login (retorna JWT) |
| GET | `/api/auth/me` | Dados do usuário logado |
| POST | `/api/teams` | Criar time |
| POST | `/api/teams/{id}/members` | Adicionar membro ao time |
| GET | `/api/teams` | Listar todos os times |
| POST | `/api/championships` | Criar campeonato |
| POST | `/api/championships/{id}/teams/{teamId}` | Adicionar time ao campeonato |
| POST | `/api/championships/{id}/start` | Iniciar campeonato |
| POST | `/api/championships/{id}/bracket` | Definir/sortear chaveamento |
| POST | `/api/matches/{id}/start` | Iniciar partida |
| POST | `/api/matches/{id}/pause` | Pausar partida |
| POST | `/api/matches/{id}/resume` | Retomar partida |
| POST | `/api/matches/{id}/finish` | Finalizar partida |
| POST | `/api/matches/{id}/goals` | Registrar gol |
| POST | `/api/matches/{id}/timer/adjust` | Ajustar timer (+/-30s) |
| GET | `/api/statistics/championship/{id}` | Classificação e artilharia |
| GET | `/api/statistics/match/{id}` | Estatísticas da partida |

---

## 4. Como Executar o Projeto

### Pré-requisitos
- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/) instalados

### Executando com Docker Compose (recomendado)

```bash
# Clone o repositório
git clone https://github.com/PauloViniciusAF/CC6240-FutSite.git
cd CC6240-FutSite

# Suba todos os serviços
docker compose up --build
```

Isso irá:
1. Subir o **PostgreSQL** na porta 5432
2. Subir o **MongoDB** na porta 27017
3. Subir o **Redis** na porta 6379
4. Buildar e subir o **Backend** (Spring Boot) na porta 8080
5. Buildar e subir o **Frontend** (React) na porta 3000

Acesse: **http://localhost:3000**

### Executando em desenvolvimento (sem Docker)

**Backend:**
```bash
# Necessita: Java 21, PostgreSQL, MongoDB, Redis rodando localmente
cd backend
./gradlew bootRun
# API disponível em http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# Frontend disponível em http://localhost:5173
```

---

## 5. Tecnologias Utilizadas

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Frontend | React + TypeScript (Vite) | React 18, TS 5.3 |
| Backend | Java + Spring Boot | Java 21, Spring Boot 3.2 |
| RDB | PostgreSQL | 16 |
| DB1 (Document) | MongoDB | 7 |
| DB2 (Key-Value) | Redis | 7 |
| Auth | JWT (jjwt) | 0.12 |
| Infra | Docker Compose | 3.8 |

---

## 6. Estrutura do Projeto

```
CC6240-FutSite/
├── docker-compose.yml          # Orquestração de todos os serviços
├── README.md                   # Este arquivo
├── description/
│   └── projeto.md              # Descrição do projeto (disciplina)
├── backend/                    # Java Spring Boot
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/futsite/
│       ├── FutsiteApplication.java
│       ├── config/             # Security, CORS, Redis, WebSocket
│       ├── controller/         # REST endpoints
│       ├── dto/                # Request/Response DTOs
│       ├── exception/          # Error handling
│       ├── model/
│       │   ├── entity/         # JPA entities (PostgreSQL)
│       │   ├── document/       # MongoDB documents
│       │   └── enums/          # Enums compartilhados
│       ├── repository/
│       │   ├── postgres/       # JPA repositories
│       │   └── mongo/          # MongoDB repositories
│       ├── security/           # JWT filter, token provider
│       └── service/            # Business logic
└── frontend/                   # React TypeScript
    ├── package.json
    ├── Dockerfile
    ├── vite.config.ts
    └── src/
        ├── api.ts              # API client (Axios)
        ├── types.ts            # TypeScript interfaces
        ├── AuthContext.tsx      # Auth state management
        ├── App.tsx             # Routes
        ├── components/         # Shared components
        └── pages/              # Page components
```
