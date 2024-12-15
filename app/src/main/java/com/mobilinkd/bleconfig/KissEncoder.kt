package com.mobilinkd.bleconfig

import android.util.Log
import java.io.OutputStream

class KissEncoder {
    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    fun encode(inputBuffer: ByteArray, output: OutputStream) {
        if (D) Log.d(TAG, "encode(): ${inputBuffer.toHexString()}")
        output.write(KISS_FEND.toInt())
        for (b in inputBuffer) {
            when (b) {
                KISS_FEND -> {
                    output.write(KISS_FESC.toInt())
                    output.write(KISS_TFEND.toInt())
                }

                KISS_FESC -> {
                    output.write(KISS_FESC.toInt())
                    output.write(KISS_TFESC.toInt())
                }

                else -> output.write(b.toInt())
            }
        }
        output.write(KISS_FEND.toInt())
    }

    companion object {
        private val TAG = KissEncoder::class.java.getSimpleName()
        private const val D = true

        private const val KISS_FEND = 0xc0.toByte()
        private const val KISS_FESC = 0xdb.toByte()
        private const val KISS_TFEND = 0xdc.toByte()
        private const val KISS_TFESC = 0xdd.toByte()
    }
}