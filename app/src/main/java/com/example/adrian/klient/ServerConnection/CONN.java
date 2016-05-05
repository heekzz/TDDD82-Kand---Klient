package com.example.adrian.klient.ServerConnection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.example.adrian.klient.R;
import com.example.adrian.klient.testSimulator.Simulator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;
import java.util.Stack;

/**
 * Created by dennisdufback on 16-04-26.
 */
public class CONN implements Runnable{
    Context context;
    boolean active, success;

    String SERVERADRESS = "2016-4.itkand.ida.liu.se";
    String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
    int SERVERPORT = 9000;
    int SERVERPORT_BACKUP = 9000;

    BufferedReader in = null;
    OutputStream fOut = null;
    PrintWriter out = null;
    Socket socket = null;
    String response;

    SharedPreferences sendPrefs;
    String SEND_PREFS = "SEND_PREFS";
    SharedPreferences prefs;
    String PREFS = "PREFS";
    Stack<String> toSend;
    JsonArray data;
    Set<String> getToSend;
    JsonParser parser;
    JsonObject fromServer;


    public CONN(Context context) {
        this.context = context.getApplicationContext();
        sendPrefs = context.getSharedPreferences(SEND_PREFS, Context.MODE_PRIVATE);
        getToSend = sendPrefs.getStringSet("TO_SEND", null);
        toSend = new Stack<>();

        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        active = prefs.getBoolean("ACTIVE", true);

        success = true;
        parser = new JsonParser();
    }


    @Override
    public void run() {
        try {
            // Connect to primary server
            socket = new Socket(SERVERADRESS, SERVERPORT);

        } catch (IOException e) {
            // Print error and try connect to backup server
            System.err.println("Cannot establish connection to " +
                    SERVERADRESS + ":" + SERVERPORT);
            System.err.println("Trying to connect to backup server on " + SERVERADRESS_BACKUP +
                    ":" + SERVERPORT_BACKUP);
            try {
                socket = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
            } catch (IOException e1) {
                e1.printStackTrace();
                System.err.println("Cannot establish connection to any server :(");
            }
        }


        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            fOut = socket.getOutputStream();
            out = new PrintWriter(fOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String mResponse;
            for(String s : getToSend){
                toSend.add(s);
            }
            //SEND AND RECEIVE
            while(!toSend.isEmpty()){
                success = false;
                String s = toSend.pop();

                System.out.println("sending " + s);

                out.println(s);
                out.flush();
                if (isFileTransfer(s)) {
                    sendFile(s);
                }

                mResponse = in.readLine();
                while (mResponse != null){
                    System.out.println("RESPONSE: " + mResponse);
                    response = mResponse;
                    setActive();
                    setSuccess();
                    setData();
                    mResponse = null;
                }
            }

            out.println("DONE");
            out.flush();

            System.out.println("Clearing sendQueue...");
            sendPrefs.edit().clear().apply();
            in.close();
            out.close();
            socket.close();
            System.out.println("Connection closed...");

        }catch (Exception e){
            System.err.println("Nothing to send");
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
                System.err.println("WRONG FILE TYPE");
                break;
        }
        System.out.println("FileSize before sending: " + byteArray.length);
        try{
            fOut.write(byteArray, 0, byteArray.length);
            fOut.flush();
        } catch(IOException e){
            e.printStackTrace();
            // Stäng om den failar
            try {
                fOut.close();
                System.out.println("STÄNGDE BUFFER");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public byte[] loadFile(int resourceId) throws IOException {

        InputStream iS = context.getResources().openRawResource(resourceId);

        //create a buffer that has the same size as the InputStream
        byte[] byteArray = new byte[iS.available()];

        BufferedInputStream bIS = new BufferedInputStream(iS);
        //read the text file as a stream, into the buffer
        bIS.read(byteArray, 0, byteArray.length);
        System.out.println("byteArray: " + byteArray.length);
        bIS.close();

        return byteArray;
    }

    private boolean isFileTransfer(String message) {
        JsonObject object = (JsonObject) new JsonParser().parse(message);
        System.out.println("IS FILE TRANSFER: " + object.get("activity").getAsString().equals("file")
                +"\n" + object);
        return object.get("activity").getAsString().equals("file");
    }

    private void setData() {
        try {
            fromServer = (JsonObject) parser.parse(response);
            data = (JsonArray) fromServer.get("data");
        }catch (Exception e){
            System.err.println("Failed to set data...");
        }
    }
    public JsonArray getData(){
        return data;
    }
    public String getResponse(){
        return response;
    }

    public void setSuccess(){
        try {
            fromServer = (JsonObject) parser.parse(response);
            success = fromServer.get("succeeded").getAsBoolean();
            if(!success){
                System.err.println("server didn't succeed for some reason");
            }
        } catch (Exception e){
            System.err.println("Tried setting success...");
        }
    }
    public int getPermission(){
        int permission = 0;
        try {
            fromServer = (JsonObject) parser.parse(response);
            permission = fromServer.get("permission").getAsInt();
        } catch (Exception w){
        }
        return permission;
    }
    private void setActive() {
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(response);
        boolean checkActive = fromServer.get("active").getAsBoolean();
        prefs.edit().putBoolean("ACTIVE", checkActive).apply();
        if(checkActive) {
            System.out.println("restarting...");
            Intent i = new Intent();
            i.putExtra("RESTART",true);
            LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        } else {
        }
    }

}

