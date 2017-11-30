package com.jujucecedudu.androidcomm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_CONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DISCONNECTION;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_READ;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_ROUTING_TABLE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_WRITE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_ROUTING_TABLE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.TYPE_STRING;

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
    private TextView mMessages;
    private TextView mConnections;
    private TextView mRoutingTable;
    private TextView mRoutingBindings;
    private Toast mToast;

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
                    //mDiscoveredDevicesText.append(deviceName + " " + deviceMAC + "\n");
                    mDeviceAdapter.addDeviceData(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.i(TAG, "onReceive, discovery started");
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i(TAG, "onReceive, discovery finished");
                    mProgressBar.setVisibility(View.GONE);
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
                    if(msgTypeW == TYPE_ROUTING_TABLE){
                        readableMsgW = "routing table";
                    }
                    else{
                        readableMsgW = new String(dataW);
                    }
                    Log.i(TAG, "Sent : " + Arrays.toString(dataW) + "/" + dataW.length + " to " + dest);
                    mMessages.append("Sent : " + readableMsgW + " to " + dest + "\n");
                    break;
                case MESSAGE_READ:
                    byte[] byteMsgR = (byte[]) msg.obj;
                    byte msgTypeR = byteMsgR[0];
                    byte[] dataR = Utils.extractDataFromMessage(byteMsgR);
                    Bundle from = msg.getData();
                    String exped = from.getString("from");
                    String readableMsgR;
                    if(msgTypeR == TYPE_ROUTING_TABLE){
                        readableMsgR = "routing table";
                    }
                    else{
                        readableMsgR = new String(dataR);
                    }
                    Log.i(TAG, "Read " + Arrays.toString(dataR) + "/" + dataR.length + " from " + exped);
                    mMessages.append("Read " + readableMsgR + " from " + exped + "\n");
                    break;
                case MESSAGE_CONNECTION:
                    BluetoothDevice device = (BluetoothDevice)msg.obj;
                    String deviceName = device.getName();
                    Log.i(TAG, "Connected to " + deviceName);
                    mConnections.append(deviceName + "\n");
                    //mBluetoothService.addRoutingEntry(device.getAddress(), 1, device.getAddress());
                    Log.d(TAG, "My initial routing : \n" + mBluetoothService.getRoutingTableStr());
                    mBluetoothService.sendRoutingTable(device);
                    break;
                case MESSAGE_DISCONNECTION:
                    Log.i(TAG, msg.obj + " disconnected ");
                    mConnections.append("-" + msg.obj + "\n");
                    //TODO remove entry from routingtable
                    break;
                case MESSAGE_ROUTING_TABLE:
                    RoutingTable table = Utils.deserializeRoutingTable((byte[])msg.obj);
                    if(table != null) {
                        Log.i(TAG, "Received routing table " + table);
                        mBluetoothService.updateRoutingFrom(msg.getData().getString("from"), table);
                        String updatedTableStr = mBluetoothService.getRoutingTableStr();
                        Log.i(TAG, "Updated my table to : " + updatedTableStr);
                        mRoutingTable.setText(updatedTableStr);
                        mRoutingBindings.setText(mBluetoothService.getRoutingBindingsStr());
                    }
                    else{
                        String errorMsg = "Routing table received null";
                        Log.d(TAG, errorMsg);
                        mToast = Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_LONG);
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.pb_progress_indicator);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_available_devices);
        mMessages = (TextView) findViewById(R.id.tv_messages);
        mConnections = (TextView) findViewById(R.id.tv_connections);
        mRoutingTable = (TextView) findViewById(R.id.tv_routing_table);
        mRoutingBindings = (TextView) findViewById(R.id.tv_routing_bindings);

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
            case R.id.action_accept:
                mBluetoothService.accept();
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
        Log.i(TAG, "Discoverable");
    }

    public void sayHello(View view){
        byte[] str = "Hello !".getBytes();
        mBluetoothService.sendMessage(Utils.getConstructedMessage(TYPE_STRING, str));
    }

    public void clearMessages(View view){
        mMessages.setText("");
    }

    @Override
    public void onListItemClick(BluetoothDevice device) {
        if (mToast != null) {
            mToast.cancel();
        }
        String text = device.getName() + " " + device.getAddress() + " clicked";
        mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        mToast.show();

        mBluetoothService.connect(device);
    }
}
