package space.pitchstone.android.presentation.extraction

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.pitchstone.android.domain.model.Attachment
import space.pitchstone.android.presentation.util.ImageCompressor
import javax.inject.Inject

/**
 * Converts content [Uri]s (from the photo picker or an incoming share intent)
 * into [Attachment]s ready for upload: images are downscaled and re-encoded as
 * JPEG, PDFs are read as-is. Unreadable or unsupported files are skipped.
 */
class AttachmentPreparer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun prepare(uris: List<Uri>): List<Attachment> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri -> toAttachment(uri) }
    }

    private fun toAttachment(uri: Uri): Attachment? {
        return try {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            when {
                mimeType == PDF_MIME_TYPE -> readPdf(uri)
                // Missing mime type is treated as an image, matching the photo-picker default.
                mimeType.startsWith(IMAGE_MIME_PREFIX) || mimeType.isEmpty() -> compressImage(uri)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImage(uri: Uri): Attachment? {
        val bytes = ImageCompressor.compressUri(context, uri) ?: return null
        val name = displayName(uri) ?: DEFAULT_IMAGE_NAME
        return Attachment(name, bytes, JPEG_MIME_TYPE)
    }

    private fun readPdf(uri: Uri): Attachment? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        // Guards against loading arbitrarily large shared documents fully into memory.
        if (bytes.size > MAX_PDF_SIZE_BYTES) return null
        val name = displayName(uri) ?: DEFAULT_PDF_NAME
        return Attachment(name, bytes, PDF_MIME_TYPE)
    }

    private fun displayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index != -1) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val IMAGE_MIME_PREFIX = "image/"
        const val DEFAULT_IMAGE_NAME = "screenshot.jpg"
        const val DEFAULT_PDF_NAME = "document.pdf"
        const val MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024
    }
}
