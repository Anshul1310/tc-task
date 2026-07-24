package com.anshul.campuscare.ui.screens

import android.graphics.Bitmap
import android.net.Uri
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
    var showWebView by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    if (showWebView) {
        LoginWebView(
            loginUrl = ApiClient.BASE_URL + "auth/login",
            onLoginComplete = {
                showWebView = false
                onLoginSuccess()
            },
            onLoadingChanged = { loadingState ->
                isLoading = loadingState
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💬",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(height = 16.dp))

            Text(
                text = "CampusCare",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            Text(
                text = "Campus Community Discussion Platform",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(height = 48.dp))

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

            Text(
                text = "Use your NIT Trichy credentials\nto sign in securely via DAuth",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoginWebView(
    loginUrl: String,
    onLoginComplete: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    var isPageLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

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

                            if (url != null && url.contains("/auth/callback")) {
                                // Extract token via JavaScript from rendered HTML page
                                view?.evaluateJavascript(
                                    "(function() { var el = document.getElementById('token'); return el ? el.innerText : ''; })();"
                                ) { tokenResult ->
                                    val cleanToken = tokenResult?.replace("\"", "")?.trim()
                                    if (!cleanToken.isNullOrEmpty() && cleanToken != "null") {
                                        ApiClient.saveAuthToken(cleanToken)
                                        onLoginComplete()
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val urlString = request?.url?.toString() ?: ""

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
