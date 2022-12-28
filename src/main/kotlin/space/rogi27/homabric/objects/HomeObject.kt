package space.rogi27.homabric.objects

import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.util.*

@ConfigSerializable
class HomeObject {
    var world: String? = null
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = 0f
    var pitch = 0f
    
    @Comment(value = "Icon must be an identifier, for example 'minecraft:cobblestone'")
    var icon: String? = null
    
    @Comment(value = "Players that can access this home")
    var allowedPlayers: ArrayList<String>? = null
    fun withData(world: String?, x: Double, y: Double, z: Double, yaw: Float, pitch: Float, allowedPlayers: ArrayList<String>?, icon: Identifier?): HomeObject {
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
            this.icon = Identifier("minecraft:map").toString()
        } else {
            this.icon = icon.toString()
        }
        return this
    }
    
    fun teleportPlayer(player: ServerPlayerEntity): Boolean {
        val homeWorld = player.getServer()!!.getWorld(
            RegistryKey.of(
                RegistryKeys.WORLD, Identifier(
                    world
                )
            )
        )
        val oldPos = player.pos
        player.teleport(homeWorld, x, y, z, yaw, pitch)
        player.getWorld().spawnParticles(ParticleTypes.GLOW_SQUID_INK, oldPos.x, oldPos.y, oldPos.z, 50, 2.0, 2.0, 2.0, 0.1)
        player.getWorld().spawnParticles(ParticleTypes.GLOW_SQUID_INK, x, y, z, 50, 2.0, 2.0, 2.0, 0.1)
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
    
    fun setIcon(item: Identifier): IconResult {
        if (Registries.ITEM[item] === Items.AIR) {
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
