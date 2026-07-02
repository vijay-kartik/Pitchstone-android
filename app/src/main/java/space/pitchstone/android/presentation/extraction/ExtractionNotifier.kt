package space.pitchstone.android.presentation.extraction

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import space.pitchstone.android.MainActivity
import space.pitchstone.android.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the single "extraction status" notification: a dismissable progress
 * notification while the agent is working, replaced by a tappable result
 * notification (success deep-links into the Confirm screen) when it finishes.
 */
@Singleton
class ExtractionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun showInProgress() {
        notify(
            baseBuilder()
                .setContentTitle(context.getString(R.string.notification_extraction_running_title))
                .setContentText(context.getString(R.string.notification_extraction_running_text))
                .setProgress(0, 0, true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .build()
        )
    }

    fun showSuccess(replyText: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_EXTRACTION_RESULT
            putExtra(EXTRA_REPLY_TEXT, replyText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_VIEW_RESULT,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notify(
            baseBuilder()
                .setContentTitle(context.getString(R.string.notification_extraction_success_title))
                .setContentText(context.getString(R.string.notification_extraction_success_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    fun showFailure(message: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_VIEW_RESULT,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notify(
            baseBuilder()
                .setContentTitle(context.getString(R.string.notification_extraction_failure_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    fun cancel() {
        notificationManager.cancel(EXTRACTION_NOTIFICATION_ID)
    }

    private fun baseBuilder(): NotificationCompat.Builder {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_extraction)
    }

    private fun notify(notification: Notification) {
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!canPost || !notificationManager.areNotificationsEnabled()) return
        notificationManager.notify(EXTRACTION_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(context.getString(R.string.notification_channel_extraction_name))
            .setDescription(context.getString(R.string.notification_channel_extraction_description))
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_VIEW_EXTRACTION_RESULT = "space.pitchstone.android.action.VIEW_EXTRACTION_RESULT"
        const val EXTRA_REPLY_TEXT = "space.pitchstone.android.extra.REPLY_TEXT"

        private const val CHANNEL_ID = "extraction_status"
        private const val EXTRACTION_NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_VIEW_RESULT = 2001
    }
}
