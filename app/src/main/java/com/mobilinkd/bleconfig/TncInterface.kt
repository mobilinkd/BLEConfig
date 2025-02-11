package com.mobilinkd.bleconfig

import android.util.Log
import java.io.OutputStream
import java.util.Calendar
import java.util.TimeZone

class TncInterface(os: OutputStream) {

    private val encoder = KissEncoder()
    private val outputStream = os
    private var tncHasChanged = false

    val modified get() = tncHasChanged

    fun flush() {
        outputStream.flush()
    }

    fun getAllValues() {
        encoder.encode(TNC_GET_ALL_VALUES, outputStream)
        outputStream.flush()
    }

    fun getBatteryLevel() {
        encoder.encode(TNC_GET_BATTERY_LEVEL, outputStream)
        outputStream.flush()
    }

    fun startReceiveAudio() {
        encoder.encode(TNC_PTT_OFF, outputStream)
        encoder.encode(TNC_STREAM_VOLUME, outputStream)
        outputStream.flush()
    }

    fun stopReceiveAudio() {
        encoder.encode(TNC_PTT_OFF, outputStream) // Stops TX and idles input processing
        outputStream.flush()
    }

    fun setTxDelay(v: Int) {
        if (D) Log.d(TAG, "setTxDelay($v)")
        TNC_SET_TX_DELAY[1] = v.toByte()
        encoder.encode(TNC_SET_TX_DELAY, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setPersistence(v: Int) {
        if (D) Log.d(TAG, "setPersistence($v)")
        TNC_SET_PERSISTENCE[1] = v.toByte()
        encoder.encode(TNC_SET_PERSISTENCE, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setSlotTime(v: Int) {
        if (D) Log.d(TAG, "setSlotTime($v)")
        TNC_SET_SLOT_TIME[1] = v.toByte()
        encoder.encode(TNC_SET_SLOT_TIME, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setDuplex(v: Boolean) {
        if (D) Log.d(TAG, "setDuplex($v)")
        TNC_SET_DUPLEX[1] = if (v) 1 else 0
        encoder.encode(TNC_SET_DUPLEX, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setUsbPowerOn(v: Boolean) {
        if (D) Log.d(TAG, "setUsbPowerOn($v)")
        TNC_SET_USB_POWER_ON[2] = if (v) 1 else 0
        encoder.encode(TNC_SET_USB_POWER_ON, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setUsbPowerOff(v: Boolean) {
        if (D) Log.d(TAG, "setUsbPowerOff($v)")
        TNC_SET_USB_POWER_OFF[2] = if (v) 1 else 0
        encoder.encode(TNC_SET_USB_POWER_OFF, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setInputGain(v: Int) {
        if (D) Log.d(TAG, "setInputGain($v)")
        TNC_SET_INPUT_GAIN[2] = ((v shr 8) and 0xFF).toByte()
        TNC_SET_INPUT_GAIN[3] = (v and 0xFF).toByte()
        encoder.encode(TNC_SET_INPUT_GAIN, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setInputTwist(v: Int) {
        if (D) Log.d(TAG, "setInputTwist($v)")
        TNC_SET_INPUT_TWIST[2] = (v and 0xFF).toByte()
        encoder.encode(TNC_SET_INPUT_TWIST, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setModemType(v: Int) {
        if (D) Log.d(TAG, "setModemType($v)")
        TNC_SET_MODEM_TYPE[3] = (v and 0xFF).toByte()
        encoder.encode(TNC_SET_MODEM_TYPE, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setPassAll(v: Boolean) {
        if (D) Log.d(TAG, "setPassAll($v)")
        TNC_SET_PASSALL[2] = if (v) 1 else 0
        encoder.encode(TNC_SET_PASSALL, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setReceivePolarity(v: Boolean) {
        if (D) Log.d(TAG, "setReceivePolarity($v)")
        TNC_SET_RX_REVERSE_POLARITY[2] = if (v) 1 else 0
        encoder.encode(TNC_SET_RX_REVERSE_POLARITY, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setTransmitPolarity(v: Boolean) {
        if (D) Log.d(TAG, "setTransmitPolarity($v)")
        TNC_SET_TX_REVERSE_POLARITY[2] = if (v) 1 else 0
        encoder.encode(TNC_SET_TX_REVERSE_POLARITY, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }


    fun setPttStyle(v: Int) {
        if (D) Log.d(TAG, "setPttStyle($v)")
        TNC_SET_PTT_CHANNEL[2] = v.toByte()
        encoder.encode(TNC_SET_PTT_CHANNEL, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setTransmitGain(v: Int) {
        if (D) Log.d(TAG, "setTransmitGain($v)")
        TNC_SET_OUTPUT_GAIN[2] = ((v shr 8) and 0xFF).toByte()
        TNC_SET_OUTPUT_GAIN[3] = (v and 0xFF).toByte()
        encoder.encode(TNC_SET_OUTPUT_GAIN, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun setTransmitTwist(v: Int) {
        if (D) Log.d(TAG, "setTransmitTwist($v)")
        TNC_SET_OUTPUT_TWIST[2] = (v and 0xFF).toByte()
        encoder.encode(TNC_SET_OUTPUT_TWIST, outputStream)
        tncHasChanged = true
        outputStream.flush()
    }

    fun sendPttOff() {
        encoder.encode(TNC_PTT_OFF, outputStream)
        outputStream.flush()
    }

    fun sendPttMark() {
        encoder.encode(TNC_PTT_MARK, outputStream)
        outputStream.flush()
    }

    fun sendPttSpace() {
        encoder.encode(TNC_PTT_SPACE, outputStream)
        outputStream.flush()
    }

    fun sendPttBoth() {
        encoder.encode(TNC_PTT_BOTH, outputStream)
        outputStream.flush()
    }

    private fun bcd(value: Int): Byte {
        return (((value / 10) * 16) + (value % 10)).toByte()
    }

    fun setDateTime() {
        if (D) Log.d(TAG,"setDateTIme")

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = calendar.time

        TNC_SET_DATETIME[2] = bcd(calendar[Calendar.YEAR] - 2000)
        TNC_SET_DATETIME[3] = bcd(calendar[Calendar.MONTH] + 1)
        TNC_SET_DATETIME[4] = bcd(calendar[Calendar.DAY_OF_MONTH])
        TNC_SET_DATETIME[5] = (calendar[Calendar.DAY_OF_WEEK] - 1).toByte()
        TNC_SET_DATETIME[6] = bcd(calendar[Calendar.HOUR_OF_DAY])
        TNC_SET_DATETIME[7] = bcd(calendar[Calendar.MINUTE])
        TNC_SET_DATETIME[8] = bcd(calendar[Calendar.SECOND])
        encoder.encode(TNC_SET_DATETIME, outputStream)
        outputStream.flush()
    }

    fun saveIfChanged() {
        if (tncHasChanged) {
            Log.i(TAG, "Saving state to EEPROM")
            encoder.encode(TNC_SAVE_EEPROM, outputStream)
            outputStream.flush()
            tncHasChanged = false
        }
    }

    companion object {
        private val TAG = TncInterface::class.java.simpleName
        private const val D = true

        const val SEND_SPACE: Int = 1
        const val SEND_MARK: Int = 2
        const val SEND_BOTH: Int = 3

        private val TNC_SET_TX_DELAY = byteArrayOf(0x01, 0)
        private val TNC_SET_PERSISTENCE = byteArrayOf(0x02, 0)
        private val TNC_SET_SLOT_TIME = byteArrayOf(0x03, 0)
        private val TNC_SET_TX_TAIL = byteArrayOf(0x04, 0)
        private val TNC_SET_DUPLEX = byteArrayOf(0x05, 0)
        private val TNC_SET_BT_CONN_TRACK = byteArrayOf(0x06, 0x45, 0)
        private val TNC_SET_VERBOSITY = byteArrayOf(0x06, 0x10, 0)
        private val TNC_SET_INPUT_ATTEN = byteArrayOf(0x06, 0x02, 0)
        private val TNC_STREAM_VOLUME = byteArrayOf(0x06, 0x05)
        private val TNC_PTT_MARK = byteArrayOf(0x06, 0x07)
        private val TNC_PTT_SPACE = byteArrayOf(0x06, 0x08)
        private val TNC_PTT_BOTH = byteArrayOf(0x06, 0x09)
        private val TNC_PTT_OFF = byteArrayOf(0x06, 0x0A)
        private val TNC_SET_OUTPUT_VOLUME = byteArrayOf(0x06, 0x01, 0)
        /* 16-bit signed. */
        private val TNC_SET_OUTPUT_GAIN = byteArrayOf(0x06, 0x01, 0, 0) // API 2.0
        private val TNC_GET_OUTPUT_VOLUME = byteArrayOf(0x06, 0x0C)
        private val TNC_SET_OUTPUT_TWIST = byteArrayOf(0x06, 0x1A, 0)
        private val TNC_SET_SQUELCH_LEVEL = byteArrayOf(0x06, 0x03, 0)
        private val TNC_GET_ALL_VALUES = byteArrayOf(0x06, 0x7F)
        private val TNC_SET_USB_POWER_ON = byteArrayOf(0x06, 0x49, 0)
        private val TNC_SET_USB_POWER_OFF = byteArrayOf(0x06, 0x4b, 0)
        private val TNC_SET_PTT_CHANNEL = byteArrayOf(0x06, 0x4f, 0)
        private val TNC_GET_PTT_CHANNEL = byteArrayOf(0x06, 0x50)
        private val TNC_SAVE_EEPROM = byteArrayOf(0x06, 0x2a)
        private val TNC_GET_BATTERY_LEVEL = byteArrayOf(0x06, 0x06)
        private val TNC_ADJUST_INPUT_LEVELSval =byteArrayOf(0x06, 0x2b)
        /* 16-bit signed. */
        private val TNC_SET_INPUT_GAIN = byteArrayOf(0x06, 0x02, 0, 0)
        private val TNC_SET_INPUT_TWIST = byteArrayOf(0x06, 0x18, 0)
        /* BCD YYMMDDWWHHMMSS */
        private val TNC_SET_DATETIME = byteArrayOf(0x06, 0x32, 0, 0, 0, 0, 0, 0, 0)
        private val TNC_GET_DATETIME = byteArrayOf(0x06, 0x31)

        private val TNC_SET_PASSALL = byteArrayOf(0x06, 0x51, 0)
        private val TNC_SET_MODEM_TYPE = byteArrayOf(0x06, 0xc1.toByte(), 0x82.toByte(), 0)

        private val TNC_SET_RX_REVERSE_POLARITY = byteArrayOf(0x06, 0x53, 0)
        private val TNC_SET_TX_REVERSE_POLARITY = byteArrayOf(0x06, 0x55, 0)

    }
}