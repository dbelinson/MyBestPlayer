package com.simpity.android.media.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.Downloader.OnRetriveDownloadInfoListener.DownloadProgressInfo;

//--------------------------------------------------------------------------
//--------------------------------------------------------------------------
public class Downloader {
	
	private Thread mDownloadThread = null;
	private boolean mStopDownload = false;
	private String mSrc = null;
	private Context mContext = null;
	
	private String TAG = "Downloader";
	
	public interface OnRetriveDownloadInfoListener {
    	
		public void DownloadInfoRetrived(DownloadProgressInfo info);  
    	
    	public class DownloadProgressInfo{
    		public String file_name = "Unknown";
    		public String content_type = null;
    		public int target_file_size = 0;
    		public int downloaded_size = 0;
    		public boolean isErrorOccured = false;
    		public String error_message = null;
    		public boolean isDownloadCompleted = false;
    		public File targetFile = null;
    		public boolean isDownloadInterrupted = false;
    	}
	}
	
	public Downloader(Context context, String connectionUrl){
		mSrc = connectionUrl;
		mContext = context;
	}
	
	public void StartDownload(final OnRetriveDownloadInfoListener listener){
		mDownloadThread = null;
		mDownloadThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Log.d(TAG, "Downloader.run");
				
				DownloadProgressInfo fInfo = new DownloadProgressInfo();
				int pos = mSrc.indexOf('/') + 1;
				int pos2 = mSrc.indexOf('/', pos);
				
				while (pos2 > 0) {
					pos = pos2 + 1;
					pos2 = mSrc.indexOf('/', pos);
				}
				fInfo.file_name = mSrc.substring(pos);
				String mFilename = fInfo.file_name;
				URL url;
				try{
					url = new URL(mSrc);
				}catch (MalformedURLException e) {
					Log.d(TAG, "Invalid link");
					if(listener != null){
						fInfo.isErrorOccured = true;
						fInfo.error_message = "Invalid link";
						listener.DownloadInfoRetrived(fInfo);
					}
					return;
				}
				URLConnection mURLConnection;
				try{
					mURLConnection = url.openConnection();
				}catch (IOException e) {
					Log.d(TAG, "Open link failed");
					if(listener != null){
						fInfo.isErrorOccured = true;
						fInfo.error_message = "Open link failed";
						listener.DownloadInfoRetrived(fInfo);
					}
					return;
				}
				
				mURLConnection.setDoInput(true); 
				mURLConnection.setUseCaches(true);
				mURLConnection.setConnectTimeout(3000);
				try {
					mURLConnection.connect();
				} catch (IOException e1) {
					Log.d(TAG, "Connect link failed");
					if(listener != null){
						fInfo.isErrorOccured = true;
						fInfo.error_message = "Connect link failed";
						listener.DownloadInfoRetrived(fInfo);
					}
					return;
				}
				fInfo.content_type = mURLConnection.getContentType();
				fInfo.target_file_size = mURLConnection.getContentLength();
				if(listener != null)
					listener.DownloadInfoRetrived(fInfo);
				
				InputStream mStream = null;
				File mTempFile = null;
				try {
					String state = Environment.getExternalStorageState();
					if (!Environment.MEDIA_MOUNTED.equals(state)) {
						Log.d(TAG, "Downloader interrupted. Reason - No SD Card.");
						if(listener != null){
							fInfo.isErrorOccured = true;
							fInfo.error_message = "Downloader interrupted. Reason - No SD Card.";
							listener.DownloadInfoRetrived(fInfo);
						}
						return;
				    }
					
				   mTempFile = new File(Environment.getExternalStorageDirectory(), "download/" + mContext.getString(Res.string.app_name));
				    if(!mTempFile.exists())
				    	mTempFile.mkdirs();
				    mTempFile = new File(mTempFile.getPath() , mFilename);
				    if(mTempFile.exists()){
				    	if(mTempFile.length() == fInfo.target_file_size){
				    		if(listener != null){
								fInfo.isDownloadCompleted = true;
								fInfo.targetFile = mTempFile;
								listener.DownloadInfoRetrived(fInfo);
							}
				    		return;
				    	}	
				    }
				    mTempFile.createNewFile();
				    mTempFile.deleteOnExit();
				    fInfo.targetFile = mTempFile;
					
					FileOutputStream output = new FileOutputStream(mTempFile);
					
					int output_size = 0, size;
					
					mStream = mURLConnection.getInputStream();
					if(mStream == null){
						Log.d(TAG, "Failed to Download file");
						if(listener != null){
							fInfo.error_message = "Failed to Download file";
							fInfo.isErrorOccured = true;
							listener.DownloadInfoRetrived(fInfo);
						}
						return;
					}
					int retry_count = 0;
					while (mStream.available() >= 0 && !mStopDownload) {
						
						int avail_bytes = mStream.available();
		                if(avail_bytes == 0 && retry_count < 5){
		                    if(fInfo.target_file_size > output_size){
		                        try{
		                            Thread.sleep(200);
		                        }
		                        catch(InterruptedException e){
		                        	if(listener != null){
										fInfo.isDownloadInterrupted = true;
										listener.DownloadInfoRetrived(fInfo);
									}
		                        	return;
		                        }
		                        retry_count++;
		                        continue;
		                    }
		                }
		                else
		                    retry_count = 0;
		               byte[] buffer = new byte[avail_bytes];
		               
		               size = mStream.read(buffer);
						
						if (size > 0) {
							output.write(buffer, 0, size);
							output_size += size;
						} else {
							break;
						}
						
						Log.d(TAG, "Loaded " + output_size + "bytes");
						if(listener != null){
							fInfo.downloaded_size = output_size;
							listener.DownloadInfoRetrived(fInfo);
						}
					}
					output.flush();
					output.close();
					if (!mStopDownload){
						if(fInfo.target_file_size == output_size){
							Log.d(TAG, "Download complete");
							if(listener != null){
								fInfo.downloaded_size = output_size;
								fInfo.isDownloadCompleted = true;
								listener.DownloadInfoRetrived(fInfo);
							}
						}else{
							Log.d(TAG, "Failed to Download file");
							if(listener != null){
								fInfo.error_message = "Failed to Download file";
								fInfo.isErrorOccured = true;
								listener.DownloadInfoRetrived(fInfo);
							}
						}
					}else{
						Log.d(TAG, "Download interrupted");
						if(listener != null){
							fInfo.isDownloadInterrupted = true;
							listener.DownloadInfoRetrived(fInfo);
						}
					}		
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
					
					if (mTempFile != null) {
						mTempFile.delete();
						mTempFile = null;
					}
					if(listener != null){
						fInfo.error_message = e.getMessage();
						fInfo.isErrorOccured = true;
						listener.DownloadInfoRetrived(fInfo);
					}
				} finally {
					try {
						if(mStream != null) {
							mStream.close();
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					mURLConnection = null;
					mDownloadThread = null;
				}
			}
		});
		mDownloadThread.setPriority(Thread.MIN_PRIORITY);
		mDownloadThread.start();
	}

	public void StopDownload(){
		mStopDownload = true;
		if(mDownloadThread != null && mDownloadThread.isAlive())
			mDownloadThread.interrupt();
	}
}
