import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { Bell, BookA, ChevronRight, Pencil, SunMoon, UserLock } from "lucide-react-native";
import { ScrollView, StyleSheet, Switch, Text, View, TouchableOpacity, NativeModules } from "react-native";
import { useRouter } from "expo-router";
import { useTheme } from "../../context/ThemeContext";
import { useTranslation } from "react-i18next";
import { useState, useEffect } from "react";

const { MeshengerApplicationModule } = NativeModules;

const avatars = [
    { id: 'avt0', source: require('../../../assets/avt_set/avt0.png') },
    { id: 'avt1', source: require('../../../assets/avt_set/avt1.png') },
    { id: 'avt2', source: require('../../../assets/avt_set/avt2.png') },
    { id: 'avt3', source: require('../../../assets/avt_set/avt3.png') },
    { id: 'avt4', source: require('../../../assets/avt_set/avt4.png') },
    { id: 'avt5', source: require('../../../assets/avt_set/avt5.png') },
    { id: 'avt6', source: require('../../../assets/avt_set/avt6.png') },
    { id: 'avt7', source: require('../../../assets/avt_set/avt7.png') },
    { id: 'avt8', source: require('../../../assets/avt_set/avt8.png') },
    { id: 'avt9', source: require('../../../assets/avt_set/avt9.png') },
    { id: 'avt10', source: require('../../../assets/avt_set/avt10.png') },
    { id: 'avt11', source: require('../../../assets/avt_set/avt11.png') },
    { id: 'avt12', source: require('../../../assets/avt_set/avt12.png') },
];

export default function SettingList() {
    const router = useRouter();
    const { t } = useTranslation();

    const { isDarkMode, toggleDarkMode, colors } = useTheme();

    const [deviceName, setDeviceName] = useState<string>('Loading...');
    const [userAvtId, setUserAvtId] = useState<string>('avt0');

    useEffect(() => {
        const fetchIdentity = async () => {
            try {
                const result = await MeshengerApplicationModule.getMyIdentity();
                setDeviceName(result.displayName || 'Galaxy S5');
                setUserAvtId(result.userAvtId || 'avt0');
            } catch (error) {
                console.error("Failed to fetch identity:", error);
            }
        };
        fetchIdentity();
    }, []);

    const selectedAvtSource = avatars.find(a => a.id === userAvtId)?.source || avatars[0].source;

    return  (
        <ScrollView 
            style={{flex: 1, backgroundColor: colors.background}}
            contentContainerStyle={{paddingBottom: 50}}
        >
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>{t('profile-info')}</Text>
                <LinearGradient 
                    colors={['#00C6FF', '#0072FF']}
                    start={{x: 0, y: 0}}
                    end={{x: 1, y: 0}}
                    style={styles.profileWrapper}
                >
                    <View style={[styles.profileContainer]}>
                        <View style={styles.iconContainer}>
                            <Image
                                source={selectedAvtSource}
                                contentFit="cover"
                                style={styles.image}
                            />
                        </View>
                        <Text style={styles.profile}>{deviceName}</Text>
                    </View>

                    <Pencil size={16} color="#fff" />
                </LinearGradient>
            </View>

            {/* Messages */}
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>{t('messages')}</Text>

                <View style={[styles.fieldContainer, { backgroundColor: colors.settingItemBg, borderColor: colors.border }]}>
                    <View style={styles.profileWrapper}>
                        <View style={styles.profileContainer}>
                            <LinearGradient 
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <UserLock size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('blocked-users')}</Text>
                        </View>
                        <ChevronRight color={colors.subText} size={16} />
                    </View>

                    <View style={styles.profileWrapper}>
                        <View style={styles.profileContainer}>
                            <LinearGradient 
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <UserLock size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('messages-pending')}</Text>
                        </View>
                        <ChevronRight color={colors.subText} size={16} />
                    </View>

                    <View style={styles.profileWrapper}>
                        <View style={styles.profileContainer}>
                            <LinearGradient 
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <UserLock size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('messages-backup')}</Text>
                        </View>
                        <ChevronRight color={colors.subText} size={16} />
                    </View>
                </View>
            </View>

            {/* Setting */}
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>{t('settings')}</Text>

                <View style={[styles.fieldContainer, { backgroundColor: colors.settingItemBg, borderColor: colors.border }]}>
                    <TouchableOpacity style={styles.profileWrapper} onPress={() => router.push("/Language")}>
                        <View style={styles.profileContainer}>
                            <LinearGradient
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <BookA size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('language')}</Text>
                        </View>
                        <ChevronRight color="#72727A" size={16} />
                    </TouchableOpacity>

                    <View style={styles.profileWrapper}>
                        <View style={styles.profileContainer}>
                            <LinearGradient
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <SunMoon size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('dark-mode')}</Text>
                        </View>
                        <Switch
                            trackColor={{ false: "#ccc", true: colors.primary }}
                            thumbColor="#fff"
                            value={isDarkMode}
                            onValueChange={toggleDarkMode}
                        />
                    </View>

                    <View style={styles.profileWrapper}>
                        <View style={styles.profileContainer}>
                            <LinearGradient
                                style={styles.menuIcon}
                                colors={['#5B8CFF', '#7C7DFF']}
                                start={{x: 0, y: 0}}
                                end={{x: 1, y: 0}}
                            >
                                <Bell size={15} color="#fff"/>
                            </LinearGradient>

                            <Text style={[styles.profile, {color: colors.text}]}>{t('notifications')}</Text>
                        </View>
                        <Switch
                            trackColor={{ false: "#ccc", true: colors.primary }}
                            thumbColor="#fff"
                        />
                    </View>
                </View>
            </View>
        </ScrollView>
    )
}

const styles = StyleSheet.create({
    image: {
        width: 45,
        height: 45,
        borderRadius: 22.5
    },
    iconContainer: {
        width: 45,
        height: 45,
        justifyContent: 'center',
        alignItems: 'center',
        borderRadius: 22.5,
        backgroundColor: 'rgba(255, 255, 255, 0.15)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.3)',
    },

    profile: {
        fontSize: 14,
        color: "#fff"
    },

    profileContainer: {
        flexDirection: "row",
        alignItems: "center",
        gap: 10
    },

    profileWrapper: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        paddingVertical: 15,
        paddingHorizontal: 15,
        borderRadius: 15
    },

    profileInfo: {
        marginTop: 20,
        gap: 10,
        width: "90%",
        alignSelf: "center",
    },

    moreText: {
        fontSize: 12,
        fontWeight: 600,
        color: "rgba(107, 114, 128, 0.8)"
    },

    menuIcon: {
        alignSelf: "flex-start",
        padding: 10,
        borderRadius: 999
    },

    fieldContainer: {
        backgroundColor: "#F2F2F2",
        borderWidth: 1,
        borderColor: "#F2F2F2",
        borderRadius: 15
    }
});