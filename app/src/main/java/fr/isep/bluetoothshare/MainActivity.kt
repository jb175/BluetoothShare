package fr.isep.bluetoothshare

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    companion object {
        //static values to put an id on requests
        private const val REQUEST_BLUETOOTH_CONNECT = 1
    }

    //which button is clicked
    private var buttonClicked = 0
    //bluetooth adapter to perform actions with bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null


    //register to enable bluetooth
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.println(Log.WARN, "BluetoothService", "Bluetooth has not or could not be enabled")
        } else {
            Log.println(Log.INFO, "BluetoothService", "Bluetooth enable")
            startActivity()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.println(Log.INFO, "BluetoothService","get Adapter")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter == null) {
            Log.println(Log.ERROR, "BluetoothService","bluetooth not available on this phone")
            finish()
        }

        val discoverActivityButton = findViewById<Button>(R.id.discoverActivityButton)
        discoverActivityButton.setOnClickListener {
            buttonClicked = 0
            checkBluetoothPermission()
        }

        val receiveActivityButton = findViewById<Button>(R.id.receiveActivityButton)
        receiveActivityButton.setOnClickListener {
            buttonClicked = 1
            checkBluetoothPermission()
        }
    }

    //execute actions when a permission is granted or not
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.println(Log.WARN, "BluetoothService","bluetooth permission not granted. Couldn't connect to bluetooth device")
                    return
                } else {
                    Log.println(Log.INFO, "BluetoothService","bluetooth permission granted")
                    enableBluetooth()
                }
            }
        }
    }

    //check if the bluetooth connect permission is allowed
    private fun checkBluetoothPermission() {
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
            startActivity()
        }
    }

    //ask to enable bluetooth
    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.println(Log.WARN, "BluetoothService", "bluetooth permission not allowed")
            return
        }
        bluetoothEnableLauncher.launch(enableBtIntent)
    }

    private fun startActivity() {
        val intent = if (buttonClicked == 0) {
            Intent(this, DiscoverActivity::class.java)
        } else {
            Intent(this, ReceiveActivity::class.java)
        }
        startActivity(intent)
    }
}