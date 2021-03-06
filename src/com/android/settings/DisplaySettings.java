/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static android.provider.Settings.System.CABL_ENABLED;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.os.Build;

import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import com.android.cabl.ICABLService;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_WIFI_DISPLAY = "wifi_display";
    private static final String KEY_CABL_BRIGHTNESS = "cabl_brightness";
    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private static final int CABL_CON_TYPE_DISABLE = 0;
    private static final int CABL_CON_TYPE_ENABLE  = 1;

    private static final String CABL_SERVICE_INTENT = "org.codeaurora.cabl.ICABLService";

    private DisplayManager mDisplayManager;
    private BrightnessPreference mBrightnessPreference;
    private Preference mBrightnessSettingsPreference;
    private CheckBoxPreference mAccelerometer;
    private WarnedListPreference mFontSizePref;
    private CheckBoxPreference mNotificationPulse;

    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;

    private WifiDisplayStatus mWifiDisplayStatus;
    private Preference mWifiDisplayPreference;

    private CheckBoxPreference mCablBrightnessPreference;
    private static boolean mCablAvailable;
    private static ICABLService mCABLService = null;
    private CABLServiceConnection mCABLServiceConn = null;
    private static boolean sCABLEnabled = true;
    private static final String PROP_CABL = "persist.env.settings.cabl";

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateAccelerometerRotationCheckbox();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        //add content adaptive backlight support
        mBrightnessPreference = (BrightnessPreference) findPreference(KEY_BRIGHTNESS);

        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        if (RotationPolicy.isRotationLockToggleSupported(getActivity()) || Build.PWV_CUSTOM_CUSTOM.contains("-43")
        || Build.PROJECT.contains("SQ27C")
         || Build.PROJECT.contains("SQ31Q")
         || Build.PROJECT.contains("SQ26B")
         || Build.PROJECT.contains("SQ27D")) {
            // If rotation lock is supported, then we do not provide this option in
            // Display settings.  However, is still available in Accessibility settings.
            getPreferenceScreen().removePreference(mAccelerometer);
        }
        
        if((Build.PROJECT.equals("SQ27")&& Build.PWV_CUSTOM_CUSTOM.equals("KPOS"))||Build.PWV_CUSTOM_CUSTOM.equals("SYB")){
        	boolean isKPOSAdmin = getActivity().getIntent().getBooleanExtra("isKPOSAdmin", false);
        	if(!isKPOSAdmin)
        		getPreferenceScreen().removePreference(mAccelerometer);
        	PreferenceScreen wallpaper = (PreferenceScreen) findPreference("wallpaper");
            if(wallpaper != null)
                getPreferenceScreen().removePreference(wallpaper);
        }
        if(Build.PWV_CUSTOM_CUSTOM.equals("SYB")){
        	PreferenceScreen screencolor= (PreferenceScreen) findPreference("screencolor_settings");
            if(screencolor != null)
                getPreferenceScreen().removePreference(screencolor);
        }

      /*  mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }*/
        
        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        long timeoutValue = (currentTimeout == Integer.MAX_VALUE) ? -1 : currentTimeout;
        mScreenTimeoutPreference.setValue(String.valueOf(timeoutValue));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(timeoutValue);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
            getPreferenceScreen().removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

        mDisplayManager = (DisplayManager)getActivity().getSystemService(
                Context.DISPLAY_SERVICE);
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        mWifiDisplayPreference = (Preference)findPreference(KEY_WIFI_DISPLAY);
        if (mWifiDisplayStatus.getFeatureState()
                == WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE) {
            getPreferenceScreen().removePreference(mWifiDisplayPreference);
            mWifiDisplayPreference = null;
        }

        mCablBrightnessPreference = (CheckBoxPreference) findPreference(KEY_CABL_BRIGHTNESS);
        sCABLEnabled = SystemProperties.getBoolean(PROP_CABL,true);
        if (!sCABLEnabled) {
             getPreferenceScreen().removePreference(mCablBrightnessPreference);
        } else {
            mCablAvailable = SystemProperties.getBoolean("ro.qualcomm.cabl", false);
            initCABLService();
        }
        if((Build.PROJECT.equals("SQ27")) && Build.PWV_CUSTOM_CUSTOM.equals("KQ")){
            if(mAccelerometer != null)
                getPreferenceScreen().removePreference(mAccelerometer);
            PreferenceScreen wallpaper = (PreferenceScreen) findPreference("wallpaper");
            if(wallpaper != null)
                getPreferenceScreen().removePreference(wallpaper);
            if(mFontSizePref != null) {
                getPreferenceScreen().removePreference(mFontSizePref);
            }
        }
        
        if((Build.PROJECT.equals("SQ27")) && (Build.PWV_CUSTOM_CUSTOM.equals("KPOS")||Build.PWV_CUSTOM_CUSTOM.equals("SYB"))){
        	removePreference("wallpaper");
        }
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout == -1) {
            summary = preference.getContext().getString(R.string.screensaver_timeout_zero_summary);
        } else if (currentTimeout < -1) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                if ((currentTimeout >= timeout)&&(timeout > 0)) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(fontSizeNames[index]);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        if (mWifiDisplayPreference != null) {
            getActivity().registerReceiver(mReceiver, new IntentFilter(
                    DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED));
            mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        }

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        if (mWifiDisplayPreference != null) {
            getActivity().unregisterReceiver(mReceiver);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
        updateWifiDisplaySummary();
        if (sCABLEnabled) {
            updateCablBrightnessCheckbox();
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void updateWifiDisplaySummary() {
        if (mWifiDisplayPreference != null) {
            switch (mWifiDisplayStatus.getFeatureState()) {
                case WifiDisplayStatus.FEATURE_STATE_OFF:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_off);
                    break;
                case WifiDisplayStatus.FEATURE_STATE_ON:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_on);
                    break;
                case WifiDisplayStatus.FEATURE_STATE_DISABLED:
                default:
                    mWifiDisplayPreference.setSummary(R.string.wifi_display_summary_disabled);
                    break;
            }
        }
    }

    private void updateAccelerometerRotationCheckbox() {
        if (getActivity() == null) return;

        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAccelerometer) {
            RotationPolicy.setRotationLockForAccessibility(
                    getActivity(), !mAccelerometer.isChecked());
        } else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
            return true;
        } else if (preference == mCablBrightnessPreference) {
            final boolean checked = mCablBrightnessPreference.isChecked();
            if (checked) {
                startCABL();
            } else {
                stopCABL();
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int oldvalue = Integer.parseInt(((ListPreference)preference).getValue());
            if (value != oldvalue) {
                Log.d(TAG, "update screen timeout from "+ oldvalue + " to " + value);
                int timeoutValue = ( -1 == value) ? Integer.MAX_VALUE : value;
            try {
                    Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, timeoutValue);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
                }
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }

        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                mWifiDisplayStatus = (WifiDisplayStatus)intent.getParcelableExtra(
                        DisplayManager.EXTRA_WIFI_DISPLAY_STATUS);
                updateWifiDisplaySummary();
            }
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    // add for UX_Brightness_settings start
    private void updateCablBrightnessCheckbox() {
        boolean cablEnabled = (1 == android.provider.Settings.Global.getInt(getContentResolver(),
                CABL_ENABLED, 0));
        //set default value for CABL check status
        if(null != mCablBrightnessPreference){
            mCablBrightnessPreference.setChecked(cablEnabled);
        }
    }

    private void startCABL() {
        if (mCablAvailable) {
            if (null != mCABLService) {
                final ContentResolver resolver = getActivity().getContentResolver();
                // Create a new thread execution
                // mCABLService.startCABL().
                // sleep will execution in the this thread.dosen't
                // Obstruction main thread.
                new Thread() {
                    public void run() {
                        try {
                            Log.d(TAG, "startCABL");
                            boolean result = mCABLService.control(CABL_CON_TYPE_ENABLE);
                            //0-disable cabl 1-enable cabl
                            Settings.Global.putInt(resolver,
                                    CABL_ENABLED,
                                    result ? 1 : 0);
                        } catch (RemoteException e) {
                                Log.e(TAG, "startCABL, exception");
                        }
                    }
                }.start();
            }
        }
    }

    private void stopCABL() {
        if (mCablAvailable
                && SystemProperties.get("init.svc.ppd").equals(
                        "running")) {
            if (null != mCABLService) {
                final ContentResolver resolver = getActivity().getContentResolver();
                // Create a new thread execution
                // mCABLService.stopCABL().
                // sleep will execution in the this thread.dosen't
                // Obstruction main thread.
                new Thread() {
                    public void run() {
                        try {
                            Log.d(TAG, "stopCABL");
                            boolean result = mCABLService.control(CABL_CON_TYPE_DISABLE);
                            //0-disable cabl 1-enable cabl
                            Settings.Global.putInt(resolver,
                                    CABL_ENABLED,
                                    result ? 0 : 1);
                        } catch (RemoteException e) {
                            Log.e(TAG, "stopCABL, exception");
                        }
                    }
                }.start();
            }
        }
    }

    private class CABLServiceConnection implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCABLService = ICABLService.Stub.asInterface((IBinder)service);
            Log.d(TAG, "onServiceConnected, service=" + mCABLService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCABLService = null;
        }

    }

    private void initCABLService(){
        Log.d(TAG, "initCABLService");

        mCABLServiceConn = new CABLServiceConnection();
        Intent i = new Intent(CABL_SERVICE_INTENT);
        boolean ret = getActivity().bindService(i, mCABLServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (sCABLEnabled){
            getActivity().unbindService(mCABLServiceConn);
        }
    }
    // add for UX_Brightness_settings end
}
