package com.example.adrian.klient.qualityOfService;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Fredrik on 16-05-05.
 */
public class ConnectionService extends Service {
    private String SERVERADRESS = "fredrikhakansson.se";
    private String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
    private int SERVERPORT = 9000;
    private int SERVERPORT_BACKUP = 9000;

    private BufferedReader in = null;
    private OutputStream fOut = null;
    private PrintWriter out = null;
    private Socket socket = null;

    private JsonParser parser;
    private JsonArray data;
    private JsonObject fromServer;
    private String request, response;

    PhoneStatus manager;
    long init, arrivalTime, deadline, lastDeadline, tMax, tMin;
    int signalLevel, counter;
    double alpha, beta;
    boolean bundling, gettingBetterSignal, readyToSend, highPrio;
    Object lock;

    protected Intent mIntent;
    private Queue<String> sendQueue;

    @Override
    public void onCreate() {
        Log.e("OnCreate", "_________________");
        Log.e("OnCreate", "Service Created");
        lock = new Object();
        sendQueue = new ConcurrentLinkedQueue<>();
        init = (System.nanoTime()/1000000);
        manager = new PhoneStatus(this);
        lastDeadline = 0;
        counter = 1;
        gettingBetterSignal = false;
        highPrio = false;
        parser = new JsonParser();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;

        Log.wtf("ONSTART", "----------------Service started----------------");
        if (intent != null) {
            String newMessage = intent.getStringExtra("MESSAGE");
            if (newMessage != null) {
                Log.wtf("ONSTART", "Request number " + counter++);
                request = newMessage;
                fromServer = (JsonObject) parser.parse(request);
                if((fromServer.get("activity").getAsString()).equals("contact")){
                    highPrio = true;
                } else {
                    highPrio = false;
                }
                // in ms
                if(sendQueue.isEmpty()){
//                    Log.wtf("ONSTART", "Send Queue was empty");
                    arrivalTime = System.nanoTime() / 1000000;
                }

                startAdapting();

                // Without adaptation
//                sendQueue.add(request);
//                send();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        Log.e("ConnectionService", "Service shutdown");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void send() {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                boolean doneSending = false;
                while (!sendQueue.isEmpty()) {
                    try {
                        // Connect to primary server
                        socket = new Socket(SERVERADRESS, SERVERPORT);

                    } catch (IOException e) {
                        // Print error and try connect to backup server
                        Log.wtf("SEND", "Cannot establish connection to " +
                                SERVERADRESS + ":" + SERVERPORT);
                        Log.wtf("SEND", "Trying to connect to backup server on " +
                                SERVERADRESS_BACKUP + ":" + SERVERPORT_BACKUP);
                        try {
                            socket = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
                        } catch (IOException e1) {
//                            e1.printStackTrace();
                            Log.wtf("SEND", "Cannot establish connection to any server :(");
                        }
                    }

                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        fOut = socket.getOutputStream();
                        out = new PrintWriter(fOut);

                        String request;
                        String mResponse;
                        int n = 0;
                        int bytes = 0;

                        //SEND AND RECEIVE
                        while (!doneSending) {
                            n++;
                            // Get the request
                            request = sendQueue.poll();
                            bytes = bytes + request.getBytes().length;
                            out.println(request);
                            out.flush();

                            Log.wtf("SEND", "sent at timestamp " + ((System.nanoTime() / 1000000) - init) + "ms");

                            mResponse = in.readLine();

                            while (mResponse != null) {
//                                Log.wtf("SEND","RESPONSE: " + mResponse);
                                response = mResponse;
                                setData();
                                mResponse = null;
                            }

                            // Resetting deadline
                            lastDeadline = System.nanoTime() / 1000000;

                            // Keep looping until we're done
                            if (!sendQueue.isEmpty()) {
                                doneSending = false;
                            } else {
                                doneSending = true;
                            }
                        }

                        out.println("DONE");
                        Log.wtf("SEND", "Done sending..., Sent " + n + " requests and a total of "+ bytes + " bytes");
                        out.flush();
                        in.close();
                        out.close();
                        socket.close();
                        Log.wtf("SEND", "Connection closed...");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

        }.execute();
    }

    private void startAdapting(){

        float battLvl = manager.getBatteryLevel();
        battLvl = 10;
        String rat = manager.getConnectionType();
        signalLevel = manager.getSignalLevel(); // 0 - 4
//        Log.wtf("INIT","SIGNAL STRENGTH = " + signalLevel);

        setTmax(battLvl);
        setAlpha(rat);
//        setBeta(manager.getSignalLevel());
        setBeta(1);
        if(!bundling){
            setDeadline(arrivalTime, getTmax(), getTmin(), getAlpha(), getBeta());
        }
        scheduleRequests();
    }

    private void checkIfSignalStrengthChanged(){
        int currentSignal = manager.getSignalLevel();
        if(currentSignal != signalLevel){
            long latest = getDeadline();
            setBeta(currentSignal);
            setDeadline(arrivalTime, getTmax(), getTmin(), getAlpha(), getBeta());
            Log.wtf("CONN", "Got a different signal strength (" + signalLevel + "->" + currentSignal + ") AND THE DIFF WAS " + (deadline - latest));
            setSignalLevel(currentSignal);
        }
    }

    private void scheduleRequests() {
        // Send right away
        if(highPrio){
            sendQueue.add(request);
            Log.wtf("SCHEDULE", "High priority packet.. Sending queue");
            readyToSend = true;
            bundling = false;
            send();
        } else {
            if (bundling) {
                Log.wtf("SCHEDULE", "Bundling, adding to queue, (" + (deadline-(System.nanoTime()/1000000))+" ms left on timer)");
                sendQueue.add(request);
            } else {

                new AsyncTask<Void, Void, Void>() {
                    double a = 0.62;
                    double T = 4000;

                    @Override
                    protected Void doInBackground(Void... params) {

                        // Add request to the queue
                        Log.wtf("SCHEDULE", "Adding request");
                        Log.wtf("SCHEDULE", "Waiting until " + deadline + " (" + (deadline-(System.nanoTime()/1000000))+ " ms left)");

                        sendQueue.add(request);
                        bundling = true;
                        readyToSend = false;

                        // TAIL ENDER: Keep sending if we request while in high power state
                        if(manager.getConnectionType().equals("MOBILE")){
                            Log.wtf("TAIL_ENDER", "diff: " + ((lastDeadline + (a * T)) - arrivalTime) + "ms");
                            if (arrivalTime < (lastDeadline + (a * T))) {
                                readyToSend = true;
                                bundling = false;
                                Log.wtf("TAIL_ENDER", "Adding request to tail, sending");
                                send();
                            }
                        }

                        long currentTime;
                        while (!readyToSend) {
                            currentTime = System.nanoTime() / 1000000;
                            checkIfSignalStrengthChanged();
                            if (currentTime >= deadline) {
                                readyToSend = true;
                                bundling = false;
                                Log.wtf("SCHEDULE", "Deadline reached!! Sending...");
                                send();
                            }
                        }
                        return null;
                    }
                }.execute();
            }
        }
    }

    private void setSignalLevel(int currentLevel){
        signalLevel = currentLevel;
    }

    private void setTmax(float battLvl){
        // HIGH BATTERY.
        if(battLvl >= 40){
            tMax = 30*1000; // 30 seconds
            tMin = 8*1000;  // 1 * tr seconds

            // MEDIUM BATTERY
        } else if (battLvl > 20 && battLvl < 40){
            tMax = 60*1000; // 60 seconds
            tMin = 16*1000; // 2 * tr seconds

            // LOW BATTERY
        } else if (battLvl <= 20){
            tMax = 90*1000; // 90 seconds
            tMin = 32*1000; // 4 * tr seconds
        }
    }

    private long getTmax(){
        return tMax;
    }

    private long getTmin(){
        return tMin;
    }

    private void setAlpha(String rat) {
        if (rat.equals("WIFI")) {
            alpha = 1 - (600.0/1400.0);
        } else {
            alpha = 1;
        }
    }

    private double getAlpha(){
        return alpha;
    }

    private void setBeta(int signalLevel) {
        double eXmin , eXmax;
        double eX = 0;

        if(manager.getConnectionType().equals("WIFI")){
            eXmin = 7.5;
            eXmax = 26.0;

            switch (signalLevel){
                case 1:
                    eX = 26.0;
                    break;
                case 2:
                    eX = 10.0;
                    break;
                case 3:
                    eX = 7.5;
                    break;
                default:
                    Log.wtf("BETA", "Got signalLevel " + signalLevel + " for some reason");
                    eX = 1.0;
                    break;
            }

        } else {

            // PÅHITTADE VÄRDEN FÖR NU
            eXmin = 300.0;
            eXmax = 450.0;
            switch (signalLevel){
                case 1:
                    eX = 450.0;
                    break;
                case 2:
                    eX = 370.0;
                    break;
                case 3:
                    eX = 320.0;
                    break;
                case 4:
                    eX = 300.0;
                    break;
                default:
                    Log.wtf("BETA", "Got signalLevel " + signalLevel + " for some reason");
                    break;
            }
        }

        Log.wtf("SET B", "x,max,min " + eX + "," + eXmax+ "," + eXmin);
        beta = (eX - eXmin)/(eXmax - eXmin);
    }

    private double getBeta(){
        return beta;
    }

    private void setDeadline(long arrival, long tMax, long tMin, double a, double b){

        // in ms
        deadline = (long)(arrival + tMin + ((tMax - tMin) * a * b));
        Log.wtf("GET DEAD", "deadline set to " + (deadline - arrival) + ", Beta=" + b + ", alpha=" + a);
        Log.wtf("GET DEAD", "deadline set to " + (deadline - arrival) + ", sending in " + (deadline - (System.nanoTime()/1000000))+" ms");
//        Log.wtf("GET DEAD" , "Arrival time btw is" + arrival);
    }

    private long getDeadline(){
//        Log.wtf("GET DEAD", "deadline is " + (deadline - arrivalTime) + " ms");
        return deadline;
    }

    private void setData() {
        try {
            mIntent.putExtra("SERVER_REPLY", response);
            fromServer = (JsonObject) parser.parse(response);
            data = (JsonArray) fromServer.get("data");
        } catch (Exception e) {
            Log.wtf("SET_DATA", "Failed to set data...");

        }
    }
}