package org.gamefunxiao.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.logging.Level;

@SuppressWarnings({"deprecation"})
public class MenuListener implements Listener {

    private final GameFunXiao plugin;

    public MenuListener(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // 第一层保护：检查点击的是否是菜单Inventory（通过InventoryHolder）
        if (clickedInventory != null && clickedInventory.getHolder() instanceof BaseMenu menu) {
            if (menu.allowsTopInventoryClick(event)) {
                return;
            }
            event.setCancelled(true);
            if (plugin.getMenuManager().tryHandleCommandBackButton(player, menu, event)) {
                return;
            }
            safeHandleClick(menu, event, player);
            return;
        }

        // 第二层保护：检查顶部Inventory是否是菜单（通过InventoryHolder）
        if (topInventory.getHolder() instanceof BaseMenu menu) {
            if (clickedInventory != null && clickedInventory.equals(topInventory) && menu.allowsTopInventoryClick(event)) {
                return;
            }
            event.setCancelled(true);
            // 只有点击的是顶部Inventory才处理点击
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                if (plugin.getMenuManager().tryHandleCommandBackButton(player, menu, event)) {
                    return;
                }
                safeHandleClick(menu, event, player);
            } else if (menu.handlesBottomInventoryClick(event)) {
                safeHandleClick(menu, event, player);
            }
            return;
        }

        if (isMountedActiveGameCraftingView(player, topInventory)) {
            // 玩家坐船时打开原版背包/工作台，不能被 MenuManager 里的残留菜单记录当成菜单点击处理。
            plugin.getMenuManager().closeMenu(player);
            return;
        }

        // 第三层保护：通过MenuManager检查
        BaseMenu menu = plugin.getMenuManager().getOpenMenu(player);
        if (menu != null) {
            event.setCancelled(true);
            // 检查是否点击的是菜单区域
            if (event.getRawSlot() < menu.getInventory().getSize()) {
                if (plugin.getMenuManager().tryHandleCommandBackButton(player, menu, event)) {
                    return;
                }
                safeHandleClick(menu, event, player);
            }
            return;
        }

        // 第四层保护：检查标题特征（最后的保险）
        String title = event.getView().getTitle();
        if (title != null && (title.contains("§0§l") || title.contains("猎人") || title.contains("排行榜") ||
            title.contains("房间") || title.contains("导航") || title.contains("游戏") || title.contains("投票") ||
            title.contains("确认") || title.contains("修饰符") || title.contains("邀请") || title.contains("传送"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClickFinalGuard(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();
        BaseMenu menu = null;
        if (clickedInventory != null && clickedInventory.getHolder() instanceof BaseMenu clickedMenu) {
            menu = clickedMenu;
        } else if (topInventory != null && topInventory.getHolder() instanceof BaseMenu topMenu) {
            menu = topMenu;
        } else {
            menu = plugin.getMenuManager().getOpenMenu(player);
        }

        if (menu == null) {
            return;
        }

        if (clickedInventory != null && clickedInventory.equals(topInventory) && menu.allowsTopInventoryClick(event)) {
            return;
        }

        // 永久兜底：即使其他插件在 LOWEST 后又改了事件状态，GameFun 菜单也不能被拿物品。
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();

        // 检查是否是我们的菜单
        if (topInventory.getHolder() instanceof BaseMenu menu) {
            if (menu.allowsInventoryDrag(event)) {
                return;
            }
            event.setCancelled(true);
            return;
        }

        BaseMenu menu = plugin.getMenuManager().getOpenMenu(player);
        if (menu != null && isMountedActiveGameCraftingView(player, topInventory)) {
            plugin.getMenuManager().closeMenu(player);
            return;
        }
        if (menu != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        BaseMenu closingMenu = null;
        if (event.getView().getTopInventory().getHolder() instanceof BaseMenu topMenu) {
            closingMenu = topMenu;
        }

        if (closingMenu != null) {
            closingMenu.onClose(event);
        }

        BaseMenu finalClosingMenu = closingMenu;
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                plugin.getMenuManager().closeMenu(player);
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BaseMenu currentMenu) {
                plugin.getMenuManager().registerMenu(player, currentMenu);
                return;
            }
            BaseMenu trackedMenu = plugin.getMenuManager().getOpenMenu(player);
            if (trackedMenu == null || finalClosingMenu == null || trackedMenu == finalClosingMenu) {
                plugin.getMenuManager().closeMenu(player);
            }
        });

        if (plugin.getPlayerListener().isForcedHunterTpSelection(player.getUniqueId())) {
            var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (room != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin,
                    () -> plugin.getPlayerListener().openHunterTeleportMenu(player, room, true));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // 防止漏斗等方块从菜单中移动物品
        if (event.getSource().getHolder() instanceof BaseMenu ||
            event.getDestination().getHolder() instanceof BaseMenu) {
            event.setCancelled(true);
        }
    }

    private void safeHandleClick(BaseMenu menu, InventoryClickEvent event, Player player) {
        try {
            menu.handleClick(event);
        } catch (Exception | LinkageError error) {
            String itemType = event.getCurrentItem() == null ? "空" : event.getCurrentItem().getType().name();
            plugin.getLogger().log(Level.SEVERE,
                    "GameFun 菜单按钮执行失败，已拦截本次点击防止菜单物品被拿走。"
                            + " menu=" + menu.getClass().getName()
                            + ", title=" + event.getView().getTitle()
                            + ", rawSlot=" + event.getRawSlot()
                            + ", slot=" + event.getSlot()
                            + ", click=" + event.getClick()
                            + ", action=" + event.getAction()
                            + ", item=" + itemType,
                    error);
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§5§5⚠ §c这个菜单按钮执行失败了，§7我已经保护住菜单物品；§e请重启/更新插件后再试。");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.72f, 0.72f);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.36f, 0.58f);
        }
    }

    private boolean isMountedActiveGameCraftingView(Player player, Inventory topInventory) {
        if (player == null || topInventory == null || (!player.isInsideVehicle() && player.getVehicle() == null)) {
            return false;
        }

        InventoryType type = topInventory.getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.WORKBENCH) {
            return false;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        return room != null
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted()
                && !room.isSpectator(player.getUniqueId());
    }
}
