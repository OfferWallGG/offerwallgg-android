package gg.offerwall.sdk

import java.io.Serializable
import java.net.URI
import java.net.URLEncoder

/**
 * Who the wall is for.
 *
 * @param publicKey your placement's public key from offerwall.gg
 * @param userId    YOUR id for this user. Rewards are credited against it in
 *                  the postback Offerwall.GG sends to your server.
 * @param signature only if your placement requires signed links: the hex
 *                  HMAC-SHA256 of `appId=<publicKey>&userId=<userId>` with your
 *                  SECRET key, computed ON YOUR SERVER and handed to the app.
 *                  Never put the secret key in an app — anyone can read it out
 *                  of the APK.
 * @param title     shown in the bar above the wall; null shows "Offers"
 * @param baseUrl   only changed for a staging copy of Offerwall.GG
 * @param params    anything else your server signed into the link, passed
 *                  through unchanged — `ts` when your placement's links expire,
 *                  the `cur*` currency values on a partner placement
 */
data class OfferwallOptions @JvmOverloads constructor(
    val publicKey: String,
    val userId: String,
    val signature: String? = null,
    val title: String? = null,
    val baseUrl: String = DEFAULT_BASE_URL,
    val params: Map<String, String> = emptyMap(),
) : Serializable {

    init {
        require(publicKey.isNotBlank()) { "publicKey is empty — copy it from your placement on offerwall.gg" }
        require(userId.isNotBlank()) { "userId is empty — pass the id of the signed-in user" }
    }

    /** The wall URL for this user, signature included when one was given. */
    fun wallUrl(): String {
        val query = buildString {
            append("userId=").append(encode(userId))
            if (!signature.isNullOrBlank()) {
                append("&signature=").append(encode(signature))
            }
            for ((key, value) in params) {
                if (key.isBlank() || key in RESERVED) continue
                append('&').append(encode(key)).append('=').append(encode(value))
            }
            append("&sdk=android&sdkVersion=").append(encode(OfferwallGG.VERSION))
        }

        return baseUrl.trimEnd('/') + "/wall/" + encode(publicKey) + "?" + query
    }

    /** The host the wall is served from; everything else leaves the app. */
    fun wallHost(): String = URI(baseUrl).host.orEmpty().lowercase()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        const val DEFAULT_BASE_URL = "https://offerwall.gg"
        private val RESERVED = setOf("userId", "signature", "sdk", "sdkVersion")
        private const val serialVersionUID = 1L
    }
}
