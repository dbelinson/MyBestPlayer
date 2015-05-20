package com.simpity.android.media.radio;

import java.util.Vector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.simpity.android.media.Res;
import com.simpity.android.media.storage.RadioPlayList;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.Storage;

public class PlaylistEditor implements ListAdapter, AdapterView.OnItemClickListener {

	private final RadioSelectActivity mActivity;
	private RadioPlayList mPlaylist;
	private Vector<RecordBase> mMembers;
	private Button mMoveUpButton, mMoveDownButton, mRemoveButton;
	private ListView mListView;
	private int mSelectedIndex = -1;
	
	//--------------------------------------------------------------------------
	public PlaylistEditor(RadioSelectActivity activity) {
		mActivity = activity;
		
		mMoveUpButton = (Button)activity.findViewById(Res.id.RadioPlaylistMoveUp);
		mMoveUpButton.setOnClickListener(new MoveUpListener());
		
		mMoveDownButton = (Button)activity.findViewById(Res.id.RadioPlaylistMoveDown);
		mMoveDownButton.setOnClickListener(new MoveDownListener());
		
		mRemoveButton = (Button)activity.findViewById(Res.id.RadioPlaylistRemove);
		mRemoveButton.setOnClickListener(new RemoveListener());
		
		mListView = (ListView)activity.findViewById(Res.id.RadioPlaylistEditList);
		mListView.setAdapter(this);
		mListView.setOnItemClickListener(this);
	}

	//--------------------------------------------------------------------------
	public void setPlaylist(RadioPlayList playlist) {
		mPlaylist = playlist;
		mMembers = playlist.getMembers();
		setSelectedIndex(playlist.getMemberCount() > 0 ? 0 : -1);
	}

	//--------------------------------------------------------------------------
	public RadioPlayList getPlaylist() {
		return mPlaylist;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return mMembers != null ? mMembers.size() : 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return position;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = LayoutInflater.from(mActivity).inflate(Res.layout.playlist_item, null);
		}
		
		RecordBase record = mMembers.get(position);
		
		ImageView check_icon = (ImageView)convertView.findViewById(Res.id.PlaylistItemCheck);
		if (check_icon != null) {
			check_icon.setVisibility(mSelectedIndex == position ? View.VISIBLE : View.INVISIBLE);
		}
		
		TextView text_view = (TextView)convertView.findViewById(Res.id.PlaylistItemText);
		if (text_view != null) {
			text_view.setText(record.getTitle());
			text_view.setTypeface(mSelectedIndex == position ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		}
		
		text_view = (TextView)convertView.findViewById(Res.id.PlaylistItemUrl);
		if (text_view != null) {
			text_view.setText(record.getUrl());
		}

		return convertView;
	}

	//--------------------------------------------------------------------------
	private final Vector<DataSetObserver> mObserver = new Vector<DataSetObserver>();
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mObserver.add(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mObserver.remove(observer);
	}
	
	//--------------------------------------------------------------------------
	private class MoveUpListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mPlaylist != null && mPlaylist.moveMember(mSelectedIndex, -1)) {
				Storage.updateRecord(mActivity, mPlaylist);
				mMembers = mPlaylist.getMembers();
				setSelectedIndex(mSelectedIndex-1);
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private class MoveDownListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mPlaylist != null && mPlaylist.moveMember(mSelectedIndex, 1)) {
				Storage.updateRecord(mActivity, mPlaylist);
				mMembers = mPlaylist.getMembers();
				setSelectedIndex(mSelectedIndex+1);
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private class RemoveListener implements View.OnClickListener, DialogInterface.OnClickListener {
		
		@Override
		public void onClick(View v) {
			if (mPlaylist != null && mSelectedIndex >= 0 && mSelectedIndex < mMembers.size()) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				builder.setMessage(mActivity.getString(Res.string.remove_from_playlist,
						//mMembers.get(mSelectedIndex).getTitle()));
						mMembers.get(mSelectedIndex).getUrl()));
				builder.setPositiveButton(Res.string.yes, this);
				builder.setNegativeButton(Res.string.no, null);
				builder.setCancelable(true);
				builder.show();
			}
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (mPlaylist.remove(mActivity, mMembers.get(mSelectedIndex))) {
				Storage.updateRecord(mActivity, mPlaylist);
				mMembers = mPlaylist.getMembers();
				if (mSelectedIndex >= mMembers.size()) {
					mSelectedIndex = mMembers.size() - 1;
				}
				setSelectedIndex(mSelectedIndex);
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
		setSelectedIndex(position);
	}
	
	//--------------------------------------------------------------------------
	private void setSelectedIndex(int position) {
		mSelectedIndex = position;
		
		if (position >= 0) {
			mMoveUpButton.setEnabled(position > 0);
			mMoveDownButton.setEnabled(position < mPlaylist.getMemberCount()-1);
			mRemoveButton.setEnabled(true);
		} else {
			mMoveUpButton.setEnabled(false);
			mMoveDownButton.setEnabled(false);
			mRemoveButton.setEnabled(false);
		}
		
		mListView.invalidateViews();
	}
}
