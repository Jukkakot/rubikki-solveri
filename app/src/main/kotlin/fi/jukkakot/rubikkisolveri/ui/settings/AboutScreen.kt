package fi.jukkakot.rubikkisolveri.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.jukkakot.rubikkisolveri.AppLinks
import fi.jukkakot.rubikkisolveri.R

/**
 * Name, version and what the app is; the open-source parts and their licences behind a button
 * ([licencesOpen] starts with them shown).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(version: String, onBack: () -> Unit, licencesOpen: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(licencesOpen) }
    val resources = LocalResources.current
    val min2phase = remember { resources.openRawResource(R.raw.min2phase_license).bufferedReader().use { it.readText() } }
    val fonts = remember { resources.openRawResource(R.raw.fonts_ofl).bufferedReader().use { it.readText() } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.version_label, version))
            Text(stringResource(R.string.about_text))
            val uriHandler = LocalUriHandler.current
            FilledTonalButton(onClick = { uriHandler.openUri(AppLinks.LATEST_APK) }) {
                Text(stringResource(R.string.about_download_latest))
            }
            // The licences must ship with the app, but they are not what the user came for.
            OutlinedButton(onClick = { open = !open }) {
                Text(stringResource(if (open) R.string.about_licences_hide else R.string.about_licences_show))
            }
            if (open) {
                Text(stringResource(R.string.about_licences), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_androidx))
                Text(stringResource(R.string.about_kotlin))
                Text(stringResource(R.string.about_min2phase))
                Text(min2phase, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                Text(stringResource(R.string.about_fonts))
                Text(fonts, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}
