package com.android.settings;

import com.android.settings.R;
import android.content.Context;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

public class NetworkSwitchPreference extends Preference{
	
	private Switch mSwitch;
	private  Context mContext;
	private DataEnabler mDataEnabler;

	public NetworkSwitchPreference(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public NetworkSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public NetworkSwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	private void init(Context context){
		mContext = context;
	}
	
	@Override
	protected void onBindView(View view) {
		// TODO Auto-generated method stub
		mSwitch = (Switch)view.findViewById(R.id.switchWidget);
		mDataEnabler = new DataEnabler(mContext, mSwitch);
		mDataEnabler.resume();
		super.onBindView(view);
	}
	
	public void resume(){
		if(mDataEnabler != null){
			mDataEnabler.resume();
		}
	}
	public void destroy(){
		mDataEnabler.destroy();
	}
	
	


	

}
