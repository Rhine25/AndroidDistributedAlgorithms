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
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_ROUTING_TABLE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_STRING;

/**
 * Created by rhine on 23/10/17.
 */
public class MyBluetoothService {
    private static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");
    private static final String TAG = "BLUETOOTH_TEST_SERVICE";

    private Handler mHandler; // handler that gets info from Bluetooth service
    private BluetoothAdapter mmBluetoothAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private LinkedList<ConnectedThread> mConnectedThreads;
    private RoutingTable mRoutingTable;
    private Context mActivity;

    public interface MessageConstants {
        public static final int MESSAGE_READ = 2;
        public static final int MESSAGE_WRITE = 3;
        public static final int MESSAGE_CONNECTION = 4;
        public static final int MESSAGE_TOAST = 5;
        public static final int MESSAGE_DISCONNECTION = 6;
        public static final int MESSAGE_ROUTING_TABLE = 7;

        byte TYPE_STRING = 0x00;
        byte TYPE_ROUTING_TABLE = 0x01;

        public static final String FROM = "from";
    }

    private void getInfoToUIThread(int type, Object data){
        Message msg = mHandler.obtainMessage(type, data);
        msg.sendToTarget();
    }

    private void getInfoToUIThread(int type, Object data, String key, String extraData){
        Message msg = mHandler.obtainMessage(type, data);
        Bundle bundle = new Bundle();
        bundle.putString(key, extraData);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public MyBluetoothService(Context context, Handler handler){
        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mActivity = context;
        mHandler = handler;
        mConnectedThreads = new LinkedList<>();
        mRoutingTable = new RoutingTable();
    }

    void discover(){
        if (!mmBluetoothAdapter.startDiscovery()) {
            Log.d(TAG, "Discovery could not launch");
            Toast.makeText(mActivity, "Error launching discovery", Toast.LENGTH_LONG).show();
        }
    }

    void connect(BluetoothDevice device){
        Log.d(TAG, "New connect thread launched");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    void accept(){
        Log.d(TAG, "New accept thread launched");
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    void setConnection(BluetoothSocket socket){
        ConnectedThread thread = new ConnectedThread(socket);
        thread.start();
        mConnectedThreads.add(thread);
    }

    void sendMessage(byte[] out){
        for(ConnectedThread connectedThread : mConnectedThreads) {
            if (connectedThread == null) {
                Log.d(TAG, "connected thread is null");
            } else {
                connectedThread.write(out);
                String msgStr = new String(out);
                getInfoToUIThread(MessageConstants.MESSAGE_WRITE, msgStr + " to " + connectedThread.mmDevice.getName());
            }
        }
    }

    void sendMessage(byte[] out, BluetoothDevice dest){
        for(ConnectedThread connectedThread : mConnectedThreads) {
            if (connectedThread == null) {
                Log.d(TAG, "connected thread is null");
            } else {
                if(connectedThread.getRemoteDevice() == dest)
                connectedThread.write(out);
                String msgStr = new String(out);
                getInfoToUIThread(MessageConstants.MESSAGE_WRITE, msgStr + " to " + connectedThread.mmDevice.getName());
                return;
            }
        }
    }

    void sendRoutingTable(BluetoothDevice dest){
        byte[] serializedTable = serializeRoutingTable(mRoutingTable);
        if(serializedTable == null){
            Log.d(TAG, "Can't send routing table, is null");
        }
        else {;
            sendMessage(getConstructedMessage(TYPE_ROUTING_TABLE, serializedTable), dest);
        }
    }

    byte[] getConstructedMessage(byte type, byte[] data){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(type);
            outputStream.write(data);
            byte msg[] = outputStream.toByteArray( );
            return msg;
        } catch (IOException e) {
            Log.e(TAG, "Couldn't construct hello msg", e);
            e.printStackTrace();
        }
        return null;
    }

    byte[] serializeRoutingTable(RoutingTable table){
        //TODO c'est pas l'objet qu'on veut serialiser c'est l'arraylist table
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(table.getTable());
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Routing table couldn't be serialized "+ e);
        }
        return null;
    }

    RoutingTable deserializeRoutingTable(byte[] bytes){
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(in);
            try {
                ArrayList<Object[]> table = (ArrayList<Object[]>) ois.readObject();
                return new RoutingTable(table);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void addRoutingEntry(String targetMAC, int cost, String nextHopMAC){
        mRoutingTable.addEntry(targetMAC, cost, nextHopMAC);
    }

    void updateRoutingFrom(String fromMAC, RoutingTable neighbourTable){
        mRoutingTable.updateFrom(fromMAC, neighbourTable.getTable());
    }

    String getRoutingTableStr(){
        return mRoutingTable.toString();
    }

    String getRoutingBindingsStr(){
        return mRoutingTable.getMACToNameBindings();
    }

    class AcceptThread extends Thread {
        private static final String TAG = "BLUETOOTH_TEST_ACCEPT";
        private static final int NB_MAX_CLIENTS = 6;

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mmBluetoothAdapter.listenUsingRfcommWithServiceRecord("AndroidComm", MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "run");
            BluetoothSocket socket = null;
            int nbConnections = 0;
            while (nbConnections < NB_MAX_CLIENTS) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    setConnection(socket);
                    Log.i(TAG, "Server accepted a client");
                    nbConnections ++;
                }
            }
            try {
                mmServerSocket.close();
                Log.i(TAG, "server has max nb clients, socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

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

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            mmBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.i(TAG, "Client is connected on a server");
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            setConnection(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    class ConnectedThread extends Thread {
        private static final String TAG = "BLUETOOTH_TEST_CONNECTD";

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            mmDevice = mmSocket.getRemoteDevice();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

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

            getInfoToUIThread(MessageConstants.MESSAGE_CONNECTION, mmDevice);
        }

        public void run() {
            Log.d(TAG, "Connected thread running");
            mmBuffer = new byte[1024];
            int numBytes;

            while (true) try {
                numBytes = mmInStream.read(mmBuffer);
                byte msgType = mmBuffer[0];
                if (msgType == TYPE_STRING) {
                    String msgStr = new String(mmBuffer, 1, numBytes-1);
                    getInfoToUIThread(MessageConstants.MESSAGE_READ, msgStr + " from " + mmDevice.getName());
                } else if (msgType == TYPE_ROUTING_TABLE) {
                    byte[] byteTable = new byte[mmBuffer.length-1];
                    System.arraycopy(mmBuffer, 1, byteTable, 0, mmBuffer.length-1);
                    getInfoToUIThread(MessageConstants.MESSAGE_ROUTING_TABLE, byteTable
                            , MessageConstants.FROM, mmDevice.getAddress());
                    Log.d(TAG, "Received routing table from " + mmDevice.getName());
                }
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                getInfoToUIThread(MESSAGE_DISCONNECTION, mmDevice.getName());
                break;
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                //TODO try with mmBuffer instead of bytes
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                Toast.makeText(mActivity, "Error sending message", Toast.LENGTH_LONG).show();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        public BluetoothDevice getRemoteDevice(){
            return mmDevice;
        }
    }
}