// Test Hello world
import { NativeModules } from 'react-native';

const { HelloWorldModule } = NativeModules;

import { Image } from "expo-image";
import { StyleSheet, Text, useWindowDimensions, View } from "react-native";
export default function Onboarding1() {
    const { width, height } = useWindowDimensions();
    return (
        <View style = {{ flex: 1}}>
            <View style = {{ width: width, height: height * 0.7 }}>
                <Image
                    source={require('@/assets/images/onboarding1.png')}
                    style={styles.imageStyle}
                    contentFit="cover"
                />
            </View>

            <View style = { styles.textContainer }>
                <Text>{HelloWorldModule.getMessage()}</Text>
                <Text style = { styles.title}>Chat without Wifi or Internet</Text>
                <Text style = { styles.subtitle }>Stay connected anywhere through Bluetooth Mesh — even when you’re offline.</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    imageStyle: {
        width: "100%",
        height: "100%"
    },

    textContainer: {
        flexShrink: 1,
        paddingHorizontal: 25,
        marginTop: 20,
        width: "100%"
    },

    title: {
        flexShrink: 1,
        fontSize: 20,
        fontWeight: "600",
        marginBottom: 10,
        textAlign: "center",
    },
    subtitle: {
        flexShrink: 1,
        lineHeight: 20,
        fontSize: 14,
        textAlign: "center",
        color: "#666",
    }
});