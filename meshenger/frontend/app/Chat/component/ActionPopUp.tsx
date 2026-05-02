import { Ban, BellOff, Search, Trash2, UserMinus } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";

const NORMAL_COLOR = "#4A90E2";
const DANGER_COLOR = "#E24B4A";

type Props = {
    setOnClose: () => void;
}

export default function ActionPopUp({setOnClose} : Props) {
    return (
        <View style={styles.container}>
            <View style={styles.option}>
                <Search size={15} color={NORMAL_COLOR} strokeWidth={1.8} />
                <Text style={styles.text}>Search Messages</Text>
            </View>

            <View style={styles.option}>
                <BellOff size={15} color={NORMAL_COLOR} strokeWidth={1.8} />
                <Text style={styles.text}>Mute Notifications</Text>
            </View>

            <View style={styles.divider} />

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
        backgroundColor: '#FFFFFF',
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
        color: NORMAL_COLOR,
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
        backgroundColor: '#E0E0E0',
        marginVertical: 4,
        marginHorizontal: 10,
    },
});