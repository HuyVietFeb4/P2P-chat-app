import { useLocalSearchParams } from 'expo-router';
import React, { useEffect } from "react";
import { KeyboardAvoidingView, NativeModules, StyleSheet, View } from "react-native";
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';
import { useBehavior } from '@/hook/useBehavior';
import Body from "./component/Body";
import Header from "./component/Header";
import Input from "./component/TextInput";
import { useBluetooth } from "@/hook/useBluetooth";
import BluetoothPopup from "../common components/BluetoothPopUp";


const { MeshengerApplicationModule } = NativeModules;

export default function ChatRoom() {
    const { id, name, avatarUrl, qrBootstrap } = useLocalSearchParams();
    const insets = useSafeAreaInsets();
    const behavior = useBehavior();
    const { colors } = useTheme();
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();

    const peerId = (id as string) ?? '';
    const rawName = typeof name === 'string' ? name : '';
    const peerName = rawName.trim().length > 0 ? rawName.trim() : peerId;
    const isDirect = peerId.startsWith('mp:');

    const bootstrap =
        typeof qrBootstrap === 'string' ? qrBootstrap : '';

    useEffect(() => {
        if (!isDirect) return;
        const mod = MeshengerApplicationModule as {
            openTwoPartySession: (a: string, b: string, c: boolean) => Promise<unknown>;
            openTwoPartySessionWithBootstrap?: (a: string, b: string, mode: string) => Promise<unknown>;
        };
        const run =
            bootstrap === 'qr_scanner' || bootstrap === 'qr_display'
                ? () =>
                      (mod.openTwoPartySessionWithBootstrap ?? (() => Promise.reject('native'))).call(
                          mod,
                          peerId,
                          peerName,
                          bootstrap,
                      )
                : () => mod.openTwoPartySession(peerId, peerName, true);

        run().catch((error: any) => {
            console.warn('open session failed:', error?.message ?? error);
        });
    }, [isDirect, peerId, peerName, bootstrap]);

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
