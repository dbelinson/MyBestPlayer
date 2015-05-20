package com.simpity.android.media.storage;

import java.net.URL;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.controls.fasttree.FastTreeItem;

public class RecordBase implements FastTreeItem, Comparable<RecordBase> {

	public static final int SET_LINK_TO_DEAD_COUNTER = 5;

	private int mId;
	private String mUrl, mGroupName, mDescription;
	private int mGroupId;
	private int mType;
	private boolean mFavorite, mNewLink, mDeadLink;
	private long mLastAccessedDateInMillis;
	private String mHostNameCache;
	protected String mTextData0, mTextData1, mTextData2, mTextData3;
	protected int mIntData0, mIntData1, mIntData2, mIntData3;
	protected final RecordsManager mRecordsManager;

	//--------------------------------------------------------------------------
	public RecordBase(RecordsManager recordsManager, Cursor cursor, TableIndex index) {
		
		mRecordsManager = recordsManager;
		
		if (index.id_index >= 0) {
			mId = cursor.getInt(index.id_index);
		}

		if (index.url_index >= 0) {
			mUrl = cursor.getString(index.url_index);
		}

		if (index.description_index >= 0) {
			mDescription = cursor.getString(index.description_index);
		}

		if (index.text_data0_index >= 0) {
			mTextData0 = cursor.getString(index.text_data0_index);
		}

		if (index.text_data1_index >= 0) {
			mTextData1 = cursor.getString(index.text_data1_index);
		}

		if (index.text_data2_index >= 0) {
			mTextData2 = cursor.getString(index.text_data2_index);
		}

		if (index.text_data3_index >= 0) {
			mTextData3 = cursor.getString(index.text_data3_index);
		}

		if (index.int_data0_index >= 0) {
			mIntData0 = cursor.getInt(index.int_data0_index);
		}

		if (index.int_data1_index >= 0) {
			mIntData1 = cursor.getInt(index.int_data1_index);
		}

		if (index.int_data2_index >= 0) {
			mIntData2 = cursor.getInt(index.int_data2_index);
		}

		if (index.int_data3_index >= 0) {
			mIntData3 = cursor.getInt(index.int_data3_index);
		}

		if (index.type_index >= 0) {
			mType = cursor.getInt(index.type_index);
		}

		if (index.group_id_index >= 0) {
			mGroupId = cursor.getInt(index.group_id_index);
		}

		if (index.last_acceessed_date_index >= 0) {
			mLastAccessedDateInMillis = cursor.getLong(index.last_acceessed_date_index);
		}
		
		if (index.flags_index >= 0) {
			int flags = cursor.getInt(index.flags_index);
			mFavorite	= ((flags & Storage.FAVORITE_FLAG) != 0); 
			mNewLink	= ((flags & Storage.NEW_LINK_FLAG) != 0); 
			mDeadLink	= ((flags & Storage.DEAD_LINK_FLAG) != 0);
		}
	}

	//--------------------------------------------------------------------------
	public RecordBase(RecordsManager recordsManager, int id, String url, 
			String description, boolean favorite, int group_id, 
			long lastAccessedDateInMillis, boolean isNewLink,
			boolean deadLinkState, int linkNotAvailableCounter) {
		
		mRecordsManager = recordsManager;
		
		setId(id);
		setUrl(url);
		setDescription(description);
		setFavorite(favorite);
		setLastAccessedDate(lastAccessedDateInMillis);
		setNewState(isNewLink);
		setLinkDeadState(deadLinkState);
		mGroupId = group_id;
		mIntData1 = linkNotAvailableCounter;
	}

	//--------------------------------------------------------------------------
	public ContentValues getContentValues() { 
		ContentValues values = new ContentValues();
		int flags = mNewLink ? Storage.NEW_LINK_FLAG : 0;
	
		if (isDeadLink())
			flags |= Storage.DEAD_LINK_FLAG;
	
		if (mFavorite)
			flags |= Storage.FAVORITE_FLAG;
		
		values.put(Storage.TYPE,				mType);
		values.put(Storage.DESCRIPTION,			mDescription);
		values.put(Storage.URL, 				mUrl);
		values.put(Storage.GROUP_ID,			mGroupId);
		values.put(Storage.TEXT_DATA0,			mTextData0 != null ? mTextData0 : "");
		values.put(Storage.TEXT_DATA1,			mTextData1 != null ? mTextData1 : "");
		values.put(Storage.TEXT_DATA2,			mTextData2 != null ? mTextData2 : "");
		values.put(Storage.TEXT_DATA3,			mTextData3 != null ? mTextData3 : "");
		values.put(Storage.INT_DATA0,			mIntData0);
		values.put(Storage.INT_DATA1,			mIntData1);
		values.put(Storage.INT_DATA2,			mIntData2);
		values.put(Storage.INT_DATA3,			mIntData3);
		values.put(Storage.FLAGS,				flags);
		values.put(Storage.LAST_ACCESSED_DATE, 	mLastAccessedDateInMillis);
		
		return values;
	}
	
	//--------------------------------------------------------------------------
	public void setId(int id) {
		mId = id;
	}

	//--------------------------------------------------------------------------
	public int getId() {
		return mId;
	}

	public void setType(int mType) {
		this.mType = mType;
	}

	public int getType() {
		return mType;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isPaid() {
		if (!VersionConfig.IS_PRO_VERSION) {
			return false;
		} else {
			GroupNameAndId group = mRecordsManager.getGroupById(mGroupId);
			return group != null && group.isPaid();
		}
	}

	//--------------------------------------------------------------------------
	public void setNewState(boolean isLinkNew) {
		mNewLink = isLinkNew;
	}

	//--------------------------------------------------------------------------
	public boolean isNewLink() {
		return mNewLink;
	}

	//--------------------------------------------------------------------------
	public void setLinkDeadState(boolean deadLinkState) {
		mDeadLink = deadLinkState;
	}

	//--------------------------------------------------------------------------
	public boolean isDeadLink() {
		return mDeadLink;
	}

	//--------------------------------------------------------------------------
	public void setUrl(String url) {
		mHostNameCache = null;
		mUrl = url;
	}

	//--------------------------------------------------------------------------
	public String getUrl() {
		return mUrl;
	}

	//--------------------------------------------------------------------------
	public void setGroup(int group_id, String group_name) {
		mGroupId = group_id;
		mGroupName = group_name;
	}

	//--------------------------------------------------------------------------
	public void setGroup(int group_id) {
		mGroupId = group_id;
	}

	//--------------------------------------------------------------------------
	public void setGroupName(String group_name) {
		mGroupName = group_name;
	}

	//--------------------------------------------------------------------------
	public String getGroupName() {
		return mGroupName;
	}

	//--------------------------------------------------------------------------
	public int getGroupId() {
		return mGroupId;
	}

	//--------------------------------------------------------------------------
	public void setFavorite(boolean favorite) {
		mFavorite = favorite;
	}

	//--------------------------------------------------------------------------
	public boolean isFavorite() {
		return mFavorite;
	}

	//--------------------------------------------------------------------------
	public void setLastAccessedDate(long lastAccessedDateInMillis) {
		mLastAccessedDateInMillis = lastAccessedDateInMillis;
	}

	//--------------------------------------------------------------------------
	public long getLastAccessedDate() {
		return mLastAccessedDateInMillis;
	}

	//--------------------------------------------------------------------------
	public void setDescription(String description) {
		mDescription = description;
	}

	//--------------------------------------------------------------------------
	public String getDescription() {
		return mDescription;
	}

	//--------------------------------------------------------------------------
	public String getHostName() {
		if (mHostNameCache != null) {
			return mHostNameCache;
		}
		try {
			URL hostResolver = new URL(mUrl);
			String hostName = hostResolver.getHost();

			boolean flag = false;
			for (int i = 0; i < hostName.length(); i++) {
				if (Character.isLetter(hostName.charAt(i)) && hostName.charAt(i) != '.') {
					flag = true;
					break;
				}
			}
			if (!flag) {
				mHostNameCache = null;
				return null;
			}

			int firstInd = hostName.indexOf(".");
			int lastInd = hostName.lastIndexOf(".");
			if (firstInd > 0 && lastInd > 0 && firstInd != lastInd) {
				while(true) {
					int temp = hostName.indexOf(".", firstInd + 1);
					if (temp < lastInd) {
						firstInd = temp;
					} else {
						break;
					}
				}
				hostName = hostName.substring(firstInd + 1);
			}

			mHostNameCache = hostName;
			return hostName;

		} catch (Exception e) {

			e.printStackTrace();
			mHostNameCache = null;
			return null;
		}
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	@Override
	public FastTreeItem getChild(int index) {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getChildCount() {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap getIcon() {
		return isPaid() ? mRecordsManager.getPaidRecordMarker() : 
			isDeadLink() ? mRecordsManager.getDeadRecordMarker() : 
			(isNewLink() ? mRecordsManager.getNewRecordMarker() : null);
	}

	//--------------------------------------------------------------------------
	@Override
	public Bitmap[] getRightIcons() {
		if (isPaid())
			return null;

		return isFavorite() ? mRecordsManager.getFavoriteONRecordMarker() : 
			mRecordsManager.getFavoriteOFFRecordMarker();
	}

	//--------------------------------------------------------------------------
	@Override
	public String getSubTitle() {
		if (isPaid())
			return "Paid";

		return getUrl();
	}

	//--------------------------------------------------------------------------
	@Override
	public String getTitle() {
		return mRecordsManager.getRecordTitle(this);
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isGroup() {
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isAvailable() {
		return !isDeadLink();
	}

	//--------------------------------------------------------------------------
	public void setLinkNotAvailable() {
		mIntData1++;
	}

	//--------------------------------------------------------------------------
	public void setLinkNotAvailable(int notAvaliableCount) {
		if (notAvaliableCount > mIntData1) {
			mIntData1 = notAvaliableCount;
		} else {
			mIntData1++;
		}
	}

	//--------------------------------------------------------------------------
	public void resetLinkNotAvailable() {
		mIntData1 = 0;
	}

	//--------------------------------------------------------------------------
	public int getLinkNotAvailableCount() {
		return mIntData1;
	}
	
	//--------------------------------------------------------------------------
	public RecordsManager getRecordsManager() {
		return mRecordsManager;
	}

	//--------------------------------------------------------------------------
	@Override
	public int compareTo(RecordBase another) {
		if (mDescription != null) {
			if (another.getDescription() != null) {
				return (mDescription.compareTo(another.getDescription()));
			} else {
				return -1;
			}
		} else {
			return 1;
		}
	}
}
