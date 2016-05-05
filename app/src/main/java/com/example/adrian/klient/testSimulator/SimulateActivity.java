package com.example.adrian.klient.testSimulator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adrian.klient.R;

import java.util.concurrent.atomic.AtomicLong;

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

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    start.addAndGet(System.currentTimeMillis());
                    s.runSmall();
                    Thread.sleep(8000);
//                    wait(8000);
                    s.runMedium();
                    Thread.sleep(8000);
//                    wait(8000);
                    s.runLarge();
                    Thread.sleep(8000);
//                    wait(8000);
                    s.sendSmall();
                    Thread.sleep(10000);
//                    wait(10000);
                    s.sendMedium();
                    Thread.sleep(18000);
//                    wait(15000);
                    s.sendLarge();
                    Thread.sleep(22000);
//                    wait(20000);
                    s.delete();
                    done[0] = true;
                    end.addAndGet(System.currentTimeMillis() - start.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        do {
            if(done[0]) {
                Toast.makeText(SimulateActivity.this,"DONE, took " +  end.get()/1000 + " ",Toast.LENGTH_SHORT).show();
            }
        }while(!done[0]);

    }

}
