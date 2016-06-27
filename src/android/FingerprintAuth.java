package com.vertafore.cordova.fingerprint;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.security.keystore.KeyProperties;
import android.util.Log;
import com.google.gson.Gson;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import android.provider.Settings;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class FingerprintAuth extends CordovaPlugin {

    public static final String PLUGIN_NAME = "FingerprintAuth";

    private boolean isAvailable = false;
    private boolean isOSSupported = false;
    private boolean isHardwareDetected = false;
    private boolean isSetup = false;
    private boolean isSecure = false;
    private boolean hasEnrolledFingerprints = false;
    private FingerprintManager fingerPrintManager;
    private KeyguardManager keyguardManager;

    private int FINGERPRINT_REQUEST_CODE = 861;

    private CallbackContext callbackContext;

    public static KeyStore keyStore;
    public static KeyGenerator keyGenerator;
    public static Cipher cipher;

    public FingerprintAuth() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);


        if (android.os.Build.VERSION.SDK_INT < 23) {
            return;
        } else {
            isOSSupported = true;
        }

        try {
            keyguardManager = cordova.getActivity().getSystemService(KeyguardManager.class);
            fingerPrintManager = cordova.getActivity().getApplicationContext().getSystemService(FingerprintManager.class);
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (Exception e) {
            this.log("Error during initialization: " + e.getMessage());
        }

    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        /*
         * One-time run. Here instead of during initialize, because it can take a while to get all of the
         * information together - better while the app is open than while the app is just loading. Maybe
         * we can make this better. In JS I'd use promises, but it looks like Java at 1.7 doesn't have anything
         * comparable besides external libraries. Let's get this stable first, then optimize!
         */
        if(!this.isSetup) {
            // We're just going to set this regardless of
            // result - if it doesn't work now, it's not ever going to work.
            this.isSetup = true;
            try {
                setup();
            } catch (Exception e) {
                this.log("Error during setup: " + e.getMessage());
            }
        }

        final FingerprintAuth self = this;
        final Gson gson = new Gson();
        switch (action) {
            case "isAvailable":
                callbackContext.success(gson.toJson(this.isAvailable));
                return true;
            case "getSupport":
                callbackContext.success(gson.toJson(self.getSupport()));
                return true;
            case "show":

                //Is this the right way to do it?
                this.callbackContext = callbackContext;

                //Let's not block the main thread.
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {

                        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
                        cordova.startActivityForResult(self, intent, FINGERPRINT_REQUEST_CODE);
                         // Thread-safe.
                    }
                });

                //This tells cordova that something is coming.. at some point. But not right now.
                PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                callbackContext.sendPluginResult(r);
                return true;

            default:
                this.log("Could not find action: " + action);
                return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        this.log("RESULT!!!: " + requestCode + ", " + resultCode);

        if(resultCode == Activity.RESULT_OK) {
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } else {
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        }
    }

    private void setup() throws Exception {
        isHardwareDetected = fingerPrintManager.isHardwareDetected();
        hasEnrolledFingerprints = fingerPrintManager.hasEnrolledFingerprints();
        keyStore = KeyStore.getInstance("AndroidKeyStore");
        cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);

        isSecure = keyguardManager.isKeyguardSecure();
        if (isHardwareDetected && hasEnrolledFingerprints) {
            isAvailable = true;
        }

    }

    private Map<String,Object> getSupport() {

        Map<String,Object> support = new HashMap<>();
        support.put("isAvailable", isAvailable);
        support.put("isOSSupported", isOSSupported);
        support.put("isHardwareDetected", isHardwareDetected);
        support.put("hasEnrolled", hasEnrolledFingerprints);
        support.put("isSecure", isSecure);

        return support;
    }

    private void log(String message) {
        Log.e(PLUGIN_NAME, message);
    }


}