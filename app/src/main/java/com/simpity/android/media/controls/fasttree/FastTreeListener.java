package com.simpity.android.media.controls.fasttree;

public interface FastTreeListener {
	public void onFastTreeItemClick(FastTreeItem item);
	public void onFastTreeItemLongClick(FastTreeItem item);
	public void onFastTreeRightIconClick(FastTreeItem item, int number);
	public void onFastTreeRightIconLongClick(FastTreeItem item, int number);
}
