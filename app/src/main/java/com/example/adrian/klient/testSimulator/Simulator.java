package com.example.adrian.klient.testSimulator;

import android.content.Context;
import android.util.Log;

import com.example.adrian.klient.R;
import com.example.adrian.klient.ServerConnection.Connection;
import com.example.adrian.klient.ServerConnection.FConnection;
import com.example.adrian.klient.ServerConnection.Request;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class Simulator {
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

        for(int i =0; i < 1; i++) {
            JsonObject toAdd = new JsonObject();
            toAdd.addProperty("lat", lat);
            toAdd.addProperty("lon", lon);
            toAdd.addProperty("event", event);
            addList.add(toAdd);

            lat += 0.01;
        }
        deleteSmall.addAll(addList);
        new Request(context,"add",addList).mapRequest();
        addList.clear();
//        new Thread(new CONN(context)).start();
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
        deleteMedium.addAll(addList);
        new Request(context,"add",addList).mapRequest();
        addList.clear();
//        new Thread(new CONN(context)).start();
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
        deleteLarge.addAll(addList);
        new Request(context,"add",addList).mapRequest();
        addList.clear();
//        new Thread(new CONN(context)).start();
    }
    public void delete() {
        System.out.println("DELETE: Size of small: " + deleteSmall.size());
        if(!deleteSmall.isEmpty()) {
            System.out.println("DELETE SMALL!!");
            new Request(context, "delete", deleteSmall).mapRequest();
            deleteSmall.clear();
        }
        if(!deleteMedium.isEmpty()) {
            new Request(context, "delete", deleteMedium).mapRequest();
            deleteMedium.clear();
        }
        if(!deleteLarge.isEmpty()) {
            new Request(context, "delete", deleteLarge).mapRequest();
            deleteLarge.clear();
        }
    }

    public void sendSmall(){
        try {
            new Request(context,"add","small",""+loadFile(R.raw.small_file).length).fileRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendMedium(){
        try {
            new Request(context,"add","medium",""+loadFile(R.raw.medium_file).length).fileRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendLarge(){
        try {
            new Request(context,"add","large",""+loadFile(R.raw.large_file).length).fileRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendOne() {
        JsonObject toAdd = new JsonObject();
        toAdd.addProperty("lat", lat);
        toAdd.addProperty("lon", lon);
        toAdd.addProperty("event", event);
        addList.add(toAdd);
        lat += 0.01;

        deleteSmall = addList;
        Log.e("Simulator", "AddList for SendOne: " + addList);
        new Request(context, "add",addList).mapRequest();

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

