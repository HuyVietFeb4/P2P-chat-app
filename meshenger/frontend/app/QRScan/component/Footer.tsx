import { Images, QrCode } from "lucide-react-native";
import { StyleSheet, Text, View, useWindowDimensions } from "react-native";

export default function Footer() {
    const { width } = useWindowDimensions();
    return (
        <View style={[styles.footerContainer, { width: width * 0.7 }]}>
            <View style={styles.actionContainer}>
                <QrCode color="#fff" />
                <Text style={styles.actionText}>My QR</Text>
            </View>

            <View style={styles.actionContainer}>
                <Images color="#fff" />
                <Text style={styles.actionText}>Album</Text>
            </View>
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
        paddingHorizontal: 40,
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
        gap: 5
    }
});