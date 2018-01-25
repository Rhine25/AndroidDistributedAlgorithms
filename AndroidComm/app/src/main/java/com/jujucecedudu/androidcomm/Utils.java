package com.jujucecedudu.androidcomm;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Rhine on 30/11/2017.
 */

public class Utils {

    private static final String TAG = "BLUETOOTH_TEST_UTILS";

    static byte[] getConstructedMessage(byte type, byte[] data){

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(type);
            outputStream.write(data);
            byte msg[] = outputStream.toByteArray( );
            return msg;
        } catch (IOException e) {
            Log.e(TAG, "Couldn't construct msg", e);
            e.printStackTrace();
        }
        return null;
    }

    static byte[] convertObjectToByteArray(Object data){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(data);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Couldn't convert object to byte array " + e);
        }
        return null;
    }

    static MessagePacket getMessagePacketFromByteMessage(byte[] message){
        byte[] m = new byte[message.length];
        for(int i=1;i<message.length;i++)
            m[i-1]=message[i];
        return Utils.deserializeMessage(m);
    }
    
    private byte[] getMACBytes(String deviceMAC){
        Log.i(TAG, "String MAC address : " + deviceMAC);
        Log.i(TAG, "Bytes MAC address : " + deviceMAC.getBytes());
        return deviceMAC.getBytes();
    }

    static byte[] extractDataFromMessage(byte[] message){
        byte[] byteTable = new byte[message.length - 1];
        System.arraycopy(message, 1, byteTable, 0, message.length - 1);
        return byteTable;
    }

    static byte[] serializeRoutingTable(RoutingTable table){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(table.getTable());
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Routing table couldn't be serialized "+ e);
        }
        return null;
    }

    static byte[] serializeMessage(MessagePacket message){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(message);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Routing table couldn't be serialized "+ e);
        }
        return null;
    }

    static RoutingTable deserializeRoutingTable(byte[] bytes){
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(in);
            try {
                ArrayList<Object[]> table = (ArrayList<Object[]>) ois.readObject();
                return new RoutingTable(table);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static MessagePacket deserializeMessage(byte[] bytes){
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(in);
            try {
                MessagePacket message = (MessagePacket) ois.readObject();
                return message;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
