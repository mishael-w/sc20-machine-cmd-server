package com.qucetel.andrew.serialjnidemo.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qucetel.andrew.serialjnidemo.MQTTManager;
import com.qucetel.andrew.serialjnidemo.IMQTTCallback;
import com.qucetel.andrew.serialjnidemo.Post;
import com.qucetel.andrew.serialjnidemo.R;
import com.qucetel.andrew.serialjnidemo.Uart;
import com.qucetel.andrew.serialjnidemo.UartRunnable;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements IMQTTCallback {
    public static final String LOG_TAG = "XAXA";
    public static Handler uiHandler;
    public MQTTManager mqttManager;

    static {
        System.loadLibrary("native-lib");
    }


    ImageView hsl0_input_status;
    ImageView hsl0_publish_status;
    ImageView hsl1_input_status;
    TextView hsl0_input_status_txt;
    TextView hsl0_publish_status_txt;
    TextView hsl1_input_status_txt;
    Uart mUart1;
    Uart mUart2;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Serial ports setup
        mUart1 = new Uart();
        final UartRunnable uartRunnable = new UartRunnable(mUart1, new UIserialDataCallback());
        if (uartRunnable.openPort("/dev/ttyHSL0")) {
            uartRunnable.setSerialPortParams(115200,8,1,'n');
            new Thread(uartRunnable).start();
        }

        mUart2 = new Uart();
        final UartRunnable uartRunnable2 = new UartRunnable(mUart2, new UIserialDataCallback());
        if(uartRunnable2.openPort("/dev/ttyHSL1")) {
            uartRunnable2.setSerialPortParams(115200,8,1,'n');
            new Thread(uartRunnable2).start();
        }

        // UI initialization

        uiHandler = new UIHandler(this);

        hsl0_input_status = (ImageView)findViewById(R.id.hsl0_input_status);
        hsl0_publish_status = (ImageView)findViewById(R.id.hsl0_publish_status);
        hsl1_input_status = (ImageView)findViewById(R.id.hsl1_input_status);
        hsl0_input_status_txt = (TextView) findViewById(R.id.hsl0_input_status_txt);
        hsl0_publish_status_txt = (TextView) findViewById(R.id.hsl0_publish_status_txt);
        hsl1_input_status_txt = (TextView) findViewById(R.id.hsl1_input_status_txt);



        // set clientID
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (mqttManager == null) {
            mqttManager = new MQTTManager(getApplicationContext(), this);
        }
        mqttManager.setClientId(telephonyManager.getDeviceId());
    }


    private boolean flag = false;
    void serialInputStatus(final int imgId,final int txtId){
        if (flag)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                flag = true;
                for (int i = 0 ; i < 2 ; i++) {
                    final int temp = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (temp == 0){
                                findViewById(imgId).setActivated(true);
                                ((TextView)findViewById(txtId)).setText("Incomming...");
                            }
                            if (temp==1){
                                findViewById(imgId).setActivated(false);
                                findViewById(imgId).setEnabled(true);
                                ((TextView)findViewById(txtId)).setText("Idle");
                            }
                        }
                    });
                    try {
                            Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(LOG_TAG, "Exception in sleep");
                        e.printStackTrace();
                    }
                }
                flag = false;
            }
        }).start();

    }

    void postSent(int imgId, int txtId){
        findViewById(imgId).setActivated(true);
        ((TextView)findViewById(txtId)).setText("Post sent");
    }

    void postWasDelivered(int imgId, int txtId){
        findViewById(imgId).setActivated(false);
        findViewById(imgId).setEnabled(true);
        ((TextView)findViewById(txtId)).setText("Idle");

    }

    @Override
    public void setStatusState(final String state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.connection_status_txt)).setText(state);
            }
        });
        Log.i(LOG_TAG, "setStatusState: " + state);
        if (state.equalsIgnoreCase("connected")){
            Log.i(LOG_TAG, "subscribing to cmd");
            mqttManager.subscribe("cmd");
            mqttManager.subscribe(mqttManager.clientId);
            mqttManager.startHeartbeat();
        }
    }

    @Override
    public void notifyUser(String msg) {
        Log.i(LOG_TAG, "notifyUser: " + msg);
        displayMsg(msg);
    }

    @Override
    public void proccessPost(String cmd) {
        try {
            Log.i(LOG_TAG, "proccessPost: " + cmd);
            final Post command = Post.postFromJson(cmd);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.command_received_txt)).setText(command.data);
                }
            });

            switch (command.type) {
                case Post.POST_TYPE_PERSONAL:
                    Log.i(LOG_TAG, "got personal post: " + cmd);
                    if (command.destination == 1)
                        mUart1.write(command.data.getBytes(), command.data.length());//write to hsl0;
                    if (command.destination == 2)
                        mUart2.write(command.data.getBytes(), command.data.length());// write to hsl1

                    break;
                case Post.POST_TYPE_GLOBAL:
                    Log.i(LOG_TAG, "got global post: " + cmd);
                    break;
                default:
            }

            displayMsg(cmd);
        }catch (Exception e){
            Log.i(LOG_TAG, "Exception: " + e.getMessage() );
        }
    }

    private void displayMsg(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mqttManager.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Destroying activity");
        if (mqttManager != null)
            mqttManager.disconnect();
        mqttManager = null;

        if (uiHandler != null)
            uiHandler.removeCallbacksAndMessages(null);
        uiHandler = null;
    }

}
