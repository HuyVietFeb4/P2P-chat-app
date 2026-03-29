import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

export default function EmergencyChat() {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>Emergency Chats</Text>
      <Text style={styles.subText}>Coming Soon...</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff5f5',
  },
  text: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#c53030',
  },
  subText: {
    fontSize: 16,
    color: '#888',
    marginTop: 10,
  },
});
