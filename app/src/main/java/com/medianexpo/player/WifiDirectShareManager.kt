package com.medianexpo.player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Wi‑Fi Direct (P2P) send / receive for MediaNexpo.
 */
class WifiDirectShareManager(private val appContext: Context) {

    companion object {
        const val PORT = 8988
        private const val TAG = "WifiDirectShare"
    }

    private val manager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    var isActive by mutableStateOf(false)
        private set
    var status by mutableStateOf("Idle")
        private set
    var isGroupOwner by mutableStateOf(false)
        private set
    var groupOwnerAddress by mutableStateOf<String?>(null)
        private set

    val peers = mutableStateListOf<WifiP2pDevice>()

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var pendingSendFile: File? = null
    private var pendingSendName: String? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        status = "Wi‑Fi Direct is off"
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    requestConnectionInfo()
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> { }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (manager == null) {
            status = "Wi‑Fi Direct not supported"
            return
        }
        if (isActive) return
        channel = manager.initialize(appContext, Looper.getMainLooper(), null)
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiverLegacy(filter)
        }
        isActive = true
        status = "Discovering peers…"
        discover()
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiverLegacy(filter: IntentFilter) {
        appContext.registerReceiver(receiver, filter)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: Exception) { }
        try {
            serverSocket?.close()
        } catch (_: Exception) { }
        serverSocket = null
        serverThread = null
        try {
            manager?.removeGroup(channel, null)
            manager?.cancelConnect(channel, null)
        } catch (_: Exception) { }
        peers.clear()
        isActive = false
        isGroupOwner = false
        groupOwnerAddress = null
        pendingSendFile = null
        pendingSendName = null
        status = "Idle"
    }

    @SuppressLint("MissingPermission")
    fun discover() {
        status = "Discovering peers…"
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                status = "Scanning for devices…"
            }
            override fun onFailure(reason: Int) {
                status = "Discover failed ($reason)"
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        manager?.requestPeers(channel) { list: WifiP2pDeviceList ->
            peers.clear()
            peers.addAll(list.deviceList)
            status = if (peers.isEmpty()) "No peers found — try again" else "Found ${peers.size} device(s)"
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        status = "Connecting to ${device.deviceName}…"
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                status = "Connecting…"
            }
            override fun onFailure(reason: Int) {
                status = "Connect failed ($reason)"
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestConnectionInfo() {
        manager?.requestConnectionInfo(channel) { info: WifiP2pInfo ->
            if (!info.groupFormed) {
                status = "Disconnected"
                isGroupOwner = false
                groupOwnerAddress = null
                return@requestConnectionInfo
            }
            isGroupOwner = info.isGroupOwner
            groupOwnerAddress = info.groupOwnerAddress?.hostAddress
            if (info.isGroupOwner) {
                status = "Connected (host) — ready to receive / send"
                startServerSocket()
            } else {
                status = "Connected (client) to $groupOwnerAddress"
                val file = pendingSendFile
                val name = pendingSendName
                if (file != null && name != null && groupOwnerAddress != null) {
                    sendFileTo(groupOwnerAddress!!, file, name)
                }
            }
        }
    }

    private fun startServerSocket() {
        if (serverThread?.isAlive == true) return
        serverThread = thread(name = "WifiDirectServer") {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(PORT)
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    thread {
                        try {
                            handleSocket(client)
                        } catch (e: Exception) {
                            Log.e(TAG, "client handle failed", e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "server socket failed", e)
                    status = "Server error: ${e.message}"
                }
            }
        }
    }

    fun queueSend(file: File, displayName: String) {
        pendingSendFile = file
        pendingSendName = displayName
        when {
            !isActive -> status = "Start Wi‑Fi Direct first"
            groupOwnerAddress == null && !isGroupOwner -> status = "Connect to a peer, then send"
            isGroupOwner -> status = "Host ready — other phone should join; file will send on connect"
            else -> groupOwnerAddress?.let { sendFileTo(it, file, displayName) }
        }
    }

    private fun sendFileTo(host: String, file: File, displayName: String) {
        status = "Sending $displayName…"
        thread {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, PORT), 15_000)
                    DataOutputStream(socket.getOutputStream()).use { dos ->
                        val nameBytes = displayName.toByteArray(Charsets.UTF_8)
                        dos.writeInt(nameBytes.size)
                        dos.write(nameBytes)
                        dos.writeLong(file.length())
                        FileInputStream(file).use { fis ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = fis.read(buf)
                                if (n <= 0) break
                                dos.write(buf, 0, n)
                            }
                        }
                        dos.flush()
                    }
                }
                status = "Sent $displayName"
                pendingSendFile = null
                pendingSendName = null
            } catch (e: Exception) {
                Log.e(TAG, "send failed", e)
                status = "Send failed: ${e.message}"
            }
        }
    }

    private fun handleSocket(socket: Socket) {
        socket.use { sock ->
            val pending = pendingSendFile
            val pendingName = pendingSendName
            if (pending != null && pendingName != null && isGroupOwner) {
                DataOutputStream(sock.getOutputStream()).use { dos ->
                    val nameBytes = pendingName.toByteArray(Charsets.UTF_8)
                    dos.writeInt(nameBytes.size)
                    dos.write(nameBytes)
                    dos.writeLong(pending.length())
                    FileInputStream(pending).use { fis ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = fis.read(buf)
                            if (n <= 0) break
                            dos.write(buf, 0, n)
                        }
                    }
                    dos.flush()
                }
                status = "Sent $pendingName"
                pendingSendFile = null
                pendingSendName = null
                return
            }

            DataInputStream(sock.getInputStream()).use { dis ->
                val nameLen = dis.readInt()
                if (nameLen <= 0 || nameLen > 1024) return
                val nameBytes = ByteArray(nameLen)
                dis.readFully(nameBytes)
                val name = String(nameBytes, Charsets.UTF_8).replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val fileLen = dis.readLong()
                if (fileLen <= 0 || fileLen > 2L * 1024 * 1024 * 1024) return

                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MediaNexpoShared"
                ).apply { mkdirs() }
                val outFile = File(dir, name)
                status = "Receiving $name…"
                FileOutputStream(outFile).use { fos ->
                    val buf = ByteArray(64 * 1024)
                    var remaining = fileLen
                    while (remaining > 0) {
                        val n = dis.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                        if (n <= 0) break
                        fos.write(buf, 0, n)
                        remaining -= n
                    }
                }
                status = "Received $name"
                Log.d(TAG, "Saved ${outFile.absolutePath}")
            }
        }
    }
}
