import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { ChevronLeft } from 'lucide-react-native';
import { Pressable, StyleSheet, Text, useWindowDimensions } from 'react-native';
import { useTranslation } from 'react-i18next';

export default function Header() {
    const router = useRouter();
    const { width, height } = useWindowDimensions();
    const { t } = useTranslation();
    return (
        <LinearGradient style={[styles.container, {width: width, height: height * 0.15}]}
                        colors={['#0F4C81', '#5F2EEA']}
                        start={{x: 0, y: 0}}
                        end={{x: 1, y: 0}} 
        >
            <Pressable onPress={() => router.back()} hitSlop={12} accessibilityRole="button">
                <ChevronLeft size={24} color="#fff" />
            </Pressable>
            <Text style={styles.title}>{t('connect-a-user')}</Text>          
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'flex-end',
        paddingHorizontal: 15,
        paddingBottom: 20
    },

    title: {
        color: '#fff',
        fontSize: 18,
        fontWeight: '500',
        marginLeft: 16,
    }
});