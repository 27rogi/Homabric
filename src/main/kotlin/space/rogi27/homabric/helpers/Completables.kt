package space.rogi27.homabric.helpers

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import space.rogi27.homabric.config.HomesConfig
import space.rogi27.homabric.objects.PlayerObject
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

object Completables {
    @Suppress("UNUSED_PARAMETER")
    fun suggestPlayers(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        HomesConfig.getPlayers().forEach { (name: String?, _: PlayerObject?) ->
            builder.suggest(name)
        }
        return builder.buildFuture()
    }
    
    fun suggestPlayerHomes(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val player = HomesConfig.getPlayer(context.source.name)
        player?.homeNames?.forEach(builder::suggest)
        return builder.buildFuture()
    }
    
    fun suggestPlayerHomesForAdmin(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val player = HomesConfig.getPlayer(context.getArgument("player", String::class.java))
        player?.homeNames?.forEach(builder::suggest)
        return builder.buildFuture()
    }
    
    fun suggestOnlinePlayerStrings(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        context.source.server.playerManager.playerList.forEach(Consumer { player: ServerPlayerEntity ->
            builder.suggest(player.entityName)
        })
        return builder.buildFuture()
    }
    
    @Throws(CommandSyntaxException::class)
    fun suggestAllowedHomes(context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val owner = context.getArgument("player", String::class.java)
        if (owner != null && HomesConfig.getPlayer(owner) != null) {
            val allowedHomes = HomesConfig.getPlayer(owner)?.getAllowedHomeNames(context.source.entity!!.entityName)
            allowedHomes!!.forEach(Consumer { text: String? -> builder.suggest(text) })
        }
        return builder.buildFuture()
    }
}