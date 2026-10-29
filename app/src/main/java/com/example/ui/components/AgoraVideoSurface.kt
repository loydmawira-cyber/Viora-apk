package com.example.ui.components

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AgoraSession

/**
 * Renders live video from an [AgoraSession]: the camera preview for the host,
 * the host's stream for everyone else. Draws nothing until video is actually available,
 * so the placeholder underneath stays visible.
 */
@Composable
fun AgoraVideoSurface(session: AgoraSession, modifier: Modifier = Modifier) {
    if (!session.engineReady) return

    if (session.isHost) {
        AndroidView(
            modifier = modifier,
            factory = { ctx -> TextureView(ctx).also { session.attachLocal(it) } }
        )
    } else {
        val uid = session.remoteUid
        if (uid != null && session.remoteVideoActive) {
            // key(uid) rebuilds the view if the host reconnects with a new uid.
            key(uid) {
                AndroidView(
                    modifier = modifier,
                    factory = { ctx -> TextureView(ctx).also { session.attachRemote(it, uid) } }
                )
            }
        }
    }
}
