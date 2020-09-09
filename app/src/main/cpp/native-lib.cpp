#include <jni.h>
#include <string>

#include "Log.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_xyz_panyi_simpleplayer_NativeBridge_stringFromJNI(JNIEnv *env, jclass clazz) {
    std::string hello = "Panyis2222";
    LOGI("Hello World!!");
    LOGI("你好 世界");
    return env->NewStringUTF(hello.c_str());
}