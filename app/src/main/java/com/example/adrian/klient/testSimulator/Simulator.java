package com.example.adrian.klient.testSimulator;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.example.adrian.klient.R;
import com.example.adrian.klient.ServerConnection.Connection;
import com.example.adrian.klient.ServerConnection.Request;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class Simulator extends AppCompatActivity{
    private ArrayList addList = new ArrayList();
    private ArrayList deleteSmall = new ArrayList();
    private ArrayList deleteMedium = new ArrayList();
    private ArrayList deleteLarge = new ArrayList();

    private Context context;
    double lat = 58.8963790165493;
    double lon = 15.460723824799063;
    String event = "Auto";
    Connection connection;
    FileConnection fileConnection;

    String smallFile,mediumFile,largeFile;
    byte[] toSend;

    public Simulator(Context context) {
        this.context = context;
    }

    public void runSmall() {

        for(int i =0; i < 5; i++) {
            JsonObject toAdd = new JsonObject();
            toAdd.addProperty("lat", lat);
            toAdd.addProperty("lon", lon);
            toAdd.addProperty("event", event);
            addList.add(toAdd);

            lat += 0.01;
        }
        deleteSmall = addList;

        Request addRequest = new Request(context,"add",addList).mapRequest();
        connection = new Connection(addRequest, context);
        Thread t = new Thread(connection);
        t.start();
    }

    public void runMedium() {
        for (int i = 0; i < 50; i++) {
            JsonObject toAdd = new JsonObject();
            toAdd.addProperty("lat", lat);
            toAdd.addProperty("lon", lon);
            toAdd.addProperty("event", event);
            addList.add(toAdd);
            lat += 0.01;
        }
        deleteMedium = addList;
        Request addRequest = new Request(context,"add",addList).mapRequest();
        connection = new Connection(addRequest, context);
        Thread t = new Thread(connection);
        t.start();
    }
    public void runLarge() {

        lon += 0.3;
        for (int i = 0; i < 500; i++) {
            JsonObject toAdd = new JsonObject();
            toAdd.addProperty("lat", lat);
            toAdd.addProperty("lon", lon);
            toAdd.addProperty("event", event);
            addList.add(toAdd);

            lat += 0.01;
        }
        deleteLarge = addList;

        Request addRequest = new Request(context,"add",addList).mapRequest();
        connection = new Connection(addRequest, context);
        Thread t = new Thread(connection);
        t.start();

    }
    public void delete() {

        if(!deleteSmall.isEmpty()) {
            Request deleteRequest = new Request(context, "delete", deleteSmall).mapRequest();
            connection = new Connection(deleteRequest, context);
            Thread t = new Thread(connection);
            t.start();
        }
        if(!deleteMedium.isEmpty()) {
            Request deleteRequest = new Request(context, "delete", deleteMedium).mapRequest();
            connection = new Connection(deleteRequest, context);
            Thread t = new Thread(connection);
            t.start();
        }
        if(!deleteLarge.isEmpty()) {
            Request deleteRequest = new Request(context, "delete", deleteLarge).mapRequest();
            connection = new Connection(deleteRequest, context);
            Thread t = new Thread(connection);
            t.start();
        }

    }

    public void sendSmall(){
        try {
            toSend = loadFile("small");
            fileConnection = new FileConnection(toSend,context);
            Thread t = new Thread(fileConnection);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendMedium(){
        try {
            toSend = loadFile("medium");
            fileConnection = new FileConnection(toSend,context);
            Thread t = new Thread(fileConnection);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void sendLarge(){
        try {
            toSend = loadFile("large");
            fileConnection = new FileConnection(toSend,context);
            Thread t = new Thread(fileConnection);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] loadFile(String fileSize) throws IOException{
        InputStream iS = null;

        switch (fileSize){
            case "small":
                //5 MB
                iS = getResources().openRawResource(R.raw.small_file);
                break;
            //10 MB
            case "medium":
                iS = getResources().openRawResource(R.raw.medium_file);
                break;
            //20 MB
            case "large":
                iS = getResources().openRawResource(R.raw.large_file);
                break;
        }

        //create a buffer that has the same size as the InputStream
        byte[] buffer = new byte[iS.available()];
        //read the text file as a stream, into the buffer
        iS.read(buffer);
//        //create a output stream to write the buffer into
//        ByteArrayOutputStream oS = new ByteArrayOutputStream();
//        //write this buffer to the output stream
//        oS.write(buffer);
//        //Close the Input and Output streams
//        oS.close();
//        iS.close();
//        return oS.toString();
        return buffer;
    }

}
