package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.HashSet;

public class BrickGuardNavigationMenu extends BaseMenu {

    public BrickGuardNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l▣ 雨云 · 板砖守卫战 ▣", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.BRICK,
                "§x§F§F§7§C§0§0▣ §x§F§F§8§8§1§1雨§x§F§F§9§4§2§2云 §x§D§D§6§6§1§1· §x§B§B§4§4§0§0板§x§9§9§3§3§0§0砖§x§6§6§1§9§0§0守卫战",
                "§8· · · · · · · · · · · · · ·",
                "§f- §6板砖队要守住核心并击杀下界核心玩家",
                "§f- §c下界砖队要摧毁板砖核心方块到 0 血量",
                "§f- §7一小时超时平局，队伍尽量平衡",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(10, createQuickMatchButton());
        inventory.setItem(12, createCreateRoomButton());
        inventory.setItem(14, createRoomListButton());
        inventory.setItem(16, createLeaderboardButton());
        inventory.setItem(22, createGuideButton());
        inventory.setItem(36, createBackButton());
    }

    private org.bukkit.inventory.ItemStack createQuickMatchButton() {
        return createItem(Material.NETHER_BRICK,
                "   §8[§x§F§F§7§C§0§0▶ §x§F§F§9§0§2§0快§x§F§F§A§4§4§0速§x§C§C§5§0§2§0匹§x§6§6§1§9§0§0配§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a自动寻找等待中的板砖守卫战房间",
                "§f- §6没有房间时会直接创建公开房间",
                "§f- §7命令: §e/gamefunxiao command quick brick_guard",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击匹配");
    }

    private org.bukkit.inventory.ItemStack createCreateRoomButton() {
        return createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A✚ §x§F§F§9§0§2§0创§x§F§F§A§4§4§0建§x§C§C§5§0§2§0房§x§6§6§1§9§0§0间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a进入板砖守卫战自己的创建菜单",
                "§f- §b人数、公开状态会按当前房间系统处理",
                "§f- §7命令: §e/gamefunxiao command create brick_guard 16",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击打开");
    }

    private org.bukkit.inventory.ItemStack createRoomListButton() {
        return createItem(Material.ENDER_EYE,
                "   §8[§x§8§8§D§D§F§F👁 §x§F§F§9§0§2§0房§x§F§F§A§4§4§0间§x§C§C§5§0§2§0列§x§6§6§1§9§0§0表§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a只显示雨云 · 板砖守卫战房间",
                "§f- §b等待房间可加入，进行中按房间系统旁观",
                "§f- §7命令: §e/gamefunxiao command rooms brick_guard",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private org.bukkit.inventory.ItemStack createLeaderboardButton() {
        return createItem(Material.RED_GLAZED_TERRACOTTA,
                "   §8[§x§F§F§D§7§0§0🏆 §x§F§F§9§0§2§0板§x§F§F§A§4§4§0砖§x§C§C§5§0§2§0战§x§6§6§1§9§0§0绩§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a查看板砖守卫战独立排行榜",
                "§f- §7不会混入猎人游戏或幸运之柱数据",
                "§f- §7命令: §e/gamefunxiao command menu brickguardleaderboard",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private org.bukkit.inventory.ItemStack createGuideButton() {
        return createItem(Material.WRITABLE_BOOK,
                "   §8[§x§F§F§B§B§6§6📖 §x§F§F§9§0§2§0玩§x§F§F§A§4§4§0法§x§C§C§5§0§2§0说§x§6§6§1§9§0§0明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §6板砖队: 挖矿、交易、守核心方块",
                "§f- §c下界砖队: 保护核心玩家并进攻核心",
                "§f- §d核心玩家会全局发光并拥有强化属性",
                "§f- §7等待大厅第一格指南针用于选择队伍",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击向聊天框发送简要规则");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 10 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.72f, 0.78f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.58f, 1.28f);
                player.closeInventory();
                plugin.getRoomManager().quickMatch(player, GameMode.BRICK_GUARD.getId());
            }
            case 12 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.22f);
                new org.gamefunxiao.menu.hunter.CreateRoomMenu(plugin, player, MenuSection.BRICK_GUARD).open();
            }
            case 14 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.72f, 1.1f);
                plugin.getMenuManager().openBrickGuardRoomListMenu(player);
            }
            case 16 -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.08f);
                plugin.getMenuManager().openBrickGuardLeaderboardMenu(player);
            }
            case 22 -> {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.75f, 1.15f);
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.35f, 1.45f);
                sendGuideLines();
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
            default -> {
            }
        }
    }

    private void sendGuideLines() {
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.guide_header"));
        player.sendMessage("§8· §6板砖队 §7守护红色陶瓦核心，挖矿交易并进攻核心玩家。");
        player.sendMessage("§8· §x§6§6§1§9§0§0下界砖队 §7保护核心玩家，推进并摧毁板砖核心。");
        player.sendMessage("§8· §d核心玩家 §7开局获得强化和发光效果，死亡会触发全队危机。");
        player.sendMessage("§8· §e胜利 §7取决于核心击破、全员击杀或一小时超时平局。");
    }

    private void createDedicatedRoom(int maxPlayers) {
        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            playErrorSound();
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("room.already_in_room"));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
        plugin.getRoomManager().createConfiguredRoom(player, GameMode.BRICK_GUARD, maxPlayers, true, new HashSet<>());
    }
}
