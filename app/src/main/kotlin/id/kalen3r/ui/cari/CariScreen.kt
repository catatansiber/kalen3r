package id.kalen3r.ui.cari

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
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
import id.kalen3r.core.Pancawara
import id.kalen3r.core.Pawukon
import id.kalen3r.core.Sasih
import id.kalen3r.core.Saptawara
import id.kalen3r.core.TithiType
import id.kalen3r.ui.KalenColors
import id.kalen3r.ui.month.MonthFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val Muted = Color(0xFF6B5B4B)
private val FieldBorder = Color(0xFFD8CBB0)
private val CardBorder = Color(0xFFEADFC9)
private val ErrorRed = Color(0xFFB3261E)

@Composable
fun CariScreen(onDateClick: (LocalDate) -> Unit) {
    val messages = CariMessages(
        emptyBirth = stringResource(R.string.cari_empty_birth),
        invalidWewaran = stringResource(R.string.cari_invalid_wewaran),
    )
    val vm: CariViewModel = viewModel(
        factory = viewModelFactory { initializer { CariViewModel(CalendarProvider.calendar, messages = messages) } },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }
    val state by vm.state.collectAsStateWithLifecycle()
    CariContent(state, vm, onDateClick)
}

@Composable
private fun CariContent(s: CariUiState, vm: CariViewModel, onDateClick: (LocalDate) -> Unit) {
    Column(Modifier.fillMaxWidth().background(KalenColors.Cream).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(KalenColors.Teal).padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(stringResource(R.string.cari_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pill(R.string.cari_tab_otonan, s.tab == CariTab.OTONAN, Modifier.weight(1f)) { vm.selectTab(CariTab.OTONAN) }
            Pill(R.string.cari_tab_odalan_pawukon, s.tab == CariTab.ODALAN_PAWUKON, Modifier.weight(1.3f)) {
                vm.selectTab(CariTab.ODALAN_PAWUKON)
            }
            Pill(R.string.cari_tab_odalan_sasih, s.tab == CariTab.ODALAN_SASIH, Modifier.weight(1.1f)) {
                vm.selectTab(CariTab.ODALAN_SASIH)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (s.tab) {
                CariTab.OTONAN -> OtonanInputs(s, vm)
                CariTab.ODALAN_PAWUKON -> WewaranInputs(s, vm)
                CariTab.ODALAN_SASIH -> SasihInputs(s, vm)
            }
            val btn = if (s.tab == CariTab.OTONAN) R.string.cari_btn_otonan else R.string.cari_btn_odalan
            Button(
                onClick = vm::search,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KalenColors.Gold, contentColor = KalenColors.Ink),
            ) { Text(stringResource(btn), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Results(s, onDateClick)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Pill(label: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        selected = selected,
        modifier = modifier.heightIn(min = 44.dp).semantics { role = Role.Tab },
        shape = RoundedCornerShape(22.dp),
        color = if (selected) KalenColors.Teal else Color.White,
        border = if (selected) null else BorderStroke(1.dp, FieldBorder),
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(label),
                color = if (selected) Color.White else KalenColors.Ink,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FieldLabel(@androidx.annotation.StringRes res: Int) {
    Text(stringResource(res), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun OtonanInputs(s: CariUiState, vm: CariViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(R.string.cari_mode_birth, s.otonanMode == OtonanMode.BIRTH, Modifier.weight(1f)) {
            vm.selectOtonanMode(OtonanMode.BIRTH)
        }
        Pill(R.string.cari_mode_wewaran, s.otonanMode == OtonanMode.WEWARAN, Modifier.weight(1f)) {
            vm.selectOtonanMode(OtonanMode.WEWARAN)
        }
    }
    if (s.otonanMode == OtonanMode.BIRTH) BirthField(s.birth, vm::setBirth) else WewaranInputs(s, vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthField(birth: LocalDate?, onPick: (LocalDate) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    Column {
        FieldLabel(R.string.cari_birth_label)
        FieldBox(Modifier.clickable(role = Role.Button) { open = true }) {
            Text(
                birth?.let { MonthFormat.longDate(it) } ?: stringResource(R.string.cari_birth_placeholder),
                color = if (birth == null) Muted else KalenColors.Ink, fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (open) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = birth?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            yearRange = 1850..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

                override fun isSelectableYear(year: Int) = year in 1850..today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        open = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.cari_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cari_cancel))
                }
            },
        ) { DatePicker(picker) }
    }
}

/** Boxed 48dp field: white, 12dp radius, #D8CBB0 border. */
@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().height(48.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(
    @androidx.annotation.StringRes label: Int,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            FieldBox(Modifier.menuAnchor()) {
                Text(text(selected), color = KalenColors.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(text(o)) },
                        onClick = { onSelect(o); expanded = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WewaranInputs(s: CariUiState, vm: CariViewModel) {
    Dropdown(R.string.cari_wuku, (1..30).toList(), s.wuku, { "$it · ${Pawukon.WUKU_NAMES[it - 1]}" }, vm::setWuku)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Dropdown(
            R.string.cari_saptawara, Saptawara.entries, s.saptawara, MonthFormat::saptawaraName, vm::setSaptawara,
            Modifier.weight(1f),
        )
        Dropdown(
            R.string.cari_pancawara, Pancawara.entries, s.pancawara, MonthFormat::pancawaraName, vm::setPancawara,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SasihInputs(s: CariUiState, vm: CariViewModel) {
    Dropdown(R.string.cari_sasih, Sasih.entries, s.sasih, MonthFormat::sasihName, vm::setSasih)
    val penanggal = stringResource(R.string.cari_penanggal)
    val pangelong = stringResource(R.string.cari_pangelong)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Dropdown(
            R.string.cari_tithi_type, TithiType.entries, s.tithiType,
            { if (it == TithiType.PENANGGAL) penanggal else pangelong }, vm::setTithiType, Modifier.weight(1f),
        )
        Dropdown(R.string.cari_tithi_number, (1..15).toList(), s.tithiNumber, { it.toString() }, vm::setTithiNumber, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(R.string.cari_purnama, s.tithiType == TithiType.PENANGGAL && s.tithiNumber == 15, Modifier.weight(1f)) {
            vm.applyPurnama()
        }
        Pill(R.string.cari_tilem, s.tithiType == TithiType.PANGELONG && s.tithiNumber == 15, Modifier.weight(1f)) {
            vm.applyTilem()
        }
    }
    Column {
        FieldLabel(R.string.cari_year)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.setYear(s.year - 1) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Remove, stringResource(R.string.cari_year_prev))
            }
            Text(
                s.year.toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.setYear(s.year + 1) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Add, stringResource(R.string.cari_year_next))
            }
        }
    }
}

@Composable
private fun Results(s: CariUiState, onDateClick: (LocalDate) -> Unit) {
    when (val r = s.result) {
        null -> Unit
        is CariResult.Error -> Surface(
            shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, ErrorRed),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(r.message, color = ErrorRed, fontSize = 14.sp, modifier = Modifier.padding(14.dp)) }
        is CariResult.Dates -> {
            if (r.items.isEmpty()) {
                val msg = if (s.tab == CariTab.ODALAN_SASIH) R.string.cari_no_dates else R.string.cari_no_dates_pawukon
                Text(stringResource(msg), color = Muted, fontSize = 14.sp)
                return
            }
            val title = if (s.tab == CariTab.ODALAN_SASIH || s.tab == CariTab.ODALAN_PAWUKON) {
                R.string.cari_results_odalan
            } else R.string.cari_results_otonan
            Text(
                stringResource(title), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            val every = s.tab != CariTab.ODALAN_SASIH
            val otonanList = s.tab == CariTab.OTONAN
            if (otonanList) {
                ResultCard(r.items.first(), every, onDateClick)
                r.items.drop(1).forEachIndexed { i, item ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { onDateClick(item.date) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.cari_otonan_row, i + 2, MonthFormat.longDate(item.date)),
                            fontSize = 14.sp, color = KalenColors.Ink,
                        )
                    }
                }
            } else {
                r.items.forEach { ResultCard(it, every, onDateClick) }
            }
        }
    }
}

@Composable
private fun ResultCard(item: CariItem, every: Boolean, onClick: (LocalDate) -> Unit) {
    Surface(
        onClick = { onClick(item.date) },
        shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.meta, color = Muted, fontSize = 12.sp)
            Text(item.title, color = KalenColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(MonthFormat.longDate(item.date), color = KalenColors.Ink, fontSize = 14.sp)
            val days = when {
                item.daysFromToday == 0 -> stringResource(R.string.cari_today)
                item.daysFromToday > 0 -> stringResource(R.string.cari_in_days, item.daysFromToday)
                else -> stringResource(R.string.cari_days_ago, -item.daysFromToday)
            }
            Text(
                if (every) stringResource(R.string.cari_in_days_every, days) else days,
                color = KalenColors.Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}
