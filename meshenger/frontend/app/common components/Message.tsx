import { CircleX } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";

export default function Message() {
    return (
        <View style={styles.container}>
            <CircleX size={15} color="#FF3B30" />
            <Text style={styles.text}>Invalid QR Code! Please try again!</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        bottom: 50,
        alignSelf: 'center',
        paddingHorizontal: 20,
        paddingVertical: 10,
        borderRadius: 20,
        borderWidth: 1.5,
        borderColor: '#FF6B6B',
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        backgroundColor: '#FFE5E5'
    },

    text: {
        fontSize: 12,
        color: '#FF3B30',
        fontWeight: 'bold'
    }
});