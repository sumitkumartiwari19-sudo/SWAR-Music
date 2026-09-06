package com.example.ui.ads

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.NeumorphicSurface
import com.example.ui.theme.NeumorphicTheme

private const val TAG = "AdBannerView"
private const val AD_BASE_URL = "https://www.highrevenueformat.com/"

/**
 * Reusable Adsterra WebView Composable supporting all three ad formats:
 * - Banner 320×50
 * - 1:1 Native Banner
 * - Banner 300×250
 *
 * Configured with transparent background, explicit user agent, third-party cookies,
 * DOM storage, JavaScript, loading placeholder, and Neumorphic container styling.
 */
@Composable
fun AdBannerView(
    adUnit: AdUnit,
    modifier: Modifier = Modifier,
    showContainerCard: Boolean = true,
    elevation: Dp = 4.dp,
    hideOnNoFill: Boolean = true
) {
    var isLoaded by remember { mutableStateOf(false) }
    var isNoFill by remember { mutableStateOf(false) }

    // Fallback safety timer: if ad network produces no creative/fill after 7s, gracefully collapse slot
    LaunchedEffect(adUnit) {
        delay(7000)
        if (!isLoaded) {
            Log.d(TAG, "[AdBannerView] Fallback timer elapsed without fill for unit='${adUnit.id}', activating graceful fallback")
            if (hideOnNoFill) {
                isNoFill = true
            }
        }
    }

    // Gracefully collapses or shows fallback if ad network returns no-fill or encounters error
    AnimatedVisibility(
        visible = !isNoFill,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val cornerRadius = when (adUnit) {
            is AdUnit.Banner320x50 -> 10.dp
            is AdUnit.NativeBanner1x1 -> 14.dp
            is AdUnit.Banner300x250 -> 16.dp
        }

        if (showContainerCard) {
            // Container sized precisely to the ad unit's real dimensions — no generic wrapper or blank margin
            NeumorphicSurface(
                modifier = modifier
                    .size(adUnit.widthDp.dp, adUnit.heightDp.dp)
                    .testTag("ad_container_${adUnit.id}"),
                cornerRadius = cornerRadius,
                elevation = elevation
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLoaded) {
                        AdLoadingPlaceholder(adUnit = adUnit)
                    }

                    AdWebView(
                        adUnit = adUnit,
                        onLoaded = {
                            isLoaded = true
                            isNoFill = false
                        },
                        onNoFill = {
                            Log.d(TAG, "[AdBannerView] No-fill callback received for unit='${adUnit.id}'")
                            if (hideOnNoFill) {
                                isNoFill = true
                            }
                        }
                    )

                    // Compact corner AD badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        AdBadge()
                    }
                }
            }
        } else {
            // Flat mode: also sized precisely to the ad unit dimensions
            Box(
                modifier = modifier
                    .size(adUnit.widthDp.dp, adUnit.heightDp.dp)
                    .testTag("ad_container_${adUnit.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (!isLoaded) {
                    AdLoadingPlaceholder(adUnit = adUnit)
                }

                AdWebView(
                    adUnit = adUnit,
                    onLoaded = {
                        isLoaded = true
                        isNoFill = false
                    },
                    onNoFill = {
                        Log.d(TAG, "[AdBannerView] No-fill callback received for unit='${adUnit.id}'")
                        if (hideOnNoFill) {
                            isNoFill = true
                        }
                    }
                )

                // Minimalist overlay badge in top corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    AdBadge()
                }
            }
        }
    }
}

/**
 * Visual "AD" badge meeting platform guidelines.
 */
@Composable
fun AdBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.85f))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AD",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = NeumorphicTheme.colors.accent,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Loading placeholder with subtle skeleton styling while ad resources resolve.
 */
@Composable
private fun AdLoadingPlaceholder(adUnit: AdUnit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(NeumorphicTheme.colors.surfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = NeumorphicTheme.colors.accent.copy(alpha = 0.6f)
            )
            Text(
                text = "Sponsored Content",
                fontSize = 11.sp,
                color = NeumorphicTheme.colors.textSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Internal AndroidView hosting the hardened WebView for Adsterra JS execution.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AdWebView(
    adUnit: AdUnit,
    onLoaded: () -> Unit,
    onNoFill: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(adUnit) {
        onDispose {
            webViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Error cleaning up WebView", e)
                }
            }
            webViewRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Transparent background with software layer type to avoid Mesa rendernode driver issues in emulator
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    allowFileAccess = false
                    allowContentAccess = false
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    // Ensure ad content scales to fit WebView container without intrinsic downscaling
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    // Explicit Mobile User Agent string
                    val defaultUa = settings.userAgentString
                    userAgentString = "$defaultUa Mobile SwarApp"
                }

                // Enable 3rd-party cookies
                try {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set third-party cookies", e)
                }

                // Expose JavaScript bridge for ad fill status detection
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onAdStatus(key: String, filled: Boolean) {
                            Log.d(TAG, "[AdBridge] unit='${adUnit.id}', key='$key', filled=$filled")
                            if (filled) {
                                onLoaded()
                            } else {
                                onNoFill()
                            }
                        }
                    },
                    "AndroidBridge"
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "[WebView onPageStarted] unit='${adUnit.id}' url='$url'")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "[WebView onPageFinished] unit='${adUnit.id}' url='$url'")
                        onLoaded()
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null
                        if (url.endsWith("favicon.ico") || url.contains("/favicon.ico")) {
                            return WebResourceResponse(
                                "image/x-icon",
                                "UTF-8",
                                204,
                                "No Content",
                                mapOf("Cache-Control" to "no-cache"),
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("favicon.ico")) {
                            return
                        }
                        Log.w(
                            TAG,
                            "[WebView Error] unit='${adUnit.id}' url='$url' errCode=${error?.errorCode} desc='${error?.description}' isMainFrame=${request?.isForMainFrame}"
                        )
                        if (request?.isForMainFrame == true || url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com")) {
                            onNoFill()
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("favicon.ico")) {
                            return
                        }
                        Log.w(
                            TAG,
                            "[WebView HttpError] unit='${adUnit.id}' url='$url' status=${errorResponse?.statusCode}"
                        )
                        if (url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com")) {
                            onNoFill()
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        val host = request.url?.host?.lowercase() ?: ""

                        // Do not intercept ad network scripts, resources, or base URL
                        if (host.contains("profitableratecpmnetwork.com") ||
                            host.contains("highrevenueformat.com") ||
                            host.contains("alwingulla.com") ||
                            url.startsWith(AD_BASE_URL) ||
                            url.contains("favicon.ico")
                        ) {
                            return false
                        }

                        // Open external browser only on user click gesture or main frame navigation
                        if (request.hasGesture() || request.isForMainFrame) {
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    true
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to launch external ad URL: $url", e)
                                    false
                                }
                            }
                        }
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(
                            TAG,
                            "[WebView Console] ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()})"
                        )
                        return true
                    }
                }

                // Explicit diagnostic log confirming loadDataWithBaseURL call for this slot
                Log.d(
                    TAG,
                    "[AdWebView] Calling loadDataWithBaseURL for unit='${adUnit.id}' (w=${adUnit.widthDp}dp, h=${adUnit.heightDp}dp) base='$AD_BASE_URL'"
                )
                loadDataWithBaseURL(
                    AD_BASE_URL,
                    adUnit.getHtmlContent(),
                    "text/html",
                    "UTF-8",
                    null
                )

                webViewRef = this
            }
        },
        update = { wv ->
            webViewRef = wv
        },
        modifier = Modifier.fillMaxSize()
    )
}
