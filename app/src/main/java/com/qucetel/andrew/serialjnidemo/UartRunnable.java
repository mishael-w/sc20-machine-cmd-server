package com.qucetel.andrew.serialjnidemo;

import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class UartRunnable implements Runnable {
    final String LOG_TAG = "UartRunnable";

    private Uart mUart;
    private char parity;
    private String port;
    private boolean isflag;
    private ISerialDataCallback serialDataCallback;

    public UartRunnable(Uart mUart, ISerialDataCallback serialDataCallback) {
        this.mUart = mUart;
        this.serialDataCallback = serialDataCallback;
        Log.i(LOG_TAG, "setting speed1");
//        setSerialPortParams(baudrate, dataBits, stopBits, parity);
    }

    public void setSerialPortParams(int baudrate, int dataBits, int stopBits, char parity){
        Log.i(LOG_TAG, "setting speed2");
        mUart.setSerialPortParams(baudrate,dataBits,stopBits,parity);
    }

    public boolean openPort(String port){
        this.port = port;
        int portStatus = mUart.open(port);
        Log.i(LOG_TAG, "openning port: "+port+", port availability: " + (portStatus>=0));
        return isflag = portStatus>=0;
    }

    public void close(){
        Log.i(LOG_TAG, "closing port: "+port);
        isflag = false;
        mUart.close();
    }

    @Override
    public void run() {
        Log.i(LOG_TAG, port+"-->run");
        byte[] buffer = new byte[1024];
        while(isflag){
            String data;
            byte[] mByte = mUart.read_data(buffer,buffer.length,1000);

            if (mByte != null && mByte.length != 0 ){
                Log.i(LOG_TAG, "-------- " + (Integer.toHexString(mByte[0])));
                if (checkUTF8(mByte)){
                    Log.i(LOG_TAG, "values are utf-8 encoded - byte array size = " + mByte.length);
                    try {
                        data = new String(mByte, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        data = "EXCEPTION WAS THROWN WHILE DECODING THE SERIAL DATA!!!\n";
                        e.printStackTrace();
                    }
                }else {
                    Log.i(LOG_TAG, "values are not utf-8 encoded - " + String.format("0x%02X", mByte[0]));
                    data = new String();
                    for (byte cell : mByte)
                        data += String.format("%02X ", cell);
                }
                Log.i(LOG_TAG, "data = " + data);
                if (serialDataCallback != null)
                    serialDataCallback.receiveSerialData(data, port);
            }else {
//                mUart.write("hallo".getBytes(), "hallo".length() );
//                Log.v(LOG_TAG, "sending hallo");
                Log.v(LOG_TAG, "buffer is either empty or null");
            }
        }
        Log.v(LOG_TAG, "isflag = false - endless loop ended");
    }

    private boolean checkUTF8(byte[] barr){

        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        ByteBuffer buf = ByteBuffer.wrap(barr);

        try {
            decoder.decode(buf);

        }
        catch(CharacterCodingException e){
            return false;
        }

        return true;
    }
}
