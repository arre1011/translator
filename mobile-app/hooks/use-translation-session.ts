import { useState, useRef, useCallback, useEffect } from 'react';
import { Alert } from 'react-native';
import { realtimeApi } from '@/services/api/realtimeApi';
import { SessionStartResponse, UsageRecord } from '@/services/api/types';
import { WebRTCService, ConnectionState } from '@/services/webrtc/webrtcService';

export type Speaker = 'A' | 'B';

export interface TranslationState {
  sessionId: string | null;
  callId: string | null;
  isConnected: boolean;
  isConnecting: boolean;
  connectionState: ConnectionState;
  activeSpeaker: Speaker | null;
  transcripts: Record<Speaker, string>;
  translations: Record<Speaker, string>;
  usage: UsageRecord | null;
  error: string | null;
}

export interface UseTranslationSessionResult extends TranslationState {
  startSession: (userId: string, sourceLang: string, targetLang: string) => Promise<void>;
  endSession: () => Promise<void>;
  setActiveSpeaker: (speaker: Speaker | null) => void;
  toggleMicrophone: (speaker: Speaker) => void;
  clearError: () => void;
}

const initialState: TranslationState = {
  sessionId: null,
  callId: null,
  isConnected: false,
  isConnecting: false,
  connectionState: 'new',
  activeSpeaker: null,
  transcripts: { A: '', B: '' },
  translations: { A: '', B: '' },
  usage: null,
  error: null,
};

export function useTranslationSession(): UseTranslationSessionResult {
  const [state, setState] = useState<TranslationState>(initialState);
  const webrtcRef = useRef<WebRTCService | null>(null);
  const sessionInfoRef = useRef<{
    userId: string;
    sourceLang: string;
    targetLang: string;
  } | null>(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      webrtcRef.current?.cleanup();
    };
  }, []);

  const updateState = useCallback((updates: Partial<TranslationState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  }, []);

  const handleConnectionStateChange = useCallback((connectionState: ConnectionState) => {
    const isConnected = connectionState === 'connected';
    updateState({ connectionState, isConnected });

    if (connectionState === 'failed' || connectionState === 'disconnected') {
      updateState({ error: 'Connection lost. Please try again.' });
    }
  }, [updateState]);

  const handleRemoteTrack = useCallback((track: MediaStreamTrack) => {
    console.log('[Translation] Remote track received:', track.kind);
    // Audio will be played automatically through the WebRTC connection
    // The remote audio track is automatically connected to the device speaker
  }, []);

  const handleError = useCallback((error: Error) => {
    console.error('[Translation] Error:', error);
    updateState({ error: error.message });
  }, [updateState]);

  const startSession = useCallback(async (
    userId: string,
    sourceLang: string,
    targetLang: string
  ) => {
    if (state.isConnecting || state.isConnected) {
      console.warn('[Translation] Session already active or connecting');
      return;
    }

    updateState({ isConnecting: true, error: null });
    sessionInfoRef.current = { userId, sourceLang, targetLang };

    try {
      // Initialize WebRTC
      webrtcRef.current = new WebRTCService();
      await webrtcRef.current.initialize({
        onConnectionStateChange: handleConnectionStateChange,
        onRemoteTrack: handleRemoteTrack,
        onError: handleError,
      });

      // Create SDP offer
      const sdpOffer = await webrtcRef.current.createOffer();

      // Send to backend to create OpenAI session
      const response: SessionStartResponse = await realtimeApi.startSession({
        userId,
        sourceLang,
        targetLang,
        sdpOffer,
      });

      // Set remote answer
      await webrtcRef.current.setRemoteAnswer(response.sdpAnswer);

      updateState({
        sessionId: response.sessionId,
        callId: response.callId,
        isConnecting: false,
        isConnected: true,
      });

      console.log('[Translation] Session started:', response.sessionId);
    } catch (error) {
      console.error('[Translation] Failed to start session:', error);
      webrtcRef.current?.cleanup();
      webrtcRef.current = null;

      const message = error instanceof Error ? error.message : 'Failed to start session';
      updateState({
        isConnecting: false,
        error: message,
      });

      Alert.alert('Connection Error', message);
    }
  }, [state.isConnecting, state.isConnected, updateState, handleConnectionStateChange, handleRemoteTrack, handleError]);

  const endSession = useCallback(async () => {
    const { sessionId } = state;

    if (!sessionId) {
      console.warn('[Translation] No active session to end');
      return;
    }

    try {
      // End session on backend
      const response = await realtimeApi.endSession(sessionId);

      updateState({
        usage: response.finalUsage,
      });

      console.log('[Translation] Session ended:', sessionId, 'Usage:', response.finalUsage);
    } catch (error) {
      console.error('[Translation] Failed to end session on backend:', error);
    } finally {
      // Cleanup WebRTC regardless of backend call success
      webrtcRef.current?.cleanup();
      webrtcRef.current = null;

      // Reset state
      setState({
        ...initialState,
        usage: state.usage, // Preserve final usage
      });
    }
  }, [state, updateState]);

  const setActiveSpeaker = useCallback((speaker: Speaker | null) => {
    updateState({ activeSpeaker: speaker });

    // Mute/unmute microphone based on active speaker
    if (webrtcRef.current) {
      webrtcRef.current.setMicrophoneMuted(speaker === null);
    }
  }, [updateState]);

  const toggleMicrophone = useCallback((speaker: Speaker) => {
    const newActiveSpeaker = state.activeSpeaker === speaker ? null : speaker;
    setActiveSpeaker(newActiveSpeaker);
  }, [state.activeSpeaker, setActiveSpeaker]);

  const clearError = useCallback(() => {
    updateState({ error: null });
  }, [updateState]);

  return {
    ...state,
    startSession,
    endSession,
    setActiveSpeaker,
    toggleMicrophone,
    clearError,
  };
}
