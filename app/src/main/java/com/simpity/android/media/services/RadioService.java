package com.simpity.android.media.services;

import java.util.Scanner;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.simpity.android.media.MediaService;
import com.simpity.android.media.Res;
import com.simpity.android.media.player.HttpProxy;
import com.simpity.android.media.radio.RadioRecordsManager;
import com.simpity.android.media.storage.RadioPlayList;
import com.simpity.android.media.storage.RadioRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.Storage;
import com.simpity.android.media.utils.LinkParser;
import com.simpity.android.media.utils.Utilities;

public class RadioService implements OnPreparedListener,
		OnErrorListener, OnCompletionListener {

	private final static String CURRENT_RADIO_LIST = "CURRENT_RADIO_LIST";
	
	private RadioNotification mNotification = null;

	private MediaPlayer mPlayer = null;
	private String mProxyUrl = null;
	private int mBuffering = 10;
	private int mProxyPort;

	private final LinkParser mLinkParser = new LinkParser();
	private final Vector<LinkParser.MetaInfo> mMetaInfoList = new Vector<LinkParser.MetaInfo>();
	private boolean mIsRadioStarted = false;
	private int mReconnectRetries = 0;
	private boolean mIsRepairing = false;
	private WifiLock mWifiLock = null;
	private LinkParser.MetaInfo mCurrentMetaInfo = null;
	private LinkParser.MetaInfo mRingingStopMetaInfo = null;

	private final Vector<RadioRecord> mRadioRecords = new Vector<RadioRecord>();
	private String mNewUrl = null;
	private int mCurrentUrlNumber = 0;
	private int mActionStage = MediaService.ACTION_RADIO_EMPTY;
	
	private String mComposition = null;
	
	private final MediaService mMediaService; 
	private final RadioRecordsManager mManager;
	
	private Runnable mPostRun = null;
	private ProxyListenerRunnable mProxyListener = null;

	private final Handler mHandler;
	
	//-------------------------------------------------------------------------
	public RadioService(MediaService media_service) {
		
		mMediaService = media_service;
		mHandler = media_service.mHandler;
		
		mManager = new RadioRecordsManager(media_service);

		if (Utilities.getOsVersion() >= 5) {
			mNotification = new RadioNotificationAPI5(mMediaService);
		} else {
			mNotification = new RadioNotificationAPI3(mMediaService);
		}
		
		cancelNotify();

		WifiManager wifi_manager = (WifiManager) mMediaService.getSystemService(Context.WIFI_SERVICE);
		if (wifi_manager != null) {
			mWifiLock = wifi_manager.createWifiLock(WifiManager.WIFI_MODE_FULL, "MediaService");
		}

		try {
			HttpProxy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		TelephonyManager tm = (TelephonyManager)mMediaService.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		String id_list = prefs.getString(CURRENT_RADIO_LIST, null);
		if (id_list != null) {
			int[] id_array = new int[0];
			Scanner scanner = new Scanner(id_list);
			scanner.useDelimiter(";");
			
			while (scanner.hasNext()) {
				try {
					int id = Integer.parseInt(scanner.next());
					int[] new_id_array = new int[id_array.length + 1];
					if (id_array.length > 0) {
						System.arraycopy(id_array, 0, new_id_array, 0, id_array.length);
					}
					new_id_array[id_array.length] = id;
					id_array = new_id_array;
					
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
			
			if (id_array.length > 0) {
				Vector<RecordBase> records = Storage.getRadioRecords(mManager, id_array);
				for (RecordBase record : records) {
					if (record instanceof RadioRecord) {
						mRadioRecords.add((RadioRecord)record);
					}
				}
				
				if (mRadioRecords.size() > 0) {
					mActionStage = MediaService.ACTION_RADIO_STOPPED;
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	public void onDestroy() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		SharedPreferences.Editor editor = prefs.edit(); 
		
		if (mRadioRecords.size() > 0) {
			StringBuilder builder = new StringBuilder();
			for (RecordBase record : mRadioRecords) {
				builder.append(record.getId());
				builder.append(';');
			}
			editor.putString(CURRENT_RADIO_LIST, builder.toString());
		} else {
			editor.remove(CURRENT_RADIO_LIST);
		}
		editor.commit();
		
		stopRadio();

		TelephonyManager tm = (TelephonyManager)mMediaService.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

		wifiRelease();
		//HttpProxy.stop();
	};

	//-------------------------------------------------------------------------
	private class ReleasePlayerRunnable implements Runnable {

		final MediaPlayer mPlayer;
		final String mProxyUrl;
		final Boolean mStopProxy;

		//----------------------------------------------------------------------
		ReleasePlayerRunnable(MediaPlayer player, String proxy_url, boolean stop_proxy) {

			mPlayer = player;
			mProxyUrl = proxy_url;
			mStopProxy = stop_proxy;

			if (mPlayer != null) {
				mPlayer.setOnErrorListener(null);
				mPlayer.setOnPreparedListener(null);
			}

		}

		//----------------------------------------------------------------------
		@Override
		public void run() {

			if (mProxyUrl != null)
				HttpProxy.close(mProxyUrl);

			if (mPlayer != null) {
				try {
					if (mPlayer.isPlaying()) {
						mPlayer.stop();
					}
				} catch (IllegalStateException ex) {
					ex.printStackTrace();
				}

				try {
					mPlayer.reset();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try {
					mPlayer.release();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			if (mStopProxy) {
				HttpProxy.stop();
			}
		}

		//----------------------------------------------------------------------
		Thread start() {
			Thread thread = new Thread(this);
			thread.start();
			return thread;
		}
	}

	//-------------------------------------------------------------------------
	private void wifiAcquire() {
		if (mWifiLock != null && !mWifiLock.isHeld())
			mWifiLock.acquire();
	}
	
	//-------------------------------------------------------------------------
	private void wifiRelease() {
		if (mWifiLock != null && mWifiLock.isHeld())
			mWifiLock.release();
	}
	
	//-------------------------------------------------------------------------
	public boolean startNewRadio(String url) {

		Vector<RecordBase> records = Storage.getRadioRecordsByUrl(mManager, url);
		if (records != null && records.size() > 0) {
			return startRadioRecords(records);
		}
		
		mRadioRecords.clear();
		mNewUrl = url;

		mReconnectRetries = 0;
		if (mIsRadioStarted)
			stopRadio();

		wifiAcquire();

		StringBuilder builder = new StringBuilder();
		builder.append('\n');
		builder.append('\n');
		builder.append('\n');
		builder.append(url);
		builder.append('\n');

		setActionStage(MediaService.ACTION_RADIO_STARTING, builder.toString());

		mMetaInfoList.clear();

		mLinkParser.SetOnEndParseLinkListener(mMetaInfolistener);
		mLinkParser.startParse(url);
		mIsRadioStarted = true;
		
		return false;
	}

	//-------------------------------------------------------------------------
	public void startRadio(final int radio_record_id) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				startRadioRecords(Storage.getRadioRecords(mManager, new int[]{ radio_record_id}));
			}
		}).start();
		
	}

	//-------------------------------------------------------------------------
	public boolean startCurrentRadio() {

		if (mRadioRecords.size() > 0) {
			if (mCurrentUrlNumber >= mRadioRecords.size()) {
				mCurrentUrlNumber = 0;
			}
			
			startRadioRecord(mRadioRecords.get(mCurrentUrlNumber));
			return true;
		}
		
		return false;
	}

	//---------------------------------------------------------------------
	public boolean startRadioPlaylist(int playlist_id) {

		Vector<RecordBase> playlists = Storage.getRadioPlaylists(mManager);

		if (playlists == null || playlists.size() == 0)
			return false;

		Vector<RecordBase> members = null;

		for (RecordBase record : playlists) {
			if (record.getId() == playlist_id) {
				if (record instanceof RadioPlayList) {
					members = ((RadioPlayList)record).getMembers();
				}
				break;
			}
		}
		
		return startRadioRecords(members);
	}
	
	//---------------------------------------------------------------------
	public boolean startRadioRecords(Vector<RecordBase> records) {
		
		if (records == null || records.size() == 0)
			return false;

		mNewUrl = null;
		mRadioRecords.clear();
		for (RecordBase record : records) {
			if (record != null && record instanceof RadioRecord) {
				mRadioRecords.add((RadioRecord)record);
			}
		}

		mCurrentUrlNumber = 0;

		if (mRadioRecords.size() > 0) {
			startRadioRecord(mRadioRecords.get(0));
			return true;
		}
		
		return false;
	}

	//---------------------------------------------------------------------
	public boolean isRadioPlaying() {
		return mPlayer != null && mPlayer.isPlaying();
	}

	//---------------------------------------------------------------------
	public void playNextRadio() {
		if (mRadioRecords.size() > 1) {
			mCurrentUrlNumber = (mCurrentUrlNumber + 1) % mRadioRecords.size();
			startRadioRecord(mRadioRecords.get(mCurrentUrlNumber));
		}
	}

	//-------------------------------------------------------------------------
	private final LinkParser.OnEndParseLinkListener mMetaInfolistener = new LinkParser.OnEndParseLinkListener() {

		@Override
		public void parseCompleted(Vector<LinkParser.MetaInfo> links) {
			if (!mIsRadioStarted) {
				return;
			}

			if (links != null && links.size() > 0) {
				
				if (mNewUrl != null) {
					for (LinkParser.MetaInfo metaInfo : links) {
						if (metaInfo != null && metaInfo.URL != null) {
							RadioRecord record = new RadioRecord(mManager, -1,
									metaInfo.NAME, metaInfo.URL, metaInfo.CONTENT, metaInfo.GENRE, 
									false, -1, 0, false, false, 0);
							record.setLastAccessedDate(System.currentTimeMillis());
							mRadioRecords.add(record);
							mNewUrl = null;
							
							if (Storage.addRecord(mMediaService, record)) {
								
								SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
								boolean is_show_dialog = prefs.getBoolean(mMediaService.getString(Res.string.pref_is_show_share_link_dlg_key), true);
								boolean is_auto_share = prefs.getBoolean(mMediaService.getString(Res.string.pref_is_auto_share_key), false);
								
								if (is_show_dialog || is_auto_share) {
									mMediaService.sendToUi(MediaService.ACTION_RADIO_NEW_RECORD, getMetaInfo(metaInfo));
								} else {
									mMediaService.sendToUi(MediaService.ACTION_RADIO_UPDATE_RECORDS);
								}
								
								/*final String name = data.get(LinkParser.MetaInfo.INDEX_NAME);
								String content = data.get(LinkParser.MetaInfo.INDEX_CONTENT);
								final String genre = data.get(LinkParser.MetaInfo.INDEX_GENRE);
	
								boolean isNewLink = mRecordsManager.add(name, address, content, genre, false, -1, Calendar.getInstance().getTimeInMillis(), false, false);
	
								boolean is_show_dlg = PreferenceManager.getDefaultSharedPreferences(RadioListActivity.this).getBoolean(getString(Res.string.pref_is_show_share_link_dlg_key), true);
								boolean is_auto_share = PreferenceManager.getDefaultSharedPreferences(RadioListActivity.this).getBoolean(getString(Res.string.pref_is_auto_share_key), false);
								if ((is_show_dlg || is_auto_share) && isNewLink) {
									mShareLinkDialog = new ShareLinkDialog(RadioListActivity.this, RadioListActivity.this.getString(Res.string.radio), Res.drawable.icon_radio);
									if (is_show_dlg) {
										mShareLinkDialog.setOnCancelListener(new Dialog.OnCancelListener() {
												@Override
												public void onCancel(DialogInterface dialog) {
													if (mShareLinkDialog.IsDialogResultYes) {
														mShareLinkDialog.PostALink(address, RadioListActivity.this.getString(Res.string.radio), genre, name, "-");
													}
												}
										});
										mShareLinkDialog.show();
									} else {
										mShareLinkDialog.PostALink(address, RadioListActivity.this.getString(Res.string.radio), genre, name, "-");
									}
								}*/
							}
							
							break;
						}
					}
				} else if (mCurrentUrlNumber >= 0 && mCurrentUrlNumber < mRadioRecords.size()) {
					
					RadioRecord record = (RadioRecord)mRadioRecords.get(mCurrentUrlNumber);
					if (record.isNewLink()) {
						record.setNewState(false);
					}
					if (record.isDeadLink()) {
						record.setLinkDeadState(false);
					}
					
					record.setLastAccessedDate(System.currentTimeMillis());
					Storage.updateRecord(mMediaService, record);
					mMediaService.sendToUi(MediaService.ACTION_RADIO_UPDATE_RECORDS);
				}
				
				if (mCurrentMetaInfo != null) {
					links.remove(mCurrentMetaInfo);
				}

				mMetaInfoList.addAll(links);
				if (mCurrentMetaInfo == null) {
					for (LinkParser.MetaInfo metaInfo : links) {
						if (metaInfo != null && metaInfo.URL != null) {
							setupPlayer(metaInfo);
							return;
						}
					}

					wifiRelease();
					setActionStage(MediaService.ACTION_RADIO_ERROR, null);
				}

			} else {

				mMetaInfoList.clear();
				wifiRelease();

				setActionStage(MediaService.ACTION_RADIO_ERROR, null);
			}
		}

		@Override
		public void onGetFirstLink(LinkParser.MetaInfo firstLink) {
			if (!mIsRadioStarted) {
				return;
			}

			mCurrentMetaInfo = firstLink;
			if (firstLink != null && firstLink.URL != null) {
				mMetaInfoList.clear();
				mMetaInfoList.add(firstLink);
				setupPlayer(firstLink);
			}
		}
	};

	//--------------------------------------------------------------------------
	private class ProxyBuffering implements Runnable {

		private final LinkParser.MetaInfo mMetaInfo;
		private int mBufferingTime = 0;
		private int mHttpAnswer = 0;

		ProxyBuffering (LinkParser.MetaInfo metadata) {
			mMetaInfo = metadata;
			//sendBufferingToUi();
		}

		@Override
		public void run() {
			
			mPostRun = null;
			if (mMetaInfo == mCurrentMetaInfo) {
				
				if (mHttpAnswer == 0) {
					mHttpAnswer = HttpProxy.getHttpAnswer(mProxyUrl);
				
					switch (mHttpAnswer) { 
					case 0:
						post(this, 250);
						break;
						
					case 200:
						sendBufferingToUi();
						post(this, 1000);
						break;

					case 301: case 302: 
						if (mProxyUrl != null) {
							String new_url = HttpProxy.getLocation(mProxyUrl);
							if (new_url != null) {
								
								LinkParser.MetaInfo info = new LinkParser.MetaInfo(new_url,
										mMetaInfo.NAME, mMetaInfo.GENRE, mMetaInfo.CONTENT,
										mMetaInfo.PROVIDER_URL);

								setupPlayer(info);
								break;
							}
						}
						// goto default:
					
					default:
						if (mProxyUrl != null) {
							HttpProxy.close(mProxyUrl);
							mProxyUrl = null;
						}
						reconnect();
					} 

					return;
				}
				
				mBufferingTime++;
				if (mBufferingTime >= mBuffering) {

					if (startPlayer()) {

						mProxyListener = new ProxyListenerRunnable(mMetaInfo.URL, mPlayer);
						mProxyListener.start();

					} else {

						if (mProxyUrl != null) {
							HttpProxy.close(mProxyUrl);
							mProxyUrl = null;
						}

						reconnect();
					}

				} else {

					post(this, 1000);
					sendBufferingToUi();
				}
			}
		}

		private void sendBufferingToUi() {
			StringBuilder builder = new StringBuilder();
			builder.append(mBufferingTime);
			setActionStage(MediaService.ACTION_RADIO_BUFFERING, builder.toString());
		}
	}

	//--------------------------------------------------------------------------
	public void startRadioRecord(RadioRecord record) {

		mReconnectRetries = 0;
		if (mIsRadioStarted)
			stopRadio();

		wifiRelease();

		StringBuilder builder = new StringBuilder();
		builder.append(record.getStationName());
		builder.append('\n');
		builder.append(record.getGenre());
		builder.append('\n');
		builder.append(record.getContentDescription()); 
		builder.append('\n');
		builder.append(record.getUrl());
		builder.append('\n');

		setActionStage(MediaService.ACTION_RADIO_STARTING, builder.toString());

		mMetaInfoList.clear();

		mLinkParser.SetOnEndParseLinkListener(mMetaInfolistener);
		mLinkParser.startParse(record.getUrl());
		mIsRadioStarted = true;

	}

	//---------------------------------------------------------------------
	public void stopRadio() {

		if (mPostRun != null) {
			mHandler.removeCallbacks(mPostRun);
			mPostRun = null;
		}

		if (mProxyListener != null) {
			mHandler.removeCallbacks(mProxyListener);
			mProxyListener = null;
		}

		mIsRadioStarted = false;
		mMetaInfoList.clear();

		mLinkParser.RemoveOnEndParseLinkListener(mMetaInfolistener);
		mLinkParser.terminateParseLink();

		destroyPlayer(false);

		cancelNotify();
		setActionStage(MediaService.ACTION_RADIO_STOPPED, null);
	}

	//--------------------------------------------------------------------------
	private boolean startPlayer() {

		if (mCurrentMetaInfo == null)
			return false;

		String url;

		if (mProxyUrl != null) {

			url = "http://localhost:" + mProxyPort;

			int pos = mProxyUrl.indexOf('/', 7);
			if (pos > 0) {
				url += mProxyUrl.substring(pos);
			}

		} else {

			url = mCurrentMetaInfo.URL;
		}

		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		try {
			mPlayer.setDataSource(url);
			mPlayer.prepareAsync();

			setActionStage(MediaService.ACTION_RADIO_STARTED, getCurrentInfo());

		} catch (Exception ex) {

			ex.printStackTrace();

			try {
				mPlayer.release();
			} catch (Exception ex2) {
			}
			mPlayer = null;
			return false;
		}

		return true;
	}

	//--------------------------------------------------------------------------
	private void destroyPlayer(boolean stop_proxy) {

		wifiRelease();

		if (mPlayer != null || mProxyUrl != null) {
			(new ReleasePlayerRunnable(mPlayer, mProxyUrl, stop_proxy)).start();
			mPlayer = null;
			mProxyUrl = null;
		} else {
			HttpProxy.stop();
		}
	}

	//--------------------------------------------------------------------------
	private void setupPlayer(LinkParser.MetaInfo metadata) {
		if (metadata == null) {
			wifiRelease();
			return;
		}

		if (metadata.URL != null) {

			destroyPlayer(false);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
			String key = mMediaService.getString(Res.string.pref_radio_buffering_key);
			String value = prefs.getString(key, "10");
			try {
				mBuffering = Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				ex.printStackTrace();
			}

			mCurrentMetaInfo = metadata;

			setActionStage(MediaService.ACTION_RADIO_CONNECTING, null);

			if (metadata.URL.substring(0, 7).equalsIgnoreCase("http://")) {
				mProxyPort = HttpProxy.connect(metadata.URL);
				if (mProxyPort > 0) {
					mProxyUrl = metadata.URL;
					post(new ProxyBuffering(metadata), 1000);
					return;
				}
			}

	        mProxyUrl = null;
			if (!startPlayer()) {
				mCurrentMetaInfo = null;
			}

		} else {

			wifiRelease();
			setActionStage(MediaService.ACTION_RADIO_ERROR, null);
			mCurrentMetaInfo = null;
		}
    }

	//-------------------------------------------------------------------------
	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mIsRadioStarted) {

			mp.start();
			mReconnectRetries = 0;
        	mIsRepairing = false;
			ongoingNotify(mCurrentMetaInfo.NAME, 
					mMediaService.getString(Res.string.app_name) + " - " + 
					mMediaService.getString(Res.string.radio), 
					Res.drawable.icon_radio);		
			
			setActionStage(MediaService.ACTION_RADIO_PLAYING, null);
			/*post(new Runnable() {
				@Override
				public void run() {
					mMediaService.mHandler.removeCallbacks(this);
					if (mIsRadioStarted && mPlayer != null) {
						// TODO
						if (!mPlayer.isPlaying()) {
							mPostRun = null;
							//repairConnect();
							// TODO
						} else {
							post(this, 500);
						}
					}
				}
			}, 500);*/
		}
	}

	//-------------------------------------------------------------------------
	private final class ReconnectingWait implements Runnable {
		
		private int mWaitTime;
		private final int mPeriod;
		
		ReconnectingWait() {
			int period = 3000;
			try {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
				String key = mMediaService.getString(Res.string.pref_radio_reconnect_period_key);
				period = Integer.valueOf(prefs.getString((key), "3000"));
			} catch (Exception ex) {
				
			}
			
			mPeriod = period;
			mWaitTime = 0;
			sendTimeToUi();
		}

		//----------------------------------------------------------------------
		@Override
		public void run() {
			mPostRun = null;
			mWaitTime += 1000;
			if (mWaitTime < mPeriod) {
				sendTimeToUi();
				post(this, 1000);
				return;
			}
			
			setActionStage(MediaService.ACTION_RADIO_RECONNECTING_START, null);
			startPlayer();
		}
		
		//----------------------------------------------------------------------
		private void sendTimeToUi() {
			String sec = Integer.toString((mPeriod - mWaitTime) / 1000);
			
			ongoingNotify(mMediaService.getString(Res.string.reconnect_delay) + ' ' + sec + 's',
					mMediaService.getString(Res.string.app_name) + " - "
							+ mMediaService.getString(Res.string.radio),
					Res.drawable.icon_radio_red);
			
			setActionStage(MediaService.ACTION_RADIO_RECONNECTING_WAIT, sec);
		}
	}
	
	//-------------------------------------------------------------------------
	private boolean reconnect() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		boolean is_reconnect = prefs.getBoolean(mMediaService.getString(Res.string.pref_radio_reconnect_on_disconnect_key), true);
		int max_retries = Integer.valueOf(prefs.getString(mMediaService.getString(Res.string.pref_radio_reconnect_retries_key), "5"));
		
		if (is_reconnect && mIsRadioStarted && mReconnectRetries < max_retries) {
			mReconnectRetries++;
			if (!mIsRepairing) {
				wifiAcquire();
				post(new ReconnectingWait(), 1000);
				mIsRepairing = true;
			}
			
			return true;

		} else {

			wifiRelease();
			setActionStage(MediaService.ACTION_RADIO_ERROR, null);
			return false;
		}
	}
	
	//-------------------------------------------------------------------------
	private boolean isUseReconnect() {
		return PreferenceManager.getDefaultSharedPreferences(mMediaService).getBoolean(
				mMediaService.getString(Res.string.pref_radio_reconnect_on_disconnect_key), true);
	}

	//-------------------------------------------------------------------------
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {

		destroyPlayer(false);
		reconnect();
		return true;
	}

	//-------------------------------------------------------------------------
	@Override
	public void onCompletion(MediaPlayer mp) {

		destroyPlayer(false);

		int index = mMetaInfoList.indexOf(mCurrentMetaInfo);
		int meta_list_size = mMetaInfoList.size();

		if (index >= 0 && index < meta_list_size-1) {
			index++;
		} else {
			index = isUseReconnect() ? 0 : -1;
		}

		if (index >= 0 && index < meta_list_size) {
			setupPlayer(mMetaInfoList.get(index));
			return;
		}

		/*MetaInfo next_metadata = null;
		boolean foundFlag = false;

		for (MetaInfo metadata : mMetaInfoList) {
			String candidateUrl = metadata.URL;
			if (mCurrentMetaInfo.URL.equalsIgnoreCase(candidateUrl)) {

				foundFlag = true;

			} else if (foundFlag) {

				setupPlayer(metadata);
				return;
			}
		}*/

		wifiRelease();
	}

	//-------------------------------------------------------------------------
	private class ProxyListenerRunnable implements Runnable {

		private final String mUrl;
		private final MediaPlayer mPlayer;
		private String mTitle = null;

		ProxyListenerRunnable(String url, MediaPlayer player) {
			mUrl = url;
			mPlayer = player;
		}

		//---------------------------------------------------------------------
		@Override
		public void run() {

			if (mPlayer == RadioService.this.mPlayer) {
				if (mTitle != null) {
					mComposition = mTitle.trim();
					mMediaService.sendToUi(MediaService.ACTION_RADIO_COMPOSITION, mComposition);
					ongoingNotify(mComposition, mCurrentMetaInfo.NAME, Res.drawable.icon_radio);
					mTitle = null;
				}

				if (HttpProxy.isTitleUpdated(mUrl)) {
					mTitle = HttpProxy.getTitle(mUrl);
				}

				start();
			}
		}

		//---------------------------------------------------------------------
		void start() {
			mHandler.postDelayed(this, 1000);
		}
	}

	//--------------------------------------------------------------------------
	private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch(state) {
			case TelephonyManager.CALL_STATE_IDLE:
				if (mRingingStopMetaInfo != null) {
					setupPlayer(mRingingStopMetaInfo);
					mRingingStopMetaInfo = null;
				}
				break;

			case TelephonyManager.CALL_STATE_RINGING:
				if (mPlayer != null || mProxyUrl != null) {
					mRingingStopMetaInfo = mCurrentMetaInfo;
					destroyPlayer(false);
					mCurrentMetaInfo = null;
				}
				break;
			}
		}
	};

	//---------------------------------------------------------------------
	private String getMetaInfo(LinkParser.MetaInfo info) {
		
		if (info == null) {
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(info.URL);
		builder.append('\n');
		builder.append(info.NAME);
		builder.append('\n');
		builder.append(info.GENRE);
		builder.append('\n');
		builder.append(info.CONTENT);
		builder.append('\n');
		builder.append(info.PROVIDER_URL);
		builder.append('\n');

		return builder.toString();
	}
	
	//---------------------------------------------------------------------
	public String getCurrentInfo() {
		return getMetaInfo(mCurrentMetaInfo);
	}
	
	//---------------------------------------------------------------------
	private void cancelNotify() {
		mNotification.cancelNotify();
	}
	
	//---------------------------------------------------------------------
	private void ongoingNotify(String msg, String title, int drawableID) {
		mNotification.ongoingNotify(msg, title, drawableID);
	}
	
	//---------------------------------------------------------------------
	private void setActionStage(int stage, String info) {
		
		if (stage != MediaService.ACTION_RADIO_COMPOSITION) {
			mActionStage = stage;
			if (stage == MediaService.ACTION_RADIO_ERROR) {
				cancelNotify();
			}
			mComposition = null;
		}
			
		mMediaService.sendToUi(stage, info);
	}
	
	//---------------------------------------------------------------------
	public int getActionStage() {
		return mActionStage;
	}

	//---------------------------------------------------------------------
	public String getComposition() {
		return mComposition;
	}
	
	//---------------------------------------------------------------------
	public boolean isPlaylist() {
		return mRadioRecords.size() > 1;
	}
	
	//---------------------------------------------------------------------
	public int getCurrentRadioId() {
		return mCurrentUrlNumber >= 0 && mCurrentUrlNumber < mRadioRecords.size() ?
				mRadioRecords.get(mCurrentUrlNumber).getId() : -1;
	}
	
	//---------------------------------------------------------------------
	private final void post(Runnable r, long delay) {
		mHandler.postDelayed(r, delay);
		mPostRun = r;
	}
}
