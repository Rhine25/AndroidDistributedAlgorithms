package com.jujucecedudu.androidcomm;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import static com.jujucecedudu.androidcomm.API.MessageTypes.*;

/**
 * Created by Rhine on 14/01/2018.
 */

public class API {

    private AlgoLamportChat algo;

    public API(AlgoLamportChat a){
        algo = a;
    }

    private static final String TAG = "BLUETOOTH_TEST_API";

    private static MyBluetoothService myBluetoothService;

    public interface MessageTypes{
        byte SEND_MESSAGE = 0x10;
        byte type2 = 0x11;
        byte type3 = 0x12;
        byte REQ = 0x13;
        byte ACK = 0x14;
        byte REL = 0x15;
    }

    public static void onMessage(byte[] message, AlgoLamportChat a){
        byte messageType = message[0];
        MessagePacket mp = Utils.getMessagePacketFromByteMessage(message);
        byte[] data = mp.getData(); //get data field of mp
        byte[] data2 = new byte[data.length-1]; //get object field from data
        for(int i=0;i<data2.length;i++){
            data2[i]=data[i+1];
        }
        switch(messageType){
            case SEND_MESSAGE:
                //MET TA PUTAIN DE FONCTION ICI
                Log.i(TAG, "Send message");
                break;
            case type2:
                Log.i(TAG, " message");
                break;
            case type3:
                Log.i(TAG, " message");
                break;
            case REQ:
                a.receiveREQ(Utils.byteToInt(data2), mp.getExpMAC());
                Log.i(TAG, "Received message example 3");
                break;
            case ACK:
                a.receiveACK(Utils.byteToInt(data2), mp.getExpMAC());
                Log.i(TAG, "Received message example 3");
                break;
            case REL:
                a.receiveREL(Utils.byteToInt(data2), mp.getExpMAC());
                Log.i(TAG, "Received message example 3");
                break;
            default:
                Log.i(TAG, "onMessage, message type is not handled : " + messageType);
        }
    }

    public void sendMessage(String dest, byte tMess, Object data){
        byte[] d = Utils.convertObjectToByteArray(data);
        byte[] data_b = new byte[d.length+1];
        data_b[0] = tMess;
        for(int i=1;i<data_b.length;i++)
            data_b[i]=d[i-1];
        MessagePacket mp = new MessagePacket(myBluetoothService.getMyMAC(),dest,data_b);
        myBluetoothService.sendMessage(mp);
    }

    public String[] getDevicesMACs(){
        return myBluetoothService.getAllDevicesMACs();
    }
    
    public int getNbEntries(){
        return myBluetoothService.getNbEntries();
    }

    public String getMyMACAddres(){
        return myBluetoothService.getMyMAC();
    }

}