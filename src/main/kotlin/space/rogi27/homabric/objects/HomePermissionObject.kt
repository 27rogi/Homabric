package space.rogi27.homabric.objects

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class HomePermissionObject {
    @Comment("Sets the maximum amount of homes per player.")
    var maxHomes = 2
}