package com.jujucecedudu.androidcomm;

import android.util.Log;

import static com.jujucecedudu.androidcomm.API.MessageTypes.*;

/**
 * Created by Rhine on 14/01/2018.
 */

public class API {
    private static final String TAG = "BLUETOOTH_TEST_API";

    public interface MessageTypes{
        byte TYPE_EXAMPLE_1 = 0x10;
        byte TYPE_EXAMPLE_2 = 0x11;
        byte TYPE_EXAMPLE_3 = 0x12;
    }

    public static void onMessage(byte[] message){
        byte messageType = message[0];
        switch(messageType){
            case TYPE_EXAMPLE_1:
                Log.i(TAG, "Received message example 1");
                break;
            case TYPE_EXAMPLE_2:
                Log.i(TAG, "Received message example 2");
                break;
            case TYPE_EXAMPLE_3:
                Log.i(TAG, "Received message example 3");
                break;
            default:
                //TODO message d'erreur
        }
    }
}