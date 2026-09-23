# Wizard Card Game
Final project of the course: Distributed Systems (87474)

## Instructions

### Application Run

#### no frontend
- run redis and backend
```bash
docker compose up wizard-engine redis -d
```
- run application (frontend)
```bash
cd ./application
npm run dev
```

#### All up
```bash
docker compose up --build
```
