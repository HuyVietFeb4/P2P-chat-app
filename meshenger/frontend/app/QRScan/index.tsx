/*import { View } from "react-native"
import Footer from "./component/Footer"
import Header from "./component/Header"
import QRCamera from "./component/QRCamera"

export default function QRScan() {
    return (
        <View style={{flex: 1}}>
            <Header />
            <QRCamera />
            <Footer />
        </View>
    )
}
*/
import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, NativeModules } from 'react-native';
import { useRouter } from 'expo-router';

// Access the native module
const { MeshengerApplicationModule } = NativeModules;

export default function AddPeerTest() {
    const router = useRouter();
    const [peerId, setPeerId] = useState('');
    const [displayName, setDisplayName] = useState('');
    const [avatarUrl, setAvatarUrl] = useState('');

    const handleConfirm = async () => {
        if (!peerId || !displayName) {
            Alert.alert("Error", "ID and Display Name are required");
            return;
        }

        try {
            // Call the Kotlin ReactMethod
            const result = await MeshengerApplicationModule.addPeer(
                peerId,
                displayName,
                avatarUrl || null
            );

            Alert.alert("Success", result, [
                { text: "Go to Chats", onPress: () => router.replace('/ChatBox') }
            ]);
        } catch (error: any) {
            console.error(error);
            Alert.alert("Database Error", error.message);
        }
    };

    return (
        <View style={styles.container}>
            <Text style={styles.title}>Manual Add Peer (Test)</Text>

            <TextInput
                style={styles.input}
                placeholder="Peer ID (e.g. 123-abc)"
                value={peerId}
                onChangeText={setPeerId}
                autoCapitalize="none"
            />

            <TextInput
                style={styles.input}
                placeholder="Display Name"
                value={displayName}
                onChangeText={setDisplayName}
            />

            <TextInput
                style={styles.input}
                placeholder="Avatar URL (Optional)"
                value={avatarUrl}
                onChangeText={setAvatarUrl}
                autoCapitalize="none"
            />

            <TouchableOpacity style={styles.button} onPress={handleConfirm}>
                <Text style={styles.buttonText}>Confirm & Save to DB</Text>
            </TouchableOpacity>

            <TouchableOpacity
                style={[styles.button, { backgroundColor: '#666', marginTop: 10 }]}
                onPress={() => router.replace('/ChatBox')} // Changed from router.back()
            >
                <Text style={styles.buttonText}>Cancel</Text>
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, padding: 20, justifyContent: 'center', backgroundColor: '#fff' },
    title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
    input: {
        borderWidth: 1,
        borderColor: '#ccc',
        padding: 12,
        borderRadius: 8,
        marginBottom: 15,
        fontSize: 16
    },
    button: {
        backgroundColor: '#007AFF',
        padding: 15,
        borderRadius: 8,
        alignItems: 'center'
    },
    buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});