#include <jni.h>
#include "sonic.h"

JNIEXPORT jlong JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeCreate(
        JNIEnv *env, jobject thiz, jint sampleRate, jint numChannels) {
    return (jlong) sonicCreateStream(sampleRate, numChannels);
}

JNIEXPORT void JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeDestroy(
        JNIEnv *env, jobject thiz, jlong handle) {
    sonicDestroyStream((sonicStream) handle);
}

JNIEXPORT void JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeSetSpeed(
        JNIEnv *env, jobject thiz, jlong handle, jfloat speed) {
    sonicSetSpeed((sonicStream) handle, speed);
}

JNIEXPORT void JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeSetPitch(
        JNIEnv *env, jobject thiz, jlong handle, jfloat pitch) {
    sonicSetPitch((sonicStream) handle, pitch);
}

JNIEXPORT void JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeSetVolume(
        JNIEnv *env, jobject thiz, jlong handle, jfloat volume) {
    sonicSetVolume((sonicStream) handle, volume);
}

JNIEXPORT jboolean JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeWrite(
        JNIEnv *env, jobject thiz, jlong handle, jshortArray samples, jint numSamples) {
    jshort *buf = (*env)->GetShortArrayElements(env, samples, NULL);
    jboolean result = sonicWriteShortToStream((sonicStream) handle, buf, numSamples)
                      ? JNI_TRUE : JNI_FALSE;
    (*env)->ReleaseShortArrayElements(env, samples, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jshortArray JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeRead(
        JNIEnv *env, jobject thiz, jlong handle, jint maxSamples) {
    int available = sonicSamplesAvailable((sonicStream) handle);
    int toRead = available < maxSamples ? available : maxSamples;
    if (toRead == 0) return NULL;

    jshortArray result = (*env)->NewShortArray(env, toRead);
    if (result == NULL) return NULL;

    jshort *buf = (*env)->GetShortArrayElements(env, result, NULL);
    sonicReadShortFromStream((sonicStream) handle, buf, toRead);
    (*env)->ReleaseShortArrayElements(env, result, buf, 0);
    return result;
}

JNIEXPORT void JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeFlush(
        JNIEnv *env, jobject thiz, jlong handle) {
    sonicFlushStream((sonicStream) handle);
}

JNIEXPORT jint JNICALL
Java_com_github_lonepheasantwarrior_talkify_util_SonicBridge_nativeSamplesAvailable(
        JNIEnv *env, jobject thiz, jlong handle) {
    return sonicSamplesAvailable((sonicStream) handle);
}
