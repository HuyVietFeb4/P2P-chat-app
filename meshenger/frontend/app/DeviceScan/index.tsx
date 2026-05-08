import { StyleSheet, View } from "react-native";

import DeviceList from "./component/DeviceList";
import Header from "./component/Header";
import Scanning from "./component/Scanning";
import { useBluetooth } from "../hook/useBluetooth";
import BluetoothPopup from "../common components/BluetoothPopUp";
import { Colors, useTheme } from "../context/ThemeContext";


export default function DeviceScan() {
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();
    const { colors } = useTheme();
    return (
        <View style={[styles.container, {backgroundColor: colors.scannedBg}]}>
            <Header />
            <Scanning />
            <DeviceList />

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
    container: {
        flex: 1
    }
});