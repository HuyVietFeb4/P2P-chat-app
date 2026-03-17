import React, { useEffect } from "react"; // 1. Add useEffect
import { View } from "react-native";
import Header from "./component/Header";
import { NativeModules } from 'react-native';

const { BleModule } = NativeModules;
export default function ChatBox() {
    useEffect(() => {
        // 2. This runs immediately when the screen opens
        if (BleModule) {
            console.log("Component loaded, calling native scan...");
            BleModule.onDemandScan(10000);
        }
    }, []);
    return (
        <View style={{flex: 1}}>
            <Header />
        </View>
    );
}