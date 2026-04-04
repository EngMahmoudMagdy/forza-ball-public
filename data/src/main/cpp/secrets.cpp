#include <jni.h>
#include <string>
#include "obfuscated_secrets.h"

static std::string decode(const unsigned char *data, size_t len) {
    if (len == 0) {
        return "";
    }
    std::string out(len, '\0');
    for (size_t i = 0; i < len; i++) {
        out[i] = static_cast<char>(data[i] ^ FORZA_XOR_MASK);
    }
    return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_forzaball_data_secrets_FootballSecrets_nativeApiKey(JNIEnv *env, jclass) {
    std::string s = decode(API_KEY_OBF, static_cast<size_t>(API_KEY_OBF_LEN));
    return env->NewStringUTF(s.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_forzaball_data_secrets_FootballSecrets_nativeBaseUrl(JNIEnv *env, jclass) {
    std::string s = decode(BASE_URL_OBF, static_cast<size_t>(BASE_URL_OBF_LEN));
    return env->NewStringUTF(s.c_str());
}
