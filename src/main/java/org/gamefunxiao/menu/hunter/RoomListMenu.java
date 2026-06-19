package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.server.ChildRoomRegistryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RoomListMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] ROOM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;
    private int page = 0;

    public RoomListMenu(GameFunXiao plugin, Player player) {
        this(plugin, player, MenuSection.HUNTER, getDefaultModes(MenuSection.HUNTER));
    }

    public RoomListMenu(GameFunXiao plugin, Player player, MenuSection menuSection, Set<GameMode> modeFilter) {
        super(plugin, player, resolveTitle(menuSection), 54);
        this.menuSection = menuSection == null ? MenuSection.HUNTER : menuSection;
        this.modeFilter = modeFilter == null ? getDefaultModes(this.menuSection) : EnumSet.copyOf(modeFilter);
    }

    public static RoomListMenu luckyPillarsOnly(GameFunXiao plugin, Player player) {
        return new RoomListMenu(plugin, player, MenuSection.LUCKY_PILLARS, getDefaultModes(MenuSection.LUCKY_PILLARS));
    }

    public static RoomListMenu brickGuardOnly(GameFunXiao plugin, Player player) {
        return new RoomListMenu(plugin, player, MenuSection.BRICK_GUARD, getDefaultModes(MenuSection.BRICK_GUARD));
    }


    public static Set<GameMode> defaultHunterFilter() {
        return EnumSet.copyOf(getDefaultModes(MenuSection.HUNTER));
    }

    public static Set<GameMode> defaultLuckyFilter() {
        return EnumSet.copyOf(getDefaultModes(MenuSection.LUCKY_PILLARS));
    }

    public static Set<GameMode> defaultBrickGuardFilter() {
        return EnumSet.copyOf(getDefaultModes(MenuSection.BRICK_GUARD));
    }


    private static String resolveTitle(MenuSection section) {
        return switch (section) {
            case LUCKY_PILLARS -> "§0§l🍀 幸运之柱房间 🍀";
            case BRICK_GUARD -> "§0§l▣ 板砖守卫战房间 ▣";
            case GENERIC, HUNTER -> "§0§l⚔ 猎人房间列表 ⚔";
        };
    }

    private static Set<GameMode> getDefaultModes(MenuSection section) {
        return switch (section) {
            case LUCKY_PILLARS -> GameMode.getLuckyPillarsSectionModes();
            case BRICK_GUARD -> GameMode.getBrickGuardSectionModes();
            case GENERIC, HUNTER -> GameMode.getHunterSectionModes();
        };
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(getTitleMaterial(),
                getTitleText(),
                "§8· · · · · · · · · · · · · ·",
                plugin.getRoomManager().isChildRoomBackendActive()
                        ? "§f当前使用 §d子服房间后端"
                        : "§f当前使用 §b本服建世界后端",
                switch (menuSection) {
                    case LUCKY_PILLARS -> "§f这里只看幸运之柱经典模式房间";
                    case BRICK_GUARD -> "§f这里只看雨云 · 板砖守卫战房间";
                    case GENERIC, HUNTER -> "§f这里只看猎人玩法相关房间";
                },
                "§8· · · · · · · · · · · · · ·"));

        displayRooms();

        List<RoomListEntry> entries = getSortedEntries();
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }
        if ((page + 1) * ITEMS_PER_PAGE < entries.size()) {
            inventory.setItem(50, createNextPageButton());
        }

        inventory.setItem(49, createRefreshButton());
        inventory.setItem(45, createBackButton());
    }

    private Material getTitleMaterial() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> Material.GOLD_BLOCK;
            case BRICK_GUARD -> Material.BRICK;
            case GENERIC, HUNTER -> Material.ENDER_EYE;
        };
    }

    private String getTitleText() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱房间";
            case BRICK_GUARD -> "§x§F§F§7§C§0§0▣ §x§F§F§8§8§1§1板§x§F§F§9§4§2§2砖§x§D§D§6§6§1§1守§x§B§B§4§4§0§0卫§x§6§6§1§9§0§0战房间";
            case GENERIC, HUNTER -> "§x§5§5§F§F§F§F👁 §x§7§7§F§F§D§D猎§x§9§9§F§F§B§B人§x§B§B§F§F§9§9房§x§D§D§F§F§7§7间";
        };
    }

    private void displayRooms() {
        List<RoomListEntry> entries = getSortedEntries();
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ROOM_SLOTS.length && startIndex + i < entries.size(); i++) {
            RoomListEntry entry = entries.get(startIndex + i);
            inventory.setItem(ROOM_SLOTS[i], createRoomItem(entry));
        }

        if (entries.isEmpty()) {
            Material emptyMaterial = switch (menuSection) {
                case LUCKY_PILLARS -> Material.GOLD_NUGGET;
                case BRICK_GUARD -> Material.BRICK;
                case GENERIC, HUNTER -> Material.STRUCTURE_VOID;
            };
            String emptyText = switch (menuSection) {
                case LUCKY_PILLARS -> "§f- §c当前没有幸运之柱房间";
                case BRICK_GUARD -> "§f- §c当前没有板砖守卫战房间";
                case GENERIC, HUNTER -> "§f- §c当前没有猎人玩法房间";
            };
            inventory.setItem(22, createItem(emptyMaterial,
                    "   §8[§7暂无房间§8]",
                    "§8· · · · · · · · · · · · · ·",
                    emptyText,
                    plugin.getRoomManager().isChildRoomBackendActive()
                            ? "§f- §e你可以先创建一个子服房间"
                            : "§f- §e你可以先创建一个本服房间",
                    "§8· · · · · · · · · · · · · ·"));
        }
    }

    private List<RoomListEntry> getSortedEntries() {
        List<RoomListEntry> entries = new ArrayList<>();

        if (plugin.getRoomManager().isChildRoomBackendActive()) {
            for (ChildRoomRegistryEntry entry : plugin.getRoomManager().getVisibleChildRoomEntries()) {
                if (!shouldShowMode(entry.getMode())) {
                    continue;
                }
                boolean custom = entry.getMode() == GameMode.CUSTOM || !entry.getModifiers().isEmpty();
                entries.add(new RoomListEntry(
                        entry.getRoomId(),
                        entry.getMode(),
                        entry.getMode().getDisplayName(),
                        entry.getState(),
                        entry.getCurrentPlayers(),
                        entry.getMaxPlayers(),
                        entry.getOwnerName(),
                        custom,
                        new LinkedHashSet<>(entry.getModifiers()),
                        true,
                        0L,
                        List.of(),
                        entry.getCreatedAt()
                ));
            }
        } else {
            for (GameRoom room : plugin.getRoomManager().getAllRooms()) {
                if (!shouldShowMode(room.getGameMode())) {
                    continue;
                }
                entries.add(new RoomListEntry(
                        room.getRoomId(),
                        room.getGameMode(),
                        room.getModeName(),
                        room.getState(),
                        room.getPlayerCount(),
                        room.getMaxPlayers(),
                        room.getOwnerName(),
                        room.isCustomRoom(),
                        new LinkedHashSet<>(room.getModifiers()),
                        false,
                        room.getGameDuration(),
                        new ArrayList<>(room.getPreyNames()),
                        room.getCreateTime()
                ));
            }
        }

        entries.sort(Comparator.comparing((RoomListEntry entry) -> entry.state() != RoomState.WAITING)
                .thenComparing(entry -> entry.state() != RoomState.STARTING)
                .thenComparingLong(RoomListEntry::createdAt));
        return entries;
    }

    private boolean shouldShowMode(GameMode mode) {
        return mode != null && modeFilter.contains(mode);
    }

    private org.bukkit.inventory.ItemStack createRoomItem(RoomListEntry entry) {
        Material material;
        String stateText;
        String stateColor;

        switch (entry.state()) {
            case WAITING -> {
                material = Material.LIME_CONCRETE;
                stateText = "等待中";
                stateColor = "§a";
            }
            case STARTING -> {
                material = Material.YELLOW_CONCRETE;
                stateText = "即将开始";
                stateColor = "§e";
            }
            case SELECTING -> {
                material = Material.CYAN_CONCRETE;
                stateText = "选择中";
                stateColor = "§b";
            }
            case PLAYING -> {
                material = entry.mode().isBrickGuard() ? Material.RED_GLAZED_TERRACOTTA : Material.RED_CONCRETE;
                stateText = "游戏中";
                stateColor = "§c";
            }
            default -> {
                material = Material.GRAY_CONCRETE;
                stateText = "已结束";
                stateColor = "§7";
            }
        }

        String roomType = entry.custom() ? "§d[自定义] " : "§b[默认] ";
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §e房间ID: §7" + entry.roomId());
        lore.add("§f- §e状态: " + stateColor + stateText);
        lore.add("§f- §e人数: §b" + entry.playerCount() + "/" + (entry.maxPlayers() == -1 ? "∞" : entry.maxPlayers()));
        lore.add("§f- §e房主: §a" + entry.ownerName());
        lore.add("§f- §e后端: " + (entry.childManaged() ? "§d子服房间" : "§b本服世界"));

        if (entry.state() == RoomState.PLAYING && entry.gameDuration() > 0L) {
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c游戏时长: §e" + formatTime(entry.gameDuration()));
            if (entry.mode().isLuckyPillars()) {
                lore.add("§f- §a玩法: 每5秒随机物品，最后存活");
            } else if (entry.mode().isBrickGuard()) {
                lore.add("§f- §6玩法: 板砖队核心方块与下界砖核心玩家对抗");
            } else if (!entry.preyNames().isEmpty()) {
                lore.add("§f- §d猎物: §f" + String.join(", ", entry.preyNames()));
            }
        }

        if (!entry.modifiers().isEmpty()) {
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6修饰符:");
            for (String modifier : entry.modifiers()) {
                lore.add("§f  - §e" + modifier);
            }
        }

        lore.add("§8· · · · · · · · · · · · · ·");
        if (entry.state() == RoomState.WAITING || entry.state() == RoomState.STARTING) {
            lore.add("§f- §a左键加入房间");
        } else if (entry.childManaged() && (entry.mode().isLuckyPillars() || entry.mode().isFlashLike())
                && (entry.state() == RoomState.PLAYING || entry.state() == RoomState.SELECTING)) {
            lore.add(entry.mode().isLuckyPillars()
                    ? "§f- §a左键进入幸运之柱旁观"
                    : "§f- §a左键进入当前猎人局旁观");
        } else if (entry.state() == RoomState.PLAYING && !entry.childManaged()) {
            lore.add("§f- §a左键旁观游戏");
        } else if (entry.state() == RoomState.PLAYING) {
            lore.add("§f- §7对子服进行中的房间暂不支持直接旁观");
        }

        lore.add(entry.childManaged() ? "§f- §7右键暂无额外操作" : "§f- §b右键查看玩家列表");
        return createItem(material, "   §8[" + roomType + stateColor + entry.modeName() + "§8]", lore);
    }

    private org.bukkit.inventory.ItemStack createRefreshButton() {
        return createItem(Material.SUNFLOWER,
                "   §8[§x§5§5§F§F§5§5🔄 §x§7§7§F§F§7§7刷§x§9§9§F§F§9§9新§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击刷新房间列表",
                "§8· · · · · · · · · · · · · ·");
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d分%d秒", minutes, seconds);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        int slotIndex = -1;
        for (int i = 0; i < ROOM_SLOTS.length; i++) {
            if (ROOM_SLOTS[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            List<RoomListEntry> entries = getSortedEntries();
            int entryIndex = page * ITEMS_PER_PAGE + slotIndex;
            if (entryIndex < entries.size()) {
                handleRoomClick(entries.get(entryIndex), event.isRightClick());
            }
            return;
        }

        switch (slot) {
            case 48 -> {
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 49 -> {
                playClickSound();
                setupItems();
            }
            case 50 -> {
                List<RoomListEntry> entries = getSortedEntries();
                if ((page + 1) * ITEMS_PER_PAGE < entries.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            case 45 -> {
                playClickSound();
                openSectionRoot();
            }
        }
    }

    private void handleRoomClick(RoomListEntry entry, boolean rightClick) {
        GameRoom localRoom = plugin.getRoomManager().getRoom(entry.roomId());

        if (rightClick) {
            if (localRoom != null) {
                playClickSound();
                new RoomPlayerListMenu(plugin, player, localRoom, menuSection, modeFilter).open();
            } else {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.mode(), "room.cannot_join"));
            }
            return;
        }

        if (entry.state() == RoomState.WAITING || entry.state() == RoomState.STARTING) {
            playSelectSound();
            if (!plugin.getRoomManager().joinRoomById(player, entry.roomId())) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.mode(), "room.cannot_join"));
            }
            return;
        }

        if (entry.state() == RoomState.PLAYING && localRoom != null) {
            String disconnectedRoomId = plugin.getRoomManager().getDisconnectedRoomId(player.getUniqueId());
            if (disconnectedRoomId != null && disconnectedRoomId.equals(localRoom.getRoomId())) {
                playClickSound();
                new ConfirmRejoinMenu(plugin, player, localRoom, menuSection, modeFilter).open();
            } else {
                playClickSound();
                new ConfirmSpectateMenu(plugin, player, localRoom, menuSection, modeFilter).open();
            }
            return;
        }

        if (entry.childManaged()
                && (entry.mode().isLuckyPillars() || entry.mode().isBrickGuard() || entry.mode().isFlashLike())
                && (entry.state() == RoomState.PLAYING || entry.state() == RoomState.SELECTING)) {
            playClickSound();
            if (!plugin.getRoomManager().joinRoomById(player, entry.roomId())) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.mode(), "room.cannot_join"));
            }
            return;
        }

        playErrorSound();
        player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.mode(), "room.cannot_join"));
    }

    private void openSectionRoot() {
        switch (menuSection) {
            case LUCKY_PILLARS -> plugin.getMenuManager().openLuckyPillarsMenu(player);
            case BRICK_GUARD -> plugin.getMenuManager().openBrickGuardMenu(player);
            case GENERIC, HUNTER -> plugin.getMenuManager().openHunterGameMenu(player);
        }
    }

    public MenuSection getMenuSection() {
        return menuSection;
    }

    public Set<GameMode> getModeFilter() {
        return EnumSet.copyOf(modeFilter);
    }

    private record RoomListEntry(
            String roomId,
            GameMode mode,
            String modeName,
            RoomState state,
            int playerCount,
            int maxPlayers,
            String ownerName,
            boolean custom,
            Set<String> modifiers,
            boolean childManaged,
            long gameDuration,
            List<String> preyNames,
            long createdAt
    ) {
    }
}
