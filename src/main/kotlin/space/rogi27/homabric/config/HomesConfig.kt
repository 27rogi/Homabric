package space.rogi27.homabric.config

import me.lortseam.completeconfig.api.ConfigContainer
import me.lortseam.completeconfig.api.ConfigEntry
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.Homabric.Companion.saveAndReloadConfig
import space.rogi27.homabric.objects.PlayerObject

object HomesConfig:ConfigContainer {
    @ConfigEntry(comment = "List of players with their homes.")
    private var players: MutableMap<String, PlayerObject?> = HashMap()
    fun getPlayer(name: String): PlayerObject? {
        return players[name]
    }
    
    fun getPlayers(): Map<String, PlayerObject?> {
        return players
    }
    
    fun getOrCreatePlayer(name: String): PlayerObject {
        if (players[name] == null) {
            Homabric.logger.warn("There is no data for $name, creating new entry.")
            players[name] = PlayerObject().withData(java.util.Map.of())
            saveAndReloadConfig()
        }
        return players[name]!!
    }
}