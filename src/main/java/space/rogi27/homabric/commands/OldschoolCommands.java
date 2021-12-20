package space.rogi27.homabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.config.HomabricConfig;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OldschoolCommands {
    public static void init() {
        if(HomabricConfig.Config.isOldschoolCommandsEnabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
                dispatcher.register(literal("sethome")
                        .requires(Permissions.require("homabric.base.set", true))
                        .then(argument("home", StringArgumentType.word())
                                .executes((BaseCommands::set)))
                        .executes(BaseCommands::set));
                dispatcher.register(literal("removehome")
                        .requires(Permissions.require("homabric.base.remove", true))
                        .then(argument("home", StringArgumentType.word())
                                .suggests(BaseCommands::suggestPlayerHomes)
                                .executes((BaseCommands::remove))
                        ));
                dispatcher.register(literal("playerhome")
                        .requires(Permissions.require("homabric.base.others", true))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("home", StringArgumentType.word())
                                        .suggests(BaseCommands::suggestAllowedHomes)
                                        .executes((BaseCommands::teleportToAllowed))
                                )
                        ));
                dispatcher.register(literal("listhome")
                        .requires(Permissions.require("homabric.base.list", true))
                        .executes((BaseCommands::list)));
                dispatcher.register(literal("allowhome")
                        .requires(Permissions.require("homabric.base.allow", true))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("home", StringArgumentType.word())
                                        .suggests(BaseCommands::suggestPlayerHomes)
                                        .executes((BaseCommands::allowHome))
                                )
                        ));
                dispatcher.register(literal("disallowhome")
                        .requires(Permissions.require("homabric.base.disallow", true))
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("home", StringArgumentType.word())
                                        .suggests(BaseCommands::suggestPlayerHomes)
                                        .executes((BaseCommands::disallowHome))
                                )
                        ));
            });
            Homabric.LOGGER.info("Oldschool commands enabled!");
        }
    }
}
