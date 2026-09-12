package com.buge.files;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IInstallerService extends IInterface {
    void install(String installerPackage, ParcelFileDescriptor apk) throws RemoteException;

    abstract class Stub extends Binder implements IInstallerService {
        private static final String DESCRIPTOR = "com.buge.files.IInstallerService";
        private static final int TRANSACTION_INSTALL = IBinder.FIRST_CALL_TRANSACTION;

        public Stub() { attachInterface(this, DESCRIPTOR); }

        public static IInstallerService asInterface(IBinder binder) {
            if (binder == null) return null;
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local instanceof IInstallerService) return (IInstallerService) local;
            return new Proxy(binder);
        }

        @Override public IBinder asBinder() { return this; }

        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            if (code == TRANSACTION_INSTALL) {
                data.enforceInterface(DESCRIPTOR);
                String installer = data.readString();
                ParcelFileDescriptor apk = data.readParcelable(ParcelFileDescriptor.class.getClassLoader());
                try {
                    install(installer, apk);
                    reply.writeNoException();
                } finally {
                    if (apk != null) {
                        try { apk.close(); } catch (java.io.IOException ignored) { }
                    }
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static final class Proxy implements IInstallerService {
            private final IBinder remote;
            Proxy(IBinder remote) { this.remote = remote; }
            @Override public IBinder asBinder() { return remote; }
            @Override public void install(String installerPackage, ParcelFileDescriptor apk) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(installerPackage);
                    data.writeParcelable(apk, 0);
                    remote.transact(TRANSACTION_INSTALL, data, reply, 0);
                    reply.readException();
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
        }
    }
}
