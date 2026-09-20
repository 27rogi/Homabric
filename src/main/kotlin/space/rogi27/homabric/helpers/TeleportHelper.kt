package space.rogi27.homabric.helpers

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.Component.translatable
import net.minecraft.server.level.ServerPlayer
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.polyfills.PermissionLevel
import java.util.*

object TeleportHelper {
    private val teleportingPlayers = HashMap<String, Int>()
    
    fun isTeleporting(player: ServerPlayer): Boolean {
        return teleportingPlayers.contains(player.stringUUID)
    }
    
    fun stopTeleport(timer: Timer, player: ServerPlayer) {
        timer.cancel()
        teleportingPlayers.remove(player.stringUUID)
    }
    
    fun runTeleport(player: ServerPlayer, onFinish: () -> Unit) {
        // bypass cooldown for players with permission OR if it was disabled in config
        if ((Permissions.check(player, "homabric.teleport.bypass", PermissionLevel.GAMEMASTERS) || (HomabricConfig.entries.teleportCooldown == 0))) {
            return onFinish()
        }
        
        val firstPos = player.position()
        val firstHealth = player.health
        val timer = Timer()
        
        // be sure to have player only once to prevent bugs
        if (teleportingPlayers[player.stringUUID] != null) {
            return player.sendSystemMessage(translatable("text.homabric.teleport_already_in").withStyle(ChatFormatting.YELLOW), false)
        }
        teleportingPlayers[player.stringUUID] = HomabricConfig.entries.teleportCooldown
        
        timer.scheduleAtFixedRate(object:TimerTask() {
            override fun run() {
                if (player.hasDisconnected()) {
                    stopTeleport(timer, player)
                    return Homabric.logger.info("Player ${player.gameProfile.name} disconnected before teleportation.")
                }
                
                if ((firstPos != player.position()) || (firstHealth > player.health)) {
                    stopTeleport(timer, player)
                    return player.sendSystemMessage(translatable("text.homabric.teleport_canceled").withStyle(ChatFormatting.RED), false)
                }
                
                teleportingPlayers[player.stringUUID] = teleportingPlayers[player.stringUUID]!!.minus(1)
                
                if (teleportingPlayers[player.stringUUID]!! < 0) {
                    stopTeleport(timer, player)
                    return onFinish()
                }
                
                // adding 1 because timer starts from 0 which is not right from player's perspective
                player.sendSystemMessage(
                    translatable(
                        "text.homabric.teleport_in_progress", literal((teleportingPlayers[player.stringUUID]!! + 1).toString()).withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.YELLOW), true
                )
            }
        }, 0, 1000)
    }
}
