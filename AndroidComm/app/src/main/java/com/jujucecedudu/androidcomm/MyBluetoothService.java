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
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.FROM;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_FOR_NEXT;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_RING_FUSION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_RING_STATUS;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_ROUTING_TABLE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_SEARCH_IN_RING;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_STRING;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_TOKEN;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_WHATS_MY_MAC;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_YOUR_MAC;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_YOUR_NEXT;

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
        byte TYPE_RING_STATUS = 0x05;
        byte TYPE_YOUR_NEXT = 0x06;
        byte TYPE_SEARCH_IN_RING = 0x07;
        byte TYPE_RING_FUSION = 0x08;
        byte TYPE_FOR_NEXT = 0x09;

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
        if(connectedDevice(device)){
            Log.d(TAG, "Already connected to device " + device.getName() +", won't connect to it again");
            return;
        }
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

    void sendMessage(MessagePacket message){ //TODO faire les petites fonctions
        Log.d(TAG, "SEND MESSAGE");
        if(message.getDest().equals(ALL)){ //send message to all directly connected devices
            Log.d(TAG, "TO ALL");
            for(ConnectedThread connectedThread : mConnectedThreads) {
                Log.d(TAG, "TO " + connectedThread.mmDevice.getName());
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
            String nextHop = mRoutingTable.getNextHopMAC(message.getDest());
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

    void sendMessageBroadcast(MessagePacket message){ //broadcast message to all devices on the network
        for(Pair pair : mRoutingTable.getMACToName()){
            String MAC = (String)pair.first;
            message.setDest(MAC);
            sendMessage(message);
        }
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

    BluetoothDevice getDeviceFromMAC(String MAC){
        for(ConnectedThread thread : mConnectedThreads){
            if(thread.mmDevice.getAddress().equals(MAC)){
                return thread.mmDevice;
            }
        }
        return null;
    }

    public String[] getAllDevicesMACs(){
        return mRoutingTable.getDevicesMACs();
    }
    
    public int getNbDevicesConnected(){
        return mRoutingTable.getNbDevicesConnected();
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

        public static final byte RING_UNKNOWN = 0x00;
        public static final byte RING = 0x01;
        public static final byte NO_RING = 0x02;

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;
        private int mmDeviceRingStatus;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            mmDevice = mmSocket.getRemoteDevice();
            mmDeviceRingStatus = RING_UNKNOWN;
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
            mmBuffer = new byte[1024]; //WARNING buffer size should be larger than the max message size, or RIP
            int numBytes;

            while (true) try {
                mmInStream.read(mmBuffer);
                MessagePacket message = Utils.deserializeMessage(mmBuffer); //TODO check if should pass mmBuffer length too
                if(!message.getDest().equals(getMyMAC())){ //transfer message
                    sendMessage(message);
                }
                else { //message is for me, treat it
                    numBytes = message.getData().length;
                    byte msgType = message.getData()[0];
                    byte[] data = new byte[numBytes];
                    System.arraycopy(message.getData(), 0, data, 0, numBytes);
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
                        case TYPE_RING_STATUS:
                            mmDeviceRingStatus = data[1];
                            getInfoToUIThread(MessageConstants.MESSAGE_READ, data
                                    , FROM, mmDevice.getAddress());
                            Log.d(TAG, "Received ring status from " + mmDevice.getName());
                            break;
                        case TYPE_YOUR_NEXT:
                            getInfoToUIThread(MessageConstants.MESSAGE_READ, data, FROM, mmDevice.getAddress());
                            Log.d(TAG, "Received new next from " + mmDevice.getName());
                            break;
                        case TYPE_SEARCH_IN_RING:
                            byte[] targetMACBytes = new byte[data.length-1];
                            System.arraycopy(data, 1, targetMACBytes, 0, data.length - 1);
                            String targetMAC = Arrays.toString(targetMACBytes);
                            if(getMyMAC().equals(message.getExpMAC())){ //if address is mine, check first byte value and deal with the shit of the rings fusion
                                if(data[1] == 0){ //if the rings are distinct
                                    getInfoToUIThread(MessageConstants.MESSAGE_READ, TYPE_RING_FUSION, FROM, message.getExpMAC());
                                }
                            }
                            else{ //if not, send to next
                                byte[] messageByte = new byte[numBytes+1];
                                messageByte[0] = TYPE_FOR_NEXT;
                                System.arraycopy(mmBuffer, 0, messageByte, 1, numBytes);
                                //TODO should not copy buffer but serialize messagePacket
                                if(getMyMAC().equals(targetMAC)){ //if i'm the target,
                                    data[1] = (byte)1; //TODO not in data but in the message we'll send
                                }
                                getInfoToUIThread(MessageConstants.MESSAGE_READ, messageByte);
                            }
                        default:
                            Log.e(TAG, "received unkwown message type " + msgType);
                            break;
                    }
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