package com.qucetel.andrew.serialjnidemo;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.qucetel.andrew.serialjnidemo.sample.IDeliveryCB;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class MQTTManager {

    private static final String CUSTOMER_SPECIFIC_IOT_ENDPOINT = "a1h0hacnnou69m-ats.iot.us-west-2.amazonaws.com";

    private IMQTTCallback callback;

    private AWSIotMqttManager mqttManager;

    private static final String LOG_TAG = MQTTManager.class.getCanonicalName();

    public MQTTManager(Context context, IMQTTCallback mCallback) {
        callback = mCallback;
        clientId = UUID.randomUUID().toString();

        // Initialize the credentials provider
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(
                context,
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        callback.notifyUser("onError: "+ e);
                        Log.e(LOG_TAG, "onError: ", e);
                    }
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            callback.notifyUser("an error accured in awsManger ctor: " + e);
            e.printStackTrace();
        }

        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_IOT_ENDPOINT);
    }

    public String clientId;

    public void connect() {
        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                    if (throwable != null) {
                        Log.e(LOG_TAG, "Connection error.", throwable);
                        callback.setStatusState("Connection error");
                        return;
                    }
                    if ( status != null)
                        callback.setStatusState( String.valueOf(status));
                }
            });
        } catch (final Exception e) {
            callback.notifyUser("Connection error: " + e);
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }

    public void subscribe(final String topic) {

        Log.d(LOG_TAG, "topic = " + topic);
        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {


                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            try {
                                String message = new String(data, "UTF-8");
                                Log.d(LOG_TAG, "Message arrived:");
                                Log.d(LOG_TAG, "Topic: " + topic);
                                Log.d(LOG_TAG, "Message: " + message);
                                callback.proccessPost(message);
                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "Message encoding error.", e);
                            }
                        }
                    });
        } catch (Exception e) {
            callback.notifyUser("Subscription error: " + e);
            Log.e(LOG_TAG, "Subscription error: " +  e);
        }
    }

    private void publishHeartbeat(String topic, String msg) {
        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            callback.notifyUser("Publish error: " + e);
            Log.e(LOG_TAG, "Publish error: " + e);
        }
    }

    public void publish(String topic, String msg) {
        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0, new IDeliveryCB(), null);
        } catch (Exception e) {
            callback.notifyUser("Publish error: " + e);
            Log.e(LOG_TAG, "Publish error: " + e);
        }
    }

    public void disconnect() {
        try {
            mqttManager.disconnect();
            stopHeartbeat();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
            callback.notifyUser("Disconnect error: " + e);
        }
    }

    public void setClientId(String mClientId){
        clientId = mClientId;
    }

    private boolean heartbeatStarted = false;
    private Handler heartbeatScheduler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Post postToBePublished =  new Post(Post.POST_TYPE_HEARTBEAT, clientId, null);
            publishHeartbeat(Post.POST_TOPIC_AVAILABILITY, postToBePublished.toJSon());
            if(heartbeatStarted) {
                startHeartbeat();
            }
        }
    };

    private void stopHeartbeat() {
        heartbeatStarted = false;
        heartbeatScheduler.removeCallbacks(runnable);
    }

    public void startHeartbeat() {
        heartbeatStarted = true;
        heartbeatScheduler.postDelayed(runnable, 2000);
    }
}
