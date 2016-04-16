package com.example.adrian.klient.ServerConnection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.adrian.klient.MainActivity;

/**
 * Created by dennisdufback on 16-04-15.
 */
public class AppRestart extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("SHUTTING DOWN...");
    }

    public void doRestart() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        System.exit(0);
    }
}


