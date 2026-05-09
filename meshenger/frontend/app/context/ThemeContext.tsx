import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { createContext, useContext, useEffect, useState } from 'react';
import { useColorScheme } from 'react-native';

export const Colors = {
  light: {
    background: '#FFFFFF',
    text: '#000000',
    subText: '#9CA3AF',
    card: '#F3F4F6',
    border: '#E5E7EB',
    primary: '#6C47FF',
    headerGradient: ['#0F4C81', '#5F2EEA'],
    footerBg: '#FFFFFF',
    iconBg: '#EDE9FF',
    sectionTitle: '#6B7280',
    settingItemBg: '#F9FAFB',
    chatBackground: '#F0F6FF',
    myBubble: '#5D8BF4',
    peerBubble: '#FFFFFF',
    myMessageText: '#FFFFFF',
    peerMessageText: '#333333',
    returnIcon: '#4DA6FF',
    scannedBg: '#FFFFF',
    cardBg: '#F0F9FF',
    scanPopUp: '#F8F9FC',
    scanPopUpBorder: '#E3E7F0',
    scanPopUpMainText: '#5F2EEA',
    scanPopUpSubText: 'rgba(55, 48, 163, 0.9)',
    scanPopUpAccent: '#5F2EEA',
    success: {
        bg: '#DCFCE7',           // Xanh lá cực nhạt
        borderColor: '#22C55E',  // Viền xanh lá tươi
        textColor: '#14532D'
    },
    warning: {
        bg: '#FEF3C7',           // Vàng nhạt
        borderColor: '#F59E0B',
        textColor: '#78350F',    // Chữ nâu cam đậm
    },
    error: {
        bg: '#FEE2E2',           // Đỏ nhạt
        borderColor: '#EF4444',
        textColor: '#991B1B',    // Chữ đỏ đậm
    },
  },
  dark: {
    background: '#313338', 
    text: '#F2F3F5',
    subText: '#B5BAC1',
    card: '#2B2D31', 
    border: '#1E1F22',
    primary: '#5865F2', 
    headerGradient: ['#0F4C81', '#5F2EEA'],
    footerBg: '#2B2D31',
    iconBg: '#35373C',
    sectionTitle: '#949BA4',
    settingItemBg: '#2B2D31',
    chatBackground: '#1E1F22',
    myBubble: '#5865F2',
    peerBubble: '#313338',
    myMessageText: '#F2F3F5',
    peerMessageText: '#DBDEE1',
    returnIcon: 'rgba(255,255,255,0.2)',
    scannedBg: '#181A20',
    cardBg: '#243447',
    scanPopUp: '#3F4248',          
    scanPopUpBorder: '#585B64',     
    scanPopUpMainText: '#FFFFFF',   
    scanPopUpSubText: '#B5BAC1',    
    scanPopUpAccent: '#949CF7',
    success: {
        bg: '#064E3B',          
        borderColor: '#10B981',  
        textColor: '#34D399',    
    },
    warning: {
        bg: '#451A03',           
        borderColor: '#F59E0B',
        textColor: '#FBBF24',   
    },
    error: {
        bg: '#450A0A',          
        borderColor: '#EF4444',
        textColor: '#F87171',    
    },
  },
};

type ThemeContextType = {
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  colors: typeof Colors.light;
};

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const systemColorScheme = useColorScheme();
  const [isDarkMode, setIsDarkMode] = useState<boolean>(systemColorScheme === 'dark');

  useEffect(() => {
    const loadTheme = async () => {
      const savedTheme = await AsyncStorage.getItem('themePreference');
      if (savedTheme !== null) {
        setIsDarkMode(savedTheme === 'dark');
      }
    };
    loadTheme();
  }, []);

  const toggleDarkMode = async () => {
    const newValue = !isDarkMode;
    setIsDarkMode(newValue);
    await AsyncStorage.setItem('themePreference', newValue ? 'dark' : 'light');
  };

  const colors = isDarkMode ? Colors.dark : Colors.light;

  return (
    <ThemeContext.Provider value={{ isDarkMode, toggleDarkMode, colors }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

export default function ThemeContextDummy() {
  return null;
}
