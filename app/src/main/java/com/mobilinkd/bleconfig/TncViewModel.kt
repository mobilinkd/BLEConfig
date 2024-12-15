package com.mobilinkd.bleconfig

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.reflect.typeOf

class TncViewModel: ViewModel() {

    val tncInputLevel: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncBatteryLevel: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncTxGain: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncTxTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncInputGain: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncSquelchLevel: MutableLiveData<Int> by lazy { MutableLiveData<Int>() } // Not used
    val tncVerbosityLevel: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncInputTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    val tncTxDelay: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncPersistence: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncSlotTime: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncTxTail: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncDuplex: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    val tncFirmwareVersion: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val tncHardwareVersion: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val tncSerialNumber: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val tncMacAddress: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val tncDateTime: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val tncConnectionTracking: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncUsbPowerOn: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncUsbPowerOff: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    val tncPttStyle: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMinimumTxTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMaximumTxTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMinimumRxTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMaximumRxTwist: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncApiVersion: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMinimumRxGain: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncMaximumRxGain: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncCapabilities: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    val tncSupportedModemTypes: MutableLiveData<ByteArray> by lazy { MutableLiveData<ByteArray>() }
    val tncModemType: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncPassall: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncRxPolarity: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tncTxPolarity: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    fun clear() {
        return
//        var failed = false
//        TncViewModel::class.java.declaredFields.forEach {
//            if (it.name.startsWith("tnc")) {
//                if (it.get(this) != null) {
//                    try {
//                        val v = MutableLiveData::class.java.getDeclaredField("value")
//                        Log.i(TAG, "${v.name} = ${v.get(this)}")
//                    } catch (_: NoSuchFieldException) {
//                        Log.i(TAG, "${it.name} has no 'value' field, ${it.type}")
//                        failed = true
//                    } catch (_: NoSuchMethodException) {
//                        Log.i(TAG, "${it.name} has no 'getValue' method, ${it.type}")
//                        failed = true
//                    }
//                }
//            }
//        }
//        if (failed) {
//            Lazy::class.java.declaredFields.forEach {
//                Log.i(TAG, "Lazy.${it.name}}")
//            }
//        }
    }

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun from_bcd(value: Byte): Int {
        return ((value.toInt() shr 4) * 10) + (value.toInt() and 15)
    }

    private fun bcdDateToString(value: ByteArray): String {
        val date: Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))

        date[from_bcd(value[0]) + 2000, from_bcd(value[1]) - 1, from_bcd(value[2]), from_bcd(value[4]), from_bcd(
            value[5]
        )] =
            from_bcd(value[6]) // SECOND

        date[Calendar.DAY_OF_WEEK] = value[3] + 1

        val format: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.calendar = date
        return format.format(date.time) + " UTC"
    }

    fun macAddressToString(a: ByteArray): String {
        val sb = StringBuilder(a.size * 3)
        for (b in a) sb.append(String.format("%02X:", b))
        return sb.dropLast(1).toString()
    }

    fun decodePacket(packet: ByteArray) {
        if (D) Log.d(TAG, "decodePacket(): ${packet.toHexString()}")
        when (packet.first().toInt()) {
            0x01 -> { // TxDelay
                tncTxDelay.value = packet[1].toUInt().toInt()
            }
            0x02 -> { // Persistence
                tncPersistence.value = packet[1].toUInt().toInt()
            }
            0x03 -> { // SlotTime
                tncSlotTime.value =  packet[1].toUInt().toInt()
            }
            0x04 -> { // TxTail
                tncTxTail.value = packet[1].toUInt().toInt()
            }
            0x05 -> { // Duplex
                tncDuplex.value = packet[1].toInt()
            }
            0x06 -> { // Hardware configuration
                decodeHardwareConfiguration(packet)
            }
            else -> { // Unexpected data...
                Log.w(TAG, "Unexpected data: " + packet.toHexString())
            }
        }
    }

    private fun toUint16(packet: ByteArray, offset: Int): Int {
        return ((packet[offset].toInt() and 0xFF) shl 8) + (packet[offset + 1].toInt() and 0xFF)
    }

    private fun decodeHardwareConfiguration(packet: ByteArray) {
        when (packet[1].toInt()) {
            4 -> { // Input Level
                tncInputLevel.value = toUint16(packet, 2)
            }
            6 -> { // Battery Level
                tncBatteryLevel.value = toUint16(packet, 2)
                Log.i(TAG, "Battery Level = ${tncBatteryLevel.value}mV")
            }
            12 -> { // TX Gain
                tncTxGain.value = toUint16(packet, 2)
            }
            13 -> { // RX Gain
                tncInputGain.value = toUint16(packet, 2)
            }
            17 -> { // Verbosity
                tncVerbosityLevel.value = packet[2].toInt()
            }
            25 -> { // RX Twist
                tncInputTwist.value = packet[2].toInt()
            }
            27 -> { // TX Twist
                tncTxTwist.value = packet[2].toInt()
            }
            33 -> { // TX Delay
                Log.i(TAG, "TX Delay = ${packet[2].toUInt().toInt()}")
                tncTxDelay.value = packet[2].toUInt().toInt()
            }
            34 -> { // Persistence
                tncPersistence.value = packet[2].toUInt().toInt()
            }
            35 -> { // Slot Time
                tncSlotTime.value = packet[2].toUInt().toInt()
            }
            36 -> { // TX Tail
                tncTxTail.value = packet[2].toUInt().toInt()
            }
            37 -> { // Duplex
                tncDuplex.value = packet[2].toInt()
            }
            40 -> { // Firmware Version
                tncFirmwareVersion.value = packet.drop(2).toByteArray().toString(Charsets.UTF_8)
            }
            41 -> { // Hardware Version
                tncHardwareVersion.value = packet.drop(2).toByteArray().toString(Charsets.UTF_8)
            }
            47 -> { // Serial Number
                tncSerialNumber.value = packet.drop(2).toByteArray().toString(Charsets.UTF_8)
            }
            48 -> { // MAC Address
                tncMacAddress.value = macAddressToString(packet.drop(2).toByteArray())
            }
            49 -> { // DateTime
                tncDateTime.value = bcdDateToString(packet.drop(2).toByteArray())
            }
            70 -> { // Connection Tracking
                tncConnectionTracking.value = packet[2].toInt()
            }
            74 -> { // USB Power On
                tncUsbPowerOn.value = packet[2].toInt()
            }
            76 -> { // USB Power Off
                tncUsbPowerOff.value = packet[2].toInt()
            }
            80 -> { // PTT Style
                tncPttStyle.value = packet[2].toInt()
            }
            82 -> { // Passall
                tncPassall.value = packet[2].toInt()
            }
            84 -> { // RX Polarity
                tncRxPolarity.value = packet[2].toInt()
            }
            86 -> { // TX Polarity
                tncTxPolarity.value = packet[2].toInt()
            }
            119 -> { // Min TX Twist
                tncMinimumTxTwist.value = packet[2].toInt()
            }
            120 -> { // Max TX Twist
                tncMaximumTxTwist.value = packet[2].toInt()
            }
            121 -> { // Min RX Twist
                tncMinimumRxTwist.value = packet[2].toInt()
            }
            122 -> { // Max RX Twist
                tncMaximumRxTwist.value = packet[2].toInt()
            }
            123 -> { // API Version
                tncApiVersion.value = packet[2].toInt()
                Log.i(TAG, "API Version = ${tncApiVersion.value}")
            }
            124 -> { // Min RX Gain
                tncMinimumRxGain.value = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            125 -> { // Max RX Gain
                tncMaximumRxGain.value = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            126 -> { // Capabilities
                tncCapabilities.value = packet[2].toInt()
            }
            -63 -> {
                decodeExtendedHardwareConfiguration(packet)
            }
            else -> {
                Log.w(TAG, "Unexpected hardware data: " + packet.toHexString())
            }
        }
    }


    private fun decodeExtendedHardwareConfiguration(packet: ByteArray) {
        when (packet[2].toInt()) {
            -127 -> { // Modem Type
                tncModemType.value = packet[3].toInt()
            }
            -125 -> { // Supported Modem Types
                tncSupportedModemTypes.value = packet.drop(3).toByteArray()
            }
            else -> {
                Log.w(TAG, "Unexpected extended data: " + packet.toHexString())
            }
        }
    }

    companion object {
        private val TAG = TncViewModel::class.java.simpleName
        private const val D = false
    }
}