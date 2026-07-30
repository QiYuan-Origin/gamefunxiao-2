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
            "§x§F§F§D§7§0§0[ GameFun 小游戏 ]",
            "§8· · · · · · · · · · · · · ·",
            "§f欢迎来到 GameFun 小游戏!",
            "§f选择你想游玩的玩法",
            "§f房间、排行和创建入口都放在对应菜单里",
            "§8· · · · · · · · · · · · · ·"));

        // 主菜单直达入口
        inventory.setItem(8, createShopButton());
        inventory.setItem(20, createHunterGameButton());
        inventory.setItem(22, createDeathSwapButton());
        inventory.setItem(24, createLuckyPillarsButton());
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
            meta.setDisplayName("   §8[§x§F§F§D§D§5§5✦ 幸运之柱§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a多人乱斗小游戏");
            lore.add("§f- §b进入房间、查看排行、创建房间");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击进入");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDeathSwapButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§6§6§0§0⟲ §x§F§F§9§9§3§3死§x§F§F§B§B§6§6亡§x§F§F§D§D§5§5互§x§F§F§F§F§9§9换§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a周期互换所有存活玩家的位置");
            lore.add("§f- §e出生点附近会筛村庄");
            lore.add("§f- §c每人只有一次生命，死亡后旁观");
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
            case 24 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.62f, 1.72f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.35f, 1.8f);
                plugin.getMenuManager().openLuckyPillarsMenu(player);
            }
            case 22 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.74f, 1.0f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.34f, 1.0f);
                plugin.getMenuManager().openDeathSwapMenu(player);
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
