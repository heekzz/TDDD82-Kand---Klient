package com.example.adrian.klient.testSimulator;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.example.adrian.klient.R;
import com.example.adrian.klient.ServerConnection.Connection;
import com.example.adrian.klient.ServerConnection.FConnection;
import com.example.adrian.klient.ServerConnection.Request;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
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
    FConnection fileConnection;

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
            deleteSmall.clear();
        }
        if(!deleteMedium.isEmpty()) {
            Request deleteRequest = new Request(context, "delete", deleteMedium).mapRequest();
            connection = new Connection(deleteRequest, context);
            Thread t = new Thread(connection);
            t.start();
            deleteMedium.clear();
        }
        if(!deleteLarge.isEmpty()) {
            Request deleteRequest = new Request(context, "delete", deleteLarge).mapRequest();
            connection = new Connection(deleteRequest, context);
            Thread t = new Thread(connection);
            t.start();
            deleteLarge.clear();
        }

    }

    public void sendSmall(){
        Request fileRequest = new Request(context,"add").fileRequest();
        connection = new Connection(fileRequest,context);
        new Thread(connection).start();
        //Get response from server
        String jsonString;
        do{
            jsonString = connection.getJson();
        } while(jsonString == null);
        System.out.println("jsonString: " + jsonString);

        //Get permission level
        JsonParser parser = new JsonParser();
        JsonObject object = (JsonObject) parser.parse(jsonString);
        boolean access = object.get("access").getAsBoolean();

        if(access){

            try {
                fileConnection = new FConnection(loadFile(R.raw.ordlista), context);
                Thread t = new Thread(fileConnection);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendMedium(){
        try {
            fileConnection = new FConnection(loadFile(R.raw.medium_file), context);
            Thread t = new Thread(fileConnection);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void sendLarge(){
        try {
            fileConnection = new FConnection(loadFile(R.raw.large_file), context);
            Thread t = new Thread(fileConnection);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
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


}
