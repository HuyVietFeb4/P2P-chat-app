import * as ImagePicker from "expo-image-picker"
import { useEffect, useState } from "react"
import { NativeModules, StyleSheet, View } from "react-native"
import Footer from "./component/Footer"
import Header from "./component/Header"
import MyQR from "./component/MyQR"
import QRCamera from "./component/QRCamera"
import { useBluetooth } from "../hook/useBluetooth";
import BluetoothPopup from "../common components/BluetoothPopUp";


export default function QRScan() {
    const [activeTab, setActiveTab] = useState<"my-qr" | "album" | "scan-qr">("scan-qr");
    const [image, setImage] = useState<string | null>(null);
    const [permission, requestPermission] = ImagePicker.useMediaLibraryPermissions();
    const [username, setUsername] = useState<string>("Loading...");
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();

    const pickImage = async () => {
        if (!permission?.granted) {
            await requestPermission();
            return;
        }

        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: false,
            aspect: [4, 3],
            quality: 1
        });

        console.log(result);

        if (!result.canceled) {
            setImage(result.assets[0].uri);
        }
    }

    return (
        <View style={{flex: 1}}>
            {
                activeTab === "scan-qr" && <Header title="Devices Scanning" instruction="Scan QR to add a new device" />
            }

            {
                activeTab === "my-qr" && <Header title="My QR Code" instruction="Show your QR code to others" />
            }
            
            {
                activeTab === "scan-qr" && <QRCamera />
            }

            {
                activeTab === "my-qr" && <MyQR />
            }

            <Footer setActiveTab={setActiveTab} openAlbum={pickImage}/>

             {showPopup && (
                            <BluetoothPopup
                                visible={true}
                                onDismiss={dismissPopup}
                            />
                        )}
        </View>
    )
}

const styles = StyleSheet.create({
    albumList: {
        position: 'absolute',
    }
});
// import React, { useState } from 'react';
// import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, NativeModules } from 'react-native';
// import { useRouter } from 'expo-router';

// // Access the native module
// const { MeshengerApplicationModule } = NativeModules;

// export default function AddPeerTest() {
//     const router = useRouter();
//     const [peerId, setPeerId] = useState('');
//     const [displayName, setDisplayName] = useState('');
//     const [avatarUrl, setAvatarUrl] = useState('');

//     const handleConfirm = async () => {
//         if (!peerId || !displayName) {
//             Alert.alert("Error", "ID and Display Name are required");
//             return;
//         }

//         try {
//             // Call the Kotlin ReactMethod
//             const result = await MeshengerApplicationModule.addPeer(
//                 peerId,
//                 displayName,
//                 avatarUrl || null
//             );

//             Alert.alert("Success", result, [
//                 { text: "Go to Chats", onPress: () => router.replace('/ChatBox') }
//             ]);
//         } catch (error: any) {
//             console.error(error);
//             Alert.alert("Database Error", error.message);
//         }
//     };

//     return (
//         <View style={styles.container}>
//             <Text style={styles.title}>Manual Add Peer (Test)</Text>

//             <TextInput
//                 style={styles.input}
//                 placeholder="Peer ID (e.g. 123-abc)"
//                 value={peerId}
//                 onChangeText={setPeerId}
//                 autoCapitalize="none"
//             />

//             <TextInput
//                 style={styles.input}
//                 placeholder="Display Name"
//                 value={displayName}
//                 onChangeText={setDisplayName}
//             />

//             <TextInput
//                 style={styles.input}
//                 placeholder="Avatar URL (Optional)"
//                 value={avatarUrl}
//                 onChangeText={setAvatarUrl}
//                 autoCapitalize="none"
//             />

//             <TouchableOpacity style={styles.button} onPress={handleConfirm}>
//                 <Text style={styles.buttonText}>Confirm & Save to DB</Text>
//             </TouchableOpacity>

//             <TouchableOpacity
//                 style={[styles.button, { backgroundColor: '#666', marginTop: 10 }]}
//                 onPress={() => router.replace('/ChatBox')} // Changed from router.back()
//             >
//                 <Text style={styles.buttonText}>Cancel</Text>
//             </TouchableOpacity>
//         </View>
//     );
// }

// const styles = StyleSheet.create({
//     container: { flex: 1, padding: 20, justifyContent: 'center', backgroundColor: '#fff' },
//     title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
//     input: {
//         borderWidth: 1,
//         borderColor: '#ccc',
//         padding: 12,
//         borderRadius: 8,
//         marginBottom: 15,
//         fontSize: 16
//     },
//     button: {
//         backgroundColor: '#007AFF',
//         padding: 15,
//         borderRadius: 8,
//         alignItems: 'center'
//     },
//     buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
// });