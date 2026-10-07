package gg.offerwall.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OfferwallTest {

    private val key = "0123456789abcdef0123456789abcdef"
    private val host = "offerwall.gg"

    private fun query(url: String): Map<String, String> =
        URI(url).rawQuery.split("&").associate {
            val (k, v) = it.split("=", limit = 2)
            k to URLDecoder.decode(v, "UTF-8")
        }

    @Test
    fun `the wall url names the placement and the user`() {
        val url = OfferwallOptions(key, "user-42").wallUrl()
        assertTrue(url.startsWith("https://offerwall.gg/wall/$key?"))
        assertEquals("user-42", query(url)["userId"])
        assertEquals("android", query(url)["sdk"])
        assertFalse("no signature unless one was given", url.contains("signature="))
    }

    @Test
    fun `a user id with spaces and unicode survives the round trip`() {
        val id = "jörg & friends/1+1=2"
        assertEquals(id, query(OfferwallOptions(key, id).wallUrl())["userId"])
    }

    @Test
    fun `a signature from the publisher's server is carried as given`() {
        // What the publisher's server computes — the same string the wall verifies.
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec("sk_test".toByteArray(), "HmacSHA256")) }
        val signature = mac.doFinal("appId=$key&userId=user-42".toByteArray()).joinToString("") { "%02x".format(it) }

        val url = OfferwallOptions(key, "user-42", signature).wallUrl()
        assertEquals(signature, query(url)["signature"])
    }

    @Test
    fun `signed extras are passed through but cannot replace the user`() {
        val url = OfferwallOptions(key, "u1", "abc", params = mapOf("ts" to "1700000000", "curName" to "Gems", "userId" to "someone-else")).wallUrl()
        assertEquals(1, Regex("[?&]userId=").findAll(url).count())
        assertEquals("u1", query(url)["userId"])
        assertEquals("1700000000", query(url)["ts"])
        assertEquals("Gems", query(url)["curName"])
    }

    @Test
    fun `a staging base url is respected`() {
        val options = OfferwallOptions(key, "u", baseUrl = "https://staging.example.com/")
        assertTrue(options.wallUrl().startsWith("https://staging.example.com/wall/"))
        assertEquals("staging.example.com", options.wallHost())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an empty user id is refused`() {
        OfferwallOptions(key, " ")
    }

    @Test
    fun `the wall's own pages stay in the app`() {
        for (path in listOf("/wall/$key?userId=u", "/wall/$key/offer/12?userId=u", "/wall/$key/history", "/wall/$key/support", "/view.php?t=abc")) {
            assertEquals(path, LinkPolicy.Decision.STAY, LinkPolicy.decide("https://offerwall.gg$path", host))
        }
    }

    @Test
    fun `starting an offer leaves the app`() {
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("https://offerwall.gg/wall/$key/click/12?userId=u", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("https://offerwall.gg/track/abc?secure_hash=x", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("https://play.google.com/store/apps/details?id=x", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("market://details?id=x", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("intent://scan/#Intent;scheme=zxing;end", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("intent:#Intent;action=android.intent.action.VIEW;end", host))
    }

    @Test
    fun `a host that only looks like ours is not ours`() {
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("https://offerwall.gg.evil.test/wall/x", host))
        assertEquals(LinkPolicy.Decision.EXTERNAL, LinkPolicy.decide("https://evil-offerwall.gg/wall/x", host))
    }

    @Test
    fun `schemes that run code or read files are refused`() {
        for (url in listOf("javascript:alert(1)", "file:///data/data/app/x", "content://contacts/1", "data:text/html,hi")) {
            assertEquals(url, LinkPolicy.Decision.BLOCK, LinkPolicy.decide(url, host))
        }
    }
}
