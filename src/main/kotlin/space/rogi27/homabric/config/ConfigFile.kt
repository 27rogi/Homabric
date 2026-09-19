package space.rogi27.homabric.config

import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import space.rogi27.homabric.Homabric

abstract class ConfigFile<T : Any>(
    fileName: String,
    private val type: Class<T>,
    private val default: () -> T,
) {
    private val loader = HoconConfigurationLoader.builder()
        .path(ConfigManager.resolve(fileName))
        .build()

    lateinit var entries: T
        private set

    init {
        @Suppress("LeakingThis")
        Homabric.logger.info("Loading config file $fileName")
        ConfigManager.register(this)
    }

    fun load() {
        val node = loader.load()
        entries = node.get(type) ?: default()
        save()
    }

    fun save() {
        val node = loader.createNode()
        node.set(type, entries)
        loader.save(node)
    }
}