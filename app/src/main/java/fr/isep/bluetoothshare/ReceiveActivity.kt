package fr.isep.bluetoothshare

import android.Manifest
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class ReceiveActivity : AppCompatActivity()  {

    //static values to put an id on requests
    companion object {
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    //bluetooth adapter to perform actions with bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null

    //receiver to detect if the bluetooth has been disabled
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

                if (newState == BluetoothAdapter.STATE_OFF) {
                    Log.println(Log.INFO, "BluetoothService", "Bluetooth has been turned off ")
                    finish()
                }
            }
        }
    }

    private inner class AcceptThread(val activity: ReceiveActivity) : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            listen()
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

        private fun listen(): BluetoothServerSocket? {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                this.activity.finish()
            }
            return bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("Bluetooth Share", MY_UUID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        Log.println(Log.INFO, "BluetoothService","get Adapter")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter == null) {
            Log.println(Log.ERROR, "BluetoothService","bluetooth not available on this phone")
            finish()
        }

        startDiscoverability()
        val acceptThread = AcceptThread(this)
        acceptThread.start()
    }

    //stop receiver
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    private fun startDiscoverability() {
        val requestCode = 1
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivityForResult(discoverableIntent, requestCode)
    }

    private fun transfer(socket: BluetoothSocket) {
        val intent = Intent(this, TransferActivity::class.java)
        startActivity(intent)
        finish()
    }
}