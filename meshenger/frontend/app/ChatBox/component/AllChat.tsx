import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  NativeModules,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from 'react-native';

const { MeshengerApplicationModule } = NativeModules;
const DEFAULT_AVATAR = require('../../../assets/images/avatar.png');

export default function ChatList() {
  const router = useRouter();
  const [chatData, setChatData] = useState([]);
  const [selectedChat, setSelectedChat] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPeers();
  }, []);

  const loadPeers = async () => {
    if (!MeshengerApplicationModule) {
      console.error("MeshengerApplicationModule is not available");
      setLoading(false);
      return;
    }

    try {
      const peers = await MeshengerApplicationModule.listPeers();
      // Map native fields to UI fields and add placeholders for message data
      const formattedPeers = peers.map((peer: any) => ({
        id: peer.id,
        name: peer.displayName,
        avatarUrl: peer.avatarUrl,
        lastMessage: 'No messages yet',
        timestamp: '',
        unreadCount: 0,
      }));
      setChatData(formattedPeers);
    } catch (error) {
      console.error("Failed to load peers:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectChat = (id: string, name: string, avatarUrl: string | null) => {
    setSelectedChat(id);
    console.log(`Opening chat with: ${name}`);

    // Navigate to Chat screen with peer info as parameters
    router.push({
      pathname: '/Chat',
      params: { id, name, avatarUrl }
    });
  };

  const renderChatItem = ({ item }: { item: any }) => {
    const isSelected = item.id === selectedChat;
    const hasUnread = item.unreadCount > 0;

    return (
      <TouchableOpacity 
        style={[styles.chatBox, isSelected && styles.selectedChatBox]} 
        onPress={() => handleSelectChat(item.id, item.name, item.avatarUrl)}
        activeOpacity={0.7}
      >
        <Image
          source={item.avatarUrl ? { uri: item.avatarUrl } : DEFAULT_AVATAR}
          style={styles.avatar}
        />
        
        <View style={styles.textContainer}>
          <Text style={styles.nameText}>{item.name}</Text>
          <Text 
            style={[styles.messageText, hasUnread && styles.messageTextUnread]} 
            numberOfLines={1}
          >
            {item.lastMessage}
          </Text>
        </View>

        <View style={styles.rightContainer}>
          <Text style={[styles.timeText, hasUnread && styles.timeTextUnread]}>
            {item.timestamp}
          </Text>
          {hasUnread && (
            <View style={styles.unreadBadge}>
              <Text style={styles.unreadText}>
                {item.unreadCount > 99 ? '99+' : item.unreadCount}
              </Text>
            </View>
          )}
        </View>

      </TouchableOpacity>
    );
  };

  if (loading) {
    return (
      <View style={[styles.container, { justifyContent: 'center' }]}>
        <ActivityIndicator size="large" color="#007aff" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>      
      <FlatList
        data={chatData}
        keyExtractor={(item) => item.id}
        renderItem={renderChatItem}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>No peers added yet.</Text>
            <Text style={styles.emptySubText}>Scan a QR code to add a peer.</Text>
          </View>
        }
        onRefresh={loadPeers}
        refreshing={loading}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  listContent: {
    paddingVertical: 10,
    flexGrow: 1,
  },
  chatBox: {
    flexDirection: 'row',
    padding: 15,
    backgroundColor: '#ffffff',
    marginHorizontal: 10,
    marginVertical: 5,
    borderRadius: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  selectedChatBox: {
    backgroundColor: '#e6f2ff',
    borderColor: '#b3d9ff',
    borderWidth: 1,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    marginRight: 15,
    backgroundColor: '#f0f0f0',
  },
  textContainer: {
    flex: 1,
    justifyContent: 'center',
    marginRight: 10,
  },
  nameText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#222',
    marginBottom: 4,
  },
  messageText: {
    fontSize: 14,
    color: '#666',
  },
  messageTextUnread: {
    color: '#333',
    fontWeight: '600',
  },
  rightContainer: {
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    height: 40,
  },
  timeText: {
    fontSize: 12,
    color: '#888',
  },
  timeTextUnread: {
    color: '#007bff',
    fontWeight: '600',
  },
  unreadBadge: {
    backgroundColor: '#ff3b30',
    width: 24,
    height: 24,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 4,
  },
  unreadText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 50,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#888',
  },
  emptySubText: {
    fontSize: 14,
    color: '#aaa',
    marginTop: 8,
  },
});
