package com.simpity.android.media.radio;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.controls.fasttree.FastTreeItem;
import com.simpity.android.media.storage.GroupNameAndId;
import com.simpity.android.media.storage.RadioRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;

public class RadioRecordsManager extends RecordsManager {

	@SuppressWarnings("unused")
	private final String TAG = "RadioRecordsManager";

	private Vector<LinkCounter> mGroupCounterList = new Vector<LinkCounter>();

	private final static String COUNT_KEY = "Radio URL Count";
	private final static String IS_FIRST_RUN = "RADIO_FIRST_RUN";

	//---------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------
	private class RadioUrl {
		private final static String URL_KEY = "Radio URL ";
		private final static String DESCRIPTION_KEY = "Radio Description ";
		private final static String CONTENT_KEY = "Radio Content ";

		String mDescription;
		String mUrl;
		String mContent;

		//----------------------------------------------------------------------
		RadioUrl(SharedPreferences preferences, int number) {
			mDescription = preferences.getString(DESCRIPTION_KEY + number, null);
			mUrl = preferences.getString(URL_KEY + number, "");
			mContent = preferences.getString(CONTENT_KEY + number, null);
		}
	}

	//---------------------------------------------------------------------------------------
	private final static String[] INIT_STATIONS = {
		"http://shoutcast.byfly.by:88/loveradio",
		"http://shoutcast.byfly.by:88/radioseven",
		"http://shoutcast.byfly.by:88/difm_vocaltrance"
	};

	//---------------------------------------------------------------------------------------
	public RadioRecordsManager(Context activity) {
		super(activity);

		mOnListChangedListener = null;
		mContext = activity;
	}
	//---------------------------------------------------------------------------------------
	public RadioRecordsManager(Context activity, OnListChangedListener listener) {
		super(activity);

		mOnListChangedListener = listener;

		//it is instead the comment &&&&&&
		updateAllRecords();

		/*&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity); 

		synchronized (mElementsSync) {
			mGroups.addAll(Storage.getAllGroup(this, Storage.INTERNET_RADIO_GROUP));
			while(mGroups.remove(null)) {
			}
		}

		//boolean is_first_run = preferences.getBoolean(IS_FIRST_RUN, true);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		try {
			current_appl_code = mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getApplicationContext().getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		synchronized (mElementsSync) {
			mElements.addAll(Storage.getRadioRecords(this));
		}
		
		if (mElements.size() == 0 &&  preferences.getBoolean(IS_FIRST_RUN, true)) {
			
			for (String station : INIT_STATIONS) {
				add(null, station, null, null, false, getGroupForRadio(station).getId(), -1, false, false);
			}

			preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();

		} else {

			if (current_appl_code != appl_version_code) {

				int count = preferences.getInt(COUNT_KEY, 0);
				for(int i=0; i < count; i++) {
					RadioUrl record = new RadioUrl(preferences, i);
					add(record.mDescription, record.mUrl, record.mContent, null, false, getGroupForRadio(record.mUrl).getId(), -1, false, false);
				}

				//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				assignRecordsToGroups();

			} else {

				Log.d("RadioRecordsManager", String.format("ELEMENTS_COUNT=%d", mElements.size()));
			}
		}
		
		synchronized (mElementsSync) {
			while(mElements.remove(null)) {
			}
		}

		sortList();
		refreshHostCounterList();
		&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&*/
	}

	//----------------------------------------------------------------------
	public void reloadRecords() {
		synchronized (mElementsSync) {
			mElements.clear();
			mElements.addAll(Storage.getRadioRecords(this));
		}
	}
	
	//----------------------------------------------------------------------
	private void assignRecordsToGroups() {
		refreshHostCounterList();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null && record.getGroupId() == -1) {
					GroupNameAndId group = getGroupForRadio((RadioRecord)record);
					record.setGroup(group.getId());
					updateRecord(record);
				}
			}
		}
	}

	//----------------------------------------------------------------------
	public Vector<RadioRecord> getAllRecords() {
		Vector<RadioRecord> result = new Vector<RadioRecord>();
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					result.add((RadioRecord)record);
				}
			}
		}
		return result;
	}

	//----------------------------------------------------------------------
	public Vector<RadioRecord> getFavoriteRecords() {
		Vector<RadioRecord> result = new Vector<RadioRecord>();
		synchronized (mElementsSync) {
			for (RecordBase radioRecord : mElements) {
				if (radioRecord != null && radioRecord.isFavorite())
					result.add((RadioRecord)radioRecord);
			}
		}
		return result;
	}

	//----------------------------------------------------------------------
	public Vector<RadioRecord> getHistoryRecords() {
		Vector<RadioRecord> result = new Vector<RadioRecord>();
		synchronized (mElementsSync) {
			for (RecordBase radioRecord : mElements) {
				if (radioRecord != null && radioRecord.getLastAccessedDate() > 0) {
					result.add((RadioRecord)radioRecord);
				}
			}
		}

		Collections.sort(result, new Comparator<RadioRecord>() {
			@Override
			public int compare(RadioRecord object1, RadioRecord object2) {
				return (int)(object2.getLastAccessedDate() - object1.getLastAccessedDate());
			}
		});
		
		if (result.size() > HISTORY_RECORDS_COUNT) { 
			result.setSize(HISTORY_RECORDS_COUNT);
		}
		
		return result;
	}

	//----------------------------------------------------------------------
	@Override
	public Vector<RecordBase> getAllRecordsForGroup(int group_id) {
		Vector<RecordBase> result = new Vector<RecordBase>();
		synchronized (mElementsSync) {
			for (RecordBase radioRecord : mElements) {
				if (radioRecord != null && radioRecord.getGroupId() == group_id)
					result.add((RadioRecord)radioRecord);
			}
		}
		return result;
	}

	//----------------------------------------------------------------------
	/*
	@Override
	public Vector<GroupNameAndId> getAllGroupForGroup(int group_id) {
		Vector<GroupNameAndId> result = new Vector<GroupNameAndId>(0);

		try {
			for (GroupNameAndId radioGroup : mRadioGroups) {
				if (radioGroup != null && radioGroup.mParentId == group_id)
					result.add(radioGroup);
			}
		} catch (ConcurrentModificationException e) {
			return getAllGroupForGroup(group_id);
		}
		return result;
	}
	*/

	/*
	//----------------------------------------------------------------------
	public GroupNameAndId getGroupByID(int group_id) {
		if (group_id < 0)
			return null;
		synchronized (groupsModificationSync) {
			for (GroupNameAndId radioGroup : mGroups) {
				if (radioGroup.mId == group_id)
					return radioGroup;
			}
		}
		return null;
	}
	*/

	//----------------------------------------------------------------------
	public Vector<FastTreeItem> getAllRootItems() {
		Vector<FastTreeItem> result = new Vector<FastTreeItem>();
		synchronized (mElementsSync) {
			for (GroupNameAndId radioGroup : mGroups) {
				if (radioGroup != null && radioGroup.getGroupId() == -1)
					result.add(radioGroup);
			}

			for (RecordBase radioRec : mElements) {
				if (radioRec != null && radioRec.getGroupId() == -1)
					result.add(radioRec);
			}
		}
		return result;
	}

	//----------------------------------------------------------------------
	private RadioRecord getRecordByURL(String url) {
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				if (record != null) {
					String rec_url = record.getUrl(); 
					if (rec_url != null && rec_url.equalsIgnoreCase(url) && record instanceof RadioRecord) {
						return (RadioRecord)record;
					}
				}
			}
		}
		return null;
	}

	//----------------------------------------------------------------------
	@Override
	public void sortList() {
		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mContext.getString(Res.string.pref_links_list_use_sorting_key), true)) {

			synchronized (mElementsSync) {
				Collections.sort(mElements);
				Collections.sort(mGroups);
			}
		}
	}

	//----------------------------------------------------------------------
	public boolean add(String stationName, String url, String contentDescription, String genre, boolean favorite, int group_id, long lastAccessedDateInMillis, boolean markAsNew, boolean markAsDead) {

		if (url == null) {
			return false;
		}

		RadioRecord u = getRecordByURL(url);
		if (u != null) {
			group_id = getGroupForRadio(u).getId();
			if ((u.getStationName() == null || u.getStationName().length() == 0) && stationName != null && stationName.length() > 0) {
				u.setStationName(stationName);
			}

			u.setGenre(genre);
			u.setLastAccessedDate(lastAccessedDateInMillis);
			u.setContentDescription(contentDescription);
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

		RadioRecord radioRec = Storage.appendRadioRecord(this, stationName, url, contentDescription,
						genre, favorite, group_id, lastAccessedDateInMillis, markAsNew, markAsDead);
		if (radioRec == null) {
			return false;
		}

		boolean result = false;
		synchronized (mElementsSync) {
			result = mElements.add(radioRec);
		}

		sortList();
		refreshHostCounterList();
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}
		return true;
	}

	//----------------------------------------------------------------------
	/*private void add(RadioRecord record) {
		Storage.addRecord(mContext, record);
		synchronized (mElementsSync) {
			mElements.add(record);
		}
		sortList();
		refreshHostCounterLst();
		if (mOnListChangedListener != null) {
			mOnListChangedListener.ListChanged();
		}
	}*/

	//----------------------------------------------------------------------
	@Override
	public boolean remove(RecordBase record) {
		boolean result = false;
		synchronized (mElementsSync) {
			result = mElements.removeElement(record);
		}
		
		result = Storage.deleteRecord(mContext, record);
		GroupNameAndId group = getGroupById(record.getGroupId());
		if (group != null && getAllRecordsForGroup(group.getId()).size() == 0) {
			synchronized (mElementsSync) {
				mGroups.remove(group);
			}
			Storage.deleteRecord(mContext, group);
			refreshHostCounterList();
		}
		
		if (mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}
		
		return result;
	}
	
	@Override
	public void updateAllRecords()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext); 

		synchronized (mElementsSync) {
			mGroups.addAll(Storage.getAllGroup(this, Storage.INTERNET_RADIO_GROUP));
			while(mGroups.remove(null)) {
			}
		}

		//boolean is_first_run = preferences.getBoolean(IS_FIRST_RUN, true);
		int appl_version_code = preferences.getInt(StreamMediaActivity.APPLICATION_VERSION_CODE, 0);
		int current_appl_code = 0;
		try {
			current_appl_code = mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getApplicationContext().getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		synchronized (mElementsSync) {
			mElements.addAll(Storage.getRadioRecords(this));
		}
		
		if (mElements.size() == 0 &&  preferences.getBoolean(IS_FIRST_RUN, true)) {
			
			for (String station : INIT_STATIONS) {
				add(null, station, null, null, false, getGroupForRadio(station).getId(), -1, false, false);
			}

			preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();

		} else {

			if (current_appl_code != appl_version_code) {

				int count = preferences.getInt(COUNT_KEY, 0);
				for(int i=0; i < count; i++) {
					RadioUrl record = new RadioUrl(preferences, i);
					add(record.mDescription, record.mUrl, record.mContent, null, false, getGroupForRadio(record.mUrl).getId(), -1, false, false);
				}

				//preferences.edit().clear().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				preferences.edit().putInt(StreamMediaActivity.APPLICATION_VERSION_CODE, current_appl_code).putBoolean(IS_FIRST_RUN, false).commit();
				assignRecordsToGroups();

			} else {

				Log.d("RadioRecordsManager", String.format("ELEMENTS_COUNT=%d", mElements.size()));
			}
		}
		
		synchronized (mElementsSync) {
			while(mElements.remove(null)) {
			}
		}

		sortList();
		refreshHostCounterList();
	}

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

			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RadioActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
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
				mUpdater = LinksUpdater.UpdateLinks(this, LinksUpdater.TYPE_RADIO,currentVersion, this);

			} else {

				Log.i(TAG, "postpone updateLinksDB");
			}

		} else {

			Log.w(TAG, "updateLinksDB is OFF");
		}
	}*/

	//----------------------------------------------------------------------
	/*@Override
	public void ErrorOccurs(Exception error) {
		Log.e("RadioRecordsManager.LinksUpdater", error.getMessage());
		if (!(error instanceof UnknownHostException) && !(error instanceof SocketTimeoutException)) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RadioActivity.class.getSimpleName(), Activity.MODE_PRIVATE);

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

	//----------------------------------------------------------------------
	@Override
	public void RecordToUpdate(RecordBase recordBase) {
		if (recordBase instanceof RadioRecord) {

			if (mOnUpdateListener != null) {
				mOnUpdateListener.OnUpdate();
			}

			RadioRecord record = (RadioRecord) recordBase;
			RadioRecord findedRec = getRecordByURL(record.getUrl());
			if (!record.isDeadLink()) {

				if (findedRec == null) {

					record.setGroup(getGroupForRadio(record).getId());
					add(record);
					mNewLinksCount++;

				} else {

					//Update old record
					if (findedRec != null) {
						boolean updateNeeded = false;
						if ((findedRec.getGenre() == null || findedRec.getGenre().length() == 0)
							&& (record.getGenre() != null
							&& record.getGenre().length() > 0)) {

							findedRec.setGenre(record.getGenre());
							updateNeeded = true;
						}

						if ((findedRec.getStationName() == null || findedRec.getStationName().length() == 0)
							&& (record.getStationName() != null
							&& record.getStationName().length() > 0)) {

							findedRec.setStationName(record.getStationName());
							updateNeeded = true;
						}

						if (updateNeeded)
							updateRecord(findedRec);
					}
				}

			} else {

				if (findedRec != null) {

					findedRec.setStationName(record.getStationName());
					findedRec.setContentDescription(record.getContentDescription());
					findedRec.setGenre(record.getGenre());
					findedRec.setLinkDeadState(record.isDeadLink());
					findedRec.setGroup(getGroupForRadio(record).getId());
					findedRec.setLinkNotAvailable(RecordBase.SET_LINK_TO_DEAD_COUNTER);
					updateRecord(findedRec);
					mDeadLinksCount++;
				}
			}
			record = null;
		}

		recordBase = null;
	}

	//----------------------------------------------------------------------
	@Override
	public void UpdateCompleted(int updatedToVersion) {

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		//SharedPreferences preferences = mContext.getApplication().getSharedPreferences(RadioActivity.class.getSimpleName(), Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();

		editor.putInt(StreamMediaActivity.CURRENT_LINKS_DB_VERSION, updatedToVersion);
		editor.putLong(StreamMediaActivity.LAST_UPDATE_LINKS_DB_DATE, Calendar.getInstance().getTimeInMillis());
		editor.commit();

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

	//----------------------------------------------------------------------
	@Override
	public String getRecordTitle(RecordBase record) {
		
		if (record instanceof RadioRecord) {
			RadioRecord radio_record = (RadioRecord)record;
			StringBuilder builder = new StringBuilder();
			String station_name = radio_record.getStationName();

			if (station_name != null) {

				builder.append(station_name);

			} else {

				builder.append(mContext.getString(Res.string.Unknown));
			}

			String description = radio_record.getContentDescription();
			if (description != null && description.length() > 0) {

				builder.append(" (");
				builder.append(radio_record.getContentDescription());
				builder.append(')');
			}

			return builder.toString();
		}
		
		return super.getRecordTitle(record);
	}

	//----------------------------------------------------------------------
	private GroupNameAndId getGroupForRadio(String url) {

		try {
			String hostName = getHostName(url);
			if (hostName != null) {

				synchronized (mElementsSync) {
					for (GroupNameAndId group : mGroups) {
						if (group.getDescription().equalsIgnoreCase(hostName))
							return group;
					}
				}

				for (LinkCounter hostLinkCounter : getHostCounterList()) {
					if (hostLinkCounter.host.equalsIgnoreCase(hostName) &&
						(hostLinkCounter.links.size() > 1 || !hostLinkCounter.isInList(url))) {

						GroupNameAndId newGroup = Storage.createNewGroup(this, Storage.INTERNET_RADIO_GROUP, hostName, -1);
						synchronized (mElementsSync) {
							mGroups.add(newGroup);
						}

						for (RadioRecord record : hostLinkCounter.links) {
							record.setGroup(newGroup.getId());
							updateRecord(record);
						}

						return newGroup;
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new GroupNameAndId(this, "", -1, Storage.INTERNET_RADIO_GROUP, -1);
	}

	//----------------------------------------------------------------------
	private GroupNameAndId getGroupForRadio(RadioRecord radioRecord) {

		try {
			String hostName = radioRecord.getHostName();
			if (hostName != null) {

				synchronized (mElementsSync) {
					for (GroupNameAndId group : mGroups) {
						if (group.getDescription().equalsIgnoreCase(hostName))
							return group;
					}
				}

				for (LinkCounter hostLinkCounter : getHostCounterList()) {
					if (hostLinkCounter.host.equalsIgnoreCase(hostName) &&
						(hostLinkCounter.links.size() > 1 || !hostLinkCounter.isInList(radioRecord))) {

						GroupNameAndId newGroup = Storage.createNewGroup(this, Storage.INTERNET_RADIO_GROUP, hostName, -1);
						synchronized (mElementsSync) {
							mGroups.add(newGroup);
						}

						for (RadioRecord record : hostLinkCounter.links) {
							record.setGroup(newGroup.getId());
							updateRecord(record);
						}

						return newGroup;
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new GroupNameAndId(this, "", -1, Storage.INTERNET_RADIO_GROUP, -1);
	}

	//----------------------------------------------------------------------
	private Vector<LinkCounter> getHostCounterList() {
		return mGroupCounterList;
	}

	//----------------------------------------------------------------------
	private void refreshHostCounterList() {
		mGroupCounterList.clear();
		boolean foundFlag = false;
		synchronized (mElementsSync) {
			for (RecordBase record : mElements) {
				String recordHostName = record.getHostName();
				if (recordHostName == null) {
					continue;
				}

				foundFlag = false;
				for (LinkCounter hostLinkCounter : mGroupCounterList) {
					if (hostLinkCounter.host.equalsIgnoreCase(recordHostName)) {
						hostLinkCounter.links.add((RadioRecord)record);
						foundFlag = true;
						break;
					}
				}

				if (!foundFlag) {
					mGroupCounterList.add(new LinkCounter(recordHostName, (RadioRecord)record));
				}
			}
		}
	}

	//----------------------------------------------------------------------
	//----------------------------------------------------------------------
	private class LinkCounter {

		public String host = null;
		public Vector<RadioRecord> links = new Vector<RadioRecord>();

		//------------------------------------------------------------------
		public LinkCounter(String host, RadioRecord link) {
			this.host = host;
			links.add(link);
		}

		//------------------------------------------------------------------
		public boolean isInList(String link) {
			for (RadioRecord record : links) {
				if (record.getUrl().equalsIgnoreCase(link))
					return true;
			}

			return false;
		}

		//------------------------------------------------------------------
		public boolean isInList(RadioRecord recordToFind) {
			for (RadioRecord record : links) {
				if (record.equals(recordToFind))
					return true;
			}
			return false;
		}
	}
}
