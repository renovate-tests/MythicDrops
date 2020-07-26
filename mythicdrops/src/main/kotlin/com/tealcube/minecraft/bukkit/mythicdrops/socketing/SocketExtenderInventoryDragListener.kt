/*
 * This file is part of MythicDrops, licensed under the MIT License.
 *
 * Copyright (C) 2019 Richard Harrah
 *
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tealcube.minecraft.bukkit.mythicdrops.socketing

import com.tealcube.minecraft.bukkit.mythicdrops.api.settings.SettingsManager
import com.tealcube.minecraft.bukkit.mythicdrops.api.tiers.TierManager
import com.tealcube.minecraft.bukkit.mythicdrops.chatColorize
import com.tealcube.minecraft.bukkit.mythicdrops.getTargetItemAndCursorAndPlayer
import com.tealcube.minecraft.bukkit.mythicdrops.stripChatColors
import com.tealcube.minecraft.bukkit.mythicdrops.strippedIndexOf
import com.tealcube.minecraft.bukkit.mythicdrops.updateCurrentItemAndSubtractFromCursor
import com.tealcube.minecraft.bukkit.mythicdrops.utils.TierUtil
import io.pixeloutlaw.minecraft.spigot.bandsaw.JulLoggerFactory
import io.pixeloutlaw.minecraft.spigot.hilt.getDisplayName
import io.pixeloutlaw.minecraft.spigot.hilt.getLore
import io.pixeloutlaw.minecraft.spigot.hilt.setLore
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class SocketExtenderInventoryDragListener(
    private val settingsManager: SettingsManager,
    private val tierManager: TierManager
) : Listener {
    companion object {
        private val logger = JulLoggerFactory.getLogger(SocketExtenderInventoryDragListener::class.java)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @Suppress("detekt.ReturnCount")
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val targetItemAndCursorAndPlayer = event.getTargetItemAndCursorAndPlayer(logger) ?: return
        val (targetItem, cursor, player) = targetItemAndCursorAndPlayer
        val cursorName = cursor.getDisplayName() ?: ""

        if (!settingsManager.socketingSettings.options.socketExtenderMaterialIds.contains(cursor.type)) {
            logger.fine("!sockettingSettings.socketExtenderMaterialIds.contains(cursor.type)")
            return
        }

        // Check if the cursor is a Socket Extender
        if (cursorName != settingsManager.socketingSettings.items.socketExtender.name.chatColorize()) {
            logger.fine("cursorName != settingsManager.socketingSettings.items.socketExtender.name.chatColorize()")
            return
        }

        // Check if the targetItem has an open socket extender slot
        val targetItemLore = targetItem.getLore()
        val strippedTargetItemLore = targetItemLore.stripChatColors()
        val indexOfFirstSocketExtenderSlot = indexOfFirstOpenSocketExtenderSlot(strippedTargetItemLore)
        val requireExtenderSlots = settingsManager.socketingSettings.options.isRequireExtenderSlotsToAddSockets
        if (
            requireExtenderSlots &&
            indexOfFirstSocketExtenderSlot < 0
        ) {
            logger.fine(
                "requireExtenderSlots && indexOfFirstSocketExtenderSlot < 0"
            )
            player.sendMessage(settingsManager.languageSettings.socketing.noSocketExtenderSlots.chatColorize())
            return
        }

        val targetItemTier = TierUtil.getTierFromItemStack(targetItem, tierManager.get())
        val chatColorForSocketSlot =
            if (targetItemTier != null && settingsManager.socketingSettings.options.useTierColorForSocketName) {
                targetItemTier.displayColor
            } else {
                settingsManager.socketingSettings.options.defaultSocketNameColorOnItems
            }
        val emptySocketString = settingsManager.socketingSettings.items.socketedItem.socket.replace(
            "%tiercolor%",
            "$chatColorForSocketSlot"
        ).chatColorize()

        val newTargetLore = if (indexOfFirstSocketExtenderSlot < 0) {
            targetItemLore + emptySocketString
        } else {
            targetItemLore.toMutableList().apply {
                set(
                    indexOfFirstSocketExtenderSlot,
                    emptySocketString
                )
            }.toList()
        }
        targetItem.setLore(newTargetLore)

        event.updateCurrentItemAndSubtractFromCursor(targetItem)
        player.sendMessage(settingsManager.languageSettings.socketing.addedSocket.chatColorize())
    }

    private fun indexOfFirstOpenSocketExtenderSlot(lore: List<String>): Int {
        val socketString =
            settingsManager.socketingSettings.items.socketExtender.slot.replace('&', '\u00A7')
                .replace("\u00A7\u00A7", "&")
                .replace("%tiercolor%", "")
        return lore.strippedIndexOf(ChatColor.stripColor(socketString), true)
    }
}
