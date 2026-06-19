package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class LeaderboardMenu extends BaseMenu {

    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;

    public LeaderboardMenu(GameFunXiao plugin, Player player) {
        this(plugin, player, MenuSection.HUNTER, defaultFilter(MenuSection.HUNTER));
    }

    public LeaderboardMenu(GameFunXiao plugin, Player player, MenuSection menuSection, Set<GameMode> modeFilter) {
        super(plugin, player, resolveTitle(menuSection), 45);
        this.menuSection = menuSection == null ? MenuSection.HUNTER : menuSection;
        this.modeFilter = modeFilter == null ? defaultFilter(this.menuSection) : EnumSet.copyOf(modeFilter);
    }

    private static String resolveTitle(MenuSection section) {
        return switch (section) {
            case LUCKY_PILLARS -> "§0§l🍀 幸运之柱排行榜 🍀";
            case GENERIC, HUNTER -> "§0§l⚔ 猎人排行榜 ⚔";
        };
    }

    private static Set<GameMode> defaultFilter(MenuSection section) {
        return switch (section) {
            case LUCKY_PILLARS -> EnumSet.of(GameMode.LUCKY_PILLARS);
            case GENERIC, HUNTER -> EnumSet.of(
                    GameMode.CLASSIC,
                    GameMode.RANDOM_COMPASS,
                    GameMode.SWAP,
                    GameMode.NO_ITEM,
                    GameMode.SURVIVAL,
                    GameMode.FLASH,
                    GameMode.FLASH_TOURNAMENT,
                    GameMode.END_FLASH,
                    GameMode.CUSTOM
            );
        };
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(getTitleMaterial(), getTitleText(),
                "§8· · · · · · · · · · · · · ·",
                switch (menuSection) {
                    case LUCKY_PILLARS -> "§f这里只看幸运之柱玩法自己的数据";
                    case GENERIC, HUNTER -> "§f这里只看猎人玩法自己的数据";
                },
                "§8· · · · · · · · · · · · · ·"));

        if (menuSection == MenuSection.LUCKY_PILLARS) {
            inventory.setItem(22, createMiniGamePointsButton());
            inventory.setItem(36, createBackButton());
            return;
        }

        inventory.setItem(19, createPassCountButton());
        inventory.setItem(22, createFastestTimeButton());
        inventory.setItem(25, createPlayCountButton());
        inventory.setItem(28, createHunterPointsButton());
        inventory.setItem(31, createPreyPointsButton());
        inventory.setItem(34, createPreyWinsButton());
        inventory.setItem(36, createBackButton());
    }

    private Material getTitleMaterial() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> Material.GOLD_BLOCK;
            case GENERIC, HUNTER -> Material.IRON_SWORD;
        };
    }

    private String getTitleText() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱榜单";
            case GENERIC, HUNTER -> "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9榜§x§C§C§F§F§9§9单";
        };
    }

    private ItemStack createPassCountButton() {
        return createButton(Material.DIAMOND, "§x§5§5§F§F§D§D通关 / 击杀榜",
                "§f- §a查看当前猎人玩法的通关次数",
                "§f- §c也可切到猎人击杀榜");
    }

    private ItemStack createFastestTimeButton() {
        return createButton(Material.CLOCK, "§x§F§F§E§E§5§5最快通关榜",
                "§f- §a只看当前猎人玩法分区的模式",
                "§f- §d右上角还能继续切具体模式");
    }

    private ItemStack createPlayCountButton() {
        return createButton(Material.BOOK, "§x§A§A§F§F§5§5游玩次数榜",
                "§f- §a统计当前猎人玩法分区的开局次数",
                "§f- §7不会混进小游戏数据");
    }

    private ItemStack createHunterPointsButton() {
        return createButton(Material.IRON_SWORD, "§x§F§F§5§5§5§5猎人积分榜",
                "§f- §c只统计猎人玩法里的猎人积分",
                "§f- §7不会串到幸运之柱");
    }

    private ItemStack createPreyPointsButton() {
        return createButton(Material.RABBIT_FOOT, "§x§5§5§F§F§A§A猎物积分榜",
                "§f- §a只统计猎人玩法里的猎物积分",
                "§f- §7更方便看当前模式生态");
    }

    private ItemStack createPreyWinsButton() {
        return createButton(Material.ENDER_EYE, "§x§8§8§D§D§F§F猎物通关榜",
                "§f- §a直接看猎物完成目标的排行榜",
                "§f- §7不再和其他小游戏混在一起");
    }

    private ItemStack createMiniGamePointsButton() {
        return createButton(Material.GOLD_BLOCK, "§x§F§F§D§D§5§5小游戏积分榜",
                "§f- §a只统计幸运之柱经典模式",
                "§f- §7当前分区就是单独的小游戏榜");
    }

    private ItemStack createButton(Material material, String name, String... lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[" + name + "§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            for (String line : lines) {
                lore.add(line);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击查看");
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

        if (menuSection == MenuSection.LUCKY_PILLARS) {
            switch (slot) {
                case 22 -> {
                    playClickSound();
                    new LeaderboardDetailMenu(plugin, player, "minigame_points", menuSection, modeFilter).open();
                }
                case 36 -> {
                    playClickSound();
                    if (menuSection == MenuSection.LUCKY_PILLARS) {
                        plugin.getMenuManager().openLuckyPillarsMenu(player);
                    } else {
                        plugin.getMenuManager().openLuckyPillarsMenu(player);
                    }
                }
                case 40 -> handleCloseButtonAction();
            }
            return;
        }

        switch (slot) {
            case 19 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "pass_count", menuSection, modeFilter).open();
            }
            case 22 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "fastest_time", menuSection, modeFilter).open();
            }
            case 25 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "play_count", menuSection, modeFilter).open();
            }
            case 28 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "hunter_points", menuSection, modeFilter).open();
            }
            case 31 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "prey_points", menuSection, modeFilter).open();
            }
            case 34 -> {
                playClickSound();
                new LeaderboardDetailMenu(plugin, player, "pass_count", menuSection, modeFilter, "prey").open();
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openHunterGameMenu(player);
            }
            case 40 -> handleCloseButtonAction();
        }
    }
}
