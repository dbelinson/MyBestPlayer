package com.simpity.android.media.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StorageHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "history";

	private static final String OLD_MAIN_TABLE_NAME = "history";
	
	private static final String MAIN_TABLE_CREATE =
		"CREATE TABLE IF NOT EXISTS " + Storage.MAIN_TABLE_NAME + " (" +
		Storage.ID 					+ " INTEGER PRIMARY KEY, " +
		Storage.DESCRIPTION			+ " TEXT, " +
		Storage.URL					+ " TEXT, " +
		Storage.TEXT_DATA0			+ " TEXT, " +
		Storage.TEXT_DATA1			+ " TEXT, " +
		Storage.TEXT_DATA2			+ " TEXT, " +
		Storage.TEXT_DATA3			+ " TEXT, " +
		Storage.INT_DATA0			+ " NUMERIC, " +
		Storage.INT_DATA1			+ " NUMERIC, " +
		Storage.INT_DATA2			+ " NUMERIC, " +
		Storage.INT_DATA3			+ " NUMERIC, " +
		Storage.TYPE				+ " NUMERIC, " +
		Storage.FLAGS				+ " NUMERIC, " +
		Storage.LAST_ACCESSED_DATE	+ " INTEGER, " +
		Storage.GROUP_ID			+ " INTEGER);" ;

	/*private static final String PLAYLIST_TABLE_CREATE =
		"CREATE TABLE IF NOT EXISTS " + PLAYLIST_TABLE_NAME + " (" +
		ID 					+ " INTEGER PRIMARY KEY, " +
		GROUP_ID			+ " INTEGER, " +
		MEMBER_ID			+ " INTEGER);" ;*/

	StorageHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(MAIN_TABLE_CREATE);
		//db.execSQL(PLAYLIST_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		try {
			onCreate(db);

			Cursor cursor = db.query(OLD_MAIN_TABLE_NAME, null, null, null, null, null, null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					try {
						String[] str_key = { 
								Storage.DESCRIPTION, 
								Storage.URL, 
								Storage.TEXT_DATA0, 
								Storage.TEXT_DATA1, 
								Storage.TEXT_DATA2, 
								Storage.TEXT_DATA3};
						String[] int_key = { 
								Storage.INT_DATA0, 
								Storage.INT_DATA1, 
								Storage.TYPE, 
								Storage.LAST_ACCESSED_DATE, 
								Storage.GROUP_ID};
						int[] str_key_index = new int[str_key.length];
						int[] int_key_index = new int[int_key.length];

						for (int i=0; i<str_key.length; i++) {
							str_key_index[i] = cursor.getColumnIndexOrThrow(str_key[i]);
						}

						for (int i=0; i<int_key.length; i++) {
							int_key_index[i] = cursor.getColumnIndexOrThrow(int_key[i]);
						}

						int new_link_index = cursor.getColumnIndexOrThrow(Storage.INT_DATA3);
						int dead_link_index = cursor.getColumnIndexOrThrow(Storage.INT_DATA2);
						int favorite_index = cursor.getColumnIndexOrThrow(Storage.OLD_FAVORITE);

						do {
							ContentValues values = new ContentValues();
							int flags = (cursor.getInt(new_link_index) != 0 ? Storage.NEW_LINK_FLAG : 0);

							if (cursor.getInt(dead_link_index) != 0)
								flags |= Storage.DEAD_LINK_FLAG;

							if (cursor.getInt(favorite_index) != 0)
								flags |= Storage.FAVORITE_FLAG;

							values.put(Storage.FLAGS, flags);

							for (int i=0; i<str_key.length; i++) {
								values.put(str_key[i], cursor.getString(str_key_index[i]));
							}

							for (int i=0; i<int_key.length; i++) {
								values.put(int_key[i], cursor.getString(int_key_index[i]));
							}

							db.insertOrThrow(Storage.MAIN_TABLE_NAME, "", values);

						} while (cursor.moveToNext());

					} catch (IllegalArgumentException ex) {

						ex.printStackTrace();
					}
				}

				cursor.close();
			}

			db.execSQL("DROP TABLE " + OLD_MAIN_TABLE_NAME);

		} catch (SQLException ex) {

			ex.printStackTrace();
		}
	}
}
