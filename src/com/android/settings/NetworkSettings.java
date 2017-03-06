package com.android.settings;

import com.android.settings.R;
import android.os.Bundle;

public class NetworkSettings extends  SettingsPreferenceFragment{
	private NetworkSwitchPreference mNetworkSwitchPreference;
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        addPreferencesFromResource(R.xml.network_settings);
        mNetworkSwitchPreference = (NetworkSwitchPreference) findPreference("mobile_network");
        
	}
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		mNetworkSwitchPreference.resume();
		super.onResume();
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mNetworkSwitchPreference.destroy();
		super.onDestroy();
	}

}
