package fr.isep.bluetoothshare

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream


class TransferActivity : AppCompatActivity() {

    private val socket: BluetoothSocket = BluetoothSocketManager.getSocket()
    private val handler: TransferHandler = TransferHandler()
    private val thread: TransferThread = TransferThread()
    private val messages = ArrayList<String>()
    private val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages)

    private inner class TransferThread : Thread() {

        private val input: InputStream = socket.inputStream
        private val output: OutputStream = socket.outputStream
        private val buffer: ByteArray = ByteArray(1024)

        override fun run() {
            while (true) {
                var content: String
                val bytes = input.read(buffer)
                if (bytes > 0) {
                    content = String(buffer, 0, bytes)
                    val message = Message()
                    val bundle = Bundle()
                    bundle.putString("content", content);
                    message.data = bundle
                    handler.sendMessage(message);
                }

            }
        }

        fun send(message: String) {
            output.write(message.toByteArray())
            output.flush()
        }

    }

    @SuppressLint("HandlerLeak")
    private inner class TransferHandler() : Handler() {
        override fun handleMessage(message: Message) {
            message.data.getString("content")?.let { addMessage(it) }
            super.handleMessage(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        val listView = findViewById<ListView>(R.id.transferActivityMessages)
        listView.adapter = adapter
        thread.start()
        val button = findViewById<Button>(R.id.transferActivityButton)
        button.setOnClickListener {
            val textView: TextView = findViewById(R.id.transferActivitySendMessage)
            val content = textView.text.toString()
            addMessage("[SENT] $content")
            thread.send("[RECEIVED] $content")
        }
    }

    fun addMessage(message: String)  {
        messages.add(message)
        adapter.notifyDataSetChanged()
    }

}