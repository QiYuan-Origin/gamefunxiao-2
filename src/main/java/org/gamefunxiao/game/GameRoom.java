package org.gamefunxiao.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameRoom {

    private final String roomId;
    private final UUID ownerUuid;
    private final GameMode gameMode;
    private final int maxPlayers;
    private final boolean isPublic;
    private final boolean isCustomRoom;
    private final Set<String> modifiers;
    private final Set<UUID> invitedPlayers;

    private RoomState state;
    private final Set<UUID> players;
    private final Set<UUID> preyPlayers;
    private final Set<UUID> spectators;
    private final Map<UUID, Location> previousLocations;
    private final Map<UUID, Double> previousHealth;
    private final Map<UUID, Integer> previousFoodLevel;
    private final Map<UUID, Integer> previousExpLevel; // 经验等级
    private final Map<UUID, Float> previousExp; // 经验进度
    private final Map<UUID, Collection<org.bukkit.potion.PotionEffect>> previousPotionEffects; // 药水效果
    private final Map<UUID, Map<String, Set<String>>> previousAdvancements; // 成就进度快照
    private final Map<UUID, Set<org.bukkit.NamespacedKey>> previousRecipes; // 已解锁配方快照

    // 本局统计数据
    private final Map<UUID, Integer> attackCount; // 攻击猎物次数
    private final Map<UUID, Double> damageDealt; // 对猎物造成的伤害
    private final Map<UUID, Double> distanceRun; // 奔跑距离
    private final Map<UUID, Location> lastLocation; // 上次位置（用于计算距离）
    private final Map<UUID, Integer> randomCompassUseCounts; // 随机指南针使用次数
    private final Set<UUID> usedPreyRespawn; // 猎物复活修饰符已使用玩家
    private final Set<String> luckyPillarBlocks; // 幸运之柱模式生成的幸运方块坐标
    private final Set<String> luckyPillarsCageBlocks; // 幸运之柱倒计时玻璃笼坐标
    private final Set<UUID> luckyPillarsEliminatedPlayers; // 幸运之柱已淘汰玩家
    private final Map<UUID, Integer> luckyPillarsKillCounts; // 幸运之柱击杀/召唤击杀统计
    private final Map<UUID, Integer> luckyPillarsSummonKillCounts; // 幸运之柱召唤物击杀统计
    private final Set<UUID> miniGameEliminatedPlayers; // 自动竞技场小游戏已淘汰玩家
    private final Map<UUID, Long> miniGameSurvivalTicks; // 自动竞技场小游戏存活时长
    private final Set<String> miniGameProtectedBlocks; // 自动竞技场核心地图方块
    private Location luckyPillarsArenaCenter; // 幸运之柱竞技场中心
    private Location luckyPillarsSpectatorSpawn; // 幸运之柱观战点
    private int luckyPillarsEliminationY; // 幸运之柱掉落淘汰高度
    private double luckyPillarsBoundaryRadius; // 幸运之柱越界半径
    private String luckyPillarsMapId = "default"; // 本局幸运之柱地图ID
    private String luckyPillarsMapName = "默认地图"; // 本局幸运之柱地图显示名
    private String luckyPillarsThemeId = "WOOL"; // 本局幸运之柱主题
    private int luckyPillarsGameTimeSeconds = 480; // 本局最长游戏时间
    private int luckyPillarsRandomItemIntervalSeconds = 5; // 随机物品间隔
    private int luckyPillarsRandomEventIntervalSeconds = 30; // 随机事件间隔
    private String miniGameMapId = "default"; // 自动竞技场地图ID
    private String miniGameMapName = "默认地图"; // 自动竞技场地图名称
    private Location miniGameArenaCenter; // 自动竞技场中心
    private Location miniGameSpectatorSpawn; // 自动竞技场观战点
    private int miniGameEliminationY = 0; // 自动竞技场淘汰高度
    private double miniGameBoundaryRadius = 0.0D; // 自动竞技场边界
    private int miniGameMaxGameTimeSeconds = 480; // 自动竞技场最大时间
    private int miniGameRound = 0; // 方块派对等回合数
    private String brickGuardMapId = "default"; // 板砖守卫战地图ID
    private String brickGuardMapName = "默认地图"; // 板砖守卫战地图名称
    private Location brickGuardBrickSpawn; // 板砖队出生点
    private Location brickGuardNetherBrickSpawn; // 下界砖队出生点
    private Location brickGuardCoreLocation; // 板砖核心位置
    private Location brickGuardFakeBorderCenter; // 伪边界中心
    private double brickGuardFakeBorderRadius = 1500.0D; // 伪边界半径

    private World gameWorld;
    private Location rewardChestLocation;
    private boolean rewardChestOpened = false;
    private long createTime;
    private long gameStartTime;
    private int countdown;
    private boolean gameStartCountdown = false; // 猎物按下开始后的3秒倒计时
    private boolean preyStarted = false; // 猎物是否已按下开始按钮（或自动开始）
    private boolean gameActuallyStarted = false; // 游戏是否正式开始（doActualGameStart后才为true）
    private boolean worldSelectionConfirmed = false; // 世界是否已提前锁定（下界篇给猎人留投票时间）
    private boolean adminForceStartUsed = false; // 管理员提前开始是否已使用
    private boolean preyQuit = false; // 猎物是否是主动退出导致游戏结束
    private Boolean preyWonResult = null; // 本局最终是否为猎物胜利
    private boolean endFlashDragonDefeated = false; // 闪光/终章闪光末影龙是否已被击败，击败后等待猎物进入龙池传送门结算
    private int preyStartCountdownSeconds = 0; // 猎物的随机倒计时秒数
    private boolean doublePreyEnabled = false; // 是否已开启双猎物
    private boolean flashTriplePreyEnabled = false; // 闪光模式是否已开启第三位猎物
    private UUID lockedFirstDualPrey; // 双猎物模式下第一位已锁定的猎物
    private DualPreyProposalType dualPreyProposalType; // 双猎物世界协商类型
    private UUID dualPreyProposalInitiator; // 双猎物世界协商发起者
    private int dualPreyProposalCountdown = 0; // 双猎物世界协商倒计时
    private UUID stackedPreyCarrier; // 双/三猎物叠骑时底部猎物
    private UUID stackedPreyPassenger; // 双/三猎物叠骑时中间猎物
    private UUID stackedThirdPreyPassenger; // 三猎物叠骑时最上方猎物
    private boolean dualPreyStackLocked = false; // 正式开始前是否锁定下车
    private boolean endChapterDivisionActive = false; // 末地篇分工阶段是否进行中
    private int endChapterDivisionCountdown = 0; // 末地篇分工阶段倒计时
    private UUID activeSwapPrey; // 互换模式当前正在游玩的猎物
    private UUID countdownSwapPrey; // 互换模式当前倒计时的猎物
    private int swapCountdownSeconds = 0; // 互换模式当前倒计时秒数
    private final Map<UUID, Location> flashTournamentStartLocations = new HashMap<>(); // 闪光赛事：猎物正式开始触发点
    private long flashTournamentMovementUnlockMillis = 0L; // 闪光赛事：进入世界后前2秒冻结移动
    private UUID victoryEffectTriggerUuid; // 本局猎人胜利特效触发者
    private Location victoryEffectLocation; // 本局猎人胜利特效播放位置
    private final Map<UUID, org.bukkit.Location> pendingRespawnLocations = new HashMap<>(); // 待复活位置
    private int worldRerollCount = 0; // 世界切换次数
    private int flashPreyVoteStage = 0; // 闪光模式猎物投票阶段：0=第一轮，1=第二轮，2=完成

    // 投票相关
    private final Map<UUID, UUID> preyVotes;
    private final Set<UUID> doublePreyVotes;
    private final Set<UUID> flashTriplePreyVotes;
    private final Set<UUID> speedUpVotes;
    private final Map<UUID, NetherHunterScenario> netherHunterScenarioVotes;
    private final Map<UUID, EndChapterKit> endPreyKitSelections;
    private final Map<UUID, EndChapterKit> endHunterKitVotes;
    private final Map<UUID, EndChapterKit> assignedEndHunterKits;
    private final Map<UUID, String> assignedEndFlashKitNames;
    private final Map<UUID, Integer> assignedEndFlashKitStartExpLevels;
    private final Map<UUID, EndPreyPosition> endPreyPositionSelections;
    private final Map<UUID, EndHunterPosition> endHunterPositionVotes;
    private final Map<UUID, EndHunterPosition> assignedEndHunterPositions;

    public GameRoom(String roomId, UUID ownerUuid, GameMode gameMode, int maxPlayers,
                    boolean isPublic, Set<String> modifiers, boolean isCustomRoom) {
        this.roomId = roomId;
        this.ownerUuid = ownerUuid;
        this.gameMode = gameMode;
        this.maxPlayers = maxPlayers;
        this.isPublic = isPublic;
        this.modifiers = modifiers != null ? new HashSet<>(modifiers) : new HashSet<>();
        this.isCustomRoom = isCustomRoom;
        this.invitedPlayers = new HashSet<>();

        this.state = RoomState.WAITING;
        this.players = new HashSet<>();
        this.preyPlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.previousLocations = new HashMap<>();
        this.previousHealth = new HashMap<>();
        this.previousFoodLevel = new HashMap<>();
        this.previousExpLevel = new HashMap<>();
        this.previousExp = new HashMap<>();
        this.previousPotionEffects = new HashMap<>();
        this.previousAdvancements = new HashMap<>();
        this.previousRecipes = new HashMap<>();

        // 初始化本局统计数据
        this.attackCount = new HashMap<>();
        this.damageDealt = new HashMap<>();
        this.distanceRun = new HashMap<>();
        this.lastLocation = new HashMap<>();
        this.randomCompassUseCounts = new HashMap<>();
        this.usedPreyRespawn = new HashSet<>();
        this.luckyPillarBlocks = new HashSet<>();
        this.luckyPillarsCageBlocks = new HashSet<>();
        this.luckyPillarsEliminatedPlayers = new HashSet<>();
        this.luckyPillarsKillCounts = new HashMap<>();
        this.luckyPillarsSummonKillCounts = new HashMap<>();
        this.miniGameEliminatedPlayers = new HashSet<>();
        this.miniGameSurvivalTicks = new HashMap<>();
        this.miniGameProtectedBlocks = new HashSet<>();

        this.createTime = System.currentTimeMillis();
        this.preyVotes = new HashMap<>();
        this.doublePreyVotes = new HashSet<>();
        this.flashTriplePreyVotes = new HashSet<>();
        this.speedUpVotes = new HashSet<>();
        this.netherHunterScenarioVotes = new HashMap<>();
        this.endPreyKitSelections = new HashMap<>();
        this.endHunterKitVotes = new HashMap<>();
        this.assignedEndHunterKits = new HashMap<>();
        this.assignedEndFlashKitNames = new HashMap<>();
        this.assignedEndFlashKitStartExpLevels = new HashMap<>();
        this.endPreyPositionSelections = new HashMap<>();
        this.endHunterPositionVotes = new HashMap<>();
        this.assignedEndHunterPositions = new HashMap<>();

        // 房主自动加入
        players.add(ownerUuid);
    }

    public String getRoomId() {
        return roomId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return Bukkit.getOfflinePlayer(ownerUuid).getName();
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public String getModeName() {
        return gameMode.getDisplayName();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isCustomRoom() {
        return isCustomRoom;
    }

    public Set<String> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public RoomState getState() {
        return state;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Set<UUID> getAllPlayerUUIDs() {
        return Collections.unmodifiableSet(players);
    }

    public Set<UUID> getPreyUUIDs() {
        return Collections.unmodifiableSet(preyPlayers);
    }

    public List<String> getPreyNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : preyPlayers) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    public boolean isPrey(UUID uuid) {
        return preyPlayers.contains(uuid);
    }

    public boolean isHunter(UUID uuid) {
        return players.contains(uuid) && !preyPlayers.contains(uuid) && !spectators.contains(uuid);
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getGameDuration() {
        if (gameStartTime == 0) return 0;
        return System.currentTimeMillis() - gameStartTime;
    }

    public void setGameStartTime(long time) {
        this.gameStartTime = time;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public boolean isPreyWon() {
        return preyWonResult != null ? preyWonResult : !preyQuit;
    }

    public void setPreyWon(boolean preyWon) {
        this.preyWonResult = preyWon;
    }

    public boolean isEndFlashDragonDefeated() {
        return endFlashDragonDefeated;
    }

    public void setEndFlashDragonDefeated(boolean endFlashDragonDefeated) {
        this.endFlashDragonDefeated = endFlashDragonDefeated;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World world) {
        this.gameWorld = world;
    }


    // 玩家管理
    public boolean addPlayer(UUID uuid) {
        if (state != RoomState.WAITING && state != RoomState.STARTING) return false;
        if (maxPlayers != -1 && players.size() >= maxPlayers) return false;
        if (!isPublic && !invitedPlayers.contains(uuid) && !uuid.equals(ownerUuid)) return false;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            previousLocations.put(uuid, player.getLocation());
        }
        return players.add(uuid);
    }

    public boolean addRejoiningHunter(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        if (!players.contains(uuid) && maxPlayers != -1 && players.size() >= maxPlayers) {
            return false;
        }
        spectators.remove(uuid);
        preyPlayers.remove(uuid);
        return players.add(uuid) || players.contains(uuid);
    }

    public boolean removePlayer(UUID uuid) {
        preyPlayers.remove(uuid);
        preyVotes.remove(uuid);
        doublePreyVotes.remove(uuid);
        flashTriplePreyVotes.remove(uuid);
        speedUpVotes.remove(uuid);
        netherHunterScenarioVotes.remove(uuid);
        endPreyKitSelections.remove(uuid);
        endHunterKitVotes.remove(uuid);
        assignedEndHunterKits.remove(uuid);
        assignedEndFlashKitNames.remove(uuid);
        assignedEndFlashKitStartExpLevels.remove(uuid);
        endPreyPositionSelections.remove(uuid);
        endHunterPositionVotes.remove(uuid);
        assignedEndHunterPositions.remove(uuid);
        luckyPillarsEliminatedPlayers.remove(uuid);
        miniGameEliminatedPlayers.remove(uuid);
        miniGameSurvivalTicks.remove(uuid);
        if (uuid != null && uuid.equals(lockedFirstDualPrey)) {
            lockedFirstDualPrey = null;
        }
        if (uuid != null && uuid.equals(dualPreyProposalInitiator)) {
            clearDualPreyProposal();
        }
        if (uuid != null && (uuid.equals(stackedPreyCarrier) || uuid.equals(stackedPreyPassenger) || uuid.equals(stackedThirdPreyPassenger))) {
            clearDualPreyStack();
        }
        flashTournamentStartLocations.remove(uuid);
        return players.remove(uuid);
    }

    public void addSpectator(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !players.contains(uuid)) {
            previousLocations.put(uuid, player.getLocation());
        }
        spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public Set<UUID> getSpectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }

    public Location getPreviousLocation(UUID uuid) {
        return previousLocations.get(uuid);
    }

    public void setPreviousLocation(UUID uuid, Location location) {
        previousLocations.put(uuid, location);
    }

    public void setPreviousHealth(UUID uuid, double health) {
        previousHealth.put(uuid, health);
    }

    public Double getPreviousHealth(UUID uuid) {
        return previousHealth.get(uuid);
    }

    public void setPreviousFoodLevel(UUID uuid, int foodLevel) {
        previousFoodLevel.put(uuid, foodLevel);
    }

    public Integer getPreviousFoodLevel(UUID uuid) {
        return previousFoodLevel.get(uuid);
    }

    public void setPreviousExpLevel(UUID uuid, int expLevel) {
        previousExpLevel.put(uuid, expLevel);
    }

    public Integer getPreviousExpLevel(UUID uuid) {
        return previousExpLevel.get(uuid);
    }

    public void setPreviousExp(UUID uuid, float exp) {
        previousExp.put(uuid, exp);
    }

    public Float getPreviousExp(UUID uuid) {
        return previousExp.get(uuid);
    }

    public void setPreviousPotionEffects(UUID uuid, Collection<org.bukkit.potion.PotionEffect> effects) {
        previousPotionEffects.put(uuid, new ArrayList<>(effects));
    }

    public Collection<org.bukkit.potion.PotionEffect> getPreviousPotionEffects(UUID uuid) {
        return previousPotionEffects.get(uuid);
    }

    public void setPreviousAdvancements(UUID uuid, Map<String, Set<String>> advancements) {
        if (advancements == null) {
            previousAdvancements.remove(uuid);
            return;
        }

        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : advancements.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        previousAdvancements.put(uuid, copy);
    }

    public Map<String, Set<String>> getPreviousAdvancements(UUID uuid) {
        return previousAdvancements.get(uuid);
    }

    public void setPreviousRecipes(UUID uuid, Set<org.bukkit.NamespacedKey> recipes) {
        if (recipes == null) {
            previousRecipes.remove(uuid);
            return;
        }
        previousRecipes.put(uuid, new HashSet<>(recipes));
    }

    public Set<org.bukkit.NamespacedKey> getPreviousRecipes(UUID uuid) {
        Set<org.bukkit.NamespacedKey> recipes = previousRecipes.get(uuid);
        return recipes == null ? null : new HashSet<>(recipes);
    }

    // 邀请管理
    public void invitePlayer(UUID uuid) {
        invitedPlayers.add(uuid);
    }

    public boolean isInvited(UUID uuid) {
        return invitedPlayers.contains(uuid);
    }

    // 猎物投票
    public void voteForPrey(UUID voter, UUID target) {
        if (players.contains(target)) {
            preyVotes.put(voter, target);
        }
    }

    public void clearPreyVotes() {
        preyVotes.clear();
    }

    public UUID getPlayerVote(UUID voter) {
        return preyVotes.get(voter);
    }

    public UUID getMostVotedPrey() {
        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID target : preyVotes.values()) {
            voteCounts.merge(target, 1, Integer::sum);
        }

        UUID mostVoted = null;
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
            }
        }
        return mostVoted;
    }

    public int getVoteCount(UUID target) {
        int count = 0;
        for (UUID voted : preyVotes.values()) {
            if (voted.equals(target)) {
                count++;
            }
        }
        return count;
    }

    public Map<UUID, Integer> getAllVoteCounts() {
        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID target : preyVotes.values()) {
            voteCounts.merge(target, 1, Integer::sum);
        }
        return voteCounts;
    }

    public void voteNetherHunterScenario(UUID voter, NetherHunterScenario scenario) {
        if (scenario == null || !players.contains(voter) || !isHunter(voter)) {
            return;
        }
        netherHunterScenarioVotes.put(voter, scenario);
    }

    public NetherHunterScenario getPlayerNetherHunterScenarioVote(UUID voter) {
        return netherHunterScenarioVotes.get(voter);
    }

    public int getNetherHunterScenarioVoteCount(NetherHunterScenario scenario) {
        int count = 0;
        for (NetherHunterScenario voted : netherHunterScenarioVotes.values()) {
            if (voted == scenario) {
                count++;
            }
        }
        return count;
    }

    public Map<NetherHunterScenario, Integer> getAllNetherHunterScenarioVoteCounts() {
        Map<NetherHunterScenario, Integer> voteCounts = new EnumMap<>(NetherHunterScenario.class);
        for (NetherHunterScenario scenario : NetherHunterScenario.values()) {
            voteCounts.put(scenario, 0);
        }
        for (NetherHunterScenario target : netherHunterScenarioVotes.values()) {
            voteCounts.merge(target, 1, Integer::sum);
        }
        return voteCounts;
    }

    public void clearNetherHunterScenarioVotes() {
        netherHunterScenarioVotes.clear();
    }

    public boolean isEndChapterDivisionActive() {
        return endChapterDivisionActive;
    }

    public void setEndChapterDivisionActive(boolean endChapterDivisionActive) {
        this.endChapterDivisionActive = endChapterDivisionActive;
    }

    public int getEndChapterDivisionCountdown() {
        return endChapterDivisionCountdown;
    }

    public void setEndChapterDivisionCountdown(int endChapterDivisionCountdown) {
        this.endChapterDivisionCountdown = endChapterDivisionCountdown;
    }

    public void setEndPreyKitSelection(UUID uuid, EndChapterKit kit) {
        if (uuid != null && kit != null) {
            endPreyKitSelections.put(uuid, kit);
        }
    }

    public EndChapterKit getEndPreyKitSelection(UUID uuid) {
        return endPreyKitSelections.get(uuid);
    }

    public void setEndHunterKitVote(UUID uuid, EndChapterKit kit) {
        if (uuid != null && kit != null) {
            endHunterKitVotes.put(uuid, kit);
        }
    }

    public EndChapterKit getEndHunterKitVote(UUID uuid) {
        return endHunterKitVotes.get(uuid);
    }

    public Map<UUID, EndChapterKit> getAllEndHunterKitVotes() {
        return Collections.unmodifiableMap(endHunterKitVotes);
    }

    public void assignEndHunterKit(UUID uuid, EndChapterKit kit) {
        if (uuid != null && kit != null) {
            assignedEndHunterKits.put(uuid, kit);
        }
    }

    public EndChapterKit getAssignedEndHunterKit(UUID uuid) {
        return assignedEndHunterKits.get(uuid);
    }

    public void clearAssignedEndHunterKits() {
        assignedEndHunterKits.clear();
    }

    public void assignEndFlashKitName(UUID uuid, String kitName) {
        if (uuid == null) {
            return;
        }
        if (kitName == null || kitName.isBlank()) {
            assignedEndFlashKitNames.remove(uuid);
            assignedEndFlashKitStartExpLevels.remove(uuid);
            return;
        }
        assignedEndFlashKitNames.put(uuid, kitName);
    }

    public String getAssignedEndFlashKitName(UUID uuid) {
        return assignedEndFlashKitNames.get(uuid);
    }

    public void clearAssignedEndFlashKitNames() {
        assignedEndFlashKitNames.clear();
        assignedEndFlashKitStartExpLevels.clear();
    }

    public void assignEndFlashKitStartExpLevel(UUID uuid, int level) {
        if (uuid == null) {
            return;
        }
        assignedEndFlashKitStartExpLevels.put(uuid, Math.max(0, Math.min(100, level)));
    }

    public int getAssignedEndFlashKitStartExpLevel(UUID uuid) {
        return uuid == null ? 0 : assignedEndFlashKitStartExpLevels.getOrDefault(uuid, 0);
    }

    public void setEndPreyPositionSelection(UUID uuid, EndPreyPosition position) {
        if (uuid != null && position != null) {
            endPreyPositionSelections.put(uuid, position);
        }
    }

    public EndPreyPosition getEndPreyPositionSelection(UUID uuid) {
        return endPreyPositionSelections.get(uuid);
    }

    public void setEndHunterPositionVote(UUID uuid, EndHunterPosition position) {
        if (uuid != null && position != null) {
            endHunterPositionVotes.put(uuid, position);
        }
    }

    public EndHunterPosition getEndHunterPositionVote(UUID uuid) {
        return endHunterPositionVotes.get(uuid);
    }

    public Map<UUID, EndHunterPosition> getAllEndHunterPositionVotes() {
        return Collections.unmodifiableMap(endHunterPositionVotes);
    }

    public void assignEndHunterPosition(UUID uuid, EndHunterPosition position) {
        if (uuid != null && position != null) {
            assignedEndHunterPositions.put(uuid, position);
        }
    }

    public EndHunterPosition getAssignedEndHunterPosition(UUID uuid) {
        return assignedEndHunterPositions.get(uuid);
    }

    public void clearAssignedEndHunterPositions() {
        assignedEndHunterPositions.clear();
    }

    public void clearEndChapterDivisionData() {
        endChapterDivisionActive = false;
        endChapterDivisionCountdown = 0;
        endPreyKitSelections.clear();
        endHunterKitVotes.clear();
        assignedEndHunterKits.clear();
        assignedEndFlashKitNames.clear();
        assignedEndFlashKitStartExpLevels.clear();
        endPreyPositionSelections.clear();
        endHunterPositionVotes.clear();
        assignedEndHunterPositions.clear();
    }

    public void voteDoublePrey(UUID uuid) {
        if (uuid != null && players.contains(uuid)) {
            doublePreyVotes.add(uuid);
        }
    }

    public boolean hasVotedDoublePrey(UUID uuid) {
        return uuid != null && doublePreyVotes.contains(uuid);
    }

    public int getDoublePreyVoteCount() {
        return doublePreyVotes.size();
    }

    public Set<UUID> getDoublePreyVotes() {
        return Collections.unmodifiableSet(doublePreyVotes);
    }

    public void clearDoublePreyVotes() {
        doublePreyVotes.clear();
    }

    public void setPrey(UUID uuid) {
        if (players.contains(uuid)) {
            preyPlayers.add(uuid);
            doublePreyVotes.remove(uuid);
            netherHunterScenarioVotes.remove(uuid);
        }
    }

    public boolean isDoublePreyEnabled() {
        return doublePreyEnabled;
    }

    public void setDoublePreyEnabled(boolean doublePreyEnabled) {
        this.doublePreyEnabled = doublePreyEnabled;
    }

    public boolean isFlashTriplePreyEnabled() {
        return flashTriplePreyEnabled;
    }

    public void setFlashTriplePreyEnabled(boolean flashTriplePreyEnabled) {
        this.flashTriplePreyEnabled = flashTriplePreyEnabled;
    }

    public void voteFlashTriplePrey(UUID uuid) {
        if (uuid != null && players.contains(uuid)) {
            flashTriplePreyVotes.add(uuid);
        }
    }

    public boolean hasVotedFlashTriplePrey(UUID uuid) {
        return uuid != null && flashTriplePreyVotes.contains(uuid);
    }

    public int getFlashTriplePreyVoteCount() {
        return flashTriplePreyVotes.size();
    }

    public Set<UUID> getFlashTriplePreyVotes() {
        return Collections.unmodifiableSet(flashTriplePreyVotes);
    }

    public void clearFlashTriplePreyVotes() {
        flashTriplePreyVotes.clear();
    }

    public UUID getLockedFirstDualPrey() {
        return lockedFirstDualPrey;
    }

    public void setLockedFirstDualPrey(UUID lockedFirstDualPrey) {
        this.lockedFirstDualPrey = lockedFirstDualPrey;
    }

    public boolean hasPendingDualPreyProposal() {
        return dualPreyProposalType != null && dualPreyProposalInitiator != null && dualPreyProposalCountdown > 0;
    }

    public DualPreyProposalType getDualPreyProposalType() {
        return dualPreyProposalType;
    }

    public UUID getDualPreyProposalInitiator() {
        return dualPreyProposalInitiator;
    }

    public int getDualPreyProposalCountdown() {
        return dualPreyProposalCountdown;
    }

    public void beginDualPreyProposal(DualPreyProposalType type, UUID initiator, int countdownSeconds) {
        this.dualPreyProposalType = type;
        this.dualPreyProposalInitiator = initiator;
        this.dualPreyProposalCountdown = countdownSeconds;
    }

    public void setDualPreyProposalCountdown(int dualPreyProposalCountdown) {
        this.dualPreyProposalCountdown = dualPreyProposalCountdown;
    }

    public void clearDualPreyProposal() {
        this.dualPreyProposalType = null;
        this.dualPreyProposalInitiator = null;
        this.dualPreyProposalCountdown = 0;
    }

    public int getFlashPreyVoteStage() {
        return flashPreyVoteStage;
    }

    public void setFlashPreyVoteStage(int flashPreyVoteStage) {
        this.flashPreyVoteStage = flashPreyVoteStage;
    }

    public UUID getStackedPreyCarrier() {
        return stackedPreyCarrier;
    }

    public UUID getStackedPreyPassenger() {
        return stackedPreyPassenger;
    }

    public boolean isDualPreyStackLocked() {
        return dualPreyStackLocked;
    }

    public void setDualPreyStack(UUID carrier, UUID passenger, boolean locked) {
        this.stackedPreyCarrier = carrier;
        this.stackedPreyPassenger = passenger;
        this.stackedThirdPreyPassenger = null;
        this.dualPreyStackLocked = locked;
    }

    public void setTriplePreyStack(UUID carrier, UUID middlePassenger, UUID topPassenger, boolean locked) {
        this.stackedPreyCarrier = carrier;
        this.stackedPreyPassenger = middlePassenger;
        this.stackedThirdPreyPassenger = topPassenger;
        this.dualPreyStackLocked = locked;
    }

    public void setDualPreyStackLocked(boolean dualPreyStackLocked) {
        this.dualPreyStackLocked = dualPreyStackLocked;
    }

    public UUID getStackedThirdPreyPassenger() {
        return stackedThirdPreyPassenger;
    }

    public boolean isDualPreyPassenger(UUID uuid) {
        return uuid != null && uuid.equals(stackedPreyPassenger);
    }

    public boolean isDualPreyCarrier(UUID uuid) {
        return uuid != null && uuid.equals(stackedPreyCarrier);
    }

    public boolean isTriplePreyPassenger(UUID uuid) {
        return uuid != null && uuid.equals(stackedThirdPreyPassenger);
    }

    public boolean isAnyStackedPreyPassenger(UUID uuid) {
        return isDualPreyPassenger(uuid) || isTriplePreyPassenger(uuid);
    }

    public void clearDualPreyStack() {
        this.stackedPreyCarrier = null;
        this.stackedPreyPassenger = null;
        this.stackedThirdPreyPassenger = null;
        this.dualPreyStackLocked = false;
    }

    // 加速投票
    public void voteSpeedUp(UUID uuid) {
        speedUpVotes.add(uuid);
    }

    public boolean shouldSpeedUp() {
        int required = players.size() <= 2 ? 2 : (players.size() + 1) / 2;
        return speedUpVotes.size() >= required;
    }

    // 倒计时
    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public boolean isGameStartCountdown() {
        return gameStartCountdown;
    }

    public void setGameStartCountdown(boolean gameStartCountdown) {
        this.gameStartCountdown = gameStartCountdown;
    }

    public boolean isPreyStarted() {
        return preyStarted;
    }

    public void setPreyStarted(boolean preyStarted) {
        this.preyStarted = preyStarted;
    }

    public boolean isGameActuallyStarted() {
        return gameActuallyStarted;
    }

    public void setGameActuallyStarted(boolean started) {
        this.gameActuallyStarted = started;
    }

    public void setFlashTournamentStartLocation(UUID uuid, Location location) {
        if (uuid == null) {
            return;
        }
        if (location == null) {
            flashTournamentStartLocations.remove(uuid);
            return;
        }
        flashTournamentStartLocations.put(uuid, location.clone());
    }

    public Location getFlashTournamentStartLocation(UUID uuid) {
        Location location = flashTournamentStartLocations.get(uuid);
        return location == null ? null : location.clone();
    }

    public void clearFlashTournamentStartLocations() {
        flashTournamentStartLocations.clear();
    }

    public long getFlashTournamentMovementUnlockMillis() {
        return flashTournamentMovementUnlockMillis;
    }

    public void setFlashTournamentMovementUnlockMillis(long unlockMillis) {
        this.flashTournamentMovementUnlockMillis = Math.max(0L, unlockMillis);
    }

    public boolean isFlashTournamentMovementLocked() {
        return flashTournamentMovementUnlockMillis > System.currentTimeMillis();
    }

    public boolean isAdminForceStartUsed() {
        return adminForceStartUsed;
    }

    public void setAdminForceStartUsed(boolean adminForceStartUsed) {
        this.adminForceStartUsed = adminForceStartUsed;
    }

    public boolean isWorldSelectionConfirmed() {
        return worldSelectionConfirmed;
    }

    public void setWorldSelectionConfirmed(boolean worldSelectionConfirmed) {
        this.worldSelectionConfirmed = worldSelectionConfirmed;
    }

    public boolean isPreyQuit() {
        return preyQuit;
    }

    public void setPreyQuit(boolean preyQuit) {
        this.preyQuit = preyQuit;
    }

    public int getPreyStartCountdownSeconds() {
        return preyStartCountdownSeconds;
    }

    public void setPreyStartCountdownSeconds(int seconds) {
        this.preyStartCountdownSeconds = seconds;
    }

    public UUID getActiveSwapPrey() {
        return activeSwapPrey;
    }

    public void setActiveSwapPrey(UUID activeSwapPrey) {
        this.activeSwapPrey = activeSwapPrey;
    }

    public UUID getCountdownSwapPrey() {
        return countdownSwapPrey;
    }

    public void setCountdownSwapPrey(UUID countdownSwapPrey) {
        this.countdownSwapPrey = countdownSwapPrey;
    }

    public int getSwapCountdownSeconds() {
        return swapCountdownSeconds;
    }

    public void setSwapCountdownSeconds(int swapCountdownSeconds) {
        this.swapCountdownSeconds = swapCountdownSeconds;
    }

    public boolean isSwapActivePrey(UUID uuid) {
        return uuid != null && uuid.equals(activeSwapPrey);
    }

    public boolean isSwapCountdownPrey(UUID uuid) {
        return uuid != null && uuid.equals(countdownSwapPrey);
    }

    public void setVictoryEffectTrigger(UUID uuid, Location location) {
        this.victoryEffectTriggerUuid = uuid;
        this.victoryEffectLocation = location == null ? null : location.clone();
    }

    public UUID getVictoryEffectTriggerUuid() {
        return victoryEffectTriggerUuid;
    }

    public Location getVictoryEffectLocation() {
        return victoryEffectLocation == null ? null : victoryEffectLocation.clone();
    }

    public void clearVictoryEffectTrigger() {
        this.victoryEffectTriggerUuid = null;
        this.victoryEffectLocation = null;
    }

    public void setPendingRespawnLocation(UUID uuid, org.bukkit.Location location) {
        pendingRespawnLocations.put(uuid, location);
    }

    public org.bukkit.Location getPendingRespawnLocation(UUID uuid) {
        return pendingRespawnLocations.remove(uuid);
    }

    public boolean canUsePreyRespawn(UUID uuid) {
        return !usedPreyRespawn.contains(uuid);
    }

    public void markPreyRespawnUsed(UUID uuid) {
        usedPreyRespawn.add(uuid);
    }

    // 修饰符检查
    public boolean hasModifier(String modifier) {
        return modifiers.contains(modifier);
    }

    // 广播消息给房间内所有玩家
    public void broadcast(String message) {
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.addAll(players);
        recipients.addAll(spectators);
        for (UUID uuid : recipients) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    // 本局统计数据方法
    public void addAttack(UUID uuid) {
        attackCount.merge(uuid, 1, Integer::sum);
    }

    public void addDamage(UUID uuid, double damage) {
        damageDealt.merge(uuid, damage, Double::sum);
    }

    public void updateDistance(UUID uuid, Location current) {
        Location last = lastLocation.get(uuid);
        if (last != null && last.getWorld() != null && current.getWorld() != null
                && last.getWorld().equals(current.getWorld())) {
            double dist = last.distance(current);
            if (dist < 10) { // 过滤传送
                distanceRun.merge(uuid, dist, Double::sum);
            }
        }
        lastLocation.put(uuid, current.clone());
    }

    public int getAttackCount(UUID uuid) {
        return attackCount.getOrDefault(uuid, 0);
    }

    public double getDamageDealt(UUID uuid) {
        return damageDealt.getOrDefault(uuid, 0.0);
    }

    public double getDistanceRun(UUID uuid) {
        return distanceRun.getOrDefault(uuid, 0.0);
    }

    // 计算贡献值（用于排行榜排序）
    public double getContribution(UUID uuid) {
        return getAttackCount(uuid) * 5.0 + getDamageDealt(uuid) * 2.0 + getDistanceRun(uuid) * 0.1;
    }

    // 世界切换次数
    public int getWorldRerollCount() {
        return worldRerollCount;
    }

    public Location getRewardChestLocation() {
        return rewardChestLocation;
    }

    public void setRewardChestLocation(Location rewardChestLocation) {
        this.rewardChestLocation = rewardChestLocation;
    }

    public boolean isRewardChestOpened() {
        return rewardChestOpened;
    }

    public void setRewardChestOpened(boolean rewardChestOpened) {
        this.rewardChestOpened = rewardChestOpened;
    }

    public int incrementRandomCompassUseCount(UUID uuid) {
        int nextCount = randomCompassUseCounts.getOrDefault(uuid, 0) + 1;
        randomCompassUseCounts.put(uuid, nextCount);
        return nextCount;
    }

    public void incrementWorldRerollCount() {
        worldRerollCount++;
    }

    public void resetWorldRerollCount() {
        worldRerollCount = 0;
    }

    private String luckyBlockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public void addLuckyPillarBlock(Location location) {
        String key = luckyBlockKey(location);
        if (!key.isEmpty()) {
            luckyPillarBlocks.add(key);
        }
    }

    public void addLuckyPillarsCageBlock(Location location) {
        String key = luckyBlockKey(location);
        if (!key.isEmpty()) {
            luckyPillarsCageBlocks.add(key);
        }
    }

    public boolean isLuckyPillarBlock(Location location) {
        return luckyPillarBlocks.contains(luckyBlockKey(location));
    }

    public void removeLuckyPillarBlock(Location location) {
        luckyPillarBlocks.remove(luckyBlockKey(location));
    }

    public int getLuckyPillarBlockCount() {
        return luckyPillarBlocks.size();
    }

    public List<Location> getLuckyPillarsCageBlockLocations() {
        List<Location> result = new ArrayList<>();
        for (String key : luckyPillarsCageBlocks) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String[] parts = key.split(":");
            if (parts.length != 4) {
                continue;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                continue;
            }
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                result.add(new Location(world, x, y, z));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void clearLuckyPillarBlocks() {
        luckyPillarBlocks.clear();
    }

    public void clearLuckyPillarsCageBlocks() {
        luckyPillarsCageBlocks.clear();
    }

    public void setLuckyPillarsArena(Location center, int eliminationY, double boundaryRadius) {
        this.luckyPillarsArenaCenter = center == null ? null : center.clone();
        this.luckyPillarsEliminationY = eliminationY;
        this.luckyPillarsBoundaryRadius = Math.max(8.0D, boundaryRadius);
    }

    public void setLuckyPillarsRuntimeSettings(String mapId, String mapName, String themeId,
                                               int gameTimeSeconds, int randomItemIntervalSeconds,
                                               int randomEventIntervalSeconds, Location spectatorSpawn) {
        this.luckyPillarsMapId = mapId == null || mapId.isBlank() ? "default" : mapId;
        this.luckyPillarsMapName = normalizeLuckyPillarsMapName(mapName);
        this.luckyPillarsThemeId = themeId == null || themeId.isBlank() ? "WOOL" : themeId.toUpperCase(Locale.ROOT);
        this.luckyPillarsGameTimeSeconds = Math.max(60, gameTimeSeconds);
        this.luckyPillarsRandomItemIntervalSeconds = Math.max(2, randomItemIntervalSeconds);
        this.luckyPillarsRandomEventIntervalSeconds = Math.max(8, randomEventIntervalSeconds);
        this.luckyPillarsSpectatorSpawn = spectatorSpawn == null ? null : spectatorSpawn.clone();
    }

    public Location getLuckyPillarsArenaCenter() {
        return luckyPillarsArenaCenter == null ? null : luckyPillarsArenaCenter.clone();
    }

    public int getLuckyPillarsEliminationY() {
        return luckyPillarsEliminationY;
    }

    public double getLuckyPillarsBoundaryRadius() {
        return luckyPillarsBoundaryRadius;
    }

    public Location getLuckyPillarsSpectatorSpawn() {
        return luckyPillarsSpectatorSpawn == null ? null : luckyPillarsSpectatorSpawn.clone();
    }

    public String getLuckyPillarsMapId() {
        return luckyPillarsMapId;
    }

    public String getLuckyPillarsMapName() {
        return normalizeLuckyPillarsMapName(luckyPillarsMapName);
    }

    public String getLuckyPillarsSizeShortName() {
        String mapName = normalizeLuckyPillarsMapName(luckyPillarsMapName);
        if (mapName.contains("小")) {
            return "小";
        }
        if (mapName.contains("大")) {
            return "大";
        }
        return "中";
    }

    public String getLuckyPillarsAdvertiseModeName() {
        return gameMode != null && gameMode.isLuckyPillars()
                ? gameMode.getDisplayName() + "-" + getLuckyPillarsMapName()
                : getModeName();
    }

    public String getLuckyPillarsThemeId() {
        return luckyPillarsThemeId;
    }

    public int getLuckyPillarsGameTimeSeconds() {
        return luckyPillarsGameTimeSeconds;
    }

    public int getLuckyPillarsRandomItemIntervalSeconds() {
        return luckyPillarsRandomItemIntervalSeconds;
    }

    public int getLuckyPillarsRandomEventIntervalSeconds() {
        return luckyPillarsRandomEventIntervalSeconds;
    }

    public void markLuckyPillarsEliminated(UUID uuid) {
        if (uuid != null && players.contains(uuid)) {
            luckyPillarsEliminatedPlayers.add(uuid);
        }
    }

    public boolean isLuckyPillarsEliminated(UUID uuid) {
        return uuid != null && luckyPillarsEliminatedPlayers.contains(uuid);
    }

    public Set<UUID> getLuckyPillarsEliminatedPlayers() {
        return Collections.unmodifiableSet(luckyPillarsEliminatedPlayers);
    }

    public void addLuckyPillarsKill(UUID uuid, boolean summonKill) {
        if (uuid == null || !players.contains(uuid)) {
            return;
        }
        luckyPillarsKillCounts.put(uuid, luckyPillarsKillCounts.getOrDefault(uuid, 0) + 1);
        if (summonKill) {
            luckyPillarsSummonKillCounts.put(uuid, luckyPillarsSummonKillCounts.getOrDefault(uuid, 0) + 1);
        }
    }

    public int getLuckyPillarsKills(UUID uuid) {
        return luckyPillarsKillCounts.getOrDefault(uuid, 0);
    }

    public int getLuckyPillarsSummonKills(UUID uuid) {
        return luckyPillarsSummonKillCounts.getOrDefault(uuid, 0);
    }

    public List<UUID> getLuckyPillarsAlivePlayers() {
        List<UUID> alive = new ArrayList<>();
        for (UUID uuid : players) {
            if (!luckyPillarsEliminatedPlayers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    alive.add(uuid);
                }
            }
        }
        return alive;
    }

    public void setMiniGameArena(String mapId, String mapName, Location center, Location spectatorSpawn,
                                 int eliminationY, double boundaryRadius, int maxGameTimeSeconds) {
        this.miniGameMapId = mapId == null || mapId.isBlank() ? "default" : mapId;
        this.miniGameMapName = mapName == null || mapName.isBlank() ? "默认地图" : mapName;
        this.miniGameArenaCenter = center == null ? null : center.clone();
        this.miniGameSpectatorSpawn = spectatorSpawn == null ? null : spectatorSpawn.clone();
        this.miniGameEliminationY = eliminationY;
        this.miniGameBoundaryRadius = Math.max(4.0D, boundaryRadius);
        this.miniGameMaxGameTimeSeconds = Math.max(30, maxGameTimeSeconds);
    }

    public String getMiniGameMapId() {
        return miniGameMapId;
    }

    public String getMiniGameMapName() {
        return miniGameMapName;
    }

    public Location getMiniGameArenaCenter() {
        return miniGameArenaCenter == null ? null : miniGameArenaCenter.clone();
    }

    public Location getMiniGameSpectatorSpawn() {
        return miniGameSpectatorSpawn == null ? null : miniGameSpectatorSpawn.clone();
    }

    public int getMiniGameEliminationY() {
        return miniGameEliminationY;
    }

    public double getMiniGameBoundaryRadius() {
        return miniGameBoundaryRadius;
    }

    public int getMiniGameMaxGameTimeSeconds() {
        return miniGameMaxGameTimeSeconds;
    }

    public int getMiniGameRound() {
        return miniGameRound;
    }

    public void setMiniGameRound(int round) {
        this.miniGameRound = Math.max(0, round);
    }

    public void markMiniGameEliminated(UUID uuid) {
        if (uuid != null && players.contains(uuid)) {
            miniGameEliminatedPlayers.add(uuid);
        }
    }

    public boolean isMiniGameEliminated(UUID uuid) {
        return uuid != null && miniGameEliminatedPlayers.contains(uuid);
    }

    public Set<UUID> getMiniGameEliminatedPlayers() {
        return Collections.unmodifiableSet(miniGameEliminatedPlayers);
    }

    public List<UUID> getMiniGameAlivePlayers() {
        List<UUID> alive = new ArrayList<>();
        for (UUID uuid : players) {
            if (!miniGameEliminatedPlayers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    alive.add(uuid);
                }
            }
        }
        return alive;
    }

    public void setMiniGameSurvivalTicks(UUID uuid, long ticks) {
        if (uuid != null && players.contains(uuid)) {
            miniGameSurvivalTicks.put(uuid, Math.max(0L, ticks));
        }
    }

    public long getMiniGameSurvivalTicks(UUID uuid) {
        return miniGameSurvivalTicks.getOrDefault(uuid, 0L);
    }

    public void addMiniGameProtectedBlock(Location location) {
        String key = luckyBlockKey(location);
        if (!key.isEmpty()) {
            miniGameProtectedBlocks.add(key);
        }
    }

    public boolean isMiniGameProtectedBlock(Location location) {
        return miniGameProtectedBlocks.contains(luckyBlockKey(location));
    }

    public void clearMiniGameState() {
        miniGameEliminatedPlayers.clear();
        miniGameSurvivalTicks.clear();
        miniGameProtectedBlocks.clear();
        miniGameMapId = "default";
        miniGameMapName = "默认地图";
        miniGameArenaCenter = null;
        miniGameSpectatorSpawn = null;
        miniGameEliminationY = 0;
        miniGameBoundaryRadius = 0.0D;
        miniGameMaxGameTimeSeconds = 480;
        miniGameRound = 0;
    }

    public void setBrickGuardRuntimeSettings(String mapId, String mapName, Location brickSpawn,
                                             Location netherBrickSpawn, Location coreLocation,
                                             Location fakeBorderCenter, double fakeBorderRadius) {
        this.brickGuardMapId = mapId == null || mapId.isBlank() ? "default" : mapId;
        this.brickGuardMapName = mapName == null || mapName.isBlank() ? "默认地图" : mapName;
        this.brickGuardBrickSpawn = brickSpawn == null ? null : brickSpawn.clone();
        this.brickGuardNetherBrickSpawn = netherBrickSpawn == null ? null : netherBrickSpawn.clone();
        this.brickGuardCoreLocation = coreLocation == null ? null : coreLocation.clone();
        this.brickGuardFakeBorderCenter = fakeBorderCenter == null ? null : fakeBorderCenter.clone();
        this.brickGuardFakeBorderRadius = Math.max(8.0D, fakeBorderRadius);
    }

    public String getBrickGuardMapId() {
        return brickGuardMapId;
    }

    public String getBrickGuardMapName() {
        return brickGuardMapName;
    }

    public Location getBrickGuardBrickSpawn() {
        return brickGuardBrickSpawn == null ? null : brickGuardBrickSpawn.clone();
    }

    public Location getBrickGuardNetherBrickSpawn() {
        return brickGuardNetherBrickSpawn == null ? null : brickGuardNetherBrickSpawn.clone();
    }

    public Location getBrickGuardCoreLocation() {
        return brickGuardCoreLocation == null ? null : brickGuardCoreLocation.clone();
    }

    public Location getBrickGuardFakeBorderCenter() {
        return brickGuardFakeBorderCenter == null ? null : brickGuardFakeBorderCenter.clone();
    }

    public double getBrickGuardFakeBorderRadius() {
        return brickGuardFakeBorderRadius;
    }

    public void clearBrickGuardState() {
        brickGuardMapId = "default";
        brickGuardMapName = "默认地图";
        brickGuardBrickSpawn = null;
        brickGuardNetherBrickSpawn = null;
        brickGuardCoreLocation = null;
        brickGuardFakeBorderCenter = null;
        brickGuardFakeBorderRadius = 1500.0D;
    }

    public void clearLuckyPillarsState() {
        luckyPillarBlocks.clear();
        luckyPillarsCageBlocks.clear();
        luckyPillarsEliminatedPlayers.clear();
        luckyPillarsKillCounts.clear();
        luckyPillarsSummonKillCounts.clear();
        luckyPillarsArenaCenter = null;
        luckyPillarsSpectatorSpawn = null;
        luckyPillarsEliminationY = 0;
        luckyPillarsBoundaryRadius = 0.0D;
        luckyPillarsMapId = "default";
        luckyPillarsMapName = resolveLuckyPillarsMapDisplayName();
        luckyPillarsThemeId = "WOOL";
        luckyPillarsGameTimeSeconds = 480;
        luckyPillarsRandomItemIntervalSeconds = 5;
        luckyPillarsRandomEventIntervalSeconds = 30;
    }

    private String normalizeLuckyPillarsMapName(String mapName) {
        if (mapName == null || mapName.isBlank()) {
            return resolveLuckyPillarsMapDisplayName();
        }
        if (mapName.contains("小型地图") || mapName.contains("中型地图") || mapName.contains("大型地图")) {
            return mapName;
        }
        if (mapName.contains("默认地图")) {
            return resolveLuckyPillarsMapDisplayName();
        }
        return mapName;
    }

    private String resolveLuckyPillarsMapDisplayName() {
        int targetPlayers = maxPlayers <= 0 ? 16 : maxPlayers;
        if (targetPlayers <= 8) {
            return "小型地图";
        }
        if (targetPlayers <= 16) {
            return "中型地图";
        }
        return "大型地图";
    }
}
