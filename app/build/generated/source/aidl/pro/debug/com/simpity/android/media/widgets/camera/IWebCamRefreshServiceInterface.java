/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\dbelinson\\AndroidStudioProjects\\StreamMediaPlayer\\app\\src\\main\\aidl\\com\\simpity\\android\\media\\widgets\\camera\\IWebCamRefreshServiceInterface.aidl
 */
package com.simpity.android.media.widgets.camera;
public interface IWebCamRefreshServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface
{
private static final java.lang.String DESCRIPTOR = "com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface interface,
 * generating a proxy if needed.
 */
public static com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface))) {
return ((com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface)iin);
}
return new com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface.Stub.Proxy(obj);
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
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.simpity.android.media.widgets.camera.IWebCamRefreshServiceInterface
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
}
}
}
