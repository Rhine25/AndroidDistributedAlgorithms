package com.jujucecedudu.androidcomm;

import java.io.Serializable;

/**
 * Created by Rhine on 31/12/2017.
 */

public class MessagePacket implements Serializable{
    String expMAC;
    String destMAC;
    byte[] data;

    public MessagePacket(String expMAC, String destMAC, byte[] data) {
        this.expMAC = expMAC;
        this.destMAC = destMAC;
        this.data = data;
    }
}
