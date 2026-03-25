import { LinearGradient } from "expo-linear-gradient";
import { Bluetooth } from "lucide-react-native";
import { useEffect, useRef } from "react";
import { Animated, StyleSheet, View, useWindowDimensions } from "react-native";

export default function Scanning() {
  const anim1 = useRef(new Animated.Value(0)).current;
  const anim2 = useRef(new Animated.Value(0)).current;
  const { height } = useWindowDimensions();

  useEffect(() => {
    const createPulse = (anim: Animated.Value, delay: number) => {
      return Animated.loop(
        Animated.sequence([
          Animated.delay(delay),
          Animated.timing(anim, {
            toValue: 1,
            duration: 2000,
            useNativeDriver: true,
          }),
          Animated.timing(anim, {
            toValue: 0,
            duration: 0,
            useNativeDriver: true,
          }),
        ])
      );
    };

    createPulse(anim1, 0).start();
    createPulse(anim2, 1000).start(); 
  }, []);

  const getStyle = (anim: Animated.Value) => ({
    transform: [
      {
        scale: anim.interpolate({
          inputRange: [0, 1],
          outputRange: [0.5, 1.5],
        }),
      },
    ],
    opacity: anim.interpolate({
      inputRange: [0, 1],
      outputRange: [0.6, 0],
    }),
  });

  return (
    <View style={[styles.container, {height: height * 0.2}]}>
      <Animated.View style={[styles.circle, getStyle(anim1)]} />
      <Animated.View style={[styles.circle, getStyle(anim2)]} />

      <LinearGradient 
        style={styles.iconContainer}
        colors={['#6366F1', '#06B6D4']}
        start={{x: 0, y: 0}}
        end={{x: 1, y: 0}}
      >
        <Bluetooth size={40} color="white" />
      </LinearGradient>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    justifyContent: "center",
    alignItems: "center",
  },

  circle: {
    position: "absolute",
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 2,
    borderColor: "#4DA6FF",
  },

  iconContainer: {
    width: 80,
    height: 80,
    borderRadius: 999,
    justifyContent: "center",
    alignItems: "center",
  },
});