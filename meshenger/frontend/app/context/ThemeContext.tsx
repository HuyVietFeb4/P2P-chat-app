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
    cardBg: '#F0F9FF'
  },
  dark: {
    background: '#313338', // Discord-like gray background
    text: '#F2F3F5',
    subText: '#B5BAC1',
    card: '#2B2D31', // Discord-like card color
    border: '#1E1F22',
    primary: '#5865F2', // Discord-like blurple
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
    cardBg: '#243447'
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
