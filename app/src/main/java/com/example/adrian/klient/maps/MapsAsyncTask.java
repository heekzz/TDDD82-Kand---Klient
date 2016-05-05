package com.example.adrian.klient.maps;

import android.content.Context;
import android.os.AsyncTask;

import com.example.adrian.klient.ServerConnection.CONN;
import com.example.adrian.klient.ServerConnection.Request;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Dotson on 2016-04-04.
 */
public class MapsAsyncTask extends AsyncTask<Void, Void, String> {

    CONN connection;
    Context context;
    private GoogleMap mMap;
    private ArrayList markerList;
    public MapsActivity mapsActivity;
    private long startTime, duration;
    private JsonArray data;

    private ArrayList deleteList, addList;

    public MapsAsyncTask(Context context, MapsActivity mapsActivity, GoogleMap mMap, ArrayList markerList) {
        this.context = context;
        this.mMap = mMap;
        this.markerList = markerList;
        this.mapsActivity = mapsActivity;
    }

    @Override
    protected String doInBackground(Void... params) {

        Iterator iterator = markerList.iterator();
        deleteList = new ArrayList();
        addList = new ArrayList();

        // Iterates over the local markers
        while (iterator.hasNext()) {
            String next = iterator.next().toString();
            String update[] = next.split(":");

            // If a marker has an "add" or "delete" tag (i.e. not "local"), then update the server with that info
            if (!update[1].equals("local")) {
                String parts[] = next.split(";");
                String lat = parts[0];
                String lon = parts[1];

                switch (update[1]) {

                    // add to deleteList
                    case "delete":
                        JsonObject toDelete = new JsonObject();
                        toDelete.addProperty("lat", lat);
                        toDelete.addProperty("lon", lon);
                        deleteList.add(toDelete);
                        break;

                    // add to addList
                    case "add":
                        String event = parts[2];
                        if (!event.isEmpty()) {
                            JsonObject toAdd = new JsonObject();
                            toAdd.addProperty("lat", lat);
                            toAdd.addProperty("lon", lon);
                            toAdd.addProperty("event", event);
                            addList.add(toAdd);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
        startTime = System.currentTimeMillis();

        // Don't do this if there's nothing to add
        if (!addList.isEmpty()) {
            new Request(context, "add", addList).mapRequest();
        }
        // Don't do this if there's nothing to delete
        if (!deleteList.isEmpty()) {
            new Request(context, "delete", deleteList).mapRequest();
        }
        // Always get the markers
        new Request(context,"get").mapRequest();
        connection = new CONN(context);
        new Thread(connection).start();

        do {
            data = connection.getData();
//            System.out.printf("waiting for response..");
        } while (data == null);

    return null;
}

    protected void onPostExecute(String result) {

        // Toasts the time it took
        duration = System.currentTimeMillis() - startTime;
        mapsActivity.makeToast(duration);

        // When all communication between client and server is complete, update the locally saved
        // markers with the downloaded ones.
        markerList.clear();
        mMap.clear();

        for (JsonElement e : data) {
            JsonObject o = e.getAsJsonObject();
            double lat = o.get("lat").getAsDouble();
            double lon = o.get("lon").getAsDouble();
            String event = o.get("event").getAsString();
            markerList.add(String.valueOf(lat) + ";" + String.valueOf(lon) + ";" + event + ";:local");
            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Pågående ärende:").snippet(event));
        }

        mapsActivity.saveMarkers();
    }
}
