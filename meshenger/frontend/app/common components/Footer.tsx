import { usePathname, useRouter } from 'expo-router';
import { Menu, MessageCircleMore, MessageSquareMore } from 'lucide-react-native';
import { StyleSheet, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { Shadow } from 'react-native-shadow-2';

const NAV_ITEMS = [
  { key: '/ChatBox', label: 'Chats', Icon: MessageCircleMore },
  { key: '/Pending', label: 'Pending', Icon: MessageSquareMore },
  { key: '/More', label: 'More', Icon: Menu },
];

const ACTIVE_COLOR = '#6C47FF';
const INACTIVE_COLOR = '#9CA3AF';
const BG_COLOR = '#FFFFFF';

export default function Footer() {
  const { width } = useWindowDimensions();
  const router = useRouter();
  const pathName = usePathname();
  const insets = useSafeAreaInsets();

  const handleNavigate = (key: string) => {
    if (pathName !== key) {
      router.replace(key as any);
    }
  };

  return (
    <Shadow
      distance={24}
      startColor={'rgba(0,0,0,0.08)'}
      offset={[0, -2]}
      sides={{ top: true }}
    >
      <SafeAreaView
        edges={['bottom']}
        style={[styles.container, { width, paddingBottom: insets.bottom }]}
      >
        <View style={styles.navRow}>
          {NAV_ITEMS.map(({ key, label, Icon }) => {
            const isActive = pathName === key;
            return (
              <TouchableOpacity
                key={key}
                style={styles.navItem}
                onPress={() => handleNavigate(key)}
                activeOpacity={0.7}
              >
                {/* Active pill indicator */}
                {isActive && <View style={styles.activePill} />}

                {/* Icon with subtle background when active */}
                <View style={[styles.iconWrap, isActive && styles.iconWrapActive]}>
                  <Icon
                    size={20}
                    color={isActive ? ACTIVE_COLOR : INACTIVE_COLOR}
                    strokeWidth={isActive ? 2.2 : 1.8}
                  />
                </View>

                {/* Label */}
                <Text style={[styles.label, isActive && styles.labelActive]}>
                  {label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </SafeAreaView>
    </Shadow>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: BG_COLOR,
    paddingTop: 10,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  navRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    paddingHorizontal: 16,
  },
  navItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 4,
    gap: 4,
    position: 'relative',
  },
  activePill: {
    position: 'absolute',
    top: -10,
    width: 32,
    height: 3,
    borderRadius: 99,
    backgroundColor: ACTIVE_COLOR,
  },
  iconWrap: {
    width: 44,
    height: 36,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconWrapActive: {
    backgroundColor: '#EDE9FF',
  },
  label: {
    fontSize: 11,
    fontWeight: '500',
    color: INACTIVE_COLOR,
    letterSpacing: 0.2,
  },
  labelActive: {
    color: ACTIVE_COLOR,
    fontWeight: '700',
  },
});