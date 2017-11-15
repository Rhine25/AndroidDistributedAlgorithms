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
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_DEVICE_NAME;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_READ;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_STATE_CHANGE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_TOAST;
import static com.jujucecedudu.androidcomm.MyBluetoothService.MessageConstants.MESSAGE_WRITE;
import static com.jujucecedudu.androidcomm.MyBluetoothService.STATE_CONNECTED;
import static com.jujucecedudu.androidcomm.MyBluetoothService.STATE_CONNECTING;
import static com.jujucecedudu.androidcomm.MyBluetoothService.STATE_LISTEN;
import static com.jujucecedudu.androidcomm.MyBluetoothService.STATE_NONE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLUETOOTH_TEST_MAIN";

    public static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    private static final int REQUEST_DISCOVERABLE = 22;
    private static final int LOCATION = 70;

    private ProgressDialog mProgressDialog;
    private TextView mDiscoveredDevicesText;

    private BluetoothAdapter mBluetoothAdapter;

    private MyBluetoothService myBluetoothService;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDiscoveredDevicesText = (TextView) findViewById(R.id.tv_available_devices);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Scanning...");
        mProgressDialog.setCancelable(false);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        myBluetoothService = new MyBluetoothService(this, mHandler);

        askForPermission(Manifest.permission.ACCESS_COARSE_LOCATION, LOCATION);

        if(mBluetoothAdapter == null){
            //device does not support bluetooth
            //TODO display text error message
            Log.d(TAG, "Bluetooth adapter is null");
        }

        if(!mBluetoothAdapter.isEnabled()){
            enableBluetooth();
        }
        registerForDiscoveryInfo();

    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
        } else {
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    mProgressDialog.dismiss();
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    myBluetoothService.setBluetoothFriend(device);
                    String deviceName = device.getName();
                    String deviceMAC = device.getAddress();
                    Log.i(TAG, "Discovered device : " + deviceName + " " + deviceMAC);
                    mDiscoveredDevicesText.append(deviceName + " " + deviceMAC + "\n");
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
    protected void onDestroy(){
        super.onDestroy();

        unregisterReceiver(myReceiver);

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
                myBluetoothService.discover();
                return true;
            case R.id.action_discoverable:
                makeDiscoverable();
                return true;
            case R.id.action_connect:
                myBluetoothService.connect();
                return true;
            case R.id.action_accept:
                myBluetoothService.accept();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void enableBluetooth(){
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        //TODO check what the result is
    }

    void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        Log.i(TAG, "Discoverable");
    }

    public void toggleBluetooth(View view){
        BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter.isEnabled()){
            myBluetoothAdapter.disable();
        }
        else{
            myBluetoothAdapter.enable();
        }
        Log.i(TAG, "toggle BT");
    }

    public void sayHello(View view){
        myBluetoothService.sendMessage("Hello !".getBytes());
    }

    private void registerForDiscoveryInfo(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);
    }
}
