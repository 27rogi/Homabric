package space.rogi27.homabric.objects

import com.mojang.brigadier.exceptions.CommandSyntaxException
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import eu.pb4.sgui.api.gui.SlotBasedGui
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import space.rogi27.homabric.config.ConfigManager
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

    fun getHomesCount(): Int {
        return homes?.values?.count { it != null } ?: 0
    }

    fun checkHomeLimit(source: CommandSourceStack): Boolean {
        return getHomesCount() >= getHomeLimit(source)
    }
    
    fun getHomeLimit(player: CommandSourceStack?): Int {
        val player = player ?: return HomabricConfig.entries.homesLimit

        if (Permissions.check(player, "homabric.limit.bypass", PermissionLevel.MODERATORS)) {
            return Int.MAX_VALUE
        }

        return HomabricConfig.entries.permissionsHomeLimit
            .asSequence()
            .filter { (permission, _) -> Permissions.check(player, "homabric.homelimit.$permission") }
            .maxOfOrNull { (_, config) -> config.maxHomes } ?: HomabricConfig.entries.homesLimit
    }
    
    @Throws(CommandSyntaxException::class)
    fun createOrUpdateHome(player: CommandSourceStack, homeName: String): HomeCreationResult {
        var result = HomeCreationResult.HOME_CREATED
        val home = homes!![homeName]
        var icon: Identifier? = null
        var allowedPlayers: ArrayList<String>? = ArrayList()
        if (home != null) {
            icon = home.icon?.let { Identifier.tryParse(it) }
            if (home.allowedPlayers!!.isNotEmpty()) allowedPlayers = home.allowedPlayers
            result = HomeCreationResult.HOME_UPDATED
        }
        homes?.set(
            homeName, HomeObject().withData(
                player.level.dimension().identifier().toString(), player.position.x, player.position.y, player.position.z, player.player!!.yRot, player.player!!.xRot, allowedPlayers, icon
            )
        )
        ConfigManager.saveAndLoadAll()
        return result
    }
    
    @Throws(CommandSyntaxException::class)
    fun getHomesGUI(source: CommandSourceStack): SimpleGui {
        val gui = SimpleGui(MenuType.GENERIC_9x6, source.player, false)
        val index = AtomicInteger()
        homes?.forEach { (key: String?, data: HomeObject?) ->
            val lore = ArrayList<Component>()
            lore.add(
                Component.translatable(
                    "X: %s Y: %s Z: %s", Component.literal(java.lang.String.valueOf(data!!.x)).withStyle(ChatFormatting.GREEN), Component.literal(java.lang.String.valueOf(data.y)).withStyle(ChatFormatting.GREEN), Component.literal(java.lang.String.valueOf(data.z)).withStyle(ChatFormatting.GREEN)
                ).withStyle(ChatFormatting.GRAY)
            )
            lore.add(
                Component.translatable(
                    "text.homabric.gui_lore_world", Component.literal(data.world!!).withStyle(ChatFormatting.GREEN)
                ).withStyle(ChatFormatting.GRAY)
            )
            if (data.allowedPlayers!!.size > 0) lore.add(
                Component.translatable(
                    "text.homabric.gui_lore_allowed", Component.literal(
                        java.lang.String.join(
                            ",", data.allowedPlayers
                        )
                    ).withStyle(ChatFormatting.GREEN)
                )
            )
            val slotItemId = data.icon?.let { Identifier.tryParse(it) }
            val slotItem = GuiElementBuilder.from(
                ItemStack(BuiltInRegistries.ITEM.getOptional(slotItemId).orElse(Items.AIR))
            ).setName(Component.literal(key!!).withStyle(ChatFormatting.YELLOW)).setLore(lore).setCallback { _: Int, _: ClickType?, _: ContainerInput?, _: SlotBasedGui ->
                gui.close()
                TeleportHelper.runTeleport(source.player!!, fun() {
                    data.teleportPlayer(source.player!!)
                    source.sendSystemMessage(
                        Component.translatable(
                            "text.homabric.teleport_done", Component.literal(key).withStyle(ChatFormatting.WHITE)
                        ).withStyle(ChatFormatting.GREEN)
                    )
                })
            }.build()
            gui.setSlot(index.get(), slotItem)
            index.getAndIncrement()
        }
        gui.lockPlayerInventory = true
        gui.title = Component.translatable(
            "text.homabric.gui_title", Component.literal(source.textName).withStyle(ChatFormatting.DARK_BLUE), Component.literal(homes!!.size.toString()), Component.literal(getHomeLimit(source).toString()).withStyle(ChatFormatting.DARK_BLUE)
        )
        return gui
    }
    
    fun removeHome(name: String): HomeRemoveResult {
        if (getHome(name) == null) {
            return HomeRemoveResult.NO_HOME
        }
        homes!!.remove(name)
        ConfigManager.saveAndLoadAll()
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
    
    fun allowHome(name: String, allowedPlayer: ServerPlayer?): HomeAllowResult {
        if (allowedPlayer == null) {
            return HomeAllowResult.NO_PLAYER
        }
        if (HomesConfig.getPlayer(allowedPlayer.gameProfile.name) == this) {
            return HomeAllowResult.NO_SELF_ALLOW
        }
        val home = getHome(name) ?: return HomeAllowResult.NO_HOME
        if (home.isAllowedFor(allowedPlayer.gameProfile.name)) {
            return HomeAllowResult.ALREADY_ALLOWED
        }
        home.allowFor(allowedPlayer.gameProfile.name)
        ConfigManager.saveAndLoadAll()
        return HomeAllowResult.HOME_ALLOWED
    }
    
    fun disallowHome(name: String, disallowedPlayer: String?): HomeDisallowResult {
        val home = getHome(name) ?: return HomeDisallowResult.NO_HOME
        if (!home.isAllowedFor(disallowedPlayer!!)) {
            return HomeDisallowResult.NOT_ALLOWED
        }
        home.disallowFor(disallowedPlayer)
        ConfigManager.saveAndLoadAll()
        return HomeDisallowResult.HOME_DISALLOWED
    }
    
    fun getAllowedHomeNames(name: String?): ArrayList<String>? {
        val names = ArrayList<String>()
        if (homes == null) return null
        homes!!.forEach { (key: String, home: HomeObject?) ->
            if (home!!.allowedPlayers != null && home.allowedPlayers!!.contains(name)) names.add(key)
            ConfigManager.saveAndLoadAll()
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
        NO_PLAYER, NO_HOME, NOT_ALLOWED, HOME_DISALLOWED
    }
    
    enum class TeleportResult {
        TELEPORT_DONE, NO_HOME
    }
    
    enum class TeleportToOtherResult {
        TELEPORT_DONE, NO_PLAYER, NO_HOME, NO_ACCESS
    }
    
    companion object {
        @Throws(CommandSyntaxException::class)
        fun teleportToOtherHome(source: CommandSourceStack, playerName: String?, homeName: String, force: Boolean): TeleportToOtherResult {
            val player = source.player!!
            val owner: PlayerObject = HomesConfig.getPlayer(playerName!!) ?: return TeleportToOtherResult.NO_PLAYER
            val home = owner.getHome(homeName) ?: return TeleportToOtherResult.NO_HOME
            if (!force) {
                if (!home.isAllowedFor(player.name.toString())) {
                    return TeleportToOtherResult.NO_ACCESS
                }
            }
            TeleportHelper.runTeleport(player, fun() {
                home.teleportPlayer(player)
                source.sendSystemMessage(
                    Component.translatable("text.homabric.teleport_done", Component.literal(homeName).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN)
                )
            })
            return TeleportToOtherResult.TELEPORT_DONE
        }
    }
}
