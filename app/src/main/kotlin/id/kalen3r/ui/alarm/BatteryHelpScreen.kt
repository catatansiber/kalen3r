package id.kalen3r.ui.alarm

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kalen3r.R
import id.kalen3r.ui.KalenColors

private val Border = Color(0xFFEADFC9)

@Composable
fun BatteryHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(KalenColors.Cream)) {
        Row(Modifier.fillMaxWidth().background(KalenColors.Teal).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Text(stringResource(R.string.help_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.help_intro), color = KalenColors.Ink, fontSize = 14.sp, lineHeight = 21.sp)
            Section(R.string.help_general_title, R.string.help_general_body)
            Button(
                onClick = { openBatteryOptimisationSettings(context) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KalenColors.Teal, contentColor = Color.White),
            ) { Text(stringResource(R.string.help_btn_battery), fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { openAppDetails(context) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.help_btn_app), color = KalenColors.Teal, fontWeight = FontWeight.Bold)
            }
            Section(R.string.help_xiaomi_title, R.string.help_xiaomi_body)
            Section(R.string.help_oppo_title, R.string.help_oppo_body)
            Section(R.string.help_huawei_title, R.string.help_huawei_body)
            Section(R.string.help_asus_title, R.string.help_asus_body)
            Section(R.string.help_other_title, R.string.help_other_body)
            Text(stringResource(R.string.help_test_note), color = KalenColors.Ink, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, @StringRes body: Int) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KalenColors.Teal)
            Text(stringResource(body), fontSize = 14.sp, lineHeight = 21.sp, color = KalenColors.Ink)
        }
    }
}
