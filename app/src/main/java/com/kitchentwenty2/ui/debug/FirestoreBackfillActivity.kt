package com.kitchentwenty2.ui.debug

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.kitchentwenty2.data.sync.LocalRoomFirestoreBackfill
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FirestoreBackfillActivity : androidx.activity.ComponentActivity() {
    @Inject
    lateinit var backfill: LocalRoomFirestoreBackfill

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
        setContentView(statusView)

        if (!intent.getBooleanExtra(EXTRA_CONFIRM, false)) {
            finishWithError("Backfill confirmation was not supplied.")
            return
        }

        statusView.text = "Starting local Room to Firestore backfill..."
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    backfill.run { message ->
                        runOnUiThread { statusView.text = message }
                    }
                }
            }.onSuccess { report ->
                val result = "Backfill complete: customers=${report.customers}, " +
                    "menuItems=${report.menuItems}, expenses=${report.expenses}, " +
                    "orders=${report.orders}, paymentLogs=${report.paymentLogs}, " +
                    "backup=${report.backupPath}"
                Log.i(LOG_TAG, result)
                setResult(Activity.RESULT_OK)
                statusView.text = result
            }.onFailure { error ->
                finishWithError(error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private fun finishWithError(message: String) {
        Log.e(LOG_TAG, "Backfill failed: $message")
        setResult(Activity.RESULT_CANCELED)
        statusView.text = "Backfill failed: $message"
    }

    private companion object {
        const val EXTRA_CONFIRM = "confirm_backfill"
        const val LOG_TAG = "FirestoreBackfill"
    }
}
