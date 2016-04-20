package com.example.adrian.klient.mathModel;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Fredrik on 16-04-18.
 */
public class PhoneStatus {
    private Context context;
    private float batteryLevel;
    private String connectionType;
    private int signalLevel;
    public final String CONNECTION_DISCONNECTED =  "DISCONNECTED";
    public final String CONNECTION_WIFI = "WIFI";
    public final String CONNECTION_MOBILE = "MOBILE";
    private int level = 3;

    public PhoneStatus(Context context) {
        this.context = context;
    }

    private void getBatteryStatus() {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, batteryFilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryLevel = level;
        Log.e("BatteryLevel", "Level: " + batteryLevel);
    }

    private void getConnectionStatus() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info != null && info.isConnectedOrConnecting()) {
            // Sets connection type e.g WIFI or MOBILE
            connectionType = info.getTypeName();
            Log.e("ConnectionType: ", "" + connectionType);
        } else {
            connectionType = CONNECTION_DISCONNECTED;
        }

        if(connectionType == "MOBILE") {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            PhoneStateListener phoneStateListener = new PhoneStateListener(){
                @Override
                public void onSignalStrengthsChanged (SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    if(signalStrength.isGsm()) {
                        signalLevel = signalStrength.getLevel();
                    }
                }
            };
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }


    }

    private int calculate(){
        int res;
        double battteryFactor = 0.8;
        double connectionFactor = 1.2;
        double connection = 0;
        double battery;

        if (batteryLevel < 20) {
            battery = 1;
        } else if (batteryLevel >= 20 && batteryLevel < 50) {
            battery = 2;
        } else if (batteryLevel >= 50 && batteryLevel < 80) {
            battery = 3;
        } else {
            battery = 4;
        }

        battery = battery * battteryFactor;

        if (connectionType.equals(CONNECTION_MOBILE)) {
            connection = 1;
            Log.e("ConnectionType", "Mobile");
        } else if (connectionType.equals(CONNECTION_WIFI)) {
            connection = 2;
            Log.e("ConnectionType", "WiFi");
        }
        connection = connection * connectionFactor;

        res = (int)connection + (int)battery;

        if (connectionType.equals(CONNECTION_DISCONNECTED)) {
            res = 0;
            Log.e("ConnectionType", "Disconnected");
        }

        Log.e("ConnectionType", "Result: " + res);
        return res;
    }

    public int getPhoneLevel() {
        getBatteryStatus();
        getConnectionStatus();
        return calculate();
    }


}
