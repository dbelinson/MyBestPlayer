package com.simpity.android.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.dialogs.DownloadDialog;
import com.simpity.android.media.dialogs.ScaleChangedHandler;
import com.simpity.android.media.dialogs.ScaleDialog;
import com.simpity.android.media.utils.Downloader;
import com.simpity.android.media.utils.Message;
import com.simpity.android.media.utils.Utilities;
import com.simpity.android.media.video.RtspRecordsManager;

public class MediaPlayerActivity extends Activity implements
		MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
		SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener,
		MediaScannerConnectionClient, OnBufferingUpdateListener {

	private final static String TAG = "MediaPlayerActivity";

	public static final String URL_DATA = "URL";
	public static final String DESCRIPTION_DATA = "DESCRIPTION";
	public static final String SESSION_ID = "SESSION";

	private static final String POSITION_KEY = "POSITION";

	protected final static int SEEKBAR_UPDATE_PERIOD = 1000;
	protected final static int PLAYER_SCROLL_STEP = 10000;
	public static final String IS_SUPRESS_NAVIGATION = "SUPRESS_NAVIGATION";
	public static final String IS_LINK_TO_SHARE = "Is Link to share";
	public static final String DO_NOT_SHOW_DOWNLOAD_QUESTION = "Is show download question";

	private MediaPlayer mPlayer;
	protected Handler mHandler = new Handler();
	protected SeekBar mSeekBar;

	private com.simpity.android.media.dialogs.WaitDialog mWaitDialog = null;
	PowerManager.WakeLock mWakeLock = null;
	ScaleDialog dlg = null;
	private String mSession;

	private MediaScannerConnection mMediaScanner = null;
	private File[] filesToAddToMedia = new File[0];
	boolean surfaceLongClick = false;
	private int errorCount = 0;

	private boolean shouldStartPlayback = true;
	private int videoW, videoH;
	private SurfaceHolder mHolder = null;
	private boolean isActivityPaused = false;
	private boolean isPlayerPrepared = false;
	private boolean isSeeckableStream = true;
	private DownloadDialog mDownloadDialog = null;
	private Downloader mDownloader;
	private SharedPreferences mPrefs = null;
	Thread killPlayerThread = null;
	
	private Ad mAdView;
	
	// --------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		setContentView(Res.layout.media_payer_view);

		if(savedInstanceState != null)
			shouldStartPlayback = false;
		
		mAdView = new Ad(this);

		if(getWindowManager().getDefaultDisplay().getOrientation() == 0)
		{
			if(mAdView != null)
				mAdView.setVisibility(View.VISIBLE);

			//Ad.Visible(this);
		}
		else
		{
			//Ad.Gone(this);
			if(mAdView != null)
				mAdView.setVisibility(View.INVISIBLE);
		}

		mWaitDialog = new com.simpity.android.media.dialogs.WaitDialog(this);
		mWaitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			if(new File(getIntent().getData().getPath()).exists())
				getIntent().putExtra(URL_DATA, getIntent().getData().getPath());
			else{
				getIntent().putExtra(URL_DATA, getIntent().getData().toString());
				//save link to history
				//RtspUrlListAdapter adapter = new RtspUrlListAdapter(this);
				//adapter.add("", getIntent().getData().toString(), true, true);
				RtspRecordsManager manager = new RtspRecordsManager(this, null);
				manager.add("", getIntent().getData().toString(), "Other", -1, false, Calendar.getInstance().getTimeInMillis(), false, false);
			}
		}

		mSession = getIntent().getStringExtra(SESSION_ID);
		SurfaceView view = (SurfaceView) findViewById(Res.id.media_player_surface);
		mHolder = view.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setSizeFromLayout();
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SurfaceClick();
			}
		});
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				return SurfaceLongClick();
			}
		});

		dlg = new ScaleDialog(this);
		dlg.SetScaleChangedHandler(new ScaleChangedHandler() {
			@Override
			public void ScaleChangedHandle(int width, int height) {
				updateSurfaceSize(dlg.GetIsUseNative(), dlg.GetScale());
			}
		});
		dlg.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				surfaceLongClick = false;
			}
		});
		
		dlg.reset();
		String description = getIntent().getStringExtra(DESCRIPTION_DATA);
		if (description == null || description.length() == 0) {
			description = getIntent().getStringExtra(URL_DATA);
		}

		if (description != null) {
			TextView title = (TextView) findViewById(Res.id.media_player_title);
			title.setText(description);
		}

		ImageButton image_button = (ImageButton)findViewById(Res.id.media_player_to_start);
		if (image_button != null) {
			image_button.setOnClickListener(MOVE_TO_START);
			//image_button.setImageResource(android.R.drawable.ic_media_previous);
		}

		image_button = (ImageButton)findViewById(Res.id.media_player_backward);
		if (image_button != null) {
			image_button.setOnClickListener(BACKWARD);
			//image_button.setImageResource(android.R.drawable.ic_media_rew);
		}

		image_button = (ImageButton)findViewById(Res.id.media_player_forward);
		if (image_button != null) {
			image_button.setOnClickListener(FORWARD);
			//image_button.setImageResource(android.R.drawable.ic_media_ff);
		}

		image_button = (ImageButton)findViewById(Res.id.media_player_play);
		if (image_button != null) {
			image_button.setOnClickListener(PLAY);
			//image_button.setImageResource(android.R.drawable.ic_media_play);
		}

		mSeekBar = (SeekBar) findViewById(Res.id.media_player_seekbar);
		mSeekBar.setOnSeekBarChangeListener(this);

		setResult(StreamMediaActivity.SUCCESS, getIntent());

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
		mMediaScanner = new MediaScannerConnection(MediaPlayerActivity.this, MediaPlayerActivity.this);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		//TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		//tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	// --------------------------------------------------------------------------
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		isActivityPaused = true;
		if(!isFinishing()){
			if (mPlayer != null && isPlayerPrepared) {
				int	position = -1;
				position = mPlayer.getCurrentPosition();
				if(mPlayer.isPlaying()){
					position = playPause();
				}
				if(mPlayer.getDuration() <=0 )
					position = -1;
				if (position > 0 && mSession != null) {
					SharedPreferences.Editor edit = getPreferences(
							Activity.MODE_PRIVATE).edit();
					edit.clear();
					edit.putString(SESSION_ID, mSession);
					edit.putInt(POSITION_KEY, position);
					edit.commit();
				}
			}
			if(mWaitDialog != null && mWaitDialog.isShowing()){
				mWaitDialog.dismiss();
			}
		}
		shouldStartPlayback = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		isActivityPaused = false;
		if(findViewById(Res.id.media_player_buttons).getVisibility() != View.VISIBLE)
			SurfaceClick();
		else{
			findViewById(Res.id.media_player_title).bringToFront();
			findViewById(Res.id.media_player_buttons).bringToFront();
		}
		if(mPlayer == null || !isPlayerPrepared){
			mWaitDialog.show(true);
		}
		/*
		else{
			if(isPlayerPrepared && mPlayer.getDuration()<=0){
				playPause();
			}
		}
		*/
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		if(isFinishing() && dlg!=null && dlg.isShowing())
			dlg.dismiss();
		if(mDownloader != null)
			mDownloader.StopDownload();
		if(mPlayer != null){
			mPlayer.setOnPreparedListener(null);
			mPlayer.setOnErrorListener(null);
		}
		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
		if (mMediaScanner.isConnected())
			mMediaScanner.disconnect();
		
		//TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		//tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		if(mAdView != null)
    		mAdView.destroy();
		
		super.onDestroy();
	};

	// --------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		/*
		if (mPlayer != null) {
			try{
				if (mPlayer.isPlaying()) {
					mPlayer.stop();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		*/
		killPlayerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				if(mPlayer != null){
					mPlayer.release();
					mPlayer = null;
				}
				Log.d(TAG, "Player is Killed");
			}
		});
		killPlayerThread.setPriority(Thread.MIN_PRIORITY);
		killPlayerThread.start();
		isPlayerPrepared = false;
	}
	
	// --------------------------------------------------------------------------

	// --------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//if(mPlayer != null)
		//	mPlayer.setDisplay(holder);
	}

	// --------------------------------------------------------------------------

	// --------------------------------------------------------------------------
	// from SurfaceHolder.Callback
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		if(mWaitDialog != null && !mWaitDialog.isShowing())
			mWaitDialog.show(true);
		isPlayerPrepared = false;
		if(killPlayerThread != null && killPlayerThread.isAlive()){
			try {
				killPlayerThread.join();
				if(isFinishing())
					return;
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				if(isFinishing())
					return;
			}
		}
		if(isFinishing())
			return;
		if(mPlayer == null){
			mPlayer = new MediaPlayer();
		}
		mPlayer.reset();
		mPlayer.setDisplay(holder);
		mPlayer.setOnCompletionListener(MediaPlayerActivity.this);
		mPlayer.setOnSeekCompleteListener(MediaPlayerActivity.this);
		mPlayer.setOnErrorListener(MediaPlayerActivity.this);
		mPlayer.setOnBufferingUpdateListener(MediaPlayerActivity.this);
		mPlayer.setOnPreparedListener(MediaPlayerActivity.this);
		mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {

			@Override
			public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
				videoH = height;
				videoW = width;
			}
		});
		String path = getIntent().getStringExtra(URL_DATA);
			try {
				mPlayer.setDataSource(path);
				if(path.startsWith(Environment.getExternalStorageDirectory().getPath()))
					mSeekBar.setSecondaryProgress(mSeekBar.getMax());
				mPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				processExeption(e, path);
			} catch (IllegalStateException e) {
				processExeption(e, path);
			} catch (IOException e) {
				processExeption(e, path);
			}
	}

	// --------------------------------------------------------------------------

	// --------------------------------------------------------------------------
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

		Message.show(this, Res.string.error, getResources().getString(
						Res.string.open_media_error, path),
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						MediaPlayerActivity.this.finish();
					}
				});
	}


	// --------------------------------------------------------------------------
	// from MediaPlayer.OnErrorListener
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		
		if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
			if(mWaitDialog != null && mWaitDialog.isShowing())
				mWaitDialog.dismiss();
			if(!mPrefs.getBoolean(DO_NOT_SHOW_DOWNLOAD_QUESTION, false)){
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
			}else
				StartDownloader();
			
			return true;
		}
		
		
		String message;
		if (extra == -10 && errorCount < 3)
		{
			errorCount++;
			SharedPreferences.Editor edit = getPreferences(
					Activity.MODE_PRIVATE).edit();
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
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(mWaitDialog != null && mWaitDialog.isShowing())
			mWaitDialog.dismiss();

		if (extra == -11)// camera settings is changed, should try to reconnect
		{
			MediaPlayerActivity.this.onStop();

			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
			dlg.setMessage("Connection lost, reconnect?");
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
							MediaPlayerActivity.this.setResult(StreamMediaActivity.CANCEL, getIntent());
							MediaPlayerActivity.this.finish();
						}
					});
			dlg.setCancelable(true);
			dlg.show();

			return true;
		}

		mPlayer.setOnErrorListener(null);
		
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			message = "Error #" + extra + ". Media server died.";
		} else if (extra == -4){ //not supported
			message = "Error #" + extra + ". Not supported";
		}else{
			message = "Error #" + extra + ". Unspecified media player error.";
		}

		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}

		setResult(StreamMediaActivity.CANCEL, getIntent());

		Message.show(this, Res.string.error, message,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
						MediaPlayerActivity.this.finish();
					}
				});

		return true;
	}
	
	private void StartDownloader(){
		if(mDownloader != null)
			mDownloader.StopDownload();
		UpdateDownloadingInfo(new Downloader.OnRetriveDownloadInfoListener.DownloadProgressInfo());
		mDownloader = new Downloader(MediaPlayerActivity.this, getIntent().getStringExtra(URL_DATA));
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
	
	
	private void UpdateDownloadingInfo(Downloader.OnRetriveDownloadInfoListener.DownloadProgressInfo info)
	{
		if(info.isErrorOccured){
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
		}else if(!info.isDownloadCompleted){
			findViewById(Res.id.media_player_surface).setVisibility(View.INVISIBLE);
			findViewById(Res.id.DownloadInfoContainer).setVisibility(View.VISIBLE);
			findViewById(Res.id.DownloadingNotes).setVisibility(View.VISIBLE);
			
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
		}else{
			mDownloader = null;
			findViewById(Res.id.media_player_surface).setVisibility(View.VISIBLE);
			findViewById(Res.id.DownloadInfoContainer).setVisibility(View.INVISIBLE);
			findViewById(Res.id.DownloadingNotes).setVisibility(View.INVISIBLE);
			getIntent().putExtra(URL_DATA, info.targetFile.getAbsolutePath());
			if(mHolder != null)
				surfaceCreated(mHolder);
		}
	}

	// --------------------------------------------------------------------------
	// from MediaPlayer.OnPreparedListener
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "onPrepared");
		setTime(Res.id.media_player_total_time, mp.getDuration());

		updateSurfaceSize(dlg.GetIsUseNative(), dlg.GetScale());

		SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
		int position = pref.getInt(POSITION_KEY, 0);

		int secProgress = mSeekBar.getSecondaryProgress();
		int maxProgress = (mp.getDuration() > 0)? mp.getDuration() : 1;
		if(maxProgress != mSeekBar.getMax()){
			mSeekBar.setMax(maxProgress);
			if(secProgress <= 100)
				mSeekBar.setSecondaryProgress((int)(((double)secProgress/(double)100) * (double)mSeekBar.getMax()));
			mSeekBar.setProgress(position);
		}
		isSeeckableStream = mp.getDuration() > 0;

		mSeekBar.postInvalidate();

		if (position > 0) {
			//enableControls(false);
			mp.seekTo(position);
		} else {
			isPlayerPrepared = true;
			if (getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				enableControls(false);
			} else {
				enableControls(true);
			}
		}

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				try{
					getIntent().putExtra(IS_LINK_TO_SHARE, true);
				}
				catch (Exception e){
					e.printStackTrace();
				}
			}
		}, 20000);

		if(isActivityPaused){
			if(mPlayer.getDuration()<=0)
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						mPlayer.stop();
					}
				});
			return;
		}

		if(shouldStartPlayback || mPlayer.getDuration() <= 0){
			mPlayer.start();
			ImageButton button = (ImageButton) findViewById(Res.id.media_player_play);
			button.setImageResource(Res.drawable.pause); // android.R.drawable.ic_media_pause);
			mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
			button.invalidate();
			if (mWaitDialog != null) {
				mWaitDialog.dismiss();
			}
		}
		if(position <= 0)
			if (mWaitDialog != null) {
				mWaitDialog.dismiss();
			}
	}

	// --------------------------------------------------------------------------
	// from MediaPlayer.OnCompletionListener
	@Override
	public void onCompletion(MediaPlayer mp) {
		ImageButton button = (ImageButton) findViewById(Res.id.media_player_play);
		button.setImageResource(Res.drawable.play); // android.R.drawable.ic_media_play);
		button.invalidate();

		setTime(Res.id.media_player_current_time, mPlayer.getDuration());

		mPlayer.release();
		mPlayer = null;

		button = (ImageButton) findViewById(Res.id.media_player_to_start);
		if (button.isPressed())
			button.setPressed(false);
		button.setEnabled(false);

		button = (ImageButton) findViewById(Res.id.media_player_backward);
		if (button.isPressed())
			button.setPressed(false);
		button.setEnabled(false);

		button = (ImageButton) findViewById(Res.id.media_player_forward);
		if (button.isPressed())
			button.setPressed(false);
		button.setEnabled(false);

		findViewById(Res.id.media_player_seekbar).setEnabled(false);

		findViewById(Res.id.media_player_play).setEnabled(true);

		if(findViewById(Res.id.media_player_buttons).getVisibility() != View.VISIBLE)
			SurfaceClick();
		else{
			findViewById(Res.id.media_player_title).bringToFront();
			findViewById(Res.id.media_player_buttons).bringToFront();
		}
		getIntent().putExtra(IS_LINK_TO_SHARE, true);
	}

	// --------------------------------------------------------------------------
	protected void moveToStart() {
		if (mPlayer == null)
			return;

		enableControls(false);
		mPlayer.seekTo(0);
		mSeekBar.setProgress(0);
	}

	// --------------------------------------------------------------------------
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
		mSeekBar.setProgress(pos);
	}

	// --------------------------------------------------------------------------
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
		mSeekBar.setProgress(pos);
	}

	// --------------------------------------------------------------------------
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
		mSeekBar.setProgress(pos);
	}

	// --------------------------------------------------------------------------
	protected int playPause() {
		int pausePosition = -1;
		if (mPlayer == null) {
			SharedPreferences.Editor edit = getPreferences(
					Activity.MODE_PRIVATE).edit();
			edit.clear();
			edit.putString(SESSION_ID, mSession);
			edit.commit();

			SurfaceView view = (SurfaceView) findViewById(Res.id.media_player_surface);
			surfaceCreated(view.getHolder());
			return -1;
		}

		ImageButton button = (ImageButton) findViewById(Res.id.media_player_play);

		if (mPlayer.isPlaying()) {
			
			if(mPlayer.getDuration() > 0){
				mPlayer.pause();
				pausePosition = mPlayer.getCurrentPosition();
			}
			else{
				pausePosition = mPlayer.getCurrentPosition();
				mPlayer.stop();
				mSeekBar.setMax(100);
			}
			button.setImageResource(Res.drawable.play); // android.R.drawable.ic_media_play);
			
		} else {
			
			pausePosition = -1;
			if(mPlayer.getDuration()>0)
				mPlayer.start();
			else{
				mWaitDialog.show(true);
				surfaceCreated(mHolder);
				return -1;
			}
			button.setImageResource(Res.drawable.pause); // android.R.drawable.ic_media_pause);
			mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
		}
		
		button.invalidate();
		if (mWaitDialog != null) {
			mWaitDialog.dismiss();
		}
		return pausePosition;
	}

	// --------------------------------------------------------------------------
	/*protected String getMediaFolder() {
		File ImageDir = Environment.getExternalStorageDirectory();
		if (!ImageDir.canWrite()) // Workaround for broken sdcard support on the
			// device.
			ImageDir = new File("/sdcard/" + getString(Res.string.imagesFolder));
		else
			ImageDir = new File(ImageDir, getString(Res.string.imagesFolder));
		// if (!ImageDir.exists())
		// ImageDir.mkdir();

		ImageDir = new File(ImageDir, getString(Res.string.app_name));
		if (!ImageDir.exists())
			ImageDir.mkdirs();

		return ImageDir.getAbsolutePath();
	}*/

	// --------------------------------------------------------------------------
	protected void GetSnapshot() {
		View s_view = findViewById(Res.id.media_player_surface);
		Bitmap bmp = Utilities.createBitmap(s_view.getWidth(), s_view.getHeight());
		if (bmp != null) {
			Canvas canvas = new Canvas(bmp);
			s_view.draw(canvas);
		} else {
			return;
		}

		String description = getIntent().getStringExtra(MediaPlayerActivity.DESCRIPTION_DATA);
		
		File imageFile = Utilities.getNewSnapshotFile(description);
		if (imageFile == null)
			return;
		
		FileOutputStream fOut = null;
		try {
			if (imageFile.createNewFile()) {
				fOut = new FileOutputStream(imageFile);
				if (bmp.compress(CompressFormat.JPEG, 100, fOut)) {
					fOut.flush();
					fOut.close();
					fOut = null;
					if (mMediaScanner.isConnected()) {
						
						mMediaScanner.scanFile(imageFile.getAbsolutePath(), "image/jpeg");
						
					} else {
						
						File[] tmp = filesToAddToMedia;
						filesToAddToMedia = new File[tmp.length + 1];
						System.arraycopy(tmp, 0, filesToAddToMedia, 0, tmp.length);
						
						/*for (int i = 0; i < tmp.length; i++) {
							filesToAddToMedia[i] = tmp[i];
						}*/
						
						filesToAddToMedia[filesToAddToMedia.length - 1] = imageFile;
						mMediaScanner.connect();
					}
				}
				if (fOut != null)
					fOut.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateFullscreenStatus(boolean bUseFullscreen)
	{
	   if(bUseFullscreen)
	   {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		else
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		findViewById(Res.id.media_player_root).requestLayout();
		findViewById(Res.id.media_player_root).invalidate();
	}

	// --------------------------------------------------------------------------
	private void SurfaceClick() {
		View view = findViewById(Res.id.media_player_buttons);
		View title = findViewById(Res.id.media_player_title);

		if (view.getVisibility() == View.VISIBLE) {
			view.setVisibility(View.GONE);
			title.setVisibility(View.GONE);
			updateFullscreenStatus(true);
		} else {
			updateFullscreenStatus(false);
			view.setVisibility(View.VISIBLE);
			title.setVisibility(View.VISIBLE);
		}
		view.invalidate();
		title.invalidate();

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				findViewById(Res.id.media_player_root).requestLayout();
				findViewById(Res.id.media_player_root).invalidate();
				updateSurfaceSize(dlg.GetIsUseNative(), dlg.GetScale());
			}
		}, 300);
	}

	// --------------------------------------------------------------------------
	private boolean SurfaceLongClick()
	{
		surfaceLongClick = true;
		dlg.show(3000);
		return true;
	}

	// --------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {
		if (mPlayer != null) {
			if (fromTouch) {
				int pos = Math.min(progress, mPlayer.getDuration() - 1000);
				mPlayer.setOnPreparedListener(null);
				mSeekBar.setOnSeekBarChangeListener(null);
				mPlayer.seekTo(pos);
			}
			setTime(Res.id.media_player_current_time, mPlayer.getCurrentPosition());
		}
	}

	// --------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	// --------------------------------------------------------------------------
	// from SeekBar.OnSeekBarChangeListener
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	// --------------------------------------------------------------------------
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		enableControls(true);
		mSeekBar.setProgress(mp.getCurrentPosition());
		mSeekBar.postInvalidate();
		mSeekBar.setOnSeekBarChangeListener(this);
		mPlayer.setOnPreparedListener(this);
		mHandler.postDelayed(new Runnable() {
				public void run() {
					if(mSeekBar.getSecondaryProgress() > (mSeekBar.getProgress() + (mSeekBar.getMax() * 0.1))){
						isPlayerPrepared = true;
						if (mWaitDialog != null) {
							mWaitDialog.dismiss();
						}
					}
					else
						mHandler.postDelayed(this, 200);
				}
			}, 200);
	}

	// --------------------------------------------------------------------------
	private Runnable mSeekUpdater = new Runnable() {
		@Override
		public void run() {
			try{
				if (mPlayer != null) {
					if(mPlayer.getDuration() > 0){
						mSeekBar.setProgress(mPlayer.getCurrentPosition());
						if (mPlayer.isPlaying())
							mHandler.postDelayed(mSeekUpdater, SEEKBAR_UPDATE_PERIOD);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	};

	// --------------------------------------------------------------------------
	private void updateSurfaceSize(boolean isUseNative, double scale) {
		int width, height, surf_width, surf_height, y = 0;
		View view = findViewById(Res.id.media_player_root);
		width = view.getWidth();
		height = view.getHeight();

		view = findViewById(Res.id.media_player_buttons);

		if (view.getVisibility() == View.VISIBLE) {
			height -= view.getHeight();
			y = findViewById(Res.id.media_player_title).getHeight();
			height -= y;
		}

		surf_height = height;
		surf_width = width;

		if (mPlayer != null) {
			if(isUseNative)
			{
				if(videoH > 0 && videoW > 0)
					scale = (double) videoW/ (double)videoH;
				else
					scale = 1.0;
			}
		}

		if(width < height){
			surf_height = height;
			surf_width = (int)((double)height * scale);
		}else{
			surf_height = width;
			surf_width = (int)((double)width * scale);
		}
		while(true){
			if(surf_width > width || surf_height > height){
				double koefW = (double)width/(double)surf_width;
				double koefH = (double)height/(double)surf_height;

				if(koefW < koefH){
					surf_width = (int)((double)surf_width * koefW);
					surf_height = (int)((double)surf_height * koefW);
				}
				else{
					surf_width = (int)((double)surf_width * koefH);
					surf_height = (int)((double)surf_height * koefH);
				}
			}
			else{
				if(y == 0 && ((width - surf_width) < 2 || (height - surf_height) < 2)){
					surf_width -= 2;
					surf_height -= 2;
				}
				break;
			}
		}

		SurfaceView sview = (SurfaceView) findViewById(Res.id.media_player_surface);
		sview.layout(((width - surf_width) / 2), ((height - surf_height)/2 + y),
					((width + surf_width) / 2), ((height - surf_height)/2 + y + surf_height));

		//sview.getHolder().getSurface().setSize(surf_width, surf_height);

		Log.d("updateSurfaceSize", String.format("view.layout(%s,%s,%s,%s)", (width - surf_width) / 2, (height - surf_height)/2 + y,	(width + surf_width) / 2, (height - surf_height)/2 + y + surf_height));
		if(y > 0){
			findViewById(Res.id.media_player_buttons).bringToFront();
			findViewById(Res.id.media_player_title).bringToFront();
		}
		else
			sview.bringToFront();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			//Ad.Visible(this);
			if(mAdView != null)
				mAdView.setVisibility(View.VISIBLE);
		}
		else
		{
			if(mAdView != null)
				mAdView.setVisibility(View.INVISIBLE);

			//Ad.Gone(this);
		}

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				updateSurfaceSize(dlg.GetIsUseNative(), dlg.GetScale());
			}
		}, 300);
	}


	// --------------------------------------------------------------------------
	private void enableControls(boolean enable) {
		if(isSeeckableStream){
			findViewById(Res.id.media_player_to_start).setEnabled(enable);
			findViewById(Res.id.media_player_backward).setEnabled(enable);
			findViewById(Res.id.media_player_forward).setEnabled(enable);
			findViewById(Res.id.media_player_seekbar).setEnabled(enable);
		}
		else{
			findViewById(Res.id.media_player_to_start).setEnabled(false);
			findViewById(Res.id.media_player_backward).setEnabled(false);
			findViewById(Res.id.media_player_forward).setEnabled(false);
			findViewById(Res.id.media_player_seekbar).setEnabled(false);
		}
		findViewById(Res.id.media_player_play).setEnabled(enable);
	}

	// --------------------------------------------------------------------------
	private void setTime(int text_view_id, int time) {
		time /= 1000;

		TextView view = (TextView) findViewById(text_view_id);
		view.setText(String.format("%d:%02d", time / 60, time % 60));
	}

	// --------------------------------------------------------------------------
	private final View.OnClickListener MOVE_TO_START = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				moveToStart();
			}
		}
	};

	// --------------------------------------------------------------------------
	private final View.OnClickListener BACKWARD = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				backward();
			}
		}
	};

	// --------------------------------------------------------------------------
	private final View.OnClickListener FORWARD = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				forward();
			}
		}
	};

	// --------------------------------------------------------------------------
	private final View.OnClickListener PLAY = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!getIntent().getBooleanExtra(IS_SUPRESS_NAVIGATION, false)) {
				playPause();
			}
		}
	};

	// --------------------------------------------------------------------------
	@Override
	public void onMediaScannerConnected() {
		for (File f : filesToAddToMedia) {
			mMediaScanner.scanFile(f.getAbsolutePath(), "image/jpeg");
		}
		filesToAddToMedia = new File[0];
	}

	// --------------------------------------------------------------------------
	@Override
	public void onScanCompleted(String path, Uri uri) {

	}

	@Override
	public void onBufferingUpdate(MediaPlayer player, int percent) {
		if(mWaitDialog != null && mWaitDialog.isShowing())
			mWaitDialog.invalidatePercent(percent);
		mSeekBar.setSecondaryProgress((int)(((double)percent/(double)100) * (double)mSeekBar.getMax()));
		mSeekBar.invalidate();
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
