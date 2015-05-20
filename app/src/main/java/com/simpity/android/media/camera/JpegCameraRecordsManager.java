package com.simpity.android.media.camera;

import java.util.Collections;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.storage.CameraRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;

public class JpegCameraRecordsManager extends RecordsManager {

	@SuppressWarnings("unused")
	private final String TAG = "JpegCameraRecordsManager";

	private final static String COUNT_KEY = "Jpeg URL Count";
	private final static String IS_FIRST_RUN = "JPEG_FIRST_RUN";

	//--------------------------------------------------------------------------
	private class JpegUrl {
		private final static String URL_KEY = "Jpeg URL ";
		private final static String DESCRIPTION_KEY = "Jpeg Description ";
		private final static String REFRESH_KEY = "Jpeg Refresh ";

		String mDescription;
		String mUrl;
		int mRefresh;

		//----------------------------------------------------------------------
		JpegUrl(SharedPreferences preferences, int number) {
			mDescription = preferences.getString(DESCRIPTION_KEY + number, "");
			mUrl = preferences.getString(URL_KEY + number, "");
			mRefresh = preferences.getInt(REFRESH_KEY + number, 1);
		}
	}

	//--------------------------------------------------------------------------
	private final static String[] INIT_CAMERA = {
		"Mathew Street",						"http://hardys.mathew.st/capture/webcam1_small.jpg",
		"Lochness",								"http://www.lochness.co.uk/livecam/img/lochness.jpg",
		"Seward Alaska Harbor",					"http://www.majormarine.com/cam/cam00.jpg",
		"Brooklyn Bridge",						"http://brooklyn-bridge.mobotixcam.de/record/current.jpg",
		"Large Hadron Collider: Underground Experimental Cavern", "http://cms.web.cern.ch/cms/cmseye/eye7.jpg",
		"Large Hadron Collider: Control Room",	"http://cms.web.cern.ch/cms/cmseye/eye5.jpg",
		"Santorini",							"http://customers.heliowebs.gr/fileadmin/matiartwebcam/image.jpg",
		"Chicago Sears Tower Camera",			"http://www.myfoxwfld.com/webcam/sears/sears.jpg",
		"Greece, Naxos.",						"http://www.naxosisland.eu/webcam/port.jpg",
		"Sierra, Nevada",						"http://www.sierranevadaski.com/_extras/fotos_camaras/mobotix/current.jpg",
	};

	private final static int[] INIT_REFRESH = {
		5,
		5,
		5,
		5,
		5*60,
		5*60,
		2*60,
		60,
		7,
		60
	};

	public JpegCameraRecordsManager(Context context, OnListChangedListener listener) {
		super(context);

		mOnListChangedListener = listener;
		
		//it is instead the comment &&&&&&
		updateAllRecords();
		
		/*&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean is_first_run = preferences.getBoolean(IS_FIRST_RUN, true);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		try {
			Context app_context = mContext.getApplicationContext();
			current_appl_code = app_context.getPackageManager().getPackageInfo(app_context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		synchronized (mElementsSync) {
			
			mElements.addAll(Storage.getCameraRecords(this));
		
			if (mElements.size() == 0 && is_first_run) {

				for (int i=0; i<INIT_REFRESH.length; i++) {

					CameraRecord camRec = Storage.appendCameraRecord(this, INIT_CAMERA[i*2], INIT_CAMERA[i*2+1], INIT_REFRESH[i], false, -1, -1, false, false);
					if (camRec != null)
						mElements.add(camRec);
				}
	
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
	
			} else {
	
				if (current_appl_code != appl_version_code) {
	
					int count = preferences.getInt(COUNT_KEY, 0);
					for(int i=0; i<count; i++) {
						JpegUrl record = new JpegUrl(preferences, i);
						CameraRecord camRec = Storage.appendCameraRecord(this, 
								record.mDescription, record.mUrl, record.mRefresh, false, -1, -1, false, false);
						
						if (camRec != null)
							mElements.add(camRec);
					}
					
					//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
					preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				}
			}

			while (mElements.remove(null)) {
			}
		}

		sortList();
		&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	}

	//--------------------------------------------------------------------------
	@Override
	public String getRecordTitle(RecordBase record) {

		if (record == null)
			return "";

		if (record instanceof CameraRecord) {
			StringBuilder builder = new StringBuilder(record.getDescription() != null ? record.getDescription() : "");
			builder.append(' ');
			builder.append(String.format(mContext.getString(Res.string.jpeg_camera_refresh_description_fmt), 
					((CameraRecord)record).getRefreshPeriod()));
	
			return builder.toString();
		}
		
		return super.getRecordTitle(record);
	}

	//--------------------------------------------------------------------------
	public Vector<CameraRecord> getAllRecords() {
		Vector<CameraRecord> result = new Vector<CameraRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					result.add((CameraRecord)record);
				}
			}
		}
		return result;
	}

	//--------------------------------------------------------------------------
	public Vector<CameraRecord> getFavoriteRecords() {
		Vector<CameraRecord> result = new Vector<CameraRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null && record.isFavorite())
					result.add((CameraRecord)record);
			}
		}
		return result;
	}

	//--------------------------------------------------------------------------
	public Vector<CameraRecord> getHistoryRecords() {
		Vector<CameraRecord> result = new Vector<CameraRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					if (record.getLastAccessedDate() > 0) {
						if (result.size() == 0 ) {

							result.add((CameraRecord)record);

						} else {

							boolean isInserted = false;
							for(int i = 0; i < result.size(); i++) {
								if (record.getLastAccessedDate() > result.get(i).getLastAccessedDate()) {
									result.insertElementAt((CameraRecord)record, i);
									isInserted = true;
									break;
								}
							}

							if (!isInserted) {
								result.add((CameraRecord)record);
							}
						}
					}
				}
			}
		}

		if (result.size() > HISTORY_RECORDS_COUNT)
			result.setSize(HISTORY_RECORDS_COUNT);

		return result;
	}

	/*
	//----------------------------------------------------------------------
	public boolean isNewLink(String url) {
		for(CameraRecord u : mJpegCameraList) {
			if (u.getUrl() != null && u.getUrl().equalsIgnoreCase(url)) {
				return false;
			}
		}
		return true;
	}
	*/

	//--------------------------------------------------------------------------
	private CameraRecord getRecordByURL(String url) {
		synchronized (mElementsSync) {
			for(RecordBase record : mElements) {
				if (record != null) {
					String rec_url = record.getUrl(); 
					if (rec_url != null && rec_url.equalsIgnoreCase(url) && record instanceof CameraRecord) {
						return (CameraRecord)record;
					}
				}
			}
		}
		return null;
	}

	//--------------------------------------------------------------------------
	public boolean add(String description, String url, int refreshPeriod, boolean favorite, int group_id, long lastAccessedDateInMillis, boolean markAsNew, boolean markAsDead) {
		if (url == null) {
			return false;
		}
		
		CameraRecord u = getRecordByURL(url);
		if (u != null) {
			if ((u.getDescription() == null || u.getDescription().length() == 0) && description != null && description.length() > 0) {
				u.setDescription(description);
			}
			u.setRefreshPeriod(refreshPeriod);
			u.setLastAccessedDate(lastAccessedDateInMillis);
			u.setNewState(markAsNew);
			u.setLinkDeadState(markAsDead);
			boolean result = Storage.updateRecord(mContext, u);
			sortList();
			if (result && mOnListChangedListener != null) {
				mOnListChangedListener.onRecordListChanged();
			}
			return false;
		}

		CameraRecord camRec = Storage.appendCameraRecord(this, description, url, refreshPeriod, favorite, group_id, lastAccessedDateInMillis, markAsNew, markAsDead);
		if (camRec == null)
			return false;

		boolean result = false;
		synchronized (mElementsSync) {
			result = mElements.add(camRec);
		}

		sortList();
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}

		return true;
	}

	//--------------------------------------------------------------------------
	/*private void add(CameraRecord record) {
		Storage.addRecord(mContext, record);
		synchronized (mElementsSync) {
			mElements.add(record);
		}
		sortList();
		if (mOnListChangedListener != null) {
			mOnListChangedListener.ListChanged();
		}
	}*/

	//--------------------------------------------------------------------------
	@Override
	public void sortList() {
		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mContext.getString(Res.string.pref_links_list_use_sorting_key), true)) {
			synchronized (mElementsSync) {
				Collections.sort(mElements);
			}
		}
	}
	
	@Override
	public void updateAllRecords()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean is_first_run = preferences.getBoolean(IS_FIRST_RUN, true);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		try {
			Context app_context = mContext.getApplicationContext();
			current_appl_code = app_context.getPackageManager().getPackageInfo(app_context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		synchronized (mElementsSync)
		{
			mElements.clear();
			
			mElements.addAll(Storage.getCameraRecords(this));
		
			if (mElements.size() == 0 && is_first_run) {

				for (int i=0; i<INIT_REFRESH.length; i++) {

					CameraRecord camRec = Storage.appendCameraRecord(this, INIT_CAMERA[i*2], INIT_CAMERA[i*2+1], INIT_REFRESH[i], false, -1, -1, false, false);
					if (camRec != null)
						mElements.add(camRec);
				}
	
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
	
			} else {
	
				if (current_appl_code != appl_version_code) {
	
					int count = preferences.getInt(COUNT_KEY, 0);
					for(int i=0; i<count; i++) {
						JpegUrl record = new JpegUrl(preferences, i);
						CameraRecord camRec = Storage.appendCameraRecord(this, 
								record.mDescription, record.mUrl, record.mRefresh, false, -1, -1, false, false);
						
						if (camRec != null)
							mElements.add(camRec);
					}
					
					//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
					preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				}
			}

			while (mElements.remove(null)) {
			}
		}

		sortList();
	}


	//--------------------------------------------------------------------------
	/*public void updateLinksDBifNeeded() {
		if (mUpdater != null)
			return;

		Log.v(TAG, "updateLinksDBifNeeded");

		mNewLinksCount = 0;
		mDeadLinksCount = 0;

		SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean is_update_enabled = defPref.getBoolean(mContext.getString(Res.string.pref_links_list_is_update_key), true);
		Log.d(TAG, String.format("is_update_enabled = %s", is_update_enabled));

		if (is_update_enabled) {

			int update_period_in_hours = defPref.getInt(mContext.getString(Res.string.pref_links_list_update_period_key), 3);
			Log.d(TAG, String.format("update_period_in_hours = %s", update_period_in_hours));

			int todayYear = Calendar.getInstance().get(Calendar.YEAR);
			Log.d(TAG, String.format("todayYear = %s", todayYear));

			int todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
			Log.d(TAG, String.format("todayDayOfYear = %s", todayDayOfYear));

			int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			Log.d(TAG, String.format("currentHour = %s", currentHour));

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(JpegCameraActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
			Calendar lastUpdatedCal = Calendar.getInstance();
			lastUpdatedCal.setTimeInMillis(preferences.getLong(StreamMediaActivity.LAST_UPDATE_LINKS_DB_DATE, 0));

			int lastUpdateYear = lastUpdatedCal.get(Calendar.YEAR);
			Log.d(TAG, String.format("lastUpdateYear = %s", lastUpdateYear));

			int lastUpdateDayOfYear = lastUpdatedCal.get(Calendar.DAY_OF_YEAR);
			Log.d(TAG, String.format("lastUpdateDayOfYear = %s", lastUpdateDayOfYear));

			int lastUpdateHour = lastUpdatedCal.get(Calendar.HOUR_OF_DAY);
			Log.d(TAG, String.format("lastUpdateHour = %s", lastUpdateHour));

			if ((todayYear - lastUpdateYear) > 0 || (todayDayOfYear - lastUpdateDayOfYear) > 0 || (currentHour - lastUpdateHour) >= update_period_in_hours) {
				int currentVersion = preferences.getInt(StreamMediaActivity.CURRENT_LINKS_DB_VERSION, 0);
				Log.d(TAG, String.format("CURRENT_LINKS_DB_VERSION = %s", currentVersion));
				mUpdater = LinksUpdater.UpdateLinks(this, LinksUpdater.TYPE_CAMERA, currentVersion, this);
			} else {
				Log.i(TAG, "postpone updateLinksDB");
			}
		} else {
			Log.w(TAG, "updateLinksDB is OFF");
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void ErrorOccurs(Exception error) {
		Log.e("JpegCameraRecordsManager.LinksUpdater", error.getMessage());
		if (!(error instanceof UnknownHostException && !(error instanceof SocketTimeoutException))) {
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(JpegCameraActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
			Calendar calendar = Calendar.getInstance();
			int hour = calendar.get(Calendar.HOUR_OF_DAY) - preferences.getInt(mContext.getString(Res.string.pref_links_list_update_period_key), 3) + 1;
			if (hour >= 0) {
				calendar.set(Calendar.HOUR_OF_DAY, hour);
				preferences.edit().putLong(StreamMediaActivity.LAST_UPDATE_LINKS_DB_DATE, calendar.getTimeInMillis()).commit();
			}
			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnErrorOccurs(error.getMessage());
			}
		} else {
			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnUpdateFinished();
			}
		}
		mUpdater = null;
	}

	//--------------------------------------------------------------------------
	@Override
	public void RecordToUpdate(RecordBase recordBase) {
		if (recordBase instanceof CameraRecord) {
			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnUpdate();
			}

			CameraRecord record = (CameraRecord) recordBase;
			CameraRecord findedRec = getRecordByURL(record.getUrl());
			if (!record.isDeadLink()) {

				if (findedRec == null) {
					add(record);
					mNewLinksCount++;

				} else {

					//Update old record
					if (findedRec != null) {
						boolean updateNeeded = false;
						if ((findedRec.getDescription() == null || findedRec.getDescription().length() == 0 || findedRec.getDescription().equalsIgnoreCase("null")) && (record.getDescription() != null && record.getDescription().length() > 0)) {
							findedRec.setDescription(record.getDescription());
							updateNeeded = true;
						}

						if (updateNeeded)
							updateRecord(findedRec);
					}
				}

			} else {

				if (findedRec != null) {
					findedRec.setLinkDeadState(record.isDeadLink());
					findedRec.setLinkNotAvailable(RecordBase.SET_LINK_TO_DEAD_COUNTER);
					updateRecord(findedRec);
					mDeadLinksCount++;
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void UpdateCompleted(int updatedToVersion) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(JpegCameraActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
		preferences.edit().putInt(StreamMediaActivity.CURRENT_LINKS_DB_VERSION, updatedToVersion).putLong(StreamMediaActivity.LAST_UPDATE_LINKS_DB_DATE, Calendar.getInstance().getTimeInMillis()).commit();

		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				boolean updateFlag = false;
				if (!record.isDeadLink() && record.getLinkNotAvailableCount() >= RecordBase.SET_LINK_TO_DEAD_COUNTER) {

					record.setLinkDeadState(true);
					mDeadLinksCount++;
					updateFlag = true;

				} else if (record.isDeadLink() && record.getLinkNotAvailableCount() == 0) {

					record.setLinkDeadState(false);
					updateFlag = true;
				}

				if (updateFlag) {
					updateRecordSkipSorting(record);
				}
			}
		}

		//sortList();

		if (mOnListChangedListener != null) {
			mOnListChangedListener.ListChanged();
		}

		if (mNewLinksCount > 0 || mDeadLinksCount > 0) {
			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnCompleted(mNewLinksCount, mDeadLinksCount);
			}
		}


		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mContext.getString(Res.string.pref_links_list_use_auto_checking_key), true)) {
			performCheckLinksAvaliability();
		} else {
			mUpdater = null;
		}
	}*/
}
