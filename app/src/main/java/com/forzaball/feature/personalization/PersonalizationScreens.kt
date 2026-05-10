package com.forzaball.feature.personalization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.ui.theme.ForzaBallPrimary

/** Full-screen gradient used on league, team, and profile onboarding steps. */
fun Modifier.personalizationPageBackground(): Modifier = composed {
    background(
        brush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                ForzaBallPrimary.copy(alpha = 0.09f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        ),
    )
}

@Composable
fun PersonalizationStep1Screen(
    selectedLeagueId: String?,
    onSelectLeague: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isLoadingTeams: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .personalizationPageBackground()
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Personalization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Step 1 of 3", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ForzaBallPrimary.copy(alpha = 0.2f)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(1f / 3f).fillMaxSize().background(ForzaBallPrimary, RoundedCornerShape(4.dp)))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select your favorite league", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp))
        Text("Choose the league your club plays in. Champions League fixtures are included when available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            domesticLeagueCatalog.forEach { league ->
                val selected = league.id == selectedLeagueId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, ForzaBallPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { onSelectLeague(league.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = league.logoUrl, contentDescription = league.name, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(league.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(league.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier.size(24.dp).border(2.dp, if (selected) ForzaBallPrimary else ForzaBallPrimary.copy(alpha = 0.3f), CircleShape).background(if (selected) ForzaBallPrimary else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            enabled = selectedLeagueId != null && !isLoadingTeams,
            colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (isLoadingTeams) "Loading teams…" else "Next", fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        }
    }
}

@Composable
fun PersonalizationStep2Screen(
    clubs: List<ClubItem>,
    selectedClubId: String?,
    onSelectClub: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    primaryActionLabel: String = "Next",
    primaryActionEnabled: Boolean = true,
    step2OfTotal: Int = 3,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .personalizationPageBackground()
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Personalization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Step 2 of $step2OfTotal", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ForzaBallPrimary.copy(alpha = 0.2f))) {
                val frac = if (step2OfTotal <= 2) 1f else 2f / 3f
                Box(modifier = Modifier.fillMaxWidth(frac).fillMaxSize().background(ForzaBallPrimary, RoundedCornerShape(4.dp)))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Pick your favorite team", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp))
        Text("Select one club. You can change it anytime from your profile.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            clubs.forEach { club ->
                val selected = club.id == selectedClubId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) ForzaBallPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        )
                        .border(
                            width = 2.dp,
                            color = if (selected) ForzaBallPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelectClub(club.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = club.crestUrl, contentDescription = club.name, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(club.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = ForzaBallPrimary, modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            enabled = primaryActionEnabled && selectedClubId != null,
            colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(primaryActionLabel, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        }
    }
}

@Composable
fun PersonalizationStep3Screen(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    profileImageUrl: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ForzaBallPrimary.copy(alpha = 0.5f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = ForzaBallPrimary,
        cursorColor = ForzaBallPrimary,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .personalizationPageBackground()
            .safeDrawingPadding(),
    ) {
    Column(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Complete Your Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Step 3 of 3", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ForzaBallPrimary.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxSize().background(ForzaBallPrimary, RoundedCornerShape(4.dp)))
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(128.dp).clip(CircleShape).background(ForzaBallPrimary.copy(alpha = 0.1f)).border(2.dp, ForzaBallPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (profileImageUrl != null) AsyncImage(model = profileImageUrl, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(48.dp), tint = ForzaBallPrimary)
                Icon(Icons.Default.Edit, contentDescription = "Edit photo", modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).padding(4.dp).background(ForzaBallPrimary, CircleShape).padding(6.dp), tint = androidx.compose.ui.graphics.Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Upload Photo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Add a photo so teammates can find you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = nickname, onValueChange = onNicknameChange, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForzaBallPrimary) }, colors = fieldColors)
            Text("Your nickname will be visible on leaderboards.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary), shape = RoundedCornerShape(12.dp)) {
            Text("Finish", fontWeight = FontWeight.Bold)
        }
    }
    }
}
