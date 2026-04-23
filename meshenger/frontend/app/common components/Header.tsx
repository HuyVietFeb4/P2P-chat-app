import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { usePathname } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { LayoutGrid, Search, ShieldAlert, UserPlus, UserRound, Users } from "lucide-react-native";
import { StyleSheet, Text, TouchableOpacity, View, useWindowDimensions } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

interface HeaderProps {
    activeIndex?: number,
    onTabPress?: (index: number) => void,
    openPopUp: boolean,
    setOpenPopUp: () => void
}

export default function Header({ activeIndex, onTabPress, openPopUp, setOpenPopUp }: HeaderProps) {
    const {width, height} = useWindowDimensions();
    const pathname = usePathname();

    const tabs = [
        { label: 'All', icon: LayoutGrid },
        { label: 'Individual', icon: UserRound },
        { label: 'Group', icon: Users },
        { label: 'Emergency', icon: ShieldAlert },
    ];

    return (
        <LinearGradient
            colors={['#0F4C81', '#5F2EEA']}
            start={{x: 0, y: 0}}
            end={{x: 1, y: 0}}
            style={{width: width, paddingTop: 10}}
        >
            <StatusBar translucent backgroundColor="transparent" style="light" />

            <SafeAreaView edges={['top']}>
                <View style={[styles.headerContainer, {width: width * 0.90}]}>
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
                                onPress={setOpenPopUp}
                            />
                        </View>
                    </View>
                    {
                        pathname === "/ChatBox" &&
                        <View style={styles.filterContainer}>
                            {tabs.map((tab, index) => {
                                const Icon = tab.icon;
                                const isActive = activeIndex === index;
                                return (
                                    <TouchableOpacity
                                        key={tab.label}
                                        style={[
                                            styles.filterFieldContainer,
                                            isActive && styles.activeFilterField
                                        ]}
                                        onPress={() => onTabPress?.(index)}
                                    >
                                        <Icon
                                            size={14}
                                            color={isActive ? '#fff' : '#5F2EEA'}
                                        />
                                        <Text style={[
                                            styles.filterText,
                                            isActive && styles.activeFilterText
                                            ]}
                                        >
                                            {tab.label}
                                        </Text>
                                    </TouchableOpacity>
                                );
                            })}
                        </View>
                    }
                </View>
            </SafeAreaView>
        </LinearGradient>
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
        marginBottom: 20
    },

    headerContainer: {
        alignSelf: 'center',
        paddingBottom: 10,
    },

    filterText: {
        fontSize: 10,
        color: '#5F2EEA',
    },

    activeFilterText: {
        color: '#fff',
        fontWeight: 'bold'
    },

    filterFieldContainer: {
        flexDirection: 'row',
        gap: 5,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 15,
    },

    activeFilterField: {
        backgroundColor: '#5F2EEA',
    },

    filterContainer: {
        flexDirection: 'row',
        backgroundColor: 'rgba(233, 230, 255, 0.8)',
        paddingVertical: 8,
        paddingHorizontal: 10,
        borderRadius: 20,
        justifyContent: 'space-between'
    },

    headerWrapper: {
        position: 'relative'
    },
});
