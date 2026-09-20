package space.rogi27.homabric.polyfills

// later versions of Minecraft use values instead of ints
object PermissionLevel {
    const val ALL = 0
    const val MODERATORS = 1
    const val GAMEMASTERS = 2
    const val ADMINS = 3
    const val OWNERS = 4
}