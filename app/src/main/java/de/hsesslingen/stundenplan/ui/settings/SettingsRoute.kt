package de.hsesslingen.stundenplan.ui.settings

/** The settings section is its own small navigation stack (hub + sub-pages), separate from the
 *  Plan/Settings top-level switch in MainActivity. */
sealed interface SettingsRoute {
    data object Hub : SettingsRoute
    data object Studiengaenge : SettingsRoute
    data object Notifications : SettingsRoute
    data object Appearance : SettingsRoute
    data object About : SettingsRoute
}
