package com.mobilinkd.bleconfig

import android.util.Log

class KissDecoder(vm: TncViewModel) {
    private var tncViewModel = vm
    private var kissState = KissState.VOID
    private var inputKissBuffer: ByteArray = ByteArray(256)
    private var inputKissBufferPos = 0

    private fun resetState() {
        kissState = KissState.VOID
        inputKissBufferPos = 0
        inputKissBuffer = ByteArray(256)
    }

    private fun receiveFrameByte(b: Byte) {
        inputKissBuffer[inputKissBufferPos++] = b
        if (inputKissBufferPos >= inputKissBuffer.size) {
            Log.e(TAG, "Input KISS buffer overflow, discarding frame")
            resetState()
        }
    }

    fun decode(data: ByteArray) {
        for (b in data) {
            when (b) {
                KISS_FEND -> {
                    when (kissState) {
                        KissState.VOID -> {
                            kissState = KissState.GET_CMD
                            inputKissBuffer = ByteArray(256)
                            inputKissBufferPos = 0
                        }
                        KissState.GET_CMD -> {
                            // ignore duplicate FEND
                        }
                        KissState.ESCAPE -> {
                            Log.e(TAG, "KISS decode error: ${data.toHexString()}")
                            kissState = KissState.VOID
                        }
                        KissState.GET_DATA -> {
                            tncViewModel.decodePacket(inputKissBuffer.take(inputKissBufferPos).toByteArray())
                            kissState = KissState.GET_CMD // Allow back-to-back frames with only one FEND
                            inputKissBuffer = ByteArray(256)
                            inputKissBufferPos = 0
                        }
                    }
                }
                KISS_FESC -> {
                    when (kissState) {
                        KissState.GET_CMD -> {
                            kissState = KissState.ESCAPE
                        }
                        KissState.GET_DATA -> {
                            kissState = KissState.ESCAPE
                        }
                        KissState.VOID -> {
                            // ignored
                        }
                        KissState.ESCAPE -> {
                            Log.e(TAG, "KISS decode error (duplicate escape): ${data.toHexString()}")
                        }
                    }
                }
                KISS_TFEND -> {
                    when (kissState) {
                        KissState.ESCAPE -> {
                            inputKissBuffer[inputKissBufferPos++] = KISS_FEND
                            kissState = KissState.GET_DATA
                        }
                        else -> {
                            inputKissBuffer[inputKissBufferPos++] = b
                        }
                    }
                }
                KISS_TFESC -> {
                    when (kissState) {
                        KissState.ESCAPE -> {
                            inputKissBuffer[inputKissBufferPos++] = KISS_FESC
                            kissState = KissState.GET_DATA
                        }
                        else -> {
                            inputKissBuffer[inputKissBufferPos++] = b
                        }
                    }
                }
                else -> {
                    inputKissBuffer[inputKissBufferPos++] = b
                    kissState = KissState.GET_DATA
                }
            }
            if (inputKissBufferPos == inputKissBuffer.size) {
                inputKissBuffer += ByteArray(256)
                Log.w(TAG, "Input buffer extended to ${inputKissBuffer.size} bytes")
            }
        }
    }

    companion object {
        private val TAG = KissDecoder::class.java.getSimpleName()
        private const val D = true

        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

        private enum class KissState {
            VOID,
            GET_CMD,
            GET_DATA,
            ESCAPE
        }

        private const val KISS_FEND = 0xc0.toByte()
        private const val KISS_FESC = 0xdb.toByte()
        private const val KISS_TFEND = 0xdc.toByte()
        private const val KISS_TFESC = 0xdd.toByte()
    }
}