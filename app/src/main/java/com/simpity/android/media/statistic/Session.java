package com.simpity.android.media.statistic;

import java.util.Calendar;

import android.content.SharedPreferences;

public class Session {
	
	private final String sessionKey;
	private final SharedPreferences mPrefs;
	private long startTime;
	
	public Session(SharedPreferences prefs, String sessionKey){
		this.sessionKey = sessionKey;
		startTime = Calendar.getInstance().getTimeInMillis();
		mPrefs = prefs;
	}
	
	public void endSession(){
		if(StatCollector.IS_COLLECT_STATISTIC){
			if(startTime > 0){
				long sessionlength = Calendar.getInstance().getTime().getTime() - startTime;
				long currentValue = mPrefs.getLong(sessionKey, 0);
				mPrefs.edit().putLong(sessionKey, currentValue + sessionlength).commit();	
				startTime = -1;
			}
		}
	}
	
	public void cancelSession(){
		startTime = -1;
	}
	
}
