import { useLocalSearchParams } from 'expo-router';
import React from "react";
import { KeyboardAvoidingView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Body from "./component/Body";
import Header from "./component/Header";
import Input from "./component/TextInput";

export default function ChatRoom() {
    const { id, name, avatarUrl } = useLocalSearchParams();
    const insets = useSafeAreaInsets();

    return (
        <KeyboardAvoidingView
            behavior="height"
            style={styles.container}
        >
            {/* 1. Reusable Header - Using the passed name and avatar */}
            <Header title="abc" avatarUrl="abc" status={true} />
            
            {/* 2. Chat Body: flex: 1 tells it to fill all space between Header and Input */}
            <View style={styles.bodyContainer}>
                <Body peerId={id as string} />
            </View>
            
            {/* 3. Text Input Area: Naturally sits at the bottom */}
            <Input peerId={id as string} />
            
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
