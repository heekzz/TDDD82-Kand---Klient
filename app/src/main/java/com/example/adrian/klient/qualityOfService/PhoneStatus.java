package com.example.adrian.klient.qualityOfService;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.util.Arrays;

/**
 * Created by Fredrik on 16-04-18.
 */
public class PhoneStatus {
    private float batteryLevel;
    private int rssi;
    private int cellularLevel;
    private double batteryVoltage;
    private double batteryTemp;
    private Context context;
    private String connectionType;
    private TelephonyManager tm;
    CircularFifoBuffer signalLevels;


    public final String CONNECTION_DISCONNECTED = "DISCONNECTED";
    public final String CONNECTION_WIFI = "WIFI";
    public final String CONNECTION_MOBILE = "MOBILE";

    public PhoneStatus(Context context) {
        this.context = context;
        signalLevels = new CircularFifoBuffer(30);

       PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                cellularLevel = signalStrength.getLevel();
//                Log.wtf("OnSignalChanged", "CellularLevel: " + cellularLevel);

            }
        };
        tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
//        Log.wtf("BEFORE SIGNAL CHANGE", "CellularLevel: " + cellularLevel);
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
//        Log.e("BatteryLevel", "Level: " + (int)batteryLevel + " %"); //print

        // Get battery voltage
        int mBatteryVoltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        batteryVoltage = ((double)mBatteryVoltage) / 1000;
//        Log.e("BatteryVoltage", "Voltage: " + batteryVoltage);

        // Get battery temperature
        int mBatteryTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        batteryTemp = ((double)mBatteryTemp) / 10;
//        Log.e("BatteryTemperature", "Temperature: " + batteryTemp);
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
//            Log.e("ConnectionType: ", "" + connectionType);
//
            // If we have a mobile connection we want to figure the signal strength since this
            // impacts our consumption a lot
            if (connectionType.equals(CONNECTION_MOBILE) && info.isConnected()) {
                try {

                    int cdmaDbm = 0;
                    int levelDbm = 0;
                    for (final CellInfo cellInfo : tm.getAllCellInfo()) {
                        if (cellInfo instanceof CellInfoGsm) {
                            final CellSignalStrengthGsm gsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
//                            dbm = gsm.getAsuLevel();
                        } else if (cellInfo instanceof CellInfoCdma) {
                            final CellSignalStrengthCdma cdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
//                            cdmaDbm = cdma.getAsuLevel();
                        } else if (cellInfo instanceof CellInfoLte) {
                            final CellSignalStrengthLte lte = ((CellInfoLte) cellInfo).getCellSignalStrength();
                            cdmaDbm = lte.getDbm();
                        } else if(cellInfo instanceof CellInfoWcdma){
                            final CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                            cdmaDbm = wcdma.getDbm();
                            signalLevels.add(cdmaDbm);
//                            signalLevel = wcdma.getLevel();
                        } else {
                            throw new Exception("Unknown type of cell signal!");
                        }



//                        Log.wtf("PHONE_STATUS", "RSSI : LEVEL " + cdmaDbm + " : " + signalLevel);
//                        signalLevel = levelDbm;
                    }
//
//                    Log.wtf("DECIBELLL","in dbm: " + cdmaDbm);
//                    Log.wtf("DECIBELLL", "level: " + levelDbm);
//
                } catch (Exception e) {
//                    Log.e("CONNECTION_MOBILE", "Unable to obtain cell signal information", e);
                }
//                signalLevel = cellularLevel;
//                Log.e("PHONESTATUS", "3G Strength: " + signalLevel);

                // If we have wifi we use WifiManager to get the signal strength
            } else if (connectionType.equals(CONNECTION_WIFI) && info.isConnected()) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    // Gets RSSI value of signal strength in dBm
                    rssi = wifiInfo.getRssi();

//                    Log.e("Wifi signal", "Level: " + signalLevel);
                }

            } else {
                connectionType = CONNECTION_DISCONNECTED;
            }
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

        if(connectionType.equals(CONNECTION_MOBILE)) {

            int s = 0;
            int l;
            Object[] latestStrenghts = signalLevels.toArray();
            Integer[] latest = Arrays.copyOf(latestStrenghts, latestStrenghts.length, Integer[].class);

            for (int current : latest) {
                s = s + current;
            }
            s = s / latest.length;

            if (s > -85)
                l = 4;
            else if (s > -95)
                l = 3;
            else if (s >= -100)
                l = 2;
            else
                l = 1;
//            Log.wtf("GET LEVEL", "S ÄR" + s + ", OCH Level är " + l);
            return l;
        } else {

            int signalLevel;

            if (rssi >= -80)
                signalLevel = 3;
            else if (rssi >= -85)
                signalLevel = 2;
            else if (rssi < -85)
                signalLevel = 1;
            else
            signalLevel = 0;
//            Log.wtf("GET LEVEL", "S ÄR" + signalLevel + ", OCH RSSI är " + rssi);
            return signalLevel;
        }
    }
}
