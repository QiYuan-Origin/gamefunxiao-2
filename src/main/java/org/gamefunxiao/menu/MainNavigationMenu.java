package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class MainNavigationMenu extends BaseMenu {

    public MainNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ GameFun 小游戏导航 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品 - 第一行中间
        inventory.setItem(4, createTitleItem(Material.NETHER_STAR,
            "§x§F§F§D§7§0§0✦ §x§F§F§A§A§0§0G§x§F§F§8§0§0§0a§x§F§F§5§5§0§0m§x§F§F§2§A§0§0e§x§D§4§0§0§F§FF§x§A§A§0§0§F§Fu§x§8§0§0§0§F§Fn §x§F§F§D§7§0§0✦",
            "§8· · · · · · · · · · · · · ·",
            "§f欢迎来到 GameFun 小游戏!",
            "§f这里现在按玩法分成不同分区入口",
            "§f猎人游戏和幸运之柱都会进自己的二级菜单",
            "§8· · · · · · · · · · · · · ·"));

        // 主菜单直达入口
        inventory.setItem(8, createShopButton());
        inventory.setItem(20, createHunterGameButton());
        inventory.setItem(22, createLuckyPillarsButton());
        inventory.setItem(24, createBrickGuardButton());
        inventory.setItem(44, createSettingsButton());

        // 关闭按钮
        inventory.setItem(40, createCloseButton());
    }

    private ItemStack createHunterGameButton() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e完整的猎人玩法总入口");
            lore.add("§f- §a创建房间、房间列表、排行榜都只看猎人玩法");
            lore.add("§f- §c不会再混进幸运之柱这些小游戏");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b多种游戏模式可选");
            lore.add("§f- §d丰富的修饰符系统");
            lore.add("§f- §6排行榜竞技");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击进入");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLuckyPillarsButton() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a多人乱斗小游戏分区");
            lore.add("§f- §b创建房间、房间列表、排行榜都只属于幸运之柱");
            lore.add("§f- §7普通模式和 PVP 大佬也在它自己的菜单里");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击进入");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBrickGuardButton() {
        ItemStack item = new ItemStack(Material.BRICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§f§f§7§c§0§0板砖 · 守卫战");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§7- 我无法介绍");
            lore.add("§7- 但你必须破坏对面的核心你才能获得胜利");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6独立玩法分区");
            lore.add("§f- §c板砖队 §7vs §x§6§6§1§9§0§0下界砖队");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击进入");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }





    private ItemStack createShopButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§5§5§F§F§A§A✦ §x§7§7§F§F§B§B小§x§9§9§F§F§C§C游§x§B§B§F§F§D§D戏§x§D§D§F§F§E§E商§x§F§F§F§F§F§F城§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a使用 §e" + plugin.getConfigManager().getMiniGameCurrencyName() + " §a购买外观");
            lore.add("§f- §d当前开放: 猎人胜利特效");
            lore.add("§f- §7只放已经完整接入游戏的商品");
            lore.add("§f- §b当前余额: §e" + plugin.getPlayerDataManager().getCoins(player.getUniqueId()));
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击进入商城");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSettingsButton() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§8§8§D§D§F§F⚙ §x§9§9§E§E§F§F个§x§A§A§F§F§F§F人§x§B§B§F§F§E§E设§x§C§C§F§F§D§D置§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b查看和调整你的小游戏设置");
            lore.add("§f- §d可快速切换已拥有的胜利特效");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击打开设置");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 8 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
                plugin.getMenuManager().openMiniGameShopCategoryMenu(player);
            }
            case 20 -> {
                playClickSound();
                plugin.getMenuManager().openHunterGameMenu(player);
            }
            case 22 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.62f, 1.72f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.35f, 1.8f);
                plugin.getMenuManager().openLuckyPillarsMenu(player);
            }
            case 24 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_COPPER_BULB_TURN_ON, 0.72f, 0.75f);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_GRINDSTONE_USE, 0.48f, 1.25f);
                plugin.getMenuManager().openBrickGuardMenu(player);
            }
            case 44 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.45f);
                plugin.getMenuManager().openSettingsCategoryMenu(player);
            }
            case 40 -> {
                handleCloseButtonAction();
            }
        }
    }

}
