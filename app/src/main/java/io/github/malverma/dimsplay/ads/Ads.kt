package io.github.malverma.dimsplay.ads

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import io.github.malverma.dimsplay.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread

/** Starts the Google Mobile Ads SDK and tells the UI when ads can be requested. */
object Ads {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initialize(context: Context) {
        val app = context.applicationContext
        val config = InitializationConfig.Builder(app.getString(R.string.admob_app_id)).build()
        // The SDK must be initialized off the main thread to avoid an ANR.
        thread(name = "ads-init") {
            MobileAds.initialize(app, config) { _ready.value = true }
        }
    }
}
