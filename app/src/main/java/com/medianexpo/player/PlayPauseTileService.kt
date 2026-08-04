package com.medianexpo.player

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Quick Settings play/pause tile.
 * Android 13+: use Settings → "Add Quick Settings tile" or the in-app prompt
 * (StatusBarManager.requestAddTileService). Then edit the shade and keep it active.
 */
@RequiresApi(Build.VERSION_CODES.N)
class PlayPauseTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        refresh()
    }

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val broadcast = Intent(MediaControlReceiver.ACTION_TOGGLE).apply {
            setPackage(packageName)
        }
        try {
            sendBroadcast(broadcast)
        } catch (_: Exception) {
            try {
                val intent = Intent(this, PlaybackService::class.java).apply {
                    action = PlaybackService.CMD_TOGGLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (_: Exception) {
                try {
                    startService(
                        Intent(this, PlaybackService::class.java).apply {
                            action = PlaybackService.CMD_TOGGLE
                        }
                    )
                } catch (_: Exception) {}
            }
        }
        // Optimistic UI flip; next onStartListening will correct from service
        val tile = qsTile ?: return
        val wasActive = tile.state == Tile.STATE_ACTIVE
        applyState(tile, !wasActive)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val playing = try {
            PlaybackService.isPlayingNow
        } catch (_: Exception) {
            false
        }
        applyState(tile, playing)
    }

    private fun applyState(tile: Tile, playing: Boolean) {
        tile.label = "MediaNexpo"
        tile.contentDescription = "Play or pause MediaNexpo"
        tile.state = if (playing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (playing) "Playing — tap pause" else "Tap to play"
        }
        try {
            tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher)
        } catch (_: Exception) {
            try {
                tile.icon = Icon.createWithResource(this, android.R.drawable.ic_media_play)
            } catch (_: Exception) {}
        }
        tile.updateTile()
    }
}
