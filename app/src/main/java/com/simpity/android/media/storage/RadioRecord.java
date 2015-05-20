package com.simpity.android.media.storage;

import android.database.Cursor;

public final class RadioRecord extends RecordBase {

	//--------------------------------------------------------------------------
	public RadioRecord(RecordsManager recordsManager, Cursor cursor, TableIndex index) {
		super(recordsManager, cursor, index);
		setType(Storage.INTERNET_RADIO);
	}

	//--------------------------------------------------------------------------
	public RadioRecord(RecordsManager recordsManager, int id, String radio_station_name, 
			String url, String content_description, String genre, boolean favorite, 
			int group_id, long lastAccessedDateInMillis, boolean isNewLink, 
			boolean isLinkToRemove, int linkNotAvailableCounter) {

		super(recordsManager, id, url, radio_station_name, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, linkNotAvailableCounter);

		setType(Storage.INTERNET_RADIO);
		setStationName(radio_station_name);
		setContentDescription(content_description);
		setGenre(genre);
	}

	//--------------------------------------------------------------------------
	public RadioRecord(RecordsManager recordsManager, int id, String radio_station_name, String url, 
			String content_description, String genre, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink, boolean isLinkToRemove) {

		super(recordsManager, id, url, radio_station_name, favorite, group_id, 
				lastAccessedDateInMillis, isNewLink, isLinkToRemove, 0);

		setType(Storage.INTERNET_RADIO);
		setStationName(radio_station_name);
		setContentDescription(content_description);
		setGenre(genre);
	}

	//--------------------------------------------------------------------------
	public void setStationName(String radio_station_name) {
		setDescription(radio_station_name);
	}

	//--------------------------------------------------------------------------
	public String getStationName() {
		String station = getDescription();
		return station == null ? "" : station;
	}

	//--------------------------------------------------------------------------
	public void setContentDescription(String content_description) {
		mTextData0 = content_description;
	}

	//--------------------------------------------------------------------------
	public String getContentDescription() {
		return mTextData0;
	}

	//--------------------------------------------------------------------------
	public void setGenre(String genre) {
		mTextData1 = genre;
	}

	//--------------------------------------------------------------------------
	public String getGenre() {
		return mTextData1;
	}

	//--------------------------------------------------------------------------
	/*@Override
	public Bitmap[] getRightIcons() {
		return mRecordsManager.getEditIcon();
	}*/

	//--------------------------------------------------------------------------
	@Override
	public int compareTo(RecordBase another_base) {
		if (another_base instanceof RadioRecord) {

			RadioRecord another = (RadioRecord)another_base;
			if (getStationName() != null) {
				if (another.getStationName() != null) {
					return getStationName().compareTo(another.getStationName());
				} else {
					return -1;
				}
			} else {
				return 1;
			}
		}

		return super.compareTo(another_base);
	}
}
