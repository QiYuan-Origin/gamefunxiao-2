package org.gamefunxiao.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;

public class BrickGuardMapEditListener implements Listener {

    private final GameFunXiao plugin;

    public BrickGuardMapEditListener(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getBrickGuardMapEditorManager() != null
                && plugin.getBrickGuardMapEditorManager().handleToolkitInteract(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getBrickGuardMapEditorManager() != null
                && plugin.getBrickGuardMapEditorManager().handleDrop(event.getPlayer(), event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .deserialize("§x§F§F§7§C§0§0工具包不能丢弃，退出编辑会自动恢复原背包"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || plugin.getBrickGuardMapEditorManager() == null) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (plugin.getBrickGuardMapEditorManager().shouldProtectEditorInventory(player, current, cursor)) {
            event.setCancelled(true);
        }
        plugin.getBrickGuardMapEditorManager().handleMenuClick(event, player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getBrickGuardMapEditorManager() != null
                && plugin.getBrickGuardMapEditorManager().isEditing(event.getPlayer())) {
            plugin.getBrickGuardMapEditorManager().exitEditorSession(event.getPlayer(), false);
        }
    }
}
