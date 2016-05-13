package com.example.adrian.klient.qualityOfService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import android.widget.Toast;

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



        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                            "STOP" // 15
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
                    handler.postDelayed(runnable, time+=7000); // 16

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

                            TelephonyManager telephonyManager = (TelephonyManager)
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