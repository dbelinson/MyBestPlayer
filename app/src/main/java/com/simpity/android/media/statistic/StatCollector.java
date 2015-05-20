package com.simpity.android.media.statistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.simpity.android.media.storage.Storage;

public class StatCollector {
	
	public static final int TYPE_VIDEO = Storage.STREAM_VIDEO_GROUP;
	public static final int TYPE_RADIO = Storage.INTERNET_RADIO_GROUP;
	public static final int TYPE_CAMERA = Storage.JPEG_CAMERA_GROUP;
	
	private static final String COUNT_TYPE_KEY_FMT = "count_of_launches_%d";
	private static final String COUNT_APP_LAUNCH_KEY = "count_app_launch";
	
	private static final String TOTAL_TIME_FOR_TYPE_KEY_FMT = "total_time_for_type_%d";

	public static final Boolean IS_COLLECT_STATISTIC = false;
	
	public static void collectAppLaunchCount(Context context){
		if(IS_COLLECT_STATISTIC){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			int currentValue = prefs.getInt(COUNT_APP_LAUNCH_KEY, 0);
			currentValue++;
			prefs.edit().putInt(COUNT_APP_LAUNCH_KEY, currentValue).commit();
		}
	}	
		
	private static void collectLaunchCount(Context context, int type){
		if(IS_COLLECT_STATISTIC){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String key = String.format(COUNT_TYPE_KEY_FMT, type);
			int currentValue = prefs.getInt(key, 0);
			currentValue++;
			prefs.edit().putInt(key, currentValue).commit();
		}
	}
	
	private static Session startCollectTime(Context context, int type){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Session session = new Session(prefs, String.format(TOTAL_TIME_FOR_TYPE_KEY_FMT, type));
		return session;
	}
	
	public static Session startCollect(Context context, int type){
		collectLaunchCount(context, type);
		return startCollectTime(context, type);
	} 
	
	public static StatSummary getStat(Context context){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return new StatSummary(
				
				prefs.getInt(COUNT_APP_LAUNCH_KEY, 0), 
				
				prefs.getInt(String.format(COUNT_TYPE_KEY_FMT, TYPE_RADIO), 0), 
				prefs.getInt(String.format(COUNT_TYPE_KEY_FMT, TYPE_VIDEO), 0), 
				prefs.getInt(String.format(COUNT_TYPE_KEY_FMT, TYPE_CAMERA), 0),
				
				prefs.getLong(String.format(TOTAL_TIME_FOR_TYPE_KEY_FMT, TYPE_RADIO), 0), 
				prefs.getLong(String.format(TOTAL_TIME_FOR_TYPE_KEY_FMT, TYPE_VIDEO), 0), 
				prefs.getLong(String.format(TOTAL_TIME_FOR_TYPE_KEY_FMT, TYPE_CAMERA), 0)
				);
	} 
}


