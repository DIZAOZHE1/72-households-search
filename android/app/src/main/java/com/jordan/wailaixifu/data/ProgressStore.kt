package com.jordan.wailaixifu.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Which stories the reader has finished or wants to watch, plus today's recommendation. */
data class Progress(
    val seen: Set<String> = emptySet(),
    val want: Set<String> = emptySet(),
    /** Cached 今日推荐 so the same day always suggests the same story. */
    val todayDate: String? = null,
    val todayKey: String? = null,
) {
    fun isSeen(key: String) = key in seen
    fun isWant(key: String) = key in want
}

/**
 * Persists 看过 / 想看 state. The source pages used `localStorage.ssjk_progress_v1` with
 * `{seen:{key:1}, want:{key:1}, today:{date,i}}`; this keeps the same shape in
 * SharedPreferences so the semantics (including 看过 cancelling 想看) match.
 */
class ProgressStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<Progress> = _state.asStateFlow()

    private fun restore(): Progress {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return Progress()
        return runCatching {
            val json = JSONObject(raw)
            Progress(
                seen = keySetOf(json.optJSONObject("seen")),
                want = keySetOf(json.optJSONObject("want")),
                todayDate = json.optJSONObject("today")?.optString("date")?.takeIf { it.isNotEmpty() },
                todayKey = json.optJSONObject("today")?.optString("key")?.takeIf { it.isNotEmpty() },
            )
        }.getOrDefault(Progress())
    }

    /**
     * Collects an object's keys into a Set. org.json exposes an Iterator here rather than a Set, so
     * this avoids relying on either the stdlib's Map extensions or SDK-version-specific helpers.
     */
    private fun keySetOf(json: JSONObject?): Set<String> {
        if (json == null) return emptySet()
        val keys = HashSet<String>(json.length())
        val iterator = json.keys()
        while (iterator.hasNext()) keys += iterator.next()
        return keys
    }

    private fun persist(progress: Progress) {
        scope.launch {
            val json = JSONObject()
            json.put("seen", keysToObject(progress.seen))
            json.put("want", keysToObject(progress.want))
            if (progress.todayDate != null && progress.todayKey != null) {
                json.put("today", JSONObject().put("date", progress.todayDate).put("key", progress.todayKey))
            }
            prefs.edit { putString(KEY_PROGRESS, json.toString()) }
        }
    }

    private fun keysToObject(keys: Set<String>): JSONObject {
        val obj = JSONObject()
        keys.forEach { obj.put(it, 1) }
        return obj
    }

    /** Toggles 看过 / 想看, mirroring `toggle(i, kind)` on the source pages. */
    fun toggle(key: String, kind: Kind) {
        _state.update { current ->
            val next = when (kind) {
                Kind.Seen -> {
                    val seen = current.seen.toggle(key)
                    // Marking a story as seen also clears its 想看 flag.
                    val want = if (key in seen) current.want - key else current.want
                    current.copy(seen = seen, want = want)
                }

                Kind.Want -> current.copy(want = current.want.toggle(key))
            }
            persist(next)
            next
        }
    }

    fun rememberToday(date: String, key: String) {
        _state.update { current ->
            val next = current.copy(todayDate = date, todayKey = key)
            persist(next)
            next
        }
    }

    fun clear() {
        _state.update { current ->
            val next = current.copy(seen = emptySet(), want = emptySet())
            persist(next)
            next
        }
    }

    enum class Kind { Seen, Want }

    private companion object {
        const val PREFS_NAME = "ssjk_progress"
        const val KEY_PROGRESS = "ssjk_progress_v1"
    }
}

private fun Set<String>.toggle(key: String): Set<String> =
    if (key in this) this - key else this + key
