package com.rihlahidali.advdisplay

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.SystemClock
import java.io.File

/** Generates a small real H.264 clip on the device; no downloaded or licensed test media. */
object FixtureVideo {
    fun create(file: File): ByteArray {
        val codec = MediaCodec.createEncoderByType("video/avc")
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val format = MediaFormat.createVideoFormat("video/avc", 160, 120).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 64000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 10)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            var frame = 0
            var track = -1
            val info = MediaCodec.BufferInfo()
            val end = SystemClock.elapsedRealtime() + 15000
            while (SystemClock.elapsedRealtime() < end) {
                if (frame <= 20) {
                    val input = codec.dequeueInputBuffer(10000)
                    if (input >= 0) {
                        val data = ByteArray(160 * 120 * 3 / 2) { 128.toByte() }
                        requireNotNull(codec.getInputBuffer(input)).put(data)
                        codec.queueInputBuffer(input, 0, if (frame == 20) 0 else data.size,
                            frame * 100000L, if (frame == 20) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        frame++
                    }
                }
                val output = codec.dequeueOutputBuffer(info, 10000)
                if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (output >= 0) {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        muxer.writeSampleData(track, requireNotNull(codec.getOutputBuffer(output)), info)
                    }
                    codec.releaseOutputBuffer(output, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        } finally {
            codec.stop()
            codec.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
        }
        return file.readBytes()
    }
}
