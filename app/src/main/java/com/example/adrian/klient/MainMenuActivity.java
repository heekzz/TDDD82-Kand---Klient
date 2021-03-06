package com.example.adrian.klient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.adrian.klient.ServerConnection.ServerActivity;
import com.example.adrian.klient.contactList.ContactList;
import com.example.adrian.klient.maps.MapsActivity;
import com.example.adrian.klient.qualityOfService.QosActivity;
import com.example.adrian.klient.testSimulator.SimulateActivity;
import com.example.adrian.klient.video.VideoCommunication;

/**
 * Created by dennisdufback on 2016-03-17.
 */
public class MainMenuActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);


        SharedPreferences userInfo = getSharedPreferences("USER_INFO", Context.MODE_PRIVATE);
        String name = userInfo.getString("USER_NAME", "Anon");

        TextView hello = (TextView) findViewById(R.id.Hello___);
        Button vidComButton = (Button) findViewById(R.id.vidComButton);
        Button mapButton = (Button) findViewById(R.id.mapButton);
        Button serverButton = (Button) findViewById(R.id.serverButton);
        Button contactButton = (Button) findViewById(R.id.contactButton);
        Button simulateButton = (Button) findViewById(R.id.simulateButton);
        Button mathButton = (Button) findViewById(R.id.mathButton);

        hello.setText("Hello, " + name);
        vidComButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);
        serverButton.setOnClickListener(this);
        contactButton.setOnClickListener(this);
        simulateButton.setOnClickListener(this);
        mathButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch(v.getId()) {
            case R.id.vidComButton:
                intent = new Intent(this, VideoCommunication.class);
                startActivity(intent);
                break;

            case R.id.mapButton:
                intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                break;

            case R.id.serverButton:
                intent = new Intent(this, ServerActivity.class);
                startActivity(intent);
                break;

            case R.id.contactButton:
                intent = new Intent(this, ContactList.class);
                startActivity(intent);
                break;
            case R.id.simulateButton:
                intent = new Intent(this, SimulateActivity.class);
                startActivity(intent);
                break;
            case R.id.mathButton:
                intent = new Intent(this, QosActivity.class);
                startActivity(intent);
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
