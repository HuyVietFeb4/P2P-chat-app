import React, { useState } from 'react';
import { 
  StyleSheet, 
  Text, 
  View, 
  Modal, 
  TextInput, 
  TouchableOpacity, 
  Image, 
  FlatList 
} from 'react-native';

export default function InfoPopUp() {
    const AVATARS = [
        { id: '1', url: 'https://i.pravatar.cc/150?u=1' },
        { id: '2', url: 'https://i.pravatar.cc/150?u=2' },
        { id: '3', url: 'https://i.pravatar.cc/150?u=3' },
        { id: '4', url: 'https://i.pravatar.cc/150?u=4' },
        { id: '5', url: 'https://i.pravatar.cc/150?u=5' },
        { id: '6', url: 'https://i.pravatar.cc/150?u=6' },
    ];

    const [modalVisible, setModalVisible] = useState(false);
    const [username, setUsername] = useState('');
    const [selectedAvatar, setSelectedAvatar] = useState(AVATARS[0].url);

    const handleSave = () => {
        console.log("Username:", username);
        console.log("Avatar URL:", selectedAvatar);
        setModalVisible(false);
    };

    return (
        <View style={styles.container}>
        <Text style={styles.title}>Welcome to the App!</Text>
        
        <TouchableOpacity 
            style={styles.openButton} 
            onPress={() => setModalVisible(true)}
        >
            <Text style={styles.textStyle}>Thiết lập Profile</Text>
        </TouchableOpacity>

        <Modal
            animationType="slide"
            transparent={true}
            visible={modalVisible}
            onRequestClose={() => setModalVisible(false)}
        >
            <View style={styles.centeredView}>
            <View style={styles.modalView}>
                <Text style={styles.modalTitle}>Thông tin cá nhân</Text>

                {/* Phần chọn Avatar */}
                <Text style={styles.label}>Chọn Avatar:</Text>
                <FlatList
                data={AVATARS}
                horizontal
                showsHorizontalScrollIndicator={false}
                keyExtractor={(item) => item.id}
                renderItem={({ item }) => (
                    <TouchableOpacity onPress={() => setSelectedAvatar(item.url)}>
                    <Image 
                        source={{ uri: item.url }} 
                        style={[
                        styles.avatarImage, 
                        selectedAvatar === item.url && styles.selectedAvatar
                        ]} 
                    />
                    </TouchableOpacity>
                )}
                style={styles.avatarList}
                />

                {/* Phần nhập Username */}
                <Text style={styles.label}>Tên người dùng:</Text>
                <TextInput
                style={styles.input}
                placeholder="Nhập tên của bạn..."
                value={username}
                onChangeText={setUsername}
                />

                {/* Nút hành động */}
                <View style={styles.buttonRow}>
                <TouchableOpacity
                    style={[styles.button, styles.buttonClose]}
                    onPress={() => setModalVisible(false)}
                >
                    <Text style={styles.textStyle}>Hủy</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={[styles.button, styles.buttonSave]}
                    onPress={handleSave}
                >
                    <Text style={styles.textStyle}>Lưu</Text>
                </TouchableOpacity>
                </View>
            </View>
            </View>
        </Modal>
        </View>
    );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    marginBottom: 20,
    fontWeight: 'bold',
  },
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)', // Làm mờ nền phía sau
  },
  modalView: {
    width: '85%',
    backgroundColor: 'white',
    borderRadius: 20,
    padding: 25,
    alignItems: 'flex-start',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 15,
    alignSelf: 'center',
  },
  label: {
    fontSize: 16,
    marginBottom: 10,
    color: '#333',
  },
  input: {
    width: '100%',
    height: 45,
    borderColor: '#ddd',
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 15,
    marginBottom: 20,
  },
  avatarList: {
    marginBottom: 20,
  },
  avatarImage: {
    width: 60,
    height: 60,
    borderRadius: 30,
    marginRight: 10,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  selectedAvatar: {
    borderColor: '#2196F3', // Màu highlight khi chọn
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
  },
  button: {
    borderRadius: 10,
    padding: 12,
    elevation: 2,
    width: '45%',
    alignItems: 'center',
  },
  buttonClose: {
    backgroundColor: '#ff5c5c',
  },
  buttonSave: {
    backgroundColor: '#2196F3',
  },
  openButton: {
    backgroundColor: '#4CAF50',
    padding: 15,
    borderRadius: 10,
  },
  textStyle: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
  },
});