package com.jujucecedudu.androidcomm;

import java.io.Serializable;

/**
 * Created by Rhine on 31/12/2017.
 */

public class MessagePacket implements Serializable{
    private String expMAC;
    private String destMAC;
    private byte[] data;

    public MessagePacket(String expMAC, String destMAC, byte[] data) {
        this.expMAC = expMAC;
        this.destMAC = destMAC;
        this.data = data;
    }

    public String getDest() {
        return destMAC;
    }

    public void setDest(String destMAC) {
        this.destMAC = destMAC;
    }

    public byte[] getData() {
        return data;
    }

    public String getExpMAC() {
        return expMAC;
    }
}
