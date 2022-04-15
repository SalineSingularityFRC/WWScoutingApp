package team.singularity.wwscoutingapp.singularitynet;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    final int REQUEST_ENABLE_BT = 1;
    public final int DEVICE_NAME = 0;
    public final int DEVICE_HARDWARE_ADDRESS = 1;
    final String NAME = "SingularityNet";
    final java.util.UUID UUID = java.util.UUID.fromString("90172b5b-9137-463e-9e4e-8c2f2735cdd1"); //must be same for devices that wish to connect

    Activity activity;
    Set<BluetoothDevice> pairedDevices;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    public Bluetooth(Activity activity) {
        this.activity = activity;
        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!this.activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this.activity, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            this.activity.finish();
        }
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!this.activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this.activity, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            this.activity.finish();
        }
        bluetoothManager = (BluetoothManager) this.activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this.activity, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            this.activity.finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    public String[][] getPairedDevices() {
        pairedDevices = bluetoothAdapter.getBondedDevices();
        String[][] devices = new String[pairedDevices.size()][2];
        if (devices.length > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (int i = 0; i < pairedDevices.size(); i++) {
                devices[i][DEVICE_NAME] = ((BluetoothDevice) pairedDevices.toArray()[i]).getName();
                devices[i][DEVICE_HARDWARE_ADDRESS] = ((BluetoothDevice) pairedDevices.toArray()[i]).getAddress(); // MAC address
            }
        }
        return devices;
    }
    public String[] getPairedDeviceNames() {
        String[][] devices = getPairedDevices();
        String[] names = new String[devices.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = devices[i][DEVICE_NAME];
        }
        return names;
    }
    public String[] getPairedDeviceAddresses() { //mac address
        String[][] devices = getPairedDevices();
        String[] addresses = new String[devices.length];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = devices[i][DEVICE_HARDWARE_ADDRESS];
        }
        return addresses;
    }

    public void manageMyConnectedSocket(BluetoothSocket socket) {
        Toast.makeText(activity, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
    }

    public BluetoothDevice getPairedDevice(String hardwareAddress) {
        String[] deviceAddresses = getPairedDeviceAddresses();
        for (int i = 0; i < deviceAddresses.length; i++) {
            if (deviceAddresses[i].equals(hardwareAddress)) {
                return (BluetoothDevice) bluetoothAdapter.getBondedDevices().toArray()[i];
            }
        }
        return null;
    }

    public void connectToDevice(BluetoothDevice device) {

    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        final String TAG = "Connect Thread";

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        final String TAG = "Accept Thread";

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}


