import { apiClient } from './apiClient';
import {
  SessionStartRequest,
  SessionStartResponse,
  SessionEndRequest,
  SessionEndResponse,
  SessionStatusResponse,
} from './types';

export const realtimeApi = {
  /**
   * Start a new translation session
   * Creates WebRTC connection with OpenAI through the backend
   */
  startSession: async (request: SessionStartRequest): Promise<SessionStartResponse> => {
    return apiClient.post<SessionStartResponse>('/realtime/session/start', request);
  },

  /**
   * End an active translation session
   * Stops monitoring and returns final usage statistics
   */
  endSession: async (sessionId: string): Promise<SessionEndResponse> => {
    const request: SessionEndRequest = { sessionId };
    return apiClient.post<SessionEndResponse>('/realtime/session/end', request);
  },

  /**
   * Get the current status of a session
   * Includes current usage statistics
   */
  getSessionStatus: async (sessionId: string): Promise<SessionStatusResponse> => {
    return apiClient.get<SessionStatusResponse>(`/realtime/session/${sessionId}`);
  },

  /**
   * Health check endpoint
   */
  healthCheck: async (): Promise<{ status: string; timestamp: string }> => {
    return apiClient.get('/health');
  },
};
