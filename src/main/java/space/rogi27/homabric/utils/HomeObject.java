package space.rogi27.homabric.utils;

import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Locale;

@ConfigSerializable
public class HomeObject {
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    @Comment(value = "Icon must be an identifier, for example 'minecraft:cobblestone'")
    public String icon;
    @Comment(value = "Players that can access this home")
    public ArrayList<String> allowedPlayers;

    public HomeObject withData(String world, double x, double y, double z, float yaw, float pitch, @Nullable ArrayList<String> allowedPlayers, @Nullable Identifier icon) {
        this.world = world;
        this.x = limitValue(x);
        this.y = limitValue(y);
        this.z = limitValue(z);
        this.yaw = limitValue(yaw);
        this.pitch = limitValue(pitch);
        if(allowedPlayers == null) {
            this.allowedPlayers = new ArrayList<>();
        } else {
            this.allowedPlayers = allowedPlayers;
        }
        if(icon == null) {
            this.icon = new Identifier("minecraft:map").toString();
        } else {
            this.icon = icon.toString();
        }
        return this;
    }

    public boolean teleportPlayer(ServerPlayerEntity player) {
        ServerWorld homeWorld = player.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(this.world)));
        Vec3d oldPos = player.getPos();
        player.teleport(homeWorld, this.x, this.y, this.z, this.yaw, this.pitch);
        player.getWorld().spawnParticles(ParticleTypes.GLOW_SQUID_INK, oldPos.x, oldPos.y, oldPos.z, 50, 2, 2, 2, 0.1);
        player.getWorld().spawnParticles(ParticleTypes.GLOW_SQUID_INK, this.x, this.y, this.z, 50, 2, 2, 2, 0.1);
        return true;
    }

    public static double limitValue(double value) {
        return (double) Math.round(value * 100) / 100;
    }

    public static float limitValue(float value) {
        return Float.parseFloat(String.format(Locale.US, "%.2f", value));
    }

    public boolean allowFor(String name) {
        if(allowedPlayers.contains(name)) return true;
        allowedPlayers.add(name);
        return true;
    }

    public boolean disallowFor(String name) {
        if(allowedPlayers.contains(name)) allowedPlayers.remove(name);
        return true;
    }

    public boolean isAllowedFor(String name) {
        return allowedPlayers.contains(name);
    }

    public IconResult setIcon(Identifier item) {
        if(Registry.ITEM.get(item) == Items.AIR) {
            return IconResult.WRONG_ICON;
        }
        this.icon = item.toString();
        return IconResult.ICON_SET;
    }

    public enum IconResult {
        WRONG_ICON,
        ICON_SET,
    }
}
