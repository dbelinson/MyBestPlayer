package com.simpity.android.media.storage;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.simpity.android.media.MediaService;

public class Storage {

	static final String MAIN_TABLE_NAME = "history2";
	//private static final String PLAYLIST_TABLE_NAME = "playlist";

	static final int DEAD_LINK_THRESHOLD = 10;
	
	//--- COMMON FIELDS --------------------------------------------------------
	static final String ID					= "id";
	static final String URL					= "url";
	static final String TEXT_DATA0			= "text_data0";
	static final String TEXT_DATA1			= "text_data1";
	static final String TEXT_DATA2			= "text_data2";
	static final String TEXT_DATA3			= "text_data3";
	static final String INT_DATA0			= "int_data0";
	static final String INT_DATA1			= "int_data1";
	static final String INT_DATA2			= "int_data2";
	static final String INT_DATA3			= "int_data3";
	static final String TYPE				= "type";
	static final String GROUP_ID			= "group_id";
	static final String LAST_ACCESSED_DATE	= "last_acceessed_date";
	static final String FLAGS				= "flags";

	static final String OLD_FAVORITE		= "favorite";
	static final String NOT_AVAILABLE_COUNT	= INT_DATA1;

	//private static final String MEMBER_ID	= "member";

	static final int NEW_LINK_FLAG 			= 1;
	static final int DEAD_LINK_FLAG			= 2;
	static final int FAVORITE_FLAG			= 4;

	//--- VIDEO FIELDS ---------------------------------------------------------
	static final String DESCRIPTION			= "description";
	private static final String CATEGORY	= TEXT_DATA1;

	//--- RADIO FIELDS ---------------------------------------------------------
	private static final String RADIO_NAME		= DESCRIPTION;
	private static final String CONTENT_TYPE	= TEXT_DATA0;
	private static final String GENRE			= TEXT_DATA1;

	//--- JPEG CAMERA FIELDS ---------------------------------------------------
	private static final String REFRESH_TIME	= INT_DATA0;

	//--- PLAYLIST FIELDS ------------------------------------------------------
	private static final String PLAYLIST_NAME	= DESCRIPTION;
	private static final String ID_LIST			= TEXT_DATA3;
	
	//--- TYPE & GROUP VALUES --------------------------------------------------
	public final static int STREAM_VIDEO			= 1;
	public final static int INTERNET_RADIO			= 2;
	public final static int JPEG_CAMERA				= 3;
	public final static int GROUP					= 0x100;
	public final static int PLAYLIST				= 0x1000;

	public final static int STREAM_VIDEO_GROUP		= GROUP | STREAM_VIDEO;
	public final static int INTERNET_RADIO_GROUP	= GROUP | INTERNET_RADIO;
	public final static int JPEG_CAMERA_GROUP		= GROUP | JPEG_CAMERA;

	public final static int INTERNET_RADIO_PLAYLIST	= PLAYLIST | INTERNET_RADIO;
	public final static int JPEG_CAMERA_PLAYLIST	= PLAYLIST | JPEG_CAMERA;

	final static Object MUTEX = new Object();

	//---- DATABASE METHODS ----------------------------------------------------
	public static boolean addRecord(Context context, RecordBase record) {

		synchronized (MUTEX) {
			StorageHelper db_helper = new StorageHelper(context);

			try {
				SQLiteDatabase db = db_helper.getWritableDatabase();

				long id = db.insertOrThrow(MAIN_TABLE_NAME, "", record.getContentValues());
				if (id >= 0) {
					record.setId((int)id);
					return true;
				}

			} catch (SQLException ex) {

				ex.printStackTrace();

			} finally {

				db_helper.close();
			}

			return false;
		}
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> addRecordsByUpdate(Context context, Vector<RecordBase> records) {

		synchronized (MUTEX) {
			StorageHelper db_helper = new StorageHelper(context);
			SQLiteDatabase db;
			Vector<RecordBase> result = new Vector<RecordBase>();

			try {
				db = db_helper.getWritableDatabase();

			} catch (SQLException ex) {

				ex.printStackTrace();
				db_helper.close();
				return result;
			}

			StringBuilder selection = new StringBuilder();
			String[] columns = {ID};
			int counter = 0;

			try {
				for (RecordBase record : records) {

					counter++;
					if ((counter & 7) == 0) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}

					if (record instanceof RadioRecord) {
						RadioRecord radio_record = (RadioRecord)record;
						String hostName = radio_record.getHostName();
						if (hostName != null) {
							selection.setLength(0);
							selection.append(DESCRIPTION);
							selection.append("='");
							selection.append(hostName);
							selection.append('\'');

							Cursor cursor = db.query(MAIN_TABLE_NAME, columns, selection.toString(), null, null, null, null);
							int id = -1;

							if (cursor != null) {
								if (cursor.moveToFirst()) {
									try {
										id = cursor.getInt(cursor.getColumnIndexOrThrow(ID));
									} catch (IllegalArgumentException ex) {
										ex.printStackTrace();
									}
								}

								cursor.close();
							}

							if (id < 0) {
								GroupNameAndId group = createNewGroup(record.getRecordsManager(),
										INTERNET_RADIO_GROUP, hostName, -1);
								if (group != null) {
									id = group.getId();
								}
							}

							if (id >= 0) {
								record.setGroup(id);
							}
						}
					}

					selection.setLength(0);
					selection.append(URL);
					selection.append("='");
					selection.append(record.getUrl());
					selection.append('\'');

					Cursor cursor = db.query(MAIN_TABLE_NAME, columns, selection.toString(), null, null, null, null);
					int id = -1;

					if (cursor != null) {
						if (cursor.moveToFirst()) {
							try {
								id = cursor.getInt(cursor.getColumnIndexOrThrow(ID));
							} catch (IllegalArgumentException ex) {
								ex.printStackTrace();
							}
						}

						cursor.close();
					}

					if (id >= 0) {

						db.update(MAIN_TABLE_NAME, record.getContentValues(), ID + '=' + id, null);

					} else if (!record.isDeadLink()) {

						long new_id = db.insertOrThrow(MAIN_TABLE_NAME, "", record.getContentValues());
						if (id >= 0) {
							record.setId((int)new_id);
						}

					} else {

						result.add(record);
					}
				}

			} catch (SQLException ex) {

				ex.printStackTrace();

			} finally {

				db_helper.close();
			}

			return result;
		}
	}

	//--------------------------------------------------------------------------
	public static boolean updateRecord(Context context, RecordBase record) {

		synchronized (MUTEX) {
			StorageHelper db_helper = new StorageHelper(context);

			try {
				SQLiteDatabase db = db_helper.getWritableDatabase();

				return db.update(MAIN_TABLE_NAME, record.getContentValues(),
						ID + '=' + record.getId(), null) > 0;

			} catch (SQLException ex) {

				ex.printStackTrace();

			} finally {

				db_helper.close();
			}

			return false;
		}
	}

	//--------------------------------------------------------------------------
	public static boolean deleteRecord(Context context, RecordBase record) {

		synchronized (MUTEX) {
			if (record != null) {
				StorageHelper db_helper = new StorageHelper(context);
				try {
					SQLiteDatabase db = db_helper.getWritableDatabase();

					return db.delete(MAIN_TABLE_NAME, ID + '=' + record.getId(), null) > 0;

				} catch (SQLException ex) {

					ex.printStackTrace();

				} finally {

					db_helper.close();
				}
			}

			return false;
		}
	}

	//--- GROUP METHODS --------------------------------------------------------
	private final static String[] GROUP_COLUMNS = {ID, TYPE, DESCRIPTION, GROUP_ID};

	private static Vector<GroupNameAndId> getGroups(RecordsManager manager, String selection) {

		synchronized (MUTEX) {
			Vector<GroupNameAndId> result = new Vector<GroupNameAndId>();
			StorageHelper db_helper = new StorageHelper(manager.getContext());

			try {
				SQLiteDatabase db = db_helper.getReadableDatabase();

				Cursor cursor = db.query(MAIN_TABLE_NAME, GROUP_COLUMNS, selection, null, null, null, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							TableIndex index = new TableIndex(cursor);

							do {
								result.add(new GroupNameAndId(manager, cursor, index));

							} while (cursor.moveToNext());

						} catch (IllegalArgumentException ex) {

							Log.d("Storage.getGroups", ex.toString());
						}
					}

					cursor.close();
				}

			} catch (SQLException ex) {

				ex.printStackTrace();

			} finally {

				db_helper.close();
			}

			while(result.remove(null)) {
			}

			return result;
		}
	}

	//--------------------------------------------------------------------------
	public static Vector<GroupNameAndId> getAllGroup(RecordsManager manager, int group_type) {

		return getGroups(manager, String.format("%s=%d", TYPE, group_type));
	}

	//--------------------------------------------------------------------------
	public static Vector<GroupNameAndId> getAllGroups(RecordsManager manager) {

		return getGroups(manager, String.format("%s=%d or %s=%d or %s=%d",
				TYPE, STREAM_VIDEO_GROUP, TYPE, INTERNET_RADIO_GROUP, TYPE, JPEG_CAMERA_GROUP));
	}

	//--------------------------------------------------------------------------
	public static Vector<GroupNameAndId> getAllGroupForGroup(RecordsManager manager, GroupNameAndId parent_group) {

		return getGroups(manager, String.format("%s=%d AND %s=%d", TYPE, parent_group.getType(),
				GROUP_ID, parent_group.getId()));
	}

	//--------------------------------------------------------------------------
	public static String getGroupName(Context context, int group_id) {

		synchronized (MUTEX) {
			StorageHelper db_helper = new StorageHelper(context);
			String result = null;

			try {
				SQLiteDatabase db = db_helper.getWritableDatabase();

				Cursor cursor = db.query(MAIN_TABLE_NAME, new String[] {DESCRIPTION},
						ID + '=' + group_id, null, null, null, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							int name_index = cursor.getColumnIndexOrThrow(DESCRIPTION);
							result = cursor.getString(name_index);

						} catch (IllegalArgumentException ex) {
							ex.printStackTrace();
						}
					}

					cursor.close();
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
			} finally {

				db_helper.close();
			}

			return result;
		}
	}

	//--------------------------------------------------------------------------
	public static GroupNameAndId createNewGroup(RecordsManager manager, int group_type,
			String group_name, int parent_group_id) {

		if ((group_type & GROUP) != Storage.GROUP)
			return null;

		GroupNameAndId group = new GroupNameAndId(manager, group_name, 0, group_type, parent_group_id);

		return addRecord(manager.getContext(), group) ? group : null;
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getAllGroupElements(RecordsManager manager, GroupNameAndId group) {

		Vector<RecordBase> result = new Vector<RecordBase>();
		if ((group.getType() & STREAM_VIDEO_GROUP) == STREAM_VIDEO_GROUP) {

			result.addAll(getVideoRecordsForGroup(manager, group.getId()));

		} else if ((group.getType() & INTERNET_RADIO_GROUP) == INTERNET_RADIO_GROUP) {

			result.addAll(getRadioRecordsForGroup(manager, group.getId()));

		} else if ((group.getType() & JPEG_CAMERA_GROUP) == JPEG_CAMERA_GROUP) {

			result.addAll(getCameraRecordsForGroup(manager, group.getId()));
		}
		return result;
	}

	//---- VIDEO RECORDS METHODS -----------------------------------------------
	private final static String[] VIDEO_COLUMNS = {ID, DESCRIPTION, URL, CATEGORY, GROUP_ID,
		LAST_ACCESSED_DATE, FLAGS, NOT_AVAILABLE_COUNT};

	private static class VideoRecordFactory extends RecordFactory {

		public VideoRecordFactory(RecordsManager manager) {
			super(manager);
		}

		@Override
		public RecordBase addRecord(Cursor cursor, TableIndex index) {
			VideoRecord rec = new VideoRecord(mRecordsManager, cursor, index);
			mRecords.add(rec);
			return rec;
		}
	}

 	//--------------------------------------------------------------------------
 	public static Vector<RecordBase> getVideoRecords(RecordsManager manager) {

 		VideoRecordFactory factory = new VideoRecordFactory(manager);
		getRecords(STREAM_VIDEO, VIDEO_COLUMNS, -1, factory);

		return factory.mRecords;
	}

 	//--------------------------------------------------------------------------
 	public static Vector<RecordBase> getVideoRecordsForGroup(RecordsManager manager, int group_id) {

 		VideoRecordFactory factory = new VideoRecordFactory(manager);
		getRecords(STREAM_VIDEO, VIDEO_COLUMNS, group_id, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static VideoRecord appendVideoRecord(RecordsManager manager, Context context,
			String description, String url, String category, int group_id, boolean favorite,
			long lastAccessedDateInMillis, boolean isNewLink, boolean deadLinkState) {

		VideoRecord record = new VideoRecord(manager, 0, description, url, category, favorite,
				group_id, lastAccessedDateInMillis, isNewLink, deadLinkState);

		if (!addRecord(context, record))
			return null;

		String group_name = getGroupName(context, group_id);
		if (group_name != null) {
			record.setGroupName(group_name);
		}

		return record;
	}

	//--------------------------------------------------------------------------
	private static boolean getRecords(int type, String[] columns,
			int group_id, RecordFactory factory) {

		StringBuilder selection = new StringBuilder();
		selection.append(TYPE);
		selection.append('=');
		selection.append(type);
		if (group_id >= 0) {
			selection.append(" AND ");
			selection.append(GROUP_ID);
			selection.append('=');
			selection.append(group_id);
		}

		return getRecords(type, columns, group_id, factory, selection.toString());
	}

	//--------------------------------------------------------------------------
	private static boolean getRecords(int type, String[] columns, int[] ids, RecordFactory factory) {

		StringBuilder selection = new StringBuilder();
		selection.append(TYPE);
		selection.append('=');
		selection.append(type);
		if (ids.length > 0) {
			selection.append(" AND ");
			selection.append(ID);
			selection.append(" IN (");
			for (int i=0; i<ids.length-1; i++) {
				selection.append(ids[i]);
				selection.append(',');
			}
			selection.append(ids[ids.length-1]);
			selection.append(')');
		}

		if (getRecords(type, columns, -1, factory, selection.toString())) {
			Vector<RecordBase> records = new Vector<RecordBase>();
			records.addAll(factory.mRecords);
			
			factory.mRecords.clear();
			for (int i=0; i<ids.length; i++) {
				for (RecordBase record : records) {
					if (record.getId() == ids[i]) {
						factory.mRecords.add(record);
						break;
					}
				}
			}
			
			return true;
		}
		
		return false;
	}

	//--------------------------------------------------------------------------
	private static boolean getRecords(int type, String[] columns,
			int group_id, RecordFactory factory, String selection) {

		synchronized (MUTEX) {
			Vector<GroupNameAndId> groups;
			String group_name;
			Context context = factory.getContext();

			if (group_id >= 0) {

				groups = null;
				group_name = getGroupName(context, group_id);

			} else {

				groups = getAllGroup(factory.getRecordsManager(), type | GROUP);
				group_name = null;
			}

			StorageHelper db_helper = new StorageHelper(context);

			try {
				SQLiteDatabase db = db_helper.getReadableDatabase();

				Cursor cursor = db.query(MAIN_TABLE_NAME, columns, selection, null, null, null, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							TableIndex index = new TableIndex(cursor);

							do {
								RecordBase rec = factory.addRecord(cursor, index);

								if (groups != null) {

									group_id = rec.getGroupId();

									for (GroupNameAndId group : groups)
										if (group.getId() == group_id) {
											rec.setGroupName(group.getDescription());
											break;
										}

								} else if (group_name != null) {

									rec.setGroupName(group_name);
								}

							} while (cursor.moveToNext());

						} catch (IllegalArgumentException ex) {

							Log.e("Storage.getRecords", ex.toString());
						}
					}

					cursor.close();

				} else {

					Log.d("Storage.getRecords", "cursor == null");
				}

			} catch (SQLException ex) {

				ex.printStackTrace();
				return false;

			} finally {

				db_helper.close();
			}

			return true;
		}
	}

	//---- RADIO RECORDS METHODS ----------------------------------------------------------------------
	private final static String[] RADIO_COLUMNS = new String[] {ID, RADIO_NAME, URL, CONTENT_TYPE, GENRE, GROUP_ID, LAST_ACCESSED_DATE, FLAGS, NOT_AVAILABLE_COUNT};

	private static class RadioRecordFactory extends RecordFactory {

		public RadioRecordFactory(RecordsManager manager) {
			super(manager);
		}

		@Override
		public RecordBase addRecord(Cursor cursor, TableIndex index) {
			RadioRecord rec = new RadioRecord(mRecordsManager, cursor, index);
			mRecords.add(rec);
			return rec;
		}

		@Override
		public PlayList createPlaylist(Cursor cursor, TableIndex index) {
			return new RadioPlayList(mRecordsManager, cursor, index);
		}

	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getRadioRecords(RecordsManager manager) {

		RadioRecordFactory factory = new RadioRecordFactory(manager);
		getRecords(INTERNET_RADIO, RADIO_COLUMNS, -1, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getRadioRecordsByUrl(RecordsManager manager, String url) {

		RadioRecordFactory factory = new RadioRecordFactory(manager);
		
		StringBuilder selection = new StringBuilder();
		selection.append(TYPE);
		selection.append('=');
		selection.append(INTERNET_RADIO);
		selection.append(" AND ");
		selection.append(URL);
		selection.append("=\'");
		selection.append(url);
		selection.append('\'');

		getRecords(INTERNET_RADIO, RADIO_COLUMNS, -1, factory, selection.toString());
		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getRadioRecords(RecordsManager manager, int[] ids) {

		RadioRecordFactory factory = new RadioRecordFactory(manager);
		getRecords(INTERNET_RADIO, RADIO_COLUMNS, ids, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getRadioRecordsForGroup(RecordsManager manager, int group_id) {

		RadioRecordFactory factory = new RadioRecordFactory(manager);
		getRecords(INTERNET_RADIO, RADIO_COLUMNS, group_id, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static RadioRecord appendRadioRecord(RecordsManager manager,
			String radio_station_name, String url, String content_description, String genre, boolean favorite,
			int group_id, long lastAccessedDateInMillis, boolean isNewLink, boolean deadLinkState) {

		RadioRecord record = new RadioRecord(manager, 0, radio_station_name, url, content_description,
				genre, favorite, group_id, lastAccessedDateInMillis, isNewLink, deadLinkState);

		Context context = manager.getContext();
		if (!addRecord(context, record))
			return null;

		String group_name = getGroupName(context, group_id);
		if (group_name != null) {
			record.setGroupName(group_name);
		}

		return record;
	}

	//--------------------------------------------------------------------------
	private final static String[] TEST_LINKS_COLUMNS = {ID, URL, FLAGS, NOT_AVAILABLE_COUNT};

	public final static Vector<LinkRecord> getAllLink(MediaService service) {
		Vector<LinkRecord> result = new Vector<LinkRecord>();
		
		synchronized (MUTEX) {
			StorageHelper db_helper = new StorageHelper(service);

			try {
				SQLiteDatabase db = db_helper.getReadableDatabase(); 
				
				Cursor cursor = db.query(MAIN_TABLE_NAME, TEST_LINKS_COLUMNS, 
						TYPE + '=' + STREAM_VIDEO + " OR " +
						TYPE + '=' + INTERNET_RADIO + " OR " +
						TYPE + '=' + JPEG_CAMERA, 
						null, null, null, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							int id_index		= cursor.getColumnIndexOrThrow(ID);
							int flags_index		= cursor.getColumnIndexOrThrow(FLAGS);
							int url_index		= cursor.getColumnIndexOrThrow(URL);
							int counter_index	= cursor.getColumnIndexOrThrow(NOT_AVAILABLE_COUNT);

							do {
								String url = cursor.getString(url_index);
								if (url != null) {
									result.add(new LinkRecord(cursor.getInt(id_index), url, 
											cursor.getInt(flags_index), 
											cursor.getInt(counter_index)));
								}

							} while (cursor.moveToNext());

						} catch (IllegalArgumentException ex) {
							
							if (service.isDebuggable()) {
								ex.printStackTrace();
							}
						}
					}

					cursor.close();
				}

			} catch (SQLException ex) {

				if (service.isDebuggable()) {
					ex.printStackTrace();
				}

			} finally {

				db_helper.close();
			}
		}
		
		return result;
	}
	
	//--------------------------------------------------------------------------
	public final static void updateLink(MediaService service, LinkRecord link, ContentValues values) {
		
		synchronized (MUTEX) {
			
			StorageHelper db_helper = new StorageHelper(service);
			try {
				SQLiteDatabase db = db_helper.getWritableDatabase();
				
				link.getContentValues(values);
				db.update(MAIN_TABLE_NAME, values, ID + '=' + link.getId(), null);
				
			} catch (Exception ex) {
				
				if (service.isDebuggable()) {
					ex.printStackTrace();
				}
				
			} finally {

				db_helper.close();
			}
		}
	}
	
	//---- RADIO PLAYLIST RECORDS METHODS --------------------------------------
	private final static String[] RADIO_PLAYLIST_COLUMNS = new String[] {
		ID, PLAYLIST_NAME, CONTENT_TYPE, GENRE, LAST_ACCESSED_DATE, FLAGS, ID_LIST};

	private static class RadioPlaylistFactory extends RecordFactory {

		public RadioPlaylistFactory(RecordsManager manager) {
			super(manager);
		}

		@Override
		public RecordBase addRecord(Cursor cursor, TableIndex index) {
			RadioPlayList rec = new RadioPlayList(mRecordsManager, cursor, index);
			mRecords.add(rec);
			return rec;
		}

		@Override
		public PlayList createPlaylist(Cursor cursor, TableIndex index) {
			return null;
		}

	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getRadioPlaylists(RecordsManager manager) {

		RadioPlaylistFactory factory = new RadioPlaylistFactory(manager);
		getRecords(INTERNET_RADIO_PLAYLIST, RADIO_PLAYLIST_COLUMNS, -1, factory);

		return factory.mRecords;
	}

	//---- JPEG CAMERA RECORDS METHODS ----------------------------------------------------------------------
	private final static String[] CAMERA_COLUMNS = {ID, DESCRIPTION, URL, REFRESH_TIME, GROUP_ID, LAST_ACCESSED_DATE, FLAGS, NOT_AVAILABLE_COUNT};

	private static class CameraRecordFactory extends RecordFactory {

		public CameraRecordFactory(RecordsManager manager) {
			super(manager);
		}

		@Override
		public RecordBase addRecord(Cursor cursor, TableIndex index) {
			CameraRecord rec = new CameraRecord(mRecordsManager, cursor, index);
			mRecords.add(rec);
			return rec;
		}
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getCameraRecords(RecordsManager manager) {

		CameraRecordFactory factory = new CameraRecordFactory(manager);
		getRecords(JPEG_CAMERA, CAMERA_COLUMNS, -1, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static Vector<RecordBase> getCameraRecordsForGroup(RecordsManager manager, int group_id) {

		CameraRecordFactory factory = new CameraRecordFactory(manager);
		getRecords(JPEG_CAMERA, CAMERA_COLUMNS, group_id, factory);

		return factory.mRecords;
	}

	//--------------------------------------------------------------------------
	public static CameraRecord appendCameraRecord(RecordsManager manager,
			String desription, String url, int refreshPeriod, boolean favorite, int group_id,
			long lastAccessedDateInMillis, boolean isNewLink, boolean deadLinkState) {

		CameraRecord record = new CameraRecord(manager, 0, desription, url, refreshPeriod, favorite, group_id, lastAccessedDateInMillis, isNewLink, deadLinkState);

		Context context = manager.getContext();
		if (!addRecord(context, record))
			return null;

		String group_name = getGroupName(context, group_id);
		if (group_name != null) {
			record.setGroupName(group_name);
		}

		return record;
	}

	//--------------------------------------------------------------------------
	public static boolean isEmpty(Context context) {

		synchronized (MUTEX) {

			boolean result = true;
			StorageHelper db_helper = null;
			try {
				db_helper = new StorageHelper(context);
				SQLiteDatabase db = db_helper.getReadableDatabase();

				Cursor cursor = db.query(MAIN_TABLE_NAME, null, null, null, null, null, null);
				if (cursor != null) {
					result = (cursor.getCount() == 0);
					cursor.close();
				}

			} catch (SQLiteException ex) {

				ex.printStackTrace();

			} catch (Exception ex) {

				ex.printStackTrace();

			} finally {
				if (db_helper != null)
					db_helper.close();
			}

			return result;
		}
	}
}
