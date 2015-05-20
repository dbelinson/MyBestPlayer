package com.simpity.android.media.widgets.radio;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface;

public class RadioWidgetProvider extends AppWidgetProvider {

	private final static String TAG = "RadioWidgetProvider";
	private static IRadioWidgetServiceInterface mRadioWidgetServiceInterface = null;
	private static boolean scheduleRequestToService = false;

	//--------------------------------------------------------------------------
	private static final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mRadioWidgetServiceInterface = IRadioWidgetServiceInterface.Stub.asInterface(service);
			if (scheduleRequestToService) {
				try {
					mRadioWidgetServiceInterface.requestWidgetUpdate();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				scheduleRequestToService = false;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");
			mRadioWidgetServiceInterface = null;
			scheduleRequestToService = false;
		}
	};

	//--------------------------------------------------------------------------
	public void onEnabled(Context context) {
		Log.i(TAG, "onEnabled");
		scheduleRequestToService = false;
		super.onEnabled(context);
		context.startService(new Intent(context.getApplicationContext(), RadioWidgetService.class));
		context.getApplicationContext().bindService(
				new Intent(context.getApplicationContext(), RadioWidgetService.class), 
				mConnection, Context.BIND_AUTO_CREATE);
	}

	//--------------------------------------------------------------------------
	public void onDisabled(Context context) {
		Log.i(TAG, "onDisabled");
		scheduleRequestToService = false;
		if (mRadioWidgetServiceInterface != null) {
			try {
				context.getApplicationContext().unbindService(mConnection);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		while (context.stopService(new Intent(context.getApplicationContext(), RadioWidgetService.class))) {
		}
		
		super.onDisabled(context);
	}

	//--------------------------------------------------------------------------
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i(TAG, "onUpdate");
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		if (mRadioWidgetServiceInterface != null) {
			
			try {
				mRadioWidgetServiceInterface.requestWidgetUpdate();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		} else {
			
			scheduleRequestToService = true;
			Log.d(TAG, "onUpdate; Service is NULL; Bind service & schedule request");
			context.getApplicationContext().bindService(
					new Intent(context.getApplicationContext(), RadioWidgetService.class), 
					mConnection, Context.BIND_AUTO_CREATE);
		}
	}
}
