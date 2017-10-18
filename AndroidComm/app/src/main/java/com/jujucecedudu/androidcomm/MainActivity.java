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

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;
    //private static final int REQUEST_DISCOVERABLE = 22;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null){
            //device does not support bluetooth
        }

        //enable bluetooth
        if(!myBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        //make the device discoverable
        /*Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        Log.i("BLUETOOTH_TEST", "Discoverable");*/

        //get paired devices
        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                Log.i("BLUETOOTH_TEST", "Paired device : " + deviceName + " " + deviceMAC);
            }
        }
        else{
            Log.i("BLUETOOTH_TEST", "No paired device");
        }

        //Get info about discovered devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Scanning...");
        progressDialog.setCancelable(false);

        //discover devices
        if(myBluetoothAdapter.startDiscovery()) {
            Log.i("BLUETOOTH_TEST", "Launched discovery");
            //it is asynchronous so the discovery is not instantaneous
        }
        else{
            Log.i("BLUETOOTH_TEST", "Discovery could not launch");
        }

        /*switch (myBluetoothAdapter.getState()){
            case BluetoothAdapter.STATE_OFF:
                Log.i("BLUETOOTH_TEST", "Current state : Off");
                break;
            case BluetoothAdapter.STATE_ON:
                Log.i("BLUETOOTH_TEST", "Current state : On");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.i("BLUETOOTH_TEST", "Current state : Turning Off");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.i("BLUETOOTH_TEST", "Current state : Turning On");
                break;
        }
        switch (myBluetoothAdapter.getScanMode()){
            case BluetoothAdapter.SCAN_MODE_NONE:
                Log.i("BLUETOOTH_TEST", "Current scan mode : None");
                break;
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                Log.i("BLUETOOTH_TEST", "Current scan mode : Connectable");
                break;
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                Log.i("BLUETOOTH_TEST", "Current scan mode : Connectable Discoverable");
                break;
        }

        Log.i("BLUETOOTH_TEST", "Currently enabled ? " + myBluetoothAdapter.isEnabled());
        Log.i("BLUETOOTH_TEST", "Currently discovering ? " + myBluetoothAdapter.isDiscovering());*/

        //here should pair to a device


        //then stop discovery as it is resource consuming
        //myBluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                Log.i("BLUETOOTH_TEST", "Discovered device : " + deviceName + " " + deviceMAC);
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.i("BLUETOOTH_TEST", "onReceive, discovery started");
                progressDialog.show();
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.i("BLUETOOTH_TEST", "onReceive, discovery finished");
                progressDialog.dismiss();
            }
            else{
                Log.i("BLUETOOTH_TEST", "onReceive, action is not handled : " + action);
            }
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();

        unregisterReceiver(myReceiver);

        Log.i("BLUETOOTH_TEST", "Unregistered receiver");
    }
}
