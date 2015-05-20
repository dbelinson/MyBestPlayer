/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\dbelinson\\AndroidStudioProjects\\StreamMediaPlayer\\app\\src\\main\\aidl\\com\\simpity\\android\\media\\widgets\\radio\\IRadioWidgetServiceInterface.aidl
 */
package com.simpity.android.media.widgets.radio;
public interface IRadioWidgetServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface
{
private static final java.lang.String DESCRIPTOR = "com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface interface,
 * generating a proxy if needed.
 */
public static com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface))) {
return ((com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface)iin);
}
return new com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface.Stub.Proxy(obj);
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
case TRANSACTION_requestWidgetUpdate:
{
data.enforceInterface(DESCRIPTOR);
this.requestWidgetUpdate();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.simpity.android.media.widgets.radio.IRadioWidgetServiceInterface
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
@Override public void requestWidgetUpdate() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_requestWidgetUpdate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_requestWidgetUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void requestWidgetUpdate() throws android.os.RemoteException;
}
