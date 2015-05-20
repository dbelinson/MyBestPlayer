package com.simpity.android.media.storage;

import android.database.Cursor;

public final class VideoRecord extends RecordBase {

	//--------------------------------------------------------------------------
	public VideoRecord(RecordsManager recordsManager, Cursor cursor, TableIndex index) {
		
		super(recordsManager, cursor, index);
		setType(Storage.STREAM_VIDEO);
	}
	
	//--------------------------------------------------------------------------
	public VideoRecord(RecordsManager recordsManager, int id, String description, 
			String url, String category, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink, boolean isLinkToRemove, 
			int linkNotAvailableCounter) {
		
		super(recordsManager, id, url, description, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, linkNotAvailableCounter);
		
		setType(Storage.STREAM_VIDEO);
		setCategory(category);
	}
	
	//--------------------------------------------------------------------------
	public VideoRecord(RecordsManager recordsManager, int id, String description, 
			String url, String category, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink, boolean isLinkToRemove) {
		
		super(recordsManager, id, url, description, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, 0);
		
		setType(Storage.STREAM_VIDEO);
		setCategory(category);
	}

	//--------------------------------------------------------------------------
	public void setCategory(String category) {
		mTextData1 = category;
	}

	//--------------------------------------------------------------------------
	public String getCategory() {
		return mTextData1;
	}
}
