package com.simpity.android.media;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.Message;

public class MJpegCameraActivity extends Activity {

	private final static int VIEW_MJPEG_ACTIVITY_CODE = 101;
	public final static String LAST_ENTERED_PORT = "Last entered port";
	
	private Ad mAdView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(Res.layout.mjpeg_camera_select);

		int lastPort = getPreferences(MODE_PRIVATE).getInt(LAST_ENTERED_PORT, Integer.MIN_VALUE);

		EditText editor = (EditText) findViewById(Res.id.mjpeg_port_editor);
		InputFilter[] filters = editor.getFilters();
		InputFilter[] finalFilters = new InputFilter[filters.length + 1];
		
		System.arraycopy(filters, 0, finalFilters, 0, filters.length);
		/*for (int i = 0; i < filters.length; i++) {
			finalFilters[i] = filters[i];
		}*/
		
		finalFilters[finalFilters.length-1] = DigitsKeyListener.getInstance();
		editor.setFilters(finalFilters);

		if(lastPort != Integer.MIN_VALUE)
		{
			editor.setText(Integer.toString(lastPort));
		}
		// editor.setText("1234");

		findViewById(Res.id.mjpeg_start_button).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						StartCommand();
					}
				});
		ListView list = (ListView) findViewById(Res.id.IPsLstView);
		InetAddressListAdapter lstAdapter = new InetAddressListAdapter();

		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface
						.getInetAddresses();

				while (inetAddresses.hasMoreElements()) {
					//InetAddress addrTmp = ;
					lstAdapter.AddItem(inetAddresses.nextElement());
					// TODO
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		list.setAdapter(lstAdapter);
		list.setFocusable(false);
		list.setEnabled(false);
		
		mAdView = new Ad(this);
	}
	
	@Override
    protected void onDestroy() {
		
		if (mAdView != null) {
			mAdView.destroy();
		}

    	super.onDestroy();
    }

	// --------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*MenuItem item = menu.add(Menu.NONE, START_COMMAND, Menu.NONE, "Start");
		item.setIcon(Res.drawable.icon_play);

		item = menu.add(Menu.NONE, BACK_COMMAND, Menu.NONE, "Back");
		item.setIcon(Res.drawable.back);*/
		
		DefaultMenu.create(menu);
		return true;
	}

	// --------------------------------------------------------------------------
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		return DefaultMenu.onItemSelected(this, item);
		
		/*switch (item.getItemId()) {
		case START_COMMAND:
			StartCommand();
			return true;

		case BACK_COMMAND:
			finish();
			return true;
		}
		return false;*/
	}

	// --------------------------------------------------------------------------
	private void StartCommand() {
		EditText editor = (EditText) findViewById(Res.id.mjpeg_port_editor);

		String port = editor.getText().toString();
		if (port.length() == 0) {
			Message.show(this, Res.string.error, Res.string.invalid_rtp_port);
			return;
		}
		try{
			if(Integer.parseInt(port) > 65535)
			{
				Message.show(this, Res.string.error, Res.string.invalid_rtp_port);
				return;
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			Message.show(this, Res.string.error, Res.string.invalid_rtp_port);
			return;
		}

		// Intent intent = new Intent(this, MJpegServer.class);
		// //MJpegCameraView.class);
		Intent intent = new Intent(this, MJpegCameraView.class);
		intent.putExtra(MJpegCameraView.PORT_DATA, port);
		startActivityForResult(intent, VIEW_MJPEG_ACTIVITY_CODE);
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(requestCode == VIEW_MJPEG_ACTIVITY_CODE && resultCode == RESULT_OK) {
			
			String portData = data.getStringExtra(MJpegCameraView.PORT_DATA);
			if (portData != null && portData.length() > 0) {
				getPreferences(MODE_PRIVATE).edit().putInt(LAST_ENTERED_PORT, Integer.parseInt(portData)).commit();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	//--------------------------------------------------------------------------
	class InetAddressListAdapter implements ListAdapter{

		InetAddress addresses[] = new InetAddress[0];

		public void AddItem(InetAddress address)
		{
			if(address != null && !address.getHostAddress().equalsIgnoreCase("127.0.0.1"))
			{
				InetAddress addressesNew[] = new InetAddress[(addresses.length + 1)];
				System.arraycopy(addresses, 0, addressesNew, 0, addresses.length);
				/*for(int i=0; i< addresses.length; i++) {
					addressesNew[i] = addresses[i];
				}*/
				
				addresses = addressesNew;
				addresses[(addresses.length - 1)] = address;
			}
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
			return addresses.length;
		}

		@Override
		public Object getItem(int position) {
			if(position < addresses.length)
				return addresses[position];
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view = null;

			if (convertView == null || !(convertView instanceof LinearLayout)) {
				LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout)inflater.inflate(Res.layout.ip_address_item, null);

			} else
				view = (LinearLayout)convertView;

			TextView text_view = (TextView)view.findViewById(Res.id.IPAddressText);

			if(text_view != null && position < getCount()) {

					text_view.setText(addresses[position].getHostAddress());
				}
			view.setEnabled(false);
			view.setFocusable(false);
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
	}
	}