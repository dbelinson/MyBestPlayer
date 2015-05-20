package com.simpity.android.media.widgets.camera;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.Utilities;
import com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface;

public class WebCamRefreshService extends Service {

	private final static String TAG = "WebCamRefreshService";

	private static WebCamRefreshService mWebCamService;

	private Vector<CameraInfo> mActiveCamsList = null;
	private Vector<UpdateListener> mCamsUpdateListeners = null;

	int DisplayWidth = 854;
	int DisplayHeight = 480;

	//--------------------------------------------------------------------------
	public class CameraInfo {
		public String url;
		public int refreshPeriod;
		public Bitmap lastImage;
		private boolean stopRefreshing = false;
		private Object refreshImageWait = new Object();

		public String userName, password = null;

		public CameraInfo(String url, int refreshTime, String username, String password) {
			this.url = url;
			this.refreshPeriod = refreshTime;
			this.lastImage = BitmapFactory.decodeResource(getResources(), Res.drawable.icon_jpeg);
			this.userName = username;
			this.password = password;
		}
	}

	//--------------------------------------------------------------------------
	public interface UpdateListener {
		void OnUpdated(CameraInfo camera);
		void onErrorOccurs(CameraInfo camera, Exception exc);
	}

	//--------------------------------------------------------------------------
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	//--------------------------------------------------------------------------
	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate");
		super.onCreate();
		mActiveCamsList = new Vector<CameraInfo>();
		mCamsUpdateListeners = new Vector<UpdateListener>();
		mWebCamService = this;

		WindowManager wm = (WindowManager) getSystemService(Activity.WINDOW_SERVICE);
		DisplayWidth = wm.getDefaultDisplay().getWidth();
		DisplayHeight = wm.getDefaultDisplay().getHeight();
		wm = null;
	}

	//--------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		for (CameraInfo camera : mActiveCamsList) {
			stopRefreshCamera(camera);
		}
		mWebCamService = null;
	}

	//--------------------------------------------------------------------------
	public static WebCamRefreshService getServiceInstance() {
		return mWebCamService;
	}

	//--------------------------------------------------------------------------
	private class MyAuthenticator extends Authenticator {

		private CameraInfo mCameraInfo;

		public MyAuthenticator(CameraInfo cameraInfo) {
			mCameraInfo = cameraInfo;
		}

		public PasswordAuthentication getPasswordAuthentication() {
			try {
				return (new PasswordAuthentication(mCameraInfo.userName,
						mCameraInfo.password.toCharArray()));
			} finally {
				Authenticator.setDefault(null);
			}
		}
	}

	//--------------------------------------------------------------------------
	public void startRefreshCamera(final CameraInfo camera, UpdateListener listener) {
		
		Log.i(TAG, "startRefreshCamera");
		if (!mCamsUpdateListeners.contains(listener)) {
			mCamsUpdateListeners.add(listener);
		}

		camera.stopRefreshing = false;
		if (!mActiveCamsList.contains(camera)) {
			new Thread(new RefreshCameraRunnable(camera)).start();
			mActiveCamsList.add(camera);
		}
	}

	//--------------------------------------------------------------------------
	public void stopRefreshCamera(CameraInfo camera) {
		Log.i(TAG, "stopRefreshCamera");
		camera.stopRefreshing = true;
		mActiveCamsList.remove(camera);
	}

	//--------------------------------------------------------------------------
	public void requestRefreshImmediate(CameraInfo camera) {
		synchronized (camera.refreshImageWait) {
			camera.refreshImageWait.notify();
		}
	}

	//--------------------------------------------------------------------------
	private final IWebCamRefreshServiceInterface.Stub mBinder = new IWebCamRefreshServiceInterface.Stub() {
	};

	//--------------------------------------------------------------------------
	private class RefreshCameraRunnable implements Runnable {

		private final CameraInfo mCamera;

		RefreshCameraRunnable(CameraInfo camera) {
			mCamera = camera;
		}

		@Override
		public void run() {
			while (!mCamera.stopRefreshing) {
				InputStream stream = null;
				ByteArrayBuffer buffer = null;
				try {
					if (mCamera.password != null) {
						Authenticator.setDefault(new MyAuthenticator(mCamera));
					}

					URLConnection connection = new URL(mCamera.url).openConnection();

					if (mCamera.stopRefreshing)
						break;

					connection.setConnectTimeout(3000);
					connection.connect();

					if (mCamera.stopRefreshing)
						break;

					stream = connection.getInputStream();

					if (mCamera.stopRefreshing)
						break;

					int readed = 0;

					int contentLength = connection.getContentLength();

					if (mCamera.stopRefreshing)
						break;

					int availiable = stream.available();

					if (mCamera.stopRefreshing)
						break;

					if (availiable <= 0)
						availiable = 8 * 1024;

					byte[] localBuf = new byte[availiable];

					buffer = new ByteArrayBuffer(contentLength > 0 ? contentLength : availiable);

					while ((readed = stream.read(localBuf, 0, availiable)) != -1 
							&& !mCamera.stopRefreshing) {
						buffer.append(localBuf, 0, readed);
					}

					localBuf = null;

					if (mCamera.stopRefreshing)
						break;

				} catch (MalformedURLException e) {
					
					for (UpdateListener listener : mCamsUpdateListeners) {
						listener.onErrorOccurs(mCamera, new Exception(
								getString(Res.string.invalid_url_error, mCamera.url)));
					}
					
				} catch (Exception e) {
					
					for (UpdateListener listener : mCamsUpdateListeners) {
						listener.onErrorOccurs(mCamera, new Exception(
								getString(Res.string.read_error, mCamera.url)));
					}
					
				} catch (OutOfMemoryError e) {
					
					for (UpdateListener listener : mCamsUpdateListeners) {
						listener.onErrorOccurs(mCamera, new Exception(e.getMessage()));
					}
					
				} finally {
					
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {
							Log.d(TAG, e.toString());
						}
					}
				}

				if (mCamera.stopRefreshing)
					break;

				if (buffer != null) {
					Bitmap mBitmap = Utilities.loadBitmap(buffer.buffer(), DisplayWidth, DisplayHeight);
					buffer = null;
					//System.gc();

					if (mBitmap != null) {
						mCamera.lastImage = mBitmap;
						if (mCamera.stopRefreshing)
							break;
						
						for (UpdateListener listener : mCamsUpdateListeners) {
							listener.OnUpdated(mCamera);
						}
						
					} else {
						
						for (UpdateListener listener : mCamsUpdateListeners) {
							listener.onErrorOccurs(mCamera, new Exception(
									getString(Res.string.read_error, mCamera.url)));
						}
					}
				}
				
				synchronized (mCamera.refreshImageWait) {					
					try {
						mCamera.refreshImageWait.wait(mCamera.refreshPeriod * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
			}
			
			Log.v(TAG, "Camera Refresh Stopped");
		}
	}
}
