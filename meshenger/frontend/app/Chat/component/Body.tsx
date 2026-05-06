import { Ionicons } from '@expo/vector-icons';
import React, { useEffect, useState } from 'react';
import { FlatList, Image, NativeModules, StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';

const { MeshengerApplicationModule } = NativeModules;
const { t } = useTranslation();

export default function Body({ peerId }: { peerId: string }) {
    const [messages, setMessages] = useState<any[]>([]);
    const { colors } = useTheme();

    useEffect(() => {
        const fetchMessages = async () => {
            try {
                let history;
                if (peerId === 'global-broadcast') {
                    history = await MeshengerApplicationModule.getGlobalConversation();
                } else {
                    history = await MeshengerApplicationModule.getConversation(peerId);
                }
                setMessages(history);
            } catch (error) {
                console.error("Failed to fetch messages:", error);
            }
        };

        fetchMessages();
        const interval = setInterval(fetchMessages, 2000); // Poll for new messages
        return () => clearInterval(interval);
    }, [peerId]);

    const formatTime = (timestamp: number) => {
        const date = new Date(timestamp);
        let hours = date.getHours();
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        return `${hours}:${minutes}${ampm}`;
    };

    const renderMessage = ({ item }: { item: any }) => {
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
