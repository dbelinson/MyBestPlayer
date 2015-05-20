package com.simpity.android.media.utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Scanner;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.simpity.android.media.Res;

public class Utilities {

	//--------------------------------------------------------------------------
	public static int getOsVersion() {
		
		String sdk = Build.VERSION.SDK;
		
		if (sdk != null) {
			try {
				int sdk_code = Integer.parseInt(sdk);
				if (sdk_code == 3) {
					return 3;
				}
			} catch (NumberFormatException ex) {
				if (sdk.equalsIgnoreCase("CUPCAKE")) {
					return 3;
				}
			}
		}

		return Build.VERSION.SDK_INT; 
	}
	
	//--------------------------------------------------------------------------
	public static Bitmap createBitmap(int width, int height) {
		Bitmap image = null; 

		try {
			image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError ex) {
			System.gc();
			try {
				image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			} catch (OutOfMemoryError ex2) {
				ex2.printStackTrace();
			}
		}
		
		return image;
	}
	
	//--------------------------------------------------------------------------
	public static Bitmap loadBitmap(String filename, int width, int height) {
		
		Bitmap image = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, opts);
		int hKoef = opts.outHeight/ height;
		int wKoef = opts.outWidth / width;
		opts.inJustDecodeBounds = false;
		int inSampleSize = (hKoef > 0) ? hKoef : 1;
		if(inSampleSize > wKoef && wKoef != 0)
			inSampleSize = wKoef;
			
		boolean gc_run = false;
		
		for (int n = 1; n <= 2 && image == null; n++) {
			try {
				opts.inSampleSize = inSampleSize;
				image = BitmapFactory.decodeFile(filename, opts);
				break;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				if (!gc_run) {
					System.gc();
					gc_run = true;
				}
			}
		}
		
		return image;
	}
	
	//--------------------------------------------------------------------------
	public static Bitmap loadBitmap(byte[] buffer, int width, int height) {
		
		Bitmap image = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opts);
		int hKoef = opts.outHeight/ height;
		int wKoef = opts.outWidth / width;
		opts.inJustDecodeBounds = false;
		int inSampleSize = (hKoef > 0) ? hKoef : 1;
		
		if (inSampleSize > wKoef && wKoef != 0)
			inSampleSize = wKoef;
		
		boolean gc_run = false;
		
		for (int n = 1; n <= 2 && image == null; n++) {
			try {
				opts.inSampleSize = inSampleSize;
				image = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opts);
				break;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				if (!gc_run) {
					System.gc();
					gc_run = true;
				}
			}
		}
		
		return image;
	}
	
	//--------------------------------------------------------------------------
	private static File getEmmcFolder() {
		File emmc = new File("/emmc");

		if (emmc.exists() && emmc.isDirectory() && emmc.canWrite())
			return emmc;

		return null;
	}

	//--------------------------------------------------------------------------
	public final static String HOME_FOLDER = "StreamMediaPlayer";
	
	public static File getHomeFolder() throws IOException {

		File storage = Environment.getExternalStorageDirectory();
		if (storage == null ||
				!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			
			storage = getEmmcFolder();

			if (storage == null)
				throw new IOException("No external storage");
		}

		File home_folder = new File(storage, HOME_FOLDER);

		if (!home_folder.exists()) {
			home_folder.mkdir();
		}

		return home_folder;
	}
	
	//--------------------------------------------------------------------------
	private static final String BACKUP_FILE_NAME = "links.backup";
	
	public static File getBackupFile() throws IOException {
		File home_folder = getHomeFolder();
		return new File(home_folder, BACKUP_FILE_NAME);
	}


	//--------------------------------------------------------------------------
	public static File getNewSnapshotFile(String description) {

		try {
			File dir = new File(getHomeFolder(), "Snapshot");
			if (!dir.exists()) {
				dir.mkdir();
			}

			Calendar cal = Calendar.getInstance();
			StringBuilder builder = new StringBuilder();
			
			builder.append(cal.get(Calendar.YEAR));
			builder.append('.');
			builder.append(cal.get(Calendar.MONTH));
			builder.append('.');
			builder.append(cal.get(Calendar.DAY_OF_MONTH));
			builder.append(' ');
			builder.append(cal.get(Calendar.HOUR_OF_DAY));
			builder.append("h ");
			builder.append(cal.get(Calendar.MINUTE));
			builder.append("m ");
			builder.append(cal.get(Calendar.SECOND));
			builder.append("s ");
			builder.append(cal.get(Calendar.MILLISECOND));
			builder.append("ms");
			builder.append(' ');
			builder.append(description);
			builder.append(".jpg");
			
			return new File(dir, builder.toString());
			
		} catch (Exception ex) {
			
			ex.printStackTrace();
			return null;
		}
	}
	
	//--------------------------------------------------------------------------
	public static int[] parseIdList(String list) {
		
		if (list != null && list.length() > 0) {
			Vector<String> ids = new Vector<String>();
			Scanner scanner = new Scanner(list);
			scanner.useDelimiter(";");
			
			while (scanner.hasNext()) {
				ids.add(scanner.next());
			}
			
			int[] result = new int[ids.size()];
			int i = 0;
			for (String id : ids) {
				try {
					result[i] = Integer.parseInt(id);
					i++;
					
				} catch (NumberFormatException ex) {
					
					int[] old_ids = result;
					result = new int[old_ids.length - 1];
					if (i > 0) {
						System.arraycopy(old_ids, 0, result, 0, i);
					}
				}
			}
			
			return result;
		}
		
		return new int[0];
	}
	
	//--------------------------------------------------------------------------
	public static AlertDialog showMessage(Context context, int message_id) {
		return showMessage(context, message_id, null);
	}
	
	//--------------------------------------------------------------------------
	public static AlertDialog showMessage(Context context, int message_id, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setNeutralButton(Res.string.ok, listener);
		builder.setMessage(message_id);
		return builder.show();
	}
	
	//--------------------------------------------------------------------------
	public static AlertDialog showQuestion(Context context, String question, 
			DialogInterface.OnClickListener yes_listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(true);
		builder.setPositiveButton(Res.string.yes, yes_listener);
		builder.setNegativeButton(Res.string.no, null);
		builder.setMessage(question);
		return builder.show();
	}
	
	//--------------------------------------------------------------------------
	public static boolean isDebuggable(Context context) {
		try {
			Context app_context = context.getApplicationContext(); 
			PackageInfo info = app_context.getPackageManager().getPackageInfo(app_context.getPackageName(), 0);
			if ((info.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
				return true;
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	//--------------------------------------------------------------------------
	static class ToastRun implements Runnable {

		private final Context mContext;
		private final int mMessageId;
		
		ToastRun(Context context, int message_id) {
			mContext = context;
			mMessageId = message_id;
		}
		
		@Override
		public void run() {
			Toast.makeText(mContext, mContext.getString(mMessageId), Toast.LENGTH_SHORT).show();
			
		}
	}
	
	public static void postToast(Handler handler, Context context, int message_id) {
		handler.post(new ToastRun(context, message_id));
	}
}
