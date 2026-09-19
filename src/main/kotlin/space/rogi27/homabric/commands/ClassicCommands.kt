package space.rogi27.homabric.commands

import com.mojang.brigadier.arguments.StringArgumentType
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.server.permissions.PermissionLevel
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.helpers.Completables

object ClassicCommands: RegistrableCommand {
    override fun register(): Boolean {
        if (HomabricConfig.entries.classicCommandsEnabled) {
            CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
                dispatcher.register(
                    Commands.literal("sethome").requires(Permissions.require("homabric.base.set", PermissionLevel.ALL)).then(
                                Commands.argument("home", StringArgumentType.word()).executes((BaseCommands::set))
                            ).executes(BaseCommands::set)
                )
                dispatcher.register(
                    Commands.literal("removehome").requires(Permissions.require("homabric.base.remove", PermissionLevel.ALL)).then(
                        Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomes).executes((BaseCommands::remove))
                    )
                )
                dispatcher.register(
                    Commands.literal("playerhome").requires(Permissions.require("homabric.base.others", PermissionLevel.ALL)).then(
                        Commands.argument("player", EntityArgument.player()).then(
                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestAllowedHomes).executes((BaseCommands::teleportToAllowed))
                        )
                    )
                )
                dispatcher.register(
                    Commands.literal("homes").requires(Permissions.require("homabric.base.list", PermissionLevel.ALL)).executes((BaseCommands::list))
                )
                dispatcher.register(
                    Commands.literal("allowhome").requires(Permissions.require("homabric.base.allow", PermissionLevel.ALL)).then(
                                Commands.argument("player", EntityArgument.player()).then(
                                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomes).executes((BaseCommands::allowHome))
                                        )
                            )
                )
                dispatcher.register(
                    Commands.literal("disallowhome").requires(Permissions.require("homabric.base.disallow", PermissionLevel.ALL)).then(
                        Commands.argument("player", EntityArgument.player()).then(
                                    Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomes).executes((BaseCommands::disallowHome))
                                )
                    )
                )
            })
            Homabric.logger.info("Classic commands enabled!")
        }
    }
}