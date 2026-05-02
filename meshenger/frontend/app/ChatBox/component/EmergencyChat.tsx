import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../../context/ThemeContext';

export default function EmergencyChat() {
  const { colors, isDarkMode } = useTheme();

  return (
    <View style={[styles.container, { backgroundColor: isDarkMode ? colors.background : '#fff5f5' }]}>
      <Text style={[styles.text, { color: isDarkMode ? '#ff8a80' : '#c53030' }]}>Emergency Chats</Text>
      <Text style={[styles.subText, { color: colors.subText }]}>Coming Soon...</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  subText: {
    fontSize: 16,
    marginTop: 10,
  },
});
