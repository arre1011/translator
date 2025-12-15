# Face-to-Face Translator App

A real-time face-to-face translation app using OpenAI's Realtime API with WebRTC for low-latency audio streaming. Built with **Spring Boot** (backend) and **React Native/Expo** (mobile).

## Features

- **Real-time Translation**: Low-latency speech-to-speech translation using OpenAI Realtime API
- **Face-to-Face Mode**: Split-screen UI where two speakers face each other (bottom panel rotated 180°)
- **15 Languages Supported**: English, Spanish, French, German, Italian, Portuguese, Chinese, Japanese, Korean, Arabic, Russian, Dutch, Polish, Turkish, Hindi
- **WebRTC Audio**: Direct audio streaming to OpenAI for minimal latency
- **Usage Tracking**: Server-side monitoring of tokens and audio seconds
- **Quota Management**: Configurable daily limits per user

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Mobile Client                          │
│  ┌──────────────┐                    ┌──────────────┐      │
│  │  Speaker A   │    Split Screen    │  Speaker B   │      │
│  │  (Top Half)  │◄──────────────────►│(Bottom Half) │      │
│  └──────────────┘                    └──────────────┘      │
│           │           WebRTC Audio            │            │
└───────────┼───────────────────────────────────┼────────────┘
            │                                   │
            └─────────────┬─────────────────────┘
                          │
        ┌─────────────────▼─────────────────┐
        │           Spring Boot             │
        │  1. Receive SDP Offer             │
        │  2. Check Quotas                  │
        │  3. Create OpenAI Session         │
        │  4. Return SDP Answer             │
        │  5. Monitor via WebSocket         │
        └─────────────────┬─────────────────┘
                          │
                          ▼
                 ┌────────────────┐
                 │ OpenAI Realtime│
                 │      API       │
                 └────────────────┘
```

## Project Structure

```
translator/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/org/trans/backend/
│   │   ├── config/            # Configuration classes
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── model/             # DTOs and domain models
│   │   ├── persistence/       # Repository layer
│   │   └── exception/         # Error handling
│   └── src/main/resources/
│       └── application.properties
│
└── mobile-app/                 # React Native / Expo
    ├── app/                    # Expo Router screens
    ├── components/translator/  # UI components
    ├── services/              # API and WebRTC services
    ├── hooks/                 # React hooks
    └── constants/             # Languages, config
```

## Prerequisites

- **Java 21** or higher
- **Node.js 18** or higher
- **Xcode** (for iOS) or **Android Studio** (for Android)
- **OpenAI API Key** with Realtime API access

## Quick Start

### 1. Backend Setup

```bash
cd backend

# Set your OpenAI API key
export OPENAI_API_KEY=sk-your-api-key-here

# Run the backend
./gradlew bootRun
```

The backend will start on `http://localhost:8080`.

### 2. Mobile App Setup

```bash
cd mobile-app

# Install dependencies
npm install

# Generate native projects (required for WebRTC)
npx expo prebuild

# Run on iOS
npx expo run:ios

# OR run on Android
npx expo run:android
```

> **Note**: This app requires a development build. It cannot run in Expo Go due to WebRTC native dependencies.

### 3. Configure API URL

For physical device testing, update the API base URL in `mobile-app/services/api/apiClient.ts`:

```typescript
const getBaseUrl = (): string => {
  if (__DEV__) {
    // Replace with your computer's local IP
    return 'http://192.168.1.XXX:8080/api';
  }
  return 'https://api.your-domain.com/api';
};
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# OpenAI Settings
openai.api-key=${OPENAI_API_KEY}
openai.base-url=https://api.openai.com
openai.realtime-model=gpt-4o-realtime-preview

# Quota Settings (per user per day)
quota.default-audio-seconds-per-day=600    # 10 minutes
quota.default-tokens-per-day=50000         # 50k tokens
```

### Mobile App Configuration

Update `mobile-app/app.json` for app-specific settings like bundle ID, permissions, and splash screen.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/realtime/session/start` | Start a translation session |
| `POST` | `/api/realtime/session/end` | End a session |
| `GET` | `/api/realtime/session/{id}` | Get session status |
| `GET` | `/api/health` | Health check |

### Start Session Request

```json
{
  "userId": "user_123",
  "sourceLang": "en",
  "targetLang": "es",
  "sdpOffer": "v=0\r\no=- ..."
}
```

### Start Session Response

```json
{
  "sessionId": "uuid",
  "callId": "call_xyz",
  "sdpAnswer": "v=0\r\no=- ...",
  "expiresAt": "2024-01-01T12:30:00Z"
}
```

## Supported Languages

| Code | Language | Code | Language |
|------|----------|------|----------|
| `en` | English | `zh` | Chinese |
| `es` | Spanish | `ja` | Japanese |
| `fr` | French | `ko` | Korean |
| `de` | German | `ar` | Arabic |
| `it` | Italian | `ru` | Russian |
| `pt` | Portuguese | `nl` | Dutch |
| `pl` | Polish | `tr` | Turkish |
| `hi` | Hindi | | |

## Development

### Running Tests

**Backend:**
```bash
cd backend
./gradlew test
```

**Mobile:**
```bash
cd mobile-app
npm test
```

### Building for Production

**Backend:**
```bash
cd backend
./gradlew build
# JAR file: build/libs/backend-0.0.1-SNAPSHOT.jar
```

**Mobile:**
```bash
cd mobile-app
npx expo build:ios    # iOS
npx expo build:android # Android
```

## Known Limitations (MVP)

1. **In-Memory Storage**: Sessions and quotas are stored in memory; restart clears data
2. **No Authentication**: User ID is passed directly; add proper auth for production
3. **Single Session**: Each user can have one active session at a time
4. **No Offline Mode**: Requires constant internet connection

## Troubleshooting

### WebRTC Connection Failed

- Ensure your device/simulator has microphone permissions
- Check that the backend is reachable from your device
- Verify your OpenAI API key has Realtime API access

### Audio Not Playing

- Check device volume and mute switch
- Ensure audio permissions are granted
- Try restarting the session

### Quota Exceeded Error

- Wait until the next day for quota reset
- Or increase limits in `application.properties`

## License

MIT License - See LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues and feature requests, please use the GitHub issue tracker.
