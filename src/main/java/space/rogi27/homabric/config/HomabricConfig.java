package space.rogi27.homabric.config;

import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.api.ConfigEntry;
import me.lortseam.completeconfig.api.ConfigGroup;
import me.lortseam.completeconfig.data.Config;
import space.rogi27.homabric.Homabric;
import space.rogi27.homabric.utils.PlayerObject;

import java.util.HashMap;
import java.util.Map;

public class HomabricConfig extends Config implements ConfigContainer {
    public HomabricConfig() {
        super("homabric");
    }

    @Transitive
    public static class Config implements ConfigGroup {
        @ConfigEntry(comment = "Do not touch this value, it allows mod to \ncheck if config file is outdated or not")
        private static int configVersion = 1;
        @ConfigEntry(comment = "This option enables alternative command variants like /sethome, /removehome and etc.")
        private static boolean enableOldschoolCommands = true;
        @ConfigEntry(comment = "Sets the maximum amount of homes per player")
        private static int maxHomes = 2;
        @ConfigEntry(comment = "List of players with their homes")
        private static Map<String, PlayerObject> players = new HashMap<>();

        public static int getConfigVersion() {
            return configVersion;
        }

        public static boolean isOldschoolCommandsEnabled() {
            return enableOldschoolCommands;
        }

        public static int maxHomes() {
            return maxHomes;
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
