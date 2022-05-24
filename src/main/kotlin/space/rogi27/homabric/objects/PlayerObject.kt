package space.rogi27.homabric.objects

import com.mojang.brigadier.exceptions.CommandSyntaxException
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text.literal
import net.minecraft.text.Text
import net.minecraft.text.Text.translatable
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import space.rogi27.homabric.Homabric
import space.rogi27.homabric.config.HomabricConfig
import space.rogi27.homabric.config.HomesConfig
import space.rogi27.homabric.helpers.TeleportHelper
import java.util.concurrent.atomic.AtomicInteger

@ConfigSerializable
class PlayerObject {
    @Comment("List of player homes")
    var homes: MutableMap<String, HomeObject?>? = null
    
    fun withData(homes: MutableMap<String, HomeObject?>?): PlayerObject {
        this.homes = homes
        return this
    }
    
    fun getHome(homeName: String): HomeObject? {
        return if (homes == null || homes!![homeName] == null) null else homes!![homeName]
    }
    
    // TODO: Find better and efficient way for checking permissions
    // Maybe use groups instead?
    fun isLimitReached(player: ServerCommandSource?): Boolean {
        val homesLimit = AtomicInteger(-1)
        HomabricConfig.permissionsHomeLimit.forEach { (key: String, permissionObject: HomePermissionObject) ->
            if (Permissions.check(
                    player!!, "homabric.homelimit.$key"
                )
            ) {
                if (homesLimit.get() < permissionObject.maxHomes) {
                    homesLimit.set(permissionObject.maxHomes)
                }
            }
        }
        // If player still has -1 limit then it means that he has no special permissions
        // set for home limit, and we should return default home value.
        // We don't use 0 because if owner needs to set home limit for some group to 0 it will cause troubles.
        return if (homesLimit.get() == -1) {
            homes!!.size >= HomabricConfig.homesLimit()
        } else homes!!.size >= homesLimit.get()
    }
    
    fun getHomeLimit(player: ServerCommandSource?): Int {
        val homesLimit = AtomicInteger(-1)
        HomabricConfig.permissionsHomeLimit.forEach { (key: String, permissionObject: HomePermissionObject) ->
            if (Permissions.check(
                    player!!, "homabric.homelimit.$key"
                )
            ) {
                if (homesLimit.get() < permissionObject.maxHomes) {
                    homesLimit.set(permissionObject.maxHomes)
                }
            }
        }
        // If player still has -1 limit then it means that he has no special permissions
        // set for home limit, and we should return default home value.
        // We don't use 0 because if owner needs to set home limit for some group to 0 it will cause troubles.
        return if (homesLimit.get() == -1) {
            HomabricConfig.homesLimit()
        } else homesLimit.get()
    }
    
    @Throws(CommandSyntaxException::class)
    fun createOrUpdateHome(player: ServerCommandSource, homeName: String): HomeCreationResult {
        var result = HomeCreationResult.HOME_CREATED
        val home = homes!![homeName]
        var icon: Identifier? = null
        var allowedPlayers: ArrayList<String>? = ArrayList()
        if (home != null) {
            icon = Identifier.tryParse(home.icon)
            if (home.allowedPlayers!!.size > 0) allowedPlayers = home.allowedPlayers
            result = HomeCreationResult.HOME_UPDATED
        }
        homes?.set(
            homeName, HomeObject().withData(
                player.world.registryKey.value.toString(), player.position.x, player.position.y, player.position.z, player.player.headYaw, player.player.pitch, allowedPlayers, icon
            )
        )
        Homabric.reloadConfig()
        return result
    }
    
    @Throws(CommandSyntaxException::class)
    fun getHomesGUI(source: ServerCommandSource): SimpleGui {
        val gui = SimpleGui(ScreenHandlerType.GENERIC_9X6, source.player, false)
        val index = AtomicInteger()
        homes?.forEach { (key: String?, data: HomeObject?) ->
            val lore = ArrayList<Text>()
            lore.add(
                Text.translatable(
                    "X: %s Y: %s Z: %s", Text.literal(java.lang.String.valueOf(data!!.x)).formatted(Formatting.GREEN), Text.literal(java.lang.String.valueOf(data.y)).formatted(Formatting.GREEN), Text.literal(java.lang.String.valueOf(data.z)).formatted(Formatting.GREEN)
                ).formatted(Formatting.GRAY)
            )
            lore.add(
                Text.translatable(
                    "text.homabric.gui_lore_world", Text.literal(data.world).formatted(Formatting.GREEN)
                ).formatted(Formatting.GRAY)
            )
            if (data.allowedPlayers!!.size > 0) lore.add(
                Text.translatable(
                    "text.homabric.gui_lore_allowed", Text.literal(
                        java.lang.String.join(
                            ",", data.allowedPlayers
                        )
                    ).formatted(Formatting.GREEN)
                )
            )
            val slotItem: GuiElementInterface = GuiElementBuilder.from(
                Registry.ITEM[Identifier.tryParse(data.icon)].defaultStack
            ).setName(Text.literal(key).formatted(Formatting.YELLOW)).setLore(lore).setCallback { _: Int, _: ClickType?, _: SlotActionType? ->
                try {
                    data.teleportPlayer(source.player)
                    gui.close()
                } catch (e: CommandSyntaxException) {
                    e.printStackTrace()
                }
                source.sendFeedback(
                    Text.translatable(
                        "text.homabric.teleport_done", Text.literal(key).formatted(Formatting.WHITE)
                    ).formatted(Formatting.GREEN), false
                )
            }.build()
            gui.setSlot(index.get(), slotItem)
            index.getAndIncrement()
        }
        gui.lockPlayerInventory = true
        gui.title = Text.translatable(
            "text.homabric.gui_title", Text.literal(source.name).formatted(Formatting.DARK_BLUE), Text.literal(homes!!.size.toString()), Text.literal(getHomeLimit(source).toString()).formatted(Formatting.DARK_BLUE)
        )
        return gui
    }
    
    fun removeHome(name: String): HomeRemoveResult {
        if (getHome(name) == null) {
            return HomeRemoveResult.NO_HOME
        }
        homes!!.remove(name)
        Homabric.reloadConfig()
        return HomeRemoveResult.HOME_REMOVED
    }
    
    val homeNames: ArrayList<String>?
        get() {
            val names = ArrayList<String>()
            if (homes == null) return null
            homes!!.forEach { (key: String) ->
                names.add(
                    key
                )
            }
            return names
        }
    
    fun allowHome(name: String, allowedPlayer: ServerPlayerEntity?): HomeAllowResult {
        if (allowedPlayer == null) {
            return HomeAllowResult.NO_PLAYER
        }
        if (HomesConfig.getPlayer(allowedPlayer.entityName) == this) {
            return HomeAllowResult.NO_SELF_ALLOW
        }
        val home = getHome(name) ?: return HomeAllowResult.NO_HOME
        if (home.isAllowedFor(allowedPlayer.entityName)) {
            return HomeAllowResult.ALREADY_ALLOWED
        }
        home.allowFor(allowedPlayer.entityName)
        Homabric.reloadConfig()
        return HomeAllowResult.HOME_ALLOWED
    }
    
    fun disallowHome(name: String, disallowedPlayer: String?): HomeDisallowResult {
        val home = getHome(name) ?: return HomeDisallowResult.NO_HOME
        if (home.isAllowedFor(disallowedPlayer!!)) {
            return HomeDisallowResult.NOT_ALLOWED
        }
        home.disallowFor(disallowedPlayer)
        Homabric.reloadConfig()
        return HomeDisallowResult.HOME_ALLOWED
    }
    
    fun getAllowedHomeNames(name: String?): ArrayList<String>? {
        val names = ArrayList<String>()
        if (homes == null) return null
        homes!!.forEach { (key: String, home: HomeObject?) ->
            if (home!!.allowedPlayers != null && home.allowedPlayers!!.contains(name)) names.add(key)
            Homabric.reloadConfig()
        }
        return names
    }
    
    enum class HomeCreationResult {
        HOME_CREATED, HOME_UPDATED
    }
    
    enum class HomeRemoveResult {
        NO_HOME, HOME_REMOVED
    }
    
    enum class HomeAllowResult {
        NO_PLAYER, NO_SELF_ALLOW, NO_HOME, ALREADY_ALLOWED, HOME_ALLOWED
    }
    
    enum class HomeDisallowResult {
        NO_PLAYER, NO_HOME, NOT_ALLOWED, HOME_ALLOWED
    }
    
    enum class TeleportResult {
        TELEPORT_DONE, NO_HOME
    }
    
    enum class TeleportToOtherResult {
        TELEPORT_DONE, NO_PLAYER, NO_HOME, NO_ACCESS
    }
    
    companion object {
        @Throws(CommandSyntaxException::class)
        fun teleportToOtherHome(source: ServerCommandSource, playerName: String?, homeName: String, force: Boolean): TeleportToOtherResult {
            val player = source.player
            val owner: PlayerObject = HomesConfig.getPlayer(playerName!!) ?: return TeleportToOtherResult.NO_PLAYER
            val home = owner.getHome(homeName) ?: return TeleportToOtherResult.NO_HOME
            if (!force) {
                if (!home.isAllowedFor(player.entityName)) {
                    return TeleportToOtherResult.NO_ACCESS
                }
            }
            TeleportHelper.runTeleport(source.player, fun() {
                home.teleportPlayer(player)
                source.sendFeedback(
                    Text.translatable("text.homabric.teleport_done", Text.literal(homeName).formatted(Formatting.WHITE)).formatted(Formatting.GREEN), false
                )
            })
            return TeleportToOtherResult.TELEPORT_DONE
        }
    }
}
