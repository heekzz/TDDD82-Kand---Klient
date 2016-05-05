package com.example.adrian.klient.ServerConnection;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class FConnection implements Runnable {

    byte[] byteArray;
    Context context;
    String fileName, response;

    CONN connection;
    boolean access, sending;

    String SERVERADRESS = "2016-4.itkand.ida.liu.se";
    String SERVERADRESS_BACKUP = "2016-3.itkand.ida.liu.se";
    int SERVERPORT = 9000;
    int SERVERPORT_BACKUP = 9000;

    public FConnection(byte[] byteArray, String fileName, Context context) throws IOException {
        this.byteArray = byteArray;
        this.fileName = fileName;
        this.context = context;
        requestFile();
    }

    @Override
    public void run() {
        BufferedReader in = null;
        OutputStream out = null;
        Socket s = null;


        if(access) {
            // Allowed to send file
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

            sending = true;
            while(sending){

                try{
                    out.write(byteArray, 0, byteArray.length);
                    out.flush();

                    response = in.readLine();

//                        while (response != null){
//                            System.out.println("FILE RESPONSE: " + response);
//                            response = null;
//                        }

                } catch(IOException e){
                    e.printStackTrace();
                }
            }
            PrintWriter pw = new PrintWriter(out);
            pw.println("DONE");
            pw.flush();


//            FileSender sender = new FileSender(out, byteArray);
//            new Thread(sender).start();
//
//            FileReceiver receiver = new FileReceiver(in);
//            new Thread(receiver).start();

        } else {
            // Not allowed to send file...
        }
    }
    void requestFile(){
        new Request(context,"add",fileName,String.valueOf(byteArray.length)).fileRequest();
        connection = new CONN(context);
        new Thread(connection).start();
        //Get response from server
        String jsonString;
        do{
            jsonString = connection.getResponse();
        } while(jsonString == null);

        JsonParser parser = new JsonParser();
        JsonObject object = (JsonObject) parser.parse(jsonString);
        access = object.get("access").getAsBoolean();
    }

}


class FileSender implements Runnable{
    private OutputStream oS;
    private byte[] byteArray;

    public FileSender(OutputStream oS, byte[] byteArray) {
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

class FileReceiver implements Runnable{
    private BufferedReader bR;
    private String response = "";

    public FileReceiver(BufferedReader bR) {
        this.bR = bR;
    }

    public void run() {

        String msg;
        try {
            while ((msg = bR.readLine()) != null){
                System.out.println("FILE RESPONSE: " + msg);
            }
        } catch (IOException e) {
        }
    }


    public String getResponse(){
        return response;
    }
}
