package com.jujucecedudu.androidcomm;

import android.util.Log;

import static com.jujucecedudu.androidcomm.API.MessageTypes.*;

/**
 * Created by Rhine on 14/01/2018.
 */

public class API {

    private static final String TAG = "BLUETOOTH_TEST_API";

    private static AlgoLamportChat mAlgo;
    private static MyBluetoothService myBluetoothService;

    public API(AlgoLamportChat a, MyBluetoothService bluetoothService){
        mAlgo = a;
        myBluetoothService = bluetoothService;
    }

    public interface MessageTypes{
        byte SEND_MESSAGE = 0x10;
        byte type2 = 0x11;
        byte type3 = 0x12;
        byte REQ = 0x13;
        byte ACK = 0x14;
        byte REL = 0x15;
    }

    public static void onMessage(byte[] message){
        byte messageType = message[0];
        MessagePacket mp = Utils.getMessagePacketFromByteMessage(message);
        byte[] data; //get data field of mp
        switch(messageType){
            case REQ:
                data = Utils.getObjectDataFromMessage(mp.getData());
                mAlgo.receiveREQ(Utils.byteToInt(data), mp.getExpMAC());
                Log.i(TAG, "Received message REQ from"+mp.getExpMAC()+"at clock "+Utils.byteToInt(data));
                break;
            case ACK:
                data = Utils.getObjectDataFromMessage(mp.getData());
                mAlgo.receiveACK(Utils.byteToInt(data), mp.getExpMAC());
                Log.i(TAG, "Received message ACK from"+mp.getExpMAC()+"at clock "+Utils.byteToInt(data));
                break;
            case REL:
                data = Utils.getObjectDataFromMessage(mp.getData());
                mAlgo.receiveREL(Utils.byteToInt(data), mp.getExpMAC());
                Log.i(TAG, "Received message REL from"+mp.getExpMAC()+"at clock "+Utils.byteToInt(data));
                break;
            default:
                Log.i(TAG, "onMessage, message type is not handled : " + messageType);
        }
    }

    public static void sendMessage(String dest, byte tMess, Object data){
        byte[] d = Utils.convertObjectToByteArray(data);
        byte[] data_b = new byte[d.length+1];
        data_b[0] = tMess;
        for(int i = 1;i < data_b.length;i++) {
            data_b[i] = d[i-1];
        }
        MessagePacket mp = new MessagePacket(myBluetoothService.getMyMAC(),dest,data_b);
        myBluetoothService.sendMessage(mp);
    }

    public static String[] getDevicesMACs(){
        return myBluetoothService.getAllDevicesMACs();
    }
    
    public static int getNbDevicesConnected(){
        return myBluetoothService.getNbDevicesConnected();
    }

    public static String getMyMACAddres(){
        return myBluetoothService.getMyMAC();
    }

}