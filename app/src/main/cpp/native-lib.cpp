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
    return (!cainfo_path) ? NULL : env->GetStringUTFChars(cainfo_path, NULL);
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

static void get_buckets_callback(uv_work_t *work_req, int status)
{
    assert(status == 0);
    get_buckets_request_t *req = (get_buckets_request_t *) work_req->data;
    jcallback_t *jcallback = (jcallback_t *) req->handle;
    JNIEnv *env = jcallback->env;
    jobject callbackObject = jcallback->callbackObject;

    if (req->error_code || req->response == NULL) {
        storj_free_get_buckets_request(req);
        free(work_req);
        char error_message[256];
        if (req->status_code == 401) {
            strcpy(error_message, "Invalid user credentials");
        } else if (req->status_code != 200 && req->status_code != 304) {
            sprintf(error_message, "Request failed with status code: %i", req->status_code);
        }
        error_callback(env, callbackObject, error_message);
        return;
    }

    jclass bucketClass = env->FindClass("name/raev/kaloyan/hellostorj/jni/Bucket");
    jobjectArray bucketArray = env->NewObjectArray(req->total_buckets, bucketClass, NULL);
    jmethodID bucketInit = env->GetMethodID(bucketClass,
                                            "<init>",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");

    for (int i = 0; i < req->total_buckets; i++) {
        storj_bucket_meta_t *bucket = &req->buckets[i];
        jobject bucketObject = env->NewObject(bucketClass,
                                              bucketInit,
                                              env->NewStringUTF(bucket->id),
                                              env->NewStringUTF(bucket->name),
                                              env->NewStringUTF(bucket->created),
                                              bucket->decrypted);
        env->SetObjectArrayElement(bucketArray, i, bucketObject);
    }

    jclass callbackClass = env->GetObjectClass(callbackObject);
    jmethodID callbackMethod = env->GetMethodID(callbackClass,
                                                "onBucketsReceived",
                                                "([Lname/raev/kaloyan/hellostorj/jni/Bucket;)V");
    env->CallVoidMethod(callbackObject, callbackMethod, bucketArray);

    json_object_put(req->response);
    storj_free_get_buckets_request(req);
    free(work_req);
}

extern "C"
JNIEXPORT void JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_getBuckets(
        JNIEnv *env,
        jclass clazz,
        jstring user,
        jstring pass,
        jstring mnemonic,
        jobject callbackObject) {
    const char *user_c = env->GetStringUTFChars(user, NULL);
    const char *pass_c = env->GetStringUTFChars(pass, NULL);
    const char *mnemonic_c = env->GetStringUTFChars(mnemonic, NULL);

    storj_http_options_t http_options = {
            .user_agent = "Hello Storj",
            .cainfo_path = get_cainfo_path(env, clazz),
            .low_speed_limit = STORJ_LOW_SPEED_LIMIT,
            .low_speed_time = STORJ_LOW_SPEED_TIME,
            .timeout = STORJ_HTTP_TIMEOUT
    };

    storj_bridge_options_t options = {
            .proto = "https",
            .host  = "api.storj.io",
            .port  = 443,
            .user  = user_c,
            .pass  = pass_c
    };

    storj_encrypt_options_t encrypt_options = {
            .mnemonic = mnemonic_c
    };

    storj_log_options_t log_options = {
            .logger = NULL,
            .level = 0
    };

    storj_env_t *storj_env = NULL;
    storj_env = storj_init_env(&options, &encrypt_options, &http_options, &log_options);
    if (!storj_env) {
        error_callback(env, callbackObject, "Cannot initialize Storj environment");
    }

    jcallback_t jcallback = {
            .env = env,
            .callbackObject = callbackObject
    };
    storj_bridge_get_buckets(storj_env, &jcallback, get_buckets_callback);

    uv_run(storj_env->loop, UV_RUN_DEFAULT);

    if (storj_env) {
        storj_destroy_env(storj_env);
    }
    env->ReleaseStringUTFChars(user, user_c);
    env->ReleaseStringUTFChars(pass, pass_c);
    env->ReleaseStringUTFChars(mnemonic, mnemonic_c);
}

static void list_files_callback(uv_work_t *work_req, int status)
{
    assert(status == 0);
    list_files_request_t *req = (list_files_request_t *) work_req->data;
    jcallback_t *jcallback = (jcallback_t *) req->handle;
    JNIEnv *env = jcallback->env;
    jobject callbackObject = jcallback->callbackObject;

    if (req->status_code != 200) {
        storj_free_list_files_request(req);
        free(work_req);
        char error_message[256];
        if (req->status_code == 404) {
            sprintf(error_message, "Bucket id [%s] does not exist", req->bucket_id);
        } else if (req->status_code == 400) {
            sprintf(error_message, "Bucket id [%s] is invalid", req->bucket_id);
        } else if (req->status_code == 401) {
            strcpy(error_message, "Invalid user credentials");
        } else {
            sprintf(error_message, "Request failed with status code: %i", req->status_code);
        }
        error_callback(env, callbackObject, error_message);
        return;
    }

    jclass fileClass = env->FindClass("name/raev/kaloyan/hellostorj/jni/File");
    jobjectArray fileArray = env->NewObjectArray(req->total_files, fileClass, NULL);
    jmethodID fileInit = env->GetMethodID(fileClass,
                                          "<init>",
                                          "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    for (int i = 0; i < req->total_files; i++) {
        storj_file_meta_t *file = &req->files[i];
        jobject fileObject = env->NewObject(fileClass,
                                            fileInit,
                                            env->NewStringUTF(file->id),
                                            env->NewStringUTF(file->filename),
                                            env->NewStringUTF(file->created),
                                            file->decrypted,
                                            file->size,
                                            env->NewStringUTF(file->mimetype),
                                            NULL, //env->NewStringUTF(file->erasure),
                                            NULL, //env->NewStringUTF(file->index),
                                            NULL); //env->NewStringUTF(file->hmac));
        env->SetObjectArrayElement(fileArray, i, fileObject);
    }

    jclass callbackClass = env->GetObjectClass(callbackObject);
    jmethodID callbackMethod = env->GetMethodID(callbackClass,
                                                "onFilesReceived",
                                                "([Lname/raev/kaloyan/hellostorj/jni/File;)V");
    env->CallVoidMethod(callbackObject, callbackMethod, fileArray);

    json_object_put(req->response);
    storj_free_list_files_request(req);
    free(work_req);
}

extern "C"
JNIEXPORT void JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_listFiles(
        JNIEnv *env,
        jclass clazz,
        jstring user_,
        jstring pass_,
        jstring mnemonic_,
        jstring bucketId_,
        jobject callbackObject) {
    const char *user = env->GetStringUTFChars(user_, 0);
    const char *pass = env->GetStringUTFChars(pass_, 0);
    const char *mnemonic = env->GetStringUTFChars(mnemonic_, 0);
    const char *bucketId = env->GetStringUTFChars(bucketId_, 0);

    storj_http_options_t http_options = {
            .user_agent = "Hello Storj",
            .cainfo_path = get_cainfo_path(env, clazz),
            .low_speed_limit = STORJ_LOW_SPEED_LIMIT,
            .low_speed_time = STORJ_LOW_SPEED_TIME,
            .timeout = STORJ_HTTP_TIMEOUT
    };

    storj_bridge_options_t options = {
            .proto = "https",
            .host  = "api.storj.io",
            .port  = 443,
            .user  = user,
            .pass  = pass
    };

    storj_encrypt_options_t encrypt_options = {
            .mnemonic = mnemonic
    };

    storj_log_options_t log_options = {
            .logger = NULL,
            .level = 0
    };

    storj_env_t *storj_env = NULL;
    storj_env = storj_init_env(&options, &encrypt_options, &http_options, &log_options);
    if (!storj_env) {
        error_callback(env, callbackObject, "Cannot initialize Storj environment");
    }

    jcallback_t jcallback = {
            .env = env,
            .callbackObject = callbackObject
    };
    storj_bridge_list_files(storj_env, bucketId, &jcallback, list_files_callback);

    uv_run(storj_env->loop, UV_RUN_DEFAULT);

    if (storj_env) {
        storj_destroy_env(storj_env);
    }

    env->ReleaseStringUTFChars(user_, user);
    env->ReleaseStringUTFChars(pass_, pass);
    env->ReleaseStringUTFChars(mnemonic_, mnemonic);
    env->ReleaseStringUTFChars(bucketId_, bucketId);
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

    if (storj_env) {
        storj_destroy_env(storj_env);
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_exportKeys(
        JNIEnv *env,
        jclass type,
        jstring location_,
        jstring passphrase_) {
    const char *location = env->GetStringUTFChars(location_, 0);
    const char *passphrase = env->GetStringUTFChars(passphrase_, 0);
    char *user = NULL;
    char *pass = NULL;
    char *mnemonic = NULL;

    jobject keysObject = NULL;
    if (!storj_decrypt_read_auth(location, passphrase, &user, &pass, &mnemonic)) {
        jclass keysClass = env->FindClass("name/raev/kaloyan/hellostorj/jni/Keys");
        jmethodID keysInit = env->GetMethodID(keysClass,
                                              "<init>",
                                              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        keysObject = env->NewObject(keysClass,
                                    keysInit,
                                    env->NewStringUTF(user),
                                    env->NewStringUTF(pass),
                                    env->NewStringUTF(mnemonic));
    }

    if (user) {
        free(user);
    }
    if (pass) {
        free(pass);
    }
    if (mnemonic) {
        free(mnemonic);
    }
    env->ReleaseStringUTFChars(location_, location);
    env->ReleaseStringUTFChars(passphrase_, passphrase);

    return keysObject;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_writeAuthFile(
        JNIEnv *env,
        jclass /* clazz */,
        jstring location,
        jstring user,
        jstring pass,
        jstring mnemonic,
        jstring passphrase) {
    const char *location_c = env->GetStringUTFChars(location, NULL);
    const char *user_c = env->GetStringUTFChars(user, NULL);
    const char *pass_c = env->GetStringUTFChars(pass, NULL);
    const char *mnemonic_c = env->GetStringUTFChars(mnemonic, NULL);
    const char *passphrase_c = env->GetStringUTFChars(passphrase, NULL);

    int result = storj_encrypt_write_auth(location_c, passphrase_c, user_c, pass_c, mnemonic_c);

    env->ReleaseStringUTFChars(location, location_c);
    env->ReleaseStringUTFChars(user, user_c);
    env->ReleaseStringUTFChars(pass, pass_c);
    env->ReleaseStringUTFChars(mnemonic, mnemonic_c);
    env->ReleaseStringUTFChars(passphrase, passphrase_c);

    return result == 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_checkMnemonic(
        JNIEnv *env,
        jclass /* clazz */,
        jstring mnemonic_) {
    const char *mnemonic = env->GetStringUTFChars(mnemonic_, NULL);
    bool result = storj_mnemonic_check(mnemonic);
    env->ReleaseStringUTFChars(mnemonic_, mnemonic);
    return result;
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
JNIEXPORT jlong JNICALL
Java_name_raev_kaloyan_hellostorj_jni_Storj_getTimestamp(
        JNIEnv *env,
        jclass /* clazz */) {
    return storj_util_timestamp();
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
