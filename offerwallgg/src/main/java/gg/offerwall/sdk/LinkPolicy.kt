package gg.offerwall.sdk

import java.net.URI

/**
 * Where a link the wall asks to open should go. Plain Kotlin, no Android
 * types, so it is unit-tested on the JVM.
 *
 * STAY in the wall: our own pages — the list, an offer's details, the rewards
 * and help tabs, a Traffic Task (/view.php), which has to stay visible for its
 * timer to run.
 *
 * LEAVE to the system: starting an offer (/wall/…/click/…) — the advertiser
 * is an app install or a sign-up, which belongs in the Play Store or a real
 * browser where the user is signed in; any other host; and any non-web scheme
 * (market:, intent:, mailto:, tel:).
 *
 * BLOCK: schemes that run code or read files (javascript:, file:, content:,
 * data:) — nothing the wall draws needs them, so a page asking for one is not
 * one of ours.
 */
object LinkPolicy {

    enum class Decision { STAY, EXTERNAL, BLOCK }

    private val blockedSchemes = setOf("javascript", "file", "content", "data", "about")

    private val schemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")

    @JvmStatic
    fun decide(url: String, wallHost: String): Decision {
        val trimmed = url.trim()

        // The scheme is read off the string, not through java.net.URI: valid
        // links such as `intent:#Intent;…;end` are not URIs it will parse.
        val scheme = schemePattern.find(trimmed)?.groupValues?.get(1)?.lowercase() ?: return Decision.BLOCK
        if (scheme in blockedSchemes) {
            return Decision.BLOCK
        }
        if (scheme != "https" && scheme != "http") {
            return Decision.EXTERNAL
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return Decision.BLOCK
        }

        val host = uri.host?.lowercase().orEmpty()
        val ours = host == wallHost || host.endsWith(".$wallHost")
        if (!ours) {
            return Decision.EXTERNAL
        }

        val path = uri.path.orEmpty()
        if (path.startsWith("/wall/") && path.contains("/click/")) {
            return Decision.EXTERNAL
        }
        if (path.startsWith("/track/")) {
            return Decision.EXTERNAL
        }

        return Decision.STAY
    }
}
