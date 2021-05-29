package info.mqtt.android.extsample.activity

import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import info.mqtt.android.extsample.R

/**
 * Provides static methods for creating and showing notifications to the user.
 */
internal object Notify {

    private var MessageID = 0

    @JvmStatic
    fun notification(context: Context, messageString: String, intent: Intent?, notificationTitle: Int) {

        //Get the notification manage which we will use to display the notification
        val ns = Context.NOTIFICATION_SERVICE
        val mNotificationManager = context.getSystemService(ns) as NotificationManager
        val `when` = System.currentTimeMillis()

        //get the notification title from the application's strings.xml file
        val contentTitle: CharSequence = context.getString(notificationTitle)

        //the message that will be displayed as the ticker
        val ticker = "$contentTitle $messageString"

        //build the pending intent that will start the appropriate activity
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        //build the notification
        val notificationCompat = NotificationCompat.Builder(context)
        notificationCompat.setAutoCancel(true)
            .setContentTitle(contentTitle)
            .setContentIntent(pendingIntent)
            .setContentText(messageString)
            .setTicker(ticker)
            .setWhen(`when`)
            .setSmallIcon(R.mipmap.ic_launcher)
        val notification = notificationCompat.build()
        //display the notification
        mNotificationManager.notify(MessageID, notification)
        MessageID++
    }

    /**
     * Display a toast notification to the user
     *
     * @param context  Context from which to create a notification
     * @param text     The text the toast should display
     * @param duration The amount of time for the toast to appear to the user
     */
    @JvmStatic
    fun toast(context: Context?, text: CharSequence?, duration: Int) {
        val toast = Toast.makeText(context, text, duration)
        toast.show()
    }
}