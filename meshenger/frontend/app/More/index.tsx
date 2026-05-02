import { useState } from "react";
import { Pressable, StyleSheet, View } from 'react-native';
import ScanPopUp from "../ChatBox/component/ScanPopUp";
import Footer from '../common components/Footer';
import Header from '../common components/Header';
import { useTheme } from "../context/ThemeContext";
import SettingList from "./component/SettingList";

export default function MoreOptions() {
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);
    const { colors } = useTheme();

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
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