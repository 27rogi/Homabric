package space.rogi27.homabric.objects

import me.lortseam.completeconfig.api.ConfigEntry
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class HomePermissionObject {
    @ConfigEntry(comment = "Sets the maximum amount of homes per player.")
    var maxHomes = 2
}