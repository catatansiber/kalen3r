package id.kalen3r.ui.month

import androidx.lifecycle.ViewModel
import id.kalen3r.core.BaliCalendar
import id.kalen3r.core.BaliDay
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MonthUiState(
    val year: Int,
    val month: Int,
    val days: List<BaliDay>,
    val isUnverified: Boolean,
    val todayDate: LocalDate,
)

/**
 * Month navigation clamped to 2000-01..2100-12. The facade caches months, so computing is done synchronously
 * (keeps the ViewModel deterministic in JVM tests).
 */
class MonthViewModel(
    private val calendar: BaliCalendar,
    today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val todayDate = today()
    private val _state = MutableStateFlow(build(todayDate.year, todayDate.monthValue))
    val state: StateFlow<MonthUiState> = _state.asStateFlow()

    fun next() = moveBy(1)
    fun previous() = moveBy(-1)

    private fun moveBy(delta: Int) {
        val s = _state.value
        val idx = s.year * 12 + (s.month - 1) + delta
        if (idx < MIN_INDEX || idx > MAX_INDEX) return
        _state.value = build(idx / 12, idx % 12 + 1)
    }

    private fun build(year: Int, month: Int): MonthUiState {
        val idx = (year * 12 + (month - 1)).coerceIn(MIN_INDEX, MAX_INDEX)
        val y = idx / 12
        val m = idx % 12 + 1
        val days = calendar.month(y, m)
        return MonthUiState(y, m, days, days.any { !it.verified }, todayDate)
    }

    companion object {
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2100
        private const val MIN_INDEX = MIN_YEAR * 12
        private const val MAX_INDEX = MAX_YEAR * 12 + 11
    }
}
