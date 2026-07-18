package org.polyfrost.hytils.client.huds

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.Item
import org.polyfrost.oneconfig.api.config.v1.annotations.MultiSelectDropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.RadioButton
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.utils.v1.dsl.mc
import org.polyfrost.hytils.client.features.game.BedWarsResourcesTracker

class BedWarsResourcesHud : LegacyHud(
    "bedwars_resources.json",
    "BedWars Resources",
    Category.INFO
) {
    private companion object {
        private const val ICON = 16
        private const val TEXT_GAP = 2
    }

    @MultiSelectDropdown(title = "Shown Resources", options = ["Iron", "Gold", "Diamond", "Emerald"])
    var shownResources = booleanArrayOf(true, true, true, true)

    @Switch(title = "Show Ender Chest Resources")
    var showEnderChest = true

    @Switch(title = "Hide When Zero")
    var hideWhenZero = true

    @Slider(title = "Padding", min = 0f, max = 20f, step = 1f)
    var padding = 5f

    @RadioButton(title = "Direction", options = ["Horizontal", "Vertical"])
    var direction = 0

    @Switch(title = "Reversed")
    var reversed = false

    @RadioButton(title = "Text Position", options = ["Left", "Right"])
    var textPosition = 1

    private val states = listOf(
        ResourceState(BedWarsResourcesTracker.Resource.IRON),
        ResourceState(BedWarsResourcesTracker.Resource.GOLD),
        ResourceState(BedWarsResourcesTracker.Resource.DIAMOND),
        ResourceState(BedWarsResourcesTracker.Resource.EMERALD)
    )

    private var actualWidth = ICON.toFloat()
    private var actualHeight = ICON.toFloat()

    override val width
        get() = actualWidth
    override val height
        get() = actualHeight

    override fun defaultPosition() = 0f to 0f

    override fun update(): Boolean {
        val textYOffset = ((ICON - mc.font.lineHeight) / 2f).toInt() + 1
        val pad = padding.toInt()
        val isHorizontal = direction == 0
        val textRight = textPosition == 1

        var cursorX = 0
        var cursorY = 0
        var maxCell = 0

        val indices = if (reversed) 3 downTo 0 else 0..3

        for (i in indices) {
            val state = states[i]

            if (!shownResources[state.resource.ordinal]) {
                state.isVisible = false
                continue
            }

            val count = getItemCount(state.resource.item)
            state.isVisible = !hideWhenZero || count > 0

            if (!state.isVisible) continue

            if (state.count != count) {
                state.count = count
                state.text = count.toString()
                state.textWidth = mc.font.width(state.text)
            }

            val textPart = if (state.textWidth > 0) TEXT_GAP + state.textWidth else 0
            val cellW = ICON + textPart

            state.iconX = if (textRight) 0 else textPart
            state.textX = if (textRight) ICON + TEXT_GAP else 0

            if (isHorizontal) {
                state.iconX += cursorX
                state.textX += cursorX
                state.iconY = 0
                state.textY = textYOffset
                
                cursorX += cellW + pad
            } else {
                state.iconY = cursorY
                state.textY = cursorY + textYOffset
                
                cursorY += ICON + pad
            }

            maxCell = maxOf(maxCell, cellW)
        }

        actualWidth = (if (isHorizontal) cursorX - pad else maxCell).coerceAtLeast(0).toFloat()
        actualHeight = (if (isHorizontal) ICON else cursorY - pad).coerceAtLeast(0).toFloat()

        return false
    }

    override fun render(mcCtx: GuiGraphicsExtractor) {
        if (states.none { it.isVisible }) return

        if (showBackground) {
            mcCtx.fill(0, 0, actualWidth.toInt(), actualHeight.toInt(), bgColor)
        }

        for (state in states) {
            if (!state.isVisible) continue

            //~ if <26.1 'item' -> 'renderItem'
            mcCtx.item(state.resource.stack, state.iconX, state.iconY)

            if (state.textWidth > 0) {
                //~ if <26.1 'text' -> 'drawString'
                mcCtx.text(mc.font, state.text, state.textX, state.textY, textColor)
            }
        }
    }

    private fun getItemCount(item: Item): Int {
        val player = mc.player ?: return 0
        val inventoryCount = player.inventory.sumOf { if (it.item == item) it.count else 0 }
        val enderChestCount = if (showEnderChest) BedWarsResourcesTracker.enderChestCounts[item] ?: 0 else 0
        return inventoryCount + enderChestCount
    }

    private class ResourceState(val resource: BedWarsResourcesTracker.Resource) {
        var count = -1
        var text = ""
        var textWidth = 0
        var iconX = 0
        var iconY = 0
        var textX = 0
        var textY = 0
        var isVisible = false
    }
}
