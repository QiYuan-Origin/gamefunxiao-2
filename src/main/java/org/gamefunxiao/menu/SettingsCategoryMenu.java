package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class SettingsCategoryMenu extends BaseMenu {

    public SettingsCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ GameFun 设置分类 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.COMPARATOR,
                "§x§8§8§D§D§F§F⚙ §x§9§9§E§E§F§F个§x§A§A§F§F§F§F人§x§B§B§F§F§E§E设§x§C§C§F§F§D§D置§x§D§D§F§F§C§C分§x§E§E§F§F§B§B类",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b先选择你要设置的分类",
                "§f- §a这样后面扩展更多设置也不会乱",
                "§8· · · · · · · · · · · · · ·"));

        boolean compact = plugin.getPlayerDataManager().isCompactMessages(player.getUniqueId());
        inventory.setItem(43, createItem(Material.BELL,
                "   §8[§x§8§8§D§D§F§F⚙ §x§A§A§F§F§F§F消§x§B§B§F§F§E§E息§x§C§C§F§F§D§D与§x§D§D§F§F§C§C音§x§E§E§F§F§B§B效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b当前消息频繁度: " + (compact ? "§b正常" : "§d繁多"),
                "§f- §a点击直接切换",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(21, createItem(Material.COMPASS,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4猎§x§F§F§8§8§8§8人§x§F§F§6§6§C§C游§x§F§F§4§4§F§F戏§x§D§D§6§6§F§F特§x§B§B§8§8§F§F效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里是猎人游戏自己的特效分类",
                "§f- §a点击进入分类",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(23, createItem(Material.END_CRYSTAL,
                "   §8[§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7幸§x§F§F§F§F§9§9运§x§D§D§F§F§B§B之§x§B§B§F§F§D§D柱§x§9§9§F§F§F§F特§x§7§7§E§E§F§F效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里是幸运之柱自己的特效分类",
                "§f- §a点击进入分类",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(25, createItem(Material.BRICK,
                "   §8[§x§F§F§7§C§0§0▣ §x§F§F§9§0§2§0板§x§F§F§A§4§4§0砖§x§C§C§5§0§2§0设§x§6§6§1§9§0§0置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §6进入板砖守卫战独立导航",
                "§f- §a房间、队伍与玩法入口在这里统一处理",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 43 -> {
                boolean compact = !plugin.getPlayerDataManager().isCompactMessages(player.getUniqueId());
                plugin.getPlayerDataManager().setMessageFrequency(player.getUniqueId(), compact ? "normal" : "chatty");
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.success")
                        + (compact ? "§b消息频繁程度已切换为 §f正常" : "§d消息频繁程度已切换为 §f繁多"));
                player.playSound(player.getLocation(), compact ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.BLOCK_NOTE_BLOCK_CHIME,
                        0.75f, compact ? 1.1f : 1.35f);
                setupItems();
                player.updateInventory();
            }
            case 21 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.65f);
                plugin.getMenuManager().openHunterGameEffectCategoryMenu(player);
            }
            case 23 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.78f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.55f, 1.3f);
                plugin.getMenuManager().openLuckyPillarsEffectCategoryMenu(player);
            }
            case 25 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.68f, 0.9f);
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.42f, 1.35f);
                plugin.getMenuManager().openBrickGuardMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
        }
    }
}
