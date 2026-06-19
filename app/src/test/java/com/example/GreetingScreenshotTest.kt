package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.WordEntity
import com.example.ui.WordItemRow
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockWord = WordEntity(
        id = 1,
        word = "Ephemeral",
        phonetic = "/ɪ/ˈfemərəl/",
        definition = "Lasting for a very short time.",
        example = "The ephemeral thrill of viral fame quickly fades.",
        type = "GRE",
        importance = 3,
        isCustom = false
    )

    composeTestRule.setContent { 
      MyApplicationTheme { 
        WordItemRow(word = mockWord, onDelete = {}) 
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
