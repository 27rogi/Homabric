package space.rogi27.homabric.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting
import space.rogi27.homabric.objects.HomePermissionObject

object HomabricConfig : ConfigFile<HomabricConfig.Config>("config.conf", Config::class.java, ::Config) {

    @ConfigSerializable
    class Config {
        @Setting("config-version")
        @Comment(
            "Examples and help: https://github.com/rogi27/Homabric/blob/master/README.md#configuration\n" +
                    "Do not touch this value, it allows mod to check if config file is outdated or not."
        )
        var configVersion: Int = 3

        @Setting("enable-classic-commands")
        @Comment("This option enables alternative command variants like /sethome, /removehome and etc.")
        var classicCommandsEnabled: Boolean = true

        @Setting("home-command-aliases")
        @Comment("Allows you to specify aliases for home teleportation commands.")
        var commandHomeAliases: Array<String> = arrayOf("home", "h")

        @Setting("homes-command-aliases")
        @Comment("Allows you to specify aliases for home management commands.")
        var commandHomesAliases: Array<String> = arrayOf("homes", "hs")

        @Setting("homes-limit")
        @Comment("Sets the maximum amount of homes per player.")
        var homesLimit: Int = 2

        @Setting("teleport-cooldown")
        @Comment(
            "Sets the timeout in seconds before player will be teleported home.\n" +
                    "You can disable this feature by setting it to 0."
        )
        var teleportCooldown: Int = 3

        @Setting("permissions-home-limit")
        @Comment(
            "You can define permissions that will override home limit for the players if they have them.\n" +
                    "Permission names are transformed to permissions like 'homabric.homelimit.<permissionName>'\n" +
                    "Example permission: vip: { max-homes=6 }\n" +
                    "If you want to disable limit use 'homabric.limit.bypass' permission."
        )
        var permissionsHomeLimit: Map<String, HomePermissionObject> = emptyMap()
    }
}