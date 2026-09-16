package space.rogi27.homabric.config

import net.fabricmc.loader.api.FabricLoader
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.nio.file.Files

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

object ConfigManager {
    private val configDir = FabricLoader.getInstance().configDir.resolve("homabric").also {
        Files.createDirectories(it)
    }

    private val registered = mutableListOf<ConfigFile<*>>()

    internal fun resolve(fileName: String) = configDir.resolve(fileName)

    internal fun register(config: ConfigFile<*>) {
        registered += config
    }

    fun loadAll() = registered.forEach { it.load() }
    fun saveAll() = registered.forEach { it.save() }
    fun saveAndLoadAll() {
        saveAll()
        loadAll()
    }
}