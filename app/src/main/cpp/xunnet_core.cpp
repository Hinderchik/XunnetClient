#include <jni.h>
#include <android/log.h>

#define LOG_TAG "XunnetCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_dev_xunnet_client_core_SingBoxCore_nativeGetVersion(JNIEnv *env, jobject /* this */) {
    LOGI("sing-box core version request");
    return env->NewStringUTF("1.8.0");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_xunnet_client_core_SingBoxCore_nativeStart(JNIEnv *env, jobject /* this */, jstring config) {
    const char *cfg = env->GetStringUTFChars(config, nullptr);
    LOGI("Starting sing-box with config: %s", cfg);
    env->ReleaseStringUTFChars(config, cfg);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_xunnet_client_core_SingBoxCore_nativeStop(JNIEnv *env, jobject /* this */) {
    LOGI("Stopping sing-box");
    return JNI_TRUE;
}
