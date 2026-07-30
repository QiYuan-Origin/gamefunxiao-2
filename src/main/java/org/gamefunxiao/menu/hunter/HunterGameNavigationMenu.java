package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class HunterGameNavigationMenu extends BaseMenu {

    public HunterGameNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l⚔ 猎人游戏 - 导航 ⚔", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.IRON_SWORD,
            "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏 §x§F§F§6§6§0§0⚔",
            "§8· · · · · · · · · · · · · ·",
            "§f选择你想要的操作",
            "§8· · · · · · · · · · · · · ·"));

        // 左上角 - 排行榜
        inventory.setItem(0, createLeaderboardButton());
        inventory.setItem(2, createPerformanceButton());

        // 右上角 - 查看所有房间
        inventory.setItem(8, createRoomListButton());

        // 中间 - 游戏模式选择
        inventory.setItem(20, createClassicModeButton());
        inventory.setItem(21, createRandomCompassModeButton());
        inventory.setItem(22, createSwapModeButton());
        inventory.setItem(23, createNoItemModeButton());
        inventory.setItem(24, createSurvivalModeButton());
        inventory.setItem(30, createFlashModeButton());
        inventory.setItem(31, createFlashTournamentModeButton());
        inventory.setItem(32, createEndFlashModeButton());

        // 右下角 - 创建房间
        inventory.setItem(53, createCreateRoomButton());
        if (player.hasPermission("gamefunxiao.admin")) {
            inventory.setItem(49, createEndFlashKitAdminButton());
        }

        // 返回按钮
        inventory.setItem(45, createBackButton());

    }

    private ItemStack createLeaderboardButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§D§7§0§0🏆 §x§F§F§B§B§0§0排§x§F§F§9§9§0§0行§x§F§F§7§7§0§0榜§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e查看各类排行榜");
            lore.add("§f- §a通关次数排行");
            lore.add("§f- §b最快通关时间");
            lore.add("§f- §d游玩次数统计");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6天榜/周榜/年榜/总榜");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击查看");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPerformanceButton() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§8§8§D§D§F§F✦ §x§A§A§E§E§F§F表§x§C§C§F§F§F§F现§x§F§F§D§D§5§5值§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c猎人表现 §7和 §a猎物表现 §7分开计算");
            lore.add("§f- §e查看段位、加扣分规则、防刷限制");
            lore.add("§f- §b也能快速打开表现排行榜");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击查看");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRoomListButton() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§5§5§F§F§F§F👁 §x§7§7§F§F§D§D查§x§9§9§F§F§B§B看§x§B§B§F§F§9§9房§x§D§D§F§F§7§7间§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e查看当前所有房间");
            lore.add("§f- §a等待中的房间");
            lore.add("§f- §c进行中的房间");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b可以加入或旁观");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击查看");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createClassicModeButton() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§5§5§F§F§5§5⚔ §x§7§7§F§F§7§7经§x§9§9§F§F§9§9典§x§B§B§F§F§B§B模§x§D§D§F§F§D§D式§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a击败末影龙");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b猎人获得追踪指南针");
            lore.add("§f- §b显示最近猎物距离");
            lore.add("§f- §d连续扔掉指南针两次");
            lore.add("§f- §d可获得一次TP机会");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6奖励: 猎人150 猎物300");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速匹配");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRandomCompassModeButton() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§A§A§5§5⏱ §x§F§F§C§C§7§7随§x§F§F§E§E§9§9机§x§D§D§F§F§9§9指§x§B§B§F§F§7§7南§x§9§9§F§F§5§5针§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a击败末影龙");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b每5分钟随机一位玩家");
            lore.add("§f- §b获得特殊指南针");
            lore.add("§f- §d使用后敌方获得发光");
            lore.add("§f- §d和缓慢+饥饿效果");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6奖励: 猎人150 猎物300");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速匹配");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSwapModeButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§D§D§5§5§F§F🔄 §x§C§C§7§7§F§F互§x§B§B§9§9§F§F换§x§A§A§B§B§F§F模§x§9§9§D§D§F§F式§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a击败末影龙");
            lore.add("§f- §c需要两位猎物");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b每1分钟两位猎物互换");
            lore.add("§f- §b共享背包和位置");
            lore.add("§f- §d猎人有追踪指南针");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6奖励: 猎人300 猎物400");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速匹配");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoItemModeButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§5§5§5§5✖ §x§F§F§7§7§7§7无§x§F§F§9§9§9§9有§x§F§F§B§B§B§B模§x§F§F§D§D§D§D式§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a击败末影龙");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c纯正的猎人游戏!");
            lore.add("§f- §c没有指南针");
            lore.add("§f- §c没有TP能力");
            lore.add("§f- §b全凭实力追踪!");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6奖励: 猎人500 猎物600");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速匹配");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSurvivalModeButton() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§E§E§5§5⏰ §x§E§E§F§F§7§7存§x§D§D§F§F§9§9活§x§C§C§F§F§B§B模§x§B§B§F§F§D§D式§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a存活30分钟");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b猎人有追踪指南针");
            lore.add("§f- §b可以TP到队友身边");
            lore.add("§f- §d猎物需要躲避追杀");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6奖励: 猎人50 猎物100");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速匹配");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFlashModeButton() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§F§F§9§9⚡ §x§F§F§E§E§6§6闪§x§F§F§D§D§3§3光§x§F§F§C§C§0§0公§x§F§F§B§B§0§0式§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e通关目标: §a直接开局厮杀并完成通关");
            lore.add("§f- §6不会进入世界选择，倒计时结束直接开始");
            lore.add("§f- §b同一时间全服只会存在一个闪光公式房间");
            lore.add("§f- §d若已有闪光公式进行中，点击会直接加入旁观");
            lore.add("§f- §c满人可达 64 人，猎人会分成三圈包围猎物");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速进入");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEndFlashModeButton() {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§B§B§8§8§F§F✦ §x§C§8§9§2§F§F终§x§D§4§9§C§F§F章 §x§E§1§A§6§F§F· §x§E§D§B§0§F§F闪§x§F§A§B§A§F§F光§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §d继承闪光公式全部特殊机制");
            lore.add("§f- §5只创建末地游戏世界，不创建主世界和下界");
            lore.add("§f- §b猎物在末地主岛或黑曜石平台附近开局");
            lore.add("§f- §c猎人依旧围三圈包围猎物");
            lore.add("§f- §e猎人/猎物 Kit 会从 OP 配置的池子中随机");
            lore.add("§f- §d右键可以编辑你的个人终章 Kit 摆放");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a左键快速进入");
            lore.add("§f- §b右键编辑个人 Kit");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFlashTournamentModeButton() {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§F§F§9§9⚡ §x§F§F§E§E§6§6闪§x§F§F§D§D§3§3光 §c§l· 赛事§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e继承闪光玩法，但猎物会先选择世界");
            lore.add("§f- §c§l赛事 §f- §7无职业头衔、无TAB职业、无游戏记分板");
            lore.add("§f- §b正式开始后只允许 §f/teammsg §b或简单语音聊天");
            lore.add("§f- §d猎人互相攻击会造成伤害");
            lore.add("§f- §c死亡必定掉落装备，最大人数 §f67 §c人");
            lore.add("§f- §e指南针只追踪猎物，不显示距离也没有传送/背包");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击快速进入赛事");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCreateRoomButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§5§5§F§F§A§A✚ §x§7§7§F§F§B§B创§x§9§9§F§F§C§C建§x§B§B§F§F§D§D房§x§D§D§F§F§E§E间§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e创建自定义房间");
            lore.add("§f- §a选择游戏模式");
            lore.add("§f- §b设置最大人数");
            lore.add("§f- §d添加修饰符");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c自定义房间不计入排行榜");
            lore.add("§f- §6(管理员房间除外)");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击创建");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEndFlashKitAdminButton() {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F终§x§F§F§D§D§A§A章§x§E§E§C§C§F§FKit§x§B§B§8§8§F§F调试§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §d管理终章 · 闪光的猎人/猎物 Kit");
            lore.add("§f- §a可从当前背包快速保存 Kit");
            lore.add("§f- §b可追加主手物品、调整概率");
            lore.add("§f- §c删除需要 Shift 点击，避免误删");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击打开管理员菜单");
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
            case 0 -> {
                // 排行榜
                playClickSound();
                plugin.getMenuManager().openLeaderboardMenu(player);
            }
            case 2 -> {
                // 表现值与段位
                playClickSound();
                plugin.getMenuManager().openHunterPerformanceMenu(player);
            }
            case 8 -> {
                // 查看房间
                playClickSound();
                plugin.getMenuManager().openRoomListMenu(player);
            }
            case 20 -> {
                // 经典模式快速匹配
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "classic");
            }
            case 21 -> {
                // 随机指南针模式
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "random_compass");
            }
            case 22 -> {
                // 互换模式
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "swap");
            }
            case 23 -> {
                // 无有模式
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "no_item");
            }
            case 24 -> {
                // 存活模式
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "survival");
            }
            case 30 -> {
                // 闪光模式
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "flash");
            }
            case 31 -> {
                // 闪光 · 赛事
                playSelectSound();
                plugin.getRoomManager().quickMatch(player, "flash_tournament");
            }
            case 32 -> {
                // 终章 · 闪光
                if (event.isRightClick()) {
                    playClickSound();
                    new EndFlashPersonalKitMenu(plugin, player).open();
                } else {
                    playSelectSound();
                    plugin.getRoomManager().quickMatch(player, "end_flash");
                }
            }
            case 53 -> {
                // 创建房间
                playClickSound();
                plugin.getMenuManager().openCreateRoomMenu(player);
            }
            case 49 -> {
                if (player.hasPermission("gamefunxiao.admin")) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.35f);
                    plugin.getMenuManager().openEndFlashKitAdminMenu(player);
                }
            }
            case 45 -> {
                // 返回
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
        }
    }
}
