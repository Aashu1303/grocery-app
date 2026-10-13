package app.rasoi.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** iOS entry point; the PascalCase name is what Swift calls. */
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController {
    RasoiApp(showCatalog = Platform.isDebugBinary, introStartDelayMillis = LAUNCH_SCREEN_FADE_MILLIS)
}

/** iOS cross-fades the launch screen into the first app frame; the intro waits it out on its matching frame 0. */
private const val LAUNCH_SCREEN_FADE_MILLIS = 200L
