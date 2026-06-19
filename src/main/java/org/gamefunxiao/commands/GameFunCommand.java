package org.gamefunxiao.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.OfflinePlayer;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.RoomManager;
import org.gamefunxiao.world.MiniGameMapManager;

import java.util.*;

public class GameFunCommand implements CommandExecutor, TabCompleter {

    private final GameFunXiao plugin;

    public GameFunCommand(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        executeRegisteredCommand(sender, command.getName(), args);
        return true;
    }

    public void executeRegisteredCommand(CommandSender sender, String commandName, String[] args) {
        if (commandName.equalsIgnoreCase("hh")) {
            handleAdvertiseShortcut(sender);
            return;
        }
        if (commandName.equalsIgnoreCase("ec")
                || commandName.equalsIgnoreCase("enderchest")
                || commandName.equalsIgnoreCase("endflashender")) {
            handleEndFlashEnderChest(sender);
            return;
        }
        if (commandName.equalsIgnoreCase("flashwiki")
                || commandName.equalsIgnoreCase("bookwiki")
                || commandName.equalsIgnoreCase("gamefunwiki")
                || commandName.equalsIgnoreCase("闪光手册")
                || commandName.equalsIgnoreCase("书wiki")) {
            handleFlashWikiBook(sender);
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
                return;
            }
            plugin.getMenuManager().openMainMenu(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> {
                int page = args.length >= 2 ? parseHelpPage(args[1]) : 1;
                sendHelp(sender, page);
            }
            case "reload" -> handleReload(sender);
            case "cleanuprooms", "roomclean" -> handleCleanupRooms(sender);
            case "editlobbytemplate", "edittemplate" -> {
                if (args.length >= 2) {
                    handleEditLobbyTemplate(sender, args[1]);
                } else {
                    handleEditLobbyTemplate(sender);
                }
            }
            case "setlobbyspawn" -> handleSetLobbySpawnCommand(sender);
            case "map", "maps", "minigamemap", "mgmap", "地图", "地图编辑" -> handleMiniGameMap(sender, args);
            case "coins", "coin", "money" -> handleCoins(sender, args);
            case "endflashkit", "efkit", "终章kit", "flashkit", "闪光kit", "调闪光kit" -> handleEndFlashKit(sender, args);
            case "endflashender", "efender", "终章末影箱" -> handleEndFlashEnderChest(sender);
            case "endflashcompass", "efcompass", "终章指南针" -> handleEndFlashCompass(sender);
            case "endflashdebug", "endflashtest", "tuneendflash", "efdebug", "调终章", "终章调试" -> handleEndFlashDebug(sender);
            case "flashuse", "flashtest", "flashitems" -> handleFlashUse(sender, args);
            case "unstablemace", "unstable", "不稳定重锤", "重锤" -> handleRemovedUnstableMace(sender);
            case "wiki", "bookwiki", "flashwiki", "guidebook", "guide", "手册", "书wiki", "闪光手册" -> handleFlashWikiBook(sender);
            case "wikiopen", "openwiki", "flashwikiopen", "打开书wiki" -> handleOpenFlashWikiBook(sender, args);
            case "flashmusic", "fmusic", "flashnote", "闪光音乐", "音符盒" -> handleFlashMusic(sender, args);
            case "lobbyinteract", "lobbyregion", "等待大厅交互", "大厅交互" -> handleLobbyInteractionRegion(sender, args);
            case "command", "cmd", "命令", "指令" -> handleCommandBranch(sender, args);
            case "huntergame", "hg" -> handleHunterGame(sender, args);
            case "leave", "quit" -> handleLeave(sender);
            case "rejoin" -> handleRejoin(sender);
            default -> sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_command"));
        }

    }

    private void handleAdvertiseShortcut(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        plugin.getPlayerListener().advertiseCurrentRoom(player);
    }

    private void sendHelp(CommandSender sender, int page) {
        List<String> entries = new ArrayList<>();
        entries.add("§e/gamefunxiao §7- §f打开主菜单");
        entries.add("§e/gamefunxiao menu §7- §f打开主菜单");
        entries.add("§e/gamefunxiao help [页码] §7- §f查看分页帮助");
        entries.add("§e/gamefunxiao huntergame §7- §f打开猎人游戏菜单");
        entries.add("§e/gamefunxiao leave §7- §f离开当前房间");
        entries.add("§e/gamefunxiao rejoin §7- §f重新加入游戏（猎人断线后使用）");
        entries.add("§e/gamefunxiao endflashkit list §7- §f查看终章闪光 Kit");
        entries.add("§e/ec §7- §f终章中距离猎物100~170格打开末影箱");
        entries.add("§e/enderchest §7- §f终章末影箱完整命令");
        entries.add("§e/gamefunxiao endflashender §7- §f同样可以打开终章末影箱");
        entries.add("§e/gamefunxiao endflashcompass §7- §f终章中重新获取指南针");
        entries.add("§e/gamefunxiao wiki §7- §f获取主 · 闪光书");
        entries.add("§e/gamefunxiao command §7- §f打开命令入口菜单，可模拟菜单按钮");
        entries.add("§e/gamefunxiao command quick <模式> §7- §f直接进入对应模式等待房间");
        entries.add("§e/flashwiki §7- §f直接领取主 · 闪光书");
        entries.add("§e/gamefunxiao flashmusic play <歌曲名> [Mall] §7- §f播放闪光音符盒音乐，Mall=完整全音");
        entries.add("§e/gamefunxiao flashmusic list [页] §7- §f查看可播放曲库");
        entries.add("§e/gamefunxiao flashmusic stop §7- §f停止自己正在播放的音乐");
        if (!sender.hasPermission("gamefunxiao.admin") && sender.hasPermission("gamefunxiao.flashuse")) {
            entries.add("§e/gamefunxiao flashuse <on|off|toggle|status> §7- §f离开房间后试用闪光物品");
        }
        entries.add("§e/hh §7- §f宣传当前房间");

        if (sender.hasPermission("gamefunxiao.admin")) {
            entries.add("§c管理员命令：");
            entries.add("§e/gamefunxiao reload §7- §f重载配置文件");
            entries.add("§e/gamefunxiao cleanuprooms §7- §f清理跨服幽灵房间");
            entries.add("§e/gamefunxiao editlobbytemplate <大厅名> §7- §f前往对应等待大厅模板进行修改");
            entries.add("§e/gamefun setlobbyspawn §7- §f在 gameing 的等待大厅世界中自动设置出生点");
            entries.add("§e/gamefunxiao map list [模式] §7- §f查看小游戏地图列表");
            entries.add("§e/gamefunxiao map create <模式> <地图ID> [人数] [显示名] §7- §f创建地图配置");
            entries.add("§e/gamefunxiao map edit <模式> <地图ID> [game|lobby] §7- §f编辑地图模板");
            entries.add("§e/gamefunxiao map active <模式> <地图ID|random> §7- §f切换当前地图或随机地图池");
            entries.add("§e/gamefunxiao map enable|disable|delete <模式> <地图ID> §7- §f管理地图启用状态和删除");
            entries.add("§e/gamefunxiao map range|time|theme|boundary <模式> <地图ID> ... §7- §f修改地图人数、时间、主题、边界");
            entries.add("§e/gamefunxiao lobbyinteract add <ID> <x1> <y1> <z1> <x2> <y2> <z2> [世界] §7- §f允许等待大厅指定区域交互");
            entries.add("§e/gamefunxiao lobbyinteract remove/list <ID> §7- §f管理等待大厅可交互区域");
            entries.add("§e/gamefunxiao coins set <数量> [玩家] §7- §f设置小游戏币");
            entries.add("§e/gamefunxiao coins add <数量> [玩家] §7- §f增加小游戏币");
            entries.add("§e/gamefunxiao coins get [玩家] §7- §f查看小游戏币");
            entries.add("§e/gamefunxiao endflashkit menu §7- §f打开终章闪光 Kit 调试菜单");
            entries.add("§e/gamefunxiao endflashkit create <hunter|prey> <0-50小数> <名字...> §7- §f保存当前背包为终章 Kit");
            entries.add("§e/gamefunxiao endflashkit createender <hunter|prey> <0-50小数> <名字...> §7- §f保存背包+末影箱为终章 Kit");
            entries.add("§e/gamefunxiao endflashkit hand <hunter|prey> <0-50小数> <名字...> §7- §f保存手中物品为终章 Kit");
            entries.add("§e/gamefunxiao endflashkit appendhand <kitId> §7- §f把手中物品追加到 Kit");
            entries.add("§e/gamefunxiao endflashkit appendenderhand <kitId> §7- §f把手中物品追加到 Kit 末影箱");
            entries.add("§e/gamefunxiao endflashkit chance <kitId> <0-50小数> §7- §f修改 Kit 权重");
            entries.add("§e/gamefunxiao endflashkit remove <kitId> §7- §f删除 Kit");
            entries.add("§e/gamefunxiao 调终章 §7- §f请求 gameing 进入固定终章调试世界");
            entries.add("§e/gamefunxiao flashuse <on|off|toggle|status> [玩家] §7- §f切换闪光测试能力");
            entries.add("§e/gamefunxiao flashmusic all <歌曲名> [Mall] §7- §f给全服播放闪光音符盒音乐");
            entries.add("§e/gamefunxiao flashmusic nearby <范围> <歌曲名> [Mall] §7- §f给附近玩家播放闪光音符盒音乐");
            entries.add("§e/gamefunxiao flashmusic stopall §7- §f停止所有闪光音符盒播放");
            entries.add("§e/gamefunxiao hg help [页码] §7- §f查看猎人游戏命令帮助");
        }

        sendPaginatedHelp(sender,
                "§x§5§5§F§F§A§A⚔ §x§6§6§F§F§B§BGameFun §x§7§7§F§F§C§C命令帮助 §x§5§5§F§F§A§A⚔",
                "/gamefunxiao help ",
                entries,
                page);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        // 检查并修复配置文件
        checkAndRepairConfigs();

        plugin.getConfigManager().reloadConfigs();
        plugin.getMessageManager().reloadMessages();
        plugin.getTabHeaderFooterManager().start();

        sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.reload_success"));
    }

    private void handleLobbyInteractionRegion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("帮助")) {
            sender.sendMessage("§x§8§8§D§D§F§F等待大厅交互区域");
            sender.sendMessage("§e/gamefunxiao lobbyinteract add <ID> <x1> <y1> <z1> <x2> <y2> <z2> [世界]");
            sender.sendMessage("§e/gamefunxiao lobbyinteract remove <ID>");
            sender.sendMessage("§e/gamefunxiao lobbyinteract list");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add", "set", "create", "添加", "设置" -> handleLobbyInteractionRegionAdd(sender, args);
            case "remove", "delete", "del", "删除" -> handleLobbyInteractionRegionRemove(sender, args);
            case "list", "列表" -> handleLobbyInteractionRegionList(sender);
            default -> sender.sendMessage("§x§F§F§8§8§5§5参数不对 §8| §7输入 §e/gamefunxiao lobbyinteract help");
        }
    }

    private void handleLobbyInteractionRegionAdd(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§x§F§F§8§8§5§5用法：§e/gamefunxiao lobbyinteract add <ID> <x1> <y1> <z1> <x2> <y2> <z2> [世界]");
            return;
        }
        String id = args[2].replace(".", "_").replace(" ", "_");
        int x1;
        int y1;
        int z1;
        int x2;
        int y2;
        int z2;
        try {
            x1 = Integer.parseInt(args[3]);
            y1 = Integer.parseInt(args[4]);
            z1 = Integer.parseInt(args[5]);
            x2 = Integer.parseInt(args[6]);
            y2 = Integer.parseInt(args[7]);
            z2 = Integer.parseInt(args[8]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§x§F§F§6§0§6§0坐标必须是整数。");
            return;
        }
        String worldName = args.length >= 10 ? args[9] : sender instanceof Player player ? player.getWorld().getName() : "world";
        String base = "lobby_interaction_regions.regions." + id;
        plugin.getConfig().set("lobby_interaction_regions.enabled", true);
        plugin.getConfig().set(base + ".world", worldName);
        plugin.getConfig().set(base + ".x1", x1);
        plugin.getConfig().set(base + ".y1", y1);
        plugin.getConfig().set(base + ".z1", z1);
        plugin.getConfig().set(base + ".x2", x2);
        plugin.getConfig().set(base + ".y2", y2);
        plugin.getConfig().set(base + ".z2", z2);
        plugin.saveConfig();
        sender.sendMessage("§x§5§5§F§F§A§A已保存等待大厅可交互区域 §f" + id + " §8| §7世界 §b" + worldName);
    }

    private void handleLobbyInteractionRegionRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§x§F§F§8§8§5§5用法：§e/gamefunxiao lobbyinteract remove <ID>");
            return;
        }
        String id = args[2].replace(".", "_").replace(" ", "_");
        String base = "lobby_interaction_regions.regions." + id;
        if (!plugin.getConfig().isSet(base)) {
            sender.sendMessage("§x§F§F§B§B§6§6没有找到区域 §f" + id);
            return;
        }
        plugin.getConfig().set(base, null);
        plugin.saveConfig();
        sender.sendMessage("§x§5§5§D§D§F§F已删除等待大厅可交互区域 §f" + id);
    }

    private void handleLobbyInteractionRegionList(CommandSender sender) {
        org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig()
                .getConfigurationSection("lobby_interaction_regions.regions");
        if (regions == null || regions.getKeys(false).isEmpty()) {
            sender.sendMessage("§x§B§B§B§B§B§B当前没有配置等待大厅可交互区域。");
            return;
        }
        sender.sendMessage("§x§8§8§D§D§F§F等待大厅可交互区域：");
        for (String id : regions.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection section = regions.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            sender.sendMessage("§8- §f" + id + " §7" + section.getString("world", "world")
                    + " §8(" + section.getInt("x1") + "," + section.getInt("y1") + "," + section.getInt("z1")
                    + " -> " + section.getInt("x2") + "," + section.getInt("y2") + "," + section.getInt("z2") + ")");
        }
    }

    private void handleCleanupRooms(CommandSender sender) {
        if (!sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        int removed = plugin.getChildServerManager().cleanupAllRegistryEntries();
        sender.sendMessage("§x§5§5§F§F§A§AGameFun §8» §a已清理 §e" + removed + " §a个跨服房间注册文件，幽灵房间会从列表消失。");
    }

    private void handleEditLobbyTemplate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (!player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        player.sendMessage("§x§F§F§8§8§5§5用法：§e/gamefunxiao editlobbytemplate <大厅名>");
    }

    private void handleEditLobbyTemplate(CommandSender sender, String lobbyWorldName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (!player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        if (lobbyWorldName == null || lobbyWorldName.isBlank()) {
            player.sendMessage("§x§F§F§8§8§5§5用法：§e/gamefunxiao editlobbytemplate <大厅名>");
            return;
        }

        String target = lobbyWorldName.trim();
        if (target.equalsIgnoreCase("hugamelobby")) {
            plugin.getChildServerManager().requestTemplateLobbyEdit(player);
            return;
        }

        for (GameMode mode : GameMode.getMiniGameMapEditableModes()) {
            for (MiniGameMapManager.MapDefinition definition : plugin.getMiniGameMapManager().getMapDefinitions(mode)) {
                if (definition.lobbyTemplateWorld().equalsIgnoreCase(target)) {
                    plugin.getMiniGameMapManager().requestEdit(player, mode, definition.mapId(),
                            MiniGameMapManager.EditWorldKind.LOBBY, definition.maxPlayers());
                    return;
                }
            }
        }

        player.sendMessage("§x§F§F§8§8§5§5⚠ §c没有找到这个等待大厅：§e" + target);
    }

    private void handleMiniGameMap(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (!player.hasPermission("gamefunxiao.admin.minigamemap") && !player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        if (args.length < 2) {
            sendMiniGameMapUsage(player);
            return;
        }

        String action = normalizeMapAction(args[1]);
        switch (action) {
            case "help" -> sendMiniGameMapUsage(player);
            case "create" -> handleMiniGameMapCreate(player, args);
            case "edit" -> handleMiniGameMapEdit(player, args);
            case "active" -> handleMiniGameMapSetActive(player, args);
            case "list" -> handleMiniGameMapList(player, args);
            case "info" -> handleMiniGameMapInfo(player, args);
            case "enable" -> handleMiniGameMapEnable(player, args, true);
            case "disable" -> handleMiniGameMapEnable(player, args, false);
            case "delete" -> handleMiniGameMapDelete(player, args);
            case "name" -> handleMiniGameMapName(player, args);
            case "range" -> handleMiniGameMapRange(player, args);
            case "time" -> handleMiniGameMapTime(player, args);
            case "theme" -> handleMiniGameMapTheme(player, args);
            case "boundary" -> handleMiniGameMapBoundary(player, args);
            case "autocreate" -> handleMiniGameMapAutoCreate(player, args);
            default -> sendMiniGameMapUsage(player);
        }
    }

    private String normalizeMapAction(String raw) {
        String action = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        return switch (action) {
            case "help", "?", "帮助" -> "help";
            case "create", "new", "add", "创建", "新增" -> "create";
            case "edit", "open", "编辑", "打开" -> "edit";
            case "setactive", "active", "use", "select", "current", "设置当前", "当前" -> "active";
            case "list", "ls", "列表" -> "list";
            case "info", "show", "查看", "信息" -> "info";
            case "enable", "on", "启用" -> "enable";
            case "disable", "off", "禁用" -> "disable";
            case "delete", "del", "remove", "rm", "删除", "移除" -> "delete";
            case "name", "rename", "display", "命名", "改名", "显示名" -> "name";
            case "range", "players", "人数", "范围" -> "range";
            case "time", "timing", "时间" -> "time";
            case "theme", "主题" -> "theme";
            case "boundary", "border", "边界" -> "boundary";
            case "autocreate", "template", "自动模板" -> "autocreate";
            default -> action;
        };
    }

    private void handleMiniGameMapCreate(Player player, String[] args) {
        if (args.length < 4) {
            sendMiniGameMapUsage(player);
            return;
        }
        GameMode mode = parseExplicitGameMode(player, args[2]);
        if (mode == null) {
            return;
        }
        String mapId = plugin.getMiniGameMapManager().normalizeMapId(args[3]);
        int maxPlayers = args.length >= 5 ? parsePositiveInt(player, args[4]) : 16;
        if (maxPlayers <= 0) {
            return;
        }
        boolean alreadyExists = plugin.getMiniGameMapManager().hasMapDefinition(mode, mapId);
        MiniGameMapManager.MapDefinition definition = plugin.getMiniGameMapManager().ensureMapDefinition(mode, mapId, maxPlayers);
        if (args.length >= 6) {
            String displayName = String.join(" ", Arrays.copyOfRange(args, 5, args.length)).replace('&', '§');
            definition = plugin.getMiniGameMapManager().setMapDisplayName(mode, mapId, displayName);
        }
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition == null ? mode : definition.mode(), mapId);
        placeholders.put("max", String.valueOf(maxPlayers));
        placeholders.put("map", definition == null ? mapId : definition.displayName());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix(alreadyExists ? "minigame_map.already_exists" : "minigame_map.created", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.58f, 1.45f);
    }

    private void handleMiniGameMapEdit(Player player, String[] args) {
        if (args.length < 4) {
            sendMiniGameMapUsage(player);
            return;
        }
        GameMode mode = parseExplicitGameMode(player, args[2]);
        if (mode == null) {
            return;
        }
        String mapId = plugin.getMiniGameMapManager().normalizeMapId(args[3]);
        if (!plugin.getMiniGameMapManager().hasMapDefinition(mode, mapId)) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.not_found", baseMapCommandPlaceholders(mode, mapId)));
            return;
        }
        MiniGameMapManager.EditWorldKind kind = args.length >= 5 ? MiniGameMapManager.EditWorldKind.fromString(args[4]) : MiniGameMapManager.EditWorldKind.GAME;
        MiniGameMapManager.MapDefinition definition = plugin.getMiniGameMapManager().getMapDefinition(mode, mapId);
        plugin.getMiniGameMapManager().requestEdit(player, mode, mapId, kind, definition == null ? 16 : definition.maxPlayers());
    }

    private void handleMiniGameMapSetActive(Player player, String[] args) {
        if (args.length < 4) {
            sendMiniGameMapUsage(player);
            return;
        }
        GameMode mode = parseExplicitGameMode(player, args[2]);
        if (mode == null) {
            return;
        }
        String rawMapId = args[3];
        boolean random = plugin.getMiniGameMapManager().isRandomActiveMapId(rawMapId);
        String mapId = random ? "random" : plugin.getMiniGameMapManager().normalizeMapId(rawMapId);
        if (!random && !plugin.getMiniGameMapManager().hasMapDefinition(mode, mapId)) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.not_found", baseMapCommandPlaceholders(mode, mapId)));
            return;
        }
        plugin.getMiniGameMapManager().setActiveMap(mode, mapId);
        Map<String, String> placeholders = baseMapCommandPlaceholders(mode, mapId);
        placeholders.put("id", mapId);
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.active_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.65f, 1.28f);
    }

    private void handleMiniGameMapList(Player player, String[] args) {
        if (args.length < 3) {
            for (GameMode mode : GameMode.getMiniGameMapEditableModes()) {
                sendMiniGameMapListForMode(player, mode);
            }
            return;
        }
        GameMode mode = parseExplicitGameMode(player, args[2]);
        if (mode != null) {
            sendMiniGameMapListForMode(player, mode);
        }
    }

    private void sendMiniGameMapListForMode(Player player, GameMode mode) {
        List<MiniGameMapManager.MapDefinition> maps = plugin.getMiniGameMapManager().getMapDefinitions(mode);
        String active = plugin.getMiniGameMapManager().getActiveMapId(mode);
        String activeDisplay = plugin.getMiniGameMapManager().isRandomActiveMapId(active) ? "random" : active;
        player.sendMessage("§x§7§D§F§F§C§8✦ §x§A§9§F§F§E§4地图编辑 §8» §b" + mode.getDisplayName() + " §f地图列表 §8(当前 §e" + activeDisplay + "§8)");
        if (maps.isEmpty()) {
            player.sendMessage("§8· §7暂无地图，使用 §e/gamefunxiao map create " + mode.getId() + " default 16 §7创建。");
            return;
        }
        for (MiniGameMapManager.MapDefinition definition : maps) {
            String mark = definition.mapId().equalsIgnoreCase(active) ? "§a✔ " : "§8- ";
            String enabled = definition.enabled() ? "§a启用" : "§c禁用";
            player.sendMessage(mark + "§e" + definition.mapId() + " §7| §f" + definition.displayName()
                    + " §7| " + enabled
                    + " §7| 人数 §b" + definition.minPlayers() + "§7-§b" + definition.maxPlayers()
                    + " §7| 出生点 §d" + definition.gameSpawns().size()
                    + " §7| 主题 §6" + definition.themeId());
        }
    }

    private void handleMiniGameMapInfo(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 4);
        if (definition != null) {
            plugin.getMiniGameMapManager().sendMapInfo(player, definition);
        }
    }

    private void handleMiniGameMapEnable(Player player, String[] args, boolean enabled) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 4);
        if (definition == null) {
            return;
        }
        definition = plugin.getMiniGameMapManager().setMapEnabled(definition.mode(), definition.mapId(), enabled);
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix(enabled ? "minigame_map.enabled" : "minigame_map.disabled", baseMapCommandPlaceholders(definition)));
        player.playSound(player.getLocation(), enabled ? org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME : org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.62f, enabled ? 1.35f : 0.82f);
    }

    private void handleMiniGameMapDelete(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 4);
        if (definition == null) {
            return;
        }
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        boolean deleted = plugin.getMiniGameMapManager().deleteMapDefinition(definition.mode(), definition.mapId());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix(deleted ? "minigame_map.deleted" : "minigame_map.not_found", placeholders));
        player.playSound(player.getLocation(), deleted ? org.bukkit.Sound.BLOCK_ANVIL_LAND : org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.55f, deleted ? 1.65f : 0.8f);
    }

    private void handleMiniGameMapName(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 5);
        if (definition == null) return;
        String displayName = String.join(" ", Arrays.copyOfRange(args, 4, args.length)).replace('&', '§');
        definition = plugin.getMiniGameMapManager().setMapDisplayName(definition.mode(), definition.mapId(), displayName);
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.renamed", baseMapCommandPlaceholders(definition)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.52f);
    }

    private void handleMiniGameMapRange(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 6);
        if (definition == null) return;
        int min = parsePositiveInt(player, args[4]);
        int max = parsePositiveInt(player, args[5]);
        if (min <= 0 || max <= 0) return;
        if (max < min) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.invalid_range", baseMapCommandPlaceholders(definition)));
            return;
        }
        definition = plugin.getMiniGameMapManager().setMapPlayerRange(definition.mode(), definition.mapId(), min, max);
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        placeholders.put("min", String.valueOf(definition.minPlayers()));
        placeholders.put("max", String.valueOf(definition.maxPlayers()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.range_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, 0.58f, 1.38f);
    }

    private void handleMiniGameMapTime(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 5);
        if (definition == null) return;
        int gameTime = parsePositiveInt(player, args[4]);
        if (gameTime <= 0) return;
        int itemInterval = args.length >= 6 ? parsePositiveInt(player, args[5]) : definition.randomItemIntervalSeconds();
        int eventInterval = args.length >= 7 ? parsePositiveInt(player, args[6]) : definition.randomEventIntervalSeconds();
        if (itemInterval <= 0 || eventInterval <= 0) return;
        definition = plugin.getMiniGameMapManager().setMapTiming(definition.mode(), definition.mapId(), gameTime, itemInterval, eventInterval);
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        placeholders.put("time", String.valueOf(definition.gameTimeSeconds()));
        placeholders.put("item", String.valueOf(definition.randomItemIntervalSeconds()));
        placeholders.put("event", String.valueOf(definition.randomEventIntervalSeconds()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.timing_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.54f, 1.7f);
    }

    private void handleMiniGameMapTheme(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 5);
        if (definition == null) return;
        definition = plugin.getMiniGameMapManager().setMapTheme(definition.mode(), definition.mapId(), args[4]);
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        placeholders.put("theme", definition.themeId());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.theme_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.48f, 1.35f);
    }

    private void handleMiniGameMapBoundary(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 5);
        if (definition == null) return;
        double radius = parsePositiveDouble(player, args[4]);
        if (radius <= 0.0D) return;
        int eliminationY = args.length >= 6 ? parseInteger(player, args[5]) : Integer.MIN_VALUE;
        if (args.length >= 6 && eliminationY == Integer.MIN_VALUE) return;
        definition = plugin.getMiniGameMapManager().setMapBoundary(definition.mode(), definition.mapId(), radius, eliminationY);
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        placeholders.put("radius", String.format(Locale.ROOT, "%.1f", definition.boundaryRadius()));
        placeholders.put("y", String.valueOf(definition.eliminationY()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.boundary_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CONDUIT_ACTIVATE, 0.52f, 1.28f);
    }

    private void handleMiniGameMapAutoCreate(Player player, String[] args) {
        MiniGameMapManager.MapDefinition definition = requireMapDefinition(player, args, 5);
        if (definition == null) return;
        Boolean value = parseBoolean(args[4]);
        if (value == null) {
            player.sendMessage("§x§F§F§8§8§5§5⚠ §c请输入 true 或 false。");
            return;
        }
        definition = plugin.getMiniGameMapManager().setMapAutoCreateTemplate(definition.mode(), definition.mapId(), value);
        Map<String, String> placeholders = baseMapCommandPlaceholders(definition);
        placeholders.put("value", definition.autoCreateTemplate() ? "开启" : "关闭");
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.autocreate_set", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_COMPARATOR_CLICK, 0.62f, value ? 1.45f : 0.78f);
    }

    private MiniGameMapManager.MapDefinition requireMapDefinition(Player player, String[] args, int minArgs) {
        if (args.length < minArgs) {
            sendMiniGameMapUsage(player);
            return null;
        }
        GameMode mode = parseExplicitGameMode(player, args[2]);
        if (mode == null) return null;
        String mapId = plugin.getMiniGameMapManager().normalizeMapId(args[3]);
        MiniGameMapManager.MapDefinition definition = plugin.getMiniGameMapManager().getMapDefinition(mode, mapId);
        if (definition == null) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.not_found", baseMapCommandPlaceholders(mode, mapId)));
            return null;
        }
        return definition;
    }

    private void sendMiniGameMapUsage(Player player) {
        player.sendMessage("§x§7§D§F§F§C§8✦ §x§A§9§F§F§E§4GameFun 地图编辑命令");
        player.sendMessage("§e/gamefunxiao map list [模式] §7- §f查看全部或指定模式地图");
        player.sendMessage("§e/gamefunxiao map create <模式> <地图ID> [最大人数] [显示名...] §7- §f创建独立地图配置");
        player.sendMessage("§e/gamefunxiao map edit <模式> <地图ID> [game|lobby] §7- §f进入游戏地图或等待大厅编辑");
        player.sendMessage("§e/gamefunxiao map active <模式> <地图ID|random> §7- §f切换固定地图或随机地图池");
        player.sendMessage("§e/gamefunxiao map enable|disable|delete <模式> <地图ID> §7- §f启用、禁用、删除地图");
        player.sendMessage("§e/gamefunxiao map name|range|time|theme|boundary|autocreate <模式> <地图ID> ... §7- §f修改显示名、人数、时间、主题、边界、模板策略");
        player.sendMessage("§8例如: §b/gamefunxiao map create lucky_pillars small 8 羊毛圆盘");
    }

    private GameMode parseExplicitGameMode(Player player, String modeId) {
        GameMode mode = GameMode.findByIdStrict(modeId).filter(GameMode::isMiniGameMapEditableMode).orElse(null);
        if (mode != null) return mode;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", modeId == null ? "null" : modeId);
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.invalid_mode", placeholders));
        return null;
    }

    private Map<String, String> baseMapCommandPlaceholders(MiniGameMapManager.MapDefinition definition) {
        return baseMapCommandPlaceholders(definition == null ? null : definition.mode(), definition == null ? "unknown" : definition.mapId(), definition == null ? "未知地图" : definition.displayName());
    }

    private Map<String, String> baseMapCommandPlaceholders(GameMode mode, String mapId) {
        return baseMapCommandPlaceholders(mode, mapId, mapId == null ? "unknown" : mapId);
    }

    private Map<String, String> baseMapCommandPlaceholders(GameMode mode, String mapId, String displayName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", mode == null ? "未知模式" : mode.getDisplayName());
        placeholders.put("id", mapId == null ? "unknown" : mapId);
        placeholders.put("map", displayName == null ? "未知地图" : displayName);
        return placeholders;
    }

    private int parsePositiveInt(Player player, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException ignored) {
        }
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
        return -1;
    }

    private int parseInteger(Player player, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
            return Integer.MIN_VALUE;
        }
    }

    private double parsePositiveDouble(Player player, String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed > 0.0D) return parsed;
        } catch (NumberFormatException ignored) {
        }
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
        return -1.0D;
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return null;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("on") || lower.equals("yes") || lower.equals("开启")) return true;
        if (lower.equals("false") || lower.equals("off") || lower.equals("no") || lower.equals("关闭")) return false;
        return null;
    }

    private void handleEndFlashDebug(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (!player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        plugin.getChildServerManager().requestEndFlashTuning(player);
    }

    private void handleCoins(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("currency.usage"));
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("get")) {
            Player target = resolveCoinTarget(sender, args, 2);
            if (target == null) return;
            sendCoinResult(sender, "currency.balance", target, plugin.getPlayerDataManager().getCoins(target.getUniqueId()));
            return;
        }

        if (!action.equals("set") && !action.equals("add")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("currency.usage"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("currency.usage"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
            return;
        }

        Player target = resolveCoinTarget(sender, args, 3);
        if (target == null) return;

        if (action.equals("set")) {
            plugin.getPlayerDataManager().setCoins(target.getUniqueId(), amount);
        } else {
            plugin.getPlayerDataManager().addCoins(target.getUniqueId(), amount);
        }

        int balance = plugin.getPlayerDataManager().getCoins(target.getUniqueId());
        sendCoinResult(sender, action.equals("set") ? "currency.set_success" : "currency.add_success", target, balance);
        if (!sender.equals(target)) {
            sendCoinResult(target, "currency.changed_notice", target, balance);
        }
    }

    private void handleEndFlashKit(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT)
                : sender instanceof Player && sender.hasPermission("gamefunxiao.admin") ? "menu" : "list";
        EndFlashKitManager manager = plugin.getEndFlashKitManager();

        if (action.equals("menu") || action.equals("gui") || action.equals("菜单") || action.equals("admin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
                return;
            }
            if (!player.hasPermission("gamefunxiao.admin")) {
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
                return;
            }
            plugin.getMenuManager().openEndFlashKitAdminMenu(player);
            return;
        }

        if (action.equals("list")) {
            sendEndFlashKitList(sender, manager);
            return;
        }

        if (action.equals("select")) {
            sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a终章 Kit 已改为每局按概率随机抽取，不支持固定指定Kit。");
            return;
        }

        if (!sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        switch (action) {
            case "create", "save" -> handleEndFlashKitCreate(sender, args, false, false);
            case "createender", "saveender" -> handleEndFlashKitCreate(sender, args, false, true);
            case "hand", "savehand" -> handleEndFlashKitCreate(sender, args, true, false);
            case "appendhand", "addhand" -> handleEndFlashKitAppendHand(sender, args);
            case "appendenderhand", "addenderhand" -> handleEndFlashKitAppendEnderHand(sender, args);
            case "chance", "setchance" -> handleEndFlashKitChance(sender, args);
            case "remove", "delete" -> handleEndFlashKitRemove(sender, args);
            case "reload" -> {
                manager.load();
                sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已重载终章闪光 Kit 数据。");
            }
            default -> sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit menu|list|create|createender|hand|appendhand|appendenderhand|chance|remove");
        }
    }

    private void handleEndFlashKitCreate(CommandSender sender, String[] args, boolean handOnly, boolean includeEnderChest) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (args.length < 5) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit "
                    + (handOnly ? "hand" : includeEnderChest ? "createender" : "create") + " <hunter|prey> <0-50小数> <名字...>");
            return;
        }
        EndFlashKitManager.Role role = EndFlashKitManager.Role.fromId(args[2]);
        if (role == null) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c角色只能是 hunter/prey。");
            return;
        }
        double chance;
        try {
            chance = EndFlashKitManager.clampChance(Double.parseDouble(args[3]));
        } catch (NumberFormatException exception) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        List<org.bukkit.inventory.ItemStack> items = handOnly
                ? plugin.getEndFlashKitManager().snapshotHand(player)
                : plugin.getEndFlashKitManager().snapshotInventory(player);
        if (items.isEmpty()) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c没有可上传的物品。");
            return;
        }
        List<org.bukkit.inventory.ItemStack> enderItems = includeEnderChest
                ? plugin.getEndFlashKitManager().snapshotEnderChest(player)
                : Collections.emptyList();
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().createKit(role, name, chance, items, enderItems, player);
        player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已创建终章 §e" + role.displayName()
                + " §aKit：§d" + kit.id() + " §7(" + kit.displayName() + "§7，权重 " + EndFlashKitManager.formatChance(kit.chance())
                + (includeEnderChest ? "，含末影箱 " + enderItems.size() + " 件" : "") + ")");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.72f, 1.32f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.5f, 1.75f);
    }

    private void handleEndFlashKitAppendHand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit appendhand <kitId>");
            return;
        }
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.getEndFlashKitManager().appendItem(args[2], hand, player)) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c追加失败，请检查 KitId 和手中物品。");
            return;
        }
        player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已把手中物品追加到 §d" + args[2] + "§a。");
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BUNDLE_INSERT, 0.8f, 1.35f);
    }

    private void handleEndFlashKitAppendEnderHand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit appendenderhand <kitId>");
            return;
        }
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.getEndFlashKitManager().appendEnderChestItem(args[2], hand, player)) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c追加失败，请检查 KitId 和手中物品。");
            return;
        }
        player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已把手中物品追加到 §d" + args[2] + " §a的末影箱 Kit。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.72f, 1.42f);
    }

    private void handleEndFlashEnderChest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null || room.getGameMode() != GameMode.END_FLASH || room.getState() != org.gamefunxiao.game.RoomState.PLAYING
                || !room.isGameActuallyStarted()) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§8§8⚠ §c只有终章 · 闪光正式开始后才能使用。");
            return;
        }
        if (!room.isPrey(player.getUniqueId())) {
            double distance = nearestPreyDistance(room, player);
            if (distance < 100.0D || distance > 170.0D) {
                player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                        + "§x§B§B§8§8§F§F✦ §f需要距离猎物 §e100~170格§f 才能打开末影箱，当前约 §c"
                        + (distance < 0 ? "无同世界猎物" : String.format(Locale.ROOT, "%.1f格", distance)) + "§f。");
                return;
            }
        }
        player.openInventory(player.getEnderChest());
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.82f, 1.28f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.32f, 1.68f);
    }

    private void handleEndFlashCompass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null || room.getGameMode() != GameMode.END_FLASH || room.getState() != org.gamefunxiao.game.RoomState.PLAYING) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§8§8⚠ §c只有终章 · 闪光中才能获取指南针。");
            return;
        }
        if (!room.isHunter(player.getUniqueId()) && !(room.isPrey(player.getUniqueId()) && room.getPreyUUIDs().size() >= 2)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§8§8⚠ §c当前身份不能获取终章指南针。");
            return;
        }
        if (hasCompass(player)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§D§D§8§8✦ §e你已经有指南针了。");
            return;
        }
        if (room.isHunter(player.getUniqueId())) {
            plugin.getGameManager().giveHunterItems(player, room);
        } else {
            plugin.getGameManager().giveFlashPreyItems(player, room);
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.78f, 1.28f);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§5§5§F§F§A§A✦ §a已补发终章指南针。");
    }

    private double nearestPreyDistance(org.gamefunxiao.game.GameRoom room, Player player) {
        double nearest = Double.MAX_VALUE;
        boolean found = false;
        for (UUID preyUuid : room.getPreyUUIDs()) {
            if (preyUuid.equals(player.getUniqueId())) {
                continue;
            }
            if (room.isSpectator(preyUuid)) {
                continue;
            }
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null || !prey.isOnline() || !prey.getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = prey.getLocation().distance(player.getLocation());
            if (distance < nearest) {
                nearest = distance;
            }
            found = true;
        }
        return found ? nearest : -1.0D;
    }

    private boolean hasCompass(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.COMPASS) {
                return true;
            }
        }
        return false;
    }

    private void handleEndFlashKitChance(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit chance <kitId> <0-50小数>");
            return;
        }
        double chance;
        try {
            chance = EndFlashKitManager.clampChance(Double.parseDouble(args[3]));
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
            return;
        }
        Player editor = sender instanceof Player player ? player : null;
        if (!plugin.getEndFlashKitManager().setChance(args[2], chance, editor)) {
            sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c没有找到这个 Kit。");
            return;
        }
        sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已把 §d" + args[2] + " §a权重设置为 §e" + EndFlashKitManager.formatChance(chance) + "§a。");
    }

    private void handleEndFlashKitRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §e用法: /gamefunxiao endflashkit remove <kitId>");
            return;
        }
        if (!plugin.getEndFlashKitManager().removeKit(args[2])) {
            sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c没有找到这个 Kit。");
            return;
        }
        sender.sendMessage("§x§B§B§8§8§F§FGameFun §8» §a已删除终章闪光 Kit：§d" + args[2]);
    }

    private void sendEndFlashKitList(CommandSender sender, EndFlashKitManager manager) {
        sender.sendMessage("");
        sender.sendMessage("§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F终章 · 闪光 Kit §7(概率会自动按权重平衡)");
        for (EndFlashKitManager.Role role : EndFlashKitManager.Role.values()) {
            sender.sendMessage("§e" + role.displayName() + "§7:");
            List<EndFlashKitManager.Kit> kits = manager.getKits(role);
            if (kits.isEmpty()) {
                sender.sendMessage("  §8- 暂无，开局会使用默认保底 Kit");
                continue;
            }
            double total = kits.stream().mapToDouble(EndFlashKitManager.Kit::chance).sum();
            for (EndFlashKitManager.Kit kit : kits) {
                double balanced = total <= 0 ? 100.0D / kits.size() : kit.chance() * 100.0D / total;
                sender.sendMessage("  §8- §d" + kit.id() + " §7| §f" + kit.displayName()
                        + " §7| 输入权重 §e" + EndFlashKitManager.formatChance(kit.chance()) + " §7| 实际约 §b" + String.format(Locale.ROOT, "%.1f", balanced) + "%"
                        + " §7| 最后编辑 §a" + kit.lastEditorName() + " §8/ §e" + kit.lastEditedAtText());
            }
        }
        sender.sendMessage("");
    }

    private void handleFlashUse(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gamefunxiao.flashuse") && !sender.hasPermission("gamefunxiao.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }

        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
        if (!action.equals("on") && !action.equals("off") && !action.equals("toggle") && !action.equals("status")) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("flash_use.usage"));
            return;
        }

        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("gamefunxiao.admin")) {
                sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                target = Bukkit.getPlayer(args[2]);
            }
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[2]);
                sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_not_found", placeholders));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("currency.console_need_player"));
            return;
        }

        boolean currentlyEnabled = plugin.getFlashModeManager().isStandaloneFlashEnabled(target.getUniqueId());
        if (action.equals("status")) {
            sendFlashUseStatus(sender, target, currentlyEnabled);
            return;
        }

        boolean enabled = action.equals("toggle") ? !currentlyEnabled : action.equals("on");
        if (enabled && plugin.getRoomManager().isInRoom(target.getUniqueId())) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("flash_use.must_leave_room", placeholders));
            if (!sender.equals(target)) {
                target.sendMessage(plugin.getMessageManager().getMessageWithPrefix("flash_use.must_leave_room_self"));
            }
            return;
        }

        plugin.getFlashModeManager().setStandaloneFlashEnabled(target, enabled);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        String key = enabled ? "flash_use.enabled" : "flash_use.disabled";
        target.sendMessage(plugin.getMessageManager().getMessageWithPrefix(key, placeholders));
        target.playSound(target.getLocation(), enabled ? org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME : org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.75f, enabled ? 1.55f : 0.82f);
        if (!sender.equals(target)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix(enabled ? "flash_use.target_enabled" : "flash_use.target_disabled", placeholders));
        }
    }

    private void sendFlashUseStatus(CommandSender sender, Player target, boolean enabled) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("state", enabled ? "§a已开启" : "§c已关闭");
        sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("flash_use.status", placeholders));
    }

    private void handleFlashMusic(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "help";
        switch (action) {
            case "help", "?" -> sendFlashMusicHelp(sender);
            case "list", "songs", "曲库" -> handleFlashMusicList(sender, args);
            case "stop", "停止" -> handleFlashMusicStop(sender);
            case "stopall", "停止全部" -> handleFlashMusicStopAll(sender);
            case "play", "播放" -> handleFlashMusicPlaySelf(sender, args, 2);
            case "all", "broadcast", "全服" -> handleFlashMusicPlayAll(sender, args, 2);
            case "nearby", "near", "附近" -> handleFlashMusicPlayNearby(sender, args);
            default -> handleFlashMusicPlaySelf(sender, args, 1);
        }
    }

    private void sendFlashMusicHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§x§B§B§8§8§F§F♫ §x§D§D§A§A§F§F闪光音符盒音乐");
        sender.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        sender.sendMessage("§e/gamefunxiao flashmusic list [页] §7- §f查看曲库");
        sender.sendMessage("§e/gamefunxiao flashmusic play <歌曲名> [Mall] §7- §f给自己播放，Mall/全音=整首全音");
        sender.sendMessage("§e/gamefunxiao flashmusic stop §7- §f停止自己的播放");
        if (hasFlashMusicAdmin(sender)) {
            sender.sendMessage("§e/gamefunxiao flashmusic all <歌曲名> [Mall] §7- §f给全服播放");
            sender.sendMessage("§e/gamefunxiao flashmusic nearby <范围> <歌曲名> [Mall] §7- §f给附近玩家播放");
            sender.sendMessage("§e/gamefunxiao flashmusic stopall §7- §f停止所有播放");
        }
        sender.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        sender.sendMessage("");
    }

    private void handleFlashMusicList(CommandSender sender, String[] args) {
        List<String> names = plugin.getFlashModeManager().getFlashNoteMusicNames();
        if (names.isEmpty()) {
            sender.sendMessage(flashMusicPrefix() + "§c曲库为空，请检查 flash-note-songs。");
            return;
        }
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        int pageSize = 12;
        int maxPage = Math.max(1, (int) Math.ceil(names.size() / (double) pageSize));
        page = Math.min(page, maxPage);
        int from = (page - 1) * pageSize;
        int to = Math.min(names.size(), from + pageSize);
        sender.sendMessage("");
        sender.sendMessage("§x§B§B§8§8§F§F♫ §x§D§D§A§A§F§F闪光曲库 §7(" + page + "/" + maxPage + "，共 " + names.size() + " 首)");
        for (int i = from; i < to; i++) {
            sender.sendMessage("§8- §e" + (i + 1) + "§7. §f" + names.get(i));
        }
        sender.sendMessage("§7播放：§e/gamefunxiao flashmusic play <歌曲名>");
        sender.sendMessage("");
    }

    private void handleFlashMusicStop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        boolean stopped = plugin.getFlashModeManager().stopFlashNoteMusic(player);
        player.sendMessage(flashMusicPrefix() + (stopped ? "§a已停止你的音符盒音乐。" : "§e你当前没有正在播放的音符盒音乐。"));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.65f, 0.72f);
    }

    private void handleFlashMusicStopAll(CommandSender sender) {
        if (!hasFlashMusicAdmin(sender)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        int stopped = plugin.getFlashModeManager().stopAllFlashNoteMusic();
        sender.sendMessage(flashMusicPrefix() + "§a已停止 §e" + stopped + " §a个闪光音符盒播放任务。");
    }

    private void handleFlashMusicPlaySelf(CommandSender sender, String[] args, int songStartIndex) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (args.length <= songStartIndex) {
            player.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic play <歌曲名>");
            return;
        }
        FlashMusicQuery musicQuery = parseFlashMusicQuery(args, songStartIndex);
        if (musicQuery.query().isBlank()) {
            player.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic play <歌曲名> [Mall]");
            return;
        }
        playFlashMusicToTargets(player, List.of(player), musicQuery.query(), musicQuery.fullMode());
    }

    private void handleFlashMusicPlayAll(CommandSender sender, String[] args, int songStartIndex) {
        if (!hasFlashMusicAdmin(sender)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        if (args.length <= songStartIndex) {
            sender.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic all <歌曲名>");
            return;
        }
        FlashMusicQuery musicQuery = parseFlashMusicQuery(args, songStartIndex);
        if (musicQuery.query().isBlank()) {
            sender.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic all <歌曲名> [Mall]");
            return;
        }
        playFlashMusicToTargets(sender, new ArrayList<>(Bukkit.getOnlinePlayers()), musicQuery.query(), musicQuery.fullMode());
    }

    private void handleFlashMusicPlayNearby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        if (!hasFlashMusicAdmin(player)) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        if (args.length < 4) {
            player.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic nearby <范围> <歌曲名>");
            return;
        }
        double radius;
        try {
            radius = Math.max(1.0D, Math.min(256.0D, Double.parseDouble(args[2])));
        } catch (NumberFormatException exception) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
            return;
        }
        FlashMusicQuery musicQuery = parseFlashMusicQuery(args, 3);
        if (musicQuery.query().isBlank()) {
            player.sendMessage(flashMusicPrefix() + "§e用法: /gamefunxiao flashmusic nearby <范围> <歌曲名> [Mall]");
            return;
        }
        List<Player> targets = player.getWorld().getPlayers().stream()
                .filter(target -> target.getLocation().distanceSquared(player.getLocation()) <= radius * radius)
                .toList();
        playFlashMusicToTargets(player, targets, musicQuery.query(), musicQuery.fullMode());
    }

    private void playFlashMusicToTargets(CommandSender sender, Collection<Player> targets, String query, boolean fullMode) {
        var result = plugin.getFlashModeManager().playFlashNoteMusic(targets, query, 3, 0.82f, fullMode);
        if (!result.success()) {
            sender.sendMessage(flashMusicPrefix() + "§c没有找到这首曲子：§e" + query + " §7（用 §f/gamefunxiao flashmusic list §7查看）");
            return;
        }
        sender.sendMessage(flashMusicPrefix() + "§a开始播放 §d" + result.songName() + " §7("
                + result.noteCount() + "音，" + result.listenerCount() + "人收听§7)");
        if (fullMode) {
            sender.sendMessage(flashMusicPrefix() + "§x§8§8§D§D§F§F已启用 Mall 完整播放：§f整首谱子里的全部音都会播放。");
        }
        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.75f, 1.55f);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.55f, 1.25f);
        }
    }

    private FlashMusicQuery parseFlashMusicQuery(String[] args, int startIndex) {
        int endIndex = args.length;
        boolean fullMode = false;
        if (endIndex > startIndex && isFlashMusicFullModeToken(args[endIndex - 1])) {
            fullMode = true;
            endIndex--;
        }
        return new FlashMusicQuery(String.join(" ", Arrays.copyOfRange(args, startIndex, endIndex)).trim(), fullMode);
    }

    private boolean isFlashMusicFullModeToken(String token) {
        if (token == null) {
            return false;
        }
        return token.equalsIgnoreCase("Mall")
                || token.equalsIgnoreCase("full")
                || token.equalsIgnoreCase("allnotes")
                || token.equalsIgnoreCase("allnote")
                || token.equalsIgnoreCase("全音")
                || token.equalsIgnoreCase("完整");
    }

    private record FlashMusicQuery(String query, boolean fullMode) {
    }

    private String flashMusicPrefix() {
        return "§x§B§B§8§8§F§F♫ §x§D§D§A§A§F§F闪光音乐 §8» ";
    }

    private boolean hasFlashMusicAdmin(CommandSender sender) {
        return sender.hasPermission("gamefunxiao.admin") || sender.hasPermission("gamefunxiao.flashmusic.admin");
    }

    private Player resolveCoinTarget(CommandSender sender, String[] args, int targetIndex) {
        if (args.length > targetIndex) {
            Player target = Bukkit.getPlayerExact(args[targetIndex]);
            if (target == null) {
                target = Bukkit.getPlayer(args[targetIndex]);
            }
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[targetIndex]);
                sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_not_found", placeholders));
                return null;
            }
            return target;
        }

        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("currency.console_need_player"));
        return null;
    }

    private void sendCoinResult(CommandSender receiver, String messageKey, Player target, int balance) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("amount", String.valueOf(balance));
        placeholders.put("currency", plugin.getConfigManager().getMiniGameCurrencyName());
        receiver.sendMessage(plugin.getMessageManager().getMessageWithPrefix(messageKey, placeholders));
    }

    private void handleRemovedUnstableMace(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix()
                + "§x§F§F§8§8§8§8✦ §7重锤核心强化已移除，§b核心盾牌§7仍可正常使用。");
        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.58f, 0.72f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BLOCK, 0.34f, 1.82f);
        }
    }

    private void checkAndRepairConfigs() {
        // 检查config.yml
        if (!new java.io.File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }

        // 检查messages.yml
        if (!new java.io.File(plugin.getDataFolder(), "messages.yml").exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // 检查config文件夹
        java.io.File configFolder = new java.io.File(plugin.getDataFolder(), "config");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        // 检查各个配置文件
        String[] configFiles = {"gamemodes.yml", "modifiers.yml", "rewards.yml", "scoreboard.yml", "minigame-maps.yml", "minigames.yml"};
        for (String fileName : configFiles) {
            java.io.File file = new java.io.File(configFolder, fileName);
            if (!file.exists()) {
                plugin.saveResource("config/" + fileName, false);
            }
        }
    }

    private void handleCommandBranch(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        CommandBackOptions backOptions = parseBackCommand(args);
        args = backOptions.args();
        String backCommand = backOptions.backCommand();

        if (args.length == 1) {
            plugin.getMenuManager().openCommandGatewayMenu(player);
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "help", "帮助" -> sendCommandBranchHelp(player);
            case "menu", "open", "gui", "打开", "界面" -> {
                if (args.length < 3) {
                    plugin.getMenuManager().openCommandGatewayMenu(player);
                    return;
                }
                openCommandMenuTarget(player, args[2], backCommand);
            }
            case "rooms", "roomlist", "房间", "查看房间" -> openCommandMenuTarget(player, "rooms", backCommand);
            case "main", "home", "hunter", "huntergame", "hg",
                    "leaderboard", "lb", "shop", "settings",
                    "victoryshop", "victorysettings", "endflashkit", "endflashkitadmin", "personalkit",
                    "endflashpersonalkit", "pass_count", "fastest_time", "play_count", "hunter_points",
                    "prey_points", "minigame_points", "主菜单", "猎人游戏", "排行榜", "商城", "设置" ->
                    openCommandMenuTarget(player, action, backCommand);
            case "create", "createroom", "创建房间", "创建" -> {
                if (args.length >= 3) {
                    handleCommandCreateRoom(player, args);
                } else {
                    openCommandMenuTarget(player, action, backCommand);
                }
            }
            case "quick", "match", "mode", "play", "模式", "匹配", "快速匹配" -> handleCommandQuickMatch(player, args);
            case "join", "加入" -> handleCommandJoinRoom(player, args);
            default -> {
                GameMode directMode = parseCommandMode(action);
                if (directMode != null) {
                    runCommandQuickMatch(player, directMode);
                    return;
                }
                player.sendMessage("§x§7§D§F§F§C§8GameFun §8» §c未知 command 分支: §e" + args[1]);
                player.sendMessage("§8· §7输入 §e/gamefunxiao command help §7查看可用分支。");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
            }
        }
    }

    private void openCommandMenuTarget(Player player, String menuId) {
        openCommandMenuTarget(player, menuId, null);
    }

    private void openCommandMenuTarget(Player player, String menuId, String backCommand) {
        if (!plugin.getMenuManager().openMenuFromCommand(player, menuId, backCommand)) {
            player.sendMessage("§x§7§D§F§F§C§8GameFun §8» §c没有找到这个菜单入口: §e" + menuId);
            player.sendMessage("§8· §7可用: §b" + String.join("§8, §b", plugin.getMenuManager().getCommandMenuIds()));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
        }
    }

    private CommandBackOptions parseBackCommand(String[] args) {
        if (args == null || args.length == 0) {
            return new CommandBackOptions(new String[0], null);
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--backcommand")) {
                String backCommand = i + 1 < args.length
                        ? String.join(" ", Arrays.copyOfRange(args, i + 1, args.length)).trim()
                        : null;
                return new CommandBackOptions(Arrays.copyOfRange(args, 0, i), normalizeBackCommand(backCommand));
            }

            String lower = arg.toLowerCase(Locale.ROOT);
            if (lower.startsWith("--backcommand=")) {
                String backCommand = arg.substring("--backcommand=".length()).trim();
                if (i + 1 < args.length) {
                    backCommand = (backCommand + " " + String.join(" ", Arrays.copyOfRange(args, i + 1, args.length))).trim();
                }
                return new CommandBackOptions(Arrays.copyOfRange(args, 0, i), normalizeBackCommand(backCommand));
            }
        }

        return new CommandBackOptions(args, null);
    }

    private String normalizeBackCommand(String command) {
        if (command == null) {
            return null;
        }
        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized.isBlank() ? null : normalized;
    }

    private record CommandBackOptions(String[] args, String backCommand) {
    }

    private void handleCommandQuickMatch(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§x§7§D§F§F§C§8GameFun §8» §e用法: §b/gamefunxiao command quick <模式>");
            player.sendMessage("§8· §7例如: §a/gamefunxiao command quick end_flash");
            return;
        }

        GameMode mode = parseCommandMode(args[2]);
        if (mode == null) {
            sendCommandInvalidMode(player, args[2]);
            return;
        }
        runCommandQuickMatch(player, mode);
    }

    private void runCommandQuickMatch(Player player, GameMode mode) {
        player.closeInventory();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.78f, 1.55f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.42f, 1.72f);
        plugin.getRoomManager().quickMatch(player, mode.getId());
    }

    private void handleCommandJoinRoom(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§x§7§D§F§F§C§8GameFun §8» §e用法: §b/gamefunxiao command join <房间ID>");
            return;
        }
        if (!plugin.getRoomManager().joinRoomById(player, args[2])) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_found"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
        }
    }


    private void handleCommandCreateRoom(Player player, String[] args) {
        if (args.length < 3) {
            openCommandMenuTarget(player, "create");
            return;
        }

        GameMode mode = parseCommandMode(args[2]);
        if (mode == null) {
            sendCommandInvalidMode(player, args[2]);
            return;
        }
        if (mode == GameMode.NETHER_CHAPTER || mode == GameMode.END_CHAPTER) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c下界篇和末地篇已从创建入口删除，请使用 §d终章 · 闪光§c。");
            return;
        }

        int defaultMaxPlayers = mode.isFlashTournament() ? 67 : (mode.isDirectFlashStart() ? 64 : 16);
        int maxPlayers = args.length >= 4 ? parseCommandMaxPlayers(args[3], defaultMaxPlayers) : defaultMaxPlayers;
        boolean isPublic = true;
        Set<String> modifiers = new HashSet<>();
        for (int i = 4; i < args.length; i++) {
            String value = args[i];
            if (value.equalsIgnoreCase("private") || value.equalsIgnoreCase("invite") || value.equalsIgnoreCase("仅邀请")) {
                isPublic = false;
            } else if (value.equalsIgnoreCase("public") || value.equalsIgnoreCase("公开")) {
                isPublic = true;
            } else if (value.startsWith("-")) {
                modifiers.add(value.substring(1));
            } else {
                modifiers.add(value);
            }
        }
        createCommandRoom(player, mode, maxPlayers, isPublic, modifiers);
    }

    private void createCommandRoom(Player player, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        if (maxPlayers != -1) {
            int minPlayers = plugin.getRoomManager().getMinimumPlayersForMode(mode);
            if (maxPlayers < minPlayers) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("count", String.valueOf(minPlayers));
                player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(mode, "room.min_players", placeholders));
                return;
            }
        }
        player.closeInventory();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.72f, 1.45f);
        plugin.getRoomManager().createConfiguredRoom(player, mode, maxPlayers, isPublic, modifiers);
    }

    private int parseCommandMaxPlayers(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "all", "unlimited", "infinite", "无限" -> -1;
            case "small", "小", "小型" -> 8;
            case "middle", "medium", "mid", "中", "中型" -> 16;
            case "large", "big", "大", "大型" -> 32;
            default -> {
                try {
                    yield Math.max(1, Integer.parseInt(raw));
                } catch (NumberFormatException ignored) {
                    yield fallback;
                }
            }
        };
    }

    private GameMode parseCommandMode(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        GameMode explicitMode = GameMode.findByIdStrict(normalized)
                .filter(GameMode::isCommandMode)
                .orElse(null);
        if (explicitMode != null) {
            return explicitMode;
        }
        return switch (normalized) {
            case "classic", "default", "normal", "普通", "经典", "经典模式" -> GameMode.CLASSIC;
            case "random", "randomcompass", "随机", "随机指南针" -> GameMode.RANDOM_COMPASS;
            case "swap", "互换", "互换模式" -> GameMode.SWAP;
            case "noitem", "no_item", "无有", "无有模式" -> GameMode.NO_ITEM;
            case "survival", "存活", "存活模式" -> GameMode.SURVIVAL;
            case "flash", "闪光", "闪光模式" -> GameMode.FLASH;
            case "flashtournament", "flash_tournament", "tournamentflash", "tournament_flash",
                    "赛事", "赛事闪光", "闪光赛事", "闪光_赛事", "普通闪光赛事" -> GameMode.FLASH_TOURNAMENT;
            case "endflash", "end_flash", "终章", "终章闪光", "终章_闪光" -> GameMode.END_FLASH;
            case "lucky", "luckypillars", "lucky_pillars", "幸运之柱", "幸运柱", "经典幸运之柱" -> GameMode.LUCKY_PILLARS;
            case "custom", "自定义" -> GameMode.CUSTOM;
            default -> null;
        };
    }

    private void sendCommandInvalidMode(Player player, String rawMode) {
        player.sendMessage("§x§7§D§F§F§C§8GameFun §8» §c未知模式: §e" + rawMode);
        player.sendMessage("§8· §7可用模式: §b" + String.join("§8, §b", getCommandModeIds()));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
    }

    private List<String> getCommandModeIds() {
        return GameMode.getCommandModes().stream()
                .map(GameMode::getId)
                .toList();
    }

    private void sendCommandBranchHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§x§7§D§F§F§C§8✦ §x§8§E§F§0§D§BGameFun Command 命令入口 §x§7§D§F§F§C§8✦");
        player.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        player.sendMessage("§e/gamefunxiao command §7- §f打开命令入口菜单");
        player.sendMessage("§e/gamefunxiao command menu <菜单> §7- §f打开指定菜单，返回会回命令入口");
        player.sendMessage("§e/gamefunxiao command menu <菜单> --backcommand <命令> §7- §f返回按钮执行指定命令");
        player.sendMessage("§e/gamefunxiao command quick <模式> §7- §f模拟点击模式按钮并进入等待房间");
        player.sendMessage("§8· §7示例: §a/gamefunxiao command quick classic §8/ §d/gamefunxiao command quick end_flash");
        player.sendMessage("§e/gamefunxiao command create <模式> [人数] [public|private] [修饰符] §7- §f直接创建房间");
        player.sendMessage("§e/gamefunxiao command rooms [all|lucky] §7- §f打开房间列表");
        player.sendMessage("§e/gamefunxiao command join <房间ID> §7- §f加入指定房间");
        player.sendMessage("§8· §7菜单ID: §b" + String.join("§8, §b", plugin.getMenuManager().getCommandMenuIds()));
        player.sendMessage("§8· §7模式ID: §d" + String.join("§8, §d", getCommandModeIds()));
        player.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        player.sendMessage("");
    }

    private void handleHunterGame(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        if (args.length == 1) {
            plugin.getMenuManager().openHunterGameMenu(player);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "help" -> {
                int page = args.length >= 3 ? parseHelpPage(args[2]) : 1;
                sendHunterGameHelp(player, page);
            }
            case "create" -> handleCreateRoom(player, args);
            case "join" -> handleJoinRoom(player, args);
            case "invite" -> handleInvitePlayer(player, args);
            case "list" -> plugin.getMenuManager().openRoomListMenu(player);
            case "leaderboard", "lb" -> plugin.getMenuManager().openLeaderboardMenu(player);
            case "setlobbyspawn" -> handleSetLobbySpawn(player);
            default -> plugin.getMenuManager().openHunterGameMenu(player);
        }
    }


    private void handleCreateRoom(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_command"));
            player.sendMessage("§e用法: /gamefunxiao hg create <模式> <人数> [修饰符...]");
            return;
        }

        String modeId = args[2].toLowerCase();
        GameMode mode = parseCommandMode(modeId);
        if (mode == null) {
            sendCommandInvalidMode(player, args[2]);
            return;
        }
        if (mode.isDirectFlashStart()) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.flash_create_disabled"));
            return;
        }
        if (mode == GameMode.NETHER_CHAPTER || mode == GameMode.END_CHAPTER) {
            player.sendMessage("§x§B§B§8§8§F§FGameFun §8» §c下界篇和末地篇已从创建入口删除，请使用 §d终章 · 闪光§c。");
            return;
        }

        int maxPlayers;
        if (args[3].equalsIgnoreCase("all")) {
            maxPlayers = -1;
        } else {
            try {
                maxPlayers = Integer.parseInt(args[3]);
                int requiredPlayers = plugin.getRoomManager().getMinimumPlayersForMode(mode);
                if (maxPlayers < requiredPlayers) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("count", String.valueOf(requiredPlayers));
                    player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(mode, "room.min_players", placeholders));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_number"));
                return;
            }
        }

        // 解析修饰符
        Set<String> modifiers = new HashSet<>();
        for (int i = 4; i < args.length; i++) {
            String mod = args[i];
            if (mod.startsWith("-")) {
                modifiers.add(mod.substring(1));
            }
        }

        plugin.getRoomManager().createConfiguredRoom(player, mode, maxPlayers, true, modifiers);
    }

    private void handleJoinRoom(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.invalid_command"));
            return;
        }

        String roomId = args[2];
        if (!plugin.getRoomManager().joinRoomById(player, roomId)) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_found"));
        }
    }

    private void handleInvitePlayer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.invite_usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            target = Bukkit.getPlayer(args[2]);
        }

        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[2]);
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_not_found", placeholders));
            return;
        }

        RoomManager.InviteResult result = plugin.getRoomManager().invitePlayerToRoom(player, target);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        switch (result) {
            case SUCCESS -> {
                var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
                if (room != null) {
                    placeholders.put("room_id", room.getRoomId());
                }
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_sent", placeholders));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.25f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.55f);
            }
            case INVITER_NOT_IN_ROOM ->
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_in_room"));
            case NOT_OWNER ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_not_owner"));
            case ROOM_PUBLIC ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_public_room"));
            case ROOM_UNAVAILABLE ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_room_unavailable"));
            case TARGET_SELF ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_target_self"));
            case TARGET_ALREADY_IN_ROOM ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_target_in_room", placeholders));
            case TARGET_IN_OTHER_ROOM ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_target_busy", placeholders));
            case TARGET_ALREADY_INVITED ->
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.invite_target_already_invited", placeholders));
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        if (!plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_in_room"));
            return;
        }

        var room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        plugin.getRoomManager().leaveRoom(player);
        player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.left"));
    }

    private void handleRejoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.already_in_room"));
            return;
        }

        if (!plugin.getRoomManager().canRejoin(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.cannot_rejoin"));
            return;
        }

        if (plugin.getRoomManager().rejoinGame(player)) {
            player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(plugin.getRoomManager().getPlayerRoom(player.getUniqueId()), "room.rejoined"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.rejoin_failed"));
        }
    }

    private void handleFlashWikiBook(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }

        ItemStack book = plugin.getFlashModeManager().createFlashGameGuideBook();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(book);
        if (leftovers.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_given"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.72f, 1.36f);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.48f, 1.62f);
            return;
        }

        Map<Integer, ItemStack> enderLeftovers = player.getEnderChest().addItem(leftovers.values().toArray(new ItemStack[0]));
        if (enderLeftovers.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_ender"));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.55f, 1.48f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.58f, 1.18f);
            return;
        }

        enderLeftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_dropped"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.55f, 1.34f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.52f, 0.92f);
    }


    private void handleOpenFlashWikiBook(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        String volume = args.length >= 2 ? args[1] : "main";
        plugin.getFlashModeManager().openFlashGameGuideBook(player, volume);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.58f, 1.28f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.28f, 1.72f);
    }

    private void sendHunterGameHelp(Player player, int page) {
        List<String> entries = new ArrayList<>();
        entries.add("§e/gamefunxiao hg §7- §f打开猎人游戏菜单");
        entries.add("§e/gamefunxiao hg help [页码] §7- §f查看分页帮助");
        entries.add("§e/gamefunxiao hg list §7- §f查看房间列表");
        entries.add("§e/gamefunxiao hg join <房间ID> §7- §f加入指定房间");
        entries.add("§e/gamefunxiao hg invite <玩家名> §7- §f邀请玩家进入你的仅邀请房间");
        entries.add("§e/gamefunxiao hg leaderboard §7- §f查看排行榜");
        entries.add("§e/hh §7- §f宣传当前等待中的房间");

        if (player.hasPermission("gamefunxiao.admin")) {
            entries.add("§c管理员命令：");
            entries.add("§e/gamefunxiao hg create <模式> <人数> [修饰符...] §7- §f创建房间");
            entries.add("§8· §7示例: §a/gamefunxiao hg create classic 16 §8/ §d/gamefunxiao hg create lucky_pillars 16");
            entries.add("§e/gamefun setlobbyspawn §7- §f在 gameing 的等待大厅世界中自动设置出生点");
            entries.add("§e/gamefunxiao 调终章 §7- §f请求 gameing 进入固定终章调试世界");
        }

        sendPaginatedHelp(player,
                "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏 §x§F§F§6§6§0§0命令帮助 §x§F§F§6§6§0§0⚔",
                "/gamefunxiao hg help ",
                entries,
                page);
    }

    private void sendPaginatedHelp(CommandSender sender, String title, String pageCommandPrefix, List<String> entries, int page) {
        final int pageSize = 8;
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, entries.size());

        sender.sendMessage("");
        sender.sendMessage(title);
        sender.sendMessage("§8第 §e" + currentPage + "§8/§e" + totalPages + " §8页");
        sender.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        for (int i = fromIndex; i < toIndex; i++) {
            sender.sendMessage(entries.get(i));
        }
        sender.sendMessage("§8· · · · · · · · · · · · · · · · · · · ·");
        sendHelpNavigator(sender, pageCommandPrefix, currentPage, totalPages);
        sender.sendMessage("");
    }

    private void sendHelpNavigator(CommandSender sender, String pageCommandPrefix, int currentPage, int totalPages) {
        Component line = Component.empty();
        if (currentPage > 1) {
            line = line.append(createHelpNavButton("§a[上一页]", pageCommandPrefix + (currentPage - 1), "§7点击切换到第 §e" + (currentPage - 1) + " §7页"));
        } else {
            line = line.append(Component.text("§8[上一页]"));
        }

        line = line.append(Component.text(" §8| §7"));
        line = line.append(Component.text("§e" + currentPage + "§8/§e" + totalPages));
        line = line.append(Component.text(" §8| "));

        if (currentPage < totalPages) {
            line = line.append(createHelpNavButton("§b[下一页]", pageCommandPrefix + (currentPage + 1), "§7点击切换到第 §e" + (currentPage + 1) + " §7页"));
        } else {
            line = line.append(Component.text("§8[下一页]"));
        }
        sender.sendMessage(line);
    }

    private Component createHelpNavButton(String text, String command, String hover) {
        return Component.text(text)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    private int parseHelpPage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void handleSetLobbySpawn(Player player) {
        if (!player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            return;
        }
        if (!isCurrentGameServer()) {
            player.sendMessage("§x§F§F§8§8§5§5⚠ §c只能在 §egameing §c服务器执行这个命令。");
            return;
        }

        World currentWorld = player.getWorld();
        if (currentWorld == null) {
            player.sendMessage("§x§F§F§8§8§5§5⚠ §c当前世界无效。");
            return;
        }

        if (currentWorld.getName().equalsIgnoreCase("hugamelobby")) {
            currentWorld.setSpawnLocation(player.getLocation());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.spawn_set"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            return;
        }

        for (GameMode mode : GameMode.getMiniGameMapEditableModes()) {
            for (MiniGameMapManager.MapDefinition definition : plugin.getMiniGameMapManager().getMapDefinitions(mode)) {
                if (!definition.lobbyTemplateWorld().equalsIgnoreCase(currentWorld.getName())) {
                    continue;
                }
                Location loc = player.getLocation().clone();
                currentWorld.setSpawnLocation(loc);
                plugin.getMiniGameMapManager().writeDefaultLobbyTemplateData(definition, loc);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("mode", definition.mode().getDisplayName());
                placeholders.put("map", definition.displayName());
                player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.lobby_spawn_set", placeholders));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                return;
            }
        }

        player.sendMessage("§x§F§F§8§8§5§5⚠ §c你当前不在任何等待大厅模板世界中。");
    }

    private void handleSetLobbySpawnCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_only"));
            return;
        }
        handleSetLobbySpawn(player);
    }

    private boolean isCurrentGameServer() {
        String targetServer = plugin.getConfigManager().getCrossServerGameServerName();
        String serverName = applyPlaceholders(null, "%qichengmorebungeeapi_server%");
        if (serverName == null || serverName.isBlank() || serverName.contains("%qichengmorebungeeapi_server%")) {
            return true;
        }
        if (sameServerName(targetServer, serverName)) {
            return true;
        }
        return sameServerName(targetServer, resolveRuntimeDirectoryServerName());
    }

    private boolean sameServerName(String expected, String actual) {
        String expectedKey = normalizeComparableServerName(expected);
        String actualKey = normalizeComparableServerName(actual);
        return !expectedKey.isBlank() && expectedKey.equals(actualKey);
    }

    private String normalizeComparableServerName(String serverName) {
        if (serverName == null) {
            return "";
        }
        String stripped = org.bukkit.ChatColor.stripColor(serverName.replace('&', '§'));
        if (stripped == null) {
            stripped = serverName;
        }
        return stripped.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveRuntimeDirectoryServerName() {
        try {
            java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("user.dir", "")).getFileName();
            if (path != null && !path.toString().isBlank()) {
                return path.toString().trim();
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private String applyPlaceholders(OfflinePlayer player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method method = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (ReflectiveOperationException exception) {
            return text;
        }
    }

    private void addEndFlashKitIds(List<String> completions) {
        for (EndFlashKitManager.Role role : EndFlashKitManager.Role.values()) {
            plugin.getEndFlashKitManager().getKits(role).stream()
                    .map(EndFlashKitManager.Kit::id)
                    .forEach(completions::add);
        }
    }

    private void addMiniGameModeCompletions(List<String> completions) {
        GameMode.getMiniGameMapEditableModes().stream()
                .map(GameMode::getId)
                .forEach(completions::add);
    }

    private GameMode findCompletionMode(String value) {
        return GameMode.findByIdStrict(value)
                .filter(GameMode::isMiniGameMapEditableMode)
                .orElse(null);
    }

    private void addMapIdCompletions(List<String> completions, GameMode mode) {
        if (mode == null) {
            return;
        }
        plugin.getMiniGameMapManager().getMapDefinitions(mode).stream()
                .map(MiniGameMapManager.MapDefinition::mapId)
                .forEach(completions::add);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabCompleteRegisteredCommand(sender, command.getName(), args);
    }

    public List<String> tabCompleteRegisteredCommand(CommandSender sender, String commandName, String[] args) {
        if (args == null || args.length == 0) {
            return Collections.emptyList();
        }
        if (commandName.equalsIgnoreCase("hh")) {
            return Collections.emptyList();
        }
        if (commandName.equalsIgnoreCase("ec")
                || commandName.equalsIgnoreCase("enderchest")
                || commandName.equalsIgnoreCase("endflashender")) {
            return Collections.emptyList();
        }
        if (commandName.equalsIgnoreCase("flashwiki")
                || commandName.equalsIgnoreCase("bookwiki")
                || commandName.equalsIgnoreCase("gamefunwiki")
                || commandName.equalsIgnoreCase("闪光手册")
                || commandName.equalsIgnoreCase("书wiki")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("menu", "help", "command", "cmd", "huntergame", "hg", "leave", "rejoin", "wiki", "guidebook", "bookwiki", "flashwiki"));
                completions.add("endflashkit");
                completions.add("flashkit");
                completions.add("endflashender");
                completions.add("endflashcompass");
                completions.add("flashmusic");
                if (sender.hasPermission("gamefunxiao.flashuse") || sender.hasPermission("gamefunxiao.admin")) {
                    completions.add("flashuse");
                }
                if (sender.hasPermission("gamefunxiao.admin")) {
                    completions.add("reload");
                    completions.add("cleanuprooms");
                    completions.add("roomclean");
                    completions.add("editlobbytemplate");
                    completions.add("edittemplate");
                    completions.add("setlobbyspawn");
                    completions.add("map");
                    completions.add("maps");
                    completions.add("minigamemap");
                    completions.add("mgmap");
                    completions.add("地图");
                    completions.add("lobbyinteract");
                    completions.add("lobbyregion");
                    completions.add("调终章");
                    completions.add("endflashdebug");
                    completions.add("efdebug");
                    completions.add("coins");
                }
        } else if (args.length == 2) {
            if (isFlashMusicCommand(args[0])) {
                return tabCompleteFlashMusic(sender, args);
            }
            if (isMiniGameMapCommand(args[0])
                    && sender.hasPermission("gamefunxiao.admin")) {
                completions.addAll(getMiniGameMapActions());
            } else if ((args[0].equalsIgnoreCase("editlobbytemplate") || args[0].equalsIgnoreCase("edittemplate"))
                    && sender.hasPermission("gamefunxiao.admin")) {
                completions.add("hugamelobby");
                for (GameMode mode : GameMode.getMiniGameMapEditableModes()) {
                    for (MiniGameMapManager.MapDefinition definition : plugin.getMiniGameMapManager().getMapDefinitions(mode)) {
                        completions.add(definition.lobbyTemplateWorld());
                    }
                }
            } else if (isLobbyInteractCommand(args[0]) && sender.hasPermission("gamefunxiao.admin")) {
                completions.addAll(Arrays.asList("add", "remove", "list"));
            } else if (isCommandBranch(args[0])) {
                completions.addAll(Arrays.asList(
                        "help", "menu", "open", "quick", "match", "create", "join", "rooms",
                        "main", "hunter", "leaderboard", "shop", "settings", "personalkit"
                ));
                if (sender.hasPermission("gamefunxiao.admin")) {
                    completions.add("endflashkit");
                }
            } else if (args[0].equalsIgnoreCase("help")) {
                completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
            } else if (args[0].equalsIgnoreCase("huntergame") || args[0].equalsIgnoreCase("hg")) {
                completions.addAll(Arrays.asList("help", "create", "join", "invite", "list", "leaderboard"));
            } else if ((args[0].equalsIgnoreCase("coins") || args[0].equalsIgnoreCase("coin") || args[0].equalsIgnoreCase("money"))
                    && sender.hasPermission("gamefunxiao.admin")) {
                completions.addAll(Arrays.asList("set", "add", "get"));
            } else if (isEndFlashKitCommand(args[0])) {
                completions.add("list");
                if (sender.hasPermission("gamefunxiao.admin")) {
                    completions.addAll(Arrays.asList("menu", "gui", "create", "createender", "hand", "appendhand", "appendenderhand", "chance", "remove", "reload"));
                }
            } else if ((args[0].equalsIgnoreCase("flashuse") || args[0].equalsIgnoreCase("flashtest") || args[0].equalsIgnoreCase("flashitems"))
                    && (sender.hasPermission("gamefunxiao.flashuse") || sender.hasPermission("gamefunxiao.admin"))) {
                completions.addAll(Arrays.asList("on", "off", "toggle", "status"));
            }
        } else if (args.length == 3) {
            if (isFlashMusicCommand(args[0])) {
                return tabCompleteFlashMusic(sender, args);
            }
            if (isMiniGameMapCommand(args[0])
                    && sender.hasPermission("gamefunxiao.admin")) {
                String action = normalizeMapAction(args[1]);
                if (getMiniGameMapActions().contains(action)) {
                    addMiniGameModeCompletions(completions);
                }
            } else if (isLobbyInteractCommand(args[0]) && sender.hasPermission("gamefunxiao.admin")) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("remove") || action.equals("delete") || action.equals("del")) {
                    addLobbyInteractionRegionIds(completions);
                } else if (action.equals("add") || action.equals("set") || action.equals("create")) {
                    completions.addAll(Arrays.asList("spawn", "signs", "frames", "build"));
                }
            } else if (isCommandBranch(args[0])) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("menu") || action.equals("open") || action.equals("gui") || action.equals("打开")) {
                    completions.addAll(plugin.getMenuManager().getCommandMenuIds());
                } else if (action.equals("quick") || action.equals("match") || action.equals("mode")
                        || action.equals("play") || action.equals("create")) {
                    completions.addAll(getCommandModeIds());
                    completions.add("endflash");
                } else if (action.equals("join") || action.equals("加入")) {
                    plugin.getRoomManager().getAllRooms().stream()
                            .map(room -> room.getRoomId())
                            .sorted()
                            .forEach(completions::add);
                } else if (action.equals("rooms") || action.equals("roomlist")) {
                    completions.addAll(Arrays.asList("all", "lucky", "hunter"));
                }
            } else if (args[0].equalsIgnoreCase("huntergame") || args[0].equalsIgnoreCase("hg")) {
                if (args[1].equalsIgnoreCase("create")) {
                    GameMode.getHunterAdminCreateModes().stream()
                            .map(GameMode::getId)
                            .forEach(completions::add);
                } else if (args[1].equalsIgnoreCase("help")) {
                    completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
                } else if (args[1].equalsIgnoreCase("join")) {
                    plugin.getRoomManager().getAllRooms().stream()
                        .map(room -> room.getRoomId())
                        .sorted()
                        .forEach(completions::add);
                } else if (args[1].equalsIgnoreCase("invite")) {
                    Player viewer = sender instanceof Player ? (Player) sender : null;
                    Bukkit.getOnlinePlayers().stream()
                        .filter(target -> viewer == null || plugin.getRoomManager().canSeeInRoomTab(viewer, target))
                        .map(Player::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(completions::add);
                }
            } else if (isEndFlashKitCommand(args[0])) {
                if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("createender") || args[1].equalsIgnoreCase("hand")) {
                    completions.addAll(Arrays.asList("hunter", "prey"));
                } else if (args[1].equalsIgnoreCase("appendhand") || args[1].equalsIgnoreCase("appendenderhand") || args[1].equalsIgnoreCase("chance") || args[1].equalsIgnoreCase("remove")) {
                    addEndFlashKitIds(completions);
                }
            } else if ((args[0].equalsIgnoreCase("coins") || args[0].equalsIgnoreCase("coin") || args[0].equalsIgnoreCase("money"))
                    && sender.hasPermission("gamefunxiao.admin")) {
                if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add")) {
                    completions.addAll(Arrays.asList("0", "100", "500", "1000", "5000"));
                } else if (args[1].equalsIgnoreCase("get")) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).forEach(completions::add);
                }
            } else if ((args[0].equalsIgnoreCase("flashuse") || args[0].equalsIgnoreCase("flashtest") || args[0].equalsIgnoreCase("flashitems"))
                    && sender.hasPermission("gamefunxiao.admin")) {
                Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).forEach(completions::add);
            }
        } else if (args.length == 4) {
            if (isFlashMusicCommand(args[0])) {
                return tabCompleteFlashMusic(sender, args);
            }
            if (isMiniGameMapCommand(args[0])
                    && sender.hasPermission("gamefunxiao.admin")) {
                String action = normalizeMapAction(args[1]);
                GameMode mode = findCompletionMode(args[2]);
                if (action.equals("create")) {
                    completions.addAll(Arrays.asList("default", "small", "middle", "large", "nether", "void", "ocean"));
                } else if (action.equals("active")) {
                    completions.addAll(Arrays.asList("random", "all"));
                    addMapIdCompletions(completions, mode);
                } else if (needsMapIdCompletion(action)) {
                    addMapIdCompletions(completions, mode);
                }
            } else if (isCommandBranch(args[0])) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("create")) {
                    completions.addAll(Arrays.asList("2", "4", "8", "16", "32", "64", "all", "small", "middle", "large"));
                }
            } else if ((args[0].equalsIgnoreCase("huntergame") || args[0].equalsIgnoreCase("hg"))
                && args[1].equalsIgnoreCase("create")) {
                completions.addAll(Arrays.asList("2", "4", "8", "16", "all"));
            } else if (isEndFlashKitCommand(args[0])) {
                if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("createender") || args[1].equalsIgnoreCase("hand") || args[1].equalsIgnoreCase("chance")) {
                    completions.addAll(Arrays.asList("0", "0.5", "1", "2.5", "5", "10", "25", "50"));
                }
            } else if ((args[0].equalsIgnoreCase("coins") || args[0].equalsIgnoreCase("coin") || args[0].equalsIgnoreCase("money"))
                    && sender.hasPermission("gamefunxiao.admin")
                    && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add"))) {
                Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).forEach(completions::add);
            }
        } else if (args.length >= 5) {
            if (isFlashMusicCommand(args[0])) {
                return tabCompleteFlashMusic(sender, args);
            }
            if (isMiniGameMapCommand(args[0]) && sender.hasPermission("gamefunxiao.admin")) {
                addMiniGameMapArgumentCompletions(completions, args);
            } else if (isCommandBranch(args[0])) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("create")) {
                    completions.addAll(Arrays.asList("public", "private", "-ThunderStorm", "-RewardChest", "-NoWorld",
                            "-NoYChange", "-YesHunterSee", "-NoLocatorBar", "-IncludeY", "-HunterDropItems",
                            "-PreyRespawn", "-HunterTPOnDeath", "-InfiniteTP"));
                }
            } else if ((args[0].equalsIgnoreCase("huntergame") || args[0].equalsIgnoreCase("hg"))
                && args[1].equalsIgnoreCase("create")) {
                // 修饰符补全，排除已使用的
                Set<String> usedModifiers = new HashSet<>();
                for (int i = 4; i < args.length - 1; i++) {
                    if (args[i].startsWith("-")) {
                        usedModifiers.add(args[i].substring(1).toLowerCase());
                    }
                }

                String[] allModifiers = {"ThunderStorm", "RewardChest", "NoWorld", "NoYChange",
                    "YesHunterSee", "NoLocatorBar", "IncludeY", "HunterDropItems", "PreyRespawn", "HunterTPOnDeath", "InfiniteTP"};

                for (String mod : allModifiers) {
                    if (!usedModifiers.contains(mod.toLowerCase())) {
                        completions.add("-" + mod);
                    }
                }
            }
        }

        // 过滤
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));

        return completions;
    }


    private boolean isMiniGameMapCommand(String value) {
        return value.equalsIgnoreCase("map")
                || value.equalsIgnoreCase("maps")
                || value.equalsIgnoreCase("minigamemap")
                || value.equalsIgnoreCase("mgmap")
                || value.equalsIgnoreCase("地图")
                || value.equalsIgnoreCase("地图编辑");
    }

    private List<String> getMiniGameMapActions() {
        return Arrays.asList("help", "list", "create", "edit", "active", "enable", "disable", "delete", "info",
                "name", "range", "time", "theme", "boundary", "autocreate");
    }

    private boolean needsMapIdCompletion(String action) {
        return action.equals("edit") || action.equals("info") || action.equals("enable") || action.equals("disable")
                || action.equals("delete") || action.equals("name") || action.equals("range") || action.equals("time")
                || action.equals("theme") || action.equals("boundary") || action.equals("autocreate");
    }

    private void addMiniGameMapArgumentCompletions(List<String> completions, String[] args) {
        String action = normalizeMapAction(args[1]);
        if (args.length == 5) {
            switch (action) {
                case "create" -> completions.addAll(Arrays.asList("2", "4", "8", "12", "16", "24", "32"));
                case "edit" -> completions.addAll(Arrays.asList("game", "lobby", "游戏地图", "等待大厅"));
                case "range" -> completions.addAll(Arrays.asList("2", "4", "8", "16"));
                case "time" -> completions.addAll(Arrays.asList("300", "480", "600", "900"));
                case "theme" -> completions.addAll(Arrays.asList("WOOL", "VOID", "OCEAN", "NETHER", "GLASS", "TNT", "MOON"));
                case "boundary" -> completions.addAll(Arrays.asList("52", "66", "96", "128"));
                case "autocreate" -> completions.addAll(Arrays.asList("true", "false", "开启", "关闭"));
            }
        } else if (args.length == 6) {
            switch (action) {
                case "range" -> completions.addAll(Arrays.asList("8", "16", "24", "32", "64"));
                case "time" -> completions.addAll(Arrays.asList("3", "5", "8", "10"));
                case "boundary" -> completions.addAll(Arrays.asList("80", "100", "120", "140"));
            }
        } else if (args.length == 7 && action.equals("time")) {
            completions.addAll(Arrays.asList("20", "30", "45", "60"));
        }
    }

    private boolean isFlashMusicCommand(String value) {
        return value.equalsIgnoreCase("flashmusic")
                || value.equalsIgnoreCase("fmusic")
                || value.equalsIgnoreCase("flashnote")
                || value.equalsIgnoreCase("闪光音乐")
                || value.equalsIgnoreCase("音符盒");
    }

    private boolean isCommandBranch(String value) {
        return value.equalsIgnoreCase("command")
                || value.equalsIgnoreCase("cmd")
                || value.equalsIgnoreCase("命令")
                || value.equalsIgnoreCase("指令");
    }

    private boolean isLobbyInteractCommand(String value) {
        return value.equalsIgnoreCase("lobbyinteract")
                || value.equalsIgnoreCase("lobbyregion")
                || value.equalsIgnoreCase("等待大厅交互")
                || value.equalsIgnoreCase("大厅交互");
    }

    private void addLobbyInteractionRegionIds(List<String> completions) {
        org.bukkit.configuration.ConfigurationSection regions = plugin.getConfig()
                .getConfigurationSection("lobby_interaction_regions.regions");
        if (regions != null) {
            completions.addAll(regions.getKeys(false));
        }
    }

    private boolean isEndFlashKitCommand(String value) {
        return value.equalsIgnoreCase("endflashkit")
                || value.equalsIgnoreCase("efkit")
                || value.equalsIgnoreCase("终章kit")
                || value.equalsIgnoreCase("flashkit")
                || value.equalsIgnoreCase("闪光kit")
                || value.equalsIgnoreCase("调闪光kit");
    }

    private List<String> tabCompleteFlashMusic(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(Arrays.asList("list", "play", "stop", "help"));
            if (hasFlashMusicAdmin(sender)) {
                completions.addAll(Arrays.asList("all", "nearby", "stopall"));
            }
            String input = args[1].toLowerCase(Locale.ROOT);
            completions.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(input));
            return completions;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if ((action.equals("list") || action.equals("songs") || action.equals("曲库")) && args.length == 3) {
            completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
            completions.removeIf(value -> !value.startsWith(args[2]));
            return completions;
        }

        if (action.equals("nearby") || action.equals("near") || action.equals("附近")) {
            if (!hasFlashMusicAdmin(sender)) {
                return Collections.emptyList();
            }
            if (args.length == 3) {
                completions.addAll(Arrays.asList("16", "32", "64", "96", "128"));
                completions.removeIf(value -> !value.startsWith(args[2]));
                return completions;
            }
            return matchingFlashMusicNames(String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        }

        if (action.equals("all") || action.equals("broadcast") || action.equals("全服")) {
            if (!hasFlashMusicAdmin(sender)) {
                return Collections.emptyList();
            }
            return matchingFlashMusicNames(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        }

        if (action.equals("play") || action.equals("播放")) {
            return matchingFlashMusicNames(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        }

        return Collections.emptyList();
    }

    private List<String> matchingFlashMusicNames(String query) {
        String normalizedQuery = normalizeCompletionText(query);
        return plugin.getFlashModeManager().getFlashNoteMusicNameHints().stream()
                .filter(name -> normalizedQuery.isBlank()
                        || normalizeCompletionText(name).contains(normalizedQuery)
                        || name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim()))
                .limit(40)
                .toList();
    }

    private String normalizeCompletionText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = Character.toLowerCase(value.charAt(i));
            if (Character.isLetterOrDigit(ch)
                    || Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN
                    || Character.UnicodeScript.of(ch) == Character.UnicodeScript.HIRAGANA
                    || Character.UnicodeScript.of(ch) == Character.UnicodeScript.KATAKANA) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}


