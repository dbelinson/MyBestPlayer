package com.simpity.android.media.player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.dialogs.DownloadDialog;
import com.simpity.android.media.dialogs.ShareLinkDialog;
import com.simpity.android.media.dialogs.WaitDialog;
import com.simpity.android.media.statistic.Session;
import com.simpity.android.media.statistic.StatCollector;
import com.simpity.android.media.utils.Command;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.Downloader;
import com.simpity.android.media.utils.Message;
import com.simpity.android.media.utils.Utilities;
import com.simpity.android.media.video.RtspRecordsManager;

public class PlayerActivity extends Activity implements
		MediaPlayer.OnCompletionListener,
		MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener,
		MediaPlayer.OnSeekCompleteListener,
		MediaPlayer.OnVideoSizeChangedListener,
		SurfaceHolder.Callback,
		SeekBar.OnSeekBarChangeListener,
		MediaScannerConnectionClient,
		OnBufferingUpdateListener {

	private final static String TAG = "PlayerActivity";

	public static final String URL_DATA = "URL";
	public static final String DESCRIPTION_DATA = "DESCRIPTION";
	public static final String SESSION_ID = "SESSION";

	private static final String POSITION_KEY = "POSITION";

	private static final int CONTROLS_AUTO_HIDE_TIME = 5000;
	
	private static final int SEEKBAR_UPDATE_PERIOD = 1000;
	private static final int PLAYER_SCROLL_STEP = 10000;
	public static final String IS_SUPRESS_NAVIGATION = "SUPRESS_NAVIGATION";
	public static final String IS_LINK_TO_SHARE = "Is Link to share";
	public static final String DO_NOT_SHOW_DOWNLOAD_QUESTION = "Is show download question";

	private MediaPlayer mPlayer;
	private Handler mHandler = new Handler();

	private WaitDialog mWaitDialog = null;
	private PowerManager.WakeLock mWakeLock = null;
	//private ScaleDialog mScaleDialog = null;
	private String mSession;

	private MediaScannerConnection mMediaScanner = null;
	private Vector<File> mFilesToAddToMedia = new Vector<File>();
	private int mErrorCount = 0;

	private boolean mShouldStartPlayback = true;
	private SurfaceHolder mHolder = null;
	private boolean mIsActivityPaused = false;
	private boolean mIsPlayerPrepared = false;
	private boolean mIsSeeckableStream = true;
	private DownloadDialog mDownloadDialog = null;
	private Dialog mAspectRatioDlg = null;
	private Downloader mDownloader;
	private SharedPreferences mPrefs = null;
	private Thread mKillPlayerThread = null;

	private SurfaceView mSurfaceView;
	private SeekBar mSeekBar;
	private ImageButton mPlayButton, mToStartButton, mRewindButton, 
			mForwardButton, mAspectRatioButton;
	private View mRootView, mControls;
	private TextView mTitleView;
	private PlayerLayout mPlayerLayout;
	
	private boolean mPenDown = false;
	private long mLastPenUp;
	private boolean mIsLinkToSave = false;
	private boolean mIsLinkToShare = false;
	private boolean mIsErrorOccured = false;
	private int mPrevOrientation;
	private RadioGroup mAspectRation_RadioGroup = null;
	
	private Session mStatSession = null;
	
	private Ad mAdView;
	
	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		mPrevOrientation = getResources().getConfiguration().orientation;
		
		setContentView(Res.layout.player_view);
		
		int orientation = getWindowManager().getDefaultDisplay().getOrientation();

		if (savedInstanceState != null)
			mShouldStartPlayback = false;

		mWaitDialog = new WaitDialog(this);
		mWaitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

		Intent intent = getIntent();		
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri intent_data = intent.getData();
			if (intent_data != null) {
				String path = intent_data.getPath();
				if (path != null && new File(path).exists())
					getIntent().putExtra(URL_DATA, path);
				else {
					getIntent().putExtra(URL_DATA, intent_data.toString());
					mIsLinkToSave = true;
				}
			}
		}

		mRootView = findViewById(Res.id.PlayerRoot);
		mControls = findViewById(Res.id.PlayerControls);
		mTitleView = (TextView)findViewById(Res.id.PlayerTitle);
		
		mPlayerLayout = (PlayerLayout)findViewById(Res.id.PlayerLayout);
		
		mAspectRatioButton = (ImageButton) findViewById(Res.id.PlayerAspectRatio);
		
		refreshAspectState();
		
		mPlayerLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SurfaceClick();
			}
		});
		
		mSession = getIntent().getStringExtra(SESSION_ID);
		mSurfaceView = (SurfaceView) findViewById(Res.id.PlayerSurface);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setSizeFromLayout();
		
		mSurfaceView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SurfaceClick();
			}
		});
		
		mSurfaceView.setOnLongClickListener(new SurfaceLongClickListener());

		String description = getIntent().getStringExtra(DESCRIPTION_DATA);
		if (description == null || description.length() == 0) {
			description = getIntent().getStringExtra(URL_DATA);
		}

		if (description != null) {
			mTitleView.setText(description);
		}

		mToStartButton = (ImageButton) findViewById(Res.id.PlayerToStart);
		if (mToStartButton != null) {
			mToStartButton.setOnClickListener(MOVE_TO_START);
		}

		mRewindButton = (ImageButton) findViewById(Res.id.PlayerRewind);
		if (mRewindButton != null) {
			mRewindButton.setOnClickListener(REWIND);
		}

		mForwardButton = (ImageButton) findViewById(Res.id.PlayerForward);
		if (mForwardButton != null) {
			mForwardButton.setOnClickListener(FORWARD);
		}

		mPlayButton = (ImageButton) findViewById(Res.id.PlayerPlay);
		if (mPlayButton != null) {
			mPlayButton.setOnClickListener(PLAY);
		}
		
		
		if (mAspectRatioButton != null) {
			mAspectRatioButton.setOnClickListener(ASPECT_RATIO);
		}
		
		mSeekBar = (SeekBar) findViewById(Res.id.PlayerSeekbar);
		mSeekBar.setOnSeekBarChangeListener(this);

		setResult(StreamMediaActivity.CANCEL, getIntent());
		//setResult(StreamMediaActivity.SUCCESS, getIntent());
		
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "MediaPlayerActivity");
		mWakeLock.acquire();

		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		String session = prefs.getString(SESSION_ID, null);
		if (session == null || mSession == null || !session.equals(mSession)) {
			SharedPreferences.Editor edit = prefs.edit();
			edit.clear();
			edit.commit();
		}

		enableControls(false);
		mMediaScanner = new MediaScannerConnection(PlayerActivity.this,
				PlayerActivity.this);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		mAdView = new Ad(this);
		
		//onOrientationChanged(getResources().getConfiguration().orientation);
		onOrientationChanged(orientation);
		
		//TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		//tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");

		mIsActivityPaused = false;
		if (mControls.getVisibility() != View.VISIBLE) {
			SurfaceClick();
		} else {
			mTitleView.bringToFront();
			mControls.bringToFront();
		}

		if (mPlayer == null || !mIsPlayerPrepared) {
			mWaitDialog.show(true);
		}
		/*
		 * else { if (isPlayerPrepared && mPlayer.getDuration()<=0) { playPause();
		 * } }
		 */
		super.onResume();
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		mIsActivityPaused = true;
		
		if (!isFinishing()) {
			if (mPlayer != null && mIsPlayerPrepared) {
				int position = mPlayer.getCurrentPosition();
				if (mPlayer.isPlaying()) {
					position = playPause();
				}
				
				if (mPlayer.getDuration() <= 0) {
					position = -1;
				}
				
				if (position > 0 && mSession != null) {
					SharedPreferences.Editor edit = getPreferences(
							Activity.MODE_PRIVATE).edit();
					edit.clear();
					edit.putString(SESSION_ID, mSession);
					edit.putInt(POSITION_KEY, position);
					edit.commit();
				}
			}
			
			if (mWaitDialog != null && mWaitDialog.isShowing()) {
				mWaitDialog.dismiss();
			}
		}
		
		mShouldStartPlayback = false;
		super.onPause();
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		if (isFinishing() && mDownloadDialog != null && mDownloadDialog.isShowing()) {
			mDownloadDialog.dismiss();
		}
		if (mDownloader != null) {
			mDownloader.StopDownload();
		}
		if (mPlayer != null) {
			mPlayer.setOnPreparedListener(null);
			mPlayer.setOnErrorListener(null);
		}
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
		if (mMediaScanner.isConnected()) {
			mMediaScanner.disconnect();
		}
		
		if(mAdView != null)
    		mAdView.destroy();
		
		//TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		//tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		super.onDestroy();
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
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Command.SETTINGS) {
			refreshAspectState();
		}
	}

	//--------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	//--------------------------------------------------------------------------
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");

		mHandler.removeCallbacks(mSeekUpdater);
		mKillPlayerThread = PlayerKiller.start(mPlayer);
		mPlayer = null;
		mIsPlayerPrepared = false;
	}

	//--------------------------------------------------------------------------
	private static class PlayerKiller implements Runnable {
		
		private final MediaPlayer mPlayer;
		
		private PlayerKiller(MediaPlayer player) {
			mPlayer = player;
		}

		@Override
		public void run()
		{
			if(mPlayer != null)
			{
				mPlayer.release();
			}
		}
		
		static Thread start(MediaPlayer player) {
			Thread thread = new Thread(new PlayerKiller(player));
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setDaemon(true);
			thread.start();
			return thread;
		}
	}
	
	//--------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	//--------------------------------------------------------------------------
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	//--------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	//--------------------------------------------------------------------------
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		if (mWaitDialog != null && !mWaitDialog.isShowing())
			mWaitDialog.show(true);
		
		mIsPlayerPrepared = false;
		
		if (mKillPlayerThread != null && mKillPlayerThread.isAlive()) {
			try {
				mKillPlayerThread.join();
				if (isFinishing())
					return;
				
			} catch (InterruptedException e1) {
				
				e1.printStackTrace();
				if (isFinishing())
					return;
			}
		}
		
		mKillPlayerThread = null;
		
		if (isFinishing())
			return;
		
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
		}
		
		mPlayer.reset();	//!
		mPlayer.setDisplay(holder);
		mPlayer.setOnCompletionListener (PlayerActivity.this);
		mPlayer.setOnSeekCompleteListener (PlayerActivity.this);
		mPlayer.setOnErrorListener (PlayerActivity.this);
		mPlayer.setOnBufferingUpdateListener (PlayerActivity.this);
		mPlayer.setOnPreparedListener (PlayerActivity.this);
		mPlayer.setOnVideoSizeChangedListener (PlayerActivity.this);
		
		String path = getIntent().getStringExtra(URL_DATA);
		try {
			mPlayer.setDataSource(path);
			if (path.startsWith(Environment.getExternalStorageDirectory().getPath())) {
				mSeekBar.setSecondaryProgress(mSeekBar.getMax());
			}
			mPlayer.prepareAsync();
			mLastPenUp = System.currentTimeMillis();
			
		} catch (Exception e) {
			
			processExeption(e, path);
		}
	}

	//--------------------------------------------------------------------------
	private void processExeption(Exception e, String path) {
		if (mWaitDialog != null) {
			mWaitDialog.dismiss();
		}

		Log.d(TAG, e.toString());
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}

		setResult(StreamMediaActivity.CANCEL, getIntent());

		Message.show(this, Res.string.error,
				getResources().getString(Res.string.open_media_error, path),
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						PlayerActivity.this.finish();
					}
				});
	}

	//--------------------------------------------------------------------------
	// from MediaPlayer.OnErrorListener
	//--------------------------------------------------------------------------
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		
		if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
			if (mWaitDialog != null && mWaitDialog.isShowing())
				mWaitDialog.dismiss();
			if (!mPrefs.getBoolean(DO_NOT_SHOW_DOWNLOAD_QUESTION, false)) {
				mDownloadDialog = new DownloadDialog(this);
				mDownloadDialog.setIcon(Res.drawable.icon_video);
				mDownloadDialog.setTitle(getTitle());
				
				mDownloadDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(Res.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mHandler.post(new Runnable() {
							
							@Override
							public void run() {
								mPrefs.edit().putBoolean(DO_NOT_SHOW_DOWNLOAD_QUESTION, mDownloadDialog.GetDoNotShowQuestionCheckedState()).commit();
								StartDownloader();
							}
						});
					}
				});
				
				mDownloadDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(Res.string.no), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
				
				mDownloadDialog.show();
				
			} else {
				
				StartDownloader();
			}
			
			return true;
		}
		
		
		String message;
		if (extra == -10 && mErrorCount < 3) {
			mErrorCount++;
			SharedPreferences.Editor edit = getPreferences(Activity.MODE_PRIVATE).edit();
			edit.clear();
			edit.putInt(POSITION_KEY, 0);
			edit.commit();
			try {
				mPlayer.reset();
				mPlayer.setOnCompletionListener(this);
				mPlayer.setOnSeekCompleteListener(this);
				mPlayer.setOnErrorListener(this);
				mPlayer.setOnPreparedListener(this);
				mPlayer.setDataSource(getIntent().getStringExtra(URL_DATA));
				mPlayer.prepareAsync();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mWaitDialog != null && mWaitDialog.isShowing())
			mWaitDialog.dismiss();

		if (extra == -11)// camera settings is changed, should try to reconnect
		{
			PlayerActivity.this.onStop();

			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
			dlg.setMessage(Res.string.video_connection_lost);
			dlg.setPositiveButton(Res.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							surfaceCreated(mHolder);
						}
					});
			dlg.setNegativeButton(Res.string.no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							PlayerActivity.this.setResult(
									StreamMediaActivity.CANCEL, getIntent());
							PlayerActivity.this.finish();
						}
					});
			dlg.setCancelable(true);
			dlg.show();

			return true;
		}

		mPlayer.setOnErrorListener(null);
		/*
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			message = "Error #" + extra + ". Media server died.";
		} else if (extra == -4) { // not supported
			message = "Error #" + extra + ". Not supported";
		} else {
			message = "Error #" + extra + ". Unspecified media player error.";
		}
		*/
		message = getString(Res.string.video_cant_be_played);
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
		mIsErrorOccured = true;
		setResult(StreamMediaActivity.CANCEL, getIntent());

		Message.show(this, Res.string.error, message,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						PlayerActivity.this.finish();
					}
				});
		if (mStatSession != null) {
			mStatSession.cancelSession();
		}
		return true;
	}
	
	//--------------------------------------------------------------------------
	private void StartDownloader() {
		if (mDownloader != null)
			mDownloader.StopDownload();
		
		UpdateDownloadingInfo(new Downloader.OnRetriveDownloadInfoListener.DownloadProgressInfo());
		mDownloader = new Downloader(PlayerActivity.this, getIntent().getStringExtra(URL_DATA));
		mDownloader.StartDownload(new Downloader.OnRetriveDownloadInfoListener() {
					
			@Override
			public void DownloadInfoRetrived(final DownloadProgressInfo info) {
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						UpdateDownloadingInfo(info);
					}
				});
			}
		});
	}
	
	//--------------------------------------------------------------------------
	private void UpdateDownloadingInfo(Downloader.OnRetriveDownloadInfoListener.DownloadProgressInfo info) {
		
		if (info.isErrorOccured) {
			
			try {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(Res.drawable.icon_video);
				builder.setTitle(getTitle());
				builder.setCancelable(true);
				builder.setMessage(info.error_message);
				builder.setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						finish();
					}
				});
				
				builder.setPositiveButton(getString(Res.string.ok), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				});
				
				builder.show();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		} else if (!info.isDownloadCompleted) {
			
			View v = findViewById(Res.id.DownloadInfoContainer);
			if (v != null) {
				v.setVisibility(View.VISIBLE);
				v.bringToFront();
				
				mSeekBar.setMax(info.target_file_size);
				mSeekBar.setSecondaryProgress(info.downloaded_size);
				
				TextView txtView = (TextView) findViewById(Res.id.DownloadingFileName);
				txtView.setText(info.file_name);
				txtView.invalidate();
				
				txtView = (TextView) findViewById(Res.id.DownloadingFileSize);
				txtView.setText(String.valueOf(info.target_file_size));
				txtView.invalidate();
				
				txtView = (TextView) findViewById(Res.id.DownloadedSize);
				txtView.setText(String.valueOf(info.downloaded_size));
				txtView.invalidate();
			}
			
		} else {
			
			mDownloader = null;
			View v = findViewById(Res.id.DownloadInfoContainer);
			if (v != null) {
				v.setVisibility(View.INVISIBLE);
			}
			
			getIntent().putExtra(URL_DATA, info.targetFile.getAbsolutePath());
			if (mHolder != null)
				surfaceCreated(mHolder);
		}
	}

	// --------------------------------------------------------------------------
	// from MediaPlayer.OnPreparedListener
	//--------------------------------------------------------------------------
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "onPrepared");
		
		setResult(StreamMediaActivity.SUCCESS, getIntent());
		mStatSession = StatCollector.startCollect(PlayerActivity.this, StatCollector.TYPE_VIDEO);
		try {
			setTime(Res.id.PlayerTotalTime, mp.getDuration());
		} catch (IllegalStateException e) {
			setTime(Res.id.PlayerTotalTime, 1);
		}
		//updateSurfaceSize(mScaleDialog.GetIsUseNative(), mScaleDialog.GetScale());

		SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
		int position = pref.getInt(POSITION_KEY, 0);

		int secProgress = mSeekBar.getSecondaryProgress();
		int maxProgress = (mp.getDuration() > 0) ? mp.getDuration() : 1;
		if (maxProgress != mSeekBar.getMax()) {
			mSeekBar.setMax(maxProgress);
			if (secProgress <= 100)
				mSeekBar.setSecondaryProgress(secProgress * mSeekBar.getMax() / 100);
			
			mSeekBar.setProgress(position);
		}
		mIsSeeckableStream = mp.getDuration() > 0;

		mSeekBar.postInvalidate();

		if (position > 0) {
			// enableControls(false);
			mp.seekTo(position);
		} else {
			mIsPlayerPrepared = true;
			if (getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				enableControls(false);
			} else {
				enableControls(true);
			}
		}

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					if (!mIsErrorOccured)
						getIntent().putExtra(IS_LINK_TO_SHARE, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 20000);

		if (mIsActivityPaused) {
			if (mPlayer.getDuration() <= 0)
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						mPlayer.stop();
					}
				});
			return;
		}

		if (mShouldStartPlayback || mPlayer.getDuration() <= 0) {
			mPlayer.start();
			mPlayButton.setImageResource(Res.drawable.pause); // android.R.drawable.ic_media_pause);
			mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
			mPlayButton.invalidate();
			if (mWaitDialog != null) {
				mWaitDialog.dismiss();
			}
		}
		if (position <= 0)
			if (mWaitDialog != null) {
				mWaitDialog.dismiss();
			}
		
		startControlsTimer();
		if (mIsLinkToSave) {
			// save link to history
			//RtspUrlListAdapter adapter = new RtspUrlListAdapter(this);
			mIsLinkToSave = false;
			String url = getIntent().getStringExtra(URL_DATA);
			
			RtspRecordsManager manager = new RtspRecordsManager(this, null);
			mIsLinkToShare = manager.add("", url, "Other", -1, false, Calendar.getInstance().getTimeInMillis(), false, false) & ShareLinkDialog.CheckLinkForSharing(url);
			//isLinkToShare = adapter.add("", getIntent().getData().toString(), true, true);
			manager.destroy();
		}
	}

	//--------------------------------------------------------------------------
	// from MediaPlayer.OnCompletionListener
	//--------------------------------------------------------------------------
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (mStatSession != null) {
			mStatSession.endSession();
		}
		
		if (mPlayer == null || mPlayer != mp)
			return;
			
		setTime(Res.id.PlayerCurrentTime, mPlayer.getDuration());
		mPlayButton.setImageResource(Res.drawable.play);
		mPlayButton.setEnabled(true);

		mKillPlayerThread = PlayerKiller.start(mPlayer);
		mPlayer = null;

		if (mToStartButton.isPressed())
			mToStartButton.setPressed(false);

		mToStartButton.setEnabled(false);

		if (mRewindButton.isPressed())
			mRewindButton.setPressed(false);

		mRewindButton.setEnabled(false);

		if (mForwardButton.isPressed())
			mForwardButton.setPressed(false);

		mForwardButton.setEnabled(false);

		mSeekBar.setEnabled(false);

		if (mControls.getVisibility() != View.VISIBLE) {
			SurfaceClick();
		} else {
			mTitleView.bringToFront();
			mControls.bringToFront();
		}
		if (!mIsErrorOccured) {
			getIntent().putExtra(IS_LINK_TO_SHARE, true);
		}
		
		if (mIsLinkToShare && !mIsErrorOccured 
				&& ShareLinkDialog.CheckLinkForSharing(getIntent().getStringExtra(URL_DATA))) {
			
			ShareLinkDialog.share(this, Res.drawable.icon_video,
					getIntent().getStringExtra(URL_DATA), 
					ShareLinkDialog.INTERNET_VIDEO, "-", "", "-");
		}
	}

	//--------------------------------------------------------------------------
	private void setSeekBar(int time) {
		int duration = mPlayer.getDuration();
		int max = mSeekBar.getMax();
		int pos;
		
		if (duration != max) {
			pos = (int)((long)time * (long)max / (long)duration);
		} else {
			pos = time;
		}
		
		mSeekBar.setProgress(pos);
	}
	
	//--------------------------------------------------------------------------
	protected void moveToStart() {
		if (mPlayer == null)
			return;

		enableControls(false);
		mPlayer.seekTo(0);
		mSeekBar.setProgress(0);
	}

	//--------------------------------------------------------------------------
	protected void moveToEnd() {
		if (mPlayer == null)
			return;

		mPlayer.pause();
		int pos = mPlayer.getDuration();

		if (mPlayer.getCurrentPosition() >= pos) {
			return;
		}

		enableControls(false);
		mPlayer.seekTo(pos);		
		mSeekBar.setProgress(mSeekBar.getMax());
	}

	//--------------------------------------------------------------------------
	protected void backward() {
		if (mPlayer == null)
			return;

		int pos = mPlayer.getCurrentPosition();

		if (pos > PLAYER_SCROLL_STEP)
			pos -= PLAYER_SCROLL_STEP;
		else
			pos = 0;

		enableControls(false);

		mPlayer.seekTo(pos);
		//mSeekBar.setProgress(pos); // TODO
		setSeekBar(pos);
	}

	//--------------------------------------------------------------------------
	protected void forward() {
		if (mPlayer == null)
			return;

		int current_pos = mPlayer.getCurrentPosition();
		int margin = mPlayer.getDuration() - 1000;

		if (current_pos > margin)
			return;

		int pos = Math.min(current_pos + PLAYER_SCROLL_STEP, margin);

		enableControls(false);

		mPlayer.seekTo(pos);
		//mSeekBar.setProgress(pos); // TODO
		setSeekBar(pos);
	}

	//--------------------------------------------------------------------------
	protected int playPause() {
		int pausePosition = -1;
		if (mPlayer == null) {
			SharedPreferences.Editor edit = getPreferences(Activity.MODE_PRIVATE).edit();
			edit.clear();
			edit.putString(SESSION_ID, mSession);
			edit.commit();

			surfaceCreated(mSurfaceView.getHolder());
			return -1;
		}

		if (mPlayer.isPlaying()) {
			if (mStatSession != null) {
				mStatSession.endSession();
			}
			
			if (mPlayer.getDuration() > 0) {
				Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						if (mPlayer != null) {
							try{
								mPlayer.pause();
								Log.d(TAG, "Player paused in thread");
							}catch (IllegalStateException e) {
								e.printStackTrace();
							}
						}
					}
				});
				th.setDaemon(true);
				th.start();
				pausePosition = mPlayer.getCurrentPosition();
				
			} else {
				
				pausePosition = mPlayer.getCurrentPosition();
				Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						if (mPlayer != null) {
							mPlayer.stop();
							Log.d(TAG, "Player stopped in thread");
						}	
					}
				});
				th.setDaemon(true);
				th.start();
				mSeekBar.setMax(100); // TODO
			}
			
			mPlayButton.setImageResource(Res.drawable.play);
			
		} else {
			
			pausePosition = -1;
			if (mPlayer.getDuration() > 0) {
				
				mPlayer.start();
				mStatSession = StatCollector.startCollect(PlayerActivity.this, StatCollector.TYPE_VIDEO);
				
			} else {
				
				mWaitDialog.show(true);
				surfaceCreated(mHolder);
				return -1;
			}
			mPlayButton.setImageResource(Res.drawable.pause);
			mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
		}

		//mPlayButton.invalidate();

		if (mWaitDialog != null) {
			mWaitDialog.dismiss();
		}
		return pausePosition;
	}

	//--------------------------------------------------------------------------
	/*protected String getMediaFolder() {
		File ImageDir = Environment.getExternalStorageDirectory();
		if (!ImageDir.canWrite()) { // Workaround for broken sdcard support on the device.
			ImageDir = new File("/sdcard/" + getString(Res.string.imagesFolder));
		} else {
			ImageDir = new File(ImageDir, getString(Res.string.imagesFolder));
		}
		
		// if (!ImageDir.exists())
		// ImageDir.mkdir();

		ImageDir = new File(ImageDir, getString(Res.string.app_name));
		if (!ImageDir.exists())
			ImageDir.mkdirs();

		return ImageDir.getAbsolutePath();
	}*/

	//--------------------------------------------------------------------------
	protected void GetSnapshot() {

		Bitmap bmp = Utilities.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight());
		if (bmp != null) {
			Canvas canvas = new Canvas(bmp);
			mSurfaceView.draw(canvas);
		} else {
			return;
		}

		String description = getIntent().getStringExtra(DESCRIPTION_DATA);
		
		File imageFile = Utilities.getNewSnapshotFile(description);
		if (imageFile == null)
			return;
		
		FileOutputStream out_stream = null;
		try {
			if (imageFile.createNewFile()) {
				out_stream = new FileOutputStream(imageFile);
				if (bmp.compress(CompressFormat.JPEG, 90, out_stream)) {
					out_stream.flush();
					out_stream.close();
					out_stream = null;
					
					if (mMediaScanner.isConnected()) {
						mMediaScanner.scanFile(imageFile.getAbsolutePath(), "image/jpeg");
					} else {						
						mFilesToAddToMedia.add(imageFile);						
						mMediaScanner.connect();
					}
				}
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			if (out_stream != null) {
				try {
					out_stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	private void updateFullscreenStatus(boolean bUseFullscreen) {
		if (bUseFullscreen) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		mRootView.requestLayout();
		mRootView.invalidate();
	}

	//--------------------------------------------------------------------------
	private class SurfaceLongClickListener implements View.OnLongClickListener {

		@Override
		public boolean onLongClick(View v) {
			
			mAspectRatioDlg = new Dialog(PlayerActivity.this);
			mAspectRatioDlg.setTitle("Aspect Ratio");
			mAspectRatioDlg.setCancelable(true);
			mAspectRatioDlg.setCanceledOnTouchOutside(true);
			
			View view = LayoutInflater.from(PlayerActivity.this).inflate(Res.layout.scale_dialog, null);
			mAspectRation_RadioGroup = (RadioGroup)view.findViewById(Res.id.radioGroup);
			
			switch(mPlayerLayout.getAspectRatio()) {
			case Original:
				mAspectRation_RadioGroup.check(Res.id.none);
				break;
				
			case FourToTree:
				mAspectRation_RadioGroup.check(Res.id.four_to_tree);
				break;
				
			case FiveToFour:
				mAspectRation_RadioGroup.check(Res.id.five_to_four);
				break;
				
			case SixteenToNine:
				mAspectRation_RadioGroup.check(Res.id.sixteen_to_nine);
				break;		
			}
			
			mAspectRation_RadioGroup.setOnCheckedChangeListener(new AspectRatioChangeListener(mAspectRatioDlg));
			
			mAspectRatioDlg.setContentView(view);
			mAspectRatioDlg.show();

			return true;
		}		
	}
	
	//--------------------------------------------------------------------------
	private class AspectRatioChangeListener implements 
			RadioGroup.OnCheckedChangeListener, Runnable {

		private Dialog mDialog;
		
		AspectRatioChangeListener(Dialog dialog) {
			mDialog = dialog;
			mHandler.postDelayed(this, 3000);
		}
		
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			switch (checkedId) {
			case Res.id.none:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.Original);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_default);
				break;

			case Res.id.four_to_tree:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.FourToTree);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_4_3);
				break;

			case Res.id.five_to_four:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.FiveToFour);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_5_4);
				break;

			case Res.id.sixteen_to_nine:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.SixteenToNine);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_16_9);
				break;

			default:
				return;
			}
		
			mPlayerLayout.requestLayout();
			mPlayerLayout.invalidate();
			run();
		}

		@Override
		public void run() {
			if (mDialog != null) {
				//mDialog.dismiss();
				try {
					mDialog.cancel();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				mDialog = null;
			}
		}		
	}
	
	//--------------------------------------------------------------------------
	private void SurfaceClick() {

		if (mControls.getVisibility() == View.VISIBLE) {
			hideControls();
		} else {
			showControls();
		}
	}
	
	//--------------------------------------------------------------------------
	private void hideControls() {
		
		try{
			if (mPlayer == null || !mPlayer.isPlaying())
				return;
		}
		catch (Exception e) {
			e.printStackTrace();
			startControlsTimer();
			return;
		}
		TranslateAnimation controls_animation = new TranslateAnimation (
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
	            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
		
		controls_animation.setDuration(300);
		controls_animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mControls.setVisibility(View.GONE);
			}
		});
		
		mControls.startAnimation(controls_animation);
		
		TranslateAnimation title_animation = new TranslateAnimation (
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
	            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, -1.0f);
		
		title_animation.setDuration(300);
		title_animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mTitleView.setVisibility(View.GONE);
			}
		});
		
		mTitleView.startAnimation(title_animation);
		
		updateFullscreenStatus(true);
	}
	
	//--------------------------------------------------------------------------
	private void showControls() {
		
		TranslateAnimation controls_animation = new TranslateAnimation (
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
	            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		
		controls_animation.setDuration(300);
		
		mControls.setVisibility(View.VISIBLE);
		mControls.startAnimation(controls_animation);
		
		TranslateAnimation title_animation = new TranslateAnimation (
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
	            Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		
		title_animation.setDuration(300);
		mTitleView.setVisibility(View.VISIBLE);
		mTitleView.startAnimation(title_animation);
		
		updateFullscreenStatus(false);
	}

	//--------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		if (mPlayer != null) {
			if (fromTouch) {
				//int pos = Math.min(progress, mPlayer.getDuration() - 1000);
				//mPlayer.setOnPreparedListener(null);
				//mSeekBar.setOnSeekBarChangeListener(null);
				
				int max = seekBar.getMax();
				int duration = mPlayer.getDuration();
				int pos;
				
				if (max != duration) {
					pos = (int)((long)progress * (long)duration / (long)max);
					pos = Math.min(pos, duration - 1);
				} else {
					pos = progress;
				}
				
				try {
					mPlayer.seekTo(pos);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					return;
				}
			}

			setTime(Res.id.PlayerCurrentTime, mPlayer.getCurrentPosition());
		}
	}

	//--------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		//seekBar.setIndeterminate(true);
	}

	//--------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		//seekBar.setIndeterminate(false);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		enableControls(true);
		mSeekBar.setProgress(mp.getCurrentPosition());
		mSeekBar.postInvalidate();
		mSeekBar.setOnSeekBarChangeListener(this);
		mPlayer.setOnPreparedListener(this);

		mHandler.postDelayed(new Runnable() {
			public void run() {
				if (mSeekBar.getSecondaryProgress() > (mSeekBar.getProgress() +
						mSeekBar.getMax() / 10)) {
					mIsPlayerPrepared = true;
					if (mWaitDialog != null) {
						mWaitDialog.dismiss();
					}
				} else {
					mHandler.postDelayed(this, 200);
				}
			}
		}, 200);
	}

	//--------------------------------------------------------------------------
	private Runnable mSeekUpdater = new Runnable() {
		@Override
		public void run() {
			try {
				if (mPlayer != null) {
					if (mPlayer.getDuration() > 0) {
						setSeekBar(mPlayer.getCurrentPosition());
						//mSeekBar.setProgress(mPlayer.getCurrentPosition());  // TODO
						if (mPlayer.isPlaying())
							mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	};

	private void refreshAspectState() {
		VideoAspectRatio  aspectRatio =  VideoAspectRatio.Original;
		String ratioValue = "";
		//------------------------------------------
		Display display = getWindowManager().getDefaultDisplay();
		int orientation = Configuration.ORIENTATION_UNDEFINED;
		if(display.getWidth() < display.getHeight())
            orientation = Configuration.ORIENTATION_PORTRAIT;
        else
        	orientation = Configuration.ORIENTATION_LANDSCAPE;
		
		//------------------------------------------
		//if (getWindowManager().getDefaultDisplay().getOrientation() == 0)
		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			ratioValue = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_video_portrait_default_ratio_key), "keep");
			if (!ratioValue.equalsIgnoreCase("keep"))
				aspectRatio = VideoAspectRatio.valueOf(ratioValue);
		}
		else
		{
			ratioValue = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(Res.string.pref_video_landscape_default_ratio_key), "keep");
			if (!ratioValue.equalsIgnoreCase("keep"))
				aspectRatio = VideoAspectRatio.valueOf(ratioValue);
		}
		if (!ratioValue.equalsIgnoreCase("keep")) {
			mPlayerLayout.setAspectRatio(aspectRatio);
			if (mAspectRatioButton != null) {
				switch(aspectRatio) {
				case Original:
					mAspectRatioButton.setImageResource(Res.drawable.icon_ar_default);
					break;
					
				case FourToTree:
					mAspectRatioButton.setImageResource(Res.drawable.icon_ar_4_3);
					break;
					
				case FiveToFour:
					mAspectRatioButton.setImageResource(Res.drawable.icon_ar_5_4);
					break;
					
				case SixteenToNine:
					mAspectRatioButton.setImageResource(Res.drawable.icon_ar_16_9);
					break;			
				}
			}
		} else {
			switch(mPlayerLayout.getAspectRatio()) {
			case Original:
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_default);
				break;
				
			case FourToTree:
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_4_3);
				break;
				
			case FiveToFour:
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_5_4);
				break;
				
			case SixteenToNine:
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_16_9);
				break;			
			}
		}
		if (mAspectRatioDlg != null && mAspectRatioDlg.isShowing() && mAspectRation_RadioGroup != null) {
			mAspectRation_RadioGroup.setOnCheckedChangeListener(null);
			switch(mPlayerLayout.getAspectRatio()) {
			case Original:
				mAspectRation_RadioGroup.check(Res.id.none);
				break;
				
			case FourToTree:
				mAspectRation_RadioGroup.check(Res.id.four_to_tree);
				break;
				
			case FiveToFour:
				mAspectRation_RadioGroup.check(Res.id.five_to_four);
				break;
				
			case SixteenToNine:
				mAspectRation_RadioGroup.check(Res.id.sixteen_to_nine);
				break;		
			}
			mAspectRation_RadioGroup.setOnCheckedChangeListener(new AspectRatioChangeListener(mAspectRatioDlg));
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mPrevOrientation != newConfig.orientation) {
			refreshAspectState();
		}
		onOrientationChanged(newConfig.orientation);
		startControlsTimer();
		mPrevOrientation = newConfig.orientation;
	}

	//--------------------------------------------------------------------------
	public void onOrientationChanged(int orientation) {
		
		Display display = getWindowManager().getDefaultDisplay();
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
		
		//if (display.getHeight() > display.getWidth()) {

			if(mAdView != null)
				mAdView.setVisibility(View.VISIBLE);

			//Ad.Visible(this);
		} else {
			if(mAdView != null)
				mAdView.setVisibility(View.INVISIBLE);

			//Ad.Gone(this);
		}
	}
	
	//--------------------------------------------------------------------------
	private void enableControls(boolean enable) {
		mPlayButton.setEnabled(enable);
		mAspectRatioButton.setEnabled(enable);

		if (!mIsSeeckableStream)
			enable = false;

		mToStartButton.setEnabled(enable);
		mRewindButton.setEnabled(enable);
		mForwardButton.setEnabled(enable);
		mSeekBar.setEnabled(enable);
	}

	//--------------------------------------------------------------------------
	private void setTime(int text_view_id, int time) {
		time /= 1000;

		TextView view = (TextView) findViewById(text_view_id);
		view.setText(String.format("%d:%02d", time / 60, time % 60));
	}

	//--------------------------------------------------------------------------
	private final View.OnClickListener MOVE_TO_START = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				moveToStart();
			}
		}
	};

	//--------------------------------------------------------------------------
	private final View.OnClickListener REWIND = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				backward();
			}
		}
	};

	//--------------------------------------------------------------------------
	private final View.OnClickListener FORWARD = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				forward();
			}
		}
	};

	//--------------------------------------------------------------------------
	private final View.OnClickListener PLAY = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				playPause();
			}
		}
	};

	//--------------------------------------------------------------------------
	private final View.OnClickListener ASPECT_RATIO = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch(mPlayerLayout.getAspectRatio()) {
			case Original:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.FourToTree);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_4_3);
				break;
				
			case FourToTree:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.FiveToFour);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_5_4);
				break;
				
			case FiveToFour:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.SixteenToNine);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_16_9);
				break;
				
			case SixteenToNine:
				mPlayerLayout.setAspectRatio(VideoAspectRatio.Original);
				mAspectRatioButton.setImageResource(Res.drawable.icon_ar_default);
				break;			
			}
			
			mPlayerLayout.requestLayout();
		}
	};
	
	//--------------------------------------------------------------------------
	@Override
	public void onMediaScannerConnected() {
		for (File file : mFilesToAddToMedia) {
			mMediaScanner.scanFile(file.getAbsolutePath(), "image/jpeg");
		}

		mFilesToAddToMedia.removeAllElements();
	}

	//--------------------------------------------------------------------------
	@Override
	public void onScanCompleted(String path, Uri uri) {
	}

	//--------------------------------------------------------------------------
	@Override
	public void onBufferingUpdate(MediaPlayer player, int percent) {
		if (mWaitDialog != null && mWaitDialog.isShowing()) {
			mWaitDialog.invalidatePercent(percent);
		}

		mSeekBar.setSecondaryProgress( percent * mSeekBar.getMax() / 100 );
		mSeekBar.invalidate();
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		mPlayerLayout.requestLayout();
	}

	//--------------------------------------------------------------------------
	public final MediaPlayer getPlayer() { 
		return mPlayer;
	}
	
	//--------------------------------------------------------------------------
	private void startControlsTimer() {
		
		mLastPenUp = System.currentTimeMillis();
		
		mHandler.postDelayed(new Runnable() {			
			@Override
			public void run() {
				long time = System.currentTimeMillis();
				if (!mPenDown && time >= mLastPenUp + CONTROLS_AUTO_HIDE_TIME) {
					if (mControls.getVisibility() == View.VISIBLE &&
							mRootView.getWidth() > mRootView.getHeight()) {
						hideControls();
					}
				}
			}
		}, CONTROLS_AUTO_HIDE_TIME);
	}
	
	//--------------------------------------------------------------------------
	private void dispatchMotionEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mPenDown = true;
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mPenDown = false;
			startControlsTimer();
			break;
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		boolean result = super.dispatchTouchEvent(event); 
		dispatchMotionEvent(event);		
		return result;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		boolean result = super.dispatchTrackballEvent(event);
		dispatchMotionEvent(event);
		return result;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean result = super.dispatchKeyEvent(event);
		
		switch (event.getAction()) {
		case KeyEvent.ACTION_DOWN:
			mPenDown = true;
			break;
			
		case KeyEvent.ACTION_UP:
			mPenDown = false;
			startControlsTimer();
			break;
		}
		
		return result;
	}
	
	//--------------------------------------------------------------------------
	/*private boolean mCallPause = false;
	
	private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch(state) {
			case TelephonyManager.CALL_STATE_IDLE:
				Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
				if (mPlayer != null && !mPlayer.isPlaying() && mCallPause) {
					playPause();
				}
				break;

			case TelephonyManager.CALL_STATE_RINGING:
				Log.i(TAG, "TelephonyManager.CALL_STATE_RINGING");
				mCallPause = (mPlayer != null && mPlayer.isPlaying());
				if (mCallPause) {
					playPause();
				}
				break;

			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.i(TAG, "TelephonyManager.CALL_STATE_OFFHOOK");
				if (!mCallPause) {
					mCallPause = (mPlayer != null && mPlayer.isPlaying());
					if (mCallPause) {
						playPause();
					}
				}
				break;
			}
		}
	};*/
}
