package com.example.adrian.klient.ServerConnection;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Fredrik on 16-03-05.
 */
public class Connection implements Runnable {
    private Context context;
    private String json;
    private String message;
    private boolean active;

    public Connection(Request request, Context context) {
        this.context = context;
        message = request.message;
        active = false;
    }

    @Override
    public void run() {

        String SERVERADRESS = "2016-4.itkand.ida.liu.se";
        String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
        int SERVERPORT = 9001;
        int SERVERPORT_BACKUP = 9001;
        BufferedReader in = null;
        PrintWriter out = null;

        /**
         * Connect to primary server, if it fails, connect to backup server
         */
        Socket s = null;
        try {
            // Connect to primary server
//            s = new Client(context).getConnection(SERVERADRESS, SERVERPORT);
            s = new Socket(SERVERADRESS, SERVERPORT);
            in = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream()));
        } catch (IOException e) {
            // Print error and try connect to backup server
            System.err.println("Cannot establish connection to " +
                    SERVERADRESS + ":" + SERVERPORT);
            System.err.println("Trying to connect to backup server on " + SERVERADRESS_BACKUP +
                    ":" + SERVERPORT_BACKUP);
            try {
//                s = new Client(context).getConnection(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
                // Connect to Backup Server
                Socket socket = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e1) {
                e1.printStackTrace();
                System.err.println("Cannot establish connection to any server :(");
            }
        }

        // Create a thread to write to
        System.out.println("MSG: " + message);
        Sender sender = new Sender(out);
        sender.setMessage(message);
        sender.setDaemon(true);
        sender.start();
        try {
            // Read messages from the server and print them
            String msg;
            while ((msg = in.readLine()) != null) {
                setActive(msg);
                setJson(msg);
            }
            if (!isActive()) {
                s.close();
                // Restart application if session isn't active
                new AppRestart().doRestart();
            }
        } catch (IOException ioe) {
            System.err.println("Connection to server broken.");
            ioe.printStackTrace();
        }
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public void setActive(String json) {
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(json);
        active = fromServer.get("active").getAsBoolean();
    }

    public boolean isActive() {
        return this.active;
    }
}


