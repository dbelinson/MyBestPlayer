package com.simpity.android.media.storage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import android.content.ContentValues;

import com.simpity.android.media.MediaService;

public class LinkTestThread implements Runnable {

	private final static String TAG = "LinkTestThread";
	
	private final static int CONNECT_TIMEOUT = 3500;
	
	private final StringBuilder mPackageBuilder = new StringBuilder();
	private final byte[] mBuffer = new byte[100];
	private final MediaService mMediaService;
	private Thread mThread = null; 
	private Socket mRtspClientSocket = null;
	private HttpURLConnection mConnection = null;
	private final TestListener mListener;
	
	public interface TestListener {
		public void onLinkTestFinished();
	}
	
	//--------------------------------------------------------------------------
	private LinkTestThread(MediaService service, TestListener listener) {
		mMediaService = service;
		mListener = listener;
	}

	//--------------------------------------------------------------------------
	public final static LinkTestThread start(MediaService service, TestListener listener) {
		LinkTestThread test = new LinkTestThread(service, listener);
		
		test.mThread = new Thread(test);
		test.mThread.start();
		
		return test;
	}
	
	//--------------------------------------------------------------------------
	public MediaService getContext() {
		return mMediaService;
	}

	//--------------------------------------------------------------------------
	@Override
	public void run() {
		
		Vector<LinkRecord> links = Storage.getAllLink(mMediaService);
		ContentValues values = new ContentValues();
		
		for (LinkRecord link : links) {
			
			if (isTerminate())
				break;
			
			try {
				int new_counter;
				
				if (testUrl(link.getUrl())) {
					new_counter = 0;
				} else {
					new_counter = link.getAvailableCounter() + 1;
				}
				
				link.setAvailableCounter(new_counter);
				
				if (link.isChanged()) {
					Storage.updateLink(mMediaService, link, values);
				}
				
			} catch (InterruptedException ex) {
				
				if (mMediaService.isDebuggable()) {
					ex.printStackTrace();
				}
			}
		}
		
		if (mListener != null) {
			mListener.onLinkTestFinished();
		}
	}
	
	//--------------------------------------------------------------------------
	public void terminate() {
		if (mThread != null) {
			if (mRtspClientSocket != null) {
				try {
					mRtspClientSocket.close();
				} catch (IOException ex) {
				}
			}
			
    		if (mConnection != null) {
    			mConnection.disconnect();
    		}

			mThread.interrupt();
			mThread = null;
		}
	}
	
	//--------------------------------------------------------------------------
	public boolean isTerminate() {
		return mThread == null;
	}
	
	//--------------------------------------------------------------------------
	public boolean testUrl(String url) throws InterruptedException {
		
		boolean result = false;
		
		if (url.startsWith("rtsp://")) {

			try {
				URI uri = new URI(url);
				InetSocketAddress destAddr = new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 554);
				
				mRtspClientSocket = new Socket();
				mRtspClientSocket.setSoTimeout(CONNECT_TIMEOUT);
				mRtspClientSocket.connect(destAddr, CONNECT_TIMEOUT);
				
				Thread.sleep(10);
				
				mPackageBuilder.setLength(0);
				mPackageBuilder.append("OPTIONS ");
				mPackageBuilder.append(url);
				mPackageBuilder.append(" RTSP/1.0\r\n" + 
						"CSeq: 0\r\n" +
						"User-Agent: Stream Media Player (Android)\r\n\r\n");
				
				mRtspClientSocket.getOutputStream().write(mPackageBuilder.toString().getBytes());
				
				int len = mRtspClientSocket.getInputStream().read(mBuffer);
				if (len >= 15 
						&& mBuffer[0] == 'R'
						&& mBuffer[1] == 'T'
						&& mBuffer[2] == 'S'
						&& mBuffer[3] == 'P'
						&& mBuffer[4] == '/'
						&& mBuffer[8] == ' ') {
					
					int pos = 9, code = 0;
					while (pos < len && mBuffer[pos] == ' ') {
						pos++;
					}
					
					if (pos < len) {
						while (mBuffer[pos] != ' ') {
							if (pos == len || mBuffer[pos] < '0' || mBuffer[pos] > '9') {
								code = 0;
								break;
							}
							
							code = code * 10 + (mBuffer[pos] - '0');
							pos++;
						}
					}
				
					result = (code == 200);
				}
				
			} catch (IOException ex) {

				if (mMediaService.isDebuggable()) {
	    			ex.printStackTrace();
	    		}
				
			} catch (URISyntaxException ex) {

				if (mMediaService.isDebuggable()) {
	    			ex.printStackTrace();
	    		}
				
	    	} catch (NullPointerException ex) {
	    		
	    		if (mMediaService.isDebuggable()) {
	    			ex.printStackTrace();
	    		}
				
			} finally {
				
				if (mRtspClientSocket != null) {
					try {
						mRtspClientSocket.close();
					} catch (IOException ex) {
						if (mMediaService.isDebuggable()) {
			    			ex.printStackTrace();
			    		}
					}
					mRtspClientSocket = null;
				}
			}
			
		} else if (url.startsWith("http://")) {
			
			try {
				//Create connection
	    		mConnection = (HttpURLConnection)new URL(url).openConnection();
	    		mConnection.setConnectTimeout(CONNECT_TIMEOUT);
	    		mConnection.setReadTimeout(CONNECT_TIMEOUT);
	    		mConnection.connect();
	    		
	    		Thread.sleep(10);
	    		
	    		result = (mConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
	    		
	    	} catch (IOException e) {
	    		
	    		if (mMediaService.isDebuggable()) {
	    			e.printStackTrace();
	    		}
				
	    	} catch (NullPointerException e) {
	    		
	    		if (mMediaService.isDebuggable()) {
	    			e.printStackTrace();
	    		}
	    		
			} finally {
	    		
	    		if (mConnection != null) {
	    			mConnection.disconnect();
	    			mConnection = null;
	    		}
	    	}
		}
		
		mMediaService.debugInfo(TAG, (result ? "Valid URL: " : "Invalid URL: ") + url);
		
		return result;
	}
}
