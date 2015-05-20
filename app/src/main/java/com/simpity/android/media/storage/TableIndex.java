package com.simpity.android.media.storage;

import android.database.Cursor;

public class TableIndex {

	public final int id_index;
	public final int url_index;
	public final int description_index;
	public final int text_data0_index;
	public final int text_data1_index;
	public final int text_data2_index;
	public final int text_data3_index;
	public final int int_data0_index;
	public final int int_data1_index;
	public final int int_data2_index;
	public final int int_data3_index;
	public final int type_index;
	public final int group_id_index;
	public final int flags_index;
	public final int last_acceessed_date_index;

	public TableIndex(Cursor cursor) {
		id_index					= getIndex(cursor, Storage.ID);
		url_index					= getIndex(cursor, Storage.URL);
		description_index			= getIndex(cursor, Storage.DESCRIPTION);
		text_data0_index			= getIndex(cursor, Storage.TEXT_DATA0);
		text_data1_index			= getIndex(cursor, Storage.TEXT_DATA1);
		text_data2_index			= getIndex(cursor, Storage.TEXT_DATA2);
		text_data3_index			= getIndex(cursor, Storage.TEXT_DATA3);
		int_data0_index				= getIndex(cursor, Storage.INT_DATA0);
		int_data1_index				= getIndex(cursor, Storage.INT_DATA1);
		int_data2_index				= getIndex(cursor, Storage.INT_DATA2);
		int_data3_index				= getIndex(cursor, Storage.INT_DATA3);
		type_index					= getIndex(cursor, Storage.TYPE);
		group_id_index				= getIndex(cursor, Storage.GROUP_ID);
		flags_index					= getIndex(cursor, Storage.FLAGS);
		last_acceessed_date_index	= getIndex(cursor, Storage.LAST_ACCESSED_DATE);
	}

	private int getIndex(Cursor cursor, String column_name) {
		try {
			return cursor.getColumnIndexOrThrow(column_name);
		} catch (IllegalArgumentException ex) {
			return -1;
		}
	}
}
