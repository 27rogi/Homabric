package space.rogi27.homabric

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import space.rogi27.homabric.commands.AdminCommands
import space.rogi27.homabric.commands.BaseCommands
import space.rogi27.homabric.commands.ClassicCommands
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.config.HomesConfig

class Homabric:ModInitializer {
    override fun onInitialize() {
        config.load()

        // TODO: find a way to dynamically check version
        if(HomabricConfig.entries.configVersion != 3) {
            throw Exception("Old config version detected! Please remove .minecraft/config/homabric to avoid errors!")
        }

        homesConfig.load()

        BaseCommands.init()
        AdminCommands.init()
        ClassicCommands.init()
    }
    
    companion object {
        @JvmField
        var logger: Logger = LogManager.getLogger("Homabric")
        val config get() = HomabricConfig
        val homesConfig get() = HomesConfig
    }
}
