package com.simpity.android.media.dialogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.Utilities;

public class ShareLinkDialog implements Runnable {

	public final static String INTERNET_VIDEO = "Internet Video";
	public final static String INTERNET_RADIO = "Internet Radio";
	public final static String JPEG_WEB_CAMERA = "JPEG Web Camera";

	public final static String SHARE_URL = "http://www.psa-mobile.com/android/smp/posturl.php";
	
	private final Handler mHandler = new Handler();
	//public boolean IsDialogResultYes = false;
	private Activity mContext = null;
	private Thread shareLinkThread = null;
	private WaitDialog mWaitDlg = null;
	private AlertDialog mDialog = null;
	private DialogInterface.OnCancelListener mListener = null;
	private CheckBox mCheckBox = null;
	
	private final String mUrl;
	private final String mPostType;
	private final String mCategory;
	private final String mDescription; 
	private String mEMail;

	//--------------------------------------------------------------------------
	public static void share(Activity context, int iconRes, String url, 
			String post_type, String category, String description, String email) {
	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context); 
		boolean is_show_dlg = prefs.getBoolean(context.getString(Res.string.pref_is_show_share_link_dlg_key), true); 
		boolean is_auto_share = prefs.getBoolean(context.getString(Res.string.pref_is_auto_share_key), false);
		if (is_show_dlg || is_auto_share) {
			
			ShareLinkDialog shareLnkDlg = new ShareLinkDialog(context, iconRes, url, post_type, category, description, email);
			if (is_show_dlg) {
				shareLnkDlg.show();
			} else {
				shareLnkDlg.postLink();
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private ShareLinkDialog(Activity context, int iconRes, String url, 
			String post_type, String category, String description, String email) {
		
		mUrl = url;
		mPostType = post_type;
		mCategory = category;
		mDescription = description; 
		mEMail = email;
		
		init(context, post_type, iconRes);
	}
	
	//--------------------------------------------------------------------------
	public void dismiss() {
		if (mWaitDlg != null && mDialog.isShowing()) {
			mWaitDlg.dismiss();
		}
		if (mDialog != null && mDialog.isShowing()) {
			mDialog.dismiss();
		}
	};
	
	//--------------------------------------------------------------------------
	public void show() {
		if (mDialog != null && !mContext.isFinishing()) {
			try{
				mDialog.show();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//--------------------------------------------------------------------------
	private void init(Activity context, String title, int iconRes) {
		mContext = context;
		View inflateView = LayoutInflater.from(mContext).inflate(Res.layout.sharelink_dialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setView(inflateView);
		builder.setTitle(title);
		builder.setIcon(iconRes);
		builder.setCancelable(true);
		builder.setPositiveButton(mContext.getString(Res.string.yes), clickListener);
		builder.setNegativeButton(mContext.getString(Res.string.no), clickListener);
		mDialog = builder.create();
		mCheckBox = (CheckBox) inflateView.findViewById(Res.id.not_show_again_checkbox);
	}
	
	//--------------------------------------------------------------------------
	public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
		mListener = listener;
	}
	
	//--------------------------------------------------------------------------
	private DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//mContext.getSharedPreferences(StreamMediaActivity.class.getName(), Activity.MODE_PRIVATE).edit().putBoolean(StreamMediaActivity.IS_SHOW_SHARE_LINK_DLG, !mCheckBox.isChecked()).commit();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
			
			editor.putBoolean(mContext.getString(Res.string.pref_is_show_share_link_dlg_key), !mCheckBox.isChecked());			
			boolean IsDialogResultYes = (which == DialogInterface.BUTTON_POSITIVE);
			if (IsDialogResultYes) {
				postLink();
			}
			editor.putBoolean(mContext.getString(Res.string.pref_is_auto_share_key), IsDialogResultYes);
			editor.commit();
			
			dialog.dismiss();
			if (mListener != null) {
				mListener.onCancel(dialog);
			}
		}
	}; 

	//--------------------------------------------------------------------------
	private void performPost(String URL, String PostType, String Category, 
			String Description, String eMail) {
		try {
			// Construct data
			StringBuilder builder = new StringBuilder();
			
			builder.append("url=");
			builder.append(URL);
			
			builder.append("&posttype=");
			if (PostType != null) {
				builder.append(PostType);
			}
			
			builder.append("&cat=");
			if (Category != null) {
				builder.append(Category);
			}
			
			builder.append("&descr=");
			if (Description != null) {
				builder.append(Description);
			}
			
			builder.append("&email=");
			if (eMail != null) {
				builder.append(eMail);
			}
			
			//builder.append("&model=");
			//builder.append(Build.MANUFACTURER);
			builder.append(' ');
			builder.append(Build.MODEL);
			
			builder.append("&aver=");
			builder.append(Build.VERSION.RELEASE);
			
			builder.append("&autopost=1");
			builder.append("&submit=Submit");
			
			String data = URLEncoder.encode(builder.toString(), "UTF-8");
			
			/*String data = URLEncoder.encode("url", "UTF-8") + "=" + URLEncoder.encode(URL, "UTF-8");
			data += "&" + URLEncoder.encode("posttype", "UTF-8") + "=" + URLEncoder.encode(PostType, "UTF-8");
			data += "&" + URLEncoder.encode("cat", "UTF-8") + "=" + URLEncoder.encode(Category, "UTF-8");
			data += "&" + URLEncoder.encode("descr", "UTF-8") + "=" + URLEncoder.encode(Description, "UTF-8");
			data += "&" + URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(eMail, "UTF-8");
			data += "&" + URLEncoder.encode("model", "UTF-8") + "=" + URLEncoder.encode(device_model, "UTF-8");
			data += "&" + URLEncoder.encode("aver", "UTF-8") + "=" + URLEncoder.encode(android_version, "UTF-8");
			data += "&" + URLEncoder.encode("autopost", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
			data += "&" + URLEncoder.encode("submit", "UTF-8") + "=" + URLEncoder.encode("Submit", "UTF-8");*/

			// Create a socket to the host
			String hostname = mContext.getString(Res.string.link_share_site_host_name);
			int port = 80;
			InetAddress addr = InetAddress.getByName(hostname);
			Socket socket = new Socket(addr, port);

			// Send header
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
			wr.write("POST ");
			wr.write(SHARE_URL);
			wr.write(" HTTP/1.0\r\n" +
					"Content-Length: ");
			wr.write(data.length());
			wr.write("\r\n" +
					"Content-Type: application/x-www-form-urlencoded\r\n" +
					"\r\n");

			// Send data
			wr.write(data);
			wr.flush();

			// Get response
			BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line;
			int count = 10;

			try {
				while (count > 0) {
					//TODO
					// Process line...
					line = rd.readLine();
					if (line != null) {
						
						Utilities.postToast(mHandler, mContext, line.contains("200") ?
								Res.string.link_shared_msg : Res.string.link_share_error_msg);
						return;
						
					} else {
						count--;
						Thread.sleep(50);
					}
				}
			} finally {
			
				try {
					wr.close();
				} catch (IOException ex) {
				}
				
				try {
					rd.close();
				} catch (IOException ex) {
				}
				
				try {
					if (socket.isConnected()) {
						socket.close();
					}
				} catch (IOException ex) {
				}
			}
			
			Utilities.postToast(mHandler, mContext, Res.string.link_share_error_msg);
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Utilities.postToast(mHandler, mContext, Res.string.link_share_error_msg);
			
		} finally {
			
			if (mWaitDlg != null)
				mWaitDlg.dismiss();
		}
	}
	
	//--------------------------------------------------------------------------
	public final static String getDeviceIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				for (Enumeration<InetAddress> e = networkInterface.getInetAddresses(); e.hasMoreElements();) {
					InetAddress addr = e.nextElement();

					if (!addr.isLoopbackAddress()) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return null;
	}

	//--------------------------------------------------------------------------
	/*public void PostALink(final String URL, final String PostType, 
			final String Category, final String Description, final String eMail) {
		
		mWaitDlg = new WaitDialog(mContext);
		mWaitDlg.show();
		
		if (shareLinkThread == null || !shareLinkThread.isAlive()) {
			shareLinkThread = new Thread(this);
			shareLinkThread.setPriority(Thread.NORM_PRIORITY);
			shareLinkThread.start();
		}
	}*/

	//--------------------------------------------------------------------------
	public void postLink() {
		mWaitDlg = new WaitDialog(mContext);
		mWaitDlg.show();
		
		if (shareLinkThread == null || !shareLinkThread.isAlive()) {
			shareLinkThread = new Thread(this);
			shareLinkThread.setPriority(Thread.NORM_PRIORITY);
			shareLinkThread.start();
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void run() {
		performPost(mUrl, mPostType, mCategory, mDescription, mEMail);
	}
	
	//--------------------------------------------------------------------------
	public static boolean CheckLinkForSharing(String link) {
		
		String link_lower = link.toLowerCase(); 
		
		if (link_lower.startsWith("content:/") 
				|| link_lower.startsWith("/sdcard/") 
				|| link_lower.contains("192.168.")) {
			return false;
		}
		
		return true;
	}

	//--------------------------------------------------------------------------
	public void setEMail(String mEMail) {
		this.mEMail = mEMail;
	}

	//--------------------------------------------------------------------------
	public String getEMail() {
		return mEMail;
	}
}
