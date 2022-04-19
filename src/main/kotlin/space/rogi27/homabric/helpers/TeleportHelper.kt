package space.rogi27.homabric.helpers

import com.sun.jna.Memory.purge
import kotlinx.coroutines.NonCancellable.cancel
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import java.util.*

object TeleportHelper {
    private val teleportingPlayers = HashMap<String, Int>()

    fun isTeleporting(player: ServerPlayerEntity): Boolean {
        return teleportingPlayers.contains(player.uuidAsString)
    }

    fun stopTeleport(timer: Timer, player: ServerPlayerEntity) {
        timer.cancel()
        teleportingPlayers.remove(player.uuidAsString)
    }

    fun runTeleport(player: ServerPlayerEntity, onFinish: () -> Unit) {
        // bypass cooldown for players with permission OR if it was disabled in config
        if((Permissions.check(player, "homabric.teleport.bypass", false) || (HomabricConfig.teleportCooldown() == 0))) {
            return onFinish()
        }

        val firstPos = player.pos
        val firstHealth = player.health
        val timer = Timer()

        // be sure to have player only once to prevent bugs
        if(teleportingPlayers[player.uuidAsString] != null) {
            return player.sendMessage(TranslatableText("text.homabric.teleport_already_in").formatted(Formatting.YELLOW), false)
        }
        teleportingPlayers[player.uuidAsString] = HomabricConfig.teleportCooldown()

        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                if(player.isDisconnected) {
                    stopTeleport(timer, player)
                    return Homabric.logger.info("Player ${player.entityName} disconnected before teleportation.")
                }

                if ((firstPos != player.pos) || (firstHealth > player.health)) {
                    stopTeleport(timer, player)
                    return player.sendMessage(TranslatableText("text.homabric.teleport_canceled").formatted(Formatting.RED), false)
                }

                teleportingPlayers[player.uuidAsString] = teleportingPlayers[player.uuidAsString]!!.minus(1)

                if (teleportingPlayers[player.uuidAsString]!! < 0) {
                    stopTeleport(timer, player)
                    return onFinish()
                }

                // adding 1 because timer starts from 0 which is not right from player's perspective
                player.sendMessage(
                    TranslatableText("text.homabric.teleport_in_progress",
                        LiteralText((teleportingPlayers[player.uuidAsString]!! + 1).toString()).formatted(Formatting.AQUA)
                    ).formatted(Formatting.YELLOW), true
                )
            }
        }, 0, 1000)
    }
}
