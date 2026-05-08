import { Image } from "expo-image";
import { Plus } from "lucide-react-native";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { useTranslation } from "react-i18next";
import { useTheme } from "@/app/context/ThemeContext";
import { getAvatarSource } from "@/assets/avatarMap";

type Props = {
    avatarName: string;
    avatarId?: string;
    status: number;
    onPress?: () => void;
    disabled?: boolean;
    loading?: boolean;
};

export default function DeviceInfo({ avatarName, avatarId, status, onPress, disabled, loading }: Props) {
    const { t } = useTranslation();
    const { colors } = useTheme();
    const avatarSource = getAvatarSource(avatarId);
    const content = (
        <View style={styles.deviceInfoContainer}>
            <View style={styles.info}>
                <View style={[styles.avatarContainer, { borderColor: colors.border }]}>
                    <Image
                        source={avatarSource}
                        style={styles.avatar}
                    />
                </View>
                <Text style={[styles.text, {color: colors.text}]} numberOfLines={1}>{avatarName}</Text>
            </View>

            <View style={styles.chatStatus}>
                <Text style={{ fontSize: 10, fontStyle: 'italic', fontWeight: '600', color: colors.text }}>
                    {t('status')}:{' '}
                    {status === 1
                        ? <Text style={{ color: '#22C55E', fontSize: 10 }}>{t('strong')}</Text>
                        : <Text style={{ color: '#EF4444', fontSize: 10 }}>{t('weak')}</Text>}
                </Text>

                <View style={styles.icon}>
                    {loading
                        ? <ActivityIndicator size="small" color="#fff" />
                        : <Plus size={15} color="#fff" fill="#fff" />}
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
        height: 35,
        borderRadius: 17.5,
    },

    avatarContainer: {
        width: 41,
        height: 41,
        justifyContent: 'center',
        alignItems: 'center',
        borderRadius: 20.5,
        backgroundColor: 'rgba(128, 128, 128, 0.08)',
        borderWidth: 1.5,
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
