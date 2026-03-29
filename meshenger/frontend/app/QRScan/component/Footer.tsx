import { Images, QrCode, ScanLine } from "lucide-react-native";
import { StyleSheet, Text, TouchableOpacity, View, useWindowDimensions } from "react-native";

type Props = {
    activeTab: "my-qr" | "album" | "scan-qr",
    setActiveTab: (tab: "my-qr" | "album" | "scan-qr") => void
}

export default function Footer({ activeTab, setActiveTab }: Props) {
    const { width } = useWindowDimensions();
    return (
        <View style={[styles.footerContainer, { width: width * 0.8 }]}>
            <TouchableOpacity style={styles.actionContainer} onPress={() => setActiveTab("my-qr")}>
                <QrCode color="#fff" />
                <Text style={styles.actionText}>My QR</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.scanQR} onPress={() => setActiveTab("scan-qr")}>
                <ScanLine color="#fff" />
            </TouchableOpacity>

            <TouchableOpacity style={styles.actionContainer} onPress={() => setActiveTab("album")}>
                <Images color="#fff" />
                <Text style={styles.actionText}>Album</Text>
            </TouchableOpacity>
        </View>
    )
}

const styles = StyleSheet.create({
    footerContainer: {
        position: 'absolute',
        bottom: 100,
        alignSelf: 'center',
        backgroundColor: 'rgba(0,0,0,0.3)',
        flexDirection: 'row',
        paddingHorizontal: 20,
        paddingVertical: 20,
        justifyContent: 'space-between',
        borderRadius: 20
    },

    actionText: {
        color: 'white',
        fontWeight: 300
    },

    actionContainer: {
        alignItems: 'center',
        gap: 5,
        paddingHorizontal: 10
    },

    scanQR: {
        position: 'absolute',
        backgroundColor: '#0082FC',
        padding: 20,
        borderRadius: 50,
        left: '50%',
        transform: [{ translateX: -10 }, { translateY: -30 }],
    },
});