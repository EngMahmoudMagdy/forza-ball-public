package com.forzaball.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forzaball.BuildConfig
import com.forzaball.R
import com.forzaball.data.preferences.LocalePreferencesRepository
import com.forzaball.data.preferences.ThemePreferencesRepository
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.AuthState
import com.forzaball.feature.personalization.catalogLeagues
import com.forzaball.ui.components.ProfileAvatarImage
import com.forzaball.ui.locale.AppLocale
import com.forzaball.ui.theme.ForzaBallBackgroundDark
import com.forzaball.ui.theme.ForzaBallError
import com.forzaball.ui.theme.ForzaBallOnSurface
import com.forzaball.ui.theme.ForzaBallOnSurfaceVariant
import com.forzaball.ui.theme.ForzaBallPrimary
import com.forzaball.ui.theme.ForzaBallSurfaceContainer
import com.forzaball.ui.theme.ForzaBallSurfaceContainerHighest
import com.forzaball.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val SettingsTileShape = RoundedCornerShape(16.dp)
private const val PRIVACY_URL = "https://forzaball.app/privacy"
private const val TERMS_URL = "https://forzaball.app/terms"

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
    val context = LocalContext.current
    val activity = LocalActivity.current

    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            selected = appLocale,
            onSelect = { locale ->
                scope.launch {
                    showLanguageSheet = false
                    if (locale != appLocale) {
                        localeRepo.setLocale(locale)
                        (activity as? ComponentActivity)?.recreate()
                    }
                }
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
    if (showAppearanceSheet) {
        AppearanceSettingsSheet(
            selected = themeMode,
            onSelect = { mode ->
                scope.launch {
                    themeRepo.setThemeMode(mode)
                    showAppearanceSheet = false
                }
            },
            onDismiss = { showAppearanceSheet = false },
        )
    }
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = onLogout,
            onDismiss = { showLogoutDialog = false },
        )
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        when (authState) {
            is AuthState.SignedIn -> {
                ProfileHeroSection(
                    displayName = userPreferences.nickname?.takeIf { it.isNotBlank() }
                        ?: authState.email.orEmpty(),
                    email = authState.email,
                    userPreferences = userPreferences,
                    fallbackUserId = authState.uid,
                    onEditProfile = onEditProfile,
                    onAvatarClick = onViewMyProfile,
                )
                Spacer(Modifier.height(8.dp))
                StitchSectionLabel(stringResource(R.string.account_settings))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClubPreferenceRow(
                        teamName = userPreferences.favoriteTeamName?.takeIf { it.isNotBlank() }
                            ?: userPreferences.favoriteTeamLeagueSlug?.let { slug ->
                                catalogLeagues.find { it.id == slug }?.name ?: slug
                            },
                        onClick = onEditFavorites,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BentoSettingsTile(
                            title = stringResource(R.string.section_appearance),
                            subtitle = appearanceSubtitle(themeMode),
                            icon = Icons.Default.Palette,
                            onClick = { showAppearanceSheet = true },
                            modifier = Modifier.weight(1f),
                        )
                        BentoSettingsTile(
                            title = stringResource(R.string.section_language),
                            subtitle = localeLabel(appLocale),
                            icon = Icons.Default.Language,
                            onClick = { showLanguageSheet = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                StitchSectionLabel(
                    text = stringResource(R.string.legal_support),
                    topPadding = 8.dp,
                )
                LegalSupportCard(
                    onPrivacyClick = { openUrl(PRIVACY_URL) },
                    onTermsClick = { openUrl(TERMS_URL) },
                    onLogoutClick = { showLogoutDialog = true },
                )
            }
            AuthState.Loading -> {
                Spacer(Modifier.height(48.dp))
                Text(
                    stringResource(R.string.loading),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            AuthState.SignedOut -> {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.sign_in_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForzaBallOnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.log_in), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.sign_up), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                StitchSectionLabel(stringResource(R.string.account_settings))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoSettingsTile(
                        title = stringResource(R.string.section_appearance),
                        subtitle = appearanceSubtitle(themeMode),
                        icon = Icons.Default.Palette,
                        onClick = { showAppearanceSheet = true },
                        modifier = Modifier.weight(1f),
                    )
                    BentoSettingsTile(
                        title = stringResource(R.string.section_language),
                        subtitle = localeLabel(appLocale),
                        icon = Icons.Default.Language,
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                StitchSectionLabel(stringResource(R.string.legal_support))
                LegalSupportCard(
                    onPrivacyClick = { openUrl(PRIVACY_URL) },
                    onTermsClick = { openUrl(TERMS_URL) },
                    onLogoutClick = { /* signed out */ },
                    showLogout = false,
                )
            }
        }
        Text(
            text = stringResource(R.string.app_version_label, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = ForzaBallOnSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileHeroSection(
    displayName: String,
    email: String?,
    userPreferences: UserPreferences,
    fallbackUserId: String,
    onEditProfile: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ForzaBallPrimary.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                            radius = 420f,
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ForzaBallPrimary, ForzaBallSurfaceContainerHighest),
                            ),
                        )
                        .padding(4.dp)
                        .clickable(onClick = onAvatarClick),
                ) {
                    ProfileAvatarImage(
                        photoUrl = userPreferences.profilePhotoUrl,
                        thumbUrl = userPreferences.profilePhotoThumbUrl,
                        cacheVersion = userPreferences.profilePhotoCacheVersion,
                        fallbackUserId = fallbackUserId,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(4.dp, ForzaBallBackgroundDark, CircleShape),
                    )
                }
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ForzaBallPrimary)
                        .border(2.dp, ForzaBallBackgroundDark, CircleShape),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_profile),
                        tint = ForzaBallOnSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                ),
                color = ForzaBallOnSurface,
                textAlign = TextAlign.Center,
            )
            email?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = ForzaBallOnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = ForzaBallPrimary.copy(alpha = 0.4f),
                        spotColor = ForzaBallPrimary.copy(alpha = 0.4f),
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.edit_profile).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ClubPreferenceRow(
    teamName: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsTileShape)
            .background(ForzaBallSurfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBox(Icons.Default.SportsSoccer)
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    stringResource(R.string.club_preference),
                    style = MaterialTheme.typography.titleSmall,
                    color = ForzaBallOnSurface,
                )
                Text(
                    text = teamName ?: stringResource(R.string.none_selected),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = ForzaBallPrimary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ForzaBallOnSurfaceVariant,
        )
    }
}

@Composable
private fun BentoSettingsTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(SettingsTileShape)
            .background(ForzaBallSurfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        SettingsIconBox(icon)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = ForzaBallOnSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = ForzaBallOnSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LegalSupportCard(
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showLogout: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsTileShape)
            .background(ForzaBallSurfaceContainer),
    ) {
        LegalRow(
            icon = Icons.Default.Policy,
            label = stringResource(R.string.privacy_policy),
            showExternal = true,
            onClick = onPrivacyClick,
        )
        LegalDivider()
        LegalRow(
            icon = Icons.Default.Description,
            label = stringResource(R.string.terms_conditions),
            showExternal = true,
            onClick = onTermsClick,
        )
        if (showLogout) {
            LegalDivider()
            LegalRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(R.string.log_out),
                showExternal = false,
                onClick = onLogoutClick,
                accent = true,
            )
        }
    }
}

@Composable
private fun LegalDivider() {
    HorizontalDivider(
        color = ForzaBallPrimary.copy(alpha = 0.1f),
        thickness = 1.dp,
    )
}

@Composable
private fun LegalRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    showExternal: Boolean,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (accent) Modifier.background(ForzaBallError.copy(alpha = 0.08f))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent) ForzaBallError else ForzaBallOnSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (accent) ForzaBallError else ForzaBallOnSurface,
            )
        }
        if (showExternal) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = ForzaBallOnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsIconBox(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ForzaBallPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ForzaBallPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StitchSectionLabel(
    text: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
        color = ForzaBallOnSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = topPadding, bottom = 8.dp),
    )
}

@Composable
private fun appearanceSubtitle(mode: ThemeMode): String = when (mode) {
    ThemeMode.Dark -> stringResource(R.string.appearance_dark_mode)
    ThemeMode.Light -> stringResource(R.string.appearance_light_mode)
    ThemeMode.System -> stringResource(R.string.theme_system)
}

@Composable
private fun localeLabel(locale: AppLocale): String = when (locale) {
    AppLocale.English -> stringResource(R.string.language_english)
    AppLocale.Arabic -> stringResource(R.string.language_arabic)
}
