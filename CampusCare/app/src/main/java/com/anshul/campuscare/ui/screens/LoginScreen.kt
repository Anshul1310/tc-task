package com.anshul.campuscare.ui.screens

// ──────────────────────────────────────────────
// Login Screen
//
// Shows the app logo and a "Login with DAuth" button.
// When the user taps login, a WebView opens that:
//   1. Goes to {BASE_URL}/auth/login
//   2. Gets redirected to DAuth login page
//   3. User enters credentials on DAuth
//   4. DAuth redirects back to {BASE_URL}/auth/callback
//   5. The server creates a session and returns a cookie
//   6. We capture that cookie and save it to SharedPreferences
// ──────────────────────────────────────────────

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    // Track whether the WebView is shown or not
    var showWebView: Boolean by remember { mutableStateOf(false) }
    var isLoading: Boolean by remember { mutableStateOf(false) }

    if (showWebView) {
        // ── WebView for DAuth Login ───────────
        LoginWebView(
            loginUrl = ApiClient.BASE_URL + "auth/login",
            callbackUrlPrefix = ApiClient.BASE_URL + "auth/callback",
            onLoginComplete = {
                showWebView = false
                onLoginSuccess()
            },
            onLoadingChanged = { loadingState: Boolean ->
                isLoading = loadingState
            }
        )
    } else {
        // ── Login Landing Page ────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Icon
            Text(
                text = "💬",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(height = 16.dp))

            // App Name
            Text(
                text = "CampusCare",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            // Subtitle
            Text(
                text = "Campus Community Discussion Platform",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(height = 48.dp))

            // Login Button
            Button(
                onClick = {
                    showWebView = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 52.dp),
                shape = RoundedCornerShape(size = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Login with DAuth",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(height = 16.dp))

            // Info text
            Text(
                text = "Use your NIT Trichy credentials\nto sign in securely via DAuth",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────
// Login WebView
// ──────────────────────────────────────────────

@Composable
fun LoginWebView(
    loginUrl: String,
    callbackUrlPrefix: String,
    onLoginComplete: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    var isPageLoading: Boolean by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    // Enable cookies in the WebView
                    val cookieManager: CookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)

                    webViewClient = object : WebViewClient() {

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            isPageLoading = true
                            onLoadingChanged(true)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                            onLoadingChanged(false)

                            // Check if we've reached the callback URL
                            if (url != null && url.contains("/auth/callback") && url.contains("code=")) {
                                transferCookiesToSession(url = url)
                                onLoginComplete()
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val urlString = request?.url?.toString() ?: ""

                            // Check if DAuth is redirecting us back to the callback URL
                            if (urlString.contains("/auth/callback") && urlString.contains("code=")) {
                                if (urlString.startsWith(ApiClient.BASE_URL)) {
                                    return false
                                }

                                val uri = request?.url
                                val query = uri?.query
                                val newUrl = ApiClient.BASE_URL + "auth/callback" + if (query != null) "?$query" else ""

                                view?.loadUrl(newUrl)
                                return true
                            }

                            return false
                        }
                    }

                    // Start the login flow
                    loadUrl(loginUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPageLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = 40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Saves the session cookie from WebView's CookieManager to ApiClient.
 */
private fun transferCookiesToSession(url: String) {
    val cookieManager: CookieManager = CookieManager.getInstance()
    val cookieString: String? = cookieManager.getCookie(url)

    if (!cookieString.isNullOrEmpty()) {
        val parts = cookieString.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("connect.sid=")) {
                ApiClient.saveSessionCookie(trimmed)
                return
            }
        }
        // Fallback: save first cookie part
        ApiClient.saveSessionCookie(cookieString.split(";")[0].trim())
    }
}
