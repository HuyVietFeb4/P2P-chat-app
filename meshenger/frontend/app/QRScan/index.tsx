import { View } from "react-native"
import Header from "./component/Header"
import QRCamera from "./component/QRCamera"

export default function QRScan() {
    return (
        <View style={{flex: 1}}>
            <Header />
            <QRCamera />
        </View>
    )
}