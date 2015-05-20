package com.simpity.android.media.widgets.camera;

import java.util.Vector;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.simpity.android.media.Res;
import com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface;
import com.simpity.android.media.widgets.camera.WebCamRefreshService.CameraInfo;

public class CameraWidgetService extends Service implements WebCamRefreshService.UpdateListener {

	private final String TAG = "CameraWidgetService";
	
	private BroadcastReceiver configChangedReceiver = null;
	private BroadcastReceiver mWidgetConfigurationReceiver = null;
	
	private Vector<WidgetCameraInfo> mCameraWidgets = null;
	
	public class WidgetCameraInfo extends CameraInfo{
		
		public int widgetID;
		
		public WidgetCameraInfo(int widgetId, String url, int refreshTime, String username, String password){
			WebCamRefreshService.getServiceInstance().super(url, refreshTime, username, password);
			this.widgetID = widgetId;
		}
	}
	
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			int[] widgetIDs = AppWidgetManager.getInstance(CameraWidgetService.this).getAppWidgetIds(new ComponentName(CameraWidgetService.this, CameraWidgetProvider.class));
			for (int widgetID : widgetIDs) {
				String url = CameraWidgetConfiguration.loadURLPref(CameraWidgetService.this, widgetID);
				int period = CameraWidgetConfiguration.loadPeriodPref(CameraWidgetService.this, widgetID);
				if(url != null){
					try {
						mBinder.startJpegCamera(
								widgetID, 
								url, 
								period, 
								CameraWidgetConfiguration.loadUsernamePref(CameraWidgetService.this, widgetID),
								CameraWidgetConfiguration.loadPasswordPref(CameraWidgetService.this, widgetID));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
			registerReceiver(mWidgetConfigurationReceiver, new IntentFilter(CameraWidgetConfiguration.ACTION_WIDGET_CONFIGURED));
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
		}
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		
		Log.i(TAG, "onCreate");
		super.onCreate();
		
		mCameraWidgets = new Vector<WidgetCameraInfo>();
		
		bindService(new Intent(this, WebCamRefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		configChangedReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG, "configChangedReceiver.onReceive");
				for (WidgetCameraInfo info : mCameraWidgets) {
					updateWidget(info.widgetID);
				}
			}
		};
		registerReceiver(configChangedReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		
		mWidgetConfigurationReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
				String url = intent.getStringExtra(CameraWidgetConfiguration.URL_KEY);
				String refreshTime = intent.getStringExtra(CameraWidgetConfiguration.PERIOD_KEY);
				try {
					mBinder.startJpegCamera(widgetId, url, Integer.valueOf(refreshTime), intent.getStringExtra(CameraWidgetConfiguration.USERNAME_KEY), intent.getStringExtra(CameraWidgetConfiguration.PWD_KEY));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}; 
		
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		for (WidgetCameraInfo wInfo : mCameraWidgets) {
			try {
				mBinder.stopJpegCamera(wInfo.widgetID);
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
		
		try {
			unbindService(mConnection);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		if (configChangedReceiver != null) {
			try {
				unregisterReceiver(configChangedReceiver);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (mWidgetConfigurationReceiver != null) {
			try {
				unregisterReceiver(mWidgetConfigurationReceiver);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		super.onDestroy();
	}
	
	private WidgetCameraInfo getCameraWidgetInfo(int widgetId){
		for (WidgetCameraInfo info : mCameraWidgets) {
			if(info.widgetID == widgetId){
				return info;
			}
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------------------------
	private PendingIntent getPendingIntentForConfigure(int widgetId){
		Intent configureIntent = new Intent(this, CameraWidgetConfiguration.class);
		configureIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
		configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		WidgetCameraInfo wInfo = getCameraWidgetInfo(widgetId);
		if(wInfo != null){
			configureIntent.putExtra(CameraWidgetConfiguration.URL_KEY, wInfo.url);
			configureIntent.putExtra(CameraWidgetConfiguration.PERIOD_KEY, String.valueOf(wInfo.refreshPeriod));
			configureIntent.putExtra(CameraWidgetConfiguration.USERNAME_KEY, wInfo.userName);
			configureIntent.putExtra(CameraWidgetConfiguration.PWD_KEY, wInfo.password);
		}
		configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		configureIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, widgetId, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntent;
	}
	
	private void setConfigurePendingIntent(RemoteViews views, int widgetId){
		views.setOnClickPendingIntent(Res.id.widgetContainer, getPendingIntentForConfigure(widgetId));
	}
	
	private void updateWidget(int widgetId){
		AppWidgetManager appMidgetMngr = AppWidgetManager.getInstance(CameraWidgetService.this);
		RemoteViews views = new RemoteViews(CameraWidgetService.this.getPackageName(), Res.layout.webcam_widget_layout);
		setConfigurePendingIntent(views, widgetId);
		WidgetCameraInfo widgetInfo = getCameraWidgetInfo(widgetId);
		if(widgetInfo != null){
			views.setImageViewBitmap(Res.id.ImageView, Bitmap.createScaledBitmap(widgetInfo.lastImage, widgetInfo.lastImage.getWidth()/2, widgetInfo.lastImage.getHeight()/2, true));
		}
		appMidgetMngr.updateAppWidget(widgetId, views);
	}
	
	//-------------------------------------------------------------------------
	private final ICameraWidgetServiceInterface.Stub mBinder = new ICameraWidgetServiceInterface.Stub() {
		
		@Override
		public void startJpegCamera(int widgetId, String url, int refreshTime, String username, String password)
				throws RemoteException {
			Log.i(TAG, "startJpegCamera");
			WidgetCameraInfo wInfo = getCameraWidgetInfo(widgetId);
			if(wInfo == null){
				wInfo = new WidgetCameraInfo(widgetId, url, refreshTime, username, password);
				mCameraWidgets.add(wInfo);
			}else{
				wInfo.refreshPeriod = refreshTime;
				wInfo.userName = username;
				wInfo.password = password;
				if(!wInfo.url.equalsIgnoreCase(url)){
					wInfo.url = url;
					wInfo.lastImage = BitmapFactory.decodeResource(getResources(), Res.drawable.icon_jpeg);
				}
			}
			WebCamRefreshService.getServiceInstance().startRefreshCamera(wInfo, CameraWidgetService.this);
			updateWidget(widgetId);
		}

		@Override
		public void stopJpegCamera(int widgetId) throws RemoteException {
			Log.i(TAG, "stopJpegCamera");
			WidgetCameraInfo wInfo = getCameraWidgetInfo(widgetId);
			WebCamRefreshService.getServiceInstance().stopRefreshCamera(wInfo);
			mCameraWidgets.remove(wInfo);
		}
	};

	@Override
	public void OnUpdated(CameraInfo camera) {
		if(camera != null && camera instanceof WidgetCameraInfo){
			updateWidget(((WidgetCameraInfo)camera).widgetID);
		}
	}

	@Override
	public void onErrorOccurs(CameraInfo camera, Exception exc) {
		// TODO Auto-generated method stub
		
	}
}
