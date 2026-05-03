import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";
import { useCommPermission } from "./hook/useCommPermission";
import { BackHandler, NativeModules } from "react-native";

// Prevent the splash screen from hiding automatically
SplashScreen.preventAutoHideAsync();

export default function App() {
    const router = useRouter();
    const { MainModule } = NativeModules;

    const { granted } = useCommPermission(); // ✅ đặt ở đây

    useEffect(() => {
        if (granted === null) return; // ⛔ chưa xong permission → giữ splash

        const checkFirstLaunch = async (): Promise<void> => {
            if (!granted) {
                // ❌ không có permission → thoát
                await SplashScreen.hideAsync();
                BackHandler.exitApp();
                return;
            }

            MainModule.ensureServiceStarted();
            
            const value = await AsyncStorage.getItem("firstLaunch");

            if (value === null) {
                await AsyncStorage.setItem("firstLaunch", "false"); // 🔥 fix bug
                router.replace("/Onboarding");
            } else {
                router.replace("/ChatBox");
            }

            await SplashScreen.hideAsync();
        };

        checkFirstLaunch();
    }, [granted]);

    return null;
}
    
//     return null;
// }


// TESTING ONLY, WILL BE DELETED LATER
// import React from "react";
// import { StyleSheet, View } from "react-native";
// import SimpleSendStrTest from "./BackendTest/SimpleSendStrTest";

// export default function Index() {
//   return (
//     <View style={ styles.container }>
//       {/* test screens first */}
//       {/* <OnboardingScreen /> */}
//       <SimpleSendStrTest />
//     </View>
//   );
// }

// const styles = StyleSheet.create({
//   container: {
//     flex: 1,
//     width: '100%',
//     height: '100%',
//     backgroundColor: '#E6F9FF'
//   }
// });