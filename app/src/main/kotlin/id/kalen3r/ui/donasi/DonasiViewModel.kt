package id.kalen3r.ui.donasi

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the owner-supplied QRIS image comes from. */
interface QrisSource {
    fun hasImage(): Boolean
    fun shareIntent(context: Context): Intent?
}

object QrisResolution {
    const val DRAWABLE_NAME = "qris_donasi"
    const val CACHE_DIR = "qris"
    const val CACHE_FILE = "qris_donasi.png"

    /** Resource id 0 means the owner has not added the PNG yet. */
    fun isAvailable(resId: Int) = resId != 0
}

/** Looks the drawable up at runtime so the build succeeds before the owner adds the PNG. */
class ResourceQrisSource(private val context: Context) : QrisSource {
    private fun resId() =
        context.resources.getIdentifier(QrisResolution.DRAWABLE_NAME, "drawable", context.packageName)

    override fun hasImage() = QrisResolution.isAvailable(resId())

    override fun shareIntent(context: Context): Intent? {
        val id = resId()
        if (!QrisResolution.isAvailable(id)) return null
        return try {
            val dir = File(context.cacheDir, QrisResolution.CACHE_DIR).apply { mkdirs() }
            val file = File(dir, QrisResolution.CACHE_FILE)
            context.resources.openRawResource(id).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }
}

sealed interface DonasiUiState {
    data object HasQr : DonasiUiState
    data object Missing : DonasiUiState
}

class DonasiViewModel(private val source: QrisSource) : ViewModel() {
    private val _state = MutableStateFlow(compute())
    val state: StateFlow<DonasiUiState> = _state.asStateFlow()

    private fun compute(): DonasiUiState = if (source.hasImage()) DonasiUiState.HasQr else DonasiUiState.Missing

    fun refresh() {
        _state.value = compute()
    }

    fun shareIntent(context: Context): Intent? = source.shareIntent(context)
}
