import { Image } from "expo-image";
import { MessageCircle } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";
import { useTranslation } from "react-i18next";

type deviceInfo = {
    avatarName: String,
    status: Number
}

export default function DeviceInfo({ avatarName, status }: deviceInfo) {
    const { t } = useTranslation();
    return (
        <View style={styles.deviceInfoContainer}>
            <View style={styles.info}>
                <Image 
                    source={require('@/assets/images/avatar.png')}
                    style={styles.avatar}
                />
                <Text style={styles.text}>{avatarName}</Text>
            </View>

            <View style={styles.chatStatus}>
                <Text style={{fontSize: 10, fontStyle: 'italic', fontWeight: 600}}>
                    {t('status')}: {status === 1 ? <Text style={{color: '#22C55E', fontSize: 10}}>{t('strong')}</Text> : <Text style={{color: '#EF4444', fontSize: 10}}>{t('weak')}</Text>}
                </Text>

                <View style={styles.icon}>
                    <MessageCircle size={15} color="#fff" fill="#fff" />
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    avatar: {
        width: 35,
        height: 35
    },

    info: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10
    },

    text: {
        fontSize: 16,
        fontWeight: 500
    },

    icon: {
        width: 40,
        height: 30,
        borderRadius: 15,
        backgroundColor: '#4DA6FF',
        alignItems: 'center',
        justifyContent: 'center'
    },

    chatStatus: {
        flexDirection: "row",
        alignItems: 'center',
        gap: 20
    },

    deviceInfoContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        padding: 20,
    }
});