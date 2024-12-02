package com.mobilinkd.bleconfig

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager


val tncInputLevelNotification = "tncRxLevel"
val tncBatteryLevelNotification = "tncBatteryLevel"
val tncTxGainNotification = "tncTxGain"
val tncTxTwistNotification = "tncTxTwist"
val tncRxGainNotification = "tncRxGain"
val tncSquelchLevelNotification = "tncSquelchLevel" // Not used
val tncVerbosityLevelNotification = "tncVerbosityTwist"
val tncRxTwistNotification = "tncRxTwist"

val tncTxDelayNotification = "tncTxDelay"
val tncPersistenceNotification = "tncPersistence"
val tncSlotTimeNotification = "tncSlotTime"
val tncTxTailNotification = "tncTxTail"
val tncDuplexNotification = "tncDuplex"

val tncFirmwareVersionNotification = "tncFirmwareVersion"
val tncHardwareVersionNotification = "tncHardwareVersion"
val tncSerialNumberNotification = "tncSerialNumber"
val tncMacAddressNotification = "tncMacAddress"
val tncDateTimeNotification = "tncDateTime"
val tncConnectionTrackingNotification = "tncConnectionTracking"
val tncUsbPowerOnNotification = "tncUsbPowerOn"
val tncUsbPowerOffNotification = "tncUsbPowerOff"

val tncPttStyleNotification = "tncPttStyle"
val tncMinimumTxTwistNotification = "tncMinimumTxTwist"
val tncMaximumTxTwistNotification = "tncMaximumTxTwist"
val tncMinimumRxTwistNotification = "tncMinimumRxTwist"
val tncMaximumRxTwistNotification = "tncMaximumRxTwist"
val tncApiVersionNotification = "tncApiVersion"
val tncMinimumRxGainNotification = "tncMinimumRxGain"
val tncMaximumRxGainNotification = "tncMaximumRxGain"
val tncCapabilitiesNotification = "tncCapabilities"

val tncModifiedNotification = "tncModified"

val tncSupportedModemTypesNotification = "tncSupportedModemTypes"
val tncModemTypeNotification = "tncModemType"
val tncPassallNotification = "tncPassall"
val tncRxPolarityNotification = "tncRxPolarity"
val tncTxPolarityNotification = "tncTxPolarity"

class TncProtocolDecoder(context: Context) {

    var tncInputLevel: Int? = null
    var tncBatteryLevel: Int? = null
    var tncTxGain: Int? = null
    var tncTxTwist: Int? = null
    var tncRxGain: Int? = null
    var tncSquelchLevel: Int? = null // Not used
    var tncVerbosityLevel: Int? = null
    var tncRxTwist: Int? = null

    var tncTxDelay: Int? = null
    var tncPersistence: Int? = null
    var tncSlotTime: Int? = null
    var tncTxTail: Int? = null
    var tncDuplex: Int? = null

    var tncFirmwareVersion: String? = null
    var tncHardwareVersion: String? = null
    var tncSerialNumber: String? = null
    var tncMacAddress: ByteArray? = null
    var tncDateTime: ByteArray? = null
    var tncConnectionTracking: Int? = null
    var tncUsbPowerOn: Int? = null
    var tncUsbPowerOff: Int? = null

    var tncPttStyle: Int? = null
    var tncMinimumTxTwist: Int? = null
    var tncMaximumTxTwist: Int? = null
    var tncMinimumRxTwist: Int? = null
    var tncMaximumRxTwist: Int? = null
    var tncApiVersion: Int? = null
    var tncMinimumRxGain: Int? = null
    var tncMaximumRxGain: Int? = null
    var tncCapabilities: Int? = null

    var tncModified: Int? = null

    var tncSupportedModemTypes: ByteArray? = null
    var tncModemType: Int? = null
    var tncPassall: Int? = null
    var tncRxPolarity: Int? = null
    var tncTxPolarity: Int? = null

    private val broadcastManager = LocalBroadcastManager.getInstance(context)

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    fun send(id: String, value: Byte) {
        val intent = Intent(id)
        intent.putExtra("arg", value.toInt())
        broadcastManager.sendBroadcast(intent)
    }

    fun send(id: String, value: Int) {
        val intent = Intent(id)
        intent.putExtra("arg", value)
        broadcastManager.sendBroadcast(intent)
    }

    fun send(id: String, value: ByteArray) {
        val intent = Intent(id)
        intent.putExtra("arg", value)
        broadcastManager.sendBroadcast(intent)
    }

    fun send(id: String, value: String) {
        val intent = Intent(id)
        intent.putExtra("arg", value)
        broadcastManager.sendBroadcast(intent)
    }

    fun sendU16(id: String, value: List<Byte>) {
        send(id, (value[0].toUInt() * 256.toUInt() + value[1].toUInt()).toInt())
    }

    fun decodePacket(packet: ByteArray) {
        when (packet.first().toInt()) {
            0x01 -> { // TxDelay
                send(tncTxDelayNotification, packet[1])
                tncTxDelay = packet[1].toInt()
            }
            0x02 -> { // Persistence
                send(tncPersistenceNotification, packet[1])
                tncPersistence = packet[1].toInt()
            }
            0x03 -> { // SlotTime
                send(tncSlotTimeNotification, packet[1])
                tncSlotTime =  packet[1].toInt()
            }
            0x04 -> { // TxTail
                send(tncTxTailNotification, packet[1])
                tncTxTail = packet[1].toInt()
            }
            0x05 -> { // Duplex
                send(tncDuplexNotification, packet[1])
                tncDuplex = packet[1].toInt()
            }
            0x06 -> { // Hardware configuration
                decodeHardwareConfiguration(packet)
            }
            else -> { // Unexpected data...
                Log.w(TAG, "Unexpected data: " + packet.toHexString())
            }
        }
    }

    private fun decodeHardwareConfiguration(packet: ByteArray) {
        when (packet[1].toInt()) {
            4 -> { // Input Level
                send(tncInputLevelNotification, packet[2])
            }
            6 -> { // Battery Level
                sendU16(tncBatteryLevelNotification, packet.drop(2))
                tncBatteryLevel = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            12 -> { // TX Gain
                sendU16(tncTxGainNotification, packet.drop(2))
                tncTxGain = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            13 -> { // RX Gain
                sendU16(tncRxGainNotification, packet.drop(2))
                tncRxGain = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            17 -> { // Verbosity
                send(tncVerbosityLevelNotification, packet[2])
                tncVerbosityLevel = packet[2].toInt()
            }
            25 -> { // RX Twist
                send(tncRxTwistNotification, packet[2])
                tncRxTwist = packet[2].toInt()
            }
            27 -> { // TX Twist
                send(tncTxTwistNotification, packet[2])
                tncTxTwist = packet[2].toInt()
            }
            33 -> { // TX Delay
                send(tncTxDelayNotification, packet[2])
                tncTxDelay = packet[2].toInt()
            }
            34 -> { // Persistence
                send(tncPersistenceNotification, packet[2])
                tncPersistence = packet[2].toInt()
            }
            35 -> { // Slot Time
                send(tncSlotTimeNotification, packet[2])
                tncSlotTime = packet[2].toInt()
            }
            36 -> { // TX Tail
                send(tncTxTailNotification, packet[2])
            }
            37 -> { // Duplex
                send(tncDuplexNotification, packet[2])
                tncTxTail = packet[2].toInt()
            }
            40 -> { // Firmware Version
                send(tncFirmwareVersionNotification, packet.drop(2).toString())
                tncFirmwareVersion = packet.drop(2).toString()
            }
            41 -> { // Hardware Version
                send(tncHardwareVersionNotification, packet.drop(2).toString())
                tncHardwareVersion = packet.drop(2).toString()
            }
            47 -> { // Serial Number
                send(tncSerialNumberNotification, packet.drop(2).toString())
                tncSerialNumber = packet.drop(2).toString()
            }
            48 -> { // MAC Address
                send(tncMacAddressNotification, packet.drop(2).toByteArray())
                tncMacAddress = packet.drop(2).toByteArray()
            }
            49 -> { // DateTime
                send(tncDateTimeNotification, packet.drop(2).toByteArray())
                tncDateTime = packet.drop(2).toByteArray()
            }
            70 -> { // Connection Tracking
                send(tncConnectionTrackingNotification, packet[2])
                tncConnectionTracking = packet[2].toInt()
            }
            74 -> { // USB Power On
                send(tncUsbPowerOnNotification, packet[2])
                tncUsbPowerOn = packet[2].toInt()
            }
            76 -> { // USB Power Off
                send(tncUsbPowerOffNotification, packet[2])
                tncUsbPowerOff = packet[2].toInt()
            }
            80 -> { // PTT Style
                send(tncPttStyleNotification, packet[2])
                tncPttStyle = packet[2].toInt()
            }
            82 -> { // Passall
                send(tncPassallNotification, packet[2])
                tncPassall = packet[2].toInt()
            }
            84 -> { // RX Polarity
                send(tncRxPolarityNotification, packet[2])
                tncRxPolarity = packet[2].toInt()
            }
            86 -> { // TX Polarity
                send(tncTxPolarityNotification, packet[2])
                tncTxPolarity = packet[2].toInt()
            }
            119 -> { // Min TX Twist
                send(tncMinimumTxTwistNotification, packet[2])
                tncMinimumTxTwist = packet[2].toInt()
            }
            120 -> { // Max TX Twist
                send(tncMaximumTxTwistNotification, packet[2])
                tncMaximumTxTwist = packet[2].toInt()
            }
            121 -> { // Min RX Twist
                send(tncMinimumRxTwistNotification, packet[2])
                tncMinimumRxTwist = packet[2].toInt()
            }
            122 -> { // Max RX Twist
                send(tncMaximumRxTwistNotification, packet[2])
                tncMaximumRxTwist = packet[2].toInt()
            }
            123 -> { // API Version
                send(tncApiVersionNotification, packet[2])
                tncApiVersion = packet[2].toInt()
            }
            124 -> { // Min RX Gain
                send(tncMinimumRxGainNotification, packet[2])
                tncMinimumRxGain = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            125 -> { // Max RX Gain
                send(tncMaximumRxGainNotification, packet[2])
                tncMaximumRxGain = (packet[2].toUInt() * 256U + packet[3].toUInt()).toInt()
            }
            126 -> { // Capabilities
                send(tncCapabilitiesNotification, packet[2])
                tncCapabilities = packet[2].toInt()
            }
            0xC1 -> {
                decodeExtendedHardwareConfiguration(packet)
            }
            else -> {
                Log.w(TAG, "Unexpected data: " + packet.toHexString())
            }
        }
    }


    private fun decodeExtendedHardwareConfiguration(packet: ByteArray) {
        when (packet[2].toInt()) {
            0x81 -> { // Modem Type
                send(tncModemTypeNotification, packet[3])
                tncModemType = packet[3].toInt()
            }
            0x83 -> { // Supported Modem Types
                send(tncSupportedModemTypesNotification, packet.drop(3).toByteArray())
                tncSupportedModemTypes = packet.drop(3).toByteArray()
            }
            else -> {
                Log.w(TAG, "Unexpected data: " + packet.toHexString())
            }
        }
    }

    companion object {
        private val TAG = TncProtocolDecoder::class.java.name
        private const val D = true
    }
}