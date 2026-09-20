package space.rogi27.homabric.objects

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import space.rogi27.homabric.Homabric
import java.util.*

@ConfigSerializable
class HomeObject {
    var world: String? = null
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = 0f
    var pitch = 0f
    
    @Comment(value = "Must be an identifier. Example: 'minecraft:cobblestone'")
    var icon: String? = null

    var allowedPlayers: ArrayList<String>? = null
    fun withData(world: String?, x: Double, y: Double, z: Double, yaw: Float, pitch: Float, allowedPlayers: ArrayList<String>?, icon: ResourceLocation?): HomeObject {
        this.world = world
        this.x = limitValue(x)
        this.y = limitValue(y)
        this.z = limitValue(z)
        this.yaw = limitValue(yaw)
        this.pitch = limitValue(pitch)
        if (allowedPlayers == null) {
            this.allowedPlayers = ArrayList()
        } else {
            this.allowedPlayers = allowedPlayers
        }
        if (icon == null) {
            this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "map").toString()
        } else {
            this.icon = icon.toString()
        }
        return this
    }
    
    fun teleportPlayer(player: ServerPlayer): Boolean {
        val server = player.level().server

        val dimensionId = world?.let { ResourceLocation.tryParse(it) }
        if (dimensionId === null) {
            Homabric.logger.error("Unable to parse identifier $world from config!")
            return false
        }

        val worldKey = ResourceKey.create(Registries.DIMENSION, dimensionId)
        val homeLevel = server!!.getLevel(worldKey)
        if (homeLevel === null) {
            Homabric.logger.error("Error while teleporting player! Unable to find level $homeLevel")
            return false
        }

        val oldPos = player.position()
        player.teleportTo(homeLevel, x, y, z, yaw, pitch)

        // workaround for bug when xp is missing after teleporting between dimensions
        if (!player.isDeadOrDying) {
            player.giveExperiencePoints(0)
        }

        player.serverLevel().sendParticles(ParticleTypes.GLOW_SQUID_INK, oldPos.x, oldPos.y, oldPos.z, 50, 2.0, 2.0, 2.0, 0.1)
        player.serverLevel().sendParticles(ParticleTypes.GLOW_SQUID_INK, x, y, z, 50, 2.0, 2.0, 2.0, 0.1)
        return true
    }
    
    fun allowFor(name: String): Boolean {
        if (allowedPlayers!!.contains(name)) return true
        allowedPlayers!!.add(name)
        return true
    }
    
    fun disallowFor(name: String): Boolean {
        if (allowedPlayers!!.contains(name)) allowedPlayers!!.remove(name)
        return true
    }
    
    fun isAllowedFor(name: String): Boolean {
        return allowedPlayers!!.contains(name)
    }
    
    fun setIcon(item: ResourceLocation): IconResult {
        if (BuiltInRegistries.ITEM.get(item) === Items.AIR) {
            return IconResult.WRONG_ICON
        }
        icon = item.toString()
        return IconResult.ICON_SET
    }
    
    enum class IconResult {
        WRONG_ICON, ICON_SET
    }
    
    companion object {
        fun limitValue(value: Double): Double {
            return Math.round(value * 100).toDouble() / 100
        }
        
        fun limitValue(value: Float): Float {
            return String.format(Locale.US, "%.2f", value).toFloat()
        }
    }
}
