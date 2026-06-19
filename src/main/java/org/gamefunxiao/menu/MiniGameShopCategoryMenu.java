package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class MiniGameShopCategoryMenu extends BaseMenu {

    public MiniGameShopCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 商城玩法分类 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.EMERALD,
                "§x§5§5§F§F§A§A✦ §x§7§7§F§F§B§B商§x§9§9§F§F§C§C城§x§B§B§F§F§D§D玩§x§D§D§F§F§E§E法§x§F§F§F§F§F§F分§x§D§D§F§F§E§E类",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b先选择要购买哪种玩法的物品",
                "§f- §a每种玩法的物品、特效和交易都独立显示",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(20, createItem(Material.COMPASS,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§B§B§3§3猎§x§F§F§9§9§6§6人§x§F§F§7§7§9§9游§x§F§F§5§5§C§C戏§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b当前可购买: 猎人胜利特效、击杀特效",
                "§f- §a点击进入猎人游戏商品分类",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(22, createItem(Material.END_CRYSTAL,
                "   §8[§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7幸§x§F§F§F§F§9§9运§x§D§D§F§F§B§B之§x§B§B§F§F§D§D柱§x§9§9§F§F§F§F商§x§7§7§E§E§F§F店§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b当前可购买: 幸运之柱胜利特效",
                "§f- §a点击进入幸运之柱商品分类",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 20 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.55f);
                plugin.getMenuManager().openHunterGameShopCategoryMenu(player);
            }
            case 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.72f);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55f, 1.95f);
                plugin.getMenuManager().openLuckyPillarsShopCategoryMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
        }
    }
}
