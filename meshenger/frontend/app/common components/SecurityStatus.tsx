import { View, Text, StyleSheet } from 'react-native';
import { KeyRound } from 'lucide-react-native';
import { useTranslation } from 'react-i18next';
import { useTheme } from '../context/ThemeContext';

export type SecurityLevel = 'weak' | 'medium' | 'strong';

type Props = {
    level?: string | null;
};

function normalizeLevel(raw: string | null | undefined): SecurityLevel {
    const s = (raw ?? 'medium').toLowerCase().trim();
    if (s === 'weak' || s === 'medium' || s === 'strong') return s;
    return 'medium';
}

export default function SecurityStatus({ level }: Props) {
    const { colors } = useTheme();
    const { t } = useTranslation();
    const tier = normalizeLevel(level);

    const palette =
        tier === 'strong'
            ? colors.success
            : tier === 'medium'
              ? colors.warning
              : colors.error;

    const labelKey =
        tier === 'strong' ? 'security-strong' : tier === 'medium' ? 'security-medium' : 'security-weak';

    return (
        <View
            style={[
                styles.statusContainer,
                { backgroundColor: palette.bg, borderColor: palette.borderColor, borderWidth: 1 },
            ]}
        >
            <KeyRound size={12} color={palette.textColor} />
            <Text style={[styles.statusText, { color: palette.textColor }]}>{t(labelKey)}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    statusText: {
        fontSize: 10,
    },
    statusContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 3,
        paddingHorizontal: 8,
        paddingVertical: 5,
        borderRadius: 999,
    },
});
