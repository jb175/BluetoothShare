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

    //static values to put an id on requests
    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 1
        private const val REQUEST_BLUETOOTH_SCAN = 2
    }

    //bluetooth adapter to perform actions with bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null

    //register to enable bluetooth
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

    //receiver to detect if the bluetooth has been disabled
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

                if (newState == BluetoothAdapter.STATE_OFF) {
                    Log.println(Log.INFO, "BluetoothService", "Bluetooth has been turned off ")
                    checkBluetoothPermission()
                }
            }
        }
    }

    //receiver to scan new devices with bluetooth
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

        Log.println(Log.INFO, "MainActivity","setup bluetooth register")
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

    //stop receiver
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(discoveryReceiver)
    }

    //execute actions when a permission is granted or not
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.println(Log.WARN, "BluetoothService","bluetooth permission not granted. Couldn't connect to bluetooth device")
                    finish()
                } else {
                    Log.println(Log.INFO, "BluetoothService","bluetooth permission granted")
                    enableBluetooth()
                }
            }
            REQUEST_BLUETOOTH_SCAN -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.println(Log.WARN, "BluetoothService","bluetooth permission not granted. Couldn't scan devices")
                    finish()
                } else {
                    Log.println(Log.INFO, "BluetoothService","bluetooth permission granted")
                    discoverBluetoothDevices()
                }
            }
        }
    }

    //check if the bluetooth connect permission is allowed
    fun checkBluetoothPermission() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.INFO, "BluetoothService","ask for bluetooth permission")
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth permission already allowed")
            enableBluetooth()
        }
    }

    //check if bluetooth is enable
    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            Log.println(Log.INFO, "BluetoothService","bluetooth not enable")
            requestEnableBluetooth()
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth ready")
            listPairedBluetoothDevices()
        }
    }

    //ask to enable bluetooth
    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.WARN, "BluetoothService", "bluetooth permission not allowed")
            finish()
        }
        bluetoothEnableLauncher.launch(enableBtIntent)
    }

    //list all known devices
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

    //ask permission to scan new devices
    private fun requestDiscoverability() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.INFO, "BluetoothService","ask for bluetooth scan permission")
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN)
        } else {
            Log.println(Log.INFO, "BluetoothService","bluetooth scan permission already allowed")
            discoverBluetoothDevices()
        }
    }

    //scan new devices
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

}