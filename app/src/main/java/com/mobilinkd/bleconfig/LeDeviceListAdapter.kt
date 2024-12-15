package com.mobilinkd.bleconfig

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobilinkd.bleconfig.databinding.LeDeviceCardViewBinding


class LeDeviceListAdapter(context: Context) : RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder>() {

    private lateinit var _binding: LeDeviceCardViewBinding
    private var _inflater = LayoutInflater.from(context)
    private var _data = mutableListOf<ScanResult>()
    private var _clickListener: BluetoothLEDeviceListener? = null

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var deviceAlias: TextView = itemView.findViewById(R.id.deviceAlias)
        var deviceMacAddress: TextView = itemView.findViewById(R.id.deviceMacAddress)
        var deviceRSSI: TextView = itemView.findViewById(R.id.deviceRSSI)
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (D) Log.d(TAG, "onClick: ${deviceAlias.text}, _clickListener: $_clickListener")
            _clickListener?.onBluetoothLEDeviceSelected(_data[adapterPosition].device)
        }
    }

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (D) Log.d(TAG, "onCreateViewHolder() group = $parent, type = $viewType")
        _binding = LeDeviceCardViewBinding.inflate(_inflater, parent, false)
        return ViewHolder(_binding.root)

    }

    // binds the data to the TextView in each row
    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (D) Log.d(TAG, "onBindViewHolder(${holder.adapterPosition}, $position)")
        val scanResult = _data[position]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.deviceAlias.text = scanResult.device.alias ?: scanResult.device.name
        } else {
            holder.deviceAlias.text = scanResult.device.name
        }
        holder.deviceMacAddress.text = scanResult.device.address
        holder.deviceRSSI.text = "${scanResult.rssi}dBm"
    }

    // total number of rows
    override fun getItemCount(): Int {
        return _data.size
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: BluetoothLEDeviceListener?) {
        this._clickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface BluetoothLEDeviceListener {
        fun onBluetoothLEDeviceSelected(device: BluetoothDevice)
    }

    @SuppressLint("MissingPermission")
    fun addDevice(scanResult: ScanResult) {
        if (scanResult.device.bondState != BluetoothDevice.BOND_BONDED) {
//            if (D) Log.d(TAG,"Ignoring unbonded device: ${scanResult.device.address}")
//            return
        }

        if (scanResult.device.name == null) {
            // Ignore partial results.
            return
        }

        for ((index, item) in _data.withIndex()) {
            if (item.device.address == scanResult.device.address) {
                _data[index] = scanResult
                notifyItemChanged(index)
                return
            }
        }
        val position = _data.size
        _data += scanResult
        notifyItemInserted(position)
        if (D) Log.d(TAG, "Added scan result for ${scanResult.device.address} at position $position")
    }

    fun getItem(id: Int): ScanResult {
        return _data[id]
    }

    fun size(): Int {
        return _data.size
    }

    companion object {
        private val TAG = LeDeviceListAdapter::class.java.simpleName
        private const val D = true
    }
}