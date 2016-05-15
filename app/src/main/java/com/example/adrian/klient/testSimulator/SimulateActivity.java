package com.example.adrian.klient.testSimulator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adrian.klient.R;

public class SimulateActivity extends AppCompatActivity {

    final Simulator s = new Simulator(SimulateActivity.this);
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulate);

        Button addSmall = (Button) findViewById(R.id.simulateSmall);
        Button addMedium = (Button) findViewById(R.id.simulateMedium);
        Button addLarge = (Button) findViewById(R.id.simulateLarge);
        Button delete = (Button) findViewById(R.id.deleteAll);
        Button fileSmall = (Button) findViewById(R.id.smallFile);
        Button fileMedium = (Button) findViewById(R.id.mediumFile);
        Button fileLarge = (Button) findViewById(R.id.largeFile);
        Button simulate = (Button) findViewById(R.id.simulate);
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
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.delete();
            }
        });
        fileSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.sendSmall();
            }
        });
        fileMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.sendMedium();
            }
        });
        fileLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.sendLarge();
            }
        });
        simulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Simulate();
            }
        });

    }

    public void Simulate(){
        Handler handler = new Handler();
        count = 0;
        final String[] testCases = new String[]{
                "mMap", // 1
                "mMap"
        };


        Runnable runnable = new Runnable() {
            int i =0;
            @Override
            public void run() {
                i++;
                Log.wtf("HANDLER_RUN","request no: " + i);
                s.runMedium();
            }
        };

        int time = 0;
        handler.postDelayed(runnable, time); // 1
        handler.postDelayed(runnable, time+=500); // 2
        handler.postDelayed(runnable, time+=500); // 2
        handler.postDelayed(runnable, time+=500); // 2
        handler.postDelayed(runnable, time+=500); // 2
        handler.postDelayed(runnable, time+=15000); // 2
        handler.postDelayed(runnable, time+=1000); // 2
        handler.postDelayed(runnable, time+=1000); // 2
        handler.postDelayed(runnable, time+=1000); // 2
        handler.postDelayed(runnable, time+=1000); // 2
        handler.postDelayed(runnable, time+=1000); // 2
        handler.postDelayed(runnable, time+=15000); // 2
        handler.postDelayed(runnable, time+=5000); // 2
        handler.postDelayed(runnable, time+=5000); // 2
        handler.postDelayed(runnable, time+=5000); // 2
        handler.postDelayed(runnable, time+=5000); // 2
        handler.postDelayed(runnable, time+=5000); // 2
        handler.postDelayed(runnable, time+=15000); // 2
        handler.postDelayed(runnable, time+=8000); // 2
        handler.postDelayed(runnable, time+=8000); // 2
        handler.postDelayed(runnable, time+=8000); // 2
        handler.postDelayed(runnable, time+=8000); // 2
        handler.postDelayed(runnable, time+=8000); // 2
        handler.postDelayed(runnable, time+=15000); // 2
        handler.postDelayed(runnable, time+=12000); // 2
        handler.postDelayed(runnable, time+=12000); // 2
        handler.postDelayed(runnable, time+=12000); // 2
        handler.postDelayed(runnable, time+=12000); // 2
        handler.postDelayed(runnable, time+=12000); // 2
        System.out.println("TOTAL TIME: " + time/1000);



//        long start, end;
//        start = System.currentTimeMillis();
//         do {
//             long curr = System.currentTimeMillis();
//             end = curr - start;
//        }while(end < time);
//        Toast.makeText(this, "Simulation done.\nTook " + end + " ms", Toast.LENGTH_LONG).show();
    }




}
