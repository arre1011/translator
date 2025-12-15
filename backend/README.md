# Translator Backend

Spring Boot backend for the Face-to-Face Translator App.

## Requirements

- Java 21+
- OpenAI API Key with Realtime API access

## Quick Start

```bash
# Set API key
export OPENAI_API_KEY=sk-your-key-here

# Run
./gradlew bootRun
```

Server starts at `http://localhost:8080`.

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# OpenAI
openai.api-key=${OPENAI_API_KEY}
openai.base-url=https://api.openai.com
openai.realtime-model=gpt-4o-realtime-preview

# Quotas (per user per day)
quota.default-audio-seconds-per-day=600
quota.default-tokens-per-day=50000
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/realtime/session/start` | Start translation session |
| POST | `/api/realtime/session/end` | End session |
| GET | `/api/realtime/session/{id}` | Get session status |
| GET | `/api/health` | Health check |

## Project Structure

```
src/main/java/org/trans/backend/
├── config/          # Configuration classes
├── controller/      # REST endpoints
├── service/         # Business logic
├── model/           # DTOs and domain objects
├── persistence/     # Repository layer
└── exception/       # Error handling
```

## Building

```bash
# Build JAR
./gradlew build

# Run JAR
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

## Testing

```bash
./gradlew test
```

## Docker (Future)

```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t translator-backend .
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-xxx translator-backend
```
