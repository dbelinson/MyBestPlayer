package com.simpity.android.media.storage;

import android.database.Cursor;

public final class CameraRecord extends RecordBase {

	//--------------------------------------------------------------------------
	public CameraRecord(RecordsManager recordsManager, Cursor cursor, TableIndex index) {
		super(recordsManager, cursor, index);
		setType(Storage.JPEG_CAMERA);
	}
	
	//--------------------------------------------------------------------------
	public CameraRecord(RecordsManager recordsManager, int id, String description, 
			String url, int refreshPeriod, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink, boolean isLinkToRemove, 
			int linkNotAvailableCounter) {
		
		super(recordsManager, id, url, description, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, linkNotAvailableCounter);
		
		setRefreshPeriod(refreshPeriod);
		setType(Storage.JPEG_CAMERA);
	}
	
	//--------------------------------------------------------------------------
	public CameraRecord(RecordsManager recordsManager, int id, String description, 
			String url, int refreshPeriod, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink, boolean isLinkToRemove) {
		
		super(recordsManager, id, url, description, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, 0);
		
		setRefreshPeriod(refreshPeriod);
		setType(Storage.JPEG_CAMERA);
	}

	//--------------------------------------------------------------------------
	public void setRefreshPeriod(int refreshPeriod) {
		mIntData0 = refreshPeriod;
	}

	//--------------------------------------------------------------------------
	public int getRefreshPeriod() {
		return mIntData0;
	}
}
