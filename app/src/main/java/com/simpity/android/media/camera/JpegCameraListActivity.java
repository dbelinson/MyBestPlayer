package com.simpity.android.media.camera;

import java.util.Calendar;
import java.util.Vector;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.simpity.android.media.services.UpdateService;
import com.simpity.android.media.storage.CameraRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.utils.BaseListAdapter;
import com.simpity.android.media.utils.Command;
import com.simpity.android.media.utils.DefaultMenu;

public class JpegCameraListActivity extends BaseLinkListActivity {

	private JpegFavoriteLinksAdapter mFavoritesAdapter;
	private JpegHistoryLinksAdapter mHistoryAdapter;

	//PowerManager.WakeLock mWakeLock = null;

	private EditText mUrlEditor, mDescriptionEditor;
	private Spinner mRefreshSpinner = null;

	private CameraRecord mCurrentItem = null;

	//private ShareLinkDialog mShareLinkDialog = null;
	//private AlertDialog mDeadLinkDialog = null;

	private JpegCameraRecordsManager mRecordsManager;
	/*
	public final static String[] REFRESH_TIMES = {
		"1 sec", "5 sec", "10 sec", "15 sec", "30 sec",
		"1 min", "10 min", "30 min", "1 hour"
	};

	public final static int[] REFRESH_TIME_VALUES = {
		1, 5, 10, 15, 30, 60, 600, 1800, 3600
	};
	*/
	
	private Ad mAdView;

	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRecordsManager = new JpegCameraRecordsManager(this, this);

		setContentView(Res.layout.jpeg_camera_select);

		init(savedInstanceState);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		//SAVE STATES
		if (getCurrentPage() == SEARCH_COMMAND) {
			
			mSavedSearchLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedSearchLinksListState);
			
		} else if (getCurrentPage() == ALL_LINKS_PAGE) {
			
			mSavedAllLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedAllLinksListState);
		}

		Parcelable savedFavoritesListState = mFavoritesList.onSaveInstanceState();
		Parcelable savedHistoryListState = mHistoryList.onSaveInstanceState();

		String savedUrlEditorText = mUrlEditor.getText().toString();
		String savedDescriptionEditorText = mDescriptionEditor.getText().toString();
		int savedRefreshSpinnerPos =  mRefreshSpinner.getSelectedItemPosition();


		Bundle savedState = new Bundle();
		super.onSaveInstanceState(savedState);

		//--------------------------------------------
		super.onConfigurationChanged(newConfig);
		setContentView(Res.layout.jpeg_camera_select);
		//--------------------------------------------

		//RESTORE STATES
		init(savedState);

		if (getCurrentPage() == SEARCH_COMMAND) {
			
			if (mSavedSearchLinksListState != null)
				mAllLinksList.restoreState(mSavedSearchLinksListState);
			
			mSearchEditor.setText(mAllLinksList.getFilter());
			
		} else if (getCurrentPage() == ALL_LINKS_PAGE) {
			
			if (mSavedAllLinksListState != null)
				mAllLinksList.restoreState(mSavedAllLinksListState);
			
			mSearchEditor.getText().clear();
		}

		mUrlEditor.setText(savedUrlEditorText);
		mDescriptionEditor.setText(savedDescriptionEditorText);
		mRefreshSpinner.setSelection(savedRefreshSpinnerPos);

		mFavoritesList.onRestoreInstanceState(savedFavoritesListState);
		mHistoryList.onRestoreInstanceState(savedHistoryListState);
	}

	//--------------------------------------------------------------------------
	private void init(Bundle savedInstanceState) {
		
		if(mAdView != null)
			mAdView.destroy();

		mAdView = new Ad(this);
	    //LinearLayout layout = (LinearLayout)findViewById(R.id.AdLayout);
	    //layout.addView(mAdView);
	    
	    // Initiate a generic request to load it with an ad
	    //mAdView.loadAd(new AdRequest());

		initTabs(savedInstanceState, this);

		mRefreshSpinner = (Spinner)findViewById(Res.id.jpeg_refresh_spinner);
		mSearchEditor = (EditText)findViewById(Res.id.jpeg_camera_search_filter);
		mUrlEditor = (EditText)findViewById(Res.id.jpeg_url_editor);
		mUrlEditor.setText("http://");
		mDescriptionEditor = (EditText)findViewById(Res.id.jpeg_description_editor);

		mAllLinksList = (FastTree) findViewById(Res.id.jpeg_camera_all_links_FastTree);
		mFavoritesList = (ListView) findViewById(Res.id.jpeg_camera_favorites);
		mHistoryList = (ListView) findViewById(Res.id.jpeg_camera_history);

		findViewById(Res.id.jpeg_start_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				StartCommand();
			}
		});

		View clearSearch = findViewById(Res.id.jpeg_camera_clear_filter_search);

		if (clearSearch != null) {
			clearSearch.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					mSearchEditor.getText().clear();
					mAllLinksList.applyFilter(null);
					mSearchEditor.requestFocus();
				}
			});
		}

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
		mAllLinksList.setScrollView((ScrollView)findViewById(Res.id.jpeg_camera_all_links));

		//ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, Res.layout.spinner_text_view, REFRESH_TIMES);
		//mRefreshSpinner.setAdapter(adapter);
		mRefreshSpinner.setSelection(2);

		if (mFavoritesAdapter == null)
			mFavoritesAdapter = new JpegFavoriteLinksAdapter();

		if (mHistoryAdapter == null)
			mHistoryAdapter = new JpegHistoryLinksAdapter();

		Vector<CameraRecord> groups = mRecordsManager.getAllRecords();
		FastTreeItem[] rootItems = new FastTreeItem[groups.size()];
		groups.toArray(rootItems);
		mAllLinksList.setRootElements(rootItems);

		if (mFavoritesList.getAdapter() == null)
			mFavoritesList.setAdapter(mFavoritesAdapter);
		
		mFavoritesList.invalidate();

		if (mHistoryList.getAdapter() == null)
			mHistoryList.setAdapter(mHistoryAdapter);
		
		mHistoryList.invalidate();
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

		if (mRecordsManager != null && isFinishing()) {
			mRecordsManager.destroy();
			mRecordsManager = null;
		}
		
		if (mAdView != null) {
			mAdView.destroy();
		}
		
		super.onDestroy();
	};

	//--------------------------------------------------------------------------
	@Override
	protected RecordsManager getRecordsManager() {
		return mRecordsManager;
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryPageId() {
		return Res.id.jpeg_camera_history;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryTabId() {
		return Res.id.JpegCameraHistoryTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewPageId() {
		return Res.id.jpeg_camera_new;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewTabId() {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksPageId() {
		return Res.id.jpeg_camera_all_links;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksTabId() {
		return Res.id.JpegCameraAllLinksTab;
	}

	@Override
	protected int getFavoritesPageId() {
		return Res.id.jpeg_camera_favorites;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getFavoritesTabId() {
		return Res.id.JpegCameraFavoritesTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchTabId() {
		return Res.id.JpegCameraSearchTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchViewId() {
		return Res.id.jpeg_camera_search;
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------
	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
			mCurrentItem = null;
			if (getCurrentPage() == FourTabActivity.FAVORITES_PAGE) {
				mCurrentItem = mFavoritesAdapter.getItem(position);
			} else if (getCurrentPage() == FourTabActivity.HISTORY_PAGE) {
				mCurrentItem = mHistoryAdapter.getItem(position);
			}
			
			String linkClickAction = PreferenceManager.getDefaultSharedPreferences(JpegCameraListActivity.this).getString(getString(Res.string.pref_links_list_click_action_key), "ShowMenu");
			if (linkClickAction.equalsIgnoreCase("ShowMenu")) {
				
				showContextMenu();
				
			} else if (linkClickAction.equalsIgnoreCase("StartContent")) {
				
				StartCommand();
			}
		}
	};

	//--------------------------------------------------------------------------
	private AdapterView.OnItemLongClickListener mItemLongClickListener = new AdapterView.OnItemLongClickListener() {

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

			item = menu.add(Menu.NONE, Command.REMOVE, Menu.NONE, Res.string.remove);
			item.setIcon(Res.drawable.icon_delete);
			
			menu.add(Menu.NONE, Command.SEND, Menu.NONE, Res.string.send_email_menu);
/*
			if (mCurrentItem.isLinkToRemove() || mCurrentItem.isNewLink())
				item = menu.add(Menu.NONE, Command.REMOVE_MARKER, Menu.NONE, Res.string.remove_marker);
*/
		} else {
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
		//menu.addSubMenu(Menu.NONE, Command.UPDATE, 0, Res.string.update_links).setIcon(Res.drawable.refresh);
		return true;
	}

	//--------------------------------------------------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DefaultMenu.onItemSelected(this, item))
			return true;

		switch(item.getItemId()) {
		case Command.START:
			StartCommand();
			return true;

		case Command.EDIT:
			EditCommand();
			return true;

		case Command.REMOVE:
			RemoveCommand();
			return true;

		case Command.BACK:
			finish();
			return true;

		case Command.UPDATE:
			//mRecordsManager.updateLinksDBifNeeded();
			// TODO Command.UPDATE
			return true;

		case Command.REMOVE_MARKER:
			mCurrentItem.setLinkDeadState(false);
			mCurrentItem.setNewState(false);
			mRecordsManager.updateRecord(mCurrentItem);
			return true;

		case Command.NEW_RECORD:
			EditText et = (EditText)findViewById(Res.id.jpeg_description_editor);
			et.getText().clear();
			et = (EditText)findViewById(Res.id.jpeg_url_editor);
			et.selectAll();
			et.requestFocus();
			setCurrentPage(FourTabActivity.NEW_PAGE, false);
			return true;
			
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

	//--------------------------------------------------------------------------
	private int getRefreshValue() {
		Spinner spinner = (Spinner)findViewById(Res.id.jpeg_refresh_spinner);
		int pos = spinner.getSelectedItemPosition();
		return pos != AdapterView.INVALID_POSITION ? Integer.parseInt(getResources().getStringArray(Res.arrays.camera_refresh_time_values)[pos]) : 10;
	}

	//--------------------------------------------------------------------------
	protected void StartCommand() {
		String address, description;
		int refresh, result_code;

		if (getCurrentPage() == NEW_PAGE) {

			EditText editor = (EditText)findViewById(Res.id.jpeg_url_editor);
			address = editor.getText().toString();

			editor = (EditText)findViewById(Res.id.jpeg_description_editor);
			description = editor.getText().toString();

			refresh = getRefreshValue();

			result_code = VIEW_NEW_ACTIVITY_CODE;

		} else {

			if (mCurrentItem == null)
				return;

			address = mCurrentItem.getUrl();
			description = mCurrentItem.getDescription();
			refresh = mCurrentItem.getRefreshPeriod();

			result_code = VIEW_HISTORY_ACTIVITY_CODE;

			if (mCurrentItem.isDeadLink()) {
				showDeadLinkDialog(mCurrentItem);
				return;
			}
		}
		
		if (address.length() == 0 || address.equals(getString(Res.string.http_prefix)))
		{
			Toast.makeText(JpegCameraListActivity.this, 
					getString(Res.string.msg_enter_url), Toast.LENGTH_LONG).show();
		}
		else
		{
			PerformStart(address, description, refresh, result_code);
		}
	}

	// --------------------------------------------------------------------------
	protected void RemoveAllDeadRecords() {
		final Vector<CameraRecord> toRemoveRecords = new Vector<CameraRecord>();
		for (CameraRecord record : mRecordsManager.getAllRecords()) {
			if (record.isDeadLink()) {
				toRemoveRecords.add(record);
			}
		}
		
		if (toRemoveRecords.size() > 0) {
			for (CameraRecord record : toRemoveRecords) {
				mRecordsManager.remove(record);
			}
		}
	}

	//--------------------------------------------------------------------------
	protected void PerformStart(RecordBase record) {
		if (record instanceof CameraRecord) {
			PerformStart(record.getUrl(), record.getDescription(), 
					((CameraRecord)record).getRefreshPeriod(), VIEW_HISTORY_ACTIVITY_CODE);
		}
	}
	
	//--------------------------------------------------------------------------
	private void PerformStart(String address, String description, int refresh, int requestCode) {
		Intent intent = new Intent(this, JpegCameraView.class);
		intent.putExtra(JpegEditActivity.DESCRIPTION_DATA, description);
		intent.putExtra(JpegEditActivity.URL_DATA, address);
		intent.putExtra(JpegEditActivity.REFRESH_DATA, Integer.toString(refresh));
		startActivityForResult(intent, requestCode);
	}

	//--------------------------------------------------------------------------
	private void EditCommand() {
		if (mCurrentItem != null) {
			Intent intent = new Intent(this, JpegEditActivity.class);
			intent.putExtra(JpegEditActivity.DESCRIPTION_DATA, mCurrentItem.getDescription());
			intent.putExtra(JpegEditActivity.URL_DATA, mCurrentItem.getUrl());
			intent.putExtra(JpegEditActivity.REFRESH_DATA, Integer.toString(mCurrentItem.getRefreshPeriod()));
			startActivityForResult(intent, EDIT_ACTIVITY_CODE);
		}
	}

	//--------------------------------------------------------------------------
	private void RemoveCommand() {
		if (mCurrentItem != null) {
			RemoveRecord(mCurrentItem);		
		}
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == StreamMediaActivity.SUCCESS) {
			switch (requestCode) {
			case VIEW_NEW_ACTIVITY_CODE: {
				EditText editor = (EditText)findViewById(Res.id.jpeg_url_editor);
				final String address = editor.getText().toString();

				editor = (EditText)findViewById(Res.id.jpeg_description_editor);
				final String description = editor.getText().toString();

				final int refresh = getRefreshValue();

				boolean isNewLink = mRecordsManager.add(description.trim(), address, refresh, false, -1, Calendar.getInstance().getTimeInMillis(), false, false);
				if (isNewLink && data != null 
						&& data.getBooleanExtra(JpegCameraView.IS_LINK_TO_SHARE, false)) {
					
					ShareLinkDialog.share(JpegCameraListActivity.this, Res.drawable.icon_jpeg,
							address, ShareLinkDialog.JPEG_WEB_CAMERA, "-", description, "-");
				}
				break;
			}
			case VIEW_HISTORY_ACTIVITY_CODE:
				if (mCurrentItem != null) {
					mCurrentItem.setLastAccessedDate(Calendar.getInstance().getTimeInMillis());
					mCurrentItem.setNewState(false);
					mCurrentItem.setLinkDeadState(false);
					mRecordsManager.updateRecord(mCurrentItem);
				}
				break;

			case EDIT_ACTIVITY_CODE:
				if (mCurrentItem != null) {
					mCurrentItem.setDescription(data.getStringExtra(JpegEditActivity.DESCRIPTION_DATA));
					mCurrentItem.setUrl(data.getStringExtra(JpegEditActivity.URL_DATA));
					mCurrentItem.setRefreshPeriod(Integer.parseInt(data.getStringExtra(JpegEditActivity.REFRESH_DATA)));
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
	private abstract class JpegBaseListAdapter extends BaseListAdapter {

		protected abstract Vector<CameraRecord> getRecords();
		protected abstract ListView getListView();
		
		//----------------------------------------------------------------------
		public int getCount() {
			return getRecords().size();
		}

		//----------------------------------------------------------------------
		public CameraRecord getItem(int position) {
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

			CameraRecord record = getRecords().get(position);

			TextView view = (TextView)convertView.findViewById(Res.id.list_item_description);
			String description = record.getDescription();
			StringBuilder builder = new StringBuilder();
			if (description != null) {
				builder.append(record.getDescription());
				builder.append(' ');
			}
			builder.append(String.format(getString(Res.string.jpeg_camera_refresh_description_fmt), record.getRefreshPeriod()));
			view.setText(builder.toString());

			view = (TextView)convertView.findViewById(Res.id.list_item_url);
			view.setText(record.getUrl());

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

			image = (ImageView)convertView.findViewById(Res.id.favoritesIcon);
			image.setImageResource(record.isFavorite() ? Res.drawable.favorites : Res.drawable.favorites_off);

			image.setOnClickListener(new FavoriteIconListener(record));

			return convertView;
		}
	}
	
	//--------------------------------------------------------------------------
	private class JpegFavoriteLinksAdapter extends JpegBaseListAdapter {
		
		@Override
		protected ListView getListView() {
			return mFavoritesList;
		}

		@Override
		protected Vector<CameraRecord> getRecords() {
			return mRecordsManager.getFavoriteRecords();
		}
	}

	//--------------------------------------------------------------------------
	private class JpegHistoryLinksAdapter extends JpegBaseListAdapter {
		
		@Override
		protected ListView getListView() {
			return mHistoryList;
		}

		@Override
		protected Vector<CameraRecord> getRecords() {
			return mRecordsManager.getHistoryRecords();
		}
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getUpdateStatusId() {
		return Res.id.jpeg_camera_updating_links;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void updateLinkList() {
		mRecordsManager = new JpegCameraRecordsManager(this, this);
		onRecordListChanged();
	}

	//--------------------------------------------------------------------------
	@Override
	public void onRecordListChanged() {
		
		mHandler.post(new Runnable() {
			@Override
			public void run() {

				Vector<CameraRecord> groups = mRecordsManager.getAllRecords();
				FastTreeItem[] rootItems = new FastTreeItem[groups.size()];
				groups.toArray(rootItems);
				mAllLinksList.setRootElements(rootItems);
				//mAllLinksList.rebuildTree();

				mFavoritesList.invalidate();
				mFavoritesList.invalidateViews();
				mHistoryList.invalidate();
				mHistoryList.invalidateViews();
			}
		});
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected int getLinkType() {
		return UpdateService.CAMERA_LINK;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getDialogIconId() {
		return Res.drawable.icon_jpeg;
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected int getDialogTitleId() {
		return Res.string.jpeg_web_camera;
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void setCurrentItem(RecordBase record) {
		if (record instanceof CameraRecord) {
			mCurrentItem = (CameraRecord)record;
		}
	}
}
