import { useRouter } from "expo-router";
import { ArrowLeft } from "lucide-react-native";
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";
import { useTranslation } from "react-i18next";
import { useTheme } from "@/app/context/ThemeContext";

export default function Header() {
    const { width, height } = useWindowDimensions();
    const router = useRouter();
    const { t } = useTranslation();
    const { colors } = useTheme();

    return (
        <View style={[{ width: width * 0.9, height: height * 0.15 }, styles.headerContainer]}>
            
            <Pressable style={[styles.left, {backgroundColor: colors.returnIcon}]} onPress={() => router.back()}>
                <ArrowLeft size={20} color='#fff' />
            </Pressable>

            <Text style={[styles.title, {color: colors.text}]}>{t('device-scanning')}</Text>

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
        borderRadius: 999,
        zIndex: 10
    },

    title: {
        textAlign: "center",
        fontSize: 18,
        fontWeight: '600'
    }
});