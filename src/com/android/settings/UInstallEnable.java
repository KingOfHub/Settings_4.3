package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class UInstallEnable implements OnCheckedChangeListener {
	
	private Context mContext;
	private Switch mSwitch;
	
	public UInstallEnable(Context context, Switch switch_) {
		mContext = context;
		mSwitch = switch_;
		Settings.Global.putInt(mContext.getContentResolver(),
	                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
		syncSwitchStat();
	}
	
	private void syncSwitchStat(){
		boolean mEnableAdb = Settings.Global.getInt(mContext.getContentResolver(),
	            Settings.Global.ADB_ENABLED, 0) != 0;
		mSwitch.setChecked(mEnableAdb);
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
		// TODO Auto-generated method stub
	    Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, isChecked ? 1 : 0);
	}
	
	public void setSwitch(Switch switch_) {
		if (mSwitch == switch_){
			syncSwitchStat();
			return;
		}

		mSwitch.setOnCheckedChangeListener(null);
		mSwitch = switch_;
		mSwitch.setOnCheckedChangeListener(this);
		syncSwitchStat();
	}

	public void resume() {
		mSwitch.setOnCheckedChangeListener(this);
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
	}
	
	public Switch getSwitch(){
		return mSwitch;
	}

}
