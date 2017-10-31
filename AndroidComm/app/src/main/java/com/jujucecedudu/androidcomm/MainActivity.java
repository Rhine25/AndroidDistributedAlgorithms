package com.jujucecedudu.androidcomm;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLUETOOTH_TEST_MAIN";

    public static final UUID MY_UUID = UUID.fromString("7255562d-a5db-43d8-a38d-874453bc589b");

    private static final int APP_DISCOVERY = 0;
    private static final int APP_DISCOVERABLE = 1;
    private static final int APP_CONNECT = 2;
    private static final int APP_ACCEPT = 3;

    private static final String[] modes = {"discovery", "discoverable", "connect", "accept"};

    private static final int MODE = APP_CONNECT;

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    private static final int REQUEST_DISCOVERABLE = 22;

    private ProgressDialog progressDialog;

    private TextView versions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        versions = (TextView) findViewById(R.id.text);
        versions.setText("Current mode is " + modes[MODE]);

        BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null){
            //device does not support bluetooth
        }

        //enable bluetooth
        if(!myBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        //get paired devices
        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                Log.i(TAG, "Paired device : " + deviceName + " " + deviceMAC);
            }
        }
        else{
            Log.i(TAG, "No paired device");
        }

        //make the device discoverable
        if(MODE == APP_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
            Log.i(TAG, "Discoverable");
        }

        //Register to get info about system discovering devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Scanning...");
        progressDialog.setCancelable(false);

        //discover devices
        if(MODE == APP_DISCOVERY) {
            if (myBluetoothAdapter.startDiscovery()) {
                Log.i(TAG, "Launched discovery");
                //it is asynchronous so the discovery is not instantaneous
            } else {
                Log.i(TAG, "Discovery could not launch");
            }

            //myBluetoothAdapter.cancelDiscovery();
            //here should pair to a device after stopping discovery process
        }

        if((MODE == APP_CONNECT || MODE == APP_ACCEPT) && pairedDevices.size() > 0){
            BluetoothDevice target = pairedDevices.iterator().next(); //Ugh, gross
            Log.i(TAG, "Target is : " + target.getName());
            if(MODE == APP_CONNECT){
                Log.i(TAG, "I'm gonna connect");
                ConnectThread connectThread = new ConnectThread(target, myBluetoothAdapter);
                //connectThread.start();
            }
            else if(MODE == APP_ACCEPT) {
                Log.i(TAG, "I'm gonna accept");
                AcceptThread acceptThread = new AcceptThread(myBluetoothAdapter);
                acceptThread.start();
            }
        }
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                Log.i(TAG, "Discovered device : " + deviceName + " " + deviceMAC);
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.i(TAG, "onReceive, discovery started");
                progressDialog.show();
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.i(TAG, "onReceive, discovery finished");
                progressDialog.dismiss();
            }
            else{
                Log.i(TAG, "onReceive, action is not handled : " + action);
            }
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();

        unregisterReceiver(myReceiver);

        Log.i(TAG, "Unregistered receiver");
    }
}
