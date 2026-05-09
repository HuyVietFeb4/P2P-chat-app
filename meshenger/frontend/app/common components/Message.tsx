import { useEffect, useRef, useState } from "react";
import { Animated, Easing, StyleSheet, Text, View } from "react-native";
import { CircleX } from "lucide-react-native";

interface MessageProps {
    visible?: boolean;
    message?: string | null;
    title?: string | null;
    onHide?: () => void;
    duration?: number;
}

export default function Message({
    visible = true,
    message,
    title,
    onHide,
    duration = 3000,
}: MessageProps) {
    const translateY = useRef(new Animated.Value(30)).current;
    const opacity = useRef(new Animated.Value(0)).current;
    const scale = useRef(new Animated.Value(0.85)).current;
    const [shouldRender, setShouldRender] = useState(visible);

    const animateIn = () => {
        setShouldRender(true);
        Animated.parallel([
            Animated.spring(translateY, {
                toValue: 0,
                useNativeDriver: true,
                tension: 80,
                friction: 10,
            }),
            Animated.timing(opacity, {
                toValue: 1,
                duration: 250,
                easing: Easing.out(Easing.cubic),
                useNativeDriver: true,
            }),
            Animated.spring(scale, {
                toValue: 1,
                useNativeDriver: true,
                tension: 100,
                friction: 10,
            }),
        ]).start();
    };

    const animateOut = () => {
        Animated.parallel([
            Animated.timing(translateY, {
                toValue: 20,
                duration: 300,
                easing: Easing.in(Easing.cubic),
                useNativeDriver: true,
            }),
            Animated.timing(opacity, {
                toValue: 0,
                duration: 300,
                easing: Easing.in(Easing.cubic),
                useNativeDriver: true,
            }),
            Animated.timing(scale, {
                toValue: 0.9,
                duration: 300,
                easing: Easing.in(Easing.cubic),
                useNativeDriver: true,
            }),
        ]).start(() => {
            setShouldRender(false);
            // Reset lại giá trị để lần sau animate in được bình thường
            translateY.setValue(30);
            opacity.setValue(0);
            scale.setValue(0.85);
            onHide?.();
        });
    };

    useEffect(() => {
        if (visible) {
            animateIn();

            const timer = setTimeout(() => {
                animateOut();
            }, duration);

            return () => clearTimeout(timer);
        } else {
            animateOut();
        }
    }, [visible]);

    if (!shouldRender) return null;

    return (
        <Animated.View
            style={[
                styles.container,
                { opacity, transform: [{ translateY }, { scale }] },
            ]}
        >
            <View style={styles.iconBackground}>
                <CircleX size={16} color="#FF3B30" strokeWidth={2.5} />
            </View>

            <View style={styles.textWrapper}>
                <Text style={styles.title}>{title}</Text>
                <Text style={styles.subtitle}>{message}</Text>
            </View>

            <View style={styles.dot} />
        </Animated.View>
    );
}

const styles = StyleSheet.create({
    container: {
        position: "absolute",
        bottom: 50,
        alignSelf: "center",
        flexDirection: "row",
        alignItems: "center",
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 18,
        maxWidth: "88%",
        flexShrink: 1,
        // position: "absolute",
        // bottom: 50,
        // alignSelf: "center",
        // paddingHorizontal: 16,
        // paddingVertical: 12,
        // borderRadius: 18,
        // flexDirection: "row",
        // alignItems: "center",
        gap: 12,
        zIndex: 10,
        backgroundColor: "rgba(255, 235, 235, 0.95)",
        borderWidth: 1,
        borderColor: "rgba(255, 107, 107, 0.3)",
        borderTopColor: "rgba(255, 255, 255, 0.8)",
        borderLeftColor: "rgba(255, 255, 255, 0.6)",
        shadowColor: "#FF3B30",
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.2,
        shadowRadius: 20,
        elevation: 10,
    },
    iconBackground: {
        width: 34,
        height: 34,
        borderRadius: 10,
        backgroundColor: "rgba(255, 59, 48, 0.12)",
        justifyContent: "center",
        alignItems: "center",
        borderWidth: 1,
        borderColor: "rgba(255, 59, 48, 0.2)",
    },
    textWrapper: {
        flexDirection: "column",
        gap: 1,
    },
    title: {
        fontSize: 13,
        color: "#CC2200",
        fontWeight: "700",
        letterSpacing: 0.2,
    },
    subtitle: {
        fontSize: 11,
        color: "#FF6B6B",
        fontWeight: "500",
        letterSpacing: 0.1,
    },
    dot: {
        width: 6,
        height: 6,
        borderRadius: 3,
        backgroundColor: "#FF3B30",
        opacity: 0.4,
        marginLeft: 4,
    },
});