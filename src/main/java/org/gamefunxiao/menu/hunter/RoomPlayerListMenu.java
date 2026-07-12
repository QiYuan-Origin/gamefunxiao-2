package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.data.PlayerData;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.util.PlayerHeadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RoomPlayerListMenu extends BaseMenu {

    private final GameRoom room;
    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 21;

    public RoomPlayerListMenu(GameFunXiao plugin, Player player, GameRoom room) {
        this(plugin, player, room, MenuSection.HUNTER, RoomListMenu.defaultHunterFilter());
    }

    public RoomPlayerListMenu(GameFunXiao plugin, Player player, GameRoom room, MenuSection menuSection, Set<GameMode> modeFilter) {
        super(plugin, player, "§0§l👥 房间玩家列表 👥", 54);
        this.room = room;
        this.menuSection = menuSection;
        this.modeFilter = modeFilter;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.PLAYER_HEAD,
            "§x§5§5§F§F§A§A👥 §x§7§7§F§F§B§B玩§x§9§9§F§F§C§C家§x§B§B§F§F§D§D列§x§D§D§F§F§E§E表 §x§5§5§F§F§A§A👥",
            "§8· · · · · · · · · · · · · ·",
            "§f房间: §e" + room.getModeName(),
            "§f人数: §b" + room.getPlayerCount() + "/" + room.getMaxPlayers(),
            "§8· · · · · · · · · · · · · ·"));

        // 如果是自定义房间，显示修饰符
        if (room.isCustomRoom() && !room.getModifiers().isEmpty()) {
            inventory.setItem(6, createModifiersItem());
        }

        // 显示玩家列表
        displayPlayers();

        // 上一页
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }

        // 下一页
        List<UUID> allPlayers = getAllPlayersSorted();
        if ((page + 1) * ITEMS_PER_PAGE < allPlayers.size()) {
            inventory.setItem(50, createNextPageButton());
        }

        // 返回按钮
        inventory.setItem(45, createBackButton());

    }

    private void displayPlayers() {
        List<UUID> allPlayers = getAllPlayersSorted();
        int startIndex = page * ITEMS_PER_PAGE;

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < slots.length && startIndex + i < allPlayers.size(); i++) {
            UUID uuid = allPlayers.get(startIndex + i);
            boolean isPrey = room.isPrey(uuid);
            inventory.setItem(slots[i], createPlayerHead(uuid, isPrey));
        }

        // 如果没有玩家，显示空提示
        if (allPlayers.isEmpty()) {
            inventory.setItem(22, createItem(Material.STRUCTURE_VOID,
                "   §8[§7暂无玩家§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §c房间内暂无玩家",
                "§8· · · · · · · · · · · · · ·"));
        }
    }

    private List<UUID> getAllPlayersSorted() {
        List<UUID> allPlayers = new ArrayList<>();
        // 猎物排在前面
        allPlayers.addAll(room.getPreyUUIDs());
        // 猎人在后面
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isPrey(uuid)) {
                allPlayers.add(uuid);
            }
        }
        return allPlayers;
    }

    private ItemStack createPlayerHead(UUID uuid, boolean isPrey) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
            String playerName = PlayerHeadUtil.getBestPlayerName(uuid, data == null ? null : data.getPlayerName());
            PlayerHeadUtil.applyPlayerSkin(meta, uuid, playerName);

            // 只在游戏进行中时显示职业信息
            boolean showRole = room.getState() == org.gamefunxiao.game.RoomState.PLAYING;

            if (showRole) {
                String roleColor = isPrey ? "§x§F§F§A§A§5§5" : "§x§5§5§A§A§F§F";
                String roleIcon = isPrey ? "🎯" : "⚔";
                String roleName = isPrey ? "猎物" : "猎人";
                meta.setDisplayName(roleColor + roleIcon + " §f" + playerName + " " + roleColor + "[" + roleName + "]");
            } else {
                // 游戏未开始时，不显示职业
                meta.setDisplayName("§x§A§A§A§A§A§A👤 §f" + playerName);
            }

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");

            // 获取玩家历史战绩
            if (data != null) {
                lore.add("§f- §e历史战绩:");
                lore.add("§f  - §a猎物通关: §b" + data.getPreyWins("total") + " §a次");
                lore.add("§f  - §c猎人胜利: §b" + data.getHunterWins("total") + " §c次");
                lore.add("§f  - §d游玩次数: §b" + data.getPlayCount("total") + " §d次");
                // 显示表现值
                int hunterPts = data.getHunterPoints("total");
                int preyPts = data.getPreyPoints("total");
                String hColor = hunterPts >= 0 ? "§6" : "§c";
                String pColor = preyPts >= 0 ? "§6" : "§c";
                lore.add("§f- §e表现值:");
                lore.add("§f  - §c猎人表现: " + hColor + hunterPts);
                lore.add("§f  - §a猎物表现: " + pColor + preyPts);

                // 只在游戏进行中时显示当前职业的详细信息
                if (showRole) {
                    if (isPrey) {
                        long fastestTime = data.getFastestTime("total");
                        if (fastestTime > 0) {
                            lore.add("§f  - §6最快通关: §b" + formatTime(fastestTime));
                        }
                    } else {
                        int kills = data.getHunterKills("total");
                        lore.add("§f  - §c击杀猎物: §b" + kills + " §c次");
                    }
                }
            } else {
                lore.add("§f- §7暂无历史战绩");
            }

            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createModifiersItem() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§A§A§5§5⚡ §x§F§F§C§C§7§7修§x§F§F§E§E§9§9饰§x§D§D§F§F§9§9符§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e当前启用的修饰符:");
            for (String modifier : room.getModifiers()) {
                lore.add("§f  - §b" + modifier);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%d时%d分%d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 48 -> {
                // 上一页
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 50 -> {
                // 下一页
                List<UUID> allPlayers = getAllPlayersSorted();
                if ((page + 1) * ITEMS_PER_PAGE < allPlayers.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            case 45 -> {
                // 返回
                playClickSound();
                new RoomListMenu(plugin, player, menuSection, modeFilter).open();
            }
        }
    }
}
