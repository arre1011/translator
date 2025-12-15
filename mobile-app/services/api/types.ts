// API Request/Response types

export interface SessionStartRequest {
  userId: string;
  sourceLang: string;
  targetLang: string;
  sdpOffer: string;
}

export interface SessionStartResponse {
  sessionId: string;
  callId: string;
  sdpAnswer: string;
  expiresAt: string; // ISO date string
}

export interface SessionEndRequest {
  sessionId: string;
}

export interface UsageRecord {
  inputTokens: number;
  outputTokens: number;
  audioInputSeconds: number;
  audioOutputSeconds: number;
  totalTokens: number;
  totalAudioSeconds: number;
}

export interface SessionEndResponse {
  sessionId: string;
  duration: string; // ISO duration
  finalUsage: UsageRecord;
}

export type SessionStatus = 'ACTIVE' | 'ENDED' | 'ERROR';

export interface SessionStatusResponse {
  sessionId: string;
  callId: string;
  userId: string;
  sourceLang: string;
  targetLang: string;
  status: SessionStatus;
  startedAt: string;
  endedAt: string | null;
  currentUsage: UsageRecord;
}

export interface ErrorResponse {
  code: string;
  message: string;
  status: number;
  timestamp: string;
}

// Language types
export interface Language {
  code: string;
  name: string;
  flag: string;
}
