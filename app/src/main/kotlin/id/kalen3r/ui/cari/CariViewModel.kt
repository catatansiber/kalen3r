package id.kalen3r.ui.cari

import androidx.lifecycle.ViewModel
import id.kalen3r.core.BaliCalendar
import id.kalen3r.core.InvalidWewaranException
import id.kalen3r.core.Pancawara
import id.kalen3r.core.Pawukon
import id.kalen3r.core.Sasih
import id.kalen3r.core.Saptawara
import id.kalen3r.core.Tithi
import id.kalen3r.core.TithiType
import id.kalen3r.ui.month.MonthFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CariTab { OTONAN, ODALAN_PAWUKON, ODALAN_SASIH }

enum class OtonanMode { BIRTH, WEWARAN }

/**
 * One result row. [title] is "Rabu Kliwon, Wuku Landep", [meta] the small grey line
 * "Landep · Rabu · Kliwon"; [daysFromToday] is negative never (searches start today).
 */
data class CariItem(
    val date: LocalDate,
    val meta: String,
    val title: String,
    val daysFromToday: Int,
)

sealed interface CariResult {
    /** An empty [items] list is not an error: the screen shows a neutral "no dates" message. */
    data class Dates(val items: List<CariItem>) : CariResult
    data class Error(val message: String) : CariResult
}

/** Texts the ViewModel needs; the screen fills them from strings.xml. [Default] mirrors strings.xml for JVM tests. */
data class CariMessages(val emptyBirth: String, val invalidWewaran: String) {
    companion object {
        val Default = CariMessages(
            emptyBirth = "Pilih tanggal lahir dulu",
            invalidWewaran = "Kombinasi wewaran tidak valid",
        )
    }
}

data class CariUiState(
    val tab: CariTab = CariTab.OTONAN,
    val otonanMode: OtonanMode = OtonanMode.BIRTH,
    val birth: LocalDate? = null,
    val wuku: Int = 1,
    val saptawara: Saptawara = Saptawara.REDITE,
    val pancawara: Pancawara = Pancawara.PAING,
    val sasih: Sasih = Sasih.KASA,
    val tithiType: TithiType = TithiType.PENANGGAL,
    val tithiNumber: Int = 15,
    val year: Int = LocalDate.now().year,
    val result: CariResult? = null,
)

/** Pure naming/formatting for result rows (JVM-testable). */
object CariFormat {
    fun title(day: id.kalen3r.core.BaliDay) =
        "${MonthFormat.saptawaraName(day.saptawara)} ${MonthFormat.pancawaraName(day.pancawara)}, " +
            "Wuku ${Pawukon.WUKU_NAMES[day.wuku - 1]}"

    fun meta(day: id.kalen3r.core.BaliDay) =
        "${Pawukon.WUKU_NAMES[day.wuku - 1]} · ${MonthFormat.saptawaraName(day.saptawara)} · " +
            MonthFormat.pancawaraName(day.pancawara)

    fun tithiLabel(type: TithiType) = if (type == TithiType.PENANGGAL) "Penanggal" else "Pangelong"
}

class CariViewModel(
    private val calendar: BaliCalendar,
    private val messages: CariMessages = CariMessages.Default,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private var todayDate = today()
    private val _state = MutableStateFlow(CariUiState(year = todayDate.year.coerceIn(MIN_YEAR, MAX_YEAR)))
    val state: StateFlow<CariUiState> = _state.asStateFlow()

    private fun update(f: (CariUiState) -> CariUiState) { _state.value = f(_state.value) }

    fun selectTab(tab: CariTab) = update { it.copy(tab = tab, result = null) }
    fun selectOtonanMode(mode: OtonanMode) = update { it.copy(otonanMode = mode, result = null) }
    fun setBirth(date: LocalDate?) = update { it.copy(birth = date) }
    fun setWuku(wuku: Int) = update { it.copy(wuku = wuku.coerceIn(1, 30)) }
    fun setSaptawara(s: Saptawara) = update { it.copy(saptawara = s) }
    fun setPancawara(p: Pancawara) = update { it.copy(pancawara = p) }
    fun setSasih(s: Sasih) = update { it.copy(sasih = s) }
    fun setTithiType(t: TithiType) = update { it.copy(tithiType = t) }
    fun setTithiNumber(n: Int) = update { it.copy(tithiNumber = n.coerceIn(1, 15)) }
    fun setYear(y: Int) = update { it.copy(year = y.coerceIn(MIN_YEAR, MAX_YEAR)) }
    fun applyPurnama() = update { it.copy(tithiType = TithiType.PENANGGAL, tithiNumber = 15) }
    fun applyTilem() = update { it.copy(tithiType = TithiType.PANGELONG, tithiNumber = 15) }

    /**
     * Re-reads the clock (call on ON_RESUME). When the day changed, the shown results are recomputed so
     * "Hari ini" / "Dalam N hari" stay correct.
     */
    fun refreshToday() {
        val now = today()
        if (now == todayDate) return
        todayDate = now
        if (_state.value.result != null) search()
    }

    fun search() {
        todayDate = today()
        val s = _state.value
        val result = try {
            when (s.tab) {
                CariTab.OTONAN -> searchOtonan(s)
                CariTab.ODALAN_PAWUKON -> CariResult.Dates(
                    calendar.odalanByPawukon(s.wuku, s.saptawara, s.pancawara, todayDate, todayDate.plusYears(3))
                        .take(MAX_RESULTS).map(::item),
                )
                CariTab.ODALAN_SASIH -> CariResult.Dates(
                    calendar.odalanBySasih(s.sasih, Tithi(s.tithiType, s.tithiNumber), s.year).map(::item),
                )
            }
        } catch (e: InvalidWewaranException) {
            CariResult.Error(e.message ?: messages.invalidWewaran)
        } catch (e: IllegalArgumentException) {
            CariResult.Error(e.message ?: messages.invalidWewaran)
        }
        update { it.copy(result = result) }
    }

    private fun searchOtonan(s: CariUiState): CariResult = when (s.otonanMode) {
        OtonanMode.BIRTH -> {
            val birth = s.birth
            if (birth == null) CariResult.Error(messages.emptyBirth)
            else CariResult.Dates(calendar.otonan(birth, OTONAN_COUNT, todayDate).map(::item))
        }
        OtonanMode.WEWARAN -> CariResult.Dates(
            calendar.findByWewaran(s.wuku, s.saptawara, s.pancawara, todayDate, OTONAN_COUNT).map(::item),
        )
    }

    private fun item(date: LocalDate): CariItem {
        val day = calendar.day(date)
        return CariItem(
            date = date,
            meta = CariFormat.meta(day),
            title = CariFormat.title(day),
            daysFromToday = ChronoUnit.DAYS.between(todayDate, date).toInt(),
        )
    }

    companion object {
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2100
        const val MAX_RESULTS = 15
        const val OTONAN_COUNT = 5
    }
}
