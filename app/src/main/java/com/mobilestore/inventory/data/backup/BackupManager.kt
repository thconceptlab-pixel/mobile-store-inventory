package com.mobilestore.inventory.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mobilestore.inventory.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manual Backup / Restore per the spec — local-only, no cloud. Room's
 * database is a single SQLite file (in WAL mode); rather than closing the
 * live Hilt-singleton database (which every screen holds a reference to),
 * export does a full WAL checkpoint so all data is flushed into the main
 * file, then copies that file. Import copies the chosen file back over the
 * database path; because the app already has that file open, a restart is
 * required afterward for the restored data to show — restartApp() below
 * handles that.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val backupsDir: File
        get() = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    /** Flushes WAL into the main db file, copies it into backupsDir, and returns a shareable content Uri. */
    suspend fun exportBackup(): Uri = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val backupFile = File(backupsDir, "MobileStoreInventory_Backup_$timestamp.db")
        dbFile.copyTo(backupFile, overwrite = true)

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
    }

    /**
     * Copies the picked backup file over the live database file. Returns
     * true on success. The caller MUST prompt the user to restart the app
     * (see restartApp()) — the currently-open database connection won't see
     * the new file's contents until the process restarts.
     */
    suspend fun importBackup(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false

            // Drop stale WAL/SHM side-files so SQLite doesn't try to replay
            // journal entries that belong to the old database.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun listLocalBackups(): List<File> =
        backupsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Force-restarts the process so the freshly-imported database is opened clean. */
    fun restartApp() {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
