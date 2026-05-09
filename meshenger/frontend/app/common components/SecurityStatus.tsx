import { View, Text, StyleSheet } from "react-native";
import { KeyRound } from "lucide-react-native"
import { useTheme } from "../context/ThemeContext";

export default function SecurityStatus() {
    const { colors } = useTheme();
    return (
        <View style={[styles.statusContainer, {backgroundColor: colors.success.bg, borderColor: colors.success.borderColor}]}>
            <KeyRound size={12} color={colors.success.textColor} />
            <Text style={[styles.statusText, {color: colors.success.textColor}]}>Weak</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    statusText: {
        fontSize: 10
    },

    statusContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 3,
        paddingHorizontal: 8,
        paddingVertical: 5,
        borderRadius: 999
    }
})