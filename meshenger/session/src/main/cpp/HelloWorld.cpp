#include <jni.h>
#include <string>

extern "C" {
    JNIEXPORT jstring
    JNICALL
    Java_com_meshenger_backend_session_HelloWorldBridge_getMessage(JNIEnv *env, jobject /* this */) {
        std::string hello = "Hello from C++ (session layer)";
        return env->NewStringUTF(hello.c_str());
    }
}