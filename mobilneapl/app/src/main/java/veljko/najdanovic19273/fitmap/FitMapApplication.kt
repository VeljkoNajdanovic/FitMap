package veljko.najdanovic19273.fitmap

import android.app.Application
import com.google.android.gms.maps.MapsInitializer

class FitMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Maps SDK; this ensures Maps are ready and picks up the API key from resources/manifest
        try {
            MapsInitializer.initialize(this.applicationContext)
        } catch (e: Exception) {
            // Swallow; Maps library may already be initialized or fail if API key invalid
            e.printStackTrace()
        }
    }
}

