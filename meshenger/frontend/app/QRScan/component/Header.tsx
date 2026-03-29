import { useRouter } from "expo-router";
import { ArrowLeft } from "lucide-react-native";
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

type Props = {
    title: String,
    instruction: String
}

export default function Header({ title, instruction }: Props) {
    const { width, height } = useWindowDimensions();
    const router = useRouter();

    return (
        <View style={[{ width: width * 0.95 }, styles.headerContainer]}>
            
            <Pressable style={styles.left} onPress={() => router.back()}>
                <ArrowLeft size={20} color='#fff' />
            </Pressable>

            <View style={styles.titleContainer}>
                <Text style={styles.title}>{title}</Text>
                <Text style={styles.instruction}>{instruction}</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    headerContainer: {
        marginTop: 20,
        justifyContent: 'center',
        alignSelf: 'center',
        paddingVertical: 30
    },

    left: {
        position: "absolute",
        left: 10,
        padding: 8,
        backgroundColor: 'rgba(0,0,0,0.3)',
        borderRadius: 999,
        zIndex: 10
    },

    title: {
        textAlign: "center",
        fontSize: 16,
        fontWeight: '600',
        color: 'white'
    },

    instruction: {
        textAlign: 'center',
        color: '#fff',
        fontSize: 12
    },

    titleContainer: {
        zIndex: 10,
        padding: 10,
        borderRadius: 12,
        alignSelf: 'center',
        gap: 5,
        backgroundColor: 'rgba(0,0,0,0.3)'
    }
});