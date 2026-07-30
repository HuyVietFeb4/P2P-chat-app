// import { useLocalSearchParams, useRouter } from 'expo-router';
// import { useEffect, useState } from 'react';
// import {
//   ActivityIndicator,
//   NativeModules,
//   StyleSheet,
//   Text,
//   TouchableOpacity,
//   View,
// } from 'react-native';
// import { useTheme } from '../context/ThemeContext';
// import Header from './component/Header';

// const { MeshengerApplicationModule } = NativeModules;

// export default function ConnectUser() {
//   const router = useRouter();
//   const { colors } = useTheme();
//   const params = useLocalSearchParams<{
//     peerId?: string;
//     username?: string;
//     noisePublicKeyBase64?: string;
//   }>();

//   const peerId = (params.peerId as string) ?? '';
//   const username = (params.username as string) ?? '';
//   const noiseB64 = (params.noisePublicKeyBase64 as string) ?? '';

//   const [saving, setSaving] = useState(true);
//   const [error, setError] = useState<string | null>(null);

//   useEffect(() => {
//     let cancelled = false;
//     (async () => {
//       if (!peerId.startsWith('mp:') || !noiseB64) {
//         setError('Missing peer or Noise key from QR.');
//         setSaving(false);
//         return;
//       }
//       try {
//         const mod = MeshengerApplicationModule as any;
//         await mod.savePeerNoisePublicFromQr(peerId, username || peerId, noiseB64);
//         if (!cancelled) setError(null);
//       } catch (e: any) {
//         if (!cancelled) setError(e?.message ?? String(e));
//       } finally {
//         if (!cancelled) setSaving(false);
//       }
//     })();
//     return () => {
//       cancelled = true;
//     };
//   }, [peerId, username, noiseB64]);

//   const openChat = () => {
//     router.replace({
//       pathname: '/Chat',
//       params: { id: peerId, name: username || peerId, qrBootstrap: 'qr_scanner' },
//     });
//   };

//   return (
//     <View style={[styles.root, { backgroundColor: colors.background }]}>
//       <Header />
//       <View style={styles.body}>
//         {saving ? (
//           <ActivityIndicator size="large" color={colors.primary} />
//         ) : error ? (
//           <Text style={[styles.msg, { color: colors.text }]}>{error}</Text>
//         ) : (
//           <>
//             <Text style={[styles.msg, { color: colors.text }]}>
//               Saved Noise key for {username || peerId}. Open chat to complete XK handshake (you are
//               the scanner / initiator).
//             </Text>
//             <TouchableOpacity style={[styles.btn, { backgroundColor: colors.primary }]} onPress={openChat}>
//               <Text style={styles.btnText}>Open chat</Text>
//             </TouchableOpacity>
//           </>
//         )}
//       </View>
//     </View>
//   );
// }

// const styles = StyleSheet.create({
//   root: { flex: 1 },
//   body: {
//     flex: 1,
//     padding: 24,
//     justifyContent: 'center',
//     gap: 16,
//   },
//   msg: { fontSize: 16, lineHeight: 22 },
//   btn: { paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
//   btnText: { color: '#fff', fontWeight: '700', fontSize: 16 },
// });

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
import { CheckCircle2, AlertCircle, User } from 'lucide-react-native'; // Thêm icon để UI sinh động
import { Image } from 'expo-image';
import { getAvatarSource } from '@/assets/avatarMap';
import { useTranslation } from 'react-i18next';

const { MeshengerApplicationModule } = NativeModules;

export default function ConnectUser() {
  const router = useRouter();
  const { colors } = useTheme();
  const params = useLocalSearchParams<{
    peerId?: string;
    username?: string;
    noisePublicKeyBase64?: string;
    avatarId?: string
  }>();

  const peerId = (params.peerId as string) ?? '';
  const username = (params.username as string) ?? '';
  const noiseB64 = (params.noisePublicKeyBase64 as string) ?? '';
  const { t } = useTranslation();
  // const avaId = require(`@/assets/avt_set/${params.avatarId}`);

  const [saving, setSaving] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Lấy chữ cái đầu của username để làm Avatar
  const avatarLabel = (username || peerId).substring(0, 2).toUpperCase();

  useEffect(() => {
    let cancelled = false;
    console.log(params.avatarId);
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
    return () => { cancelled = true; };
  }, [peerId, username, noiseB64]);

  const openChat = () => {
    router.replace({
      pathname: '/Chat',
      params: { id: peerId, name: username || peerId, qrBootstrap: 'qr_scanner', security: 'strong' },
    });
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <Header />
      
      <View style={styles.container}>
        {/* Phần Avatar & Thông tin User */}
        <View style={styles.profileSection}>
          <View style={styles.avatar}>
            <Image
              source={getAvatarSource(params.avatarId)}
              style={styles.avatarImage}
              contentFit="cover"        
              transition={500}           
            />
          </View>
          <Text style={[styles.username, { color: colors.text }]}>{username || 'Unknown User'}</Text>
        </View>

        <View style={styles.statusSection}>
          {saving ? (
            <View style={styles.loadingBox}>
              <ActivityIndicator size="large" color={colors.primary} />
              <Text style={{ color: colors.subText, marginTop: 10 }}>Saving contact...</Text>
            </View>
          ) : error ? (
            /* Card thông báo Lỗi */
            <View style={[styles.statusCard, { backgroundColor: colors.error.bg, borderColor: colors.error.borderColor }]}>
              <AlertCircle color={colors.error.textColor} size={24} />
              <Text style={[styles.statusText, { color: colors.error.textColor }]}>{error}</Text>
            </View>
          ) : (
            /* Card thông báo Thành công */
            <View style={styles.successContainer}>
              <View style={[styles.statusCard, { backgroundColor: colors.success.bg, borderColor: colors.success.borderColor }]}>
                <CheckCircle2 color={colors.success.textColor} size={24} />
                <View style={{ flex: 1 }}>
                  <Text style={[styles.statusText, { color: colors.success.textColor, fontWeight: 'bold' }]}>
                    {t('successful-connect')}
                  </Text>
                  <Text style={[styles.statusSubText, { color: colors.success.textColor }]}>
                    {t('noise-key-saved')}
                  </Text>
                </View>
              </View>

              <TouchableOpacity 
                style={[styles.btn, { backgroundColor: colors.primary }]} 
                onPress={openChat}
              >
                <Text style={styles.btnText}>{t('start-chatting-now')}</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 40,
    alignItems: 'center',
  },
  profileSection: {
    alignItems: 'center',
    marginBottom: 40,
  },
  avatar: {
    width: 100,
    height: 100,
    borderRadius: 50,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
    // Hiệu ứng đổ bóng nhẹ cho avatar
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  avatarText: {
    fontSize: 32,
    fontWeight: 'bold',
  },
  username: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  peerId: {
    fontSize: 14,
    fontFamily: 'monospace', // PeerID thường dài, dùng monospace nhìn chuyên nghiệp hơn
  },
  statusSection: {
    width: '100%',
  },
  statusCard: {
    flexDirection: 'row',
    padding: 16,
    borderRadius: 16,
    borderWidth: 1.5,
    gap: 12,
    alignItems: 'flex-start',
    marginBottom: 24,
  },
  statusText: {
    fontSize: 15,
    lineHeight: 20,
  },
  statusSubText: {
    fontSize: 13,
    marginTop: 4,
    opacity: 0.8,
  },
  successContainer: {
    width: '100%',
  },
  btn: {
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    width: '100%',
    elevation: 2,
  },
  btnText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  loadingBox: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImage: {
    width: '100%',      
    height: '100%',
    borderRadius: 999
  },
});
