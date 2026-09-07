package com.kitchentwenty2.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private const val DATE_FORMAT = "dd/MM/yyyy"

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun parseDate(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val date = sdf.parse(dateStr)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun isToday(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) == getStartOfDay(System.currentTimeMillis())
    }

    fun isFuture(timestamp: Long): Boolean {
        return getStartOfDay(timestamp) > getStartOfDay(System.currentTimeMillis())
    }

    fun getDisplayDateLabel(timestamp: Long): String {
        val formatted = formatDate(timestamp)
        return when {
            isToday(timestamp) -> "Today ($formatted)"
            getStartOfDay(timestamp) == getStartOfDay(System.currentTimeMillis() + 86400000) -> "Tomorrow ($formatted)"
            getStartOfDay(timestamp) == getStartOfDay(System.currentTimeMillis() - 86400000) -> "Yesterday ($formatted)"
            else -> formatted
        }
    }
}
