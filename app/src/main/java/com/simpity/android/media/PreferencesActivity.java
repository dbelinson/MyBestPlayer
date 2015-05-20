package com.simpity.android.media;

import java.text.DateFormat;
import java.util.Calendar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.simpity.android.media.Res;
import com.simpity.android.media.storage.BackupListener;
import com.simpity.android.media.storage.BackupRestore;
import com.simpity.android.media.storage.BackupWriter;

public class PreferencesActivity extends PreferenceActivity implements 
		BackupListener, DialogInterface.OnClickListener {

	private final Handler mHandler = new Handler();
	
	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setTitle(getString(Res.string.app_name)+" Preferences");
		addPreferencesFromResource(Res.xml.preferences);
		
		Preference pref = findPreference (getString(Res.string.pref_backup_now_key));
		if (pref != null) {
			
			setBackupTime(pref);
			
			pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					BackupWriter.startBackup(PreferencesActivity.this, PreferencesActivity.this);
					return true;
				}
			});
		}
		
		pref = findPreference (getString(Res.string.pref_backup_restore_key));
		if (pref != null) {
			
			if (!BackupWriter.isBackupExists()) {
				pref.setEnabled(false);
			}
			
			pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(PreferencesActivity.this);
					dialog.setMessage(Res.string.restore_question);
					dialog.setPositiveButton(Res.string.yes, PreferencesActivity.this);
					dialog.setNegativeButton(Res.string.no, null);
					dialog.setCancelable(true);
					dialog.show();

					return true;
				}
			});
		}
	}

	//--------------------------------------------------------------------------
	private WaitDialog mWaitDialog = null;
	
	@Override
	public void StorageBackupError(String error) {
		final String message = error;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
					mWaitDialog = null;
				}
				
				AlertDialog.Builder dialog = new AlertDialog.Builder(PreferencesActivity.this);
				
				dialog.setMessage(message);
				dialog.setCancelable(true);
				dialog.setNeutralButton(android.R.string.ok, null);
				dialog.show();
			}
		});
	}

	//--------------------------------------------------------------------------
	@Override
	public void StorageBackupStarted() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
				}
				
				mWaitDialog = WaitDialog.show(PreferencesActivity.this, Res.string.pref_backup_category);
			}
		});
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void StorageBackupFinished() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
				}
				
				Preference pref = findPreference (getString(Res.string.pref_backup_now_key));
				if (pref != null) {
					setBackupTime(pref);
					
					pref = findPreference (getString(Res.string.pref_backup_restore_key));
					if (pref != null) {
						pref.setEnabled(true);
					}
					
					getListView().invalidateViews();
				}
			}
		});
	}
	
	//--------------------------------------------------------------------------
	private void setBackupTime(Preference pref) {
		long time = BackupWriter.getLastBackupTime(this);
		if (time > 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(time);
			
			pref.setSummary(getString(Res.string.last_backup) + ' ' + 
					DateFormat.getDateTimeInstance().format(calendar.getTime()));
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void StorageRestoreStarted() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
				}
				
				mWaitDialog = WaitDialog.show(PreferencesActivity.this, Res.string.pref_backup_restore);
			}
		});
	}

	//--------------------------------------------------------------------------
	@Override
	public void StorageRestoreFinished() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
				}
			}
		});
	}

	//--------------------------------------------------------------------------
	@Override
	public void onClick(DialogInterface dialog, int which) {
		BackupRestore.start(this, this);
	}
}
