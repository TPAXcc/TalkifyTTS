package com.github.lonepheasantwarrior.talkify.util

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * PCM 16-bit 单声道保调时间拉伸器
 *
 * 基于 WSOLA (Waveform Similarity Overlap-Add) 算法实现
 * 每帧搜索与前一帧尾部波形最相似的对齐位置，再做 Hann 窗 OLA 累加
 * 在改变播放速度的同时保持音调和音色不变
 *
 * 适用于 TTS 语音场景，支持 0.5x ~ 5.0x 的速度范围
 *
 * @property speedRatio 速度比率，1.0 为原始速度，>1 加速，<1 减速
 */
class PcmTimeStretcher(private val speedRatio: Float) {

    companion object {
        private const val FRAME_SIZE = 400
        private const val HOP_SIZE = 200
        private const val SEARCH_RADIUS = 40
        private const val MIN_SPEED = 0.5f
        private const val MAX_SPEED = 5.0f
        private const val ACCUM_SIZE = FRAME_SIZE * 4
    }

    private val effectiveSpeed: Float = speedRatio.coerceIn(MIN_SPEED, MAX_SPEED)

    val needsProcessing: Boolean get() = effectiveSpeed != 1.0f

    private var inputBuffer = ShortArray(0)
    private var inputPos = 0

    private val accumBuffer = FloatArray(ACCUM_SIZE)
    private var writePos = 0L
    private var readPos = 0L

    private var prevFrameRaw = FloatArray(0)

    private val hannWindow = FloatArray(FRAME_SIZE) { i ->
        (0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * i / (FRAME_SIZE - 1)))).toFloat()
    }

    fun process(inputPcm: ByteArray): ByteArray {
        if (!needsProcessing) return inputPcm

        val newSamples = bytesToShorts(inputPcm)
        if (inputPos > 0) {
            val remaining = inputBuffer.copyOfRange(inputPos, inputBuffer.size)
            inputBuffer = remaining + newSamples
            inputPos = 0
        } else {
            inputBuffer = inputBuffer + newSamples
        }

        val analysisHop = (HOP_SIZE * effectiveSpeed).roundToInt().coerceAtLeast(1)
        val estimatedOutput = (inputBuffer.size * 2 / analysisHop + 2) * HOP_SIZE
        val output = ByteArray(estimatedOutput * 2)
        var outIdx = 0

        while (inputPos + FRAME_SIZE <= inputBuffer.size) {
            val bestOffset = if (prevFrameRaw.isEmpty()) {
                0
            } else {
                findBestOffset(inputBuffer, inputPos, prevFrameRaw)
            }

            val frameStart = (inputPos + bestOffset).coerceAtLeast(0)
                .coerceAtMost(maxOf(0, inputBuffer.size - FRAME_SIZE))

            val rawFrame = FloatArray(FRAME_SIZE) { i ->
                inputBuffer[frameStart + i].toFloat() / Short.MAX_VALUE
            }

            for (i in 0 until FRAME_SIZE) {
                accumBuffer[(writePos.toInt() + i) % ACCUM_SIZE] += rawFrame[i] * hannWindow[i]
            }

            prevFrameRaw = rawFrame.copyOf()

            writePos += HOP_SIZE
            inputPos += analysisHop

            while (readPos + HOP_SIZE <= writePos) {
                val idx = (readPos % ACCUM_SIZE.toLong()).toInt()
                val value = accumBuffer[idx]
                accumBuffer[idx] = 0f
                val sample = (value * Short.MAX_VALUE)
                    .roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                if (outIdx + 2 <= output.size) {
                    output[outIdx] = (sample and 0xFF).toByte()
                    output[outIdx + 1] = (sample shr 8).toByte()
                    outIdx += 2
                }
                readPos++
            }
        }

        if (inputPos < inputBuffer.size) {
            inputBuffer = inputBuffer.copyOfRange(inputPos, inputBuffer.size)
        } else {
            inputBuffer = ShortArray(0)
        }
        inputPos = 0

        return if (outIdx == output.size) output else output.copyOf(outIdx)
    }

    private fun findBestOffset(buffer: ShortArray, currentPos: Int, prevFrame: FloatArray): Int {
        val tailSize = min(HOP_SIZE, prevFrame.size)
        var bestOffset = 0
        var bestCorrelation = -1f

        for (offset in -SEARCH_RADIUS..SEARCH_RADIUS) {
            val candidatePos = currentPos + offset
            if (candidatePos < 0 || candidatePos + tailSize > buffer.size) continue

            var dotProduct = 0f
            var normA = 0f
            var normB = 0f

            for (i in 0 until tailSize) {
                val a = prevFrame[FRAME_SIZE - tailSize + i]
                val b = buffer[candidatePos + i].toFloat() / Short.MAX_VALUE
                dotProduct += a * b
                normA += a * a
                normB += b * b
            }

            if (normA > 0f && normB > 0f) {
                val correlation = dotProduct / (sqrt(normA) * sqrt(normB))
                if (correlation > bestCorrelation) {
                    bestCorrelation = correlation
                    bestOffset = offset
                }
            }
        }

        return bestOffset
    }

    fun reset() {
        inputBuffer = ShortArray(0)
        inputPos = 0
        accumBuffer.fill(0f)
        writePos = 0L
        readPos = 0L
        prevFrameRaw = FloatArray(0)
    }

    private fun bytesToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            shorts[i] = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
        return shorts
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8).toByte()
        }
        return bytes
    }
}
