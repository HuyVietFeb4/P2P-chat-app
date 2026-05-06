import { Ionicons } from '@expo/vector-icons';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
    FlatList,
    Image,
    DeviceEventEmitter,
    InteractionManager,
    NativeModules,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';

const { MeshengerApplicationModule } = NativeModules;

type ChatMessage = {
    id: string;
    sessionId: string;
    senderId: string;
    status: string;
    timestamp: number;
    fromMe: boolean;
    text: string;
};

/** Normalize native WritableMap → predictable shape (avoids bad keys / FlatList crashes). */
function normalizeConversationRow(raw: unknown): ChatMessage | null {
    if (raw == null || typeof raw !== 'object') return null;
    const row = raw as Record<string, unknown>;
    const id = row.id != null ? String(row.id) : '';
    if (!id) return null;
    const n = Number(row.timestamp);
    const timestamp =
        typeof row.timestamp === 'number' && !Number.isNaN(row.timestamp)
            ? row.timestamp
            : !Number.isNaN(n)
              ? n
              : Date.now();
    return {
        id,
        sessionId: String(row.sessionId ?? ''),
        senderId: String(row.senderId ?? ''),
        status: String(row.status ?? 'SENT'),
        timestamp,
        fromMe: Boolean(row.fromMe),
        text: row.text != null ? String(row.text) : '',
    };
}

export default function Body({ peerId }: { peerId: string }) {
    const { t } = useTranslation();
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const { colors } = useTheme();
    const listRef = useRef<FlatList<ChatMessage>>(null);

    const isGlobal = peerId === 'global-broadcast';

    const fetchHistory = useCallback(async () => {
        try {
            const rawList = isGlobal
                ? await MeshengerApplicationModule.getGlobalConversation()
                : await MeshengerApplicationModule.getConversation(peerId);

            const list = Array.isArray(rawList)
                ? rawList
                      .map((row: unknown) => normalizeConversationRow(row))
                      .filter((row): row is ChatMessage => row !== null)
                : [];
            setMessages(list);
        } catch (error) {
            console.error('Failed to fetch messages:', error);
        }
    }, [isGlobal, peerId]);

    useEffect(() => {
        fetchHistory();
    }, [fetchHistory]);

    useEffect(() => {
        let cancelled = false;

        const sub = DeviceEventEmitter.addListener('onNewMessage', (event: unknown) => {
            const evt = event as Record<string, unknown>;
            const matches = isGlobal
                ? evt?.sessionType === 'GlobalChat' || evt?.chatId === 'global-chat'
                : evt?.peerId === peerId;
            if (!matches) return;

            if (__DEV__) {
                console.log('[MeshengerChat][JS] onNewMessage', {
                    screenPeerId: peerId,
                    eventId: evt?.id,
                    chatId: evt?.chatId,
                });
            }

            const normalized = normalizeConversationRow(evt);
            if (!normalized) return;

            setMessages((prev) => {
                if (prev.some((m) => m.id === normalized.id)) return prev;
                return [...prev, normalized];
            });

            // Defer SQLite resync + avoid racing layout (can crash Native FlatList on some RN builds).
            InteractionManager.runAfterInteractions(() => {
                setTimeout(() => {
                    if (!cancelled) void fetchHistory();
                }, 150);
            });
        });

        return () => {
            cancelled = true;
            sub.remove();
        };
    }, [isGlobal, peerId, fetchHistory]);

    useEffect(() => {
        // Light polling as a safety net for events fired before the screen mounted.
        const interval = setInterval(fetchHistory, 5000);
        return () => clearInterval(interval);
    }, [fetchHistory]);

    useEffect(() => {
        if (messages.length === 0) return;
        const task = InteractionManager.runAfterInteractions(() => {
            requestAnimationFrame(() => {
                try {
                    listRef.current?.scrollToEnd({ animated: true });
                } catch {
                    /* noop: some RN versions crash if layout not ready */
                }
            });
        });
        return () => task.cancel();
    }, [messages.length]);

    const formatTime = (timestamp: number) => {
        if (!Number.isFinite(timestamp)) return '--';
        const date = new Date(timestamp);
        let hours = date.getHours();
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        return `${hours}:${minutes}${ampm}`;
    };

    const renderMessage = ({ item }: { item: ChatMessage }) => {
        const isMe = item.fromMe;
        return (
            <View style={[styles.messageRow, isMe ? styles.myMessageRow : styles.peerMessageRow]}>
                {!isMe && (
                    <Image
                        source={{ uri: 'https://i.pravatar.cc/150?u=alice' }}
                        style={styles.messageAvatar}
                    />
                )}
                <View style={[styles.bubble, isMe ? { backgroundColor: colors.myBubble, borderBottomRightRadius: 5 } : { backgroundColor: colors.peerBubble, borderBottomLeftRadius: 5, borderWidth: 1, borderColor: colors.border }]}>
                    <Text style={[styles.messageText, { color: isMe ? colors.myMessageText : colors.peerMessageText }]}>
                        {item.text}
                    </Text>
                    <View style={styles.footer}>
                        <Text style={[styles.timeText, { color: isMe ? 'rgba(255, 255, 255, 0.7)' : colors.subText }]}>
                            {formatTime(item.timestamp)}
                        </Text>
                        {isMe && (
                            <View style={styles.statusContainer}>
                                <Ionicons name="checkmark-done" size={14} color="white" style={{marginLeft: 4}} />
                                <Text style={[styles.sentText, { color: 'white' }]}>{t("sent")}</Text>
                            </View>
                        )}
                    </View>
                </View>
            </View>
        );
    };

    return (
        <FlatList
            ref={listRef}
            data={messages}
            extraData={messages}
            renderItem={renderMessage}
            keyExtractor={(item) => item.id}
            contentContainerStyle={styles.listContainer}
            inverted={false}
        />
    );
}

const styles = StyleSheet.create({
    listContainer: {
        paddingHorizontal: 15,
        paddingVertical: 20,
    },
    messageRow: {
        flexDirection: 'row',
        marginBottom: 20,
        maxWidth: '85%',
    },
    myMessageRow: {
        alignSelf: 'flex-end',
    },
    peerMessageRow: {
        alignSelf: 'flex-start',
    },
    messageAvatar: {
        width: 35,
        height: 35,
        borderRadius: 17.5,
        marginRight: 8,
        alignSelf: 'flex-start',
        marginTop: 5,
    },
    bubble: {
        padding: 12,
        borderRadius: 20,
        position: 'relative',
    },
    messageText: {
        fontSize: 15,
        lineHeight: 20,
    },
    footer: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'flex-end',
        marginTop: 5,
    },
    timeText: {
        fontSize: 10,
    },
    statusContainer: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    sentText: {
        fontSize: 10,
        marginLeft: 2,
    },
});
