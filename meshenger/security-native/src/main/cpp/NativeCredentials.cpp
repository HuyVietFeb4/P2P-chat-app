//
// Created by Viet on 07/03/2026.
//

#include <jni.h>
#include <string>
//#include "signature-check.cpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_meshenger_backend_security_1native_NativeCredentials_getAppSecretKey(
        JNIEnv* env,
        jobject thiz,
        jobject context) {
            
    unsigned char hidden_key[] = {
            0x4D, 0x65, 0x73, 0x68, 0x65, 0x6E, 0x67, 0x65, 0x72, 0x5F, 
            0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x5F, 0x41, 0x70, 0x70, 
            0x5F, 0x4B, 0x65, 0x79
    };

    // The length is 29 bytes
    int key_len = 24;
    char mask = 0x74;

    std::string encrypted = "";
    for (int i = 0; i < key_len; i++) {
        encrypted += (char)(hidden_key[i] ^ mask);
    }

    return env->NewStringUTF(encrypted.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_meshenger_backend_security_1native_NativeCredentials_getGlobalChatKey(
        JNIEnv* env,
        jobject thiz,
        jobject context) {
            
    unsigned char hidden_key[] = {
            0x4D, 0x65, 0x73, 0x68, 0x65, 0x6E, 0x67, 0x65, 0x72, 0x5F,
            0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x5F, 0x41, 0x6C, 0x6C,
            0x5F, 0x43, 0x68, 0x61, 0x74, 0x5F, 0x4B, 0x65, 0x79
    };

    // The length is 29 bytes
    int key_len = 29;
    char mask = 0x42;

    std::string encrypted = "";
    for (int i = 0; i < key_len; i++) {
        encrypted += (char)(hidden_key[i] ^ mask);
    }

    return env->NewStringUTF(encrypted.c_str());
}