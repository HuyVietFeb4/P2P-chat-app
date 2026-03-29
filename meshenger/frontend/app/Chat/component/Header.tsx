import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, SafeAreaView, Platform, StatusBar } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

const DEFAULT_AVATAR = require('../../../assets/images/avatar.png');

export default function Header({ title, avatarUrl }: { title: string, avatarUrl?: string }) {
    const router = useRouter();

    const handleBack = () => {
        console.log("Back button pressed");
        if (router.canGoBack()) {
            router.back();
        } else {
            router.replace('/ChatBox');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.headerContainer}>
                <View style={styles.leftSection}>
                    <TouchableOpacity
                        onPress={handleBack}
                        style={styles.backButton}
                        activeOpacity={0.7}
                        hitSlop={{ top: 15, bottom: 15, left: 15, right: 15 }}
                    >
                        <Ionicons name="arrow-back-circle" size={40} color="#4A90E2" />
                    </TouchableOpacity>
                    <Image
                        source={avatarUrl ? { uri: avatarUrl } : DEFAULT_AVATAR}
                        style={styles.avatar}
                    />
                    <Text style={styles.titleText} numberOfLines={1}>
                        {title}
                    </Text>
                </View>

                <TouchableOpacity style={styles.optionsButton} activeOpacity={0.7}>
                    <MaterialCommunityIcons name="dots-horizontal-circle" size={40} color="#4A90E2" />
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: {
        backgroundColor: '#FFFFFF',
        paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 0,
    },
    headerContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 15,
        paddingVertical: 10,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    leftSection: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    backButton: {
        marginRight: 10,
    },
    avatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        backgroundColor: '#f0f0f0',
    },
    titleText: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#000',
        marginLeft: 15,
    },
    optionsButton: {
        marginLeft: 10,
    },
});
