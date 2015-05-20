package com.simpity.android.media.storage;

import java.net.URI;
import java.util.Collections;
import java.util.Vector;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.LinksChecker;

public class RecordsManager implements LinksChecker.OnLinksCheckerListener {
	
	protected final int HISTORY_RECORDS_COUNT = 10;
	
	private final static int ICON_EDIT			= 0;
	private final static int ICON_NEW			= 1;
	private final static int ICON_DEAD			= 2;
	private final static int ICON_PAID			= 3;
	private final static int ICON_FAVORITE_ON	= 4;
	private final static int ICON_FAVORITE_OFF	= 5;
	private final static int ICON_COUNT			= 6;
	
	private final static int[] ICON_RES_ID = {
		Res.drawable.icon_edit,
		Res.drawable.icon_marker_new,
		Res.drawable.icon_marker_na,
		Res.drawable.icon_marker_paid,
		Res.drawable.favorites,
		Res.drawable.favorites_off
	};
	
	private final Bitmap[] mIcons = new Bitmap[ICON_COUNT];
	
	private Bitmap[] mEditIcon, mFavoriteOn, mFavoriteOff;
	
	protected final Vector<GroupNameAndId> mGroups = new Vector<GroupNameAndId>();
	protected final Vector<RecordBase> mElements = new Vector<RecordBase>();
	protected final Vector<PlayList> mPlayLists = new Vector<PlayList>();

	protected final Object mElementsSync = new Object();
	
	protected int mNewLinksCount = 0;
	protected int mDeadLinksCount = 0;
	
	protected OnListChangedListener mOnListChangedListener = null;
	
	protected Context mContext = null;
	private LinksChecker mLinksChecker = null; 
	
	//--------------------------------------------------------------------------
	public interface OnListChangedListener {
    	public void onRecordListChanged();  
    }
	
	//--------------------------------------------------------------------------
	public void destroy() {
		
		for (int i=0; i<ICON_COUNT; i++) {
			if (mIcons[i] != null) {
				mIcons[i].recycle();
				mIcons[i] = null; 
			}
		}
			
		mFavoriteOn = null;
		mFavoriteOff = null;
			
		cleanup();
	}
	
	//--------------------------------------------------------------------------
	protected void cleanup() {
		
		mGroups.clear();
		mElements.clear();
		mPlayLists.clear();
		mOnListChangedListener = null;
		if (mLinksChecker != null) {
			mLinksChecker.terminate();
			mLinksChecker = null;
		}
	}
	
	//--------------------------------------------------------------------------
	public RecordsManager(Context context) {
		mContext = context;
		
		Resources resources = context.getResources();
		for (int i=0; i<ICON_COUNT; i++) {
			mIcons[i] = BitmapFactory.decodeResource(resources, ICON_RES_ID[i]);
		}
		
		mEditIcon = new Bitmap[] { mIcons[ICON_EDIT]};
		mFavoriteOn  = new Bitmap[] { mIcons[ICON_FAVORITE_ON]};
		mFavoriteOff = new Bitmap[] { mIcons[ICON_FAVORITE_OFF]};
	}
	
	//--------------------------------------------------------------------------
	public Context getContext() {
		return mContext;
	}

	//--------------------------------------------------------------------------
	public GroupNameAndId getGroupById(int groupId) {
		synchronized (mElementsSync) {
			for (GroupNameAndId group : mGroups) {
				if (group.getId() == groupId)
					return group;
			}
		}
		return null;
	}
	
	//--------------------------------------------------------------------------
	public Bitmap getNewRecordMarker() {
		return mIcons[ICON_NEW];
	}
	
	//--------------------------------------------------------------------------
	public Bitmap getDeadRecordMarker() {
		return mIcons[ICON_DEAD];
	}
	
	//--------------------------------------------------------------------------
	public Bitmap getPaidRecordMarker() {
		return mIcons[ICON_PAID];
	}
	
	//--------------------------------------------------------------------------
	public Bitmap[] getFavoriteONRecordMarker() {
		return mFavoriteOn;
	}
	
	//--------------------------------------------------------------------------
	public Bitmap[] getFavoriteOFFRecordMarker() {
		return mFavoriteOff;
	}
	
	//--------------------------------------------------------------------------
	public Bitmap[] getEditIcon() {
		return mEditIcon;
	}

	//--------------------------------------------------------------------------
	protected String getHostName(String url) {
		try {
			URI hostResolver = new URI(url);
			String hostName = hostResolver.getHost();
			
			boolean flag = false;
			for (int i = 0; i < hostName.length(); i++) {
				if (Character.isLetter(hostName.charAt(i)) && hostName.charAt(i) != '.') {
					flag = true;
					break;
				}	
			}
			if (!flag) {
				return null;
			}
			
			int firstInd = hostName.indexOf(".");
			int lastInd = hostName.lastIndexOf(".");
			if (firstInd > 0 && lastInd > 0 && firstInd != lastInd) {
				while(true) {
					int temp = hostName.indexOf(".", firstInd + 1);
					if (temp < lastInd) {
						firstInd = temp;
					} else
						break;
				}
				hostName = hostName.substring(firstInd + 1);
			}
			return hostName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//--------------------------------------------------------------------------
	public void sortList() {
		if (mElements.size() > 0 ) {
			String sort_key = mContext.getString(Res.string.pref_links_list_use_sorting_key);
			if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(sort_key, true)) {
				synchronized (mElementsSync) {
					try {
						Collections.sort(mElements);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	//--------------------------------------------------------------------------
	public boolean updateRecord(RecordBase record) {
		if (record == null)
			return false;
		
		boolean result = Storage.updateRecord(mContext, record);
		sortList();
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}
		
		return result;
	}
	
	//--------------------------------------------------------------------------
	protected boolean updateRecordSkipSorting(RecordBase record) {
		if (record == null)
			return false;
		
		boolean result = Storage.updateRecord(mContext, record);
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}
		return result;
	}
	
	//--------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	protected void performCheckLinksAvaliability() {
		
		Vector<RecordBase> elements = null;
		synchronized (mElementsSync) {
			elements = (Vector<RecordBase>)mElements.clone();	
		}
		
		mLinksChecker =  LinksChecker.CheckLinks(elements, this);
	}

	//---------------------------------------------------------------------------------------
	@Override
	public void onFinished() {
		mLinksChecker = null;
	}

	//---------------------------------------------------------------------------------------
	@Override
	public void onLinkChecked(RecordBase record, int checkState, int currentRecordNum, int totalRecCount) {
		
		if (checkState == LinksChecker.STATE_NOT_AVALIABLE) {
			record.setLinkNotAvailable();
		} else if (checkState == LinksChecker.STATE_AVALIABLE) {
			record.resetLinkNotAvailable();
		} else {
			return;
		}

		updateRecord(record);
	}
	
	//---------------------------------------------------------------------------------------
	public void setOnListChangedListener(OnListChangedListener listener) {
		mOnListChangedListener = listener;
	}
	
	//----------------------------------------------------------------------
	public String getRecordTitle(RecordBase record) {
		return record.getDescription();
	}

	//-------------------------------------------------------------------------
	public Vector<GroupNameAndId> getAllGroupForGroup(int group_id) {
		return new Vector<GroupNameAndId>(0);
	}
	
	//-------------------------------------------------------------------------
	public Vector<RecordBase> getAllRecordsForGroup(int group_id) {
		return new Vector<RecordBase>();
	}

	//-------------------------------------------------------------------------
	public boolean remove(RecordBase record) {
		boolean result = false;
		synchronized (mElementsSync) {
			result = mElements.removeElement(record);
		}

		result = Storage.deleteRecord(mContext, record);
		if (result && mOnListChangedListener != null) {
			mOnListChangedListener.onRecordListChanged();
		}
		
		return result;
	}

	//The function of base class is empty
	public void updateAllRecords()
	{
	}
}
