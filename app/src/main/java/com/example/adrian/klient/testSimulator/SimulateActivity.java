package com.example.adrian.klient.testSimulator;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adrian.klient.R;
import com.example.adrian.klient.qualityOfService.ConnectionService;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;

public class SimulateActivity extends AppCompatActivity {

    final Simulator s = new Simulator(SimulateActivity.this);

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
        final AtomicLong start = new AtomicLong(0), end= new AtomicLong(0);
                    startService(new Intent(getApplicationContext(), ConnectionService.class));
        final boolean[] next = new boolean[1];
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                    start.addAndGet(System.currentTimeMillis());
                    s.runSmall();
//        CountDownTimer count = new CountDownTimer(3000, 1000) {
//
//            public void onTick(long millisUntilFinished) {
//                Log.d("CountDown", "seconds remaining: " + millisUntilFinished / 1000);
//            }
//
//            public void onFinish() {
//                Log.d("CountDown", "done!");
//                next[0] = true;
//            }
//        }.start();
//                    wait(8000);
                    s.runMedium();
//        waitFor(5);
//                    wait(8000);
                    s.runLarge();
        waitFor(5);
//                    wait(8000);
                    s.sendSmall();
////                    wait(10000);
//                    s.sendMedium();
////                    wait(15000);
//                    s.sendLarge();
//                    wait(20000);
//                    s.delete();
//                    done[0] = true;
//                    end.addAndGet(System.currentTimeMillis() - start.get());
//
//            }
//        }).start();
//        do {
//            if(done[0]) {
//                Toast.makeText(SimulateActivity.this,"DONE, took " +  end.get()/1000 + " ",Toast.LENGTH_SHORT).show();
//            }
//        }while(!done[0]);

    }

    private void waitFor(int seconds) {
        long start = System.currentTimeMillis();
        long end;
        do {
            end = System.currentTimeMillis();
        } while (end - start < seconds*1000);

    }

}
