package fr.isep.bluetoothshare

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID


class MyAdapter(context: Context, private val devices: List<BluetoothDevice>) : ArrayAdapter<BluetoothDevice>(context, R.layout.list_item, devices) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val device = devices[position]
        val textViewName = view.findViewById<TextView>(R.id.name)
        val textViewAddress = view.findViewById<TextView>(R.id.address)

        val activity = context as DiscoverActivity
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.WARN, "BluetoothService", "bluetooth permission not allowed")
            activity.finish()
        }

        val address = device.address
        var name = device.name

        if (name.isNullOrEmpty())
            name = "[no name]"

        textViewName.text = name
        textViewAddress.text = address

        return view
    }
}

class DiscoverActivity : AppCompatActivity()  {

    //static values to put an id on requests
    companion object {
        private const val REQUEST_BLUETOOTH_SCAN = 2
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    //bluetooth adapter to perform actions with bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val alreadyPairedBluetoothDevices = ArrayList<BluetoothDevice>()
    private val bluetoothDevices = ArrayList<BluetoothDevice>()

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
                    if (device != null && device.name != null) {
                        bluetoothDevices.add(device)
                        Log.println(Log.INFO, "BluetoothDevices", String.format("   %s %s", device.name, device.address))
                    }

                    val listView = findViewById<ListView>(R.id.listDevicesScaned)
                    listView.adapter = context?.let { MyAdapter(it, bluetoothDevices) }

                    listView.setOnItemClickListener { _, _, i, _ ->
                        val selectedElement = bluetoothDevices[i]
                        // Handle click on selected element
                        Log.println(Log.INFO, "BluetoothService", String.format("connection to %s %s",selectedElement.name, selectedElement.address))
                        val connectThread = ConnectThread(selectedElement, context as DiscoverActivity)
                        connectThread.start()
                    }
                }
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice, val activity: DiscoverActivity) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            createBluetoothSocket(device)
        }

        override fun run() {
            Log.println(Log.INFO, "BluetoothService", "start connection")

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.WARN, "BluetoothService","bluetooth connect permission not granted")
                activity.finish()
            }
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            
            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                Log.println(Log.INFO, "BluetoothService", "connected to the device!")
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                transfer(socket)

            }
        }

        private fun createBluetoothSocket(device : BluetoothDevice): BluetoothSocket? {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.WARN, "BluetoothService","bluetooth connect permission not granted (2)")
                activity.finish()
            }
            return device.createRfcommSocketToServiceRecord(MY_UUID)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        Log.println(Log.INFO, "BluetoothService","get Adapter")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter == null) {
            Log.println(Log.ERROR, "BluetoothService","bluetooth not available on this phone")
            finish()
        }

        listPairedBluetoothDevices()
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

    //list all known devices
    private fun listPairedBluetoothDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            finish()
            Log.println(Log.WARN, "BluetoothService", "Bluetooth permission is not allowed")
        }

        Log.println(Log.INFO, "BluetoothDevices", "already connected devices:")

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            alreadyPairedBluetoothDevices .add(device)

            Log.println(Log.INFO, "BluetoothDevices", "   ${device.name} ${device.address}")
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

    private fun transfer(socket: BluetoothSocket) {
        val intent = Intent(this, TransferActivity::class.java)
        startActivity(intent)
    }
}