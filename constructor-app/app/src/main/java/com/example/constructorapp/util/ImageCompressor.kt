package com.example.constructorapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Client-Side Image Compression Engine matching webapp specification:
 * - Max dimension: 2560px (longest edge)
 * - Compression format: JPEG
 * - Initial quality: 80%
 * - Target file size: < 1.5MB (1,500,000 bytes)
 * - Automatic EXIF orientation normalization
 * - Coroutine-safe on Dispatchers.IO
 */
object ImageCompressor {
    private const val MAX_DIMENSION = 2560
    private const val TARGET_MAX_BYTES = 1_500_000L // 1.5 MB
    private const val INITIAL_QUALITY = 80
    private const val MIN_QUALITY = 45
    private const val QUALITY_STEP = 5

    /**
     * Compresses an image given its Android content Uri.
     */
    suspend fun compressImage(context: Context, imageUri: Uri): File = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // Step 1: Read EXIF orientation before stream consumption
        val rotationDegrees = getExifRotation(context, imageUri)

        // Step 2: Decode image bounds without allocating pixel memory
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("Unable to open input stream from URI: $imageUri")

        val origWidth = options.outWidth
        val origHeight = options.outHeight

        // Step 3: Compute inSampleSize (power of 2) to fit within MAX_DIMENSION
        var inSampleSize = 1
        val maxDim = maxOf(origWidth, origHeight)
        if (maxDim > MAX_DIMENSION) {
            inSampleSize = 2
            while ((maxDim / inSampleSize) > MAX_DIMENSION) {
                inSampleSize *= 2
            }
        }

        // Step 4: Decode downsampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IOException("Failed to decode bitmap from URI: $imageUri")

        // Step 5: Rotate bitmap if EXIF indicated non-zero orientation
        val rotatedBitmap = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
            if (rotated != sampledBitmap) sampledBitmap.recycle()
            rotated
        } else {
            sampledBitmap
        }

        // Step 6: Scale down proportionally if still exceeding MAX_DIMENSION
        val finalBitmap = if (rotatedBitmap.width > MAX_DIMENSION || rotatedBitmap.height > MAX_DIMENSION) {
            val ratio = minOf(MAX_DIMENSION.toFloat() / rotatedBitmap.width, MAX_DIMENSION.toFloat() / rotatedBitmap.height)
            val newWidth = (rotatedBitmap.width * ratio).toInt()
            val newHeight = (rotatedBitmap.height * ratio).toInt()
            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)
            if (scaled != rotatedBitmap) rotatedBitmap.recycle()
            scaled
        } else {
            rotatedBitmap
        }

        // Step 7: Iterative JPEG quality reduction to meet < 1.5MB ceiling
        val outputDir = File(context.cacheDir, "compressed_images").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "compressed_${UUID.randomUUID()}.jpg")

        var quality = INITIAL_QUALITY
        var streamBytes: ByteArray

        do {
            val byteStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
            streamBytes = byteStream.toByteArray()
            quality -= QUALITY_STEP
        } while (streamBytes.size > TARGET_MAX_BYTES && quality >= MIN_QUALITY)

        finalBitmap.recycle()

        FileOutputStream(outputFile).use { fos ->
            fos.write(streamBytes)
            fos.flush()
        }

        outputFile
    }

    /**
     * Compresses an existing File.
     */
    suspend fun compressImageFile(context: Context, inputFile: File): File = withContext(Dispatchers.IO) {
        val uri = Uri.fromFile(inputFile)
        compressImage(context, uri)
    }

    /**
     * Reads EXIF rotation from image stream.
     */
    private fun getExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }
}
