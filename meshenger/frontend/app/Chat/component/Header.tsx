import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { ArrowLeft, Ellipsis } from "lucide-react-native";
import { useState } from "react";
import { Modal, Pressable, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { Shadow } from "react-native-shadow-2";
import { useTheme } from "../../context/ThemeContext";
import ActionPopUp from "./ActionPopUp";

type Props = {
    title: string,
    avatarUrl: string,
    status: boolean
}

export default function Header({ title, avatarUrl, status }: Props) {
    const router = useRouter();
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);
    const { colors, isDarkMode } = useTheme();

    return (
        <>
            <StatusBar translucent backgroundColor="transparent" style={isDarkMode ? "light" : "dark"} />
                <Shadow
                    distance={20}
                    startColor={isDarkMode ? "rgba(0, 0, 0, 0.2)" : "rgba(13, 10, 44, 0.07)"}
                    offset={[0, 4]}
                    sides={{ bottom: true, top: false, start: false, end: false }}
                    style={[styles.shadow, { backgroundColor: colors.background }]}
                >
                    <View style={styles.header}>
                        <View style={styles.account}>
                            <TouchableOpacity style={[styles.icon, { backgroundColor: colors.primary }]} onPress={() => router.back()}>
                                <ArrowLeft size={20} color="white" />
                            </TouchableOpacity>

                            <View style={styles.profile}>
                                <Image
                                    source={require("@/assets/images/avatar.png")}
                                    contentFit="cover"
                                    style={styles.image}
                                />
                                <Text style={[styles.text, { color: colors.text }]}>{title}</Text>
                            </View>
                        </View>

                        <TouchableOpacity style={[styles.icon, { backgroundColor: colors.primary }]} onPress={() => setOpenPopUp(true)}>
                            <Ellipsis size={20} color="white" />
                        </TouchableOpacity>
                    </View>
                </Shadow>

                <Modal
                    visible={openPopUp}
                    transparent
                    animationType="fade"
                    onRequestClose={() => setOpenPopUp(false)}
                >
                    <Pressable style={styles.overlay} onPress={() => setOpenPopUp(false)}>
                        <Pressable style={styles.popupWrapper} onPress={(e) => e.stopPropagation()}>
                            <ActionPopUp setOnClose={() => setOpenPopUp(false)} />
                        </Pressable>
                    </Pressable>
                </Modal>
        </>
    );
}

const styles = StyleSheet.create({
    headerContainer: {
        backgroundColor: 'white',
    },
    shadow: {
        width: "100%",
    },
    icon: {
        width: 30,
        height: 30,
        borderRadius: 15,
        alignItems: "center",
        justifyContent: "center"
    },
    text: {
        fontSize: 18,
        fontWeight: "bold"
    },
    account: {
        flexDirection: "row",
        alignItems: "center",
        gap: 24
    },
    image: {
        width: 35,
        height: 35
    },
    profile: {
        flexDirection: "row",
        alignItems: "center",
        gap: 10
    },
    header: {
        width: "90%",
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        alignSelf: "center",
        paddingBottom: 15,
    },
    overlay: {
        flex: 1
    },
    popupWrapper: {
        position: 'absolute',
        top: 70,
        right: 15
    },
})
