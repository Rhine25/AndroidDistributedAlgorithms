package com.jujucecedudu.androidcomm;

import java.util.ArrayList;

/**
 * Created by vincent on 24/01/18.
 */

public class AlgoLamportChat {

    public int getIdByMAC(String mac, String[] macTable, int size){
        for(int i = 0 ; i < size ; i++){
            if(mac.compareTo(macTable[i])==0){
                return i;
            }
        }
        return -1;
    }

    API api = new API(this);

    public int numProc;
    public int nbVoisins;
    public String[] macTable;
    public int nbNeigbours;
    public int procId;

    public boolean inCriticalSection = false;


    public int clock;

    //tableau d'horloges et l'indice correspond à un participant
    //il me faut la liste des participants et leur mac adresse

    public int[] F_H;

    //tableau des états des participants
    public byte[] F_M;

    public String[] assocMacId;



    public void init(){

        clock = 0;

        nbNeigbours = api.getNbEntries();

        macTable = api.getDevicesMACs();
        macTable = new String[nbNeigbours];

        //TODO à trouver
        // procId = ;






        F_H = new int[nbNeigbours+1];
        F_M = new byte[nbNeigbours+1];

    }

    public void askForSC(){

        clock += 1;
        for(int i=0 ; i<nbNeigbours ; i++){
            api.sendMessage(macTable[i], API.MessageTypes.REQ, clock);
        }
        F_H[procId]=clock;
        F_M[procId]= API.MessageTypes.REQ;

        int neigbourId = 0;

        while(!inCriticalSection){
            for(int j=0 ; j<nbNeigbours ; j++){
                inCriticalSection &= ( (F_H[procId] < F_H[j]) || ((F_H[procId] == F_H[j])&& procId < j));
            }
        }

        //Log.e(TAG, "Couldn't convert object to byte array " + e);

    }

    public void receiveREQ(int rClock, String emmet){
        clock = Math.max(clock, rClock) + 1;

        int idEmmet = getIdByMAC(emmet, macTable, nbNeigbours);
        F_H[idEmmet] = rClock;
        F_M[idEmmet] = API.MessageTypes.REQ;

        api.sendMessage(emmet, API.MessageTypes.ACK, clock);

        // envoyer ACK à emmet
    }

    public void receiveACK(int rClock, String emmet){
        int idEmmet = getIdByMAC(emmet, macTable, nbNeigbours);

        clock = Math.max(clock, rClock) + 1;

        if(F_M[idEmmet] != API.MessageTypes.REQ){
            F_H[idEmmet] = rClock;
            F_M[idEmmet] = API.MessageTypes.ACK;
        }
    }

    public void freeSC(){
        clock++;
        for(int i=0 ; i<nbNeigbours ; i++){
            api.sendMessage(macTable[i], API.MessageTypes.REL, clock);
        }
        F_H[procId] = clock;
        F_M[procId] = API.MessageTypes.REL;
    }

    public void receiveREL(int rClock, String emmet){
        int idEmmet = getIdByMAC(emmet, macTable, nbNeigbours);
        clock = Math.max(clock, rClock) + 1;
        F_H[idEmmet] = rClock;
        F_M[idEmmet] = API.MessageTypes.REL;
    }


}
