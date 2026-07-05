package veljko.najdanovic19273.fitmap

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import veljko.najdanovic19273.fitmap.ui.navigation.AppNavigation
import veljko.najdanovic19273.fitmap.ui.theme.FitMapTheme
import veljko.najdanovic19273.fitmap.util.CloudinaryHelper

class MainActivity : ComponentActivity() {

    // Launcher za traženje dozvole za notifikacije
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notifikacije dozvoljene!")
            checkNotificationChannelSettings()
        } else {
            Log.w("MainActivity", "⚠️ Korisnik je odbio notifikacije!")
            // Možeš prikazati dijalog koji objašnjava važnost notifikacija
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Starting app initialization")

            // Inicijalizuj Cloudinary
            CloudinaryHelper.initialize(this)

            // NOVO: Proveri i zatraži dozvole za notifikacije
            checkAndRequestNotificationPermissions()

            setContent {
                FitMapTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
            Log.d("MainActivity", "App initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            throw e
        }
    }

    private fun checkAndRequestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ zahteva POST_NOTIFICATIONS dozvolu
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Na starijim verzijama proveravamo da li su notifikacije omogućene
            checkNotificationChannelSettings()
        }
    }

    private fun checkNotificationChannelSettings() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Provera da li su notifikacije omogućene na nivou aplikacije
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()

        Log.d("MainActivity", "Notifikacije omogućene: $areNotificationsEnabled")

        if (!areNotificationsEnabled) {
            Log.w("MainActivity", "⚠️ Notifikacije su ONEMOGUĆENE u podešavanjima!")
            // Ovde možeš prikazati dijalog koji će odvesti korisnika u podešavanja
            // openNotificationSettings()
        }

        // Provera kanala na Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = notificationManager.notificationChannels
            Log.d("MainActivity", "Broj notification kanala: ${channels.size}")

            channels.forEach { channel ->
                Log.d("MainActivity", "Kanal: ${channel.name}, Importance: ${channel.importance}")
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.parse("package:$packageName")
                }
            }
        }
        startActivity(intent)
    }
}
