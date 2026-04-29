import React, { useState, useEffect, useRef } from 'react';
import { 
  View, 
  TextInput, 
  TouchableOpacity, 
  Text, 
  StyleSheet, 
  NativeModules, 
  NativeEventEmitter, 
  FlatList,
  KeyboardAvoidingView,
  Platform,
  EmitterSubscription
} from 'react-native';
import { 
  SafeAreaProvider, 
  SafeAreaView, 
  useSafeAreaInsets 
} from 'react-native-safe-area-context';

// --- Interface ---
interface ChatMessage {
  id: string;
  content: string;
  peerId: string;
  action: 'Send' | 'Receive';
  timestamp: string;
}

// --- Native Module Setup ---
const { SessionModule } = NativeModules;
const sessionEvents = new NativeEventEmitter(SessionModule);

const ChatScreen = () => {
  const [text, setText] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const flatListRef = useRef<FlatList>(null);
  
  // This hook detects navigation buttons vs gesture bars automatically
  const insets = useSafeAreaInsets();

  useEffect(() => {
    const subscription: EmitterSubscription = sessionEvents.addListener('onNewMessage', (event: any) => {
      const newMessage: ChatMessage = {
        id: Date.now().toString() + Math.random().toString(),
        content: event.Message,
        peerId: event.PeerID,
        action: event.Action as 'Send' | 'Receive',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      setMessages((prevMessages) => [...prevMessages, newMessage]);
    });

    return () => subscription.remove();
  }, []);

  const handleSend = () => {
    if (SessionModule && text.trim().length > 0) {
      SessionModule.globalChatSendMessageStr(text);
      setText('');
    }
  };

  const renderItem = ({ item }: { item: ChatMessage }) => {
    const isMe = item.action === 'Send';
    return (
      <View style={[styles.messageWrapper, isMe ? styles.myWrapper : styles.peerWrapper]}>
        {!isMe && <Text style={styles.peerIdText}>Peer: {item.peerId}</Text>}
        <View style={[styles.bubble, isMe ? styles.myBubble : styles.peerBubble]}>
          <Text style={[styles.messageText, isMe ? styles.myText : styles.peerText]}>
            {item.content}
          </Text>
          <Text style={[styles.timeText, isMe ? styles.myTime : styles.peerTime]}>
            {item.timestamp}
          </Text>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      {/* Header: Uses top insets to avoid the camera notch */}
      <View style={[styles.header, { paddingTop: Math.max(insets.top, 20) }]}>
        <Text style={styles.headerTitle}>Global Mesh Chat</Text>
      </View>

      <KeyboardAvoidingView 
        // Android works best with undefined if windowSoftInputMode is set to adjustResize
        behavior={Platform.OS === 'ios' ? 'padding' : undefined} 
        style={styles.flex1}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
      >
        <FlatList
          ref={flatListRef}
          data={messages}
          renderItem={renderItem}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.listContent}
          onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
        />

        {/* Input: Uses bottom insets to sit perfectly above buttons or gestures */}
        <View style={[
          styles.inputContainer, 
          { paddingBottom: Math.max(insets.bottom, 10) }
        ]}>
          <TextInput
            style={styles.input}
            placeholder="Type a message..."
            value={text}
            onChangeText={setText}
            multiline
            placeholderTextColor="#666"
          />
          <TouchableOpacity 
            style={[styles.sendButton, text.trim().length === 0 && styles.disabledButton]} 
            onPress={handleSend}
            disabled={text.trim().length === 0}
          >
            <Text style={styles.sendButtonText}>Send</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
};

// --- Main Export with Provider ---
const SimpleSendStrTest = () => {
  return (
    <SafeAreaProvider>
      <ChatScreen />
    </SafeAreaProvider>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#E5DDD5' },
  flex1: { flex: 1 },
  header: { 
    paddingBottom: 15, 
    backgroundColor: '#075E54', 
    alignItems: 'center',
    elevation: 4
  },
  headerTitle: { color: 'white', fontSize: 18, fontWeight: 'bold' },
  listContent: { paddingHorizontal: 10, paddingVertical: 20 },
  messageWrapper: { marginBottom: 10, maxWidth: '80%' },
  myWrapper: { alignSelf: 'flex-end' },
  peerWrapper: { alignSelf: 'flex-start' },
  peerIdText: { fontSize: 10, color: '#555', marginBottom: 2, marginLeft: 5 },
  bubble: { 
    paddingHorizontal: 12, 
    paddingVertical: 8, 
    borderRadius: 15,
    elevation: 1
  },
  myBubble: { backgroundColor: '#DCF8C6', borderTopRightRadius: 0 },
  peerBubble: { backgroundColor: 'white', borderTopLeftRadius: 0 },
  messageText: { fontSize: 16, color: '#000' },
  myText: { color: '#000' },
  peerText: { color: '#000' },
  timeText: { fontSize: 10, marginTop: 4, alignSelf: 'flex-end' },
  myTime: { color: '#666' },
  peerTime: { color: '#999' },
  inputContainer: { 
    flexDirection: 'row', 
    paddingHorizontal: 10,
    paddingTop: 10,
    backgroundColor: 'white', 
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: '#ccc'
  },
  input: { 
    flex: 1, 
    backgroundColor: '#f0f0f0', 
    borderRadius: 20, 
    paddingHorizontal: 15, 
    paddingVertical: 8,
    maxHeight: 100,
    fontSize: 16,
    color: '#000'
  },
  sendButton: { 
    marginLeft: 10, 
    backgroundColor: '#075E54', 
    width: 60, 
    height: 40, 
    borderRadius: 20, 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  disabledButton: { backgroundColor: '#ccc' },
  sendButtonText: { color: 'white', fontWeight: 'bold' }
});

export default SimpleSendStrTest;