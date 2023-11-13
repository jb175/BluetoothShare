package fr.isep.bluetoothshare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val discoverActivityButton = findViewById<Button>(R.id.discoverActivityButton)
        discoverActivityButton.setOnClickListener {
            val intent = Intent(this, DiscoverActivity::class.java)
            startActivity(intent)
        }

        val receiveActivityButton = findViewById<Button>(R.id.receiveActivityButton)
        receiveActivityButton.setOnClickListener {
            val intent = Intent(this, ReceiveActivity::class.java)
            startActivity(intent)
        }
    }

}