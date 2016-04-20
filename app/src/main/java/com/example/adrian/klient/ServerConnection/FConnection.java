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

    Context context;
    byte[] byteArray;

    String SERVERADRESS = "2016-4.itkand.ida.liu.se";
    String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
    int SERVERPORT = 9001;
    int SERVERPORT_BACKUP = 9001;

    public FConnection(byte[] byteArray, Context context) throws IOException {
        this.byteArray = byteArray;
        this.context = context;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        OutputStream out = null;
        /**
         * Connect to primary server, if it fails, connect to backup server
         */
        Socket s = null;
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
                s = new Socket(SERVERADRESS_BACKUP, SERVERPORT_BACKUP);
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
        }

            FileSender sender = new FileSender(out, byteArray, s);
            new Thread(sender).start();

            FileReceiver receiver = new FileReceiver(in);
            new Thread(receiver).start();

//        try {
//            String msg;
//            while ((msg = in.readLine()) != null){
//                System.out.println("MSG: " + msg);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}



class FileSender implements Runnable {
    private OutputStream oS;
    private byte[] byteArray;

    public FileSender(OutputStream oS, byte[] byteArray, Socket s) {
        this.byteArray = byteArray;
        this.oS = oS;
    }

    public void run() {

        try {
            try{
                oS.write(byteArray, 0, byteArray.length);
            } catch(IOException e){
                e.printStackTrace();
            }
            oS.flush();
            oS.close();

        } catch (IOException e) {
        }
    }
}

class FileReceiver extends Thread{
    private BufferedReader bR;
    private String response = "";

    public FileReceiver(BufferedReader bR) {
        this.bR = bR;
    }

    public void run() {

        String msg;
        try {
            while ((msg = bR.readLine()) != null){
                setResponse(msg);
                System.out.println("MSG: " + msg);
            }
        } catch (IOException e) {
        }
    }

    public void setResponse(String msg) {
        response = msg;
    }
    public String getResponse(){
        return response;
    }
}
