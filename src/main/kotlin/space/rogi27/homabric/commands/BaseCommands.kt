package space.rogi27.homabric.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.EntitySelector
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomesConfig.getOrCreatePlayer
import space.rogi27.homabric.helpers.Completables.suggestAllowedHomes
import space.rogi27.homabric.helpers.Completables.suggestOnlinePlayerStrings
import space.rogi27.homabric.helpers.Completables.suggestPlayerHomes
import space.rogi27.homabric.helpers.TeleportHelper
import space.rogi27.homabric.objects.HomeObject
import space.rogi27.homabric.objects.PlayerObject

object BaseCommands {
    fun init() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(registerBaseCommands("home"))
            dispatcher.register(registerBaseCommands("h"))
        })
    }
    
    private fun registerBaseCommands(name: String?): LiteralArgumentBuilder<ServerCommandSource?>? {
        return CommandManager.literal(name).requires(Permissions.require("homabric.base.use", 0)).then(CommandManager.literal("set").requires(Permissions.require("homabric.base.set", 0))
                        .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                .executes(Command { context: CommandContext<ServerCommandSource> -> set(context) })
                        ).executes { context: CommandContext<ServerCommandSource> -> set(context) }).then(CommandManager.literal("remove").requires(Permissions.require("homabric.base.remove", 0))
                        .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                .executes((Command { context: CommandContext<ServerCommandSource> -> remove(context) }))
                        )
                ).then(
                    CommandManager.literal("list").requires(Permissions.require("homabric.base.list", 0)).executes((Command { context: CommandContext<ServerCommandSource> -> list(context) }))
                ).then(CommandManager.literal("allow").requires(Permissions.require("homabric.base.allow", 0))
                        .then(CommandManager.argument("player", EntityArgumentType.player()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<ServerCommandSource> -> allowHome(context) }))
                                )
                        )
                ).then(CommandManager.literal("disallow").requires(Permissions.require("homabric.base.disallow", 0))
                        .then(CommandManager.argument("player", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<ServerCommandSource> -> disallowHome(context) }))
                                )
                        )
                ).then(CommandManager.literal("p").requires(Permissions.require("homabric.base.others", 0))
                        .then(CommandManager.argument("player", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestAllowedHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<ServerCommandSource> -> teleportToAllowed(context) }))
                                )
                        )
                ).then(CommandManager.literal("setIcon").requires(Permissions.require("homabric.base.setIcon", 0))
                        .then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }.then(
                                    CommandManager.argument("item", IdentifierArgumentType.identifier()).executes((Command { context: CommandContext<ServerCommandSource> -> setIcon(context) }))
                                )
                        )
                ).then(CommandManager.argument("home", StringArgumentType.word()).suggests { context: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }.requires(Permissions.require("homabric.base.byName", 0))
                        .executes((Command { context: CommandContext<ServerCommandSource> -> teleport(context, true) }))
                ).executes((Command { context: CommandContext<ServerCommandSource> -> teleport(context, false) }))
    }
    
    @Throws(CommandSyntaxException::class)
    fun setIcon(context: CommandContext<ServerCommandSource>): Int {
        val homeName = context.getArgument("home", String::class.java)
        val home: HomeObject? = getOrCreatePlayer(context.source.name).getHome(homeName)
        if (home == null) {
            context.source.sendFeedback(Text.translatable("text.homabric.no_home").formatted(Formatting.RED), false)
            return 1
        }
        when (home.setIcon(context.getArgument("item", Identifier::class.java))) {
            HomeObject.IconResult.WRONG_ICON -> {
                context.source.sendFeedback(Text.translatable("text.homabric.no_icon"), false)
            }
            HomeObject.IconResult.ICON_SET -> {
                context.source.sendFeedback(
                    Text.translatable(
                        "text.homabric.icon_changed", Text.literal(homeName).formatted(
                            Formatting.WHITE
                        ), Registry.ITEM[context.getArgument(
                            "item", Identifier::class.java
                        )].name.copy().formatted(Formatting.AQUA)
                    ).formatted(Formatting.GREEN), false
                )
            }
        }
        Homabric.saveAndReloadConfig()
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun teleport(context: CommandContext<ServerCommandSource>, hasName: Boolean): Int {
        var homeName = "home"
        if (hasName) {
            if (context.getArgument("home", String::class.java).isNotEmpty()) homeName = context.getArgument("home", String::class.java)
        }
        val player: PlayerObject = getOrCreatePlayer(context.source.name)
        val home: HomeObject? = player.getHome(homeName)
        
        if (home == null) {
            context.source.sendFeedback(Text.translatable("text.homabric.no_home"), false)
            return 0
        }
        
        if (context.source.player == null) return 0
        TeleportHelper.runTeleport(context.source.player!!, fun() {
            home.teleportPlayer(context.source.player!!)
            context.source.sendFeedback(
                Text.translatable(
                    "text.homabric.teleport_done", Text.literal(homeName).formatted(Formatting.WHITE)
                ).formatted(Formatting.GREEN), false
            )
        })
        
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun teleportToAllowed(context: CommandContext<ServerCommandSource>): Int {
        val owner = context.getArgument("player", String::class.java)
        val homeName = context.getArgument("home", String::class.java)
        val result: PlayerObject.TeleportToOtherResult = PlayerObject.teleportToOtherHome(context.source, owner, homeName, false)
        when (result) {
            PlayerObject.TeleportToOtherResult.NO_PLAYER -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.no_player_exists").formatted(Formatting.RED), false
                )
            }
            PlayerObject.TeleportToOtherResult.NO_HOME -> {
                context.source.sendFeedback(Text.translatable("text.homabric.no_home").formatted(Formatting.RED), false)
            }
            PlayerObject.TeleportToOtherResult.NO_ACCESS -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.no_home_access").formatted(Formatting.RED), false
                )
            }
            PlayerObject.TeleportToOtherResult.TELEPORT_DONE -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.teleport_done").formatted(Formatting.RED), false
                )
            }
        }
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun set(context: CommandContext<ServerCommandSource>): Int {
        if (!context.source.entity!!.isPlayer) {
            return 0
        }
        var homeName: String? = "home"
        try {
            if (context.getArgument("home", String::class.java) != null) homeName = context.getArgument("home", String::class.java)
        } catch (ex: Exception) {
            Homabric.logger.warn("Using command without name, referencing home")
        }
        val player: PlayerObject? = getOrCreatePlayer(context.source.name)
        if (player != null) {
            if (player.isLimitReached(context.source)) {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.home_limit_reached").formatted(Formatting.RED), false
                )
                return 1
            }
        }
        val result: PlayerObject.HomeCreationResult? = homeName?.let { player?.createOrUpdateHome(context.source, it) }
        if (result === PlayerObject.HomeCreationResult.HOME_CREATED) {
            context.source.sendFeedback(
                Text.translatable(
                    "text.homabric.new_home_created", Text.literal(homeName).formatted(
                        Formatting.WHITE
                    )
                ).formatted(Formatting.GREEN), false
            )
        } else {
            context.source.sendFeedback(
                Text.translatable(
                    "text.homabric.home_location_updated", Text.literal(homeName).formatted(
                        Formatting.WHITE
                    )
                ).formatted(Formatting.GREEN), false
            )
        }
        Homabric.saveAndReloadConfig()
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun remove(context: CommandContext<ServerCommandSource>): Int {
        if (!context.source.entity!!.isPlayer) {
            return 0
        }
        val homeName = context.getArgument("home", String::class.java)
        when (getOrCreatePlayer(context.source.name).removeHome(homeName)) {
            PlayerObject.HomeRemoveResult.NO_HOME -> {
                context.source.sendFeedback(Text.translatable("text.homabric.no_home").formatted(Formatting.RED), false)
            }
            PlayerObject.HomeRemoveResult.HOME_REMOVED -> {
                context.source.sendFeedback(
                    Text.translatable(
                        "text.homabric.home_removed", Text.literal(homeName).formatted(
                            Formatting.WHITE
                        )
                    ).formatted(Formatting.GREEN), false
                )
            }
        }
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun list(context: CommandContext<ServerCommandSource>): Int {
        val playerData: PlayerObject = getOrCreatePlayer(context.source.name)
        val gui: SimpleGui = playerData.getHomesGUI(context.source)
        gui.open()
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun allowHome(context: CommandContext<ServerCommandSource>): Int {
        val homeName = context.getArgument("home", String::class.java)
        val allowedPlayer = context.getArgument("player", EntitySelector::class.java).getPlayer(context.source)
        when (getOrCreatePlayer(context.source.name).allowHome(homeName, allowedPlayer)) {
            PlayerObject.HomeAllowResult.NO_PLAYER -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.no_player_exists").formatted(Formatting.RED), false
                )
            }
            PlayerObject.HomeAllowResult.NO_SELF_ALLOW -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.allow_self").formatted(Formatting.BLUE), false
                )
            }
            PlayerObject.HomeAllowResult.NO_HOME -> {
                context.source.sendFeedback(Text.translatable("text.homabric.no_home").formatted(Formatting.RED), false)
            }
            PlayerObject.HomeAllowResult.ALREADY_ALLOWED -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.already_allowed").formatted(Formatting.RED), false
                )
            }
            PlayerObject.HomeAllowResult.HOME_ALLOWED -> {
                context.source.sendFeedback(
                    Text.translatable(
                        "text.homabric.allowed", Text.literal(homeName).formatted(
                            Formatting.WHITE
                        ), Text.literal(allowedPlayer.entityName).formatted(Formatting.AQUA)
                    ).formatted(
                        Formatting.GREEN
                    ), false
                )
            }
        }
        return 1
    }
    
    @Throws(CommandSyntaxException::class)
    fun disallowHome(context: CommandContext<ServerCommandSource>): Int {
        val homeName = context.getArgument("home", String::class.java)
        val disallowedPlayer = context.getArgument("player", String::class.java)
        when (getOrCreatePlayer(context.source.name).disallowHome(homeName, disallowedPlayer)) {
            PlayerObject.HomeDisallowResult.NO_PLAYER -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.no_player_exists").formatted(Formatting.RED), false
                )
            }
            PlayerObject.HomeDisallowResult.NO_HOME -> {
                context.source.sendFeedback(Text.translatable("text.homabric.no_home").formatted(Formatting.RED), false)
            }
            PlayerObject.HomeDisallowResult.NOT_ALLOWED -> {
                context.source.sendFeedback(
                    Text.translatable("text.homabric.no_player_disallow").formatted(Formatting.RED), false
                )
            }
            PlayerObject.HomeDisallowResult.HOME_ALLOWED -> {
                context.source.sendFeedback(
                    Text.translatable(
                        "text.homabric.disallowed", Text.literal(homeName).formatted(
                            Formatting.WHITE
                        ), Text.literal(disallowedPlayer).formatted(Formatting.AQUA)
                    ).formatted(Formatting.GREEN), false
                )
            }
        }
        return 1
    }
}