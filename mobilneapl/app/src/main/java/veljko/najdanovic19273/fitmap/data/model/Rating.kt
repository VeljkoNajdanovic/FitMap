package veljko.najdanovic19273.fitmap.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Rating(
    @DocumentId
    val id: String = "",
    val objectId: String = "",
    val authorId: String = "",
    val value: Float = 0f, // Ocena od 1 do 5
    @ServerTimestamp
    val createdAt: Date? = null
) {
    // No-arg constructor za Firestore deserijalizaciju
    constructor() : this(
        id = "",
        objectId = "",
        authorId = "",
        value = 0f,
        createdAt = null
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "objectId" to objectId,
            "authorId" to authorId,
            "value" to value,
            "createdAt" to createdAt
        )
    }
}
