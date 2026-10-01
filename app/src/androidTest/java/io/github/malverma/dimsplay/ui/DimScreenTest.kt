package io.github.malverma.dimsplay.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DimScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var level by mutableIntStateOf(40)
    private var dimming by mutableStateOf(false)

    private fun setScreen(showPermissionBanner: Boolean = false) {
        compose.setContent {
            DimsplayTheme {
                DimScreen(
                    level = level,
                    dimming = dimming,
                    extraDim = false,
                    showPermissionBanner = showPermissionBanner,
                    onLevelChange = { level = it },
                    onLevelChangeFinished = {},
                    onDimmingChange = { dimming = it },
                    onExtraDimChange = {},
                    onGrantPermission = {},
                )
            }
        }
    }

    @Test
    fun sliderUpdatesValueLabel() {
        setScreen()

        compose.onNodeWithTag(TAG_SLIDER)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(75f) }

        compose.onNodeWithTag(TAG_LEVEL_VALUE).assertTextEquals("75%")
        compose.onNodeWithTag(TAG_SLIDER).assertRangeInfoEquals(ProgressBarRangeInfo(75f, 0f..100f, steps = 99))
    }

    @Test
    fun dimmingSwitchReflectsState() {
        setScreen()

        compose.onNodeWithTag(TAG_DIMMING).performScrollTo().assertIsOff()
        compose.onNodeWithText("Overlay is off").assertExists()

        compose.onNodeWithTag(TAG_DIMMING).performClick().assertIsOn()
        compose.onNodeWithText("Overlay is running").assertExists()
    }

    @Test
    fun permissionBannerShownWhenPermissionMissing() {
        setScreen(showPermissionBanner = true)

        compose.onNodeWithText("Grant permission").assertExists()
    }
}
