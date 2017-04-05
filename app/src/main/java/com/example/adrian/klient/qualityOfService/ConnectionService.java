package com.example.adrian.klient.qualityOfService;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.adrian.klient.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
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
    boolean success;

    PhoneStatus manager;
    long init, arrivalTime, deadline, lastDeadline;
    boolean sending, waitingForRequest, gettingBetterSignal, readyToSend;
    Object lock;

    protected Intent mIntent;
    private Queue<String> sendQueue;

    @Override
    public void onCreate() {
        Log.e("OnCreate", "_________________");
        Log.e("OnCreate", "Service Created");
//        sendQueue = new TreeMap<>();
        lock = new Object();
        sendQueue = new ConcurrentLinkedQueue<>();
        init = (System.nanoTime()/1000000);
        manager = new PhoneStatus(this);
        lastDeadline = 0;
        sending = false;
        waitingForRequest = false;
        gettingBetterSignal = false;
        parser = new JsonParser();
        Toast.makeText(getApplicationContext(), "Connection service started", Toast.LENGTH_LONG).show();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;

        Log.wtf("ONSTART", "__________________");
        Log.wtf("ONSTART", "Service started");
        Log.wtf("ONSTART", "__________________");
        if (intent != null) {
            String newMessage = intent.getStringExtra("MESSAGE");
            if (newMessage != null) {

                request = newMessage;
                arrivalTime = System.nanoTime() / 1000000;

                // Start adaptation
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

                        //SEND AND RECEIVE
                        while (!doneSending) {
                            n++;
                            // Get the request
                            request = sendQueue.poll();
                            out.println(request);
                            out.flush();

                            Log.wtf("SEND", "sent at timestamp " + ((System.nanoTime() / 1000000) - init) + "ms");

                            mResponse = in.readLine();

                            while (mResponse != null) {
//                                Log.wtf("SEND","RESPONSE: " + mResponse);
                                response = mResponse;
                                setActive();
                                setSuccess();
                                setData();
                                mResponse = null;
                            }

                            if (isFileTransfer(request)) {
                                sendFile(request);
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
                        Log.wtf("SEND", "Done sending...");
                        Log.wtf("SEND", "Sent " + n + " requests");
                        out.flush();
                        in.close();
                        out.close();
                        socket.close();
                        Log.wtf("SEND", "Connection closed...");
                    synchronized (lock){
                        waitingForRequest = false;
                        sending = false;
                        Log.wtf("SEND", "Notifying send lock");
                        lock.notify();
                    }
//
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
        String rat = manager.getConnectionType();
        int signalStrength = manager.getSignalLevel(); // 0 - 4

        // Three different main 'states'
        // HIGH BATTERY.
        if(battLvl >= 60){
            if(rat.equals("WIFI")){
                // (SS, factor, timer)
            // Bundle by smallest amount, timer 30s
                scheduleWiFi(signalStrength, 1.0, 30);

            } else if(rat.equals("MOBILE")){
                // Keep standard deadline timer
                scheduleMobile(signalStrength, 1.0);
            }

            // MEDIUM BATTERY
        } else if (battLvl > 20 && battLvl < 60){
            if(rat.equals("WIFI")){
                // Increase buffer size by a factor of 3.0, timer 45s
                scheduleWiFi(signalStrength, 3.0, 45);

            } else if(rat.equals("MOBILE")){
                // Increase deadline by a factor of 3.0
                scheduleMobile(signalStrength, 3.0);
            }

            // LOW BATTERY
        } else if (battLvl <= 20){

            if(rat.equals("WIFI")){
                // Increase buffer size by a factor of 4, timer 60s
                scheduleWiFi(signalStrength, 4.0, 60);
            } else if(rat.equals("MOBILE")){
                // Increase deadline by a factor of 4
                scheduleMobile(signalStrength, 4.0);
            }
        }

    }


    private long updateBuffer(){
        int totalSize = 0;
        for(String req : sendQueue){
            byte[] size = req.getBytes();
            if(isFileTransfer(req)){
                JsonObject object = (JsonObject) new JsonParser().parse(req);
                JsonObject o = (JsonObject) object.get("data").getAsJsonArray().get(0);
                totalSize += o.get("filesize").getAsInt();
            }
            totalSize += size.length;
        }
        return totalSize;
    }

    private void scheduleWiFi(final int signalStrength, final double factor, final long timer) {

        Log.wtf("SCHEDULING","Signal strength for current req: " + signalStrength);

        if(sending){
            Log.wtf("SCHEDULING_WIFI", "sending = true, Adding to sendQueueueu");
            synchronized (lock){
                while(sending){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                waitingForRequest = false;
            }

        }
        if(waitingForRequest) {
            Log.wtf("SCHEDULING_WIFI","Waiting for buffer to fill.. adding to queue");
            sendQueue.add(request);
            Log.wtf("SCHEDULING_WIFI", "BUFFER SIZE: " + updateBuffer() + " bytes");
        } else {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    double BUFF_SIZE = 0;
                    switch (signalStrength) {
                        case 0:
                            BUFF_SIZE = 15000*factor; // 15kB
                            break;
                        case 1:
                            BUFF_SIZE = 12000*factor; // 12kB
                            break;
                        case 2:
                            BUFF_SIZE = 7000*factor; // 7kB
                            break;
                    }

                    // Add request to the queue
                    Log.wtf("SCHEDULE WIFI", "Added first request");
                    Log.wtf("SCHEDULE WIFI", "Waiting to fill " +BUFF_SIZE / 1000+ " kB or for 20s to pass");
                    // Always 30s deadline timer
                    deadline = (System.nanoTime() / 1000000) + (timer * 1000);

                    sendQueue.add(request);
                    readyToSend = false;
                    waitingForRequest = true;

                    while (!readyToSend) {
                        long currentTime = System.nanoTime() / 1000000;

                        // When the buffer is full, send, unless we have bad signal Strength
                        if (updateBuffer() >= BUFF_SIZE) {
                            Log.wtf("SCHEDULE WIFI", "Buffer is full now! Sending...");
                            sending = true;
                            readyToSend = true;

                            if (manager.getSignalLevel() == 0) { // Not for 1,2
                                Log.wtf("SCHEDULE WIFI", "Super bad Signal strength, waiting for better connection");
                                gettingBetterSignal = true;

                                waitForBetterConnection();

                                try {
                                    Log.wtf("SCHEDULING", "waiting on lock");

                                    synchronized (lock) {
                                        while (gettingBetterSignal) {
                                            lock.wait();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                            send();
                        } else if(currentTime >= deadline){
                            sending = true;
                            readyToSend = true;
                            Log.wtf("SCHEDULE_WIFI", "Deadline reached!! Sending...");
                            send();
                        }

                    }

                    return null;
                }

                private void waitForBetterConnection() {
                    Log.wtf("SCHEDULE WIFI", "Waiting for better connection for max 20 seconds");
                    final Timer timer = new Timer();

                    timer.schedule(new TimerTask() {
                        int i = 0;
                        int n = 0;

                        @Override
                        public void run() {
                            n++;
                            Log.wtf("TIMER WIFI", "getting level...");
                            if (manager.getSignalLevel() > 0) { // 0 is worst
                                i++;
                                Log.wtf("TIMER WIFI", "Got better signal strength! i= " + i);
                            } else { // reset counter
                                Log.wtf("TIMER WIFI", "Not good yet");
                                i = 0;
                            }
                            // 2.5 seconds of better Wifi or 20 s has passed
                            if (i == 5 || n == 40) {
                                if (n == 40) {
                                    Log.wtf("TIMER WIFI", "20s passed, sending");
                                }
                                synchronized (lock){
                                    gettingBetterSignal = false;
                                    lock.notify();
                                }
                                timer.cancel();
                            }

                        }
                    }, 0, 500);
                }

            }.execute();
        }


    }

    private void scheduleMobile(final int signalStrength, final double factor){

        if(sending){
            Log.wtf("SCHEDULING_3G", "Waiting for send Lock");
            synchronized (lock){
                while(sending){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                waitingForRequest = false;
            }
        }
        if(waitingForRequest) {
            Log.wtf("SCHEDULING_3G","Waiting on first request.. Adding to sendQueue");
            sendQueue.add(request);
        } else {
            new AsyncTask<Void, Void, Void>() {
                double a = 0.62;
                double T = 4000; // High power state for 4 seconds (3G)

                @Override
                protected Void doInBackground(Void... params) {

                    // This decides the deadline timer
                    Log.wtf("SCHEDULING_3G","Signal strength for this request is " + signalStrength);
                    int limit = 0;
                    double increase = 0;
                    deadline = (System.nanoTime() / 1000000);
                    switch (signalStrength) { // 0-4 for 3G
                        case 0:
                            increase = (15*1000)*factor; //15s standard
                            deadline += increase;
                            Log.wtf("SCHEDULING_3G", "Gave request a deadline of "+ increase/1000 +" s");
                            limit = 20;
                            break;
                        case 1:
                            increase += (12*1000)*factor; //12s standard
                            deadline += increase;
                            Log.wtf("SCHEDULING_3G", "Gave request a deadline of "+ increase/1000 +" s");
                            limit = 15;
                            break;
                        case 2:
                            increase += (9*1000)*factor; //9s standard
                            deadline += increase;
                            Log.wtf("SCHEDULING_3G", "Gave request a deadline of "+ increase/1000 +" s");
                            limit = 10;
                            break;
                        case 3:
                            increase += (7*1000)*factor;//7s standard
                            deadline += increase;
                            Log.wtf("SCHEDULING_3G", "Gave request a deadline of "+ increase/1000 +" s");
                            break;
                        case 4:
                            increase += (5*1000)*factor; //5s standard
                            deadline += increase;
                            Log.wtf("SCHEDULING_3G", "Gave request a deadline of "+ increase/1000 +" s");
                            break;
                    }

                    // Keep sending if we request while in high power state
                    Log.wtf("SCHEDULING_3G", "diff: " + ((lastDeadline + (a * T)) - arrivalTime) + "ms");
                    if (arrivalTime < (lastDeadline + (a * T))) {
                        readyToSend = true;
                        sendQueue.add(request);
                        Log.wtf("SCHEDULING_3G", "Adding request to tail, sending");
                        sending = true;
                        send();
                    } else {
                        // Request needs to be put in queue
                        // Add the request to the queue and wait for its deadline
                        Log.wtf("SCHEDULING_3G", "First request");
                        Log.wtf("SCHEDULING_3G", "Putting request in queue, not sending.");
                        sendQueue.add(request);
                        readyToSend = false;
                        waitingForRequest = true;

                    }
                    // A request is in the queue
                    long currentTime;
                    while (!readyToSend) {
                        // Checks deadline time
                        currentTime = System.nanoTime() / 1000000;
                        // Deadline reached
                        if (currentTime >= deadline) {
                            sending = true;
                            Log.wtf("SCHEDULING_3G", "deadline was reached");

                            if (manager.getSignalLevel() <= 2) {
                                Log.wtf("SCHEDULING_3G", "Signal strength is bad, waiting for better connection");
                                gettingBetterSignal = true;

                                waitForBetterConnection(limit);

                                try {
                                    Log.wtf("SCHEDULING_3G", "waiting on lock");

                                    synchronized (lock){
                                        while(gettingBetterSignal){
                                            lock.wait();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.wtf("SCHEDULING_3G", "Signal Level Fine.. Sending");
                            }

                            send();

                            Log.wtf("SCHEDULING_3G", "Setting stupid variables");
                            readyToSend = true;
                        }
                        updateBuffer();

                    }
                    return null;
                }

                private void waitForBetterConnection(final int limit) {
                    Log.wtf("TIMER_3G", "Waiting for better connection for max 20 seconds");
                    final Timer timer = new Timer();

                    timer.schedule(new TimerTask() {
                        int i = 0;
                        int n = 0;
                        int signal;

                        public void run() {
                            n++;
                            Log.wtf("TIMER_3G", "getting level...");
                            signal = manager.getSignalLevel();
                            Log.wtf("TIMER_3G","Current: " + signal);
                            // Check if we get a good enough signal
                            if (signal > 2) {
                                i++;
                                Log.wtf("TIMER_3G", "Got better signal strength! i= " + i);
                            } else { // reset counter
                                Log.wtf("TIMER_3G", "Not good yet");
                                i = 0;
                            }
                            // 2.5 seconds of better 3G or limit has passed
                            if (i == 5 || n == (2 * limit)) {
                                Log.wtf("TIMER_3G", "Done... Notifying listener");

                                synchronized (lock) {
                                    gettingBetterSignal = false;
                                    lock.notify();
                                }
                                timer.cancel();
                            }

                        }
                    },0,500);
                }

            }.execute();
        }
    }

    private void sendFile(String fileJson) throws IOException {
        JsonObject object = (JsonObject) new JsonParser().parse(fileJson);
        JsonObject dataArray = (JsonObject) object.get("data").getAsJsonArray().get(0);
        String fileName = dataArray.get("filename").getAsString();
        byte[] byteArray;

        switch (fileName) {
            case "small":
                byteArray = loadFile(R.raw.small_file);
                break;
            case "medium":
                byteArray = loadFile(R.raw.medium_file);
                break;
            case "large":
                byteArray = loadFile(R.raw.large_file);
                break;
            default:
                byteArray = null;
                Log.wtf("SEND_FILE", "WRONG FILE TYPE");
                break;
        }
//        System.out.println("FileSize before sending: " + byteArray.length);
        try {

            fOut.write(byteArray, 0, byteArray.length);
            fOut.write("\ndone".getBytes());
            fOut.flush();
            Log.wtf("FILE_TRANSFER","SENT!");

            String fileResponse;
            fileResponse = in.readLine();
            Log.wtf("FILE_TRANSFER", "Response: " + fileResponse);
        } catch (IOException e) {
            e.printStackTrace();
            // Stäng om den failar
            fOut.close();
            Log.wtf("FILE_TRANSFER", "Failed, closing stream");
        }
    }

    public byte[] loadFile(int resourceId) throws IOException {

        InputStream iS = getApplicationContext().getResources().openRawResource(resourceId);

        //create a buffer that has the same size as the InputStream
        byte[] byteArray = new byte[iS.available()];

        BufferedInputStream bIS = new BufferedInputStream(iS);
        //read the text file as a stream, into the buffer
        bIS.read(byteArray, 0, byteArray.length);
        Log.wtf("LOAD_FILE", "byteArray: " + byteArray.length);
        bIS.close();

        return byteArray;
    }

    /**
     * Checks a Json string if it is a file transfer
     *
     * @param message Json String to check
     * @return true if its a file request
     */
    private boolean isFileTransfer(String message) {
        JsonObject object = (JsonObject) new JsonParser().parse(message);
//        System.out.println("IS FILE TRANSFER: " +
//                object.get("activity").getAsString().equals("file") + "\n" + object);
        return object.get("activity").getAsString().equals("file");
    }

    public void setSuccess() {
        try {
            fromServer = (JsonObject) parser.parse(response);
            success = fromServer.get("succeeded").getAsBoolean();
            if (!success) {
                System.err.println("server didn't succeed for some reason");
            }
        } catch (Exception e) {
            Log.wtf("SET_SUCCESS", "Tried setting success...");
        }
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

    private void setActive() {
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(response);
        boolean checkActive = fromServer.get("active").getAsBoolean();
//        prefs.edit().putBoolean("ACTIVE", checkActive).apply();
        if(!checkActive) {
            Log.wtf("SET_ACTIVE", "restarting...");
            Intent i = new Intent();
            i.putExtra("RESTART",true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        } else {
        }
    }
}