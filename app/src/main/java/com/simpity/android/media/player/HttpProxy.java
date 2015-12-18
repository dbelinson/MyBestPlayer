package com.simpity.android.media.player;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;


import com.simpity.android.media.storage.RadioRecord;

import java.io.IOException;

public  class HttpProxy extends Activity implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnCompletionListener {

	 static RadioRecord mRecord;
	 MediaPlayer  mMediaPlayer=null;
	String LOG_TAG="myLog";

	public void init(RadioRecord record){
        mRecord = record;
	}


	public  void start() throws Exception{

		try {
			Log.d(LOG_TAG, "start Stream");
			mMediaPlayer = new MediaPlayer();
			final String DATA_STREAM = "http://online.radiorecord.ru:8101/rr_128";
			mMediaPlayer.setDataSource(DATA_STREAM);
					mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			Log.d(LOG_TAG, "prepareAsync   Stream");
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.prepareAsync();
			Log.d(LOG_TAG, "prepareAsync   Stream2");


		} catch (IllegalArgumentException e) {
			// ...
		} catch (IllegalStateException e) {
			// ...
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d(LOG_TAG, "onCompletion");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMP();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(LOG_TAG, "onPrepared");
		mp.start();
	}

	private void releaseMP() {
		if (mMediaPlayer != null) {
			try {
				mMediaPlayer.release();
				mMediaPlayer = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void stop()throws Exception {
		releaseMP();
	}

	public native static  int connect(String url);
	public native static void close(String url);
	public native static boolean isClosed(String url);

	public native static boolean isTitleUpdated(String url);
	public native static String getTitle(String url);

	public native static String getLocation(String url);

	public native static int getHttpAnswer(String url);


}
