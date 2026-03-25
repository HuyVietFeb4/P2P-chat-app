import { useRouter } from "expo-router";
import { ArrowLeft } from "lucide-react-native";
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

export default function Header() {
    const { width, height } = useWindowDimensions();
    const router = useRouter();
    console.log(router.canGoBack());

    return (
        <View style={[{ width: width * 0.9, height: height * 0.15 }, styles.headerContainer]}>
            
            <Pressable style={styles.left} onPress={() => router.back()}>
                <ArrowLeft size={20} color='#fff' />
            </Pressable>

            <Text style={styles.title}>Devices Scanning</Text>

        </View>
    );
}

const styles = StyleSheet.create({
    headerContainer: {
        marginTop: 20,
        justifyContent: 'center',
        alignSelf: 'center'
    },

    left: {
        position: "absolute",
        left: 20,
        padding: 5,
        backgroundColor: '#4DA6FF',
        borderRadius: 999,
        zIndex: 10
    },

    title: {
        textAlign: "center",
        fontSize: 18,
        fontWeight: '600'
    }
});