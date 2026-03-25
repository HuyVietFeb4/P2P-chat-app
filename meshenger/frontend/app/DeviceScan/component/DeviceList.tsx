import { BadgePlus, Smartphone } from "lucide-react-native";
import { ScrollView, StyleSheet, Text, View, useWindowDimensions } from "react-native";

import DeviceInfo from "./DeviceInfo";
import TypingDots from "./TypingDots";

export default function DeviceList() {
    const { width, height } = useWindowDimensions();
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
                <ScrollView style={styles.scrollContainer}>
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                    <DeviceInfo />
                </ScrollView>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 20,
        backgroundColor: "pink"
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