package com.forzaball.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.R
import com.forzaball.ui.theme.ForzaBallPrimary
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val pickPhoto = rememberProfilePhotoPicker(onPhotoSelected = viewModel::uploadPhoto)

    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(ForzaBallPrimary.copy(alpha = 0.1f))
                    .border(2.dp, ForzaBallPrimary.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = pickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                val photo = ui.photoUrl ?: ui.thumbUrl
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = ForzaBallPrimary, modifier = Modifier.size(48.dp))
                }
                if (ui.isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp), color = ForzaBallPrimary)
                } else {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .padding(4.dp)
                            .background(ForzaBallPrimary, CircleShape)
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.upload_photo), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.upload_photo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForzaBallPrimary.copy(alpha = 0.5f),
                cursorColor = ForzaBallPrimary,
            )
            OutlinedTextField(
                value = ui.nickname,
                onValueChange = viewModel::setNickname,
                label = { Text(stringResource(R.string.nickname)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = ForzaBallPrimary) },
                colors = fieldColors,
            )
            Text(
                stringResource(R.string.nickname_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::saveProfile,
                enabled = !ui.isSaving && ui.nickname.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (ui.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
