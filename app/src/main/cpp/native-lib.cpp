/***************************************************************************
 * Copyright (C) 2017 Kaloyan Raev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ***************************************************************************/
#include <jni.h>
#include <string>
#include <storj.h>
#include <nettle/version.h>
#include <microhttpd/microhttpd.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_getTimestamp(
        JNIEnv *env,
        jclass /* clazz */) {
    return storj_util_timestamp();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_generateMnemonic(
        JNIEnv *env,
        jclass /* clazz */,
        jint strength) {
    char *mnemonic = NULL;
    storj_mnemonic_generate(strength, &mnemonic);
    return env->NewStringUTF(mnemonic);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_NativeLibraries_getJsonCVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(json_c_version());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_NativeLibraries_getCurlVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(curl_version());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_NativeLibraries_getLibuvVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(uv_version_string());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_NativeLibraries_getNettleVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    char version[5];
    sprintf(version, "%d.%d", nettle_version_major(), nettle_version_minor());
    return env->NewStringUTF(version);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_jni_NativeLibraries_getMHDVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF(MHD_get_version());
}
