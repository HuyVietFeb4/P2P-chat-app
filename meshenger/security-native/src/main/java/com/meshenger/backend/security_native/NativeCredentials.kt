package com.meshenger.backend.security_native

object NativeCredentials {

    init {
        // This must match the name defined in your CMakeLists.txt add_library
        System.loadLibrary("security_native")
    }

    /**
     * Retrieves the secret key from the C++ layer.
     * Note: Context is not passed as per your request.
     */
    external fun getAppSecretKey(): String
    external fun getGlobalChatKey(): String
    external fun getTwoPartyChatKey(): String
}