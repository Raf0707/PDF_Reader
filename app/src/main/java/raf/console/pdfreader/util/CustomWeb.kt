package raf.console.zickreee.util

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CustomWebView(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // Отключаем перевод
                settings.setSupportMultipleWindows(false)
                settings.setSupportZoom(false)

                // Устанавливаем WebViewClient для обработки загрузки страницы
                webViewClient = WebViewClient()

                // Загружаем URL
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url) // Обновляем URL, если он изменился
        }
    )
}