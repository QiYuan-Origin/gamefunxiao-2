package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;

public class ConfirmForceStartMenu extends BaseMenu {

    private final GameRoom room;
    private boolean canClick = false;

    public ConfirmForceStartMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l⚡ 确认强制开始 ⚡", 27);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.REDSTONE_TORCH,
            "§x§F§F§5§5§5§5⚡ §x§F§F§7§7§7§7强§x§F§F§9§9§9§9制§x§F§F§B§B§B§B开§x§F§F§D§D§D§D始 §x§F§F§5§5§5§5⚡",
            "§8· · · · · · · · · · · · · ·",
            "§f确定要强制开始游戏吗?",
            "§c请等待3秒后才能点击确认",
            "§8· · · · · · · · · · · · · ·"));

        // 确认按钮（初始为灰色）
        inventory.setItem(11, createItem(Material.GRAY_CONCRETE,
            "   §8[§7等待中...§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §7请等待3秒",
            "§8· · · · · · · · · · · · · ·"));

        // 取消按钮
        inventory.setItem(15, createItem(Material.RED_CONCRETE,
            "   §8[§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7取§x§F§F§9§9§9§9消§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §c点击取消",
            "§8· · · · · · · · · · · · · ·"));

        // 3秒后启用确认按钮
        new BukkitRunnable() {
            @Override
            public void run() {
                if (room.isAdminForceStartUsed() || room.getState() != org.gamefunxiao.game.RoomState.STARTING && room.getState() != org.gamefunxiao.game.RoomState.WAITING) {
                    inventory.setItem(11, createItem(Material.RED_CONCRETE,
                        "   §8[§c已失效§8]",
                        "§8· · · · · · · · · · · · · ·",
                        "§f- §7本次提前开始已经被使用",
                        "§8· · · · · · · · · · · · · ·"));
                    return;
                }
                canClick = true;
                inventory.setItem(11, createItem(Material.LIME_CONCRETE,
                    "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7确§x§9§9§F§F§9§9认§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a点击确认强制开始",
                    "§8· · · · · · · · · · · · · ·"));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        }.runTaskLater(plugin, 60L); // 3秒
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 11 -> {
                if (canClick) {
                    if (room.isAdminForceStartUsed()) {
                        playErrorSound();
                        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.force_start_already_used"));
                        player.closeInventory();
                        return;
                    }
                    playConfirmSound();
                    player.closeInventory();

                    // 根据配置决定强制开始模式
                    String forceMode = plugin.getConfigManager().getConfig().getString("hunter_game.force_start_mode", "direct");
                    if (room.getState() == org.gamefunxiao.game.RoomState.WAITING) {
                        plugin.getGameManager().startCountdown(room);
                    }
                    if (room.getGameMode().isDeathSwap()) {
                        plugin.getGameManager().forceStartNow(room);
                    } else if ("skip_to_1min".equals(forceMode)) {
                        // 跳至1分钟
                        plugin.getGameManager().speedUpCountdown(room);
                    } else {
                        // 直接开始
                        plugin.getGameManager().forceStartNow(room);
                    }
                } else {
                    playErrorSound();
                }
            }
            case 15 -> {
                playCancelSound();
                player.closeInventory();
            }
        }
    }
}
