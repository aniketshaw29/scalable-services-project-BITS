# Prerequisites — Installation Guide

Install everything you need to run Campus EventHub locally on macOS.

**Required tools:**
- Java **21+** and Maven 3.8+ — for the 14 Spring Boot services
- Node.js 18+ and npm — for the React frontend
- PostgreSQL 13+ — one shared instance, 12 databases
- RabbitMQ 3.x — message broker
- Docker (optional) — for running infra in containers, or the full stack via Docker Compose

All tools below are installed via **Homebrew**. If you don't have Homebrew yet:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

---

## Java 21

```bash
brew install openjdk@21
```

Add to your shell profile (`~/.zshrc`):

```bash
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Verify
```bash
java -version
# Expected: openjdk version "21.x.x" ...
```

---

## Maven

```bash
brew install maven
```

### Verify
```bash
mvn -version
# Expected: Apache Maven 3.x.x ...
```

---

## Node.js

```bash
brew install node@22
```

Add to your shell profile if brew instructs you to:

```bash
echo 'export PATH="/opt/homebrew/opt/node@22/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Verify
```bash
node -v   # Expected: v22.x.x
npm -v    # Expected: 10.x.x
```

---

## PostgreSQL

Download and install **Postgres.app** — the easiest way to run PostgreSQL on macOS, no terminal setup needed.

1. Go to [postgresapp.com](https://postgresapp.com)
2. Download the latest release (PostgreSQL 16 or 17)
3. Move `Postgres.app` to your `/Applications` folder
4. Open the app and click **Initialize** to create your first server
5. Click **Start** — the elephant icon in the menu bar means it's running

Add the CLI tools to your PATH (paste into `~/.zshrc`):

```bash
echo 'export PATH="/Applications/Postgres.app/Contents/Versions/latest/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Set the postgres user password

```bash
psql postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"
```

### Create the 12 databases

```bash
for db in event_db registration_db venue_db attendance_db ticket_db notification_db \
          certificate_db feedback_db leaderboard_db announcement_db resource_db sponsor_db; do
  psql -U postgres -c "CREATE DATABASE $db;"
done
```

### Verify
```bash
psql -U postgres -c '\l'
# Should list all 12 databases
```

---

## RabbitMQ

```bash
brew install rabbitmq
brew services start rabbitmq
```

Enable the management UI plugin (run once):

```bash
rabbitmq-plugins enable rabbitmq_management
```

### Verify
```bash
# Check the management UI is up
open http://localhost:15672
# Login: guest / guest
```

---

## Docker (optional)

Required only if you want to run the full stack via Docker Compose or Kubernetes, or if you prefer Docker-managed PostgreSQL/RabbitMQ instead of Homebrew.

```bash
brew install --cask docker
open /Applications/Docker.app
```

Wait for Docker Desktop to finish starting, then verify:

```bash
docker --version
docker compose version
```

---

## Quick Verification Checklist

Run this to confirm everything is ready:

```bash
echo "=== Java ===" && java -version
echo "=== Maven ===" && mvn -version
echo "=== Node.js ===" && node -v && npm -v
echo "=== PostgreSQL ===" && psql -U postgres -c 'SELECT version();'
echo "=== RabbitMQ ===" && curl -s -o /dev/null -w "%{http_code}" http://localhost:15672/api/overview -u guest:guest
```

Expected output:
```
=== Java ===
openjdk version "21.x.x" ...
=== Maven ===
Apache Maven 3.x.x ...
=== Node.js ===
v22.x.x
10.x.x
=== PostgreSQL ===
PostgreSQL 15.x ...
=== RabbitMQ ===
200
```

---

## Ready to Run

Once all prerequisites are installed and the 12 databases exist:

```bash
# From the project root
./start-all.sh
```

See [RUNNING_LOCALLY.md](RUNNING_LOCALLY.md) for the full local run guide,
or [DOCKER.md](DOCKER.md) if you prefer Docker Compose.
