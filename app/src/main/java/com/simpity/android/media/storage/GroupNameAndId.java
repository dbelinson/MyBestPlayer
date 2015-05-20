package com.simpity.android.media.storage;

import java.util.Vector;

import android.database.Cursor;
import android.graphics.Bitmap;

import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.controls.fasttree.FastTreeItem;

public class GroupNameAndId extends RecordBase {

	private final boolean mPaid;
	
	//--------------------------------------------------------------------------
	public GroupNameAndId(RecordsManager manager, Cursor cursor, TableIndex index) {
		super(manager, cursor, index);
		mPaid = false;
	}
	
	//--------------------------------------------------------------------------
	public GroupNameAndId(RecordsManager manager, String name, int id, 
			int groupType, int parentGroupID){
		super (manager, id, "", name, false, parentGroupID, 0, false, false, 0);
		setType(groupType);
		mPaid = false;
	}
	
	//--------------------------------------------------------------------------
	public GroupNameAndId(RecordsManager manager, String name, int id, 
			int groupType, int parentGroupID, boolean isPayed) {
		super (manager, id, "", name, false, parentGroupID, 0, false, false, 0);
		setType(groupType);
		mPaid = isPayed;
	}
	
	//--------------------------------------------------------------------------
	public boolean isRootGroup(){
		return getGroupId() == -1;
	}
	
	//--------------------------------------------------------------------------
	public boolean isPaid() {
		return !VersionConfig.IS_PRO_VERSION && mPaid;
	}

	//--------------------------------------------------------------------------
	@Override
	public FastTreeItem getChild(int index) {
		try {
			Vector<GroupNameAndId> subGroups = mRecordsManager.getAllGroupForGroup(getId());
			Vector<RecordBase> childElements = mRecordsManager.getAllRecordsForGroup(getId());
			
			if(index >= 0 && index < subGroups.size()){
				return subGroups.elementAt(index);
			} else {
				index = index - subGroups.size();
				if (index >= 0 && index < childElements.size()) {
					return childElements.get(index);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getChildCount() {
		try {
			return mRecordsManager.getAllGroupForGroup(getId()).size() 
				+ mRecordsManager.getAllRecordsForGroup(getId()).size();
				
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap getIcon() {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap[] getRightIcons() {
		
		if(isPaid()) {
			return new Bitmap[] { mRecordsManager.getPaidRecordMarker()};
		}
		
		try {
			Vector<GroupNameAndId> subGroups = null;
			Vector<RecordBase> childElements = null;
			
			subGroups = mRecordsManager.getAllGroupForGroup(getId());
			childElements = mRecordsManager.getAllRecordsForGroup(getId());
			
			Bitmap newMarker = null;
			Bitmap deadMarker = null;
			
			for (RecordBase record : childElements) {
				if (newMarker != null && deadMarker != null)
					break;
				
				if (record.isNewLink() && newMarker == null) {
					newMarker = mRecordsManager.getNewRecordMarker();
				} else if(record.isDeadLink() && deadMarker == null) {
					deadMarker = mRecordsManager.getDeadRecordMarker();
				}
			}
			
			if (newMarker == null || deadMarker == null) {
				for (GroupNameAndId group : subGroups) {
					if (newMarker == null || deadMarker == null) {
						Bitmap[] temp = group.getRightIcons();
						if (newMarker == null)
							newMarker = temp[0];
						
						if (deadMarker == null)
							deadMarker = temp[1];
					} else
						break;
				}
			}
			
			return new Bitmap[] {newMarker, deadMarker};
			
		} catch (Exception e) {
			return null;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public String getSubTitle() {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public String getTitle() {
		return getDescription();
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isGroup() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isAvailable() {
		return true;
	}
}
