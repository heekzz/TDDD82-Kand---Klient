package com.example.adrian.klient.ServerConnection;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class FSender implements Runnable{
    private OutputStream oS;
    private DataOutputStream dOS;
    private byte[] byteArray;

    public FSender(byte[] byteArray, OutputStream oS) {
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