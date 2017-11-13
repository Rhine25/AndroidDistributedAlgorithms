package com.jujucecedudu.androidcomm;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLUETOOTH_TEST_MAIN";

    public static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    private static final int REQUEST_DISCOVERABLE = 22;
    private static final int LOCATION = 70;

    private ProgressDialog mProgressDialog;
    private TextView mDiscoveredDevicesText;
    private TextView mPairedDevicesText;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice mBluetoothFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDiscoveredDevicesText = (TextView) findViewById(R.id.tv_available_devices);
        mPairedDevicesText = (TextView) findViewById(R.id.tv_paired_devices);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Scanning...");
        mProgressDialog.setCancelable(false);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        askForPermission(Manifest.permission.ACCESS_COARSE_LOCATION, LOCATION);

        if(mBluetoothAdapter == null){
            //device does not support bluetooth
            //TODO display text error message
            Log.d(TAG, "Bluetooth adapter is null");
        }

        if(!mBluetoothAdapter.isEnabled()){
            enableBluetooth();
        }

        listPairedDevices();
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
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBluetoothFriend = device;
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
                discover();
                return true;
            case R.id.action_discoverable:
                makeDiscoverable();
                return true;
            case R.id.action_connect:
                connect();
                return true;
            case R.id.action_accept:
                accept();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    public void clearPairedDevices(View view){
        BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices){
            Method m = null;
            try {
                m = device.getClass().getMethod("removeBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableBluetooth(){
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        //TODO check what the result is
    }

    private void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        Log.i(TAG, "Discoverable");
    }

    private void discover(){
        if (mBluetoothAdapter.startDiscovery()) {
            Log.i(TAG, "Launched discovery");
            //it is asynchronous so the discovery is not instantaneous
        } else {
            Log.i(TAG, "Discovery could not launch");
        }

        //myBluetoothAdapter.cancelDiscovery();
        //here should pair to a device after stopping discovery process
    }

    private void pair(){

    }

    private void connect(){
        Set<BluetoothDevice> pairedDevices = getPairedDevices();
        if(pairedDevices.size() > 0) {
            Log.i(TAG, "I'm gonna connect");
            BluetoothDevice target = getPairedDevices().iterator().next(); //Ugh, gross
            Log.i(TAG, "Target is : " + target.getName());
            ConnectThread connectThread = new ConnectThread(target, mBluetoothAdapter);
            connectThread.start();
        }
        else if(mBluetoothFriend != null) {
            Log.i(TAG, "Don't know any friends yet");
            Log.i(TAG, "I'm gonna try to pair and connect");
            Log.i(TAG, "Target is : " + mBluetoothFriend.getName());
            ConnectThread connectThread = new ConnectThread(mBluetoothFriend, mBluetoothAdapter);
            connectThread.start();
        }
        else{
            Log.i(TAG, "Can't connect, don't see anyone yet");
        }
    }

    private void accept(){
        Log.i(TAG, "I'm gonna accept");
        AcceptThread acceptThread = new AcceptThread(mBluetoothAdapter);
        acceptThread.start();
    }

    private void sendMessage(){

    }

    private Set<BluetoothDevice> getPairedDevices(){
        return mBluetoothAdapter.getBondedDevices();
    }

    private void listPairedDevices(){
        Set<BluetoothDevice> pairedDevices = getPairedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                int bondState = device.getBondState();
                mPairedDevicesText.append(deviceName + " " + deviceMAC);
                switch(bondState){
                    case BOND_NONE:
                        mPairedDevicesText.append(" Not bonded");
                        break;
                    case BOND_BONDED:
                        mPairedDevicesText.append(" Bonded");
                        break;
                    case BOND_BONDING:
                        mPairedDevicesText.append(" Bonding");
                }
                mPairedDevicesText.append("\n");
                Log.i(TAG, "Paired device : " + deviceName + " " + deviceMAC);
            }
        }
        else{
            //mPairedDevicesText.setText("No paired device");
            Log.i(TAG, "No paired device");
        }
    }

    private void registerForDiscoveryInfo(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);
    }
}
