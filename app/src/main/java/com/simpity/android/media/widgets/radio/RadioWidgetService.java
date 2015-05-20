package com.simpity.android.media.widgets.radio;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.simpity.android.media.IMediaServiceInterface;
import com.simpity.android.media.MediaService;
import com.simpity.android.media.Res;
import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.radio.RadioSelectActivity;
import com.simpity.android.media.services.RadioNotification;
import com.simpity.android.media.utils.LinkParser;
import com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface;

public class RadioWidgetService extends Service {

	private final String TAG = "RadioWidgetService";
	
	public  final static String WIDGET_ACTIONS = "com.psa.android.media.widgets.radio.actions";
	public  final static String ACTION_KEY = "ACTION";
	
	private final static String ACTION_START	= "ACTION_START";
	private final static String ACTION_STOP	= "ACTION_STOP";
	private final static String ACTION_NEXT	= "ACTION_NEXT";
	
	private final static int START_CODE	= 10;
	private final static int STOP_CODE	= 20;
	private final static int NEXT_CODE	= 30;
	private final static int INFO_CODE	= 40;
	
	private IMediaServiceInterface mRadioServiceInterface = null;
	
	private String mStation = "";
	private String mTitle = "";
	
	//--------------------------------------------------------------------------
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRadioServiceInterface = IMediaServiceInterface.Stub.asInterface(service);
			
			boolean playbackState = false, is_playlist = false;
			try {
				playbackState = mRadioServiceInterface.isRadioPlaying();
				is_playlist = mRadioServiceInterface.isRadioPlaylistPlaying();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
			updateWidget(playbackState, is_playlist);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mRadioServiceInterface = null;
		}
	};
	
	//--------------------------------------------------------------------------
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onCreate() {
		
		Log.i(TAG, "onCreate");
		super.onCreate();
		
		bindService(new Intent(this, MediaService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		registerReceiver(mConfigChangedReceiver,  new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		registerReceiver(mMediaServiceReceiver,   new IntentFilter(VersionConfig.MEDIA_SERVICE_INTENT));
		registerReceiver(mWidgetMessagesReceiver, new IntentFilter(WIDGET_ACTIONS));
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		if (mRadioServiceInterface != null){
			try {
				unbindService(mConnection);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		try {
			unregisterReceiver(mConfigChangedReceiver);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			unregisterReceiver(mMediaServiceReceiver);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			unregisterReceiver(mWidgetMessagesReceiver);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		super.onDestroy();
	}
	
	//-------------------------------------------------------------------------
	private final IRadioWidgetServiceInterface.Stub mBinder = new IRadioWidgetServiceInterface.Stub() {
		@Override
		public void requestWidgetUpdate() throws RemoteException {
			Log.d(TAG, "requestWidgetUpdate");
			
			boolean playbackState = false, is_playlist = false;
			if (mRadioServiceInterface != null) {
				try {
					playbackState = mRadioServiceInterface.isRadioPlaying();
					is_playlist = mRadioServiceInterface.isRadioPlaylistPlaying();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}				
			
			updateWidget(playbackState, is_playlist);
		}
	};

	//--------------------------------------------------------------------------
	private void setPlayStopPendingIntent(RemoteViews views, boolean isPlaying){
		
		Intent playOrStopIntent = new Intent(WIDGET_ACTIONS);
		int request_code;
		
		if (!isPlaying) {
			playOrStopIntent.putExtra(ACTION_KEY, ACTION_START);
			request_code = START_CODE;
		} else {
			playOrStopIntent.putExtra(ACTION_KEY, ACTION_STOP);
			request_code = STOP_CODE;
		}
		
		views.setOnClickPendingIntent(Res.id.WidgetPlayerPlay, 
				PendingIntent.getBroadcast(this, request_code, playOrStopIntent, 
						PendingIntent.FLAG_UPDATE_CURRENT));
		views.setImageViewResource(Res.id.WidgetPlayerPlay, 
				isPlaying ? Res.drawable.pause : Res.drawable.play);
	}
	
	//-------------------------------------------------------------------------
	private void updateWidget(boolean playbackState, boolean is_playlist) {
		AppWidgetManager app_widget_manager = AppWidgetManager.getInstance(this);
		RemoteViews views = new RemoteViews(getPackageName(), Res.layout.radio_widget_layout);
		
		setPlayStopPendingIntent(views, playbackState);
		
		views.setCharSequence(Res.id.WidgetRadioStation, "setText", mStation);
		views.setCharSequence(Res.id.WidgetRadioTitle, "setText", mTitle);
		views.setViewVisibility(Res.id.WidgetPlayerNext, is_playlist ? View.VISIBLE : View.GONE);

		Intent intent = new Intent(getApplicationContext(), RadioSelectActivity.class);
		intent.setAction(RadioNotification.ACTION_BRING_TO_FRONT);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		
		views.setOnClickPendingIntent(Res.id.WidgetRadioInfo, 
				PendingIntent.getActivity(this, INFO_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		
		intent = new Intent(WIDGET_ACTIONS);
		intent.putExtra(ACTION_KEY, ACTION_NEXT);
		
		views.setOnClickPendingIntent(Res.id.WidgetPlayerNext, 
				PendingIntent.getBroadcast(this, NEXT_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		
		app_widget_manager.updateAppWidget(new ComponentName(RadioWidgetService.this, RadioWidgetProvider.class), views);
	}
	
	//-------------------------------------------------------------------------
	private final BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "mConfigChangedReceiver.onReceive");
			
			try {
				mBinder.requestWidgetUpdate();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	//-------------------------------------------------------------------------
	private final BroadcastReceiver mMediaServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "mMediaServiceReceiver.onReceive");
			
			try {
				String info = intent.getStringExtra(MediaService.SEND_DATA);

				switch (intent.getIntExtra(MediaService.SEND_ACTION, 0)) {
				
				case MediaService.ACTION_RADIO_PLAYING:
					mTitle = getString(Res.string.playing);
					break;

				case MediaService.ACTION_RADIO_STARTED:
					if (info != null) {
						String[] data = info.split("\n");
						if (data != null && data.length > LinkParser.MetaInfo.INDEX_NAME) {
							mStation = data[LinkParser.MetaInfo.INDEX_NAME];
						}
					}
					break;

				case MediaService.ACTION_RADIO_ERROR:
					mTitle = getString(Res.string.error);
					break;
						
				case MediaService.ACTION_RADIO_STOPPED:
					mTitle = getString(Res.string.stopped);
					break;
						
				case MediaService.ACTION_RADIO_STARTING:
					mTitle = getString(Res.string.starting);
					mStation = "";
					if (info != null) {
						String[] data = info.split("\n");
						if (data != null && data.length > 0) {
							mStation = data[0];
						}
					}
					break;
					
				case MediaService.ACTION_RADIO_RECONNECTING_WAIT:
					mTitle = getString(Res.string.reconnect_delay);
					if (info != null) {
						mTitle = mTitle + ' ' + info + "s";
					}
					break;
						
				case MediaService.ACTION_RADIO_RECONNECTING_START:
					mTitle = getString(Res.string.reconnecting);
					break;
						
				case MediaService.ACTION_RADIO_COMPOSITION:
					if (info != null && info.length() > 0) {
						mTitle = info;
					} else {
						mTitle = "";
					}
					break;
						
				case MediaService.ACTION_RADIO_BUFFERING:
					mTitle = getString(Res.string.buffering);
					if (info != null) {
						mTitle = mTitle + ' ' + info + "s";
					}
					break;
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				mBinder.requestWidgetUpdate();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	//-------------------------------------------------------------------------
	private final BroadcastReceiver mWidgetMessagesReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "widgetMessagesReceiver.onReceive");
			
			String intent_action = intent.getAction();
			String widget_action = intent.getStringExtra(ACTION_KEY);
			
			if (mRadioServiceInterface != null 
					&& intent_action != null 
					&& widget_action != null
					&& intent_action.equalsIgnoreCase(WIDGET_ACTIONS)) {
				
				try {
					if (ACTION_START.equalsIgnoreCase(widget_action)) {

						mRadioServiceInterface.startCurrentRadio();

					} else if (ACTION_STOP.equalsIgnoreCase(widget_action)) {

						mRadioServiceInterface.stopRadio();
						
					} else if (ACTION_NEXT.equalsIgnoreCase(widget_action)) {
	
						mRadioServiceInterface.nextRadio();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};

}
