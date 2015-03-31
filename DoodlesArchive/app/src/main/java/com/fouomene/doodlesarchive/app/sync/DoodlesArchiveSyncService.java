package com.fouomene.doodlesarchive.app.sync;

/**
 * Created by FOUOMENE on 19/03/2015. EmailAuthor: fouomenedaniel@gmail.com .
 */
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DoodlesArchiveSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static DoodlesArchiveSyncAdapter sDoodlesArchiveSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("DoodlesArchiveSyncService", "onCreate - DoodlesArchiveSyncService");
        synchronized (sSyncAdapterLock) {
            if (sDoodlesArchiveSyncAdapter == null) {
                sDoodlesArchiveSyncAdapter = new DoodlesArchiveSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sDoodlesArchiveSyncAdapter.getSyncAdapterBinder();
    }
}