package app.rasoi.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** iOS entry point; the PascalCase name is what Swift calls. */
@Suppress("FunctionNaming")
@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController { RasoiApp(showCatalog = Platform.isDebugBinary) }
