package ai.rever.boss.plugin.dynamic.arcade

import ai.rever.boss.plugin.dynamic.arcade.battleship.BattleshipService
import ai.rever.boss.plugin.dynamic.arcade.mirrordash.MirrorDashEngine
import ai.rever.boss.plugin.dynamic.arcade.mirrordash.MirrorDashScreen
import ai.rever.boss.plugin.dynamic.arcade.mirrordash.MirrorDashViewModel
import ai.rever.boss.plugin.dynamic.arcade.skystack.SkyStackEngine
import ai.rever.boss.plugin.dynamic.arcade.skystack.SkyStackScreen
import ai.rever.boss.plugin.dynamic.arcade.skystack.SkyStackViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertTrue

/** The surrounding host area must stay untouched, not merely the cropped game screenshot. */
@OptIn(ExperimentalTestApi::class)
class ArcadePaneClippingTest {
    @Test fun mirrorDashOffscreenObjectsCannotPaintOverHostChrome() = checkContainment(true)
    @Test fun skyStackOffscreenSlicesCannotPaintOverAdjacentPanes() = checkContainment(false)

    private fun checkContainment(mirror: Boolean) {
        for (density in listOf(1f, 2f)) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val vmScope = CoroutineScope(Job().apply { cancel() } + Dispatchers.Unconfined)
            try {
                val leaderboard = LeaderboardService(null, null)
                val credits = CreditsService(null, null) { scope }
                val services = ArcadeServices({ scope }, null, null, leaderboard, credits,
                    BattleshipService(null, null), null, null, null)
                val dash = MirrorDashViewModel(vmScope, services)
                val sky = SkyStackViewModel(vmScope, services)
                dash.engine.resize(320f / density, 240f / density)
                // Incoming gates legitimately start above the game viewport.
                dash.engine.obstacles.add(MirrorDashEngine.Obstacle(0.35f, 0.16f, -20f / density, 60f / density))
                // A falling fragment can legitimately leave the visible playfield.
                sky.engine.slices.add(SkyStackEngine.Slice(-1000f, 0f, 200f, 200f, 0f, 0f, 1f, 280f))
                val surrounding = Color(0xFF123456)
                runDesktopComposeUiTest(640, 480) {
                    mainClock.autoAdvance = false
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(density)) {
                            Box(Modifier.fillMaxSize().background(surrounding)) {
                                Box(Modifier.offset((48f / density).dp, (48f / density).dp)
                                    .requiredSize((320f / density).dp, (240f / density).dp)
                                    .testTag("game-pane")) {
                                    if (mirror) MirrorDashScreen(dash, leaderboard) {}
                                    else SkyStackScreen(sky, leaderboard) {}
                                }
                            }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    val bounds = onNodeWithTag("game-pane").fetchSemanticsNode().boundsInRoot
                    val pixels = onRoot().captureToImage().toPixelMap()
                    var outsideChanges = 0
                    var insideChanges = 0
                    for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                        if (pixels[x, y].toArgb() != surrounding.toArgb()) {
                            val inside = x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom
                            if (inside) insideChanges++ else outsideChanges++
                        }
                    }
                    assertTrue(insideChanges > 100, "The actual game must have rendered")
                    assertTrue(outsideChanges == 0,
                        "Game painted $outsideChanges pixels outside its pane (mirror=$mirror, density=$density)")
                }
            } finally { scope.cancel(); vmScope.cancel() }
        }
    }
}
