package com.simpity.android.media;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.IMediaServiceInterface;
import com.simpity.android.media.Res;
import com.simpity.android.media.camera.JpegCameraListActivity;
import com.simpity.android.media.dialogs.WhatsNewDlg;
import com.simpity.android.media.radio.RadioSelectActivity;
import com.simpity.android.media.statistic.StatCollector;
import com.simpity.android.media.storage.BackupListener;
import com.simpity.android.media.storage.BackupRestore;
import com.simpity.android.media.storage.BackupWriter;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.video.RtspListActivity;
//import com.simpity.android.slideshow.R;
//import com.simpity.android.slideshow.SlideViewerActivity.PostAdRequest;

public class StreamMediaActivity extends Activity implements Runnable, BackupListener {

	public final static String APPLICATION_VERSION_CODE = "Application version code";
	public final static String APPLICATION_VERSION_STRING = "Application version string";
	public final static String CURRENT_LINKS_DB_VERSION = "CURRENT_LINKS_DB_VERSION";
	public final static String LAST_UPDATE_LINKS_DB_DATE = "LAST_UPDATE_LINKS_DB_DATE"; 
	
	final static public int CANCEL  = 0x1000;
	final static public int SUCCESS = 0x1001;
	final static public int RESULT_SOCKET_ERROR = 0x1002;

	private final Handler mHandler = new Handler();
	@SuppressWarnings("unused")
	private IMediaServiceInterface mUpdateServiceInterface;
	
	private Ad mAdView;
	
	//--------------------------------------------------------------------------
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mUpdateServiceInterface = IMediaServiceInterface.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mUpdateServiceInterface = null;
		}
	};

	//--------------------------------------------------------------------------
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(Res.layout.main);
        
		bindService(new Intent(this, MediaService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		initAdView();
        initListeners();
        
        new RecordsManager(this);

        mHandler.post(this);
	}
	
    //--------------------------------------------------------------------------
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(Res.layout.main);
    	initAdView();
    	initListeners();
    };
    
    //--------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
		unbindService(mConnection);
		
		if (mAdView != null) {
			mAdView.destroy();
		}

    	super.onDestroy();
    }
    
    //--------------------------------------------------------------------------
	@Override
	public void run() {
		
		/*SharedPreferences preferences = getPreferences(Activity.MODE_PRIVATE);
		int current_appl_code = 0;
		int appl_version_code = preferences.getInt(APPLICATION_VERSION_CODE, 0);
		try {
			current_appl_code = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		if (appl_version_code < current_appl_code) { // update
		
			preferences.edit().putInt(APPLICATION_VERSION_CODE, current_appl_code).
			putString(APPLICATION_VERSION_STRING, getString(Res.string.versionName)).commit();
			
			//TODO: remove hardcoded condition, use APPLICATION_VERSION_STRING to define condition
			if (appl_version_code < 18) {
				WhatsNewDlg.show(this, Res.raw.whats_new, Res.string.whats_new_title, true,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								WhatsNewDlg.show(StreamMediaActivity.this, Res.raw.msg_from_dev,
										Res.string.msg_from_dev_title, false, null);
							}
						});
			}
		}

		StatCollector.collectAppLaunchCount(this);

		if (appl_version_code == 0 && Storage.isEmpty(this)) {
			
			BackupRestore.start(this, this);
			
		} else {
			
			BackupWriter.backupBySchedule(this);
		}*/
		StatCollector.collectAppLaunchCount(this);
	}

    //--------------------------------------------------------------------------
    private void initListeners() {
    	
    	findViewById(Res.id.rtsp_video_shape).setOnClickListener(new Button.OnClickListener() {
    		@Override
    		public void onClick(View arg0) {
				startActivity(new Intent(StreamMediaActivity.this,
						RtspListActivity.class));
			}
		});

        findViewById(Res.id.jpeg_camera_shape).setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View arg0) {
				startActivity(new Intent(StreamMediaActivity.this,
						JpegCameraListActivity.class));
			}
		});

	/*	findViewById(Res.id.mjpeg_camera_shape).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(StreamMediaActivity.this,
						MJpegCameraActivity.class));
			}
		});*/

		findViewById(Res.id.local_video_shape).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(StreamMediaActivity.this,
						LocalVideoActivity.class));
			}
		});

		((ImageView)findViewById(Res.id.simpity_logo)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StreamMediaActivity.this, AboutActivity.class));
			}
		});

		findViewById(Res.id.radio_shape).setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(StreamMediaActivity.this, RadioSelectActivity.class));
			}
		});
    }
    
    private void initAdView()
    {
    	if(mAdView != null)
    		mAdView.destroy();
 
        mAdView = new Ad(this);
    }

	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		DefaultMenu.create(menu);
		return true;
	}

	//--------------------------------------------------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultMenu.onItemSelected(this, item);
	}

	//--------------------------------------------------------------------------
	private WaitDialog mWaitDialog;
	
	@Override
	public void StorageRestoreStarted() {
		
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mWaitDialog = WaitDialog.show(
						StreamMediaActivity.this, Res.string.pref_backup_restore);
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
					mWaitDialog = null;
				}
			}
		});
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void StorageBackupStarted() {
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void StorageBackupFinished() {
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void StorageBackupError(String error) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mWaitDialog != null) {
					mWaitDialog.close();
					mWaitDialog = null;
				}
			}
		});
	}
}
