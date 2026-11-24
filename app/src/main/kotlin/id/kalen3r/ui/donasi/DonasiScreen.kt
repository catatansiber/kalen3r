package id.kalen3r.ui.donasi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.kalen3r.R
import id.kalen3r.ui.KalenColors

private val Border = Color(0xFFEADFC9)
private val BodyInk = Color(0xFF4A3A22)
private val Muted = Color(0xFF6B5B4B)
private val GoldTint = Color(0xFFFFF1C9)

@Composable
fun DonasiScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val vm: DonasiViewModel = viewModel(
        factory = viewModelFactory { initializer { DonasiViewModel(ResourceQrisSource(appContext)) } },
    )
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(KalenColors.Cream)) {
        Row(Modifier.fillMaxWidth().background(KalenColors.Teal).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(stringResource(R.string.donasi_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.donasi_headline), color = KalenColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.donasi_body), color = BodyInk, fontSize = 15.sp, lineHeight = 22.5.sp)

            when (state) {
                DonasiUiState.HasQr -> {
                    QrCard()
                    val noApp = stringResource(R.string.donasi_share_unavailable)
                    val chooserTitle = stringResource(R.string.donasi_share_chooser)
                    Button(
                        onClick = {
                            val intent = vm.shareIntent(context)
                            if (intent == null) {
                                vm.refresh()
                            } else {
                                try {
                                    context.startActivity(Intent.createChooser(intent, chooserTitle))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, noApp, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KalenColors.Gold, contentColor = KalenColors.Ink),
                    ) { Text(stringResource(R.string.donasi_share), fontWeight = FontWeight.Bold) }
                }
                DonasiUiState.Missing -> MissingCard()
            }

            Text(
                stringResource(R.string.donasi_footer),
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QrCard() {
    val context = LocalContext.current
    val resId = remember {
        context.resources.getIdentifier(QrisResolution.DRAWABLE_NAME, "drawable", context.packageName)
    }
    val penerima = stringResource(R.string.donasi_penerima).trim()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(resId),
                contentDescription = stringResource(R.string.donasi_qris_description),
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f),
            )
            if (penerima.isNotEmpty()) {
                Text(
                    stringResource(R.string.donasi_penerima_label, penerima),
                    color = KalenColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Text(stringResource(R.string.donasi_steps), color = BodyInk, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun MissingCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GoldTint),
        border = BorderStroke(1.dp, KalenColors.Gold),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.donasi_missing_title), color = KalenColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.donasi_missing_body), color = BodyInk, fontSize = 15.sp)
        }
    }
}
