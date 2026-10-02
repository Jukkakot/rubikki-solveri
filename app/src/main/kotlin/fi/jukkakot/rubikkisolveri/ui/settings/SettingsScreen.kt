package fi.jukkakot.rubikkisolveri.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: AppLanguage,
    themeMode: ThemeMode,
    version: String,
    onLanguage: (AppLanguage) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onOpenLog: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle(R.string.settings_language)
            Column(Modifier.selectableGroup()) {
                // Language names are written in their own language so they are found in either UI.
                Choice("Suomi", language == AppLanguage.FINNISH) { onLanguage(AppLanguage.FINNISH) }
                Choice("English", language == AppLanguage.ENGLISH) { onLanguage(AppLanguage.ENGLISH) }
            }
            HorizontalDivider()
            SectionTitle(R.string.settings_theme)
            Column(Modifier.selectableGroup()) {
                Choice(stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM) { onThemeMode(ThemeMode.SYSTEM) }
                Choice(stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT) { onThemeMode(ThemeMode.LIGHT) }
                Choice(stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK) { onThemeMode(ThemeMode.DARK) }
            }
            HorizontalDivider()
            SectionTitle(R.string.settings_diagnostics)
            ListItem(
                headlineContent = { Text(stringResource(R.string.log_title)) },
                supportingContent = { Text(stringResource(R.string.log_open_summary)) },
                modifier = Modifier.clickable(onClick = onOpenLog),
            )
            Text(
                stringResource(R.string.version_label, version),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onSelect: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    )
}
