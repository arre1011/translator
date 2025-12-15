import React, { useState, useCallback, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { Ionicons } from '@expo/vector-icons';
import { useThemeColor } from '@/hooks/use-theme-color';
import { useTranslationSession, Speaker } from '@/hooks/use-translation-session';
import { SpeakerPanel } from '@/components/translator/SpeakerPanel';
import { DEFAULT_SOURCE_LANG, DEFAULT_TARGET_LANG } from '@/constants/languages';

// Generate a simple user ID for MVP (in production, use auth)
const generateUserId = () => `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

export default function TranslatorScreen() {
  const backgroundColor = useThemeColor({}, 'background');
  const textColor = useThemeColor({}, 'text');

  const [languageA, setLanguageA] = useState(DEFAULT_SOURCE_LANG);
  const [languageB, setLanguageB] = useState(DEFAULT_TARGET_LANG);
  const [userId] = useState(generateUserId);

  const {
    isConnected,
    isConnecting,
    activeSpeaker,
    transcripts,
    translations,
    error,
    startSession,
    endSession,
    toggleMicrophone,
    clearError,
  } = useTranslationSession();

  // Handle errors
  useEffect(() => {
    if (error) {
      Alert.alert('Error', error, [{ text: 'OK', onPress: clearError }]);
    }
  }, [error, clearError]);

  const handleMicPress = useCallback(async (speaker: Speaker) => {
    if (!isConnected && !isConnecting) {
      // Start a new session
      const sourceLang = speaker === 'A' ? languageA : languageB;
      const targetLang = speaker === 'A' ? languageB : languageA;
      await startSession(userId, sourceLang, targetLang);
    }

    // Toggle microphone for this speaker
    toggleMicrophone(speaker);
  }, [isConnected, isConnecting, languageA, languageB, userId, startSession, toggleMicrophone]);

  const handleEndSession = useCallback(() => {
    Alert.alert(
      'End Session',
      'Are you sure you want to end the translation session?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'End', style: 'destructive', onPress: endSession },
      ]
    );
  }, [endSession]);

  const handleSwapLanguages = useCallback(() => {
    if (isConnected) {
      Alert.alert('Cannot Swap', 'Please end the current session before changing languages.');
      return;
    }
    setLanguageA(languageB);
    setLanguageB(languageA);
  }, [isConnected, languageA, languageB]);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor }]}>
      <StatusBar style="auto" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.title, { color: textColor }]}>Face-to-Face Translator</Text>
        <View style={styles.headerControls}>
          {isConnected && (
            <TouchableOpacity onPress={handleEndSession} style={styles.endButton}>
              <Ionicons name="stop-circle" size={24} color="#FF3B30" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* Connection Status */}
      <View style={styles.statusBar}>
        {isConnecting ? (
          <View style={styles.statusContent}>
            <ActivityIndicator size="small" color="#007AFF" />
            <Text style={[styles.statusText, { color: textColor }]}>Connecting...</Text>
          </View>
        ) : isConnected ? (
          <View style={styles.statusContent}>
            <View style={styles.statusDot} />
            <Text style={[styles.statusText, { color: textColor }]}>Connected</Text>
          </View>
        ) : (
          <Text style={[styles.statusHint, { color: textColor }]}>
            Tap a microphone to start translating
          </Text>
        )}
      </View>

      {/* Speaker A - Top Half */}
      <SpeakerPanel
        position="top"
        language={languageA}
        onLanguageChange={setLanguageA}
        excludeLanguage={languageB}
        onMicPress={() => handleMicPress('A')}
        isRecording={activeSpeaker === 'A'}
        isConnected={isConnected || isConnecting}
        transcript={transcripts.A}
        translation={translations.A}
      />

      {/* Divider with Swap Button */}
      <View style={styles.divider}>
        <View style={styles.dividerLine} />
        <TouchableOpacity
          style={[styles.swapButton, isConnected && styles.swapButtonDisabled]}
          onPress={handleSwapLanguages}
          disabled={isConnected}
        >
          <Ionicons
            name="swap-vertical"
            size={24}
            color={isConnected ? '#888' : '#007AFF'}
          />
        </TouchableOpacity>
        <View style={styles.dividerLine} />
      </View>

      {/* Speaker B - Bottom Half (rotated for face-to-face) */}
      <SpeakerPanel
        position="bottom"
        language={languageB}
        onLanguageChange={setLanguageB}
        excludeLanguage={languageA}
        onMicPress={() => handleMicPress('B')}
        isRecording={activeSpeaker === 'B'}
        isConnected={isConnected || isConnecting}
        transcript={transcripts.B}
        translation={translations.B}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  headerControls: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  endButton: {
    padding: 8,
  },
  statusBar: {
    alignItems: 'center',
    paddingVertical: 8,
  },
  statusContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#34C759',
    marginRight: 8,
  },
  statusText: {
    fontSize: 14,
    fontWeight: '500',
  },
  statusHint: {
    fontSize: 14,
    opacity: 0.6,
    fontStyle: 'italic',
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginVertical: 4,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: '#E0E0E0',
  },
  swapButton: {
    padding: 12,
    marginHorizontal: 12,
    borderRadius: 20,
    backgroundColor: 'rgba(0, 122, 255, 0.1)',
  },
  swapButtonDisabled: {
    opacity: 0.5,
  },
});
