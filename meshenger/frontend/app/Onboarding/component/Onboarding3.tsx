import { Image } from "expo-image";
import { StyleSheet, Text, useWindowDimensions, View } from "react-native";
export default function Onboarding3() {
    const { width, height } = useWindowDimensions();
    return (
        <>
            <View style = {{ width: width, height: height * 0.7, justifyContent: "flex-end" }}>
                <Image
                    source={require('@/assets/images/onboarding3.png')}
                    style={styles.imageStyle}
                    contentFit="contain"
                />
            </View>

            <View style = { styles.textContainer }>
                <Text style = { styles.title}>Instant messaging, redefined.</Text>
                <Text style = { styles.subtitle }>No setup needed — just open the app and start chatting with nearby friends.</Text>
            </View>
        </>
    );
}

const styles = StyleSheet.create({
    imageStyle: {
        width: "100%",
        height: "90%",
        alignSelf: "center"
    },

    textContainer: {
        alignItems: "center",
        paddingHorizontal: 25,
        marginTop: 20
    },

    title: {
        fontSize: 20,
        fontWeight: "600",
        marginBottom: 10,
        textAlign: "center"
    },
    subtitle: {
        fontSize: 14,
        textAlign: "center",
        color: "#666"
    }
});