package id.kalen3r.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kalen3r.CalendarProvider
import id.kalen3r.R
import id.kalen3r.core.BaliDay
import id.kalen3r.core.RerainanKind
import id.kalen3r.core.Triwara
import id.kalen3r.core.Pawukon
import id.kalen3r.ui.KalenColors
import id.kalen3r.ui.month.MonthFormat

private val GoldTint = Color(0xFFF4D98B)
private val CardBorder = Color(0xFFEADFC9)
private val Muted = Color(0xFF6B5B4B)

/** Main rerainan = first holy day other than Purnama/Tilem/Kajeng Kliwon; those come next; none -> null. */
internal fun mainRerainan(day: BaliDay): String? {
    val minor = setOf(RerainanKind.PURNAMA, RerainanKind.TILEM, RerainanKind.KAJENG_KLIWON)
    return (day.rerainan.firstOrNull { it.kind !in minor } ?: day.rerainan.firstOrNull())?.name
}

@Composable
fun DetailScreen(rawDate: String?, onBack: () -> Unit, onRemind: () -> Unit) {
    val date = remember(rawDate) { parseDetailDate(rawDate) }
    if (date == null) {
        InvalidDate(onBack)
        return
    }
    val day = remember(date) { CalendarProvider.calendar.day(date) }
    DetailContent(day, onBack, onRemind)
}

@Composable
private fun InvalidDate(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(KalenColors.Cream).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.invalid_date_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KalenColors.Ink)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.invalid_date_message), color = KalenColors.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back)) }
    }
}

@Composable
fun DetailContent(day: BaliDay, onBack: () -> Unit, onRemind: () -> Unit) {
    val pasaran = MonthFormat.pancawaraName(day.pancawara)
    val sapta = MonthFormat.saptawaraName(day.saptawara)
    val main = mainRerainan(day) ?: "$sapta $pasaran"
    val others = day.rerainan.map { it.name }.filter { it != main }.distinct()
    Column(Modifier.fillMaxSize().background(KalenColors.Cream).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(KalenColors.Teal).padding(start = 8.dp, end = 16.dp, bottom = 16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Column(Modifier.padding(start = 8.dp)) {
                Text(MonthFormat.longDate(day.date), color = GoldTint, fontSize = 14.sp)
                Text(main, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                if (others.isNotEmpty()) Text(others.joinToString(" · "), color = GoldTint, fontSize = 13.sp)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(stringResource(R.string.wewaran_title)) { WewaranGrid(day) }
            Card(stringResource(R.string.rerainan_title)) {
                if (day.rerainan.isEmpty()) {
                    Text(stringResource(R.string.no_rerainan), color = KalenColors.Ink, fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        day.rerainan.forEach { Text(it.name, color = KalenColors.Ink, fontSize = 14.sp) }
                    }
                }
            }
            if (!day.verified) {
                Text(stringResource(R.string.unverified_notice), color = Muted, fontSize = 12.sp)
            }
            Button(
                onClick = onRemind,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KalenColors.Gold, contentColor = KalenColors.Ink),
            ) { Text(stringResource(R.string.remind_me), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp), color = Color.White,
        border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
            content()
        }
    }
}

@Composable
private fun WewaranGrid(day: BaliDay) {
    val sasih = MonthFormat.sasihName(day.sasih).let {
        if (day.isNampih) stringResource(R.string.nampih_suffix, it) else it
    }
    val triwara = stringResource(
        when (day.triwara) {
            Triwara.PASAH -> R.string.triwara_pasah
            Triwara.BETENG -> R.string.triwara_beteng
            Triwara.KAJENG -> R.string.triwara_kajeng
        },
    )
    val items = listOf(
        stringResource(R.string.wewaran_wuku) to Pawukon.WUKU_NAMES[day.wuku - 1],
        stringResource(R.string.wewaran_saptawara) to MonthFormat.saptawaraName(day.saptawara),
        stringResource(R.string.wewaran_pancawara) to MonthFormat.pancawaraName(day.pancawara),
        stringResource(R.string.wewaran_triwara) to triwara,
        stringResource(R.string.wewaran_sasih) to sasih,
        stringResource(R.string.wewaran_caka) to day.cakaYear.toString(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row {
                row.forEach { (k, v) ->
                    Column(Modifier.weight(1f)) {
                        Text(k, fontSize = 11.sp, color = Muted)
                        Text(v, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KalenColors.Ink)
                    }
                }
            }
        }
    }
}
