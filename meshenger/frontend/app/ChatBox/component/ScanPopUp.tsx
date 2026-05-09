import { useRouter } from "expo-router";
import { QrCode, Tablet, UserRoundPlus } from "lucide-react-native";
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";
import { useTranslation } from "react-i18next";
import { useTheme } from "@/app/context/ThemeContext";

type Props = {
    setOnClose: () => void;
}

export default function ScanPopUp({ setOnClose }: Props) {
    const router = useRouter();
    const { width } = useWindowDimensions();
    const { t } = useTranslation();
    const { colors } = useTheme();

    const handleRouteDeviceScan = () => {
        setOnClose();
        router.push("/DeviceScan");
    }

    const handleRouteQRScan = () => {
        setOnClose();
        router.push("/QRScan");
    }

    return (
        <View style={[styles.addUserContainer, {width: width * 0.4, backgroundColor: colors.scanPopUp}]}>
            <View style={[styles.addUser, {borderColor: colors.scanPopUpBorder}]}>
                <UserRoundPlus
                    size={20}
                    color={colors.scanPopUpMainText}
                />
                <Text style={[styles.addUserText, {color: colors.scanPopUpMainText}]}>{t("add-users")}</Text>
            </View>

            <View style={styles.addUserActionContainer}>
                <Pressable style={styles.addUserAction} onPress={handleRouteDeviceScan}>
                    <Tablet
                        size={20}
                        color={colors.scanPopUpAccent}
                    />
                    <Text style={[
                        styles.addUserText,
                        { color: colors.scanPopUpSubText }
                    ]}>
                        {t("devices")}
                    </Text>
                </Pressable>

                <Pressable style={styles.addUserAction} onPress={handleRouteQRScan}>
                    <QrCode
                        size={20}
                        color={colors.scanPopUpAccent}
                    />
                    <Text style={[
                        styles.addUserText,
                        { color: colors.scanPopUpSubText }]}
                    >
                        {t("qr")}
                    </Text>
                </Pressable>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    addUserContainer: {
        backgroundColor: 'white',
        paddingVertical: 10,
        borderRadius: 20,
        position: 'absolute',
        right: 15,
        top: 80,
        borderWidth: 0.5,
        borderColor: 'rgba(95, 46, 234, 0.2)',
        elevation: 6,
        zIndex: 10
    },

    addUser: {
        flexDirection: 'row',
        gap: 5,
        justifyContent: 'center',
        alignItems: 'center',
        paddingBottom: 3,
        borderBottomWidth: 0.25,
    },

    addUserText: {
        fontSize: 12,
        fontWeight: 'bold',
    },
    
    addUserActionContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingTop: 15,
        paddingHorizontal: 20,
        gap: 20,
        alignItems: 'center'
    },

    addUserAction: {
        gap: 3,
        alignItems: 'center'
    }
});