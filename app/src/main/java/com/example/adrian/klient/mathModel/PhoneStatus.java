package com.example.adrian.klient.mathModel;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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

    public PhoneStatus(Context context) {
        this.context = context;
    }

    /**
     * This method updates the battery status values needed for calculations in the math formula
     */
    private void getBatteryStatus() {
        // IntentFilter that listens to when the battery is changed and gets data from it
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, batteryFilter);

        // Get battery level (if 56% the values is 56)
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        // Get the scale (should be 100 but we collect in anyway)
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Calc percentage
        batteryLevel = ((float)level / (float)scale) * 100;
        Log.e("BatteryLevel", "Level: " + (int)batteryLevel + " %"); //print
    }

    /**
     * Updates the current connection
     */
    private void getConnectionStatus() {
        // ConnectivityManager handles info about our connections
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get our current network info
        NetworkInfo info = cm.getActiveNetworkInfo();

        // Sets connection type e.g WIFI or MOBILE. If disconnected we set status for this also
        if (info != null && info.isConnectedOrConnecting()) {
            connectionType = info.getTypeName();
            Log.e("ConnectionType: ", "" + connectionType);
        } else {
            connectionType = CONNECTION_DISCONNECTED;
        }

        // If we have a mobile connection we want to figure the signal strength since this
        // impacts our consumption a lot
        if(connectionType.equals(CONNECTION_MOBILE) && info.isConnected()) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Fetches the signal strength
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

        // If we have wifi we use WifiManager to get signal strength
        if (connectionType.equals(CONNECTION_WIFI) && info.isConnected()) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                // Gets rssi value of signal strength in dBm
                int rssi = wifiInfo.getRssi();

                // Gives us a value of the different signal strength with 4 different levels
                int wifiLevel = wifiManager.calculateSignalLevel(rssi, 4);
                Log.e("Wifi signal", "Level: " + wifiLevel);
            }

        }


    }

    private int calculate(){
        int res;
        double batteryFactor = 0.8;
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

        battery = battery * batteryFactor;

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
