package com.simpity.android.media.widgets.camera;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.simpity.android.media.Res;

public class CameraWidgetConfiguration extends Activity {
	
	public static final String ACTION_WIDGET_CONFIGURED = "com.psa.android.media.widgets.camera.ACTION_WIDGET_CONFIGURED"; 
	
	private static final String PREF_URL_KEY = "URL_";
	private static final String PREF_PERIOD_KEY = "REFRESH_PERIOD_";
	private static final String PREF_USERNAME_KEY = "USERNAME_";
	private static final String PREF_PASSWORD_KEY = "PASSWORD_";
	
	private final static int DEFAULT_REFRESH_TIME = 5;//5 sec
	
	public static final String URL_KEY = "URL";
	public static final String PERIOD_KEY = "REFRESH_PERIOD";
	public static final String USERNAME_KEY = "USERNAME_KEY";
	public static final String PWD_KEY = "PWD_KEY";
	
	private EditText mAppWidgetUrl, mUsername, mPassword;
	private Spinner mRefreshSpinner;
	private CheckBox mEnableSecurity;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setResult(RESULT_CANCELED);
		setContentView(Res.layout.webcam_widget_configure);
		mRefreshSpinner = (Spinner)findViewById(Res.id.jpeg_refresh_spinner);
		mAppWidgetUrl = (EditText) findViewById(Res.id.camera_widget_url);
		mUsername = (EditText) findViewById(Res.id.Username);
		mPassword = (EditText) findViewById(Res.id.Password);
		mEnableSecurity = ((CheckBox)findViewById(Res.id.enableSecurity));
		
		mEnableSecurity.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				findViewById(Res.id.securitySection).setVisibility(isChecked ? View.VISIBLE : View.GONE);
			}
		});
		
		mAppWidgetUrl.setText(getIntent().getStringExtra(URL_KEY));
		int position = 0;
		for (String refreshTime : getResources().getStringArray(Res.arrays.camera_refresh_time_values)) {
			if(refreshTime.equalsIgnoreCase(getIntent().getStringExtra(PERIOD_KEY))){
				mRefreshSpinner.setSelection(position);
				position = -1;
				break;
			}else
				position++;
		}
		if(position != -1){
			mRefreshSpinner.setSelection(1);
		}
		
		String username = getIntent().getStringExtra(USERNAME_KEY);
		if(username != null){
			mUsername.setText(username);
			mEnableSecurity.setChecked(true);
		}
		
		String pwd = getIntent().getStringExtra(PWD_KEY);
		if(pwd != null){
			mPassword.setText(pwd);
			mEnableSecurity.setChecked(true);
		}
		
		findViewById(Res.id.start_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String URLLink = mAppWidgetUrl.getText().toString();
				if(URLLink == null || URLLink.trim().length() ==0 || mRefreshSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION){
					//TODO show some message
					return;
				}
				String period = getResources().getStringArray(Res.arrays.camera_refresh_time_values)[mRefreshSpinner.getSelectedItemPosition()];
				
				Intent result = new Intent(ACTION_WIDGET_CONFIGURED);
				result.putExtras(getIntent());
				result.putExtra(URL_KEY, URLLink);
				result.putExtra(PERIOD_KEY, period);
				
				int widgetID = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
				
				String userName = null, pwd = null;
				
				if(mEnableSecurity.isChecked()){
					userName = mUsername.getText().toString();
					pwd = mPassword.getText().toString();
				}
				
				result.putExtra(USERNAME_KEY, userName);
				result.putExtra(PWD_KEY, pwd);
					
				saveUsernamePref(CameraWidgetConfiguration.this, widgetID, userName);
				savePwdPref(CameraWidgetConfiguration.this, widgetID, pwd);
				
				sendBroadcast(result);
				
				saveURLPref(CameraWidgetConfiguration.this, widgetID, URLLink);
				savePeriodPref(CameraWidgetConfiguration.this, widgetID, Integer.valueOf(period));
				
				setResult(RESULT_OK, getIntent());
				finish();
			}
		});
	
		
	}
	
	public static void saveURLPref(Context context, int mAppWidgetId, String text) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		prefs.putString(PREF_URL_KEY + String.valueOf(mAppWidgetId), text);
		prefs.commit();
	}
	
	public static void saveUsernamePref(Context context, int mAppWidgetId, String text) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		prefs.putString(PREF_USERNAME_KEY + String.valueOf(mAppWidgetId), text);
		prefs.commit();
	}
	
	public static void savePwdPref(Context context, int mAppWidgetId, String text) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		prefs.putString(PREF_PASSWORD_KEY + String.valueOf(mAppWidgetId), text);
		prefs.commit();
	}

	public static void savePeriodPref(Context context, int mAppWidgetId, int period) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		prefs.putInt(PREF_PERIOD_KEY + String.valueOf(mAppWidgetId), period);
		prefs.commit();
	}
	
	public static String loadURLPref(Context context, int mAppWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREF_URL_KEY + String.valueOf(mAppWidgetId), null);
	}
	
	public static String loadUsernamePref(Context context, int mAppWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREF_USERNAME_KEY + String.valueOf(mAppWidgetId), null);
	}
	
	public static String loadPasswordPref(Context context, int mAppWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(PREF_PASSWORD_KEY + String.valueOf(mAppWidgetId), null);
	}
	
	public static int loadPeriodPref(Context context, int mAppWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(PREF_PERIOD_KEY + String.valueOf(mAppWidgetId), DEFAULT_REFRESH_TIME);
	}
	
	public static void removeWidgetPref(Context context, int mAppWidgetId){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().remove(PREF_PERIOD_KEY + String.valueOf(mAppWidgetId)).remove(PREF_URL_KEY + String.valueOf(mAppWidgetId)).remove(PREF_USERNAME_KEY + String.valueOf(mAppWidgetId)).remove(PREF_PASSWORD_KEY + String.valueOf(mAppWidgetId)).commit();
	}
}
