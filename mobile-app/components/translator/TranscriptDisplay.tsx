import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useThemeColor } from '@/hooks/use-theme-color';

interface TranscriptDisplayProps {
  text: string;
  label?: string;
  isLoading?: boolean;
  maxHeight?: number;
}

export function TranscriptDisplay({
  text,
  label = 'Transcript',
  isLoading = false,
  maxHeight = 100,
}: TranscriptDisplayProps) {
  const textColor = useThemeColor({}, 'text');
  const backgroundColor = useThemeColor({}, 'background');

  return (
    <View style={styles.container}>
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
      <ScrollView
        style={[styles.textContainer, { backgroundColor, maxHeight }]}
        showsVerticalScrollIndicator={false}
      >
        {isLoading ? (
          <Text style={[styles.loadingText, { color: textColor }]}>
            Listening...
          </Text>
        ) : text ? (
          <Text style={[styles.text, { color: textColor }]}>{text}</Text>
        ) : (
          <Text style={[styles.placeholder, { color: textColor }]}>
            Speak to see transcript
          </Text>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 4,
    textTransform: 'uppercase',
    opacity: 0.7,
  },
  textContainer: {
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  text: {
    fontSize: 15,
    lineHeight: 22,
  },
  placeholder: {
    fontSize: 15,
    fontStyle: 'italic',
    opacity: 0.5,
  },
  loadingText: {
    fontSize: 15,
    fontStyle: 'italic',
    opacity: 0.7,
  },
});
