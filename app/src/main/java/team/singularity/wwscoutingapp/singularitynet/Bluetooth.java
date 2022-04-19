package team.singularity.wwscoutingapp.singularitynet;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    final int REQUEST_ENABLE_BT = 1;
    public final int DEVICE_NAME = 0;
    public final int DEVICE_HARDWARE_ADDRESS = 1;
    final String NAME = "SingularityNet";
    final UUID UUID_INSECURE = UUID.fromString("90172b5b-9137-463e-9e4e-8c2f2735cdd1"); //must be same for devices that wish to connect
    final String TAG = "Bluetooth";

    Activity activity;
    Set<BluetoothDevice> pairedDevices;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    ConnectThread connectThread;
    AcceptThread acceptThread;
    ConnectedThread connectedThread;

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
        int permissionCheck = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH);
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

    public void connectToDevice(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }
    public void connectToDevice(int index) {
        BluetoothDevice device = getPairedDevice(index);
        connectToDevice(device);
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
    public BluetoothDevice getPairedDevice(int index) {
        pairedDevices = bluetoothAdapter.getBondedDevices();
        return (BluetoothDevice) pairedDevices.toArray()[index];
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
        Log.i(TAG, "got a connection");
        Toast.makeText(activity, "got a message", Toast.LENGTH_SHORT).show();
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

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        final String TAG = "Connect Thread";
        UUID deviceUUID;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            deviceUUID = uuid;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // UUID is the app's UUID string of the device being connected to.
                Log.i(TAG, "Attempting to make InsecureRfcommSocket with UUID of: " + deviceUUID);
                tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed. Could not make InsecureRfcommSocket", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "ConnectThread running.");
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.e(TAG, "Could not connect to device.");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            clientConnected(mmSocket, mmDevice);
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
                // UUID_INSECURE is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID_INSECURE);
                Log.i(TAG, "Setting listening thread wit UUID of: " + UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "AcceptThread running");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            // This is a blocking call, so it's being run on a different thread
            // in order to keep the UI running.
            while (true) {
                Log.i(TAG, "Listening for client.");
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed.", e);
                    break;
                }
                Log.i(TAG, "Tried to get a connection.");
                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.i(TAG, "Connection accepted.");
                    serverConnected(socket);
                    try {
                        mmServerSocket.close();
                        Log.i(TAG, "Connection closed.");
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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        final String TAG = "Connected Thread";

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "Starting Connected Thread.");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                // Read from InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.i(TAG, "Incoming Message: " + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream. " + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.i(TAG, "Writing to OutputStream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Write: Error writing to OutputStream. " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                Log.i(TAG, "Connection closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void start() {
        Log.i(TAG, "Started.");
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.i(TAG, "startClient method called");
        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    private void clientConnected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.i(TAG, "Client Connected method started.");
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }
    private void serverConnected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.i(TAG, "Server Connected method started.");
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    public void write(byte[] out) {
        connectedThread.write(out);
    }
}


