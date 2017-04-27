package com.example.adrian.klient.qualityOfService;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.adrian.klient.R;
import com.example.adrian.klient.testSimulator.Simulator;

public class QosActivity extends AppCompatActivity {


    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver connectionReceiver;
    final Simulator s = new Simulator(QosActivity.this);
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qos);


        final TextView connType = (TextView) findViewById(R.id.conn_type_res);
        final TextView sigStrength = (TextView) findViewById(R.id.sig_strength_res);
        final TextView batteryVoltage = (TextView) findViewById(R.id.bat_vol_res);
        final TextView batteryPercentage = (TextView) findViewById(R.id.bat_per_res);
        final TextView batteryTemperature = (TextView) findViewById(R.id.bat_temp_res);
        Button updateButton = (Button) findViewById(R.id.updateBar);
        final TextView ipAddress = (TextView) findViewById(R.id.ipAddressText);

        final PhoneStatus phoneStatus = new PhoneStatus(this);
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

//                        if(ActivityCompat.shouldShowRequestPermissionRationale(,Manifest.permission.ACCESS_COARSE_LOCATION)){
//                            Log.wtf("PERMISSION","Show explanation");
//                        } else {

            Log.wtf("PERMISSION", "REQUESTING PERMISSION");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1337);

//                        }


        }


        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler();
                count = 0;
                final String[] testCases = new String[]{
                        "mMap", // 1
                        "mMap"
                };


                Runnable runnable = new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        i++;
                        Log.wtf("HANDLER_RUN", "request no: " + i);
                        s.runMedium();
                    }
                };
            }
        });

        /**
         * BroadcastReceiver monitor intents and perform actions when we have changes
         * in in this case the battery.
         */
        batteryReceiver = new BroadcastReceiver() {
            int scale = -1;
            int level = -1;
            int voltage = -1;
            int temp = -1;

            // Called when changes in battery occurs.
            @Override
            public void onReceive(Context context, Intent intent) {
                // Gets current values of battery level, temperature of battery and battery voltage
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                // Prints current values
                Log.e("BatteryManager", "level is " + level + "/" + scale + ", temp is " + temp
                        + ", voltage is " + voltage);

                double percentage = ((double) level / (double) scale) * 100;
                double voltageD = (double) voltage / 1000;
                double tempD = (double) temp / 10;
                Log.e("Percentage", percentage + " %");

                // Updates graphical presentation in app
                batteryPercentage.setText((int) percentage + " %");
                batteryTemperature.setText(tempD + " C");
                batteryVoltage.setText(voltageD + " V");
            }
        };

        connectionReceiver = new BroadcastReceiver() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            int connectionType = -1;

            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                WifiManager mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();

                ipAddress.setText(intToIP(mWifiInfo.getIpAddress()));

                boolean isConnected = info != null && info.isConnectedOrConnecting();

                if (isConnected) {
                    connectionType = info.getType();
                    switch (connectionType) {
                        case ConnectivityManager.TYPE_WIFI:
                            connType.setText(info.getTypeName());
                            int rssi = mWifiInfo .getRssi();
                            sigStrength.setText("Wifi lvl: " + WifiManager.calculateSignalLevel(rssi, 3));
                            break;
                        case ConnectivityManager.TYPE_MOBILE:
                            connType.setText(info.getTypeName());

                            final TelephonyManager telephonyManager = (TelephonyManager)
                                    getSystemService(Context.TELEPHONY_SERVICE);

                            PhoneStateListener phoneStateListener = new PhoneStateListener() {
                                @Override
                                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                                    super.onSignalStrengthsChanged(signalStrength);
                                    if (signalStrength.isGsm()) {
                                        sigStrength.setText("Lvl: " + signalStrength.getLevel());
                                    }
                                }
                            };

                            telephonyManager.listen(phoneStateListener,
                                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                            break;
                        default:
                            Log.e("Connection type", "Unknown connection type");
                    }
                } else {
                    connType.setText("No connection");
                }

            }
        };



        IntentFilter filter1 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        IntentFilter filter2 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        IntentFilter filter3 = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        registerReceiver(batteryReceiver, filter1);
        registerReceiver(connectionReceiver, filter2);
        registerReceiver(connectionReceiver, filter3);

    }
    public String intToIP(int i) {
        return (( i & 0xFF)+ "."+((i >> 8 ) & 0xFF)+
                "."+((i >> 16 ) & 0xFF)+"."+((i >> 24 ) & 0xFF));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(connectionReceiver);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        IntentFilter filter1 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        IntentFilter filter2 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(batteryReceiver, filter1);
        registerReceiver(connectionReceiver, filter2);
    }
}