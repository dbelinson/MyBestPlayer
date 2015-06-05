/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\dbelinson\\AndroidStudioProjects\\MyBestPlayer\\app\\src\\main\\aidl\\com\\simpity\\android\\media\\widgets\\camera\\ICameraWidgetServiceInterface.aidl
 */
package com.simpity.android.media.widgets.camera;
public interface ICameraWidgetServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface
{
private static final java.lang.String DESCRIPTOR = "com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface interface,
 * generating a proxy if needed.
 */
public static com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface))) {
return ((com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface)iin);
}
return new com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_startJpegCamera:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
int _arg2;
_arg2 = data.readInt();
java.lang.String _arg3;
_arg3 = data.readString();
java.lang.String _arg4;
_arg4 = data.readString();
this.startJpegCamera(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_stopJpegCamera:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.stopJpegCamera(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.simpity.android.media.widgets.camera.ICameraWidgetServiceInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void startJpegCamera(int widgetId, java.lang.String url, int refreshTime, java.lang.String username, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(widgetId);
_data.writeString(url);
_data.writeInt(refreshTime);
_data.writeString(username);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_startJpegCamera, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void stopJpegCamera(int widgetId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(widgetId);
mRemote.transact(Stub.TRANSACTION_stopJpegCamera, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_startJpegCamera = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_stopJpegCamera = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void startJpegCamera(int widgetId, java.lang.String url, int refreshTime, java.lang.String username, java.lang.String password) throws android.os.RemoteException;
public void stopJpegCamera(int widgetId) throws android.os.RemoteException;
}
