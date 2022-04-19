package space.rogi27.homabric.config

import me.lortseam.completeconfig.api.ConfigContainer
import me.lortseam.completeconfig.api.ConfigEntry
import space.rogi27.homabric.objects.HomePermissionObject

object HomabricConfig:ConfigContainer {
    @ConfigEntry(
        comment = "Examples and help: https://github.com/rogi27/Homabric/blob/master/README.md#configuration" + "Do not touch this value, it allows mod to check if config file is outdated or not."
    )
    var configVersion = 3
    
    @ConfigEntry(comment = "This option enables alternative command variants like /sethome, /removehome and etc.")
    private var enableClassicCommands = true
    
    @ConfigEntry(comment = "Sets the maximum amount of homes per player.")
    private var homesLimit = 2
    
    @ConfigEntry(
        comment = "Sets the timeout in seconds before player will be teleported home." + "\nYou can disable this feature by setting it to 0."
    )
    private var teleportCooldown = 3
    
    @ConfigEntry(
        comment = "You can define permissions that will override home limit for the players if they have them." + "\nPermission names are transformed to permissions like 'homabric.homelimit.<permissionName>'" + "\nExample permission: vip: { max-homes=6 }"
    )
    var permissionsHomeLimit: Map<String, HomePermissionObject> = HashMap()
    
    fun teleportCooldown(): Int {
        return teleportCooldown
    }
    
    fun areClassicCommandsEnabled(): Boolean {
        return enableClassicCommands
    }
    
    fun homesLimit(): Int {
        return homesLimit
    }
}