package com.forzaball.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.min

data class ProcessedProfileImages(
    val fullJpeg: ByteArray,
    val thumbJpeg: ByteArray,
)

object ProfileImageProcessor {
    private const val FULL_MAX_PX = 800
    private const val THUMB_MAX_PX = 128
    private const val FULL_QUALITY = 85
    private const val THUMB_QUALITY = 75

    fun process(context: Context, uri: Uri): ProcessedProfileImages {
        val source = decodeSampled(context, uri, maxSide = 2048)
        val oriented = applyExifOrientation(context, uri, source)
        val squared = centerCropSquare(oriented)
        if (squared !== oriented && oriented !== source) oriented.recycle()
        if (source !== oriented && source !== squared) source.recycle()
        val full = scaleToMax(squared, FULL_MAX_PX)
        val thumb = scaleToMax(squared, THUMB_MAX_PX)
        squared.recycle()
        val fullBytes = compressJpeg(full, FULL_QUALITY)
        val thumbBytes = compressJpeg(thumb, THUMB_QUALITY)
        full.recycle()
        thumb.recycle()
        return ProcessedProfileImages(fullBytes, thumbBytes)
    }

    private fun decodeSampled(context: Context, uri: Uri, maxSide: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, opts)
                ?: error("Could not decode image")
        }
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val rotation = runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                val exif = ExifInterface(stream!!)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)
        if (rotation == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCropSquare(source: Bitmap): Bitmap {
        val side = min(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return Bitmap.createBitmap(source, x, y, side, side)
    }

    private fun scaleToMax(source: Bitmap, maxPx: Int): Bitmap {
        val side = max(source.width, source.height)
        if (side <= maxPx) return source.copy(Bitmap.Config.ARGB_8888, true)
        val scale = maxPx.toFloat() / side
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
