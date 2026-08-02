package com.medianexpo.player

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

class EpubReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriString = intent.getStringExtra("EPUB_URI")
        val epubUri = uriString?.let { Uri.parse(it) }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    primary = Color(0xFFBB86FC)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    if (epubUri != null) {
                        EpubReaderScreen(epubUri = epubUri, onBack = { finish() })
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No EPUB provided", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpubReaderScreen(epubUri: Uri, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(epubUri) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            try {
                extractEpubHtml(context, epubUri)
            } catch (e: Exception) {
                null to (e.message ?: "Failed to open EPUB")
            }
        }
        htmlContent = result.first
        error = result.second
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFBB86FC)
                )
            }
            Text("EPUB Reader", color = Color(0xFFBB86FC), fontSize = 18.sp)
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $error", color = Color.Red)
                }
            }
            htmlContent != null -> {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.domStorageEnabled = false
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = WebViewClient()
                            setBackgroundColor(0xFF121212.toInt())
                        }
                    },
                    update = { webView ->
                        val body = htmlContent ?: ""
                        webView.loadDataWithBaseURL(
                            null,
                            """
                            <html><head>
                            <meta name="viewport" content="width=device-width, initial-scale=1">
                            <style>
                              body { background:#121212; color:#eee; font-family:serif; padding:16px; line-height:1.6; }
                              img { max-width:100%; height:auto; }
                              a { color:#BB86FC; }
                            </style>
                            </head><body>$body</body></html>
                            """.trimIndent(),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun extractEpubHtml(
    context: android.content.Context,
    uri: Uri
): Pair<String?, String?> {
    val input = context.contentResolver.openInputStream(uri)
        ?: return null to "Cannot open file"
    return try {
        val zip = ZipInputStream(input)
        val entries = mutableMapOf<String, ByteArray>()
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val name = entry.name.replace('\\', '/')
                entries[name] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()

        val container = entries.entries.firstOrNull {
            it.key.endsWith("META-INF/container.xml", ignoreCase = true)
        }?.value?.toString(Charsets.UTF_8)
            ?: return null to "Missing container.xml"

        val opfPath = Regex("""full-path\s*=\s*"([^"]+)"""").find(container)?.groupValues?.get(1)
            ?: return null to "Cannot find OPF path"
        val opfBytes = entries[opfPath] ?: entries.entries.firstOrNull {
            it.key.endsWith(opfPath.substringAfterLast('/'), ignoreCase = true)
        }?.value
            ?: return null to "OPF not found: $opfPath"

        val opfDir = opfPath.substringBeforeLast('/', "")
        val opfText = opfBytes.toString(Charsets.UTF_8)

        val manifest = mutableMapOf<String, String>()
        Regex("""<item\b[^>]*>""").findAll(opfText).forEach { m ->
            val tag = m.value
            val id = Regex("""id\s*=\s*"([^"]+)"""").find(tag)?.groupValues?.get(1)
            val href = Regex("""href\s*=\s*"([^"]+)"""").find(tag)?.groupValues?.get(1)
            if (id != null && href != null) manifest[id] = href
        }

        val spineIds = Regex("""<itemref[^>]+idref\s*=\s*"([^"]+)"""").findAll(opfText)
            .map { it.groupValues[1] }
            .toList()

        val htmlParts = mutableListOf<String>()
        for (id in spineIds) {
            val href = manifest[id] ?: continue
            val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
            val normalized = fullPath.replace("//", "/")
            val bytes = entries[normalized]
                ?: entries.entries.firstOrNull {
                    it.key.endsWith(href.substringAfterLast('/'), ignoreCase = true)
                }?.value
                ?: continue
            var text = bytes.toString(Charsets.UTF_8)
            text = text.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            val bodyMatch = Regex("""<body[^>]*>([\s\S]*?)</body>""", RegexOption.IGNORE_CASE).find(text)
            if (bodyMatch != null) {
                htmlParts.add(bodyMatch.groupValues[1])
            } else {
                htmlParts.add(text)
            }
        }

        if (htmlParts.isEmpty()) {
            entries.filter {
                it.key.endsWith(".xhtml", true) || it.key.endsWith(".html", true)
            }.toSortedMap().forEach { (_, bytes) ->
                var text = bytes.toString(Charsets.UTF_8)
                val bodyMatch = Regex("""<body[^>]*>([\s\S]*?)</body>""", RegexOption.IGNORE_CASE).find(text)
                htmlParts.add(bodyMatch?.groupValues?.get(1) ?: text)
            }
        }

        if (htmlParts.isEmpty()) return null to "No readable content found"
        htmlParts.joinToString("<hr style='border-color:#333;margin:24px 0'/>") to null
    } catch (e: Exception) {
        null to (e.message ?: "EPUB parse error")
    } finally {
        try {
            input.close()
        } catch (_: Exception) {
        }
    }
}
