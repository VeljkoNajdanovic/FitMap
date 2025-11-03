package veljko.najdanovic19273.fitmap.ui.navigation

object Screen {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val MAP = "map"
    const val ADD_OBJECT = "add_object"
    const val OBJECT_DETAILS = "object_details/{objectId}"
    const val LEADERBOARD = "leaderboard"

    fun objectDetails(objectId: String) = "object_details/$objectId"
}
