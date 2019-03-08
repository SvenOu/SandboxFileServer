package com.sv.appfile.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.sv.appfile.R;
import com.sv.appfile.componet.AndroidWebServer;
import com.sv.appfile.service.FileService;
import com.sv.appfile.service.impl.FileServiceImpl;

import fi.iki.elonen.NanoHTTPD;

public class WebServerActivity extends AppCompatActivity {
    private static final String TAG = WebServerActivity.class.getName();

    private String applicationId;
    private int serverPort;

    // INSTANCE OF ANDROID WEB SERVER
    private AndroidWebServer androidWebServer;
    private FileService fileService;

    // VIEW
    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton floatingActionButtonOnOff;
    private View textViewMessage;
    private TextView textViewIpAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_server);

        applicationId = getIntent().getStringExtra("applicationId");
        serverPort = getIntent().getIntExtra("serverPort",
                AndroidWebServer.SERVER_PORT);

        // INIT VIEW
        fileService = new FileServiceImpl(this);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        textViewMessage = findViewById(R.id.tv_message);
        textViewIpAccess = findViewById(R.id.tv_ipAccess);
        floatingActionButtonOnOff = findViewById(R.id.fbtn_OnOff);

        AndroidWebServer.SERVER_PORT = serverPort;
        androidWebServer = AndroidWebServer.getInstance().init(getApplication(),
                new AndroidWebServer.Listener() {
                    @Override
                    public void onNetChange(Context context, Intent intent) {
                        setIpAccess();
                    }

                    @Override
                    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        return fileService.handerUri(applicationId, session);
                    }

                    @Override
                    public void onStartAndroidWebServer() {

                    }

                    @Override
                    public void onStopAndroidWebServer() {

                    }
                });


        setIpAccess();

        floatingActionButtonOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSerer();
            }
        });
        toggleSerer();
    }

    private void toggleSerer() {
        if (isConnectedInWifi()) {
            if (!androidWebServer.isStarted() && androidWebServer.startAndroidWebServer()) {
                androidWebServer.setStarted(true);
                textViewMessage.setVisibility(View.VISIBLE);
                floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorGreen));
            } else if (androidWebServer.stopAndroidWebServer()) {
                androidWebServer.setStarted(false);
                textViewMessage.setVisibility(View.INVISIBLE);
                floatingActionButtonOnOff.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
            }
        } else {
            Snackbar.make(coordinatorLayout, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show();
        }
    }

    //region Private utils Method
    private void setIpAccess() {
        textViewIpAccess.setText(getIpAccess());
    }

    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":" + serverPort + "/"+ applicationId;
    }

    public boolean isConnectedInWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()
                && wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }
    //endregion

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (androidWebServer.isStarted()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.dialog_exit_message)
                        .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        androidWebServer.stopAndroidWebServer();
        androidWebServer.destoryInstance();
    }
}
