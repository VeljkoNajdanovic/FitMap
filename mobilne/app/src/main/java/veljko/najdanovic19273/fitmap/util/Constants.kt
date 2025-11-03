package veljko.najdanovic19273.fitmap.util

object Constants {
    // Firebase Collections
    const val USERS_COLLECTION = "users"
    const val OBJECTS_COLLECTION = "objects"
    const val COMMENTS_COLLECTION = "comments"
    const val RATINGS_COLLECTION = "ratings"
    const val USER_LOCATIONS_COLLECTION = "user_locations"

    // Cloudinary Configuration (UNSIGNED upload - bezbedno za klijent)
    // NAPOMENA: NE čuvaj API_SECRET u klijentu! Koristi unsigned preset.
    const val CLOUDINARY_CLOUD_NAME = "dhogxvur5"
    const val CLOUDINARY_UPLOAD_PRESET = "fitmap_uploads" // Unsigned upload preset

    // Points System
    const val POINTS_ADD_OBJECT = 10
    const val POINTS_ADD_COMMENT = 3
    const val POINTS_ADD_RATING = 2

    // Notification Radius (u metrima)
    const val NOTIFICATION_RADIUS = 100.0

    // Location Update Interval
    const val LOCATION_UPDATE_INTERVAL = 10000L // 10 sekundi
    const val LOCATION_FASTEST_INTERVAL = 5000L // 5 sekundi
}
