import { BadgePlus, Smartphone } from "lucide-react-native";
import { useEffect, useState } from "react";
import { Animated, StyleSheet, Text, View, useWindowDimensions } from "react-native";
import { deviceList } from "./data";
import DeviceInfo from "./DeviceInfo";
import TypingDots from "./TypingDots";

type deviceType = {
    id: number,
    deviceName: string,
    statusNumber: number
};

export default function DeviceList() {
    const [deviceInfo, setDeviceInfo] = useState<deviceType[] | null>([]);
    const { width, height } = useWindowDimensions();
    const fadeAnim = useState(new Animated.Value(0))[0];
    const translateY = useState(new Animated.Value(20))[0];

    useEffect(() => {
        setTimeout(() => {
            setDeviceInfo(deviceList);
            Animated.parallel([
                Animated.timing(fadeAnim, {
                    toValue: 1,
                    duration: 500,
                    useNativeDriver: true,
                }),
                Animated.timing(translateY, {
                    toValue: 0,
                    duration: 500,
                    useNativeDriver: true,
                })
            ]).start();
        }, 5000)
    }, []);

    return (
        <View style={[styles.container, {width: width, height: height * 0.5}]}>
            <View style={styles.row}>
                <Smartphone size={20} color='#4DA6FF' />
                <Text style={styles.text}>Scanning</Text>
                <TypingDots />
            </View>

            <View style={styles.scannedDevices}>
                <BadgePlus size={20} color='rgba(0, 0, 0, 0.65)'/>
                <Text>Scanned Devices</Text>
            </View>

            <View style={{ flexShrink: 1 }}>
                <Animated.ScrollView style={[styles.scrollContainer, deviceInfo?.length === 0 && { borderWidth: 0 }, {opacity: fadeAnim, transform: [{ translateY }]}]}>
                    {
                        deviceInfo?.map((device) => (
                            <DeviceInfo key={device.id} avatarName={device.deviceName} status={device.statusNumber} />
                        ))
                    }
                </Animated.ScrollView>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 20
    },

    row: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: 'center',
        gap: 8,
    },

    text: {
        fontSize: 14,
        fontWeight: "800",
        fontStyle: 'italic',
        color: '#4DA6FF'
    },

    scrollContainer: {
        backgroundColor: '#F0F9FF',
        borderRadius: 12,
        marginTop: 20,
        borderWidth: 0.5,
        borderColor: '#0EA5E9',
    },

    scannedDevices: {
        marginTop: 20,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10
    }
});