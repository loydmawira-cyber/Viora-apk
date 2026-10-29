package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.BuildConfig
import com.example.model.User
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String) : Exception(message)

/**
 * Minimal Supabase client (Auth + PostgREST + RPC) using only built-in Android classes,
 * so it adds no new libraries to the build.
 *
 * When SUPABASE_URL / SUPABASE_ANON_KEY are not set, [isConfigured] is false and the app
 * keeps running on demo data.
 *
 * Only the anon key lives in the app. It is safe to ship: row-level security protects the data.
 * Never put the service_role key here.
 */
object SupabaseApi {

    private val baseUrl: String get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") &&
            !baseUrl.contains("your-project") &&
            anonKey.length > 20 &&
            !anonKey.startsWith("your_")

    private var prefs: SharedPreferences? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAtMs: Long = 0L
    private val refreshLock = Mutex()

    var userId: String? = null
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences("viora_session", Context.MODE_PRIVATE)
        prefs = p
        accessToken = p.getString("access", null)
        refreshToken = p.getString("refresh", null)
        expiresAtMs = p.getLong("expires", 0L)
        userId = p.getString("uid", null)
    }

    fun hasSession(): Boolean = !refreshToken.isNullOrBlank()

    // ------------------------------------------------------------------ AUTH

    /** Returns true when the user is signed in right away, false when email confirmation is needed. */
    suspend fun signUp(email: String, password: String, username: String, displayName: String): Boolean {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put(
                "data",
                JSONObject()
                    .put("username", username)
                    .put("display_name", displayName)
                    .put("age_confirmed", true)
            )
        val text = safe { call("POST", "/auth/v1/signup", body.toString()) }
        val json = JSONObject(text)
        return if (json.has("access_token")) {
            saveSession(json)
            true
        } else {
            false
        }
    }

    suspend fun signIn(email: String, password: String) {
        val body = JSONObject().put("email", email).put("password", password)
        val text = safe { call("POST", "/auth/v1/token?grant_type=password", body.toString()) }
        saveSession(JSONObject(text))
    }

    /** Sends Supabase's password-recovery email; redirect behavior follows the Supabase project settings. */
    suspend fun requestPasswordReset(email: String) {
        val body = JSONObject().put("email", email)
        safe { call("POST", "/auth/v1/recover", body.toString()) }
    }

    /** True when the session was refreshed. False when the saved session is no longer valid. */
    suspend fun refresh(): Boolean = refreshLock.withLock {
        val rt = refreshToken ?: return false
        try {
            val text = call("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", rt).toString())
            saveSession(JSONObject(text))
            true
        } catch (e: ApiException) {
            clearSession()
            false
        } catch (e: IOException) {
            throw ApiException("No internet connection")
        }
    }

    suspend fun signOut() {
        val token = accessToken
        clearSession()
        if (token != null) {
            try {
                call("POST", "/auth/v1/logout", null, token)
            } catch (_: Exception) {
                // Local session is already cleared; nothing else to do.
            }
        }
    }

    // ------------------------------------------------------------------ DATA

    suspend fun rpc(name: String, args: JSONObject = JSONObject()): String =
        authed("POST", "/rest/v1/rpc/$name", args.toString())

    suspend fun select(pathAndQuery: String): JSONArray =
        JSONArray(authed("GET", "/rest/v1/$pathAndQuery"))

    suspend fun insert(table: String, row: JSONObject) {
        authed("POST", "/rest/v1/$table", row.toString(), mapOf("Prefer" to "return=minimal"))
    }

    /** Loads the signed-in user's profile and wallet into the app's User model. */
    suspend fun fetchMyUser(): User {
        val uid = userId ?: throw ApiException("Please sign in again")
        val profile = select("profiles?id=eq.$uid&select=*").optJSONObject(0)
            ?: throw ApiException("Profile not found. Try signing in again.")
        val wallet = select("coin_wallets?user_id=eq.$uid&select=*").optJSONObject(0)

        val role = UserRole.values().firstOrNull { it.name.equals(profile.str("role"), ignoreCase = true) } ?: UserRole.VIEWER
        return User(
            id = uid,
            username = profile.str("username"),
            displayName = profile.str("display_name").ifBlank { profile.str("username") },
            avatarUrl = profile.str("avatar_url"),
            bio = profile.str("bio"),
            role = role,
            coinBalance = wallet?.optLong("coin_balance", 0L) ?: 0L,
            diamondBalance = wallet?.optLong("diamond_balance", 0L) ?: 0L,
            followersCount = 0,
            followingCount = 0,
            isVerified = profile.optBoolean("is_verified", false),
            level = profile.optInt("level", 1),
            isBanned = profile.str("status").equals("banned", ignoreCase = true),
            createdAtMs = parseIso(profile.str("created_at"))
        )
    }

    // ------------------------------------------------------------------ INTERNALS

    private fun saveSession(json: JSONObject) {
        accessToken = json.getString("access_token")
        refreshToken = json.str("refresh_token").ifBlank { refreshToken }
        expiresAtMs = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
        json.optJSONObject("user")?.let { userId = it.str("id") }
        prefs?.edit()
            ?.putString("access", accessToken)
            ?.putString("refresh", refreshToken)
            ?.putLong("expires", expiresAtMs)
            ?.putString("uid", userId)
            ?.apply()
    }

    private fun clearSession() {
        accessToken = null
        refreshToken = null
        expiresAtMs = 0L
        userId = null
        prefs?.edit()?.clear()?.apply()
    }

    private suspend fun authed(
        method: String,
        path: String,
        body: String? = null,
        extra: Map<String, String> = emptyMap()
    ): String {
        if (System.currentTimeMillis() > expiresAtMs - 60_000L && hasSession()) refresh()
        val token = accessToken ?: throw ApiException("Please sign in again")
        return safe {
            try {
                call(method, path, body, token, extra)
            } catch (e: ApiException) {
                // Token expired between the check and the call: refresh once and retry.
                if (e.message?.contains("JWT", ignoreCase = true) == true && refresh()) {
                    call(method, path, body, accessToken, extra)
                } else {
                    throw e
                }
            }
        }
    }

    private suspend fun <T> safe(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: IOException) {
            throw ApiException("No internet connection. Check your network and try again.")
        }
    }

    private suspend fun call(
        method: String,
        path: String,
        body: String?,
        bearer: String? = null,
        extra: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("apikey", anonKey)
            // Publishable keys (sb_publishable_...) are API keys, not JWTs. Only send
            // Authorization for an authenticated user token, or a legacy anon JWT.
            val authorizationToken = bearer ?: anonKey.takeIf { it.startsWith("eyJ") }
            authorizationToken?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            extra.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw ApiException(friendlyError(text, code))
            text
        } finally {
            conn.disconnect()
        }
    }

    private fun friendlyError(text: String, code: Int): String {
        val raw = try {
            val j = JSONObject(text)
            listOf("msg", "error_description", "message", "hint", "error")
                .firstNotNullOfOrNull { k -> j.str(k).ifBlank { null } }
        } catch (_: Exception) {
            null
        } ?: "Request failed ($code)"

        return when {
            raw.contains("Invalid login credentials", true) -> "Wrong email or password."
            raw.contains("already registered", true) -> "An account with this email already exists. Try signing in."
            raw.contains("Database error saving new user", true) ->
                "That username is already taken or not allowed. Try a different one."
            raw.contains("Email not confirmed", true) -> "Please confirm your email first. Check your inbox."
            raw.contains("Password should be", true) -> "Password is too short. Use at least 8 characters."
            raw.contains("rate limit", true) -> "Too many attempts. Please wait a minute and try again."
            else -> raw
        }
    }

    private fun parseIso(s: String): Long {
        if (s.isBlank()) return System.currentTimeMillis()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

/** JSON null and missing keys both become an empty string (org.json would return the text "null"). */
fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")
