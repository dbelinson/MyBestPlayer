package com.simpity.android.media.services;

import java.io.StringReader;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.simpity.android.media.MediaService;
import com.simpity.android.media.storage.CameraRecord;
import com.simpity.android.media.storage.RadioRecord;
import com.simpity.android.media.storage.RecordBase;
import com.simpity.android.media.storage.RecordsManager;
import com.simpity.android.media.storage.Storage;
import com.simpity.android.media.storage.VideoRecord;

public class UpdateService implements Runnable, ContentHandler {

	private final static String TAG = "UpdateService";

	public final static int VIDEO_LINK			= 0;
	public final static int RADIO_LINK			= 1;
	public final static int CAMERA_LINK			= 2;
	public final static int NEW_LINK			= 0;
	public final static int DEAD_LINK			= 3;
	public final static int NEW_VIDEO_LINK		= NEW_LINK + VIDEO_LINK;
	public final static int NEW_RADIO_LINK		= NEW_LINK + RADIO_LINK;
	public final static int NEW_CAMERA_LINK		= NEW_LINK + CAMERA_LINK;
	public final static int DEAD_VIDEO_LINK		= DEAD_LINK + VIDEO_LINK;
	public final static int DEAD_RADIO_LINK		= DEAD_LINK + RADIO_LINK;
	public final static int DEAD_CAMERA_LINK	= DEAD_LINK + CAMERA_LINK;

	public final static int UPDATE_STATE_IDLE	= 0;
	public final static int UPDATE_STATE_CONNECTING	= 1;
	public final static int UPDATE_STATE_PARSING	= 2;

	private final static String UPDATE_VERSION = "UPDATE_VERSION";
	private final static String UPDATE_DATE = "UPDATE_DATE";
	private final static String LINKS_COUNT = "LINKS_COUNT";

	private final static String STR_TYPE_RADIO	= "Internet Radio";
	private final static String STR_TYPE_VIDEO	= "Internet Video";
	private final static String STR_TYPE_CAMERA	= "JPEG Web Camera";

	private static final String POSTS_TAG			= "Posts";
	private static final String CURRENT_VER_ATTR	= "currentVer";
	private static final String POST_TAG			= "Post";
	private static final String DATE_TAG			= "Date";
	private static final String URL_TAG			    = "Url";
	private static final String POST_TYPE_TAG		= "PostType";
	private static final String CAT_TAG 			= "Cat";
	private static final String DESCR_TAG			= "Descr";
	private static final String EMAIL_TAG	        = "Email";
	private static final String STATUS_TAG	        = "Status";

	private static final String STATUS_APPROVED     = "approved";
	private static final String STATUS_DELETED      = "deleted";

	@SuppressWarnings("unused")
	private final static int DATE_INDEX			= 0;
	private final static int URL_INDEX			= 1;
	private final static int POST_TYPE_INDEX	= 2;
	private final static int CATEGORY_INDEX		= 3;
	private final static int DESCRIPTION_INDEX	= 4;
	@SuppressWarnings("unused")
	private final static int EMAIL_INDEX		= 5;
	private final static int STATUS_INDEX		= 6;
	private final static int INDEX_COUNT		= 7;

	private final static String[] DATA_TAGS = {
		DATE_TAG,
		URL_TAG,
		POST_TYPE_TAG,
		CAT_TAG,
		DESCR_TAG,
		EMAIL_TAG,
		STATUS_TAG
	};

	private final String[] mData = new String[INDEX_COUNT];

	private int mState = UPDATE_STATE_IDLE;
	private Thread hThread = null;

	private boolean mContentElement = false;
	private boolean mIsNew, mIsDead;
	private int nCurrentIndex = -1;

	private final Vector<RecordBase> mResult = new Vector<RecordBase>();
	private int mResultVersion = -1;
	private int[] mLinkCount = new int[6];
	@SuppressWarnings("unused")
	private boolean mSuccessUpdate;
	private RecordsManager mRecordsManager;
	
	private final MediaService mMediaService; 

	private Timer mTimer;

	//-------------------------------------------------------------------------
	public UpdateService(MediaService media_service) {
		mMediaService = media_service;

		mTimer = new Timer();
		mTimer.schedule(new UpdateTask(), 100, 60*60*1000);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		String link_count = prefs.getString(LINKS_COUNT, null);
		if (link_count != null) {
			Scanner scanner = new Scanner(link_count);
			scanner.useDelimiter(";");
			for (int i=0; i<mLinkCount.length && scanner.hasNext(); i++) {
				try {
					mLinkCount[i] = Integer.parseInt(scanner.next());
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	public void onDestroy() {
		try {
			mTimer.cancel();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		mTimer = null;
	}

	//-------------------------------------------------------------------------
	public int getUpdateState() {
		return mState;
	}

	//---------------------------------------------------------------------
	public int getLinkCount(int type) {
		int count;

		if (type >= 0 && type < mLinkCount.length) {
			count = mLinkCount[type];
			mLinkCount[type] = 0;
			
			if (count > 0) {
				saveLinkCount();
			}
		} else {
			count = 0;
		}

		return count;
	}

	//-------------------------------------------------------------------------
	private class UpdateTask extends TimerTask {
		
		@Override
		public void run() {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
			long last_update = prefs.getInt(UPDATE_VERSION, 0);

			if (System.currentTimeMillis() - last_update > 24*60*60*1000 && hThread == null) {
				hThread = new Thread(UpdateService.this);
				hThread.start();
			}
		}
	}

	//-------------------------------------------------------------------------
	private void saveLinkCount() {
		StringBuilder builder = new StringBuilder();

		for (int count : mLinkCount) {
			builder.append(count);
			builder.append(';');
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(LINKS_COUNT, builder.toString());
		editor.commit();
	}

	//--------------------------------------------------------------------------
	private int getUpdateVersion() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		return prefs.getInt(UPDATE_VERSION, 0);
	}

	//--------------------------------------------------------------------------
	private void setUpdateVersion(int version) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mMediaService);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putInt(UPDATE_VERSION, version);
		editor.putLong(UPDATE_DATE, System.currentTimeMillis());
		editor.commit();
	}

	//--------------------------------------------------------------------------
	private void setState(int state) {
		mState = state;
		mMediaService.sendToUi(MediaService.ACTION_UPDATE_STATE_CHANGED, Integer.toString(state));
		// TODO
	}

	//--------------------------------------------------------------------------
	private String normalizeXml(String xml) {
		
		if (mMediaService.isDebuggable())
			Log.i(TAG, "normalizeXml started");
		
		StringBuilder buffer = new StringBuilder();
		int start = 0;
		int pos = xml.indexOf('&');
		boolean valid;
		
		while (pos >= 0) {
			if (pos > start)
				buffer.append(xml.substring(start, pos));
			
			valid = false;
			
			switch (xml.charAt(pos+1)) {
			case 'a':
				valid = ((xml.charAt(pos+2) == 'm'
						&& xml.charAt(pos+3) == 'p'
						&& xml.charAt(pos+4) == ';') 
					|| (xml.charAt(pos+2) == 'p'
						&& xml.charAt(pos+3) == 'o'
						&& xml.charAt(pos+4) == 's'
						&& xml.charAt(pos+5) == ';'));
				break;
				
			case 'g':
			case 'l':
				valid = (xml.charAt(pos+2) == 't' && xml.charAt(pos+3) == ';');
				break;
				
			case 'q':
				valid = (xml.charAt(pos+2) == 'u'
						&& xml.charAt(pos+3) == 'o'
						&& xml.charAt(pos+4) == 't'
						&& xml.charAt(pos+5) == ';');
				break;
			}

			if (valid) {
				buffer.append('&');
			} else {
				buffer.append("&amp;");
			}
			
			start = pos + 1;
			pos = xml.indexOf('&', start);
		}
		
		if (start < xml.length())
			buffer.append(xml.substring(start));
		
		if (mMediaService.isDebuggable())
			Log.i(TAG, "normalizeXml finished");
		
		return buffer.toString();
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void run() {

		mSuccessUpdate = false;
		
		mRecordsManager = new RecordsManager(mMediaService);
		mResultVersion = -1;
		mResult.clear();

		HttpClient http_client = null;
		StringBuilder request_builder = new StringBuilder();

		request_builder.append("http://www.psa-mobile.com/android/smp/getposts.php?fromVer=");
		request_builder.append(getUpdateVersion());
		request_builder.append("&ptype=*");

		String request = request_builder.toString();

		try {
			setState(UPDATE_STATE_CONNECTING);

			if (mMediaService.isDebuggable())
				Log.d(TAG, request);
			
			http_client = new DefaultHttpClient();
			String response_body = http_client.execute(new HttpPost(request),
					new BasicResponseHandler());

			int xml_start = response_body.indexOf("<?xml");
			if (xml_start < 0) {
				
				if (mMediaService.isDebuggable()) 
					Log.e(TAG, response_body);

				return;
			}
			
			if (xml_start > 0)
			{
				int last_tag_pos = response_body.indexOf("</Posts>");
				
				if (last_tag_pos < 0)
					response_body = response_body.substring(xml_start);
				else
					response_body = response_body.substring(xml_start, last_tag_pos + 8);
			}

			response_body = normalizeXml(response_body);
			
			if (mMediaService.isDebuggable())
				Log.d(TAG, response_body);
			
			http_client.getConnectionManager().shutdown();
			http_client = null;

			setState(UPDATE_STATE_PARSING);

			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader xml_reader = parser.getXMLReader();
			xml_reader.setContentHandler(this);
			xml_reader.parse(new InputSource(new StringReader(response_body)));

			if (mResult.size() > 0) {
				Vector<RecordBase> ignored = Storage.addRecordsByUpdate(mMediaService, mResult);
				for (RecordBase record : ignored) {
					if (record instanceof VideoRecord) {
						mLinkCount[DEAD_VIDEO_LINK]--;
					} else if (record instanceof RadioRecord) {
						mLinkCount[DEAD_RADIO_LINK]--;
					} else if (record instanceof CameraRecord) {
						mLinkCount[DEAD_CAMERA_LINK]--;
					}
				}

				mResult.clear();
			}

			mSuccessUpdate = true;

			saveLinkCount();
			
			if (mResultVersion >= 0) {
				setUpdateVersion(mResultVersion);
			}

		} catch (Exception e) {

			if (mMediaService.isDebuggable())
				e.printStackTrace();

		} catch (OutOfMemoryError e) {

			if (mMediaService.isDebuggable())
				e.printStackTrace();

		} finally {

			if (http_client != null && http_client.getConnectionManager() != null) {
				http_client.getConnectionManager().shutdown();
			}

			mRecordsManager.destroy();
			mRecordsManager = null;

			setState(UPDATE_STATE_IDLE);
			hThread = null;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void startDocument() throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		nCurrentIndex = -1;
		if (mContentElement) {

			for (int i=0; i<INDEX_COUNT; i++) {
				if (localName.equalsIgnoreCase(DATA_TAGS[i])) {
					nCurrentIndex = i;
					break;
				}
			}

		} else if (localName.equalsIgnoreCase(POST_TAG)) {

			mContentElement = true;

		} else if (localName.equalsIgnoreCase(POSTS_TAG))
		{
			try
			{
				mResultVersion = Integer.parseInt(atts.getValue(CURRENT_VER_ATTR));
			}
			catch(NumberFormatException e)
			{
				Log.d(TAG, e.getMessage());
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void characters(char[] buffer, int startIndex, int length) throws SAXException {
		if (buffer != null && startIndex >= 0 && length > 0 && nCurrentIndex > 0) {
			String data = String.copyValueOf(buffer, startIndex, length);

			if (nCurrentIndex >= 0 && nCurrentIndex < INDEX_COUNT) {
				if (mData[nCurrentIndex] == null) {
					mData[nCurrentIndex] = data;
				} else {
					mData[nCurrentIndex] = mData[nCurrentIndex].concat(data);
				}

				if (nCurrentIndex == STATUS_INDEX) {

					if (mData[STATUS_INDEX].equalsIgnoreCase(STATUS_APPROVED)) {
						mIsNew = true;
					} else if (mData[STATUS_INDEX].equalsIgnoreCase(STATUS_DELETED)) {
						mIsDead = true;
					}
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {

		if (localName.equalsIgnoreCase(POST_TAG)) {

			mContentElement = false;

			if (mData[URL_INDEX] != null) {
				RecordBase record = null;

				if (STR_TYPE_RADIO.equalsIgnoreCase(mData[POST_TYPE_INDEX])) {

					record = new RadioRecord(mRecordsManager, -1, mData[DESCRIPTION_INDEX],
								mData[URL_INDEX], null, mData[CATEGORY_INDEX],
								false, -1, -1, mIsNew, mIsDead);

					if (mIsNew) {
						mLinkCount[NEW_RADIO_LINK]++;
					}

					if (mIsDead) {
						mLinkCount[DEAD_RADIO_LINK]++;
					}

				} else if (STR_TYPE_VIDEO.equalsIgnoreCase(mData[POST_TYPE_INDEX])) {

					record = new VideoRecord(mRecordsManager, -1, mData[DESCRIPTION_INDEX],
								mData[URL_INDEX], mData[CATEGORY_INDEX],
								false, -1, -1, mIsNew, mIsDead);

					if (mIsNew) {
						mLinkCount[NEW_VIDEO_LINK]++;
					}

					if (mIsDead) {
						mLinkCount[DEAD_VIDEO_LINK]++;
					}

				} else if (STR_TYPE_CAMERA.equalsIgnoreCase(mData[POST_TYPE_INDEX])) {

					record = new CameraRecord(mRecordsManager, -1, mData[DESCRIPTION_INDEX],
								mData[URL_INDEX], 10, false, -1, -1, mIsNew, mIsDead);

					if (mIsNew) {
						mLinkCount[NEW_CAMERA_LINK]++;
					}

					if (mIsDead) {
						mLinkCount[DEAD_CAMERA_LINK]++;
					}
				}

				if (record != null) {
					mResult.add(record);
				}
			}

			for (int i=0; i<INDEX_COUNT; i++) {
				mData[i] = null;
			}

			mIsNew = false;
			mIsDead = false;

		} else {

			if (nCurrentIndex >= 0 && localName.equalsIgnoreCase(DATA_TAGS[nCurrentIndex])) {
				nCurrentIndex = -1;
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public void endDocument() throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void endPrefixMapping(String arg0) throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void processingInstruction(String arg0, String arg1) throws SAXException {
	}

	//--------------------------------------------------------------------------
	@Override
	public void setDocumentLocator(Locator arg0) {
	}

	//--------------------------------------------------------------------------
	@Override
	public void skippedEntity(String name) throws SAXException {
	}
}
