package veljko.najdanovic19273.fitmap.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import veljko.najdanovic19273.fitmap.MainActivity
import veljko.najdanovic19273.fitmap.R

class FitMapMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "fitmap_notifications"
        private const val CHANNEL_NAME = "FitMap Obaveštenja"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // Poziva se kada se primi nova poruka
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            showNotification(
                title = it.title ?: "FitMap",
                body = it.body ?: ""
            )
        }

        // Obrada podataka iz poruke
        message.data.isNotEmpty().let {
            // Možeš ovde obraditi custom podatke
            val objectId = message.data["objectId"]
            val userId = message.data["userId"]
            // ...
        }
    }

    // Poziva se kada se generiše novi FCM token
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Ovde treba sačuvati token u Firestore
        // To ćeš raditi iz AuthRepository.updateFcmToken()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Obaveštenja o objektima i korisnicima u blizini"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

