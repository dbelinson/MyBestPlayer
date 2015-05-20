package com.simpity.android.media.storage;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;

public abstract class RecordFactory {

	public final Vector<RecordBase> mRecords = new Vector<RecordBase>();
	protected final RecordsManager mRecordsManager;
	
	//--------------------------------------------------------------------------
	public RecordFactory(RecordsManager manager) {
		mRecordsManager = manager;
	}

	//--------------------------------------------------------------------------
	public abstract RecordBase addRecord(Cursor cursor, TableIndex index);

	//--------------------------------------------------------------------------
	public PlayList createPlaylist(Cursor cursor, TableIndex index) {
		return new PlayList(mRecordsManager, cursor, index);
	}

	//--------------------------------------------------------------------------
	public Context getContext() {
		return mRecordsManager.getContext();
	}

	//--------------------------------------------------------------------------
	public RecordsManager getRecordsManager() {
		return mRecordsManager;
	}
}
