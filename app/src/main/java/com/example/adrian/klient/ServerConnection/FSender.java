package com.example.adrian.klient.ServerConnection;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dennisdufback on 16-04-14.
 */
public class FSender extends Thread {
    private OutputStream oS;
    private byte[] myByteArray;

    public FSender(OutputStream oS, byte[] myByteArray)
    {
        this.oS = oS;
        this.myByteArray = myByteArray;
    }

    public void run()
    {
        try {
            oS.write(myByteArray,0,myByteArray.length);
            oS.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
