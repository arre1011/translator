import { Tabs } from 'expo-router';
import React from 'react';

import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function TabLayout() {
  const colorScheme = useColorScheme();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: Colors[colorScheme ?? 'light'].tint,
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarStyle: {
          display: 'none', // Hide tab bar for full-screen translator
        },
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: 'Translate',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="mic.fill" color={color} />,
        }}
      />
      <Tabs.Screen
        name="explore"
        options={{
          href: null, // Hide this tab for MVP
          title: 'Settings',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="gearshape.fill" color={color} />,
        }}
      />
    </Tabs>
  );
}
