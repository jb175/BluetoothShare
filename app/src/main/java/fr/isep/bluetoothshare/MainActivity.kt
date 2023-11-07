package fr.isep.bluetoothshare

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 2
    }

    private val bluetoothReceiver = BluetoothReceiver(this)
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.println(Log.WARN, "BluetoothService", "Bluetooth has not or could not be enabled")
            finish()
        } else {
            Log.println(Log.INFO, "BluetoothService", "Bluetooth enable")
            discoverBluetoothDevices()
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.println(Log.INFO, "BluetoothService", "test receive?")
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    Log.println(Log.INFO, "BluetoothService", "new bluetooth device detected")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    println(device.toString())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.println(Log.INFO, "StartMainActivity","setup register")
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        Log.println(Log.INFO, "BluetoothService","get Adapter")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter == null) {
            Log.println(Log.ERROR, "BluetoothService","bluetooth not available on this phone")
            finish()
        }

        checkBluetoothPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.println(Log.WARN, "BluetoothService","bluetooth permission not allowed")
                    finish()
                } else {
                    Log.println(Log.INFO, "BluetoothService","bluetooth permission granted")
                    enableBluetooth()
                }
            }
        }
    }


    fun checkBluetoothPermission() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.INFO, "BluetoothService","ask for bluetooth permission")
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth permission already allowed")
            enableBluetooth()
        }
    }


    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            Log.println(Log.INFO, "BluetoothService","bluetooth not enable")
            requestEnableBluetooth()
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth ready")
            discoverBluetoothDevices()
        }
    }

    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.WARN, "BluetoothService", "bluetooth permission not allowed")
            finish()
        }
        bluetoothEnableLauncher.launch(enableBtIntent)
    }

    private fun discoverBluetoothDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            finish()
            Log.println(Log.WARN, "BluetoothService", "Bluetooth permission is not allowed")
        }

        Log.println(Log.INFO, "BluetoothService", "already connected devices:")

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
            println(deviceName+deviceHardwareAddress)
        }

        Log.println(Log.INFO, "BluetoothService", "Bluetooth discovery will start")
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(discoveryReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(discoveryReceiver)
    }
}