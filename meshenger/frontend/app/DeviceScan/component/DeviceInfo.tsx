import { Image } from "expo-image";
import { MessageCircle } from "lucide-react-native";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { useTranslation } from "react-i18next";

type Props = {
    avatarName: string;
    status: number;
    onPress?: () => void;
    disabled?: boolean;
    loading?: boolean;
};

export default function DeviceInfo({ avatarName, status, onPress, disabled, loading }: Props) {
    const { t } = useTranslation();
    const content = (
        <View style={styles.deviceInfoContainer}>
            <View style={styles.info}>
                <Image
                    source={require('@/assets/images/avatar.png')}
                    style={styles.avatar}
                />
                <Text style={styles.text} numberOfLines={1}>{avatarName}</Text>
            </View>

            <View style={styles.chatStatus}>
                <Text style={{ fontSize: 10, fontStyle: 'italic', fontWeight: '600' }}>
                    {t('status')}:{' '}
                    {status === 1
                        ? <Text style={{ color: '#22C55E', fontSize: 10 }}>{t('strong')}</Text>
                        : <Text style={{ color: '#EF4444', fontSize: 10 }}>{t('weak')}</Text>}
                </Text>

                <View style={styles.icon}>
                    {loading
                        ? <ActivityIndicator size="small" color="#fff" />
                        : <MessageCircle size={15} color="#fff" fill="#fff" />}
                </View>
            </View>
        </View>
    );

    if (!onPress) return content;
    return (
        <Pressable
            onPress={onPress}
            disabled={disabled || loading}
            style={({ pressed }) => [pressed && { opacity: 0.6 }, disabled && { opacity: 0.5 }]}
        >
            {content}
        </Pressable>
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
        gap: 10,
        flex: 1,
        marginRight: 10,
    },

    text: {
        fontSize: 16,
        fontWeight: '500',
        flexShrink: 1,
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
        alignItems: "center",
        gap: 20
    },

    deviceInfoContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 20,
    }
});
