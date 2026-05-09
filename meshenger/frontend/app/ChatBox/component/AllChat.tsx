import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Image,
    DeviceEventEmitter,
    NativeModules,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAvatarSource } from '../../../assets/avatarMap';

const { MeshengerApplicationModule } = NativeModules;
const DEFAULT_AVATAR = require('../../../assets/images/avatar.png');

export default function ChatList() {
  const router = useRouter();
  const [chatData, setChatData] = useState([]);
  const [selectedChat, setSelectedChat] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const { colors, isDarkMode } = useTheme();
  const { t } = useTranslation();

  const loadPeers = useCallback(async () => {
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
        avatarId: peer.avatarId,
        lastMessage: 'No messages yet',
        timestamp: '',
        unreadCount: 0,
      }));

      // Sort chats so that the global chat (id === 'global-broadcast') is always pinned to the top
      const globalChat = formattedPeers.find((peer: any) => peer.id === 'global-broadcast');
      const otherChats = formattedPeers.filter((peer: any) => peer.id !== 'global-broadcast');
      const sortedPeers = globalChat ? [globalChat, ...otherChats] : otherChats;

      setChatData(sortedPeers);
    } catch (error) {
      console.error("Failed to load peers:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadPeers();
    }, [loadPeers]),
  );

  useEffect(() => {
    const sub = DeviceEventEmitter.addListener('onPeerDisplayNameUpdated', () => {
      loadPeers();
    });
    return () => sub.remove();
  }, [loadPeers]);

  const handleSelectChat = (id: string, name: string, avatarId: string | null) => {
    setSelectedChat(id);
    console.log(`Opening chat with: ${name}`);

    // Navigate to Chat screen with peer info as parameters
    router.push({
      pathname: '/Chat',
      params: { id, name, avatarUrl: avatarId }
    });
  };

  const renderChatItem = ({ item }: { item: any }) => {
    const isSelected = item.id === selectedChat;
    const hasUnread = item.unreadCount > 0;

    return (
      <TouchableOpacity 
        style={[
            styles.chatBox,
            { backgroundColor: isDarkMode ? colors.card : '#ffffff' },
            isSelected && { backgroundColor: isDarkMode ? '#35373C' : '#e6f2ff', borderColor: isDarkMode ? colors.primary : '#b3d9ff', borderWidth: 1 }
        ]}
        onPress={() => handleSelectChat(item.id, item.name, item.avatarId)}
        activeOpacity={0.7}
      >
        <Image
          source={getAvatarSource(item.avatarId)}
          style={[styles.avatar, { backgroundColor: isDarkMode ? '#35373C' : '#f0f0f0' }]}
        />
        
        <View style={styles.textContainer}>
          <Text style={[styles.nameText, { color: colors.text }]}>{item.name}</Text>
          <Text 
            style={[styles.messageText, { color: colors.subText }, hasUnread && { color: colors.text, fontWeight: '600' }]}
            numberOfLines={1}
          >
            {item.lastMessage}
          </Text>
        </View>

        <View style={styles.rightContainer}>
          <Text style={[styles.timeText, { color: colors.subText }, hasUnread && { color: colors.primary, fontWeight: '600' }]}>
            {item.timestamp}
          </Text>
          {hasUnread && (
            <View style={[styles.unreadBadge, { backgroundColor: colors.primary }]}>
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
      <View style={[styles.container, { justifyContent: 'center', backgroundColor: colors.background }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <FlatList
        data={chatData}
        keyExtractor={(item) => item.id}
        renderItem={renderChatItem}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={[styles.emptyText, { color: colors.subText }]}>{t("no-peers-added-yet")}</Text>
            <Text style={[styles.emptySubText, { color: colors.subText, opacity: 0.7 }]}>{t("scan-a-qr-code-to-add-a-peer")}</Text>
          </View>
        }
        onRefresh={loadPeers}
        refreshing={loading}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  listContent: {
    paddingVertical: 10,
    flexGrow: 1,
  },
  chatBox: {
    flexDirection: 'row',
    padding: 15,
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
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    marginRight: 15,
  },
  textContainer: {
    flex: 1,
    justifyContent: 'center',
    marginRight: 10,
  },
  nameText: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  messageText: {
    fontSize: 14,
  },
  rightContainer: {
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    height: 40,
  },
  timeText: {
    fontSize: 12,
  },
  unreadBadge: {
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
  },
  emptySubText: {
    fontSize: 14,
    marginTop: 8,
  },
});
