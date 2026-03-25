import { Image } from "expo-image";
import { MessageCircle } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";

export default function DeviceInfo() {
    return (
        <View style={styles.deviceInfoContainer}>
            <View style={styles.info}>
                <Image 
                    source={require('@/assets/images/avatar.png')}
                    style={styles.avatar}
                />
                <Text style={styles.text}>Alice</Text>
            </View>

            <View style={styles.chatStatus}>
                <Text>
                    Status: <Text>Strong</Text>
                </Text>

                <View style={styles.icon}>
                    <MessageCircle size={15} color="#fff" fill="#fff" />
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    avatar: {
        width: 35,
        height: 35
    },

    info: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10
    },

    text: {
        fontSize: 16,
        fontWeight: 500
    },

    icon: {
        width: 40,
        height: 30,
        borderRadius: 15,
        backgroundColor: '#4DA6FF',
        alignItems: 'center',
        justifyContent: 'center'
    },

    chatStatus: {
        flexDirection: "row",
        alignItems: 'center',
        gap: 20
    },

    deviceInfoContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        padding: 20,
    }
});