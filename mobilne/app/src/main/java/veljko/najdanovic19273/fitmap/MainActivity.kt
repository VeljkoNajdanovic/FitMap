package veljko.najdanovic19273.fitmap

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import veljko.najdanovic19273.fitmap.ui.navigation.AppNavigation
import veljko.najdanovic19273.fitmap.ui.theme.FitMapTheme
import veljko.najdanovic19273.fitmap.util.CloudinaryHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Starting app initialization")

            // Inicijalizuj Cloudinary
            CloudinaryHelper.initialize(this)

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
}
