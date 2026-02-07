package ru.dimarzio.rulearn2.application

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.utils.notifyPermissionGranted
import java.io.File

class RepeatReceiver : BroadcastReceiver() {
    companion object {
        private const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel"
    }

    private inline fun <R> Database.use(block: Database.() -> R) = database.use { block() }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val result = runCatching {
            intent.getStringExtra("folder")
                ?.let { folder -> Database(File(folder)) } // Create new instance because of possible closing main one.
                ?.use { getCoursesNames().filter { course -> getRepeatWords(course) > 0 } }
        }

        val courses = result.getOrDefault(emptyList())

        if (courses?.isNotEmpty() == true && context.notifyPermissionGranted) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_reminder_24)
                .setContentTitle("It's time to refresh your memory!")
                .setContentText("Some words for repetition are found in ${courses.joinToString()}")
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        1,
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }
}