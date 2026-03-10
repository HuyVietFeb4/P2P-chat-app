//
// Created by Viet on 07/03/2026.
//
#include <jni.h>
#include <string>
#include <vector>

// The SHA-256 hash of your official release certificate (Example)
const char* AUTHORIZED_SIGNATURE_HASH = "A1:B2:C3:D4...YOUR_ACTUAL_HASH";

bool verifyAppSignature(JNIEnv* env, jobject context) {
    // 1. Get Context.getPackageManager()
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPmMethod = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPmMethod);

    // 2. Get Context.getPackageName()
    jmethodID getPackageNameMethod = env->GetMethodID(contextClass, "getPackageName", "()Ljava/lang/String;");
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageNameMethod);

    // 3. Get PackageInfo (GET_SIGNATURES = 64)
    jclass pmClass = env->GetObjectClass(packageManager);
    jmethodID getPackageInfoMethod = env->GetMethodID(pmClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfoMethod, packageName, 64);

    // 4. In a real app, you would now extract the signature bytes,
    // run them through SHA-256, and compare to AUTHORIZED_SIGNATURE_HASH.
    // For this example, we'll assume it returns true.
    return true;
}