package xyz.xenondevs.nova.ui.item

import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

class VisualizeRegionItem(
    private val regionUUID: UUID,
    private val getRegion: () -> Region,
) : BaseItem() {
    
    override fun getItemProvider() =
        object : ItemProvider {
            override fun get() = null
            override fun getFor(playerUUID: UUID): ItemStack {
                val visible = VisualRegion.isVisible(playerUUID, regionUUID)
                return (if (visible) CoreGUIMaterial.AREA_BTN_ON.clientsideProvider
                else CoreGUIMaterial.AREA_BTN_OFF.clientsideProvider).get()
            }
        }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        VisualRegion.toggleView(player, regionUUID, getRegion())
        notifyWindows()
    }
    
}