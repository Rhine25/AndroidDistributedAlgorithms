package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;

/**
 * Created by rhine on 23/10/17.
 */
public class MyBluetoothService {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int NEW_CONNECTION = 4;

    private static final String TAG = "BLUETOOTH_TEST_SERVICE";
    private Handler mHandler; // handler that gets info from Bluetooth service
    private BluetoothAdapter mmBluetoothAdapter;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectThread;
    private LinkedList<ConnectedThread> mConnectedThreads;
    private RoutingTable mRoutingTable;

    public MyBluetoothService(Context context, Handler handler){
        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mConnectedThreads = new LinkedList<>();
        mRoutingTable = new RoutingTable();
    }

    void discover(){
        if (mmBluetoothAdapter.startDiscovery()) {
            Log.i(TAG, "Launched discovery");
            //it is asynchronous so the discovery is not instantaneous
        } else {
            Log.i(TAG, "Discovery could not launch");
        }

        //myBluetoothAdapter.cancelDiscovery();
        //here should pair to a device after stopping discovery process
    }

    void connect(BluetoothDevice device){
        Log.i(TAG, "I'm gonna try to pair and connect");
        Log.i(TAG, "Target is : " + device.getName());
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }

    void accept(){
        Log.i(TAG, "I'm gonna accept");
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    void sendMessage(byte[] out){
        for(ConnectedThread connectedThread : mConnectedThreads) {
            if (connectedThread == null) {
                Log.d(TAG, "connected thread is null");
            } else {
                connectedThread.write(out);
            }
        }
    }

    void sendMessage(byte[] out, BluetoothDevice dest){
        for(ConnectedThread connectedThread : mConnectedThreads) {
            if (connectedThread == null) {
                Log.d(TAG, "connected thread is null");
            } else {
                if(connectedThread.mmSocket.getRemoteDevice() == dest)
                connectedThread.write(out);
            }
        }
    }

    void sendRoutingTable(BluetoothDevice dest){
        sendMessage(serializeRoutingTable(mRoutingTable), dest); //TODO should check not null serialize
    }

    byte[] serializeRoutingTable(RoutingTable table){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(table);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    RoutingTable deserializeRoutingTable(byte[] bytes){
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(in);
            try {
                RoutingTable table = (RoutingTable) ois.readObject();
                return table;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void setConnection(BluetoothSocket socket){
        ConnectedThread thread = new ConnectedThread(socket);
        thread.start();
        mConnectedThreads.add(thread);
    }

    void addRoutingEntry(String targetMAC, int cost, String nextHopMAC){
        mRoutingTable.addEntry(targetMAC, cost, nextHopMAC);
    }

    void updateRoutingFrom(String fromMAC, RoutingTable neighbourTable){
        mRoutingTable.updateFrom(fromMAC, neighbourTable);
    }

    String getRoutingTableStr(){
        return mRoutingTable.toString();
    }

    LinkedList<ConnectedThread> getConnectedThreads(){
        return mConnectedThreads;
    }

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    public interface MessageConstants {
        public static final int MESSAGE_STATE_CHANGE = 1;
        public static final int MESSAGE_READ = 2;
        public static final int MESSAGE_WRITE = 3;
        public static final int MESSAGE_CONNECTION = 4;
        public static final int MESSAGE_TOAST = 5;
        public static final int MESSAGE_DISCONNECTION = 6;
        public static final int MESSAGE_ROUTING_TABLE = 7;

        // ... (Add other message types here as needed.)
    }

    class AcceptThread extends Thread {
        private static final String TAG = "BLUETOOTH_TEST_ACCEPT";

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mmBluetoothAdapter.listenUsingRfcommWithServiceRecord("AndroidComm", MainActivity.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "run");
            BluetoothSocket socket = null;
            int nbConnections = 0;
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
                    setConnection(socket);
                    Log.i(TAG, "Server accepted a client");
                    nbConnections ++;
                    /*try {
                        mmServerSocket.close(); //or let it be if we want to accept other connections
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the connect socket", e);
                    }*/
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

    class ConnectThread extends Thread {
        private static final String TAG = "BLUETOOTH_TEST_CONNECT";

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        //private final BluetoothAdapter mBluetoothAdapter;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MainActivity.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mmBluetoothAdapter.cancelDiscovery();
            Log.i(TAG, "run");
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.i(TAG, "Client is connected on a server");

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
            setConnection(mmSocket);
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

    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            Message connectedMsg = mHandler.obtainMessage(
                    MessageConstants.MESSAGE_CONNECTION, mmSocket.getRemoteDevice());
            connectedMsg.sendToTarget();
        }

        public void run() {
            Log.d(TAG, "Connected thread running");
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            //receive routingtable, but we're not sure that's the first thing we receive
            try {
                Log.d(TAG, "Connected thread waiting for routing table");
                numBytes = mmInStream.read(mmBuffer);
                Message readMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_ROUTING_TABLE, mmBuffer);
                Bundle bundle = new Bundle();
                bundle.putString("from", mmSocket.getRemoteDevice().getAddress());
                readMsg.setData(bundle);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "Error while receiving routing table", e);
                e.printStackTrace();
            }
            Log.d(TAG, "Received routing table, waiting for standard messages from friends");

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    String msgStr = new String(mmBuffer, 0, numBytes);
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, msgStr + " from " + mmSocket.getRemoteDevice().getName());
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    Message readMsg = mHandler.obtainMessage(
                            MESSAGE_DISCONNECTION, mmSocket.getRemoteDevice().getName());
                    readMsg.sendToTarget();
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Share the sent message with the UI activity.
                String msgStr = new String(bytes);
                //TODO try with mmBuffer instead of bytes
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, msgStr + " to " + mmSocket.getRemoteDevice().getName());
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                //TODO maybe register to receive those ? 0:)
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        public BluetoothDevice getRemoteDevice(){
            return mmSocket.getRemoteDevice();
        }
    }
}