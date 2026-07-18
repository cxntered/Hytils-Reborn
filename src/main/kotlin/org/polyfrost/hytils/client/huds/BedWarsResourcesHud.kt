package org.polyfrost.hytils.client.huds

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.MultiSelectDropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.RadioButton
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.utils.v1.dsl.mc
import org.polyfrost.hytils.client.features.game.BedWarsResourcesTracker
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Text

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

    @Switch(title = "Expand Resource Counts")
    var expandResourceCounts = false

    @Text(title = "Add Symbol")
    var addSymbol = " + "

    @Text(title = "Equals Symbol")
    var equalsSymbol = " = "

    @Color(title = "Ender Chest Color")
    var enderChestColor = PolyColor.PURPLE

    @Color(title = "Total Color")
    var totalColor = PolyColor.GREEN

    @Color(title = "Symbol Color")
    var symbolColor = PolyColor.rgb(85, 85, 85)

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

    private val states = arrayOf(
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

    override fun setup() {
        super.setup()

        if (isReal) {
            listOf(
                "addSymbol", "equalsSymbol", "enderChestColor", "totalColor", "symbolColor"
            ).forEach { hideIf(it, "expandResourceCounts") }
        }
    }

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

            val item = state.resource.item
            //~ if 1.21.4 '?.sumOf' -> '?.items?.sumOf'
            val inventoryCount = mc.player?.inventory?.sumOf { if (it.item == item) it.count else 0 } ?: 0
            val enderChestCount = if (showEnderChest) BedWarsResourcesTracker.enderChestCounts[item] ?: 0 else 0
            val totalCount = inventoryCount + enderChestCount

            state.isVisible = !hideWhenZero || totalCount > 0
            if (!state.isVisible) continue

            val shouldExpand = showEnderChest && expandResourceCounts && (!hideWhenZero || enderChestCount > 0)

            if (state.inventoryCount != inventoryCount || state.enderChestCount != enderChestCount
                || state.expanded != shouldExpand || state.lastAdd != addSymbol || state.lastEquals != equalsSymbol
            ) {
                
                state.inventoryCount = inventoryCount
                state.enderChestCount = enderChestCount
                state.expanded = shouldExpand
                state.lastAdd = addSymbol
                state.lastEquals = equalsSymbol

                if (shouldExpand) {
                    state.inventoryText = inventoryCount.toString()
                    state.enderChestText = enderChestCount.toString()
                    state.totalText = totalCount.toString()
                    
                    state.inventoryWidth = mc.font.width(state.inventoryText)
                    state.addWidth = mc.font.width(addSymbol)
                    state.enderChestWidth = mc.font.width(state.enderChestText)
                    state.equalsWidth = mc.font.width(equalsSymbol)
                    val totalWidth = mc.font.width(state.totalText)

                    state.textWidth = state.inventoryWidth + state.addWidth + state.enderChestWidth + state.equalsWidth + totalWidth
                } else {
                    state.singleText = totalCount.toString()
                    state.textWidth = mc.font.width(state.singleText)
                }
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

            if (state.textWidth <= 0) continue

            //~ if <26.1 'mcCtx.text' -> 'mcCtx.drawString' {
            if (state.expanded) {
                var x = state.textX
                    
                mcCtx.text(mc.font, state.inventoryText, x, state.textY, textColor)
                x += state.inventoryWidth

                mcCtx.text(mc.font, state.lastAdd, x, state.textY, symbolColor.argb)
                x += state.addWidth

                mcCtx.text(mc.font, state.enderChestText, x, state.textY, enderChestColor.argb)
                x += state.enderChestWidth

                mcCtx.text(mc.font, state.lastEquals, x, state.textY, symbolColor.argb)
                x += state.equalsWidth

                mcCtx.text(mc.font, state.totalText, x, state.textY, totalColor.argb)
            } else {
                mcCtx.text(mc.font, state.singleText, state.textX, state.textY, textColor)
            }
            //~}
        }
    }

    private class ResourceState(val resource: BedWarsResourcesTracker.Resource) {
        var inventoryCount = -1
        var enderChestCount = -1

        var expanded = false
        var lastAdd = ""
        var lastEquals = ""

        var singleText = ""
        var inventoryText = ""
        var enderChestText = ""
        var totalText = ""

        var inventoryWidth = 0
        var addWidth = 0
        var enderChestWidth = 0
        var equalsWidth = 0

        var textWidth = 0
        var iconX = 0
        var iconY = 0
        var textX = 0
        var textY = 0
        var isVisible = false
    }
}
