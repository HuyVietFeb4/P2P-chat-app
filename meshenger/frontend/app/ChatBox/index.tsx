import React, { useRef, useState } from "react";
import { FlatList, Pressable, StyleSheet, useWindowDimensions, View } from "react-native";
import Footer from "../common components/Footer";
import Header from "../common components/Header";
import AllChat from "./component/AllChat";
import EmergencyChat from "./component/EmergencyChat";
import GroupChat from "./component/GroupChat";
import IndividualChat from "./component/IndividualChat";
import ScanPopUp from "./component/ScanPopUp";

const { BleModule } = NativeModules;
export default function ChatBox() {
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);
    const { width, height } = useWindowDimensions();
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
        flatListRef.current?.scrollToIndex({ index, animated: false });
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
            <Header activeIndex={activeIndex} onTabPress={handleTabPress} openPopUp={openPopUp} setOpenPopUp={() => setOpenPopUp(true)} />

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

             {
                openPopUp ? (
                    <>
                        <Pressable style={StyleSheet.absoluteFill} onPress={() => setOpenPopUp(false)}>
                        </Pressable>
                        <ScanPopUp setOnClose={() => setOpenPopUp(false)} />
                    </>
                ) : null
            }
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff'
    }
});
