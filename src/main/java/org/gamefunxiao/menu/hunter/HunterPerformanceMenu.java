package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.data.PlayerData;
import org.gamefunxiao.game.HunterPerformanceRank;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class HunterPerformanceMenu extends BaseMenu {

    public HunterPerformanceMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l⚔ 猎人游戏表现值 ⚔", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.NETHERITE_SWORD,
                "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9表§x§C§C§F§F§9§9现§x§8§8§D§D§F§F值",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e这里能看段位、规则和防刷限制",
                "§f- §c猎人表现 §7和 §a猎物表现 §7分开计算",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(10, createMyRankButton());
        inventory.setItem(12, createHunterRuleButton());
        inventory.setItem(14, createPreyRuleButton());
        inventory.setItem(16, createRankTableButton());
        inventory.setItem(28, createHunterLeaderboardButton());
        inventory.setItem(30, createPreyLeaderboardButton());
        inventory.setItem(32, createAntiFarmButton());
        inventory.setItem(36, createBackButton());
    }

    private ItemStack createMyRankButton() {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int hunter = data.getHunterPointsTotal();
        int prey = data.getPreyPointsTotal();
        return createButton(Material.PLAYER_HEAD,
                "§x§8§8§D§D§F§F我的段位",
                "§f- §c猎人表现: §6" + hunter + " §8| " + HunterPerformanceRank.coloredHunterRank(hunter),
                "§f- §a猎物表现: §6" + prey + " §8| " + HunterPerformanceRank.coloredPreyRank(prey),
                "§f- §7两边分开涨，适合看你更像猎人还是猎物");
    }

    private ItemStack createHunterRuleButton() {
        return createButton(Material.IRON_SWORD,
                "§x§F§F§5§5§5§5猎人表现规则",
                "§f- §c猎人胜利 §6+5",
                "§f- §c击杀猎物 §6+6 §7，最终击杀额外 §6+4",
                "§f- §c对猎物每造成30点有效伤害 §6+1",
                "§f- §7单局最多获得 §e25 §7表现");
    }

    private ItemStack createPreyRuleButton() {
        return createButton(Material.RABBIT_FOOT,
                "§x§5§5§F§F§A§A猎物表现规则",
                "§f- §a猎物胜利 §6+8",
                "§f- §a每存活6分钟 §6+2 §7，反杀猎人 §6+3",
                "§f- §a移动每800格 §6+1",
                "§f- §7单局最多获得 §e30 §7表现");
    }

    private ItemStack createRankTableButton() {
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §70 §8→ §x§C§C§8§8§5§5木猎人 §7/ §x§C§C§8§8§5§5木猎物");
        lore.add("§f- §7100 §8→ §x§A§A§A§A§A§A石猎人 §7/ §x§A§A§A§A§A§A石猎物");
        lore.add("§f- §7250 §8→ §x§D§D§D§D§D§D铁猎人 §7/ §x§D§D§D§D§D§D铁猎物");
        lore.add("§f- §7500 §8→ §x§F§F§D§D§5§5金猎人 §7/ §x§F§F§D§D§5§5金猎物");
        lore.add("§f- §7900 §8→ §x§5§5§F§F§F§F钻石猎人 §7/ §x§5§5§F§F§F§F钻石猎物");
        lore.add("§f- §71400 §8→ §x§D§D§D§D§D§D合金猎人 §7/ §x§D§D§D§D§D§D合金猎物");
        lore.add("§f- §72000 §8→ §x§8§8§5§5§F§F末影猎人 §7/ §x§8§8§5§5§F§F末影猎物");
        lore.add("§f- §73000 §8→ §x§C§C§5§5§F§F龙王猎人 §7/ §x§C§C§5§5§F§F龙王猎物");
        lore.add("§8· · · · · · · · · · · · · ·");
        return createButton(Material.NETHERITE_INGOT, "§x§F§F§D§D§5§5段位表", lore);
    }

    private ItemStack createHunterLeaderboardButton() {
        return createButton(Material.GOLD_INGOT,
                "§x§F§F§7§7§3§3猎人表现榜",
                "§f- §c查看猎人表现排行榜",
                "§f- §7按猎人身份的表现值排序");
    }

    private ItemStack createPreyLeaderboardButton() {
        return createButton(Material.EMERALD,
                "§x§5§5§F§F§A§A猎物表现榜",
                "§f- §a查看猎物表现排行榜",
                "§f- §7按猎物身份的表现值排序");
    }

    private ItemStack createAntiFarmButton() {
        return createButton(Material.SHIELD,
                "§x§8§8§D§D§F§F防刷限制",
                "§f- §7同一猎物反复被杀，只算 §e1 §7次击杀表现",
                "§f- §7同一猎人被反杀，最多算 §e2 §7次",
                "§f- §75分钟内结束，本局获得表现会减半",
                "§f- §7胜利但移动少于200格，猎物表现减半",
                "§f- §c死亡/主动退出会扣表现");
    }

    private ItemStack createButton(Material material, String name, String... lines) {
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        for (String line : lines) {
            lore.add(line);
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        return createButton(material, name, lore);
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[" + name + "§8]");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        switch (slot) {
            case 10, 12, 14, 16, 32 -> playClickSound();
            case 28 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "hunter_points").open();
            }
            case 30 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "prey_points").open();
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openHunterGameMenu(player);
            }
        }
    }

    @Override
    protected void playOpenSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.62f, 1.0f);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.55f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.45f, 1.0f);
            }
        }, 4L);
    }
}
