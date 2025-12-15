# API Documentation

## Base URL

- **Development**: `http://localhost:8080/api`
- **Production**: `https://api.your-domain.com/api`

## Authentication

Currently, the MVP uses a simple `userId` passed in the request body. For production, implement proper authentication (JWT, OAuth2).

---

## Endpoints

### Health Check

Check if the API is running.

```
GET /api/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "service": "translator-backend"
}
```

---

### Start Session

Create a new translation session and establish WebRTC connection with OpenAI.

```
POST /api/realtime/session/start
```

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "user_123456",
  "sourceLang": "en",
  "targetLang": "es",
  "sdpOffer": "v=0\r\no=- 4611731400430051336 2 IN IP4 127.0.0.1\r\n..."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier for the user |
| `sourceLang` | string | Yes | ISO 639-1 language code for source |
| `targetLang` | string | Yes | ISO 639-1 language code for target |
| `sdpOffer` | string | Yes | WebRTC SDP offer from client |

**Success Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "callId": "call_abc123xyz",
  "sdpAnswer": "v=0\r\no=- 1234567890 2 IN IP4 0.0.0.0\r\n...",
  "expiresAt": "2024-01-15T11:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | string | UUID for this session |
| `callId` | string | OpenAI call identifier |
| `sdpAnswer` | string | WebRTC SDP answer for client |
| `expiresAt` | string | ISO 8601 timestamp when session expires |

**Error Responses:**

*Quota Exceeded (402):*
```json
{
  "code": "QUOTA_EXCEEDED",
  "message": "Quota exceeded for user: user_123456",
  "status": 402,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

*Validation Error (400):*
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z",
  "errors": {
    "userId": "userId is required",
    "sourceLang": "sourceLang is required"
  }
}
```

*OpenAI Error (502):*
```json
{
  "code": "OPENAI_ERROR",
  "message": "Failed to communicate with OpenAI: Connection timeout",
  "status": 502,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### End Session

End an active translation session and get final usage statistics.

```
POST /api/realtime/session/end
```

**Request Body:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | string | Yes | Session ID from start response |

**Success Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "duration": "PT5M30S",
  "finalUsage": {
    "inputTokens": 1250,
    "outputTokens": 1180,
    "audioInputSeconds": 120,
    "audioOutputSeconds": 115,
    "totalTokens": 2430,
    "totalAudioSeconds": 235
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | string | Session identifier |
| `duration` | string | ISO 8601 duration |
| `finalUsage` | object | Usage statistics |
| `finalUsage.inputTokens` | number | Tokens used for input |
| `finalUsage.outputTokens` | number | Tokens used for output |
| `finalUsage.audioInputSeconds` | number | Seconds of audio input |
| `finalUsage.audioOutputSeconds` | number | Seconds of audio output |

**Error Response (404):**
```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "Session not found: 550e8400-...",
  "status": 404,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Get Session Status

Get the current status and usage of an active session.

```
GET /api/realtime/session/{sessionId}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | string | Session ID to query |

**Success Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "callId": "call_abc123xyz",
  "userId": "user_123456",
  "sourceLang": "en",
  "targetLang": "es",
  "status": "ACTIVE",
  "startedAt": "2024-01-15T10:30:00Z",
  "endedAt": null,
  "currentUsage": {
    "inputTokens": 500,
    "outputTokens": 480,
    "audioInputSeconds": 45,
    "audioOutputSeconds": 42,
    "totalTokens": 980,
    "totalAudioSeconds": 87
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `ACTIVE`, `ENDED`, or `ERROR` |
| `startedAt` | string | ISO 8601 start timestamp |
| `endedAt` | string | ISO 8601 end timestamp (null if active) |
| `currentUsage` | object | Current usage statistics |

---

## Language Codes

Supported ISO 639-1 language codes:

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

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `QUOTA_EXCEEDED` | 402 | User has exceeded daily quota |
| `SESSION_NOT_FOUND` | 404 | Session ID does not exist |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `OPENAI_ERROR` | 502 | OpenAI API communication error |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## Rate Limits

| Limit Type | Default Value | Scope |
|------------|---------------|-------|
| Audio seconds per day | 600 (10 min) | Per user |
| Tokens per day | 50,000 | Per user |

Quotas reset at midnight UTC.

---

## Example Usage (cURL)

### Start a Session

```bash
curl -X POST http://localhost:8080/api/realtime/session/start \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user_123",
    "sourceLang": "en",
    "targetLang": "es",
    "sdpOffer": "v=0\r\no=- 123 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"
  }'
```

### Check Session Status

```bash
curl http://localhost:8080/api/realtime/session/550e8400-e29b-41d4-a716-446655440000
```

### End a Session

```bash
curl -X POST http://localhost:8080/api/realtime/session/end \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## WebSocket Events (Backend Monitoring)

The backend monitors OpenAI's WebSocket for these events:

| Event Type | Description |
|------------|-------------|
| `session.created` | Session established |
| `session.updated` | Session configuration changed |
| `response.done` | Response complete (contains usage) |
| `error` | Error occurred |

Usage data is extracted from `response.done` events and accumulated per session.
