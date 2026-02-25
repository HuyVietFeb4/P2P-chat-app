import { View } from "react-native";
import Header from "./component/Header";
import Footer from "./component/Footer";

export default function ChatBox() {
    return (
        <View style={{flex: 1}}>
            <Header />
            <View style={{flex: 1}} />
            <Footer />
        </View>
    );
}