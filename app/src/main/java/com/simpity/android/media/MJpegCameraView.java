package com.simpity.android.media;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.Message;
import com.simpity.android.media.utils.Utilities;
import com.simpity.android.protocol.Rtp;
import com.simpity.android.protocol.RtpPacketHandler;

public class MJpegCameraView extends Activity implements RtpPacketHandler,
		MediaScannerConnectionClient {

	private final static String LOG_TAG = MJpegCameraView.class.getSimpleName();

	final static public String PORT_DATA = "port";
	private int mPort;

	private Rtp mRtp;
	private SurfaceHolder mHolder;
	private byte[] buffer = new byte[188];
	private byte[] data = new byte[512 * 1024];
	private int data_size = 0;
	private boolean fLost = false;
	private Bitmap currentFrame = null;
	private MediaScannerConnection mMediaScanner = null;
	private File[] filesToAddToMedia = new File[0];
	
	private Ad mAdView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Log.d("MJpegCameraView", "onCreate point 1");
		Intent intent = getIntent();

		//Log.d("MJpegCameraView", "onCreate point 2");

		// setTitle(intent.getStringExtra(MJpegEditActivity.DESCRIPTION_DATA));
		setContentView(Res.layout.mjpeg_view);

		if(getWindowManager().getDefaultDisplay().getOrientation()== 0) {
			Ad.Visible(this);
		} else {
			Ad.Gone(this);
		}

		Log.d("MJpegCameraView", "onCreate point 3");

		String port = intent.getStringExtra(PORT_DATA);
		mPort = Integer.parseInt(port);

		Log.d("MJpegCameraView", "onCreate point 4");

		SurfaceView surface = (SurfaceView) findViewById(Res.id.mjpeg_camera_view);
		mHolder = surface.getHolder();

		Log.d("MJpegCameraView", "onCreate point 5");
		mMediaScanner = new MediaScannerConnection(MJpegCameraView.this,
				MJpegCameraView.this);
		setResult(RESULT_OK, getIntent());
		
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
	
	@Override
    protected void onDestroy() {
		
		if (mAdView != null) {
			mAdView.destroy();
		}

    	super.onDestroy();
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("MJpegCameraView", "onStart");
		mRtp = new Rtp(mPort);
		mRtp.setRtpPacketHandler(this);
		data_size = 0;

		try {
			mRtp.start(null, -1);
		} catch (SocketException e) {
			rtpStartException(e);
		} catch (UnknownHostException e) {
			rtpStartException(e);
		}
	}

	private void rtpStartException(Exception e) {
		Log.e(LOG_TAG, e.toString());
		mRtp = null;
		Message.show(this, Res.string.error, "RTP start error:\n"
				+ e.getMessage(), new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				setResult(RESULT_CANCELED, getIntent());
				MJpegCameraView.this.finish();
				/*
				 * (new Handler()).post(new Runnable() {
				 *
				 * @Override public void run() { MJpegCameraView.this.finish();
				 * } });
				 */
			}
		});
	}

	@Override
	protected void onStop() {
		Log.d("MJpegCameraView", "onStop");

		if (mRtp != null) {
			mRtp.stop();
			mRtp = null;
		}
		if (mMediaScanner.isConnected())
			mMediaScanner.disconnect();
		super.onStop();
	}

	private Paint mPaint = new Paint();

	@Override
	public void RtpPacketHandle(byte[] buf, int start, int length) {
		ByteArrayInputStream stream = new ByteArrayInputStream(buf, start,
				length);
		int off;

		while (stream.available() >= 188) {
			stream.read(buffer, 0, 4);

			if ((buffer[1] & 0x40) != 0) {
				if (data_size > 0) {
					if (!fLost) {
						int n = 0;
						while (n < data_size) {
							if (data[n] == -1 && data[n + 1] == -40) {
								Bitmap bmp = BitmapFactory.decodeByteArray(
										data, n, data_size - n);
								currentFrame = bmp;
								//SaveSnapshot();
								if (bmp != null) {
									Canvas canvas = mHolder.lockCanvas();
									if (canvas != null) {
										canvas.drawBitmap(bmp, 0, 0, mPaint);
										mHolder.unlockCanvasAndPost(canvas);
									}
								}
								break;
							}
							n++;
						}
					}
					data_size = 0;
					fLost = false;
				}
			}

			if ((buffer[3] & 0x20) != 0) {
				off = stream.read();
				if(off > 0 && buffer.length >= 5 + off)
					stream.read(buffer, 5, off);
				off += 5;
			} else
				off = 4;

			if (!fLost)
				data_size += stream.read(data, data_size, 188 - off);
			else
				stream.skip(188 - off);
		}

		fLost = false;
	}

	public Boolean SaveSnapshot() {

		File imageFile = Utilities.getNewSnapshotFile("MJpegCamera");
		boolean result = SaveCurrentFrame(imageFile);
		if (result) {
			if (mMediaScanner.isConnected()) {
				
				mMediaScanner.scanFile(imageFile.getAbsolutePath(), "image/jpeg");
				
			} else {
				
				File[] tmp = filesToAddToMedia;
				filesToAddToMedia = new File[filesToAddToMedia.length + 1];
				
				System.arraycopy(tmp, 0, filesToAddToMedia, 0, tmp.length);
				/*for (int i = 0; i < tmp.length; i++) {
					filesToAddToMedia[i] = tmp[i];
				}*/
				
				filesToAddToMedia[filesToAddToMedia.length - 1] = imageFile;
				mMediaScanner.connect();
			}
		}
		return result;
	}

	private Boolean SaveCurrentFrame(File imgToSave) {
		if (currentFrame != null) {
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
					currentFrame.compress(CompressFormat.JPEG, 100, fOut);
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
			else
				return false;
		} else
			return false;
	}

	@Override
	public void RtpPacketLost(int count) {
		data_size = 0;
		fLost = true;
	}

	@Override
	public void onMediaScannerConnected() {
		for (File f : filesToAddToMedia) {
			mMediaScanner.scanFile(f.getAbsolutePath(), "image/jpeg");
		}
		filesToAddToMedia = new File[0];
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
	}
}
