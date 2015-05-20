package com.simpity.android.media.storage;

import java.util.Vector;

import android.graphics.Bitmap;

import com.simpity.android.media.controls.fasttree.FastTreeItem;

public class GroupRecord implements FastTreeItem, Comparable<GroupRecord> {

	private final Vector<FastTreeItem> mChildren = new Vector<FastTreeItem>();
	private final String mName, mUpperName;
	
	public GroupRecord(String name) {
		mUpperName = name.toUpperCase();
		if (name.length() > 0 && name.charAt(0) != mUpperName.charAt(0)) {
			name = mUpperName.substring(0, 1) + name.substring(1);
		}
		
		mName = name;
	}
	
	public void add(FastTreeItem item) {
		mChildren.add(item);
	}
	
	@Override
	public FastTreeItem getChild(int index) {
		return mChildren.get(index);
	}

	@Override
	public int getChildCount() {
		return mChildren.size();
	}

	@Override
	public Bitmap getIcon() {
		return null;
	}

	@Override
	public Bitmap[] getRightIcons() {
		return null;
	}

	@Override
	public String getSubTitle() {
		return null;
	}

	@Override
	public String getTitle() {
		return mName;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isGroup() {
		return true;
	}

	@Override
	public boolean isPaid() {
		return false;
	}

	@Override
	public int compareTo(GroupRecord another) {
		return mUpperName.compareTo(another.mUpperName);
	}
	
	public int compareUpperName(String upper_name) {
		return mUpperName.compareTo(upper_name);
	}
}
