package space.rogi27.homabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.config.HomabricConfig;
import space.rogi27.homabric.utils.HomeObject;
import space.rogi27.homabric.utils.PlayerObject;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BaseCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(bindHomeToolsTo("home"));
            dispatcher.register(bindHomeToolsTo("h"));
        });
    }

    public static LiteralArgumentBuilder<ServerCommandSource> bindHomeToolsTo(String name) {
        LiteralArgumentBuilder<ServerCommandSource> item = literal(name)
                .requires(Permissions.require("homabric.base.use", true))
                .then(literal("set")
                        .requires(Permissions.require("homabric.base.set", true))
                        .then(argument("home", StringArgumentType.word())
                                .suggests(BaseCommands::suggestPlayerHomes)
                                .executes((BaseCommands::set)))
                        .executes(BaseCommands::set))
                .then(literal("remove")
                        .requires(Permissions.require("homabric.base.remove", true))
                        .then(argument("home", StringArgumentType.word())
                                .suggests(BaseCommands::suggestPlayerHomes)
                                .executes((BaseCommands::remove))
                        )
                )
                .then(literal("list")
                        .requires(Permissions.require("homabric.base.list", true))
                        .executes((BaseCommands::list))
                )
                .then(literal("allow")
                        .requires(Permissions.require("homabric.base.allow", true))
                        .then(argument("player", EntityArgumentType.player())
                            .suggests(BaseCommands::suggestOnlinePlayerStrings)
                            .then(argument("home", StringArgumentType.word())
                                    .suggests(BaseCommands::suggestPlayerHomes)
                                    .executes((BaseCommands::allowHome))
                            )
                        )
                )
                .then(literal("disallow")
                        .requires(Permissions.require("homabric.base.disallow", true))
                        .then(argument("player", StringArgumentType.word())
                                .suggests(BaseCommands::suggestOnlinePlayerStrings)
                                .then(argument("home", StringArgumentType.word())
                                        .suggests(BaseCommands::suggestPlayerHomes)
                                        .executes((BaseCommands::disallowHome))
                                )
                        )
                )
                .then(literal("p")
                        .requires(Permissions.require("homabric.base.others", true))
                        .then(argument("player", StringArgumentType.word())
                            .suggests(BaseCommands::suggestOnlinePlayerStrings)
                            .then(argument("home", StringArgumentType.word())
                                        .suggests(BaseCommands::suggestAllowedHomes)
                                        .executes((BaseCommands::teleportToAllowed))
                            )
                        )
                )
                .then(literal("setIcon")
                        .requires(Permissions.require("homabric.base.setIcon", true))
                        .then(argument("home", StringArgumentType.word())
                                .suggests(BaseCommands::suggestPlayerHomes)
                                .then(argument("item", IdentifierArgumentType.identifier())
                                        .executes((BaseCommands::setIcon))
                                )
                        )
                )
                .then(argument("home", StringArgumentType.word())
                        .suggests(BaseCommands::suggestPlayerHomes)
                        .requires(Permissions.require("homabric.base.byName", true))
                        .executes((BaseCommands::teleportByName))
                )
                .executes((BaseCommands::teleport));
        return item;
    }

    public static CompletableFuture<Suggestions> suggestOnlinePlayerStrings(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach((player) -> {
            builder.suggest(player.getEntityName());
        });
        return builder.buildFuture();
    }


    public static CompletableFuture<Suggestions> suggestPlayerHomes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        PlayerObject player = HomabricConfig.Config.getPlayer(context.getSource().getName());
        if(player != null) player.getHomeNames().forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestAllowedHomes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String owner = context.getArgument("player", String.class);
        if(owner != null && HomabricConfig.Config.getPlayer(owner) != null) {
            ArrayList<String> allowedHomes = HomabricConfig.Config
                    .getPlayer(owner)
                    .getAllowedHomeNames(context.getSource().getEntity().getEntityName());

            allowedHomes.forEach(builder::suggest);
        }
        return builder.buildFuture();
    }

    public static int setIcon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String homeName = context.getArgument("home", String.class);
        HomeObject home = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName()).getHome(homeName);

        if(home == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home"), false);
            return 1;
        }

        HomeObject.IconResult result = home.setIcon(context.getArgument("item", Identifier.class));
        switch (result) {
            case WRONG_ICON -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_icon"), false);
            }
            case ICON_SET -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.icon_changed", new LiteralText(homeName).formatted(Formatting.WHITE), Registry.ITEM.get(context.getArgument("item", Identifier.class)).getName().shallowCopy().formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
            }
        }

        Homabric.reloadConfig();
        return 1;
    }

    public static int teleport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String homeName = "home";
        PlayerObject player = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName());
        PlayerObject.TeleportResult result = player.teleportToHome(context.getSource(), homeName);

        switch (result) {
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home"), false);
            }
            case TELEPORT_DONE -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.teleport_done", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
            }
        }
        return 1;
    }

    public static int teleportByName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            String homeName = context.getArgument("home", String.class);
            PlayerObject player = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName());
            PlayerObject.TeleportResult result = player.teleportToHome(context.getSource(), homeName);

            switch (result) {
                case NO_HOME -> {
                    context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home"), false);
                }
                case TELEPORT_DONE -> {
                    context.getSource().sendFeedback(new TranslatableText("text.homabric.teleport_done", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
                }
            }
            return 1;
    }

    public static int teleportToAllowed(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String owner = context.getArgument("player", String.class);
        String homeName = context.getArgument("home", String.class);

        PlayerObject.TeleportToOtherResult result = PlayerObject.teleportToOtherHome(context.getSource(), owner, homeName, false);

        switch (result) {
            case NO_PLAYER -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            }
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home"), false);
            }
            case NO_ACCESS -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home_access").formatted(Formatting.RED), false);
            }
            case TELEPORT_DONE -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.teleport_done", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
            }
        }
        return 1;
    }

    public static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(!context.getSource().getEntity().isPlayer()) {
            return 0;
        }

        String homeName = "home";
        try {
            if(context.getArgument("home", String.class) != null) homeName = context.getArgument("home", String.class);
        } catch (Exception ex) {
            Homabric.LOGGER.warn("Using command without name, referencing home");
        }

        PlayerObject player = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName());
        if(player.isLimitReached(context.getSource())) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.home_limit_reached").formatted(Formatting.RED), false);
            return 1;
        }

        PlayerObject.HomeCreationResult result = player.createOrUpdateHome(context.getSource(), homeName);

        if(result == PlayerObject.HomeCreationResult.HOME_CREATED) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.new_home_created", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
        } else {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.home_location_updated", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
        }

        Homabric.reloadConfig();
        return 1;
    }

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(!context.getSource().getEntity().isPlayer()) {
            return 0;
        }

        String homeName = context.getArgument("home", String.class);
        PlayerObject.HomeRemoveResult result = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName()).removeHome(homeName);

        switch (result) {
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home").formatted(Formatting.RED), false);
            }
            case HOME_REMOVED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.home_removed", new LiteralText(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
            }
        }
        return 1;
    }

    public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerObject playerData = HomabricConfig.Config.getPlayer(context.getSource().getName());
        if (playerData == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_homes").formatted(Formatting.YELLOW), false);
            return 1;
        }

        SimpleGui gui = playerData.getHomesGUI(context.getSource());
        gui.open();
        return 1;
    }

    public static int allowHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String homeName = context.getArgument("home", String.class);
        ServerPlayerEntity allowedPlayer = context.getArgument("player", EntitySelector.class).getPlayer(context.getSource());

        PlayerObject.HomeAllowResult result = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName()).allowHome(homeName, allowedPlayer);

        switch (result) {
            case NO_PLAYER -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            }
            case NO_SELF_ALLOW -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.allow_self").formatted(Formatting.BLUE), false);
            }
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home").formatted(Formatting.RED), false);
            }
            case ALREADY_ALLOWED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.already_allowed").formatted(Formatting.RED), false);
            }
            case HOME_ALLOWED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.allowed", new LiteralText(homeName).formatted(Formatting.WHITE), new LiteralText(allowedPlayer.getEntityName()).formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
            }
        }

        return 1;
    }

    public static int disallowHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String homeName = context.getArgument("home", String.class);
        String disallowedPlayer = context.getArgument("player", String.class);

        PlayerObject.HomeDisallowResult result = HomabricConfig.Config.getOrCreatePlayer(context.getSource().getName()).disallowHome(homeName, disallowedPlayer);

        switch (result) {
            case NO_PLAYER -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            }
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home").formatted(Formatting.RED), false);
            }
            case NOT_ALLOWED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_disallow").formatted(Formatting.RED), false);
            }
            case HOME_ALLOWED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.disallowed", new LiteralText(homeName).formatted(Formatting.WHITE), new LiteralText(disallowedPlayer).formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
            }
        }

        return 1;
    }
}
