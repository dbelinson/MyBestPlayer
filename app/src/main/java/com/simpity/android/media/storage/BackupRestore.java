package com.simpity.android.media.storage;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.simpity.android.media.utils.Utilities;

public class BackupRestore implements Runnable, ContentHandler {

	private final Context mContext;
	private final BackupListener mListener;

	private int id, int_data0, int_data1, int_data2, int_data3, type, flags,
			group_id, last_acceessed_date;
	private StringBuilder url = new StringBuilder();
	private StringBuilder text_data0 = new StringBuilder();
	private StringBuilder text_data1 = new StringBuilder();
	private StringBuilder text_data2 = new StringBuilder();
	private StringBuilder text_data3 = new StringBuilder();
	private StringBuilder description = new StringBuilder();
	private StringBuilder current = null;

	StorageHelper db_helper;
	SQLiteDatabase db;

	//----------------------------------------------------------------------
	private BackupRestore(Context context, BackupListener listener) {
		mContext = context;
		mListener = listener;
	}

	//----------------------------------------------------------------------
	@Override
	public void run() {
		synchronized (Storage.MUTEX) {
			try {
				File backup_file = Utilities.getBackupFile();
				InputSource input_source = new InputSource(new FileInputStream(backup_file));

				if (mListener != null) {
					mListener.StorageRestoreStarted();
				}

				SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xml_reader = parser.getXMLReader();
		    	xml_reader.setContentHandler(this);

				db_helper = new StorageHelper(mContext);
				db = db_helper.getWritableDatabase();

		    	xml_reader.parse(input_source);

		    	if (mListener != null) {
					mListener.StorageRestoreFinished();
				}

			} catch (Exception ex) {
				
				ex.printStackTrace();
				if (mListener != null) {
					mListener.StorageBackupError(ex.getLocalizedMessage());
				}
				
			} catch (OutOfMemoryError ex) {
				
				ex.printStackTrace();
				if (mListener != null) {
					mListener.StorageBackupError(ex.getLocalizedMessage());
				}
				
			} finally {
				if (db_helper != null) {
					db_helper.close();
					db_helper = null;
				}
			}
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void startDocument() throws SAXException {
	}

	//----------------------------------------------------------------------
	@Override
	public void setDocumentLocator(Locator locator) {
	}

	//----------------------------------------------------------------------
	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	//----------------------------------------------------------------------
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	//----------------------------------------------------------------------
	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	//----------------------------------------------------------------------
	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	//----------------------------------------------------------------------
	private int getIntAttribute(Attributes atts, String qName) {
		String value = atts.getValue(qName);
		if (value == null)
			return 0;

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (qName.equals(BackupWriter.BACKUP_TAG)) {

			try {
				db.delete(Storage.MAIN_TABLE_NAME, null, null);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		} else if (qName.equals(BackupWriter.RECORD_TAG)) {

			id			= getIntAttribute(atts, Storage.ID);
			int_data0	= getIntAttribute(atts, Storage.INT_DATA0);
			int_data1	= getIntAttribute(atts, Storage.INT_DATA1);
			int_data2	= getIntAttribute(atts, Storage.INT_DATA2);
			int_data3	= getIntAttribute(atts, Storage.INT_DATA3);
			type		= getIntAttribute(atts, Storage.TYPE);
			group_id	= getIntAttribute(atts, Storage.GROUP_ID);
			last_acceessed_date = getIntAttribute(atts, Storage.LAST_ACCESSED_DATE);

			if (atts.getValue(Storage.OLD_FAVORITE) != null) {

				flags = int_data3 != 0 ? Storage.NEW_LINK_FLAG : 0;

				if (int_data2 != 0)
					flags |= Storage.DEAD_LINK_FLAG;

				if (getIntAttribute(atts, Storage.OLD_FAVORITE) != 0)
					flags |= Storage.FAVORITE_FLAG;

				int_data2 = int_data3 = 0;

			} else {

				flags = getIntAttribute(atts, Storage.FLAGS);
			}

			url.setLength(0);
			text_data0.setLength(0);
			text_data1.setLength(0);
			text_data2.setLength(0);
			text_data3.setLength(0);
			description.setLength(0);

			current = null;

		} else if (qName.equals(Storage.URL)) {

			current = url;

		} else if (qName.equals(Storage.DESCRIPTION)) {

			current = description;

		} else if (qName.equals(Storage.TEXT_DATA0)) {

			current = text_data0;

		} else if (qName.equals(Storage.TEXT_DATA1)) {

			current = text_data1;

		} else if (qName.equals(Storage.TEXT_DATA2)) {

			current = text_data2;

		} else if (qName.equals(Storage.TEXT_DATA3)) {

			current = text_data3;
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		if (current != null) {
			current.append(ch, start, length);
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {

		if (current != null) {
			current.append(ch, start, length);
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (qName.equals(BackupWriter.BACKUP_TAG)) {

		} else if (qName.equals(BackupWriter.RECORD_TAG)) {

			ContentValues values = new ContentValues();

			values.put(Storage.ID,					id);
			values.put(Storage.TYPE,				type);
			values.put(Storage.FLAGS,				flags);
			values.put(Storage.GROUP_ID,			group_id);
			values.put(Storage.INT_DATA0,			int_data0);
			values.put(Storage.INT_DATA1,			int_data1);
			values.put(Storage.INT_DATA2,			int_data2);
			values.put(Storage.INT_DATA3,			int_data3);
			values.put(Storage.LAST_ACCESSED_DATE, 	last_acceessed_date);

			if (description.length() > 0)
				values.put(Storage.DESCRIPTION, description.toString());

			if (url.length() > 0)
				values.put(Storage.URL, url.toString());

			if (text_data0.length() > 0)
				values.put(Storage.TEXT_DATA0, text_data0.toString());

			if (text_data0.length() > 0)
				values.put(Storage.TEXT_DATA1, text_data1.toString());

			if (text_data0.length() > 0)
				values.put(Storage.TEXT_DATA2, text_data2.toString());

			if (text_data0.length() > 0)
				values.put(Storage.TEXT_DATA3, text_data3.toString());

			try {
				db.insertOrThrow(Storage.MAIN_TABLE_NAME, "", values);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		} else if (qName.equals(Storage.URL)
				|| qName.equals(Storage.DESCRIPTION)
				|| qName.equals(Storage.TEXT_DATA0)
				|| qName.equals(Storage.TEXT_DATA1)
				|| qName.equals(Storage.TEXT_DATA2)
				|| qName.equals(Storage.TEXT_DATA3)) {

			current = null;
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void endDocument() throws SAXException {
	}
	
	//--------------------------------------------------------------------------
	public static void start(Context context, BackupListener listener) {

		Thread thread = new Thread(new BackupRestore(context, listener));
		thread.start();
	}

}