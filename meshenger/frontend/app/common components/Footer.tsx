import { usePathname, useRouter } from 'expo-router';
import { Menu, MessageCircleMore, MessageSquareMore } from 'lucide-react-native';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Shadow } from 'react-native-shadow-2';
import { useTheme } from '../context/ThemeContext';
import { useTranslation } from 'react-i18next';

export default function Footer() {
  const router = useRouter();
  const pathName = usePathname();
  const insets = useSafeAreaInsets();
  const { colors, isDarkMode } = useTheme();
  const { t } = useTranslation();

  const NAV_ITEMS = [
    { key: '/ChatBox', label: t('chats'), Icon: MessageCircleMore },
    { key: '/Pending', label: t('pending'), Icon: MessageSquareMore },
    { key: '/More', label: t('more'), Icon: Menu },
  ];

  const handleNavigate = (key: string) => {
    if (pathName !== key) {
      router.replace(key as any);
    }
  };

  const ACTIVE_COLOR = colors.primary;
  const INACTIVE_COLOR = colors.subText;
  const BG_COLOR = colors.footerBg;

  return (
    // Sử dụng Shadow bao bọc toàn bộ để tạo hiệu ứng đổ bóng phía trên footer
    <Shadow
      distance={15}
      startColor={isDarkMode ? 'rgba(0,0,0,0.4)' : 'rgba(0,0,0,0.05)'}
      offset={[0, -2]}
      stretch
      sides={{ top: true, bottom: false, start: false, end: false }}
    >
      <View
        style={[
          styles.container,
          {
            backgroundColor: BG_COLOR,
            paddingBottom: insets.bottom > 0 ? insets.bottom : 15, // Đảm bảo có padding ở dưới
          },
        ]}
      >
        <View style={styles.navRow}>
          {NAV_ITEMS.map(({ key, label, Icon }) => {
            const isActive = pathName === key;
            return (
              <TouchableOpacity
                key={key}
                style={styles.navItem}
                onPress={() => handleNavigate(key)}
                activeOpacity={0.6}
              >
                {/* Thanh chỉ báo phía trên (Indicator) */}
                <View 
                  style={[
                    styles.activePill, 
                    { backgroundColor: isActive ? ACTIVE_COLOR : 'transparent' }
                  ]} 
                />

                <View style={[
                  styles.iconWrap, 
                  isActive && { backgroundColor: colors.iconBg || 'rgba(0,0,0,0.05)' }
                ]}>
                  <Icon
                    size={22}
                    color={isActive ? ACTIVE_COLOR : INACTIVE_COLOR}
                    strokeWidth={isActive ? 2.5 : 2}
                  />
                </View>

                <Text
                  style={[
                    styles.label,
                    { color: isActive ? ACTIVE_COLOR : INACTIVE_COLOR },
                    isActive && { fontWeight: '700' }
                  ]}
                >
                  {label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>
    </Shadow>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 8,
  },
  navRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  navItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    position: 'relative',
  },
  activePill: {
    position: 'absolute',
    top: -8, // Đẩy lên sát mép trên của footer
    width: 24,
    height: 4,
    borderBottomLeftRadius: 4,
    borderBottomRightRadius: 4,
  },
  iconWrap: {
    width: 50,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  label: {
    fontSize: 12,
    fontWeight: '500',
  },
});