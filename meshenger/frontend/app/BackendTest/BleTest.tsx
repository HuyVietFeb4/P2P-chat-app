import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, NativeModules, SafeAreaView } from 'react-native';
import  requestBlePermissions from '../utils/permissions';

// Define the bridge interface
interface BleModuleInterface {
  onDemandScan(period: number): void;
  onStartAdvertise(): void;
}

const { BleModule } = NativeModules as { BleModule: BleModuleInterface };

type BleMode = 'IDLE' | 'SCANNING' | 'ADVERTISING';

const BleTest: React.FC = () => {
  const [mode, setMode] = useState<BleMode>('IDLE');

  const handleScan = async () => {
    const hasPermission = await requestBlePermissions();
    if (hasPermission && BleModule) {
      setMode('SCANNING');
      BleModule.onDemandScan(10000);
      // Auto-reset UI after 10s to match your native SCAN_PERIOD
      setTimeout(() => setMode('IDLE'), 10000);
    }
  };

  const handleAdvertise = async () => {
    const hasPermission = await requestBlePermissions();
    if (hasPermission && BleModule) {
      setMode('ADVERTISING');
      BleModule.onBackgroundAdvertise();
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.header}>BLE P2P Test Bench</Text>
      
      <View style={styles.statusBox}>
        <Text style={styles.statusLabel}>Current Status:</Text>
        <Text style={[styles.statusValue, { color: mode === 'IDLE' ? '#666' : '#2196F3' }]}>
          {mode}
        </Text>
      </View>

      <View style={styles.buttonContainer}>
        <TouchableOpacity 
          style={[styles.button, mode === 'SCANNING' && styles.activeBtn]} 
          onPress={handleScan}
          disabled={mode !== 'IDLE'}
        >
          <Text style={styles.btnText}>Start 10s Scan</Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={[styles.button, mode === 'ADVERTISING' && styles.activeBtn]} 
          onPress={handleAdvertise}
          disabled={mode !== 'IDLE'}
        >
          <Text style={styles.btnText}>Start Advertise</Text>
        </TouchableOpacity>

        {mode !== 'IDLE' && (
           <TouchableOpacity style={styles.resetBtn} onPress={() => setMode('IDLE')}>
             <Text style={styles.resetText}>Stop/Reset</Text>
           </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5', alignItems: 'center', justifyContent: 'center' },
  header: { fontSize: 24, fontWeight: 'bold', marginBottom: 30 },
  statusBox: { padding: 20, backgroundColor: '#fff', borderRadius: 10, width: '80%', alignItems: 'center', elevation: 3 },
  statusLabel: { color: '#888', fontSize: 14 },
  statusValue: { fontSize: 22, fontWeight: '800', marginTop: 5 },
  buttonContainer: { marginTop: 40, width: '80%' },
  button: { backgroundColor: '#2196F3', padding: 15, borderRadius: 8, marginBottom: 15, alignItems: 'center' },
  activeBtn: { backgroundColor: '#4CAF50' },
  btnText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  resetBtn: { marginTop: 10, alignItems: 'center' },
  resetText: { color: '#FF5252', fontWeight: 'bold' }
});

export default BleTest;