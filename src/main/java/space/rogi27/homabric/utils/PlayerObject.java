package space.rogi27.homabric.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.config.HomabricConfig;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@ConfigSerializable
public class PlayerObject {
    @Comment("List of player homes")
    public Map<String, HomeObject> homes;

    public PlayerObject withData(Map<String, HomeObject> homes) {
        this.homes = homes;
        return this;
    }

    public Map<String, HomeObject> getHomes() {
        if (this.homes == null) return null;
        return homes;
    }

    public HomeObject getHome(String homeName) {
        if (this.homes == null || this.homes.get(homeName) == null) return null;
        return homes.get(homeName);
    }

    // TODO: Find better and efficient way for checking permissions
    // Maybe use groups instead?
    public boolean isLimitReached(ServerCommandSource player) {
        AtomicInteger homesLimit = new AtomicInteger(-1);
        HomabricConfig.Config.getPermissionsHomeLimit().forEach((key, permissionObject) -> {
            if(Permissions.check(player, "homabric.homelimit."+key)) {
                if (homesLimit.get() < permissionObject.maxHomes) {
                    homesLimit.set(permissionObject.maxHomes);
                }
            }
        });
        // If player still has -1 limit then it means that he has no special permissions
        // set for home limit, and we should return default home value.
        // We don't use 0 because if owner needs to set home limit for some group to 0 it will cause troubles.
        if(homesLimit.get() == -1) {
            return this.homes.size() >= HomabricConfig.Config.homesLimit();
        }
        return this.homes.size() >= homesLimit.get();
    }

    public int getHomeLimit(ServerCommandSource player) {
        AtomicInteger homesLimit = new AtomicInteger(-1);
        HomabricConfig.Config.getPermissionsHomeLimit().forEach((key, permissionObject) -> {
            if(Permissions.check(player, "homabric.homelimit."+key)) {
                if (homesLimit.get() < permissionObject.maxHomes) {
                    homesLimit.set(permissionObject.maxHomes);
                }
            }
        });
        // If player still has -1 limit then it means that he has no special permissions
        // set for home limit, and we should return default home value.
        // We don't use 0 because if owner needs to set home limit for some group to 0 it will cause troubles.
        if(homesLimit.get() == -1) {
            return HomabricConfig.Config.homesLimit();
        }
        return homesLimit.get();
    }

    public HomeCreationResult createOrUpdateHome(ServerCommandSource player, String homeName) throws CommandSyntaxException {
        HomeCreationResult result = HomeCreationResult.HOME_CREATED;
        HomeObject home = this.getHomes().get(homeName);
        Identifier icon = null;
        ArrayList<String> allowedPlayers = new ArrayList<>();
        if(home != null) {
            icon = Identifier.tryParse(home.icon);
            if(home.allowedPlayers.size() > 0) allowedPlayers = home.allowedPlayers;
            result = HomeCreationResult.HOME_UPDATED;
        }
        this.getHomes().put(homeName, new HomeObject().withData(
                player.getWorld().getRegistryKey().getValue().toString(),
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z,
                player.getPlayer().headYaw,
                player.getPlayer().getPitch(),
                allowedPlayers,
                icon
        ));

        Homabric.reloadConfig();
        return result;
    }

    public SimpleGui getHomesGUI(ServerCommandSource source) throws CommandSyntaxException {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, source.getPlayer(), false);

        AtomicInteger index = new AtomicInteger();
        this.getHomes().forEach((key, data) -> {
            ArrayList<Text> lore = new ArrayList<>();
            lore.add(new TranslatableText("X: %s Y: %s Z: %s",
                    new LiteralText(String.valueOf(data.x)).formatted(Formatting.GREEN),
                    new LiteralText(String.valueOf(data.y)).formatted(Formatting.GREEN),
                    new LiteralText(String.valueOf(data.z)).formatted(Formatting.GREEN)).formatted(Formatting.GRAY));
            lore.add(new TranslatableText("text.homabric.gui_lore_world", new LiteralText(data.world).formatted(Formatting.GREEN)).formatted(Formatting.GRAY));
            if(data.allowedPlayers.size() > 0) lore.add(new TranslatableText("text.homabric.gui_lore_allowed", new LiteralText(String.join(",", data.allowedPlayers)).formatted(Formatting.GREEN)));

            GuiElementInterface slotItem = GuiElementBuilder
                    .from(Registry.ITEM.get(Identifier.tryParse(data.icon)).getDefaultStack())
                    .setName(new LiteralText(key).formatted(Formatting.YELLOW))
                    .setLore(lore)
                    .setCallback((i, type, action) -> {
                        try {
                            data.teleportPlayer(source.getPlayer());
                            gui.close();
                        } catch (CommandSyntaxException e) {
                            e.printStackTrace();
                        }
                        source.sendFeedback(new TranslatableText("text.homabric.teleport_done", new LiteralText(key).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false);
                    })
                    .build();
            gui.setSlot(index.get(), slotItem);
            index.getAndIncrement();
        });

        gui.setLockPlayerInventory(true);
        gui.setTitle(new TranslatableText("text.homabric.gui_title",
                new LiteralText(source.getName()).formatted(Formatting.DARK_BLUE),
                new LiteralText(String.valueOf(this.homes.size())),
                new LiteralText(String.valueOf(this.getHomeLimit(source))).formatted(Formatting.DARK_BLUE)
        ));
        return gui;
    }

    public static TeleportToOtherResult teleportToOtherHome(ServerCommandSource source, String playerName, String homeName, boolean force) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        PlayerObject owner = HomabricConfig.Config.getPlayer(playerName);
        if(owner == null) {
            return TeleportToOtherResult.NO_PLAYER;
        }

        HomeObject home = owner.getHome(homeName);
        if(home == null) {
            return TeleportToOtherResult.NO_HOME;
        }
        if(!force) {
            if(!home.isAllowedFor(player.getEntityName())) {
                return TeleportToOtherResult.NO_ACCESS;
            }
        }

        home.teleportPlayer(player);
        return TeleportToOtherResult.TELEPORT_DONE;
    }

    public TeleportResult teleportToHome(ServerCommandSource source, String homeName) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        HomeObject home = this.getHome(homeName);
        if(home == null) {
            return TeleportResult.NO_HOME;
        }

        home.teleportPlayer(player);
        return TeleportResult.TELEPORT_DONE;
    }

    public HomeRemoveResult removeHome(String name) {
        if(this.getHome(name) == null) {
            return HomeRemoveResult.NO_HOME;
        }

        this.homes.remove(name);

        Homabric.reloadConfig();
        return HomeRemoveResult.HOME_REMOVED;
    }

    public ArrayList<String> getHomeNames() {
        ArrayList<String> names = new ArrayList<>();
        if (this.getHomes() == null) return null;
        this.getHomes().forEach((key, home) -> {
            names.add(key);
        });
        return names;
    }

    public HomeAllowResult allowHome(String name, ServerPlayerEntity allowedPlayer) {
        String homeName = name;

        if(allowedPlayer == null) {
            return HomeAllowResult.NO_PLAYER;
        }

        if(Objects.equals(HomabricConfig.Config.getPlayer(allowedPlayer.getEntityName()), this)) {
            return HomeAllowResult.NO_SELF_ALLOW;
        }

        HomeObject home = this.getHome(homeName);
        if(home == null) {
            return HomeAllowResult.NO_HOME;
        }

        if(home.isAllowedFor(allowedPlayer.getEntityName())) {
            return HomeAllowResult.ALREADY_ALLOWED;
        }

        home.allowFor(allowedPlayer.getEntityName());
        Homabric.reloadConfig();

        return HomeAllowResult.HOME_ALLOWED;
    }

    public HomeDisallowResult disallowHome(String name, String disallowedPlayer) {
        String homeName = name;

        HomeObject home = this.getHome(homeName);
        if(home == null) {
            return HomeDisallowResult.NO_HOME;
        }

        if(home.isAllowedFor(disallowedPlayer)) {
            return HomeDisallowResult.NOT_ALLOWED;
        }

        home.disallowFor(disallowedPlayer);
        Homabric.reloadConfig();

        return HomeDisallowResult.HOME_ALLOWED;
    }

    public ArrayList<String> getAllowedHomeNames(String name) {
        ArrayList<String> names = new ArrayList<>();
        if (this.getHomes() == null) return null;
        this.getHomes().forEach((key, home) -> {
            if(home.allowedPlayers != null && home.allowedPlayers.contains(name)) names.add(key);
            Homabric.reloadConfig();
        });
        return names;
    }

    public enum HomeCreationResult {
        HOME_CREATED,
        HOME_UPDATED,
    }

    public enum HomeRemoveResult {
        NO_HOME,
        HOME_REMOVED,
    }

    public enum HomeAllowResult {
        NO_PLAYER,
        NO_SELF_ALLOW,
        NO_HOME,
        ALREADY_ALLOWED,
        HOME_ALLOWED,
    }

    public enum HomeDisallowResult {
        NO_PLAYER,
        NO_HOME,
        NOT_ALLOWED,
        HOME_ALLOWED,
    }

    public enum TeleportResult {
        TELEPORT_DONE,
        NO_HOME,
    }

    public enum TeleportToOtherResult {
        TELEPORT_DONE,
        NO_PLAYER,
        NO_HOME,
        NO_ACCESS,
    }
}
