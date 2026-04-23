import { usePathname, useRouter } from 'expo-router';
import { Menu, MessageCircleMore, MessageSquareMore } from 'lucide-react-native';
import { StyleSheet, Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { Shadow } from 'react-native-shadow-2';

export default function Footer() {
  const { width, height } = useWindowDimensions();
  const router = useRouter();
  const pathName = usePathname();

  const handleNavigateChat = () : void => {
    if (pathName != "/ChatBox") {
      router.replace("/ChatBox");
    }
  }

  const handleNavigateMore = () : void => {
    if (pathName != "/More") {
      router.replace("/More");
    }
  }

  return (
    <Shadow
      distance={20}
      startColor={'rgba(13, 10, 44, 0.06)'}
      offset={[0, -4]}
      sides={{top: true}}
      style={[styles.footerContainer, { width: width, height: height * 0.12 }]}
    >
      <View style={styles.footerWrapper}>
        <View style={styles.footerActionsContainer}>
          <TouchableOpacity style={styles.actionContainer} onPress={handleNavigateChat}>
            <MessageCircleMore size={25} color={'#6B7280'} />
            <Text style={styles.footerName}>Chats</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.actionContainer}>
            <MessageSquareMore size={25} color={'#6B7280'} />
            <Text style={styles.footerName}>Pending chats</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.actionContainer} onPress={handleNavigateMore}>
            <Menu size={25} color={'#6B7280'} />
            <Text style={styles.footerName}>More</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Shadow>
  );
}

const styles = StyleSheet.create({
  footerContainer: {
    backgroundColor: '#fff'
  },
  footerName: {
    fontSize: 14,
    color: '#6B7280',
    fontWeight: 'bold'
  },
  actionContainer: {
    flexDirection: 'column',
    gap: 5,
    alignItems: 'center',
  },
  footerActionsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
    footerWrapper: {
        flex: 1,
        justifyContent: 'center',
    }
});