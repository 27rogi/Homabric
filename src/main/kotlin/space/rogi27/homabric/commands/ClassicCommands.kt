package space.rogi27.homabric.commands

import com.mojang.brigadier.arguments.StringArgumentType
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.helpers.Completables

object ClassicCommands {
    fun init() {
        if (HomabricConfig.areClassicCommandsEnabled()) {
            CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
                dispatcher.register(
                    Commands.literal("sethome").requires(Permissions.require("homabric.base.set", 0)).then(
                                Commands.argument("home", StringArgumentType.word()).executes((BaseCommands::set))
                            ).executes(BaseCommands::set)
                )
                dispatcher.register(
                    Commands.literal("removehome").requires(Permissions.require("homabric.base.remove", 0)).then(
                        Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomes).executes((BaseCommands::remove))
                    )
                )
                dispatcher.register(
                    Commands.literal("playerhome").requires(Permissions.require("homabric.base.others", 0)).then(
                        Commands.argument("player", EntityArgument.player()).then(
                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestAllowedHomes).executes((BaseCommands::teleportToAllowed))
                        )
                    )
                )
                dispatcher.register(
                    Commands.literal("homes").requires(Permissions.require("homabric.base.list", 0)).executes((BaseCommands::list))
                )
                dispatcher.register(
                    Commands.literal("allowhome").requires(Permissions.require("homabric.base.allow", 0)).then(
                                Commands.argument("player", EntityArgument.player()).then(
                                            Commands.argument("home", StringArgumentType.word()).suggests(Completables::suggestPlayerHomes).executes((BaseCommands::allowHome))
                                        )
                            )
                )
                dispatcher.register(
                    Commands.literal("disallowhome").requires(Permissions.require("homabric.base.disallow", 0)).then(
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