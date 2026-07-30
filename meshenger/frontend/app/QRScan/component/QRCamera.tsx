
// import { useRouter } from "expo-router";
// import { useEffect } from 'react';
// import { StyleSheet, Text, View } from "react-native";
// import { Camera, useCameraDevice, useCameraPermission, useCodeScanner } from "react-native-vision-camera";

// export default function QRCamera() {
//     const router = useRouter();
//     const device = useCameraDevice('back');
//     const { hasPermission, requestPermission } = useCameraPermission();
//     const codeScanner = useCodeScanner({
//         codeTypes: ['qr'],
//         onCodeScanned: (code) => {
//             console.log(code)
//         }
//     });

//     useEffect(() => {
//         if (!hasPermission) {
//             requestPermission();
//         }
//     }, [hasPermission]);

//     if (device == null) {
//     return (
//         <View>
//             <Text>Loading camera...</Text>
//         </View>
//     );
// }

//     return (
//         <Camera
//             style={StyleSheet.absoluteFill}
//             device={device}
//             isActive={true}
//             codeScanner={codeScanner}
//         />
//     )
// }

import Message from "@/app/common components/Message";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useRouter } from "expo-router";
import { Camera } from "lucide-react-native";
import { useState, useRef } from "react";
import { StyleSheet, Text, TouchableOpacity, View, NativeModules } from "react-native";
import { useTranslation } from "react-i18next";

export default function QRCamera() {
    const [permission, requestPermission] = useCameraPermissions();
    const [scanned, setScanned] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const router = useRouter();
    const { t } = useTranslation();
    const { MeshengerApplicationModule } = NativeModules;
    const scanLock = useRef(false);

    if (!permission) {
        return (
            <View style={{flex: 0.5, justifyContent: 'center', alignItems: 'center', gap: 10}}>
                <Camera size={40} color='#B0B3B8' />
                <Text style={{color: '#B0B3B8', fontWeight: 'bold'}}>{t('camera-is-loading-please-wait')}</Text>
            </View>
        );
    }

    const requestCameraPermission = async () => {
        if (!permission.granted) {
            await requestPermission();
        }
    }

    const handleScan = async ({ data }: { data: string }) => {
       if (scanLock.current) return;
       scanLock.current = true;
       try {
            setScanned(true);
            const jsonData = JSON.parse(data);
            const peerId =
                (typeof jsonData.peerId === 'string' && jsonData.peerId.startsWith('mp:') && jsonData.peerId) ||
                (jsonData.mpAddress != null ? `mp:${String(jsonData.mpAddress)}` : '');
            const username = typeof jsonData.username === 'string' ? jsonData.username : '';
            const noisePublicKeyBase64 =
                (typeof jsonData.noisePublicKeyBase64 === 'string' && jsonData.noisePublicKeyBase64) ||
                (typeof jsonData.xkey === 'string' && jsonData.xkey) ||
                '';
            const avatarId = jsonData.avatarId;

            // console.log("Peer ID: ", peerId);
            const selfPublicKey = await MeshengerApplicationModule.getIdentityForQr();

            if (!peerId || !username || !noisePublicKeyBase64) {
                throw new Error(t('invalid-qr'));
            }

            if (selfPublicKey.noisePublicKeyBase64 === noisePublicKeyBase64) {
                throw new Error(t('cannot-scan-self'))
            }

            router.push({
                pathname: '/ConnectUser',
                params: { peerId, username, noisePublicKeyBase64, avatarId },
            });
            return;
       } catch (err) {
            if (err instanceof Error) {
                setError(err.message);
            }
            setError(t('invalid-qr'));
       } finally {
            setTimeout(() => {
                scanLock.current = false;
                setError(null);
            }, 1500);
       }
    };

    return (
        <View style={StyleSheet.absoluteFill}>
            {
                !permission?.granted ? (
                    <View style={{flex: 1, justifyContent: 'center', alignItems: 'center', gap: 10}}>
                        <Camera size={40} color='#B0B3B8' />
                        <Text style={{color: '#B0B3B8', fontWeight: 'bold'}}>{t('we-need-your-permission-to-access-the-camera')}</Text>
                        <TouchableOpacity style={{backgroundColor: '#0082FC', paddingHorizontal: 20, paddingVertical: 10, borderRadius: 12}} onPress={requestCameraPermission}>
                            <Text style={{color: 'white', fontWeight: 'bold'}}>{t('grant-permission')}</Text>
                        </TouchableOpacity>
                    </View>
                ) : (
                    <CameraView style={[StyleSheet.absoluteFill, {position: 'absolute'}]} 
                        autofocus='on'
                        facing='back'
                        barcodeScannerSettings={{
                            barcodeTypes: ['qr']
                        }}
                        onBarcodeScanned={handleScan}
                    />
                )
            }

            <Message visible={!!error} message={error} title={t('scan-qr-error')} />
        </View>
    );  
}

