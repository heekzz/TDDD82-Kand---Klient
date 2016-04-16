package com.example.adrian.klient.mathModelData;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SignalStrength;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.adrian.klient.R;

import org.w3c.dom.Text;

public class MathActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        final TextView connType = (TextView) findViewById(R.id.conn_type_res);
        TextView signalStrength = (TextView) findViewById(R.id.sig_strength_res);
        final TextView batteryVoltage = (TextView) findViewById(R.id.bat_vol_res);
        final TextView batteryPercentage = (TextView) findViewById(R.id.bat_per_res);
        final TextView batteryTemperature = (TextView) findViewById(R.id.bat_temp_res);


        BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            int scale = -1;
            int level = -1;
            int voltage = -1;
            int temp = -1;
            @Override
            public void onReceive(Context context, Intent intent) {
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

//                Log.e("BatteryManager", "level is " + level + "/" + scale + ", temp is " + temp + ", voltage is " + voltage);
                int percentage = (level/scale)*100;
                batteryPercentage.setText(percentage + " %");
                batteryTemperature.setText(temp + " C");
                batteryVoltage.setText(voltage + " V");
            }
        };

        BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
            int connectionType = -1;

            @Override
            public void onReceive(Context context, Intent intent) {
                connectionType = intent.getIntExtra(ConnectivityManager.CONNECTIVITY_ACTION, -1);
            }
        };



        IntentFilter filter1 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        IntentFilter filter2 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(batteryReceiver, filter1);
        registerReceiver(connectionReceiver, filter2);
    }

}
