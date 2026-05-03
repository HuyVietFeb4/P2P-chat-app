import { Ban, BellOff, Search, Trash2, UserMinus } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";
import { useTheme } from "../../context/ThemeContext";

const DANGER_COLOR = "#E24B4A";

type Props = {
    setOnClose: () => void;
}

export default function ActionPopUp({setOnClose} : Props) {
    const { colors } = useTheme();

    return (
        <View style={[styles.container, { backgroundColor: colors.card }]}>
            <View style={styles.option}>
                <Search size={15} color={colors.primary} strokeWidth={1.8} />
                <Text style={[styles.text, { color: colors.text }]}>Search Messages</Text>
            </View>

            <View style={styles.option}>
                <BellOff size={15} color={colors.primary} strokeWidth={1.8} />
                <Text style={[styles.text, { color: colors.text }]}>Mute Notifications</Text>
            </View>

            <View style={[styles.divider, { backgroundColor: colors.border }]} />

            <View style={styles.option}>
                <Trash2 size={15} color={DANGER_COLOR} strokeWidth={1.8} />
                <Text style={[styles.text, styles.dangerText]}>Clear Chat</Text>
            </View>

            <View style={styles.option}>
                <Ban size={15} color={DANGER_COLOR} strokeWidth={1.8} />
                <Text style={[styles.text, styles.dangerText]}>Block User</Text>
            </View>

            <View style={styles.option}>
                <UserMinus size={15} color={DANGER_COLOR} strokeWidth={1.8} />
                <Text style={[styles.text, styles.dangerText]}>Delete User</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        borderRadius: 12,
        paddingVertical: 6,
        paddingHorizontal: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.12,
        shadowRadius: 12,
        elevation: 8,
        alignSelf: "flex-start",
    },

    text: {
        fontSize: 14,
    },

    dangerText: {
        color: DANGER_COLOR,
    },

    option: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        paddingVertical: 11,
        paddingHorizontal: 14,
    },

    divider: {
        height: 0.5,
        marginVertical: 4,
        marginHorizontal: 10,
    },
});
