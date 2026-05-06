import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import * as Localization from "expo-localization";

import en from "./locales/eng.json"
import vi from "./locales/vi.json";

const resources = {
  en: { translation: en },
  vi: { translation: vi },
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: Localization.getLocales()[0]?.languageCode || "vi",
    fallbackLng: "en",
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;