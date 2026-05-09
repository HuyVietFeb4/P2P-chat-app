import React, { useState, useEffect } from 'react';
import { 
  StyleSheet, 
  Text, 
  View, 
  Modal, 
  TextInput, 
  TouchableOpacity, 
  Image, 
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Dimensions,
  NativeModules
} from 'react-native';
import { useTranslation } from 'react-i18next';

const { width } = Dimensions.get('window');

type Props = {
  onClose: () => void,
  onSaved: () => Promise<void>;
}

export default function InfoPopUp({ onClose, onSaved }: Props) {
  const AVATARS = [
      { id: '1', url: require('@/assets/avt_set/avt0.png'), name: "avt0" },
      { id: '2', url: require('@/assets/avt_set/avt1.png'), name: "avt1" },
      { id: '3', url: require('@/assets/avt_set/avt2.png'), name: "avt2" },
      { id: '4', url: require('@/assets/avt_set/avt3.png'), name: "avt3" },
      { id: '5', url: require('@/assets/avt_set/avt4.png'), name: "avt4" },
      { id: '6', url: require('@/assets/avt_set/avt5.png'), name: "avt5" },
      { id: '7', url: require('@/assets/avt_set/avt6.png'), name: "avt6" },
      { id: '8', url: require('@/assets/avt_set/avt7.png'), name: "avt7" },
      { id: '9', url: require('@/assets/avt_set/avt8.png'), name: "avt8" },
      { id: '10', url: require('@/assets/avt_set/avt9.png'), name: "avt9" },
      { id: '11', url: require('@/assets/avt_set/avt10.png'), name: "avt10" },
      { id: '12', url: require('@/assets/avt_set/avt11.png'), name: "avt11" },
      { id: '13', url: require('@/assets/avt_set/avt12.png'), name: "avt12" },
    ];

    const { MeshengerApplicationModule } = NativeModules;
    const [username, setUsername] = useState('');
    const [selectedAvatar, setSelectedAvatar] = useState(AVATARS[0].name);
    const [savedName, setSavedName] = useState('');

    useEffect(() => {
      const fetchUser = async () => {
        const user = await MeshengerApplicationModule.getMyIdentity();
        setSavedName(user.displayName);
        setUsername(user.displayName);
        setSelectedAvatar(user.userAvtId);
      }

      fetchUser();
    }, [])

    const { t } = useTranslation();

    const handleSave = async () => {
        try {
          await MeshengerApplicationModule.updateMyProfile(username.trim() || savedName, selectedAvatar);
          await onSaved();
          onClose();
        } catch (e) {
          console.error("Failed to update profile", e)
        }
    };

    return (
        <Modal
            animationType="fade"
            transparent={true}
            visible={true}
            onRequestClose={onClose}
        >
            <KeyboardAvoidingView
                style={{ flex: 1 }}
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            >
                <View style={styles.overlay}>
                    <View style={styles.modalCard}>
                        {/* Header */}
                        <View style={styles.header}>
                            <View style={styles.indicator} />
                            <Text style={styles.modalTitle}>{t('profile-info')}</Text>
                        </View>

                        {/* Avatar Picker */}
                        <Text style={styles.sectionLabel}>{t('select-your-avatar')}</Text>
                        <FlatList
                            data={AVATARS}
                            horizontal
                            showsHorizontalScrollIndicator={false}
                            keyExtractor={(item) => item.id}
                            contentContainerStyle={styles.avatarListContent}
                            renderItem={({ item }) => {
                                const isSelected = selectedAvatar === item.name;
                                return (
                                    <TouchableOpacity 
                                        activeOpacity={0.8}
                                        onPress={() => setSelectedAvatar(item.name)}
                                        style={[
                                            styles.avatarWrapper,
                                            isSelected && styles.selectedAvatarWrapper
                                        ]}
                                    >
                                        <Image source={item.url} style={styles.avatarImage} />
                                        {isSelected && <View style={styles.checkBadge} />}
                                    </TouchableOpacity>
                                );
                            }}
                            style={styles.avatarList}
                        />

                        {/* Input Field */}
                        <Text style={styles.sectionLabel}>{t('username')}</Text>
                        <TextInput
                            style={styles.input}
                            placeholder={t('enter-your-username')}
                            placeholderTextColor="#94a3b8"
                            value={username}
                            onChangeText={setUsername}
                        />

                        {/* Actions */}
                        <View style={styles.buttonRow}>
                            <TouchableOpacity
                                style={[styles.button, styles.buttonCancel]}
                                onPress={onClose}
                            >
                                <Text style={styles.cancelText}>{t('cancel')}</Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={[styles.button, styles.buttonSave]}
                                onPress={handleSave}
                            >
                                <Text style={styles.saveText}>{t('save')}</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    overlay: {
        flex: 1,
        backgroundColor: 'rgba(15, 23, 42, 0.7)', // Màu nền tối xanh slate deep
        justifyContent: 'center',
        alignItems: 'center',
    },
    modalCard: {
        width: width * 0.9,
        backgroundColor: 'white',
        borderRadius: 32,
        paddingHorizontal: 24,
        paddingBottom: 32,
        paddingTop: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.15,
        shadowRadius: 20,
        elevation: 10,
    },
    header: {
        alignItems: 'center',
        marginBottom: 24,
    },
    indicator: {
        width: 40,
        height: 4,
        backgroundColor: '#e2e8f0',
        borderRadius: 2,
        marginBottom: 16,
    },
    modalTitle: {
        fontSize: 22,
        fontWeight: '700',
        color: '#1e293b',
    },
    sectionLabel: {
        fontSize: 14,
        fontWeight: '600',
        color: '#64748b',
        marginBottom: 12,
        textTransform: 'uppercase',
        letterSpacing: 0.5,
    },
    avatarList: {
        marginBottom: 24,
        marginHorizontal: -24, // Cho phép list cuộn sát mép
    },
    avatarListContent: {
        paddingHorizontal: 24,
    },
    avatarWrapper: {
        marginRight: 16,
        padding: 3,
        borderRadius: 40,
        borderWidth: 2,
        borderColor: 'transparent',
    },
    selectedAvatarWrapper: {
        borderColor: '#6366f1', // Indigo primary
    },
    avatarImage: {
        width: 64,
        height: 64,
        borderRadius: 32,
        backgroundColor: '#f1f5f9',
    },
    checkBadge: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        width: 18,
        height: 18,
        borderRadius: 9,
        backgroundColor: '#6366f1',
        borderWidth: 2,
        borderColor: 'white',
    },
    input: {
        width: '100%',
        height: 56,
        backgroundColor: '#f8fafc',
        borderRadius: 16,
        paddingHorizontal: 20,
        fontSize: 16,
        color: '#1e293b',
        borderWidth: 1,
        borderColor: '#e2e8f0',
        marginBottom: 28,
    },
    buttonRow: {
        flexDirection: 'row',
        gap: 12,
    },
    button: {
        flex: 1,
        height: 56,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
    },
    buttonCancel: {
        backgroundColor: '#f1f5f9',
    },
    buttonSave: {
        backgroundColor: '#6366f1',
        shadowColor: '#6366f1',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    cancelText: {
        color: '#64748b',
        fontWeight: '600',
        fontSize: 16,
    },
    saveText: {
        color: 'white',
        fontWeight: '700',
        fontSize: 16,
    },
});