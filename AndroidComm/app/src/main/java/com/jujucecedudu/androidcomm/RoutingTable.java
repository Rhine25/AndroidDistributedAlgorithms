package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Rhine on 22/11/2017.
 */

public class RoutingTable implements Serializable{
    private static final String TAG = "BLUETOOTH_TEST_ROUTING";

    private ArrayList<Object[]> table; //CHECKME maybe this should be a set ?
    private ArrayList<Pair<String, String>> mMACToName;
    private int mNbDevices;

    public RoutingTable() {
        table = new ArrayList<>();
        mMACToName = new ArrayList<>();
        mNbDevices = 0;
    }

    public void addEntry(String targetMAC, int cost, String nextHopMAC){ //TODO third element might be the connectedThread ?
        Object[] entry = {targetMAC, cost, nextHopMAC};
        table.add(entry);
        if(!knownHost(targetMAC)){
            addNewHost(targetMAC);
        }
    }

    public void deleteEntry(String deviceMAC){
        //TODO virer le chemin vers lui

        //TODO les chemins par lui ont un coup de -1
    }

    public void updateFrom(String neighbourMAC, RoutingTable neighbourTable){
        for (Object[] entry : neighbourTable.getTable()) {
            String targetMAC = (String)entry[0];
            if(!knownPathToHost(targetMAC)) {
                Log.d(TAG, "Don't know a path to host : " + targetMAC);
                addEntry(targetMAC, (int) entry[1] + 1, neighbourMAC);
            }
            else{
                Log.d(TAG, "Already know path to host : " + targetMAC);
            }
            //TODO get shortest path
        }
    }

    private boolean knownPathToHost(String deviceMAC){
        for (Object[] entry : table) {
            if((String)entry[0] == deviceMAC){
                return true;
            }
        }
        return false;
    }

    private boolean knownHost(String deviceMAC){
        for(Pair pair : mMACToName){
            if(pair.first == deviceMAC){
                return true;
            }
        }
        return false;
    }

    private void addNewHost(String hostMAC){
        mMACToName.add(new Pair<String, String>(hostMAC, Integer.toString(mNbDevices)));
        mNbDevices ++;
    }

    private String getHostNumber(String hostMAC){
        for(Pair pair : mMACToName){
            if(pair.first == hostMAC){
                return (String)pair.second;
            }
        }
        return null;
    }

    public String getNextHopMAC(String targetMAC){
        for(Object[] entry : table){
            if((String)entry[0] == targetMAC){
                return (String)entry[2];
            }
        }
        return null;
    }

    public ArrayList<Object[]> getTable() {
        return table;
    }

    @Override
    public String toString() {
        String str = "";
        for(Object[] entry : table){
            str += getHostNumber((String)entry[0]) + "/" + entry[1] + " -> " + getHostNumber((String)entry[2]) + "\n";
        }
        return str;
    }
}
