package id.kalen3r.ui.donasi

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

/** Pure description of the share Intent so the decision logic is JVM-testable. */
data class ShareDecision(
    val action: String,
    val mimeType: String,
    val uri: Any,
    val flags: Int,
)

fun interface QrisCopier {
    /** Copies the drawable [resId] into [cacheDir]/qris and returns the final file. May throw IOException. */
    fun copy(resId: Int, cacheDir: File): File
}

/** Pure planner: resource lookup, copy and Uri creation are injected. */
class QrisSharePlanner(
    private val lookup: (String) -> Int,
    private val copier: QrisCopier,
    private val uriFor: (File) -> Any,
) {
    fun hasImage() = QrisResolution.isAvailable(lookup(QrisResolution.DRAWABLE_NAME))

    fun plan(cacheDir: File): ShareDecision? {
        val id = lookup(QrisResolution.DRAWABLE_NAME)
        if (!QrisResolution.isAvailable(id)) return null
        return try {
            val file = copier.copy(id, cacheDir)
            ShareDecision(
                action = Intent.ACTION_SEND,
                mimeType = "image/png",
                uri = uriFor(file),
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: Exception) {
            null
        }
    }
}

/** Real source: looks the drawable up at runtime so the build succeeds before the owner adds the PNG. */
class ResourceQrisSource(private val appContext: Context) : QrisSource {
    private val planner = QrisSharePlanner(
        lookup = { name -> appContext.resources.getIdentifier(name, "drawable", appContext.packageName) },
        copier = { id, cacheDir ->
            val dir = File(cacheDir, QrisResolution.CACHE_DIR).apply { mkdirs() }
            val tmp = File.createTempFile("qris", ".tmp", dir)
            try {
                appContext.resources.openRawResource(id).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                val target = File(dir, QrisResolution.CACHE_FILE)
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                target
            } finally {
                tmp.delete()
            }
        },
        uriFor = { file -> FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file) },
    )

    override fun hasImage() = planner.hasImage()

    override fun shareIntent(context: Context): Intent? {
        val d = planner.plan(context.cacheDir) ?: return null
        val uri = d.uri as Uri
        return Intent(d.action).apply {
            type = d.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(d.flags)
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
