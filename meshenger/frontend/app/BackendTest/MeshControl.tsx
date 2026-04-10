import React, { useState } from 'react';
import { 
  View, 
  Text, 
  TouchableOpacity, 
  StyleSheet, 
  NativeModules, 
  SafeAreaView, 
  Alert 
} from 'react-native';
import requestBlePermissions from '../utils/permissions';

// Define the bridge to include your new Service methods
interface BleModuleInterface {
  startMeshService(): void;
  stopMeshService(): void;
}

const { BleModule } = NativeModules as { BleModule: BleModuleInterface };

const MeshControl: React.FC = () => {
  const [isServiceRunning, setIsServiceRunning] = useState(false);

  const handleToggleService = async () => {
    const hasPermission = await requestBlePermissions();
    
    if (!hasPermission) {
      Alert.alert("Permission Denied", "Bluetooth and Location permissions are required.");
      return;
    }

    if (!BleModule) {
      Alert.alert("Error", "BleModule not found.");
      return;
    }

    if (!isServiceRunning) {
      // 1. Start the Self-Healing Background Service
      BleModule.startMeshService();
      setIsServiceRunning(true);
      console.log("Mesh Service Started");
    } else {
      // 2. Stop the Service and release all hardware
      BleModule.stopMeshService();
      setIsServiceRunning(false);
      console.log("Mesh Service Stopped");
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.header}>Meshenger Mesh</Text>
        
        <View style={styles.statusIndicator}>
          <View style={[
            styles.dot, 
            { backgroundColor: isServiceRunning ? '#42b72a' : '#FA3E3E' }
          ]} />
          <Text style={styles.statusText}>
            System: {isServiceRunning ? 'Active & Self-Healing' : 'Offline'}
          </Text>
        </View>

        <TouchableOpacity 
          style={[
            styles.button, 
            { backgroundColor: isServiceRunning ? '#FA3E3E' : '#1877F2' }
          ]} 
          onPress={handleToggleService}
        >
          <Text style={styles.btnText}>
            {isServiceRunning ? 'Stop Mesh Service' : 'Start Mesh Service'}
          </Text>
        </TouchableOpacity>

        <Text style={styles.description}>
          {isServiceRunning 
            ? "The background service is currently managing connections, scanning for peers, and maintaining the mesh health automatically."
            : "Starting the service will initiate background scanning and advertising to build your local P2P network."}
        </Text>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { 
    flex: 1, 
    backgroundColor: '#F0F2F5', 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  card: {
    backgroundColor: '#FFFFFF',
    padding: 30,
    borderRadius: 20,
    width: '90%',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
  },
  header: { 
    fontSize: 24, 
    fontWeight: 'bold', 
    color: '#1C1E21', 
    marginBottom: 20 
  },
  statusIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 30,
    backgroundColor: '#F8F9FA',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 10,
  },
  statusText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#4B4C4E',
  },
  button: { 
    width: '100%',
    paddingVertical: 16, 
    borderRadius: 12, 
    alignItems: 'center',
    marginBottom: 20,
  },
  btnText: { 
    color: '#FFFFFF', 
    fontWeight: '700', 
    fontSize: 16 
  },
  description: {
    textAlign: 'center',
    color: '#65676B',
    fontSize: 13,
    lineHeight: 20,
  }
});

export default MeshControl;