import { Ionicons } from '@expo/vector-icons';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Image, NativeEventEmitter, NativeModules, StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';

const { MeshengerApplicationModule } = NativeModules;
const meshEvents = new NativeEventEmitter(MeshengerApplicationModule);

type ChatMessage = {
    id: string;
    sessionId: string;
    senderId: string;
    status: string;
    timestamp: number;
    fromMe: boolean;
    text: string;
};

export default function Body({ peerId }: { peerId: string }) {
    const { t } = useTranslation();
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const { colors } = useTheme();
    const listRef = useRef<FlatList<ChatMessage>>(null);

    const isGlobal = peerId === 'global-broadcast';

    const fetchHistory = useCallback(async () => {
        try {
            const history: ChatMessage[] = isGlobal
                ? await MeshengerApplicationModule.getGlobalConversation()
                : await MeshengerApplicationModule.getConversation(peerId);
            setMessages(history);
        } catch (error) {
            console.error("Failed to fetch messages:", error);
        }
    }, [isGlobal, peerId]);

    useEffect(() => {
        fetchHistory();
    }, [fetchHistory]);

    useEffect(() => {
        const sub = meshEvents.addListener('onNewMessage', (event: any) => {
            // Direct chats use peerId; global chat uses chatId='global-chat' with sessionType='GlobalChat'.
            const matches = isGlobal
                ? event?.sessionType === 'GlobalChat' || event?.chatId === 'global-chat'
                : event?.peerId === peerId;
            if (!matches) return;

            const incoming: ChatMessage = {
                id: String(event.id),
                sessionId: String(event.sessionId ?? ''),
                senderId: String(event.senderId ?? ''),
                status: String(event.status ?? 'SENT'),
                timestamp: Number(event.timestamp ?? Date.now()),
                fromMe: Boolean(event.fromMe),
                text: String(event.text ?? ''),
            };

            setMessages((prev) => {
                if (prev.some((m) => m.id === incoming.id)) return prev;
                return [...prev, incoming];
            });
        });
        return () => sub.remove();
    }, [isGlobal, peerId]);

    useEffect(() => {
        // Light polling as a safety net for events fired before the screen mounted.
        const interval = setInterval(fetchHistory, 5000);
        return () => clearInterval(interval);
    }, [fetchHistory]);

    useEffect(() => {
        if (messages.length === 0) return;
        const t = setTimeout(() => listRef.current?.scrollToEnd({ animated: true }), 50);
        return () => clearTimeout(t);
    }, [messages.length]);

    const formatTime = (timestamp: number) => {
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
