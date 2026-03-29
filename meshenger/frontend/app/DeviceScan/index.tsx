import { StyleSheet, View } from "react-native";

import DeviceList from "./component/DeviceList";
import Header from "./component/Header";
import Scanning from "./component/Scanning";

export default function DeviceScan() {
    return (
        <View style={styles.container}>
            <Header />
            <Scanning />
            <DeviceList />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    }
});