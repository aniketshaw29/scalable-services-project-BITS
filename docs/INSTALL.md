# Prerequisites — Installation Guide

Install everything you need to run Campus EventHub locally on macOS, Linux, or Windows.

---

## Java 17

### macOS

**Option A — Homebrew (recommended)**
```bash
brew install openjdk@17
# Add to shell profile (~/.zshrc or ~/.bash_profile)
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Option B — SDKMAN (manages multiple Java versions)**
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.11-tem
sdk use java 17.0.11-tem
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

### Linux (RHEL/Fedora)
```bash
sudo dnf install java-17-openjdk-devel -y
```

### Windows
Download and run the installer from [Adoptium](https://adoptium.net/temurin/releases/?version=17).
Choose the `.msi` installer for `Windows x64 JDK 17`.

### Verify
```bash
java -version
# Expected: openjdk version "17.x.x" ...
```

---

## Maven

### macOS
```bash
brew install maven
```

### Linux (Ubuntu/Debian)
```bash
sudo apt install maven -y
```

### Linux (RHEL/Fedora)
```bash
sudo dnf install maven -y
```

### Windows
Download the binary zip from [maven.apache.org](https://maven.apache.org/download.cgi),
extract to `C:\Program Files\Apache\maven`, and add `bin\` to your `PATH` environment variable.

### Verify
```bash
mvn -version
# Expected: Apache Maven 3.x.x ...
```

---

## PostgreSQL

### macOS

**Option A — Homebrew**
```bash
brew install postgresql@15
brew services start postgresql@15
# Add to shell profile
echo 'export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Option B — Postgres.app (GUI, easiest)**
Download from [postgresapp.com](https://postgresapp.com), drag to Applications, click "Initialize".

### Linux (Ubuntu/Debian)
```bash
sudo apt install postgresql postgresql-contrib -y
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Linux (RHEL/Fedora)
```bash
sudo dnf install postgresql-server postgresql-contrib -y
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Windows
Download and run the installer from [postgresql.org](https://www.postgresql.org/download/windows/).
During install, set the password for the `postgres` user to `postgres`.

### Docker (easiest option — no installation required)
```bash
docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:15-alpine
```

### Configure user password (native install only)

The app expects user `postgres` with password `postgres`. After installing:

```bash
# Connect as postgres superuser
sudo -u postgres psql        # Linux
psql -U postgres             # macOS / Windows

# Set password
ALTER USER postgres WITH PASSWORD 'postgres';
\q
```

### Create the 12 databases

```bash
for db in event_db registration_db venue_db attendance_db ticket_db notification_db \
          certificate_db feedback_db leaderboard_db announcement_db resource_db sponsor_db; do
  psql -U postgres -c "CREATE DATABASE $db;"
done
```

Or paste in `psql`:
```sql
CREATE DATABASE event_db;
CREATE DATABASE registration_db;
CREATE DATABASE venue_db;
CREATE DATABASE attendance_db;
CREATE DATABASE ticket_db;
CREATE DATABASE notification_db;
CREATE DATABASE certificate_db;
CREATE DATABASE feedback_db;
CREATE DATABASE leaderboard_db;
CREATE DATABASE announcement_db;
CREATE DATABASE resource_db;
CREATE DATABASE sponsor_db;
```

### Verify
```bash
psql -U postgres -c '\l'
# Should list all 12 databases
```

---

## RabbitMQ

### macOS
```bash
brew install rabbitmq
brew services start rabbitmq
```

Enable the management UI plugin (run once):
```bash
rabbitmq-plugins enable rabbitmq_management
```

### Linux (Ubuntu/Debian)
```bash
# Install Erlang first (RabbitMQ depends on it)
sudo apt install erlang -y

# Add RabbitMQ signing key and repo
curl -fsSL https://packagecloud.io/rabbitmq/rabbitmq-server/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/rabbitmq.gpg
echo "deb [signed-by=/usr/share/keyrings/rabbitmq.gpg] https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu focal main" | sudo tee /etc/apt/sources.list.d/rabbitmq.list

sudo apt update
sudo apt install rabbitmq-server -y
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# Enable management UI
sudo rabbitmq-plugins enable rabbitmq_management
```

### Linux (RHEL/Fedora)
```bash
sudo dnf install erlang rabbitmq-server -y
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server
sudo rabbitmq-plugins enable rabbitmq_management
```

### Windows
Download and install **Erlang OTP** first from [erlang.org](https://www.erlang.org/downloads),
then download and run the RabbitMQ installer from [rabbitmq.com](https://www.rabbitmq.com/install-windows.html).

After install, open RabbitMQ Command Prompt (Start Menu) and run:
```
rabbitmq-plugins enable rabbitmq_management
rabbitmq-service start
```

### Docker (easiest option — no installation required)
```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.12-management
```
The `-management` image has the UI plugin pre-installed.

### Verify
- AMQP port: `telnet localhost 5672` (or `nc -zv localhost 5672`)
- Management UI: open [http://localhost:15672](http://localhost:15672) → login `guest` / `guest`

---

## Docker (optional — for Docker Compose or running infra in containers)

### macOS / Windows
Download and install **Docker Desktop** from [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop).
Docker Compose v2 is bundled.

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install ca-certificates curl gnupg -y

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
sudo tee /etc/apt/sources.list.d/docker.list

sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-compose-plugin -y

# Allow running docker without sudo
sudo usermod -aG docker $USER
newgrp docker
```

### Verify
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
echo "=== PostgreSQL ===" && psql -U postgres -c 'SELECT version();'
echo "=== RabbitMQ ===" && curl -s -o /dev/null -w "%{http_code}" http://localhost:15672/api/overview -u guest:guest
```

Expected output:
```
=== Java ===
openjdk version "17.x.x" ...
=== Maven ===
Apache Maven 3.x.x ...
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
