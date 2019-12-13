package com.iflytek.cyber.iot.show.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Nullable

object ConfigUtils {
    private const val PREF_NAME = "com.iflytek.cyber.iot.show.core.CONFIG"

    const val KEY_RECOGNIZER_PROFILE = "recognizer_profile"
    const val KEY_SCREEN_AUTO_LOCK = "screen_auto_lock"
    const val KEY_CACHE_USER_INFO = "cache_user_info"
    const val KEY_VOICE_WAKEUP_ENABLED = "voice_wakeup_enabled"
    const val KEY_VOICE_BUTTON_ENABLED = "voice_button_enabled"
    const val KEY_SETUP_COMPLETED = "setup_completed"
    const val KEY_VERSION_CODE = "version_code"
    const val KEY_OTA_REQUEST = "ota_request"
    const val KEY_OTA_VERSION_ID = "ota_version_id"
    const val KEY_OTA_VERSION_NAME = "ota_version_name"
    const val KEY_OTA_VERSION_DESCRIPTION = "ota_version_description"
    const val KEY_BANNERS = "banners"
    const val KEY_BACKGROUND_RECOGNIZE = "background_recognize"
    const val KEY_DEVELOPER_OPTIONS = "developer_options"
    const val KEY_CLIENT_ID = "client_id"
    const val KEY_OTA_DISABLED = "ota_enabled"
    const val KEY_CACHE_WAKE_WORD = "cache_wake_word"
    const val KEY_WAKE_WORD_SUCCEED = "wake_word_succeed"
    const val KEY_SLEEP_TIME = "sleep_time"
    const val KEY_RESPONSE_SOUND = "response_sound"

    private var pref: SharedPreferences? = null

    private val listeners = HashSet<OnConfigChangedListener>()

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            listeners.map {
                try {
                    it.onConfigChanged(key, sharedPreferences.all[key])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    fun init(context: Context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        pref?.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    fun registerOnConfigChangedListener(listener: OnConfigChangedListener) {
        listeners.add(listener)
    }

    fun unregisterOnConfigChangedListener(listener: OnConfigChangedListener) {
        listeners.remove(listener)
    }

    fun destroy() {
        pref?.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    /**
     * Set a String value in the preferences editor, to be written back once
     * [.commit] or [.apply] are called.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.  Passing `null`
     * for this argument is equivalent to calling [.remove] with
     * this key.
     *
     */
    fun putString(key: String, value: String?) {
        pref?.edit()?.putString(key, value)?.apply()
    }

    /**
     * Set a set of String values in the preferences editor, to be written
     * back once [.commit] or [.apply] is called.
     *
     * @param key The name of the preference to modify.
     * @param values The set of new values for the preference.  Passing `null`
     * for this argument is equivalent to calling [.remove] with
     * this key.
     */
    fun putStringSet(key: String, values: Set<String>?) {
        pref?.edit()?.putStringSet(key, values)?.apply()
    }

    /**
     * Set an int value in the preferences editor, to be written back once
     * [.commit] or [.apply] are called.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     *
     */
    fun putInt(key: String, value: Int) {
        pref?.edit()?.putInt(key, value)?.apply()
    }

    /**
     * Set a long value in the preferences editor, to be written back once
     * [.commit] or [.apply] are called.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     *
     */
    fun putLong(key: String, value: Long) {
        pref?.edit()?.putLong(key, value)?.apply()
    }

    /**
     * Set a float value in the preferences editor, to be written back once
     * [.commit] or [.apply] are called.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     *
     */
    fun putFloat(key: String, value: Float) {
        pref?.edit()?.putFloat(key, value)?.apply()
    }

    /**
     * Set a boolean value in the preferences editor, to be written back
     * once [.commit] or [.apply] are called.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     *
     */
    fun putBoolean(key: String, value: Boolean) {
        pref?.edit()?.putBoolean(key, value)?.apply()
    }

    /**
     * Mark in the editor that a preference value should be removed, which
     * will be done in the actual preferences once [.commit] is
     * called.
     *
     *
     * Note that when committing back to the preferences, all removals
     * are done first, regardless of whether you called remove before
     * or after put methods on this editor.
     *
     * @param key The name of the preference to remove.
     *
     */
    fun remove(key: String) {
        pref?.edit()?.remove(key)?.apply()
    }

    fun removeAll() {
        pref?.let { pref ->
            val editor = pref.edit()
            pref.all.map {
                editor.remove(it.key)
            }
            editor.apply()
        }
    }

    /**
     * Retrieve all values from the preferences.
     *
     *
     * Note that you *must not* modify the collection returned
     * by this method, or alter any of its contents.  The consistency of your
     * stored data is not guaranteed if you do.
     *
     * @return Returns a map containing a list of pairs key/value representing
     * the preferences.
     *
     * @throws NullPointerException
     */
    fun getAll(): Map<String, *>? = pref?.all

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a String.
     *
     * @throws ClassCastException
     */
    @Nullable
    fun getString(key: String, defValue: String?): String? {
        return pref?.getString(key, defValue) ?: defValue
    }

    /**
     * Retrieve a set of String values from the preferences.
     *
     *
     * Note that you *must not* modify the set instance returned
     * by this call.  The consistency of the stored data is not guaranteed
     * if you do, nor is your ability to modify the instance at all.
     *
     * @param key The name of the preference to retrieve.
     * @param defValues Values to return if this preference does not exist.
     *
     * @return Returns the preference values if they exist, or defValues.
     * Throws ClassCastException if there is a preference with this name
     * that is not a Set.
     *
     * @throws ClassCastException
     */
    @Nullable
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return pref?.getStringSet(key, defValues) ?: defValues
    }

    /**
     * Retrieve an int value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * an int.
     *
     * @throws ClassCastException
     */
    fun getInt(key: String, defValue: Int): Int {
        return pref?.getInt(key, defValue) ?: defValue
    }

    /**
     * Retrieve a long value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a long.
     *
     * @throws ClassCastException
     */
    fun getLong(key: String, defValue: Long): Long {
        return pref?.getLong(key, defValue) ?: defValue
    }

    /**
     * Retrieve a float value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a float.
     *
     * @throws ClassCastException
     */
    fun getFloat(key: String, defValue: Float): Float {
        return pref?.getFloat(key, defValue) ?: defValue
    }

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a boolean.
     *
     * @throws ClassCastException
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return pref?.getBoolean(key, defValue) ?: defValue
    }

    /**
     * Checks whether the preferences contains a preference.
     *
     * @param key The name of the preference to check.
     * @return Returns true if the preference exists in the preferences,
     * otherwise false.
     */
    operator fun contains(key: String): Boolean {
        return pref?.contains(key) == true
    }

    interface OnConfigChangedListener {
        fun onConfigChanged(key: String, value: Any?)
    }
}