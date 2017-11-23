package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothDevice;

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
    private ArrayList<Object[]> table; //CHECKME maybe this should be a set ?

    public RoutingTable() {
        table = new ArrayList<>();
        //TODO faire une entrée vers soi avec un coup de 0
    }

    public void addEntry(String targetMAC, int cost, String nextHopMAC){ //TODO third element might be the connectedThread ?
        Object[] entry = {targetMAC, cost, nextHopMAC};
        table.add(entry);
    }

    public void deleteEntry(String deviceMAC){
        //TODO virer le chemin vers lui

        //TODO les chemins par lui ont un coup de -1
    }

    public void updateFrom(String neighbourMAC, RoutingTable neighbourTable){
        for (Object[] entry : neighbourTable.getTable()) {
            addEntry((String)entry[0], (int)entry[1]+1, neighbourMAC);
        }
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
            str += entry[0] + "/" + entry[1] + " -> " + entry[2] + "\n";
        }
        return str;
    }
}
