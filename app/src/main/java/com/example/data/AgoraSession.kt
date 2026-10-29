package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.BuildConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

enum class AgoraState { IDLE, CONNECTING, CONNECTED, FAILED }

/**
 * One live room's Agora connection.
 *
 * Host: publishes camera + mic. Everyone else (viewers, silent moderators): audience only.
 * The token comes from the `agora-token` Edge Function, which also decides who may publish.
 *
 * Lifecycle: create with remember(), call [start], and always call [release] when the room closes.
 */
class AgoraSession(
    context: Context,
    private val roomId: String,
    val isHost: Boolean
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var engine: RtcEngine? = null
    private var channelName: String = ""
    private var released = false

    /** True when the build has a real Agora App ID, Supabase is on, and this is a real (UUID) room. */
    val isAvailable: Boolean = isConfigured() && SupabaseApi.isConfigured && isUuid(roomId)

    var state by mutableStateOf(AgoraState.IDLE)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** True once the engine exists, so video views can attach to it. */
    var engineReady by mutableStateOf(false)
        private set

    /** The host's Agora uid as seen by a viewer (null until the host is in the channel). */
    var remoteUid by mutableStateOf<Int?>(null)
        private set
    var remoteVideoActive by mutableStateOf(false)
        private set

    // ---------------------------------------------------------------- start / stop

    fun start(scope: CoroutineScope) {
        if (!isAvailable || state == AgoraState.CONNECTING || state == AgoraState.CONNECTED) return
        state = AgoraState.CONNECTING
        errorMessage = null
        scope.launch {
            try {
                val creds = fetchCredentials()
                if (released) return@launch
                withContext(Dispatchers.Main) { joinChannel(creds) }
            } catch (e: Exception) {
                Log.e(TAG, "Agora start failed", e)
                fail(e.message ?: "Could not connect to the live video")
            }
        }
    }

    fun release() {
        released = true
        val e = engine ?: return
        engine = null
        engineReady = false
        remoteUid = null
        remoteVideoActive = false
        state = AgoraState.IDLE
        runCatching { if (isHost) e.stopPreview() }
        runCatching { e.leaveChannel() }
        // RtcEngine.destroy() blocks, so keep it off the main thread.
        Thread { runCatching { RtcEngine.destroy() } }.start()
    }

    // ---------------------------------------------------------------- host controls

    fun setMicMuted(muted: Boolean) {
        engine?.muteLocalAudioStream(muted)
    }

    /** Stops sending video (viewers see the "paused" state). The host's own preview keeps running. */
    fun setVideoPaused(paused: Boolean) {
        engine?.muteLocalVideoStream(paused)
    }

    fun switchCamera() {
        engine?.switchCamera()
    }

    // ---------------------------------------------------------------- video surfaces

    fun attachLocal(view: TextureView) {
        engine?.setupLocalVideo(VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, 0))
    }

    fun attachRemote(view: TextureView, uid: Int) {
        engine?.setupRemoteVideo(VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid))
    }

    // ---------------------------------------------------------------- internals

    private class Credentials(val token: String, val channelName: String, val uid: Int)

    private suspend fun fetchCredentials(): Credentials = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("roomId", roomId)
            .put("role", if (isHost) "publisher" else "subscriber")

        // A host enters the room a moment before the server row finishes saving, so retry briefly.
        var lastError: Exception? = null
        repeat(5) { attempt ->
            try {
                val json = JSONObject(SupabaseApi.invokeFunction("agora-token", body))
                return@withContext Credentials(
                    token = json.getString("token"),
                    channelName = json.getString("channelName"),
                    uid = json.getInt("uid")
                )
            } catch (e: Exception) {
                lastError = e
                if (e.message?.contains("Room not found", ignoreCase = true) != true) throw e
                if (attempt < 4) delay(1000)
            }
        }
        throw lastError ?: ApiException("Could not get a live video token")
    }

    private fun joinChannel(creds: Credentials) {
        if (released) return
        channelName = creds.channelName

        val config = RtcEngineConfig().apply {
            mContext = appContext
            mAppId = BuildConfig.AGORA_APP_ID.trim()
            mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            mEventHandler = handler
        }
        val e = try {
            RtcEngine.create(config)
        } catch (ex: Exception) {
            fail("Video engine failed to start: ${ex.message}")
            return
        }
        engine = e

        e.enableVideo()
        e.enableAudio()
        e.setClientRole(if (isHost) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE)

        if (isHost) {
            e.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VideoDimensions(720, 1280),
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
            )
            e.startPreview()
        }
        engineReady = true

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            clientRoleType = if (isHost) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishCameraTrack = isHost
            publishMicrophoneTrack = isHost
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        val result = e.joinChannel(creds.token, creds.channelName, creds.uid, options)
        if (result != 0) fail("Could not join the live video (error $result)")
    }

    private fun fail(message: String) {
        main.post {
            errorMessage = message
            state = AgoraState.FAILED
        }
    }

    private fun renewToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject(
                    SupabaseApi.invokeFunction(
                        "agora-token",
                        JSONObject().put("roomId", roomId).put("role", if (isHost) "publisher" else "subscriber")
                    )
                )
                engine?.renewToken(json.getString("token"))
            } catch (e: Exception) {
                Log.w(TAG, "Token renewal failed", e)
            }
        }
    }

    private val handler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            main.post { state = AgoraState.CONNECTED }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            // Only the host publishes, so any remote broadcaster is the host.
            main.post { remoteUid = uid }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            main.post {
                if (remoteUid == uid) {
                    remoteUid = null
                    remoteVideoActive = false
                }
            }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            main.post {
                if (remoteUid == null || remoteUid == uid) {
                    remoteUid = uid
                    remoteVideoActive = state == Constants.REMOTE_VIDEO_STATE_DECODING
                }
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            if (state == Constants.CONNECTION_STATE_FAILED) fail("Lost connection to the live video")
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            renewToken()
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error $err (channel=$channelName)")
        }
    }

    companion object {
        private const val TAG = "AgoraSession"

        fun isConfigured(): Boolean {
            val id = BuildConfig.AGORA_APP_ID.trim()
            return id.length == 32 && !id.startsWith("your_")
        }

        private fun isUuid(s: String): Boolean = runCatching { UUID.fromString(s) }.isSuccess
    }
}
