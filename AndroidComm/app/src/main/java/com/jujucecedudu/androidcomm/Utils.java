package com.jujucecedudu.androidcomm;

import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

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
            Log.e(TAG, "Couldn't construct hello msg", e);
            e.printStackTrace();
        }
        return null;
    }

    static byte[] extractDataFromMessage(byte[] message){
        byte[] byteTable = new byte[message.length - 1];
        System.arraycopy(message, 1, byteTable, 0, message.length - 1);
        return byteTable;
    }

    static byte[] extractDataFromMessage(byte[] message, int length){
        byte[] byteTable = new byte[length - 1];
        System.arraycopy(message, 1, byteTable, 0, length - 1);
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

}
