package com.qucetel.andrew.serialjnidemo.sample;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.qucetel.andrew.serialjnidemo.Post;
import com.qucetel.andrew.serialjnidemo.R;

import java.lang.ref.WeakReference;

import static com.qucetel.andrew.serialjnidemo.Post.POST_TYPE_REPORT;

public class UIHandler extends Handler {
    public static final int UI_EVENT_PUBLISHING_POST = 2;
    public final static int UI_EVENT_POST_WAS_DELIVERED = 3;
    public static final int UI_EVENT_PUBLISHING_STANDBY = 4;
    public final static int UI_EVENT_SERIAL_INCOMMING = 5;

    private static final String TAG = "UIHandler";
    private WeakReference <MainActivity> uiActivityRefference;

    UIHandler(MainActivity activityRefference) {
        this.uiActivityRefference = new WeakReference<>( activityRefference);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Log.v(TAG, "Received a message!!!");
        if (uiActivityRefference.get() == null)
            return;
        MainActivity activity = uiActivityRefference.get();
        switch (msg.what) {
            case POST_TYPE_REPORT:
                int hslNum = msg.arg1;
                if (hslNum == 0){
                    activity.serialInputStatus(R.id.hsl0_input_status, R.id.hsl0_input_status_txt);
                }else if (hslNum == 1){
                    activity.serialInputStatus(R.id.hsl1_input_status, R.id.hsl1_input_status_txt);
                }
                activity.postSent(R.id.hsl0_publish_status, R.id.hsl0_publish_status_txt);
                String data = (String) msg.obj;
                Post postToBePublished =  new Post(POST_TYPE_REPORT, activity.mqttManager.clientId, data);
                Log.v(TAG, "publishing: " + postToBePublished.toJSon());
                activity.mqttManager.publish(Post.POST_TOPIC_AVAILABILITY, postToBePublished.toJSon() );
                break;
            case UI_EVENT_POST_WAS_DELIVERED:
                activity.postWasDelivered(R.id.hsl0_publish_status, R.id.hsl0_publish_status_txt);
                break;

        }
    }
}
