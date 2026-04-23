import { useState } from "react";
import { Pressable, StyleSheet, View } from 'react-native';
import ScanPopUp from "../ChatBox/component/ScanPopUp";
import Footer from '../common components/Footer';
import Header from '../common components/Header';
import SettingList from "./component/SettingList";

export default function MoreOptions() {
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);
    return (
        <View style={styles.container}>
            <Header openPopUp={openPopUp} setOpenPopUp={() => setOpenPopUp(true)}  />
            <SettingList />
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
        backgroundColor: "white"
    }
});