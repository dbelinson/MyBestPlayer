package com.simpity.android.media.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Log;

public class LinkParser implements Runnable, ContentHandler {

	private final static boolean IS_CHECK_LINK_CONNECTION = false;
	
	private final static String ENTRY = "entry";
	private final static String REF = "ref";
	private final static String HREF = "href";

	private Thread mParseThread = null;
	private Vector<OnEndParseLinkListener> mListeners = new Vector<OnEndParseLinkListener>();
	private boolean mContentElement = false;
	private final Vector<String> mResult = new Vector<String>();
	private String mUrl;

	//-------------------------------------------------------------------------
	public static class MetaInfo {

		public static final int INDEX_URL = 0;
		public static final int INDEX_NAME = 1;
		public static final int INDEX_GENRE = 2;
		public static final int INDEX_CONTENT = 3;
		public static final int INDEX_PROVIDER_URL = 4;

		public final String URL;
		public final String NAME;
		public final String GENRE;
		public final String CONTENT;
		public final String PROVIDER_URL;

		public MetaInfo(String url, String name, String genre, String content,
				String provider_url) {
			URL = url;
			NAME = name;
			GENRE = genre;
			CONTENT = content;
			PROVIDER_URL = provider_url;
		}
	}

	//-------------------------------------------------------------------------
	public interface OnEndParseLinkListener {
		public void parseCompleted(Vector<MetaInfo> links);

		public void onGetFirstLink(MetaInfo firstLink);
	}

	//-------------------------------------------------------------------------
	public void SetOnEndParseLinkListener(OnEndParseLinkListener listener) {
		if (listener != null) {
			if (!mListeners.contains(listener)) {
				mListeners.add(listener);
			}
		}
	}

	//-------------------------------------------------------------------------
	public boolean RemoveOnEndParseLinkListener(OnEndParseLinkListener listener) {
		return listener != null ? mListeners.remove(listener) : false;
	}

	//-------------------------------------------------------------------------
	public void startParse(String url) {
		if (mParseThread != null) {
			mParseThread.interrupt();
			mParseThread = null;
		}
		
		mUrl = url;
		mParseThread = new Thread(this);
		mParseThread.setPriority(Thread.MAX_PRIORITY);
		mParseThread.start();
	}

	//-------------------------------------------------------------------------
	public void terminateParseLink() {
		if (mParseThread != null) {
			mParseThread.interrupt();
			mParseThread = null;
		}
	}

	//-------------------------------------------------------------------------
	@Override
	public void run() {
		
		if (mUrl == null) {
			return;
		}

		Log.i("LinkParser : INPUT URL ", mUrl);
		Vector<String> result = new Vector<String>();
		String url = mUrl.trim().replaceAll("\n", "").replaceAll("\r", "");
		int ind = url.lastIndexOf(".");
		String extension = ind > 0 ? url.substring(url.lastIndexOf(".")) : "";

		if (extension.length() > 4) {
			for (int i = 1; i < extension.length(); i++) {
				if (!Character.isLetter(extension.charAt(i))
						&& !Character.isDigit(extension.charAt(i))) {
					extension = extension.substring(0, i);
					break;
				}
			}
		}
		if (extension.equalsIgnoreCase(".m3u")
				|| extension.equalsIgnoreCase(".pls")) {

			result.addAll(parseAsText(url));

		} else if (extension.equalsIgnoreCase(".asx")
				|| extension.equalsIgnoreCase(".aspx")) {

			result.addAll(parseAsXml(url));

		} else {
			
			if (url.startsWith("http://")) {
				Vector<String> res = parseAsText(url);
				if (res != null && res.size() > 0) {
					result.addAll(res);
				} else {
					result.add(url);
				}
			} else {
				result.add(url);
			}
		}

		boolean firstFlag = false;
		Vector<MetaInfo> finalResult = new Vector<MetaInfo>();
		for (int i = 0; i < result.size(); i++) {
			String string = result.elementAt(i);
			if (string.endsWith("=.asf")) {
				int index = string.indexOf("://");
				if (index >= 0) {
					String correctedUrl = "rtsp" + string.substring(index);
					result.insertElementAt(correctedUrl, i);
					result.removeElement(string);
				}
			}

			MetaInfo info = getMetaInfo(result.elementAt(i));
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return;
			}

			if (info == null)
				continue;

			Log.d("LinkParser : result", info.URL);
			if (!firstFlag) {
				firstFlag = true;
				for (OnEndParseLinkListener listener : mListeners) {
					listener.onGetFirstLink(info);
				}
			}
			finalResult.add(info);
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			return;
		}
		
		for (OnEndParseLinkListener listener : mListeners) {
			listener.parseCompleted(finalResult);
		}
	}

	//-------------------------------------------------------------------------
	private MetaInfo getMetaInfo(String url) {
		MetaInfo result = null;
		if (url != null) {
			if (url.toLowerCase().startsWith("rtsp://")
					|| url.toLowerCase().startsWith("mms:/")) {
				
				result = new MetaInfo(url.replaceFirst("mms:/", "rtsp:/"), null, null, null, null);
				return result;
				
			} else {
				
				HttpURLConnection connection = null;
				try {
					// Create connection
					connection = (HttpURLConnection) new URL(url).openConnection();
					connection.setConnectTimeout(10000);
					connection.connect();

					switch (connection.getResponseCode()) {
					
					case HttpURLConnection.HTTP_OK:
					case -1: {
						String contentType = connection.getContentType();
						if (contentType == null) {
							contentType = "audio/unknown";
						}
						
						Map<String, List<String>> headerFields = connection.getHeaderFields();
						Log.d("Link Parser", " --------- Meta Info (start) ----------");
						for (String headerKey : headerFields.keySet())
						{
							if(headerKey != null)
							{
								List<String> values = headerFields.get(headerKey);
								StringBuilder builder = new StringBuilder(headerKey);
								builder.append('=');
								for (String val : values)
								{
									builder.append(val);
									builder.append(';');
								}
								Log.i("Meta Info", builder.toString());
							}
						}
						Log.d("Link Parser", " --------- Meta Info (end) ----------");
						// if (contentType.toLowerCase().startsWith("audio/") ||
						// contentType.toLowerCase().endsWith("/ogg")) {
						String genre	= connection.getHeaderField("icy-genre");
						String name		= connection.getHeaderField("icy-name");
						String urlText	= connection.getHeaderField("icy-url");
						String bitrate	= connection.getHeaderField("icy-br");
						if (bitrate != null) {
							contentType += " " + bitrate + " kbits";
						}
						result = new MetaInfo(url, name, genre, contentType, urlText);
						break;
					}
					
					case HttpURLConnection.HTTP_MOVED_PERM:
					case HttpURLConnection.HTTP_MOVED_TEMP:
						url = connection.getHeaderField("Location");
						if (url != null) {
							result = getMetaInfo(url);
						}
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					result = null;

				} finally {
					if (connection != null) {
						connection.disconnect();
					}
				}
			}
		}
		return result;
	}

	//-------------------------------------------------------------------------
	private Vector<String> parseAsText(String url) {
		url = url.trim().replaceAll("\n", "").replaceAll("\r", "");
		Vector<String> result = new Vector<String>();
		HttpURLConnection connection = null;
		try {
			// Create connection
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(5000);
			connection.connect();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return result;
			}
			int responceCode = connection.getResponseCode();
			int counter = 0;
			if (responceCode == HttpURLConnection.HTTP_OK || responceCode == -1) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream()));
				String str = reader.readLine();
				while (str != null && counter < 30) {
					try {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							break;
						}
						String[] values = str.split("=", 2);
						if (values.length == 2) {
							str = values[1].trim().replaceAll("\n", "").replaceAll("\r", "");
						}
						str = str.trim().replaceAll("\n", "");
						URL candidateUrl = new URL(str);
						if (IS_CHECK_LINK_CONNECTION) {
							HttpURLConnection candidateConnection = null;
							try {
								candidateConnection = (HttpURLConnection) candidateUrl.openConnection();
								candidateConnection.setConnectTimeout(5000);
								candidateConnection.connect();
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									break;
								}
								int candidateResponceCode = connection.getResponseCode();
								if (candidateResponceCode == HttpURLConnection.HTTP_OK
										|| candidateResponceCode == -1) {
									String contentType = candidateConnection.getContentType();
									if (contentType == null
											|| contentType.toLowerCase().contains("audio")) {
										result.add(str);
									}
								}
							} finally {
								if (candidateConnection != null)
									candidateConnection.disconnect();
							}
						} else {
							result.add(str);
						}
						str = reader.readLine();
						counter = 0;
						
					} catch (Exception e) {
						
						// e.printStackTrace();
						str = reader.readLine();
						counter++;
						continue;
					}
				}
				
				if (reader != null) {
					reader.close();
					reader = null;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return result;
	}

	//-------------------------------------------------------------------------
	private Vector<String> parseAsXml(String url) {
		mResult.clear();
		url = url.trim().replaceAll("\n", "").replaceAll("\r", "");

		HttpURLConnection connection = null;
		try {
			// Create connection
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(5000);
			connection.connect();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return mResult;
			}
			int responceCode = connection.getResponseCode();
			if (responceCode == HttpURLConnection.HTTP_OK || responceCode == -1) {
				StringBuilder stringBuilder = new StringBuilder();
				InputStreamReader isReader = new InputStreamReader(connection.getInputStream());
				char[] buf = new char[512];
				int readedCount = isReader.read(buf);
				
				while (readedCount > 0) {
					stringBuilder.append(buf, 0, readedCount);
					readedCount = isReader.read(buf);
				}
				
				isReader.close();
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					return mResult;
				}

				InputSource inputSource = new InputSource(new StringReader(
						stringBuilder.toString().toLowerCase()));
				Log.d("parseAsXml", stringBuilder.toString());
				SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				XMLReader xr = parser.getXMLReader();
				xr.setContentHandler(this);
				xr.parse(inputSource);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
		}
		
		return mResult;
	}
	
	//-------------------------------------------------------------------------
	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) 
			throws SAXException {
		
		if (mContentElement) {
			if (localName.equalsIgnoreCase(REF)) {
				String value = atts.getValue(HREF.toLowerCase());
				if (value == null) {
					value = atts.getValue(HREF.toUpperCase());
				}
				if (value != null) {
					mResult.add(value);
				}
			}
		} else if (localName.equalsIgnoreCase(ENTRY)) {
			mContentElement = true;
		}
	}

	//-------------------------------------------------------------------------
	@Override
	public void startDocument() throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void setDocumentLocator(Locator locator) {
	}

	//-------------------------------------------------------------------------
	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equalsIgnoreCase(ENTRY)) {
			mContentElement = false;
		}
	}

	//-------------------------------------------------------------------------
	@Override
	public void endDocument() throws SAXException {
	}

	//-------------------------------------------------------------------------
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}
}
