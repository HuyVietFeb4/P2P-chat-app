
import { useRouter } from "expo-router";
import { useEffect } from 'react';
import { StyleSheet, Text, View } from "react-native";
import { Camera, useCameraDevice, useCameraPermission, useCodeScanner } from "react-native-vision-camera";
export default function QRCamera() {
    const router = useRouter();
    const device = useCameraDevice('back');
    const { hasPermission, requestPermission } = useCameraPermission();
    const codeScanner = useCodeScanner({
        codeTypes: ['qr'],
        onCodeScanned: (code) => {
            console.log(code)
        }
    });

    useEffect(() => {
        if (!hasPermission) {
            requestPermission();
        }
    }, [hasPermission]);

    if (device == null) {
    return (
        <View>
            <Text>Loading camera...</Text>
        </View>
    );
}

    return (
        <Camera
            style={StyleSheet.absoluteFill}
            device={device}
            isActive={true}
            codeScanner={codeScanner}
        />
    )
}

