package com.medianexpo.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Tasker / automation API — the kind of thing XDA power users expect.
 *
 * Send broadcasts (or startService with action) to control playback:
 *   com.medianexpo.player.action.PLAY
 *   com.medianexpo.player.action.PAUSE
 *   com.medianexpo.player.action.TOGGLE
 *   com.medianexpo.player.action.NEXT
 *   com.medianexpo.player.action.PREV
 *   com.medianexpo.player.action.STOP
 *
 * App also fires:
 *   com.medianexpo.player.action.META_CHANGED
 *   extras: title, artist, album, playing (boolean), position_ms, duration_ms
 */
class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("MediaControl", "received $action")
        val svc = Intent(context, PlaybackService::class.java).apply {
            this.action = when (action) {
                ACTION_PLAY -> PlaybackService.CMD_PLAY
                ACTION_PAUSE -> PlaybackService.CMD_PAUSE
                ACTION_TOGGLE -> PlaybackService.CMD_TOGGLE
                ACTION_NEXT -> PlaybackService.CMD_NEXT
                ACTION_PREV -> PlaybackService.CMD_PREV
                ACTION_STOP -> PlaybackService.CMD_STOP
                else -> return
            }
        }
        try {
            context.startForegroundService(svc)
        } catch (e: Exception) {
            context.startService(svc)
        }
    }

    companion object {
        const val ACTION_PLAY = "com.medianexpo.player.action.PLAY"
        const val ACTION_PAUSE = "com.medianexpo.player.action.PAUSE"
        const val ACTION_TOGGLE = "com.medianexpo.player.action.TOGGLE"
        const val ACTION_NEXT = "com.medianexpo.player.action.NEXT"
        const val ACTION_PREV = "com.medianexpo.player.action.PREV"
        const val ACTION_STOP = "com.medianexpo.player.action.STOP"
        const val ACTION_META_CHANGED = "com.medianexpo.player.action.META_CHANGED"
    }
}
