import { useRouter } from "expo-router";
import { BadgePlus, Smartphone } from "lucide-react-native";
import { useCallback, useEffect, useRef, useState } from "react";
import {
    ActivityIndicator,
    Alert,
    Animated,
    NativeModules,
    StyleSheet,
    Text,
    View,
    useWindowDimensions,
} from "react-native";
import DeviceInfo from "./DeviceInfo";
import TypingDots from "./TypingDots";
import { useTranslation } from "react-i18next";

const { MeshengerApplicationModule } = NativeModules;
const POLL_INTERVAL_MS = 2000;
const ANNOUNCE_INTERVAL_MS = 8000;

type MeshPeer = {
    id: string;
    displayName: string;
    mpAddress: string;
    mpAddressBase64: string;
};

export default function DeviceList() {
    const router = useRouter();
    const [peers, setPeers] = useState<MeshPeer[]>([]);
    const [connectingId, setConnectingId] = useState<string | null>(null);
    const [bootstrapping, setBootstrapping] = useState(true);
    const { width, height } = useWindowDimensions();
    const fadeAnim = useRef(new Animated.Value(0)).current;
    const translateY = useRef(new Animated.Value(20)).current;
    const animatedIn = useRef(false);
    const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const announceRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const { t } = useTranslation();

    const clearTimers = useCallback(() => {
        if (pollRef.current) {
            clearInterval(pollRef.current);
            pollRef.current = null;
        }
        if (announceRef.current) {
            clearInterval(announceRef.current);
            announceRef.current = null;
        }
    }, []);

    const refreshPeers = useCallback(async () => {
        try {
            const result = await MeshengerApplicationModule.refreshMeshScanPeers();
            const list: MeshPeer[] = (result?.peers ?? []) as MeshPeer[];
            setPeers(list);
            if (!animatedIn.current && list.length > 0) {
                animatedIn.current = true;
                Animated.parallel([
                    Animated.timing(fadeAnim, { toValue: 1, duration: 400, useNativeDriver: true }),
                    Animated.timing(translateY, { toValue: 0, duration: 400, useNativeDriver: true }),
                ]).start();
            }
        } catch (error) {
            console.warn("refreshMeshScanPeers failed:", error);
        }
    }, [fadeAnim, translateY]);

    const beginPollingAndAnnounce = useCallback(
        (enableAnnounce: boolean) => {
            clearTimers();
            pollRef.current = setInterval(refreshPeers, POLL_INTERVAL_MS);
            if (enableAnnounce) {
                announceRef.current = setInterval(() => {
                    MeshengerApplicationModule.sendMeshBootstrap?.().catch((err: any) => {
                        console.warn("sendMeshBootstrap failed:", err?.message ?? err);
                    });
                }, ANNOUNCE_INTERVAL_MS);
            }
        },
        [clearTimers, refreshPeers],
    );

    const runInitialScan = useCallback(async (): Promise<boolean> => {
        setBootstrapping(true);
        try {
            const initial = await MeshengerApplicationModule.startMeshDeviceScan();
            const list: MeshPeer[] = (initial?.peers ?? []) as MeshPeer[];
            setPeers(list);
            if (list.length > 0 && !animatedIn.current) {
                animatedIn.current = true;
                Animated.parallel([
                    Animated.timing(fadeAnim, { toValue: 1, duration: 400, useNativeDriver: true }),
                    Animated.timing(translateY, { toValue: 0, duration: 400, useNativeDriver: true }),
                ]).start();
            }
            return true;
        } catch (error) {
            console.warn("startMeshDeviceScan failed:", error);
            return false;
        } finally {
            setBootstrapping(false);
        }
    }, [fadeAnim, translateY]);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            const scanOk = await runInitialScan();
            if (cancelled) return;
            // Always poll the in-memory mesh registry so peers appear after inbound bootstrap
            // even when startMeshDeviceScan failed (e.g. display name not set yet).
            void refreshPeers();
            beginPollingAndAnnounce(scanOk);
        })();
        return () => {
            cancelled = true;
            clearTimers();
        };
    }, [beginPollingAndAnnounce, clearTimers, runInitialScan, refreshPeers]);

    const handleSelect = useCallback(
        async (peer: MeshPeer) => {
            if (connectingId) return;
            setConnectingId(peer.id);
            try {
                await MeshengerApplicationModule.connectToMeshPeer(peer.mpAddress, peer.displayName);
                await MeshengerApplicationModule.openTwoPartySession(peer.id, peer.displayName, true);
                router.push({
                    pathname: "/Chat",
                    params: { id: peer.id, name: peer.displayName },
                });
            } catch (error: any) {
                console.error("Failed to connect to peer:", error);
                Alert.alert("Connection failed", error?.message ?? String(error));
            } finally {
                setConnectingId(null);
            }
        },
        [connectingId, router],
    );

    return (
        <View style={[styles.container, { width, height: height * 0.5 }]}>
            <View style={styles.row}>
                <Smartphone size={20} color="#4DA6FF" />
                <Text style={styles.text}>{t('scanning')}</Text>
                <TypingDots />
            </View>

            <View style={styles.scannedDevices}>
                <BadgePlus size={20} color="rgba(0, 0, 0, 0.65)" />
                <Text>
                    {t('scanned-devices')} ({peers.length})
                </Text>
            </View>

            <View style={{ flexShrink: 1 }}>
                <Animated.ScrollView
                    style={[
                        styles.scrollContainer,
                        peers.length === 0 && { borderWidth: 0 },
                        { opacity: fadeAnim, transform: [{ translateY }] },
                    ]}
                >
                    {peers.length === 0 && !bootstrapping && (
                        <View style={styles.empty}>
                            <Text style={styles.emptyText}>{t('no-devices-found-yet')}</Text>
                            <Text style={styles.emptySubText}>{t('make-sure-another-devices')}</Text>
                        </View>
                    )}

                    {peers.map((peer) => (
                        <DeviceInfo
                            key={peer.id}
                            avatarName={peer.displayName}
                            status={1}
                            disabled={connectingId !== null && connectingId !== peer.id}
                            loading={connectingId === peer.id}
                            onPress={() => handleSelect(peer)}
                        />
                    ))}
                </Animated.ScrollView>

                {bootstrapping && (
                    <View style={styles.bootstrapping}>
                        <ActivityIndicator color="#4DA6FF" />
                        <Text style={styles.bootstrappingText}>Announcing on mesh...</Text>
                    </View>
                )}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 20,
    },
    row: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: 8,
    },
    text: {
        fontSize: 14,
        fontWeight: "800",
        fontStyle: "italic",
        color: "#4DA6FF",
    },
    scrollContainer: {
        backgroundColor: "#F0F9FF",
        borderRadius: 12,
        marginTop: 20,
        borderWidth: 0.5,
        borderColor: "#0EA5E9",
    },
    scannedDevices: {
        marginTop: 20,
        flexDirection: "row",
        alignItems: "center",
        gap: 10,
    },
    empty: {
        padding: 24,
        alignItems: "center",
    },
    emptyText: {
        fontSize: 15,
        fontWeight: "600",
        color: "#1F2937",
    },
    emptySubText: {
        fontSize: 12,
        color: "#6B7280",
        marginTop: 4,
        textAlign: "center",
    },
    bootstrapping: {
        marginTop: 12,
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: 8,
    },
    bootstrappingText: {
        fontSize: 12,
        color: "#4DA6FF",
        fontStyle: "italic",
    },
});
