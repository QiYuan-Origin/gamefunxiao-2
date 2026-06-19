package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.HashSet;

public class CommandGatewayMenu extends BaseMenu {

    public CommandGatewayMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ GameFun 命令入口 ✦", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.COMMAND_BLOCK,
                "§x§7§D§F§F§C§8✦ §x§8§E§F§0§D§B命§x§9§F§E§1§E§E令§x§B§0§D§2§F§F入§x§C§8§C§4§F§A口 §x§E§0§B§6§F§5✦",
                "§8· · · · · · · · · · · · · ·",
                "§f从 §b/gamefunxiao command §f打开的界面",
                "§f这里进入的菜单，点 §c返回 §f会回到这里",
                "§f也可以直接用命令模拟菜单按钮",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(10, createItem(Material.NETHER_STAR,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§0§0主§x§F§F§8§0§0§0菜§x§F§F§5§5§0§0单§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开 GameFun 主导航",
                "§f- §7命令: §e/gamefunxiao command menu main",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(11, createItem(Material.IRON_SWORD,
                "   §8[§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开猎人游戏导航",
                "§f- §b可选择模式、房间、排行榜",
                "§f- §7命令: §e/gamefunxiao command menu hunter",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(12, createItem(Material.BRICK,
                "   §8[§x§F§F§7§C§0§0▣ §x§F§F§9§0§2§0板§x§F§F§A§4§4§0砖§x§C§C§5§0§2§0守§x§6§6§1§9§0§0卫战§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开雨云 · 板砖守卫战导航",
                "§f- §6快速匹配、创建房间、排行榜都在这里",
                "§f- §7命令: §e/gamefunxiao command menu brickguard",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(14, createItem(Material.ENDER_EYE,
                "   §8[§x§5§5§F§F§F§F👁 §x§7§7§F§F§D§D查§x§9§9§F§F§B§B看§x§B§B§F§F§9§9房§x§D§D§F§F§7§7间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开所有房间列表",
                "§f- §b左键加入，游戏中可旁观",
                "§f- §7命令: §e/gamefunxiao command rooms",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(15, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A✚ §x§7§7§F§F§B§B创§x§9§9§F§F§C§C建§x§B§B§F§F§D§D房§x§D§D§F§F§E§E间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开创建房间菜单",
                "§f- §d也可直接: §ecommand create <模式> <人数>",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(16, createItem(Material.GOLD_INGOT,
                "   §8[§x§F§F§D§7§0§0🏆 §x§F§F§B§B§0§0排§x§F§F§9§9§0§0行§x§F§F§7§7§0§0榜§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开排行榜菜单",
                "§f- §7命令: §e/gamefunxiao command leaderboard",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(20, createQuickButton(GameMode.CLASSIC, Material.COMPASS,
                "§x§5§5§F§F§5§5经典模式", "模拟猎人菜单经典模式按钮"));
        inventory.setItem(21, createQuickButton(GameMode.RANDOM_COMPASS, Material.RECOVERY_COMPASS,
                "§x§F§F§C§C§7§7随机指南针", "模拟猎人菜单随机指南针按钮"));
        inventory.setItem(22, createQuickButton(GameMode.SWAP, Material.ENDER_PEARL,
                "§x§D§D§8§8§F§F互换模式", "模拟猎人菜单互换模式按钮"));
        inventory.setItem(23, createQuickButton(GameMode.FLASH, Material.BLAZE_POWDER,
                "§x§F§F§E§E§6§6闪光模式", "模拟猎人菜单闪光模式按钮"));
        inventory.setItem(24, createQuickButton(GameMode.END_FLASH, Material.END_CRYSTAL,
                "§x§D§D§A§A§F§F终章 · 闪光", "模拟猎人菜单终章闪光按钮"));
        inventory.setItem(25, createQuickButton(GameMode.FLASH_TOURNAMENT, Material.RED_BANNER,
                "§x§F§F§F§F§9§9闪光 §c§l赛事", "进入静默赛事：选世界、无职业前缀、67人"));
        inventory.setItem(31, createQuickButton(GameMode.BRICK_GUARD, Material.NETHER_BRICK,
                "§x§F§F§7§C§0§0板砖 §x§6§6§1§9§0§0守卫战", "模拟板砖守卫战快速匹配按钮"));

        inventory.setItem(37, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A✦ §x§7§7§F§F§B§B小§x§9§9§F§F§C§C游§x§B§B§F§F§D§D戏§x§D§D§F§F§E§E商§x§F§F§F§F§F§F城§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a打开小游戏商城",
                "§f- §7命令: §e/gamefunxiao command shop",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(38, createItem(Material.COMPARATOR,
                "   §8[§x§8§8§D§D§F§F⚙ §x§A§A§F§F§F§F个人设置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b打开个人游戏设置",
                "§f- §7命令: §e/gamefunxiao command settings",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(39, createItem(Material.FIREWORK_ROCKET,
                "   §8[§x§F§F§B§B§5§5✦ §x§F§F§D§D§8§8胜§x§D§D§F§F§A§A利§x§B§B§F§F§C§C特§x§9§9§F§F§E§E效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a左键打开特效商城",
                "§f- §d右键打开已拥有特效设置",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(40, createItem(Material.WRITABLE_BOOK,
                "   §8[§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F终章个人Kit§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b打开个人终章 Kit 摆放",
                "§f- §7命令: §e/gamefunxiao command personalkit",
                "§8· · · · · · · · · · · · · ·"));
        if (player.hasPermission("gamefunxiao.admin")) {
            inventory.setItem(41, createItem(Material.END_CRYSTAL,
                    "   §8[§x§B§B§8§8§F§F✦ §x§F§F§D§D§A§A终章Kit管理§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c管理员入口",
                    "§f- §a打开终章闪光 Kit 调试菜单",
                    "§f- §7命令: §e/gamefunxiao command endflashkit",
                    "§8· · · · · · · · · · · · · ·"));
        }

        inventory.setItem(49, createPlainCloseButton());
    }

    private org.bukkit.inventory.ItemStack createQuickButton(GameMode mode, Material material, String coloredName, String note) {
        return createItem(material,
                "   §8[§x§5§5§F§F§A§A▶ " + coloredName + "§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击后直接进入等待房间",
                "§f- §e动作: §b" + note,
                "§f- §7命令: §e/gamefunxiao command quick " + mode.getId(),
                "§8· · · · · · · · · · · · · ·");
    }


    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 10 -> openMenu("main");
            case 11 -> openMenu("hunter");
            case 12 -> openMenu("brickguard");
            case 14 -> openMenu("rooms");
            case 15 -> openMenu("create");
            case 16 -> openMenu("leaderboard");
            case 20 -> quickMatch(GameMode.CLASSIC);
            case 21 -> quickMatch(GameMode.RANDOM_COMPASS);
            case 22 -> quickMatch(GameMode.SWAP);
            case 23 -> quickMatch(GameMode.FLASH);
            case 24 -> quickMatch(GameMode.END_FLASH);
            case 25 -> quickMatch(GameMode.FLASH_TOURNAMENT);
            case 31 -> quickMatch(GameMode.BRICK_GUARD);
            case 37 -> openMenu("shop");
            case 38 -> openMenu("settings");
            case 39 -> openMenu(event.isRightClick() ? "victorysettings" : "victoryshop");
            case 40 -> openMenu("personalkit");
            case 41 -> {
                if (player.hasPermission("gamefunxiao.admin")) {
                    openMenu("endflashkit");
                }
            }
            case 49 -> handlePlainCloseAction();
            default -> playClickSound();
        }
    }

    private void openMenu(String menuId) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.45f);
        plugin.getMenuManager().openMenuFromCommand(player, menuId);
    }

    private void quickMatch(GameMode mode) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.78f, 1.55f);
        player.closeInventory();
        plugin.getRoomManager().quickMatch(player, mode.getId());
    }

}
