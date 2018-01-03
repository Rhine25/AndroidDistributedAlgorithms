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
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.UUID;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.FROM;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_ROUTING_TABLE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_STRING;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_TOKEN;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_WHATS_MY_MAC;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_YOUR_MAC;

/**
 * Created by rhine on 23/10/17.
 */
public class MyBluetoothService {
    private static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");
    static final String ALL = "ALL";
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
        public static final int MESSAGE_DISCONNECTION = 6;

        byte TYPE_STRING = 0x00;
        byte TYPE_ROUTING_TABLE = 0x01;
        byte TYPE_TOKEN = 0x02;
        byte TYPE_WHATS_MY_MAC = 0x03;
        byte TYPE_YOUR_MAC = 0x04;

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

    private void setConnection(BluetoothSocket socket){
        ConnectedThread thread = new ConnectedThread(socket);
        thread.start();
        mConnectedThreads.add(thread);
    }

    void sendMessage(MessagePacket message){
        if(message.destMAC.equals(ALL)){ //send message to all directly connected devices
            for(ConnectedThread connectedThread : mConnectedThreads) {
                if (connectedThread == null) {
                    Log.d(TAG, "connected thread is null");
                } else {
                    byte[] out = Utils.serializeMessage(message);
                    connectedThread.write(out);
                    getInfoToUIThread(MessageConstants.MESSAGE_WRITE, out, "to", connectedThread.mmDevice.getName());
                }
            }
        }
        else{ //send message to a specific device
            String nextHop = mRoutingTable.getNextHopMAC(message.destMAC);
            for(ConnectedThread connectedThread : mConnectedThreads) {
                if (connectedThread == null) {
                    Log.d(TAG, "connected thread is null");
                } else {
                    if(connectedThread.getRemoteDevice().getAddress().equals(nextHop)) {
                        byte[] out = Utils.serializeMessage(message);
                        connectedThread.write(out);
                        getInfoToUIThread(MessageConstants.MESSAGE_WRITE, out, "to", connectedThread.mmDevice.getName());
                        return;
                    }
                }
            }
        }

    }

    void sendMessageBroadcast(byte[] out){ //broadcast message to all devices on the network
        for(Pair pair : mRoutingTable.getMACToName()){
            //TODO send message to device
        }
    }

    void sendMessage_(byte[] out, BluetoothDevice dest){ //send message to any device on the network
        Object[] entry = mRoutingTable.getShortestPathTo(dest.getAddress());
        //TODO add dest to messages
    }

    void sendRoutingTable(BluetoothDevice dest){
        byte[] serializedTable = Utils.serializeRoutingTable(mRoutingTable);
        if(serializedTable == null){
            Log.d(TAG, "Can't send routing table, is null");
        }
        else {
            byte[] table = Utils.getConstructedMessage(TYPE_ROUTING_TABLE, serializedTable);
            MessagePacket messagePacket = new MessagePacket(mRoutingTable.getMyMAC(), dest.getAddress(), table);
            sendMessage(messagePacket);
        }
    }

    RoutingTable updateRoutingFrom(String fromMAC, RoutingTable neighbourTable){
        return mRoutingTable.updateFrom(fromMAC, neighbourTable.getTable());
    }

    String getRoutingTableStr(){
        return mRoutingTable.toString();
    }

    String getRoutingBindingsStr(){
        return mRoutingTable.getMACToNameBindings();
    }

    String getConnectedThreadsStr(){
        String str = "";
        for(ConnectedThread connectedThread : mConnectedThreads) {
            str += connectedThread.toString() + "\n";
        }
        return str;
    }

    boolean connectedDevice(BluetoothDevice device){
        for(ConnectedThread connectedThread : mConnectedThreads) {
            if(connectedThread.getRemoteDevice().getAddress().equals(device.getAddress())){
                return true;
            }
        }
        return false;
    }

    boolean knowMyMAC(){
        return !mRoutingTable.getMyMAC().equals("");
    }

    String getMyMAC(){
        return mRoutingTable.getMyMAC();
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
                mmInStream.read(mmBuffer);
                MessagePacket message = Utils.deserializeMessage(mmBuffer);
                if(!message.destMAC.equals(getMyMAC())){

                }
                numBytes = message.data.length;
                byte msgType = message.data[0];
                byte[] data = new byte[numBytes];
                System.arraycopy(message.data, 0, data, 0, numBytes);
                //TODO check if for me, if not, just send it to right person
                switch (msgType) {
                    case TYPE_STRING:
                        getInfoToUIThread(MessageConstants.MESSAGE_READ, data, FROM, mmDevice.getName());
                        break;
                    case TYPE_ROUTING_TABLE:
                        getInfoToUIThread(MessageConstants.MESSAGE_READ, data
                                , FROM, mmDevice.getAddress());
                        Log.d(TAG, "Received routing table from " + mmDevice.getName());
                        break;
                    case TYPE_TOKEN:
                        getInfoToUIThread(MessageConstants.MESSAGE_READ, data);
                        Log.d(TAG, "Received token from " + mmDevice.getName());
                        break;
                    case TYPE_WHATS_MY_MAC:
                        byte[] msgData = Utils.getConstructedMessage(TYPE_YOUR_MAC, mmDevice.getAddress().getBytes());
                        MessagePacket messagePacket = new MessagePacket(mRoutingTable.getMyMAC(), mmDevice.getAddress(), msgData);
                        sendMessage(messagePacket);
                        Log.d(TAG, "Received mac request from " + mmDevice.getName());
                        break;
                    case TYPE_YOUR_MAC:
                        mRoutingTable.setMyMAC(new String(Utils.extractDataFromMessage(data)));
                        Log.d(TAG, "Received my mac from " + mmDevice.getName() + " : " + mRoutingTable.getMyMAC());
                    default:
                        Log.e(TAG, "received unkwown message type " + msgType);
                        break;
                }
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                getInfoToUIThread(MESSAGE_DISCONNECTION, mmDevice.getName());
                mConnectedThreads.remove(this);
                break;
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
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

        @Override
        public String toString() {
            return "Connected to " + mmDevice.getName();
        }
    }
}