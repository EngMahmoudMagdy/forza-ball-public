package com.forzaball.app.feature.personalization

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.app.ui.theme.ForzaBallPrimary

data class LeagueItem(val id: String, val name: String, val country: String, val logoUrl: String?)
data class ClubItem(val id: String, val name: String, val leagueId: String, val crestUrl: String?)

/** League ids match [API-Football](https://www.api-football.com/documentation-v3) `league` resource. */
val defaultLeagues = listOf(
    LeagueItem("39", "Premier League", "England", "https://lh3.googleusercontent.com/aida-public/AB6AXuAmtStcogjmWj7G6BNMpp5J3kaAnIMQkDGe8cSSsqnOfIy1cEur2AsNNyXtXcRzNvAj8QEIYIY0ZkeuE9VDc0laCh4g6nmQZgmjCkviNSILHHn7UI7QNzetiyUS4jWRJxkJ_qzjcDnBh7OcS7oANNRRTwmpMtqnjMq2kbjHmfMp7jDTDWPsSoe8HuT69cOcbS3HSsHVcbThQYe8wPpeuBssoGExOL-z7uMNc3TN58GQDnnbH5iRp6Wtq16d8yzBpFR2kqTNSCSeECNF"),
    LeagueItem("140", "La Liga", "Spain", null),
    LeagueItem("135", "Serie A", "Italy", null),
    LeagueItem("78", "Bundesliga", "Germany", null),
    LeagueItem("61", "Ligue 1", "France", null),
    LeagueItem("2", "Champions League", "International", null),
    LeagueItem("253", "MLS", "USA", null),
)

/** Team ids match API-Football `teams` resource (used for fixtures, live scores, injuries). */
val defaultClubs = listOf(
    ClubItem("50", "Man City", "39", "https://media.api-sports.io/football/teams/50.png"),
    ClubItem("40", "Liverpool", "39", "https://media.api-sports.io/football/teams/40.png"),
    ClubItem("42", "Arsenal", "39", "https://media.api-sports.io/football/teams/42.png"),
    ClubItem("541", "Real Madrid", "140", "https://media.api-sports.io/football/teams/541.png"),
    ClubItem("529", "Barcelona", "140", "https://media.api-sports.io/football/teams/529.png"),
    ClubItem("489", "AC Milan", "135", "https://media.api-sports.io/football/teams/489.png"),
    ClubItem("496", "Juventus", "135", "https://media.api-sports.io/football/teams/496.png"),
    ClubItem("157", "FC Bayern", "78", "https://media.api-sports.io/football/teams/157.png"),
    ClubItem("85", "Paris Saint-Germain", "61", "https://media.api-sports.io/football/teams/85.png"),
)

@Composable
fun PersonalizationStep1Screen(
    selectedLeagueIds: Set<String>,
    onToggleLeague: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
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
        Text("Select Your Favorite Leagues", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp))
        Text("Choose at least one league.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            defaultLeagues.forEach { league ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleLeague(league.id) }.padding(16.dp).border(1.dp, ForzaBallPrimary.copy(alpha = 0.1f), RoundedCornerShape(0.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = league.logoUrl, contentDescription = league.name, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(league.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(league.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier.size(24.dp).border(2.dp, if (league.id in selectedLeagueIds) ForzaBallPrimary else ForzaBallPrimary.copy(alpha = 0.3f), CircleShape).background(if (league.id in selectedLeagueIds) ForzaBallPrimary else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (league.id in selectedLeagueIds) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), enabled = selectedLeagueIds.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary), shape = RoundedCornerShape(12.dp)) {
            Text("Next", fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PersonalizationStep2Screen(
    selectedLeagueIds: Set<String>,
    selectedClubIds: Set<String>,
    onToggleClub: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clubsForLeagues = defaultClubs.filter { it.leagueId in selectedLeagueIds }
    val maxPerLeague = 3
    val countByLeague = clubsForLeagues.groupBy { it.leagueId }.mapValues { it.value.count { c -> c.id in selectedClubIds } }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Personalization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Step 2 of 3", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ForzaBallPrimary.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxWidth(2f / 3f).fillMaxSize().background(ForzaBallPrimary, RoundedCornerShape(4.dp)))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Follow Your Favorite Clubs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp))
        Text("Select up to 3 teams per league.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            clubsForLeagues.forEach { club ->
                val count = countByLeague[club.leagueId] ?: 0
                val selected = club.id in selectedClubIds
                val canSelect = selected || count < maxPerLeague
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = canSelect) { onToggleClub(club.id) }.padding(12.dp).border(2.dp, if (selected) ForzaBallPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).background(if (selected) ForzaBallPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = club.crestUrl, contentDescription = club.name, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(club.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = ForzaBallPrimary, modifier = Modifier.size(24.dp))
                    else if (canSelect) Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary), shape = RoundedCornerShape(12.dp)) {
            Text("Next", fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
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
    Column(modifier = modifier.fillMaxSize()) {
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
