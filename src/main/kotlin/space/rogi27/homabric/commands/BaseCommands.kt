package space.rogi27.homabric.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
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
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import space.rogi27.homabric.config.ConfigManager
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.config.HomesConfig
import space.rogi27.homabric.helpers.CommandUtils.getHomeArg
import space.rogi27.homabric.helpers.CommandUtils.getPlayerObject
import space.rogi27.homabric.helpers.Completables.suggestAllowedHomes
import space.rogi27.homabric.helpers.Completables.suggestOnlinePlayerStrings
import space.rogi27.homabric.helpers.Completables.suggestPlayerHomes
import space.rogi27.homabric.objects.HomeObject
import space.rogi27.homabric.objects.PlayerObject
import space.rogi27.homabric.polyfills.PermissionLevel

object BaseCommands: RegistrableCommand {
    override fun register(): Boolean {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            HomabricConfig.entries.commandHomeAliases.forEach { alias -> dispatcher.register(registerTeleport(alias)) }
            HomabricConfig.entries.commandHomesAliases.forEach { alias -> dispatcher.register(registerManagement(alias)) }
        })
        return true
    }

    fun registerManagement(name: String): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal(name)
            .requires(Permissions.require("homabric.base.use", PermissionLevel.ALL))
            .executes { context -> list(context) }
            .then(
                Commands.literal("set")
                    .requires(Permissions.require("homabric.base.set", PermissionLevel.ALL))
                    .executes { context -> set(context) }
                    .then(
                        Commands.argument("home", StringArgumentType.word())
                            .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                            .executes { context -> set(context) }
                    )
            )
            .then(
                Commands.literal("remove")
                    .requires(Permissions.require("homabric.base.remove", PermissionLevel.ALL))
                    .then(
                        Commands.argument("home", StringArgumentType.word())
                            .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                            .executes { context -> remove(context) }
                    )
            )
            .then(
                Commands.literal("allow")
                    .requires(Permissions.require("homabric.base.allow", PermissionLevel.ALL))
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .suggests { context, suggestionsBuilder -> suggestOnlinePlayerStrings(context, suggestionsBuilder) }
                            .then(
                                Commands.argument("home", StringArgumentType.word())
                                    .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                                    .executes { context -> allow(context) }
                            )
                    )
            )
            .then(
                Commands.literal("disallow")
                    .requires(Permissions.require("homabric.base.disallow", PermissionLevel.ALL))
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                            .suggests { context, suggestionsBuilder -> suggestOnlinePlayerStrings(context, suggestionsBuilder) }
                            .then(
                                Commands.argument("home", StringArgumentType.word())
                                    .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                                    .executes { context -> disallow(context) }
                            )
                    )
            )
            .then(
                Commands.literal("setIcon")
                    .requires(Permissions.require("homabric.base.setIcon", PermissionLevel.ALL))
                    .then(
                        Commands.argument("home", StringArgumentType.word())
                            .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                            .then(
                                Commands.argument("item", ResourceLocationArgument.id())
                                    .executes { context -> setIcon(context) }
                            )
                    )
            )
            .then(
                Commands.literal("p")
                    .requires(Permissions.require("homabric.base.others", PermissionLevel.ALL))
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                            .suggests { context, suggestionsBuilder -> suggestOnlinePlayerStrings(context, suggestionsBuilder) }
                            .then(
                                Commands.argument("home", StringArgumentType.word())
                                    .suggests { context, suggestionsBuilder -> suggestAllowedHomes(context, suggestionsBuilder) }
                                    .executes { context -> teleportToAllowed(context) }
                            )
                    )
            )
    }

    fun registerTeleport(name: String): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal(name)
            .requires(Permissions.require("homabric.base.use", PermissionLevel.ALL))
            .executes { context -> teleport(context, false) }
            .then(
                Commands.argument("home", StringArgumentType.word())
                    .requires(Permissions.require("homabric.base.byName", PermissionLevel.ALL))
                    .suggests { context, suggestionsBuilder -> suggestPlayerHomes(context, suggestionsBuilder) }
                    .executes { context -> teleport(context, true) }
            )
    }

    @Throws(CommandSyntaxException::class)
    fun setIcon(context: CommandContext<CommandSourceStack>): Int {
        val homeName = context.getHomeArg()
        val player = context.getPlayerObject() ?: return 0
        val home: HomeObject? = player.getHome(homeName)
        if (home == null) {
            context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            return 1
        }
        when (home.setIcon(context.getArgument("item", ResourceLocation::class.java))) {
            HomeObject.IconResult.WRONG_ICON -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_icon"))
            }
            HomeObject.IconResult.ICON_SET -> {
                context.source.sendSystemMessage(
                    Component.translatable(
                        "text.homabric.icon_changed", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), ItemStack(BuiltInRegistries.ITEM.get(context.getArgument(
                            "item", ResourceLocation::class.java
                        ))).displayName.copy().withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                )
            }
        }
        HomesConfig.save()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun teleport(context: CommandContext<CommandSourceStack>, hasName: Boolean): Int {
        val player = context.getPlayerObject() ?: return 0
        val homeName = context.getHomeArg()
        val result = player.teleportToHome(context.source, homeName)
        when (result) {
            PlayerObject.TeleportResult.NO_PLAYER -> {
                context.source.sendSystemMessage(
                    Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED)
                )
            }
            PlayerObject.TeleportResult.NO_HOME -> {
                context.source.sendSystemMessage(Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED))
            }
            PlayerObject.TeleportResult.TELEPORT_DONE -> {}
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun teleportToAllowed(context: CommandContext<CommandSourceStack>): Int {
        val owner = context.getArgument("player", String::class.java)
        val homeName = context.getHomeArg()
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
            PlayerObject.TeleportToOtherResult.TELEPORT_DONE -> {}
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun set(context: CommandContext<CommandSourceStack>): Int {
        val player = context.getPlayerObject() ?: return 0
        val homeName = context.getHomeArg()
        if (player.checkHomeLimit(context.source)) {
            context.source.sendSystemMessage(
                Component.translatable("text.homabric.home_limit_reached").withStyle(ChatFormatting.RED)
            )
            return 1
        }
        val result: PlayerObject.HomeCreationResult = player.createOrUpdateHome(context.source, homeName)
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
        val player = context.getPlayerObject() ?: return 0
        val homeName = context.getArgument("home", String::class.java)
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
        val player = context.getPlayerObject() ?: return 0
        val gui: SimpleGui = player.getHomesGUI(context.source)
        gui.open()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun allow(context: CommandContext<CommandSourceStack>): Int {
        val player = context.getPlayerObject() ?: return 0
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
    fun disallow(context: CommandContext<CommandSourceStack>): Int {
        val owner = context.getPlayerObject() ?: return 0
        val homeName = context.getArgument("home", String::class.java)
        val player = context.getArgument("player", String::class.java)
        when (owner.disallowHome(homeName, player)) {
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
                        ), Component.literal(player).withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                )
            }
        }
        return 1
    }
}