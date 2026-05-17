import Message from '@/app/common components/Message';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, useWindowDimensions, View, Modal, FlatList, Image, NativeModules } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
// 1. CHANGE: Import expo-device
import * as Device from 'expo-device';
import { AVATAR_LIST, getAvatarSource } from '../../../assets/avatarMap';

const { MeshengerApplicationModule } = NativeModules;



export default function Onboarding5() {
    const { width, height } = useWindowDimensions();
    const [deviceName, setDeviceName] = useState('Loading...');
    const [isModalVisible, setModalVisible] = useState(false);
    const [selectedAvtId, setSelectedAvtId] = useState<string>('avt0');
    const [error, setError] = useState<string | null>(null);
    const router = useRouter();
    const { t } = useTranslation();

    // 2. CHANGE: Update useEffect logic
    useEffect(() => {
        // Device.modelName is synchronous in Expo
        // We use a fallback logic: modelName -> designName -> generic string
        const name = Device.modelName || Device.designName || 'Galaxy S Series';
        setDeviceName(name);
    }, []);

    const handleNavigateToChat = async () => {
        let validateError = null;
        if (!deviceName.trim()) {
            validateError = t('device-name-cannot-be-empty');
        }

        if (deviceName.length > 15) {
            validateError = t('device-15-char');
        }

        if (validateError) {
            setError(validateError);
            setTimeout(() => setError(null), 3000);
            return;
        }

        if (error) {
            setTimeout(() => setError(null), 3000);
            return;
        } else {
            try {
                await MeshengerApplicationModule.updateMyProfile(deviceName.trim(), selectedAvtId);
            } catch (e) {
                console.error("Failed to update profile", e);
            }
            await AsyncStorage.setItem("firstLaunch", deviceName.trim());
            router.replace("/ChatBox");
        }
    };

    return (
        <View style={styles.container}>
            {/* Background Gradient Ellipse */}
            <LinearGradient
                colors={['#278EFF', '#278EFF']}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.backgroundEllipse}
            />

            <SafeAreaView style={styles.safeArea}>
                <KeyboardAvoidingView
                    behavior={Platform.OS === "ios" ? "padding" : "height"}
                    style={styles.keyboardView}
                >
                    <ScrollView
                        contentContainerStyle={styles.scrollContent}
                        showsVerticalScrollIndicator={false}
                        bounces={false}
                    >
                        {/* Header Section */}
                        <View style={styles.headerSection}>
                            <Text style={styles.title}>{t('spice-up-your')}</Text>

                            <View style={styles.iconContainer}>
                                {selectedAvtId ? (
                                    <Image
                                        source={getAvatarSource(selectedAvtId)}
                                        style={{ width: 100, height: 100, borderRadius: 50 }}
                                    />
                                ) : (
                                    <MaterialCommunityIcons name="bluetooth" size={60} color="#FFFFFF" />
                                )}
                            </View>

                            <LinearGradient
                                colors={['#0F2027', '#203A43', '#2C5364']}
                                start={{ x: 0, y: 0 }}
                                end={{ x: 1, y: 0 }}
                                style={styles.actionButton}
                            >
                                <TouchableOpacity style={styles.buttonContent} activeOpacity={0.8} onPress={() => setModalVisible(true)}>
                                    <MaterialCommunityIcons name="plus" size={20} color="#FFFFFF" />
                                    <Text style={styles.buttonText}>{t('select-avatar')}</Text>
                                </TouchableOpacity>
                            </LinearGradient>
                        </View>

                        {/* Input Section */}
                        <View style={styles.formSection}>
                            <Text style={styles.label}>{t('please-enter-your-device-name')}</Text>

                            <View style={styles.inputContainer}>
                                <TextInput
                                    style={styles.input}
                                    placeholder={t('device-name')}
                                    placeholderTextColor="#999999"
                                    value={deviceName}
                                    onChangeText={setDeviceName}
                                />
                            </View>

                            <LinearGradient
                                colors={['#0F2027', '#203A43', '#2C5364']}
                                start={{ x: 0, y: 0 }}
                                end={{ x: 1, y: 0 }}
                                style={[styles.actionButton, styles.chatButton]}
                            >
                                <TouchableOpacity
                                    style={styles.buttonContent}
                                    activeOpacity={0.8}
                                    onPress={handleNavigateToChat}
                                >
                                    <Text style={[styles.buttonText, { fontSize: 18 }]}>{t('go-to-chat')}</Text>
                                    <MaterialCommunityIcons name="arrow-right" size={22} color="#FFFFFF" />
                                </TouchableOpacity>
                            </LinearGradient>
                        </View>

                        {/* Footer Section */}
                        <View style={styles.footerSection}>
                            <Text style={styles.quote}>
                                {t('a-good-name-is')}
                            </Text>
                        </View>
                    </ScrollView>
                </KeyboardAvoidingView>
            </SafeAreaView>
            <Message visible={!!error} message={error} title="Username error!" />

            {/* Avatar Selection Modal */}
            <Modal
                visible={isModalVisible}
                transparent={true}
                animationType="slide"
                onRequestClose={() => setModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>{t('choose-a-avatar')}</Text>
                        <FlatList
                            data={AVATAR_LIST.filter(a => a.id !== 'avt0')}
                            numColumns={3}
                            keyExtractor={(item) => item.id}
                            showsVerticalScrollIndicator={false}
                            contentContainerStyle={{ paddingVertical: 10, alignItems: 'center' }}
                            columnWrapperStyle={{ gap: 20, justifyContent: 'center', marginBottom: 20 }}
                            renderItem={({ item }) => (
                                <TouchableOpacity
                                    style={[
                                        styles.avatarOption,
                                        selectedAvtId === item.id && styles.avatarOptionSelected
                                    ]}
                                    onPress={() => {
                                        setSelectedAvtId(item.id);
                                        setModalVisible(false);
                                    }}
                                >
                                    <Image source={item.source} style={styles.avatarImage} />
                                </TouchableOpacity>
                            )}
                        />
                        <TouchableOpacity
                            style={styles.closeModalButton}
                            onPress={() => setModalVisible(false)}
                        >
                            <Text style={styles.closeModalButtonText}>{t('cancel')}</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#E6F9FF',
    },
    // Background Shape
    backgroundEllipse: {
        position: 'absolute',
        width: 873,
        height: 873,
        // Centered horizontally based on the center of the shape
        left: '50%',
        // Positioned vertically relative to screen top
        top: -250,
        marginLeft: -436.5, // Shift left by half width to center
        borderRadius: 873 / 2,
        zIndex: 0,
    },
    safeArea: {
        flex: 1,
        zIndex: 1,
    },
    keyboardView: {
        flex: 1,
    },
    scrollContent: {
        flexGrow: 1,
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingBottom: 30,
    },

    // --- Header Section ---
    headerSection: {
        alignItems: 'center',
        marginTop: 40,
        width: '100%',
        gap: 25,
    },
    title: {
        fontSize: 30,
        fontWeight: '700',
        color: '#FFFFFF',
        textAlign: 'center',
        lineHeight: 36,
    },
    iconContainer: {
        width: 100,
        height: 100,
        justifyContent: 'center',
        alignItems: 'center',
        borderRadius: 50,
        backgroundColor: 'rgba(255, 255, 255, 0.15)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.3)',
    },

    // --- Form Section ---
    formSection: {
        width: '100%',
        alignItems: 'center',
        marginTop: 50,
        gap: 15,
    },
    label: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
        textAlign: 'center',
    },
    inputContainer: {
        width: '100%',
        maxWidth: 350,
        height: 50,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        justifyContent: 'center',
        paddingHorizontal: 15,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    input: {
        fontSize: 16,
        color: '#000',
        height: '100%',
    },

    // --- Buttons ---
    actionButton: {
        borderRadius: 12,
        overflow: 'hidden',
        minWidth: 140,
        elevation: 4,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.2,
        shadowRadius: 3,
    },
    chatButton: {
        marginTop: 20,
        width: '100%',
        maxWidth: 350,
        height: 50,
    },
    buttonContent: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 12,
        paddingHorizontal: 20,
        gap: 8,
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 14,
        fontWeight: '600',
    },

    // --- Footer ---
    footerSection: {
        marginTop: 'auto',
        paddingTop: 40,
        width: '90%',
    },
    quote: {
        fontSize: 14,
        fontWeight: '500',
        color: '#666666',
        textAlign: 'center',
        fontStyle: 'italic',
        lineHeight: 20,
    },
    // --- Modal Styles ---
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    modalContent: {
        width: '85%',
        backgroundColor: '#FFF',
        borderRadius: 24,
        padding: 24,
        alignItems: 'center',
        maxHeight: '75%',
    },
    modalTitle: {
        fontSize: 22,
        fontWeight: 'bold',
        marginBottom: 15,
        color: '#333',
    },
    avatarOption: {
        padding: 4,
        borderRadius: 50,
        borderWidth: 3,
        borderColor: 'transparent',
    },
    avatarOptionSelected: {
        borderColor: '#278EFF',
    },
    avatarImage: {
        width: 65,
        height: 65,
        borderRadius: 32.5,
    },
    closeModalButton: {
        marginTop: 20,
        paddingVertical: 10,
        paddingHorizontal: 30,
        backgroundColor: '#EAEAEA',
        borderRadius: 20,
    },
    closeModalButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#555',
    },
});