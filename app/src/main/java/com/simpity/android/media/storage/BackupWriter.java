package com.simpity.android.media.storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.Utilities;

//--------------------------------------------------------------------------
public class BackupWriter implements Runnable {
	
	private final static String LAST_BACKUP_DATE = "LAST_BACKUP_DATE";
	final static String BACKUP_TAG = "Backup";
	final static String RECORD_TAG = "Record";

	private final Context mContext;
	private final BackupListener mBackupListener;

	//----------------------------------------------------------------------
	BackupWriter(Context context, BackupListener listener) {
		mContext = context;
		mBackupListener = listener;
	}

	//----------------------------------------------------------------------
	private BufferedWriter createBackupFile() throws IOException {

		File backup_file = Utilities.getBackupFile();
		if (backup_file.exists()) {
			backup_file.delete();
		}

		backup_file.createNewFile();
		return new BufferedWriter(new FileWriter(backup_file));
	}

	//----------------------------------------------------------------------
	@Override
	public void run() {

		synchronized (Storage.MUTEX) {
			StorageHelper db_helper = new StorageHelper(mContext);
			SQLiteDatabase db = db_helper.getReadableDatabase();
			try{
				Cursor cursor = db.query(Storage.MAIN_TABLE_NAME, null, null, null, null, null, null);
				if (cursor == null || !cursor.moveToFirst()) {

					if (mBackupListener != null) {
						mBackupListener.StorageBackupError(mContext.getString(Res.string.no_links));
					}

					if (cursor != null) {
						cursor.close();
					}

					return;
				}

				BufferedWriter writer;

				try {
					writer = createBackupFile();
				} catch (IOException ex) {
					ex.printStackTrace();
					if (mBackupListener != null) {
						mBackupListener.StorageBackupError(ex.getLocalizedMessage());
					}
					return;
				}

				if (mBackupListener != null) {
					mBackupListener.StorageBackupStarted();
				}

				try {
					writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
					writer.newLine();
					writer.write("<" + BACKUP_TAG + ">");

					String[] int_name = new String[] {
							Storage.ID,
							Storage.INT_DATA0,
							Storage.INT_DATA1,
							Storage.INT_DATA2,
							Storage.INT_DATA3,
							Storage.TYPE,
							Storage.FLAGS,
							Storage.GROUP_ID,
							Storage.LAST_ACCESSED_DATE
					};

					int[] int_index = new int[int_name.length];
					for (int i=0; i<int_name.length; i++) {
						int_index[i] = cursor.getColumnIndexOrThrow(int_name[i]);
					}

					String[] str_name = new String[] {
							Storage.URL,
							Storage.DESCRIPTION,
							Storage.TEXT_DATA0,
							Storage.TEXT_DATA1,
							Storage.TEXT_DATA2,
							Storage.TEXT_DATA3
					};

					int[] str_index = new int[str_name.length];
					for (int i=0; i<str_name.length; i++) {
						str_index[i] = cursor.getColumnIndexOrThrow(str_name[i]);
					}

					do {
						writer.newLine();
						writer.write("\t<" + RECORD_TAG);

						for (int i=0; i<int_index.length; i++) {
							int data = cursor.getInt(int_index[i]);
							writer.newLine();
							writer.write("\t\t");
							writer.write(int_name[i]);
							writer.write("=\"");
							writer.write(Integer.toString(data));
							writer.write("\"");
						}

						writer.write(">");

						for (int i=0; i<str_index.length; i++) {
							String data = cursor.getString(str_index[i]);
							if (data != null && data.length() > 0) {
								writer.newLine();
								writer.write("\t\t<");
								writer.write(str_name[i]);

								if (data.indexOf('<') >= 0 ||
									data.indexOf('>') >= 0 ||
									data.indexOf('&') >= 0) {

									writer.write("><![CDATA[");
									writer.write(data);
									writer.write("]]></");

								} else {

									writer.write(">");
									writer.write(data);
									writer.write("</");
								}

								writer.write(str_name[i]);
								writer.write(">");
							}
						}

						writer.newLine();
						writer.write("\t</" + RECORD_TAG + ">");

					} while (cursor.moveToNext());


					writer.newLine();
					writer.write("</" + BACKUP_TAG + ">");

				} catch (IOException ex) {
					ex.printStackTrace();
					if (mBackupListener != null) {
						mBackupListener.StorageBackupError(ex.getLocalizedMessage());
					}
					return;
				}

				try {
					writer.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			finally{
				db_helper.close();
				db.close();
			}
		}

		if (mBackupListener != null) {
			mBackupListener.StorageBackupFinished();
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putLong(LAST_BACKUP_DATE, System.currentTimeMillis());
		editor.commit();
	}
	
	//--------------------------------------------------------------------------
	public static boolean isBackupExists() {
		File backup_file;

		try {
			backup_file = Utilities.getBackupFile();
		} catch (IOException e) {
			return false;
		}

		return backup_file.exists();
	}

	//--------------------------------------------------------------------------
	public static void startBackup(Context context, BackupListener listener) {
		Thread thread = new Thread(new BackupWriter(context, listener));
		thread.start();
	}

	//--------------------------------------------------------------------------
	public static long getLastBackupTime(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(LAST_BACKUP_DATE, 0L);
	}
	
	//--------------------------------------------------------------------------
	public static void backupBySchedule(Context context) {

		String schedule_key = context.getString(Res.string.pref_backup_schedule_key);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		long schedule_time = prefs.getLong(LAST_BACKUP_DATE, 0L);
		int value;

		try {
			value = Integer.parseInt(prefs.getString(schedule_key, "0"));
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			return;
		}

		switch (value) {
		case 1: // Daily
			schedule_time += 24*60*60*1000l;
			break;
		case 2: // Weekly
			schedule_time += 7*24*60*60*1000l;
			break;
		case 3: // Monthly
			schedule_time += 30*24*60*60*1000l;
			break;
		default:
			return;
		}

		if (System.currentTimeMillis() >= schedule_time) {
			startBackup(context, null);
		}
	}

}