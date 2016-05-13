package com.example.adrian.klient.ServerConnection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.adrian.klient.qualityOfService.ConnectionService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dennisdufback on 2016-03-14.
 */

public class Request {
    String action, id, message;
    String[] args;
    List<JsonObject> argList;
    JsonObject request, data;
    JsonArray dataArray;
    SharedPreferences userPrefs;
    SharedPreferences sendPrefs;
    String USER_PREFS = "USER_INFO";
    String SEND_PREFS = "SEND_PREFS";
    SharedPreferences.Editor editor;
    Set<String> toSend;
    Context context;

    public Request(Context context,String action, String... args){
        this.context = context;
        this.action = action;
        this.args = args;

        request = new JsonObject();
        dataArray = new JsonArray();
        data = new JsonObject();

        userPrefs = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        id = userPrefs.getString("USER_ID", "1234abcd");
        sendPrefs = context.getSharedPreferences(SEND_PREFS, Context.MODE_PRIVATE);
        toSend = sendPrefs.getStringSet("TO_SEND", new HashSet<String>());
    }
    public Request(Context context,String action, List<JsonObject> argList){
        this.context = context;
        this.action = action;
        this.argList = argList;

        request = new JsonObject();
        dataArray = new JsonArray();
        data = new JsonObject();

        userPrefs = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        id = userPrefs.getString("USER_ID", "1234abcd");
        sendPrefs = context.getSharedPreferences(SEND_PREFS, Context.MODE_PRIVATE);
        toSend = sendPrefs.getStringSet("TO_SEND", new HashSet<String>());
    }



    public Request NFCRequest(){

        request.addProperty("activity","nfc");
        request.addProperty("action", action);
        request.addProperty("sessionid", id);

        //args[0] contains the NFCid
        data.addProperty("NFCid", args[0]);
        dataArray.add(data);
        request.add("data", dataArray);
//        message = request.toString();
        toSend.add(request.toString());
        editor = sendPrefs.edit();
        editor.putStringSet("TO_SEND", toSend);
        editor.apply();

        return this;
    }

    public Request contactRequest() {

        request.addProperty("activity", "contact");
        request.addProperty("action", action);
        request.addProperty("sessionid", id);

        switch (action){
            case "get":
                break;
            case "delete":
                data.addProperty("deletekey", args[0]);
                break;
            case "add":
                break;
        }
        dataArray.add(data);
        request.add("data", dataArray);
        Intent intent = new Intent(context, ConnectionService.class);
        intent.putExtra("MESSAGE", request.toString());
        context.startService(intent);
//        message = request.toString();
//        toSend.add(request.toString());
//        editor = sendPrefs.edit();
//        editor.putStringSet("TO_SEND", toSend);
//        editor.apply();

        return this;
    }

    public Request mapRequest() {

        request.addProperty("activity","map");
        request.addProperty("action", action);
        request.addProperty("sessionid", id);

        switch (action){
            case "get":
                break;
            default:
                for(JsonObject o : argList){
                    dataArray.add(o);
                }
                break;
        }
        request.add("data", dataArray);
//        message = request.toString();

        Log.e("Request/MapRequest", "Request to be sent: " + request);
        /**
         * TODO: Background service test -  work in progress
         */
        Intent intent = new Intent(context, ConnectionService.class);

        intent.putExtra("MESSAGE", request.toString());
        context.startService(intent);
        /**
         *
         */

//        toSend.add(request.toString());
//        editor = sendPrefs.edit();
//        editor.putStringSet("TO_SEND", toSend);
//        editor.apply();

        return this;
    }

    public Request passRequest () {

        int nfc_id = userPrefs.getInt("NFC_ID",1337);

        request.addProperty("activity","pass");
        request.addProperty("action",action);
        request.addProperty("sessionid", id);

        //args[0] contains the NFCid
        data.addProperty("NFCid", nfc_id);
        data.addProperty("pass", args[0]);
        dataArray.add(data);

        request.add("data", dataArray);
//        message = request.toString();
        toSend.add(request.toString());

        editor = sendPrefs.edit();
        editor.putStringSet("TO_SEND", toSend);
        editor.apply();

        return this;
    }

    public Request fileRequest(){
        request.addProperty("activity","file");
        request.addProperty("action",action);
        request.addProperty("sessionid", id);
        data.addProperty("filename",args[0]);
        data.addProperty("filesize", args[1]);

        dataArray.add(data);


        request.add("data", dataArray);

        Intent intent = new Intent(context, ConnectionService.class);
        intent.putExtra("MESSAGE", request.toString());
        context.startService(intent);


        return this;
    }
}
