import { LinearGradient } from "expo-linear-gradient";
import { useRouter } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { ChevronLeft } from "lucide-react-native";
import { StyleSheet, Text, TouchableOpacity, View, useWindowDimensions } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useTranslation } from "react-i18next";

export default function Header() {
    const { width } = useWindowDimensions();
    const router = useRouter();
    const { t } = useTranslation();

    return (
        <LinearGradient
            colors={['#0F4C81', '#5F2EEA']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 0 }}
            style={{ width }}
        >
            <StatusBar translucent backgroundColor="transparent" style="light" />

            <SafeAreaView edges={['top']}>
                <View style={[styles.headerContainer, { width: width * 0.90 }]}>
                    <TouchableOpacity
                        onPress={() => router.back()}
                        style={styles.backButton}
                        activeOpacity={0.7}
                    >
                        <ChevronLeft size={22} color="#fff" />
                    </TouchableOpacity>

                    <Text style={styles.title}>{t('select-a-language')}</Text>

                    <View style={styles.placeholder} />
                </View>
            </SafeAreaView>
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
    headerContainer: {
        alignSelf: 'center',
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 14,
    },

    backButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: 'rgba(255,255,255,0.15)',
        alignItems: 'center',
        justifyContent: 'center',
    },

    title: {
        flex: 1,
        textAlign: 'center',
        fontSize: 16,
        fontWeight: '500',
        color: '#fff',
    },

    placeholder: {
        width: 36,
    },
});