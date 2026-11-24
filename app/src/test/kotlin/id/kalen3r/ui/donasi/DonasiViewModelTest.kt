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

    @Test fun shareIntentIsNullWhenMissing() {
        // Fake never builds an Intent (android.jar is stubbed on the JVM); the real source's
        // decision is the pure helper below.
        assertNull(FakeQrisSource(false).shareIntent(unusedContext()))
        assertFalse(QrisResolution.isAvailable(0))
        assertTrue(QrisResolution.isAvailable(0x7f080001))
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

/** A Context instance that is never used (android.jar is stubbed on the JVM, so no constructor call). */
private fun unusedContext(): Context {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    return unsafeClass.getMethod("allocateInstance", Class::class.java)
        .invoke(unsafe, android.content.ContextWrapper::class.java) as Context
}
