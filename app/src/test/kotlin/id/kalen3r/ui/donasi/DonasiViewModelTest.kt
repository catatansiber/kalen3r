package id.kalen3r.ui.donasi

import android.content.Context
import android.content.Intent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeQrisSource(var present: Boolean) : QrisSource {
    override fun hasImage() = present
    override fun shareIntent(context: Context): Intent? = null
}

class DonasiViewModelTest {
    @Test fun showsQrWhenImagePresent() {
        assertEquals(DonasiUiState.HasQr, DonasiViewModel(FakeQrisSource(true)).state.value)
    }

    @Test fun showsFallbackWhenImageMissing() {
        assertEquals(DonasiUiState.Missing, DonasiViewModel(FakeQrisSource(false)).state.value)
    }

    private val uri = Any()
    private fun planner(id: Int, copier: QrisCopier = QrisCopier { _, d -> File(d, "x.png") }) =
        QrisSharePlanner({ id }, copier, { uri })

    @Test fun shareIntentIsNullWhenMissing() {
        val p = planner(0)
        assertFalse(p.hasImage())
        assertNull(p.plan(File("cache")))
    }

    @Test fun shareDecisionWhenPresent() {
        val p = planner(0x7f080001)
        assertTrue(p.hasImage())
        val d = p.plan(File("cache"))!!
        assertEquals(Intent.ACTION_SEND, d.action)
        assertEquals("image/png", d.mimeType)
        assertTrue(d.uri === uri)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, d.flags)
        assertEquals(0, d.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test fun copyFailureYieldsNullNotCrash() {
        val p = planner(0x7f080001) { _, _ -> throw java.io.IOException("disk full") }
        assertNull(p.plan(File("cache")))
    }

    @Test fun donasiStateDoesNotChangeAfterRefresh() {
        val src = FakeQrisSource(true)
        val vm = DonasiViewModel(src)
        vm.refresh()
        assertEquals(DonasiUiState.HasQr, vm.state.value)
        src.present = false
        vm.refresh()
        assertEquals(DonasiUiState.Missing, vm.state.value)
    }

    @Test fun manifestHasNoInternetPermission() {
        val text = File("src/main/AndroidManifest.xml").readText()
        // The comment in the manifest says "No INTERNET permission"; check the permission name only.
        assertFalse(text.contains("android.permission.INTERNET"))
    }

    @Test fun fileProviderExposesOnlyQrisCache() {
        val xml = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(xml.contains("<cache-path"))
        assertTrue(xml.contains("path=\"qris/\""))
        assertFalse(xml.contains("<files-path") || xml.contains("<external"))
    }
}
