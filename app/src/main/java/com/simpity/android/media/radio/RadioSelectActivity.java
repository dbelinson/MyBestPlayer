package com.simpity.android.media.radio;

import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.simpity.android.media.Ad;
import com.simpity.android.media.IMediaServiceInterface;
import com.simpity.android.media.MediaService;
import com.simpity.android.media.Res;
import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.controls.EditTextWithHistory;
import com.simpity.android.media.controls.fasttree.FastTree;
import com.simpity.android.media.controls.fasttree.FastTreeItem;
import com.simpity.android.media.controls.fasttree.FastTreeListener;
import com.simpity.android.media.dialogs.ShareLinkDialog;
import com.simpity.android.media.player.HttpProxy;
import com.simpity.android.media.services.UpdateService;
import com.simpity.android.media.storage.GroupRecord;
import com.simpity.android.media.storage.PlayList;
import com.simpity.android.media.storage.RadioPlayList;
import com.simpity.android.media.storage.RadioRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;
import com.simpity.android.media.utils.Command;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.LinkParser;
import com.simpity.android.media.utils.Utilities;


public class RadioSelectActivity extends Activity implements
		RecordsManager.OnListChangedListener, EditTextWithHistory.TextChangeListener {

	private final static long ANIMATION_TIME = 250;
	private final static String SEARCH_HISTORY_KEY = "RADIO_SEARCH_HISTORY";
	
	private FastTree mContentTreeView;
	private View mSearchPanel;
	private EditTextWithHistory mSearchEditor, mDirectLinkEditor;
	
	private RecordBase mCurrentRecord;
	private IMediaServiceInterface mServiceInterface = null;
	private ImageButton mPlayStopButton, mNextButton;
	private boolean mPlaying;
	private final Vector<RadioPlayList> mPlaylists = new Vector<RadioPlayList>();
	
	private int mTopPageId;
	private int mPage1TitleId;
	private int[] mBackPageIds = new int[10];
	private int mBackPageIdCount = 0;
	private RadioRecordsManager mRecordsManager;
	private final Handler mHandler = new Handler();
	private PlaylistEditor mPlaylistEditor;
	private String mStartUrl;
	
	private Ad mAdView;
	
	//--------------------------------------------------------------------------
	private interface FastTreeDataSource {
		public FastTreeItem[] getElements();
	}
	
	private FastTreeDataSource mFastTreeDataSource = null;
	private boolean mNeedUpdateFastTreeData = false;
	
	//--------------------------------------------------------------------------
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceInterface = IMediaServiceInterface.Stub.asInterface(service);
			try {
				showUpdateResult();
				
				if (mStartUrl != null) {

					mServiceInterface.startNewRadio(mStartUrl);
					mStartUrl = null;

				} else if (mServiceInterface.getRadioCurrentAction() != MediaService.ACTION_RADIO_EMPTY) {
					
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							try {
								int id = mServiceInterface.getCurrentRadioId();
								
								if (id < 0) {
									return;
								}
								
								Vector<RecordBase> records = Storage.getRadioRecords(
										mRecordsManager, new int[]{id});
								
								if (records.size() == 0) {
									return;
								}
								
								mCurrentRecord = records.get(0);
								
								if (!(mCurrentRecord instanceof RadioRecord)) {
									mCurrentRecord = null;
									return;
								}
								
								RadioRecord record = (RadioRecord)records.get(0);
								
								View view = findViewById(Res.id.CurrentRadioControl);
								if (view.getVisibility() != View.VISIBLE)
									view.setVisibility(View.VISIBLE);
								
								setPlaying(true);
									
								mNextButton.setVisibility(mServiceInterface.isRadioPlaylistPlaying() ? View.VISIBLE : View.GONE);
								
								setRadioInfo(record.getStationName(), record.getGenre(), 
										record.getContentDescription(), record.getUrl(), null);
								
								int action = mServiceInterface.getRadioCurrentAction();
								processAction(MediaService.ACTION_RADIO_STARTED, mServiceInterface.getRadioInfo());
								if (action != MediaService.ACTION_RADIO_STARTED) {
									processAction(action, null);
								}
								
								if (action == MediaService.ACTION_RADIO_PLAYING) {
									processAction(MediaService.ACTION_RADIO_COMPOSITION, 
											mServiceInterface.getRadioComposition());
								}
								
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					});
					
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mServiceInterface = null;
		}
	};

	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(Res.layout.radio_select_pages);

		mBackPageIdCount = 0;
		mTopPageId = Res.id.RadioSelectPage0;
		
		mRecordsManager = new RadioRecordsManager(this, this);
		
		ListView list_view = (ListView)findViewById(Res.id.RadioSelectPage0);

		Page0Adapter page0_adapter = new Page0Adapter();
		list_view.setAdapter(page0_adapter);
		list_view.setOnItemClickListener(page0_adapter);

		mSearchPanel = findViewById(Res.id.RadioSearchPanel);
		
		mSearchEditor = (EditTextWithHistory)findViewById(Res.id.RadioSearchEditor);
		mSearchEditor.setHistoryKey(SEARCH_HISTORY_KEY);
		/*mSearchEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					// TODO
				}
				return false;
			}
		});*/
		
		mContentTreeView = (FastTree)findViewById(Res.id.RadioSelectFastTree);
		
		mPlayStopButton = (ImageButton)findViewById(Res.id.CurrentRadioPlayStop);
		mPlayStopButton.setOnClickListener(mPlayPauseClickListener);
		
		mNextButton = (ImageButton)findViewById(Res.id.CurrentRadioNext);
		mNextButton.setOnClickListener(mNextClickListener);
		
		mDirectLinkEditor = (EditTextWithHistory)findViewById(Res.id.RadioEditUrl); 
		mDirectLinkEditor.setUrlInputType();
		mDirectLinkEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				//if (actionId == Res.string.start) {
					startEditorLink();
				//	return true;
				//}
				return false;
			}
		});
		
		findViewById(Res.id.RadioEditStartButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startEditorLink();
			}
		});
		
		findViewById(Res.id.CurrentRadioInfoPanel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPage(Res.id.RadioSelectPage2, Res.string.radio);
			}
		});

		Intent intent = getIntent();
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			mStartUrl = intent.getData().toString();
		} else {
			mStartUrl = null;
		}
		
		//----------------------------------------------------------------------------------
		//registerReceiver(mServiceMsgsReceiver, new IntentFilter(VersionConfig.MEDIA_SERVICE_INTENT));
		
		IntentFilter filter = new IntentFilter(VersionConfig.MEDIA_SERVICE_INTENT);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		//filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mServiceMsgsReceiver, filter);
		//----------------------------------------------------------------------------------

		bindService(new Intent(this, MediaService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		updateTitle(getResources().getConfiguration().orientation);
		setRadioTitle(Res.string.radio);
		
		mAdView = new Ad(this);
		
		mPlaylistEditor = new PlaylistEditor(this);
		
		reloadPlaylists();
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		mSearchEditor.storeHistory();
		
		unbindService(mConnection);
		unregisterReceiver(mServiceMsgsReceiver);
		
		if (mAdView != null) {
			mAdView.destroy();
		}

		super.onDestroy();
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		updateTitle(newConfig.orientation);
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		DefaultMenu.create(menu);
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultMenu.onItemSelected(this, item);
	}

	//--------------------------------------------------------------------------
	private void updateTitle(int orientation) {
		View text1 = findViewById(Res.id.RadioSelectTitleText1);
		View text2 = findViewById(Res.id.RadioSelectTitleText2);
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			text1.setVisibility(View.GONE);
			text2.setVisibility(View.VISIBLE);
		} else {
			text1.setVisibility(View.VISIBLE);
			text2.setVisibility(View.GONE);
		}	
	}
	
	//--------------------------------------------------------------------------
	private void setRadioTitle(int text_id) {
		
		((TextView)findViewById(Res.id.RadioSelectTitleText1)).setText(text_id);
		((TextView)findViewById(Res.id.RadioSelectTitleText2)).setText(text_id);
		
		switch (mTopPageId) {
		
		case Res.id.RadioSelectPage1:
			mPage1TitleId = text_id;
			break;
			
		case Res.id.RadioPlaylistEditPage:
			if (mPlaylistEditor != null) {
				RadioPlayList playlist = mPlaylistEditor.getPlaylist();
				if (playlist != null) {
					String title = getString(text_id) + " - " + playlist.getTitle();
					((TextView)findViewById(Res.id.RadioSelectTitleText1)).setText(title);
					((TextView)findViewById(Res.id.RadioSelectTitleText2)).setText(title);
				}
			}
			break;
		}

	}
	
	//--------------------------------------------------------------------------
	private interface RadioSelectRun {
		void run(RadioSelectActivity activity);
	}
	
	//--------------------------------------------------------------------------
	private enum Page0Items {
		Favorites(Res.string.favorites, Res.drawable.icon_favorites, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showFavorites();
			}
		}),
		NewRecord(Res.string.direct_link, Res.drawable.icon_edit, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showNewLinkPage();
			}
		}),
		History(Res.string.history, Res.drawable.history, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showHistory();
			}
		}),
		Stations(Res.string.stations, Res.drawable.icon_station, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showStations();
			}
		}),
		Genres(Res.string.genres_item, Res.drawable.icon_radio, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showGenres();
			}
		}),
		Playlists(Res.string.playlists, Res.drawable.icon_playlist, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showPlaylists();
			}
		}),
		Search(Res.string.search, Res.drawable.search, new RadioSelectRun() {
			@Override
			public void run(RadioSelectActivity activity) {
				activity.showSearch();
			}
		});
		
		final int mNameId, mIconId;
		final RadioSelectRun mAction;
		
		private Page0Items(int name_id, int icon_id, RadioSelectRun action) {
			mNameId = name_id;
			mIconId = icon_id;
			mAction = action;
		}
	}

	//--------------------------------------------------------------------------
	private class Page0Adapter implements ListAdapter, AdapterView.OnItemClickListener {

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			return Page0Items.values().length;
		}
		
		@Override
		public boolean isEmpty() {
			return getCount() == 0;
		}

		@Override
		public Object getItem(int position) {
			return Page0Items.values()[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(RadioSelectActivity.this).inflate(Res.layout.list0_item, null);
			}
			
			ImageView icon = (ImageView)convertView.findViewById(Res.id.List0ItemIcon);
			if (icon != null)
				icon.setImageResource(Page0Items.values()[position].mIconId);
			
			TextView name = (TextView)convertView.findViewById(Res.id.List0ItemText);
			if (name != null)
				name.setText(Page0Items.values()[position].mNameId);
			
			return convertView;
		}

		private Vector<DataSetObserver> mObserver = new Vector<DataSetObserver>();
		
		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			mObserver.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mObserver.remove(observer);
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Page0Items.values()[position].mAction.run(RadioSelectActivity.this);
		}
	}
	
	//--------------------------------------------------------------------------
	private void showPage(int page_id, int title_id) {

		if (page_id == Res.id.RadioSelectPage2 && mTopPageId == Res.id.RadioSelectPage2)
			return;
		
		final View current_view = findViewById(mTopPageId);
		TranslateAnimation animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, -1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(ANIMATION_TIME);
		animation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				current_view.setVisibility(View.INVISIBLE);
			}
		});
		
		current_view.startAnimation(animation);
		
		View view = findViewById(page_id);
		view.setVisibility(View.VISIBLE);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(ANIMATION_TIME);
		view.startAnimation(animation);
		
		if (mBackPageIdCount == mBackPageIds.length) {
			int[] old_ids = mBackPageIds;
			mBackPageIds = new int[old_ids.length*2];
			System.arraycopy(old_ids, 0, mBackPageIds, 0, old_ids.length);
		}

		mBackPageIds[mBackPageIdCount] = mTopPageId;
		mBackPageIdCount++;
		mTopPageId = page_id;
		
		setRadioTitle(title_id);
	}
	
	//--------------------------------------------------------------------------
	private boolean backPage() {
		
		if (mBackPageIdCount <= 0)
			return false;
		
		final View current_view = findViewById(mTopPageId);
		TranslateAnimation animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		animation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				current_view.setVisibility(View.INVISIBLE);
			}
		});
		
		current_view.startAnimation(animation);

		mBackPageIdCount--;
		mTopPageId = mBackPageIds[mBackPageIdCount];
		switch (mTopPageId) {
		case Res.id.RadioSelectPage1:
			setRadioTitle(mPage1TitleId);
			if (mNeedUpdateFastTreeData) {
				mContentTreeView.setRootElements(mFastTreeDataSource.getElements());
				mNeedUpdateFastTreeData = false;
			}
			break;
			
		case Res.id.RadioSelectPage0:
		default:
			setRadioTitle(Res.string.radio);
			break;
		}
		
		View view = findViewById(mTopPageId);
		view.setVisibility(View.VISIBLE);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, -1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		view.startAnimation(animation);
		
		return true;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (backPage()) {
				return true;
			}
		}
		return super.onKeyDown (keyCode, event);
	}
	
	//--------------------------------------------------------------------------
	private void showListPage(int title_id, FastTreeDataSource source) {
		mSearchPanel.setVisibility(View.GONE);
		mSearchEditor.setEditTextChangeListener(null);
		mSearchEditor.storeHistory();
		mSearchEditor.setText("");
		
		mFastTreeDataSource = source;
		mNeedUpdateFastTreeData = false;

		mContentTreeView.setRootElementsAndClearFilter(null);
		mContentTreeView.setRootElements(source.getElements());
		mContentTreeView.setFastTreeListener(mRadioRecordListener);
		
		showPage(Res.id.RadioSelectPage1, title_id);
	}
	
	//--------------------------------------------------------------------------
	private void showFavorites() {
		showListPage(Res.string.favorites, new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				reloadPlaylists();
				
				Vector<RecordBase> favorite_list = new Vector<RecordBase>();
				
				for (RadioPlayList playlist : mPlaylists) {
					if (playlist.isFavorite()) {
						favorite_list.add(playlist);
					}
				}
				
				Vector<RadioRecord> records = mRecordsManager.getFavoriteRecords();
				if (records != null && records.size() > 0) {
					favorite_list.addAll(records);
				}
				
				Collections.sort(favorite_list);
				
				RecordBase[] records_array = new RecordBase[favorite_list.size()];
				favorite_list.toArray(records_array);
				return records_array;
			}
		});
	}

	//--------------------------------------------------------------------------
	private void showNewLinkPage() {
			
		mDirectLinkEditor.setText(getString(Res.string.http_prefix));
		mDirectLinkEditor.reloadHistory();
		
		showPage(Res.id.RadioEditPage, Res.string.direct_link);
	}

	//--------------------------------------------------------------------------
	private void showHistory() {
		showListPage(Res.string.history, new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				Vector<RadioRecord> records = mRecordsManager.getHistoryRecords();
				RadioRecord[] records_array = new RadioRecord[records.size()];
				records.toArray(records_array);
				return records_array;
			}
		});
	}

	//--------------------------------------------------------------------------
	private void showSearch() {

		mSearchEditor.setText("");
		mSearchEditor.reloadHistory();
		mSearchEditor.setEditTextChangeListener(this);
		mSearchPanel.setVisibility(View.VISIBLE);
		
		mFastTreeDataSource = new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				Vector<RadioRecord> records = mRecordsManager.getAllRecords();
				RadioRecord[] records_array = new RadioRecord[records.size()];
				records.toArray(records_array);
				return records_array;
			}
		};
		
		mNeedUpdateFastTreeData = false;
		
		mContentTreeView.setRootElementsAndClearFilter(mFastTreeDataSource.getElements());
		mContentTreeView.setFastTreeListener(mRadioRecordListener);
		
		showPage(Res.id.RadioSelectPage1, Res.string.search);
	}

	//--------------------------------------------------------------------------
	private void showStations() {
		showListPage(Res.string.stations, new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				Vector<RadioRecord> all_records = mRecordsManager.getAllRecords();
				Comparator<RadioRecord> comparator = new Comparator<RadioRecord>() {
					@Override
					public int compare(RadioRecord record1, RadioRecord record2) {
						String station1 = record1.getStationName();
						String station2 = record2.getStationName();
						
						station1 = (station1 == null ? "" : station1.toLowerCase());
						station2 = (station2 == null ? "" : station2.toLowerCase());
						
						return station1.compareTo(station2);
					}
				};

				Collections.sort(all_records, comparator);
				
				Vector<FastTreeItem> stations = new Vector<FastTreeItem>();
				FastTreeItem last_station = null;
				
				for (RadioRecord record : all_records) {
					if (record != null) {
						try {
							if (stations.size() > 0 && last_station != null) {
	
								if (last_station instanceof GroupRecord) {
	
									GroupRecord group = (GroupRecord)last_station; 
									if (record.getStationName().equalsIgnoreCase(group.getTitle())) {
	
										group.add(record);
	
									} else {
	
										stations.add(record);
										last_station = record;
									}
	
								} else {
	
									RadioRecord last_radio = (RadioRecord)last_station;
									if (record.getStationName().equalsIgnoreCase(last_radio.getStationName())) {
	
										GroupRecord station = new GroupRecord(record.getStationName());
										station.add(last_radio);
										station.add(record);
										stations.remove(last_station);
										stations.add(station);
										last_station = station;
	
									} else {
	
										stations.add(record);
										last_station = record;
									}
								}
	
							} else {
	
								stations.add(record);
								last_station = record;
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				FastTreeItem[] records_array = new FastTreeItem[stations.size()];
				stations.toArray(records_array);
				return records_array;
			}
		});
	}
	
	//--------------------------------------------------------------------------
	private void showGenres() {

		showListPage(Res.string.genres_item, new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				Vector<RadioRecord> all_records = mRecordsManager.getAllRecords();
				Vector<GroupRecord> stations = new Vector<GroupRecord>();
				Scanner scanner;

				for (RadioRecord record : all_records) {
					if (record != null) {
						String genre = record.getGenre();
						if (genre != null) { // && genre.length() > 0) {
							
							scanner = new Scanner(record.getGenre());
							scanner.useDelimiter(",");
							while (scanner.hasNext()) {
								
								genre = scanner.next().trim();
								if (genre != null && genre.length() > 0) {
	
									boolean new_station = true;
									String upper_genre = genre.toUpperCase(); 
									
									for (GroupRecord station : stations) {
										if (station.compareUpperName(upper_genre) == 0) {
											new_station = false;
											station.add(record);
											break;
										}
									}
									
									if (new_station) {
										GroupRecord station = new GroupRecord(genre);
										station.add(record);
										stations.add(station);
									}
								}
							}
						}
					}
				}
				
				Collections.sort(stations);
				
				GroupRecord[] records_array = new GroupRecord[stations.size()];
				stations.toArray(records_array);
				return records_array;
			}
		});
	}
	
	//--------------------------------------------------------------------------
	private void reloadPlaylists() {
		Vector<RecordBase> playlists = Storage.getRadioPlaylists(mRecordsManager);

		mPlaylists.clear();
		if (playlists != null) {
			for (RecordBase playlist : playlists) {
				if (playlist instanceof RadioPlayList) {
					mPlaylists.add((RadioPlayList)playlist);
				}
			}
		}

		Collections.sort(mPlaylists);
	}
	
	//--------------------------------------------------------------------------
	/*private RadioPlayList[] getPlaylistsForContentTree() {
		reloadPlaylists();
		
		int count = mPlaylists.size();
		RadioPlayList[] records_array = new RadioPlayList[count];
		if (count > 0) {
			mPlaylists.toArray(records_array);
		}
	
		return records_array;
	}*/
	
	//--------------------------------------------------------------------------
	private void showPlaylists() {
		showListPage(Res.string.playlists, new FastTreeDataSource() {
			@Override
			public FastTreeItem[] getElements() {
				reloadPlaylists();
				
				int count = mPlaylists.size();
				RadioPlayList[] records_array = new RadioPlayList[count];
				if (count > 0) {
					mPlaylists.toArray(records_array);
				}
			
				return records_array;
			}
		});
	}
	
	//--------------------------------------------------------------------------
	private final FastTreeListener mRadioRecordListener = new FastTreeListener() {

		//----------------------------------------------------------------------
		@Override
		public void onFastTreeItemClick(FastTreeItem item)
		{
			/*if (item instanceof RadioRecord) {
				startRadio((RadioRecord)item);
			} else if (item instanceof PlayList) {
				startPlaylist((PlayList)item);
			}*/
			
			String linkClickAction =
				PreferenceManager.getDefaultSharedPreferences(RadioSelectActivity.this).getString(getString(Res.string.pref_links_list_click_action_key), "ShowMenu");
			
			if (item instanceof RadioRecord)
			{
				if (linkClickAction.equalsIgnoreCase("ShowMenu"))
				{
					(new RadioMenu((RadioRecord)item)).show();
				}
				else if (linkClickAction.equalsIgnoreCase("StartContent"))
				{
					startRadio((RadioRecord)item);
				}
			}
			else if (item instanceof RadioPlayList)
			{
				if (linkClickAction.equalsIgnoreCase("ShowMenu"))
				{
					(new PlaylistMenu((RadioPlayList)item)).show();
				}
				else if (linkClickAction.equalsIgnoreCase("StartContent"))
				{
					startPlaylist((RadioPlayList)item);
				}
			}
		}

		//----------------------------------------------------------------------
		@Override
		public void onFastTreeItemLongClick(FastTreeItem item)
		{
			if (item instanceof RadioRecord)
			{
				(new RadioMenu((RadioRecord)item)).show();
			}
			else if (item instanceof RadioPlayList)
			{
				(new PlaylistMenu((RadioPlayList)item)).show();
			}
		}

		//----------------------------------------------------------------------
		@Override
		public void onFastTreeRightIconClick(FastTreeItem item, int number) {
			if (item instanceof RecordBase) {
				RecordBase record = (RecordBase)item;
				record.setFavorite(!record.isFavorite());
				mContentTreeView.invalidate();
				updateRecordInThread(record);
			}
		}

		//----------------------------------------------------------------------
		@Override
		public void onFastTreeRightIconLongClick(FastTreeItem item, int number) {
			onFastTreeItemLongClick(item);
		}
	};
	
	//--------------------------------------------------------------------------
	private void updateRecordInThread(final RecordBase record) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Storage.updateRecord(RadioSelectActivity.this, record);
			}
		});
		thread.start();
	}
	
	//--------------------------------------------------------------------------
	private final class RadioMenu implements DialogInterface.OnClickListener {

		private final static int PLAY_COMMAND = 0;
		private final static int FAVORITE_COMMAND = 1;
		private final static int ADD_TO_PLAYLIST_COMMAND = 2;
		private final static int REMOVE_COMMAND = 3;
		private final static int SEND_EMAIL = 4;
		private final static int COMMAND_COUNT = 5;
		//private final static int CATEGORY_COMMAND = 5;
		
		private final RadioRecord mRecord;
		
		RadioMenu(RadioRecord record) {
			mRecord = record;
		}
		
		//----------------------------------------------------------------------
		void show() {
			String[] items = new String[COMMAND_COUNT];

			items[PLAY_COMMAND]		= getString(Res.string.start);
			items[FAVORITE_COMMAND]	= getString(mRecord.isFavorite() ? 
					Res.string.remove_from_favorites : Res.string.add_to_favorites);
			items[ADD_TO_PLAYLIST_COMMAND] = getString(Res.string.add_to_playlist);
			items[REMOVE_COMMAND]	= getString(Res.string.remove);
			items[SEND_EMAIL]	= getString(Res.string.send_email_menu);
			//items[CATEGORY_COMMAND]	= getString(Res.string.category);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(RadioSelectActivity.this);
			builder.setItems(items, this);
			//builder.setTitle(mRecord.getTitle());
			builder.setTitle(mRecord.getUrl());
			builder.setCancelable(true);
			
			builder.show().setCanceledOnTouchOutside(true);
		}
		
		//----------------------------------------------------------------------
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case PLAY_COMMAND:
				startRadio(mRecord);
				break;
				
			case FAVORITE_COMMAND:
				mRecord.setFavorite(!mRecord.isFavorite());
				mContentTreeView.invalidate();
				updateRecordInThread(mRecord);
				break;

			case ADD_TO_PLAYLIST_COMMAND:
				(new PlaylistSelector(mRecord)).show();
				break;

			case REMOVE_COMMAND:
				Utilities.showQuestion(RadioSelectActivity.this,
						//getString(Res.string.remove_question, mRecord.getTitle()), 
						getString(Res.string.remove_question, mRecord.getUrl()),
						new RadioRemoveYesListener(mRecord));
				break;
				
			case SEND_EMAIL:
				
				Intent send_mail_intent = new Intent(Intent.ACTION_SEND);
				send_mail_intent.setType("message/rfc822");
				send_mail_intent.putExtra(Intent.EXTRA_TEXT, mRecord.getUrl());
				send_mail_intent.putExtra(Intent.EXTRA_SUBJECT, "subject" /*getString(R.string.email_subject)*/);
				startActivity(Intent.createChooser(send_mail_intent, getString(Res.string.email_chooser_title)));
				
				break;

				
			//case CATEGORY_COMMAND:
			//	break;
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private class RadioRemoveYesListener implements DialogInterface.OnClickListener, Runnable {

		private final RadioRecord mRecord;
		
		RadioRemoveYesListener(RadioRecord record) {
			mRecord = record;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			(new Thread(this)).start();
		}

		@Override
		public void run() {
			reloadPlaylists();
			for (RadioPlayList playlist : mPlaylists) {
				if (playlist.isExists(mRecord)) {
					playlist.remove(RadioSelectActivity.this, mRecord);
				}
			}
			
			mRecordsManager.remove(mRecord);
			
			// Item from mFastTreeDataSource is deleted in onRecordListChanged
		}
	}
	
	//--------------------------------------------------------------------------
	private final class PlaylistMenu implements DialogInterface.OnClickListener {

		private final static int PLAY_COMMAND = 0;
		private final static int FAVORITE_COMMAND = 1;
		private final static int EDIT_COMMAND = 2;
		private final static int REMOVE_COMMAND = 3;
		private final static int COMMAND_COUNT = 4;
		
		private final RadioPlayList mPlaylist;
		
		PlaylistMenu(RadioPlayList playlist) {
			mPlaylist = playlist;
		}
		
		//----------------------------------------------------------------------
		void show() {
			String[] items_array = new String[COMMAND_COUNT];
			
			items_array[PLAY_COMMAND] = getString(Res.string.start);
			items_array[FAVORITE_COMMAND] = getString(mPlaylist.isFavorite() ? 
					Res.string.remove_from_favorites : Res.string.add_to_favorites);
			items_array[EDIT_COMMAND] = getString(Res.string.edit);
			items_array[REMOVE_COMMAND] = getString(Res.string.remove);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(RadioSelectActivity.this);
			builder.setItems(items_array, this);
			builder.setTitle(mPlaylist.getTitle());
			builder.setCancelable(true);
			
			builder.show().setCanceledOnTouchOutside(true);
		}
		
		//----------------------------------------------------------------------
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case PLAY_COMMAND:
				startPlaylist(mPlaylist);
				break;
				
			case FAVORITE_COMMAND:
				mPlaylist.setFavorite(!mPlaylist.isFavorite());
				mContentTreeView.invalidate();
				updateRecordInThread(mPlaylist);
				break;
				
			case EDIT_COMMAND:
				if (mPlaylistEditor != null) {
					mPlaylistEditor.setPlaylist(mPlaylist);
					showPage(Res.id.RadioPlaylistEditPage, Res.string.edit);
				}
				break;

			case REMOVE_COMMAND:
				Utilities.showQuestion(RadioSelectActivity.this, 
						getString(Res.string.remove_question, mPlaylist.getTitle()), 
						new PlaylistRemoveYesListener(mPlaylist));
				break;
				
			/*case CATEGORY_COMMAND:
				break;*/
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private class PlaylistRemoveYesListener implements DialogInterface.OnClickListener {

		private final RadioPlayList mPlaylist;
		
		PlaylistRemoveYesListener(RadioPlayList playlist) {
			mPlaylist = playlist;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			Storage.deleteRecord(RadioSelectActivity.this, mPlaylist);
			mContentTreeView.setRootElements(mFastTreeDataSource.getElements());
			mNeedUpdateFastTreeData = false;
		}
	}
	
	//--------------------------------------------------------------------------
	private class PlaylistCreator implements DialogInterface.OnClickListener {
		
		private final RadioRecord mRecord;
		private EditText mEditor;
		
		PlaylistCreator(RadioRecord record) {
			mRecord = record;
			
		}
		
		//----------------------------------------------------------------------
		void show() {
			show(null);
		}
		
		//----------------------------------------------------------------------
		private void show(String name) {
			
			View root_view = LayoutInflater.from(RadioSelectActivity.this).inflate(Res.layout.playlist_new, null);
			mEditor = (EditText)root_view.findViewById(Res.id.PlaylistNewEditor);
			if (name != null) {
				mEditor.setText(name.trim());
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(RadioSelectActivity.this);
			builder.setTitle(Res.string.select_playlist);
			builder.setView(root_view);
			builder.setCancelable(true);
			builder.setPositiveButton(Res.string.ok, this);
			builder.setNegativeButton(Res.string.cancel, null);
			builder.show();
		}

		//----------------------------------------------------------------------
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String name = mEditor.getText().toString().trim();
			if (name.length() == 0) {
				Utilities.showMessage(RadioSelectActivity.this, 
						Res.string.invalid_playlist_name, new RestartDialog(name));
				return;
			}

			for (RecordBase playlist : mPlaylists) {
				if (name.equalsIgnoreCase(playlist.getDescription())) {
					AlertDialog.Builder builder = new AlertDialog.Builder(RadioSelectActivity.this);
					builder.setCancelable(true);
					builder.setPositiveButton(Res.string.yes, new AddToExistingPlaylist((RadioPlayList)playlist));
					builder.setNegativeButton(Res.string.no, new RestartDialog(name));
					//builder.setMessage(getString(Res.string.playlist_exists, playlist.getTitle(), mRecord.getTitle()));
					builder.setMessage(getString(Res.string.playlist_exists, playlist.getTitle(), mRecord.getUrl()));
					builder.show();
					
					return;
				}
			}
			
			RadioPlayList playlist = new RadioPlayList(mRecord.getRecordsManager(), -1, name);
			Storage.addRecord(RadioSelectActivity.this, playlist);
			mPlaylists.add(playlist);
			playlist.addNew(RadioSelectActivity.this, mRecord);
		}
		
		//----------------------------------------------------------------------
		private class AddToExistingPlaylist implements DialogInterface.OnClickListener {

			private final RadioPlayList mPlaylist;
			
			AddToExistingPlaylist(RadioPlayList playlist) {
				mPlaylist = playlist;
			}
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addRecordToPlaylist(mRecord, mPlaylist);
			}
		}		
		
		//----------------------------------------------------------------------
		private class RestartDialog implements DialogInterface.OnClickListener {

			private final String mName;
			
			RestartDialog(String name) {
				mName = name;
			}
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				show(mName);
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private class PlaylistSelector implements DialogInterface.OnClickListener {
		
		private final RadioRecord mRecord;
		
		PlaylistSelector(RadioRecord record) {
			mRecord = record;
		}
		
		//----------------------------------------------------------------------
		void show() {
			reloadPlaylists();
			
			int playlist_count = mPlaylists.size();
			CharSequence[] items = new CharSequence[playlist_count+1];
			items[0] = Html.fromHtml("<b>" + getString(Res.string.new_playlist) + "</b>");
			for (int i=0; i<playlist_count; i++) {
				items[i+1] = mPlaylists.get(i).getTitle();
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(RadioSelectActivity.this);
			builder.setItems(items, this);
			builder.setTitle(Res.string.select_playlist);
			builder.setCancelable(true);
			
			builder.show().setCanceledOnTouchOutside(true);
		}

		//----------------------------------------------------------------------
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == 0) {
				(new PlaylistCreator(mRecord)).show();
			} else if (which > 0 && which <= mPlaylists.size()) {
				addRecordToPlaylist(mRecord, mPlaylists.get(which-1));
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private void addRecordToPlaylist(RecordBase record, RadioPlayList playlist) {
		
		if (playlist.isExists(record)) {
			
			Toast.makeText(RadioSelectActivity.this, 
					//getString(Res.string.already_in_playlist, record.getTitle(), playlist.getTitle()), 
					getString(Res.string.already_in_playlist, record.getUrl(), playlist.getTitle()), 
					Toast.LENGTH_LONG).show();
			
		} else if (playlist.addNew(RadioSelectActivity.this, record)) {

			Toast.makeText(RadioSelectActivity.this, 
					//getString(Res.string.added_to_playlist, record.getTitle(), playlist.getTitle()), 
					getString(Res.string.added_to_playlist, record.getUrl(), playlist.getTitle()), 
					Toast.LENGTH_LONG).show();

		} else {
			
			// TODO error
		}	
	}
	
	//--------------------------------------------------------------------------
	public void startRadio(RadioRecord record) {
		
		if (mServiceInterface != null) {
			try {
				//new HttpProxy().init(record);
				mServiceInterface.startRadio(record.getId());
			} catch (RemoteException e) {
				e.printStackTrace();
				return;
			}catch (Exception e){

			}
		} else {
			return;
		}
		
		record.setLastAccessedDate(System.currentTimeMillis());
		mRecordsManager.updateRecord(record);
		
		TextView text_view = (TextView)findViewById(Res.id.CurrentRadioStation);
		if (text_view != null) {
			text_view.setText(record.getStationName());
		}

		setRadioInfo(record.getStationName(), record.getGenre(), 
				record.getContentDescription(), record.getUrl(), null);
		
		mNextButton.setVisibility(View.GONE);
		mCurrentRecord = record;
		
		View view = findViewById(Res.id.CurrentRadioControl);
		if (view.getVisibility() != View.VISIBLE)
			view.setVisibility(View.VISIBLE);
		
		showPage(Res.id.RadioSelectPage2, Res.string.radio);
		
		setPlaying(true);
	}

	//--------------------------------------------------------------------------
	public void startPlaylist(PlayList playlist) {
		
		if (playlist.getMemberCount() == 0) {
			Toast.makeText(this, Res.string.empty_playlist, Toast.LENGTH_SHORT).show();
			return;
		}
			
		if (mServiceInterface != null) {
			try {
				mServiceInterface.startRadioPlaylist(playlist.getId());
			} catch (RemoteException e) {
				e.printStackTrace();
				return;
			}
		} else {
			return;
		}
		
		mNextButton.setVisibility(playlist.getMemberCount() > 1 ? View.VISIBLE : View.GONE);
		
		mCurrentRecord = playlist;
		
		View view = findViewById(Res.id.CurrentRadioControl);
		if (view.getVisibility() != View.VISIBLE)
			view.setVisibility(View.VISIBLE);
		
		showPage(Res.id.RadioSelectPage2, Res.string.radio);
		
		setPlaying(true);
	}
	
	//--------------------------------------------------------------------------
	private void setRadioInfoText(String text, int view_id) {
		TextView text_view = (TextView)findViewById(view_id);
		if (text_view != null) {
			if (text != null && !text.equalsIgnoreCase("null")) {
				text_view.setText(text);
			} else {
				text_view.setText(Res.string.Unknown);
			}
		}
	}
	//--------------------------------------------------------------------------
	private void setRadioInfo(String name, String genre, String content_type, String url, String url_text) {

		setRadioInfoText(name, Res.id.RadioSelectRadioName);
		setRadioInfoText(genre, Res.id.RadioSelectGenre);
		setRadioInfoText(content_type, Res.id.RadioSelectContentType);
		
		TextView text_view = (TextView)findViewById(Res.id.CurrentRadioStation);
		if (text_view != null)
		{
			if(!name.equalsIgnoreCase("null"))
			{
				text_view.setText(name);
			}
			else if(!url.equalsIgnoreCase("null"))
			{
				text_view.setText(url);
			}
			else
			{
				text_view.setText("");
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private void postRadioInfoInvalidate() {
		int[] text_view_ids = {
				Res.id.RadioSelectRadioName, 
				Res.id.RadioSelectGenre, 
				Res.id.RadioSelectContentType
		};
		
		for (int id : text_view_ids) {
			TextView text_view = (TextView)findViewById(id);
			if (text_view != null) {
				text_view.postInvalidate();
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private void setRadioTitle(String text) {
		if (text == null) {
			text = getString(Res.string.Unknown);
		}
			
		TextView text_view = (TextView)findViewById(Res.id.CurrentRadioTitle);
		if (text_view != null) {
			text_view.setText(text);
			text_view.postInvalidate();
		}
		
		text_view = (TextView)findViewById(Res.id.RadioSelectComposition);
		if (text_view != null) {
			text_view.setText(text);
			text_view.postInvalidate();
		}
	}
	
	//--------------------------------------------------------------------------
	public void setPlaying(boolean playing) {
		mPlaying = playing;
		mPlayStopButton.setImageResource(playing ? Res.drawable.stop : Res.drawable.play);
	}

	//--------------------------------------------------------------------------
	private final View.OnClickListener mPlayPauseClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			if (mServiceInterface != null) {

				try {
					if (mPlaying) {
						
						mServiceInterface.stopRadio();
						setPlaying(false);
						
					} else if (mCurrentRecord != null) {
						
						if (mCurrentRecord instanceof RadioRecord) {
							
							mServiceInterface.startRadio(mCurrentRecord.getId());
							setPlaying(true);
							
						} else if (mCurrentRecord instanceof RadioPlayList) {
							
							mServiceInterface.startRadioPlaylist(mCurrentRecord.getId());
							setPlaying(true);
						}
					}
					
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	//--------------------------------------------------------------------------
	private final View.OnClickListener mNextClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			if (mServiceInterface != null) {

				try {
					if (mPlaying) {						
						mServiceInterface.nextRadio();
					}
					
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	//--------------------------------------------------------------------------
	private void processAction(int action, String info) {
		try {
			boolean playing = true;
			View view = findViewById(Res.id.CurrentRadioControl);
			
			if (action != MediaService.ACTION_UPDATE_STATE_CHANGED 
					&& view.getVisibility() != View.VISIBLE) {
				view.setVisibility(View.VISIBLE);
				mNextButton.setVisibility(mServiceInterface.isRadioPlaylistPlaying() ? View.VISIBLE : View.GONE);
			}

			switch (action) {
			
			case MediaService.ACTION_RADIO_PLAYING:
				setRadioTitle(getString(Res.string.playing));
				break;

			case MediaService.ACTION_RADIO_STARTED:
				if (info != null) {
					String[] data = info.split("\n");
					if (data != null && data.length > LinkParser.MetaInfo.INDEX_PROVIDER_URL) {
						setRadioInfo(data[LinkParser.MetaInfo.INDEX_NAME], 
								data[LinkParser.MetaInfo.INDEX_GENRE], 
								data[LinkParser.MetaInfo.INDEX_CONTENT], 
								data[LinkParser.MetaInfo.INDEX_URL], 
								data[LinkParser.MetaInfo.INDEX_PROVIDER_URL]);
						postRadioInfoInvalidate();
					}
				}
				break;

			case MediaService.ACTION_RADIO_ERROR:
				setRadioTitle(getString(Res.string.error));
				playing = false;
				break;

			case MediaService.ACTION_RADIO_STOPPED:
				setRadioTitle(getString(Res.string.stopped));
				playing = false;
				break;

			case MediaService.ACTION_RADIO_STARTING:
				setRadioTitle(getString(Res.string.starting));
				if (info != null) {
					String[] data = info.split("\n");
					String station = null;
					if (data != null) {
						if (data.length > 0) {
							station = data[0];
						}

						setRadioInfo(station, 
								data.length > 1 ? data[1] : null, // genre
								data.length > 2 ? data[2] : null, // content_type 
								data.length > 3 ? data[3] : null, // url 
								null);
					}

					if (station != null) {
						TextView text_view = (TextView)findViewById(Res.id.CurrentRadioStation);
						if (text_view != null)
							text_view.setText(station);
					}
				}
				break;

			case MediaService.ACTION_RADIO_RECONNECTING_WAIT:
				if (info != null) {
					setRadioTitle(getString(Res.string.reconnect_delay) + ' ' + info + "s");
				} else {
					setRadioTitle(getString(Res.string.reconnect_delay));
				}
				break;
					
			case MediaService.ACTION_RADIO_RECONNECTING_START:
				setRadioTitle(getString(Res.string.reconnecting));
				break;

			case MediaService.ACTION_RADIO_COMPOSITION:
				if (info != null && info.length() > 0) {
					setRadioTitle(info);
				}
				break;

			case MediaService.ACTION_RADIO_BUFFERING:
				if (info != null) {
					setRadioTitle(getString(Res.string.buffering) + ' ' + info + "s");
				} else {
					setRadioTitle(getString(Res.string.buffering));
				}
				break;

				
			case MediaService.ACTION_RADIO_NEW_RECORD:
				if (info != null) {
					String[] data = info.split("\n");
					if (data != null && data.length >= 4) {

						String address	= data[0];
						String name		= data[1];
						String genre	= data[2];
						//String content	= data[3];
						
						ShareLinkDialog.share(this, Res.drawable.icon_radio,
								address, ShareLinkDialog.INTERNET_RADIO, genre, name, "-");
					}
				}
				// goto MediaService.ACTION_RADIO_UPDATE_RECORDS

			case MediaService.ACTION_RADIO_UPDATE_RECORDS:
				mRecordsManager.reloadRecords();
				if (mTopPageId == Res.id.RadioSelectPage1) {
					mContentTreeView.setRootElements(mFastTreeDataSource.getElements());
				} else {
					mNeedUpdateFastTreeData = true;
				}
				return;

			case MediaService.ACTION_UPDATE_STATE_CHANGED:
				if (info != null) {
					try {
						updateStateChanged(Integer.parseInt(info));
					} catch (NumberFormatException ex) {
					}
				}
				return;
				
			default:
				Log.d("RadioSelectActivity", "processAction. DEFAULT");
			}
			
			if (playing != mPlaying)
				setPlaying(playing);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//--------------------------------------------------------------------------
	BroadcastReceiver mServiceMsgsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifi.getConnectionInfo();
		    if(!wifi.isWifiEnabled())
		    {
		    	Log.d("BroadcastReceiver", "You are NOT connected");
		    }

			processAction(intent.getIntExtra(MediaService.SEND_ACTION, 0), 
					intent.getStringExtra(MediaService.SEND_DATA));
		}
	};
	
	//--------------------------------------------------------------------------
	private void updateStateChanged(int state) {
		
		if (state == UpdateService.UPDATE_STATE_IDLE) {
			showUpdateResult();
		}
		
		/*View update_status_view = findViewById(Res.id.radio_updating_links);
		
		switch (state) {
		case UpdateService.UPDATE_STATE_IDLE:
			if (update_status_view != null) {
				update_status_view.setVisibility(View.INVISIBLE);
			}
			
			if (showUpdateResult()) {
				updateLinkList();
			}
			break;
			
		case UpdateService.UPDATE_STATE_CONNECTING:
			if (update_status_view != null) {
				update_status_view.setVisibility(View.VISIBLE);
			}
			break;

		case UpdateService.UPDATE_STATE_PARSING:
			if (update_status_view != null) {
				update_status_view.setVisibility(View.VISIBLE);
			}
			break;
		}*/
	}

	//--------------------------------------------------------------------------
	private boolean showUpdateResult() {
		
		if (mServiceInterface != null) {
			
			mRecordsManager.reloadRecords();
			
			try {
				int new_count  = mServiceInterface.getUpdatedLinkCount(UpdateService.NEW_RADIO_LINK);
				int dead_count = mServiceInterface.getUpdatedLinkCount(UpdateService.DEAD_RADIO_LINK);
				
				if (new_count + dead_count > 0) {
					
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setNeutralButton(Res.string.ok, null);
					builder.setCancelable(true);
					
					View contentView = getLayoutInflater().inflate(Res.layout.links_update_completed_dlg, null);
					
					TextView text_view = (TextView)contentView.findViewById(Res.id.new_links_count);
					if (text_view != null) {
						text_view.setText(Integer.toString(new_count));
					}
					
					text_view = (TextView)contentView.findViewById(Res.id.dead_links_count);
					if (text_view != null) {
						text_view.setText(Integer.toString(dead_count));
					}

					builder.setView(contentView);
					builder.show().setCanceledOnTouchOutside(true);
					
					return true;
				}
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public void onRecordListChanged() {
		// TODO
		if (mRecordsManager != null) {
			mRecordsManager.reloadRecords();
			if (mTopPageId == Res.id.RadioSelectPage1) {
				mContentTreeView.setRootElements(mFastTreeDataSource.getElements());
				//mContentTreeView.invalidate();
			} else {
				mNeedUpdateFastTreeData = true;
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private void startEditorLink() {
		
		String address = null;
		//int requestCode = VIEW_NEW_ACTIVITY_CODE;
		
		if (mTopPageId == Res.id.RadioEditPage) {
			
			address = mDirectLinkEditor.getText().trim();
			if (address.length() == 0 || address.equals(getString(Res.string.http_prefix)))
			{
				Toast.makeText(RadioSelectActivity.this, 
					getString(Res.string.msg_enter_url), Toast.LENGTH_LONG).show();
				return;
			}
			
			//requestCode = VIEW_NEW_ACTIVITY_CODE;
			// TODO
			mDirectLinkEditor.storeHistory();
			mDirectLinkEditor.reloadHistory();
			
			try {
				mServiceInterface.startNewRadio(address);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		} else {
			
			/*if (mCurrentItem == null)
				return;
			
			address = mCurrentItem.getUrl();
			requestCode = VIEW_HISTORY_ACTIVITY_CODE;

			if (mCurrentItem.isDeadLink()) {
				showDeadLinkDialog(mCurrentItem);
				return;
			}*/
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void onEditTextChanged(String newText) {
		mContentTreeView.applyFilter(newText);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == 0)
		{
			if(requestCode == Command.SETTINGS)
			{
				mRecordsManager.updateAllRecords();
				onRecordListChanged();
				//mContentTreeView.invalidate();
			}
		}
	}
}
