package com.sv.appfile;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.sv.appfile.view.WebServerActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class SandboxFileServerPlugin extends CordovaPlugin {
    private static final String ACTION_OPEN_PRIVATE_FTP_SERVER = "openPrivateFtpServer";
    private static final String KEY_APPLICATION_ID = "applicationId";
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final int DEFAULT_SERVER_PORT = 8889;
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (ACTION_OPEN_PRIVATE_FTP_SERVER.equals(action)) {
            doAction(ACTION_OPEN_PRIVATE_FTP_SERVER, args, callbackContext);
            return true;
        }
        return false;
    }
    private void doAction(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, WebServerActivity.class);
        if(args.length() <= 0){
            callbackContext.error("args is empty!");
            return;
        }

        String applicationId = context.getApplicationContext().getPackageName();
        int serverPort = DEFAULT_SERVER_PORT;

        JSONObject param = (JSONObject) args.get(0);
        if(!TextUtils.isEmpty(param.getString(KEY_APPLICATION_ID))){
            applicationId = param.getString(KEY_APPLICATION_ID);
        }

        if(!TextUtils.isEmpty(param.getString(KEY_SERVER_PORT))){
            serverPort = param.getInt(KEY_SERVER_PORT);
        }

        intent.putExtra(KEY_APPLICATION_ID, applicationId);
        intent.putExtra(KEY_SERVER_PORT, serverPort);

        cordova.getActivity().startActivity(intent);
        callbackContext.success();
    }
}
