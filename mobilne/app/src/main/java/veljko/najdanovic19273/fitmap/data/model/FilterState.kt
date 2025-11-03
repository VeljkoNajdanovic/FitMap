package veljko.najdanovic19273.fitmap.data.model

/**
 * Drži stanje filtera za objekte na mapi
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class FilterState(
    val searchQuery: String = "",
    val selectedType: ObjectType? = null, // null = svi tipovi
    val radiusInMeters: Double? = null, // null = bez radius filtera
    val minRating: Float = 0f // 0 = sve ocene
) {
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun isActive(): Boolean {
        return searchQuery.isNotBlank() ||
               selectedType != null ||
               radiusInMeters != null ||
               minRating > 0f
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun reset(): FilterState {
        return FilterState()
    }
}

/**
 * Predefinisane opcije za radius pretragu
 */
@Suppress("unused")
enum class RadiusOption(val meters: Double, @Suppress("unused") val displayName: String) {
    RADIUS_100M(100.0, "100m"),
    RADIUS_500M(500.0, "500m"),
    RADIUS_1KM(1000.0, "1 km"),
    RADIUS_5KM(5000.0, "5 km"),
    RADIUS_10KM(10000.0, "10 km"),
    ALL(Double.MAX_VALUE, "Sve");

    companion object {
        @Suppress("unused", "MemberVisibilityCanBePrivate")
        fun fromMeters(meters: Double?): RadiusOption {
            if (meters == null) return ALL
            return entries.firstOrNull { it.meters == meters } ?: ALL
        }
    }
}
