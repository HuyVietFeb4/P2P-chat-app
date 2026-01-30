import React from "react";
import { StyleSheet, View } from "react-native";

export default function MenuDot() {
    return (
        <View style = { styles.container}>
            <View style = { styles.dot }></View>
            <View style = { styles.dot }></View>
            <View style = { styles.dot }></View>
            <View style = { styles.dot }></View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: "row",
        justifyContent: "center",
        alignItems: "center",
        paddingVertical: 20
    },
    dot: {
        width: 10,
        height: 10,
        borderRadius: 5,
        backgroundColor: "#D9D9D9",
        marginHorizontal: 4
    }
});