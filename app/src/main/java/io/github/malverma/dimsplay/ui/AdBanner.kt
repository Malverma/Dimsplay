package io.github.malverma.dimsplay.ui

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import io.github.malverma.dimsplay.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TAG = "AdBanner"
private val FIRST_RETRY_DELAY = 15.seconds
private val MAX_RETRY_DELAY = 5.minutes

/**
 * Anchored adaptive banner spanning the available width. It takes no space until an ad has
 * loaded, so a pending or failed load never leaves an empty strip; nothing loads until [ready].
 */
@Composable
fun AdBanner(ready: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adUnitId = stringResource(R.string.admob_banner_unit_id)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        var ad by remember(adSize) { mutableStateOf<BannerAd?>(null) }

        LaunchedEffect(ready, adSize) {
            if (!ready) return@LaunchedEffect
            // Retry failed loads (e.g. offline at launch) with backoff, so the banner appears once
            // the connection returns instead of staying gone until the app restarts.
            var retryDelay = FIRST_RETRY_DELAY
            while (ad == null) {
                when (val result = BannerAd.load(BannerAdRequest.Builder(adUnitId, adSize).build())) {
                    is AdLoadResult.Success -> ad = result.ad
                    is AdLoadResult.Failure -> {
                        Log.w(TAG, "Banner failed to load, retrying in $retryDelay: ${result.error}")
                        delay(retryDelay)
                        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
                    }
                }
            }
        }
        ad?.let { loaded ->
            // Destroying the AdView also destroys the ad registered with it.
            key(loaded) {
                AndroidView(
                    factory = { ctx -> AdView(ctx).apply { registerBannerAd(loaded, ctx as Activity) } },
                    modifier = Modifier.fillMaxWidth().height(adSize.height.dp).background(DimColors.Bg),
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}
