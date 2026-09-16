package com.kitchentwenty2.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.ProfitAndLossReport
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class ReportsRangePreset(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    CUSTOM("Custom Range")
}

data class ReportDateRange(val startDateMillis: Long, val endDateMillis: Long)

data class ReportsHistoryUiState(
    val range: ReportDateRange = currentMonthRange(),
    val selectedPreset: ReportsRangePreset = ReportsRangePreset.THIS_MONTH,
    val report: ProfitAndLossReport = ProfitAndLossReport()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val selection = MutableStateFlow(ReportsRangePreset.THIS_MONTH to currentMonthRange())

    val uiState: StateFlow<ReportsHistoryUiState> = selection
        .flatMapLatest { (preset, range) ->
            orderRepository.getProfitAndLossReport(range.startDateMillis, range.endDateMillis)
                .map { report -> ReportsHistoryUiState(range, preset, report) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReportsHistoryUiState()
        )

    fun onPresetSelected(preset: ReportsRangePreset) {
        if (preset != ReportsRangePreset.CUSTOM) {
            selection.value = preset to rangeFor(preset)
        }
    }

    fun onDateRangeSelected(startDateMillis: Long?, endDateMillis: Long?) {
        if (startDateMillis == null || endDateMillis == null) return
        val start = DateTimeUtils.getStartOfDay(minOf(startDateMillis, endDateMillis))
        val end = DateTimeUtils.getEndOfDay(maxOf(startDateMillis, endDateMillis))
        selection.value = ReportsRangePreset.CUSTOM to ReportDateRange(start, end)
    }
}

private fun rangeFor(preset: ReportsRangePreset): ReportDateRange {
    val calendar = Calendar.getInstance()
    return when (preset) {
        ReportsRangePreset.THIS_MONTH -> monthRange(calendar, 0)
        ReportsRangePreset.LAST_MONTH -> monthRange(calendar, -1)
        ReportsRangePreset.THIS_QUARTER -> {
            val start = (calendar.clone() as Calendar).apply {
                set(Calendar.MONTH, get(Calendar.MONTH) / 3 * 3)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            ReportDateRange(
                DateTimeUtils.getStartOfDay(start.timeInMillis),
                DateTimeUtils.getEndOfDay(System.currentTimeMillis())
            )
        }
        ReportsRangePreset.CUSTOM -> currentMonthRange()
    }
}

private fun monthRange(base: Calendar, offset: Int): ReportDateRange {
    val start = (base.clone() as Calendar).apply {
        add(Calendar.MONTH, offset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val end = (start.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }
    return ReportDateRange(
        DateTimeUtils.getStartOfDay(start.timeInMillis),
        DateTimeUtils.getEndOfDay(end.timeInMillis)
    )
}

private fun currentMonthRange(): ReportDateRange = monthRange(Calendar.getInstance(), 0)
