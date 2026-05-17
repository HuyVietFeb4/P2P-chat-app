import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";
import { useCommPermission } from "@/hook/useCommPermission";
import { BackHandler, NativeModules } from "react-native";

const DEFAULT_NATIVE_DISPLAY_NAME = "Local User";

// Prevent the splash screen from hiding automatically
SplashScreen.preventAutoHideAsync();

export default function App() {
    const router = useRouter();
    const { MainModule, MeshengerApplicationModule } = NativeModules;

    const { granted } = useCommPermission(); // ✅ đặt ở đây

    // useEffect(() => {
    //     if (granted === null) return; // ⛔ chưa xong permission → giữ splash

    //     const checkFirstLaunch = async (): Promise<void> => {
    //         // if (!granted) {
    //         //     // ❌ không có permission → thoát
    //         //     await SplashScreen.hideAsync();
    //         //     BackHandler.exitApp();
    //         //     return;
    //         // }

    //         // MainModule.ensureServiceStarted();

    //         // // Onboarding used to save the name only in AsyncStorage; mesh/chat use SQLite via native.
    //         // const value = await AsyncStorage.getItem("firstLaunch");
    //         // if (
    //         //     value &&
    //         //     value !== "false" &&
    //         //     MeshengerApplicationModule?.getMyProfile &&
    //         //     MeshengerApplicationModule?.updateMyProfile
    //         // ) {
    //         //     try {
    //         //         const profile = await MeshengerApplicationModule.getMyProfile();
    //         //         if (profile?.displayName === DEFAULT_NATIVE_DISPLAY_NAME) {
    //         //             await MeshengerApplicationModule.updateMyProfile(value, null);
    //         //         }
    //         //     } catch {
    //         //         /* ignore sync failure; user can set name on Device Scan */
    //         //     }
    //         // }

    //         // if (value === null) {
    //         //     await AsyncStorage.setItem("firstLaunch", "false"); // 🔥 fix bug
    //         //     router.replace("/Onboarding");
    //         // } else {
    //         //     router.replace("/ChatBox");
    //         // }

    //         if (!granted) {
    //                 await SplashScreen.hideAsync();
    //                 BackHandler.exitApp();
    //                 return;
    //         }

    //         const isFirstLaunch = await AsyncStorage.getItem("firstLaunch");

    //         const profile = await MeshengerApplicationModule.getMyProfile();
    //         const canGetUserName = profile && profile.displayName !== "Local User";
    //         MainModule.ensureServiceStarted();

    //         if (canGetUserName && isFirstLaunch) {
    //             router.replace('/ChatBox');
    //         } else {
    //             router.replace('/Onboarding');
    //         }

    //         await SplashScreen.hideAsync();
    //     };

    //     checkFirstLaunch();
    // }, [granted]);

    useEffect(() => {
        if (granted === null) return;

        const initApp = async () => {
            try {
                if (!granted) {
                    return BackHandler.exitApp();
                }

                const isFirstLaunch = await AsyncStorage.getItem("firstLaunch");
                let profile;

                try {
                    profile = await MeshengerApplicationModule.getMyProfile();
                } catch (e) {
                    console.warn("MeshengerApplicationModule not ready", e);
                    profile = null;
                }

                const canGetUserName = profile && profile.displayName !== DEFAULT_NATIVE_DISPLAY_NAME;
                MainModule?.ensureServiceStarted?.();

                if (canGetUserName && isFirstLaunch) {
                    router.replace("/ChatBox");
                } else {
                    router.replace("/Onboarding");
                }

            } catch (e) {
                console.error("Error in app init", e);
            } finally {
                await SplashScreen.hideAsync();
            }
        };

        initApp();
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