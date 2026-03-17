import { QrCode, Tablet, UserRoundPlus } from "lucide-react-native";
import { StyleSheet, Text, useWindowDimensions, View } from "react-native";

export default function ScanPopUp() {
    const { width } = useWindowDimensions();
    return (
        <View style={[styles.addUserContainer, {width: width * 0.4}]}>
            <View style={styles.addUser}>
                <UserRoundPlus
                    size={20}
                    color="#5F2EEA"
                />
                <Text style={styles.addUserText}>Add users</Text>
            </View>

            <View style={styles.addUserActionContainer}>
                <View style={styles.addUserAction}>
                    <Tablet
                        size={20}
                        color="#5F2EEA"
                    />
                    <Text style={styles.addUserText}>Devices</Text>
                </View>

                <View style={styles.addUserAction}>
                    <QrCode
                        size={20}
                        color="#5F2EEA"
                    />
                    <Text style={styles.addUserText}>QR</Text>
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    addUserContainer: {
        backgroundColor: 'white',
        paddingVertical: 10,
        borderRadius: 20,
        position: 'absolute',
        right: 15,
        top: 90,
        borderWidth: 0.5,
        borderColor: 'rgba(95, 46, 234, 0.2)',
        elevation: 6
    },

    addUser: {
        flexDirection: 'row',
        gap: 5,
        justifyContent: 'center',
        alignItems: 'center',
        paddingBottom: 3,
        borderBottomWidth: 0.25,
        borderColor: '#3730A3'
    },

    addUserText: {
        fontSize: 12,
        fontWeight: 'bold',
        color: 'rgba(55, 48, 163, 0.9)'
    },
    
    addUserActionContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingTop: 15,
        paddingHorizontal: 20,
        gap: 20,
        alignItems: 'center'
    },

    addUserAction: {
        gap: 3,
        alignItems: 'center'
    }
});