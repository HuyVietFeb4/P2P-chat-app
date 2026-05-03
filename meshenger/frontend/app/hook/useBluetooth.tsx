// hooks/useBluetooth.ts
import { useEffect, useRef, useState, useCallback } from "react";
import { BleManager, State } from "react-native-ble-plx";
import * as IntentLauncher from "expo-intent-launcher";

export function useBluetooth() {
    const bleManager = useRef(new BleManager()).current;

    const [isOn, setIsOn] = useState<boolean | null>(null);
    const [showPopup, setShowPopup] = useState(false);

    useEffect(() => {
        const sub = bleManager.onStateChange((state) => {
            if (state === State.PoweredOn) {
                setIsOn(true);
                setShowPopup(false);
            } else {
                setIsOn(false);
                setShowPopup(true);
            }
        }, true);

        return () => {
            sub.remove();
        };
    }, []);

    const openBluetoothSettings = useCallback(async () => {
        await IntentLauncher.startActivityAsync(
            "android.settings.BLUETOOTH_SETTINGS"
        );
    }, []);

    const dismissPopup = useCallback(() => {
        setShowPopup(false);
    }, []);

    return {
        isOn,
        showPopup,
        openBluetoothSettings,
        dismissPopup,
    };
}