import React from "react";
import { StyleSheet, View } from "react-native";
import OnboardingScreen from "./Onboarding";

export default function Index() {
  return (
    <View style={ styles.container }>
      <OnboardingScreen />

    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    width: '100%',
    height: '100%',
    backgroundColor: '#E6F9FF'
  }
});
