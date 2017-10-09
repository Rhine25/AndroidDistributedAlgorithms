package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 12;

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

        //get paired devices
        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                Log.i("BLUETOOTH_TEST", "Paired device : " + deviceName + " " + deviceMAC);
            }
        }

        //Get info about discovered devices
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, filter);

        //discover devices
        myBluetoothAdapter.startDiscovery();

        //here should pair to a device

        //then stop discovery as it is resource consuming
        myBluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
            }
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();

        unregisterReceiver(myReceiver);
    }
}
