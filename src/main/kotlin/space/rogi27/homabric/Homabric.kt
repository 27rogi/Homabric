package space.rogi27.homabric

import me.lortseam.completeconfig.data.Config
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
        homesConfig.load()
        
        BaseCommands.init()
        AdminCommands.init()
        ClassicCommands.init()
    }
    
    companion object {
        @JvmField
        var logger: Logger = LogManager.getLogger("Homabric")
        val config: Config = Config("homabric", arrayOf("homabric.config"), HomabricConfig)
        val homesConfig: Config = Config("homabric", arrayOf("homabric.homes"), HomesConfig)
        
        @JvmStatic
        fun saveAndReloadConfig() {
            config.save()
            homesConfig.save()
            config.load()
            homesConfig.load()
        }
    }
}
