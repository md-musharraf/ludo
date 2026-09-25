package com.example.ludo.core.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.ludo.audio.SoundEffectManager

/** Small persisted user preferences (private app storage; excluded from backups). */
object AppSettings {
    private const val PREFS = "ludo_settings"
    private const val KEY_SOUND = "sound_enabled"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        SoundEffectManager.isSoundEnabled = p.getBoolean(KEY_SOUND, true)
    }

    var soundEnabled: Boolean
        get() = SoundEffectManager.isSoundEnabled
        set(value) {
            SoundEffectManager.isSoundEnabled = value
            prefs?.edit { putBoolean(KEY_SOUND, value) }
        }
}
