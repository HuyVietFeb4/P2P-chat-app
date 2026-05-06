import { Bluetooth } from "lucide-react-native";
import { Modal, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useCallback } from "react";
import * as IntentLauncher from "expo-intent-launcher";
import { BackHandler } from "react-native";
import { useTranslation } from "react-i18next";

type Props = {
    visible: boolean;
    onDismiss: () => void;
};

export default function BluetoothPopup({ visible, onDismiss }: Props) {
    const { t } = useTranslation();
    const handleEnable = useCallback(async () => {
        // Kéo thanh tác vụ xuống và mở Quick Settings để bật Bluetooth
        await IntentLauncher.startActivityAsync(
            "android.settings.BLUETOOTH_SETTINGS"
        );
    }, []);

    const handleExit = useCallback(() => {
        onDismiss();
        BackHandler.exitApp();
    }, [onDismiss]);

    return (
        <Modal
            visible={visible}
            transparent
            animationType="fade"
            statusBarTranslucent
            onRequestClose={handleExit}
        >
            <View style={styles.overlay}>
                <View style={styles.popup}>
                    <View style={styles.iconWrap}>
                        <Bluetooth size={28} color="#5F2EEA" />
                    </View>

                    <Text style={styles.title}>{t("enable-bluetooth")}</Text>
                    <Text style={styles.desc}>
                        {t("meshenger-needs-bluetooth-to-discover-peers")}
                    </Text>

                    <TouchableOpacity style={styles.btnEnable} onPress={handleEnable} activeOpacity={0.85}>
                        <Text style={styles.btnEnableText}>{t("enable-bluetooth")}</Text>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.btnCancel} onPress={handleExit} activeOpacity={0.6}>
                        <Text style={styles.btnCancelText}>{t("exit-app")}</Text>
                    </TouchableOpacity>
                </View>
            </View>
        </Modal>
    );
}

const styles = StyleSheet.create({
    overlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.45)',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 28,
    },

    popup: {
        backgroundColor: '#fff',
        borderRadius: 20,
        paddingHorizontal: 20,
        paddingTop: 28,
        paddingBottom: 18,
        width: '100%',
        alignItems: 'center',
    },

    iconWrap: {
        width: 64,
        height: 64,
        borderRadius: 32,
        backgroundColor: '#EEEDFE',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
    },

    title: {
        fontSize: 17,
        fontWeight: '600',
        color: '#1a1a1a',
        marginBottom: 8,
        textAlign: 'center',
    },

    desc: {
        fontSize: 13,
        color: '#5F5E5A',
        textAlign: 'center',
        lineHeight: 20,
        marginBottom: 22,
    },

    btnEnable: {
        backgroundColor: '#5F2EEA',
        borderRadius: 12,
        paddingVertical: 13,
        width: '100%',
        alignItems: 'center',
        marginBottom: 8,
    },

    btnEnableText: {
        color: '#fff',
        fontSize: 15,
        fontWeight: '600',
    },

    btnCancel: {
        paddingVertical: 10,
        width: '100%',
        alignItems: 'center',
    },

    btnCancelText: {
        fontSize: 13,
        fontWeight: '500',
        color: '#888780',
    },
});