package com.simpity.android.media.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;

import android.content.ContentValues;
import android.util.Log;

import com.simpity.android.protocol.AbsoluteTime;
import com.simpity.android.protocol.BaseTime;
import com.simpity.android.protocol.Rtp;
import com.simpity.android.protocol.Rtsp.ContentType;
import com.simpity.android.protocol.RtspCode;
import com.simpity.android.protocol.RtspField;
import com.simpity.android.protocol.RtspTransport;
import com.simpity.android.protocol.Sdp;
import com.simpity.android.protocol.SdpAttribute;
import com.simpity.android.protocol.SdpLine;
import com.simpity.android.protocol.SdpLineType;
import com.simpity.android.protocol.SdpMediaAnnouncements;

public class RTSPClient {

	public final static int DEFAULT_PORT = 554;

	public final static int OPTIONS_COMMAND			= 0x0001;
	public final static int DESCRIBE_COMMAND		= 0x0002;
	public final static int ANNOUNCE_COMMAND		= 0x0004;
	public final static int SETUP_COMMAND			= 0x0008;
	public final static int PLAY_COMMAND			= 0x0010;
	public final static int PAUSE_COMMAND			= 0x0020;
	public final static int TEARDOWN_COMMAND		= 0x0040;
	public final static int GET_PARAMETER_COMMAND	= 0x0080;
	public final static int SET_PARAMETER_COMMAND	= 0x0100;
	public final static int REDIRECT_COMMAND		= 0x0200;
	public final static int RECORD_COMMAND			= 0x0400;

	public final InetAddress mAddress;
	public int mPort;

	//CSeq - current RTSP command number

	private int CSeq = 1;

	private Socket mSocket;
	private int mSupportCommand = 0xFFFF;

	//--------------------------------------------------------------------------
	private String mContentURL = null;
	private String mVideoContentURL = null;
	private String mAudioContentURL = null;

	private String mResponseCodeText = null;
	private ContentValues mResponseFields = new ContentValues();
	private Sdp mSdpContent = null;
	private String[] mContent = null;
	private String mSession = null;
	private RtspTransport[] mTransport = null;
	private Rtp[] mRtpChannels = null;
	private String hostName = null;

	//--------------------------------------------------------------------------
	public RTSPClient(String urlAddress) throws IOException {

		mContentURL = urlAddress;
		mTransport = new RtspTransport[2];
		mRtpChannels = new Rtp[2];
		mTransport[ContentType.AUDIO.index] = new RtspTransport(ContentType.AUDIO);
		mTransport[ContentType.VIDEO.index] = new RtspTransport(ContentType.VIDEO);
		hostName = urlAddress.split("/")[2];
		String[] addr = hostName.split(":");
		if(addr.length > 0)
			{
				mAddress = InetAddress.getByName(addr[0]);
				hostName = addr[0];
			}
		else
			mAddress = InetAddress.getByName(hostName);
		mPort = DEFAULT_PORT;
		if(addr.length > 1)
		{
			mPort = Integer.parseInt(addr[1]);
		}
		mSocket = new Socket(mAddress, mPort);
	}

	//--------------------------------------------------------------------------
	public void close() {
		try {
			mSocket.close();
		} catch (IOException e) {
			Log.e("Rtsp.close", e.toString());
		}
	}

	//--------------------------------------------------------------------------
	final public Sdp getSDPContent() { return mSdpContent;}
	final public String[] getContent() { return mContent;}
	final public String getResponseText() { return mResponseCodeText;}
	final public ContentValues getResponseFields() { return mResponseFields;}
	final public String getSessionId() { return mSession;}

	//--------------------------------------------------------------------------
	private int sendCommand(String command, byte[] param) {

		if(param != null) {
			if(param.length > 2) {
				command = String.format("%sContent-Length: %d\r\n\r\n",
						command.substring(0, command.length()-2), param.length);
			}
			else
				param = null;
		}

		OutputStream out;
		InputStream in;
		byte[] buffer = StringToUtf8(command);
		int size;

		if(param != null) {
			byte[] buf = buffer;
			int n = 0;

			buffer = new byte[buffer.length + param.length - 2];
			for(byte b : buf) {
				buffer[n] = b; n++;
			}
			for(int i=0; i<param.length-2; i++) {
				buffer[n] = param[i]; n++;
			}
		}

		mResponseFields.clear();
		mContent = null;
		mSdpContent = null;

		Log.d("RTSP command", command);

		try {
			out = mSocket.getOutputStream();
			in = mSocket.getInputStream();

			out.write(buffer);

			buffer = new byte[4096];
			size = in.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			mResponseCodeText = null;
			return RtspCode.IO_EXCEPTION;
		}

		int response_code;

		try {
			if (buffer[0] != 'R' ||
				buffer[1] != 'T' ||
				buffer[2] != 'S' ||
				buffer[3] != 'P' ||
				buffer[4] != '/' ||
				buffer[5] != '1' ||
				buffer[6] != '.' ||
				buffer[7] != '0') {
				return RtspCode.INVALID_RESPONSE;
			}

			int n = 8;
			while(buffer[n] == ' ') n++;

			response_code = 0;
			while(buffer[n] >= '0' && buffer[n] <= '9') {
				response_code = response_code*10 + (buffer[n] - '0');
				n++;
			}

			while(buffer[n] == ' ') n++;

			int len = 0;
			while(buffer[n+len] != '\r') len++;
			mResponseCodeText = new String(buffer, n, len);

			n += len + 2;

			Log.d("RTSP response", "RTSP/1.0 " + response_code + " " + mResponseCodeText);

			String text = Utf8ToString(buffer, n, size-n);

			Scanner scanner = new Scanner(text);
			scanner.useDelimiter("\r\n");

			while(scanner.hasNext()) {
				String str = scanner.next();
				Log.d("RTSP response", str);

				if(str.length() == 0)
					break;

				n = str.indexOf(':');
				if(n > 0) {
					int m = n+1;
					while(str.charAt(m) == ' ') m++;

					mResponseFields.put(str.substring(0, n), str.substring(m));
				}
			}

			if(scanner.hasNext()) {
				String content = mResponseFields.getAsString(RtspField.CONTENT_TYPE);

				if(content != null && content.indexOf("application/sdp") >= 0)
					mSdpContent = new Sdp(scanner);
				else {
					Vector<String> lines = new Vector<String>();

					do {
						String str = scanner.next();
						Log.d("RTSP response", str);

						if(str.length() > 0)
							lines.add(str);
					}
					while(scanner.hasNext());

					if(lines.size() > 0) {
						mContent = new String[lines.size()];
						lines.toArray(mContent);
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			mResponseCodeText = null;
			return RtspCode.INVALID_RESPONSE;
		}

		CSeq++;

		return response_code;
	}

	//--------------------------------------------------------------------------
	final static public byte[] StringToUtf8(String text) {
		int i, size = 0, ch;

		for(i=0; i<text.length(); i++) {
			ch = text.charAt(i) & 0xFFFF;
			if(ch < 0x80)
				size++;
			else if(ch < 0x800)
				size += 2;
			else
				size += 3;
		}

		byte[] out = new byte[size];
		size = 0;

		for(i=0; i<text.length(); i++) {
			ch = text.charAt(i) & 0xFFFF;
			if(ch < 0x80) {
				out[size] = (byte)ch; size++;
			}
			else if(ch < 0x800) {
				out[size] = (byte)((ch >> 6) | 0xC0); size++;
				out[size] = (byte)((ch & 0x3F) | 0x80); size++;
			}
			else {
				out[size] = (byte)((ch >> 12) | 0xE0); size++;
				out[size] = (byte)(((ch >> 6) & 0x3F) | 0x80); size++;
				out[size] = (byte)((ch & 0x3F) | 0x80); size++;
			}
		}
		return out;
	}

	//--------------------------------------------------------------------------
	final static public String Utf8ToString(byte[] buf, int start, int src_len) {
		char[] buffer = new char[src_len];
		int len = 0, n = start;

		src_len += start;

		while(n < src_len) {
			if(buf[n] >= 0) {
				buffer[len] = (char)buf[n];
				n++;
			} else if((buf[n] & 0x60) == 0x40) {
				if( n+1 >= src_len ||
					buf[n+1] >= 0 || (buf[n+1] & 0x40) != 0)
					break; // UTF-8 invalid char

				buffer[len] = (char)((((int)buf[n] & 0x1F) << 6)
						+ ((int)buf[n+1] & 0x3F));
				n += 2;
			} else if((buf[n] & 0x70) == 0x60) {
				if( n+2 >= src_len ||
					buf[n+1] >= 0 || (buf[n+1] & 0x40) != 0 ||
					buf[n+2] >= 0 || (buf[n+2] & 0x40) != 0)
					break; // UTF-8 invalid char

				buffer[len] = (char)((((int)buf[n] & 0xF) << 12)
						+ (((int)buf[n+1] & 0x3F) << 6)
						+ ((int)buf[n+2] & 0x3F));
				n += 3;
			} else {
				if( n+3 >= src_len ||
					buf[n+1] >= 0 || (buf[n+1] & 0x40) != 0 ||
					buf[n+2] >= 0 || (buf[n+2] & 0x40) != 0 ||
					buf[n+3] >= 0 || (buf[n+3] & 0x40) != 0)
					break; // UTF-8 invalid char

				buffer[len] = (char)((((int)buf[n] & 0xF) << 18)
						+ (((int)buf[n+1] & 0x3F) << 12)
						+ (((int)buf[n+2] & 0x3F) << 6)
						+ ((int)buf[n+3] & 0x3F));
				n += 4;
			}
			len++;
		}

		return new String(buffer, 0, len);
	}

	/**
	 * Options() - send command OPTIONS
	 *
	 * An OPTIONS request may be issued at any time, e.g., if the client is
	 * about to try a nonstandard request. It does not influence server state.
	 */
	public int Options(String require, String proxy_require) {
		if((mSupportCommand & OPTIONS_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		String command = String.format(
				"OPTIONS %s RTSP/1.0\r\nCSeq: %d\r\nAccept: application/sdp\r\n",
				mContentURL, CSeq);

		if(require != null && require.length() > 0)
			command += RtspField.REQUIRE + ": " + require + "\r\n";

		if(proxy_require != null && proxy_require.length() > 0)
			command += RtspField.PROXY_REQUIRE + ": " + proxy_require + "\r\n";

		command += "\r\n";

		int code = sendCommand(command, null);
		if(code == RtspCode.NOT_IMPLEMENTED) {
			mSupportCommand &= ~OPTIONS_COMMAND;
		}

		return code;
	}

	/**
	 * Describe() - send command DESCRIBE.
	 *
	 * The DESCRIBE method retrieves the description of a presentation or media
	 * object identified by the request URL from a server. It may use the Accept
	 * header to specify the description formats that the client understands.
	 * The server responds with a description of the requested resource. The
	 * DESCRIBE reply-response pair constitutes the media initialization phase
	 * of RTSP.
	 */
	public int Describe() {
		if((mSupportCommand & DESCRIBE_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		String command = String.format(
				"DESCRIBE %s RTSP/1.0\r\nCSeq: %d\r\nAccept: application/sdp\r\n\r\n",
				mContentURL, CSeq);

		int code = sendCommand(command, null);

		mVideoContentURL = null;
		mAudioContentURL = null;

		switch(code) {
		case RtspCode.NOT_IMPLEMENTED:
			mSupportCommand &= ~DESCRIBE_COMMAND;
			break;

		case RtspCode.OK: {
			String base = mResponseFields.getAsString(RtspField.CONTENT_BASE);

			String url = findControlAttribute(0);
			if(url != null && !"*".equals(url))
				mContentURL = base != null ? base + url : url;
			else
				mContentURL = base;

			int index = mSdpContent.getLineIndex(0, SdpLineType.MEDIA_ANNOUNCEMENTS);

			if (index >= 0) {
				SdpMediaAnnouncements media = (SdpMediaAnnouncements) mSdpContent.get(index);
				url = findControlAttribute(index + 1);

				if (media.Media.equalsIgnoreCase("video")) {
					if (url != null)
						mVideoContentURL = base != null ? base + url : url;
					else
						mVideoContentURL = base;
				} else if (media.Media.equalsIgnoreCase("audio")) {
					if (url != null)
						mAudioContentURL = base != null ? base + url : url;
					else
						mAudioContentURL = base;
				}

				index = mSdpContent.getLineIndex(index + 1,	SdpLineType.MEDIA_ANNOUNCEMENTS);

				if (index >= 0) {
					media = (SdpMediaAnnouncements) mSdpContent.get(index);
					url = findControlAttribute(index + 1);

					if (media.Media.equalsIgnoreCase("video")) {
						if (url != null)
							mVideoContentURL = base != null ? base + url : url;
						else
							mVideoContentURL = base;
					} else if (media.Media.equalsIgnoreCase("audio")) {
						if (url != null)
							mAudioContentURL = base != null ? base + url : url;
						else
							mAudioContentURL = base;
					}
				}
			}
		}
			break;
		}

		return code;
	}

	//--------------------------------------------------------------------------
	private String findControlAttribute(int start) {
		Vector<SdpLine> lines = mSdpContent.getContent();
		SdpAttribute attr;

		for(int i=start; i<lines.size(); i++) {
			SdpLine line = lines.get(i);

			switch(line.getType()) {
			case ATTRIBUTE:
				attr = (SdpAttribute)line;
				if("control".equals(attr.Attribute)) {
					return attr.Value;
				}
				break;
			case MEDIA_ANNOUNCEMENTS:
				return null;
			}
		}

		return null;
	}

	/**
	 * Setup() - send command SETUP.
	 *
	 * The SETUP request for a URI specifies the transport mechanism to be used
	 * for the streamed media.
	 *
	 * @return
	 */
	public int Setup(ContentType type) {
		if((mSupportCommand & SETUP_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		String address = getAddress(type);

		if(address == null)
			return RtspCode.INVALID_ADDRESS;

		StringBuilder builder = new StringBuilder();
		builder.append("SETUP ");
		builder.append(address);
		builder.append(" RTSP/1.0\r\nCSeq: ");
		builder.append(CSeq);
		builder.append("\r\n");
		builder.append("User-Agent: Stream Media Player (Linux;Android)\r\n");
		builder.append(GetTransport(type).toString()+"\r\n");
		builder.append("Blocksize: 1400");
		if(mSession != null) {
			builder.append("\r\n" + RtspField.SESSION + ": ");
			builder.append(mSession);
		}
		builder.append("\r\n\r\n");

		int code = sendCommand(builder.toString(), null);
		if(code == RtspCode.OK) {
			if(mSession == null) {
				String session = mResponseFields.getAsString(RtspField.SESSION);
				if(session != null) {
					int n = session.indexOf(';');
					mSession = n < 0 ? session : session.substring(0, n);
				}
			}

			String responce_transport = mResponseFields.getAsString(RtspField.TRANSPORT);
			if(responce_transport != null)
				mTransport[type.index] = new RtspTransport(responce_transport);
			else
				mTransport[type.index] = GetTransport(type);

		} else if(code == RtspCode.NOT_IMPLEMENTED) {
			mSupportCommand &= ~SETUP_COMMAND;
		}
		mRtpChannels[type.index] = new Rtp(mTransport[type.index].client_port);
		mRtpChannels[type.index].setRtpPacketHandler(mTransport[type.index]);
		return code;
	}

	private RtspTransport GetTransport(ContentType type)
	{
		return mTransport[type.index];
	}

	public OutputStream GetData(ContentType type)
	{
		return mTransport[type.index].GetData();
	}

	//--------------------------------------------------------------------------
	private String getAddress(ContentType type) {
		switch(type) {
		case VIDEO:
			return mVideoContentURL;

		case AUDIO:
			return mAudioContentURL;

		case GLOBAL:
			return mContentURL;
		}
		return null;
	}

	/**
	 * Play() - send command PLAY.
	 *
	 * The PLAY method tells the server to start sending data via the mechanism
	 * specified in SETUP. A client MUST NOT issue a PLAY request until any
	 * outstanding SETUP requests have been acknowledged as successful.
	 *
	 * The PLAY request positions the normal play time to the beginning of the
	 * range specified and delivers stream data until the end of the range is
	 * reached. PLAY requests may be pipelined (queued); a server MUST queue
	 * PLAY requests to be executed in order. That is, a PLAY request arriving
	 * while a previous PLAY request is still active is delayed until the first
	 * has been completed.
	 */
	public int Play(ContentType type, BaseTime start, BaseTime end, AbsoluteTime time) {
		if((mSupportCommand & PLAY_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		if(mSession != null) {
			String address = getAddress(type);

			if(address == null)
				return RtspCode.INVALID_ADDRESS;

			String command = String.format(
					"PLAY %s RTSP/1.0\r\nCSeq: %d\r\nSession: %s\r\n",
					address, CSeq, mSession);

			if(start != null) {
				command += RtspField.RANGE + ": " + start.getPrefix() + "=" + start.getValue() + "-";
				if(end != null) {
					if(!start.getPrefix().equals(end.getPrefix())) {
						return RtspCode.INVALID_RANGE_END;
					}
					command += start.getValue();
				}

				if(time != null) {
					command += ";time=" + time.getValue();
				}
			} else if(end != null || time != null) {
				return RtspCode.INVALID_RANGE_START;
			}

			command += "\r\n";

			int code = sendCommand(command, null);

			try {
				switch (type) {
				case AUDIO:
					mRtpChannels[type.index].start(hostName, mTransport[type.index].server_port);
				break;
				case VIDEO:
					mRtpChannels[type.index].start(hostName, mTransport[type.index].server_port);
				break;
				case GLOBAL:
					mRtpChannels[ContentType.AUDIO.index].start(hostName, mTransport[ContentType.AUDIO.index].server_port);
					mRtpChannels[ContentType.VIDEO.index].start(hostName, mTransport[ContentType.VIDEO.index].server_port);
					return RtspCode.OK;
				default:
					break;
				}


			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return RtspCode.IO_EXCEPTION;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return RtspCode.IO_EXCEPTION;
			}

			if(code == RtspCode.NOT_IMPLEMENTED) {
				mSupportCommand &= ~PLAY_COMMAND;
			}
		}

		return RtspCode.INVALID_SESSION_ID;
	}

	/**
	 * Pause() - send command PAUSE.
	 *
	 * The PAUSE request causes the stream delivery to be interrupted (halted)
	 * temporarily. If the request URL names a stream, only playback and
	 * recording of that stream is halted. For example, for audio, this is
	 * equivalent to muting. If the request URL names a presentation or group of
	 * streams, delivery of all currently active streams within the presentation
	 * or group is halted. After resuming playback or recording, synchronization
	 * of the tracks MUST be maintained. Any server resources are kept, though
	 * servers MAY close the session and free resources after being paused for
	 * the duration specified with the timeout parameter of the Session header
	 * in the SETUP message.
	 */
	public int Pause(ContentType type) {
		if((mSupportCommand & PAUSE_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		if(mSession != null) {
			String address = getAddress(type);

			if(address == null)
				return RtspCode.INVALID_ADDRESS;

			String command = String.format(
					"PAUSE %s RTSP/1.0\r\nCSeq: %d\r\nSession: %s\r\n\r\n",
					address, CSeq, mSession);

			int code = sendCommand(command, null);
			switch (type) {
			case AUDIO:
				mRtpChannels[type.index].stop();
				mTransport[type.index].Stop();
			break;
			case VIDEO:
				mRtpChannels[type.index].stop();
				mTransport[type.index].Stop();
			break;
			case GLOBAL:
				mRtpChannels[ContentType.AUDIO.index].stop();
				mTransport[ContentType.AUDIO.index].Stop();

				mRtpChannels[ContentType.VIDEO.index].stop();
				mTransport[ContentType.VIDEO.index].Stop();
			break;
			default:
				break;
			}
			if(code == RtspCode.NOT_IMPLEMENTED) {
				mSupportCommand &= ~PAUSE_COMMAND;
			}
			return code;
		}
		return RtspCode.INVALID_SESSION_ID;
	}

	/**
	 * Teardown() - send command TEARDOWN.
	 *
	 * The TEARDOWN request stops the stream delivery for the given URI, freeing
	 * the resources associated with it. If the URI is the presentation URI for
	 * this presentation, any RTSP session identifier associated with the
	 * session is no longer valid. Unless all transport parameters are defined
	 * by the session description, a SETUP request has to be issued before the
	 * session can be played again.
	 */
	public int Teardown(ContentType type) {
		if((mSupportCommand & TEARDOWN_COMMAND) == 0)
			return RtspCode.NOT_IMPLEMENTED;

		if(mSession != null) {
			String address = getAddress(type);

			if(address == null)
				return RtspCode.INVALID_ADDRESS;

			String command = String.format(
					"TEARDOWN %s RTSP/1.0\r\nCSeq: %d\r\nSession: %s\r\n\r\n",
					address, CSeq, mSession);

			int code = sendCommand(command, null);
			switch (type) {
			case AUDIO:
				mRtpChannels[type.index].stop();
				mTransport[type.index].Stop();
				mRtpChannels[type.index].setRtpPacketHandler(null);
			break;
			case VIDEO:
				mRtpChannels[type.index].stop();
				mTransport[type.index].Stop();
				mRtpChannels[type.index].setRtpPacketHandler(null);
			break;
			case GLOBAL:
				mRtpChannels[ContentType.AUDIO.index].stop();
				mTransport[ContentType.AUDIO.index].Stop();
				mRtpChannels[ContentType.AUDIO.index].setRtpPacketHandler(null);

				mRtpChannels[ContentType.VIDEO.index].stop();
				mTransport[ContentType.VIDEO.index].Stop();
				mRtpChannels[ContentType.VIDEO.index].setRtpPacketHandler(null);
			break;
			default:
				break;
			}

			if(code == RtspCode.OK) {
				mSession = null;
				mContentURL = mVideoContentURL = mAudioContentURL = null;
			} else if(code == RtspCode.NOT_IMPLEMENTED) {
				mSupportCommand &= ~TEARDOWN_COMMAND;
			}

			return code;
		}

		return RtspCode.INVALID_SESSION_ID;
	}
}
