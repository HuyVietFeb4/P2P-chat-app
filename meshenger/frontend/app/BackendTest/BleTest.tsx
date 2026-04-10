import React, { useState } from 'react';
import { 
  View, 
  Text, 
  TouchableOpacity, 
  StyleSheet, 
  NativeModules, 
  SafeAreaView, 
  ScrollView,
  Alert 
} from 'react-native';
import requestBlePermissions from '../utils/permissions';

// 1. Define the interface for our Kotlin Bridge
interface BleModuleInterface {
  onDemandScan(period: number): void;
  onBackgroundAdvertise(): void;
  connectToDiscoveredPeers(): void;
  testSendMsgClientToServer(msg: string): void;
  testSendMsgServerToClient(msg: string): void;
  startServer(): void; // Trigger MeshManager.startServer()
}

const { BleModule } = NativeModules as { BleModule: BleModuleInterface };

type BleMode = 'IDLE' | 'SCANNING' | 'ADVERTISING' | 'MESH_CONNECTING' | 'SERVER_ACTIVE';

const BleTest: React.FC = () => {
  const [mode, setMode] = useState<BleMode>('IDLE');

  // 0. Initialize the GATT Server (Open the door)
  const handleStartServer = async () => {
    const hasPermission = await requestBlePermissions();
    if (hasPermission && BleModule) {
      BleModule.startServer();
      setMode('SERVER_ACTIVE');
      Alert.alert("Success", "GATT Server is now active. You can now advertise.");
    }
  };

  // 1. Discovery (Scanning)
  const handleScan = async () => {
    const hasPermission = await requestBlePermissions();
    if (hasPermission && BleModule) {
      setMode('SCANNING');
      BleModule.onDemandScan(10000);
      
      setTimeout(() => {
        setMode('IDLE');
      }, 10000);
    }
  };

  // 2. Become Discoverable (Advertising)
  const handleAdvertise = async () => {
    if (BleModule) {
      setMode('ADVERTISING');
      BleModule.onBackgroundAdvertise();
    }
  };

  // 3. Initiate Connections to found peers
  const handleConnectMesh = () => {
    if (BleModule) {
      setMode('MESH_CONNECTING');
      BleModule.connectToDiscoveredPeers();
      
      // Auto-reset UI state after a few seconds
      setTimeout(() => setMode('IDLE'), 3000);
    }
  };

  // 4. Data Transfer Testing
  const handleTestMessage = (direction: 'C2S' | 'S2C') => {
    if (!BleModule) return;

    const timestamp = new Date().toLocaleTimeString();
    if (direction === 'C2S') {
      const msg = `Client Message at ${timestamp}`;
      BleModule.testSendMsgClientToServer(msg);
    } else {
      const msg = `Server Notification at ${timestamp}`;
      BleModule.testSendMsgServerToClient(msg);
    }
  };

  const resetMode = () => setMode('IDLE');

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={styles.header}>Meshenger P2P Bench</Text>
        
        <View style={styles.statusBox}>
          <Text style={styles.statusLabel}>Current State</Text>
          <Text style={[
            styles.statusValue, 
            { color: mode === 'IDLE' ? '#7f8c8d' : '#2980b9' }
          ]}>
            {mode.replace('_', ' ')}
          </Text>
        </View>

        <View style={styles.buttonContainer}>
          
          {/* --- SERVER SETUP --- */}
          <Text style={styles.sectionLabel}>1. Server Management</Text>
          <TouchableOpacity 
            style={[styles.button, { backgroundColor: '#FF9800' }]} 
            onPress={handleStartServer}
          >
            <Text style={styles.btnText}>Start GATT Server</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.button, styles.secondaryBtn, mode === 'ADVERTISING' && styles.activeBtn]} 
            onPress={handleAdvertise}
          >
            <Text style={styles.btnText}>Broadcast Presence (Advertise)</Text>
          </TouchableOpacity>

          {/* --- CLIENT SETUP --- */}
          <Text style={[styles.sectionLabel, { marginTop: 20 }]}>2. Client Management</Text>
          
          <TouchableOpacity 
            style={[styles.button, mode === 'SCANNING' && styles.activeBtn]} 
            onPress={handleScan}
            disabled={mode === 'SCANNING'}
          >
            <Text style={styles.btnText}>Scan for Peers (10s)</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.button, styles.meshBtn]} 
            onPress={handleConnectMesh}
          >
            <Text style={styles.btnText}>Connect to Found Peers</Text>
          </TouchableOpacity>

          {/* --- DATA TESTING --- */}
          <View style={styles.testSection}>
            <Text style={styles.sectionLabel}>3. Data Transfer Tests</Text>
            
            <TouchableOpacity 
              style={[styles.button, styles.msgBtn]} 
              onPress={() => handleTestMessage('C2S')}
            >
              <Text style={styles.btnText}>Send: Client ➔ Server</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={[styles.button, styles.msgBtn, { backgroundColor: '#E91E63' }]} 
              onPress={() => handleTestMessage('S2C')}
            >
              <Text style={styles.btnText}>Send: Server ➔ Client</Text>
            </TouchableOpacity>
          </View>

          {mode !== 'IDLE' && (
            <TouchableOpacity style={styles.resetBtn} onPress={resetMode}>
              <Text style={styles.resetText}>Reset UI State</Text>
            </TouchableOpacity>
          )}
        </View>

        <View style={styles.infoBox}>
          <Text style={styles.infoTitle}>Operational Note:</Text>
          <Text style={styles.infoText}>
            • Start Server first, then Advertise.{"\n"}
            • On the second device, Scan then Connect.{"\n"}
            • Check Logcat for data arrival verification.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F0F2F5' },
  scrollContent: { alignItems: 'center', paddingVertical: 40 },
  header: { fontSize: 26, fontWeight: 'bold', color: '#1C1E21', marginBottom: 30 },
  statusBox: { 
    padding: 20, 
    backgroundColor: '#FFFFFF', 
    borderRadius: 12, 
    width: '85%', 
    alignItems: 'center', 
    elevation: 3 
  },
  statusLabel: { color: '#65676B', fontSize: 11, textTransform: 'uppercase', letterSpacing: 1 },
  statusValue: { fontSize: 22, fontWeight: '800', marginTop: 4 },
  buttonContainer: { marginTop: 30, width: '85%' },
  sectionLabel: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#8a8d91',
    marginBottom: 10,
    textTransform: 'uppercase',
  },
  button: { 
    backgroundColor: '#1877F2', 
    paddingVertical: 14, 
    borderRadius: 10, 
    marginBottom: 10, 
    alignItems: 'center'
  },
  secondaryBtn: { backgroundColor: '#42b72a' },
  meshBtn: { backgroundColor: '#673AB7' },
  msgBtn: { backgroundColor: '#34495e' },
  activeBtn: { backgroundColor: '#bdc3c7' },
  btnText: { color: '#FFFFFF', fontWeight: '700', fontSize: 15 },
  resetBtn: { marginTop: 10, alignItems: 'center' },
  resetText: { color: '#FA3E3E', fontWeight: '600' },
  testSection: {
    marginTop: 20,
    paddingTop: 20,
    borderTopWidth: 1,
    borderTopColor: '#dcdde1'
  },
  infoBox: {
    marginTop: 30,
    padding: 15,
    width: '85%',
    backgroundColor: '#E7F3FF',
    borderRadius: 8
  },
  infoTitle: { fontWeight: 'bold', color: '#0C2D48', marginBottom: 4 },
  infoText: { color: '#1877F2', fontSize: 13, lineHeight: 18 }
});

export default BleTest;