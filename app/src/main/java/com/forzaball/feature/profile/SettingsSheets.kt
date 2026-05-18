package com.forzaball.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.forzaball.R
import com.forzaball.ui.locale.AppLocale
import com.forzaball.ui.theme.ForzaBallError
import com.forzaball.ui.theme.ForzaBallErrorContainer
import com.forzaball.ui.theme.ForzaBallOnSurface
import com.forzaball.ui.theme.ForzaBallOnSurfaceVariant
import com.forzaball.ui.theme.ForzaBallOutline
import com.forzaball.ui.theme.ForzaBallPrimary
import com.forzaball.ui.theme.ForzaBallSurfaceContainer
import com.forzaball.ui.theme.ForzaBallSurfaceContainerHigh
import com.forzaball.ui.theme.ForzaBallSurfaceContainerHighest
import com.forzaball.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    selected: AppLocale,
    onSelect: (AppLocale) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pending by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) { pending = selected }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForzaBallSurfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(ForzaBallSurfaceContainerHighest),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.select_language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = ForzaBallPrimary,
                    ),
                )
                Text(
                    text = stringResource(R.string.language_sheet_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = ForzaBallOnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppLocale.entries.forEach { locale ->
                    val label = when (locale) {
                        AppLocale.English -> stringResource(R.string.language_english)
                        AppLocale.Arabic -> stringResource(R.string.language_arabic)
                    }
                    LanguageOptionRow(
                        label = label,
                        selected = pending == locale,
                        onClick = { pending = locale },
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ForzaBallSurfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Button(
                    onClick = {
                        onSelect(pending)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.confirm_selection),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) ForzaBallPrimary.copy(alpha = 0.1f) else Color.Transparent,
            )
            .then(
                if (selected) Modifier.border(1.dp, ForzaBallPrimary.copy(alpha = 0.2f), shape)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) ForzaBallPrimary.copy(alpha = 0.2f)
                        else ForzaBallSurfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = null,
                    tint = if (selected) ForzaBallPrimary else ForzaBallOnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = ForzaBallOnSurface,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ForzaBallPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = ForzaBallOnSurface,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, ForzaBallSurfaceContainerHighest, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsSheet(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pending by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) { pending = selected }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForzaBallSurfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(ForzaBallOutline.copy(alpha = 0.5f)),
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.appearance_settings),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                ),
                color = ForzaBallOnSurface,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .background(ForzaBallSurfaceContainerHighest, CircleShape),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = ForzaBallOnSurface)
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, null, tint = ForzaBallPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.theme_mode),
                        style = MaterialTheme.typography.labelMedium,
                        color = ForzaBallOnSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ThemeModeChip(
                        label = stringResource(R.string.theme_dark),
                        icon = Icons.Default.DarkMode,
                        selected = pending == ThemeMode.Dark,
                        onClick = { pending = ThemeMode.Dark },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeModeChip(
                        label = stringResource(R.string.theme_light),
                        icon = Icons.Default.LightMode,
                        selected = pending == ThemeMode.Light,
                        onClick = { pending = ThemeMode.Light },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeModeChip(
                        label = stringResource(R.string.theme_system),
                        icon = Icons.Default.SettingsBrightness,
                        selected = pending == ThemeMode.System,
                        onClick = { pending = ThemeMode.System },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Button(
                onClick = {
                    onSelect(pending)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.save_appearance),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(ForzaBallPrimary.copy(alpha = 0.1f))
                        .border(2.dp, ForzaBallPrimary, shape)
                } else {
                    Modifier
                        .background(ForzaBallSurfaceContainerHighest)
                        .clickable(onClick = onClick)
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = if (selected) ForzaBallPrimary else ForzaBallOnSurfaceVariant)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ForzaBallPrimary else ForzaBallOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ForzaBallSurfaceContainerHigh,
            modifier = Modifier.border(1.dp, ForzaBallPrimary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ForzaBallErrorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = ForzaBallError,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.logout_confirm_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = ForzaBallOnSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.logout_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForzaBallOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallError),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.log_out),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForzaBallSurfaceContainerHighest,
                        contentColor = ForzaBallOnSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
