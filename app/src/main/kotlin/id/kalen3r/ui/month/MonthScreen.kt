package id.kalen3r.ui.month

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.kalen3r.CalendarProvider
import id.kalen3r.R
import id.kalen3r.core.BaliDay
import id.kalen3r.core.RerainanKind
import id.kalen3r.ui.KalenColors
import java.time.LocalDate

private val GoldTint = Color(0xFFF4D98B)
private val Red = Color(0xFFB3261E)
private val CellBorder = Color(0xFFEADFC9)
private val Muted = Color(0xFF6B5B4B)
private val PurnamaFill = Color(0xFFFFF1C9)
private val PurnamaText = Color(0xFF7A5200)

@Composable
fun MonthScreen(onDayClick: (LocalDate) -> Unit) {
    val vm: MonthViewModel = viewModel(
        factory = viewModelFactory { initializer { MonthViewModel(CalendarProvider.calendar) } },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }
    val state by vm.state.collectAsStateWithLifecycle()
    MonthContent(state, vm::previous, vm::next, onDayClick)
}

@Composable
fun MonthContent(
    state: MonthUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(KalenColors.Cream).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().background(KalenColors.Teal).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_month), tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    MonthFormat.monthTitle(state.year, state.month),
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
                Text(MonthFormat.sasihSubtitle(state.days), color = GoldTint, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month), tint = Color.White)
            }
        }
        if (state.isUnverified) {
            Text(
                stringResource(R.string.month_unverified),
                color = Muted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        WeekdayRow()
        DayGrid(state, onDayClick)
        Legend(state)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WeekdayRow() {
    val names = listOf(
        R.string.weekday_min, R.string.weekday_sen, R.string.weekday_sel, R.string.weekday_rab,
        R.string.weekday_kam, R.string.weekday_jum, R.string.weekday_sab,
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        names.forEachIndexed { i, id ->
            Text(
                stringResource(id), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (i == 0) Red else Muted,
            )
        }
    }
}

@Composable
private fun DayGrid(state: MonthUiState, onDayClick: (LocalDate) -> Unit) {
    val lead = state.days.first().date.dayOfWeek.value % 7 // Sunday = 0
    val cells: List<BaliDay?> = List(lead) { null } + state.days
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f)) {
                        if (day != null) DayCell(day, day.date == state.todayDate) { onDayClick(day.date) }
                    }
                }
            }
        }
    }
}

private fun BaliDay.marker(): RerainanKind? = when {
    isPurnama -> RerainanKind.PURNAMA
    isTilem -> RerainanKind.TILEM
    rerainan.any { it.kind == RerainanKind.KAJENG_KLIWON } -> RerainanKind.KAJENG_KLIWON
    rerainan.isNotEmpty() -> RerainanKind.OTHER
    else -> null
}

@Composable
private fun DayCell(day: BaliDay, isToday: Boolean, onClick: () -> Unit) {
    val marker = day.marker()
    val fill = when (marker) {
        RerainanKind.PURNAMA -> PurnamaFill
        RerainanKind.TILEM -> KalenColors.Ink
        else -> Color.White
    }
    val border = when {
        isToday -> BorderStroke(2.dp, KalenColors.Teal)
        marker == RerainanKind.PURNAMA -> BorderStroke(2.dp, KalenColors.Gold)
        else -> BorderStroke(1.dp, CellBorder)
    }
    val numberColor = when {
        marker == RerainanKind.TILEM -> Color.White
        day.date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Red
        else -> KalenColors.Ink
    }
    val label = when (marker) {
        RerainanKind.PURNAMA -> stringResource(R.string.marker_purnama)
        RerainanKind.TILEM -> stringResource(R.string.marker_tilem)
        RerainanKind.KAJENG_KLIWON -> stringResource(R.string.marker_kajeng)
        RerainanKind.OTHER -> stringResource(R.string.marker_other)
        else -> ""
    }
    val labelColor = when (marker) {
        RerainanKind.PURNAMA -> PurnamaText
        RerainanKind.TILEM -> GoldTint
        RerainanKind.KAJENG_KLIWON -> KalenColors.Teal
        else -> Muted
    }
    val pasaran = MonthFormat.pancawaraName(day.pancawara)
    val desc = stringResource(
        R.string.cell_description,
        MonthFormat.longDate(day.date),
        (listOf(pasaran) + day.rerainan.map { it.name }).joinToString(", "),
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp), color = fill, border = border,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).semantics { contentDescription = desc },
    ) {
        Column(
            Modifier.padding(vertical = 4.dp).clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(day.date.dayOfMonth.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = numberColor)
            Text(label, fontSize = 10.sp, color = labelColor, maxLines = 1, modifier = Modifier.heightIn(min = 12.dp))
            Text(pasaran, fontSize = 9.sp, color = if (marker == RerainanKind.TILEM) Color.White else Muted, maxLines = 1)
        }
    }
}

@Composable
private fun Legend(state: MonthUiState) {
    val entries = MonthFormat.legend(state.days)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.legend_title), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
        if (entries.isEmpty()) {
            Text(stringResource(R.string.legend_empty), fontSize = 13.sp, color = KalenColors.Ink)
        }
        entries.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dot = when (e.primary) {
                    RerainanKind.PURNAMA -> KalenColors.Gold
                    RerainanKind.TILEM -> KalenColors.Ink
                    RerainanKind.KAJENG_KLIWON -> KalenColors.Teal
                    else -> Red
                }
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(10.dp))
                Text(
                    "${MonthFormat.shortDay(e.date)} · ${e.names.joinToString(", ")}",
                    fontSize = 13.sp, color = KalenColors.Ink,
                )
            }
        }
    }
}
