package id.kalen3r.ui.alarm

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.kalen3r.R
import id.kalen3r.alarm.AlarmChannels
import id.kalen3r.alarm.AlarmScheduler
import id.kalen3r.alarm.AlarmSettings
import id.kalen3r.alarm.AlarmSettingsData
import id.kalen3r.alarm.AlarmSounds
import id.kalen3r.alarm.RainanGroup
import id.kalen3r.alarm.SoundChoice
import id.kalen3r.ui.KalenColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val GoldTintFill = Color(0xFFFFF1C9)
private val CardBorder = Color(0xFFEADFC9)
private val Muted = Color(0xFF6B5B4B)
private val Hhmm = DateTimeFormatter.ofPattern("HH:mm")

private fun anyOn(s: AlarmSettingsData) = s.triSandya.any { it.enabled } || s.rainanGroups.isNotEmpty() || s.customReminders.isNotEmpty()

@Composable
fun AlarmScreen(onOpenHelp: () -> Unit) {
    val context = LocalContext.current
    val store = remember { AlarmSettings(context) }
    val loaded by store.flow.collectAsStateWithLifecycle(initialValue = null)
    val settings = loaded
    val requestNotifications = rememberNotificationRequester()

    // permission state is re-read whenever the user comes back from a system settings screen
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notifOk = remember(tick) { notificationsEnabled(context) }
    val exactOk = remember(tick) { AlarmScheduler(context).canScheduleExact() }

    // Tri Sandya alarms are on by default, so ask once when the screen opens if nobody has been asked yet
    LaunchedEffect(settings != null) { if (settings != null && anyOn(settings)) requestNotifications() }

    fun change(transform: (AlarmSettingsData) -> AlarmSettingsData) {
        val after = settings?.let(transform)
        AlarmScheduler.updateSettings(context, transform)
        if (after != null && anyOn(after)) requestNotifications()
    }

    Column(Modifier.fillMaxSize().background(KalenColors.Cream)) {
        Row(Modifier.fillMaxWidth().background(KalenColors.Teal).padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(stringResource(R.string.alarm_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (settings == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!notifOk) {
                WarningRow(R.string.warn_notif_off, R.string.warn_notif_button) { openAppNotificationSettings(context) }
            }
            if (!exactOk) {
                WarningRow(R.string.warn_exact_off, R.string.warn_exact_button) { openExactAlarmSettings(context) }
            }
            HelpCard(onOpenHelp)

            SectionLabel(R.string.section_tri_sandya)
            WhiteCard {
                val names = listOf(R.string.slot_pagi, R.string.slot_siang, R.string.slot_sore)
                settings.triSandya.forEachIndexed { i, slot ->
                    if (i > 0) HorizontalDivider(color = CardBorder)
                    val name = stringResource(names[i])
                    SwitchRow(
                        label = name, checked = slot.enabled,
                        switchDescription = stringResource(R.string.slot_switch_description, name),
                        onChange = { on -> change { s -> s.copy(triSandya = s.triSandya.mapIndexed { j, x -> if (j == i) x.copy(enabled = on) else x }) } },
                        trailing = {
                            TimeLink(slot.time, stringResource(R.string.slot_time_description, name, slot.time.format(Hhmm))) { t ->
                                change { s -> s.copy(triSandya = s.triSandya.mapIndexed { j, x -> if (j == i) x.copy(time = t) else x }) }
                            }
                        },
                    )
                }
            }

            SectionLabel(R.string.section_rainan)
            WhiteCard {
                val rows = listOf(
                    RainanGroup.PURNAMA to R.string.rainan_purnama,
                    RainanGroup.TILEM to R.string.rainan_tilem,
                    RainanGroup.KAJENG_KLIWON to R.string.rainan_kajeng_kliwon,
                    RainanGroup.HARI_RAYA_BESAR to R.string.rainan_hari_raya_besar,
                    RainanGroup.ANGGARA_KASIH to R.string.rainan_anggara_kasih,
                    RainanGroup.BUDA_CEMENG to R.string.rainan_buda_cemeng,
                )
                rows.forEach { (group, res) ->
                    val label = stringResource(res)
                    SwitchRow(
                        label = label, checked = group in settings.rainanGroups,
                        switchDescription = stringResource(R.string.rainan_switch_description, label),
                        onChange = { on -> change { s -> s.copy(rainanGroups = if (on) s.rainanGroups + group else s.rainanGroups - group) } },
                    )
                    HorizontalDivider(color = CardBorder)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rainan_time_label), Modifier.weight(1f), color = KalenColors.Ink, fontSize = 15.sp)
                    TimeLink(settings.rainanTime, stringResource(R.string.rainan_time_description, settings.rainanTime.format(Hhmm))) { t ->
                        change { s -> s.copy(rainanTime = t) }
                    }
                }
            }

            SectionLabel(R.string.section_suara)
            WhiteCard {
                val hasFile = remember { AlarmChannels.hasTriSandyaFile(context) }
                val selected = AlarmSounds.effective(settings.sound, hasFile)
                AlarmSounds.available(hasFile).forEach { choice ->
                    val label = stringResource(if (choice == SoundChoice.TRI_SANDYA) R.string.sound_tri_sandya else R.string.sound_default)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected == choice, role = Role.RadioButton, onClick = { change { it.copy(sound = choice) } }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == choice, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp), color = KalenColors.Ink, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCard(onOpenHelp: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = GoldTintFill, border = BorderStroke(1.dp, KalenColors.Gold), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.alarm_help_card_title), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KalenColors.Ink)
            Text(stringResource(R.string.alarm_help_card_text), fontSize = 13.sp, color = KalenColors.Ink, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onOpenHelp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.alarm_help_card_link), color = KalenColors.Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun WarningRow(@StringRes text: Int, @StringRes button: Int, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFDE8E4), border = BorderStroke(1.dp, Color(0xFFB3261E)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(text), fontSize = 14.sp, color = KalenColors.Ink, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(button), color = KalenColors.Teal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(stringResource(text), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp)) { content() }
    }
}

/** Whole label area toggles (big target); the time link on the right is a separate target. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, switchDescription: String, onChange: (Boolean) -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .semantics { contentDescription = switchDescription },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, Modifier.weight(1f), color = KalenColors.Ink, fontSize = 15.sp)
            Switch(
                checked = checked, onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = KalenColors.Teal, checkedThumbColor = Color.White),
            )
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLink(time: LocalTime, description: String, onPicked: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.heightIn(min = 48.dp).padding(start = 8.dp).clickable(role = Role.Button) { open = true }.padding(horizontal = 8.dp).semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time.format(Hhmm), color = KalenColors.Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    if (open) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.time_picker_title)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { open = false; onPicked(LocalTime.of(state.hour, state.minute)) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.time_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.time_picker_cancel)) }
            },
        )
    }
}
