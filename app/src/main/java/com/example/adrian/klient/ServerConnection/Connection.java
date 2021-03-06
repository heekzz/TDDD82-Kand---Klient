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
    private String message, response;
    private boolean active, success;

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
        Socket s = null;
        BufferedReader in = null;
        PrintWriter out = null;

        /**
         * Connect to primary server, if it fails, connect to backup server
         */
        try {
            // Connect to primary server
            s = new Socket(SERVERADRESS, SERVERPORT);

        } catch (IOException e) {
            // Print error and try connect to backup server
            System.err.println("Cannot establish connection to " +
                    SERVERADRESS + ":" + SERVERPORT);
            System.err.println("Trying to connect to backup server on " + SERVERADRESS_BACKUP +
                    ":" + SERVERPORT_BACKUP);
            try {
                // Connect to Backup Server
                s = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
            } catch (IOException e1) {
                e1.printStackTrace();
                System.err.println("Cannot establish connection to any server :(");
                System.exit(-1);
            }
        }
        // Get input and output streams from socket
        try {
            in = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(
                new OutputStreamWriter(s.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a thread to write to
        System.out.println("MSG: " + message);
        Sender sender = new Sender(out);
        sender.setMessage(message);
        sender.setDaemon(true);
        sender.start();
        try {
            // Read messages from the server and print them
            while ((response = in.readLine()) != null) {
                setActive();
                setSuccess();
            }
            if (!isActive()) {
//                s.close();
                // Restart application if session isn't active
                new AppRestart();
            }
        } catch (IOException ioe) {
            System.err.println("Connection to server broken.");
            ioe.printStackTrace();
        }
    }

    public void setSuccess(){
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(response);
        try {
            success = fromServer.get("succeeded").getAsBoolean();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean getSuccess(){
        return this.success;
    }

    public String getResponse() {
        return response;
    }

    public void setActive() {
        JsonParser parser = new JsonParser();
        JsonObject fromServer = (JsonObject) parser.parse(response);
        active = fromServer.get("active").getAsBoolean();
    }

    public boolean isActive() {
        return this.active;
    }
}


