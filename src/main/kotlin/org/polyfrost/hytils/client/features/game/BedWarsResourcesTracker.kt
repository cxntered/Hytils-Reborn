package org.polyfrost.hytils.client.features.game

import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.polyfrost.hytils.client.data.providers.LanguageData
import org.polyfrost.hytils.client.events.ChatReceiveEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe
import org.polyfrost.oneconfig.utils.v1.dsl.mc

object BedWarsResourcesTracker {
    val enderChestCounts = mutableMapOf<Item, Int>()

    @Subscribe
    fun onTick(event: TickEvent.Start) {
        //~ if <26.2 '.gui.screen()' -> '.screen'
        val screen = mc.gui.screen() as? ContainerScreen ?: return
        val title = screen.title.string
        if (title != "Ender Chest") return

        for (resource in Resource.entries) {
            //? if >=1.21.5 {
            val count = screen.menu.container.sumOf { if (it.item == resource.item) it.count else 0 }
            //?} else {
            /*val container = screen.menu.container
            var count = 0
            for (i in 0 until container.containerSize) {
                val stack = container.getItem(i)
                if (stack.item == resource.item) {
                    count += stack.count
                }
            }
            *///?}
            enderChestCounts[resource.item] = count
        }
    }

    @Subscribe
    fun onChatReceived(event: ChatReceiveEvent) {
        val match = LanguageData.DEPOSIT_ITEMS.find(event.unformattedMessage) ?: return
        val item = match.groups["item"]?.value ?: return
        val chest = match.groups["chest"]?.value ?: return
        val total = match.groups["total"]?.value?.toIntOrNull() ?: return

        if (chest != "Ender Chest") return

        val resource = Resource.fromDisplayName(item) ?: return
        enderChestCounts[resource.item] = total
    }

    @Subscribe
    fun onWorldLoad(event: WorldEvent.Load) {
        enderChestCounts.clear()
    }

    enum class Resource(val item: Item, val displayName: String) {
        IRON(Items.IRON_INGOT, "Iron Ingot"),
        GOLD(Items.GOLD_INGOT, "Gold Ingot"),
        DIAMOND(Items.DIAMOND, "Diamond"),
        EMERALD(Items.EMERALD, "Emerald");

        val stack by lazy { ItemStack(item) }

        companion object {
            fun fromItem(item: Item) = entries.find { it.item == item }
            fun fromDisplayName(displayName: String) = entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }
    }
}
