import React, { useRef, useState } from "react";
import { View, FlatList, useWindowDimensions, StyleSheet } from "react-native";
import Header from "./component/Header";
import Footer from "./component/Footer";
import AllChat from "./component/AllChat";
import IndividualChat from "./component/IndividualChat";
import GroupChat from "./component/GroupChat";
import EmergencyChat from "./component/EmergencyChat";

export default function ChatBox() {
    const { width } = useWindowDimensions();
    const flatListRef = useRef<FlatList>(null);
    const [activeIndex, setActiveIndex] = useState(0);

    const screens = [
        { id: 'all', component: <AllChat /> },
        { id: 'individual', component: <IndividualChat /> },
        { id: 'group', component: <GroupChat /> },
        { id: 'emergency', component: <EmergencyChat /> },
    ];

    const handleTabPress = (index: number) => {
        setActiveIndex(index);
        flatListRef.current?.scrollToIndex({ index, animated: true });
    };

    const onViewableItemsChanged = useRef(({ viewableItems }: any) => {
        if (viewableItems.length > 0) {
            setActiveIndex(viewableItems[0].index);
        }
    }).current;

    const viewabilityConfig = useRef({
        itemVisiblePercentThreshold: 50
    }).current;

    return (
        <View style={styles.container}>
            <Header activeIndex={activeIndex} onTabPress={handleTabPress} />

            <FlatList
                ref={flatListRef}
                data={screens}
                renderItem={({ item }) => (
                    <View style={{ width: width, flex: 1 }}>
                        {item.component}
                    </View>
                )}
                horizontal
                pagingEnabled
                showsHorizontalScrollIndicator={false}
                onViewableItemsChanged={onViewableItemsChanged}
                viewabilityConfig={viewabilityConfig}
                keyExtractor={(item) => item.id}
            />

            <Footer />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff'
    }
});
