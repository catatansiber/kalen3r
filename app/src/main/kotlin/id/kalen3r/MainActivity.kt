package id.kalen3r

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.kalen3r.ui.AppNav
import id.kalen3r.ui.KalenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KalenTheme {
                AppNav()
            }
        }
    }
}
