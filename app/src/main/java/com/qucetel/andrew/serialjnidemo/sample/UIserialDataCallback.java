package com.qucetel.andrew.serialjnidemo.sample;

import android.os.Message;

import com.qucetel.andrew.serialjnidemo.ISerialDataCallback;

import static com.qucetel.andrew.serialjnidemo.Post.POST_TYPE_REPORT;
import static com.qucetel.andrew.serialjnidemo.sample.MainActivity.uiHandler;

public class UIserialDataCallback implements ISerialDataCallback{

    @Override
    public void receiveSerialData(String data, String port) {

        Message m = Message.obtain(uiHandler, POST_TYPE_REPORT);
        if (port.contains("0"))
            m.arg1 = 0;
        else if (port.contains("1"))
            m.arg1 = 1;

        m.obj = data;
        uiHandler.sendMessage(m);
    }
}
