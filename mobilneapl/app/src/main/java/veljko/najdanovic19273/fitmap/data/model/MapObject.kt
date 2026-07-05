package veljko.najdanovic19273.fitmap.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MapObject(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ObjectType = ObjectType.GYM,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val attributes: Map<String, String> = emptyMap(), // Dodatni atributi (npr. "wifi_password": "12345")
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val commentsCount: Int = 0,
    val parentGymId: String? = null  // ID roditeljske teretane (null za same teretane)
) {
    // No-arg constructor za Firestore deserijalizaciju
    constructor() : this(
        id = "",
        title = "",
        description = "",
        type = ObjectType.GYM,
        location = GeoPoint(0.0, 0.0),
        imageUrl = "",
        authorId = "",
        authorName = "",
        createdAt = null,
        attributes = emptyMap(),
        averageRating = 0.0,
        ratingsCount = 0,
        commentsCount = 0,
        parentGymId = null
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "type" to type.name,
            "location" to location,
            "imageUrl" to imageUrl,
            "authorId" to authorId,
            "authorName" to authorName,
            "createdAt" to createdAt,
            "attributes" to attributes,
            "averageRating" to averageRating,
            "ratingsCount" to ratingsCount,
            "commentsCount" to commentsCount,
            "parentGymId" to parentGymId
        )
    }

    companion object {
        // Pomoćna metoda za kreiranje MapObject-a iz Firestore DocumentSnapshot
        fun fromFirestore(documentId: String, data: Map<String, Any?>): MapObject? {
            return try {
                val typeString = data["type"] as? String ?: return null
                val type = try {
                    ObjectType.valueOf(typeString)
                } catch (e: Exception) {
                    return null
                }

                MapObject(
                    id = documentId, // KLJUČNO: Eksplicitno postavljamo ID!
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    type = type,
                    location = data["location"] as? com.google.firebase.firestore.GeoPoint
                        ?: com.google.firebase.firestore.GeoPoint(0.0, 0.0),
                    imageUrl = data["imageUrl"] as? String ?: "",
                    authorId = data["authorId"] as? String ?: "",
                    authorName = data["authorName"] as? String ?: "",
                    createdAt = data["createdAt"] as? java.util.Date,
                    attributes = (data["attributes"] as? Map<*, *>)?.mapKeys { it.key.toString() }
                        ?.mapValues { it.value.toString() } ?: emptyMap(),
                    averageRating = (data["averageRating"] as? Number)?.toDouble() ?: 0.0,
                    ratingsCount = (data["ratingsCount"] as? Number)?.toInt() ?: 0,
                    commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0,
                    parentGymId = data["parentGymId"] as? String
                )
            } catch (e: Exception) {
                android.util.Log.e("MapObject", "Greška pri parsiranju objekta: ${e.message}", e)
                null
            }
        }
    }
}

enum class ObjectType {
    GYM,                    // Teretana - glavna lokacija (samo ova se dodaje direktno na mapu)
    EQUIPMENT,              // Sprava u teretani (dodaje se unutar teretane)
    FREE_EQUIPMENT,         // Slobodna sprava (dodaje se unutar teretane)
    CROWDED_AREA,           // Gužva u sali (dodaje se unutar teretane)
    TRAINER_RECOMMENDATION, // Preporuka trenera (dodaje se unutar teretane)
    EVENT                   // Fitnes događaj (dodaje se unutar teretane)
}
