import { ScanQrCode } from "lucide-react-native";
import { StyleSheet, Text, View, useWindowDimensions, NativeModules } from "react-native";
import QRCode from "react-native-qrcode-svg";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

const { MeshengerApplicationModule } = NativeModules;

export default function MyQR() {
    const { width, height } = useWindowDimensions();
    const [id, setId] = useState<string>("");
    const [mpAddress, setMPAddress] = useState<string>("");
    const [username, setUsername] = useState<string>("");
    const [xkey, setXKey] = useState<string>("");
    const [edkey, setEDKey] = useState<string>("");
    const [avatarId, setAvatarId] = useState<string>("");
    const { t } = useTranslation();

    useEffect(() => {
        const loadData = async () => {
            try {
                const mod = MeshengerApplicationModule as any;
                const identity = await mod.getIdentityForQr();
                setUsername(identity.username ?? '');
                setId(identity.peerId ?? '');
                setMPAddress(identity.mpAddress ?? '');
                setXKey(identity.noisePublicKeyBase64 ?? '');
                setAvatarId(identity.avatarId);
                try {
                    const keys = await mod.loadRemotePeerRawKey?.('local-device', 'ALL');
                    setEDKey(keys?.['ED25519_RAW'] ?? '');
                } catch {
                    setEDKey('');
                }
            } catch (e) {
                console.error('Failed to load QR data:', e);
            }
        };

        loadData();
    }, []);

    // The data carried by the QR code
    const userQRData = {
        peerId: id,
        mpAddress,
        noisePublicKeyBase64: xkey,
        username,
        edkey,
        avatarId
    };

    return (
        <View style={[{ height: height * 0.5 }, styles.container]}>
            <View style={styles.card}>
                <View style={styles.avatarImage}>
                    <ScanQrCode size={40} color="#fff" />
                </View>

                <Text style={styles.username}>{username}</Text>

                <View style={styles.qrWrapper}>
                    <View style={styles.qrBox}>
                        {id ? (
                            <QRCode
                                value={JSON.stringify(userQRData)}
                                size={width * 0.4}
                            />
                        ) : (
                            <View style={{ width: width * 0.4, height: width * 0.4, justifyContent: 'center', alignItems: 'center' }}>
                                <Text>{t('loading')}</Text>
                            </View>
                        )}
                    </View>
                    <View style={[styles.corner, styles.tl]} />
                    <View style={[styles.corner, styles.tr]} />
                    <View style={[styles.corner, styles.bl]} />
                    <View style={[styles.corner, styles.br]} />
                </View>

                <Text style={styles.hint}>{t('allow-other-to')}</Text>
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
    avatarImage: {
        backgroundColor: "#0082FC",
        borderRadius: 999,
        padding: 15,
        position: "absolute",
        top: -30
    }
});
