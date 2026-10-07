package gg.offerwall.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * The Offerwall.GG Android SDK.
 *
 *     OfferwallGG.show(activity, OfferwallOptions("YOUR_PUBLIC_KEY", currentUser.id))
 *
 * From Java:
 *
 *     OfferwallGG.show(activity, new OfferwallOptions("YOUR_PUBLIC_KEY", user.getId()));
 *
 * Rewards reach YOUR SERVER as a signed postback, never the app — see
 * [OfferwallListener] for why.
 */
object OfferwallGG {

    const val VERSION: String = BuildConfig.SDK_VERSION

    /** Told when the wall opens and closes. Optional; hold no Activity in it. */
    @JvmStatic
    var listener: OfferwallListener? = null

    /** Open the wall full screen. */
    @JvmStatic
    fun show(context: Context, options: OfferwallOptions) {
        val intent = Intent(context, OfferwallActivity::class.java)
            .putExtra(OfferwallActivity.EXTRA_OPTIONS, options)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** The wall URL, for apps that open it somewhere of their own. */
    @JvmStatic
    fun wallUrl(options: OfferwallOptions): String = options.wallUrl()
}
