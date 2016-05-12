package com.example.adrian.klient.testSimulator;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adrian.klient.R;
import com.example.adrian.klient.qualityOfService.ConnectionService;

import java.util.concurrent.atomic.AtomicLong;

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
        final boolean[] done = {false};
//        startService(new Intent(getApplicationContext(), ConnectionService.class));
        Handler handler = new Handler();
        count = 0;
        final String[] testCases = new String[]{
                "sMap", // 1
                "mMap", // 2
                "lMap", // 3
                "sFile", // 4
                "sMap", // 5
                "mMap", // 6
                "lMap", // 7
                "mFile", // 8
                "sMap", // 9
                "mMap", // 10
                "lMap", // 11
                "lFile", // 12
                "lMap", // 13
                "sFile", // 14
                "delete", // 15
                "STOP" // 16
        };


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Count = " + count);
                switch (testCases[count]) {
                    case "sMap":
                        s.runSmall();
                        break;
                    case "mMap":
                        s.runMedium();
                        break;
                    case "lMap":
                        s.runLarge();
                        break;
                    case "sFile":
                        s.sendSmall();
                        break;
                    case "mFile":
                        s.sendMedium();
                        break;
                    case "lFile":
                        s.sendLarge();
                        break;
                    case "delete":
                        s.delete();
                        break;
                    default:
                        try {
                            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                            r.play();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), "Test DONE!", Toast.LENGTH_LONG).show();
                        System.out.println("Error in switch");
                        break;
                }
                count = (count + 1) % testCases.length;
            }
        };
        int time = 0;
        handler.postDelayed(runnable, time); // 1
        handler.postDelayed(runnable, time+=3000); // 2
        handler.postDelayed(runnable, time+=1000); // 3
        handler.postDelayed(runnable, time+=4000); // 4
        handler.postDelayed(runnable, time+=10000); // 5
        handler.postDelayed(runnable, time+=2000); // 6
        handler.postDelayed(runnable, time+=4000); // 7
        handler.postDelayed(runnable, time+=1000); // 8
        handler.postDelayed(runnable, time+=20000); // 9
        handler.postDelayed(runnable, time+=8000); // 10
        handler.postDelayed(runnable, time+=7000); // 11
        handler.postDelayed(runnable, time+=16000); // 12
        handler.postDelayed(runnable, time+=1000); // 13
        handler.postDelayed(runnable, time+=1000); //14
        handler.postDelayed(runnable, time+=6000); // 15
        handler.postDelayed(runnable, time+=1000); // 16

//        long start, end;
//        start = System.currentTimeMillis();
//         do {
//             long curr = System.currentTimeMillis();
//             end = curr - start;
//        }while(end < time);
//        Toast.makeText(this, "Simulation done.\nTook " + end + " ms", Toast.LENGTH_LONG).show();
    }




}
