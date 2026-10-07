package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconToggle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The row on top of both scans' camera picture: back, [middle] (the video scan's ring, the guided
 * scan's face marks), the torch where the device has one, and a ⋮ menu of [menu] items (none: no
 * menu). [menu] gets a function that closes the menu.
 */
@Composable
fun ScanOverlayBar(
    onBack: () -> Unit,
    torch: Boolean,
    onTorch: (Boolean) -> Unit,
    torchAvailable: Boolean,
    modifier: Modifier = Modifier,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    middle: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackButton(onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { middle() }
        if (torchAvailable) {
            RoundIconToggle(checked = torch, onCheckedChange = onTorch) {
                Icon(painterResource(Res.drawable.ic_torch), contentDescription = stringResource(Res.string.scan_torch))
            }
        }
        if (menu != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                RoundIconButton(onClick = { open = true }) {
                    Icon(painterResource(Res.drawable.ic_more), contentDescription = stringResource(Res.string.solve_menu))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
            }
        }
    }
}
