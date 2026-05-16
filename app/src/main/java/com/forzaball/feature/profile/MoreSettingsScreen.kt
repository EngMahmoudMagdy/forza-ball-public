package com.forzaball.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.data.preferences.LocalePreferencesRepository
import com.forzaball.data.preferences.ThemePreferencesRepository
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.AuthState
import com.forzaball.feature.personalization.catalogLeagues
import com.forzaball.ui.locale.AppLocale
import com.forzaball.ui.theme.ForzaBallPrimary
import com.forzaball.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MoreSettingsScreen(
    authState: AuthState,
    userPreferences: UserPreferences,
    onEditProfile: () -> Unit,
    onViewMyProfile: () -> Unit,
    onEditFavorites: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeRepo: ThemePreferencesRepository = koinInject()
    val localeRepo: LocalePreferencesRepository = koinInject()
    val themeMode by themeRepo.observeThemeMode().collectAsState(initial = ThemeMode.Dark)
    val appLocale by localeRepo.observeLocale().collectAsState(initial = AppLocale.English)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (authState) {
            is AuthState.SignedIn -> {
                SettingsSection(title = stringResource(R.string.section_profile)) {
                    authState.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Text(
                            text = stringResource(R.string.signed_in_as, email),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = onViewMyProfile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.view_my_profile), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold)
                    }
                }

                SettingsSection(title = stringResource(R.string.section_club_preferences)) {
                    Text(
                        stringResource(R.string.favorite_league),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val leagueLabel = userPreferences.favoriteTeamLeagueSlug?.let { slug ->
                        catalogLeagues.find { it.id == slug }?.name ?: slug
                    }
                    Text(
                        leagueLabel ?: stringResource(R.string.none_selected),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.favorite_team),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        userPreferences.favoriteTeamName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.none_selected),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onEditFavorites,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.change_favorite_team), fontWeight = FontWeight.Bold)
                    }
                }
            }
            AuthState.Loading -> {
                Text(stringResource(R.string.loading))
            }
            AuthState.SignedOut -> {
                SettingsSection(title = stringResource(R.string.section_profile)) {
                    Text(
                        stringResource(R.string.sign_in_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.log_in), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSignUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.sign_up), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.section_appearance)) {
            Text(
                stringResource(R.string.theme),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.Dark -> stringResource(R.string.theme_dark)
                    ThemeMode.Light -> stringResource(R.string.theme_light)
                    ThemeMode.System -> stringResource(R.string.theme_system)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = themeMode == mode,
                            onClick = { scope.launch { themeRepo.setThemeMode(mode) } },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = { scope.launch { themeRepo.setThemeMode(mode) } },
                        colors = RadioButtonDefaults.colors(selectedColor = ForzaBallPrimary),
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        SettingsSection(title = stringResource(R.string.section_language)) {
            AppLocale.entries.forEach { locale ->
                val label = when (locale) {
                    AppLocale.English -> stringResource(R.string.language_english)
                    AppLocale.Arabic -> stringResource(R.string.language_arabic)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = appLocale == locale,
                            onClick = { scope.launch { localeRepo.setLocale(locale) } },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = appLocale == locale,
                        onClick = { scope.launch { localeRepo.setLocale(locale) } },
                        colors = RadioButtonDefaults.colors(selectedColor = ForzaBallPrimary),
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (authState is AuthState.SignedIn) {
            SettingsSection(title = stringResource(R.string.section_account)) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.log_out), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
