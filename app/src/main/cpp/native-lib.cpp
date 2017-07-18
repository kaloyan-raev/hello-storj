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

typedef struct {
    JNIEnv *env;
    jobject callbackObject;
} jcallback_t;

const char *get_cainfo_path(JNIEnv *env, jclass clazz) {
    jfieldID field = env->GetStaticFieldID(clazz, "caInfoPath", "Ljava/lang/String;");
    jstring cainfo_path = (jstring) env->GetStaticObjectField(clazz, field);
    return env->GetStringUTFChars(cainfo_path, NULL);
}

static void error_callback(JNIEnv *env, jobject callbackObject, const char *message) {
    jclass callbackClass = env->GetObjectClass(callbackObject);
    jmethodID callbackMethod = env->GetMethodID(callbackClass,
                                                "onError",
                                                "(Ljava/lang/String;)V");
    env->CallVoidMethod(callbackObject,
                        callbackMethod,
                        env->NewStringUTF(message));
}

static void get_info_callback(uv_work_t *work_req, int status)
{
    assert(status == 0);
    json_request_t *req = (json_request_t *) work_req->data;
    jcallback_t *jcallback = (jcallback_t *) req->handle;
    JNIEnv *env = jcallback->env;
    jobject callbackObject = jcallback->callbackObject;

    if (req->error_code || req->response == NULL) {
        free(req);
        free(work_req);
        char error_message[256];
        if (req->error_code) {
            sprintf(error_message, "Request failed, reason: %s",
                   curl_easy_strerror((CURLcode) req->error_code));
        } else {
            strcpy(error_message, "Failed to get info.");
        }
        error_callback(env, callbackObject, error_message);
        return;
    }

    struct json_object *info;
    json_object_object_get_ex(req->response, "info", &info);

    struct json_object *title;
    json_object_object_get_ex(info, "title", &title);
    struct json_object *description;
    json_object_object_get_ex(info, "description", &description);
    struct json_object *version;
    json_object_object_get_ex(info, "version", &version);
    struct json_object *host;
    json_object_object_get_ex(req->response, "host", &host);

    jclass callbackClass = env->GetObjectClass(callbackObject);
    jmethodID callbackMethod = env->GetMethodID(callbackClass,
                                             "onInfoReceived",
                                             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    env->CallVoidMethod(callbackObject,
                        callbackMethod,
                        env->NewStringUTF(json_object_get_string(title)),
                        env->NewStringUTF(json_object_get_string(description)),
                        env->NewStringUTF(json_object_get_string(version)),
                        env->NewStringUTF(json_object_get_string(host)));

    json_object_put(req->response);
    free(req);
    free(work_req);
}

extern "C"
JNIEXPORT void JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_getInfo(
        JNIEnv *env,
        jclass clazz,
        jobject callbackObject) {
    storj_bridge_options_t options = {
            .proto = "https",
            .host  = "api.storj.io",
            .port  = 443,
            .user  = NULL,
            .pass  = NULL
    };

    storj_http_options_t http_options = {
            .user_agent = "Hello Storj",
            .cainfo_path = get_cainfo_path(env, clazz),
            .low_speed_limit = STORJ_LOW_SPEED_LIMIT,
            .low_speed_time = STORJ_LOW_SPEED_TIME,
            .timeout = STORJ_HTTP_TIMEOUT
    };

    storj_log_options_t log_options = {
            .logger = NULL,
            .level = 0
    };

    storj_env_t *storj_env = NULL;
    storj_env = storj_init_env(&options, NULL, &http_options, &log_options);
    if (!storj_env) {
        error_callback(env, callbackObject, "Cannot initialize Storj environment");
    }

    jcallback_t jcallback = {
            .env = env,
            .callbackObject = callbackObject
    };
    storj_bridge_get_info(storj_env, &jcallback, get_info_callback);

    uv_run(storj_env->loop, UV_RUN_DEFAULT);
}

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
