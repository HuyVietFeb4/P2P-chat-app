import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { LayoutGrid, Search, ShieldAlert, UserPlus, UserRound, Users } from "lucide-react-native";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View, useWindowDimensions } from "react-native";
import ScanPopUp from "./ScanPopUp";


export default function Header() {
    const [openPopUp, setOpenPopUp] = useState<boolean>(false);
    const {width, height} = useWindowDimensions();

    return (
        <>
            <LinearGradient
                colors={['#0F4C81', '#5F2EEA']}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
                style={{width: width, height: height * 0.2}}
            >
                <View style={[styles.headerContainer, {width: width * 0.9}]}>
                    <View style={styles.actionContainer}> 
                        <View style={styles.appTitleContainer}>
                            <Image
                                source={require('@/assets/images/appIcon.png')}
                                style={styles.imageStyle}
                                contentFit="cover"
                            />
                            <Text style={styles.headerText}>Meshenger</Text>
                        </View>
                        <View style={styles.actionIconsContainer}>
                            <Search
                                size={22}
                                color="#fff"
                            />
                            <UserPlus
                                size={22}
                                color='#fff'
                                onPress={() => setOpenPopUp(true)}
                            />
                        </View>
                    </View>
                    <View style={styles.filterContainer}>
                        <View style={styles.filterFieldContainer}>
                            <LayoutGrid
                                size={12}
                                color='#5F2EEA'
                            />
                            <Text style={styles.filterText}>All</Text>
                        </View>
                        <View style={styles.filterFieldContainer}>
                            <UserRound
                                size={12}
                                color='#5F2EEA'
                            />
                            <Text style={styles.filterText}>Individual</Text>
                        </View>
                        <View style={styles.filterFieldContainer}>
                            <Users
                                size={12}
                                color='#5F2EEA'
                            />
                            <Text style={styles.filterText}>Group</Text>
                        </View>
                        <View style={styles.filterFieldContainer}>
                            <ShieldAlert
                                size={12}
                                color='#5F2EEA'
                            />
                            <Text style={styles.filterText}>Emergency</Text>
                        </View>
                    </View>
                </View>
            </LinearGradient>

            {
                openPopUp ? (
                    <Pressable style={styles.overlay} onPress={() => setOpenPopUp(false)}>
                        <ScanPopUp />
                    </Pressable>
                ) : null
            }
        </>
    );
}

const styles = StyleSheet.create({
    imageStyle: {
        width: 30,
        height: 32
    },

    headerText: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#fff'
    },

    appTitleContainer: {
        flexDirection: 'row',
        gap: 10
    },

    actionIconsContainer: {
        flexDirection: 'row',
        gap: 20,
    },

    actionContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 25
    },

    headerContainer: {
        alignSelf: 'center',
        marginTop: 'auto',
        paddingBottom: 5
    },

    filterText: {
        fontSize: 12,
        color: '#5F2EEA'
    },

    filterFieldContainer: {
        flexDirection: 'row',
        gap: 5,
        alignItems: 'center'
    },

    filterContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        backgroundColor: 'rgba(233, 230, 255, 0.8)',
        paddingHorizontal: 20,
        paddingVertical: 10,
        borderRadius: 20,
    },

    headerWrapper: {
        position: 'relative'
    },

    overlay: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0
    }
});