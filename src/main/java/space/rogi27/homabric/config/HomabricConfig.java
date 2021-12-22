package space.rogi27.homabric.config;

import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.api.ConfigEntry;
import me.lortseam.completeconfig.api.ConfigGroup;
import me.lortseam.completeconfig.data.Config;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.utils.HomePermissionObject;
import space.rogi27.homabric.utils.PlayerObject;

import java.util.HashMap;
import java.util.Map;

public class HomabricConfig extends Config implements ConfigContainer {
    public HomabricConfig() {
        super("homabric");
    }

    @Transitive
    public static class Config implements ConfigGroup {
        @ConfigEntry(comment = "Do not touch this value, it allows mod to \ncheck if config file is outdated or not.")
        private static int configVersion = 2;
        @ConfigEntry(comment = "This option enables alternative command variants like /sethome, /removehome and etc.")
        private static boolean enableOldschoolCommands = true;
        @ConfigEntry(comment = "Sets the maximum amount of homes per player.")
        private static int homesLimit = 2;
        @ConfigEntry(comment = "You can define permissions that will override home limit for the players if they have them." +
                "\nPermission names are transformed to permissions like 'homabric.homelimit.<permissionName>'" +
                "\nExample permission: vip: { max-homes=6 }")
        private static Map<String, HomePermissionObject> permissionsHomeLimit = new HashMap<>();

        @ConfigEntry(comment = "List of players with their homes.")
        private static Map<String, PlayerObject> players = new HashMap<>();

        public static int getConfigVersion() {
            return configVersion;
        }

        public static boolean isOldschoolCommandsEnabled() {
            return enableOldschoolCommands;
        }

        public static int homesLimit() {
            return homesLimit;
        }

        public static Map<String, HomePermissionObject> getPermissionsHomeLimit() {
            return permissionsHomeLimit;
        }

        public static PlayerObject getPlayer(String name) {
            return players.get(name);
        }

        public static Map<String, PlayerObject> getPlayers() {
            return players;
        }

        public static PlayerObject getOrCreatePlayer(String name) {
            if(players.get(name) == null) {
                Homabric.LOGGER.warn("There is no data for " + name + ", creating new entry.");
                players.put(name, new PlayerObject().withData(Map.of()));
                Homabric.reloadConfig();
            }
            return players.get(name);
        }
    }
}
