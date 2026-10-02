package fi.jukkakot.rubikkisolveri.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data object LogRoute

@Serializable
data object ManualInputRoute

/** [cube] is a colour string (see Cube.toColorString), or null for a solved cube. */
@Serializable
data class FreeCubeRoute(val cube: String? = null)
