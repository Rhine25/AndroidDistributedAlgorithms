package com.jujucecedudu.androidcomm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import static com.jujucecedudu.androidcomm.MyBluetoothService.ALL;
import static com.jujucecedudu.androidcomm.MyBluetoothService.ConnectedThread.NO_RING;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.FROM;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_CONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_READ;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_WRITE;
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

public class MainActivity extends AppCompatActivity implements DeviceAdapter.ListItemClickListener{
    private static final String TAG = "BLUETOOTH_TEST_MAIN";

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    private static final int REQUEST_DISCOVERABLE = 22;
    private static final int LOCATION = 70;

    private DeviceAdapter mDeviceAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private MyBluetoothService mBluetoothService;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private Button mToken;
    private Button mRunAlgo;
    private TextView mMessagesTextView;
    private TextView mConnectionsTextView;
    private TextView mRoutingTableTextView;
    private TextView mRoutingBindingsTextView;
    private TextView mConnectedThreadsTextView;
    private Toast mToast;
    private BluetoothDevice mNext;
    private BluetoothDevice mPrevious;
    private byte mRingStatus;
    private boolean autoConnect;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceMAC = device.getAddress();
                    Log.i(TAG, "Discovered device : " + deviceName + " " + deviceMAC);
                    mDeviceAdapter.addDeviceData(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.i(TAG, "onReceive, discovery started");
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i(TAG, "onReceive, discovery finished");
                    mProgressBar.setVisibility(View.GONE);
                    if(autoConnect){
                        ArrayList<BluetoothDevice> discovered = mDeviceAdapter.getDiscoveredDevices();
                        for(BluetoothDevice discoveredDevice : discovered){
                            mBluetoothService.connect(discoveredDevice);
                        }
                    }
                    break;
                default:
                    Log.i(TAG, "onReceive, action is not handled : " + action);
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] byteMsgW = (byte[]) msg.obj;
                    byte msgTypeW = byteMsgW[0];
                    byte[] dataW = Utils.extractDataFromMessage(byteMsgW);
                    Bundle to = msg.getData();
                    String dest = to.getString("to");
                    String readableMsgW;
                    switch (msgTypeW) {
                        case TYPE_ROUTING_TABLE:
                            readableMsgW = "routing table";
                            break;
                        case TYPE_STRING:
                            readableMsgW = new String(dataW);
                            break;
                        case TYPE_TOKEN:
                            readableMsgW = "token";
                            break;
                        case TYPE_WHATS_MY_MAC:
                            readableMsgW = "mac request";
                            break;
                        case TYPE_YOUR_MAC:
                            readableMsgW = "mac answer";
                            break;
                        case TYPE_RING_STATUS:
                            readableMsgW = "ring status";
                            break;
                        case TYPE_YOUR_NEXT:
                            readableMsgW = "next";
                            break;
                        case TYPE_SEARCH_IN_RING:
                            readableMsgW = "analyse ring";
                            break;
                        default:
                            readableMsgW = "";
                            break;
                    }
                    Log.i(TAG, "Sent : " + readableMsgW/*Arrays.toString(dataW)*/ + "/" + dataW.length + " to " + dest);
                    mMessagesTextView.append("Sent : " + readableMsgW + " to " + dest + "\n");
                    break;
                case MESSAGE_READ:
                    byte[] byteMsgR = (byte[]) msg.obj;
                    byte msgTypeR = byteMsgR[0];
                    byte[] dataR = Utils.extractDataFromMessage(byteMsgR);
                    Bundle from = msg.getData();
                    String expedMAC = from.getString(FROM);
                    String readableMsgR;
                    switch (msgTypeR) {
                        case TYPE_STRING:
                            readableMsgR = new String(dataR);
                            break;
                        case TYPE_ROUTING_TABLE:
                            readableMsgR = "routing table";
                            RoutingTable table = Utils.deserializeRoutingTable(dataR);
                            if(table != null) {
                                Log.i(TAG, "Received routing table " + table);
                                RoutingTable newEntries = mBluetoothService.updateRoutingFrom(msg.getData().getString(FROM), table);
                                if(newEntries.getNbDevicesConnected() > 0){
                                    //update others about the new entry(ies) in the routing table
                                    byte[] data = Utils.getConstructedMessage(TYPE_ROUTING_TABLE, Utils.serializeRoutingTable(newEntries));
                                    MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), ALL, data);
                                    mBluetoothService.sendMessage(messagePacket);
                                }
                                String updatedTableStr = mBluetoothService.getRoutingTableStr();
                                Log.i(TAG, "Updated my table to : " + updatedTableStr);
                                mRoutingTableTextView.setText(updatedTableStr);
                                mRoutingBindingsTextView.setText(mBluetoothService.getRoutingBindingsStr());
                            }
                            else{
                                String errorMsg = "Routing table received null";
                                Log.d(TAG, errorMsg);
                                mToast = Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_LONG);
                            }
                            break;
                        case TYPE_TOKEN:
                            mDeviceAdapter.clearDevices();
                            readableMsgR = "token";
                            makeTokenVisible();
                            break;
                        case TYPE_RING_STATUS:
                            Log.d(TAG, "TREATING RING STATUS");
                            BluetoothDevice expeditor = mBluetoothService.getDeviceFromMAC(expedMAC);
                            if(dataR[0] == NO_RING){
                                Log.d(TAG, "He's not in a ring");
                                if(mRingStatus == NO_RING){
                                    Log.d(TAG, "I'm not in a ring either");
                                    mNext = expeditor;
                                    mPrevious = expeditor;
                                    Log.d(TAG, "My next is now " + mNext.getName());
                                }
                                else{
                                    Log.d(TAG, "I'm already in a ring, I'll invite him in");
                                    byte[] data = Utils.getConstructedMessage(TYPE_YOUR_NEXT, mNext.getAddress().getBytes());
                                    MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), expeditor.getAddress(), data);
                                    mBluetoothService.sendMessage(messagePacket);
                                    mNext = expeditor;
                                }
                            }
                            else{ //in RING
                                Log.d(TAG, "He's already in a ring");
                                if(mRingStatus == NO_RING){
                                    Log.d(TAG, "I'm alone and sad, he surely is going to invite me in");
                                    //nothing to do here, it's done in your next reception
                                }
                                else{
                                    Log.d(TAG, "I'm in a ring too, maybe we're already part of the same ring party ?");
                                    if(mBluetoothService.getMyMAC().compareTo(expedMAC) > 0){
                                        byte[] byteTargetMAC = expedMAC.getBytes();
                                        byte[] msgData = new byte[byteTargetMAC.length + 1];
                                        msgData[0] = (byte)0;
                                        System.arraycopy(byteTargetMAC, 0, msgData, 1, byteTargetMAC.length);
                                        byte[] data = Utils.getConstructedMessage(TYPE_SEARCH_IN_RING, msgData);
                                        MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), mNext.getAddress(), data);
                                        mBluetoothService.sendMessage(messagePacket);
                                    }
                                    else{
                                        //je suis le maillon faible, pas à moi de check
                                    }
                                }
                            }
                            readableMsgR = "ring status";
                            break;
                        case TYPE_YOUR_NEXT:
                            String nextMAC = Arrays.toString(dataR);
                            mNext = mBluetoothService.getDeviceFromMAC(nextMAC);
                            String previousMAC = expedMAC;
                            mPrevious = mBluetoothService.getDeviceFromMAC(previousMAC);
                            Log.d(TAG, "Set " + mNext.getName() + " as next and " + mPrevious.getName() + " as previous");
                            readableMsgR = "new next";
                            break;
                        case TYPE_RING_FUSION:
                            //send expedMAC our next;
                            byte[] data = Utils.getConstructedMessage(TYPE_YOUR_NEXT, expedMAC.getBytes());
                            MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), expedMAC, data);
                            mBluetoothService.sendMessage(messagePacket);
                            //set him as our new next
                            mNext = mBluetoothService.getDeviceFromMAC(expedMAC);
                            readableMsgR = "ring fusion";
                            break;
                        case TYPE_FOR_NEXT: //
                            MessagePacket message = Utils.deserializeMessage(dataR); //TODO check if should pass mmBuffer length too
                            message.setDest(mNext.getAddress());
                            mBluetoothService.sendMessage(message);
                        default:
                            readableMsgR = "Developer message type";
                            Log.d(TAG, "Read message from API, transmitting it");
                            API.onMessage(byteMsgR);
                            break;
                    }
                    Log.i(TAG, "Read " + Arrays.toString(dataR) + "/" + dataR.length + " from " + expedMAC);
                    mMessagesTextView.append("Read " + readableMsgR + " from " + expedMAC + "\n");
                    break;
                case MESSAGE_CONNECTION:
                    BluetoothDevice device = (BluetoothDevice)msg.obj;
                    String deviceName = device.getName();
                    Log.i(TAG, "Connected to " + deviceName + " " + device.getAddress());
                    mConnectionsTextView.append(deviceName + "\n");

                    if (!mBluetoothService.knowMyMAC()){
                        Log.d(TAG, "Asked for my MAC");
                        byte[] request = new byte[]{TYPE_WHATS_MY_MAC};
                        MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), device.getAddress(), request);
                        mBluetoothService.sendMessage(messagePacket);
                    }
                    while(!mBluetoothService.knowMyMAC()){} //WARNING this is gonna freeze the app, but it's a little patchup

                    Log.d(TAG, "My initial routing : \n'" + mBluetoothService.getRoutingTableStr() + "'");
                    mBluetoothService.sendRoutingTable(device);
                    mConnectedThreadsTextView.setText(mBluetoothService.getConnectedThreadsStr());
                    //send ring status request
                    Log.d(TAG, "Asked for ring status");
                    byte[] request = new byte[]{TYPE_RING_STATUS, mRingStatus};
                    MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), device.getAddress(), request);
                    mBluetoothService.sendMessage(messagePacket);
                    Log.d(TAG, "RING STATUS sent : " + Arrays.toString(messagePacket.getData()));
                    break;
                case MESSAGE_DISCONNECTION:
                    Log.i(TAG, msg.obj + " disconnected ");
                    mConnectionsTextView.append("-" + msg.obj + "\n");
                    mConnectedThreadsTextView.setText(mBluetoothService.getConnectedThreadsStr());
                    //TODO remove entry from routingtable and send update to others
                    break;
                default:
                    Log.e(TAG, "Received message of unknown type");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoConnect = false;
        mRingStatus = NO_RING;
        mNext = null;
        mPrevious = null;

        mProgressBar = (ProgressBar) findViewById(R.id.pb_progress_indicator);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_available_devices);
        mToken = (Button) findViewById(R.id.bt_token);
        mMessagesTextView = (TextView) findViewById(R.id.tv_messages);
        mConnectionsTextView = (TextView) findViewById(R.id.tv_connections);
        mRoutingTableTextView = (TextView) findViewById(R.id.tv_routing_table);
        mRoutingBindingsTextView = (TextView) findViewById(R.id.tv_routing_bindings);
        mConnectedThreadsTextView = (TextView) findViewById(R.id.tv_connected_threads);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mDeviceAdapter = new DeviceAdapter(this);
        mRecyclerView.setAdapter(mDeviceAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothService = new MyBluetoothService(this, mHandler);

        if(mBluetoothAdapter == null){
            //device does not support bluetooth
            //TODO display text error message
            Log.d(TAG, "Bluetooth adapter is null");
        }
        else{
            askForPermission(Manifest.permission.ACCESS_COARSE_LOCATION, LOCATION);
            enableBluetooth();
            registerForDiscoveryInfo();
            while(!mBluetoothAdapter.isEnabled()){}
            mBluetoothService.accept();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.i(TAG, "Unregistered receiver");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_discover:
                mDeviceAdapter.clearDevices();
                mBluetoothService.discover();
                return true;
            case R.id.action_discoverable:
                makeDiscoverable();
                return true;
            case R.id.action_start_ring:
                initRing();
                return true;
            case R.id.action_auto_connect:
                autoConnect = true;
                makeDiscoverable();
                mDeviceAdapter.clearDevices();
                mBluetoothService.discover();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult " + requestCode + ", " + resultCode);
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if(resultCode != RESULT_OK){
                Log.d(TAG, "Bluetooth was not enabled");
            }
            else{
                Log.i(TAG, "Bluetooth enabled");
            }
        }
        else if(requestCode == REQUEST_DISCOVERABLE){
            if(requestCode != RESULT_OK){
                Log.d(TAG, "Device was not made discoverable");
            }
            else{
                Log.i(TAG, "Device made discoverable");
            }
        }
    }

    private void askForPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
        }
    }

    private void registerForDiscoveryInfo(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private void enableBluetooth(){
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    public void toggleBluetooth(View view){
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
        else{
            mBluetoothAdapter.enable();
        }
        Log.i(TAG, "toggle BT");
    }

    void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        Log.i(TAG, "Requested discoverable");
    }

    public void sayHello(View view){
        Log.d(TAG, "HELLO");
        byte[] str = "Hello !".getBytes();
        byte[] data = Utils.getConstructedMessage(TYPE_STRING, str);
        MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), ALL, data);
        mBluetoothService.sendMessage(messagePacket);
        //TODO CHECK THIS, CLICKING HELLO DOES NOT DO ANYTHING in broadcast :/
        //TODO swipe tabs ! \0/
    }

    public void clearMessages(View view){
        mMessagesTextView.setText("");
    }

    private void createToken(){
        makeTokenVisible();
    }

    public void sendToken(View view){
        if(mNext != null) {
            Log.d(TAG, "TOKEN SENT");
            byte[] token = new byte[]{TYPE_TOKEN};
            MessagePacket messagePacket = new MessagePacket(mBluetoothService.getMyMAC(), mNext.getAddress(), token);
            mBluetoothService.sendMessage(messagePacket);
            makeTokenInvisible();
        }
    }

    private void makeTokenVisible(){
        mToken.setVisibility(View.VISIBLE);
    }

    private void makeTokenInvisible(){
        mToken.setVisibility(View.GONE);
    }

    private void initRing(){
        mDeviceAdapter.clearDevices();
        createToken();
    }

    @Override
    public void onListItemClick(BluetoothDevice device) {
        if (mToast != null) {
            mToast.cancel();
        }
        String text;
        if(!mBluetoothService.connectedDevice(device)) {
            text = device.getName() + " " + device.getAddress() + " connecting to";
            mBluetoothService.connect(device);
        }
        else{
            text = "Good try :p";
            /*text = device.getName() + " " + device.getAddress() + " set as next";
            setNext(device);*/
        }

        mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void setNext(BluetoothDevice device) {
        mNext = device;
    }

    public void runAlgo(View view){
        AlgoLamportChat algo = new AlgoLamportChat();
        new API(algo, mBluetoothService);
        algo.init();
    }
}
