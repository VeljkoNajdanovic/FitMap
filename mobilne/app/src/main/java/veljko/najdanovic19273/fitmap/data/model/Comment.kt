package veljko.najdanovic19273.fitmap.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId
    val id: String = "",
    val objectId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val text: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
) {
    // No-arg constructor za Firestore deserijalizaciju
    constructor() : this(
        id = "",
        objectId = "",
        authorId = "",
        authorName = "",
        authorImageUrl = "",
        text = "",
        createdAt = null
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "objectId" to objectId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorImageUrl" to authorImageUrl,
            "text" to text,
            "createdAt" to createdAt
        )
    }
}
