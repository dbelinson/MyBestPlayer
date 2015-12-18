package com.simpity.android.media;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.simpity.android.media.IMediaServiceInterface;
import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.services.RadioService;
import com.simpity.android.media.services.TestLinksService;
import com.simpity.android.media.services.UpdateService;
import com.simpity.android.media.utils.Utilities;

public class MediaService extends Service {

	public final static String ServiceIntent = "com.simpity.android.media.service.BROADCAST";

	public final static String SEND_ACTION = "action";
	public final static String SEND_DATA = "data";

	public final static int ACTION_RADIO_EMPTY				= 0;
	public final static int ACTION_RADIO_STARTING			= 1;
	public final static int ACTION_RADIO_STARTED			= 2;
	public final static int ACTION_RADIO_CONNECTING			= 3;
	public final static int ACTION_RADIO_BUFFERING			= 4;
	public final static int ACTION_RADIO_PLAYING			= 5;
	public final static int ACTION_RADIO_STOPPED			= 6;
	public final static int ACTION_RADIO_RECONNECTING_WAIT	= 7;
	public final static int ACTION_RADIO_RECONNECTING_START	= 8;
	public final static int ACTION_RADIO_ERROR				= 9;
	public final static int ACTION_RADIO_COMPOSITION		= 10;
	public final static int ACTION_RADIO_UPDATE_RECORDS		= 11;
	public final static int ACTION_RADIO_NEW_RECORD			= 12;

	public final static int ACTION_UPDATE_STATE_CHANGED	= 100;

	public final Handler mHandler = new Handler();

	private RadioService mRadioService;
	private UpdateService mUpdateService;
	private TestLinksService mTestLinksService;
	private boolean mDebuggable;

	//-------------------------------------------------------------------------
	@Override
	public void onCreate() {
		super.onCreate();

		//mUpdateService = new UpdateService(this);
		mRadioService = new RadioService(this);
		mTestLinksService = new TestLinksService(this);
		mDebuggable = Utilities.isDebuggable(this);
	}

	//-------------------------------------------------------------------------
	@Override
	public void onDestroy() {

		if (mUpdateService != null) {
			mUpdateService.onDestroy();
			mUpdateService = null;
		}

		if (mRadioService != null) {
			mRadioService.onDestroy();
			mRadioService = null;
		}

		if (mTestLinksService != null) {
			mTestLinksService.onDestroy();
			mTestLinksService = null;
		}

		super.onDestroy();
	};

	//-------------------------------------------------------------------------
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	//-------------------------------------------------------------------------
	private final IMediaServiceInterface.Stub mBinder = new IMediaServiceInterface.Stub() {

		//---------------------------------------------------------------------
		@Override
		public void startNewRadio(String url) throws RemoteException {

			if (mRadioService != null) {
				mRadioService.startNewRadio(url);
			}
		}

		//---------------------------------------------------------------------
		@Override
		public void startRadio(int radio_record_id) throws RemoteException {

			if (mRadioService != null) {
				mRadioService.startRadio(radio_record_id);
			}
		}

		//---------------------------------------------------------------------
		@Override
		public boolean startRadioPlaylist(int playlist_id) throws RemoteException {

			if (mRadioService != null) {
				return mRadioService.startRadioPlaylist(playlist_id);
			}

			return false;
		}

		//---------------------------------------------------------------------
		@Override
		public void stopRadio() throws RemoteException {

			if (mRadioService != null) {
				mRadioService.stopRadio();
			}
		}

		//---------------------------------------------------------------------
		@Override
		public boolean isRadioPlaying() throws RemoteException {

			if (mRadioService != null) {
				return mRadioService.isRadioPlaying();
			}

			return false;
		}

		//---------------------------------------------------------------------
		@Override
		public void nextRadio() throws RemoteException {

			if (mRadioService != null) {
				mRadioService.playNextRadio();
			}
		}

		//---------------------------------------------------------------------
		/*public void stopReconnecting() {
			if (mRadioService != null) {
				mRadioService.stopReconnecting();
			}
		}*/

		//---------------------------------------------------------------------
		@Override
		public boolean isRadioPlaylistPlaying() throws RemoteException {
			return mRadioService != null && mRadioService.isPlaylist();
		}

		//---------------------------------------------------------------------
		@Override
		public int getRadioCurrentAction() throws RemoteException {
			return mRadioService != null ? mRadioService.getActionStage() : ACTION_RADIO_EMPTY;
		}

		//---------------------------------------------------------------------
		@Override
		public String getRadioInfo() throws RemoteException {
			return mRadioService != null ? mRadioService.getCurrentInfo() : null;
		}

		//---------------------------------------------------------------------
		@Override
		public String getRadioComposition() throws RemoteException {
			return mRadioService != null ? mRadioService.getComposition() : null;
		}

		//---------------------------------------------------------------------
		@Override
		public void startCurrentRadio() throws RemoteException {
			if (mRadioService != null) {
				mRadioService.startCurrentRadio();
			}
		}

		//---------------------------------------------------------------------
		@Override
		public int getCurrentRadioId() throws RemoteException {
			return mRadioService != null ? mRadioService.getCurrentRadioId() : -1;
		}
		
		//---------------------------------------------------------------------
		@Override
		public int getUpdateState() throws RemoteException {
			return mUpdateService != null ? mUpdateService.getUpdateState() : 0;
		}

		//---------------------------------------------------------------------
		@Override
		public int getUpdatedLinkCount(int type) throws RemoteException {
			return mUpdateService != null ? mUpdateService.getLinkCount(type) : 0;
		}
	};

	//-------------------------------------------------------------------------
	public void sendToUi(int action) {
		sendToUi(action, null);
	}

	//-------------------------------------------------------------------------
	public void sendToUi(int action, String data) {
		Intent intent = new Intent(VersionConfig.MEDIA_SERVICE_INTENT);
		intent.putExtra(SEND_ACTION, action);
		if (data != null)
			intent.putExtra(SEND_DATA, data);

		sendBroadcast(intent);

		if (mDebuggable) {
			switch (action) {
			case ACTION_RADIO_EMPTY:
				Log.d("sendToUi", "ACTION_RADIO_EMPTY");
				break;

			case ACTION_RADIO_STARTING:
				Log.d("sendToUi", "ACTION_RADIO_STARTING");
				break;

			case ACTION_RADIO_STARTED:
				Log.d("sendToUi", "ACTION_RADIO_STARTED");
				break;

			case ACTION_RADIO_CONNECTING:
				Log.d("sendToUi", "ACTION_RADIO_CONNECTING");
				break;

			case ACTION_RADIO_BUFFERING:
				Log.d("sendToUi", "ACTION_RADIO_BUFFERING");
				break;

			case ACTION_RADIO_PLAYING:
				Log.d("sendToUi", "ACTION_RADIO_PLAYING");
				break;

			case ACTION_RADIO_STOPPED:
				Log.d("sendToUi", "ACTION_RADIO_STOPPED");
				break;

			case ACTION_RADIO_RECONNECTING_WAIT:
				Log.d("sendToUi", "ACTION_RADIO_RECONNECTING_WAIT");
				break;

			case ACTION_RADIO_RECONNECTING_START:
				Log.d("sendToUi", "ACTION_RADIO_RECONNECTING_START");
				break;

			case ACTION_RADIO_ERROR:
				Log.d("sendToUi", "ACTION_RADIO_ERROR");
				break;

			case ACTION_RADIO_COMPOSITION:
				Log.d("sendToUi", "ACTION_RADIO_COMPOSITION");
				break;

			case ACTION_RADIO_UPDATE_RECORDS:
				Log.d("sendToUi", "ACTION_RADIO_UPDATE_RECORDS");
				break;

			case ACTION_UPDATE_STATE_CHANGED:
				Log.d("sendToUi", "ACTION_UPDATE_STATE_CHANGED");
				break;
			}

			if (data != null && data.length() > 0) {
				Log.d("sendToUi data", data);
			}
		}

		if (mTestLinksService != null) {
			switch (action) {
			case ACTION_RADIO_PLAYING:
				mTestLinksService.setEnableTest(true);
				break;
	
			case ACTION_RADIO_STOPPED:
				mTestLinksService.setEnableTest(false);
				break;
			}
		}
	}

	//-------------------------------------------------------------------------
	public void debugInfo(String tag, String info) {
		if (mDebuggable) {
			Log.d(tag, info);
		}
	}
	
	//-------------------------------------------------------------------------
	public boolean isDebuggable() {
		return mDebuggable;
	}
}
