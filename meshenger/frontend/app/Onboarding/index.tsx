import React, { ComponentType, useRef, useState } from "react";
import { FlatList, View, ViewToken, useWindowDimensions } from "react-native";
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
    {
        id: 1,
        component: Onboarding1
    },

    {
        id: 2,
        component: Onboarding2,
    },

    {
        id: 3,
        component: Onboarding3
    },

    {
        id: 4,
        component: Onboarding4
    }
]

export default function OnboardingScreen() {
    const [currentIndex, setCurrentIndex] = useState<number>(0); 
    const flatListRef = useRef<FlatList<OnboardingItem> | null>(null);
    const { width, height } = useWindowDimensions();

    const onViewableItemsChanged = useRef(({ viewableItems }: { viewableItems: ViewToken[] }) => {
        if (viewableItems.length > 0 && viewableItems[0].index != null) {
            setCurrentIndex(viewableItems[0].index as number);
        }
    }).current;

    const viewConfig = useRef({ viewAreaCoveragePercentThreshold: 50 }).current;

    const renderItem = ({ item }: { item: OnboardingItem }) => {
        const Component = item.component;
        return (
            <View style={{ width: width, height: '100%' }}>
                <Component />
            </View>
        );
    }
    
    return (
        <View>
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
            />
            <MenuDot totalDots={SCREENS.length} currentIndex={currentIndex} />
        </View>
    );
}