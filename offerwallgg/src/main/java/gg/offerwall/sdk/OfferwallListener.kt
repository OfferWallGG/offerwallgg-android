package gg.offerwall.sdk

/**
 * What the wall tells the app. Every method has a default, so implement only
 * what you need.
 *
 * Rewards are NOT reported here: a conversion is confirmed by the advertiser,
 * sometimes hours later, and Offerwall.GG sends it to YOUR SERVER as a signed
 * postback. Credit users there — a reward the app decided on by itself is a
 * reward anybody can fake.
 */
interface OfferwallListener {
    /** The wall screen is showing. */
    fun onOpened() {}

    /** The wall screen was closed, by the user or by the app. */
    fun onClosed() {}

    /** An offer was started and handed to the store or the browser. */
    fun onOfferStarted(url: String) {}

    /** The wall could not be loaded (no connection, for example). */
    fun onError(description: String) {}
}
