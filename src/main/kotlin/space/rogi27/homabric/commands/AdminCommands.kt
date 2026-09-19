package space.rogi27.homabric.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.ConfigManager
import space.rogi27.homabric.config.HomesConfig
import space.rogi27.homabric.helpers.Completables
import space.rogi27.homabric.helpers.TeleportHelper
import space.rogi27.homabric.objects.HomeObject
import space.rogi27.homabric.objects.PlayerObject

object AdminCommands {
    fun init() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("homabric").requires(Permissions.require("homabric.admin.use", PermissionLevel.GAMEMASTERS)).then(
                    Commands.literal("reload").requires(Permissions.require("homabric.admin.reload", PermissionLevel.GAMEMASTERS)).executes(AdminCommands::reload)
                ).then(
                    Commands.literal("teleport").requires(Permissions.require("homabric.admin.teleport", PermissionLevel.GAMEMASTERS)).then(
                        Commands.argument("player", StringArgumentType.word()).suggests(Completables::suggestPlayers).then(
                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomesForAdmin).executes((AdminCommands::teleport))
                        )
                    )
                ).then(
                    Commands.literal("set").requires(Permissions.require("homabric.admin.set", PermissionLevel.GAMEMASTERS)).then(
                        Commands.argument("player", StringArgumentType.word()).suggests(Completables::suggestPlayers).then(
                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomesForAdmin).executes((AdminCommands::set))
                        )
                    )
                ).then(
                    Commands.literal("remove").requires(Permissions.require("homabric.admin.remove", PermissionLevel.GAMEMASTERS)).then(
                        Commands.argument("player", StringArgumentType.word()).suggests(Completables::suggestPlayers).then(
                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomesForAdmin).executes((AdminCommands::remove))
                        )
                    )
                ).then(
                    Commands.literal("list").requires(Permissions.require("homabric.admin.list", PermissionLevel.GAMEMASTERS)).then(
                        Commands.argument("player", StringArgumentType.word()).suggests(Completables::suggestPlayers).executes((AdminCommands::list))
                    )
                ).executes((AdminCommands::info))
            )
        })
    }

    @Throws(CommandSyntaxException::class)
    fun reload(context: CommandContext<CommandSourceStack>): Int {
        Homabric.config.load()
        Homabric.homesConfig.load()
        context.source.sendSuccess(
            { Component.translatable("text.homabric.admin_config_reloaded").withStyle(ChatFormatting.GRAY) }, false
        )
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun info(context: CommandContext<CommandSourceStack>): Int {
        val info = listOf(
            "%s by Rogi27", "%s", "%s", "%s"
        )
        context.source.sendSuccess(
            {
                Component.translatable(
                    java.lang.String.join("\n", info),
                    Component.literal("Homabric").withStyle(ChatFormatting.AQUA),
                    Component.translatable("text.homabric.admin_info_line1").withStyle(ChatFormatting.GRAY),
                    Component.literal(" - /help homabric").withStyle(ChatFormatting.GRAY),
                    Component.literal(" - /help home").withStyle(ChatFormatting.GRAY)
                ).withStyle(ChatFormatting.GREEN)
            }, false
        )
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun teleport(context: CommandContext<CommandSourceStack>): Int {
        val homeName = context.getArgument("home", String::class.java)
        val player = HomesConfig.getPlayer(context.getArgument("player", String::class.java))
        if (player == null) {
            context.source.sendSuccess(
                { Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED) }, false
            )
            return 1
        }

        val home: HomeObject? = player.getHome(homeName)
        if (home == null) {
            context.source.sendSuccess({ Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED) }, false)
            return 0
        }

        if (context.source.player == null) return 0
        TeleportHelper.runTeleport(context.source.player!!, fun() {
            home.teleportPlayer(context.source.player!!)
            context.source.sendSuccess(
                {
                    Component.translatable(
                        "text.homabric.teleport_done", Component.literal(homeName).withStyle(ChatFormatting.WHITE)
                    ).withStyle(ChatFormatting.GREEN)
                }, false
            )
        })
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun set(context: CommandContext<CommandSourceStack>): Int {
        if (context.source.entity!! !is ServerPlayer) return 0
        var homeName: String? = "home"
        try {
            if (context.getArgument("home", String::class.java) != null) homeName = context.getArgument("home", String::class.java)
        } catch (_: Exception) {
        }
        val player = HomesConfig.getPlayer(context.getArgument("player", String::class.java))
        if (player == null) {
            context.source.sendSuccess(
                { Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED) }, false
            )
            return 1
        }
        val result: PlayerObject.HomeCreationResult = player.createOrUpdateHome(context.source, homeName!!)
        if (result === PlayerObject.HomeCreationResult.HOME_CREATED) {
            context.source.sendSuccess(
                {
                    Component.translatable(
                        "text.homabric.admin_new_home_created", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), context.getArgument("player", String::class.java).format(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                }, false
            )
        } else {
            context.source.sendSuccess(
                {
                    Component.translatable(
                        "text.homabric.admin_home_location_updated", Component.literal(homeName).withStyle(
                            ChatFormatting.WHITE
                        ), context.getArgument("player", String::class.java).format(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.GREEN)
                }, false
            )
        }
        ConfigManager.saveAndLoadAll()
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun remove(context: CommandContext<CommandSourceStack>): Int {
        if (context.source.entity!! !is ServerPlayer) return 0
        val homeName = context.getArgument("home", String::class.java)
        val player = HomesConfig.getPlayer(
            context.getArgument(
                "player", String::class.java
            )
        )
        if (player == null) {
            context.source.sendSuccess(
                { Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.RED) }, false
            )
            return 1
        }
        val result: PlayerObject.HomeRemoveResult = player.removeHome(homeName)
        when (result) {
            PlayerObject.HomeRemoveResult.NO_HOME -> {
                context.source.sendSuccess({ Component.translatable("text.homabric.no_home").withStyle(ChatFormatting.RED) }, false)
            }
            PlayerObject.HomeRemoveResult.HOME_REMOVED -> {
                context.source.sendSuccess(
                    {
                        Component.translatable(
                            "text.homabric.admin_home_removed", Component.literal(homeName).withStyle(
                                ChatFormatting.WHITE
                            ), context.getArgument("player", String::class.java).format(ChatFormatting.AQUA)
                        ).withStyle(ChatFormatting.GREEN)
                    }, false
                )
            }
        }
        return 1
    }

    @Throws(CommandSyntaxException::class)
    fun list(context: CommandContext<CommandSourceStack>): Int {
        val playerData = HomesConfig.getPlayer(context.getArgument("player", String::class.java))
        if (playerData == null) {
            context.source.sendSystemMessage(Component.translatable("text.homabric.no_player_exists").withStyle(ChatFormatting.YELLOW))
            return 0
        }
        val gui = playerData.getHomesGUI(context.source)
        gui.open()
        return 1
    }
}