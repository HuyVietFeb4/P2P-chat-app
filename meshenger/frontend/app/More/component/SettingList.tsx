import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { Bell, BookA, ChevronRight, Pencil, SunMoon, UserLock } from "lucide-react-native";
import { ScrollView, StyleSheet, Switch, Text, View, TouchableOpacity, NativeModules } from "react-native";
import { useRouter } from "expo-router";
import { useTheme } from "../../context/ThemeContext";
import { useTranslation } from "react-i18next";
import { useState, useEffect } from "react";
import { getAvatarSource } from "../../../assets/avatarMap";
import InfoPopUp from "./InfoPopUp";

const { MeshengerApplicationModule } = NativeModules;



export default function SettingList() {
    const router = useRouter();
    const { t } = useTranslation();

    const { isDarkMode, toggleDarkMode, colors } = useTheme();

    const [deviceName, setDeviceName] = useState<string>('Loading...');
    const [userAvtId, setUserAvtId] = useState<string>('avt0');
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);

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

    const selectedAvtSource = getAvatarSource(userAvtId);

    return  (
        <View style={{flex: 1}}>
            <ScrollView 
                style={{flex: 1, backgroundColor: colors.background}}
                contentContainerStyle={{paddingBottom: 50}}
            >
                <TouchableOpacity style={styles.profileInfo} onPress={() => setOpenPopUp(true)}>
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
                            <Text style={[styles.profile, {fontWeight: 'bold'}]}>{deviceName}</Text>
                        </View>

                        <Pencil size={16} color="#fff" />
                    </LinearGradient>
                </TouchableOpacity>

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
                
                {
                    openPopUp && (
                        <InfoPopUp onClose={() => setOpenPopUp(false)} />
                    )
                }
            </ScrollView>
        </View>
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
        color: "#fff",
        fontWeight: 500
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