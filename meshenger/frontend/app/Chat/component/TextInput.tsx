import React, { useState } from 'react';
import { View, TextInput as RNTextInput, TouchableOpacity, StyleSheet, NativeModules } from 'react-native';
import { Ionicons, FontAwesome } from '@expo/vector-icons';

const { MeshengerApplicationModule } = NativeModules;

export default function TextInput({ peerId }: { peerId: string }) {
    const [message, setMessage] = useState('');

    const handleSend = async () => {
        if (message.trim()) {
            try {
                await MeshengerApplicationModule.sendMessage(peerId, message.trim());
                setMessage('');
            } catch (error) {
                console.error("Failed to send message:", error);
            }
        }
    };

    return (
        <View style={styles.container}>
            <TouchableOpacity style={styles.plusButton}>
                <Ionicons name="add-circle" size={45} color="#4A90E2" />
            </TouchableOpacity>

            <View style={styles.inputWrapper}>
                <RNTextInput
                    style={styles.input}
                    value={message}
                    onChangeText={setMessage}
                    placeholder="Type a message"
                    placeholderTextColor="#999"
                />
            </View>

            <TouchableOpacity onPress={handleSend} style={styles.sendButton}>
                <Ionicons name="send" size={26} color="#FFFFFF" />
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        paddingHorizontal: 10,
        paddingVertical: 10,
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
    },
    plusButton: {
        marginRight: 5,
    },
    inputWrapper: {
        flex: 1,
        backgroundColor: '#F2F2F2',
        borderRadius: 25,
        paddingHorizontal: 15,
        height: 45,
        justifyContent: 'center',
        marginRight: 10,
    },
    input: {
        fontSize: 16,
        color: '#333',
    },
    sendButton: {
        backgroundColor: '#4A90E2',
        width: 45,
        height: 45,
        borderRadius: 22.5,
        justifyContent: 'center',
        alignItems: 'center',
    },
});
