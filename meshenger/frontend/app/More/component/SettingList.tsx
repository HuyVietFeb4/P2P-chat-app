import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { Bell, BookA, ChevronRight, Pencil, SunMoon, UserLock } from "lucide-react-native";
import { ScrollView, StyleSheet, Switch, Text, View, TouchableOpacity } from "react-native";
import { useRouter } from "expo-router";
import { useTheme } from "../../context/ThemeContext";

export default function SettingList() {
    const router = useRouter();

    const { isDarkMode, toggleDarkMode, colors } = useTheme();

    return  (
        <ScrollView 
            style={{flex: 1, backgroundColor: colors.background}}
            contentContainerStyle={{paddingBottom: 50}}
        >
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>Profile Info</Text>
                <LinearGradient 
                    colors={['#00C6FF', '#0072FF']}
                    start={{x: 0, y: 0}}
                    end={{x: 1, y: 0}}
                    style={styles.profileWrapper}
                >
                    <View style={[styles.profileContainer]}>
                        <Image
                            source={require("@/assets/images/avatar.png")}
                            contentFit="cover"
                            style={styles.image}
                        />
                        <Text style={styles.profile}>Galaxy S5</Text>
                    </View>

                    <Pencil size={16} color="#fff" />
                </LinearGradient>
            </View>

            {/* Messages */}
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>Messages</Text>

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

                            <Text style={[styles.profile, {color: colors.text}]}>Blocked Users</Text>
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

                            <Text style={[styles.profile, {color: colors.text}]}>Messages Pending</Text>
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

                            <Text style={[styles.profile, {color: colors.text}]}>Messages Backup</Text>
                        </View>
                        <ChevronRight color={colors.subText} size={16} />
                    </View>
                </View>
            </View>

            {/* Setting */}
            <View style={styles.profileInfo}>
                <Text style={[styles.moreText, { color: colors.sectionTitle }]}>Settings</Text>

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

                            <Text style={[styles.profile, {color: colors.text}]}>Language</Text>
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

                            <Text style={[styles.profile, {color: colors.text}]}>Dark Mode</Text>
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

                            <Text style={[styles.profile, {color: colors.text}]}>Notification</Text>
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
        height: 45
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