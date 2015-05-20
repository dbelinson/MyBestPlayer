package com.simpity.android.media.controls;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.simpity.android.media.Res;

public abstract class SdCardBrowserBase extends ListView {

	SdCardListAdapter mAdapter = new SdCardListAdapter();
	private String[] mExtensions = null;
	private String CurrentFolder = null;
	
	//--------------------------------------------------------------------------
	public interface OnFolderChangeListener {
		public void onChange(File folder);
	}

	private OnFolderChangeListener mOnFolderChangeListener = null;

	//--------------------------------------------------------------------------
	public interface OnFileClickListener {
		public void onClick(File file);
	}

	private OnFileClickListener mOnFileClickListener = null;

	//--------------------------------------------------------------------------
	public SdCardBrowserBase(Context context) {
		super(context);
		init();
	}

	//--------------------------------------------------------------------------
	public SdCardBrowserBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	//--------------------------------------------------------------------------
	public SdCardBrowserBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	//--------------------------------------------------------------------------
	private final void init() {
		mExtensions = getExtensions();
		File mediaDir = Environment.getExternalStorageDirectory();
		if (!mediaDir.canWrite())
			mediaDir = new File("/sdcard");

		if(!mediaDir.exists()) //mediaDir.mkdir();
			mediaDir = new File("/");

		CurrentFolder = mediaDir.getAbsolutePath();

		mAdapter.setCurrentDir(mediaDir);

		setOnItemClickListener(mAdapter);
		
		setAdapter(mAdapter);
	}

	protected final void reInit() {
		mExtensions = getExtensions();
		setCurrentFolder(CurrentFolder);
		setOnItemClickListener(mAdapter);
		setAdapter(mAdapter);
	}

	//--------------------------------------------------------------------------
	public String getCurrentFolder() {
		return CurrentFolder;
	}

	//--------------------------------------------------------------------------
	public void setCurrentFolder(String folder) {
		CurrentFolder = folder;
		mAdapter.setCurrentDir(new File(folder));
	}

	//--------------------------------------------------------------------------
	public void setOnFolderChangeListener(OnFolderChangeListener listener) {
		mOnFolderChangeListener = listener;
	}

	//--------------------------------------------------------------------------
	public void setOnFileClickListener(OnFileClickListener listener) {
		mOnFileClickListener = listener;
	}

	//--------------------------------------------------------------------------
	class SdCardListAdapter implements ListAdapter, AdapterView.OnItemClickListener {

		File mCurrentDir;
		File[] mDirs = new File[0];
		File[] mFiles = new File[0];

		void setCurrentDir(File path) {
			mCurrentDir = path;

			mDirs = path.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});

			if (mDirs != null){
				Arrays.sort(mDirs, new Comparator<File>() {
					@Override
					public int compare(File file1, File file2) {
						return file1.getName().compareToIgnoreCase(file2.getName());
					}
				});
				
				if(path.getParentFile() != null) {
					File[] dirs = mDirs;
					mDirs = new File[mDirs.length + 1];
					mDirs[0] = path.getParentFile();
					
					System.arraycopy(dirs, 0, mDirs, 1, dirs.length);
					/*for(int i=1; i<mDirs.length; i++) {
						mDirs[i] = dirs[i-1];
					}*/
				}
			}
			else{
				mDirs = new File[1];
				mDirs[0] = path.getParentFile();
			}

			mFiles = path.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					String name = pathname.getName();
					int len = name.length(), ext_len;

					if(!pathname.isDirectory()) {
						if(mExtensions != null) {
							for(int i=0; i<mExtensions.length; i++) {
								ext_len = mExtensions[i].length();
								if(len >= ext_len && mExtensions[i].
										equalsIgnoreCase(name.substring(len-ext_len))) {
									return true;
								}
							}
						}
						else
							return true;
					}

					/*if(!pathname.isDirectory() && name.length() > 4) {
						String ext = name.substring(name.length()-4);
						return ext.equalsIgnoreCase(".3gp")
							|| ext.equalsIgnoreCase(".mp4");
					}*/
					return false;
				}
			});
			
			if (mFiles != null) {
				Arrays.sort(mFiles, new Comparator<File>() {
					@Override
					public int compare(File file1, File file2) {
						return file1.getName().compareToIgnoreCase(file2.getName());
					}
				});
			}
		}

		public int getItemPosition(String path){
			for(int i = 0; i < mDirs.length; i++){
				if(mDirs[i].getAbsolutePath().equalsIgnoreCase(path))
					return i;
			}
			int result = mDirs.length;
			for(int i = 0; i < mFiles.length; i++){
				if(mFiles[i].getAbsolutePath().equalsIgnoreCase(path))
					return result + i;
			}
			return 0;
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			return ((mDirs != null) ? mDirs.length : 0 ) + ((mFiles != null) ? mFiles.length : 0);
		}

		@Override
		public Object getItem(int position) {
			if(position < mDirs.length)
				return mDirs[position];

			position -= mDirs.length;
			return position < mFiles.length ? mFiles[position] : null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view = null;

			if (convertView == null || !(convertView instanceof LinearLayout)) {
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout)inflater.inflate(Res.layout.browser_item, null);

			} else
				view = (LinearLayout)convertView;

			ImageView image_view = (ImageView)view.findViewById(Res.id.BrowserItemIcon);
			TextView text_view = (TextView)view.findViewById(Res.id.BrowserItemText);

			if(text_view != null && position < getCount()) {
				if(position == 0 && mCurrentDir.getParentFile() != null) {
					text_view.setText("..");
					image_view.setImageResource(Res.drawable.icon_up_folder);
				} else {
					File file = position < mDirs.length ? mDirs[position] : mFiles[position-mDirs.length];
					String filename = file.getName();
					text_view.setText(filename);

					if(position < mDirs.length)
						image_view.setImageResource(Res.drawable.icon_folder);
					else
						image_view.setImageResource(getIconResource(filename));
				}
			}

			return view;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return getCount() == 0;
		}

		private ArrayList<DataSetObserver> mDataSetObserverList = new ArrayList<DataSetObserver>();

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
		    mDataSetObserverList.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			int index = mDataSetObserverList.indexOf(observer);

		    if(index >= 0 && index < mDataSetObserverList.size())
		       mDataSetObserverList.remove(index);
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if(position < mDirs.length) {
				if(mOnFolderChangeListener != null) {
					mOnFolderChangeListener.onChange(mDirs[position]);
				}
				//setCurrentDir(mDirs[position]);
				setCurrentFolder(mDirs[position].getAbsolutePath());
				invalidateViews();
			} else {
				position -= mDirs.length;
				if(position < mFiles.length) {
					//clickOnFile(mFiles[position].getAbsolutePath());
					if(mOnFileClickListener != null) {
						mOnFileClickListener.onClick(mFiles[position]);
					}
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	abstract protected String[] getExtensions();
	abstract protected int getIconResource(String filename);
	//abstract protected void clickOnFile(String path);
}
