package com.simpity.android.media;

//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.net.UnknownHostException;
//import java.util.Calendar;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.simpity.android.media.Res;
import com.simpity.android.media.utils.DefaultMenu;
//import android.content.Intent;
//import android.view.Window;
//import android.widget.Toast;

public class MJpegServer extends Activity implements
		SurfaceHolder.Callback, Camera.PreviewCallback {

//	private final static String LOG_TAG = MJpegServer.class.getSimpleName();

	final static public String PORT_DATA = "port";
	final static public String ADDRESS_DATA = "address";

	private final static int MAX_MPEG_BLOCK = 5;
	private final static int MPEG_BLOCK_SIZE = 188;
//	private final static int MPEG_BLOCK_DATA_SIZE = 184;

//	private String mAddress;
//	private int mPort;
	private SurfaceHolder mHolder;
    Camera mCamera;
    //DatagramSocket mSocket = null;
    //DatagramPacket mPacket = null;
//    private int mSequenceNumber = 1;
    private byte[] mData = new byte[MPEG_BLOCK_SIZE*MAX_MPEG_BLOCK + 12];

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(Res.layout.mjpeg_view);

//		Intent intent = getIntent();

        /*mAddress = intent.getStringExtra(ADDRESS_DATA);

        String port = intent.getStringExtra(PORT_DATA);
        mPort = Integer.parseInt(port);*/

		/*try {
			mPacket = new DatagramPacket(mData, mData.length, InetAddress.getByName(mAddress), mPort);
		} catch (UnknownHostException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}

        try {
			if(mPacket != null)
				mSocket = new DatagramSocket(mPort);
		} catch (SocketException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}*/

        SurfaceView surface = (SurfaceView) findViewById(Res.id.mjpeg_camera_view);
		mHolder = surface.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setSizeFromLayout();

		mData[0] = (byte)0x80;
		mData[1] = (byte)0x37;

		int id = (int)(Math.random() * 0x7FFFFFFF);
		mData[8] = (byte)((id >> 24) & 0xFF);
		mData[9] = (byte)((id >> 16) & 0xFF);
		mData[10] = (byte)((id >> 8) & 0xFF);
		mData[11] = (byte)(id & 0xFF);
    }

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		DefaultMenu.create(menu);
		return true;
	}

	//--------------------------------------------------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultMenu.onItemSelected(this, item);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFormat(PixelFormat.JPEG);
        params.setPreviewSize(320, 240);
        params.setPreviewFrameRate(10);
        mCamera.setParameters(params);
        mCamera.setPreviewCallback(this);
        //mCamera.setPreviewDisplay(holder);
        mCamera.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
        mCamera = null;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Log.d("MJpegServer", "onPreviewFrame");

		// TODO Auto-generated method stub
		/*if(mSocket != null) {
			int time = (int)Calendar.getInstance().getTimeInMillis();
			int size = data.length;
			int block_count = (size + MPEG_BLOCK_DATA_SIZE - 1) / MPEG_BLOCK_DATA_SIZE;
			int packet_count = (block_count + MAX_MPEG_BLOCK - 1) / MAX_MPEG_BLOCK;

			ByteArrayInputStream stream = new ByteArrayInputStream(data, 0, size);

			int n = 0, offset;

			for(int i=0; i<packet_count; i++) {
				offset = 12;

				for(int k=0; k<MAX_MPEG_BLOCK && n<block_count; k++) {
					mData[offset] = 0x47; offset++;
					mData[offset] = 0; offset++;
					if(n == 0) {

					}
					mData[offset] = 0; offset++;
					mData[offset] = 0; offset++;

					if(stream.available() > MPEG_BLOCK_DATA_SIZE) {
						stream.read(mData, offset, MPEG_BLOCK_DATA_SIZE);
					}
					else {
						stream.read(mData, offset, stream.available());
					}
					n++;
				}
			}
		}*/
	}
}
