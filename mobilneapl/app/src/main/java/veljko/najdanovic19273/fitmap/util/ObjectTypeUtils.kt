package veljko.najdanovic19273.fitmap.util

import veljko.najdanovic19273.fitmap.data.model.ObjectType

fun getObjectTypeName(type: ObjectType): String {
    return when (type) {
        ObjectType.GYM -> "🏋️ Teretana"
        ObjectType.EQUIPMENT -> "💪 Sprava u teretani"
        ObjectType.FREE_EQUIPMENT -> "✅ Slobodna sprava"
        ObjectType.CROWDED_AREA -> "👥 Gužva u sali"
        ObjectType.TRAINER_RECOMMENDATION -> "🎯 Preporuka trenera"
        ObjectType.EVENT -> "📅 Fitnes događaj"
    }
}
