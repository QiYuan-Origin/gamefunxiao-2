package org.gamefunxiao.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.server.ChildRoomRegistryEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"deprecation"})
public class RoomManager {

    private boolean isDisabledMode(GameMode mode) {
        return mode != null && mode.isLegacyRemovedMode();
    }


    public enum InviteResult {
        SUCCESS,
        INVITER_NOT_IN_ROOM,
        NOT_OWNER,
        ROOM_PUBLIC,
        ROOM_UNAVAILABLE,
        TARGET_SELF,
        TARGET_ALREADY_IN_ROOM,
        TARGET_IN_OTHER_ROOM,
        TARGET_ALREADY_INVITED
    }

    private final GameFunXiao plugin;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerRooms = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> playerInventories = new ConcurrentHashMap<>();
    private final Map<UUID, String> disconnectedHunters = new ConcurrentHashMap<>(); // 存储断线的猎人
    private final Map<UUID, HunterReconnectSnapshot> disconnectedHunterSnapshots = new ConcurrentHashMap<>(); // 猎人断线时的局内状态
    private final Map<UUID, EndingChatIsolation> endingChatIsolations = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private boolean childBackendFallbackWarned = false;

    private static class EndingChatIsolation {
        private final String modeName;
        private final Set<UUID> receivers;
        private final long expiresAt;

        private EndingChatIsolation(String modeName, Set<UUID> receivers, long expiresAt) {
            this.modeName = modeName;
            this.receivers = receivers;
            this.expiresAt = expiresAt;
        }
    }

    public RoomManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    private String getModeMessageWithPrefix(GameMode mode, String path) {
        return mode != null && mode.isLuckyPillars()
                ? plugin.getMessageManager().getLuckyPillarsMessageWithPrefix(path)
                : mode != null && (mode.isStandaloneMiniGame() || mode.isIndependentMode())
                ? plugin.getMessageManager().getMiniGameMessageWithPrefix(path)
                : plugin.getMessageManager().getHunterGameMessageWithPrefix(path);
    }

    private String getModeMessageWithPrefix(GameMode mode, String path, Map<String, String> placeholders) {
        return mode != null && mode.isLuckyPillars()
                ? plugin.getMessageManager().getLuckyPillarsMessageWithPrefix(path, placeholders)
                : mode != null && (mode.isStandaloneMiniGame() || mode.isIndependentMode())
                ? plugin.getMessageManager().getMiniGameMessageWithPrefix(path, placeholders)
                : plugin.getMessageManager().getHunterGameMessageWithPrefix(path, placeholders);
    }

    private String getRoomMessageWithPrefix(GameRoom room, String path) {
        return getModeMessageWithPrefix(room == null ? null : room.getGameMode(), path);
    }

    private String getRoomMessageWithPrefix(GameRoom room, String path, Map<String, String> placeholders) {
        return getModeMessageWithPrefix(room == null ? null : room.getGameMode(), path, placeholders);
    }

    public GameRoom createRoom(Player player, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        return createRoom(player, mode, maxPlayers, isPublic, modifiers, null, true);
    }

    public void createConfiguredRoom(Player player, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        if (isDisabledMode(mode)) {
            player.sendMessage(getModeMessageWithPrefix(mode, "room.invalid_mode"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
            return;
        }
        if (mode.isFlashLike()) {
            quickMatch(player, mode.getId());
            return;
        }
        if (shouldUseCrossServerBackend()
                && plugin.getChildServerManager().createCrossServerRoom(player, mode, maxPlayers, isPublic, modifiers)) {
            return;
        }
        if (shouldUseChildServerBackend()
                && plugin.getChildServerManager().createLobbyRoom(player, mode, maxPlayers, isPublic, modifiers)) {
            return;
        }
        createRoom(player, mode, maxPlayers, isPublic, modifiers);
    }

    public GameRoom createRoom(Player player, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers,
                               String forcedRoomId, boolean persistSession) {
        if (mode.isFlashLike()) {
            maxPlayers = mode.isFlashTournament() ? 67 : 64;
            isPublic = true;
            modifiers = new HashSet<>();
        }
        String roomId = forcedRoomId != null ? forcedRoomId : generateRoomId();
        boolean isCustom = mode == GameMode.CUSTOM || !modifiers.isEmpty();

        // 先保存玩家当前位置、血量、饱食度和经验
        Location previousLocation = player.getLocation().clone();
        double previousHealth = player.getHealth();
        int previousFoodLevel = player.getFoodLevel();
        int previousExpLevel = player.getLevel();
        float previousExp = player.getExp();
        Collection<org.bukkit.potion.PotionEffect> previousPotionEffects = new ArrayList<>(player.getActivePotionEffects());
        if (persistSession) {
            savePlayerInventoryWithSession(player, previousLocation);
        } else {
            savePlayerInventory(player);
        }

        org.gamefunxiao.world.MiniGameMapManager.MapDefinition waitingMap = null;
        GameRoom room = new GameRoom(roomId, player.getUniqueId(), mode, maxPlayers, isPublic, modifiers, isCustom);
        if (mode.isMiniGameMapEditableMode() && plugin.getMiniGameMapManager() != null) {
            waitingMap = plugin.getMiniGameMapManager().findUsableMap(mode, Math.max(2, maxPlayers <= 0 ? 2 : maxPlayers));
            if (waitingMap != null) {
                if (mode.isLuckyPillars()) {
                    room.setLuckyPillarsRuntimeSettings(
                            waitingMap.mapId(),
                            waitingMap.displayName(),
                            waitingMap.themeId(),
                            waitingMap.gameTimeSeconds(),
                            waitingMap.randomItemIntervalSeconds(),
                            waitingMap.randomEventIntervalSeconds(),
                            null
                    );
                }
            }
        }
        // 设置房主的之前位置、血量、饱食度和经验
        room.setPreviousLocation(player.getUniqueId(), previousLocation);
        room.setPreviousHealth(player.getUniqueId(), previousHealth);
        room.setPreviousFoodLevel(player.getUniqueId(), previousFoodLevel);
        room.setPreviousExpLevel(player.getUniqueId(), previousExpLevel);
        room.setPreviousExp(player.getUniqueId(), previousExp);
        room.setPreviousPotionEffects(player.getUniqueId(), previousPotionEffects);
        room.setPreviousAdvancements(player.getUniqueId(), snapshotPlayerAdvancements(player));
        room.setPreviousRecipes(player.getUniqueId(), snapshotPlayerRecipes(player));
        resetPlayerForRoomSession(player);
        clearRoleNameTag(player);

        rooms.put(roomId, room);
        playerRooms.put(player.getUniqueId(), roomId);

        if (mode.isMiniGameMapEditableMode() && room.getGameWorld() == null) {
            World preCreatedWorld = plugin.getWorldManager().createLuckyPillarsWorld(roomId, mode, waitingMap);
            if (preCreatedWorld != null) {
                room.setGameWorld(preCreatedWorld);
                if (mode.isLuckyPillars()) {
                    plugin.getGameManager().prepareLuckyPillarsArenaBeforeStart(room);
                }
            }
        }

        // 传送玩家到等待大厅
        teleportToLobby(player, room);

        // 设置TAB前缀（房主创建时也需要）
        updatePlayerTabName(player, roomId);
        refreshPlayerVisibility();

        // 创建记分板
        plugin.getScoreboardManager().createScoreboard(player);
        clearRoleNameTag(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("room_id", roomId);
        placeholders.put("mode", mode.getDisplayName());
        player.sendMessage(getModeMessageWithPrefix(mode, "room.created", placeholders));
        if (!isPublic) {
            player.sendMessage(getModeMessageWithPrefix(mode, "room.invite_command_hint"));
        }

        plugin.getChildServerManager().syncRoom(room);
        return room;
    }

    public void quickMatch(Player player, String modeId) {
        GameMode mode = GameMode.fromId(modeId);
        if (isDisabledMode(mode)) {
            player.sendMessage(getModeMessageWithPrefix(mode, "room.invalid_mode"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
            return;
        }
        if (shouldUseCrossServerBackend()) {
            quickMatchChildServer(player, modeId);
            return;
        }
        if (shouldUseChildServerBackend()) {
            quickMatchChildServer(player, modeId);
            return;
        }

        quickMatchCurrentServer(player, modeId);
    }

    private void quickMatchCurrentServer(Player player, String modeId) {
        GameMode mode = GameMode.fromId(modeId);
        // 检查玩家是否已在房间中
        if (playerRooms.containsKey(player.getUniqueId())) {
            player.sendMessage(getModeMessageWithPrefix(mode, "room.already_in_room"));
            return;
        }

        if (mode.isFlashLike()) {
            if (handleFlashQuickMatchCurrentServer(player, mode)) {
                return;
            }
            createRoom(player, mode, mode.isFlashTournament() ? 67 : 64, true, new HashSet<>());
            return;
        }

        // 查找等待中或倒计时中的同模式房间
        for (GameRoom room : rooms.values()) {
            if ((room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) &&
                room.getGameMode() == mode &&
                room.isPublic() &&
                !room.isCustomRoom() &&
                (room.getMaxPlayers() == -1 || room.getPlayerCount() < room.getMaxPlayers())) {

                joinRoom(player, room);
                return;
            }
        }

        // 没有找到合适的房间，创建新房间
        createRoom(player, mode, 16, true, new HashSet<>());
    }

    private void quickMatchChildServer(Player player, String modeId) {
        GameMode mode = GameMode.fromId(modeId);
        if (playerRooms.containsKey(player.getUniqueId())) {
            player.sendMessage(getModeMessageWithPrefix(mode, "room.already_in_room"));
            return;
        }

        List<ChildRoomRegistryEntry> entries = new ArrayList<>(plugin.getChildServerManager().getLobbyRoomEntries());
        entries.sort(Comparator.comparing((ChildRoomRegistryEntry entry) -> entry.getState() != RoomState.WAITING)
                .thenComparingLong(ChildRoomRegistryEntry::getCreatedAt));

        if (mode.isFlashLike()) {
            if (handleFlashQuickMatchRemote(player, entries, mode)) {
                return;
            }
            if (shouldUseCrossServerBackend()) {
                plugin.getChildServerManager().createCrossServerRoom(player, mode, mode.isFlashTournament() ? 67 : 64, true, new HashSet<>());
            } else {
                plugin.getChildServerManager().createLobbyRoom(player, mode, mode.isFlashTournament() ? 67 : 64, true, new HashSet<>());
            }
            return;
        }

        for (ChildRoomRegistryEntry entry : entries) {
            boolean joinableState = entry.getState() == RoomState.WAITING || entry.getState() == RoomState.STARTING;
            boolean sameMode = entry.getMode() == mode;
            boolean publicRoom = entry.isPublic();
            boolean defaultRoom = entry.getMode() != GameMode.CUSTOM && entry.getModifiers().isEmpty();
            boolean availableSlot = entry.getMaxPlayers() == -1 || entry.getCurrentPlayers() < entry.getMaxPlayers();

            if (joinableState && sameMode && publicRoom && defaultRoom && availableSlot) {
                joinRemoteRoom(player, entry.getRoomId());
                return;
            }
        }

        if (shouldUseCrossServerBackend()) {
            plugin.getChildServerManager().createCrossServerRoom(player, mode, 16, true, new HashSet<>());
        } else {
            plugin.getChildServerManager().createLobbyRoom(player, mode, 16, true, new HashSet<>());
        }
    }

    public boolean joinRoomById(Player player, String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            if (isDisabledMode(room.getGameMode())) {
                player.sendMessage(getRoomMessageWithPrefix(room, "room.invalid_mode"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
                return false;
            }
            if ((room.getGameMode().isFlashLike() || room.getGameMode().isLuckyPillars() || room.getGameMode().isStandaloneMiniGame())
                    && (room.getState() == RoomState.PLAYING || room.getState() == RoomState.SELECTING)) {
                spectateRoom(player, room);
            } else {
                joinRoom(player, room);
            }
            return true;
        }

        if (shouldUseCrossServerBackend() || shouldUseChildServerBackend()) {
            for (ChildRoomRegistryEntry entry : plugin.getChildServerManager().getLobbyRoomEntries()) {
                if (entry.getRoomId().equals(roomId) && isDisabledMode(entry.getMode())) {
                    player.sendMessage(getModeMessageWithPrefix(entry.getMode(), "room.invalid_mode"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.85f, 1.0f);
                    return false;
                }
            }
            return joinRemoteRoom(player, roomId);
        }
        return false;
    }

    private boolean handleFlashQuickMatchCurrentServer(Player player, GameMode mode) {
        GameRoom flashRoom = null;
        for (GameRoom room : rooms.values()) {
            if (room.getGameMode() == mode && room.getState() != RoomState.ENDED) {
                if (flashRoom == null || room.getCreateTime() < flashRoom.getCreateTime()) {
                    flashRoom = room;
                }
            }
        }
        if (flashRoom == null) {
            return false;
        }

        if ((flashRoom.getState() == RoomState.WAITING || flashRoom.getState() == RoomState.STARTING)
                && (flashRoom.getMaxPlayers() == -1 || flashRoom.getPlayerCount() < flashRoom.getMaxPlayers())) {
            joinRoom(player, flashRoom);
            return true;
        }

        if (flashRoom.getState() == RoomState.PLAYING || flashRoom.getState() == RoomState.SELECTING) {
            spectateRoom(player, flashRoom);
            return true;
        }

        player.sendMessage(getModeMessageWithPrefix(mode, "room.join_failed"));
        return true;
    }

    private boolean handleFlashQuickMatchRemote(Player player, List<ChildRoomRegistryEntry> entries, GameMode mode) {
        ChildRoomRegistryEntry flashEntry = null;
        for (ChildRoomRegistryEntry entry : entries) {
            if (entry.getMode() == mode && entry.getState() != RoomState.ENDED) {
                flashEntry = entry;
                break;
            }
        }
        if (flashEntry == null) {
            return false;
        }

        boolean joinableState = flashEntry.getState() == RoomState.WAITING || flashEntry.getState() == RoomState.STARTING;
        boolean availableSlot = flashEntry.getMaxPlayers() == -1 || flashEntry.getCurrentPlayers() < flashEntry.getMaxPlayers();
        if (joinableState && availableSlot) {
            joinRemoteRoom(player, flashEntry.getRoomId());
            return true;
        }

        if (flashEntry.getState() == RoomState.PLAYING || flashEntry.getState() == RoomState.SELECTING) {
            return plugin.getChildServerManager().spectateCrossServerRoom(player, flashEntry.getRoomId());
        }

        player.sendMessage(getModeMessageWithPrefix(mode, "room.join_failed"));
        return true;
    }

    public Collection<ChildRoomRegistryEntry> getVisibleChildRoomEntries() {
        if (!shouldUseCrossServerBackend() && !shouldUseChildServerBackend()) {
            return Collections.emptyList();
        }
        return plugin.getChildServerManager().getLobbyRoomEntries();
    }

    public boolean isChildRoomBackendActive() {
        return shouldUseCrossServerBackend() || shouldUseChildServerBackend();
    }

    private boolean joinRemoteRoom(Player player, String roomId) {
        if (shouldUseCrossServerBackend()) {
            return plugin.getChildServerManager().joinCrossServerRoom(player, roomId);
        }
        return plugin.getChildServerManager().joinLobbyRoom(player, roomId);
    }

    public void joinRoom(Player player, GameRoom room) {
        joinRoom(player, room, true);
    }

    public void joinRoom(Player player, GameRoom room, boolean persistSession) {
        if (playerRooms.containsKey(player.getUniqueId())) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.already_in_room"));
            return;
        }

        if (room.getState() != RoomState.WAITING && room.getState() != RoomState.STARTING) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.game_started"));
            return;
        }

        if (room.getMaxPlayers() != -1 && room.getPlayerCount() >= room.getMaxPlayers()) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.join_failed"));
            return;
        }

        if (!room.isPublic()
            && !player.getUniqueId().equals(room.getOwnerUuid())
            && !room.isInvited(player.getUniqueId())) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.invite_only"));
            return;
        }

        if (!room.addPlayer(player.getUniqueId())) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.join_failed"));
            return;
        }

        // 保存玩家当前位置、血量、饱食度、经验和buff状态
        Location previousLocation = player.getLocation().clone();
        double previousHealth = player.getHealth();
        int previousFoodLevel = player.getFoodLevel();
        int previousExpLevel = player.getLevel();
        float previousExp = player.getExp();
        Collection<org.bukkit.potion.PotionEffect> previousPotionEffects = new ArrayList<>(player.getActivePotionEffects());

        room.setPreviousLocation(player.getUniqueId(), previousLocation);
        room.setPreviousHealth(player.getUniqueId(), previousHealth);
        room.setPreviousFoodLevel(player.getUniqueId(), previousFoodLevel);
        room.setPreviousExpLevel(player.getUniqueId(), previousExpLevel);
        room.setPreviousExp(player.getUniqueId(), previousExp);
        room.setPreviousPotionEffects(player.getUniqueId(), previousPotionEffects);
        room.setPreviousAdvancements(player.getUniqueId(), snapshotPlayerAdvancements(player));
        room.setPreviousRecipes(player.getUniqueId(), snapshotPlayerRecipes(player));
        if (persistSession) {
            savePlayerInventoryWithSession(player, previousLocation);
        } else {
            savePlayerInventory(player);
        }
        resetPlayerForRoomSession(player);
        clearRoleNameTag(player);

        playerRooms.put(player.getUniqueId(), room.getRoomId());
        teleportToLobby(player, room);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("current", String.valueOf(room.getPlayerCount()));
        placeholders.put("max", room.getMaxPlayers() == -1 ? "∞" : String.valueOf(room.getMaxPlayers()));

        room.broadcast(getRoomMessageWithPrefix(room, "room.player_joined", placeholders));

        // 设置TAB列表显示名称
        updatePlayerTabName(player, room.getRoomId());
        refreshPlayerVisibility();

        // 创建记分板
        plugin.getScoreboardManager().createScoreboard(player);
        clearRoleNameTag(player);

        // 刷新大厅物品（人数可能达到双猎物投票条件）
        plugin.getGameManager().refreshLobbyItems(room);

        // 检查是否可以开始倒计时
        checkStartCondition(room);
        plugin.getChildServerManager().syncRoom(room);
    }

    public InviteResult invitePlayerToRoom(Player inviter, Player target) {
        GameRoom room = getPlayerRoom(inviter.getUniqueId());
        if (room == null) {
            return InviteResult.INVITER_NOT_IN_ROOM;
        }

        if (!room.getOwnerUuid().equals(inviter.getUniqueId())) {
            return InviteResult.NOT_OWNER;
        }

        if (room.isPublic()) {
            return InviteResult.ROOM_PUBLIC;
        }

        if (room.getState() != RoomState.WAITING && room.getState() != RoomState.STARTING) {
            return InviteResult.ROOM_UNAVAILABLE;
        }

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            return InviteResult.TARGET_SELF;
        }

        if (room.getAllPlayerUUIDs().contains(target.getUniqueId())) {
            return InviteResult.TARGET_ALREADY_IN_ROOM;
        }

        if (playerRooms.containsKey(target.getUniqueId())) {
            return InviteResult.TARGET_IN_OTHER_ROOM;
        }

        if (room.isInvited(target.getUniqueId())) {
            return InviteResult.TARGET_ALREADY_INVITED;
        }

        room.invitePlayer(target.getUniqueId());
        sendInviteMessage(inviter, target, room);
        return InviteResult.SUCCESS;
    }

    private void sendInviteMessage(Player inviter, Player target, GameRoom room) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("owner", inviter.getName());
        placeholders.put("player", target.getName());
        placeholders.put("room_id", room.getRoomId());
        placeholders.put("mode", room.getModeName());
        placeholders.put("current", String.valueOf(room.getPlayerCount()));
        placeholders.put("max", room.getMaxPlayers() == -1 ? "∞" : String.valueOf(room.getMaxPlayers()));

        String messageText = getRoomMessageWithPrefix(room, "room.invited_click_join", placeholders);
        String hoverText = plugin.getMessageManager().getMessage("room.invited_hover", placeholders);

        Component message = LegacyComponentSerializer.legacySection()
            .deserialize(messageText)
            .clickEvent(ClickEvent.runCommand("/gamefunxiao hg join " + room.getRoomId()))
            .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(hoverText)));

        target.sendMessage(message);
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.45f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.playSound(target.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.55f, 1.8f);
            }
        }, 4L);
    }

    public void handlePlayerDisconnect(Player player) {
        if (player == null) {
            return;
        }

        String roomId = playerRooms.get(player.getUniqueId());
        if (roomId == null) {
            return;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            playerRooms.remove(player.getUniqueId());
            return;
        }

        UUID playerUUID = player.getUniqueId();
        RoomState currentState = room.getState();
        boolean wasPrey = room.isPrey(playerUUID);
        boolean wasSpectator = room.isSpectator(playerUUID);

        if (shouldKeepHunterReconnectSnapshot(room, currentState, wasPrey, wasSpectator)) {
            captureHunterReconnectSnapshot(player, room);
            handleHunterDisconnect(playerUUID, roomId);
            refreshPlayerVisibility();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            room.broadcast(getRoomMessageWithPrefix(room, "room.player_left", placeholders));

            if (!hasOnlineActiveHunter(room, playerUUID)) {
                boolean isOneVsOne = room.getPlayerCount() == 2 && room.getPreyUUIDs().size() == 1;
                boolean oneVsOnePenalty = plugin.getConfigManager().getConfig().getBoolean("hunter_game.one_vs_one_quit_penalty", true);
                if (isOneVsOne && oneVsOnePenalty) {
                    plugin.getGameManager().endGameWithoutReward(room);
                } else {
                    plugin.getGameManager().endGame(room, true);
                }
            }
            return;
        }

        leaveRoom(player);
    }

    private boolean hasOnlineActiveHunter(GameRoom room, UUID ignoredUuid) {
        if (room == null) {
            return false;
        }
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (uuid == null || uuid.equals(ignoredUuid) || room.isPrey(uuid) || disconnectedHunters.containsKey(uuid)) {
                continue;
            }
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter != null && hunter.isOnline()) {
                return true;
            }
        }
        return false;
    }

    public void leaveRoom(Player player) {
        String roomId = playerRooms.get(player.getUniqueId());
        if (roomId == null) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        UUID playerUUID = player.getUniqueId();
        RoomState currentState = room.getState();
        boolean wasPrey = room.isPrey(playerUUID);
        boolean wasSpectator = room.isSpectator(playerUUID);
        boolean keepHunterReconnect = shouldKeepHunterReconnectSnapshot(room, currentState, wasPrey, wasSpectator);
        if (keepHunterReconnect) {
            captureHunterReconnectSnapshot(player, room);
        }

        if (plugin.getChildServerManager().isManagedNodeRoom(roomId)) {
            handleManagedNodeLeave(player, room, currentState, wasPrey, wasSpectator);
            return;
        }

        if (plugin.getChildServerManager().isManagedCrossServerRoom(roomId)) {
            handleManagedCrossServerLeave(player, room, currentState, wasPrey, wasSpectator);
            return;
        }

        // 统一恢复玩家状态：背包、成就、配方、Buff、经验、血量、飞行/旁观/闪光临时属性都在这里清掉
        restorePlayerAfterRoom(room, player, true);

        room.removePlayer(playerUUID);
        room.removeSpectator(playerUUID);
        playerRooms.remove(playerUUID);
        refreshPlayerVisibility();

        // 根据玩家身份发送不同的退出消息
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        if (wasSpectator) {
            room.broadcast(getRoomMessageWithPrefix(room, "room.spectator_left", placeholders));
        } else {
            room.broadcast(getRoomMessageWithPrefix(room, "room.player_left", placeholders));
        }

        // 检查游戏状态并处理
        if (currentState == RoomState.STARTING) {
            // 倒计时中，如果人数不足2人，取消倒计时
            if (room.getPlayerCount() < getMinimumPlayersForMode(room.getGameMode())) {
                plugin.getGameManager().cancelCountdown(room);
                room.setState(RoomState.WAITING);
                room.broadcast(getRoomMessageWithPrefix(room, "game.countdown_cancelled"));
            }
        } else if (currentState == RoomState.PLAYING) {
            if (room.getGameMode().isLuckyPillars()) {
                if (!wasSpectator) {
                    plugin.getGameManager().checkLuckyPillarsWin(room);
                }
                return;
            }
            if (room.getGameMode().isStandaloneMiniGame()) {
                if (!wasSpectator) {
                    plugin.getGameManager().checkStandaloneMiniGameWin(room);
                }
                return;
            }
            if (room.getGameMode().isIndependentMode()) {
                if (!wasSpectator && room.getPlayerCount() <= 1) {
                    plugin.getGameManager().endGameWithoutReward(room);
                }
                return;
            }
            // 游戏进行中
            if (wasPrey) {
                // 猎物主动退出，标记为猎物退出
                room.setPreyQuit(true);
                // 猎人胜利
                plugin.getGameManager().endGame(room, false);
            } else if (!wasSpectator) {
                // 猎人主动退出，记录到 disconnectedHunters 以便 rejoin
                handleHunterDisconnect(playerUUID, roomId);

                // 检查是否还有猎人
                boolean hasHunter = false;
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    if (!room.isPrey(uuid)) {
                        hasHunter = true;
                        break;
                    }
                }
                if (!hasHunter) {
                    // 播放连续3次钟声音效（猎人全部退出）
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            // 第一声
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                            // 第二声（延迟10 ticks）
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (p.isOnline()) {
                                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                                }
                            }, 10L);
                            // 第三声（延迟20 ticks）
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (p.isOnline()) {
                                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                                }
                            }, 20L);
                        }
                    }

                    boolean isOneVsOne = room.getPlayerCount() + 1 == 2 && room.getPreyUUIDs().size() == 1;
                    boolean oneVsOnePenalty = plugin.getConfigManager().getConfig().getBoolean("hunter_game.one_vs_one_quit_penalty", true);
                    if (isOneVsOne && oneVsOnePenalty) {
                        plugin.getGameManager().endGameWithoutReward(room);
                    } else {
                        plugin.getGameManager().endGame(room, true);
                    }
                }
            }
        } else if (currentState == RoomState.SELECTING) {
            // 世界选择阶段，如果猎物退出，取消游戏
            if (wasPrey) {
                room.setState(RoomState.WAITING);
                room.broadcast(getRoomMessageWithPrefix(room, "game.cancelled"));
                // 传送所有玩家回大厅
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        teleportToLobby(p, room);
                    }
                }
            }
        }

        if (currentState == RoomState.WAITING || currentState == RoomState.STARTING) {
            plugin.getGameManager().refreshLobbyItems(room);
        }

        // 如果房间空了，删除房间
        if (room.getPlayerCount() == 0) {
            deleteRoom(roomId);
        }
    }

    private void handleManagedNodeLeave(Player player, GameRoom room, RoomState currentState,
                                        boolean wasPrey, boolean wasSpectator) {
        UUID playerUUID = player.getUniqueId();
        if (shouldKeepHunterReconnectSnapshot(room, currentState, wasPrey, wasSpectator)) {
            captureHunterReconnectSnapshot(player, room);
        }

        restorePlayerAfterRoom(room, player, false);
        room.removePlayer(playerUUID);
        room.removeSpectator(playerUUID);
        playerRooms.remove(playerUUID);
        refreshPlayerVisibility();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        if (wasSpectator) {
            room.broadcast(getRoomMessageWithPrefix(room, "room.spectator_left", placeholders));
        } else {
            room.broadcast(getRoomMessageWithPrefix(room, "room.player_left", placeholders));
        }

        if (currentState == RoomState.PLAYING) {
            if (room.getGameMode().isStandaloneMiniGame()) {
                if (!wasSpectator) {
                    plugin.getGameManager().checkStandaloneMiniGameWin(room);
                }
            } else if (room.getGameMode().isIndependentMode()) {
                if (!wasSpectator && room.getPlayerCount() <= 1) {
                    plugin.getGameManager().endGameWithoutReward(room);
                }
            } else if (wasPrey) {
                room.setPreyQuit(true);
                plugin.getGameManager().endGame(room, false);
            } else if (!wasSpectator) {
                handleHunterDisconnect(playerUUID, room.getRoomId());
                boolean hasHunter = false;
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    if (!room.isPrey(uuid)) {
                        hasHunter = true;
                        break;
                    }
                }
                if (!hasHunter) {
                    plugin.getGameManager().endGameWithoutReward(room);
                }
            }
        } else if (currentState == RoomState.STARTING) {
            if (room.getPlayerCount() < getMinimumPlayersForMode(room.getGameMode())) {
                plugin.getGameManager().cancelCountdown(room);
                room.setState(RoomState.WAITING);
                room.broadcast(getRoomMessageWithPrefix(room, "game.countdown_cancelled"));
            }
        } else if (currentState == RoomState.SELECTING && wasPrey) {
            room.setState(RoomState.WAITING);
            room.broadcast(getRoomMessageWithPrefix(room, "game.cancelled"));
        }

        plugin.getChildServerManager().returnManagedRoomPlayerToLobby(player);
        plugin.getChildServerManager().syncRoom(room);

        if (room.getPlayerCount() == 0) {
            deleteRoom(room.getRoomId());
            plugin.getChildServerManager().scheduleNodeShutdown();
        }
    }

    private void handleManagedCrossServerLeave(Player player, GameRoom room, RoomState currentState,
                                               boolean wasPrey, boolean wasSpectator) {
        UUID playerUUID = player.getUniqueId();
        if (shouldKeepHunterReconnectSnapshot(room, currentState, wasPrey, wasSpectator)) {
            captureHunterReconnectSnapshot(player, room);
        }

        restorePlayerAfterRoom(room, player, false);
        room.removePlayer(playerUUID);
        room.removeSpectator(playerUUID);
        playerRooms.remove(playerUUID);
        refreshPlayerVisibility();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        if (wasSpectator) {
            room.broadcast(getRoomMessageWithPrefix(room, "room.spectator_left", placeholders));
        } else {
            room.broadcast(getRoomMessageWithPrefix(room, "room.player_left", placeholders));
        }

        if (currentState == RoomState.PLAYING) {
            if (room.getGameMode().isStandaloneMiniGame()) {
                if (!wasSpectator) {
                    plugin.getGameManager().checkStandaloneMiniGameWin(room);
                }
            } else if (room.getGameMode().isIndependentMode()) {
                if (!wasSpectator && room.getPlayerCount() <= 1) {
                    plugin.getGameManager().endGameWithoutReward(room);
                }
            } else if (wasPrey) {
                room.setPreyQuit(true);
                plugin.getGameManager().endGame(room, false);
            } else if (!wasSpectator) {
                handleHunterDisconnect(playerUUID, room.getRoomId());
            }
        } else if (currentState == RoomState.STARTING) {
            if (room.getPlayerCount() < getMinimumPlayersForMode(room.getGameMode())) {
                plugin.getGameManager().cancelCountdown(room);
                room.setState(RoomState.WAITING);
                room.broadcast(getRoomMessageWithPrefix(room, "game.countdown_cancelled"));
            }
        }

        plugin.getChildServerManager().returnCrossServerRoomPlayerToLobby(player);
        plugin.getChildServerManager().syncRoom(room);

        if (room.getPlayerCount() == 0) {
            deleteRoom(room.getRoomId());
        }
    }

    public void spectateRoom(Player player, GameRoom room) {
        if (room.getState() != RoomState.PLAYING) {
            player.sendMessage(getRoomMessageWithPrefix(room, "room.not_playing"));
            return;
        }

        // 保存旁观者进入前状态，避免旁观结束后留下闪光/猎人游戏 Buff 和临时成就
        Location previousLocation = player.getLocation().clone();
        double previousHealth = player.getHealth();
        int previousFoodLevel = player.getFoodLevel();
        int previousExpLevel = player.getLevel();
        float previousExp = player.getExp();
        Collection<org.bukkit.potion.PotionEffect> previousPotionEffects = new ArrayList<>(player.getActivePotionEffects());
        room.setPreviousLocation(player.getUniqueId(), previousLocation);
        room.setPreviousHealth(player.getUniqueId(), previousHealth);
        room.setPreviousFoodLevel(player.getUniqueId(), previousFoodLevel);
        room.setPreviousExpLevel(player.getUniqueId(), previousExpLevel);
        room.setPreviousExp(player.getUniqueId(), previousExp);
        room.setPreviousPotionEffects(player.getUniqueId(), previousPotionEffects);

        // 保存玩家背包
        savePlayerInventory(player);
        room.setPreviousAdvancements(player.getUniqueId(), snapshotPlayerAdvancements(player));
        room.setPreviousRecipes(player.getUniqueId(), snapshotPlayerRecipes(player));
        resetPlayerRecipes(player);
        ensurePlayerRecipesAvailable(player);
        clearPlayerActivePotionEffects(player);

        room.addSpectator(player.getUniqueId());
        playerRooms.put(player.getUniqueId(), room.getRoomId());

        // 传送到游戏世界（猎物附近）
        if (room.getGameWorld() != null) {
            // 找到第一个猎物的位置
            org.bukkit.Location targetLoc = null;
            for (java.util.UUID preyUUID : room.getPreyUUIDs()) {
                org.bukkit.entity.Player prey = org.bukkit.Bukkit.getPlayer(preyUUID);
                if (prey != null && prey.isOnline()) {
                    // 传送到猎物附近（上方5格）
                    targetLoc = prey.getLocation().clone().add(0, 5, 0);
                    break;
                }
            }

            // 如果没有找到猎物，传送到出生点
            if (targetLoc == null) {
                targetLoc = room.getGameWorld().getSpawnLocation();
            }

            // 使用 mvtp 传送（会被世界默认模式覆盖）
            final org.bukkit.Location finalLoc = targetLoc;
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                    "mvtp " + player.getName() + " " + room.getGameWorld().getName());

            // 延迟设置旁观模式和给物品（等待 mvtp 完成）
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 传送到目标位置
                player.teleport(finalLoc);
                // 设置为旁观模式（覆盖世界默认模式）
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                // 给旁观退出物品
                plugin.getGameManager().giveSpectatorItems(player);
                // 播放传送音效
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                // 创建记分板
                plugin.getScoreboardManager().createScoreboard(player);
                plugin.getGameManager().syncEndDimensionBrightness(player, room);

                applyRoleNameTags(room);
                setSpectatorNameTag(player, room.getRoomId());
            }, 10L); // 延迟10 tick (0.5秒)
        }

        // 设置旁观者头顶名字前缀
        setSpectatorNameTag(player, room.getRoomId());

        // 通知房间内玩家（如果配置允许）
        if (plugin.getConfigManager().getConfig().getBoolean("hunter_game.chat.notify_spectator_join", true)) {
            if (!player.hasPermission("gamefunxiao.admin")) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                room.broadcast(getRoomMessageWithPrefix(room, "room.spectator_joined", placeholders));
            }
        }
    }

    private void teleportToLobby(Player player, GameRoom room) {
        if (room != null && room.getGameMode() != null
                && room.getGameMode().isMiniGameMapEditableMode()
                && plugin.getMiniGameMapManager() != null) {
            org.gamefunxiao.world.MiniGameMapManager.MapDefinition waitingMap =
                    plugin.getMiniGameMapManager().findUsableMap(room.getGameMode(),
                            Math.max(2, room.getMaxPlayers() <= 0 ? 2 : room.getMaxPlayers()));
            if (waitingMap != null) {
                if (room.getGameMode().isLuckyPillars()) {
                    room.setLuckyPillarsRuntimeSettings(
                            waitingMap.mapId(),
                            waitingMap.displayName(),
                            waitingMap.themeId(),
                            waitingMap.gameTimeSeconds(),
                            waitingMap.randomItemIntervalSeconds(),
                            waitingMap.randomEventIntervalSeconds(),
                            room.getLuckyPillarsSpectatorSpawn()
                    );
                }
            }
        }
        // 获取或创建该房间的大厅世界
        World lobbyWorld = plugin.getWorldManager().getLobbyWorld(room.getRoomId());
        if (lobbyWorld == null) {
            plugin.getLogger().info("为房间 " + room.getRoomId() + " 创建新的大厅世界...");
            lobbyWorld = plugin.getWorldManager().createLobbyWorld(room.getRoomId(), room.getGameMode());

            if (lobbyWorld == null) {
                plugin.getLogger().severe("创建大厅世界失败！房间ID: " + room.getRoomId());
                player.sendMessage(getRoomMessageWithPrefix(room, "room.lobby_creation_failed"));
                return;
            }
            plugin.getLogger().info("大厅世界创建成功: " + lobbyWorld.getName());
        } else {
            plugin.getLogger().info("使用已存在的大厅世界: " + lobbyWorld.getName());
        }

        // 传送玩家
        Location spawnLoc = lobbyWorld.getSpawnLocation();
        plugin.getLogger().info("传送玩家 " + player.getName() + " 到大厅世界 " + lobbyWorld.getName() +
                               " 坐标: " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());
        clearRoleNameTag(player);
        player.teleport(spawnLoc);
        clearRoleNameTag(player);
        updatePlayerTabName(player, room.getRoomId());

        // 回满血量和饱食度
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        // 给玩家等待大厅物品
        plugin.getGameManager().giveLobbyItems(player, room);
    }

    private void checkStartCondition(GameRoom room) {
        int minPlayers = getMinimumPlayersForMode(room.getGameMode());
        int waitCountdown = plugin.getConfigManager().getConfig().getInt("hunter_game.wait_countdown", 300);
        if (room.getPlayerCount() >= minPlayers && room.getState() == RoomState.WAITING) {
            room.setCountdown(waitCountdown);
            plugin.getGameManager().startCountdown(room);
        }
    }

    private String generateRoomId() {
        String roomId;
        int attempts = 0;
        do {
            // 生成100000~999999之间的随机数字
            int randomNum = 100000 + random.nextInt(900000);
            roomId = String.valueOf(randomNum);
            attempts++;

            // 防止无限循环（虽然概率极低）
            if (attempts > 100) {
                plugin.getLogger().warning("生成房间ID失败，尝试次数过多");
                roomId = String.valueOf(System.currentTimeMillis() % 1000000);
                break;
            }
        } while (rooms.containsKey(roomId));

        return roomId;
    }

    public String generateExternalRoomId() {
        return generateRoomId();
    }

    public void deleteRoom(String roomId) {
        GameRoom room = rooms.remove(roomId);
        if (room != null) {
            keepEndingChatIsolation(room, 120_000L);
            // 清除所有玩家的房间记录
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                playerRooms.remove(uuid);
                if (roomId.equals(disconnectedHunters.get(uuid))) {
                    disconnectedHunters.remove(uuid);
                    disconnectedHunterSnapshots.remove(uuid);
                }
            }
            // 清除旁观者的房间记录
            for (UUID uuid : room.getSpectators()) {
                playerRooms.remove(uuid);
            }

            // 世界卸载/删除比较重，游戏结束后不要和玩家恢复挤在同一 tick 里做，避免结束瞬间爆 ping
            plugin.getWorldManager().deleteRoomWorldsLater(roomId);
            plugin.getChildServerManager().onManagedRoomDeleted(roomId);
            Bukkit.getScheduler().runTaskLater(plugin, this::refreshPlayerVisibility, 2L);
        }
    }

    public void keepEndingChatIsolation(GameRoom room, long durationMillis) {
        if (room == null) {
            return;
        }
        if (room.getGameMode().isLuckyPillars()) {
            clearEndingChatIsolation(room);
            return;
        }
        Set<UUID> receivers = new HashSet<>();
        receivers.addAll(room.getAllPlayerUUIDs());
        receivers.addAll(room.getSpectators());
        if (receivers.isEmpty()) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + Math.max(5_000L, durationMillis);
        EndingChatIsolation isolation = new EndingChatIsolation(room.getModeName(), Collections.unmodifiableSet(new HashSet<>(receivers)), expiresAt);
        for (UUID uuid : receivers) {
            endingChatIsolations.put(uuid, isolation);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::cleanupExpiredEndingChatIsolations,
                Math.max(20L, durationMillis / 50L + 20L));
    }

    public void clearEndingChatIsolation(GameRoom room) {
        if (room == null) {
            return;
        }
        Set<UUID> receivers = new HashSet<>();
        receivers.addAll(room.getAllPlayerUUIDs());
        receivers.addAll(room.getSpectators());
        for (UUID uuid : receivers) {
            endingChatIsolations.remove(uuid);
        }
    }

    public boolean isChatIsolated(UUID uuid) {
        return getEndingChatIsolation(uuid) != null || playerRooms.containsKey(uuid);
    }

    public Set<UUID> getEndingChatReceivers(UUID uuid) {
        EndingChatIsolation isolation = getEndingChatIsolation(uuid);
        return isolation == null ? Collections.emptySet() : isolation.receivers;
    }

    public String getEndingChatModeName(UUID uuid) {
        EndingChatIsolation isolation = getEndingChatIsolation(uuid);
        return isolation == null ? null : isolation.modeName;
    }

    private EndingChatIsolation getEndingChatIsolation(UUID uuid) {
        EndingChatIsolation isolation = endingChatIsolations.get(uuid);
        if (isolation == null) {
            return null;
        }
        if (System.currentTimeMillis() > isolation.expiresAt) {
            endingChatIsolations.remove(uuid, isolation);
            return null;
        }
        return isolation;
    }

    private void cleanupExpiredEndingChatIsolations() {
        long now = System.currentTimeMillis();
        endingChatIsolations.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        refreshPlayerVisibility();
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public GameRoom getPlayerRoom(UUID uuid) {
        String roomId = playerRooms.get(uuid);
        if (roomId == null) {
            return null;
        }
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            playerRooms.remove(uuid);
            return null;
        }
        if (!room.getAllPlayerUUIDs().contains(uuid) && !room.isSpectator(uuid)) {
            playerRooms.remove(uuid);
            return null;
        }
        return room;
    }

    public Collection<GameRoom> getAllRooms() {
        return rooms.values();
    }

    public boolean isInRoom(UUID uuid) {
        return getPlayerRoom(uuid) != null;
    }

    private boolean shouldUseChildServerBackend() {
        if (plugin.getConfigManager().getHunterGameBackendMode() != HunterGameBackendMode.CHILD_SERVER) {
            return false;
        }

        if (plugin.getChildServerManager().isLobbyMode()) {
            return true;
        }

        if (!childBackendFallbackWarned) {
            childBackendFallbackWarned = true;
            plugin.getLogger().warning("hunter_game.room_backend 已设置为 child_server，但当前服不是子服大厅控制端，已自动回退为当前服务器创建世界模式。");
        }
        return false;
    }

    private boolean shouldUseCrossServerBackend() {
        return plugin.getConfigManager().getHunterGameBackendMode() == HunterGameBackendMode.CROSS_SERVER
                && plugin.getConfigManager().isCrossServerBackendEnabled();
    }

    public boolean canSeeInRoomTab(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return true;
        }
        String viewerRoom = playerRooms.get(viewer.getUniqueId());
        String targetRoom = playerRooms.get(target.getUniqueId());
        EndingChatIsolation viewerEnding = getEndingChatIsolation(viewer.getUniqueId());
        EndingChatIsolation targetEnding = getEndingChatIsolation(target.getUniqueId());
        if (viewerRoom == null && targetRoom == null) {
            if (viewerEnding == null && targetEnding == null) {
                return true;
            }
            return viewerEnding != null && viewerEnding == targetEnding;
        }
        if (viewerRoom == null || targetRoom == null) {
            return false;
        }
        return viewerRoom.equals(targetRoom);
    }

    public boolean isSameRoom(Player viewer, Player target) {
        return canSeeInRoomTab(viewer, target);
    }

    public void refreshPlayerVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) {
                    continue;
                }
                if (canSeeInRoomTab(viewer, target)) {
                    viewer.showPlayer(plugin, target);
                } else {
                    viewer.hidePlayer(plugin, target);
                }
            }
        }
    }

    private void savePlayerInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                copy[i] = contents[i].clone();
            }
        }
        playerInventories.put(player.getUniqueId(), copy);
        player.getInventory().clear();
    }

    private void savePlayerInventoryWithSession(Player player, Location previousLocation) {
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                copy[i] = contents[i].clone();
            }
        }
        playerInventories.put(player.getUniqueId(), copy);
        player.getInventory().clear();
        // 持久化到文件，防止强制关服丢失
        plugin.getPlayerDataManager().savePlayerSession(player, previousLocation, copy);
    }

    private boolean shouldKeepHunterReconnectSnapshot(GameRoom room, RoomState state, boolean wasPrey, boolean wasSpectator) {
        return room != null
                && state == RoomState.PLAYING
                && !wasPrey
                && !wasSpectator
                && room.getGameMode().usesHunterReconnectSnapshot();
    }

    private void captureHunterReconnectSnapshot(Player player, GameRoom room) {
        if (player == null || room == null) {
            return;
        }
        disconnectedHunterSnapshots.put(player.getUniqueId(),
                new HunterReconnectSnapshot(player, room.getRoomId(), playerInventories.get(player.getUniqueId())));
    }

    private void restoreHunterReconnectSnapshot(Player player, HunterReconnectSnapshot snapshot, GameRoom room) {
        if (player == null || snapshot == null || room == null) {
            return;
        }

        if (snapshot.previousInventoryContents != null && snapshot.previousInventoryContents.length > 0) {
            ItemStack[] previousCopy = HunterReconnectSnapshot.cloneItemArray(snapshot.previousInventoryContents);
            playerInventories.put(player.getUniqueId(), previousCopy);
            Location previousLocation = room.getPreviousLocation(player.getUniqueId());
            if (previousLocation != null) {
                plugin.getPlayerDataManager().savePlayerSession(player, previousLocation, previousCopy);
            }
        }

        applyHunterReconnectSnapshot(player, snapshot, room, true);

        long[] verifyDelays = new long[]{2L, 10L, 30L};
        for (long delay : verifyDelays) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                GameRoom currentRoom = getPlayerRoom(player.getUniqueId());
                if (player.isOnline() && currentRoom != null && currentRoom.getRoomId().equals(room.getRoomId())) {
                    applyHunterReconnectSnapshot(player, snapshot, room, delay <= 2L);
                }
            }, delay);
        }
    }

    private void applyHunterReconnectSnapshot(Player player, HunterReconnectSnapshot snapshot, GameRoom room, boolean applyVelocity) {
        if (player == null || snapshot == null || room == null) {
            return;
        }

        Location target = snapshot.location == null ? null : snapshot.location.clone();
        if (target == null || target.getWorld() == null) {
            World gameWorld = room.getGameWorld();
            target = gameWorld == null ? player.getLocation() : gameWorld.getSpawnLocation();
        }

        player.teleport(target);
        player.getInventory().clear();
        player.getInventory().setStorageContents(HunterReconnectSnapshot.cloneItemArray(snapshot.storageContents));
        player.getInventory().setArmorContents(HunterReconnectSnapshot.cloneItemArray(snapshot.armorContents));
        player.getInventory().setItemInOffHand(snapshot.offHand == null ? null : snapshot.offHand.clone());
        if (snapshot.heldSlot >= 0 && snapshot.heldSlot <= 8) {
            player.getInventory().setHeldItemSlot(snapshot.heldSlot);
        }

        clearPlayerActivePotionEffects(player);
        for (org.bukkit.potion.PotionEffect effect : snapshot.potionEffects) {
            if (effect != null) {
                player.addPotionEffect(effect, true);
            }
        }

        player.setGameMode(snapshot.gameMode == null ? org.bukkit.GameMode.SURVIVAL : snapshot.gameMode);
        player.setAllowFlight(snapshot.allowFlight);
        if (snapshot.allowFlight) {
            player.setFlying(snapshot.flying);
        } else {
            player.setFlying(false);
        }
        player.setFoodLevel(snapshot.foodLevel);
        player.setSaturation(snapshot.saturation);
        player.setExhaustion(snapshot.exhaustion);
        player.setLevel(snapshot.level);
        player.setExp(snapshot.exp);
        player.setTotalExperience(snapshot.totalExperience);
        player.setFireTicks(snapshot.fireTicks);
        player.setFreezeTicks(snapshot.freezeTicks);
        player.setNoDamageTicks(snapshot.noDamageTicks);
        player.setFallDistance(snapshot.fallDistance);
        player.setAbsorptionAmount(snapshot.absorption);
        double maxHealth = Math.max(1.0D, player.getMaxHealth());
        player.setHealth(Math.max(1.0D, Math.min(snapshot.health, maxHealth)));
        if (applyVelocity) {
            player.setVelocity(snapshot.velocity == null ? new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D) : snapshot.velocity.clone());
        }
        player.updateInventory();
    }

    public void restorePlayerInventory(Player player) {
        ItemStack[] saved = playerInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().clear();
            player.getInventory().setContents(saved);
        }
        // 清除持久化的会话文件
        plugin.getPlayerDataManager().clearPlayerSession(player.getUniqueId());
    }

    public void restorePlayerAfterRoom(GameRoom room, UUID uuid, boolean teleportToPrevious) {
        if (room == null || uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            restorePlayerAfterRoom(room, player, teleportToPrevious);
        }
    }

    private static class HunterReconnectSnapshot {
        private final String roomId;
        private final Location location;
        private final ItemStack[] storageContents;
        private final ItemStack[] armorContents;
        private final ItemStack offHand;
        private final int heldSlot;
        private final org.bukkit.GameMode gameMode;
        private final boolean allowFlight;
        private final boolean flying;
        private final double health;
        private final double absorption;
        private final int foodLevel;
        private final float saturation;
        private final float exhaustion;
        private final int level;
        private final float exp;
        private final int totalExperience;
        private final int fireTicks;
        private final int freezeTicks;
        private final int noDamageTicks;
        private final float fallDistance;
        private final org.bukkit.util.Vector velocity;
        private final Collection<org.bukkit.potion.PotionEffect> potionEffects;
        private final ItemStack[] previousInventoryContents;

        private HunterReconnectSnapshot(Player player, String roomId, ItemStack[] previousInventoryContents) {
            this.roomId = roomId;
            this.location = player.getLocation().clone();
            this.storageContents = cloneItemArray(player.getInventory().getStorageContents());
            this.armorContents = cloneItemArray(player.getInventory().getArmorContents());
            ItemStack hand = player.getInventory().getItemInOffHand();
            this.offHand = hand == null ? null : hand.clone();
            this.heldSlot = player.getInventory().getHeldItemSlot();
            this.gameMode = player.getGameMode();
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
            this.health = player.getHealth();
            this.absorption = player.getAbsorptionAmount();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.exhaustion = player.getExhaustion();
            this.level = player.getLevel();
            this.exp = player.getExp();
            this.totalExperience = player.getTotalExperience();
            this.fireTicks = player.getFireTicks();
            this.freezeTicks = player.getFreezeTicks();
            this.noDamageTicks = player.getNoDamageTicks();
            this.fallDistance = player.getFallDistance();
            this.velocity = player.getVelocity().clone();
            this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
            this.previousInventoryContents = cloneItemArray(previousInventoryContents);
        }

        private static ItemStack[] cloneItemArray(ItemStack[] source) {
            if (source == null) {
                return new ItemStack[0];
            }
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                copy[i] = source[i] == null ? null : source[i].clone();
            }
            return copy;
        }
    }

    public void restorePlayerAfterRoom(GameRoom room, Player player, boolean teleportToPrevious) {
        if (room == null || player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        plugin.getGameManager().clearManagedEndDimensionBrightness(player);
        plugin.getFlashModeManager().cleanupFlashPlayerState(player);

        if (teleportToPrevious) {
            Location prevLoc = room.getPreviousLocation(uuid);
            if (prevLoc != null) {
                player.teleport(prevLoc);
            }
        }

        restorePlayerInventory(player);
        if (plugin.getPlayerListener() != null) {
            plugin.getPlayerListener().suppressAdvancementMessages(uuid, 80L);
        }
        restorePlayerAdvancements(player, room);
        ensurePlayerRecipesAvailable(player);
        resetPlayerRuntimeState(player);
        // 房间结束后只保留进入前背包/位置/成就快照，运行状态一律清干净，避免上把燃烧、饱食度、经验条、药水等带到下一把。
        resetPlayerVitalsAndProgress(player);
        clearRoleNameTag(player);
        player.setPlayerListName(null);
        plugin.getScoreboardManager().removeScoreboard(player);
    }

    public void resetPlayerForRoomSession(Player player) {
        if (player == null) {
            return;
        }
        clearRoleNameTag(player);
        player.setPlayerListName(null);
        resetPlayerRuntimeState(player);
        resetPlayerVitalsAndProgress(player);
        clearPlayerAdvancements(player);
        resetPlayerRecipes(player);
        ensurePlayerRecipesAvailable(player);
        clearRoleNameTag(player);
    }

    public void resetPlayerForGameStart(GameRoom room, Player player) {
        if (player == null) {
            return;
        }
        plugin.getFlashModeManager().cleanupFlashPlayerState(player);
        if (room != null) {
            clearPlayerAdvancements(player);
        }
        resetPlayerRuntimeState(player);
        resetPlayerVitalsAndProgress(player);
        ensurePlayerRecipesAvailable(player);
    }

    public void resetPlayerForServerReturn(GameRoom room, Player player) {
        if (player == null) {
            return;
        }
        plugin.getGameManager().clearManagedEndDimensionBrightness(player);
        plugin.getFlashModeManager().cleanupFlashPlayerState(player);
        if (room != null) {
            restorePlayerAdvancements(player, room);
        }
        resetPlayerRuntimeState(player);
        resetPlayerVitalsAndProgress(player);
        clearRoleNameTag(player);
        player.setPlayerListName(null);
        plugin.getScoreboardManager().removeScoreboard(player);
    }

    private void resetPlayerRuntimeState(Player player) {
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGravity(true);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setGliding(false);
        player.setVisualFire(false);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0.0F);
        player.setNoDamageTicks(0);
        player.setAbsorptionAmount(0.0D);
        player.setVelocity(new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D));
        player.setArrowsInBody(0, false);
        player.setBeeStingersInBody(0);
        player.setArrowCooldown(0);
        player.setBeeStingerCooldown(0);
        player.setExpCooldown(0);
        player.setWardenWarningCooldown(0);
        player.resetCooldown();
        player.clearActiveItem();
        clearPlayerActivePotionEffects(player);
    }

    private void resetPlayerVitalsAndProgress(Player player) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        double maxHealth = Math.max(1.0D, player.getMaxHealth());
        try {
            player.setHealth(maxHealth);
        } catch (IllegalArgumentException ignored) {
            player.setHealth(Math.max(1.0D, Math.min(player.getHealth(), maxHealth)));
        }
    }

    private void restorePlayerPotionEffects(Player player, Collection<org.bukkit.potion.PotionEffect> previousPotionEffects) {
        clearPlayerActivePotionEffects(player);
        if (previousPotionEffects == null || previousPotionEffects.isEmpty()) {
            return;
        }
        for (org.bukkit.potion.PotionEffect effect : previousPotionEffects) {
            if (effect != null) {
                player.addPotionEffect(effect, true);
            }
        }
    }

    private void clearPlayerActivePotionEffects(Player player) {
        if (player == null) {
            return;
        }
        for (org.bukkit.potion.PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void cleanupAllRooms() {
        // 恢复所有玩家状态并清理房间
        for (GameRoom room : rooms.values()) {
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                restorePlayerAfterRoom(room, uuid, true);
            }
            for (UUID uuid : room.getSpectators()) {
                restorePlayerAfterRoom(room, uuid, true);
            }
        }
        rooms.clear();
        playerRooms.clear();
        playerInventories.clear();
        disconnectedHunters.clear();
        disconnectedHunterSnapshots.clear();
        endingChatIsolations.clear();
    }

    public int getMinimumPlayersForMode(GameMode mode) {
        if (mode == GameMode.SWAP) {
            return 3;
        }
        if (mode == GameMode.TNT_RUN || mode == GameMode.BLOCK_PARTY) {
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getConfig("minigames");
            String modePath = mode == GameMode.TNT_RUN ? "tnt_run" : "block_party";
            if (cfg != null) {
                String active = cfg.getString(modePath + ".active_map", "default");
                int activeMin = cfg.getInt(modePath + ".maps." + active + ".min_players", -1);
                if (activeMin > 0) {
                    return Math.max(2, activeMin);
                }
                org.bukkit.configuration.ConfigurationSection maps = cfg.getConfigurationSection(modePath + ".maps");
                if (maps != null) {
                    int best = Integer.MAX_VALUE;
                    for (String id : maps.getKeys(false)) {
                        if (!maps.getBoolean(id + ".enabled", true)) {
                            continue;
                        }
                        best = Math.min(best, Math.max(2, maps.getInt(id + ".min_players", 2)));
                    }
                    if (best != Integer.MAX_VALUE) {
                        return best;
                    }
                }
            }
        }
        return plugin.getConfigManager().getConfig().getInt("hunter_game.min_players", 2);
    }

    public void restorePlayerAdvancements(Player player, GameRoom room) {
        if (player == null || room == null) {
            return;
        }

        if (plugin.getPlayerListener() != null) {
            // 恢复快照会 revoke/award criteria，所有模式退出服务器或游戏结束时都不应该刷成就提示。
            plugin.getPlayerListener().suppressAdvancementMessages(player.getUniqueId(), 240L);
        }

        Map<String, Set<String>> snapshot = room.getPreviousAdvancements(player.getUniqueId());
        if (snapshot != null) {
            Iterator<Advancement> iterator = Bukkit.advancementIterator();
            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                Set<String> expectedCriteria = snapshot.getOrDefault(advancement.getKey().toString(), Collections.emptySet());

                for (String awarded : new HashSet<>(progress.getAwardedCriteria())) {
                    if (!expectedCriteria.contains(awarded)) {
                        progress.revokeCriteria(awarded);
                    }
                }

                for (String criterion : expectedCriteria) {
                    if (!progress.getAwardedCriteria().contains(criterion)) {
                        progress.awardCriteria(criterion);
                    }
                }
            }
        }

        Set<NamespacedKey> recipeSnapshot = room.getPreviousRecipes(player.getUniqueId());
        if (recipeSnapshot != null && !recipeSnapshot.isEmpty()) {
            player.discoverRecipes(new ArrayList<>(recipeSnapshot));
        }
    }

    private void clearPlayerAdvancements(Player player) {
        if (player == null) {
            return;
        }
        if (plugin.getPlayerListener() != null) {
            // 进入/重置房间时清成就也静默，避免后续触发提示。
            plugin.getPlayerListener().suppressAdvancementMessages(player.getUniqueId(), 240L);
        }
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String awarded : new HashSet<>(progress.getAwardedCriteria())) {
                try {
                    progress.revokeCriteria(awarded);
                } catch (IllegalArgumentException ignored) {
                    // 某些数据包进度在服务端刷新时可能瞬间失效，跳过即可，下一次重置还会继续清。
                }
            }
        }
    }

    private Map<String, Set<String>> snapshotPlayerAdvancements(Player player) {
        Map<String, Set<String>> snapshot = new HashMap<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (!progress.getAwardedCriteria().isEmpty()) {
                snapshot.put(advancement.getKey().toString(), new HashSet<>(progress.getAwardedCriteria()));
            }
        }
        return snapshot;
    }

    private Set<NamespacedKey> snapshotPlayerRecipes(Player player) {
        return new HashSet<>(player.getDiscoveredRecipes());
    }

    private void resetPlayerRecipes(Player player) {
        // 不再清空玩家已解锁配方。
        // 某些服务器开启 limited crafting 时，直接 undiscover 会导致基础配方（例如工作台）都无法合成。
    }

    public void ensurePlayerRecipesAvailable(Player player) {
        if (player == null) {
            return;
        }

        List<NamespacedKey> recipeKeys = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof org.bukkit.Keyed keyed) {
                recipeKeys.add(keyed.getKey());
            }
        }

        if (!recipeKeys.isEmpty()) {
            player.discoverRecipes(recipeKeys);
        }
    }

    // 处理猎人断线
    public void handleHunterDisconnect(UUID hunterUUID, String roomId) {
        disconnectedHunters.put(hunterUUID, roomId);

        // 5分钟后自动清除
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            disconnectedHunters.remove(hunterUUID);
            disconnectedHunterSnapshots.remove(hunterUUID);
        }, 20L * 60 * 5);
    }

    // 检查玩家是否可以重新加入
    public boolean canRejoin(UUID playerUUID) {
        return disconnectedHunters.containsKey(playerUUID);
    }

    // 重新加入游戏
    public boolean rejoinGame(Player player) {
        UUID playerUUID = player.getUniqueId();
        String roomId = disconnectedHunters.get(playerUUID);

        if (roomId == null) {
            return false;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null || room.getState() != RoomState.PLAYING) {
            disconnectedHunters.remove(playerUUID);
            disconnectedHunterSnapshots.remove(playerUUID);
            return false;
        }

        HunterReconnectSnapshot snapshot = disconnectedHunterSnapshots.get(playerUUID);

        // 检查是否还有至少一个猎人在游戏中。带有断线快照的玩家允许直接回到原局内状态，
        // 避免自己还没重新放回房间时被“没有猎人”判断挡掉。
        boolean hasActiveHunter = false;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isPrey(uuid) && Bukkit.getPlayer(uuid) != null) {
                hasActiveHunter = true;
                break;
            }
        }

        if (!hasActiveHunter && snapshot == null) {
            disconnectedHunters.remove(playerUUID);
            disconnectedHunterSnapshots.remove(playerUUID);
            player.sendMessage(getRoomMessageWithPrefix(room, "room.no_hunters_left"));
            return false;
        }

        // 重新加入进行中的房间：PLAYING 状态下不能走普通 addPlayer，否则会被等待房间限制挡住。
        if (!room.addRejoiningHunter(playerUUID)) {
            disconnectedHunters.remove(playerUUID);
            disconnectedHunterSnapshots.remove(playerUUID);
            return false;
        }
        playerRooms.put(playerUUID, roomId);
        disconnectedHunters.remove(playerUUID);
        snapshot = disconnectedHunterSnapshots.remove(playerUUID);

        if (snapshot != null && roomId.equals(snapshot.roomId)) {
            restoreHunterReconnectSnapshot(player, snapshot, room);
        } else if (room.getGameWorld() != null) {
            player.teleport(room.getGameWorld().getSpawnLocation());
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            plugin.getGameManager().giveHunterItems(player, room);
        }

        plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
        plugin.getGameManager().syncEndDimensionBrightness(player, room);
        updatePlayerTabNameWithRole(player, room.getRoomId(), false,
                room.getGameMode() == GameMode.END_FLASH ? room.getAssignedEndFlashKitName(playerUUID) : null);
        setRoleNameTag(player, room.getRoomId(), false,
                room.getGameMode() == GameMode.END_FLASH ? room.getAssignedEndFlashKitName(playerUUID) : null);
        refreshPlayerVisibility();
        plugin.getScoreboardManager().createScoreboard(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        room.broadcast(getRoomMessageWithPrefix(room, "room.player_rejoined", placeholders));

        return true;
    }

    // 获取断线的房间ID
    public String getDisconnectedRoomId(UUID playerUUID) {
        return disconnectedHunters.get(playerUUID);
    }

    public void clearDisconnectedHunter(UUID playerUUID) {
        disconnectedHunters.remove(playerUUID);
        disconnectedHunterSnapshots.remove(playerUUID);
    }

    // 更新玩家TAB列表显示名称（等待大厅，无职业）
    private void updatePlayerTabName(Player player, String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.getGameMode().isFlashTournament()) {
            player.setPlayerListName("§f" + player.getName());
            return;
        }
        if (room != null && room.getGameMode() == GameMode.LUCKY_PILLARS) {
            player.setPlayerListName(null);
            return;
        }
        String gradientRoomId = formatGradientRoomId(roomId);
        String tabName = "§8[§x§5§5§F§F§A§A等待§8] " + gradientRoomId + " §f" + player.getName();
        player.setPlayerListName(tabName);
    }

    // 更新玩家TAB列表显示名称（游戏中，带职业颜色）
    public void updatePlayerTabNameWithRole(Player player, String roomId, boolean isPrey) {
        updatePlayerTabNameWithRole(player, roomId, isPrey, null);
    }

    // 更新玩家TAB列表显示名称（游戏中，终章闪光可显示 Kit 名）
    public void updatePlayerTabNameWithRole(Player player, String roomId, boolean isPrey, String roleLabel) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.getGameMode().isFlashTournament()) {
            setTournamentLocatorColor(player, room, isPrey);
            player.setPlayerListName("§f" + player.getName());
            return;
        }
        if (room != null && room.getGameMode() == GameMode.LUCKY_PILLARS) {
            player.setPlayerListName(null);
            return;
        }
        if (room != null && room.getGameMode().isIndependentMode()) {
            String gradientRoomId = formatGradientRoomId(roomId);
            String label = normalizeRoleLabel(roleLabel, "参赛", 18);
            String roleTag = "§b[" + label + "§b]";
            String tabName = gradientRoomId + " " + roleTag + " §f" + player.getName();
            player.setPlayerListName(tabName);
            return;
        }
        String gradientRoomId = formatGradientRoomId(roomId);
        String label = normalizeRoleLabel(roleLabel, isPrey ? "猎物" : "猎人", 18);
        String roleTag = (isPrey ? "§a[" : "§c[") + label + (isPrey ? "§a]" : "§c]");
        String tabName = gradientRoomId + " " + roleTag + " §f" + player.getName();
        player.setPlayerListName(tabName);
    }

    /**
     * 给房间所有玩家设置头顶名字前缀（确定猎物后调用）
     */
    public void applyRoleNameTags(GameRoom room) {
        if (room != null && room.getGameMode().isFlashTournament()) {
            clearAllRoleNameTags(room);
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    setTournamentLocatorColor(p, room, room.isPrey(uuid));
                    p.setPlayerListName("§f" + p.getName());
                }
            }
            for (UUID uuid : room.getSpectators()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setPlayerListName("§f" + p.getName());
                }
            }
            return;
        }
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (room.getGameMode() == GameMode.LUCKY_PILLARS) {
                setRoleNameTag(p, room.getRoomId(), false, "");
                updatePlayerTabNameWithRole(p, room.getRoomId(), false, "");
                continue;
            }
            if (room.getGameMode().isIndependentMode()) {
                setRoleNameTag(p, room.getRoomId(), false, "参赛");
                updatePlayerTabNameWithRole(p, room.getRoomId(), false, "参赛");
                continue;
            }
            boolean isPrey = room.isPrey(uuid);
            String roleLabel = room.getGameMode() == GameMode.END_FLASH ? room.getAssignedEndFlashKitName(uuid) : null;
            setRoleNameTag(p, room.getRoomId(), isPrey, roleLabel);
        }
    }

    public void refreshRoleNameTags(GameRoom room) {
        if (room == null) {
            return;
        }
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
            clearWaitingRoomRoleNameTags(room);
            return;
        }
        if (room.getGameMode().isFlashTournament()) {
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    setTournamentLocatorColor(player, room, room.isPrey(uuid));
                    player.setPlayerListName("§f" + player.getName());
                }
            }
            for (UUID uuid : room.getSpectators()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    setSpectatorNameTag(player, room.getRoomId());
                }
            }
            return;
        }
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (room.getGameMode() == GameMode.LUCKY_PILLARS) {
                    if (room.isLuckyPillarsEliminated(uuid)) {
                        setSpectatorNameTag(player, room.getRoomId());
                    } else {
                        setRoleNameTag(player, room.getRoomId(), false, "");
                        updatePlayerTabNameWithRole(player, room.getRoomId(), false, "");
                    }
                    continue;
                }
                if (room.getGameMode().isIndependentMode()) {
                    setRoleNameTag(player, room.getRoomId(), false, "参赛");
                    updatePlayerTabNameWithRole(player, room.getRoomId(), false, "参赛");
                    continue;
                }
                setRoleNameTag(player, room.getRoomId(), room.isPrey(uuid),
                        room.getGameMode() == GameMode.END_FLASH ? room.getAssignedEndFlashKitName(uuid) : null);
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                setSpectatorNameTag(player, room.getRoomId());
            }
        }
    }

    private void setTournamentLocatorColor(Player player, GameRoom room, boolean isPrey) {
        if (player == null || room == null) {
            return;
        }
        clearRoleNameTag(player);

        String teamName = "gf_t" + (isPrey ? "p_" : "h_") + player.getName();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        java.util.Set<java.util.UUID> viewers = new java.util.HashSet<>();
        viewers.addAll(room.getAllPlayerUUIDs());
        viewers.addAll(room.getSpectators());

        for (java.util.UUID uuid : viewers) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            }

            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix("");
            team.setSuffix("");
            team.setColor(isPrey ? ChatColor.GREEN : ChatColor.RED);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }

        org.bukkit.scoreboard.Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (mainScoreboard != null) {
            org.bukkit.scoreboard.Team team = mainScoreboard.getTeam(teamName);
            if (team == null) {
                team = mainScoreboard.registerNewTeam(teamName);
            }
            team.setPrefix("");
            team.setSuffix("");
            team.setColor(isPrey ? ChatColor.GREEN : ChatColor.RED);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }
    }

    /**
     * 给单个玩家设置头顶名字前缀
     */
    public void setRoleNameTag(Player player, String roomId, boolean isPrey) {
        setRoleNameTag(player, roomId, isPrey, null);
    }

    /**
     * 给单个玩家设置头顶名字前缀，roleLabel 不为空时显示 Kit 名，但保留队伍颜色
     */
    public void setRoleNameTag(Player player, String roomId, boolean isPrey, String roleLabel) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.getGameMode().isFlashTournament()) {
            setTournamentLocatorColor(player, room, isPrey);
            player.setPlayerListName("§f" + player.getName());
            return;
        }
        if (room != null && room.getGameMode() == GameMode.LUCKY_PILLARS) {
            String teamName = "gf_lp_" + player.getName();
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            clearRoleNameTag(player);

            java.util.Set<java.util.UUID> allPlayers = new java.util.HashSet<>();
            allPlayers.addAll(room.getAllPlayerUUIDs());
            allPlayers.addAll(room.getSpectators());

            for (java.util.UUID uuid : allPlayers) {
                org.bukkit.entity.Player viewer = org.bukkit.Bukkit.getPlayer(uuid);
                if (viewer == null || !viewer.isOnline()) continue;

                org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();
                if (scoreboard == null || scoreboard == org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                }

                org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }
                team.setPrefix("");
                team.setSuffix("");
                team.setColor(ChatColor.RESET);
                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                }
            }
            return;
        }
        if (room != null && room.getGameMode().isIndependentMode()) {
            String teamName = "gf_ind_" + player.getName();
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            String label = normalizeRoleLabel(roleLabel, "参赛", 16);
            String prefix = "§b[" + label + "§b] §r";

            clearRoleNameTag(player);

            java.util.Set<java.util.UUID> allPlayers = new java.util.HashSet<>();
            allPlayers.addAll(room.getAllPlayerUUIDs());
            allPlayers.addAll(room.getSpectators());

            for (java.util.UUID uuid : allPlayers) {
                org.bukkit.entity.Player viewer = org.bukkit.Bukkit.getPlayer(uuid);
                if (viewer == null || !viewer.isOnline()) continue;

                org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();
                if (scoreboard == null || scoreboard == org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard()) {
                    scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                }

                org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }
                team.setPrefix(prefix);
                team.setColor(ChatColor.AQUA);
                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                }
            }
            return;
        }
        String teamName = "gf_" + (isPrey ? "prey_" : "hunt_") + player.getName();
        // 长度限制16
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        String label = normalizeRoleLabel(roleLabel, isPrey ? "猎物" : "猎人", 16);
        String prefix = (isPrey ? "§a[" : "§c[") + label + (isPrey ? "§a] §r" : "§c] §r");

        // 先移除旧的
        clearRoleNameTag(player);

        // 获取房间内所有玩家（包括旁观者）
        if (room == null) return;

        java.util.Set<java.util.UUID> allPlayers = new java.util.HashSet<>();
        allPlayers.addAll(room.getAllPlayerUUIDs());
        allPlayers.addAll(room.getSpectators());

        // 在每个玩家的记分板上设置团队前缀
        for (java.util.UUID uuid : allPlayers) {
            org.bukkit.entity.Player viewer = org.bukkit.Bukkit.getPlayer(uuid);
            if (viewer == null || !viewer.isOnline()) continue;

            // 获取玩家的记分板
            org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard == null || scoreboard == org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard()) {
                // 如果玩家没有自定义记分板，使用主记分板
                scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            }

            // 在这个记分板上创建或获取团队
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix(prefix);
            team.setColor(isPrey ? ChatColor.GREEN : ChatColor.RED);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }
    }

    private String normalizeRoleLabel(String roleLabel, String fallback, int maxLength) {
        String label = roleLabel == null || roleLabel.isBlank() ? fallback : roleLabel;
        label = ChatColor.stripColor(label);
        if (label == null || label.isBlank()) {
            label = fallback;
        }
        label = label.replace('[', ' ').replace(']', ' ').trim();
        int safeMax = Math.max(2, maxLength);
        if (label.length() > safeMax) {
            label = label.substring(0, safeMax);
        }
        return label;
    }

    /**
     * 给旁观者设置头顶名字前缀
     */
    public void setSpectatorNameTag(Player player, String roomId) {
        GameRoom tournamentRoom = rooms.get(roomId);
        if (tournamentRoom != null && tournamentRoom.getGameMode().isFlashTournament()) {
            clearRoleNameTag(player);
            player.setPlayerListName("§f" + player.getName());
            return;
        }
        String teamName = "gf_spec_" + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        clearRoleNameTag(player);

        // 设置头顶前缀：[房间号] [旁观]
        String gradientRoomId = formatGradientRoomId(roomId);
        String prefix = gradientRoomId + " §7[旁观] §r";

        // 获取房间内所有玩家（包括旁观者）
        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        java.util.Set<java.util.UUID> allPlayers = new java.util.HashSet<>();
        allPlayers.addAll(room.getAllPlayerUUIDs());
        allPlayers.addAll(room.getSpectators());

        // 在每个玩家的记分板上设置团队前缀
        for (java.util.UUID uuid : allPlayers) {
            org.bukkit.entity.Player viewer = org.bukkit.Bukkit.getPlayer(uuid);
            if (viewer == null || !viewer.isOnline()) continue;

            // 获取玩家的记分板
            org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard == null || scoreboard == org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard()) {
                // 如果玩家没有自定义记分板，使用主记分板
                scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            }

            // 在这个记分板上创建或获取团队
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix(prefix);
            team.setColor(ChatColor.GRAY);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }

        // 设置TAB列表名称
        String tabName = gradientRoomId + " §7[旁观] §f" + player.getName();
        player.setPlayerListName(tabName);
    }

    /**
     * 清除玩家头顶名字前缀
     */
    public void clearRoleNameTag(Player player) {
        if (player == null) {
            return;
        }
        String entryName = player.getName();

        // 从所有在线玩家的记分板中移除该玩家的 GameFun 身份团队
        for (org.bukkit.entity.Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard scoreboard = onlinePlayer.getScoreboard();
            if (scoreboard == null) continue;
            clearRoleNameTagFromScoreboard(scoreboard, entryName);
        }

        // 也从主记分板中移除
        if (org.bukkit.Bukkit.getScoreboardManager() != null) {
            org.bukkit.scoreboard.Scoreboard mainScoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            clearRoleNameTagFromScoreboard(mainScoreboard, entryName);
        }
    }

    private void clearWaitingRoomRoleNameTags(GameRoom room) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                clearRoleNameTag(player);
                updatePlayerTabName(player, room.getRoomId());
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                clearRoleNameTag(player);
                player.setPlayerListName("§f" + player.getName());
            }
        }
    }

    private void clearRoleNameTagFromScoreboard(org.bukkit.scoreboard.Scoreboard scoreboard, String entryName) {
        if (scoreboard == null || entryName == null || entryName.isBlank()) {
            return;
        }
        for (org.bukkit.scoreboard.Team team : new java.util.ArrayList<>(scoreboard.getTeams())) {
            if (!isGameFunRoleTeamForEntry(team, entryName)) {
                continue;
            }
            team.removeEntry(entryName);
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private boolean isGameFunRoleTeamForEntry(org.bukkit.scoreboard.Team team, String entryName) {
        if (team == null || !team.hasEntry(entryName)) {
            return false;
        }

        String teamName = team.getName() == null ? "" : team.getName().toLowerCase(Locale.ROOT);
        if (teamName.startsWith("gf_")) {
            return true;
        }

        String prefix = stripScoreboardColor(team.getPrefix());
        String suffix = stripScoreboardColor(team.getSuffix());
        String markerText = (prefix + " " + suffix).replace(" ", "");
        return markerText.contains("[猎人]")
                || markerText.contains("[猎物]")
                || markerText.contains("[柱上]")
                || markerText.contains("[旁观]")
                || markerText.contains("")
                || markerText.contains("猎人")
                || markerText.contains("猎物")
                || markerText.contains("旁观");
    }

    private String stripScoreboardColor(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? "" : stripped;
    }

    /**
     * 清除房间所有玩家的头顶名字前缀
     */
    public void clearAllRoleNameTags(GameRoom room) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) clearRoleNameTag(p);
        }
        for (UUID uuid : room.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) clearRoleNameTag(p);
        }
    }

    // 格式化房间ID为渐变色
    private String formatGradientRoomId(String roomId) {
        // GameFun 渐变色：从青色到橙色
        // §x§5§5§F§F§A§A (青色) -> §x§F§F§A§A§5§5 (橙色)
        String[] colors = {
            "§x§5§5§F§F§A§A",
            "§x§7§7§F§F§8§8",
            "§x§9§9§F§F§6§6",
            "§x§B§B§F§F§4§4",
            "§x§D§D§F§F§2§2",
            "§x§F§F§F§F§0§0",
            "§x§F§F§D§D§0§0",
            "§x§F§F§B§B§0§0"
        };

        StringBuilder result = new StringBuilder("§8[§r");
        String idStr = " " + roomId + " ";
        int colorIndex = 0;

        for (int i = 0; i < idStr.length(); i++) {
            char c = idStr.charAt(i);
            result.append(colors[colorIndex % colors.length]).append(c);
            if (c != ' ') {
                colorIndex++;
            }
        }

        result.append("§8]§r");
        return result.toString();
    }
}

