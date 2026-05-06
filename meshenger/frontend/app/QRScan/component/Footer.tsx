import { Images, QrCode, ScanLine } from "lucide-react-native";
import { StyleSheet, Text, TouchableOpacity, View, useWindowDimensions } from "react-native";
import { useTranslation } from "react-i18next";

type Props = {
    setActiveTab: (tab: "my-qr" | "scan-qr") => void,
    openAlbum: () => Promise<void>;
}

export default function Footer({ setActiveTab, openAlbum }: Props) {
    const { width } = useWindowDimensions();
    const { t } = useTranslation();

    return (
        <View style={[styles.footerContainer, { width: width * 0.8 }]}>
            <TouchableOpacity style={styles.actionContainer} onPress={() => setActiveTab("my-qr")}>
                <QrCode color="#fff" />
                <Text style={styles.actionText}>{t('my-qr')}</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.scanQR} onPress={() => setActiveTab("scan-qr")}>
                <ScanLine color="#fff" />
            </TouchableOpacity>

            <TouchableOpacity style={styles.actionContainer} onPress={openAlbum}>
                <Images color="#fff" />
                <Text style={styles.actionText}>{t('album')}</Text>
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