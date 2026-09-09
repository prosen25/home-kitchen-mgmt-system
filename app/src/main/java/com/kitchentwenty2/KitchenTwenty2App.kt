package com.kitchentwenty2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.kitchentwenty2.data.sync.SyncManager

@HiltAndroidApp
class KitchenTwenty2App : Application() {
    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        // Start background sync manager to process queued writes and enable Firestore local persistence
        try {
            syncManager.start()
        } catch (_: Exception) {
            // swallow errors during startup to avoid crashing the app
        }
    }
}
