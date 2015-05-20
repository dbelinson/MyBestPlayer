package com.simpity.android.media.services;

import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.simpity.android.media.MediaService;
import com.simpity.android.media.storage.LinkTestThread;

public class TestLinksService implements LinkTestThread.TestListener {

	private final static String TAG = "TestLinksService"; 
	
	private final static String LAST_TEST_DATE_KEY = "LAST_TEST_DATE"; 
	private final static long TEST_PERIOD = 8*60*60*1000; 
	
	private final MediaService mMediaService;
	private boolean mNeedTest = false;
	private boolean mEnableTest = false;
	//private TestTimerTask mTimerTask = null;
	private Timer mTimer = new Timer();
	private LinkTestThread mTestThread = null;

	//--------------------------------------------------------------------------
	private class TestTimerTask extends TimerTask {
		@Override
		public void run() {
			if (mEnableTest) {
				startTest();
			} else {
				mNeedTest = true;
			}
		}
	}
	
	//--------------------------------------------------------------------------
	public TestLinksService(MediaService media_service) {
		mMediaService = media_service;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(media_service);
		long last_test = prefs.getLong(LAST_TEST_DATE_KEY, 0);
		long current_time = System.currentTimeMillis();
		
		mNeedTest = (last_test + TEST_PERIOD) <= current_time;
		if (!mNeedTest) {
			startTimer((last_test + TEST_PERIOD) - current_time);
		}
	}

	//--------------------------------------------------------------------------
	public void onDestroy() {
		if (mTimer != null) {
			mTimer.purge();
			mTimer = null;
		}
		
		if (mTestThread != null) {
			mTestThread.terminate();
			mTestThread = null;
		}
	}
	
	//--------------------------------------------------------------------------
	public void setEnableTest(boolean enable_test) {
		if (mNeedTest) {
			startTest();
		} else {
			mEnableTest = enable_test;
		}
	}

	//--------------------------------------------------------------------------
	private void startTimer(long delay) {
		try {
			mTimer.schedule(new TestTimerTask(), delay);
		} catch (Exception ex) {
			mNeedTest = true;
		}
	}
	
	//--------------------------------------------------------------------------
	private void startTest() {
		
		if (mTestThread != null) {
			mTestThread.terminate();
		}
		
		mMediaService.debugInfo(TAG, "Links test was started");
		mTestThread = LinkTestThread.start(mMediaService, this);
		mMediaService.debugInfo(TAG, "Links test was finished");
	}

	//--------------------------------------------------------------------------
	@Override
	public void onLinkTestFinished() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(LAST_TEST_DATE_KEY, System.currentTimeMillis());
		editor.commit();
		startTimer(TEST_PERIOD);
	}
}
