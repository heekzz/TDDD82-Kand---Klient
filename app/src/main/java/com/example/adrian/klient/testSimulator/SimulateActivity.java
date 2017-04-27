package com.example.adrian.klient.testSimulator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.adrian.klient.R;

public class SimulateActivity extends AppCompatActivity {

    final Simulator s = new Simulator(SimulateActivity.this);
    private int count;
    private long init = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulate);

        Button addSmall = (Button) findViewById(R.id.simulateSmall);
        Button addMedium = (Button) findViewById(R.id.simulateMedium);
        Button addLarge = (Button) findViewById(R.id.simulateLarge);
//        Button delete = (Button) findViewById(R.id.deleteAll);
//        Button fileSmall = (Button) findViewById(R.id.smallFile);
//        Button fileMedium = (Button) findViewById(R.id.mediumFile);
//        Button fileLarge = (Button) findViewById(R.id.largeFile);
        Button simulate_1 = (Button) findViewById(R.id.simulate_1);
        Button simulate_2 = (Button) findViewById(R.id.simulate_2);
        Button simulate_3 = (Button) findViewById(R.id.simulate_3);
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

//                        if(ActivityCompat.shouldShowRequestPermissionRationale(,Manifest.permission.ACCESS_COARSE_LOCATION)){
//                            Log.wtf("PERMISSION","Show explanation");
//                        } else {

            Log.wtf("PERMISSION", "REQUESTING PERMISSION");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1337);

//                        }


        }

        addSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.runSmall();
            }
        });
        addMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.runMedium();
            }
        });
        addLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.runLarge();
            }
        });

        simulate_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Simulate_1();
            }
        });
        simulate_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Simulate_2();
            }
        });

        simulate_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Simulate_3();
            }
        });

    }

    // Simulate activity running for 4 minutes and sending with a 5 second interval
    public void Simulate_1(){
        Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            int i =0;
            @Override
            public void run() {
                i++;
//                Log.wtf("HANDLER_RUN","request no: " + i);
                s.runSmall();
            }
        };

        int time = 0;
        int delay = 8000; // 5 seconds interval
        handler.postDelayed(runnable, time); // 1
        while(time/1000 < 60) {
            handler.postDelayed(runnable, time+=delay); // 2
        }
    }

    // Simulate activity running for 1 minutes and sending with a 10 second interval
    public void Simulate_2(){
        Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            int i =0;
            @Override
            public void run() {
                i++;
                Log.wtf("HANDLER_RUN","request no: " + i);
                s.runSmall();
            }
        };

        int time = 0;
        int delay = 8000; // 8 seconds interval
        handler.postDelayed(runnable, time); // 1
        while(time/1000 < 60) {
            handler.postDelayed(runnable, time+=delay); // 2
        }
        System.out.println("TOTAL TIME: " + time/1000);
    }

    public void Simulate_3(){
        s.getContacts();
    }




}
