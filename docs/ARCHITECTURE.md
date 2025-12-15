# Architecture Documentation

## Overview

The Face-to-Face Translator App is a real-time translation system that uses OpenAI's Realtime API for speech-to-speech translation. The architecture is designed for low latency while maintaining server-side control over sessions and usage tracking.

## System Components

### 1. Mobile Client (React Native / Expo)

The mobile client handles:
- User interface for two speakers facing each other
- WebRTC connection management
- Audio capture and playback
- Session state management

**Key Technologies:**
- React Native 0.81.5
- Expo 54
- react-native-webrtc for WebRTC
- Axios for HTTP requests

### 2. Backend Server (Spring Boot)

The backend handles:
- Session orchestration
- Quota management
- OpenAI API communication
- Usage tracking via WebSocket monitoring

**Key Technologies:**
- Spring Boot 4.0
- Java 21
- WebFlux (WebClient for reactive HTTP)
- WebSocket client for monitoring

### 3. OpenAI Realtime API

OpenAI provides:
- WebRTC endpoint for audio streaming
- Real-time speech recognition
- Translation using GPT models
- Text-to-speech for translated output

## Data Flow

### Session Start Flow

```
┌──────────┐     ┌─────────┐     ┌────────┐
│  Mobile  │     │ Backend │     │ OpenAI │
└────┬─────┘     └────┬────┘     └───┬────┘
     │                │              │
     │ 1. Create WebRTC Offer        │
     │──────────────► │              │
     │                │              │
     │                │ 2. Check Quota
     │                │──────┐       │
     │                │      │       │
     │                │◄─────┘       │
     │                │              │
     │                │ 3. POST /v1/realtime/sessions
     │                │─────────────►│
     │                │              │
     │                │ 4. SDP Answer + Call ID
     │                │◄─────────────│
     │                │              │
     │                │ 5. Start Monitoring WebSocket
     │                │─────────────►│
     │                │              │
     │ 6. SDP Answer  │              │
     │◄───────────────│              │
     │                │              │
     │ 7. WebRTC Connection          │
     │───────────────────────────────►
     │                │              │
```

### Audio Translation Flow

```
┌──────────┐                    ┌────────┐     ┌─────────┐
│  Mobile  │                    │ OpenAI │     │ Backend │
└────┬─────┘                    └───┬────┘     └────┬────┘
     │                              │               │
     │ 1. Audio Stream (WebRTC)     │               │
     │─────────────────────────────►│               │
     │                              │               │
     │                              │ 2. Process Audio
     │                              │──────┐        │
     │                              │      │        │
     │                              │◄─────┘        │
     │                              │               │
     │ 3. Translated Audio (WebRTC) │               │
     │◄─────────────────────────────│               │
     │                              │               │
     │                              │ 4. Usage Event (WS)
     │                              │──────────────►│
     │                              │               │
     │                              │               │ 5. Update Usage
     │                              │               │──────┐
     │                              │               │      │
     │                              │               │◄─────┘
     │                              │               │
```

## Component Details

### Mobile App Components

#### WebRTC Service (`services/webrtc/webrtcService.ts`)

Manages the WebRTC connection lifecycle:

```typescript
class WebRTCService {
  // Initialize with microphone access
  async initialize(callbacks): Promise<void>

  // Create SDP offer for backend
  async createOffer(): Promise<string>

  // Set remote answer from OpenAI
  async setRemoteAnswer(sdp: string): Promise<void>

  // Control microphone
  setMicrophoneMuted(muted: boolean): void

  // Cleanup resources
  cleanup(): void
}
```

#### Translation Hook (`hooks/use-translation-session.ts`)

React hook managing translation state:

```typescript
function useTranslationSession() {
  return {
    // State
    sessionId, callId, isConnected, isConnecting,
    activeSpeaker, transcripts, translations,

    // Actions
    startSession, endSession, toggleMicrophone
  }
}
```

#### UI Components

| Component | Purpose |
|-----------|---------|
| `SpeakerPanel` | Container for each speaker's UI |
| `MicButton` | Large microphone button with states |
| `LanguageSelector` | Modal for language selection |
| `TranscriptDisplay` | Shows speech transcript |
| `TranslationDisplay` | Shows translated text |

### Backend Components

#### Controller Layer

**RealtimeSessionController** - REST endpoints:

```java
@PostMapping("/start")
SessionStartResponse startSession(SessionStartRequest request)

@PostMapping("/end")
SessionEndResponse endSession(SessionEndRequest request)

@GetMapping("/{sessionId}")
SessionStatusResponse getStatus(String sessionId)
```

#### Service Layer

**SessionService** - Orchestrates session lifecycle:

```java
public SessionStartResponse startSession(request) {
    // 1. Check quota
    // 2. Call OpenAI
    // 3. Store session
    // 4. Start monitoring
    // 5. Return response
}
```

**QuotaService** - Manages usage limits:

```java
public boolean hasRemainingQuota(userId)
public boolean deductUsage(userId, audioSeconds, tokens)
public UserQuota getQuota(userId)
```

**OpenAiRealtimeService** - OpenAI API client:

```java
public OpenAiCallResponse createWebRtcCall(
    sdpOffer, sourceLang, targetLang
)
```

**OpenAiWebSocketMonitor** - Usage tracking:

```java
public void startMonitoring(callId, sessionId)
public void stopMonitoring(sessionId)
public UsageRecord getCurrentUsage(sessionId)
```

#### Repository Layer

In-memory implementations using `ConcurrentHashMap`:

```java
interface SessionRepository {
    TranslationSession save(session)
    Optional<TranslationSession> findById(sessionId)
    List<TranslationSession> findByUserId(userId)
}

interface QuotaRepository {
    UserQuota getOrCreate(userId, defaultAudio, defaultTokens)
    Optional<UserQuota> findByUserId(userId)
}
```

## Data Models

### Domain Models

```java
// Translation session
record TranslationSession(
    String sessionId,
    String callId,
    String userId,
    String sourceLang,
    String targetLang,
    Instant startedAt,
    SessionStatus status,
    UsageRecord usageRecord
)

// User quota
record UserQuota(
    String userId,
    int audioSecondsRemaining,
    int tokensRemaining,
    LocalDate quotaResetDate
)

// Usage tracking
record UsageRecord(
    int inputTokens,
    int outputTokens,
    int audioInputSeconds,
    int audioOutputSeconds
)
```

### API DTOs

```java
// Request to start session
record SessionStartRequest(
    String userId,
    String sourceLang,
    String targetLang,
    String sdpOffer
)

// Response with SDP answer
record SessionStartResponse(
    String sessionId,
    String callId,
    String sdpAnswer,
    Instant expiresAt
)
```

## Security Considerations

### Current (MVP)

- User ID passed in request body
- No authentication/authorization
- CORS allows all origins

### Production Recommendations

1. **Authentication**: Add JWT or OAuth2
2. **Rate Limiting**: Implement per-user rate limits
3. **HTTPS**: Use TLS for all connections
4. **API Key Security**: Store keys in secure vault
5. **Input Validation**: Validate all inputs strictly
6. **Logging**: Audit all session operations

## Scalability

### Current Limitations

- In-memory storage (data lost on restart)
- Single instance only
- No load balancing

### Scaling Strategy

1. **Database**: Replace in-memory with PostgreSQL/Redis
2. **Stateless Backend**: Use external session store
3. **Horizontal Scaling**: Add load balancer
4. **WebSocket Scaling**: Use Redis pub/sub for monitoring

```
                    ┌─────────────┐
                    │Load Balancer│
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Backend  │    │ Backend  │    │ Backend  │
    │    #1    │    │    #2    │    │    #3    │
    └────┬─────┘    └────┬─────┘    └────┬─────┘
         │               │               │
         └───────────────┼───────────────┘
                         ▼
                  ┌─────────────┐
                  │   Redis     │
                  │  (Sessions) │
                  └─────────────┘
```

## Error Handling

### Backend Errors

| Exception | HTTP Status | Code |
|-----------|-------------|------|
| `QuotaExceededException` | 402 | `QUOTA_EXCEEDED` |
| `SessionNotFoundException` | 404 | `SESSION_NOT_FOUND` |
| `OpenAiException` | 502 | `OPENAI_ERROR` |
| Validation errors | 400 | `VALIDATION_ERROR` |

### Mobile Error Handling

```typescript
try {
  await startSession(userId, source, target);
} catch (error) {
  if (error.code === 'QUOTA_EXCEEDED') {
    Alert.alert('Quota Exceeded', 'Daily limit reached');
  } else {
    Alert.alert('Error', error.message);
  }
}
```

## Monitoring & Observability

### Logging

Backend logs include:
- Session start/end with IDs
- OpenAI API calls and responses
- WebSocket connection status
- Usage accumulation events
- Error details

### Metrics to Track

1. **Performance**: Session start latency, WebRTC connection time
2. **Usage**: Active sessions, tokens consumed, audio minutes
3. **Errors**: API failures, WebSocket disconnections
4. **Business**: Sessions per user, popular language pairs

## Future Enhancements

1. **Transcript Display**: Stream transcripts to mobile via WebSocket
2. **Conversation History**: Store and replay past translations
3. **Offline Mode**: Cache recent translations
4. **Multi-Party**: Support more than 2 speakers
5. **Custom Voices**: Let users choose TTS voice
6. **Pronunciation Feedback**: Help users learn pronunciation
