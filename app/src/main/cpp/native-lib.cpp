#include <jni.h>
#include <string>
#include <storj.h>
#include <nettle/version.h>
#include <microhttpd/microhttpd.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_name_raev_kaloyan_hellostorj_Storj_getTimestamp(
        JNIEnv *env,
        jclass /* clazz */) {
    return storj_util_timestamp();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_Storj_generateMnemonic(
        JNIEnv *env,
        jclass /* clazz */,
        jint strength) {
    char *mnemonic = NULL;
    storj_mnemonic_generate(strength, &mnemonic);
    return env->NewStringUTF(mnemonic);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_NativeLibraries_getJsonCVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(json_c_version());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_NativeLibraries_getCurlVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(curl_version());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_NativeLibraries_getLibuvVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(uv_version_string());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_NativeLibraries_getNettleVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    char version[5];
    sprintf(version, "%d.%d", nettle_version_major(), nettle_version_minor());
    return env->NewStringUTF(version);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_NativeLibraries_getMHDVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(MHD_get_version());
}
