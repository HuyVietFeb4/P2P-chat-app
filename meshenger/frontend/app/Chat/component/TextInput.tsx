import React, { useState } from "react";
import { Plus, SendHorizontal } from "lucide-react-native";
import { StyleSheet, TextInput, TouchableOpacity, View, NativeModules } from "react-native";
import { useTheme } from "../../context/ThemeContext";

const { MeshengerApplicationModule } = NativeModules;

type Props = {
    peerId: string
}

export default function Input({peerId} : Props) {
    const { colors } = useTheme();
    const [message, setMessage] = useState('');

    const handleSend = async () => {
        if (message.trim()) {
            try {
                if (peerId === 'global-broadcast') {
                    await MeshengerApplicationModule.globalChatSendMessageStr(message.trim());
                } else {
                    // Placeholder for peer-to-peer messaging
                    console.log("Sending to peer:", peerId, message);
                }
                setMessage('');
            } catch (error) {
                console.error("Failed to send message:", error);
            }
        }
    };

    return (
        <View style={styles.chatInput}>
            <TouchableOpacity style={[styles.icon, { backgroundColor: colors.primary }]}>
                <Plus size={20} color="white" />
            </TouchableOpacity>

            <TextInput
                placeholder="Type a message"
                style={[styles.input, { backgroundColor: colors.card, color: colors.text, borderColor: colors.border }]}
                placeholderTextColor={colors.subText}
                value={message}
                onChangeText={setMessage}
                onSubmitEditing={handleSend}
            />

            <TouchableOpacity
                style={[styles.icon, { backgroundColor: colors.primary }]}
                onPress={handleSend}
            >
                <SendHorizontal size={15} color="white" />
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    icon: {
        width: 30,
        height: 30,
        borderRadius: 15,
        alignItems: "center",
        justifyContent: "center"
    },

    input: {
        width: "70%",
        borderRadius: 15,
        paddingHorizontal: 10,
        paddingVertical: 10,
        borderWidth: 1,
    },

    chatInput: {
        flexDirection: "row",
        width: "90%",
        alignSelf: "center",
        alignItems: "center",
        justifyContent: "space-between",
        paddingVertical: 10,
    },
})
