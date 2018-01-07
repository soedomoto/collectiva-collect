/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.collectiva.CollectivaLoginActivity;
import org.odk.collect.android.collectiva.CollectivaMenuMainActivity;
import org.odk.collect.android.collectiva.CollectivaPreferences;
import org.odk.collect.android.collectiva.Var;
import org.odk.collect.android.database.ActivityLogger;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.preferences.PreferenceKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.odk.collect.android.preferences.PreferenceKeys.KEY_METADATA_EMAIL;
import static org.odk.collect.android.preferences.PreferenceKeys.KEY_METADATA_PHONENUMBER;
import static org.odk.collect.android.preferences.PreferenceKeys.KEY_METADATA_USERNAME;

public class SplashScreenActivity extends Activity implements IPropertyManager{

    private static final int mSplashTimeout = 2000; // milliseconds
    private static final boolean EXIT = true;

    private AlertDialog mAlertDialog;
    private SharedPreferences collectPreferences;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash_screen);

        collectPreferences = getSharedPreferences(CollectivaPreferences.COLLECTIVA_PREFERENCES_KEY,MODE_PRIVATE);

        // get the shared preferences object
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = sharedPreferences.edit();

        // get the package info object with version number
        PackageInfo packageInfo = null;

        try {
            packageInfo =
                    getPackageManager().getPackageInfo(getPackageName(),
                            PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            Timber.e(e, "Unable to get package info");
        }

        if (sharedPreferences.getLong(PreferenceKeys.KEY_LAST_VERSION, 0)
                < packageInfo.versionCode) {
            editor.putLong(PreferenceKeys.KEY_LAST_VERSION, packageInfo.versionCode);
            editor.apply();

//            firstRun = true;
        }

        startSplashScreen();

    }


    private void endSplashScreen() {

        initPropertyManager(this);
        PropertyManager mgr = new PropertyManager(this);

        FormController.initializeJavaRosa(mgr);
        Collect.mActivityLogger = new ActivityLogger(
                mgr.getSingularProperty(PropertyManager.PROPMGR_DEVICE_ID));

//        check permission
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

//        launch new activity and close splash screen
        startStandartModeForSimpleUser();

//        if you use for advanced, which support authentication, and multiple survei, use this method
//        startAdvancedModeForSupportLoginAndMultiSurvei("http://yourserveraddress");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("DEBUGCOLL","requestCode :"+requestCode);
        switch (requestCode){
            case 1:
                boolean allgranted = true;
                for (int i=0; i<grantResults.length;i++) {
                    Log.d("DEBUGCOLL","permission: "+permissions[i]+", is "+grantResults[i]);
                    if(grantResults[i] !=  PackageManager.PERMISSION_GRANTED) allgranted = false;
                }

                if(allgranted){
                    endSplashScreen();
                }else {
                    Toast.makeText(this, "Collectiva can't start without all that permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void requestPermission() {
        Log.d("DEBUGCOLL","request permission");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }

    private boolean isAllPermissionGranted(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }

    public void startStandartModeForSimpleUser(){
        Editor editor = collectPreferences.edit();
        editor.putBoolean(Var.COLLECTIVA_MODE_COMPLEKS, false);
        editor.apply();
        startActivity(new Intent(SplashScreenActivity.this, CollectivaMenuMainActivity.class));
        finish();
    }

    public void startAdvancedModeForSupportLoginAndMultiSurvei(String serverUrl){
        boolean isLogin = collectPreferences.getBoolean(CollectivaLoginActivity.ISLOGIN, false);
        Intent intent;
        if(!isLogin) {
            intent = new Intent(SplashScreenActivity.this, CollectivaLoginActivity.class);
        }else {
            intent = new Intent(SplashScreenActivity.this, CollectivaMenuMainActivity.class);
        }
        Editor editor = collectPreferences.edit();
        editor.putString(Var.URL_MAIN_SERVER, serverUrl);
        editor.putBoolean(Var.COLLECTIVA_MODE_COMPLEKS, true);
        editor.apply();
        startActivity(intent);
        finish();
    }


    private void startSplashScreen() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isAllPermissionGranted()) endSplashScreen();
                else requestPermission();
            }
        },1000);
    }


    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger().logAction(this,
                                "createErrorDialog", "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public static final String PROPMGR_DEVICE_ID        = "deviceid";
    public static final String PROPMGR_SUBSCRIBER_ID    = "subscriberid";
    public static final String PROPMGR_SIM_SERIAL       = "simserial";
    public static final String PROPMGR_PHONE_NUMBER     = "phonenumber";
    public static final String PROPMGR_USERNAME         = "username";
    public static final String PROPMGR_EMAIL            = "email";

    private static final String ANDROID6_FAKE_MAC = "02:00:00:00:00:00";

    private static final String SCHEME_USERNAME     = "username";
    private static final String SCHEME_TEL          = "tel";
    private static final String SCHEME_MAILTO       = "mailto";
    private static final String SCHEME_IMSI         = "imsi";
    private static final String SCHEME_SIMSERIAL    = "simserial";
    private static final String SCHEME_IMEI         = "imei";
    private static final String SCHEME_MAC          = "mac";

    private final Map<String, String> mProperties = new HashMap<>();

    public String getName() {
        return "Property Manager";
    }

    private class IdAndPrefix {
        String id;
        String prefix;

        IdAndPrefix(String id, String prefix) {
            this.id = id;
            this.prefix = prefix;
        }
    }

    private boolean isAllPermissionGranted(Context context){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }


    private void initPropertyManager(Context context) {
        Timber.i("calling constructor");

        // Device-defined properties
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        IdAndPrefix idp = findDeviceId(context, telMgr);
        putProperty(PROPMGR_DEVICE_ID,     idp.prefix,          idp.id);
        putProperty(PROPMGR_PHONE_NUMBER,  SCHEME_TEL,          telMgr.getLine1Number());
        putProperty(PROPMGR_SUBSCRIBER_ID, SCHEME_IMSI,         telMgr.getSubscriberId());
        putProperty(PROPMGR_SIM_SERIAL,    SCHEME_SIMSERIAL,    telMgr.getSimSerialNumber());

        // User-defined properties. Will replace any above with the same PROPMGR_ name.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        initUserDefined(prefs, KEY_METADATA_USERNAME,    PROPMGR_USERNAME,      SCHEME_USERNAME);
        initUserDefined(prefs, KEY_METADATA_PHONENUMBER, PROPMGR_PHONE_NUMBER,  SCHEME_TEL);
        initUserDefined(prefs, KEY_METADATA_EMAIL,       PROPMGR_EMAIL,         SCHEME_MAILTO);
    }

    private IdAndPrefix findDeviceId(Context context, TelephonyManager telephonyManager) {
        final String androidIdName = Settings.Secure.ANDROID_ID;
        String deviceId = telephonyManager.getDeviceId();
        String scheme = null;

        if (deviceId != null) {
            if ((deviceId.contains("*") || deviceId.contains("000000000000000"))) {
                deviceId = Settings.Secure.getString(context.getContentResolver(), androidIdName);
                scheme = androidIdName;
            } else {
                scheme = SCHEME_IMEI;
            }
        }

        if (deviceId == null) {
            // no SIM -- WiFi only
            // Retrieve WiFiManager
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // Get WiFi status
            WifiInfo info = wifi.getConnectionInfo();
            if (info != null && !ANDROID6_FAKE_MAC.equals(info.getMacAddress())) {
                deviceId = info.getMacAddress();
                scheme = SCHEME_MAC;
            }
        }

        // if it is still null, use ANDROID_ID
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), androidIdName);
            scheme = androidIdName;
        }

        return new IdAndPrefix(deviceId, scheme);
    }

    /**
     * Initializes a property and its associated “with URI” property, from shared preferences.
     * @param preferences the shared preferences object to be used
     * @param prefKey the preferences key
     * @param propName the name of the property to set
     * @param scheme the scheme for the associated “with URI” property
     */
    private void initUserDefined(SharedPreferences preferences, String prefKey,
                                 String propName, String scheme) {
        putProperty(propName, scheme, preferences.getString(prefKey, null));
    }

    private void putProperty(String propName, String scheme, String value) {
        if (value != null) {
            mProperties.put(propName, value);
            mProperties.put(withUri(propName), scheme + ":" + value);
        }
    }

    @Override
    public List<String> getProperty(String propertyName) {
        return null;
    }

    @Override
    public String getSingularProperty(String propertyName) {
        // for now, all property names are in english...
        return mProperties.get(propertyName.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public void setProperty(String propertyName, String propertyValue) {
    }

    @Override
    public void setProperty(String propertyName, List<String> propertyValue) {
    }

    @Override
    public void addRules(IPropertyRules rules) {
    }

    @Override
    public List<IPropertyRules> getRules() {
        return null;
    }

    public static String withUri(String name) {
        return "uri:" + name;
    }

}
