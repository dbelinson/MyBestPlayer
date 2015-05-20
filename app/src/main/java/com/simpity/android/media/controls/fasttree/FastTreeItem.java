package com.simpity.android.media.controls.fasttree;

import android.graphics.Bitmap;

public interface FastTreeItem {

	public boolean isGroup();
	public int getChildCount();
	public FastTreeItem getChild(int index);
	
	public String getTitle();
	public String getSubTitle();
	public Bitmap getIcon();
	public Bitmap[] getRightIcons();
	public boolean isPaid();
	public boolean isAvailable();
}
