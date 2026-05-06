import { Image } from "expo-image";
import { StyleSheet, Text, useWindowDimensions, View } from "react-native";
import { useTranslation } from "react-i18next";
export default function Onboarding4() {
    const { width, height } = useWindowDimensions();
    const { t } = useTranslation();
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
                <Text style = { styles.title}>{t('let-meshed-up')}</Text>
                <Text style = { styles.subtitle }>{t('enable-bluetooth-and-explore')}</Text>
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