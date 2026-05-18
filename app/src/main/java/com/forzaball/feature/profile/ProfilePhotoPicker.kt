package com.forzaball.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.launch

/**
 * Gallery pick → crop/rotate screen → cropped square JPEG for upload.
 */
@Composable
fun rememberProfilePhotoPicker(
    onPhotoSelected: suspend (Uri) -> Unit,
    onError: (Throwable) -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val cropped = result.uriContent ?: return@rememberLauncherForActivityResult
            scope.launch {
                runCatching { onPhotoSelected(cropped) }.onFailure(onError)
            }
        }
    }

    fun launchCrop(sourceUri: Uri) {
        cropLauncher.launch(
            CropImageContractOptions(
                uri = sourceUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true,
                    cropShape = CropImageView.CropShape.RECTANGLE,
                    allowRotation = true,
                    allowFlipping = true,
                    initialCropWindowPaddingRatio = 0.12f,
                    activityBackgroundColor = Color.BLACK,
                    toolbarColor = Color.BLACK,
                    toolbarTitleColor = Color.WHITE,
                    toolbarBackButtonColor = Color.WHITE,
                    toolbarTintColor = Color.WHITE,
                    activityMenuIconColor = Color.WHITE,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 95,
                ),
            ),
        )
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) launchCrop(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    return {
        val pickRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val permission = Manifest.permission.READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    pickLauncher.launch(pickRequest)
                } else {
                    permissionLauncher.launch(permission)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    pickLauncher.launch(pickRequest)
                } else {
                    permissionLauncher.launch(permission)
                }
            }
            else -> pickLauncher.launch(pickRequest)
        }
    }
}
