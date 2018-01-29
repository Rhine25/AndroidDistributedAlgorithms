package com.jujucecedudu.androidcomm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class Main2Activity extends AppCompatActivity {

    private static final String TAG = "BLUETOOTH_TEST_MAIN2";
    AlgoLamportChat algo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        

        Log.i(TAG, "activity 2 started");


        algo.init();

        Log.i(TAG, "Algo is started");

    }
}
