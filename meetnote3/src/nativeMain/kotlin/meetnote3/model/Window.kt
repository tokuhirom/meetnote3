package meetnote3.model

import platform.CoreGraphics.CGWindowID

data class Window(
    val active: Boolean,
    val onScreen: Boolean,
    val owningApplication: Application?,
    val title: String?,
    val windowID: CGWindowID,
)
