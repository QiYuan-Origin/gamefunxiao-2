package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class DeathSwapShopCategoryMenu extends BaseMenu {

    private final boolean returnToNavigation;

    public DeathSwapShopCategoryMenu(GameFunXiao plugin, Player player) {
        this(plugin, player, false);
    }

    public DeathSwapShopCategoryMenu(GameFunXiao plugin, Player player, boolean returnToNavigation) {
        super(plugin, player, "§0§l⟲ 死亡互换商店 ⟲", 45);
        this.returnToNavigation = returnToNavigation;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.EMERALD,
                "§x§F§F§6§6§0§0⟲ §x§F§F§8§8§2§2死§x§F§F§A§A§4§4亡§x§F§F§C§C§6§6互§x§F§F§E§E§8§8换§x§F§F§F§F§A§A商§x§F§F§D§D§7§7店",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里是死亡互换自己的商城分类",
                "§f- §a现在可购买胜利样式",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createItem(Material.FIREWORK_ROCKET,
                "   §8[§x§F§F§6§6§0§0⟲ §x§F§F§8§8§2§2购§x§F§F§A§A§4§4买§x§F§F§C§C§6§6胜§x§F§F§E§E§8§8利§x§F§F§F§F§A§A样§x§F§F§D§D§7§7式§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b死亡互换结算时在胜者位置播放",
                "§f- §a点击打开购买界面",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.36f);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55f, 1.78f);
                new DeathSwapVictoryEffectShopMenu(plugin, player, returnToNavigation).open();
            }
            case 36 -> {
                playClickSound();
                if (returnToNavigation) {
                    plugin.getMenuManager().openDeathSwapMenu(player);
                } else {
                    plugin.getMenuManager().openMiniGameShopCategoryMenu(player);
                }
            }
            default -> {
            }
        }
    }
}
