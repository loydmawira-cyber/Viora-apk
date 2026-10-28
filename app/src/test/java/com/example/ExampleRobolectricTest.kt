package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VioraRepository
import com.example.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Viora", appName)
  }

  @Test
  fun `verify profile media unlock with coins`() {
    val media = VioraRepository.profileMedia.value.firstOrNull { it.id == "media_05" }
    assertTrue(media != null && media.isLocked)

    val initialCoins = VioraRepository.currentUser.value.coinBalance
    val result = VioraRepository.unlockProfileMedia("media_05")
    assertTrue(result.isSuccess)

    val updatedUser = VioraRepository.currentUser.value
    assertEquals(initialCoins - media!!.coinPrice, updatedUser.coinBalance)
  }

  @Test
  fun `verify coin gated live stream unlock`() {
    val stream = VioraRepository.liveStreams.value.firstOrNull { it.id == "stream_02" }
    assertTrue(stream != null && stream.isCoinGated)

    val initialCoins = VioraRepository.currentUser.value.coinBalance
    val result = VioraRepository.unlockLiveStream("stream_02")
    assertTrue(result.isSuccess)

    val updatedUser = VioraRepository.currentUser.value
    assertEquals(initialCoins - stream!!.entryCoinPrice, updatedUser.coinBalance)
  }

  @Test
  fun `verify live streams provide picture snapshot URLs`() {
    val streams = VioraRepository.liveStreams.value
    assertTrue(streams.isNotEmpty())
    streams.forEach { stream ->
      assertTrue("Stream ${stream.id} must have a valid snapshotUrl", stream.snapshotUrl.isNotBlank())
    }
  }
}
