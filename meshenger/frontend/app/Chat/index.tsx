import React from "react";
import { View, KeyboardAvoidingView, Platform, StyleSheet } from "react-native";
import { useLocalSearchParams } from 'expo-router';
import Header from "./component/Header";
import Body from "./component/Body";
import TextInput from "./component/TextInput";

export default function ChatRoom() {
    const { id, name, avatarUrl } = useLocalSearchParams();

    return (
        /* KeyboardAvoidingView replaces the standard wrapper View. 
           It automatically shrinks or pads the screen when the keyboard appears. */
        <KeyboardAvoidingView 
            style={styles.container}
            behavior={Platform.OS === "ios" ? "padding" : undefined}
            keyboardVerticalOffset={Platform.OS === "ios" ? 0 : 20} // Adjust if you have a top navigation bar
        >
            {/* 1. Reusable Header - Using the passed name and avatar */}
            <Header
                title={(name as string) || "Chat"}
                avatarUrl={avatarUrl as string}
            />
            
            {/* 2. Chat Body: flex: 1 tells it to fill all space between Header and Input */}
            <View style={styles.bodyContainer}>
                <Body peerId={id as string} />
            </View>
            
            {/* 3. Text Input Area: Naturally sits at the bottom */}
            <TextInput peerId={id as string} />
            
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F0F6FF', // The light blue background from your XML
    },
    bodyContainer: {
        flex: 1,
    }
});
