package com.simpity.android.media.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.simpity.android.media.controls.fasttree.FastTreeItem;
import com.simpity.android.media.utils.Utilities;

public class PlayList extends RecordBase {

	protected int[] mMemberIds;

	//--------------------------------------------------------------------------
	public PlayList(RecordsManager manager, Cursor cursor, TableIndex index) {
		super (manager, cursor, index);		
		mMemberIds = Utilities.parseIdList(mTextData3);
	}

	//--------------------------------------------------------------------------
	public PlayList(RecordsManager manager, int id, String title) {
		super (manager, id, null, title, false, 0, 0, false, false, 0);
		mMemberIds = new int[0];
	}

	//--------------------------------------------------------------------------
	@Override
	public ContentValues getContentValues() {
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<mMemberIds.length; i++) {
			builder.append(mMemberIds[i]);
			builder.append(';');
		}
		
		mTextData3 = builder.toString(); 
		
		return super.getContentValues();
	}
	
	//--------------------------------------------------------------------------
	public int getMemberCount() {
		return mMemberIds == null ? 0 : mMemberIds.length;
	}
	
	//--------------------------------------------------------------------------
	public boolean isExists(RecordBase record) {
		for(int i=0; i<mMemberIds.length; i++) {
			if (mMemberIds[i] == record.getId())
				return true;
		}
		
		return false;
	}

	//--------------------------------------------------------------------------
	public boolean addNew(Context context, RecordBase record) {
		if (!isExists(record)) {
			int[] old_ids = mMemberIds;
			int count = old_ids.length;
			
			mMemberIds = new int[count + 1];
			System.arraycopy(old_ids, 0, mMemberIds, 0, count);
			mMemberIds[count] = record.getId();
			
			if (!Storage.updateRecord(context, this)) {
				mMemberIds = old_ids;
				return false;
			}
			
			return true;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	public boolean remove(Context context, RecordBase record) {
		int count = mMemberIds.length;
		for (int i=0; i<count; i++) {
			if (mMemberIds[i] == record.getId()) {

				int[] old_ids = mMemberIds;
				mMemberIds = new int[count - 1];
				
				if (i > 0) {
					System.arraycopy(old_ids, 0, mMemberIds, 0, i);
				}
				
				if (i+1 < count) {
					System.arraycopy(old_ids, i+1, mMemberIds, i, count-i-1);
				}
				
				if (!Storage.updateRecord(context, this)) {
					mMemberIds = old_ids;
					return false;
				}
				
				return true;
			}
		}
		
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public FastTreeItem getChild(int index) {
		return null; //mMembers.get(index);
	}

	//--------------------------------------------------------------------------
	@Override
	public int getChildCount() {
		return 0; //mMembers.size();
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap getIcon() {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap[] getRightIcons() {
		return isFavorite() ? mRecordsManager.getFavoriteONRecordMarker() : 
			mRecordsManager.getFavoriteOFFRecordMarker();
	}
	
	//--------------------------------------------------------------------------
	@Override
	public String getSubTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public String getTitle() {
		return getDescription();
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isAvailable() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isGroup() {
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isPaid() {
		return false;
	}

	//--------------------------------------------------------------------------
	public boolean moveMember(int index, int offset) {
		int new_index = index + offset;
		
		if (offset == 0 || index < 0 || index >= mMemberIds.length 
				|| new_index < 0 || new_index >= mMemberIds.length) {
			return false;
		}
		
		if (new_index < index) {
			
			while (index > new_index) {
				int id = mMemberIds[index];
				mMemberIds[index] = mMemberIds[index-1];
				mMemberIds[index-1] = id;
				index--;
			}
			
		} else {
			
			while (index < new_index) {
				int id = mMemberIds[index];
				mMemberIds[index] = mMemberIds[index+1];
				mMemberIds[index+1] = id;
				index++;
			}
		}
		
		return true;
	}
}
