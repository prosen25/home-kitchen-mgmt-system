package com.kitchentwenty2.util

import android.os.Build
import com.kitchentwenty2.data.local.dao.AppErrorLogDao
import com.kitchentwenty2.data.local.entity.AppErrorLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorLogger @Inject constructor(
    private val appErrorLogDao: AppErrorLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logException(throwable: Throwable, screenOrFeatureName: String? = null) {
        scope.launch {
            try {
                val errorLog = AppErrorLogEntity(
                    errorType = throwable.javaClass.simpleName,
                    message = throwable.message ?: "Unknown Error",
                    stackTrace = throwable.stackTraceToString(),
                    screenOrFeatureName = screenOrFeatureName,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
                )
                appErrorLogDao.insertLog(errorLog)
            } catch (e: Exception) {
                // Failsafe to prevent recursive logging failure
                e.printStackTrace()
            }
        }
    }
}
