package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.data.PlayerData;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.util.PlayerHeadUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class LeaderboardDetailMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] DATA_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final String type;
    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;
    private final boolean roleSwitchLocked;
    private String timeRange = "total";
    private String roleType = "all";
    private String fastestModeId = "all";
    private int page = 0;

    public LeaderboardDetailMenu(GameFunXiao plugin, Player player, String type) {
        this(plugin, player, type, MenuSection.HUNTER, defaultFilter(MenuSection.HUNTER), null);
    }

    public LeaderboardDetailMenu(GameFunXiao plugin, Player player, String type, MenuSection menuSection, Set<GameMode> modeFilter) {
        this(plugin, player, type, menuSection, modeFilter, null);
    }

    public LeaderboardDetailMenu(GameFunXiao plugin, Player player, String type, MenuSection menuSection, Set<GameMode> modeFilter, String forcedRoleType) {
        super(plugin, player, resolveTitle(type, menuSection), 54);
        this.type = type;
        this.menuSection = menuSection == null ? MenuSection.HUNTER : menuSection;
        this.modeFilter = modeFilter == null ? defaultFilter(this.menuSection) : EnumSet.copyOf(modeFilter);
        this.roleSwitchLocked = forcedRoleType != null && !forcedRoleType.isBlank();

        if (roleSwitchLocked) {
            this.roleType = forcedRoleType;
        } else if ("fastest_time".equals(type)) {
            this.roleType = "prey";
        } else if ("hunter_points".equals(type)) {
            this.roleType = "hunter";
        } else if ("prey_points".equals(type)) {
            this.roleType = "prey";
        }
    }

    private static String resolveTitle(String type, MenuSection section) {
        if (section == MenuSection.LUCKY_PILLARS) {
            return "§0§l🍀 幸运之柱榜单详情 🍀";
        }
        return switch (type) {
            case "pass_count" -> "§0§l✓ 通关次数排行榜 ✓";
            case "fastest_time" -> "§0§l⏱ 最快通关排行榜 ⏱";
            case "play_count" -> "§0§l📊 游玩次数排行榜 📊";
            case "hunter_points" -> "§0§l⚔ 猎人积分排行榜 ⚔";
            case "prey_points" -> "§0§l🎯 猎物积分排行榜 🎯";
            case "minigame_points" -> "§0§l🍀 小游戏积分排行榜 🍀";
            default -> "§0§l🏆 排行榜详情 🏆";
        };
    }

    private static Set<GameMode> defaultFilter(MenuSection section) {
        return switch (section) {
            case LUCKY_PILLARS -> GameMode.getLuckyPillarsSectionModes();
            case GENERIC, HUNTER -> GameMode.getHunterSectionModes();
        };
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem());
        inventory.setItem(0, createTimeRangeSwitchButton());

        if ("fastest_time".equals(type)) {
            inventory.setItem(8, createFastestModeSwitchButton());
        } else if (!roleSwitchLocked && shouldShowRoleSwitch()) {
            inventory.setItem(8, createRoleTypeSwitchButton());
        }

        displayLeaderboardData();

        List<PlayerData> data = getLeaderboardData();
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }
        if ((page + 1) * ITEMS_PER_PAGE < data.size()) {
            inventory.setItem(50, createNextPageButton());
        }

        inventory.setItem(45, createBackButton());
        inventory.setItem(49, createInfoItem());
    }

    private boolean shouldShowRoleSwitch() {
        return !"hunter_points".equals(type) && !"prey_points".equals(type) && !"minigame_points".equals(type);
    }

    private ItemStack createTitleItem() {
        Material material = switch (type) {
            case "pass_count" -> Material.DIAMOND;
            case "fastest_time" -> Material.CLOCK;
            case "play_count" -> Material.BOOK;
            case "hunter_points" -> Material.IRON_SWORD;
            case "prey_points" -> Material.RABBIT_FOOT;
            case "minigame_points" -> Material.GOLD_BLOCK;
            default -> Material.NETHER_STAR;
        };

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §e当前时间范围: §b" + getTimeRangeName(timeRange));
        lore.add("§f- §e当前分区: §b" + getSectionName());
        if ("fastest_time".equals(type)) {
            lore.add("§f- §e当前模式: §d" + getFastestModeName(fastestModeId));
        } else if (shouldShowRoleSwitch()) {
            lore.add("§f- §e当前角色筛选: §b" + getRoleDisplayName(roleType));
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        return createItem(material, getTitleText(), lore);
    }

    private String getTitleText() {
        return switch (type) {
            case "pass_count" -> "§x§5§5§F§F§D§D✓ §x§7§7§F§F§C§C通§x§9§9§F§F§B§B关§x§B§B§F§F§A§A / §x§D§D§F§F§9§9击§x§F§F§E§E§8§8杀榜";
            case "fastest_time" -> "§x§F§F§E§E§5§5⏱ §x§F§F§D§D§7§7最§x§F§F§C§C§9§9快§x§F§F§B§B§B§B通§x§F§F§A§A§D§D关";
            case "play_count" -> "§x§A§A§F§F§5§5📊 §x§B§B§F§F§7§7游§x§C§C§F§F§9§9玩§x§D§D§F§F§B§B次§x§E§E§F§F§D§D数";
            case "hunter_points" -> "§x§F§F§5§5§5§5⚔ §x§F§F§7§7§5§5猎§x§F§F§9§9§5§5人§x§F§F§B§B§5§5积§x§F§F§D§D§5§5分榜";
            case "prey_points" -> "§x§5§5§F§F§A§A🎯 §x§7§7§F§F§B§B猎§x§9§9§F§F§C§C物§x§B§B§F§F§D§D积§x§D§D§F§F§E§E分榜";
            case "minigame_points" -> "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6小§x§F§F§B§B§7§7游§x§F§F§A§A§8§8戏§x§F§F§9§9§9§9积§x§F§F§8§8§A§A分榜";
            default -> "§x§F§F§D§7§0§0🏆 §x§F§F§B§B§0§0排§x§F§F§9§9§0§0行§x§F§F§7§7§0§0榜";
        };
    }

    private ItemStack createTimeRangeSwitchButton() {
        String[] ranges = {"day", "week", "month", "year", "total"};
        int currentIndex = findIndex(ranges, timeRange);
        int prevIndex = (currentIndex - 1 + ranges.length) % ranges.length;
        int nextIndex = (currentIndex + 1) % ranges.length;

        return createItem(Material.CLOCK,
                "   §8[§x§F§F§D§7§0§0⏰ §x§F§F§B§B§0§0时§x§F§F§9§9§0§0间§x§F§F§7§7§0§0范§x§F§F§5§5§0§0围§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a当前: §e" + getTimeRangeName(ranges[currentIndex]),
                "§f- §7上一个: §b" + getTimeRangeName(ranges[prevIndex]),
                "§f- §7下一个: §d" + getTimeRangeName(ranges[nextIndex]),
                "§8· · · · · · · · · · · · · ·",
                "§f- §e左键 §7切换到下一个",
                "§f- §e右键 §7切换到上一个",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createRoleTypeSwitchButton() {
        String[] roles = {"all", "prey", "hunter"};
        int currentIndex = findIndex(roles, roleType);
        int prevIndex = (currentIndex - 1 + roles.length) % roles.length;
        int nextIndex = (currentIndex + 1) % roles.length;

        return createItem(Material.ENDER_EYE,
                "   §8[§x§5§5§F§F§D§D👥 §x§7§7§F§F§C§C角§x§9§9§F§F§B§B色§x§B§B§F§F§A§A筛§x§D§D§F§F§9§9选§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a当前: §b" + getRoleDisplayName(roles[currentIndex]),
                "§f- §7上一个: §d" + getRoleDisplayName(roles[prevIndex]),
                "§f- §7下一个: §e" + getRoleDisplayName(roles[nextIndex]),
                "§8· · · · · · · · · · · · · ·",
                "§f- §e左键 §7下一个",
                "§f- §e右键 §7上一个",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createFastestModeSwitchButton() {
        String[] modes = getFastestModeIds();
        int currentIndex = findIndex(modes, fastestModeId);
        int prevIndex = (currentIndex - 1 + modes.length) % modes.length;
        int nextIndex = (currentIndex + 1) % modes.length;

        return createItem(getFastestModeMaterial(modes[currentIndex]),
                "   §8[§x§8§8§D§D§F§F✦ §x§9§9§D§D§F§F模§x§A§A§D§D§F§F式§x§B§B§D§D§F§F筛§x§C§C§D§D§F§F选§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a当前: §b" + getFastestModeName(modes[currentIndex]),
                "§f- §7上一个: §d" + getFastestModeName(modes[prevIndex]),
                "§f- §7下一个: §e" + getFastestModeName(modes[nextIndex]),
                "§8· · · · · · · · · · · · · ·",
                "§f- §e左键 §7下一个",
                "§f- §e右键 §7上一个",
                "§8· · · · · · · · · · · · · ·");
    }

    private void displayLeaderboardData() {
        List<PlayerData> data = getLeaderboardData();
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < DATA_SLOTS.length && startIndex + i < data.size(); i++) {
            inventory.setItem(DATA_SLOTS[i], createPlayerHead(data.get(startIndex + i), startIndex + i + 1));
        }

        if (data.isEmpty()) {
            inventory.setItem(22, createItem(Material.STRUCTURE_VOID,
                    "   §8[§7暂无数据§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c当前条件下暂无数据",
                    "§f- §e去开几把再回来看看吧",
                    "§8· · · · · · · · · · · · · ·"));
        }
    }

    private ItemStack createPlayerHead(PlayerData data, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PlayerHeadUtil.applyPlayerSkin(meta, data.getUuid(), data.getPlayerName());
        meta.setDisplayName(getRankColor(rank) + getRankIcon(rank) + " §f" + data.getPlayerName());

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        switch (type) {
            case "pass_count" -> appendPassCountLore(lore, data);
            case "fastest_time" -> appendFastestLore(lore, data);
            case "play_count" -> lore.add("§f- §d游玩次数: §e" + getPlayCount(data) + " §d次");
            case "hunter_points" -> lore.add("§f- §c猎人积分: §6" + getHunterPoints(data));
            case "prey_points" -> lore.add("§f- §a猎物积分: §6" + getPreyPoints(data));
            case "minigame_points" -> lore.add("§f- §e小游戏积分: §6" + getMiniGamePoints(data));
            default -> lore.add("§f- §7暂无显示内容");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void appendPassCountLore(List<String> lore, PlayerData data) {
        if ("prey".equals(roleType)) {
            lore.add("§f- §a猎物通关: §e" + getPreyWins(data) + " §a次");
            return;
        }
        if ("hunter".equals(roleType)) {
            lore.add("§f- §c猎人击杀: §e" + getHunterKills(data) + " §c次");
            return;
        }
        lore.add("§f- §a猎物通关: §e" + getPreyWins(data) + " §a次");
        lore.add("§f- §c猎人击杀: §e" + getHunterKills(data) + " §c次");
        lore.add("§f- §b总计: §e" + (getPreyWins(data) + getHunterKills(data)) + " §b次");
    }

    private void appendFastestLore(List<String> lore, PlayerData data) {
        long fastest = data.getFastestTime(timeRange, fastestModeId);
        if (fastest > 0L) {
            lore.add("§f- §e最快通关: §b" + formatTime(fastest));
            lore.add("§f- §7模式: §d" + getFastestModeName(fastestModeId));
        } else {
            lore.add("§f- §7暂无记录");
        }
    }

    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        if (menuSection == MenuSection.LUCKY_PILLARS) {
            lore.add("§f- §a这里只统计幸运之柱经典模式");
            lore.add("§f- §7不会混入猎人玩法数据");
        } else {
            lore.add("§f- §a这里只统计当前猎人玩法分区的数据");
            lore.add("§f- §7不会和小游戏积分串一起");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        return createItem(Material.PAPER, "   §8[§e说明§8]", lore);
    }

    private List<PlayerData> getLeaderboardData() {
        return plugin.getLeaderboardManager().getLeaderboard(
                type,
                timeRange,
                roleType,
                fastestModeId,
                getModeIdFilter()
        );
    }

    private Set<String> getModeIdFilter() {
        return modeFilter.stream()
                .map(GameMode::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String[] getFastestModeIds() {
        List<String> result = new ArrayList<>();
        result.add("all");
        for (GameMode mode : modeFilter) {
            if (!mode.supportsFastestTimeLeaderboard()) {
                continue;
            }
            result.add(mode.getId());
        }
        return result.toArray(new String[0]);
    }

    private int findIndex(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                return i;
            }
        }
        return 0;
    }

    private String getTimeRangeName(String range) {
        return switch (range) {
            case "day" -> "天榜";
            case "week" -> "周榜";
            case "month" -> "月榜";
            case "year" -> "年榜";
            default -> "总榜";
        };
    }

    private String getRoleDisplayName(String role) {
        return switch (role) {
            case "prey" -> "猎物榜";
            case "hunter" -> "猎人榜";
            default -> "全部";
        };
    }

    private String getSectionName() {
        return switch (menuSection) { case LUCKY_PILLARS -> "幸运之柱"; case GENERIC, HUNTER -> "猎人玩法"; };
    }

    private String getFastestModeName(String modeId) {
        if (modeId == null || modeId.isBlank() || "all".equalsIgnoreCase(modeId)) {
            return "当前分区全部模式";
        }
        return GameMode.findByIdStrict(modeId)
                .map(GameMode::getDisplayName)
                .orElse(modeId);
    }

    private Material getFastestModeMaterial(String modeId) {
        if (modeId == null || modeId.isBlank() || "all".equalsIgnoreCase(modeId)) {
            return Material.NETHER_STAR;
        }
        return switch (modeId) {
            case "classic" -> Material.DIAMOND_SWORD;
            case "random_compass" -> Material.COMPASS;
            case "swap" -> Material.ENDER_PEARL;
            case "no_item" -> Material.BARRIER;
            case "survival" -> Material.OAK_SAPLING;
            case "flash" -> Material.AMETHYST_SHARD;
            case "flash_tournament" -> Material.RED_BANNER;
            case "end_flash" -> Material.END_CRYSTAL;
            default -> Material.CLOCK;
        };
    }

    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§x§F§F§D§7§0§0";
            case 2 -> "§x§C§0§C§0§C§0";
            case 3 -> "§x§C§D§7§F§3§2";
            default -> "§7";
        };
    }

    private String getRankIcon(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + rank;
        };
    }

    private int getPreyWins(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getPreyWins(timeRange, mode.getId())).sum();
    }

    private int getHunterKills(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getHunterKills(timeRange, mode.getId())).sum();
    }

    private int getPlayCount(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getPlayCount(timeRange, mode.getId())).sum();
    }

    private int getHunterPoints(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getHunterPoints(timeRange, mode.getId())).sum();
    }

    private int getPreyPoints(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getPreyPoints(timeRange, mode.getId())).sum();
    }

    private int getMiniGamePoints(PlayerData data) {
        return modeFilter.stream().mapToInt(mode -> data.getMiniGamePoints(timeRange, mode.getId())).sum();
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        if (hours > 0) {
            return String.format("%d时%d分%d秒", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        }
        return String.format("%d秒", seconds);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 0 -> {
                String[] ranges = {"day", "week", "month", "year", "total"};
                int currentIndex = findIndex(ranges, timeRange);
                if (event.isLeftClick()) {
                    currentIndex = (currentIndex + 1) % ranges.length;
                } else if (event.isRightClick()) {
                    currentIndex = (currentIndex - 1 + ranges.length) % ranges.length;
                }
                playSelectSound();
                timeRange = ranges[currentIndex];
                page = 0;
                setupItems();
            }
            case 8 -> handleSwitchButton(event);
            case 48 -> {
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 50 -> {
                List<PlayerData> data = getLeaderboardData();
                if ((page + 1) * ITEMS_PER_PAGE < data.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            case 45 -> {
                playClickSound();
                new LeaderboardMenu(plugin, player, menuSection, modeFilter).open();
            }
        }
    }

    private void handleSwitchButton(InventoryClickEvent event) {
        if ("fastest_time".equals(type)) {
            String[] modes = getFastestModeIds();
            int currentIndex = findIndex(modes, fastestModeId);
            if (event.isLeftClick()) {
                currentIndex = (currentIndex + 1) % modes.length;
            } else if (event.isRightClick()) {
                currentIndex = (currentIndex - 1 + modes.length) % modes.length;
            }
            playSelectSound();
            fastestModeId = modes[currentIndex];
            page = 0;
            setupItems();
            return;
        }

        if (roleSwitchLocked || !shouldShowRoleSwitch()) {
            return;
        }

        String[] roles = {"all", "prey", "hunter"};
        int currentIndex = findIndex(roles, roleType);
        if (event.isLeftClick()) {
            currentIndex = (currentIndex + 1) % roles.length;
        } else if (event.isRightClick()) {
            currentIndex = (currentIndex - 1 + roles.length) % roles.length;
        }
        playSelectSound();
        roleType = roles[currentIndex];
        page = 0;
        setupItems();
    }
}

