package gg.offerwall.sdk

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfferwallActivityTest {

    private val key = "0123456789abcdef0123456789abcdef"

    private fun launch(options: OfferwallOptions = OfferwallOptions(key, "user-42", title = "Earn coins")) =
        Robolectric.buildActivity(
            OfferwallActivity::class.java,
            Intent(RuntimeEnvironment.getApplication(), OfferwallActivity::class.java)
                .putExtra(OfferwallActivity.EXTRA_OPTIONS, options),
        ).setup()

    private fun wallOf(activity: OfferwallActivity): OfferwallView {
        fun find(view: android.view.View): OfferwallView? = when (view) {
            is OfferwallView -> view
            is android.view.ViewGroup -> (0 until view.childCount).asSequence().mapNotNull { find(view.getChildAt(it)) }.firstOrNull()
            else -> null
        }
        return find(activity.window.decorView)!!
    }

    @Test
    fun `the activity opens the user's wall`() {
        val activity = launch().get()
        val loaded = shadowOf(wallOf(activity) as WebView).lastLoadedUrl
        assertTrue(loaded, loaded.startsWith("https://offerwall.gg/wall/$key?userId=user-42"))
    }

    @Test
    fun `javascript and storage are on, file access is off`() {
        val settings = wallOf(launch().get()).settings
        assertTrue(settings.javaScriptEnabled)
        assertTrue(settings.domStorageEnabled)
        assertEquals(false, settings.allowFileAccess)
    }

    @Test
    fun `starting an offer hands it to the system and tells the listener`() {
        var started: String? = null
        OfferwallGG.listener = object : OfferwallListener {
            override fun onOfferStarted(url: String) { started = url }
        }
        val activity = launch().get()
        val wall = wallOf(activity)
        val click = "https://offerwall.gg/wall/$key/click/12?userId=user-42"

        val handled = shadowOf(wall as WebView).webViewClient.shouldOverrideUrlLoading(wall, click)
        assertTrue("the wall does not load the click itself", handled)

        val next = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(next)
        assertEquals(Intent.ACTION_VIEW, next.action)
        assertEquals(Uri.parse(click), next.data)
        assertEquals(click, started)
        OfferwallGG.listener = null
    }

    @Test
    fun `the wall's own pages stay in the WebView`() {
        val wall = wallOf(launch().get())
        val handled = shadowOf(wall as WebView).webViewClient.shouldOverrideUrlLoading(wall, "https://offerwall.gg/wall/$key/offer/12?userId=user-42")
        assertEquals(false, handled)
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun `an intent link cannot start a component of the host app`() {
        val wall = wallOf(launch().get())
        val hostile = "intent:#Intent;action=android.intent.action.VIEW;component=com.host.app/.AdminActivity;S.browser_fallback_url=https%3A%2F%2Fexample.com;end"
        shadowOf(wall as WebView).webViewClient.shouldOverrideUrlLoading(wall, hostile)
        val next = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(next)
        assertNull("the component the page wrote is removed", next.component)
    }

    @Test
    fun `javascript urls are refused without opening anything`() {
        val wall = wallOf(launch().get())
        assertTrue(shadowOf(wall as WebView).webViewClient.shouldOverrideUrlLoading(wall, "javascript:alert(1)"))
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun `closing tells the listener`() {
        var closed = false
        OfferwallGG.listener = object : OfferwallListener {
            override fun onClosed() { closed = true }
        }
        launch().pause().stop().destroy()
        assertTrue(closed)
        OfferwallGG.listener = null
    }

    @Test
    fun `without options the activity closes instead of crashing`() {
        val activity = Robolectric.buildActivity(OfferwallActivity::class.java).setup().get()
        assertTrue(activity.isFinishing)
    }
}
