package com.oterman.rundemo.service.sync

import android.content.Context
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.util.RLog

class RunningShoeSyncService(
    private val context: Context,
    private val repository: RunningShoeRepository = RunningShoeRepository(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) {
    companion object {
        private const val TAG = "RunningShoeSyncService"
        private const val PULL_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val PREF_LAST_PULL = "running_shoe_last_pull"
    }

    /**
     * Full sync: pull from server then push local changes
     */
    suspend fun fullSync() {
        try {
            pullFromServer()
            syncToServer()
        } catch (e: Exception) {
            RLog.e(TAG, "fullSync failed", e)
        }
    }

    /**
     * Pull shoes from server, with 24h cooldown
     */
    suspend fun pullFromServer(force: Boolean = false) {
        if (!force) {
            val lastPull = preferencesManager.getLongValue(PREF_LAST_PULL)
            if (System.currentTimeMillis() - lastPull < PULL_COOLDOWN_MS) {
                RLog.d(TAG, "pullFromServer skipped: cooldown not expired")
                return
            }
        }

        repository.pullFromServer().onSuccess {
            preferencesManager.saveLongValue(PREF_LAST_PULL, System.currentTimeMillis())
            RLog.d(TAG, "pullFromServer success")
        }.onFailure { e ->
            RLog.e(TAG, "pullFromServer failed", e)
        }
    }

    /**
     * Push unsynced local shoes to server
     */
    suspend fun syncToServer() {
        repository.syncToServer().onSuccess {
            RLog.d(TAG, "syncToServer success")
        }.onFailure { e ->
            RLog.e(TAG, "syncToServer failed", e)
        }
    }

    /**
     * Sync all historical data (for first-time login)
     */
    suspend fun syncHistoricalData() {
        try {
            syncToServer()
            RLog.d(TAG, "syncHistoricalData complete")
        } catch (e: Exception) {
            RLog.e(TAG, "syncHistoricalData failed", e)
        }
    }
}
