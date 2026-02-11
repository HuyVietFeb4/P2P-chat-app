import React, { ComponentType, useRef, useState } from "react";
import { FlatList, View, ViewToken, useWindowDimensions, StyleSheet, TouchableOpacity, Text } from "react-native";
import { useRouter } from "expo-router";
import MenuDot from "./component/MenuDot";
import Onboarding1 from "./component/Onboarding1";
import Onboarding2 from "./component/Onboarding2";
import Onboarding3 from "./component/Onboarding3";
import Onboarding4 from "./component/Onboarding4";

type OnboardingItem = {
    id: number;
    component: ComponentType<any>;
};

const SCREENS: OnboardingItem[] = [
    { id: 1, component: Onboarding1 },
    { id: 2, component: Onboarding2 },
    { id: 3, component: Onboarding3 },
    { id: 4, component: Onboarding4 }
];

export default function OnboardingScreen() {
    const [currentIndex, setCurrentIndex] = useState<number>(0);
    const flatListRef = useRef<FlatList<OnboardingItem> | null>(null);
    const { width } = useWindowDimensions();
    const router = useRouter();

    const onViewableItemsChanged = useRef(({ viewableItems }: { viewableItems: ViewToken[] }) => {
        if (viewableItems.length > 0 && viewableItems[0].index != null) {
            setCurrentIndex(viewableItems[0].index as number);
        }
    }).current;

    const viewConfig = useRef({ viewAreaCoveragePercentThreshold: 50 }).current;

    const renderItem = ({ item }: { item: OnboardingItem }) => {
        const Component = item.component;
        return (
            <View style={{ width: width }}>
                <Component />
            </View>
        );
    }

    return (
        <View style={styles.container}>
        <View style={styles.contentWrapper}>
            <FlatList
                ref={flatListRef}
                data={SCREENS}
                renderItem={renderItem}
                horizontal
                pagingEnabled
                showsHorizontalScrollIndicator={false}
                onViewableItemsChanged={onViewableItemsChanged}
                viewabilityConfig={viewConfig}
                keyExtractor={(item) => item.id.toString()}
                bounces={false}
            />               
            <MenuDot totalDots={SCREENS.length} currentIndex={currentIndex} />
        </View>

        <View style={styles.buttonFooter}>
            {currentIndex === SCREENS.length - 1 && (
                <TouchableOpacity 
                    style={styles.button}
                    activeOpacity={0.8}
                    onPress={() => router.push('/Onboarding/component/Onboarding5')}
                >
                    <Text style={styles.buttonText}>Get Started!</Text>
                </TouchableOpacity>
            )}
        </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#E6F9FF',
    },
    // Main wrapper for List and Dots
    contentWrapper: {
        flex: 1, // Takes up all available space except for the footer height
        justifyContent: 'flex-start',
    },
    // Dedicated Footer for the Button
    buttonFooter: {
        height: 70, // Fixed height for the button area
        paddingHorizontal: 25,
        justifyContent: 'center', // Vertically center the button in the footer
        alignItems: 'flex-end',   // Push button to the Right
        marginBottom: 50,         // Safe margin from bottom of screen
    },
    button: {
        width: 151,
        height: 45,
        backgroundColor: '#00E0FF',
        borderRadius: 15,
        justifyContent: 'center',
        alignItems: 'center',
        // Shadow props
        shadowColor: "#000",
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 3.84,
        elevation: 5,
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '700',
        textAlign: 'center',
    }
});