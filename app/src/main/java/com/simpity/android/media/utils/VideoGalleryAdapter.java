package com.simpity.android.media.utils;

import java.util.Vector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Video;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class VideoGalleryAdapter implements ListAdapter {

	private class VideoElement {
		
		final int mId;
		final boolean mExternal;
		final String mTitle, mDescription;
		int mThumbId;
		String mThumbFilePath; 
		
		VideoElement(int id, boolean external, String title, String description) {
			mId = id;
			mExternal = external;
			mTitle = title;
			mDescription = description;
			mThumbId = -1;
		}
	}

	private Vector<VideoElement> mVideos = new Vector<VideoElement>();
	private Context mContext;
	
	private final static String EXTERNAL_THUMBNAILS = "content://media/external/video/thumbnails";
	private final static String INTERNAL_THUMBNAILS = "content://media/internal/video/thumbnails";

	private final static String VideoThumbnails_ID = "_id";			  //Video.Thumbnails._ID
	private final static String VideoThumbnailsDATA = "_data";        //Video.Thumbnails.DATA
	private final static String VideoThumbnailsVIDEO_ID = "video_id"; //Video.Thumbnails.VIDEO_ID
	private final static int BuildVERSION_CODES_ECLAIR = 5;           //Android 2.0 
	
	//--------------------------------------------------------------------------
	public VideoGalleryAdapter(Context context) {
		mContext = context;
		scan(context);	
	}

	//--------------------------------------------------------------------------
	private void scan(ContentResolver resolver, Uri uri, boolean external) {
		String[] projection = new String[] {
				Video.Media._ID,
				Video.Media.TITLE,
				Video.Media.DESCRIPTION,
				Video.Media.DISPLAY_NAME
			};
			
		Cursor cursor = resolver.query(uri, projection, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				try {
					int id_index = cursor.getColumnIndexOrThrow(Video.Media._ID);
					int title_index = cursor.getColumnIndexOrThrow(Video.Media.TITLE);
					int description_index = cursor.getColumnIndexOrThrow(Video.Media.DESCRIPTION);
					int name_index = cursor.getColumnIndexOrThrow(Video.Media.DISPLAY_NAME);
					
					do {
						int id = cursor.getInt(id_index);
						String title = cursor.getString(title_index);
						String description = cursor.getString(description_index);
						
						if (title == null || title.length() == 0)
							title = cursor.getString(name_index);
							
						mVideos.add(new VideoElement(id, external, title, description));
						
					} while (cursor.moveToNext());
					
				} catch (IllegalArgumentException ex) {
					ex.printStackTrace();
				}
			}
			
			cursor.close();
		}
	}
	
	//--------------------------------------------------------------------------
	private void scan(Context context) {
		ContentResolver resolver = context.getContentResolver();
		
		mVideos.removeAllElements();
		
		scan(resolver, Video.Media.EXTERNAL_CONTENT_URI, true);
		//scan(resolver, Video.Media.INTERNAL_CONTENT_URI, false);
	}
	
	//--------------------------------------------------------------------------
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return mVideos.size();
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return mVideos.get(position);
	}

	//--------------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return mVideos.get(position).mId;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		VideoElement e = mVideos.get(position);
		
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					Res.layout.video_gallery_element, null);
		}
		
		ImageView image = (ImageView)convertView.findViewById(Res.id.VideoGalleryPreview);
		
		int version;
		if (Build.VERSION.SDK.equalsIgnoreCase("CUPCAKE") || Build.VERSION.SDK.equalsIgnoreCase("3")) {
			version = 3;
		} else {
			version = AndroidVersionHelper.getVersion();
		}

		if (version >= BuildVERSION_CODES_ECLAIR){//Build.VERSION_CODES.ECLAIR) {
			
			if (e.mThumbId == -1) {

				Uri uri = Uri.parse(e.mExternal ? EXTERNAL_THUMBNAILS : INTERNAL_THUMBNAILS);
				
				Cursor cursor = mContext.getContentResolver().query(uri, 
						new String[] {
						VideoThumbnails_ID,	//Video.Thumbnails._ID
						VideoThumbnailsDATA	//Video.Thumbnails.DATA 
						}, 
						VideoThumbnailsVIDEO_ID	//Video.Thumbnails.VIDEO_ID
						 + '=' + e.mId, null, null);
				
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							e.mThumbId = cursor.getInt(cursor.getColumnIndexOrThrow(VideoThumbnails_ID));			//Video.Thumbnails._ID));
							e.mThumbFilePath = cursor.getString(cursor.getColumnIndexOrThrow(VideoThumbnailsDATA));	//Video.Thumbnails.DATA));
						} catch (IllegalArgumentException ex) {
							ex.printStackTrace();
						}
					}
					
					cursor.close();
				}
				
				if (e.mThumbId == -1)
					e.mThumbId = -2;
			}
		
			if (e.mThumbId >= 0) {
				//image.setVisibility(View.VISIBLE);
				if(e.mThumbFilePath == null){
					Uri uri = Uri.parse(e.mExternal ? EXTERNAL_THUMBNAILS : INTERNAL_THUMBNAILS);
					image.setImageURI(Uri.withAppendedPath(uri, "" + e.mThumbId));
				}else{
					image.setImageBitmap(Utilities.loadBitmap(e.mThumbFilePath, 128, 96));
				}
			} else {
				//image.setVisibility(View.GONE);
				image.setImageResource(Res.drawable.icon_video);
			}
		} else {
			//image.setVisibility(View.GONE);
			image.setImageResource(Res.drawable.icon_video);
		}
		
		TextView text = (TextView)convertView.findViewById(Res.id.VideoGalleryTitle);
		text.setText(e.mTitle);
		
		text = (TextView)convertView.findViewById(Res.id.VideoGalleryDescription);
		if (e.mDescription != null && e.mDescription.length() > 0) {
			text.setVisibility(View.VISIBLE);
			text.setText(e.mDescription);
		} else {
			text.setVisibility(View.GONE);
		}
		
		return convertView;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return mVideos.size() == 0;
	}

	//--------------------------------------------------------------------------
	private Vector<DataSetObserver> mDataSetObservers = new Vector<DataSetObserver>();
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.add(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.remove(observer);
	}

}
