// import { Ionicons } from '@expo/vector-icons';
// import React, { useEffect, useState, useRef } from 'react';
// import { FlatList, Image, NativeModules, StyleSheet, Text, View, NativeScrollEvent, NativeSyntheticEvent } from 'react-native';
// import { useTheme } from '../../context/ThemeContext';
// import { useTranslation } from 'react-i18next';
// import { ChevronDown } from 'lucide-react-native';

// const { MeshengerApplicationModule } = NativeModules;


// export default function Body({ peerId }: { peerId: string }) {
//     const [messages, setMessages] = useState<any[]>([]);
//     const { colors } = useTheme();
//     const flatListRef = useRef<FlatList<any>>(null);
//     const { t } = useTranslation();
//     const [showScrollButton, setShowScrollButton] = useState(false);
//     const [isFirstLoad, setIsFirstLoad] = useState(true);

//     const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
//         const { layoutMeasurement, contentOffset, contentSize } = event.nativeEvent;
        
//         // Calculate distance from the bottom
//         const distanceFromBottom = contentSize.height - layoutMeasurement.height - contentOffset.y;

//         // Show button if we are more than 200 pixels away from the bottom
//         if (distanceFromBottom > 200) {
//             setShowScrollButton(true);
//         } else {
//             setShowScrollButton(false);
//         }
//     };

//     const scrollToBottom = () => {
//         flatListRef.current?.scrollToEnd({ animated: true });
//     };

//     useEffect(() => {
//         const fetchMessages = async () => {
//             try {
//                 let history;
//                 if (peerId === 'global-broadcast') {
//                     history = await MeshengerApplicationModule.getGlobalConversation();
//                 } else {
//                     history = await MeshengerApplicationModule.getConversation(peerId);
//                 }
//                 setMessages(history);
//             } catch (error) {
//                 console.error("Failed to fetch messages:", error);
//             }
//         };

//         fetchMessages();
//         const interval = setInterval(fetchMessages, 2000); // Poll for new messages
//         return () => clearInterval(interval);
//     }, [peerId]);

//     const formatTime = (timestamp: number) => {
//         const date = new Date(timestamp);
//         let hours = date.getHours();
//         const minutes = date.getMinutes().toString().padStart(2, '0');
//         const ampm = hours >= 12 ? 'PM' : 'AM';
//         hours = hours % 12;
//         hours = hours ? hours : 12;
//         return `${hours}:${minutes}${ampm}`;
//     };

//     const renderMessage = ({ item }: { item: any }) => {
//         const isMe = item.fromMe;
//         return (
//             <View style={[styles.messageRow, isMe ? styles.myMessageRow : styles.peerMessageRow]}>
//                 {!isMe && (
//                     <Image
//                         source={{ uri: 'https://i.pravatar.cc/150?u=alice' }}
//                         style={styles.messageAvatar}
//                     />
//                 )}
//                 <View style={[styles.bubble, isMe ? { backgroundColor: colors.myBubble, borderBottomRightRadius: 5 } : { backgroundColor: colors.peerBubble, borderBottomLeftRadius: 5, borderWidth: 1, borderColor: colors.border }]}>
//                     <Text style={[styles.messageText, { color: isMe ? colors.myMessageText : colors.peerMessageText }]}>
//                         {item.text}
//                     </Text>
//                     <View style={styles.footer}>
//                         <Text style={[styles.timeText, { color: isMe ? 'rgba(255, 255, 255, 0.7)' : colors.subText }]}>
//                             {formatTime(item.timestamp)}
//                         </Text>
//                         {isMe && (
//                             <View style={styles.statusContainer}>
//                                 <Ionicons name="checkmark-done" size={14} color="white" style={{marginLeft: 4}} />
//                                 <Text style={[styles.sentText, { color: 'white' }]}>{t("sent")}</Text>
//                             </View>
//                         )}
//                     </View>
//                 </View>
//             </View>
//         );
//     };

//     return (
//         <View style={{flex: 1}}>
//             <FlatList
//                 ref={flatListRef}
//                 data={messages}
//                 renderItem={renderMessage}
//                 keyExtractor={(item) => item.id}
//                 contentContainerStyle={styles.listContainer}
//                 inverted={false}
//                 scrollEventThrottle={16} // Improves scroll tracking frequency
//                 onScroll={handleScroll}
//                 onContentSizeChange={() => {
//                         if (isFirstLoad) {
//                             flatListRef.current?.scrollToIndex({ index: 1 });
//                             console.log(isFirstLoad)
//                             setIsFirstLoad(false);
//                         } else if (!showScrollButton) {
//                             flatListRef.current?.scrollToEnd({ animated: true });
//                         }
//                     }
//                 }
//                 // onLayout={() => flatListRef.current?.scrollToEnd({ animated: true })}
//             />
//         </View>
//     );
// }

// const styles = StyleSheet.create({
//     listContainer: {
//         paddingHorizontal: 15,
//         paddingVertical: 20,
//     },
//     messageRow: {
//         flexDirection: 'row',
//         marginBottom: 20,
//         maxWidth: '85%',
//     },
//     myMessageRow: {
//         alignSelf: 'flex-end',
//     },
//     peerMessageRow: {
//         alignSelf: 'flex-start',
//     },
//     messageAvatar: {
//         width: 35,
//         height: 35,
//         borderRadius: 17.5,
//         marginRight: 8,
//         alignSelf: 'flex-start',
//         marginTop: 5,
//     },
//     bubble: {
//         padding: 12,
//         borderRadius: 20,
//         position: 'relative',
//     },
//     messageText: {
//         fontSize: 15,
//         lineHeight: 20,
//     },
//     footer: {
//         flexDirection: 'row',
//         alignItems: 'center',
//         justifyContent: 'flex-end',
//         marginTop: 5,
//     },
//     timeText: {
//         fontSize: 10,
//     },
//     statusContainer: {
//         flexDirection: 'row',
//         alignItems: 'center',
//     },
//     sentText: {
//         fontSize: 10,
//         marginLeft: 2,
//     },
// });

import { Ionicons } from '@expo/vector-icons';
import React, { useEffect, useState, useRef } from 'react';
import { 
    FlatList, 
    Image, 
    NativeModules, 
    StyleSheet, 
    Text, 
    View, 
    NativeScrollEvent, 
    NativeSyntheticEvent, 
    TouchableOpacity 
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';
import { ChevronDown } from 'lucide-react-native';

const { MeshengerApplicationModule } = NativeModules;

export default function Body({ peerId }: { peerId: string }) {
    const [messages, setMessages] = useState<any[]>([]);
    const { colors } = useTheme();
    const flatListRef = useRef<FlatList<any>>(null);
    const { t } = useTranslation();
    const [showScrollButton, setShowScrollButton] = useState(false);

    const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
        // In an inverted list, 0 is the bottom. 
        // contentOffset.y increases as you scroll UP (towards older messages).
        const offsetY = event.nativeEvent.contentOffset.y;

        // Show button if we have scrolled up more than 200 pixels
        if (offsetY > 200) {
            setShowScrollButton(true);
        } else {
            setShowScrollButton(false);
        }
    };

    const scrollToBottom = () => {
        // In an inverted list, index 0 is the bottom (most recent)
        if (messages.length > 0) {
            flatListRef.current?.scrollToIndex({ index: 0, animated: true });
        }
    };

    useEffect(() => {
        const fetchMessages = async () => {
            try {
                let history;
                if (peerId === 'global-broadcast') {
                    history = await MeshengerApplicationModule.getGlobalConversation();
                } else {
                    history = await MeshengerApplicationModule.getConversation(peerId);
                }

                /**
                 * IMPORTANT: Since inverted={true}, index 0 is the bottom.
                 * If your native module returns [Oldest -> Newest], 
                 * we MUST reverse it so it becomes [Newest -> Oldest].
                 */
                const newestFirst = [...history].reverse();
                setMessages(newestFirst);
            } catch (error) {
                console.error("Failed to fetch messages:", error);
            }
        };

        fetchMessages();
        const interval = setInterval(fetchMessages, 2000); 
        return () => clearInterval(interval);
    }, [peerId]);

    const formatTime = (timestamp: number) => {
        const date = new Date(timestamp);
        let hours = date.getHours();
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12 || 12;
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
                <View style={[
                    styles.bubble, 
                    isMe 
                        ? { backgroundColor: colors.myBubble, borderBottomRightRadius: 5 } 
                        : { backgroundColor: colors.peerBubble, borderBottomLeftRadius: 5, borderWidth: 1, borderColor: colors.border }
                ]}>
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
        <View style={{ flex: 1 }}>
            <FlatList
                ref={flatListRef}
                data={messages}
                renderItem={renderMessage}
                keyExtractor={(item) => item.id}
                contentContainerStyle={styles.listContainer}
                inverted={true} // This makes the list start at the bottom by default
                scrollEventThrottle={16}
                onScroll={handleScroll}
            />

            {showScrollButton && (
                <TouchableOpacity 
                    style={[styles.scrollButton, { backgroundColor: colors.background || 'white' }]} 
                    onPress={scrollToBottom}
                    activeOpacity={0.9}
                >
                    <ChevronDown size={24} color={colors.text || 'black'} />
                </TouchableOpacity>
            )}
        </View>
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
    scrollButton: {
        position: 'absolute',
        bottom: 20,
        width: 44,
        height: 44,
        borderRadius: 22,
        justifyContent: 'center',
        alignItems: 'center',
        elevation: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.2,
        shadowRadius: 3,
        borderWidth: 1,
        borderColor: 'rgba(0,0,0,0.05)',
        alignSelf: "center"
    }
});
