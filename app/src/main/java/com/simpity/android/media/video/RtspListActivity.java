package com.simpity.android.media.video;

import java.util.Calendar;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.simpity.android.media.Ad;
import com.simpity.android.media.BaseLinkListActivity;
import com.simpity.android.media.FourTabActivity;
import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.controls.fasttree.FastTree;
import com.simpity.android.media.controls.fasttree.FastTreeItem;
import com.simpity.android.media.dialogs.ShareLinkDialog;
import com.simpity.android.media.player.PlayerActivity;
import com.simpity.android.media.services.UpdateService;
import com.simpity.android.media.storage.GroupNameAndId;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.VideoRecord;
import com.simpity.android.media.utils.BaseListAdapter;
import com.simpity.android.media.utils.Command;
import com.simpity.android.media.utils.DefaultMenu;

public class RtspListActivity extends BaseLinkListActivity {

	private EditText mUrlEditor, mDescriptionEditor;
	private Spinner mCategoryEditor;

	private RtspFavoritesAdapter mFavoritesAdapter;
	private RtspHistoryAdapter mHistoryAdapter;

	private VideoRecord mCurrentItem = null;
	private GroupNameAndId mCurrentGroup = null;
	private RtspRecordsManager mRecordsManager;

	private ShareLinkDialog shareLnkDlg = null;
	//private AlertDialog deadLinkDlg = null;
	private AlertDialog changeCategoryDlg = null;
	
	private Ad mAdView;

	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRecordsManager = new RtspRecordsManager(this, this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		setContentView(Res.layout.rtsp_select);

		initAdView();
		init(savedInstanceState);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		//SAVE STATES
		int savedCategoryEditorPos = mCategoryEditor.getSelectedItemPosition();
		String savedUrlEditorText = mUrlEditor.getText().toString();
		String savedDescriptionEditorText = mDescriptionEditor.getText().toString();
		//String savedStatusBarText = statusBarTextView.getText().toString();
		//int savedStatusBarVisibility = statusBarTextView.getVisibility();

		switch (getCurrentPage()) {
		case SEARCH_COMMAND:
			mSavedSearchLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedSearchLinksListState);
			break;
			
		case ALL_LINKS_PAGE:
			mSavedAllLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedAllLinksListState);
		}

		Parcelable savedFavoritesListState = mFavoritesList.onSaveInstanceState();
		Parcelable savedHistoryListState = mHistoryList.onSaveInstanceState();

		Bundle savedState = new Bundle();
		super.onSaveInstanceState(savedState);


		//--------------------------------------------
		super.onConfigurationChanged(newConfig);
		setContentView(Res.layout.rtsp_select);
		
		initAdView();
		//--------------------------------------------

		//RESTORE STATES
		init(savedState);

		mCategoryEditor.setSelection(savedCategoryEditorPos);
		mUrlEditor.setText(savedUrlEditorText);
		mDescriptionEditor.setText(savedDescriptionEditorText);
		//statusBarTextView.setText(savedStatusBarText);
		//statusBarTextView.setVisibility(savedStatusBarVisibility);

		switch (getCurrentPage()) {
		case SEARCH_COMMAND:
			if (mSavedSearchLinksListState != null) {
				mAllLinksList.restoreState(mSavedSearchLinksListState);
			}
			mSearchEditor.setText(mAllLinksList.getFilter());
			break;
			
		case ALL_LINKS_PAGE:
			if (mSavedAllLinksListState != null) {
				mAllLinksList.restoreState(mSavedAllLinksListState);
			}
			mSearchEditor.getText().clear();
			break;
		}

		mFavoritesList.onRestoreInstanceState(savedFavoritesListState);
		mHistoryList.onRestoreInstanceState(savedHistoryListState);
	}

	//--------------------------------------------------------------------------
	private void init(Bundle savedInstanceState) {

		initTabs(savedInstanceState, this);

		mSearchEditor = (EditText)findViewById(Res.id.rtsp_search_filter);

		mAllLinksList = (FastTree) findViewById(Res.id.rtsp_all_links_FastTree);
		mFavoritesList = (ListView) findViewById(Res.id.rtsp_favorites);
		mHistoryList = (ListView) findViewById(Res.id.rtsp_history);

		mFavoritesList.setOnItemClickListener(mItemClickListener);
		mHistoryList.setOnItemClickListener(mItemClickListener);

		mFavoritesList.setOnItemLongClickListener(mItemLongClickListener);
		mHistoryList.setOnItemLongClickListener(mItemLongClickListener);

		mAllLinksList.setFastTreeListener(this);

		registerForContextMenu (mAllLinksList);
		registerForContextMenu (mFavoritesList);
		registerForContextMenu (mHistoryList);

		mAllLinksList.setOpenedFolderIcon(Res.drawable.expander_ic_maximized);
		mAllLinksList.setClosedFolderIcon(Res.drawable.expander_ic_minimized);
		mAllLinksList.setScrollView((ScrollView)findViewById(Res.id.rtsp_all_links));

		mUrlEditor = (EditText) findViewById(Res.id.rtsp_url_editor);

		mDescriptionEditor = (EditText)findViewById(Res.id.rtsp_description_editor);

		mCategoryEditor = (Spinner)findViewById(Res.id.category);

		//statusBarTextView = (TextView) findViewById(Res.id.statusBar);

		findViewById(Res.id.rtsp_start_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				StartCommand();
			}
		});

		findViewById(Res.id.rtsp_clear_filter_search).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearchEditor.getText().clear();
				mAllLinksList.applyFilter(null);
				mSearchEditor.requestFocus();
			}
		});

		findViewById(Res.id.addCategory).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
			}
		});

		if (mFavoritesAdapter == null)
			mFavoritesAdapter = new RtspFavoritesAdapter();

		if (mHistoryAdapter == null)
			mHistoryAdapter = new RtspHistoryAdapter();

		Vector<GroupNameAndId> groups = mRecordsManager.getAllRootGroup();
		FastTreeItem[] rootItems = new FastTreeItem[groups.size()];
		groups.toArray(rootItems);
		mAllLinksList.setRootElements(rootItems);

		if (mFavoritesList.getAdapter() == null) {
			mFavoritesList.setAdapter(mFavoritesAdapter);
		}
		mFavoritesList.invalidate();

		if (mHistoryList.getAdapter() == null) {
			mHistoryList.setAdapter(mHistoryAdapter);
		}
		mHistoryList.invalidate();
	}

	//--------------------------------------------------------------------------
	@Override
	protected RecordsManager getRecordsManager() {
		return mRecordsManager;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
		/*try {
			mRecordsManager.updateLinksDBifNeeded();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		if (shareLnkDlg != null)
			shareLnkDlg.dismiss();

		if (changeCategoryDlg != null)
			changeCategoryDlg.dismiss();

		if (mRecordsManager != null && isFinishing()) {
			mRecordsManager.destroy();
			mRecordsManager = null;
		}
		
		if (mAdView != null) {
			mAdView.destroy();
		}

		super.onDestroy();
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryPageId() {
		return Res.id.rtsp_history;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryTabId() {
		return Res.id.RtspVideoHistoryTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewPageId() {
		return Res.id.rtsp_new;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewTabId() {
		return -1;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksPageId() {
		return Res.id.rtsp_all_links;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksTabId() {
		return Res.id.RtspVideoAllLinksTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getFavoritesPageId() {
		return Res.id.rtsp_favorites;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getFavoritesTabId() {
		return Res.id.RtspVideoFavoritesTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchTabId() {
		return Res.id.RtspSearchTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchViewId() {
		return Res.id.rtsp_search;
	}

	//--------------------------------------------------------------------------
	private AdapterView.OnItemClickListener mItemClickListener =
		new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
			mCurrentItem = null;
			if (getCurrentPage() == FourTabActivity.FAVORITES_PAGE) {
				mCurrentItem = mFavoritesAdapter.getItem(position);
			} else if (getCurrentPage() == FourTabActivity.HISTORY_PAGE) {
				mCurrentItem = mHistoryAdapter.getItem(position);
			}
			String linkClickAction = PreferenceManager.getDefaultSharedPreferences(RtspListActivity.this).getString(getString(Res.string.pref_links_list_click_action_key), "ShowMenu");
			if (linkClickAction.equalsIgnoreCase("ShowMenu")) {
				if (getCurrentPage() == FourTabActivity.ALL_LINKS_PAGE)
					openContextMenu (mAllLinksList);
				if (getCurrentPage() == FourTabActivity.FAVORITES_PAGE)
					openContextMenu (mFavoritesList);
				if (getCurrentPage() == FourTabActivity.HISTORY_PAGE)
					openContextMenu (mHistoryList);
			}
			else if (linkClickAction.equalsIgnoreCase("StartContent")) {
				StartCommand();
			}
		}
	};

	//--------------------------------------------------------------------------
	private AdapterView.OnItemLongClickListener mItemLongClickListener =
		new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
			mCurrentItem = null;
			if (getCurrentPage() == FourTabActivity.FAVORITES_PAGE) {
				mCurrentItem = mFavoritesAdapter.getItem(position);
				openContextMenu (mFavoritesList);
			} else if (getCurrentPage() == FourTabActivity.HISTORY_PAGE) {
				mCurrentItem = mHistoryAdapter.getItem(position);
				openContextMenu (mHistoryList);
			}
			return true;
		}
	};

	//--------------------------------------------------------------------------
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		if (mCurrentItem != null) {
			MenuItem item;

			menu.setHeaderTitle(mCurrentItem.getDescription() == null || mCurrentItem.getDescription().length() == 0 ?
					mCurrentItem.getUrl() : mCurrentItem.getDescription());

			item = menu.add(Menu.NONE, Command.START, Menu.NONE, Res.string.start);
			item.setIcon(Res.drawable.icon_play);

			item = menu.add(Menu.NONE, Command.EDIT, Menu.NONE, Res.string.edit);
			item.setIcon(Res.drawable.icon_edit);

			item = menu.add(Menu.NONE, Command.CHANGE_CATEGORY, Menu.NONE, Res.string.category);

			item = menu.add(Menu.NONE, Command.REMOVE, Menu.NONE, Res.string.remove);
			item.setIcon(Res.drawable.icon_delete);

			menu.add(Menu.NONE, Command.SEND, Menu.NONE, Res.string.send_email_menu);

			/*
			if (mCurrentItem.isLinkToRemove() || mCurrentItem.isNewLink())
				item = menu.add(Menu.NONE, Command.REMOVE_MARKER, Menu.NONE, Res.string.remove_marker);

			for (VideoRecord record : RtspRecordsManager.getInstance(this).getAllRecords()) {
				if (record.isLinkToRemove()) {
					item = menu.add(Menu.NONE, Command.REMOVE_ALL_MARKED, Menu.NONE, Res.string.remove_all_marked);
					break;
				}
			}
			*/

		} else if (mCurrentGroup != null ) {
			MenuItem item;
			menu.setHeaderTitle(mCurrentGroup.getDescription());

			item = menu.add(Menu.NONE, Command.ADD_GROUP, 1, Res.string.create_group);

			if (!mCurrentGroup.isRootGroup()) {

				item = menu.add(Menu.NONE, Command.EDIT_GROUP, 0, Res.string.rename);
				item.setIcon(Res.drawable.icon_edit);

				item = menu.add(Menu.NONE, Command.REMOVE_GROUP, 2, Res.string.remove);
				item.setIcon(Res.drawable.icon_delete);
			}
		} else{
			super.onCreateContextMenu (menu, v, menuInfo);
		}
	}

	//--------------------------------------------------------------------------
	public boolean onContextItemSelected (MenuItem item) {
		return onOptionsItemSelected (item);
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		DefaultMenu.create(menu);
		menu.addSubMenu(Menu.NONE, Command.NEW_RECORD, 0, Res.string.new_record).setIcon(Res.drawable.icon_edit);
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		onCreateOptionsMenu(menu);
		return true;
	}

	//--------------------------------------------------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DefaultMenu.onItemSelected(this, item))
			return true;

		switch (item.getItemId()) {
		case Command.START:
			StartCommand();
			return true;

		case Command.EDIT:
			EditCommand();
			return true;

		case Command.REMOVE:
			RemoveCommand();
			return true;

		case Command.UPDATE:
			//mRecordsManager.updateLinksDBifNeeded();
			// TODO Command.UPDATE
			return true;

		case Command.NEW_RECORD:
			EditText et = (EditText)findViewById(Res.id.rtsp_description_editor);
			et.getText().clear();
			et = (EditText)findViewById(Res.id.rtsp_url_editor);
			et.selectAll();
			et.requestFocus();
			setCurrentPage(FourTabActivity.NEW_PAGE, false);
			return true;

		/*
		case Command.REMOVE_MARKER:
			mCurrentItem.setToRemoveState(false);
			mCurrentItem.setNewState(false);
			RtspRecordsManager.getInstance(this).updateRecord(mCurrentItem);
			return true;
		case Command.REMOVE_ALL_MARKED:
			RemoveAllMarkedRecords(true);
			return true;
		*/

		case Command.EDIT_GROUP:{
			if (mCurrentGroup != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(String.format("%s '%s'", getString(Res.string.rename), mCurrentGroup.getDescription()));
				final EditText group_name = new EditText(this);
				group_name.setText(mCurrentGroup.getDescription());
				group_name.selectAll();
				builder.setView(group_name);
				builder.setPositiveButton(Res.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mCurrentGroup.setDescription(group_name.getText().toString());
						mRecordsManager.updateGroup(mCurrentGroup);
					}
				});
				builder.setNegativeButton(Res.string.cancel, null);
				builder.show();
			}
			return true;
			}
		/*
		case Command.REMOVE_GROUP:{
			if (mCurrentGroup != null) {
				mRecordsManager.removeGroup(mCurrentGroup);
			}
			return true;
		}
		case Command.ADD_GROUP:{
			if (mCurrentGroup != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(String.format("%s for '%s'", getString(Res.string.create_group), mCurrentGroup.mName));
				final EditText group_name = new EditText(this);
				builder.setView(group_name);
				builder.setPositiveButton(Res.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mRecordsManager.createGroup(mCurrentGroup, group_name.getText().toString());
					}
				});
				builder.setNegativeButton(Res.string.cancel, null);
				builder.show();
			}
			return true;
		}
		*/
		case Command.CHANGE_CATEGORY:{
			showChangeCategoryDlg();
			return true;
		}
		
		case Command.SEND:

			if(mCurrentItem != null)
			{
				Intent send_mail_intent = new Intent(Intent.ACTION_SEND);
				send_mail_intent.setType("message/rfc822");
				send_mail_intent.putExtra(Intent.EXTRA_TEXT, mCurrentItem.getUrl());
				send_mail_intent.putExtra(Intent.EXTRA_SUBJECT, "subject" /*getString(R.string.email_subject)*/);
				startActivity(Intent.createChooser(send_mail_intent, getString(Res.string.email_chooser_title)));
			}
			return true;
		}
		return false;
	}


	// --------------------------------------------------------------------------
	private void showChangeCategoryDlg() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(Res.arrays.video_categories, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String categoryToSet = getResources().getStringArray(Res.arrays.video_categories)[which];
				mCurrentItem.setCategory(categoryToSet);
				GroupNameAndId group = mRecordsManager.getGroupByName(categoryToSet);
				mCurrentItem.setGroup(group.getId(), group.getDescription());
				mRecordsManager.updateRecord(mCurrentItem);
			}
		});
		changeCategoryDlg = builder.show();
	}

	// --------------------------------------------------------------------------
	protected void RemoveAllDeadRecords() {
		final Vector<VideoRecord> toRemoveRecords = new Vector<VideoRecord>();
		for (VideoRecord record : mRecordsManager.getAllRecords()) {
			if (record.isDeadLink()) {
				toRemoveRecords.add(record);
			}
		}
		if (toRemoveRecords.size() > 0) {
			for (VideoRecord record : toRemoveRecords) {
				mRecordsManager.remove(record);
			}
		}
	}

	//--------------------------------------------------------------------------
	protected void PerformStart(RecordBase record) {
		PerformStart(record.getUrl(), record.getDescription(), VIEW_HISTORY_ACTIVITY_CODE);
	}
	
	//--------------------------------------------------------------------------
	private void PerformStart(String address, String description, int requestCode) {

		try {
			if (mMediaServiceInterface != null && mMediaServiceInterface.isRadioPlaying()) {
				mMediaServiceInterface.stopRadio();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		if (address.length() > 8) {
			if (address.substring(0, 7).equalsIgnoreCase("rtsp://")
					|| address.substring(0, 7).equalsIgnoreCase("http://")
					|| address.substring(0, 8).equalsIgnoreCase("https://")
					|| address.substring(0, 6).equalsIgnoreCase("mms://")) {

			} else {
				address = "rtsp://" + address;
			}
		}

		Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.IS_SUPRESS_NAVIGATION, false);
		intent.putExtra(PlayerActivity.DESCRIPTION_DATA, description);
		intent.putExtra(PlayerActivity.URL_DATA, address);
		intent.putExtra(PlayerActivity.SESSION_ID, Integer.toHexString((int) (Math.random() * (double) 0x7FFFFFFF)));

		startActivityForResult(intent, requestCode);

	}

	// --------------------------------------------------------------------------
	protected void StartCommand() {
		String address, description;
		int result;

		if (getCurrentPage() == NEW_PAGE) {

			EditText editor = (EditText) findViewById(Res.id.rtsp_url_editor);
			address = editor.getText().toString().trim();

			editor = (EditText) findViewById(Res.id.rtsp_description_editor);
			description = editor.getText().toString().trim();

			result = VIEW_NEW_ACTIVITY_CODE;

		} else {

			if (mCurrentItem == null)
				return;

			address = mCurrentItem.getUrl();
			description = mCurrentItem.getDescription();
			result = VIEW_HISTORY_ACTIVITY_CODE;

			if (mCurrentItem.isDeadLink()) {
				showDeadLinkDialog(mCurrentItem);
				return;
			}
		}
		
		if(address.length() != 0)
		{
			PerformStart(address, description, result);
		}
		else
		{
			Toast.makeText(RtspListActivity.this, 
				getString(Res.string.msg_url_empty), Toast.LENGTH_LONG).show();
		}
	}

	// --------------------------------------------------------------------------
	private void EditCommand() {
		if (mCurrentItem != null) {
			Intent intent = new Intent(this, RtspEditActivity.class);
			intent.putExtra(RtspEditActivity.DESCRIPTION_DATA, mCurrentItem.getDescription());
			intent.putExtra(RtspEditActivity.URL_DATA, mCurrentItem.getUrl());
			intent.putExtra(RtspEditActivity.CATEGORY_DATA, mCurrentItem.getCategory());
			startActivityForResult(intent, EDIT_ACTIVITY_CODE);
		}
	}

	// --------------------------------------------------------------------------
	private void RemoveCommand() {
		if (mCurrentItem != null) {
			RemoveRecord(mCurrentItem);
		}
	}

	// --------------------------------------------------------------------------
	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		if (resultCode == StreamMediaActivity.SUCCESS) {
			switch (requestCode) {
			case VIEW_NEW_ACTIVITY_CODE:
			{
				String address = mUrlEditor.getText().toString().trim();
				String description = mDescriptionEditor.getText().toString().trim();
				String category = mCategoryEditor.getSelectedItem().toString().trim();

				boolean isNewLink = mRecordsManager.add(description, address, category, -1, false, Calendar.getInstance().getTimeInMillis(), false, false);

				if (isNewLink && ShareLinkDialog.CheckLinkForSharing(address)) {
					if (data != null && data.getBooleanExtra(PlayerActivity.IS_LINK_TO_SHARE, false)) {
						ShareLinkDialog.share(RtspListActivity.this, Res.drawable.icon_video,
								address, ShareLinkDialog.INTERNET_VIDEO, category, description, "-");
					}
				}
			}
			break;

			case VIEW_HISTORY_ACTIVITY_CODE:
				{
					if (mCurrentItem != null) {
						mCurrentItem.setLastAccessedDate(Calendar.getInstance().getTimeInMillis());
						mCurrentItem.setNewState(false);
						mCurrentItem.setLinkDeadState(false);
						mRecordsManager.updateRecord(mCurrentItem);
					}
					break;
				}
			case EDIT_ACTIVITY_CODE:
				if (mCurrentItem != null) {
					mCurrentItem.setDescription(data.getStringExtra(RtspEditActivity.DESCRIPTION_DATA));
					mCurrentItem.setUrl(data.getStringExtra(RtspEditActivity.URL_DATA));
					mCurrentItem.setCategory(data.getStringExtra(RtspEditActivity.CATEGORY_DATA));
					GroupNameAndId groupToAssign = mRecordsManager.getGroupByName(mCurrentItem.getCategory());
					mCurrentItem.setGroup(groupToAssign.getId(), groupToAssign.getDescription());
					mRecordsManager.updateRecord(mCurrentItem);
				}
				break;
			}
		}
		else if(resultCode == 0)
		{
			if(requestCode == Command.SETTINGS)
			{
				mRecordsManager.updateAllRecords();
				onRecordListChanged();

				int number =
					Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_links_list_default_tab_key), "0"));
				setCurrentPage(number, true);
			}
		}
	}

	//--------------------------------------------------------------------------
	private abstract class RtspBaseListAdapter extends BaseListAdapter {
		
		protected abstract Vector<VideoRecord> getRecords();
		protected abstract ListView getListView();
		
		//----------------------------------------------------------------------
		public int getCount() {
			return getRecords().size();
		}

		//----------------------------------------------------------------------
		public VideoRecord getItem(int position) {
			try {
				return getRecords().get(position);
			} catch (Exception e) {
				getListView().invalidateViews();
				return null;
			}
		}

		//----------------------------------------------------------------------
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(Res.layout.url_list_item, null);
			}
			
			VideoRecord record = getRecords().get(position);

			TextView view = (TextView)convertView.findViewById(Res.id.list_item_description);
			view.setText(record.getDescription());

			ImageView image = (ImageView)convertView.findViewById(Res.id.linkActionIcon);
			if (record.isNewLink()) {
				
				image.setImageResource(Res.drawable.icon_marker_new);
				image.setVisibility(View.VISIBLE);
				
			} else if (record.isDeadLink()) {
				
				image.setImageResource(Res.drawable.icon_marker_na);
				image.setVisibility(View.VISIBLE);
				
			} else {
				
				image.setVisibility(View.GONE);
			}

			view = (TextView)convertView.findViewById(Res.id.list_item_url);
			view.setText(record.getUrl());

			image = (ImageView)convertView.findViewById(Res.id.favoritesIcon);
			image.setImageResource(record.isFavorite() ? 
					Res.drawable.favorites : Res.drawable.favorites_off);
			image.setOnClickListener(new FavoriteIconListener(record));

			return convertView;
		}
	}
	
	//--------------------------------------------------------------------------
	private class RtspHistoryAdapter extends RtspBaseListAdapter {

		@Override
		protected ListView getListView() {
			return mHistoryList;
		}

		@Override
		protected Vector<VideoRecord> getRecords() {
			return mRecordsManager.getHistoryRecords();
		}
	}

	//--------------------------------------------------------------------------
	private class RtspFavoritesAdapter extends RtspBaseListAdapter {

		@Override
		protected ListView getListView() {
			return mFavoritesList;
		}

		@Override
		protected Vector<VideoRecord> getRecords() {
			return mRecordsManager.getFavoriteRecords();
		}
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getUpdateStatusId() {
		return Res.id.video_updating_links;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void updateLinkList() {
		mRecordsManager = new RtspRecordsManager(this, this);
		onRecordListChanged();
		// TODO
	}

	//--------------------------------------------------------------------------
	@Override
	public void onRecordListChanged() {
		
		if (mInvalidateListRunnable == null) {
			mInvalidateListRunnable = new invalidateListRunnable(); 
			mHandler.post(mInvalidateListRunnable);
		}
	}

	//--------------------------------------------------------------------------
	private class invalidateListRunnable implements Runnable {

		@Override
		public void run() {
			mFavoritesList.invalidate();
			mFavoritesList.invalidateViews();
			mHistoryList.invalidate();
			mHistoryList.invalidateViews();
			mAllLinksList.rebuildTree();
			mInvalidateListRunnable = null;
		}
	}

	private invalidateListRunnable mInvalidateListRunnable = null;

	//--------------------------------------------------------------------------
	@Override
	protected int getLinkType() {
		return UpdateService.VIDEO_LINK;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getDialogIconId() {
		return Res.drawable.icon_video;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getDialogTitleId() {
		return Res.string.internet_stream_media;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void setCurrentItem(RecordBase record) {
		if (record instanceof VideoRecord) {
			mCurrentItem = (VideoRecord)record;
			mCurrentGroup = null;
		}
	}
	
	private void initAdView()
    {
    	if(mAdView != null)
    		mAdView.destroy();
 
        mAdView = new Ad(this);
    }
}
