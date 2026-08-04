package com.medianexpo.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Private Vault — PIN-locked hidden media (songs, videos, photos).
 * URIs are hidden from main library lists while the vault is locked.
 */
object PrivateVaultStore {
    private const val PREFS = "private_vault"
    private const val KEY_PIN = "pin_hash"
    private const val KEY_URIS = "uris"
    private const val KEY_UNLOCKED = "session_unlocked"

    fun hasPin(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PIN, null).isNullOrEmpty()

    fun setPin(context: Context, pin: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN, hash(pin))
            .apply()
    }

    fun checkPin(context: Context, pin: String): Boolean {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PIN, null)
            ?: return false
        return stored == hash(pin)
    }

    fun isUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)

    fun setUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNLOCKED, unlocked)
            .apply()
    }

    fun uris(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URIS, null)
            ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun add(context: Context, uri: Uri) {
        val set = uris(context).toMutableSet()
        set.add(uri.toString())
        saveUris(context, set)
    }

    fun remove(context: Context, uri: Uri) {
        val set = uris(context).toMutableSet()
        set.remove(uri.toString())
        saveUris(context, set)
    }

    fun contains(context: Context, uri: Uri): Boolean =
        uris(context).contains(uri.toString())

    private fun saveUris(context: Context, set: Set<String>) {
        val arr = JSONArray()
        set.forEach { arr.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URIS, arr.toString())
            .apply()
    }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
