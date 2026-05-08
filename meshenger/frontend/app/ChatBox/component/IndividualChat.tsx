import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  DeviceEventEmitter,
  NativeModules,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';

const { MeshengerApplicationModule } = NativeModules;
const DEFAULT_AVATAR = require('../../../assets/images/avatar.png');

type DirectPeer = {
  id: string;
  displayName: string;
};

export default function IndividualChat() {
  const router = useRouter();
  const { colors, isDarkMode } = useTheme();
  const { t } = useTranslation();
  const [peers, setPeers] = useState<DirectPeer[]>([]);
  const [loading, setLoading] = useState(true);

  const loadPeers = useCallback(async () => {
    try {
      const all = await MeshengerApplicationModule.listPeers();
      const direct: DirectPeer[] = (all ?? [])
        .filter((p: any) => typeof p?.id === 'string' && p.id.startsWith('mp:'))
        .map((p: any) => ({ id: p.id, displayName: p.displayName ?? p.id }));
      setPeers(direct);
    } catch (error) {
      console.error('Failed to load direct peers:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      loadPeers();
    }, [loadPeers]),
  );

  React.useEffect(() => {
    const handshakeSub = DeviceEventEmitter.addListener('onIncomingHandshake', () => {
      loadPeers();
    });
    const messageSub = DeviceEventEmitter.addListener('onNewMessage', (event: any) => {
      if (event?.sessionType === 'TwoPartyChat') loadPeers();
    });
    const nameSub = DeviceEventEmitter.addListener('onPeerDisplayNameUpdated', () => {
      loadPeers();
    });
    return () => {
      handshakeSub.remove();
      messageSub.remove();
      nameSub.remove();
    };
  }, [loadPeers]);

  const openChat = (peer: DirectPeer) => {
    router.push({ pathname: '/Chat', params: { id: peer.id, name: peer.displayName } });
  };

  if (loading) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <FlatList
        data={peers}
        keyExtractor={(item) => item.id}
        contentContainerStyle={peers.length === 0 ? styles.emptyContainer : styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyInner}>
            <Text style={[styles.emptyTitle, { color: colors.text }]}>{t('no-direct-peers-yet')}</Text>
            <Text style={[styles.emptySubtitle, { color: colors.subText }]}>{t('open-device-scan-hint')}</Text>
          </View>
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            style={[styles.chatBox, { backgroundColor: isDarkMode ? colors.card : '#ffffff' }]}
            onPress={() => openChat(item)}
            activeOpacity={0.7}
          >
            <Image source={DEFAULT_AVATAR} style={styles.avatar} />
            <View style={styles.textContainer}>
              <Text style={[styles.nameText, { color: colors.text }]}>{item.displayName}</Text>
              <Text style={[styles.peerIdText, { color: colors.subText }]} numberOfLines={1}>
                {item.id}
              </Text>
            </View>
          </TouchableOpacity>
        )}
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
  },
  nameText: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  peerIdText: {
    fontSize: 12,
  },
  emptyContainer: {
    flexGrow: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  emptyInner: {
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  emptySubtitle: {
    fontSize: 13,
    marginTop: 8,
    textAlign: 'center',
  },
});
