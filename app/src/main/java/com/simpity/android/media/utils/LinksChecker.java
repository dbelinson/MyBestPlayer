package com.simpity.android.media.utils;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.Vector;

import org.apache.http.util.EncodingUtils;

import com.simpity.android.media.storage.RecordBase;

public class LinksChecker implements Runnable {
	
	@SuppressWarnings("unused")
	private final static String TAG = "LinksStateChecker";
	
	public static int STATE_NOT_CHECKED		= 0;
	public static int STATE_CHECKING		= 1;
	public static int STATE_AVALIABLE		= 2;
	public static int STATE_NOT_AVALIABLE	= 3;
	
	private final static int CONNECT_TIMEOUT = 3500;
	
	//------------------------------------------------------------------------------------------------------------------
	public interface OnLinksCheckerListener {
    	public void onFinished();
    	public void onLinkChecked(RecordBase record, int checkState, int currentRecordNum, int totalRecCount);
    }

	private final OnLinksCheckerListener mListener;
	private final Vector<RecordBase> mLinks;
	private Thread mThread = null;
	
	//------------------------------------------------------------------------------------------------------------------
	private LinksChecker(Vector<RecordBase> links, OnLinksCheckerListener listener) {
		mListener = listener;
		mLinks = links;
	}

	//------------------------------------------------------------------------------------------------------------------
	public static LinksChecker CheckLinks(Vector<RecordBase> links, OnLinksCheckerListener listener) {
		LinksChecker checker = new LinksChecker(links, listener); 

		checker.mThread = new Thread(checker);
		checker.mThread.setPriority(Thread.NORM_PRIORITY);
		checker.mThread.start();
		
		return checker;
	}
	
	//--------------------------------------------------------------------------
	public void terminate() {
		if (mThread != null) {
			Thread thread = mThread;
			mThread = null;
			thread.interrupt();
		}
	}
	
	//--------------------------------------------------------------------------
	public boolean isChecking() {
		return mThread != null;
	}

	//--------------------------------------------------------------------------
	@Override
	public void run() {
		try {
			int currentNum = 0;
			int total = mLinks.size();
			byte[] buffer = new byte[100];
			StringBuilder packgBuilder = new StringBuilder();
			
			for (RecordBase recordBase : mLinks) {
				if (mThread == null) {
					return;
				}
				
				currentNum++;
				Thread.sleep(10);
				
				if (recordBase.isPaid()) {
					continue;
				}
				
				mListener.onLinkChecked(recordBase, STATE_CHECKING, currentNum, total);
				
				if (recordBase.getUrl().startsWith("rtsp://")) {
					try {
						URI uri = new URI(recordBase.getUrl());
						InetSocketAddress destAddr = new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 554);
						Socket rtspClientSocket = new Socket();
						rtspClientSocket.setSoTimeout(CONNECT_TIMEOUT);
						try {
							rtspClientSocket.connect(destAddr, CONNECT_TIMEOUT);
							Thread.sleep(10);
							packgBuilder.setLength(0);
							packgBuilder.append("OPTIONS ");
							packgBuilder.append(recordBase.getUrl());
							packgBuilder.append(" RTSP/1.0\r\n" + 
									"CSeq: 0\r\n" +
									"User-Agent: Stream Media Player (Android)\r\n\r\n");
							rtspClientSocket.getOutputStream().write(packgBuilder.toString().getBytes());
							int readedCount = rtspClientSocket.getInputStream().read(buffer);
							Thread.sleep(10);
							if (EncodingUtils.getString(buffer, 0, readedCount, "UTF-8").contains("RTSP/1.0 200 OK")) {
								mListener.onLinkChecked(recordBase, STATE_AVALIABLE, currentNum, total);
							} else {
								mListener.onLinkChecked(recordBase, STATE_NOT_AVALIABLE, currentNum, total);
							}

						} catch (InterruptedException e) {
							
							return;
							
						} finally {
							
							rtspClientSocket.close();
							
						}
					} catch (Exception e) {
						
						mListener.onLinkChecked(recordBase, STATE_NOT_AVALIABLE, currentNum, total);
					}
					
				} else if (recordBase.getUrl().startsWith("http://")) {
					
					HttpURLConnection connection = null;
					try {
						//Create connection
			    		connection = (HttpURLConnection)new URL(recordBase.getUrl()).openConnection();
			    		connection.setConnectTimeout(CONNECT_TIMEOUT);
			    		connection.setReadTimeout(CONNECT_TIMEOUT);
			    		connection.connect();
			    		Thread.sleep(10);
			    		int responceCode = connection.getResponseCode();
			    		if (responceCode >= 500 
			    				|| responceCode == HttpURLConnection.HTTP_FORBIDDEN 
			    				|| responceCode == HttpURLConnection.HTTP_GONE 
			    				|| responceCode == HttpURLConnection.HTTP_MOVED_PERM 
			    				|| responceCode == HttpURLConnection.HTTP_MOVED_TEMP 
			    				|| responceCode == HttpURLConnection.HTTP_NOT_FOUND) {
			    			mListener.onLinkChecked(recordBase, STATE_NOT_AVALIABLE, currentNum, total);
			    		} else {
			    			mListener.onLinkChecked(recordBase, STATE_AVALIABLE, currentNum, total);
			    		}
			    		
			        } catch (InterruptedException e) {
			        	
						return;
						
					} catch (Exception e) {
						
						mListener.onLinkChecked(recordBase, STATE_NOT_AVALIABLE, currentNum, total);
						
			    	} finally {
			    		
			    		if (connection != null) {
			    			connection.disconnect();
			    		}
			    	}
			    	
				} else {
					
					//UNKNOWN TYPE
					mListener.onLinkChecked(recordBase, STATE_AVALIABLE, currentNum, total);
				}
			}
			
		} catch (Exception e) {
		} finally {
			mThread = null;
			mListener.onFinished();
		}
	}
}
