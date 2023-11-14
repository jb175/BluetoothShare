package fr.isep.bluetoothshare;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketManager {

    private static BluetoothSocket socket;

    public static BluetoothSocket getSocket() {
        return socket;
    }

    public static void setSocket(BluetoothSocket socket) {
        BluetoothSocketManager.socket = socket;
    }

}
