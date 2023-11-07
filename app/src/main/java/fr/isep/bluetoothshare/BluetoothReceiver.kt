package fr.isep.bluetoothshare

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver(private val mainActivity: MainActivity): BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            if (newState == BluetoothAdapter.STATE_OFF) {
                Log.println(Log.INFO, "BluetoothService", "Bluetooth has been turned off ")
                mainActivity.checkBluetoothPermission()
            }
        }
    }
}