package space.rogi27.homabric.helpers

import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import space.rogi27.homabric.config.HomesConfig.getOrCreatePlayer
import space.rogi27.homabric.objects.PlayerObject

object CommandUtils {
    fun CommandContext<CommandSourceStack>.getPlayer(sendError: Boolean = true): ServerPlayer? {
        val player = this.source.player
        if (player == null) {
            this.source.sendSystemMessage(Component.translatable("text.homabric.no_player").withStyle(ChatFormatting.RED))
        }
        return player
    }

    fun CommandContext<CommandSourceStack>.getPlayerObject(sendError: Boolean = true): PlayerObject? {
        val serverPlayer = this.getPlayer(sendError) ?: return null
        return getOrCreatePlayer(serverPlayer)
    }

    fun CommandContext<CommandSourceStack>.getHomeArg(argName: String = "home"): String {
        return try {
            this.getArgument(argName, String::class.java)
        } catch (_: Exception) {
            "home"
        }
    }
}