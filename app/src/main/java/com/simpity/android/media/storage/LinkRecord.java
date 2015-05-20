package com.simpity.android.media.storage;

import android.content.ContentValues;

public class LinkRecord {

	private final int mId;
	private final String mUrl;
	private int mFlags;
	private int mAvailableCounter;
	private boolean mChanged;
	
	//--------------------------------------------------------------------------
	public LinkRecord(int id, String url, int flags, int counter) {
		mId = id;
		mUrl = url;
		mFlags = flags;
		mAvailableCounter = counter;
		mChanged = false;
	}

	//--------------------------------------------------------------------------
	public int getId() {
		return mId;
	}
	
	//--------------------------------------------------------------------------
	public String getUrl() {
		return mUrl;
	}

	//--------------------------------------------------------------------------
	public void setAvailableCounter(int counter) {
		if (mAvailableCounter != counter) {
			mAvailableCounter = counter;
			mChanged = true;
			
			if ((mFlags & Storage.DEAD_LINK_FLAG) != 0 && counter == 0) {
				mFlags &= ~Storage.DEAD_LINK_FLAG;
				//service.debugInfo("Storage", "Clear dead link flag");
			}

			if (counter >= Storage.DEAD_LINK_THRESHOLD) {
				mFlags |= Storage.DEAD_LINK_FLAG;
				//service.debugInfo("Storage", "Set dead link flag");
			}
		}
	}

	//--------------------------------------------------------------------------
	public int getAvailableCounter() {
		return mAvailableCounter;
	}

	//--------------------------------------------------------------------------
	public boolean isChanged() {
		return mChanged;
	}
	
	//--------------------------------------------------------------------------
	public void getContentValues(ContentValues values) {
		values.clear();
		values.put(Storage.FLAGS, mFlags);
		values.put(Storage.NOT_AVAILABLE_COUNT, mAvailableCounter);
	}

}
