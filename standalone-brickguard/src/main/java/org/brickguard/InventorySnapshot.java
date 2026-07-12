package org.brickguard;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;

final class InventorySnapshot {
    final Location location;
    final GameMode gameMode;
    final boolean allowFlight;
    final boolean flying;
    final ItemStack[] contents;
    final ItemStack[] armor;
    final ItemStack offhand;
    final int heldSlot;
    final int level;
    final float exp;
    final int totalExperience;

    InventorySnapshot(Player player) {
        location = player.getLocation().clone();
        gameMode = player.getGameMode();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        level = player.getLevel();
        exp = player.getExp();
        totalExperience = player.getTotalExperience();
        PlayerInventory inv = player.getInventory();
        contents = cloneItems(inv.getContents());
        armor = cloneItems(inv.getArmorContents());
        offhand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        heldSlot = inv.getHeldItemSlot();
    }

    void restore(Player player) {
        restoreInventoryOnly(player);
        restoreExperience(player);
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        }
    }

    void restoreInventoryOnly(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setContents(cloneItems(contents));
        inv.setArmorContents(cloneItems(armor));
        inv.setItemInOffHand(offhand == null ? null : offhand.clone());
        inv.setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
    }

    private void restoreExperience(Player player) {
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setTotalExperience(Math.max(0, totalExperience));
        player.setLevel(Math.max(0, level));
        player.setExp(Math.max(0.0F, Math.min(1.0F, exp)));
    }

    static ItemStack[] cloneItems(ItemStack[] source) {
        return Arrays.stream(source).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }
}
