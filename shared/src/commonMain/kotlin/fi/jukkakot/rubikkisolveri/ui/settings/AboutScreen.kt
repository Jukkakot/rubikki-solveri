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
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.jukkakot.rubikkisolveri.AppLinks
import fi.jukkakot.rubikkisolveri.Platform
import fi.jukkakot.rubikkisolveri.res.*

/**
 * Name, version and what the app is; the open-source parts and their licences behind a button
 * ([licencesOpen] starts with them shown).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(version: String, onBack: () -> Unit, licencesOpen: Boolean = false, platform: Platform = Platform.ANDROID) {
    var open by rememberSaveable { mutableStateOf(licencesOpen) }
    val min2phase by produceState("") { value = Res.readBytes("files/min2phase_license.txt").decodeToString() }
    val fonts by produceState("") { value = Res.readBytes("files/fonts_ofl.txt").decodeToString() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.version_label, version))
            Text(stringResource(if (platform == Platform.WEB) Res.string.about_text_web else Res.string.about_text))
            val uriHandler = LocalUriHandler.current
            FilledTonalButton(onClick = { uriHandler.openUri(AppLinks.LATEST_APK) }) {
                Text(stringResource(Res.string.about_download_latest))
            }
            // The licences must ship with the app, but they are not what the user came for.
            OutlinedButton(onClick = { open = !open }) {
                Text(stringResource(if (open) Res.string.about_licences_hide else Res.string.about_licences_show))
            }
            if (open) {
                Text(stringResource(Res.string.about_licences), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.about_androidx))
                Text(stringResource(Res.string.about_kotlin))
                Text(stringResource(Res.string.about_min2phase))
                Text(min2phase, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                Text(stringResource(Res.string.about_fonts))
                Text(fonts, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}
