import * as ImagePicker from "expo-image-picker"
import { useState } from "react"
import { StyleSheet, View } from "react-native"
import Footer from "./component/Footer"
import Header from "./component/Header"
import MyQR from "./component/MyQR"
import QRCamera from "./component/QRCamera"

export default function QRScan() {
    const [activeTab, setActiveTab] = useState<"my-qr" | "album" | "scan-qr">("scan-qr");
    const [image, setImage] = useState<string | null>(null);
    const [permission, requestPermission] = ImagePicker.useMediaLibraryPermissions();

    
    
    const pickImage = async () => {
        if (!permission?.granted) {
            await requestPermission();
            return;
        }

        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: false,
            aspect: [4, 3],
            quality: 1
        });

        console.log(result);

        if (!result.canceled) {
            setImage(result.assets[0].uri);
        }
    }

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

            <Footer setActiveTab={setActiveTab} openAlbum={pickImage}/>
        </View>
    )
}

const styles = StyleSheet.create({
    albumList: {
        position: 'absolute',
    }
});
