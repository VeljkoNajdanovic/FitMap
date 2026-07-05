package veljko.najdanovic19273.fitmap.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val id: String = "",

    val username: String = "",
    val email: String = "",

    val firstName: String = "",
    val lastName: String = "",

    @get:PropertyName("phoneNumber")
    @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("profileImageUrl")
    @set:PropertyName("profileImageUrl")
    var profileImageUrl: String = "",

    val points: Int = 0,

    @ServerTimestamp
    val createdAt: Date? = null,

    val fcmToken: String = ""
) {
    // No-arg constructor za Firestore deserijalizaciju
    constructor() : this(
        id = "",
        username = "",
        email = "",
        firstName = "",
        lastName = "",
        phoneNumber = "",
        profileImageUrl = "",
        points = 0,
        createdAt = null,
        fcmToken = ""
    )

    // Computed properties koje Firebase treba da ignoriše
    @get:Exclude
    val fullName: String
        get() = "$firstName $lastName".trim()

    @get:Exclude
    val uid: String
        get() = id

    @get:Exclude
    val phone: String
        get() = phoneNumber

    @get:Exclude
    val imageUrl: String
        get() = profileImageUrl

    // Convert to map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "profileImageUrl" to profileImageUrl,
            "points" to points,
            "createdAt" to createdAt,
            "fcmToken" to fcmToken
        )
    }
}
