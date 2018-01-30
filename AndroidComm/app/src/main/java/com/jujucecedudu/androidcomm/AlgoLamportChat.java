package com.jujucecedudu.androidcomm;

import android.util.Log;

/**
 * Created by vincent on 24/01/18.
 */

public class AlgoLamportChat{
    private static final String TAG = "BLUETOOTH_TEST_ALGO";

    public int getIdByMAC(String mac, String[] macTable, int size){
        for(int i = 0 ; i < size ; i++){
            if(mac.compareTo(macTable[i])==0){
                return i;
            }
        }
        return -1;
    }

    public boolean amITheLowerMAC(String myMac, String[] neighbours){
        for(int i = 0 ; i < neighbours.length ; i++){
            if(myMac.compareTo(neighbours[i]) > 0){
                return false;
            }
        }
        return true;
    }

    //API api;

    public AlgoLamportChat(){
        super();
    }



    public String[] macTable;
    public int nbNeigbours;
    public int procId;

    public boolean inCriticalSection;

    public boolean amITheLowerMac;
    public boolean gotCriticalSection;

    public int clock;

    //tableau d'horloges et l'indice correspond à un participant
    //il me faut la liste des participants et leur mac adresse

    public int[] F_H;

    //tableau des états des participants
    public byte[] F_M;

    public String[] assocMacId;



    public void init(){
        Log.d(TAG, "I AM INIT");
        clock = 0;

        nbNeigbours = API.getNbDevicesConnected();

        macTable = API.getDevicesMACs();
        macTable = new String[nbNeigbours];

        amITheLowerMac = amITheLowerMAC(API.getMyMACAddres(), macTable);



        if(amITheLowerMac){
            inCriticalSection = true;
        }else{
            inCriticalSection = false;
        }


        F_H = new int[nbNeigbours+1];
        F_M = new byte[nbNeigbours+1];

    }

    public void askForSC(){
        Log.d(TAG, "I ASK FOR SC");
        clock += 1;
        for(int i=0 ; i<nbNeigbours ; i++){
            API.sendMessage(macTable[i], API.MessageTypes.REQ, clock);
            Log.d(TAG, "I SEND REQ( "+ clock + " ) to "+macTable[i]);
        }
        F_H[procId]=clock;
        F_M[procId]= API.MessageTypes.REQ;

        int neigbourId = 0;

        while(!inCriticalSection){
            for(int j=0 ; j<nbNeigbours ; j++){
                inCriticalSection = ( (F_H[procId] < F_H[j]) || ((F_H[procId] == F_H[j])&& procId < j));
            }
        }
    }
    
    public void receiveREQ(int rClock, String emitt){
        Log.d(TAG, "I RECEIVE REQ( "+ rClock + " ) from "+emitt);
        //clock increment
        clock = Math.max(clock, rClock) + 1;
        //get the emitter ID from the MAC/ID table
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);

        //update request clock array
        F_H[idEmitt] = rClock;
        //update message received array
        F_M[idEmitt] = API.MessageTypes.REQ;

        //send message to the emitter
        API.sendMessage(emitt, API.MessageTypes.ACK, clock);
        Log.d(TAG, "I SEND ACK( "+ clock + " ) to "+emitt);
    }

    public void receiveACK(int rClock, String emitt){
        Log.d(TAG, "I RECEIVE ACK( "+ rClock + " ) from "+emitt);
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);

        clock = Math.max(clock, rClock) + 1;

        if(F_M[idEmitt] != API.MessageTypes.REQ){
            F_H[idEmitt] = rClock;
            F_M[idEmitt] = API.MessageTypes.ACK;
        }
    }

    public void freeSC(){
        Log.d(TAG, "I FREE SC");
        clock++;
        inCriticalSection = false;
        for(int i=0 ; i<nbNeigbours ; i++){
            API.sendMessage(macTable[i], API.MessageTypes.REL, clock);
            Log.d(TAG, "I SEND REL( "+ clock + " ) to "+macTable[i]);
        }
        F_H[procId] = clock;
        F_M[procId] = API.MessageTypes.REL;
    }

    public void receiveREL(int rClock, String emitt){
        Log.d(TAG, "I RECEIVE REL( "+ rClock + " ) from "+emitt);
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);
        clock = Math.max(clock, rClock) + 1;
        F_H[idEmitt] = rClock;
        F_M[idEmitt] = API.MessageTypes.REL;
    }


}
