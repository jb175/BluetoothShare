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
        private const val REQUEST_BLUETOOTH_SCAN = 3
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
            listPairedBluetoothDevices()
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.WARN, "BluetoothService","bluetooth connect permission not granted")
                return
            }
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val name = device?.name
                    val address = device?.address
                    Log.println(Log.INFO, "BluetoothDevices", "   $name $address")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.println(Log.INFO, "MainActivity","setup register")
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

        Log.println(Log.INFO, "BluetoothService", "request id: $requestCode")
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
            REQUEST_BLUETOOTH_SCAN -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.println(Log.WARN, "BluetoothService","bluetooth permission not allowed")
                    finish()
                } else {
                    Log.println(Log.INFO, "BluetoothService","bluetooth permission granted")
                    discoverBluetoothDevices()
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
            listPairedBluetoothDevices()
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

    private fun listPairedBluetoothDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            finish()
            Log.println(Log.WARN, "BluetoothService", "Bluetooth permission is not allowed")
        }

        Log.println(Log.INFO, "BluetoothDevices", "already connected devices:")

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
            Log.println(Log.INFO, "BluetoothDevices", "   $deviceName $deviceHardwareAddress")
        }

        requestDiscoverability()
    }

    private fun requestDiscoverability() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.INFO, "BluetoothService","ask for bluetooth scan permission")
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth scan permission already allowed")
            discoverBluetoothDevices()
        }
    }

    private fun discoverBluetoothDevices() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(discoveryReceiver, filter)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.WARN, "BluetoothService","doesn't have permission to scan devices")
            finish()
        } else {
            bluetoothAdapter?.startDiscovery()
            Log.println(Log.INFO, "BluetoothDevices","new devices scanned:")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(discoveryReceiver)
    }
}