package com.example.adrian.klient.ServerConnection;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by dennisdufback on 2016-03-14.
 */

public class Request {
    public String action;
    public String message;
    public String[] args;
    SharedPreferences preferences;
    public String PREFS = "USER_INFO";
    public String id;
    private List<JsonObject> argList;

    public Request(Context context,String action, String... args){

        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.action = action;
        this.args = args;
    }
    public Request(Context context,String action, List argList){

        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.action = action;
        this.argList = argList;
    }



    public Request NFCRequest(){
        JsonObject request = new JsonObject();

        request.addProperty("activity","nfc");
        request.addProperty("action",action);
        request.addProperty("id",0);


        // Array object for data params
        JsonObject params = new JsonObject();
        JsonArray data = new JsonArray();

        //args[0] contains the NFCid
        params.addProperty("NFCid",args[0]);
        data.add(params);
        request.add("data", data);
        message = request.toString();
        return this;
    }

    public Request contactRequest() {
        id = preferences.getString("USER_ID", "1234abcd");
        JsonObject request = new JsonObject();

        request.addProperty("activity", "contact");
        request.addProperty("action", action);
        request.addProperty("id",id);

        // Array object for data params
        JsonObject params = new JsonObject();
        switch (action){
            case "get":
                break;
            case "delete":
                params.addProperty("deletekey",args[0]);
                break;
            case "add":
                break;
        }
        JsonArray data = new JsonArray();
        data.add(params);
        request.add("data", data);
        message = request.toString();
        return this;
    }

    public Request mapRequest() {
        // Anon's ID
        id = preferences.getString("USER_ID", "1234abcd");

        JsonObject request = new JsonObject();
        request.addProperty("activity","map");
        request.addProperty("action",action);
        request.addProperty("id", id);

        JsonArray data = new JsonArray();
        switch (action){
            case "get":
                break;
            default:
                for(JsonObject o : argList){
                    data.add(o);
                }
                break;
        }
        request.add("data", data);

        message = request.toString();
        return this;
    }

    public Request passRequest () {
        JsonObject request = new JsonObject();

        int nfc_id = preferences.getInt("NFC_ID",1337);

        request.addProperty("activity","nfc");
        request.addProperty("action",action);

        // Array object for data params
        JsonObject params = new JsonObject();
        JsonArray data = new JsonArray();

        //args[0] contains the NFCid
        params.addProperty("NFCid", nfc_id);
        params.addProperty("pass", args[0]);
        data.add(params);
        request.add("data", data);
        message = request.toString();
        return this;
    }

}
