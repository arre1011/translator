import {
  RTCPeerConnection,
  RTCSessionDescription,
  RTCIceCandidate,
  mediaDevices,
  MediaStream,
  MediaStreamTrack,
} from 'react-native-webrtc';

export type ConnectionState = 'new' | 'connecting' | 'connected' | 'disconnected' | 'failed' | 'closed';

export interface WebRTCCallbacks {
  onConnectionStateChange?: (state: ConnectionState) => void;
  onRemoteTrack?: (track: MediaStreamTrack, streams: readonly MediaStream[]) => void;
  onError?: (error: Error) => void;
}

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
];

export class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private pendingCandidates: RTCIceCandidate[] = [];
  private callbacks: WebRTCCallbacks = {};
  private isInitialized = false;

  /**
   * Initialize WebRTC with microphone access
   */
  async initialize(callbacks: WebRTCCallbacks = {}): Promise<void> {
    if (this.isInitialized) {
      console.warn('[WebRTC] Already initialized');
      return;
    }

    this.callbacks = callbacks;

    try {
      // Create peer connection
      const configuration = {
        iceServers: ICE_SERVERS,
        iceCandidatePoolSize: 10,
      };

      this.peerConnection = new RTCPeerConnection(configuration);
      this.setupPeerConnectionHandlers();

      // Get microphone access
      this.localStream = await mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
        video: false,
      });

      // Add local audio track to peer connection
      this.localStream.getTracks().forEach((track) => {
        if (this.peerConnection && this.localStream) {
          this.peerConnection.addTrack(track, this.localStream);
        }
      });

      this.isInitialized = true;
      console.log('[WebRTC] Initialized successfully');
    } catch (error) {
      console.error('[WebRTC] Initialization failed:', error);
      this.callbacks.onError?.(error as Error);
      throw error;
    }
  }

  /**
   * Create SDP offer for the backend
   */
  async createOffer(): Promise<string> {
    if (!this.peerConnection) {
      throw new Error('WebRTC not initialized');
    }

    try {
      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: false,
      });

      await this.peerConnection.setLocalDescription(offer);

      // Wait for ICE gathering to complete (or timeout)
      await this.waitForIceGathering();

      const localDescription = this.peerConnection.localDescription;
      if (!localDescription?.sdp) {
        throw new Error('Failed to create SDP offer');
      }

      console.log('[WebRTC] Offer created');
      return localDescription.sdp;
    } catch (error) {
      console.error('[WebRTC] Failed to create offer:', error);
      this.callbacks.onError?.(error as Error);
      throw error;
    }
  }

  /**
   * Set remote SDP answer from OpenAI (via backend)
   */
  async setRemoteAnswer(sdpAnswer: string): Promise<void> {
    if (!this.peerConnection) {
      throw new Error('WebRTC not initialized');
    }

    try {
      const answer = new RTCSessionDescription({
        type: 'answer',
        sdp: sdpAnswer,
      });

      await this.peerConnection.setRemoteDescription(answer);
      console.log('[WebRTC] Remote answer set');

      // Process any pending ICE candidates
      await this.processPendingCandidates();
    } catch (error) {
      console.error('[WebRTC] Failed to set remote answer:', error);
      this.callbacks.onError?.(error as Error);
      throw error;
    }
  }

  /**
   * Mute/unmute local microphone
   */
  setMicrophoneMuted(muted: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((track) => {
        track.enabled = !muted;
      });
      console.log(`[WebRTC] Microphone ${muted ? 'muted' : 'unmuted'}`);
    }
  }

  /**
   * Check if microphone is muted
   */
  isMicrophoneMuted(): boolean {
    if (!this.localStream) return true;
    const audioTracks = this.localStream.getAudioTracks();
    return audioTracks.length === 0 || !audioTracks[0].enabled;
  }

  /**
   * Get the remote audio stream for playback
   */
  getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  /**
   * Get current connection state
   */
  getConnectionState(): ConnectionState {
    if (!this.peerConnection) return 'new';
    return this.peerConnection.connectionState as ConnectionState;
  }

  /**
   * Clean up all resources
   */
  cleanup(): void {
    console.log('[WebRTC] Cleaning up...');

    // Stop local stream tracks
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        track.stop();
      });
      this.localStream = null;
    }

    // Stop remote stream tracks
    if (this.remoteStream) {
      this.remoteStream.getTracks().forEach((track) => {
        track.stop();
      });
      this.remoteStream = null;
    }

    // Close peer connection
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }

    this.pendingCandidates = [];
    this.callbacks = {};
    this.isInitialized = false;

    console.log('[WebRTC] Cleanup complete');
  }

  private setupPeerConnectionHandlers(): void {
    if (!this.peerConnection) return;

    // Connection state changes
    this.peerConnection.onconnectionstatechange = () => {
      const state = this.peerConnection?.connectionState as ConnectionState;
      console.log('[WebRTC] Connection state:', state);
      this.callbacks.onConnectionStateChange?.(state);

      if (state === 'failed') {
        this.callbacks.onError?.(new Error('WebRTC connection failed'));
      }
    };

    // ICE connection state
    this.peerConnection.oniceconnectionstatechange = () => {
      console.log('[WebRTC] ICE state:', this.peerConnection?.iceConnectionState);
    };

    // ICE candidate
    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('[WebRTC] ICE candidate gathered');
        // For OpenAI Realtime, we include all candidates in the initial offer
        // so we don't need to send them separately
      }
    };

    // Remote track received
    this.peerConnection.ontrack = (event) => {
      console.log('[WebRTC] Remote track received:', event.track.kind);

      if (event.streams && event.streams[0]) {
        this.remoteStream = event.streams[0];
      } else {
        // Create a new stream if none provided
        if (!this.remoteStream) {
          this.remoteStream = new MediaStream();
        }
        this.remoteStream.addTrack(event.track);
      }

      this.callbacks.onRemoteTrack?.(event.track, event.streams);
    };

    // Negotiation needed
    this.peerConnection.onnegotiationneeded = () => {
      console.log('[WebRTC] Negotiation needed');
    };
  }

  private async waitForIceGathering(timeout = 5000): Promise<void> {
    if (!this.peerConnection) return;

    if (this.peerConnection.iceGatheringState === 'complete') {
      return;
    }

    return new Promise((resolve) => {
      const checkState = () => {
        if (this.peerConnection?.iceGatheringState === 'complete') {
          resolve();
        }
      };

      this.peerConnection.onicegatheringstatechange = checkState;

      // Timeout fallback
      setTimeout(() => {
        console.log('[WebRTC] ICE gathering timeout, proceeding with current candidates');
        resolve();
      }, timeout);
    });
  }

  private async processPendingCandidates(): Promise<void> {
    if (!this.peerConnection) return;

    for (const candidate of this.pendingCandidates) {
      try {
        await this.peerConnection.addIceCandidate(candidate);
      } catch (error) {
        console.warn('[WebRTC] Failed to add pending candidate:', error);
      }
    }
    this.pendingCandidates = [];
  }
}

// Singleton instance for convenience
export const webrtcService = new WebRTCService();
