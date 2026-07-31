package com.medianexpo.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Lightweight LAN HTTP share server on port 8765.
 *
 * GET  /           HTML status + upload form
 * GET  /download   Serve currently shared file
 * POST /upload     multipart file → Downloads/MediaNexpoShared
 */
class LocalShareService : Service() {

    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)

    companion object {
        const val PORT = 8765
        const val ACTION_START = "com.medianexpo.player.SHARE_START"
        const val ACTION_STOP = "com.medianexpo.player.SHARE_STOP"
        const val EXTRA_SHARE_URI = "share_uri"
        const val EXTRA_SHARE_NAME = "share_name"
        const val CHANNEL_ID = "local_share"
        const val NOTIF_ID = 4201

        @Volatile var isRunning: Boolean = false
            private set
        @Volatile var localUrl: String = ""
            private set
        @Volatile var sharedFileName: String? = null
            private set
        @Volatile var lastReceivedName: String? = null
            private set
        @Volatile var receiveCount: Int = 0
            private set

        @Volatile private var sharedLocalPath: String? = null

        fun start(context: Context, shareUri: Uri? = null, shareName: String? = null) {
            val i = Intent(context, LocalShareService::class.java).apply {
                action = ACTION_START
                if (shareUri != null) {
                    putExtra(EXTRA_SHARE_URI, shareUri.toString())
                    putExtra(EXTRA_SHARE_NAME, shareName ?: "shared_file")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LocalShareService::class.java).apply { action = ACTION_STOP }
            )
        }

        fun getWifiIp(context: Context): String {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val ip = wm.connectionInfo.ipAddress
                if (ip != 0) {
                    return String.format(
                        "%d.%d.%d.%d",
                        ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff
                    )
                }
            } catch (_: Exception) { }
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    if (!intf.isUp || intf.isLoopback) continue
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress ?: continue
                        }
                    }
                }
            } catch (_: Exception) { }
            return "0.0.0.0"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val uriStr = intent?.getStringExtra(EXTRA_SHARE_URI)
                val name = intent?.getStringExtra(EXTRA_SHARE_NAME)
                if (uriStr != null) {
                    prepareSharedFile(Uri.parse(uriStr), name ?: "shared_file")
                }
                startAsForeground()
                startServer()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Local Share", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MediaNexpo sharing")
            .setContentText("Active on port $PORT — ${getWifiIp(this)}")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun prepareSharedFile(uri: Uri, name: String) {
        try {
            val dir = File(cacheDir, "share_out").apply { mkdirs() }
            val safe = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val out = File(dir, safe)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            sharedLocalPath = out.absolutePath
            sharedFileName = safe
        } catch (e: Exception) {
            Log.e("LocalShare", "prepareSharedFile failed", e)
            sharedLocalPath = null
            sharedFileName = null
        }
    }

    private fun startServer() {
        if (running.get()) {
            val ip = getWifiIp(this)
            localUrl = "http://$ip:$PORT/"
            return
        }
        running.set(true)
        val ip = getWifiIp(this)
        localUrl = "http://$ip:$PORT/"
        isRunning = true

        serverThread = thread(name = "LocalShareHttp") {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d("LocalShare", "Listening $localUrl")
                while (running.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        thread { handleClient(client) }
                    } catch (e: Exception) {
                        if (running.get()) Log.e("LocalShare", "accept", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalShare", "server", e)
            } finally {
                isRunning = false
                running.set(false)
            }
        }
    }

    private fun stopServer() {
        running.set(false)
        isRunning = false
        localUrl = ""
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        serverThread = null
    }

    private fun handleClient(socket: Socket) {
        socket.use { sock ->
            val input = sock.getInputStream()
            val out = sock.getOutputStream()
            val headerBytes = readHttpHeaders(input) ?: return
            val headerText = String(headerBytes, Charsets.ISO_8859_1)
            val lines = headerText.split("\r\n")
            if (lines.isEmpty()) return
            val req = lines[0].split(" ")
            if (req.size < 2) return
            val method = req[0]
            val path = req[1]
            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) break
                val c = line.indexOf(':')
                if (c > 0) headers[line.substring(0, c).trim().lowercase()] = line.substring(c + 1).trim()
            }
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0

            when {
                method == "GET" && (path == "/" || path.startsWith("/?")) ->
                    writeResponse(out, 200, "text/html; charset=utf-8", indexHtml())
                method == "GET" && path.startsWith("/download") ->
                    serveSharedFile(out)
                method == "POST" && path.startsWith("/upload") ->
                    handleUpload(input, headers["content-type"] ?: "", contentLength, out)
                else ->
                    writeResponse(out, 404, "text/plain", "Not found")
            }
        }
    }

    /** Read until \r\n\r\n */
    private fun readHttpHeaders(input: InputStream): ByteArray? {
        val buf = ByteArrayOutputStream()
        var prev = 0
        var state = 0 // count trailing \r\n\r\n
        while (true) {
            val b = input.read()
            if (b == -1) return if (buf.size() > 0) buf.toByteArray() else null
            buf.write(b)
            when (state) {
                0 -> state = if (b == '\r'.code) 1 else 0
                1 -> state = if (b == '\n'.code) 2 else 0
                2 -> state = if (b == '\r'.code) 3 else 0
                3 -> if (b == '\n'.code) return buf.toByteArray() else state = 0
            }
            if (buf.size() > 64 * 1024) return buf.toByteArray()
            prev = b
        }
    }

    private fun indexHtml(): String {
        val share = if (sharedFileName != null) {
            "<h2>Download</h2><p><a href=\"/download\">${sharedFileName}</a></p>"
        } else ""
        return """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>MediaNexpo Share</title>
            <style>
              body{font-family:sans-serif;background:#121212;color:#eee;padding:24px;max-width:480px;margin:auto}
              h1,h2{color:#BB86FC} a{color:#BB86FC}
              input,button{font-size:16px;margin:8px 0;padding:12px;width:100%;box-sizing:border-box;border-radius:8px;border:none}
              button{background:#BB86FC;color:#000;font-weight:bold}
            </style></head><body>
            <h1>MediaNexpo LAN</h1>
            <p>Received this session: $receiveCount</p>
            $share
            <h2>Upload to this phone</h2>
            <form action="/upload" method="POST" enctype="multipart/form-data">
              <input type="file" name="file" required>
              <button type="submit">Upload</button>
            </form>
            </body></html>
        """.trimIndent()
    }

    private fun serveSharedFile(out: OutputStream) {
        val path = sharedLocalPath
        val name = sharedFileName
        if (path == null || name == null || !File(path).exists()) {
            writeResponse(out, 404, "text/plain", "No file shared")
            return
        }
        val file = File(path)
        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n" +
            "Content-Length: ${file.length()}\r\n" +
            "Content-Disposition: attachment; filename=\"$name\"\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray())
        FileInputStream(file).use { it.copyTo(out) }
        out.flush()
    }

    private fun handleUpload(input: InputStream, contentType: String, contentLength: Int, out: OutputStream) {
        try {
            if (!contentType.contains("multipart/form-data") || contentLength <= 0) {
                writeResponse(out, 400, "text/plain", "Expected multipart upload")
                return
            }
            val boundary = contentType.substringAfter("boundary=", "").trim()
            if (boundary.isEmpty()) {
                writeResponse(out, 400, "text/plain", "Missing boundary")
                return
            }
            val body = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = input.read(body, read, contentLength - read)
                if (n <= 0) break
                read += n
            }

            // Find filename
            val asText = String(body, Charsets.ISO_8859_1)
            val fileNameRegex = Regex("filename=\"([^\"]+)\"")
            val match = fileNameRegex.find(asText)
            val fileName = match?.groupValues?.get(1)?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                ?: "upload_${System.currentTimeMillis()}"

            // Find start of file data after headers of the part
            val sep = "\r\n\r\n"
            val headerEnd = asText.indexOf(sep)
            if (headerEnd < 0) {
                writeResponse(out, 400, "text/plain", "Bad multipart")
                return
            }
            val dataStart = headerEnd + 4
            val endBoundary = asText.lastIndexOf("--$boundary")
            val dataEnd = if (endBoundary > dataStart) {
                // strip trailing \r\n before boundary
                var end = endBoundary
                if (end >= 2 && body[end - 2] == '\r'.code.toByte() && body[end - 1] == '\n'.code.toByte()) {
                    end -= 2
                }
                end
            } else read

            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MediaNexpoShared"
            ).apply { mkdirs() }
            val dest = File(dir, fileName)
            FileOutputStream(dest).use { fos ->
                fos.write(body, dataStart, (dataEnd - dataStart).coerceAtLeast(0))
            }
            lastReceivedName = fileName
            receiveCount++
            Log.d("LocalShare", "Saved upload: ${dest.absolutePath}")
            writeResponse(out, 200, "text/html; charset=utf-8",
                "<html><body style='background:#121212;color:#eee;font-family:sans-serif;padding:24px'>" +
                    "<h2 style='color:#BB86FC'>Uploaded</h2><p>$fileName</p>" +
                    "<p><a style='color:#BB86FC' href='/'>Back</a></p></body></html>")
        } catch (e: Exception) {
            Log.e("LocalShare", "upload failed", e)
            writeResponse(out, 500, "text/plain", "Upload failed: ${e.message}")
        }
    }

    private fun writeResponse(out: OutputStream, code: Int, type: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val status = when (code) {
            200 -> "OK"; 400 -> "Bad Request"; 404 -> "Not Found"; else -> "Error"
        }
        out.write("HTTP/1.1 $code $status\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}
