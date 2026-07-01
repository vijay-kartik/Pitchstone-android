package space.pitchstone.android.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {
    fun compressUri(context: Context, uri: Uri, maxDimension: Int = 2000, quality: Int = 85): ByteArray? {
        return try {
            val contentResolver = context.contentResolver
            
            // First decode bounds to find dimensions
            var inputStream = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate scale factor
            val width = options.outWidth
            val height = options.outHeight
            var scale = 1
            if (width > maxDimension || height > maxDimension) {
                val largest = max(width, height)
                scale = (largest / maxDimension).coerceAtLeast(1)
            }
            
            // Decode with scaling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
            inputStream.close()
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            
            byteArray
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
