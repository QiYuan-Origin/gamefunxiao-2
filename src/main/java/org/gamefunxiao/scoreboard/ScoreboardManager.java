package org.gamefunxiao.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation"})
public class ScoreboardManager {

    private static final String SCOREBOARD_OBJECTIVE = "gamefun";
    private static final String LUCKY_PILLARS_HEALTH_OBJECTIVE = "gf_lp_hp";
    private static final int MAX_SCOREBOARD_LINES = 15;

    private final GameFunXiao plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<String, BukkitTask> endedScoreboardTasks = new HashMap<>(); // 房间ID -> 清除任务
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private BukkitTask updateTask;

    public ScoreboardManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动记分板更新任务（每秒更新一次）
     */
    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (GameRoom room : plugin.getRoomManager().getAllRooms()) {
                    // 更新玩家记分板
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            updateScoreboard(player, room);
                        }
                    }
                    // 更新旁观者记分板
                    for (UUID uuid : room.getSpectators()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            updateScoreboard(player, room);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每20 ticks (1秒) 更新一次
    }

    /**
     * 停止记分板更新任务
     */
    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    /**
     * 为玩家创建记分板
     */
    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    /**
     * 清除玩家记分板
     */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 更新玩家记分板
     */
    private void updateScoreboard(Player player, GameRoom room) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            removeScoreboard(player);
            return;
        }
        if (room != null && room.getGameMode().isFlashTournament() && room.getState() == RoomState.PLAYING) {
            removeScoreboard(player);
            plugin.getRoomManager().refreshRoleNameTags(room);
            return;
        }

        Scoreboard current = player.getScoreboard();
        Objective currentSidebar = current == null ? null : current.getObjective(DisplaySlot.SIDEBAR);
        if (plugin.getConfigManager().shouldScoreboardRespectForeignSidebar()
                && currentSidebar != null
                && !SCOREBOARD_OBJECTIVE.equals(currentSidebar.getName())) {
            // 其他插件正在显示侧边栏时不硬抢，避免 TAB / AnimatedScoreboard 来回抢导致一闪一闪。
            return;
        }

        Scoreboard scoreboard = getOrCreateScoreboard(player);
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }

        RoomState state = room.getState();
        if (state != RoomState.ENDED) {
            cancelEndedScoreboardClear(room.getRoomId());
        }

        List<String> lines = new ArrayList<>();
        String configSection = "waiting";
        switch (state) {
            case WAITING, STARTING -> lines = getWaitingLines(room);
            case SELECTING -> {
                configSection = "selecting";
                lines = getSelectingLines(room);
            }
            case PLAYING -> {
                configSection = room.getGameMode() == GameMode.SWAP && room.isSwapCountdownPrey(player.getUniqueId())
                        ? "swap_countdown"
                        : "playing";
                lines = getPlayingLines(room, player);
            }
            case ENDED -> {
                configSection = "ended";
                lines = getEndedLines(room, player);
                scheduleEndedScoreboardClear(room);
            }
            default -> {
                removeScoreboard(player);
                return;
            }
        }

        List<String> configuredLines = room.getGameMode().isLuckyPillars()
                ? Collections.emptyList()
                : getConfiguredLines(configSection, room, player);
        if (!configuredLines.isEmpty()) {
            lines = configuredLines;
        }

        if (lines.size() > MAX_SCOREBOARD_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_SCOREBOARD_LINES));
        }

        Objective objective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        String title = room.getGameMode().isLuckyPillars()
                ? "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱"
                : color(plugin.getConfigManager().getScoreboardTitle());
        if (objective == null) {
            objective = scoreboard.registerNewObjective(SCOREBOARD_OBJECTIVE, "dummy", title);
        } else if (!Objects.equals(objective.getDisplayName(), title)) {
            objective.setDisplayName(title);
        }
        if (objective.getDisplaySlot() != DisplaySlot.SIDEBAR) {
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        updateBelowNameHealth(scoreboard, room);

        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String line = Objects.requireNonNullElse(lines.get(i), "");
            String entryName = getHiddenColorCode(i);
            Team team = getOrCreateLineTeam(scoreboard, i);

            syncTeamEntry(team, entryName);
            team.prefix(legacySerializer.deserialize(line));
            team.suffix(Component.empty());

            org.bukkit.scoreboard.Score scoreObj = objective.getScore(entryName);
            scoreObj.setScore(score--);
            if (plugin.getConfigManager().shouldShowScoreboardNumbers()) {
                scoreObj.numberFormat(null);
            } else {
                scoreObj.numberFormat(NumberFormat.blank());
            }
        }

        for (int i = lines.size(); i < MAX_SCOREBOARD_LINES; i++) {
            clearLine(scoreboard, i);
        }
        plugin.getRoomManager().refreshRoleNameTags(room);
    }

    private Scoreboard getOrCreateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard != null) {
            return scoreboard;
        }

        Scoreboard newScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        playerScoreboards.put(player.getUniqueId(), newScoreboard);
        return newScoreboard;
    }

    private Team getOrCreateLineTeam(Scoreboard scoreboard, int index) {
        String teamName = "sb_line_" + index;
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        return team;
    }

    private void syncTeamEntry(Team team, String entryName) {
        if (team.hasEntry(entryName) && team.getEntries().size() == 1) {
            return;
        }

        for (String existingEntry : new HashSet<>(team.getEntries())) {
            team.removeEntry(existingEntry);
        }
        team.addEntry(entryName);
    }

    private void clearLine(Scoreboard scoreboard, int index) {
        String entryName = getHiddenColorCode(index);
        scoreboard.resetScores(entryName);

        Team team = scoreboard.getTeam("sb_line_" + index);
        if (team == null) {
            return;
        }

        if (team.hasEntry(entryName)) {
            team.removeEntry(entryName);
        }
        for (String extraEntry : new HashSet<>(team.getEntries())) {
            team.removeEntry(extraEntry);
        }
        team.prefix(Component.empty());
        team.suffix(Component.empty());
    }

    /**
     * 生成隐藏的颜色代码，用于使记分板行唯一但不显示数字
     */
    private String getHiddenColorCode(int index) {
        return ChatColor.values()[index].toString() + "§r";
    }

    private void updateBelowNameHealth(Scoreboard scoreboard, GameRoom room) {
        if (scoreboard == null) {
            return;
        }
        Objective healthObjective = scoreboard.getObjective(LUCKY_PILLARS_HEALTH_OBJECTIVE);
        if (room == null || !room.getGameMode().isLuckyPillars() || room.getState() != RoomState.PLAYING) {
            if (healthObjective != null) {
                healthObjective.unregister();
            }
            return;
        }
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective(LUCKY_PILLARS_HEALTH_OBJECTIVE, "health", "§c❤");
        } else if (!Objects.equals(healthObjective.getDisplayName(), "§c❤")) {
            healthObjective.setDisplayName("§c❤");
        }
        if (healthObjective.getDisplaySlot() != DisplaySlot.BELOW_NAME) {
            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    /**
     * 等待大厅 + 倒计时阶段的记分板
     */
    private List<String> getWaitingLines(GameRoom room) {
        if (room.getGameMode() == GameMode.LUCKY_PILLARS) {
            return getLuckyPillarsWaitingLines(room);
        }
        List<String> lines = new ArrayList<>();

        lines.add("§7");
        lines.add("§f当前人数: §a" + room.getPlayerCount() + "§7/§e" +
                 (room.getMaxPlayers() == -1 ? "∞" : room.getMaxPlayers()));
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§f房间号: §d" + room.getRoomId());
        lines.add("§7");

        // 倒计时显示
        if (room.getState() == RoomState.STARTING) {
            int countdown = room.getCountdown();
            lines.add("§f距开始: §e" + formatCountdown(countdown));
        } else {
            lines.add("§7等待更多玩家...");
        }

        if (room.isDoublePreyEnabled() && room.getLockedFirstDualPrey() != null) {
            Player locked = Bukkit.getPlayer(room.getLockedFirstDualPrey());
            String lockedName = locked != null ? locked.getName() : Bukkit.getOfflinePlayer(room.getLockedFirstDualPrey()).getName();
            lines.add("§f双猎物: §a已开启");
            lines.add("§f第一猎物: §c" + (lockedName == null ? "未知" : lockedName));
        } else if (plugin.getGameManager().shouldOfferDoublePreyVote(room)) {
            lines.add("§f双猎物票: §d" + room.getDoublePreyVoteCount() + "§7/§d" +
                    plugin.getGameManager().getDoublePreyRequiredYesVotes(room));
        }

        lines.add("§7");
        lines.add("§8HunterGame.server");
        lines.add("§7");

        return lines;
    }

    private List<String> getConfiguredLines(String section, GameRoom room, Player viewer) {
        FileConfiguration scoreboardConfig = plugin.getConfigManager().getConfig("scoreboard");
        if (scoreboardConfig == null) {
            return Collections.emptyList();
        }

        List<String> rawLines = scoreboardConfig.getStringList("lines." + section);
        if (rawLines.isEmpty()) {
            return Collections.emptyList();
        }

        List<HunterRankData> ranks = getTopHuntersWithData(room, 3);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("room_id", room.getRoomId());
        placeholders.put("mode", getColoredGameMode(room.getGameMode()));
        placeholders.put("current_players", String.valueOf(room.getPlayerCount()));
        placeholders.put("max_players", room.getMaxPlayers() == -1 ? "∞" : String.valueOf(room.getMaxPlayers()));
        placeholders.put("state", getStateText(room.getState()));
        placeholders.put("countdown", formatCountdown(room.getCountdown()));
        placeholders.put("elapsed_time", formatElapsedTime(room.getGameDuration()));
        placeholders.put("prey_names", getPreyNames(room));
        placeholders.put("hunter_count", String.valueOf(getHunterCount(room)));
        placeholders.put("winner", room.isPreyWon() ? "§a猎物胜利" : "§c猎人胜利");
        placeholders.put("swap_countdown", formatCountdown(room.getSwapCountdownSeconds()));
        placeholders.put("active_swap_prey", getActiveSwapPreyName(room));
        placeholders.put("end_flash_command_hint", room.getGameMode() == GameMode.END_FLASH
                ? "§d末影箱: §f/ec §7/ §f/enderchest"
                : "");
        for (int i = 0; i < 3; i++) {
            placeholders.put("rank_" + (i + 1), formatRankLine(ranks, i));
        }

        List<String> lines = new ArrayList<>();
        for (String raw : rawLines) {
            String line = Objects.requireNonNullElse(raw, "");
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            line = color(line);
            if (line.isBlank() && raw.contains("{end_flash_command_hint}")) {
                continue;
            }
            if (!line.isBlank() || !raw.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private String getStateText(RoomState state) {
        return switch (state) {
            case WAITING -> "等待中";
            case STARTING -> "即将开始";
            case SELECTING -> "选择世界";
            case PLAYING -> "游戏中";
            case ENDED -> "已结束";
        };
    }

    private String getPreyNames(GameRoom room) {
        Set<UUID> preyUUIDs = room.getPreyUUIDs();
        if (preyUUIDs.isEmpty()) {
            return "暂无";
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : preyUUIDs) {
            Player prey = Bukkit.getPlayer(uuid);
            names.add(prey == null ? "未知" : prey.getName());
        }
        return String.join("§7, §a", names);
    }

    private int getHunterCount(GameRoom room) {
        int hunterCount = 0;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isPrey(uuid)) {
                hunterCount++;
            }
        }
        return hunterCount;
    }

    private String getActiveSwapPreyName(GameRoom room) {
        if (room.getActiveSwapPrey() == null) {
            return "未知";
        }
        Player active = Bukkit.getPlayer(room.getActiveSwapPrey());
        return active == null ? "未知" : active.getName();
    }

    private String formatRankLine(List<HunterRankData> ranks, int index) {
        if (ranks == null || index >= ranks.size()) {
            return "§f- §7暂无数据";
        }
        HunterRankData data = ranks.get(index);
        Player hunter = Bukkit.getPlayer(data.uuid);
        if (hunter == null) {
            return "§f- §7暂无数据";
        }
        String[] medals = {"🥇", "🥈", "🥉"};
        String distanceStr = data.distance == -1 ? "?" : String.valueOf(data.distance);
        return "§f" + medals[index] + " " + hunter.getName() + " §bM:" + distanceStr + "米 §6D:" + data.damagePoints + "点";
    }

    private String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 世界选择阶段的记分板
     */
    private List<String> getSelectingLines(GameRoom room) {
        List<String> lines = new ArrayList<>();

        lines.add("§7");

        // 显示游戏模式
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));

        // 显示猎物名字
        Set<UUID> preyUUIDs = room.getPreyUUIDs();
        if (!preyUUIDs.isEmpty()) {
            List<String> preyNames = new ArrayList<>();
            for (UUID preyUUID : preyUUIDs) {
                Player prey = Bukkit.getPlayer(preyUUID);
                preyNames.add(prey != null ? prey.getName() : "未知");
            }
            lines.add("§f🏹 猎物: §a" + String.join("§7, §a", preyNames));
        }

        if (room.getGameMode() == GameMode.END_CHAPTER && room.isEndChapterDivisionActive()) {
            int divisionCountdown = room.getEndChapterDivisionCountdown();
            lines.add("§f阶段: §d猎物猎人分工");
            lines.add("§f分工剩余: §e" + formatCountdown(divisionCountdown));
            lines.add("§f- §b猎物选择自己的 Kit 与位置");
            lines.add("§f- §d猎人投票倾向自己的 Kit 与位置");
            lines.add("§7");
            lines.add("§8HunterGame.server");
            lines.add("§7");
            return lines;
        }

        int countdown = room.getCountdown();
        if (room.hasModifier("NoWorld")) {
            lines.add("§f传送剩余: §e" + formatCountdown(countdown));
        } else {
            lines.add("§f选择剩余: §e" + formatCountdown(countdown));
        }

        if (room.getGameMode() == GameMode.NETHER_CHAPTER && room.isWorldSelectionConfirmed()) {
            lines.add("§f世界状态: §a已锁定");
            lines.add("§f投票剩余: §e" + formatCountdown(countdown));
        } else if (room.hasPendingDualPreyProposal()) {
            String action = room.getDualPreyProposalType() == org.gamefunxiao.game.DualPreyProposalType.REROLL_WORLD
                    ? "换世界"
                    : "定世界";
            lines.add("§f双猎物协商: §d" + action);
            lines.add("§f回应剩余: §e" + formatCountdown(room.getDualPreyProposalCountdown()));
        }

        lines.add("§7");

        // 世界切换次数（只在切换>=1次后显示）
        int rerollCount = room.getWorldRerollCount();
        if (rerollCount >= 1) {
            lines.add("§f已切换世界: §b" + rerollCount + "次");
            lines.add("§7");
        }

        lines.add("§8HunterGame.server");
        lines.add("§7");

        return lines;
    }

    /**
     * 游戏进行中的记分板
     */
    private List<String> getPlayingLines(GameRoom room, Player viewer) {
        if (room.getGameMode().isLuckyPillars()) {
            return getLuckyPillarsPlayingLines(room);
        }
        if (room.getGameMode().isIndependentMode()) {
            return getIndependentModePlayingLines(room);
        }

        if (room.getGameMode() == GameMode.SWAP && room.isSwapCountdownPrey(viewer.getUniqueId())) {
            return getSwapCountdownLines(room);
        }

        List<String> lines = new ArrayList<>();

        lines.add("§7");

        // 显示游戏模式
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));

        // 显示猎物名字
        Set<UUID> preyUUIDs = room.getPreyUUIDs();
        if (!preyUUIDs.isEmpty()) {
            List<String> preyNames = new ArrayList<>();
            for (UUID uuid : preyUUIDs) {
                Player prey = Bukkit.getPlayer(uuid);
                if (prey != null) {
                    preyNames.add(prey.getName());
                }
            }
            lines.add("§f🏹 猎物: §a" + String.join("§7, §a", preyNames));
        }

        // 猎人数量
        int hunterCount = 0;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isPrey(uuid)) {
                hunterCount++;
            }
        }
        lines.add("§f⚔ 猎人: §c" + hunterCount + "人");

        // 已进行时间
        long elapsedTime = room.getGameDuration();
        lines.add("§f已进行: §e" + formatElapsedTime(elapsedTime));
        if (room.getGameMode() == GameMode.END_FLASH) {
            lines.add("§d末影箱: §f/ec §7/ §f/enderchest");
        }

        lines.add("§7");

        // 检查是否正在传送（游戏开始时间为0说明还没正式开始）
        if (elapsedTime == 0) {
            // 传送过程中显示提示信息
            lines.add("§e§l正在传送中...");
            lines.add("§7");
            lines.add("§f- §b即将开始游戏");
            lines.add("§f- §7请稍候...");
            lines.add("§7");
            lines.add("§8HunterGame.server");
            lines.add("§7");
            return lines;
        }

        if (room.getGameMode() == GameMode.NO_ITEM) {
            lines.add("§e§l纯净模式");
            lines.add("§f- §c无指南针追踪");
            lines.add("§f- §7不显示排行信息");
        } else {
            lines.add("§e§l追踪排行");

            // 获取前3名猎人排行（带详细数据）
            List<HunterRankData> topHunters = getTopHuntersWithData(room, 3);

            if (topHunters.isEmpty()) {
                lines.add("§f- §7暂无数据");
            } else {
                String[] medals = {"🥇", "🥈", "🥉"};
                for (int i = 0; i < topHunters.size(); i++) {
                    HunterRankData data = topHunters.get(i);
                    Player hunter = Bukkit.getPlayer(data.uuid);
                    if (hunter != null) {
                        String medal = i < medals.length ? medals[i] : "§7#" + (i + 1);
                        String distanceStr = data.distance == -1 ? "?" : String.valueOf(data.distance);
                        lines.add("§f" + medal + " " + hunter.getName() +
                                 " §bM:" + distanceStr + "米 §6D:" + data.damagePoints + "点");
                    }
                }
            }
        }

        lines.add("§7");
        lines.add("§8HunterGame.server");
        lines.add("§7");
        return lines;
    }

    private List<String> getLuckyPillarsPlayingLines(GameRoom room) {
        List<String> lines = new ArrayList<>();
        int alive = room.getLuckyPillarsAlivePlayers().size();
        int eliminated = room.getLuckyPillarsEliminatedPlayers().size();
        lines.add("§7");
        lines.add("§f🕹 模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§f🗺 地图: §e" + room.getLuckyPillarsMapName());
        lines.add("§f🍀 存活玩家: §a" + alive + "§7/§e" + room.getPlayerCount());
        lines.add("§f☠ 已淘汰: §c" + eliminated);
        lines.add("§f✨ 发放间隔: §a" + room.getLuckyPillarsRandomItemIntervalSeconds() + "秒/次");
        lines.add("§f已进行: §e" + formatElapsedTime(room.getGameDuration()));
        lines.add("§7");
        if (!room.isGameActuallyStarted()) {
            lines.add("§e§l准备开始...");
            lines.add("§f- §7独立柱台与玻璃笼正在就位");
        } else {
            lines.add("§e§l经典规则");
            lines.add("§f- §6开局在玻璃笼里倒计时");
            lines.add("§f- §a开笼后按配置间隔获得随机原版物品");
            lines.add("§f- §5召唤物击杀算召唤者");
            lines.add("§f- §c掉下去、出界或死亡淘汰");
        }
        lines.add("§7");
        lines.add("§8LuckyPillars.classic");
        return lines;
    }

    private List<String> getLuckyPillarsWaitingLines(GameRoom room) {
        List<String> lines = new ArrayList<>();
        lines.add("§7");
        lines.add("§f🕹 模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§f🗺 地图: §e" + room.getLuckyPillarsMapName());
        lines.add("§f👥 当前人数: §a" + room.getPlayerCount() + "§7/§e" +
                (room.getMaxPlayers() == -1 ? "∞" : room.getMaxPlayers()));
        lines.add("§f🏷 房间号: §6" + room.getRoomId());
        lines.add("§7");
        if (room.getState() == RoomState.STARTING) {
            lines.add("§f距开始: §e" + formatCountdown(room.getCountdown()));
        } else {
            lines.add("§f状态: §6等待更多选手");
        }
        lines.add("§7");
        lines.add("§e§l经典说明");
        lines.add("§f- §6高空独立柱台 + 玻璃笼倒计时");
        lines.add("§f- §a开局破笼后进入随机原版物品乱斗");
        lines.add("§f- §c最后存活的人获胜");
        lines.add("§7");
        lines.add("§8LuckyPillars.classic");
        return lines;
    }

    private List<String> getIndependentModePlayingLines(GameRoom room) {
        List<String> lines = new ArrayList<>();
        lines.add("§7");
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§f👥 参赛人数: §b" + room.getPlayerCount());
        lines.add("§f已进行: §e" + formatElapsedTime(room.getGameDuration()));
        lines.add("§7");
        lines.add("§e§l独立模式");
        lines.add("§f- §b当前不会启用猎物 / 猎人流程");
        lines.add("§f- §a不会出现选猎物、选世界、猎人盲目提示");
        lines.add("§f- §d可继续在此基础上单独扩展新玩法");
        lines.add("§7");
        lines.add("§8HunterGame.server");
        lines.add("§7");
        return lines;
    }

    /**
     * 获取前N名猎人（按距离+伤害排序）
     */
    private List<Map.Entry<UUID, Double>> getTopHunters(GameRoom room, int limit) {
        Map<UUID, Double> hunterScores = new HashMap<>();

        // 获取猎物位置
        Set<UUID> preyUUIDs = room.getPreyUUIDs();
        if (preyUUIDs.isEmpty()) {
            return new ArrayList<>();
        }

        UUID preyUUID = room.getGameMode() == GameMode.SWAP && room.getActiveSwapPrey() != null
                ? room.getActiveSwapPrey()
                : preyUUIDs.iterator().next();
        Player prey = Bukkit.getPlayer(preyUUID);
        if (prey == null || !prey.isOnline()) {
            return new ArrayList<>();
        }

        // 计算每个猎人的分数
        for (UUID hunterUUID : room.getAllPlayerUUIDs()) {
            if (room.isPrey(hunterUUID)) continue;

            Player hunter = Bukkit.getPlayer(hunterUUID);
            if (hunter == null || !hunter.isOnline()) continue;

            // 距离分数：距离越近分数越高
            double distance = calculateTrackingDistance(room, hunter.getLocation(), prey.getLocation());
            double distanceScore = 1000.0 / (distance + 1);

            // 伤害分数
            double damageScore = room.getDamageDealt(hunterUUID) * 2.0;

            // 综合分数
            double totalScore = distanceScore + damageScore;
            hunterScores.put(hunterUUID, totalScore);
        }

        // 排序并返回前N名
        return hunterScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取前N名猎人（带详细数据：距离和伤害点数）
     */
    private List<HunterRankData> getTopHuntersWithData(GameRoom room, int limit) {
        List<HunterRankData> hunterDataList = new ArrayList<>();

        // 获取猎物位置
        Set<UUID> preyUUIDs = room.getPreyUUIDs();
        if (preyUUIDs.isEmpty()) {
            return hunterDataList;
        }

        UUID preyUUID = room.getGameMode() == GameMode.SWAP && room.getActiveSwapPrey() != null
                ? room.getActiveSwapPrey()
                : preyUUIDs.iterator().next();
        Player prey = Bukkit.getPlayer(preyUUID);
        if (prey == null || !prey.isOnline()) {
            return hunterDataList;
        }

        // 计算每个猎人的数据
        for (UUID hunterUUID : room.getAllPlayerUUIDs()) {
            if (room.isPrey(hunterUUID)) continue;

            Player hunter = Bukkit.getPlayer(hunterUUID);
            if (hunter == null || !hunter.isOnline()) continue;

            // 检查猎人和猎物是否在同一个世界
            if (!hunter.getWorld().equals(prey.getWorld())) {
                // 不在同一个世界，只显示伤害数据，距离显示为 "?"
                double damageDealt = room.getDamageDealt(hunterUUID);
                int damagePoints = (int) Math.round(damageDealt / 2.0);
                double damageScore = damageDealt * 2.0;
                // 使用 -1 表示距离未知
                hunterDataList.add(new HunterRankData(hunterUUID, -1, damagePoints, damageScore));
                continue;
            }

            // 距离（米）
            double distance = calculateTrackingDistance(room, hunter.getLocation(), prey.getLocation());
            int distanceMeters = (int) Math.round(distance);

            // 伤害点数（2点血 = 1点，即除以2）
            double damageDealt = room.getDamageDealt(hunterUUID);
            int damagePoints = (int) Math.round(damageDealt / 2.0);

            // 综合分数（用于排序）
            double distanceScore = 1000.0 / (distance + 1);
            double damageScore = damageDealt * 2.0;
            double totalScore = distanceScore + damageScore;

            hunterDataList.add(new HunterRankData(hunterUUID, distanceMeters, damagePoints, totalScore));
        }

        // 按综合分数排序并返回前N名
        return hunterDataList.stream()
                .sorted((a, b) -> Double.compare(b.totalScore, a.totalScore))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 猎人排行数据类
     */
    private static class HunterRankData {
        UUID uuid;
        int distance;      // 距离（米）
        int damagePoints;  // 伤害点数（2点血=1点）
        double totalScore; // 综合分数（用于排序）

        HunterRankData(UUID uuid, int distance, int damagePoints, double totalScore) {
            this.uuid = uuid;
            this.distance = distance;
            this.damagePoints = damagePoints;
            this.totalScore = totalScore;
        }
    }

    /**
     * 获取游戏模式的彩色显示
     */
    private String getColoredGameMode(GameMode mode) {
        return switch (mode) {
            case CLASSIC -> "§x§5§5§F§F§A§A经§x§6§6§F§F§B§B典§x§7§7§F§F§C§C模§x§8§8§F§F§D§D式"; // 青色渐变
            case RANDOM_COMPASS -> "§x§F§F§D§7§0§0随§x§F§F§B§B§0§0机§x§F§F§9§9§0§0指§x§F§F§7§7§0§0南§x§F§F§5§5§0§0针"; // 橙色渐变
            case SWAP -> "§x§F§F§5§5§F§F互§x§D§D§5§5§F§F换§x§B§B§5§5§F§F模§x§9§9§5§5§F§F式"; // 紫色渐变
            case NO_ITEM -> "§x§F§F§5§5§5§5无§x§F§F§7§7§7§7有§x§F§F§9§9§9§9模§x§F§F§B§B§B§B式"; // 红色渐变
            case SURVIVAL -> "§x§5§5§F§F§5§5存§x§7§7§F§F§7§7活§x§9§9§F§F§9§9模§x§B§B§F§F§B§B式"; // 绿色渐变
            case NETHER_CHAPTER -> "§x§F§F§6§6§0§0下§x§F§F§8§8§2§2界§x§F§F§A§A§4§4篇"; // 熔岩渐变
            case END_CHAPTER -> "§x§B§B§8§8§F§F末§x§C§C§9§9§F§F地§x§D§D§A§A§F§F篇"; // 末地渐变
            case FLASH -> "§x§F§F§F§F§9§9闪§x§F§F§E§E§6§6光§x§F§F§D§D§3§3模§x§F§F§C§C§0§0式"; // 金光渐变
            case FLASH_TOURNAMENT -> "§x§F§F§F§F§9§9闪§x§F§F§D§D§5§5光 §c§l赛事";
            case END_FLASH -> "§x§B§B§8§8§F§F终§x§D§D§A§A§F§F章§x§F§F§D§D§8§8·§x§F§F§F§F§A§A闪§x§D§D§F§F§C§C光"; // 终章渐变
            case LUCKY_PILLARS -> "§x§F§F§D§D§5§5幸§x§F§F§C§C§6§6运§x§F§F§B§B§7§7之§x§F§F§A§A§8§8柱";
            case BRICK_GUARD -> "§x§F§F§7§C§0§0板§x§F§F§9§0§2§0砖§x§D§D§5§5§1§1守§x§9§9§3§3§0§0卫§x§6§6§1§9§0§0战";
            case LUCKY_PILLARS_PVP -> "§x§F§F§8§8§5§5幸§x§F§F§A§A§6§6运§x§F§F§C§C§7§7之§x§F§F§E§E§8§8柱§x§F§F§6§6§6§6PVP";
            case TNT_RUN -> "§x§F§F§8§8§5§5T§x§F§F§9§9§6§6N§x§F§F§A§A§7§7T§x§F§F§B§B§8§8跑§x§F§F§C§C§9§9酷";
            case BLOCK_PARTY -> "§x§D§D§8§8§F§F方§x§C§C§9§9§F§F块§x§B§B§A§A§F§F派§x§A§A§B§B§F§F对";
            case CUSTOM -> "§x§F§F§F§F§5§5自§x§F§F§F§F§7§7定§x§F§F§F§F§9§9义§x§F§F§F§F§B§B模§x§F§F§F§F§D§D式"; // 黄色渐变
        };
    }

    /**
     * 格式化倒计时时间
     */
    private String formatCountdown(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return String.format("%d分%d秒", minutes, secs);
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 格式化已进行时间
     */
    private String formatElapsedTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%d时%d分", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 游戏结束阶段的记分板
     */
    private List<String> getEndedLines(GameRoom room, Player viewer) {
        List<String> lines = new ArrayList<>();

        lines.add("§7");

        // 显示游戏模式
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§7");

        if (room.getGameMode().isLuckyPillars()) {
            lines.add("§x§F§F§D§D§5§5🍀 §e§l幸运之柱经典已结束");
            lines.add("§7");
            lines.add("§f地图: §e" + room.getLuckyPillarsMapName());
            String winnerName = "无人";
            List<UUID> alivePlayers = room.getLuckyPillarsAlivePlayers();
            if (!alivePlayers.isEmpty()) {
                Player winner = Bukkit.getPlayer(alivePlayers.get(0));
                if (winner != null) {
                    winnerName = winner.getName();
                }
            }
            lines.add("§f胜者: §a" + winnerName);
            lines.add("§f存活玩家: §a" + room.getLuckyPillarsAlivePlayers().size());
            lines.add("§f淘汰玩家: §c" + room.getLuckyPillarsEliminatedPlayers().size());
            lines.add("§f游戏时长: §e" + formatElapsedTime(Math.max(0L, System.currentTimeMillis() - room.getGameStartTime())));
            lines.add("§7");
            lines.add("§8LuckyPillars.classic");
            lines.add("§7");
            return lines;
        }

        // 判断是猎人胜利还是猎物胜利
        boolean preyWon = room.isPreyWon();

        if (preyWon) {
            // 猎物胜利
            lines.add("§x§5§5§F§F§5§5✓ §a§l猎物胜利！");
            lines.add("§7");

            // 显示猎物名字
            Set<UUID> preyUUIDs = room.getPreyUUIDs();
            if (!preyUUIDs.isEmpty()) {
                List<String> preyNames = new ArrayList<>();
                for (UUID uuid : preyUUIDs) {
                    Player prey = Bukkit.getPlayer(uuid);
                    if (prey != null) {
                        preyNames.add(prey.getName());
                    }
                }
                lines.add("§f🏹 猎物: §a" + String.join("§7, §a", preyNames));
            }

            lines.add("§7");
            lines.add("§f游戏时长: §e" + formatElapsedTime(System.currentTimeMillis() - room.getGameStartTime()));
        } else {
            // 猎人胜利
            lines.add("§x§F§F§5§5§5§5⚔ §c§l猎人胜利！");
            lines.add("§7");

            // 显示猎人数量
            int hunterCount = 0;
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                if (room.isHunter(uuid)) {
                    hunterCount++;
                }
            }
            lines.add("§f⚔ 猎人: §c" + hunterCount + "人");

            lines.add("§7");
            lines.add("§f游戏时长: §e" + formatElapsedTime(System.currentTimeMillis() - room.getGameStartTime()));

            lines.add("§7");

            if (room.getGameMode() != GameMode.NO_ITEM) {
                List<HunterRankData> topHunters = getTopHuntersWithData(room, 3);
                if (!topHunters.isEmpty()) {
                    lines.add("§e§l🏆 MVP排行");
                    for (int i = 0; i < topHunters.size(); i++) {
                        HunterRankData data = topHunters.get(i);
                        Player hunter = Bukkit.getPlayer(data.uuid);
                        if (hunter != null) {
                            String medal = i == 0 ? "🥇" : (i == 1 ? "🥈" : "🥉");
                            String distanceStr = data.distance >= 0 ? data.distance + "m" : "?";
                            lines.add("§f" + medal + " " + hunter.getName() + " §7(" + distanceStr + ")");
                        }
                    }
                }
            }
        }

        lines.add("§7");
        lines.add("§8HunterGame.server");
        lines.add("§7");

        return lines;
    }

    private List<String> getSwapCountdownLines(GameRoom room) {
        List<String> lines = new ArrayList<>();
        lines.add("§7");
        lines.add("§f🕹 游戏模式: " + getColoredGameMode(room.getGameMode()));
        lines.add("§f状态: §8暗处待命");
        lines.add("§f接管倒计时: §e" + formatCountdown(room.getSwapCountdownSeconds()));
        String activeName = "未知";
        if (room.getActiveSwapPrey() != null) {
            Player active = Bukkit.getPlayer(room.getActiveSwapPrey());
            if (active != null) {
                activeName = active.getName();
            }
        }
        lines.add("§f当前接力: §a" + activeName);
        lines.add("§7");
        lines.add("§f- §8你正在等待接管");
        lines.add("§f- §8游戏聊天已隔离");
        lines.add("§7");
        lines.add("§8HunterGame.server");
        lines.add("§7");
        return lines;
    }

    private double calculateTrackingDistance(GameRoom room, org.bukkit.Location hunterLoc, org.bukkit.Location preyLoc) {
        if (room.hasModifier("IncludeY")) {
            return Math.sqrt(
                    Math.pow(hunterLoc.getX() - preyLoc.getX(), 2) +
                    Math.pow(hunterLoc.getZ() - preyLoc.getZ(), 2)
            );
        }
        return hunterLoc.distance(preyLoc);
    }

    /**
     * 清除所有记分板
     */
    public void clearAll() {
        for (UUID uuid : new HashSet<>(playerScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeScoreboard(player);
            }
        }
        playerScoreboards.clear();

        // 取消所有延迟清除任务
        for (BukkitTask task : endedScoreboardTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        endedScoreboardTasks.clear();
    }

    /**
     * 为游戏结束状态安排延迟清除记分板（5秒后）
     */
    private void scheduleEndedScoreboardClear(GameRoom room) {
        String roomId = room.getRoomId();

        // 如果已经有任务在运行，不重复创建
        if (endedScoreboardTasks.containsKey(roomId)) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 清除该房间所有玩家的记分板
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    removeScoreboard(player);
                }
            }
            // 清除旁观者的记分板
            for (UUID uuid : room.getSpectators()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    removeScoreboard(player);
                }
            }
            endedScoreboardTasks.remove(roomId);
        }, 100L); // 5秒 = 100 ticks

        endedScoreboardTasks.put(roomId, task);
    }

    private void cancelEndedScoreboardClear(String roomId) {
        BukkitTask task = endedScoreboardTasks.remove(roomId);
        if (task != null) {
            task.cancel();
        }
    }
}
