import { ScanQrCode } from "lucide-react-native";
import { StyleSheet, Text, View, useWindowDimensions } from "react-native";
import QRCode from "react-native-qrcode-svg";

type QRType = {
    key: String,
    username: String
}

const userQRData: QRType = {
    key: "abcxyz",
    username: "John Doe"
}

export default function MyQR() {
    const { width, height } = useWindowDimensions();
    return (
        <View style={[{height: height * 0.5}, styles.container]}>

            <View style={styles.card}>
                <View style={styles.avatarImage}>
                    <ScanQrCode size={40} color="#fff" />
                </View>

                <Text style={styles.username}>{userQRData.username as string}</Text>

                <View style={styles.qrWrapper}>
                    <View style={styles.qrBox}>
                        <QRCode
                            value={JSON.stringify(userQRData)}
                            size={width * 0.4}
                        />
                    </View>
                    <View style={[styles.corner, styles.tl]} />
                    <View style={[styles.corner, styles.tr]} />
                    <View style={[styles.corner, styles.bl]} />
                    <View style={[styles.corner, styles.br]} />
                </View>

                <Text style={styles.hint}>Allow others to scan the code to connect.</Text>
            </View>
        </View>
    );
}

const TEAL = "#00C2A8";

const styles = StyleSheet.create({
    container: {
        alignItems: "center",
        justifyContent: "center",
        gap: 20,
        backgroundColor: "#F4F6FA",
    },
    card: {
        width: "90%",
        alignSelf: "center",
        backgroundColor: "#fff",
        borderRadius: 24,
        padding: 28,
        alignItems: "center",
        shadowColor: "#000",
        shadowOpacity: 0.07,
        shadowRadius: 16,
        shadowOffset: { width: 0, height: 4 },
        elevation: 4,
    },
    iconRow: {
        flexDirection: "row",
        alignItems: "center",
        gap: 8,
        marginBottom: 8,
    },
    cardTitle: {
        fontSize: 16,
        fontWeight: "700",
        color: "#1A1D2E",
    },
    username: {
        fontSize: 22,
        fontWeight: "800",
        color: "#1A1D2E",
        marginBottom: 24,
        marginTop: 20
    },
    qrWrapper: {
        position: "relative",
        padding: 16,
        marginBottom: 20,
    },
    qrBox: {
        padding: 12,
        backgroundColor: "#fff",
        borderRadius: 12,
    },
    corner: {
        position: "absolute",
        width: 24,
        height: 24,
        borderColor: TEAL,
    },
    tl: { top: 0, left: 0, borderTopWidth: 3, borderLeftWidth: 3, borderTopLeftRadius: 8 },
    tr: { top: 0, right: 0, borderTopWidth: 3, borderRightWidth: 3, borderTopRightRadius: 8 },
    bl: { bottom: 0, left: 0, borderBottomWidth: 3, borderLeftWidth: 3, borderBottomLeftRadius: 8 },
    br: { bottom: 0, right: 0, borderBottomWidth: 3, borderRightWidth: 3, borderBottomRightRadius: 8 },
    hint: {
        fontSize: 13,
        color: "#7B8299",
        textAlign: "center",
    },
    actions: {
        flexDirection: "row",
        width: "100%",
        gap: 12,
    },
    btnOutline: {
        flex: 1,
        paddingVertical: 15,
        borderRadius: 14,
        borderWidth: 1.5,
        borderColor: "#E5E9F2",
        alignItems: "center",
        backgroundColor: "#fff",
    },
    btnOutlineText: {
        fontSize: 14,
        fontWeight: "600",
        color: "#1A1D2E",
    },
    btnPrimary: {
        flex: 1,
        paddingVertical: 15,
        borderRadius: 14,
        backgroundColor: TEAL,
        alignItems: "center",
    },
    btnPrimaryText: {
        fontSize: 14,
        fontWeight: "600",
        color: "#fff",
    },

    avatarImage: {
        backgroundColor: "#0082FC",
        borderRadius: 999,
        padding: 15,
        position: "absolute",
        top: -30
    }
});