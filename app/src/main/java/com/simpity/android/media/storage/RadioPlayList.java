package com.simpity.android.media.storage;

import java.util.Vector;

import android.database.Cursor;

public class RadioPlayList extends PlayList {

	//--------------------------------------------------------------------------	
	public RadioPlayList(RecordsManager manager, Cursor cursor, TableIndex index) {
		super (manager, cursor, index);
		setType(Storage.INTERNET_RADIO_PLAYLIST);
	}
	
	//--------------------------------------------------------------------------	
	public RadioPlayList(RecordsManager manager, int id, String title) {
		super (manager, id, title);
		setType(Storage.INTERNET_RADIO_PLAYLIST);
	}
	
	//--------------------------------------------------------------------------
	public Vector<RecordBase> getMembers() {
		if (mMemberIds != null && mMemberIds.length > 0) {
			return Storage.getRadioRecords(mRecordsManager, mMemberIds);
		}
		
		return new Vector<RecordBase>();
	}
}
