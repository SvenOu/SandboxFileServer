package com.sv.appfile.componet;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import fi.iki.elonen.NanoHTTPD;

public class AndroidWebServer extends NanoHTTPD {
    private static final String TAG = AndroidWebServer.class.getName();

    // change port before first getInstance
    public static int SERVER_PORT = 8889;
    private BroadcastReceiver broadcastReceiverNetworkState;
    private WeakReference<Application> applicationWeakReference;
    private Listener listener;
    private boolean isStarted = false;
    private boolean hasInit = false;

    public interface Listener{
        void onNetChange(Context context, Intent intent);
        Response serve(IHTTPSession session);
        void onStartAndroidWebServer();
        void onStopAndroidWebServer();
    }

    private volatile static AndroidWebServer instance;

    private AndroidWebServer (){
        super(SERVER_PORT);
    }

    public static AndroidWebServer getInstance() {
        if (instance == null) {
            synchronized (AndroidWebServer.class) {
                if (instance == null) {
                    instance = new AndroidWebServer();
                }
            }
        }
        return instance;
    }

    public void destoryInstance(){
        Context context = applicationWeakReference.get();
        isStarted = false;
        if (broadcastReceiverNetworkState != null) {
            context.unregisterReceiver(broadcastReceiverNetworkState);
        }
        this.closeAllConnections();
        instance = null;
    }

    public AndroidWebServer init(Application application, Listener listener){
        if(hasInit){
            Log.w(TAG, "AndroidWebServer already init.");
            return this;
        }
        this.applicationWeakReference = new WeakReference<>(application);
        this.listener = listener;
        // INIT BROADCAST RECEIVER TO LISTEN NETWORK STATE CHANGED
        initBroadcastReceiverNetworkStateChanged(applicationWeakReference.get());

        hasInit = true;
        return this;
    }

    private void initBroadcastReceiverNetworkStateChanged(Context context) {
        if(null == broadcastReceiverNetworkState){
            final IntentFilter filters = new IntentFilter();
            filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            filters.addAction("android.net.wifi.STATE_CHANGE");
            broadcastReceiverNetworkState = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(listener != null){
                        listener.onNetChange(context, intent);
                    }
                }
            };
            context.registerReceiver(broadcastReceiverNetworkState, filters);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        if(listener != null){
            return listener.serve(session);
        }
        return null;
    }

    public boolean startAndroidWebServer() {
        if (!this.isStarted()) {
            try {
                this.start();
                if(null != listener){
                    listener.onStartAndroidWebServer();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(applicationWeakReference.get(),
                        "start android web server fail",
                        Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    public boolean stopAndroidWebServer() {
        if (this.isStarted() && instance != null) {
            this.stop();
            if(null != listener){
                listener.onStopAndroidWebServer();
            }
            return true;
        }
        return false;
    }
    //endregion

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
