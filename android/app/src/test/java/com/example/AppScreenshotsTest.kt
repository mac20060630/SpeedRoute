package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import com.example.ui.WelcomeScreen
import com.example.ui.SpeedCameraScreen
import com.example.ui.TrustScreen
import com.example.ui.OnboardingViewModel
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class AppScreenshotsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun capture_welcome_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        WelcomeScreen(navController = rememberNavController())
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/welcome_screen.png")
  }

  @Test
  fun capture_speed_camera_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SpeedCameraScreen(navController = rememberNavController(), viewModel = OnboardingViewModel())
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/speed_camera_screen.png")
  }

  @Test
  fun capture_trust_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        TrustScreen(navController = rememberNavController(), viewModel = OnboardingViewModel())
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/trust_screen.png")
  }
}
