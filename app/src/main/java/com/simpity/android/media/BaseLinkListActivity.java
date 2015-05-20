package com.simpity.android.media;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.simpity.android.media.IMediaServiceInterface;
import com.simpity.android.media.Res;
import com.simpity.android.media.VersionConfig;
import com.simpity.android.media.controls.fasttree.FastTree;
import com.simpity.android.media.controls.fasttree.FastTreeItem;
import com.simpity.android.media.controls.fasttree.FastTreeListener;
import com.simpity.android.media.services.UpdateService;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;

public abstract class BaseLinkListActivity extends FourTabActivity implements
		RecordsManager.OnListChangedListener, FourTabActivity.onTabClickListener, FastTreeListener {

	protected final static int VIEW_NEW_ACTIVITY_CODE = 101;
	protected final static int VIEW_HISTORY_ACTIVITY_CODE = 102;
	protected final static int EDIT_ACTIVITY_CODE = 103;
	
	protected IMediaServiceInterface mMediaServiceInterface;
	protected final Handler mHandler = new Handler();
	
	protected Bundle mSavedAllLinksListState = null;
	protected Bundle mSavedSearchLinksListState = null;

	private AlertDialog mUpdateResultDialog = null;
	private RemoveDialog mRemoveDialog = null;
	private AlertDialog mDeadLinkDialog = null;
	
	protected FastTree mAllLinksList;
	protected ListView mFavoritesList;
	protected ListView mHistoryList;
	protected EditText mSearchEditor;

	//--------------------------------------------------------------------------
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mMediaServiceInterface = IMediaServiceInterface.Stub.asInterface(service);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						updateStateChanged(mMediaServiceInterface.getUpdateState());
					
					} catch (RemoteException ex) {
						ex.printStackTrace();
					}
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mMediaServiceInterface = null;
		}
	};
	
	//--------------------------------------------------------------------------
	private BroadcastReceiver mServiceMsgsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifi.getConnectionInfo();
		    if(!wifi.isWifiEnabled())
		    {
		    	Log.d("BroadcastReceiver", "You are NOT connected");
		    }

		    onReceiveServiceMessage(context, intent);
		}
	};
	
	//--------------------------------------------------------------------------
	protected void onReceiveServiceMessage(Context context, Intent intent) {
		int command = intent.getIntExtra(MediaService.SEND_ACTION, -1);
		if (command == MediaService.ACTION_UPDATE_STATE_CHANGED) {
			try {
				String state = intent.getStringExtra(MediaService.SEND_DATA);
				if (state != null) {
					updateStateChanged(Integer.parseInt(state));
				}
			} catch (NumberFormatException ex) {
			}
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		bindService(new Intent(this, MediaService.class), mConnection, Context.BIND_AUTO_CREATE);
		registerReceiver(mServiceMsgsReceiver, new IntentFilter(VersionConfig.MEDIA_SERVICE_INTENT));
		
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mHandler.removeCallbacks(this);
				if (getCurrentPage() == SEARCH_COMMAND && mSearchEditor != null) {
					String filter = mSearchEditor.getText().toString();
					if (filter.length() == 0)
						filter = null;

					String oldFilter = null;
					if (mAllLinksList != null) {
						oldFilter = mAllLinksList.getFilter();
					}
					
					if ((oldFilter == null && filter == null) ||
						(oldFilter != null && oldFilter.equalsIgnoreCase(filter))) {
						mHandler.postDelayed(this, 100);
						return;
					}
					
					if (mAllLinksList != null) {
						mAllLinksList.applyFilter(filter);
					}
				}
				
				mHandler.postDelayed(this, 100);
			}
		}, 100);


	}

	//--------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mMediaServiceInterface != null) {
			try {
				updateStateChanged(mMediaServiceInterface.getUpdateState());
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		
		if (mUpdateResultDialog != null) {
			mUpdateResultDialog.dismiss();
			mUpdateResultDialog = null;
		}
		
		if (mRemoveDialog != null) {
			mRemoveDialog.dismiss();
			mRemoveDialog = null;
		}
		
		if (mDeadLinkDialog != null) {
			mDeadLinkDialog.dismiss();
			mDeadLinkDialog = null;
		}
		
		unregisterReceiver(mServiceMsgsReceiver);
		unbindService(mConnection);
		
		super.onDestroy();
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onTabClicked(int prevTabNumber, int tabNumber, int settedPage) {
		if (prevTabNumber == ALL_LINKS_PAGE && tabNumber != ALL_LINKS_PAGE) {
			mSavedAllLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedAllLinksListState);
		}
		
		if (prevTabNumber == SEARCH_COMMAND && tabNumber != SEARCH_COMMAND) {
			mSavedSearchLinksListState = new Bundle();
			mAllLinksList.storeState(mSavedSearchLinksListState);
		}

		if (tabNumber == SEARCH_COMMAND && prevTabNumber != SEARCH_COMMAND) {
			if (mSavedSearchLinksListState != null)
				mAllLinksList.restoreState(mSavedSearchLinksListState);
			
			mSearchEditor.setText(mAllLinksList.getFilter());
			mSearchEditor.requestFocus();
			mSearchEditor.selectAll();
			return;
		}
		
		if (tabNumber == ALL_LINKS_PAGE && prevTabNumber != ALL_LINKS_PAGE) {
			if (mSavedAllLinksListState != null) {
				mAllLinksList.restoreState(mSavedAllLinksListState);
			}
			mSearchEditor.getText().clear();
		}
	}
	
	//--------------------------------------------------------------------------
	private void updateStateChanged(int state) {
		
		View update_status_view = findViewById(getUpdateStatusId());
		
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
		}
	}
	
	//--------------------------------------------------------------------------
	protected abstract int getUpdateStatusId();
	protected abstract void updateLinkList();
	protected abstract int getLinkType();
	protected abstract RecordsManager getRecordsManager();
	
	//--------------------------------------------------------------------------
	protected boolean showUpdateResult() {
		
		if (mMediaServiceInterface != null) {
			try {
				int link_type  = getLinkType(); 
				int new_count  = mMediaServiceInterface.getUpdatedLinkCount(link_type + UpdateService.NEW_LINK);
				int dead_count = mMediaServiceInterface.getUpdatedLinkCount(link_type + UpdateService.DEAD_LINK);
				
				if (new_count + dead_count > 0) {
					
					if (mUpdateResultDialog != null && mUpdateResultDialog.isShowing()) {
						mUpdateResultDialog.dismiss();
						mUpdateResultDialog = null;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					//builder.setIcon(Res.drawable.icon_radio);
					//builder.setTitle(Res.string.radio);
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
					
					mUpdateResultDialog = builder.show();
					mUpdateResultDialog.setCanceledOnTouchOutside(true);
					mUpdateResultDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							mUpdateResultDialog = null;
						}
					});
					
					return true;
				}
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	//--------------------------------------------------------------------------
	class RemoveDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

		private final RecordBase mRecord;
		AlertDialog mDialog;
		
		//----------------------------------------------------------------------
		RemoveDialog(RecordBase record) {
			mRecord = record;
		}
		
		//----------------------------------------------------------------------
		@Override
		public void onClick(DialogInterface dialog, int which) {
			getRecordsManager().remove(mRecord);
			onRecordListChanged();
		}

		//----------------------------------------------------------------------
		void dismiss() {
			if (mDialog != null) {
				mDialog.dismiss();
			}
		}

		//----------------------------------------------------------------------
		@Override
		public void onDismiss(DialogInterface dialog) {
			mRemoveDialog = null;
		}
	}
	
	//--------------------------------------------------------------------------
	protected void RemoveRecord(RecordBase record) {
		
		if (record != null) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			mRemoveDialog = new RemoveDialog(record); 
			
			String url = record.getUrl();
			String description = record.getDescription();

			dialog.setIcon(getDialogIconId());
			dialog.setTitle(getDialogTitleId());
			dialog.setMessage(description == null || description.length() == 0 
					? getString(Res.string.remove_question, url) 
					: getString(Res.string.remove_question2, record.getDescription(), url));

			dialog.setPositiveButton(Res.string.yes, mRemoveDialog);
			dialog.setNegativeButton(Res.string.no, null);
			dialog.setCancelable(true);

			mRemoveDialog.mDialog = dialog.show();
			mRemoveDialog.mDialog.setOnDismissListener(mRemoveDialog);
		}
	}
	
	//--------------------------------------------------------------------------
	private class TryConnectListener implements DialogInterface.OnClickListener {

		private final RecordBase mRecord;
		
		TryConnectListener(RecordBase record) {
			mRecord = record;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			PerformStart(mRecord);
		}
	}
	
	//--------------------------------------------------------------------------
	private class RemoveThisListener implements DialogInterface.OnClickListener {

		private final RecordBase mRecord;
		
		RemoveThisListener(RecordBase record) {
			mRecord = record;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			mRecord.getRecordsManager().remove(mRecord);
		}
	}
	
	//--------------------------------------------------------------------------	
	protected void showDeadLinkDialog(RecordBase record) {
		
		if (mDeadLinkDialog != null && mDeadLinkDialog.isShowing())
			mDeadLinkDialog.dismiss();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(getDialogIconId());
		builder.setTitle(getDialogTitleId());
		builder.setMessage(Res.string.msg_remove_marked_link);
		builder.setPositiveButton(Res.string.try_connect, new TryConnectListener(record));
		builder.setNeutralButton(Res.string.remove_this, new RemoveThisListener(record));
		builder.setNegativeButton(Res.string.remove_all_marked, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				RemoveAllDeadRecords();
			}
		});
		
		mDeadLinkDialog = builder.show();
		mDeadLinkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				mDeadLinkDialog = null;
			}
		});
	}
	
	//--------------------------------------------------------------------------
	protected abstract void PerformStart(RecordBase record);
	protected abstract void RemoveAllDeadRecords();
	protected abstract int getDialogIconId();
	protected abstract int getDialogTitleId();
	
	//--------------------------------------------------------------------------
	/*protected void RemoveAllDeadRecords() {
		RecordsManager manager = getRecordsManager();
		final Vector<RadioRecord> toRemoveRecords = new Vector<RadioRecord>();
		for (RecordBase record : manager.getAllRecords()) {
			if (record.isDeadLink()) {
				toRemoveRecords.add(record);
			}
		}
		if (toRemoveRecords.size() > 0) {
			for (RadioRecord record : toRemoveRecords) {
				mRecordsManager.remove(record);
			}
		}
	}*/
	
	//--------------------------------------------------------------------------
	protected void showContextMenu() {
		switch (getCurrentPage()) {
		case FourTabActivity.ALL_LINKS_PAGE:
		case FourTabActivity.SEARCH_COMMAND:
			openContextMenu (mAllLinksList);
			break;
			
		case FourTabActivity.FAVORITES_PAGE:
			openContextMenu (mFavoritesList);
			break;
			
		case FourTabActivity.HISTORY_PAGE:
			openContextMenu (mHistoryList);
			break;
		}
	}
	
	//--------------------------------------------------------------------------
	protected class FavoriteIconListener implements View.OnClickListener {

		private final RecordBase mRecord;
		
		public FavoriteIconListener (RecordBase record) {
			mRecord = record;
		}
		
		@Override
		public void onClick(View v) {
			mRecord.setFavorite(!mRecord.isFavorite());
			getRecordsManager().updateRecord(mRecord);
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onFastTreeRightIconLongClick(FastTreeItem element, int number) {
		onFastTreeRightIconClick(element, number);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onFastTreeRightIconClick(FastTreeItem element, int number) {
		if (!(element instanceof RecordBase) || number != 0)
			return;
		
		if (element.isPaid()) {
			showPayedGroupDialog();
			return;
		}
		
		RecordBase record = (RecordBase)element;
		setCurrentItem(record);
		record.setFavorite(!record.isFavorite());
		getRecordsManager().updateRecord(record);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onFastTreeItemLongClick(FastTreeItem element) {
		if (!(element instanceof RecordBase))
			return;
		
		if (element.isPaid()) {
			showPayedGroupDialog();
			return;
		}
		
		setCurrentItem((RecordBase)element);
		showContextMenu();
	}

	//--------------------------------------------------------------------------
	@Override
	public void onFastTreeItemClick(FastTreeItem element) {
		if (!(element instanceof RecordBase))
			return;
		
		if (element.isPaid()) {
			showPayedGroupDialog();
			return;
		}
		
		setCurrentItem((RecordBase)element);
		String linkClickAction = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_links_list_click_action_key), "ShowMenu");
		if (linkClickAction.equalsIgnoreCase("ShowMenu")) {
			showContextMenu();
		} else if (linkClickAction.equalsIgnoreCase("StartContent")) {
			StartCommand();
		}
	}
	
	//--------------------------------------------------------------------------
	protected abstract void setCurrentItem(RecordBase record);
	protected abstract void StartCommand();
	
	//--------------------------------------------------------------------------
	protected void showPayedGroupDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(getDialogIconId());
		builder.setTitle(getDialogTitleId());
		builder.setMessage(Res.string.free_version_limitation_msg);
		builder.setPositiveButton(Res.string.install, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.psa.android.pro.media")));
			}
		});
		
		builder.setNegativeButton(Res.string.cancel, null);
		builder.show();
	}
}
