import { useState } from "react";
import { FlatList, StyleSheet, Text, TouchableOpacity, View } from "react-native";

type Language = { code: string; flag: string; name: string };

const LANGUAGES: Language[] = [
    { code: 'vi', flag: '🇻🇳', name: 'Vietnamese' },
    { code: 'en', flag: '🇺🇸', name: 'English' },
];

export default function LanguageList() {
    const [selected, setSelected] = useState<string>('vi');

    const renderItem = ({ item }: { item: Language }) => {
        const isActive = selected === item.code;
        return (
            <TouchableOpacity
                style={[styles.row, isActive && styles.rowActive]}
                onPress={() => setSelected(item.code)}
                activeOpacity={0.7}
            >
                <Text style={styles.flag}>{item.flag}</Text>
                <Text style={[styles.langName, isActive && styles.langNameActive]}>
                    {item.name}
                </Text>
                <View style={[styles.radio, isActive && styles.radioActive]}>
                    {isActive && <View style={styles.radioInner} />}
                </View>
            </TouchableOpacity>
        );
    };

    return (
        <FlatList
            data={LANGUAGES}
            keyExtractor={item => item.code}
            renderItem={renderItem}
            contentContainerStyle={styles.list}
            scrollEnabled={false}
        />
    );
}

const styles = StyleSheet.create({
    list: {
        padding: 14,
        gap: 8,
    },

    row: {
        backgroundColor: '#fff',
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

    langNameActive: { color: '#3C3489' },

    radio: {
        width: 20, height: 20, borderRadius: 10,
        borderWidth: 1.5, borderColor: '#ccc',
        alignItems: 'center', justifyContent: 'center',
    },

    radioActive: { borderColor: '#5F2EEA', backgroundColor: '#5F2EEA' },

    radioInner: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#fff' },
});