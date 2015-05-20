/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\dbelinson\\AndroidStudioProjects\\StreamMediaPlayer\\app\\src\\main\\aidl\\com\\simpity\\android\\media\\IMediaServiceInterface.aidl
 */
package com.simpity.android.media;
public interface IMediaServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.simpity.android.media.IMediaServiceInterface
{
private static final java.lang.String DESCRIPTOR = "com.simpity.android.media.IMediaServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.simpity.android.media.IMediaServiceInterface interface,
 * generating a proxy if needed.
 */
public static com.simpity.android.media.IMediaServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.simpity.android.media.IMediaServiceInterface))) {
return ((com.simpity.android.media.IMediaServiceInterface)iin);
}
return new com.simpity.android.media.IMediaServiceInterface.Stub.Proxy(obj);
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
case TRANSACTION_getUpdateState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getUpdateState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getUpdatedLinkCount:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.getUpdatedLinkCount(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_startNewRadio:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.startNewRadio(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_startCurrentRadio:
{
data.enforceInterface(DESCRIPTOR);
this.startCurrentRadio();
reply.writeNoException();
return true;
}
case TRANSACTION_startRadio:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.startRadio(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_startRadioPlaylist:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.startRadioPlaylist(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_stopRadio:
{
data.enforceInterface(DESCRIPTOR);
this.stopRadio();
reply.writeNoException();
return true;
}
case TRANSACTION_nextRadio:
{
data.enforceInterface(DESCRIPTOR);
this.nextRadio();
reply.writeNoException();
return true;
}
case TRANSACTION_isRadioPlaying:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isRadioPlaying();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isRadioPlaylistPlaying:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isRadioPlaylistPlaying();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getRadioCurrentAction:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getRadioCurrentAction();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getCurrentRadioId:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getCurrentRadioId();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getRadioInfo:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRadioInfo();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getRadioComposition:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRadioComposition();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.simpity.android.media.IMediaServiceInterface
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
@Override public int getUpdateState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getUpdateState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getUpdatedLinkCount(int type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(type);
mRemote.transact(Stub.TRANSACTION_getUpdatedLinkCount, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void startNewRadio(java.lang.String url) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(url);
mRemote.transact(Stub.TRANSACTION_startNewRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void startCurrentRadio() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startCurrentRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void startRadio(int radio_record_id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(radio_record_id);
mRemote.transact(Stub.TRANSACTION_startRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean startRadioPlaylist(int playlist_id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(playlist_id);
mRemote.transact(Stub.TRANSACTION_startRadioPlaylist, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void stopRadio() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void nextRadio() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_nextRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isRadioPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isRadioPlaying, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isRadioPlaylistPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isRadioPlaylistPlaying, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//void stopReconnecting();

@Override public int getRadioCurrentAction() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRadioCurrentAction, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getCurrentRadioId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentRadioId, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getRadioInfo() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRadioInfo, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getRadioComposition() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRadioComposition, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getUpdateState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getUpdatedLinkCount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_startNewRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_startCurrentRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_startRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_startRadioPlaylist = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_stopRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_nextRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_isRadioPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_isRadioPlaylistPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getRadioCurrentAction = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getCurrentRadioId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getRadioInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getRadioComposition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
}
public int getUpdateState() throws android.os.RemoteException;
public int getUpdatedLinkCount(int type) throws android.os.RemoteException;
public void startNewRadio(java.lang.String url) throws android.os.RemoteException;
public void startCurrentRadio() throws android.os.RemoteException;
public void startRadio(int radio_record_id) throws android.os.RemoteException;
public boolean startRadioPlaylist(int playlist_id) throws android.os.RemoteException;
public void stopRadio() throws android.os.RemoteException;
public void nextRadio() throws android.os.RemoteException;
public boolean isRadioPlaying() throws android.os.RemoteException;
public boolean isRadioPlaylistPlaying() throws android.os.RemoteException;
//void stopReconnecting();

public int getRadioCurrentAction() throws android.os.RemoteException;
public int getCurrentRadioId() throws android.os.RemoteException;
public java.lang.String getRadioInfo() throws android.os.RemoteException;
public java.lang.String getRadioComposition() throws android.os.RemoteException;
}
