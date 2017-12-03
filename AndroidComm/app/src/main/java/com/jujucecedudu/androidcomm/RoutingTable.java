package com.jujucecedudu.androidcomm;

import android.util.Log;
import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Rhine on 22/11/2017.
 */

public class RoutingTable implements Serializable{
    private static final String TAG = "BLUETOOTH_TEST_ROUTING";

    private ArrayList<Object[]> table; //CHECKME maybe this should be a set ?
    private ArrayList<Pair<String, String>> mMACToName;
    private int mNbDevices;
    private String mMAC;

    public RoutingTable() {
        table = new ArrayList<>();
        mMACToName = new ArrayList<>();
        mNbDevices = 0;
        mMAC = "";
    }

    public RoutingTable(ArrayList<Object[]> table){
        this.table = table;
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

    public void updateFrom(String neighbourMAC, ArrayList<Object[]> neighbourTable){
        if(!knownPathToHost(neighbourMAC)) {
            Log.d(TAG, "Don't know a path to connected host : " + neighbourMAC);
            addEntry(neighbourMAC, 1, neighbourMAC);
        }
        for (Object[] entry : neighbourTable) {
            String targetMAC = getTargetMAC(entry);
            if(!knownPathToHost(targetMAC)) {
                Log.d(TAG, "Don't know a path to host : " + targetMAC);
                addEntry(targetMAC, getCost(entry) + 1, neighbourMAC);
            }
            else{
                Log.d(TAG, "Already know path to host : " + targetMAC);
            }
            //TODO get shortest path
        }
    }

    private boolean knownPathToHost(String deviceMAC){
        if (itsMe(deviceMAC)){
            return true;
        }
        for (Object[] entry : table) {
            String entryMAC = getTargetMAC(entry);
            if(entryMAC.equals(deviceMAC)){
                return true;
            }
        }
        return false;
    }

    private boolean knownHost(String deviceMAC){
        if (itsMe(deviceMAC)){
            return true;
        }
        for(Pair pair : mMACToName){
            if(pair.first.equals(deviceMAC)){
                return true;
            }
        }
        return false;
    }

    private boolean itsMe(String deviceMAC){
        return deviceMAC.equals(mMAC);
    }

    private void addNewHost(String hostMAC){
        mMACToName.add(new Pair<String, String>(hostMAC, Integer.toString(mNbDevices)));
        mNbDevices ++;
    }

    private String getHostNumber(String hostMAC){
        for(Pair pair : mMACToName){
            if(pair.first.equals(hostMAC)){
                return (String)pair.second;
            }
        }
        return null;
    }

    public String getNextHopMAC(String targetMAC){
        for(Object[] entry : table){
            if(getTargetMAC(entry).equals(targetMAC)){
                return getNextHopMAC(entry);
            }
        }
        return null;
    }

    public String getMACToNameBindings(){
        String str = "";
        for(Pair<String, String> entry : mMACToName){
            str += entry.first + " is " + entry.second + "\n";
        }
        return str;
    }

    public String getMyMAC(){
        return mMAC;
    }

    public void setMyMAC(String mAC){
        mMAC = mAC;
    }

    public ArrayList<Object[]> getTable() {
        return table;
    }

    private String getTargetMAC(Object[] entry){
        return (String)entry[0];
    }

    private int getCost(Object[] entry){
        return (int)entry[1];
    }

    private String getNextHopMAC(Object[] entry){
        return (String)entry[2];
    }

    @Override
    public String toString() {
        String str = "";
        if(mMACToName == null){
            for(Object[] entry : table){
                str += getTargetMAC(entry) + "/" + getCost(entry) + " -> " + getNextHopMAC(entry) + "\n";
            }
        }
        else {
            for (Object[] entry : table) {
                str += getHostNumber(getTargetMAC(entry)) + "/" + getCost(entry) + " -> " + getHostNumber(getNextHopMAC(entry)) + "\n";
            }
        }
        return str;
    }
}
