import React, { useState } from 'react';
import {View, Text, StyleSheet, TextInput, TouchableOpacity, useWindowDimensions, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context'; // Recommended for professional apps, or use standard View with paddingTop

export default function Onboarding5() {
    const { width, height } = useWindowDimensions();
    const [deviceName, setDeviceName] = useState('Galaxy S Series');

    return (
        <View style={styles.container}>
            {/* Background Gradient Ellipse 
              (We keep absolute here because it is a background decoration separate from the layout flow)
            */}
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
                            <Text style={styles.title}>Spice Up Your Avatar{'\n'}and Name!</Text>
                            
                            <View style={styles.iconContainer}>
                                <MaterialCommunityIcons name="bluetooth" size={60} color="#FFFFFF" />
                            </View>

                            <LinearGradient
                                colors={['#0F2027', '#203A43', '#2C5364']}
                                start={{ x: 0, y: 0 }}
                                end={{ x: 1, y: 0 }}
                                style={styles.actionButton}
                            >
                                <TouchableOpacity style={styles.buttonContent} activeOpacity={0.8}>
                                    <MaterialCommunityIcons name="plus" size={20} color="#FFFFFF" />
                                    <Text style={styles.buttonText}>Add image</Text>
                                </TouchableOpacity>
                            </LinearGradient>
                        </View>

                        {/* Input Section */}
                        <View style={styles.formSection}>
                            <Text style={styles.label}>Please enter your device name</Text>
                            
                            <View style={styles.inputContainer}>
                                <TextInput
                                    style={styles.input}
                                    placeholder="Device Name"
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
                                    onPress={() => console.log('Navigate to Chat')}
                                >
                                    <Text style={[styles.buttonText, { fontSize: 18 }]}>Go to chat!</Text>
                                    <MaterialCommunityIcons name="arrow-right" size={22} color="#FFFFFF" />
                                </TouchableOpacity>
                            </LinearGradient>
                        </View>

                        {/* Footer Section */}
                        <View style={styles.footerSection}>
                            <Text style={styles.quote}>
                                "A good name is rather to be chosen than great riches." Proverbs 22:1
                            </Text>
                        </View>
                    </ScrollView>
                </KeyboardAvoidingView>
            </SafeAreaView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#E6F9FF',
    },
    // Background Shape - Kept Absolute as it is decorative
    backgroundEllipse: {
        position: 'absolute',
        width: 873,
        height: 873,
        left: '50%',
        top: '25%',
        marginLeft: -436.5, // Half of width
        marginTop: -436.5,  // Half of height
        borderRadius: 873 / 2, // Mathematically correct circle
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
        paddingBottom: 30, // Bottom padding for scroll
    },
    
    // --- Header Section ---
    headerSection: {
        alignItems: 'center',
        marginTop: 40,
        width: '100%',
        gap: 25, // Uniform spacing between elements
    },
    title: {
        fontSize: 30,
        fontWeight: '700',
        color: '#FFFFFF',
        textAlign: 'center',
        lineHeight: 36,
        // fontFamily: 'Afacad', 
    },
    iconContainer: {
        width: 100,
        height: 100,
        justifyContent: 'center',
        alignItems: 'center',
        borderRadius: 50,
        backgroundColor: 'rgba(255, 255, 255, 0.15)', // Subtle background for the icon
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
        // fontFamily: 'Afacad',
    },
    inputContainer: {
        width: '100%',
        maxWidth: 350, // Prevents input from getting too wide on tablets
        height: 50,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        justifyContent: 'center',
        paddingHorizontal: 15,
        // Professional Shadow
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
        // fontFamily: 'Roboto',
    },

    // --- Buttons ---
    actionButton: {
        borderRadius: 12, // Modern border radius
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
        maxWidth: 350, // Matches input width
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
        // fontFamily: 'Poppins',
    },

    // --- Footer ---
    footerSection: {
        marginTop: 'auto', // Pushes footer to the bottom of the scroll view
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
        // fontFamily: 'Roboto',
    },
});