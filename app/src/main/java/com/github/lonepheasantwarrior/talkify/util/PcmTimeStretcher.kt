package com.github.lonepheasantwarrior.talkify.util

/**
 * PCM 16-bit 单声道时间拉伸器
 *
 * 基于 Sonic 库实现，使用 PICOLA 算法进行基音同步时间拉伸
 * 在改变播放速度的同时保持音调和音色不变
 *
 * 适用于 TTS 语音场景，支持 0.5x ~ 5.0x 的速度范围
 *
 * @property speedRatio 速度比率，1.0 为原始速度，>1 加速，<1 减速
 */
class PcmTimeStretcher(private val speedRatio: Float) {

    companion object {
        private const val MIN_SPEED = 0.5f
        private const val MAX_SPEED = 5.0f
        private const val SAMPLE_RATE = 24000
        private const val READ_BUFFER_SIZE = 8192
    }

    private val effectiveSpeed: Float = speedRatio.coerceIn(MIN_SPEED, MAX_SPEED)

    val needsProcessing: Boolean get() = effectiveSpeed != 1.0f

    private var sonic: SonicBridge? = null

    fun process(inputPcm: ByteArray): ByteArray {
        if (!needsProcessing) return inputPcm

        val s = getOrCreateSonic()
        val samples = bytesToShorts(inputPcm)
        s.write(samples, samples.size)

        val output = ByteArray(READ_BUFFER_SIZE * 2)
        var outIdx = 0

        while (s.samplesAvailable() > 0) {
            val chunk = s.read(READ_BUFFER_SIZE) ?: break
            for (sample in chunk) {
                if (outIdx + 2 <= output.size) {
                    output[outIdx] = (sample.toInt() and 0xFF).toByte()
                    output[outIdx + 1] = (sample.toInt() shr 8).toByte()
                    outIdx += 2
                }
            }
        }

        return if (outIdx == output.size) output else output.copyOf(outIdx)
    }

    private fun getOrCreateSonic(): SonicBridge {
        return sonic ?: SonicBridge(SAMPLE_RATE, 1).also {
            it.setSpeed(effectiveSpeed)
            it.setPitch(1.0f)
            it.setVolume(1.0f)
            sonic = it
        }
    }

    fun reset() {
        sonic?.release()
        sonic = null
    }

    private fun bytesToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            shorts[i] = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
        return shorts
    }
}
