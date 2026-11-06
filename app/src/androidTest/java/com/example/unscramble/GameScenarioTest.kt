package com.example.unscramble

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.example.unscramble.data.allWords
import com.example.unscramble.ui.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class GameScenarioTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun errorCorrectAnswerRotationSkipFinishAndRestart() {
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        rule.waitUntil(10000) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        rule.onNodeWithText("Enter your word").performTextInput("wrong-answer")
        rule.onNodeWithText("Submit").performScrollTo().performClick()
        rule.onNodeWithText("Wrong Guess!").assertExists()
        screenshot("unscramble-error")
        // Read the displayed word; solve by letter multiset using the unmodified production dictionary.
        val scrambled = rule.runOnIdle {
            ViewModelProvider(rule.activity)[GameViewModel::class.java].uiState.value.currentScrambledWord
        }
        val answer = allWords.first { it.toList().sorted() == scrambled.toList().sorted() }
        rule.onNodeWithText("Wrong Guess!").performTextInput(answer)
        rule.onNodeWithText("Submit").performScrollTo().performClick()
        rule.onNodeWithText("Score: 20").assertExists()
        rule.onNodeWithText("2/10").assertExists()
        rule.onNodeWithText("Enter your word").performTextInput("partial")
        val beforeRotation = rule.runOnIdle {
            ViewModelProvider(rule.activity)[GameViewModel::class.java].uiState.value
        }
        screenshot("unscramble-portrait")
        val oldActivity = rule.activity
        rule.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(10000) { rule.activity !== oldActivity && rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        rule.onNodeWithText("2/10").assertExists()
        rule.onNodeWithText("Enter your word").assertTextContains("partial")
        rule.runOnIdle {
            assertEquals(beforeRotation, ViewModelProvider(rule.activity)[GameViewModel::class.java].uiState.value)
        }
        screenshot("unscramble-landscape")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Enter your word").assertTextContains("partial")
        repeat(9) { rule.onNodeWithText("Skip").performScrollTo().performClick() }
        rule.onNodeWithText("Congratulations!").assertExists()
        rule.onNodeWithText("You scored: 20").assertExists()
        screenshot("unscramble-finished")
        rule.onNodeWithText("Play Again").performClick()
        rule.onNodeWithText("1/10").assertExists()
        rule.onNodeWithText("Score: 0").assertExists()
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.waitForIdle(1000, 10000)
        val file = File(instrumentation.targetContext.getExternalFilesDir("evidence"), "$name.png")
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        instrumentation.uiAutomation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/lab6-$name.png").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
}
