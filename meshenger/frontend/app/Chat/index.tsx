import { useLocalSearchParams } from 'expo-router';
import React, { useEffect } from "react";
import { KeyboardAvoidingView, NativeModules, StyleSheet, View } from "react-native";
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';
import { useBehavior } from '../hook/useBehavior';
import Body from "./component/Body";
import Header from "./component/Header";
import Input from "./component/TextInput";
import { useBluetooth } from "../hook/useBluetooth";
import BluetoothPopup from "../common components/BluetoothPopUp";


const { MeshengerApplicationModule } = NativeModules;

export default function ChatRoom() {
    const { id, name, avatarUrl } = useLocalSearchParams();
    const insets = useSafeAreaInsets();
    const behavior = useBehavior();
    const { colors } = useTheme();
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();

    const peerId = (id as string) ?? '';
    const rawName = typeof name === 'string' ? name : '';
    const peerName = rawName.trim().length > 0 ? rawName.trim() : peerId;
    const isDirect = peerId.startsWith('mp:');

    useEffect(() => {
        if (!isDirect) return;
        // Idempotent on the native side: if a session is already open (e.g. created by the
        // responder fallback), this resolves without recreating it.
        MeshengerApplicationModule.openTwoPartySession(peerId, peerName, true).catch(
            (error: any) => {
                console.warn("openTwoPartySession failed:", error?.message ?? error);
            },
        );
    }, [isDirect, peerId, peerName]);

    return (
        <SafeAreaView style={{flex: 1, backgroundColor: colors.background}}>
            <KeyboardAvoidingView
                behavior={behavior}
                keyboardVerticalOffset={insets.bottom}
                style={[styles.container, { backgroundColor: colors.chatBackground }]}
            >
                <Header title={peerName} avatarUrl={(avatarUrl as string) ?? ''} status={true} />

                <View style={styles.bodyContainer}>
                    <Body peerId={peerId} />
                </View>

                <Input peerId={peerId} />

                {showPopup && (
                    <BluetoothPopup
                        visible={true}
                        onDismiss={dismissPopup}
                    />
                )}
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    bodyContainer: {
        flex: 1,
    }
});
