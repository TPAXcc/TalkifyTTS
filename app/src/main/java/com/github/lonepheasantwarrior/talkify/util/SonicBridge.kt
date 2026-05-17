package com.github.lonepheasantwarrior.talkify.util

/**
 * Sonic 时间拉伸库的 JNI 桥接
 *
 * 基于 Google 的 Sonic 算法实现高质量语音时间拉伸
 * 使用 PICOLA 算法进行基音同步重采样，高速下无金属音
 */
class SonicBridge(sampleRate: Int, numChannels: Int = 1) {

    private var handle: Long = 0

    init {
        handle = nativeCreate(sampleRate, numChannels)
    }

    fun setSpeed(speed: Float) {
        if (handle != 0L) nativeSetSpeed(handle, speed)
    }

    fun setPitch(pitch: Float) {
        if (handle != 0L) nativeSetPitch(handle, pitch)
    }

    fun setVolume(volume: Float) {
        if (handle != 0L) nativeSetVolume(handle, volume)
    }

    fun write(samples: ShortArray, numSamples: Int): Boolean {
        if (handle == 0L) return false
        return nativeWrite(handle, samples, numSamples)
    }

    fun read(maxSamples: Int): ShortArray? {
        if (handle == 0L) return null
        return nativeRead(handle, maxSamples)
    }

    fun flush() {
        if (handle != 0L) nativeFlush(handle)
    }

    fun samplesAvailable(): Int {
        if (handle == 0L) return 0
        return nativeSamplesAvailable(handle)
    }

    fun release() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun nativeCreate(sampleRate: Int, numChannels: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeSetSpeed(handle: Long, speed: Float)
    private external fun nativeSetPitch(handle: Long, pitch: Float)
    private external fun nativeSetVolume(handle: Long, volume: Float)
    private external fun nativeWrite(handle: Long, samples: ShortArray, numSamples: Int): Boolean
    private external fun nativeRead(handle: Long, maxSamples: Int): ShortArray?
    private external fun nativeFlush(handle: Long)
    private external fun nativeSamplesAvailable(handle: Long): Int

    companion object {
        init {
            System.loadLibrary("sonic")
        }
    }
}
