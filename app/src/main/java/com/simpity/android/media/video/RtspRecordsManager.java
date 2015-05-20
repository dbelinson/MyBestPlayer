package com.simpity.android.media.video;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.storage.GroupNameAndId;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;
import com.simpity.android.media.storage.VideoRecord;

public class RtspRecordsManager extends RecordsManager {

	@SuppressWarnings("unused")
	private final String TAG = "RtspRecordsManager";

	private final static String COUNT_KEY = "Rtsp URL Count";
	private final static String IS_FIRST_RUN = "RTSP_FIRST_RUN";

	//-------------------------------------------------------------------------
	class RtspUrl {
		private final static String URL_KEY = "Rtsp URL ";
		private final static String DESCRIPTION_KEY = "Rtsp Description ";
		private final static String IS_NEW_KEY = "Is new URL ";

		String mDescription;
		String mUrl;
		boolean mIsNew = false;

		//----------------------------------------------------------------------
		RtspUrl(SharedPreferences preferences, int number) {
			mDescription = preferences.getString(DESCRIPTION_KEY + number, "");
			mUrl = preferences.getString(URL_KEY + number, "");
			mIsNew = preferences.getBoolean(IS_NEW_KEY + number, false);
		}
	}

	//-------------------------------------------------------------------------
	private final static String[] LINKS = {
		"News",				"rtsp://streaming.prd.transpera.com/stream/0005/0414/9156/content.trans_hinted.3gp", 	"News",
		"Comedy",			"rtsp://stream.zoovision.com/Movies/vader_sessions.3gp", 								"Fun",
		"Travel",			"rtsp://streaming.prd.transpera.com/stream/0020/0235/5285/cbc_0210_manila_480x270.trans_hinted.3gp", "Travel",
		"Crime",			"rtsp://video2.multicasttech.com/AFTVCrime3GPP296.sdp", 								"Crime",
		"Horror",			"rtsp://video2.multicasttech.com/AFTVHorror3GPP296.sdp",								"Fun",
		"MysteryFree.TV",	"rtsp://video2.multicasttech.com/AFTVMystery3GPP296.sdp",								"TV",
		"SciFiFree.TV",		"rtsp://video2.multicasttech.com/AFTVSciFi3GPP296.sdp", 								"TV",
		"Food & Drink",		"rtsp://stream.zoovision.com/budgethealthnut/budgethealthnut_appetizers_480x270.3gp",	"Other",
		"Sports",			"rtsp://stream.zoovision.com/cbssports/0805_nba.3gp",									"Sports",
		"Michael Jackson and Billie Jean","rtsp://stream.zoovision.com/sony/MichaelJackson_BillieJean.3gp", 		"Music",
		"Documentary",		"rtsp://stream.zoovision.com/tv/The_Warren_Report-1964_CBS_TV_News_Special.3gp", 		"Other",
		"Jazz",				"rtsp://digitalbroadcast.streamguys.net/live-studio.sdp", 								"Music",
		"Adventure",		"rtsp://video3.multicasttech.com/AFTVAdventure3GPP296.sdp",								"TV",
		"Cartoons!",		"rtsp://video3.multicasttech.com/AFTVCartoons3GPP296.sdp", 								"Cartoons",
		"Classics!",		"rtsp://video3.multicasttech.com/AFTVClassics3GPP296.sdp", 								"TV",
		//"IndyMovies.TV",	"rtsp://video3.multicasttech.com/AFTVCartoons3GPP296.sdp",								"TV",
		"TV2 News(Denmark)","rtsp://3gp-tv2.unwire.dk/livestreaming/tv2/tv2news/tv2news_108k.sdp",					"News",
		"AZ TV (Slovakia)",	"rtsp://stream.the.sk/live/aztv/aztv-hm.3gp", 											"TV",
		"Comedy TV",		"rtsp://video3.multicasttech.com/AFTVComedy3GPP296.sdp", 								"TV"
	};

	public RtspRecordsManager(Context context, OnListChangedListener listener) {
		super(context);
		
		mOnListChangedListener = listener;
		
		//it is instead the comment &&&&&&
		updateAllRecords();
		
		/*&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		//SharedPreferences preferences = activity.getApplication().getSharedPreferences(RtspActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		initGroups();
		try {
			current_appl_code = mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getApplicationContext().getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		synchronized (mElementsSync) {
			mElements.addAll(Storage.getVideoRecords(this));
		
			if (mElements.size() == 0 && preferences.getBoolean(IS_FIRST_RUN, true)) {
	
				VideoRecord record;
				for (int i=0; i<LINKS.length; i+=3) {
					record = Storage.appendVideoRecord(this, mContext, LINKS[i], LINKS[i+1], LINKS[i+2], getGroupByName(LINKS[i+2]).getId(), false, -1, false, false);
					if (record != null)
						mElements.add(record);
				}
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
	
			} else {
				
				if (current_appl_code != appl_version_code) {
					
					int count = preferences.getInt(COUNT_KEY, 0);
					for(int i = 0; i < count; i++) {
						RtspUrl record = new RtspUrl(preferences, i);
						VideoRecord videoRecord = Storage.appendVideoRecord(this, mContext, record.mDescription, record.mUrl, "Other", getGroupByName("Other").getId(), false, -1, false, false);
						if (videoRecord != null)
							mElements.add(videoRecord);
					}
	
					//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
					preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				}
	
				assignRecordsToGroups();
				
				Log.d("RtspRecordsManager", String.format("ELEMENTS_COUNT=%d", mElements.size()));
			}
		
			while(mElements.remove(null)) {
			}
		}
		
		sortList();
		&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	}

	//-------------------------------------------------------------------------
	private void initGroups() {
		
		mGroups.addAll(Storage.getAllGroup(this, Storage.STREAM_VIDEO_GROUP));
		
		String[] categories = mContext.getResources().getStringArray(Res.arrays.video_categories);
		
		for (String categorie : categories) {
			boolean foundFlag = false;
			for (GroupNameAndId group : mGroups) {
				if (categorie.equalsIgnoreCase(group.getDescription())) {
					foundFlag = true;
					break;
				}
			}
			if (!foundFlag) {
				GroupNameAndId group = Storage.createNewGroup(this, Storage.STREAM_VIDEO_GROUP, categorie, -1);
				mGroups.add(group);
			}
		}

		Collections.sort(mGroups);
	}

	//-------------------------------------------------------------------------
	private void assignRecordsToGroups() {
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null && record.getGroupId() == -1) {
					record.setGroup(getGroupByName(((VideoRecord)record).getCategory()).getId());
					updateRecord((VideoRecord)record);
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	public GroupNameAndId getGroupByName(String groupName) {
		for (GroupNameAndId group : mGroups) {
			if (group.getDescription().equalsIgnoreCase(groupName)) {
				return group;
			}
		}
		return getGroupByName(mContext.getResources().getStringArray(Res.arrays.video_categories)[0]);
	}

	//-------------------------------------------------------------------------
	public Vector<VideoRecord> getAllRecords() {
		Vector<VideoRecord> result = new Vector<VideoRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					result.add((VideoRecord)record);
				}
			}
		}
		return result;
	}

	//-------------------------------------------------------------------------
	@Override
	public Vector<RecordBase> getAllRecordsForGroup(int group_id) {
		Vector<RecordBase> result = new Vector<RecordBase>();
		synchronized (mElementsSync) {
			for (RecordBase videoRecord : mElements) {
				if (videoRecord != null && videoRecord.getGroupId() == group_id)
					result.add((VideoRecord)videoRecord);
			}
		}
		return result;
	}

	//-------------------------------------------------------------------------
	/*@Override
	public Vector<GroupNameAndId> getAllGroupForGroup(int group_id) {
		Vector<GroupNameAndId> result = new Vector<GroupNameAndId>(0);
		
		try{
			for (GroupNameAndId videoGroup : mVideoGroups) {
				if (videoGroup != null && videoGroup.mParentId == group_id)
					result.add(videoGroup);
			}
		}catch (ConcurrentModificationException e) {
			return getAllGroupForGroup(group_id);
		}

		return result;
	}*/

	//-------------------------------------------------------------------------
	public Vector<GroupNameAndId> getAllRootGroup() {
		Vector<GroupNameAndId> result = new Vector<GroupNameAndId>();
		try{
			for (GroupNameAndId videoGroup : mGroups) {
				if (videoGroup != null && videoGroup.getGroupId() == -1)
					result.add(videoGroup);
			}
		}catch (ConcurrentModificationException e) {
			return getAllRootGroup();
		}
		return result;
	}


	//-------------------------------------------------------------------------
	public Vector<VideoRecord> getFavoriteRecords() {
		Vector<VideoRecord> result = new Vector<VideoRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null && record.isFavorite()) {
					result.add((VideoRecord)record);
				}
			}
		}
		return result;
	}

	//--------------------------------------------------------------------------
	public Vector<VideoRecord> getHistoryRecords() {
		Vector<VideoRecord> result = new Vector<VideoRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null && record.getLastAccessedDate() > 0) {
					if (result.size() == 0 ) {
						
						result.add((VideoRecord)record);
						
					} else {
						
						boolean isInserted = false;
						for(int i = 0; i < result.size(); i++) {
							if (record.getLastAccessedDate() > result.get(i).getLastAccessedDate()) {
								result.insertElementAt((VideoRecord)record, i);
								isInserted = true;
								break;
							}
						}
						if (!isInserted) {
							result.add((VideoRecord)record);
						}
					}
				}
			}
		}
		
		if (result.size() > HISTORY_RECORDS_COUNT) {
			result.setSize(HISTORY_RECORDS_COUNT);
		}
		
		return result;
	}

	//--------------------------------------------------------------------------
	private VideoRecord getRecordByURL(String url) {
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					String rec_url = record.getUrl();
					if (rec_url != null && rec_url.equalsIgnoreCase(url) && record instanceof VideoRecord) {
						return (VideoRecord)record;
					}
				}
			}
		}
		return null;
	}

	//--------------------------------------------------------------------------
	public boolean add(String description, String url, String category, int group_id, boolean favorite, long lastAccessedDateInMillis, boolean markAsNew, boolean markAsDead) {
		if (url == null) {
			return false;
		}

		if (group_id < 0) {
			group_id = getGroupByName(category).getId();
		}

		VideoRecord u = getRecordByURL(url);
		if (u != null) {
			if ((u.getDescription() == null || u.getDescription().length() == 0) && description != null && description.length() > 0) {
				u.setDescription(description);
			}
			u.setLastAccessedDate(lastAccessedDateInMillis);
			u.setCategory(category);
			u.setNewState(markAsNew);
			u.setLinkDeadState(markAsDead);
			u.setGroup(group_id);
			boolean result = Storage.updateRecord(mContext, u);
			sortList();
			if (result && mOnListChangedListener != null) {
				mOnListChangedListener.onRecordListChanged();
			}
			return false;
		}

		VideoRecord videoRecord = Storage.appendVideoRecord(this, mContext, description, url, category, group_id, favorite, lastAccessedDateInMillis, markAsNew, markAsDead);
		if (videoRecord == null)
			return false;

		synchronized (mElementsSync) {
			mElements.add(videoRecord);
		}

		sortList();
		if (mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}

		return true;
	}

	//----------------------------------------------------------------------
	public boolean updateGroup(GroupNameAndId group) {

		boolean result = Storage.updateRecord(mContext, group);

		sortList();
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}

		return result;
	}
	
	@Override
	public void updateAllRecords()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		//SharedPreferences preferences = activity.getApplication().getSharedPreferences(RtspActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		initGroups();
		try {
			current_appl_code = mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getApplicationContext().getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		synchronized (mElementsSync)
		{
			mElements.clear();
			mElements.addAll(Storage.getVideoRecords(this));
		
			if (mElements.size() == 0 && preferences.getBoolean(IS_FIRST_RUN, true)) {
	
				VideoRecord record;
				for (int i=0; i<LINKS.length; i+=3) {
					record = Storage.appendVideoRecord(this, mContext, LINKS[i], LINKS[i+1], LINKS[i+2], getGroupByName(LINKS[i+2]).getId(), false, -1, false, false);
					if (record != null)
						mElements.add(record);
				}
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
	
			} else {
				
				if (current_appl_code != appl_version_code) {
					
					int count = preferences.getInt(COUNT_KEY, 0);
					for(int i = 0; i < count; i++) {
						RtspUrl record = new RtspUrl(preferences, i);
						VideoRecord videoRecord = Storage.appendVideoRecord(this, mContext, record.mDescription, record.mUrl, "Other", getGroupByName("Other").getId(), false, -1, false, false);
						if (videoRecord != null)
							mElements.add(videoRecord);
					}
	
					//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
					preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				}
	
				assignRecordsToGroups();
				
				Log.d("updateAllRecords", String.format("ELEMENTS_COUNT=%d", mElements.size()));
			}
		
			while(mElements.remove(null)) {
			}
		}
		
		sortList();
	}

	/*
	//----------------------------------------------------------------------
	public boolean removeGroup(GroupNameAndId group) {
		if (group.isRootGroup())
			return false;
		boolean result = mVideoGroups.removeElement(group);
		result = Storage.deleteGroupRecord(mContext, group);
		for (GroupNameAndId subGroup : getAllGroupForGroup(group.getId())) {
			subGroup.mParentId = group.mParentId;
			updateGroup(subGroup);
		}
		for (VideoRecord childRecord : getAllRecordsForGroup(group.getId())) {
			childRecord.setGroup(group.mParentId, Storage.getGroupName(mContext, group.mParentId));
			updateRecord(childRecord);
		}
		if (result && mOnListChangedListener != null) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					mOnListChangedListener.ListChanged();
				}
			});
		}
		return result;
	}


	//----------------------------------------------------------------------
	public boolean createGroup(GroupNameAndId parentGroup, String group_name) {
		if (parentGroup == null)
			return false;
		GroupNameAndId newGroup = Storage.createNewGroup(mContext, Storage.STREAM_VIDEO_GROUP, group_name, parentGroup.getId());
		if (newGroup != null) {
			mVideoGroups.add(newGroup);
			if (mOnListChangedListener != null) {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						mOnListChangedListener.ListChanged();
					}
				});
			}
			return true;
		}
		return false;
	}

	*/

	//----------------------------------------------------------------------
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
			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RtspActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
			Calendar lastUpdatedCal = Calendar.getInstance();
			lastUpdatedCal.setTimeInMillis(preferences.getLong(StreamMediaActivity.LAST_UPDATE_LINKS_DB_DATE, 0));

			int lastUpdateYear = lastUpdatedCal.get(Calendar.YEAR);
			Log.d(TAG, String.format("lastUpdateYear = %s", lastUpdateYear));

			int lastUpdateDayOfYear = lastUpdatedCal.get(Calendar.DAY_OF_YEAR);
			Log.d(TAG, String.format("lastUpdateDayOfYear = %s", lastUpdateDayOfYear));

			int lastUpdateHour = lastUpdatedCal.get(Calendar.HOUR_OF_DAY);
			Log.d(TAG, String.format("lastUpdateHour = %s", lastUpdateHour));

			if ((todayYear - lastUpdateYear) > 0
				|| (todayDayOfYear - lastUpdateDayOfYear) > 0
				|| (currentHour - lastUpdateHour) >= update_period_in_hours) {

				int currentVersion = preferences.getInt(StreamMediaActivity.CURRENT_LINKS_DB_VERSION, 0);
				Log.d(TAG, String.format("CURRENT_LINKS_DB_VERSION = %s", currentVersion));
				mUpdater = LinksUpdater.UpdateLinks(this, LinksUpdater.TYPE_VIDEO, currentVersion, this);

			} else {

				Log.i(TAG, "postpone updateLinksDB");
			}

		} else {

			Log.w(TAG, "updateLinksDB is OFF");
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void ErrorOccurs(Exception error) {

		Log.e("RtspRecordsManager.LinksUpdater", error.getMessage());

		if (!(error instanceof UnknownHostException) && !(error instanceof SocketTimeoutException)) {

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RtspActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
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

	//-------------------------------------------------------------------------
	@Override
	public void RecordToUpdate(RecordBase recordBase) {
		if (recordBase instanceof VideoRecord) {
			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnUpdate();
			}

			VideoRecord record = (VideoRecord) recordBase;
			VideoRecord findedRec = getRecordByURL(record.getUrl());
			if (!record.isDeadLink()) {
				if (findedRec == null) {

					record.setGroup(getGroupByName(record.getCategory()).getId());
					add(record);
					//add(record.getDescription(), record.getUrl(), record.getCategory(), getGroupByName(record.getCategory()).getId(), record.isFavorite(), record.getLastAccessedDate(), record.isNewLink(), record.isDeadLink());
					mNewLinksCount++;

				} else {

					//Update old record
					if (findedRec != null) {
						boolean updateNeeded = false;

						if ((findedRec.getCategory() == null || findedRec.getCategory().length() == 0)
							&& (record.getCategory() != null
							&& record.getCategory().length() > 0)) {

							findedRec.setCategory(record.getCategory());
							findedRec.setGroup(getGroupByName(record.getCategory()).getId());
							updateNeeded = true;
						}

						if ((findedRec.getDescription() == null || findedRec.getDescription().length() == 0)
							&& (record.getDescription() != null
							&& record.getDescription().length() > 0)) {

							findedRec.setDescription(record.getDescription());
							updateNeeded = true;
						}

						if (updateNeeded)
							updateRecord(findedRec);
					}
				}
			} else {
				if (findedRec != null) {
					findedRec.setCategory(record.getCategory());
					findedRec.setDescription(record.getDescription());
					findedRec.setLinkDeadState(record.isDeadLink());
					findedRec.setGroup(getGroupByName(record.getCategory()).getId());
					findedRec.setLinkNotAvailable(RecordBase.SET_LINK_TO_DEAD_COUNTER);
					updateRecord(findedRec);
					//add(record.getDescription(), record.getUrl(), record.getCategory(), getGroupByName(record.getCategory()).getId(), record.isFavorite(), record.getLastAccessedDate(), record.isNewLink(), record.isDeadLink());
					mDeadLinksCount++;
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	@Override
	public void UpdateCompleted(int updatedToVersion) {

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RtspActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
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
