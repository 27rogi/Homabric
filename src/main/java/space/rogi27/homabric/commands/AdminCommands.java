package space.rogi27.homabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.config.HomabricConfig;
import space.rogi27.homabric.utils.PlayerObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AdminCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("homabric")
                    .requires(Permissions.require("homabric.admin.use", 2))
                    .then(literal("reload")
                            .requires(Permissions.require("homabric.admin.reload", 2))
                            .executes(AdminCommands::reload)
                    )
                    .then(literal("teleport")
                            .requires(Permissions.require("homabric.admin.teleport", 2))
                            .then(argument("player", StringArgumentType.word())
                                    .suggests(AdminCommands::suggestPlayers)
                                    .then(argument("home", StringArgumentType.word())
                                            .suggests(AdminCommands::suggestPlayerHomes)
                                            .executes((AdminCommands::teleport))
                                    )
                            )
                    )
                    .then(literal("set")
                            .requires(Permissions.require("homabric.admin.set", 2))
                            .then(argument("player", StringArgumentType.word())
                                    .suggests(AdminCommands::suggestPlayers)
                                    .then(argument("home", StringArgumentType.word())
                                            .suggests(AdminCommands::suggestPlayerHomes)
                                            .executes((AdminCommands::set))
                                    )
                            )
                    )
                    .then(literal("remove")
                            .requires(Permissions.require("homabric.admin.remove", 2))
                            .then(argument("player", StringArgumentType.word())
                                    .suggests(AdminCommands::suggestPlayers)
                                    .then(argument("home", StringArgumentType.word())
                                            .suggests(AdminCommands::suggestPlayerHomes)
                                            .executes((AdminCommands::remove))
                                    )
                            )
                    )
                    .then(literal("list")
                            .requires(Permissions.require("homabric.admin.list", 2))
                            .then(argument("player", StringArgumentType.word())
                                    .suggests(AdminCommands::suggestPlayers)
                                    .executes((AdminCommands::list))
                            )
                    ).executes((AdminCommands::info))
            );
        });
    }

    public static CompletableFuture<Suggestions> suggestPlayers(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        HomabricConfig.Config.getPlayers().forEach((name, data) -> {
            builder.suggest(name);
        });
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestPlayerHomes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        PlayerObject player = HomabricConfig.Config.getPlayer(context.getArgument("player", String.class));
        if(player != null) {
            player.getHomeNames().forEach(builder::suggest);
        }
        return builder.buildFuture();
    }

    public static int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Homabric.config.load();
        context.getSource().sendFeedback(new TranslatableText("text.homabric.admin_config_reloaded").formatted(Formatting.GRAY), false);
        return 1;
    }

    public static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        List<String> info = Arrays.asList(
                "%s by Rogi27",
                "%s",
                "%s",
                "%s"
        );
        context.getSource().sendFeedback(new TranslatableText(String.join("\n", info),
                new LiteralText("Homabric").formatted(Formatting.AQUA),
                new TranslatableText("text.homabric.admin_info_line1").formatted(Formatting.GRAY),
                new LiteralText(" - /help homabric").formatted(Formatting.GRAY),
                new LiteralText(" - /help home").formatted(Formatting.GRAY)
        ).formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int teleport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String homeName = context.getArgument("home", String.class);
        PlayerObject player = HomabricConfig.Config.getPlayer(context.getArgument("player", String.class));
        if(player == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            return 1;
        }
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

        PlayerObject player = HomabricConfig.Config.getPlayer(context.getArgument("player", String.class));
        if(player == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            return 1;
        }
        PlayerObject.HomeCreationResult result = player.createOrUpdateHome(context.getSource(), homeName);

        if(result == PlayerObject.HomeCreationResult.HOME_CREATED) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.admin_new_home_created", new LiteralText(homeName).formatted(Formatting.WHITE), context.getArgument("player", String.class).formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
        } else {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.admin_home_location_updated", new LiteralText(homeName).formatted(Formatting.WHITE), context.getArgument("player", String.class).formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
        }

        Homabric.reloadConfig();
        return 1;
    }

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(!context.getSource().getEntity().isPlayer()) {
            return 0;
        }

        String homeName = context.getArgument("home", String.class);

        PlayerObject player = HomabricConfig.Config.getPlayer(context.getArgument("player", String.class));
        if(player == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_player_exists").formatted(Formatting.RED), false);
            return 1;
        }

        PlayerObject.HomeRemoveResult result = player.removeHome(homeName);

        switch (result) {
            case NO_HOME -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.no_home").formatted(Formatting.RED), false);
            }
            case HOME_REMOVED -> {
                context.getSource().sendFeedback(new TranslatableText("text.homabric.admin_home_removed", new LiteralText(homeName).formatted(Formatting.WHITE), context.getArgument("player", String.class).formatted(Formatting.AQUA)).formatted(Formatting.GREEN), false);
            }
        }
        return 1;
    }

    public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerObject playerData = HomabricConfig.Config.getPlayer(context.getArgument("player", String.class));
        if (playerData == null) {
            context.getSource().sendFeedback(new TranslatableText("text.homabric.no_homes").formatted(Formatting.YELLOW), false);
            return 1;
        }

        SimpleGui gui = playerData.getHomesGUI(context.getSource());
        gui.open();
        return 1;
    }
}
