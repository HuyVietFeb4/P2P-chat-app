import { useState, useEffect } from "react";
import { FlatList, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import Header from "./component/Header";
import { useTranslation } from "react-i18next";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useBluetooth } from "@/hook/useBluetooth";
import BluetoothPopup from "../common components/BluetoothPopUp";
import { useTheme } from "../context/ThemeContext";

type Language = { code: string; flag: string; name: string };

export default function Language() {
    const { t, i18n } = useTranslation();
    const [selected, setSelected] = useState<string>("");
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();
    const { colors } = useTheme();

    const LANGUAGES: Language[] = [
        { code: 'vi', flag: '🇻🇳', name:  t('vietnamese')},
        { code: 'en', flag: '🇺🇸', name: t('english') },
    ];

    useEffect(() => {
        const loadLanguage = async () => {
            const savedLan = await AsyncStorage.getItem("language");
            if (savedLan) {
                setSelected(savedLan);
            }
        }

        loadLanguage();
    }, []);

    const handleSelect = async (code: string) => {
        setSelected(code);
        await AsyncStorage.setItem("language", code);
        i18n.changeLanguage(code);
    };

    const renderItem = ({ item }: { item: Language }) => {
        const isActive = selected === item.code;
        return (
            <TouchableOpacity
                style={[styles.row, isActive && styles.rowActive, {backgroundColor: colors.cardBg}]}
                onPress={() => handleSelect(item.code)}
                activeOpacity={0.7}
            >
                <Text style={styles.flag}>{item.flag}</Text>
                <Text style={[styles.langName, isActive && styles.langNameActive, {color: colors.text}]}>
                    {item.name}
                </Text>
                <View style={[styles.radio, isActive && styles.radioActive]}>
                    {isActive && <View style={styles.radioInner} />}
                </View>
            </TouchableOpacity>
        );
    };

    return (
        <View style={{flex: 1, backgroundColor: colors.scannedBg}}>
            <Header />
            <FlatList
                data={LANGUAGES}
                keyExtractor={item => item.code}
                renderItem={renderItem}
                contentContainerStyle={styles.list}
                scrollEnabled={false}
            />

             {showPopup && (
                            <BluetoothPopup
                                visible={true}
                                onDismiss={dismissPopup}
                            />
                        )}
        </View>
    );
}

const styles = StyleSheet.create({
    list: {
        padding: 14,
        gap: 8,
    },

    row: {
        borderRadius: 12,
        paddingVertical: 14,
        paddingHorizontal: 14,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        borderWidth: 0.5,
        borderColor: 'rgba(0,0,0,0.07)',
    },

    rowActive: {
        borderWidth: 1.5,
        borderColor: '#5F2EEA',
        backgroundColor: '#EEEDFE',
    },

    flag: { fontSize: 26 },

    langName: {
        flex: 1,
        fontSize: 15,
        fontWeight: '500',
        color: '#2C2C2A',
    },

    langNameActive: { fontWeight: 'bold' },

    radio: {
        width: 20, height: 20, borderRadius: 10,
        borderWidth: 1.5, borderColor: '#ccc',
        alignItems: 'center', justifyContent: 'center',
    },

    radioActive: { borderColor: '#5F2EEA', backgroundColor: '#5F2EEA' },

    radioInner: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#fff' },
});