#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_name_raev_kaloyan_hellostorj_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
