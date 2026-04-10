import { useEffect, useRef } from "react";
import { Animated, StyleSheet, View } from "react-native";

export default function TypingDots() {
  const dot1 = useRef(new Animated.Value(0.3)).current;
  const dot2 = useRef(new Animated.Value(0.3)).current;
  const dot3 = useRef(new Animated.Value(0.3)).current;

  const createFade = (anim: Animated.Value, delay: number) => {
    return Animated.loop(
      Animated.sequence([
        Animated.delay(delay),
        Animated.timing(anim, {
          toValue: 1,
          duration: 400,
          useNativeDriver: true,
        }),
        Animated.timing(anim, {
          toValue: 0.3,
          duration: 400,
          useNativeDriver: true,
        }),
      ])
    );
  };

  useEffect(() => {
    createFade(dot1, 0).start();
    createFade(dot2, 150).start();
    createFade(dot3, 300).start();
  }, []);

  const style = (anim: Animated.Value) => ({
    opacity: anim,
  });

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.dot, style(dot1)]} />
      <Animated.View style={[styles.dot, style(dot2)]} />
      <Animated.View style={[styles.dot, style(dot3)]} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    gap: 4,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: "#4DA6FF",
  },
});