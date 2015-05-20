package com.simpity.android.media.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.dialogs.WaitDialog;
import com.simpity.android.media.statistic.Session;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.Utilities;
import com.simpity.android.media.widgets.camera.WebCamRefreshService;
import com.simpity.android.media.widgets.camera.WebCamRefreshService.CameraInfo;
import com.simpity.android.media.widgets.camera.WebCamRefreshService.UpdateListener;

public class JpegCameraView extends Activity implements MediaScannerConnectionClient, UpdateListener {

	//private final static String TAG = "JpegCameraView";

	public static final String IS_LINK_TO_SHARE = "Is Link to share";
	
	private Handler mHandler = new Handler();
	private MediaScannerConnection mMediaScanner = null;
	private File[] mFilesToAddToMedia = new File[0];
	private boolean mIsShowError = true;
	private String mRefreshToastText;
	private WaitDialog mWaitDialog = null;
	private ImageView mImageView = null;
	private Session mStatSession = null;
	private CameraInfo mCamera = null;
	private View mRefreshBtnView = null; 
	private boolean mScheduleRequestToService = false;
	private Ad mAdView = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindService(new Intent(this, WebCamRefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(Res.layout.jpeg_camera);
		
		mWaitDialog = new WaitDialog(this);
		mWaitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

		/*
		if(getWindowManager().getDefaultDisplay().getOrientation() == 0)
			Ad.Visible(this);
		else
			Ad.Gone(this);
		*/
		
		mImageView = (ImageView) findViewById(Res.id.jpeg_camera_view);
		
		mRefreshBtnView = findViewById(Res.id.jpeg_view_refresh_button);
		
		mImageView.setScrollContainer(true);
		mImageView.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);
		mImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(findViewById(Res.id.jpeg_view_buttons_panel).getVisibility() == View.VISIBLE){
					setFullScreenMode(true);
			    }else{
					setFullScreenMode(false);
			    }
			}
		});
		
		setResult(StreamMediaActivity.CANCEL, getIntent());
		
		mRefreshBtnView.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						if(mCamera != null){
							mIsShowError = true;
							v.setEnabled(false);
							WebCamRefreshService service = WebCamRefreshService.getServiceInstance();
							if (service != null) {
								service.requestRefreshImmediate(mCamera);
							}
						}
					}
				});
		mMediaScanner = new MediaScannerConnection(JpegCameraView.this,	JpegCameraView.this);
		mWaitDialog.show();
		
		mAdView = new Ad(this);
		
		if(mAdView != null)
		{
			if(getWindowManager().getDefaultDisplay().getOrientation() == 0)
				mAdView.setVisibility(View.VISIBLE);
			else
				mAdView.setVisibility(View.GONE);
		}
	}
	
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder i_service) {
			WebCamRefreshService service = WebCamRefreshService.getServiceInstance();
			if (service != null) {
				mCamera = service.new CameraInfo(getIntent().getStringExtra(JpegEditActivity.URL_DATA), Integer.parseInt(getIntent().getStringExtra(JpegEditActivity.REFRESH_DATA)), null, null);
				if(mScheduleRequestToService){
					mScheduleRequestToService = false;
					service.startRefreshCamera(mCamera, JpegCameraView.this);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
		}
	};
	
	private void setFullScreenMode(boolean enableState){
		if(enableState)
		{
			findViewById(Res.id.jpeg_view_buttons_panel).setVisibility(View.GONE);
			findViewById(Res.id.header).setVisibility(View.GONE);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	    }
		else
		{
			findViewById(Res.id.jpeg_view_buttons_panel).setVisibility(View.VISIBLE);
			findViewById(Res.id.header).setVisibility(View.VISIBLE);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }
		findViewById(Res.id.root_jpeg_layout).requestLayout();
	    findViewById(Res.id.root_jpeg_layout).invalidate();
	}

	@Override 
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	/*
    	if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Ad.Visible(this);
		} else {
			Ad.Gone(this);
		}
		*/
		if(mAdView != null)
		{
			if(getWindowManager().getDefaultDisplay().getOrientation() == 0)
				mAdView.setVisibility(View.VISIBLE);
			else
				mAdView.setVisibility(View.GONE);
		}
    };
    
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
    
	@Override
	protected void onStart() {
		super.onStart();
		
		String description = getIntent().getStringExtra(JpegEditActivity.DESCRIPTION_DATA);
		
		if (description == null || description.trim().length() == 0) {
			description = getIntent().getStringExtra(JpegEditActivity.URL_DATA);
		}
		
		((TextView)findViewById(Res.id.header)).setText(description);
		
		if(mCamera != null){
			WebCamRefreshService service = WebCamRefreshService.getServiceInstance();
			if (service != null) {
				service.startRefreshCamera(mCamera, this);
			}
		}else{
			mScheduleRequestToService = true;
		}
	}

	public Boolean SaveSnapshot() {

		Intent intent = getIntent();

		String description = intent.getStringExtra(JpegEditActivity.DESCRIPTION_DATA);

		File imageFile = Utilities.getNewSnapshotFile(description);
		if (imageFile == null)
			return false;
		
		boolean result = SaveCurrentFrame(imageFile);
		if (result) {
			if (mMediaScanner.isConnected()) {
				
				mMediaScanner.scanFile(imageFile.getAbsolutePath(), "image/jpeg");
				
			} else {
				
				File[] tmp = mFilesToAddToMedia;
				mFilesToAddToMedia = new File[mFilesToAddToMedia.length + 1];
				System.arraycopy(tmp, 0, mFilesToAddToMedia, 0, tmp.length);
				
				/*for (int i = 0; i < tmp.length; i++) {
					filesToAddToMedia[i] = tmp[i];
				}*/
				
				mFilesToAddToMedia[mFilesToAddToMedia.length - 1] = imageFile;
				mMediaScanner.connect();
			}
		}
		return result;
	}

	private Boolean SaveCurrentFrame(File imgToSave) {
		if (mCamera != null && mCamera.lastImage != null) {
			FileOutputStream fOut = null;
			try {
				if (imgToSave.createNewFile()) {
					fOut = new FileOutputStream(imgToSave);
				} else {
					// file already exist
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			if (fOut != null)
				try {
					mCamera.lastImage.compress(CompressFormat.JPEG, 100, fOut);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				} finally {
					if (fOut != null) {
						try {
							fOut.flush();
							fOut.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
		} else
			return false;
		return false;
	}

	private void refreshToast(String text) {
		if(mIsShowError){
			mIsShowError = false;
			mRefreshToastText = text;
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(JpegCameraView.this, mRefreshToastText, Toast.LENGTH_LONG).show();
				}
			});
		}
	}
	
	@Override
	protected void onStop() {
		WebCamRefreshService service = WebCamRefreshService.getServiceInstance();
		if (service != null) {
			service.stopRefreshCamera(mCamera);
		}
		
		mIsShowError = false;
		if (mMediaScanner.isConnected()) {
			mMediaScanner.disconnect();
		}
		
		if(mStatSession != null){
			mStatSession.endSession();
			mStatSession = null;
		}
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		try {
			unbindService(mConnection);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		if (mAdView != null) {
			mAdView.destroy();
			mAdView = null;
		}

		super.onDestroy();
	}

	@Override
	public void onMediaScannerConnected() {
		for (File f : mFilesToAddToMedia) {
			mMediaScanner.scanFile(f.getAbsolutePath(), "image/jpeg");
		}
		mFilesToAddToMedia = new File[0];
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
	}

	@Override
	public void OnUpdated(CameraInfo camera) {
		if(mCamera.equals(camera)){
			mIsShowError = false;
			setResult(StreamMediaActivity.SUCCESS, getIntent());
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mRefreshBtnView.setEnabled(true);
					mImageView.setImageBitmap(mCamera.lastImage);
					mImageView.invalidate();
					setResult(StreamMediaActivity.SUCCESS, getIntent());
					if(mWaitDialog != null && mWaitDialog.isShowing()){
						mWaitDialog.dismiss();
						mWaitDialog = null;
						setFullScreenMode(true);
						
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
					}
				}
			});
		}
	}

	@Override
	public void onErrorOccurs(CameraInfo camera, final Exception exc) {
		if(mCamera.equals(camera)){
			mHandler.post(new Runnable() {
	
				@Override
				public void run() {
					if(mWaitDialog != null && mWaitDialog.isShowing()){
						mWaitDialog.dismiss();
						mWaitDialog = null;
					}
					mRefreshBtnView.setEnabled(true);
					if(mIsShowError){
						refreshToast(exc.getLocalizedMessage());
					}
				}
			});
		}
	}
}
