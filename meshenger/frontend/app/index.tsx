import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";

SplashScreen.preventAutoHideAsync();

export default function App() {
    const router = useRouter();
    useEffect(() => {
        const checkFirstLaunch = async (): Promise<void> => {
            const value = await AsyncStorage.getItem("firstLaunch");

            if (value === null) {
                await AsyncStorage.setItem("firstLaunch", "false");
                router.replace("/Onboarding");
            } else {
                router.replace("/ChatBox");
            }
            await SplashScreen.hideAsync();
        };

        checkFirstLaunch();
    }, []);
    
    return null;
}