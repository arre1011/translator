# Development Guide

This guide covers setting up the development environment and common development tasks.

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21+ | Backend runtime |
| Node.js | 18+ | Mobile app tooling |
| Gradle | 8+ | Backend build (wrapper included) |
| Xcode | 15+ | iOS development |
| Android Studio | Latest | Android development |

### Required Accounts

- **OpenAI Account** with Realtime API access
- **Apple Developer Account** (for iOS device testing)

## Environment Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/translator.git
cd translator
```

### 2. Backend Setup

```bash
cd backend

# Create environment file (don't commit this!)
echo "OPENAI_API_KEY=sk-your-key-here" > .env

# Or export directly
export OPENAI_API_KEY=sk-your-key-here

# Verify Java version
java -version  # Should show 21+

# Run backend
./gradlew bootRun
```

### 3. Mobile Setup

```bash
cd mobile-app

# Install dependencies
npm install

# Generate native projects
npx expo prebuild

# iOS: Install pods
cd ios && pod install && cd ..
```

## Running the App

### Backend

```bash
cd backend

# Development mode with hot reload
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

Backend runs on `http://localhost:8080`.

### Mobile App

#### iOS Simulator

```bash
cd mobile-app
npx expo run:ios
```

#### Android Emulator

```bash
cd mobile-app
npx expo run:android
```

#### Physical Device

1. Find your computer's local IP address:
   ```bash
   # macOS
   ipconfig getifaddr en0

   # Linux
   hostname -I
   ```

2. Update API URL in `mobile-app/services/api/apiClient.ts`:
   ```typescript
   return 'http://YOUR_IP:8080/api';
   ```

3. Run on device:
   ```bash
   npx expo run:ios --device
   # or
   npx expo run:android --device
   ```

## Project Structure

### Backend

```
backend/
├── src/main/java/org/trans/backend/
│   ├── BackendApplication.java     # Entry point
│   ├── config/                     # Spring configuration
│   │   ├── OpenAiProperties.java   # OpenAI config binding
│   │   ├── QuotaProperties.java    # Quota config binding
│   │   ├── WebClientConfig.java    # HTTP client setup
│   │   └── CorsConfig.java         # CORS settings
│   ├── controller/                 # REST endpoints
│   │   ├── RealtimeSessionController.java
│   │   └── HealthController.java
│   ├── service/                    # Business logic
│   │   ├── SessionService.java     # Session orchestration
│   │   ├── QuotaService.java       # Quota management
│   │   ├── OpenAiRealtimeService.java
│   │   └── OpenAiWebSocketMonitor.java
│   ├── model/                      # Data models
│   │   ├── dto/                    # API DTOs
│   │   ├── domain/                 # Business entities
│   │   └── openai/                 # OpenAI models
│   ├── persistence/                # Data access
│   │   ├── SessionRepository.java
│   │   ├── QuotaRepository.java
│   │   └── impl/                   # In-memory impls
│   └── exception/                  # Error handling
└── src/main/resources/
    └── application.properties      # Configuration
```

### Mobile App

```
mobile-app/
├── app/                           # Expo Router screens
│   ├── _layout.tsx                # Root layout
│   └── (tabs)/
│       ├── _layout.tsx            # Tab configuration
│       └── index.tsx              # Translator screen
├── components/
│   ├── translator/                # Translation UI
│   │   ├── SpeakerPanel.tsx
│   │   ├── MicButton.tsx
│   │   ├── LanguageSelector.tsx
│   │   ├── TranscriptDisplay.tsx
│   │   ├── TranslationDisplay.tsx
│   │   └── PlayAudioButton.tsx
│   └── ui/                        # Shared UI components
├── services/
│   ├── api/                       # Backend API client
│   │   ├── apiClient.ts
│   │   ├── realtimeApi.ts
│   │   └── types.ts
│   └── webrtc/                    # WebRTC service
│       └── webrtcService.ts
├── hooks/                         # React hooks
│   └── use-translation-session.ts
├── constants/                     # App constants
│   └── languages.ts
└── assets/                        # Images, fonts
```

## Common Tasks

### Adding a New Language

1. Add to `mobile-app/constants/languages.ts`:
   ```typescript
   export const SUPPORTED_LANGUAGES: Language[] = [
     // ... existing languages
     { code: 'vi', name: 'Vietnamese', flag: '🇻🇳' },
   ];
   ```

2. Add to backend `OpenAiRealtimeService.java`:
   ```java
   private String getLanguageName(String code) {
       return switch (code.toLowerCase()) {
           // ... existing cases
           case "vi" -> "Vietnamese";
           default -> code;
       };
   }
   ```

### Modifying Quota Limits

Edit `backend/src/main/resources/application.properties`:

```properties
quota.default-audio-seconds-per-day=1200  # 20 minutes
quota.default-tokens-per-day=100000       # 100k tokens
```

### Adding a New API Endpoint

1. Add DTO in `model/dto/`:
   ```java
   public record NewRequest(String field) {}
   public record NewResponse(String result) {}
   ```

2. Add service method in `service/`:
   ```java
   public NewResponse doSomething(NewRequest request) {
       // Implementation
   }
   ```

3. Add controller endpoint:
   ```java
   @PostMapping("/new-endpoint")
   public ResponseEntity<NewResponse> newEndpoint(
       @Valid @RequestBody NewRequest request) {
       return ResponseEntity.ok(service.doSomething(request));
   }
   ```

### Adding a New UI Component

1. Create component in `components/translator/`:
   ```typescript
   // NewComponent.tsx
   export function NewComponent({ prop }: Props) {
     return <View>...</View>;
   }
   ```

2. Export from index (if using):
   ```typescript
   export { NewComponent } from './NewComponent';
   ```

3. Use in screen:
   ```typescript
   import { NewComponent } from '@/components/translator/NewComponent';
   ```

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "SessionServiceTest"

# With coverage
./gradlew test jacocoTestReport
```

### Mobile Tests

```bash
cd mobile-app

# Run Jest tests
npm test

# Watch mode
npm test -- --watch

# Coverage
npm test -- --coverage
```

## Debugging

### Backend Debugging

1. **IntelliJ IDEA**: Run `BackendApplication` in Debug mode
2. **VS Code**: Use Java Extension debugger
3. **Remote Debug**:
   ```bash
   ./gradlew bootRun --debug-jvm
   # Connect debugger to port 5005
   ```

### Mobile Debugging

1. **React Native Debugger**: Press `j` in terminal to open
2. **Chrome DevTools**: Shake device → Debug Remote JS
3. **Flipper**: Install Flipper for advanced debugging

### Logging

**Backend** - Set log levels in `application.properties`:
```properties
logging.level.org.trans.backend=DEBUG
logging.level.org.springframework.web=DEBUG
```

**Mobile** - Use console.log (visible in Metro):
```typescript
console.log('[WebRTC]', 'Connection state:', state);
```

## Code Style

### Backend (Java)

- Use Java Records for DTOs
- Follow Spring conventions
- Use constructor injection
- Add `@Slf4j` for logging

### Mobile (TypeScript)

- Use functional components
- Use TypeScript strictly
- Follow React hooks rules
- Use named exports

## Git Workflow

1. Create feature branch:
   ```bash
   git checkout -b feature/new-feature
   ```

2. Make changes and commit:
   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

3. Push and create PR:
   ```bash
   git push origin feature/new-feature
   ```

### Commit Message Format

```
type(scope): description

[optional body]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## Troubleshooting

### "Cannot find module 'react-native-webrtc'"

```bash
cd mobile-app
npx expo prebuild --clean
npm install
```

### Backend won't start

1. Check Java version: `java -version` (needs 21+)
2. Check port 8080 is free: `lsof -i :8080`
3. Check OpenAI API key is set

### WebRTC connection fails

1. Check microphone permissions
2. Verify backend is reachable
3. Check OpenAI API key has Realtime access
4. Look for errors in Metro console

### iOS build fails

```bash
cd mobile-app/ios
pod deintegrate
pod install
cd ..
npx expo run:ios
```

### Android build fails

```bash
cd mobile-app/android
./gradlew clean
cd ..
npx expo run:android
```
