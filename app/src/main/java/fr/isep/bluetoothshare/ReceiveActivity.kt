package fr.isep.bluetoothshare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
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
import java.io.IOException
import java.util.UUID

class ReceiveActivity : AppCompatActivity()  {

    //static values to put an id on requests
    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 1
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
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
            startDiscoverability()
            val acceptThread = AcceptThread()
            acceptThread.start()
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

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("Bluetooth Share", MY_UUID)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            Log.println(Log.INFO, "BluetoothService", "open the accept thread")
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                        mmServerSocket?.accept()
                    } catch (e: IOException) {
                        Log.println(Log.ERROR, "BluetoothService", "error during the accept thread")
                        shouldLoop = false
                        null
                    }
                Log.println(Log.INFO, "BluetoothService", "socket? ${socket.toString()}")
                socket?.also {
                    transfer(it)
                    Log.println(Log.INFO, "BluetoothService", "connection detected")
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
                Log.println(Log.INFO, "BluetoothService", "close the accept thread")
            } catch (e: IOException) {
                Log.println(Log.ERROR, "BluetoothService", "Could not close the connect socket")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

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
        }
    }

    //check if the bluetooth connect permission is allowed
    fun checkBluetoothPermission() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.INFO, "BluetoothService","ask for bluetooth permission")
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT
            )
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
            startDiscoverability()
            val acceptThread = AcceptThread()
            acceptThread.start()
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

    private fun startDiscoverability() {
        val requestCode = 1
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivityForResult(discoverableIntent, requestCode)
    }

    private fun transfer(socket: BluetoothSocket) {
        BluetoothSocketManager.setSocket(socket)
        val intent = Intent(this, TransferActivity::class.java)
        startActivity(intent)
    }
}