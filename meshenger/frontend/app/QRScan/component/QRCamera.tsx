
import { useRouter } from "expo-router";
import { useEffect } from 'react';
import { StyleSheet, Text, View, useWindowDimensions } from "react-native";
import { Camera, useCameraDevice, useCameraPermission } from "react-native-vision-camera";
export default function QRCamera() {
    const { width, height } = useWindowDimensions();
    const router = useRouter();
    const device = useCameraDevice('back');
    const { hasPermission, requestPermission } = useCameraPermission();

    if (device == null) {
    return (
        <View>
            <Text>Loading camera...</Text>
        </View>
    );
}

    useEffect(() => {
        if (!hasPermission) {
            requestPermission();
        }
    }, [hasPermission]);

    return (
        <View style={styles.cameraContainer}>
            <Camera
                style={[{width: width * 0.8, height: width * 0.8}]}
                device={device}
                isActive={true}
            />
        </View>
    )
}

const styles = StyleSheet.create({
    cameraContainer: {
        flex: 0.5,
        justifyContent: 'center',
        alignItems: 'center'  
    }
});