package com.jujucecedudu.androidcomm;


/**
 * Created by vincent on 24/01/18.
 */

public class AlgoLamportChat{

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

    public AlgoLamportChat(){
        super();
    }

    API api = new API(this);


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

        clock = 0;

        nbNeigbours = api.getNbEntries();

        macTable = api.getDevicesMACs();
        macTable = new String[nbNeigbours];

        amITheLowerMac = amITheLowerMAC(api.getMyMACAddres(), macTable);



        if(amITheLowerMac){
            inCriticalSection = true;
        }else{
            inCriticalSection = false;
        }


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
                inCriticalSection = ( (F_H[procId] < F_H[j]) || ((F_H[procId] == F_H[j])&& procId < j));
            }
        }
    }
    
    public void receiveREQ(int rClock, String emitt){
        //clock increment
        clock = Math.max(clock, rClock) + 1;
        //get the emitter ID from the MAC/ID table
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);

        //update request clock array
        F_H[idEmitt] = rClock;
        //update message received array
        F_M[idEmitt] = API.MessageTypes.REQ;

        //send message to the emitter
        api.sendMessage(emitt, API.MessageTypes.ACK, clock);

    }

    public void receiveACK(int rClock, String emitt){
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);

        clock = Math.max(clock, rClock) + 1;

        if(F_M[idEmitt] != API.MessageTypes.REQ){
            F_H[idEmitt] = rClock;
            F_M[idEmitt] = API.MessageTypes.ACK;
        }
    }

    public void freeSC(){
        clock++;
        inCriticalSection = false;
        for(int i=0 ; i<nbNeigbours ; i++){
            api.sendMessage(macTable[i], API.MessageTypes.REL, clock);
        }
        F_H[procId] = clock;
        F_M[procId] = API.MessageTypes.REL;
    }

    public void receiveREL(int rClock, String emitt){
        int idEmitt = getIdByMAC(emitt, macTable, nbNeigbours);
        clock = Math.max(clock, rClock) + 1;
        F_H[idEmitt] = rClock;
        F_M[idEmitt] = API.MessageTypes.REL;
    }


}
