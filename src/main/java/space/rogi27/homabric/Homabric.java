package space.rogi27.homabric;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import space.rogi27.homabric.commands.AdminCommands;
import space.rogi27.homabric.commands.BaseCommands;
import space.rogi27.homabric.commands.OldschoolCommands;
import space.rogi27.homabric.config.HomabricConfig;

public class Homabric implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Homabric");
    public static final HomabricConfig config = new HomabricConfig();

    @Override
    public void onInitialize() {
        config.load();
        if(HomabricConfig.Config.getConfigVersion() != 1) {
            LOGGER.error("[Homabric:ERROR] You have outdated configuration file, be sure to update it to new version!");
        }

        BaseCommands.init();
        AdminCommands.init();
        OldschoolCommands.init();

        LOGGER.info("[Homabric:DONE] Homabric is ready to help you with your adventures!");
    }

    public static void reloadConfig() {
        config.save();
        config.load();
    }
}
