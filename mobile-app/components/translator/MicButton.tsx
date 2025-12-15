import React from 'react';
import { TouchableOpacity, StyleSheet, ViewStyle, Animated } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useThemeColor } from '@/hooks/use-theme-color';

interface MicButtonProps {
  isActive: boolean;
  isRecording: boolean;
  onPress: () => void;
  disabled?: boolean;
  size?: 'small' | 'medium' | 'large';
  style?: ViewStyle;
}

export function MicButton({
  isActive,
  isRecording,
  onPress,
  disabled = false,
  size = 'large',
  style,
}: MicButtonProps) {
  const backgroundColor = useThemeColor({}, 'background');
  const textColor = useThemeColor({}, 'text');
  const pulseAnim = React.useRef(new Animated.Value(1)).current;

  React.useEffect(() => {
    if (isRecording) {
      // Pulse animation when recording
      const pulse = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, {
            toValue: 1.1,
            duration: 500,
            useNativeDriver: true,
          }),
          Animated.timing(pulseAnim, {
            toValue: 1,
            duration: 500,
            useNativeDriver: true,
          }),
        ])
      );
      pulse.start();
      return () => pulse.stop();
    } else {
      pulseAnim.setValue(1);
    }
  }, [isRecording, pulseAnim]);

  const handlePress = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    onPress();
  };

  const sizeStyles = {
    small: { button: 50, icon: 24 },
    medium: { button: 70, icon: 32 },
    large: { button: 90, icon: 44 },
  };

  const { button: buttonSize, icon: iconSize } = sizeStyles[size];

  const buttonStyles = [
    styles.button,
    {
      width: buttonSize,
      height: buttonSize,
      borderRadius: buttonSize / 2,
      backgroundColor: isActive ? '#FF3B30' : disabled ? '#888' : '#007AFF',
    },
    style,
  ];

  return (
    <Animated.View style={{ transform: [{ scale: pulseAnim }] }}>
      <TouchableOpacity
        style={buttonStyles}
        onPress={handlePress}
        disabled={disabled}
        activeOpacity={0.7}
      >
        <Ionicons
          name={isRecording ? 'mic' : 'mic-outline'}
          size={iconSize}
          color="#FFFFFF"
        />
      </TouchableOpacity>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 6,
    elevation: 8,
  },
});
