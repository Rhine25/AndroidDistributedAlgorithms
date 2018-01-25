package com.jujucecedudu.androidcomm;

import android.util.Log;

import static com.jujucecedudu.androidcomm.API.MessageTypes.*;

/**
 * Created by Rhine on 14/01/2018.
 */

public class API {

    /*AlgoLamportChat algo;

    public API(AlgoLamportChat a){
        algo = a;
    }*/

    private static final String TAG = "BLUETOOTH_TEST_API";

    private static MyBluetoothService myBluetoothService;

    public interface MessageTypes{
        byte TYPE_EXAMPLE_1 = 0x10;
        byte TYPE_EXAMPLE_2 = 0x11;
        byte TYPE_EXAMPLE_3 = 0x12;
        byte REQ = 0x13;
        byte ACK = 0x14;
        byte REL = 0x15;
    }

    public static void initApi(MyBluetoothService bluetoothService){
        myBluetoothService = bluetoothService;
    }

    public static void onMessage(byte[] message){
        byte messageType = message[0];
        switch(messageType){
            case TYPE_EXAMPLE_1:
                Log.i(TAG, "Received message example 1");
                break;
            case TYPE_EXAMPLE_2:
                Log.i(TAG, "Received message example 2");
                break;
            case TYPE_EXAMPLE_3:
                Log.i(TAG, "Received message example 3");
                break;
            case REQ:
                //receiveREQ(emmeteur, horloge);
                Log.i(TAG, "Received message example 3");
                break;
            case ACK:
                //receiveACK(emmeteur, horloge);
                Log.i(TAG, "Received message example 3");
                break;
            case REL:
                //receiveREL(emmeteur, horloge);
                Log.i(TAG, "Received message example 3");
                break;
            default:
                //TODO message d'erreur
                Log.e(TAG, "Unknown message type : " + messageType);
        }
    }

    public void sendMessage(String dest, String tMess, Object data){
        byte[] data_b = Utils.convertObjectToByteArray(data);
        MessagePacket mp = new MessagePacket(myBluetoothService.getMyMAC(),dest,data_b);
        myBluetoothService.sendMessage(mp);
    }
    
    public String[] getDevicesMACs(){
        return myBluetoothService.getAllDevicesMACs();
    }
    
     public int getNbEntries(){
        return myBluetoothService.getNbEntries();
    }
}