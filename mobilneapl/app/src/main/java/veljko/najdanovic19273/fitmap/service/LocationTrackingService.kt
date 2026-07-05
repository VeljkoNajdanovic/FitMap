package veljko.najdanovic19273.fitmap.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import veljko.najdanovic19273.fitmap.MainActivity
import veljko.najdanovic19273.fitmap.R
import veljko.najdanovic19273.fitmap.util.Constants
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Background service za praćenje lokacije korisnika
 * Periodično šalje lokaciju na Firebase i detektuje blizinu objekata
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = FirebaseFirestore.getInstance()
    private val notifiedObjects = mutableSetOf<String>() // Sprečava duplikate notifikacija - samo prvi put za svaki objekat
    private val pendingNotifications = mutableSetOf<String>() // Kratkotrajna zaštita od duplog poziva (500ms)

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val CHANNEL_NAME = "Praćenje lokacije"


        private const val PROXIMITY_RADIUS_METERS = 100.0 // 100 metara
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 sekundi
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 sekundi
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "═══════════════════════════════���═══════")
        Log.d(TAG, "🚀 LocationTrackingService KREIRAN")
        Log.d(TAG, "═══════════════════════════════════════")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Foreground service pokrenut")

        setupLocationCallback()
        startLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Location Tracking Service pokrenut")
        return START_STICKY // Automatski restartuj ako sistem ubije servis
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Nova lokacija: ${location.latitude}, ${location.longitude}")

                    // 1. Sačuvaj lokaciju u Firestore
                    saveLocationToFirestore(location)

                    // 2. Proveri blizinu objekata
                    checkProximityToObjects(location)

                    // 3. NOVO: Pošalji broadcast sa novom lokacijom
                    sendLocationBroadcast(location)
                }
            }
        }
    }

    // NOVO: Funkcija za slanje broadcast-a sa lokacijom
    private fun sendLocationBroadcast(location: Location) {
        val intent = Intent("veljko.najdanovic19273.fitmap.LOCATION_UPDATE").apply {
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("accuracy", location.accuracy)
        }
        sendBroadcast(intent)
        Log.d(TAG, "📡 Broadcast poslat: ${location.latitude}, ${location.longitude}")
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "✅ Location updates započeti (interval: ${LOCATION_UPDATE_INTERVAL/1000}s)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ GREŠKA: Nedostaje dozvola za lokaciju!", e)
        }
    }

    private fun saveLocationToFirestore(location: Location) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ Korisnik nije prijavljen, ne mogu da sačuvam lokaciju")
            return
        }

        val locationData = hashMapOf(
            "userId" to userId,
            "location" to GeoPoint(location.latitude, location.longitude),
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "accuracy" to location.accuracy
        )

        firestore.collection(Constants.USER_LOCATIONS_COLLECTION)
            .add(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Lokacija sačuvana u Firestore: ${location.latitude}, ${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ GREŠKA pri čuvanju lokacije: ${e.message}", e)
            }
    }

    private fun checkProximityToObjects(currentLocation: Location) {
        Log.d(TAG, "🔍 Proveravam objekte u blizini...")

        // Učitaj sve objekte iz Firestore-a
        firestore.collection(Constants.OBJECTS_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "📦 Učitano ${snapshot.size()} objekata iz baze")

                if (snapshot.isEmpty) {
                    Log.w(TAG, "⚠️ Nema nijednog objekta u bazi!")
                    return@addOnSuccessListener
                }

                snapshot.documents.forEach { doc ->
                    val objectId = doc.id
                    val title = doc.getString("title") ?: "Nepoznat objekat"
                    val geoPoint = doc.getGeoPoint("location")
                    val type = doc.getString("type") ?: "GYM"
                    val parentGymId = doc.getString("parentGymId")

                    geoPoint?.let {
                        val distance = calculateDistance(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            it.latitude,
                            it.longitude
                        )

                        Log.d(TAG, "📍 '$title': ${distance.toInt()}m (parentGymId: ${parentGymId ?: "null"})")

                        // PRESKOČIMO child objekte (sprave u teretanama) - šaljemo notifikaciju SAMO za glavne teretane
                        if (parentGymId != null) {
                            Log.d(TAG, "⏭️ Preskačem child objekat '$title' - notifikacija će stići samo za roditeljsku teretanu")
                            return@let // Nastavi sa sledećim objektom
                        }

                        // Ako je objekat u blizini
                        if (distance <= PROXIMITY_RADIUS_METERS) {
                            // Proveri da li je objekat već notifikovan
                            if (!notifiedObjects.contains(objectId)) {
                                Log.d(TAG, "🔔 BLIZU! Šaljem notifikaciju za '$title' (${distance.toInt()}m)")

                                // Dodaj objekat u notifikovane
                                notifiedObjects.add(objectId)

                                // Šalji notifikaciju samo za glavne teretane
                                sendProximityNotification(objectId, title, type, distance.toInt())
                            }
                        }

                        // Resetuj notifikaciju ako korisnik ode daleko
                        if (distance > PROXIMITY_RADIUS_METERS * 2) {
                            if (notifiedObjects.remove(objectId)) {
                                Log.d(TAG, "🔄 Reset notifikacije za '$title' (daleko: ${distance.toInt()}m)")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ GREŠKA pri učitavanju objekata: ${e.message}", e)
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // metara
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun sendProximityNotification(objectId: String, title: String, type: String, distance: Int) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "📲 ŠALJEM NOTIFIKACIJU:")
        Log.d(TAG, "   Objekat: $title")
        Log.d(TAG, "   Tip: $type")
        Log.d(TAG, "   Distanca: ${distance}m")
        Log.d(TAG, "═══════════════════════════════════════")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                Log.e(TAG, "❌ KANAL ZA NOTIFIKACIJE NE POSTOJI! Kreiram ponovo...")
                createNotificationChannel()
            } else {
                Log.d(TAG, "✅ Kanal postoji: ${channel.name}, importance: ${channel.importance}")
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OBJECT_ID", objectId)
        }

        val uniqueRequestCode = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // KLJUČNO: Full screen intent za GARANTOVANI heads-up prikaz
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            uniqueRequestCode + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeEmoji = when (type) {
            "GYM" -> "🏋️"
            "EQUIPMENT" -> "💪"
            "EVENT" -> "📅"
            "TRAINER_RECOMMENDATION" -> "🎯"
            else -> "📍"
        }

        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$typeEmoji Objekat u blizini!")
            .setContentText("$title je na ${distance}m od vas")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$title je na ${distance}m od vas. Kliknite da vidite detalje."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(defaultSoundUri)
            .setLights(android.graphics.Color.BLUE, 1000, 1000)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)  // SVAKI PUT zvuk i vibracija
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setGroup("proximity_group_$objectId")  // Svaki objekat ima svoju grupu
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .build()

        val uniqueNotificationId = System.currentTimeMillis().toInt()

        try {
            notificationManager.notify(uniqueNotificationId, notification)
            Log.d(TAG, "✅ Notifikacija poslata sa JEDINSTVENIM ID: $uniqueNotificationId (Objekat: $title)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ GREŠKA pri slanju notifikacije: ${e.message}", e)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FitMap aktivna")
            .setContentText("Praćenje lokacije i objekata u blizini...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // NE brišemo kanal svaki put - samo kreiraj ako ne postoji
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null) {
                Log.d(TAG, "✅ Kanal već postoji, preskačem kreiranje")
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "VAŽNE notifikacije za objekte u blizini"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅✅✅ KANAL KREIRAN SA MAKSIMALNIM PRIORITETOM ✅✅✅")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "═══════════════════════════════���═══════")
        Log.d(TAG, "🛑 LocationTrackingService ZAUSTAVLJEN")
        Log.d(TAG, "═══════════════════════���═══════���═══════")
    }
}
