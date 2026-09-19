package space.rogi27.homabric.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.item.ItemStack
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.ConfigManager
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

    private fun registerBaseCommands(name: String): LiteralArgumentBuilder<CommandSourceStack?>? {
        return Commands.literal(name).requires(Permissions.require("homabric.base.use", PermissionLevel.ALL)).then(Commands.literal("set").requires(Permissions.require("homabric.base.set", PermissionLevel.ALL))
                        .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                .executes(Command { context: CommandContext<CommandSourceStack> -> set(context) })
                        ).executes { context: CommandContext<CommandSourceStack> -> set(context) }).then(Commands.literal("remove").requires(Permissions.require("homabric.base.remove", PermissionLevel.ALL))
                        .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                .executes((Command { context: CommandContext<CommandSourceStack> -> remove(context) }))
                        )
                ).then(
                    Commands.literal("list").requires(Permissions.require("homabric.base.list", PermissionLevel.ALL)).executes((Command { context: CommandContext<CommandSourceStack> -> list(context) }))
                ).then(Commands.literal("allow").requires(Permissions.require("homabric.base.allow", PermissionLevel.ALL))
                        .then(Commands.argument("player",
                            EntityArgument.player()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<CommandSourceStack> -> allowHome(context) }))
                                )
                        )
                ).then(Commands.literal("disallow").requires(Permissions.require("homabric.base.disallow", PermissionLevel.ALL))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<CommandSourceStack> -> disallowHome(context) }))
                                )
                        )
                ).then(Commands.literal("p").requires(Permissions.require("homabric.base.others", PermissionLevel.ALL))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestOnlinePlayerStrings(context, (builder)!!) }
                                .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestAllowedHomes(context, (builder)!!) }
                                        .executes((Command { context: CommandContext<CommandSourceStack> -> teleportToAllowed(context) }))
                                )
                        )
                ).then(Commands.literal("setIcon").requires(Permissions.require("homabric.base.setIcon", PermissionLevel.ALL))
                        .then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }.then(
                                    Commands.argument("item",
                                        IdentifierArgument.id()).executes((Command { context: CommandContext<CommandSourceStack> -> setIcon(context) }))
                                )
                        )
                ).then(Commands.argument("home", StringArgumentType.word()).suggests { context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder? -> suggestPlayerHomes(context, (builder)!!) }.requires(Permissions.require("homabric.base.byName", PermissionLevel.ALL))
                        .executes((Command { context: CommandContext<CommandSourceStack> -> teleport(context, true) }))
                ).executes((Command { context: CommandContext<CommandSourceStack> -> teleport(context, false) }))
    }

    fun isPlayer(context: CommandContext<CommandSourceStack>): Boolean {
        return context.source.player?.gameProfile is GameProfile
    }

    @Throws(CommandSyntaxException::class)
    fun setIcon(context: CommandContext<CommandSourceStack>): Int {
        val homeName = context.getArgument("home", String::class.java)
        if (!isPlayer(context)) {
            Homabric.logger.error("Unable to get homes because context source is not a player!")
            return 0
        }
        val home: HomeObject? = getOrCreatePlayer(context.source.player!!.gameProfile.name).getHome(homeName)
        if (home == null) {
            context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            return 1
        }
        when (home.setIcon(context.getArgument("item", Identifier::class.java))) {
            HomeObject.IconResult.WRONG_ICON -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_icon"))
            }
            HomeObject.IconResult.ICON_SET -> {
                context.source.sendSystemMessage(
                    Component.translatable(
                        "text.homabric.icon_changed", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), ItemStack(BuiltInRegistries.ITEM[context.getArgument(
                            "item", Identifier::class.java
                        )].get().value()).itemName.copy().withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                )
            }
        }
        ConfigManager.saveAndLoadAll()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun teleport(context: CommandContext<CommandSourceStack>, hasName: Boolean): Int {
        var homeName = "home"
        if (hasName) {
            if (context.getArgument("home", String::class.java).isNotEmpty()) homeName = context.getArgument("home", String::class.java)
        }
        if (context.source.player === null) {
            Homabric.logger.error("Unable to get homes because context source is not a player!")
            return 0
        }
        val player: PlayerObject = getOrCreatePlayer(context.source.player!!.gameProfile.name)
        val home: HomeObject? = player.getHome(homeName)

        if (home == null) {
            context.source.sendSystemMessage(Component.translatable("text.homabric.no_home"))
            return 0
        }

        if (context.source.player == null) return 0
        TeleportHelper.runTeleport(context.source.player!!, fun() {
            home.teleportPlayer(context.source.player!!)
            context.source.sendSystemMessage(
                Component.translatable(
                    "text.homabric.teleport_done", Component.literal(homeName).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.GREEN)
            )
        })

        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun teleportToAllowed(context: CommandContext<CommandSourceStack>): Int {
        val owner = context.getArgument("player", String::class.java)
        val homeName = context.getArgument("home", String::class.java)
        val result: PlayerObject.TeleportToOtherResult = PlayerObject.teleportToOtherHome(context.source, owner, homeName, false)
        when (result) {
            PlayerObject.TeleportToOtherResult.NO_PLAYER -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.TeleportToOtherResult.NO_HOME -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            }
            PlayerObject.TeleportToOtherResult.NO_ACCESS -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_home_access").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.TeleportToOtherResult.TELEPORT_DONE -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.teleport_done").withStyle(ChatFormatting.RED)
                )
            }
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun set(context: CommandContext<CommandSourceStack>): Int {
        if (context.source.player === null) {
            return 0
        }
        val homeName = try {
            context.getArgument("home", String::class.java)
        } catch (_: Exception) {
            "home"
        }
        val player: PlayerObject? = getOrCreatePlayer(context.source.player)
        if (player != null) {
            if (player.checkHomeLimit(context.source)) {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.home_limit_reached").withStyle(ChatFormatting.RED)
                )
                return 1
            }
        }
        val result: PlayerObject.HomeCreationResult = player!!.createOrUpdateHome(context.source, homeName)
        if (result === PlayerObject.HomeCreationResult.HOME_CREATED) {
            context.source.sendSystemMessage(
                Component.translatable(
                    "text.homabric.new_home_created", Component.literal(homeName).withStyle(
                        ChatFormatting.WHITE
                    )
                ).withStyle(ChatFormatting.GREEN)
            )
        } else {
            context.source.sendSystemMessage(
                Component.translatable(
                    "text.homabric.home_location_updated", Component.literal(homeName).withStyle(
                        ChatFormatting.WHITE
                    )
                ).withStyle(ChatFormatting.GREEN)
            )
        }
        ConfigManager.saveAndLoadAll()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun remove(context: CommandContext<CommandSourceStack>): Int {
        if (context.source.player === null) {
            return 0
        }
        val homeName = context.getArgument("home", String::class.java)
        val player = getOrCreatePlayer(context.source.player)
        if (player === null) {
            Homabric.logger.error("Player not found")
            return 0
        }
        when (player.removeHome(homeName)) {
            PlayerObject.HomeRemoveResult.NO_HOME -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            }
            PlayerObject.HomeRemoveResult.HOME_REMOVED -> {
                context.source.sendSystemMessage(
                    Component.translatable(
                        "text.homabric.home_removed", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        )
                    ).withStyle(ChatFormatting.GREEN)
                )
            }
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun list(context: CommandContext<CommandSourceStack>): Int {
        val player: PlayerObject? = getOrCreatePlayer(context.source.player)
        if (player === null) {
            Homabric.logger.error("Player not found")
            return 0
        }
        val gui: SimpleGui = player.getHomesGUI(context.source)
        gui.open()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun allowHome(context: CommandContext<CommandSourceStack>): Int {
        val player: PlayerObject? = getOrCreatePlayer(context.source.player)
        if (player === null) {
            Homabric.logger.error("Player not found")
            return 0
        }
        val homeName = context.getArgument("home", String::class.java)
        val allowedPlayer = EntityArgument.getPlayer(context, "player")
        when (player.allowHome(homeName, allowedPlayer)) {
            PlayerObject.HomeAllowResult.NO_PLAYER -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.HomeAllowResult.NO_SELF_ALLOW -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.allow_self").withStyle(ChatFormatting.BLUE)
                )
            }
            PlayerObject.HomeAllowResult.NO_HOME -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            }
            PlayerObject.HomeAllowResult.ALREADY_ALLOWED -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.already_allowed").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.HomeAllowResult.HOME_ALLOWED -> {
                context.source.sendSystemMessage(
                    Component.translatable(
                        "text.homabric.allowed", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), Component.literal(allowedPlayer.gameProfile.name).withStyle(ChatFormatting.AQUA)
                    ).withStyle(
                        ChatFormatting.GREEN
                    )
                )
            }
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun disallowHome(context: CommandContext<CommandSourceStack>): Int {
        val player: PlayerObject? = getOrCreatePlayer(context.source.player)
        if (player === null) {
            Homabric.logger.error("Player not found")
            return 0
        }
        val homeName = context.getArgument("home", String::class.java)
        val disallowedPlayer = context.getArgument("player", String::class.java)
        when (player.disallowHome(homeName, disallowedPlayer)) {
            PlayerObject.HomeDisallowResult.NO_PLAYER -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.HomeDisallowResult.NO_HOME -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            }
            PlayerObject.HomeDisallowResult.NOT_ALLOWED -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_player_disallow").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.HomeDisallowResult.HOME_DISALLOWED -> {
                context.source.sendSystemMessage(
                    Component.translatable(
                        "text.homabric.disallowed", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), Component.literal(disallowedPlayer).withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                )
            }
        }
        return 1
    }
}