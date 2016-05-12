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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private String response;
    boolean active, success;

    PhoneStatus manager;
    long deadline, lastD;

    protected Intent mIntent;
    private HashMap sendQueue;

    private AsyncTask asyncTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;

        Log.e("OnStartCommand:", "Service started");
        if (intent != null) {
            String newMessage = intent.getStringExtra("MESSAGE");
            Log.e("OnStartCommand", "Single message:" + newMessage);
            if (newMessage != null) {

                long time = System.nanoTime() / 1000000;
                deadline = time + 5000;
                sendQueue.put(time,newMessage);
                Log.e("ONSTART", "deadline: " + deadline);
                Log.e("ONSTART", "lastdeadline" + lastD);
//                if (asyncTask.)
                send();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.e("OnCreate:", "Service Created");
        sendQueue = new HashMap();
        parser = new JsonParser();
        manager = new PhoneStatus(this);
        lastD = 0;
        Toast.makeText(getApplicationContext(), "Connection service started", Toast.LENGTH_LONG).show();
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
            sendQueue.putAll((Map) s);


        }

        String newMessage = intent.getStringExtra("MESSAGE");
        Log.e("OnHandleIntent", "Single message:" + newMessage);
        if (newMessage != null) {
            long time = System.nanoTime() / 1000000;
            sendQueue.put(time, newMessage);
            send();
        }


        return null;
    }

    private void send() {
        asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                int bufferSize = getBufferSize();
                Log.wtf("BUFFER","Total buffer size: " + bufferSize);


//                adapt();

                while (!sendQueue.isEmpty()) {

                    Log.wtf("SEND","starting scheduling...");
                    Log.e("SEND", "deadline: " + deadline);
                    Log.e("SEND", "lastdeadline" + lastD);
                    long toSend = schedule(lastD);

                    try {
                        // Connect to primary server
                        socket = new Socket(SERVERADRESS, SERVERPORT);

                    } catch (IOException e) {
                        // Print error and try connect to backup server
                        System.err.println("Cannot establish connection to " +
                                SERVERADRESS + ":" + SERVERPORT);
                        System.err.println("Trying to connect to backup server on " +
                                SERVERADRESS_BACKUP + ":" + SERVERPORT_BACKUP);
                        try {
                            socket = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
                        } catch (IOException e1) {
//                            e1.printStackTrace();
                            System.err.println("Cannot establish connection to any server :(");
                        }
                    }


                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        fOut = socket.getOutputStream();
                        out = new PrintWriter(fOut);


                        String mResponse;
                        //SEND AND RECEIVE
                        while (!sendQueue.isEmpty()) {
                            success = false;
                            //////
                            String messageToSend = (String) sendQueue.get(toSend);
                            /////

                            System.out.println("sending " + messageToSend);

                            out.println(messageToSend);
                            out.flush();

                            sendQueue.remove(toSend);
                            mResponse = in.readLine();


                            while (mResponse != null) {
                                System.out.println("RESPONSE: " + mResponse);
                                response = mResponse;
                                setActive();
                                setSuccess();
                                setData();
                                mResponse = null;
                            }

                            if (isFileTransfer(messageToSend)) {
                                sendFile(messageToSend);
                            }

                        }

                        out.println("DONE");
                        out.flush();
                        in.close();
                        out.close();
                        socket.close();
                        System.out.println("Connection closed...");
                        lastD = deadline;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

        }.execute();
    }


    private void adapt(){
        float battLvl = manager.getBatteryLevel();
        String rat = manager.getConnectionType();
        int signalStrength = manager.getSignalLevel(); // 0 - 4

        // Three different main 'states'
        if(battLvl >= 60){
            switch (rat){
                case "WIFI":
                    switch (signalStrength){
                    }
                    // adapt the shit out of the connection
                    break;
                case "MOBILE":
                    //
                    break;

            }
        } else if (battLvl > 20 && battLvl < 60){

        } else if (battLvl <= 20){

        }
    }

    private int getBufferSize(){
        Iterator i = sendQueue.entrySet().iterator();
        int totalSize = 0;
        int n = 0;
        while(i.hasNext()){
            n++;
            Map.Entry pair = (Map.Entry) i.next();
            String req = (String) pair.getValue();
            byte[] size = req.getBytes();
            if(isFileTransfer(req)){
                JsonObject object = (JsonObject) new JsonParser().parse(req);
                JsonObject o = (JsonObject) object.get("data").getAsJsonArray().get(0);
                totalSize += o.get("filesize").getAsInt();
            }
            Log.wtf("BUFFER","current req: " + req + " with size: "+ size.length);
            totalSize += size.length;
        }
        Log.wtf("BUFFER", "Number of requests: " + n);
        return totalSize;
    }

    private long schedule(long lastD){
        boolean readyToSend = false;
        long arrivalTime;
        Iterator i = sendQueue.entrySet().iterator();
        Map.Entry pair = (Map.Entry) i.next();
        double x = 0.62;
        double T = 4000.0;

//        if(manager.getConnectionType().equals("MOBILE")){
            arrivalTime = (long) pair.getKey();                             Log.wtf("SCHEDULING","arrivalTime: " + arrivalTime);
            String req = (String) pair.getValue();

            while(!readyToSend){                                  // Log.wtf("SCHEDULING","stuck in while loop");
                long currentTime = System.nanoTime() / 1000000;             Log.wtf("SCHEDULING","currentTime: " + currentTime);
                if((lastD + x*T) < arrivalTime){                            Log.wtf("SCHEDULING","Next request came close to last sent message");
                    readyToSend = true;
                } else {
                    // Add the request to the queue
                }
                // Checks deadline time (5s)
                if(currentTime >= deadline){
                    Log.wtf("SCHEDULING","deadline reached");
                    readyToSend = true;
                }
            }

//        } else {
//            // WiFi
//
//        }
        return arrivalTime;

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
                System.err.println("WRONG FILE TYPE");
                break;
        }
//        System.out.println("FileSize before sending: " + byteArray.length);
        try {

            fOut.write(byteArray, 0, byteArray.length);
            fOut.write("\ndone".getBytes());
            fOut.flush();
            System.out.println("SENT!");

            String fileResponse;
            fileResponse = in.readLine();
            System.out.println("FileResponse: " + fileResponse);
        } catch (IOException e) {
            e.printStackTrace();
            // Stäng om den failar
            fOut.close();
            System.out.println("STÄNGDE BUFFER");
        }
    }

    public byte[] loadFile(int resourceId) throws IOException {

        InputStream iS = getApplicationContext().getResources().openRawResource(resourceId);

        //create a buffer that has the same size as the InputStream
        byte[] byteArray = new byte[iS.available()];

        BufferedInputStream bIS = new BufferedInputStream(iS);
        //read the text file as a stream, into the buffer
        bIS.read(byteArray, 0, byteArray.length);
        System.out.println("byteArray: " + byteArray.length);
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
            System.err.println("Tried setting success...");
        }
    }

    private void setData() {
        try {
            mIntent.putExtra("SERVER_REPLY", response);
            fromServer = (JsonObject) parser.parse(response);
            data = (JsonArray) fromServer.get("data");
        } catch (Exception e) {
            System.err.println("Failed to set data...");

        }
    }

    private void setActive() {
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(response);
        boolean checkActive = fromServer.get("active").getAsBoolean();
//        prefs.edit().putBoolean("ACTIVE", checkActive).apply();
        if(!checkActive) {
            System.out.println("restarting...");
            Intent i = new Intent();
            i.putExtra("RESTART",true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        } else {
        }
    }
}