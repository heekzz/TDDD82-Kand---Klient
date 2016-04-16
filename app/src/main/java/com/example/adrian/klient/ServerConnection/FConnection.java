package com.example.adrian.klient.ServerConnection;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class FConnection implements Runnable {

    private Context context;
    private byte[] byteArray;

    public FConnection(byte[] byteArray, Context context) throws IOException {
        this.context = context;
        this.byteArray = byteArray;
    }

    @Override
    public void run() {
        String SERVERADRESS = "2016-4.itkand.ida.liu.se";
        String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
        int SERVERPORT = 9001;
        int SERVERPORT_BACKUP = 9001;
        BufferedReader in = null;
        OutputStream out = null;
        /**
         * Connect to primary server, if it fails, connect to backup server
         */
        Socket s = null;
        try {
            // Connect to primary server
            s = new Client(context).getConnection(SERVERADRESS, SERVERPORT);
        } catch (IOException e) {
            // Print error and try connect to backup server
            System.err.println("Cannot establish connection to " +
                    SERVERADRESS + ":" + SERVERPORT);
            System.err.println("Trying to connect to backup server on " + SERVERADRESS_BACKUP +
                    ":" + SERVERPORT_BACKUP);
            try {
                s = new Client(context).getConnection(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
            } catch (IOException e1) {
                e1.printStackTrace();
                System.err.println("Cannot establish connection to any server :(");
            }
        }
        try {
            in = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            out = s.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("");
        }

        FSender sender = new FSender(out,byteArray);
        sender.setDaemon(true);
        sender.start();
    }
}
