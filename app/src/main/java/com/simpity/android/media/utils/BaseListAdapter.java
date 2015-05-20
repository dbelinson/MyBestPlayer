package com.simpity.android.media.utils;

import java.util.Vector;

import android.database.DataSetObserver;
import android.widget.ListAdapter;

public abstract class BaseListAdapter implements ListAdapter {

	protected Vector<DataSetObserver> mDataSetObserverList = new Vector<DataSetObserver>();
	
	//----------------------------------------------------------------------
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	//----------------------------------------------------------------------
	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	//----------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return position;
	}

	//----------------------------------------------------------------------
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	//----------------------------------------------------------------------
	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	//----------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return false;
	}

	//----------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	//----------------------------------------------------------------------
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObserverList.add(observer);
	}

	//----------------------------------------------------------------------
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObserverList.remove(observer);
	}
}
