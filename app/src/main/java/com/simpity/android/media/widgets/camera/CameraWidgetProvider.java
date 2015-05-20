package com.simpity.android.media.widgets.camera;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface;

public class CameraWidgetProvider extends AppWidgetProvider {
	
	private final static String TAG = "CameraWidgetProvider";
	private static ICameraWidgetServiceInterface mCameraWidgetServiceInterface = null;
	
	@Override
	public void onEnabled(Context context) {
		Log.i(TAG, "onEnabled");
		super.onEnabled(context);
		context.startService(new Intent(context.getApplicationContext(), CameraWidgetService.class));
		context.getApplicationContext().bindService(new Intent(context.getApplicationContext(), CameraWidgetService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDisabled(Context context) {
		Log.i(TAG, "onDisabled");
		if(mCameraWidgetServiceInterface != null){
			try{
				context.getApplicationContext().unbindService(mConnection);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			mCameraWidgetServiceInterface = null;
		}
		
		while(context.stopService(new Intent(context.getApplicationContext(), CameraWidgetService.class))){
		}
		super.onDisabled(context);
	}
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i(TAG, "onUpdate");
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		if(mCameraWidgetServiceInterface == null){
			context.getApplicationContext().bindService(new Intent(context.getApplicationContext(), CameraWidgetService.class), mConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Log.i(TAG, "onDeleted");
		for (int widgetId : appWidgetIds) {
			CameraWidgetConfiguration.removeWidgetPref(context, widgetId);
			if(mCameraWidgetServiceInterface != null){
				try {
					mCameraWidgetServiceInterface.stopJpegCamera(widgetId);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		super.onDeleted(context, appWidgetIds);
	}
	
	private static final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mCameraWidgetServiceInterface = ICameraWidgetServiceInterface.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");
			mCameraWidgetServiceInterface = null;
		}
	};
	
}
