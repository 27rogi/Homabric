package space.rogi27.homabric.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.helpers.Completables

object ClassicCommands {
    fun init() {
        if (HomabricConfig.areClassicCommandsEnabled()) {
            CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource?>, _, _ ->
                dispatcher.register(
                    CommandManager.literal("sethome").requires(Permissions.require("homabric.base.set", 0))
                        .then(CommandManager.argument("home", StringArgumentType.word())
                            .executes((BaseCommands::set))
                        )
                        .executes(BaseCommands::set)
                )
                dispatcher.register(
                    CommandManager.literal("removehome").requires(Permissions.require("homabric.base.remove", 0)).then(
                        CommandManager.argument("home", StringArgumentType.word())
                                .suggests(Completables::suggestPlayerHomes)
                                .executes((BaseCommands::remove))
                    )
                )
                dispatcher.register(
                    CommandManager.literal("playerhome").requires(Permissions.require("homabric.base.others", 0)).then(
                        CommandManager.argument("player", EntityArgumentType.player()).then(
                            CommandManager.argument("home", StringArgumentType.word())
                                    .suggests(Completables::suggestAllowedHomes)
                                    .executes((BaseCommands::teleportToAllowed))
                        )
                    )
                )
                dispatcher.register(
                    CommandManager.literal("listhome").requires(Permissions.require("homabric.base.list", 0))
                            .executes((BaseCommands::list))
                )
                dispatcher.register(
                    CommandManager.literal("allowhome").requires(Permissions.require("homabric.base.allow", 0))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("home", StringArgumentType.word())
                                            .suggests(Completables::suggestPlayerHomes).executes((BaseCommands::allowHome))
                                    )
                            )
                )
                dispatcher.register(
                    CommandManager.literal("disallowhome").requires(Permissions.require("homabric.base.disallow", 0)).then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("home", StringArgumentType.word())
                                .suggests(Completables::suggestPlayerHomes).executes((BaseCommands::disallowHome))
                            )
                    )
                )
            })
            Homabric.logger.info("[Homabric:INFO] Classic commands enabled!")
        }
    }
}