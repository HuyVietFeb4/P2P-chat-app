import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Alert,
    DeviceEventEmitter,
    FlatList,
    Image,
    NativeModules,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from 'react-native';
import Footer from '../common components/Footer';
import Header from '../common components/Header';
import { useTheme } from '../context/ThemeContext';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
import { getAvatarSource } from '../../assets/avatarMap';
import { useBluetooth } from '@/hook/useBluetooth';
import BluetoothPopup from '../common components/BluetoothPopUp';

const { MeshengerApplicationModule } = NativeModules;

type InviteRow = { peerId: string; displayName: string; avatarId?: string };

export default function PendingScreen() {
    const router = useRouter();
    const { colors, isDarkMode } = useTheme();
    const { t } = useTranslation();
    const params = useLocalSearchParams<{ outgoingPeerId?: string; outgoingName?: string }>();
    const { showPopup, openBluetoothSettings, dismissPopup } = useBluetooth();

    const outgoingPeerId =
        typeof params.outgoingPeerId === 'string' ? params.outgoingPeerId.trim() : '';
    const outgoingName =
        typeof params.outgoingName === 'string'
            ? params.outgoingName.trim()
            : outgoingPeerId;

    const [incomingInvites, setIncomingInvites] = useState<InviteRow[]>([]);

    useFocusEffect(
        useCallback(() => {
            let cancelled = false;
            (async () => {
                try {
                    const mod = MeshengerApplicationModule as any;
                    if (typeof mod.getPendingIncomingInvites !== 'function') return;
                    const rows = await mod.getPendingIncomingInvites();
                    if (cancelled || !Array.isArray(rows)) return;
                    const mapped: InviteRow[] = rows
                        .map((r: any) => ({
                            peerId: r?.peerId != null ? String(r.peerId) : '',
                            displayName:
                                r?.displayName != null && String(r.displayName).trim().length > 0
                                    ? String(r.displayName)
                                    : (r?.peerId != null ? String(r.peerId) : ''),
                            avatarId: r?.avatarId != null ? String(r.avatarId) : undefined,
                        }))
                        .filter((r) => r.peerId.length > 0);
                    setIncomingInvites(mapped);
                } catch {
                    /* native not rebuilt yet */
                }
            })();
            return () => {
                cancelled = true;
            };
        }, []),
    );

    const pushIncomingUnique = useCallback((row: InviteRow) => {
        setIncomingInvites((prev) => {
            if (prev.some((p) => p.peerId === row.peerId)) return prev;
            return [...prev, row];
        });
    }, []);

    const removeIncoming = useCallback((peerId: string) => {
        setIncomingInvites((prev) => prev.filter((p) => p.peerId !== peerId));
    }, []);

    /** Optional: RN Android may omit trailing null-args when calling Kotlin. Keep native defaults in sync. */
    const respondInvite = async (fromPeerId: string, accept: boolean, inviterDisplayName: string | null) => {
        const mod = MeshengerApplicationModule;
        try {
            if (typeof mod.respondDirectChatInvite === 'function') {
                await mod.respondDirectChatInvite(fromPeerId, accept, inviterDisplayName);
            } else {
                throw new Error('respondDirectChatInvite not available — rebuild native app');
            }
        } catch (e: any) {
            throw new Error(e?.message ?? String(e));
        }
    };

    useEffect(() => {
        const subInvite = DeviceEventEmitter.addListener('onIncomingDirectChatInvite', (e: any) => {
            const pid = typeof e?.peerId === 'string' ? e.peerId.trim() : '';
            const dn = typeof e?.displayName === 'string' ? e.displayName.trim() : pid;
            const avt = typeof e?.avatarId === 'string' ? e.avatarId.trim() || undefined : undefined;
            if (!pid) return;
            pushIncomingUnique({ peerId: pid, displayName: dn, avatarId: avt });
        });

        const subAccepted = DeviceEventEmitter.addListener('onDirectChatInviteAccepted', (e: any) => {
            const pid = typeof e?.peerId === 'string' ? e.peerId.trim() : '';
            const dn = typeof e?.displayName === 'string' ? e.displayName.trim() : pid;
            const avt = typeof e?.avatarId === 'string' ? e.avatarId.trim() : undefined;
            if (!pid) return;

            removeIncoming(pid);

            if (outgoingPeerId && outgoingPeerId === pid) {
                router.push({
                    pathname: '/Chat',
                    params: { id: pid, name: dn || outgoingName || pid, avatarUrl: avt },
                });
            }
        });

        const subRejected = DeviceEventEmitter.addListener('onDirectChatInviteRejected', (e: any) => {
            const pid = typeof e?.peerId === 'string' ? e.peerId.trim() : '';
            if (outgoingPeerId && pid === outgoingPeerId) {
                Alert.alert(t('invite-rejected-title'), t('invite-rejected-body'));
                router.replace('/ChatBox' as any);
            }
        });

        return () => {
            subInvite.remove();
            subAccepted.remove();
            subRejected.remove();
        };
    }, [outgoingPeerId, outgoingName, pushIncomingUnique, removeIncoming, router, t]);

    const onAcceptIncoming = async (item: InviteRow) => {
        try {
            await respondInvite(item.peerId, true, item.displayName);
            removeIncoming(item.peerId);
            router.push({
                pathname: '/Chat',
                params: { id: item.peerId, name: item.displayName, avatarUrl: item.avatarId },
            });
        } catch (e: any) {
            Alert.alert(t('error'), e?.message ?? String(e));
        }
    };

    const onRejectIncoming = async (item: InviteRow) => {
        try {
            await respondInvite(item.peerId, false, null);
            removeIncoming(item.peerId);
        } catch (e: any) {
            Alert.alert(t('error'), e?.message ?? String(e));
        }
    };

    const outgoingBanner = useMemo(() => {
        if (!outgoingPeerId) return null;
        return (
            <View
                style={[
                    styles.card,
                    { backgroundColor: isDarkMode ? colors.card : '#f0f9ff', borderColor: colors.primary },
                ]}
            >
                <Text style={[styles.cardTitle, { color: colors.text }]}>{t('waiting-for-accept-title')}</Text>
                <Text style={[styles.cardSub, { color: colors.subText }]}>
                    {outgoingName || outgoingPeerId}
                </Text>
                <Text style={[styles.cardHint, { color: colors.subText }]}>{t('waiting-for-accept-hint')}</Text>
            </View>
        );
    }, [colors.card, colors.primary, colors.subText, colors.text, isDarkMode, outgoingName, outgoingPeerId, t]);

    return (
        <View style={[styles.safe, { backgroundColor: colors.background }]}>
            <Header openPopUp={false} setOpenPopUp={() => { }} />
            <View style={styles.content}>
                <Text style={[styles.sectionTitle, { color: colors.text }]}>{t('pending-chat-requests')}</Text>
                {outgoingBanner}

                <FlatList
                    data={incomingInvites}
                    keyExtractor={(item) => item.peerId}
                    ListEmptyComponent={
                        incomingInvites.length === 0 && !outgoingPeerId ? (
                            <Text style={[styles.empty, { color: colors.subText }]}>
                                {t('pending-no-incoming')}
                            </Text>
                        ) : null
                    }
                    renderItem={({ item }) => (
                        <View
                            style={[styles.rowCard, { backgroundColor: colors.card, borderColor: colors.border }]}
                        >
                            <View style={styles.rowCardHeader}>
                                <Image
                                    source={getAvatarSource(item.avatarId)}
                                    style={styles.inviteAvatar}
                                />
                                <View style={styles.rowCardText}>
                                    <Text style={[styles.peerName, { color: colors.text }]}>{item.displayName}</Text>
                                    {/* <Text style={[styles.peerId, { color: colors.subText }]}>{item.peerId}</Text> */}
                                </View>
                            </View>
                            <View style={styles.rowActions}>
                                <TouchableOpacity
                                    style={[styles.btn, styles.reject, { borderColor: colors.border }]}
                                    onPress={() => onRejectIncoming(item)}
                                >
                                    <Text style={{ color: colors.text }}>{t('reject')}</Text>
                                </TouchableOpacity>
                                <TouchableOpacity
                                    style={[styles.btn, styles.accept, { backgroundColor: colors.primary }]}
                                    onPress={() => onAcceptIncoming(item)}
                                >
                                    <Text style={{ color: '#fff', fontWeight: '600' }}>{t('accept')}</Text>
                                </TouchableOpacity>
                            </View>
                        </View>
                    )}
                />
            </View>
            <Footer />

            {showPopup && (
                <BluetoothPopup
                    visible={true}
                    onDismiss={dismissPopup}
                />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    safe: {
        flex: 1,
    },
    content: {
        flex: 1,
        paddingHorizontal: 16,
        paddingTop: 8,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        marginBottom: 12,
    },
    card: {
        borderRadius: 12,
        borderWidth: 1,
        padding: 14,
        marginBottom: 16,
    },
    cardTitle: {
        fontSize: 15,
        fontWeight: '700',
    },
    cardSub: {
        marginTop: 4,
        fontSize: 15,
    },
    cardHint: {
        marginTop: 8,
        fontSize: 12,
    },
    empty: {
        textAlign: 'center',
        marginTop: 24,
        fontSize: 14,
    },
    rowCard: {
        borderRadius: 12,
        borderWidth: 1,
        padding: 14,
        marginBottom: 12,
    },
    rowCardHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        marginBottom: 10,
    },
    inviteAvatar: {
        width: 44,
        height: 44,
        borderRadius: 22,
    },
    rowCardText: {
        flex: 1,
    },
    peerName: {
        fontSize: 16,
        fontWeight: '600',
    },
    peerId: {
        fontSize: 12,
        marginTop: 2,
    },
    rowActions: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
        gap: 10,
        marginTop: 12,
    },
    btn: {
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 10,
        minWidth: 88,
        alignItems: 'center',
    },
    reject: {
        borderWidth: 1,
    },
    accept: {},
});
