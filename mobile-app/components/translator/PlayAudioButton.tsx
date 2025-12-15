import React from 'react';
import { TouchableOpacity, Text, StyleSheet, ViewStyle } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useThemeColor } from '@/hooks/use-theme-color';

interface PlayAudioButtonProps {
  onPress: () => void;
  isPlaying?: boolean;
  disabled?: boolean;
  style?: ViewStyle;
}

export function PlayAudioButton({
  onPress,
  isPlaying = false,
  disabled = false,
  style,
}: PlayAudioButtonProps) {
  const textColor = useThemeColor({}, 'text');

  const handlePress = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    onPress();
  };

  return (
    <TouchableOpacity
      style={[
        styles.button,
        disabled && styles.buttonDisabled,
        style,
      ]}
      onPress={handlePress}
      disabled={disabled}
      activeOpacity={0.7}
    >
      <Ionicons
        name={isPlaying ? 'pause-circle' : 'play-circle'}
        size={28}
        color={disabled ? '#888' : '#007AFF'}
      />
      <Text style={[styles.buttonText, { color: disabled ? '#888' : textColor }]}>
        {isPlaying ? 'Pause' : 'Play Audio'}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: 'rgba(0, 122, 255, 0.1)',
    alignSelf: 'flex-start',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 8,
  },
});
