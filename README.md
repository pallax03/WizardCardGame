# Wizard Card Game

This project implements a multiplayer version of the **Wizard** card game. It features a microservices architecture relying on an asynchronous Scala backend (using Vert.x and Prolog for game logic) and a modern React/Next.js frontend.

## Architecture & Tech Stack

- Backend (`./wizardEngine`): Written in **Scala 3**, it handles game state, AI computation, and client connections
  - **Vert.x**: Asynchronous framework handling the HTTP server, WebSockets, and Redis client.
  - **tuProlog (2p-core)**: Logic programming engine used to implement and evaluate the game's core rules.
  - **Tapir**: Defines HTTP endpoints and auto-generates the Swagger documentation.
  - **Roles**: Can be instantiated as the main `engine` (handling WS/HTTP) or as a `bot_worker` (handling AI logic autonomously).
- Frontend (`./application`): Client-side web application built with **Next.js**
- Infrastructure
  - **Redis**: Used as an in-memory datastore and Pub/Sub message broker to sync state between the Engine and the Bot Workers.
  - **Caddy**: Lightweight reverse proxy to efficiently route traffic to the Frontend, API, and WebSockets.

---

## Getting Started

You can run this project in different ways depending on your needs. No `.env` file is required unless you want to override default settings.

### Start the full stack (including Frontend)
To start the entire environment (including the web application) using Docker Compose, you must explicitly enable the `frontend` profile:
```bash
docker compose --profile frontend up --build
```

### Start the cluster (except Frontend)
By default, the frontend is excluded from the default profile. This allows you to run just the backend architecture (Redis, Engine, 3 Bot Workers, and Caddy) via Docker Compose:
```bash
docker compose up --build
```

### Start specific services (Local Development)
If you want to debug or develop individual components, you can start only the necessary dependencies via Docker and run the rest natively.

**1. Infrastructure (Redis)**
Start only the Redis container by specifying its compose service name:
```bash
docker compose up redis
```

**2. Backend (Scala)**
Navigate to the `wizardEngine` directory and use `sbt`:
- **Start the Main Engine**: 
  ```bash
  sbt run
  ```
  *(Exposes HTTP on port `5001` and WS on port `5002`)*

- **Start a Bot Worker** (in a separate terminal): 
  ```bash
  ROLE=bot_worker sbt run
  ```

**3. Frontend (Next.js)**
Navigate to the `application` directory and use `npm`:
```bash
npm install
npm run dev
```
*(Available on [http://localhost:3000](http://localhost:3000))*

### Accessing the application
Once started, you can access the services at:
- **Game (Frontend)**: [http://localhost:3000](http://localhost:3000)
- **API Docs (Swagger)**: [http://localhost:5001/docs](http://localhost:5001/docs)