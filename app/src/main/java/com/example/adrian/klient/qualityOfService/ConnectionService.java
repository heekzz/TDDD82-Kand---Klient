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
import java.util.ArrayList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Fredrik on 16-05-05.
 */
public class ConnectionService extends Service {
    private String SERVERADRESS = "2016-4.itkand.ida.liu.se";
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
    boolean sending, bufferFull, gettingBetterSignal, readyToSend;
    long currentBuffer;

    protected Intent mIntent;
    private Queue<String> sendQueue;
//    private TreeMap sendQueue;

    private AsyncTask asyncTask;

    @Override
    public void onCreate() {
        Log.e("OnCreate", "_________________");
        Log.e("OnCreate", "Service Created");
//        sendQueue = new TreeMap<>();
        sendQueue = new ConcurrentLinkedQueue<>();

        init = (System.nanoTime()/1000000);

        parser = new JsonParser();
        manager = new PhoneStatus(this);
        lastDeadline = 0;
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
//                deadline = arrivalTime + 7000;
//                Log.wtf("ONSTART", "request arrived at: " + arrivalTime);
//                Log.wtf("ONSTART", "last deadline was at: " + lastDeadline);

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
        mIntent = intent;

        Log.e("OnBind:", "Service started");

        ArrayList<String> s = intent.getStringArrayListExtra("MESSAGE_LIST");
        Log.e("OnBind", "ArrayList:" + s);
        if (s != null) {
//            sendQueue.putAll((Map) s);
        }

        String newMessage = intent.getStringExtra("MESSAGE");
        Log.e("OnHandleIntent", "Single message:" + newMessage);
        if (newMessage != null) {
            startAdapting();
//            send();
        }


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
                        Log.wtf("SEND","Cannot establish connection to " +
                                SERVERADRESS + ":" + SERVERPORT);
                        Log.wtf("SEND", "Trying to connect to backup server on " +
                                SERVERADRESS_BACKUP + ":" + SERVERPORT_BACKUP);
                        try {
                            socket = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
                        } catch (IOException e1) {
//                            e1.printStackTrace();
                            Log.wtf("SEND","Cannot establish connection to any server :(");
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
//                            Iterator i = sendQueue.entrySet().iterator();
//                            Map.Entry pair = (Map.Entry) i.next();
//                            request = (String) pair.getValue();
//                            long key = (long) pair.getKey();

//                            Log.wtf("SEND", "polled request from queue");
                            request = sendQueue.poll();

//                            Log.wtf("SEND", "sending " + request);

                            out.println(request);
                            out.flush();

                            Log.wtf("SEND", "sent at timestamp " + ((System.nanoTime()/1000000) - init) + "ms");

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

//                            Log.wtf("SEND", "resetting deadline to right NOW!");
                            lastDeadline = System.nanoTime() / 1000000;

                            // Keep looping until we're done
                            if(!sendQueue.isEmpty()){
                                doneSending = false;
                            } else {
                                doneSending = true;
                                sending = false;
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
                        Log.wtf("SEND", "\n");

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

        // HIGH BATTERY. Only bundle if 3G
        if(battLvl >= 60){
            if(rat.equals("WIFI")){
                // Don't bundle, just send
//                sendQueue.put(arrivalTime,request);
                sendQueue.add(request);
                send();
            } else if(rat.equals("MOBILE")){
                // High battery and 3G - Bundle small
                schedule(rat,"small", signalStrength);
            }

            // MEDIUM BATTERY
        } else if (battLvl > 20 && battLvl < 60){
            if(rat.equals("WIFI")){
                // Medium battery and WiFi, bundle small
                schedule(rat,"small", signalStrength);

            } else if(rat.equals("MOBILE")){
                // Medium battery and 3G, bundle medium
                schedule(rat,"medium", signalStrength);
            }

            // LOW BATTERY
        } else if (battLvl <= 20){

            if(rat.equals("WIFI")){
                // Low battery and WiFi, bundle large only if ss = 1,2 else wait
                schedule(rat,"medium", signalStrength);
            } else if(rat.equals("MOBILE")){
                // Low battery and 3G, bundle large and send when buffer full only if ss = 3,4 else wait
                schedule(rat,"large", signalStrength);
            }
        }
    }


    private void updateBuffer(){
//        Iterator i = sendQueue.entrySet().iterator();
//        Iterator i = sendQueue.iterator();
        int totalSize = 0;
//        Log.wtf("BUFFER", "Number of requests: " + sendQueue.size());
        int n = 0;
//        while(i.hasNext()){
        for(String req : sendQueue){
             n++;
            byte[] size = req.getBytes();
            if(isFileTransfer(req)){
                JsonObject object = (JsonObject) new JsonParser().parse(req);
                JsonObject o = (JsonObject) object.get("data").getAsJsonArray().get(0);
                totalSize += o.get("filesize").getAsInt();
            }
//            Log.wtf("BUFFER","req "+n+" har size: "+ size.length);
            totalSize += size.length;
//            Log.wtf("BUFFER","totalt: "+ totalSize);
        }
        currentBuffer = totalSize;
    }

    private void schedule(final String rat, final String lvl, final int signalStrength) {

        Log.wtf("SCHEDULING","Signal strength for current req: " + signalStrength);
        final Object lock = new Object();

        switch (rat) {
            case "WIFI":

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        int BUFF_SIZE = 0;
                        switch (lvl) {
                            case "small":
                                BUFF_SIZE = 40000; // 40kB
                                break;
                            case "medium":
                                BUFF_SIZE = 100000; // 100kB
                                break;
                            case "large":
                                BUFF_SIZE = 200000; // 200kB
                                break;
                        }
                        // Add request to the queue
                        Log.wtf("SCHEDULE WIFI", "adding request to queueue");
//                        sendQueue.put(arrivalTime, request);
                        sendQueue.add(request);

                        while(!bufferFull){
                            // When the buffer is full, send, unless we have bad signal Strength
                            if (currentBuffer >= BUFF_SIZE) {
                                bufferFull = true;
                                Log.wtf("SCHEDULE WIFI", "Buffer is full now..");
                                if(signalStrength == 0){ // Not for 1,2
                                    Log.wtf("SCHEDULE WIFI", "Super bad Signal strength, waiting for better connection");
                                    gettingBetterSignal = true;

                                    waitForBetterConnection();

                                    try {
                                        Log.wtf("SCHEDULING", "waiting on lock");

                                        synchronized (lock){
                                            while(gettingBetterSignal){
                                                lock.wait();
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                                bufferFull = false;
                                send();
                            }
                            updateBuffer();
                        }

                        return null;
                    }

                    private void waitForBetterConnection() {
                        Log.wtf("SCHEDULE WIFI","Waiting for better connection for max 20 seconds");
                        final Timer timer = new Timer();

                        timer.schedule(new TimerTask() {
                            int i = 0;
                            int n = 0;

                            @Override
                            public void run() {
                                n++;
                                Log.wtf("TIMER WIFI","getting level...");
                                if(signalStrength > 0){ // 0 is worst
                                    i++;
                                    Log.wtf("TIMER WIFI","Got better signal strength! i= " + i);
                                } else { // reset counter
                                    Log.wtf("TIMER WIFI","Not good yet");
                                    i = 0;
                                }
                                // 2.5 seconds of better Wifi or 20 s has passed
                                if(i == 5 || n == 40){
                                    if(n==5){
                                        Log.wtf("TIMER WIFI", "20s passed, sending");
                                    }
                                    lock.notify();
                                    timer.cancel();

                                }

                            }
                        },0,500);
                    }

                }.execute();


                break;


            case "MOBILE":


                if(sending){
                    sendQueue.add(request);
                } else {
                    new AsyncTask<Void, Void, Void>() {
                        double a = 0.62;
                        double T = 4000; // High power state for 4 seconds (3G)

                        @Override
                        protected Void doInBackground(Void... params) {
                            int BUFF_SIZE = 0;
                            switch (lvl) {
                                case "small":
                                    BUFF_SIZE = 40000; // 40kB
                                    break;
                                case "medium":
                                    BUFF_SIZE = 100000; // 100kB
                                    break;
                                case "large":
                                    BUFF_SIZE = 200000; // 200kB
                                    break;
                            }

                            // This decides the deadline timer
                            int limit = 0;
                            switch (signalStrength) { // 0-4 for 3G
                                case 0:
                                    deadline = arrivalTime + 20000; // 20s
                                    Log.wtf("SCHEDULING", "Giving request a deadline of 20s");
                                    limit = 20;
                                    break;
                                case 1:
                                    deadline = arrivalTime + 15000; // 15s
                                    Log.wtf("SCHEDULING", "Giving request a deadline of 15s");
                                    limit = 15;
                                    break;
                                case 2:
                                    deadline = arrivalTime + 9000; // 9s
                                    Log.wtf("SCHEDULING", "Giving request a deadline of 9s");
                                    limit = 10;
                                    break;
                                case 3:
                                    deadline = arrivalTime + 7000; // 7s
                                    Log.wtf("SCHEDULING", "Giving request a deadline of 7s");
                                    break;
                                case 4:
                                    deadline = arrivalTime + 5000; // 5s
                                    Log.wtf("SCHEDULING", "Giving request a deadline of 5s");
                                    break;
                            }

                            // Keep sending if we request while in high power state
                            Log.wtf("SCHEDULING", "diff: " + ((lastDeadline + (a * T)) - arrivalTime) + "ms");
                            if (arrivalTime < (lastDeadline + (a * T))) {
                                readyToSend = true;
//                                    sendQueue.put(arrivalTime, request);
                                sendQueue.add(request);
                                Log.wtf("SCHEDULING", "close incoming request, sending");
                                send();
                            } else {
                                // Request needs to be put in queue
                                // Add the request to the queue and wait for its deadline
                                sendQueue.add(request);
                                readyToSend = false;
                                sending = true;
                                Log.wtf("SCHEDULING", "putting request in queue, not sending.");

                            }
                            // A request is in the queue
                            long currentTime;
                            while (!readyToSend) {
                                // Checks deadline time
                                currentTime = System.nanoTime() / 1000000;
                                if (currentTime >= deadline || currentBuffer >= BUFF_SIZE) {
                                    if (currentTime >= deadline) {
                                        Log.wtf("SCHEDULING", "deadline was reached");
                                        if (signalStrength <= 2) {
                                            Log.wtf("SCHEDULING", "Signal strength is bad, waiting for better connection");

                                            gettingBetterSignal = true;

                                            waitForBetterConnection(limit);

                                            try {
                                                Log.wtf("SCHEDULING", "waiting on lock");

                                                synchronized (lock){
                                                    while(gettingBetterSignal){
                                                        lock.wait();
                                                    }
                                                }
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            send();
                                        }
                                    } else {
                                        Log.wtf("SCHEDULING", "buffer is full, sending");
                                        Log.wtf("SCHEDULING", (deadline - currentTime) + "s left on deadline timer");
                                        send();
                                    }
                                    Log.wtf("SCHEDULING", "setting stupid variables");
                                    readyToSend = true;
                                }
                                updateBuffer();

                            }
                            return null;
                        }

                        private void waitForBetterConnection(final int limit) {
                            Log.wtf("SCHEDULE 3G", "Waiting for better connection for max 20 seconds");
                            final Timer timer = new Timer();

                            timer.schedule(new TimerTask() {
                                int i = 0;
                                int n = 0;
                                int signal;
                                int prevSignal = signalStrength;


                                public void run() {
                                    n++;
                                    Log.wtf("TIMER 3G", "getting level...");
                                    signal = manager.getSignalLevel();
                                    // Check if we get a _better_ signal
//                                    if (signal > prevSignal) {
                                    if (signal > 2) {
                                        i++;
                                        Log.wtf("TIMER 3G", "Got better signal strength! i= " + i);
                                    } else { // reset counter
                                        Log.wtf("TIMER 3G", "Not good yet");
                                        i = 0;
                                    }
                                    // 2.5 seconds of better 3G or limit has passed
                                    if (i == 5 || n == (2 * limit)) {
                                        Log.wtf("TIMER 3G", "Done... Notifying listener");

                                        synchronized (lock){
                                            gettingBetterSignal = false;
                                            lock.notify();
                                            send();
                                        }
                                        timer.cancel();
                                    }

                                }
                            },0,500);
                        }

                    }.execute();
                }

                break;
            default:
                Log.wtf("SCHEDULE", "NO INTERNET CONNECTION DUMMY");

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