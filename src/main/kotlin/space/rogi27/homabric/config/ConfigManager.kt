package space.rogi27.homabric.config

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

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