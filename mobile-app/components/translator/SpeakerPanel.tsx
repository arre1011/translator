import React from 'react';
import { View, StyleSheet, ViewStyle } from 'react-native';
import { useThemeColor } from '@/hooks/use-theme-color';
import { LanguageSelector } from './LanguageSelector';
import { MicButton } from './MicButton';
import { TranscriptDisplay } from './TranscriptDisplay';
import { TranslationDisplay } from './TranslationDisplay';
import { PlayAudioButton } from './PlayAudioButton';

interface SpeakerPanelProps {
  position: 'top' | 'bottom';
  language: string;
  onLanguageChange: (lang: string) => void;
  excludeLanguage?: string;
  onMicPress: () => void;
  isRecording: boolean;
  isConnected: boolean;
  transcript: string;
  translation: string;
  onPlayAudio?: () => void;
  hasAudio?: boolean;
  isPlayingAudio?: boolean;
  style?: ViewStyle;
}

export function SpeakerPanel({
  position,
  language,
  onLanguageChange,
  excludeLanguage,
  onMicPress,
  isRecording,
  isConnected,
  transcript,
  translation,
  onPlayAudio,
  hasAudio = false,
  isPlayingAudio = false,
  style,
}: SpeakerPanelProps) {
  const backgroundColor = useThemeColor({}, 'background');

  const panelStyles = [
    styles.panel,
    position === 'bottom' && styles.panelBottom,
    { backgroundColor },
    style,
  ];

  return (
    <View style={panelStyles}>
      <View style={styles.header}>
        <LanguageSelector
          selectedLanguage={language}
          onSelectLanguage={onLanguageChange}
          excludeLanguage={excludeLanguage}
          label={position === 'top' ? 'Speaker A' : 'Speaker B'}
        />
      </View>

      <View style={styles.content}>
        <TranscriptDisplay
          text={transcript}
          isLoading={isRecording}
          maxHeight={80}
        />
        <TranslationDisplay
          text={translation}
          maxHeight={80}
        />
      </View>

      <View style={styles.controls}>
        <MicButton
          isActive={isRecording}
          isRecording={isRecording}
          onPress={onMicPress}
          disabled={!isConnected && !isRecording}
          size="large"
        />
        {hasAudio && onPlayAudio && (
          <PlayAudioButton
            onPress={onPlayAudio}
            isPlaying={isPlayingAudio}
            style={styles.playButton}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  panel: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
    margin: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  panelBottom: {
    // Bottom panel is rotated 180deg for face-to-face usage
    transform: [{ rotate: '180deg' }],
  },
  header: {
    marginBottom: 8,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
  },
  controls: {
    alignItems: 'center',
    paddingTop: 16,
  },
  playButton: {
    marginTop: 12,
  },
});
