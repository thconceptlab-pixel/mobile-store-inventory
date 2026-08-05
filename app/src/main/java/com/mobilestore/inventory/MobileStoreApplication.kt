package com.mobilestore.inventory

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Root Application class. Annotated with @HiltAndroidApp to trigger Hilt's
 * code generation, including a base class for the app that serves as the
 * application-level dependency container.
 *
 * This app is 100% offline: no network module, no auth, no cloud sync.
 * All persistence goes through Room (see data/local/AppDatabase.kt).
 */
@HiltAndroidApp
class MobileStoreApplication : Application()
