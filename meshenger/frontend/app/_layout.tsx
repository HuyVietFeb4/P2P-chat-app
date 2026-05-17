import { Stack } from "expo-router";
import { ThemeProvider } from "./context/ThemeContext";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTranslation } from "react-i18next";
import { useEffect } from "react";
import * as Localization from 'expo-localization';
import "./utils/i18n";

export default function RootLayout() {
  const { t, i18n } = useTranslation();
  const locales = Localization.getLocales();
  const languageCode = locales[0]?.languageCode || 'vi';

  useEffect(() => {
    const getLanguage = async () => {
      let lan = await AsyncStorage.getItem("language");
      if (!lan) {
        await AsyncStorage.setItem("language", languageCode);
        lan = languageCode;
      }
      
      await i18n.changeLanguage(lan);
    }

    getLanguage();
  }, []);

  return (
    <ThemeProvider>
      <Stack screenOptions={{ headerShown: false }}/>
    </ThemeProvider>
  );
}
