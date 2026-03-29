import { useState } from "react"
import { View } from "react-native"
import Footer from "./component/Footer"
import Header from "./component/Header"
import MyQR from "./component/MyQR"
import QRCamera from "./component/QRCamera"

export default function QRScan() {
    const [activeTab, setActiveTab] = useState<"my-qr" | "album" | "scan-qr">("scan-qr");
    return (
        <View style={{flex: 1}}>
            {
                activeTab === "scan-qr" && <Header title="Devices Scanning" instruction="Scan QR to add a new device" />
            }

            {
                activeTab === "my-qr" && <Header title="My QR Code" instruction="Show your QR code to others" />
            }
            
            {
                activeTab === "scan-qr" && <QRCamera />
            }

            {
                activeTab === "my-qr" && <MyQR />
            }

            <Footer activeTab={activeTab} setActiveTab={setActiveTab} />
        </View>
    )
}