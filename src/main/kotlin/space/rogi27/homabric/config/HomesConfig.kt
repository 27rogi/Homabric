package space.rogi27.homabric.config

import net.minecraft.server.level.ServerPlayer
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.objects.PlayerObject

object HomesConfig : ConfigFile<HomesConfig.Config>("homes.conf", Config::class.java, ::Config) {

    @ConfigSerializable
    class Config {
        @Comment("List of players with their homes.")
        @Setting("players")
        var players: MutableMap<String, PlayerObject?> = HashMap()
    }

    fun getPlayer(name: String): PlayerObject? {
        return entries.players[name]
    }
    
    fun getPlayers(): Map<String, PlayerObject?> {
        return entries.players
    }
    
    fun getOrCreatePlayer(name: String): PlayerObject {
        if (entries.players[name] == null) {
            Homabric.logger.warn("There is no data for $name, creating new entry.")
            entries.players[name] = PlayerObject().withData(java.util.Map.of())
            ConfigManager.saveAndLoadAll()
        }
        return entries.players[name]!!
    }

    fun getOrCreatePlayer(player: ServerPlayer?): PlayerObject? {
        if (player === null) {
            Homabric.logger.error("Unable to get homes because context source is not a player!")
            return null
        }
        return getOrCreatePlayer(player.gameProfile.name)
    }
}