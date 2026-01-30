import { View } from "react-native";
import MenuDot from "./component/MenuDot";
import Onboarding4 from "./component/Onboarding4";

export default function OnboardingScreen() {
    return (
        <View style={{flex: 1}}>
            <Onboarding4 />
            <MenuDot />
        </View>
    );
}