package com.jujucecedudu.androidcomm;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.Toast;

import java.util.UUID;

import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_READ;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_WRITE;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.ListItemClickListener{
    private static final String TAG = "BLUETOOTH_TEST_MAIN";

    public static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    private static final int REQUEST_DISCOVERABLE = 22;
    private static final int LOCATION = 70;

    private ProgressDialog mProgressDialog;
    //private TextView mDiscoveredDevicesText;
    private RecyclerView mRecyclerView;
    private DeviceAdapter mDeviceAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private MyBluetoothService mBluetoothService;

    private Toast mToast;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.i(TAG, "Sent : " + writeMessage + " to my friend");
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i(TAG, "Read " + readMessage + " from my friend");
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    mProgressDialog.dismiss();
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceMAC = device.getAddress();
                    Log.i(TAG, "Discovered device : " + deviceName + " " + deviceMAC);
                    //mDiscoveredDevicesText.append(deviceName + " " + deviceMAC + "\n");
                    mDeviceAdapter.addDeviceData(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.i(TAG, "onReceive, discovery started");
                    mProgressDialog.show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i(TAG, "onReceive, discovery finished");
                    mProgressDialog.dismiss();
                    break;
                default:
                    Log.i(TAG, "onReceive, action is not handled : " + action);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mDiscoveredDevicesText = (TextView) findViewById(R.id.tv_available_devices);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_available_devices);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mDeviceAdapter = new DeviceAdapter(this);
        mRecyclerView.setAdapter(mDeviceAdapter);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Scanning for devices...");
        mProgressDialog.setCancelable(false);

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
        //TODO check what the result is
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
        mBluetoothService.sendMessage("Hello !".getBytes());
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
