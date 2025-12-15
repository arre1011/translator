import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useThemeColor } from '@/hooks/use-theme-color';

interface TranslationDisplayProps {
  text: string;
  label?: string;
  isLoading?: boolean;
  maxHeight?: number;
}

export function TranslationDisplay({
  text,
  label = 'Translation',
  isLoading = false,
  maxHeight = 100,
}: TranslationDisplayProps) {
  const textColor = useThemeColor({}, 'text');

  return (
    <View style={styles.container}>
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
      <ScrollView
        style={[styles.textContainer, { maxHeight }]}
        showsVerticalScrollIndicator={false}
      >
        {isLoading ? (
          <Text style={[styles.loadingText, { color: textColor }]}>
            Translating...
          </Text>
        ) : text ? (
          <Text style={[styles.text, { color: textColor }]}>{text}</Text>
        ) : (
          <Text style={[styles.placeholder, { color: textColor }]}>
            Translation will appear here
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
    backgroundColor: 'rgba(0, 122, 255, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(0, 122, 255, 0.2)',
  },
  text: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '500',
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
