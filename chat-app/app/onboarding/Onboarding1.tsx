import { Image } from "expo-image";
import { Dimensions, Text, View } from 'react-native';

const { height, width } = Dimensions.get('window');

export default function Onboarding1() {
    return (
        <View>
            <Image
                source={require('../../assets/images/onboarding1.svg')}
                style={{
                    width: width, 
                    height: height * 0.6
                }}
            />

            <Text style={{
                fontSize: 20, 
                fontWeight: 'bold', 
                textAlign: 'center', 
                paddingVertical: 15
                }}>
                Chat without Wifi or Internet
            </Text>

            <Text style={{
                fontSize: 14,
                textAlign: 'center',
                width: width * 0.8,
                alignSelf: 'center'
            }}>
                Stay connected anywhere through Bluetooth Mesh - even when you're offline.
            </Text>
        </View>
    );
}