package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Turns any photo the user picks into a small, square, upright JPEG that is cheap to upload. */
object AvatarImage {
    private const val TARGET_SIZE = 512
    private const val JPEG_QUALITY = 88

    fun toSquareJpeg(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // 1. Read the size first so big photos are decoded at a smaller scale.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); true }
        if (opened != true) throw ApiException("Could not open that picture")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ApiException("That file is not a picture")

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_SIZE && bounds.outHeight / (sample * 2) >= TARGET_SIZE) {
            sample *= 2
        }

        // 2. Decode.
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw ApiException("Could not read that picture")

        // 3. Turn it upright if the camera stored it rotated.
        val degrees = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val upright = if (degrees != 0f) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
        } else decoded

        // 4. Centre-crop to a square and shrink.
        val side = minOf(upright.width, upright.height)
        val square = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val finalBitmap = if (side > TARGET_SIZE) Bitmap.createScaledBitmap(square, TARGET_SIZE, TARGET_SIZE, true) else square

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return out.toByteArray()
    }
}
