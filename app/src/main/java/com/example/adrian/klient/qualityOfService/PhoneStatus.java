package com.example.adrian.klient.qualityOfService;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Fredrik on 16-04-18.
 */
public class PhoneStatus {
    private float batteryLevel;
    private int signalLevel;
    private double batteryVoltage;
    private double batteryTemp;
    private Context context;
    private String connectionType;

    public final String CONNECTION_DISCONNECTED =  "DISCONNECTED";
    public final String CONNECTION_WIFI = "WIFI";
    public final String CONNECTION_MOBILE = "MOBILE";

    public PhoneStatus(Context context) {
        this.context = context;
    }

    /**
     * Updates the battery status values needed for calculations in the math formula
     */
    private void updateBatteryStatus() {
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

        // Get battery voltage
        int mBatteryVoltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        batteryVoltage = ((double)mBatteryVoltage) / 1000;
        Log.e("BatteryVoltage", "Voltage: " + batteryVoltage);

        // Get battery temperature
        int mBatteryTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        batteryTemp = ((double)mBatteryTemp) / 10;
        Log.e("BatteryTemperature", "Temperature: " + batteryTemp);
    }

    /**
     * Updates the current connection
     */
    private void updateConnectionStatus() {
        // ConnectivityManager handles info about our connections
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get our current network info
        NetworkInfo info = cm.getActiveNetworkInfo();

        // Sets connection type e.g WIFI or MOBILE. If disconnected we set status for this also
        if (info != null && info.isConnectedOrConnecting()) {
            connectionType = info.getTypeName();
            Log.e("ConnectionType: ", "" + connectionType);

            // If we have a mobile connection we want to figure the signal strength since this
            // impacts our consumption a lot
            if(connectionType.equals(CONNECTION_MOBILE) && info.isConnected()) {
                final TelephonyManager telephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                // Fetches the signal strength of mobile connection
                PhoneStateListener phoneStateListener = new PhoneStateListener(){
                    @Override
                    public void onSignalStrengthsChanged (SignalStrength signalStrength) {
                        super.onSignalStrengthsChanged(signalStrength);
                        if(signalStrength.isGsm()) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                signalLevel = signalStrength.getLevel();
                            } else {
                                int lvl  = signalStrength.getGsmSignalStrength();
                                signalLevel = getGsmLevel(lvl);
                            }
                        }
                    }
                };
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                Log.e("3G Level", "Level 3G: " + signalLevel);
            }

            // If we have wifi we use WifiManager to get the signal strength
            if (connectionType.equals(CONNECTION_WIFI) && info.isConnected()) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    // Gets RSSI value of signal strength in dBm
                    int rssi = wifiInfo.getRssi();

                    // Gives us a value of the different signal strength with 5 different levels (0-4)
                    int wifiLevel = WifiManager.calculateSignalLevel(rssi, 3);

                    // Scale 1-4 instead of 0-3
                    signalLevel = wifiLevel + 1;
                    Log.e("Wifi signal", "Level: " + signalLevel);
                }

            }
        } else {
            connectionType = CONNECTION_DISCONNECTED;
        }

    }

    private int getGsmLevel(int lvl) {
        if(lvl < 6 || lvl == 99) {
            return 0;
        } else if (lvl >= 6 && lvl < 12) {
            return 1;
        } else if (lvl >= 12 && lvl < 18) {
            return 2;
        } else if (lvl >= 18 && lvl < 24) {
            return 3;
        } else {
            return 4;
        }
    }

    // Getters
    /**
     * @return The phones battery percentage
     */
    public float getBatteryLevel() {
        updateBatteryStatus();
        return batteryLevel;
    }

    /**
     *
     * @return The phones battery voltage
     */
    public double getBatteryVoltage() {
        updateBatteryStatus();
        return batteryVoltage;
    }

    /**
     *
     * @return Temperatur of the phones battery
     */
    public double getBatteryTemp() {
        updateBatteryStatus();
        return batteryTemp;
    }

    /**
     *
     * @return Current connection type of the phpne
     */
    public String getConnectionType() {
        updateConnectionStatus();
        return connectionType;
    }

    /**
     *
     * @return Signal strength level of current connection
     */
    public int getSignalLevel() {
        updateConnectionStatus();
        return signalLevel;
    }
}
