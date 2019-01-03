package com.qucetel.andrew.serialjnidemo.sample;

import android.util.Log;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;

import static com.qucetel.andrew.serialjnidemo.sample.MainActivity.uiHandler;
import static com.qucetel.andrew.serialjnidemo.sample.UIHandler.UI_EVENT_POST_WAS_DELIVERED;

public class IDeliveryCB implements AWSIotMqttMessageDeliveryCallback {
    @Override
    public void statusChanged(MessageDeliveryStatus status, Object userData) {
        Log.i("IDeliveryCB", "message status: " + status.name());
        uiHandler.sendEmptyMessage(UI_EVENT_POST_WAS_DELIVERED);
    }
}
