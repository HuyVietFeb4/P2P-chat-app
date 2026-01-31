import React from "react";
import { StyleSheet, View } from "react-native";

export default function MenuDot({ totalDots, currentIndex }: { totalDots?: number; currentIndex?: number }) {
    return (
        <View style = { styles.container}>
            {Array.from({ length: totalDots || 0 }).map((_, index) => (
                <View 
                    key={index} 
                    style = {[ styles.dot, index === currentIndex ? styles.activeDot : null ]} 
                />
            ))}
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
        backgroundColor: "#FFFFFF",
        marginHorizontal: 4,
        borderWidth: 1,
        borderColor: "#D9D9D9"
    },

    activeDot: {
        backgroundColor: "#00E5FF",
    }
});