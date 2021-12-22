package space.rogi27.homabric.utils;

import me.lortseam.completeconfig.api.ConfigEntry;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class HomePermissionObject {
    @ConfigEntry(comment = "Sets the maximum amount of homes per player.")
    public int maxHomes = 2;
}
