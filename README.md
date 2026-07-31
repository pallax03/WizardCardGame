# Wizard Card Game
Final project of the course: Distributed Systems (87474)

## Instructions

### Application Run

#### Test in Deployment
- run redis
```bash
docker compose up
```
- run wizard game engine
```bash
sbt run
```
- run application (frontend)
```bash
npm run dev
```

#### Test Production
```bash
docker compose up --build
```
