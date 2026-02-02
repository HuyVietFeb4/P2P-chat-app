import { Image } from "expo-image";
import { StyleSheet, Text, useWindowDimensions, View } from "react-native";
export default function Onboarding4() {
    const { width, height } = useWindowDimensions();
    return (
        <>
            <View style = {{ width: width, height: height * 0.7 }}>
                <Image
                    source={require('@/assets/images/onboarding4.png')}
                    style={styles.imageStyle}
                    contentFit="contain"
                />
            </View>

            <View style = { styles.textContainer }>
                <Text style = { styles.title}>Let’s mesh up and start chatting.</Text>
                <Text style = { styles.subtitle }>Enable Bluetooth and explore your connected world.</Text>
            </View>
        </>
    );
}

const styles = StyleSheet.create({
    imageStyle: {
        width: "100%",
        height: "100%",
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