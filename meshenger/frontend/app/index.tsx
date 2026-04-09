import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";

// Prevent the splash screen from hiding automatically
SplashScreen.preventAutoHideAsync();

export default function App() {
    const router = useRouter();

    useEffect(() => {
        const checkFirstLaunch = async (): Promise<void> => {
            // Check if the app has been launched before
            const value = await AsyncStorage.getItem("firstLaunch");

            if (value === null) {
                // First time launch: set flag and go to Onboarding
                router.replace("/Onboarding");
            } else {
                // Subsequent launches: go straight to the ChatBox
                router.replace("/ChatBox");
            }
            // Hide the splash screen once routing is determined
            await SplashScreen.hideAsync();
        };

        checkFirstLaunch();
    }, []);
    
    return null;
}


