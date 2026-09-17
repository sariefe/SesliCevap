package com.example

import com.example.ui.viewmodel.VoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun voiceUiState_defaultModeIsVoiceEnabled() {
    val state = VoiceUiState()
    assertTrue("Default interaction mode should be voice-enabled", state.isVoiceEnabled)
    assertTrue("Auto speak should be enabled by default", state.autoSpeak)
  }

  @Test
  fun voiceUiState_canSwitchToTextOnlyMode() {
    val state = VoiceUiState(isVoiceEnabled = false)
    assertFalse("Voice interaction should be disabled in text-only mode", state.isVoiceEnabled)
  }
}
