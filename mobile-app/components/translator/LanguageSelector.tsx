import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
  StyleSheet,
  SafeAreaView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useThemeColor } from '@/hooks/use-theme-color';
import { SUPPORTED_LANGUAGES, getLanguageByCode } from '@/constants/languages';
import { Language } from '@/services/api/types';

interface LanguageSelectorProps {
  selectedLanguage: string;
  onSelectLanguage: (code: string) => void;
  excludeLanguage?: string;
  label?: string;
}

export function LanguageSelector({
  selectedLanguage,
  onSelectLanguage,
  excludeLanguage,
  label,
}: LanguageSelectorProps) {
  const [modalVisible, setModalVisible] = useState(false);
  const backgroundColor = useThemeColor({}, 'background');
  const textColor = useThemeColor({}, 'text');

  const selectedLang = getLanguageByCode(selectedLanguage);
  const availableLanguages = SUPPORTED_LANGUAGES.filter(
    (lang) => lang.code !== excludeLanguage
  );

  const handleSelect = (code: string) => {
    onSelectLanguage(code);
    setModalVisible(false);
  };

  const renderLanguageItem = ({ item }: { item: Language }) => (
    <TouchableOpacity
      style={[
        styles.languageItem,
        item.code === selectedLanguage && styles.languageItemSelected,
      ]}
      onPress={() => handleSelect(item.code)}
    >
      <Text style={styles.languageFlag}>{item.flag}</Text>
      <Text style={[styles.languageName, { color: textColor }]}>{item.name}</Text>
      {item.code === selectedLanguage && (
        <Ionicons name="checkmark" size={24} color="#007AFF" />
      )}
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {label && <Text style={[styles.label, { color: textColor }]}>{label}</Text>}
      <TouchableOpacity
        style={[styles.selector, { backgroundColor: backgroundColor }]}
        onPress={() => setModalVisible(true)}
      >
        <Text style={styles.selectorFlag}>{selectedLang?.flag || '🌐'}</Text>
        <Text style={[styles.selectorText, { color: textColor }]}>
          {selectedLang?.name || 'Select'}
        </Text>
        <Ionicons name="chevron-down" size={20} color={textColor} />
      </TouchableOpacity>

      <Modal
        visible={modalVisible}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <SafeAreaView style={[styles.modalContent, { backgroundColor }]}>
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: textColor }]}>
                Select Language
              </Text>
              <TouchableOpacity onPress={() => setModalVisible(false)}>
                <Ionicons name="close" size={28} color={textColor} />
              </TouchableOpacity>
            </View>
            <FlatList
              data={availableLanguages}
              keyExtractor={(item) => item.code}
              renderItem={renderLanguageItem}
              showsVerticalScrollIndicator={false}
            />
          </SafeAreaView>
        </View>
      </Modal>
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
  selector: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  selectorFlag: {
    fontSize: 24,
    marginRight: 10,
  },
  selectorText: {
    flex: 1,
    fontSize: 16,
    fontWeight: '500',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: '70%',
    paddingBottom: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  languageItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E0E0E0',
  },
  languageItemSelected: {
    backgroundColor: 'rgba(0, 122, 255, 0.1)',
  },
  languageFlag: {
    fontSize: 28,
    marginRight: 14,
  },
  languageName: {
    flex: 1,
    fontSize: 16,
  },
});
