// hooks/useCommPermission.ts
import { useEffect, useState } from "react";
import { BackHandler, NativeModules } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import requestBlePermissions from "../utils/permissions";

export function useCommPermission() {
    const [granted, setGranted] = useState<boolean | null>(null);
    useEffect(() => {
        let isMounted = true;

        const init = async () => {
            try {
                const hasAsked = await AsyncStorage.getItem("askedPermission");

                const result = await requestBlePermissions();

                if (!isMounted) return;

                setGranted(result);

                // lưu lại là đã hỏi permission
                await AsyncStorage.setItem("askedPermission", "true");

                // ❌ chỉ exit nếu là lần đầu
                if (!result && !hasAsked) {
                    setTimeout(() => {
                        BackHandler.exitApp();
                    }, 500);
                }

            } catch (err) {
                console.log("Permission error:", err);

                if (isMounted) {
                    setGranted(false);
                }
            }
        };

        init();

        return () => {
            isMounted = false;
        };
    }, []);

    return { granted };
}