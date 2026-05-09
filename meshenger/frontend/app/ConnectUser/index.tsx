import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  NativeModules,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import Header from './component/Header';

const { MeshengerApplicationModule } = NativeModules;

export default function ConnectUser() {
  const router = useRouter();
  const { colors } = useTheme();
  const params = useLocalSearchParams<{
    peerId?: string;
    username?: string;
    noisePublicKeyBase64?: string;
  }>();

  const peerId = (params.peerId as string) ?? '';
  const username = (params.username as string) ?? '';
  const noiseB64 = (params.noisePublicKeyBase64 as string) ?? '';

  const [saving, setSaving] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!peerId.startsWith('mp:') || !noiseB64) {
        setError('Missing peer or Noise key from QR.');
        setSaving(false);
        return;
      }
      try {
        const mod = MeshengerApplicationModule as any;
        await mod.savePeerNoisePublicFromQr(peerId, username || peerId, noiseB64);
        if (!cancelled) setError(null);
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setSaving(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [peerId, username, noiseB64]);

  const openChat = () => {
    router.replace({
      pathname: '/Chat',
      params: { id: peerId, name: username || peerId, qrBootstrap: 'qr_scanner' },
    });
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <Header />
      <View style={styles.body}>
        {saving ? (
          <ActivityIndicator size="large" color={colors.primary} />
        ) : error ? (
          <Text style={[styles.msg, { color: colors.text }]}>{error}</Text>
        ) : (
          <>
            <Text style={[styles.msg, { color: colors.text }]}>
              Saved Noise key for {username || peerId}. Open chat to complete XK handshake (you are
              the scanner / initiator).
            </Text>
            <TouchableOpacity style={[styles.btn, { backgroundColor: colors.primary }]} onPress={openChat}>
              <Text style={styles.btnText}>Open chat</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  body: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
    gap: 16,
  },
  msg: { fontSize: 16, lineHeight: 22 },
  btn: { paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  btnText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
