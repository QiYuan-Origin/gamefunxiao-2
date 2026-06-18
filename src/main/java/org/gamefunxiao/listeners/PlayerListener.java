package org.gamefunxiao.listeners;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.DualPreyProposalType;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.hunter.EndFlashKitDetailMenu;
import org.gamefunxiao.menu.hunter.InvitePlayerMenu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"deprecation"})
public class PlayerListener implements Listener {

    private final GameFunXiao plugin;
    private final Map<UUID, Long> advertiseCooldowns = new HashMap<>();
    private final Map<UUID, Integer> compassDropCount = new HashMap<>();
    private final Map<UUID, Long> lastCompassDrop = new HashMap<>();
    private final Map<UUID, Long> compassTpCooldown = new HashMap<>(); // 指南针TP 15分钟冷却
    private final Map<UUID, Long> hunterTpLootLockUntil = new HashMap<>();
    private final Map<UUID, Long> endFlashHunterRespawnUntil = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> endFlashHunterRespawnCountdownTasks = new HashMap<>();
    private final Map<UUID, QuickSwapAttackSnapshot> quickSwapAttackSnapshots = new HashMap<>();
    private final Map<UUID, Integer> pendingQuickSwapRestoreSlots = new HashMap<>();
    private final Set<UUID> forcedHunterTpSelection = new HashSet<>();
    private final Set<UUID> randomCompassEating = new HashSet<>();
    private final Map<UUID, Long> endFlashExitPortalActionCooldowns = new HashMap<>();
    private final Map<UUID, Long> advancementMessageSuppressUntil = new HashMap<>();
    private final Map<UUID, RecentCombatHit> recentCombatHits = new HashMap<>();
    private static final long RECENT_COMBAT_HIT_EXPIRE_MS = 15000L;
    private final Map<UUID, org.bukkit.GameMode> dimensionGameModeRestore = new HashMap<>();
    private final Map<UUID, Long> flashTournamentPreyInputUntil = new HashMap<>();
    // 传送门位置缓存：玩家UUID -> 传送门位置（用于下界门串联）
    private final Map<UUID, org.bukkit.Location> portalLocationCache = new HashMap<>();

    public PlayerListener(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    private Location getWorldSelectionPreySafeLocation(GameRoom room, Location viewSource) {
        if (room == null || room.getGameWorld() == null) {
            return null;
        }
        Location spawn = room.getGameWorld().getSpawnLocation().clone();
        spawn.setX(spawn.getBlockX() + 0.5D);
        spawn.setZ(spawn.getBlockZ() + 0.5D);
        if (viewSource != null) {
            spawn.setYaw(viewSource.getYaw());
            spawn.setPitch(viewSource.getPitch());
        }
        return spawn;
    }

    private void returnWorldSelectionPrey(Player player, GameRoom room, Location viewSource, boolean feedback) {
        Location safe = getWorldSelectionPreySafeLocation(room, viewSource);
        if (safe == null || safe.getWorld() == null) {
            return;
        }
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.teleport(safe);
        if (feedback) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.72f, 1.28f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.34f, 1.72f);
        }
    }

    private boolean shouldReturnWorldSelectionPrey(GameRoom room, Location to, Location spawnLoc) {
        if (room == null || to == null || spawnLoc == null || spawnLoc.getWorld() == null || to.getWorld() == null) {
            return false;
        }
        if (!to.getWorld().equals(spawnLoc.getWorld())) {
            return true;
        }
        World world = spawnLoc.getWorld();
        return to.getY() < spawnLoc.getY() - 1.15D || to.getY() <= world.getMinHeight() + 6.0D;
    }

    private boolean isSwapHoldingPlayer(GameRoom room, Player player) {
        return room != null
                && player != null
                && room.getState() == RoomState.PLAYING
                && room.getGameMode() == GameMode.SWAP
                && room.isSwapCountdownPrey(player.getUniqueId());
    }

    private GameRoom resolveManagedRoom(Player player, World world) {
        if (player == null) {
            return null;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room != null) {
            return room;
        }

        // 闪光测试是大厅里的单人试用状态，不应该被旧房间世界映射或临时大厅世界锁方块交互。
        // 否则玩家客户端会先显示已放置方块，但服务端随后取消，表现成“自己看得见、别人看不见”的假方块。
        if (plugin.getFlashModeManager().isStandaloneFlashEnabled(player.getUniqueId())) {
            return null;
        }

        if (world == null) {
            return null;
        }

        String roomId = plugin.getWorldManager().getRoomIdByWorld(world);
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        return plugin.getRoomManager().getRoom(roomId);
    }

    private boolean shouldLockBlockInteraction(GameRoom room) {
        if (room == null) {
            return false;
        }
        return switch (room.getState()) {
            case WAITING, STARTING, SELECTING -> true;
            case PLAYING -> !room.isGameActuallyStarted();
            default -> false;
        };
    }

    private boolean shouldLockWorldInteraction(Player player, World world, GameRoom room) {
        return shouldLockWorldInteraction(player, world, room, null);
    }

    private boolean shouldLockWorldInteraction(Player player, World world, GameRoom room, Location interactionLocation) {
        if (shouldLockBlockInteraction(room)) {
            return true;
        }
        return shouldProtectLobbyLikeWorld(player, world, interactionLocation);
    }

    private boolean shouldProtectLobbyLikeWorld(Player player, World world, Location interactionLocation) {
        if (world == null || !plugin.getWorldManager().isLobbyLikeWorld(world)) {
            return false;
        }
        if (canEditLobbyLikeWorld(player)) {
            return false;
        }
        return !isAllowedLobbyInteractionLocation(world, interactionLocation);
    }

    private boolean isAllowedLobbyInteractionLocation(World world, Location location) {
        if (world == null || location == null) {
            return false;
        }
        org.bukkit.configuration.ConfigurationSection root = plugin.getConfigManager().getConfig()
                .getConfigurationSection("lobby_interaction_regions");
        if (root == null || !root.getBoolean("enabled", true)) {
            return false;
        }
        org.bukkit.configuration.ConfigurationSection regions = root.getConfigurationSection("regions");
        if (regions == null) {
            return false;
        }
        String worldName = world.getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (String id : regions.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection section = regions.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String configuredWorld = section.getString("world", "");
            if (!configuredWorld.equalsIgnoreCase(worldName)) {
                continue;
            }
            int minX = Math.min(section.getInt("x1"), section.getInt("x2"));
            int maxX = Math.max(section.getInt("x1"), section.getInt("x2"));
            int minY = Math.min(section.getInt("y1"), section.getInt("y2"));
            int maxY = Math.max(section.getInt("y1"), section.getInt("y2"));
            int minZ = Math.min(section.getInt("z1"), section.getInt("z2"));
            int maxZ = Math.max(section.getInt("z1"), section.getInt("z2"));
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                return true;
            }
        }
        return false;
    }

    private boolean canEditLobbyLikeWorld(Player player) {
        return player != null
                && player.hasPermission("gamefunxiao.admin")
                && player.getGameMode() == org.bukkit.GameMode.CREATIVE;
    }

    private boolean shouldCancelProtectedBlockInteract(Player player, GameRoom room, PlayerInteractEvent event) {
        if (event == null) {
            return false;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK && action != Action.PHYSICAL) {
            return false;
        }
        Location interactionLocation = event.getClickedBlock() == null ? player.getLocation() : event.getClickedBlock().getLocation();
        World world = interactionLocation == null ? player.getWorld() : interactionLocation.getWorld();
        return shouldLockWorldInteraction(player, world, room, interactionLocation);
    }

    private void cancelProtectedBlockInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    private boolean shouldCancelProtectedEntityInteract(Player player, Entity entity) {
        if (player == null || entity == null || entity instanceof Player) {
            return false;
        }
        if (!isProtectedLobbyEntityType(entity.getType())) {
            return false;
        }
        GameRoom room = resolveManagedRoom(player, entity.getWorld());
        return shouldLockWorldInteraction(player, entity.getWorld(), room, entity.getLocation());
    }

    private boolean isProtectedLobbyEntityType(EntityType type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case ITEM_FRAME, GLOW_ITEM_FRAME, PAINTING, ARMOR_STAND,
                 ITEM_DISPLAY, BLOCK_DISPLAY, TEXT_DISPLAY, INTERACTION -> true;
            default -> false;
        };
    }

    private boolean isRoomWorld(GameRoom room, World world) {
        if (room == null || world == null) {
            return false;
        }
        World gameWorld = room.getGameWorld();
        if (gameWorld != null && gameWorld.equals(world)) {
            return true;
        }
        String worldRoomId = plugin.getWorldManager().getRoomIdByWorld(world);
        return worldRoomId != null && worldRoomId.equals(room.getRoomId());
    }

    private boolean isFlashExitPortalTeleport(GameRoom room, World world, PlayerTeleportEvent.TeleportCause cause) {
        return room != null
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted()
                && room.getGameMode().isFlashLike()
                && cause == PlayerTeleportEvent.TeleportCause.END_PORTAL
                && world != null
                && world.getEnvironment() == World.Environment.THE_END
                && isRoomWorld(room, world);
    }

    private boolean isEndPortalBlockAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y >= world.getMinHeight() && y < world.getMaxHeight()
                && world.getBlockAt(x, y, z).getType() == Material.END_PORTAL) {
            return true;
        }
        int belowY = y - 1;
        return belowY >= world.getMinHeight() && belowY < world.getMaxHeight()
                && world.getBlockAt(x, belowY, z).getType() == Material.END_PORTAL;
    }

    private boolean isFlashExitPortalMove(GameRoom room, Location to) {
        if (to == null || to.getWorld() == null) {
            return false;
        }
        return room != null
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted()
                && room.getGameMode().isFlashLike()
                && to.getWorld().getEnvironment() == World.Environment.THE_END
                && isRoomWorld(room, to.getWorld())
                && isEndPortalBlockAt(to);
    }

    private boolean startEndFlashExitPortalAction(Player player, GameRoom room) {
        if (player == null || room == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        endFlashExitPortalActionCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        Long until = endFlashExitPortalActionCooldowns.get(player.getUniqueId());
        if (until != null && until > now) {
            return false;
        }
        long cooldownMillis = room.isPrey(player.getUniqueId()) ? 3500L : 700L;
        endFlashExitPortalActionCooldowns.put(player.getUniqueId(), now + cooldownMillis);
        return true;
    }

    private boolean isPlayerOwnInventoryView(Inventory topInventory, Player player) {
        if (topInventory == null) {
            return false;
        }
        return topInventory.getType() == InventoryType.CRAFTING
                || topInventory.getType() == InventoryType.CREATIVE
                || topInventory.getHolder() == player;
    }

    private boolean canEditEndFlashStartupInventory(InventoryClickEvent event, Player player) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return false;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!isPlayerOwnInventoryView(topInventory, player)) {
            return false;
        }

        // 终章·闪光开局发完 Kit 后，允许整理自己的快捷栏、背包、盔甲、副手和原版合成格。
        // 外部容器 / 自定义菜单不是玩家自己的背包视图，这里继续交给锁定逻辑或 MenuListener 处理。
        return clickedInventory.equals(player.getInventory()) || clickedInventory.equals(topInventory);
    }

    private boolean canDragEndFlashStartupInventory(InventoryDragEvent event, Player player) {
        Inventory topInventory = event.getView().getTopInventory();
        if (isPlayerOwnInventoryView(topInventory, player)) {
            return true;
        }

        // 如果玩家碰巧打开了外部容器，只允许拖动不涉及顶部容器的玩家背包槽位，避免开局搬容器物品。
        int topSize = topInventory == null ? 0 : topInventory.getSize();
        return event.getRawSlots().stream().noneMatch(rawSlot -> rawSlot < topSize);
    }

    private boolean isCraftingView(Inventory topInventory) {
        if (topInventory == null) {
            return false;
        }
        InventoryType type = topInventory.getType();
        return type == InventoryType.CRAFTING || type == InventoryType.WORKBENCH;
    }

    private boolean isMountedActiveGame(Player player, GameRoom room) {
        return player != null
                && room != null
                && (player.isInsideVehicle() || player.getVehicle() != null)
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted()
                && !room.isSpectator(player.getUniqueId());
    }

    private boolean isActiveGameCraftingClick(InventoryClickEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || room.isSpectator(player.getUniqueId())) {
            return false;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!isCraftingView(topInventory)) {
            return false;
        }

        Inventory clickedInventory = event.getClickedInventory();
        return clickedInventory != null
                && (clickedInventory.equals(topInventory) || clickedInventory.equals(player.getInventory()));
    }

    private boolean isMountedActiveGameCraftingClick(InventoryClickEvent event, Player player, GameRoom room) {
        return isMountedActiveGame(player, room) && isActiveGameCraftingClick(event, player, room);
    }

    private boolean isMountedActiveGameCraftingDrag(InventoryDragEvent event, Player player, GameRoom room) {
        return event != null
                && isMountedActiveGame(player, room)
                && isCraftingView(event.getView().getTopInventory());
    }

    private boolean openMountedCraftingTable(PlayerInteractEvent event, Player player, GameRoom room, Action action) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || action != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.CRAFTING_TABLE
                || !isMountedActiveGame(player, room)) {
            return false;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        plugin.getRoomManager().ensurePlayerRecipesAvailable(player);

        Location workbenchLocation = event.getClickedBlock().getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            GameRoom currentRoom = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (player.isOnline() && isMountedActiveGame(player, currentRoom)) {
                plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
                player.openWorkbench(workbenchLocation, true);
            }
        });
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getFlashModeManager() != null) {
                plugin.getFlashModeManager().syncFlashItemLore(player);
            }
        }, 40L);

        if (plugin.getPlayerDataManager().getProxyTransferRoomId(player.getUniqueId()) != null) {
            event.joinMessage(null);
        }
        if (plugin.getChildServerManager().handleTemplateLobbyEditJoin(player)) {
            event.joinMessage(null);
            return;
        }
        if (plugin.getChildServerManager().handleMiniGameMapEditJoin(player)) {
            event.joinMessage(null);
            return;
        }
        if (plugin.getChildServerManager().handleEndFlashTuningJoin(player)) {
            event.joinMessage(null);
            return;
        }
        if (plugin.getChildServerManager().handleCrossServerPlayerJoin(player)) {
            event.joinMessage(null);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getChildServerManager().requestCrossServerEndpoint(player);
                    plugin.getChildServerManager().requestEndFlashKitSync(player);
                    plugin.getRoomManager().refreshPlayerVisibility();
                    plugin.getTabHeaderFooterManager().applyLater(player);
                }
            }, 20L);
            return;
        }
        plugin.getChildServerManager().handleLobbyJoinRestore(player);

        // 清除可能残留的职业前缀（防止服务器重启后前缀未清除）
        plugin.getRoomManager().clearRoleNameTag(player);

        // 更新玩家名称
        var data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setPlayerName(player.getName());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !plugin.getRoomManager().isInRoom(player.getUniqueId())) {
                plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
            }
        }, 1L);
        // 检查是否有待恢复的游戏会话（强制关服后重新上线）
        if (!plugin.getChildServerManager().isNodeMode()) {
            plugin.getPlayerDataManager().checkPendingRecovery(player);
        }
        plugin.getChildServerManager().handleNodePlayerJoin(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            boolean rejoined = false;
            if (plugin.getRoomManager().canRejoin(player.getUniqueId())) {
                rejoined = plugin.getRoomManager().rejoinGame(player);
                if (rejoined) {
                    GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
                    player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.rejoined"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.86f, 1.18f);
                }
            }
            if (!rejoined && !plugin.getRoomManager().isInRoom(player.getUniqueId())) {
                plugin.getPlayerDataManager().restoreLocalRoomSessionIfPresent(player);
            }
        }, 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getChildServerManager().requestCrossServerEndpoint(player);
                plugin.getChildServerManager().requestEndFlashKitSync(player);
                plugin.getRoomManager().refreshPlayerVisibility();
                plugin.getTabHeaderFooterManager().applyLater(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        cancelEndFlashHunterRespawnCountdown(playerId);
        InvitePlayerMenu.cancelSearchInput(plugin, player, false);
        EndFlashKitDetailMenu.cancelRenameInput(plugin, player, false);
        EndFlashKitDetailMenu.cancelGuideInput(plugin, player, false);

        if (plugin.getChildServerManager().isTransferInProgress(playerId)) {
            event.quitMessage(null);
            plugin.getChildServerManager().markTransferHandled(playerId);
            return;
        }

        // 清除传送门位置缓存
        portalLocationCache.remove(playerId);
        dimensionGameModeRestore.remove(playerId);
        if (plugin.getRoomManager().isInRoom(playerId)) {
            suppressAdvancementMessages(playerId, 240L);
        } else {
            advancementMessageSuppressUntil.remove(playerId);
        }

        // 如果在房间中，离开房间
        if (plugin.getRoomManager().isInRoom(playerId)) {
            event.quitMessage(null);
            // 玩家退出时统一交给 RoomManager 处理。
            // 这里之前单独写了一套猎人/猎物判胜逻辑，导致任意非 1v2 的猎人掉线都会直接 endGame(room, true)，
            // 表现为“一个人退出，全房间结束并显示猎物胜利”。RoomManager.leaveRoom 已经会先移除退出者，
            // 再检查是否真的没有猎人/猎物，避免单个猎人退出就误结算。
            plugin.getRoomManager().handlePlayerDisconnect(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // 游戏指南针左/右键也需要处理，其他逻辑仍以右键为主。
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.PHYSICAL) {
            return;
        }

        // 检查玩家是否在房间中
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            if (shouldCancelProtectedBlockInteract(player, null, event)) {
                cancelProtectedBlockInteract(event);
            }
            return;
        }

        if (isSwapHoldingPlayer(room, player)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        if (openMountedCraftingTable(event, player, room, action)) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && room.hasModifier("RewardChest")
                && room.getRewardChestLocation() != null
                && event.getClickedBlock().getLocation().equals(room.getRewardChestLocation().getBlock().getLocation())) {
            room.setRewardChestOpened(true);
        }

        // 获取本次交互使用的手持物品，保证副手指南针也能打开闪光背包。
        ItemStack item = event.getItem();
        Material type = item == null ? Material.AIR : item.getType();
        RoomState state = room.getState();

        // 获取CustomModelData
        int modelData = 0;
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            modelData = item.getItemMeta().getCustomModelData();
        }

        // 根据房间状态处理
        // 旁观者退出物品（任何状态均可触发）
        if (modelData == 10008 && room.isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            plugin.getRoomManager().leaveRoom(player);
            return;
        }

        if (state == RoomState.WAITING || state == RoomState.STARTING) {
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                if (shouldCancelProtectedBlockInteract(player, room, event)) {
                    cancelProtectedBlockInteract(event);
                }
                return;
            }
            if (handleLobbyItem(player, room, type, modelData, state)) {
                event.setCancelled(true);
                return;
            }
            if (shouldCancelProtectedBlockInteract(player, room, event)) {
                cancelProtectedBlockInteract(event);
            }
        } else if (state == RoomState.SELECTING) {
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                if (shouldCancelProtectedBlockInteract(player, room, event)) {
                    cancelProtectedBlockInteract(event);
                }
                return;
            }
            if (room.isEndChapterDivisionActive()) {
                if (handleEndChapterDivisionItem(player, room, type, modelData)) {
                    event.setCancelled(true);
                    return;
                }
            } else if (room.isPrey(player.getUniqueId())) {
                if (handleWorldSelectionItem(player, room, type, modelData)) {
                    event.setCancelled(true);
                    return;
                }
            } else if (handleHunterSelectionItem(player, room, type, modelData)) {
                event.setCancelled(true);
                return;
            }
            if (shouldCancelProtectedBlockInteract(player, room, event)) {
                cancelProtectedBlockInteract(event);
            }
        } else if (state == RoomState.PLAYING) {
            if (item != null && item.getType() != Material.AIR
                    && plugin.getFlashModeManager().handleFlashCompassBackpackInteract(event, player, room, item)) {
                return;
            }
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                if (shouldCancelProtectedBlockInteract(player, room, event)) {
                    cancelProtectedBlockInteract(event);
                }
                return;
            }
            if (plugin.getGameManager().handleLuckyPillarsSpawnEggUse(event, player, room)) {
                return;
            }
            if (handleGameItem(player, room, type, modelData)) {
                event.setCancelled(true);
                return;
            }
            if (shouldCancelProtectedBlockInteract(player, room, event)) {
                cancelProtectedBlockInteract(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onForbiddenBedUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            return;
        }

        World world = player.getWorld();
        if (plugin.getWorldManager().getRoomIdByWorld(world) == null) {
            return;
        }

        if (world.getEnvironment() != World.Environment.NETHER && world.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (!(clickedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData)) {
            return;
        }

        event.setCancelled(true);
        destroyBedPair(clickedBlock, bedData);

        org.bukkit.Location explodeLoc = clickedBlock.getLocation().add(0.5, 0.5, 0.5);
        world.createExplosion(explodeLoc.getX(), explodeLoc.getY(), explodeLoc.getZ(), 5.0F, true, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onForbiddenRespawnAnchorUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            return;
        }

        World world = player.getWorld();
        if (plugin.getWorldManager().getRoomIdByWorld(world) == null) {
            return;
        }

        if (world.getEnvironment() == World.Environment.NETHER) {
            return;
        }

        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getType() != Material.RESPAWN_ANCHOR) {
            return;
        }

        event.setCancelled(true);
        clickedBlock.setType(Material.AIR, false);

        org.bukkit.Location explodeLoc = clickedBlock.getLocation().add(0.5, 0.5, 0.5);
        world.createExplosion(explodeLoc.getX(), explodeLoc.getY(), explodeLoc.getZ(), 5.0F, true, true);
    }

    private boolean handleLobbyItem(Player player, GameRoom room, Material type, int modelData, RoomState state) {
        // 使用CustomModelData识别物品
        if (modelData > 0) {
            switch (modelData) {
                case 10001 -> {
                    // 投票猎物
                    if (!room.getGameMode().usesPreySelection()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.prey_vote_unavailable_for_mode"));
                        return true;
                    }
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    new org.gamefunxiao.menu.hunter.VotePreyMenu(plugin, player, room).open();
                    return true;
                }
                case 10002 -> {
                    // 管理员强制开始
                    if (player.hasPermission("gamefunxiao.admin")) {
                        if (room.isAdminForceStartUsed()) {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.force_start_already_used"));
                            return true;
                        }
                        int requiredPlayers = plugin.getRoomManager().getMinimumPlayersForMode(room.getGameMode());
                        if (room.getPlayerCount() >= requiredPlayers) {
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                            new org.gamefunxiao.menu.hunter.ConfirmForceStartMenu(plugin, player, room).open();
                        } else {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("count", String.valueOf(requiredPlayers));
                    player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.need_more_players", placeholders));
                        }
                        return true;
                    }
                }
                case 10003 -> {
                    // 宣传房间
                    handleAdvertise(player, room);
                    return true;
                }
                case 10012 -> {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
                    new org.gamefunxiao.menu.hunter.VoteDoublePreyMenu(plugin, player, room).open();
                    return true;
                }
                case 10022 -> {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.35f);
                    plugin.getGameManager().handleFlashTriplePreyVote(player, room);
                    return true;
                }
                case 10004 -> {
                    // 退出游戏
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                    plugin.getRoomManager().leaveRoom(player);
                    player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.you_left"));
                    return true;
                }
            }
        }
        return false;
    }

    private void destroyBedPair(org.bukkit.block.Block clickedBlock, org.bukkit.block.data.type.Bed bedData) {
        clickedBlock.setType(Material.AIR, false);

        org.bukkit.block.Block otherPart = bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD
                ? clickedBlock.getRelative(bedData.getFacing().getOppositeFace())
                : clickedBlock.getRelative(bedData.getFacing());

        if (otherPart.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
            otherPart.setType(Material.AIR, false);
        }
    }

    private boolean handleWorldSelectionItem(Player player, GameRoom room, Material type, int modelData) {
        if (modelData > 0) {
            switch (modelData) {
                case 10005 -> {
                    if (plugin.getGameManager().requiresDualPreyWorldAgreement(room)) {
                        if (room.hasPendingDualPreyProposal()) {
                            plugin.getGameManager().approveDualPreyWorldProposal(room, player, false);
                        } else {
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.35f);
                            plugin.getGameManager().startDualPreyWorldProposal(room, player, DualPreyProposalType.CONFIRM_WORLD);
                        }
                        return true;
                    }

                    if (room.getGameMode() == org.gamefunxiao.game.GameMode.NETHER_CHAPTER
                            && room.getCountdown() > plugin.getConfigManager().getNetherHunterVoteTime()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.35f);
                        plugin.getGameManager().lockNetherChapterWorldSelection(room);
                        return true;
                    }

                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    plugin.getGameManager().confirmSelectedWorld(room);
                    return true;
                }
                case 10006 -> {
                    if (plugin.getGameManager().requiresDualPreyWorldAgreement(room)) {
                        if (room.hasPendingDualPreyProposal()) {
                            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_waiting_other"));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        } else {
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.2f);
                            plugin.getGameManager().startDualPreyWorldProposal(room, player, DualPreyProposalType.REROLL_WORLD);
                        }
                        return true;
                    }

                    plugin.getGameManager().rerollSelectedWorld(room);
                    return true;
                }
                case 10013 -> {
                    if (room.hasPendingDualPreyProposal()) {
                        plugin.getGameManager().rejectDualPreyWorldProposal(room, player);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleEndChapterDivisionItem(Player player, GameRoom room, Material type, int modelData) {
        if (room.getGameMode() != GameMode.END_CHAPTER || !room.isEndChapterDivisionActive()) {
            return false;
        }

        if (modelData == 10004) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            plugin.getRoomManager().leaveRoom(player);
                    player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.you_left"));
            return true;
        }

        if (room.isPrey(player.getUniqueId())) {
            if (modelData == 10018) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.3f);
                new org.gamefunxiao.menu.hunter.EndPreyKitMenu(plugin, player, room).open();
                return true;
            }
            if (modelData == 10019) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.75f, 1.45f);
                new org.gamefunxiao.menu.hunter.EndPreyPositionMenu(plugin, player, room).open();
                return true;
            }
            return false;
        }

        if (room.isHunter(player.getUniqueId())) {
            if (modelData == 10020) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.4f);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
                new org.gamefunxiao.menu.hunter.VoteEndHunterKitMenu(plugin, player, room).open();
                return true;
            }
            if (modelData == 10021) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.35f);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.55f, 1.5f);
                new org.gamefunxiao.menu.hunter.VoteEndHunterPositionMenu(plugin, player, room).open();
                return true;
            }
        }

        return false;
    }

    private boolean handleHunterSelectionItem(Player player, GameRoom room, Material type, int modelData) {
        if (room.getGameMode() != org.gamefunxiao.game.GameMode.NETHER_CHAPTER || !room.isHunter(player.getUniqueId())) {
            return false;
        }

        if (modelData == 10011) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.4f);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
            new org.gamefunxiao.menu.hunter.VoteHunterPositionMenu(plugin, player, room).open();
            return true;
        }

        return false;
    }

    private boolean handleGameItem(Player player, GameRoom room, Material type, int modelData) {
        if (room.getGameMode().isBrickGuard()) {
            return plugin.getBrickGuardManager().handleGameItem(player, room, player.getInventory().getItemInMainHand(), type, modelData);
        }
        // 开始游戏按钮
        if (modelData == 10007 && room.isPrey(player.getUniqueId())) {
            plugin.getGameManager().triggerGameStart(room, player);
            return true;
        }

        if (type == Material.RECOVERY_COMPASS && modelData == 10009) {
            startRandomCompassEating(player);
            return false;
        }
        return false;
    }

    public void advertiseCurrentRoom(Player player) {
        if (player == null) {
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_in_room"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (room.getState() != RoomState.WAITING && room.getState() != RoomState.STARTING) {
            String messagePath = room.getState() == RoomState.PLAYING ? "room.game_started" : "room.invite_room_unavailable";
            player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, messagePath));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        handleAdvertise(player, room);
    }

    private void handleAdvertise(Player player, GameRoom room) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = 30000; // 30秒冷却

        if (advertiseCooldowns.containsKey(uuid)) {
            long lastUse = advertiseCooldowns.get(uuid);
            if (now - lastUse < cooldown) {
                int remaining = (int) ((cooldown - (now - lastUse)) / 1000);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(remaining));
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "general.cooldown", placeholders));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        advertiseCooldowns.put(uuid, now);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);

        String playerName = player.getName();
        String mode = room.getGameMode() != null && room.getGameMode().isLuckyPillars()
                ? room.getLuckyPillarsAdvertiseModeName()
                : room.getModeName();
        String current = String.valueOf(room.getPlayerCount());
        String max = room.getMaxPlayers() == -1 ? "\u221E" : String.valueOf(room.getMaxPlayers());
        String roomId = room.getRoomId();
        String messageText = "§6🔔 §f" + playerName + " §7喊话房间 §e" + mode + " §a§n<加入>";

        net.kyori.adventure.text.Component message = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
            .deserialize(messageText)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/gamefunxiao hg join " + roomId))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                    "§a点击加入房间\n\n"
                            + "§7房间ID: §e" + roomId + "\n"
                            + "§7模式: §e" + mode + "\n"
                            + "§7人数: §a" + current + "§7/§a" + max)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID targetUuid = p.getUniqueId();
            GameRoom targetRoom = plugin.getRoomManager().getPlayerRoom(targetUuid);
            if (targetRoom != null && targetRoom.getState() == RoomState.PLAYING) {
                continue; // 跳过正在游戏中的玩家
            }
            p.sendMessage(message);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }

        plugin.getChildServerManager().forwardRoomAdvertiseToLobby(player, room, messageText);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            plugin.getFlashModeManager().handleSwordWaveDrop(event, player, null);
            return;
        }

        if (isSwapHoldingPlayer(room, player)) {
            event.setCancelled(true);
            return;
        }

        // 在大厅、世界选择阶段禁止丢弃物品
        if (room.getState() == RoomState.WAITING ||
            room.getState() == RoomState.STARTING ||
            room.getState() == RoomState.SELECTING) {
            event.setCancelled(true);
            return;
        }

        if (room.getGameMode().isBrickGuard() && plugin.getBrickGuardManager().handleDropItem(room, player, item)) {
            event.setCancelled(true);
            return;
        }

        // 旁观者退出物品（扔出即退出）
        if (room.isSpectator(player.getUniqueId())) {
            int modelData = 0;
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                modelData = item.getItemMeta().getCustomModelData();
            }
            if (modelData == 10008) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                plugin.getRoomManager().leaveRoom(player);
                return;
            }
            event.setCancelled(true);
            return;
        }

        // 游戏中，只禁止丢弃指南针（猎人用于TP功能）
        if (room.getState() == RoomState.PLAYING) {
            if (room.getGameMode().isStandaloneMiniGame()) {
                event.setCancelled(true);
                return;
            }

            if (plugin.getFlashModeManager().handleSwordWaveDrop(event, player, room)) {
                return;
            }

            if (item != null && item.getType() == Material.COMPASS) {
                // Q 丢指南针是传送菜单手势，不应该被闪光模式的左键个人背包误识别。
                plugin.getFlashModeManager().suppressFlashCompassBackpackOpen(player, 650L);
            }

            // 检查是否是队伍指南针（用于TP功能）
            if (isTeamTeleportCompassUser(player, room, item)) {
                event.setCancelled(true); // 禁止指南针掉落

                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();

                // 检查是否在2秒内连续扔
                if (lastCompassDrop.containsKey(uuid) && now - lastCompassDrop.get(uuid) < 2000) {
                    int count = compassDropCount.getOrDefault(uuid, 0) + 1;
                    compassDropCount.put(uuid, count);

                    if (count >= 2) {
                        // 获得TP机会
                        compassDropCount.remove(uuid);
                        lastCompassDrop.remove(uuid);

                        if (room.isHunter(uuid) && plugin.getFlashModeManager().isHunterWithinPreyDistance(room, player, 70.0D)) {
                            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c你距离猎物太近，70格内不能传送到其他猎人身边。");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 0.82f);
                            return;
                        }

                        // 检查指南针传送冷却
                        long nowCheck = System.currentTimeMillis();
                        long remainingMillis = getCompassTpRemainingMillis(uuid);
                        if (remainingMillis > 0L) {
                            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.tp_cooldown")
                                    .replace("{time}", formatDurationZh(remainingMillis)));
                            return;
                        }

                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.tp_available"));

                        // 打开TP菜单（冷却时间将在实际传送后设置）
                        player.closeInventory();
                        new org.gamefunxiao.menu.hunter.TeleportTeammateMenu(plugin, player, room).open();
                    }
                } else {
                    compassDropCount.put(uuid, 1);
                }
                lastCompassDrop.put(uuid, now);
            }
            // 其他物品允许丢弃
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (isSwapHoldingPlayer(room, player)) {
            event.setCancelled(true);
            return;
        }
        if (!isHunterTpLootLocked(player.getUniqueId())) {
            return;
        }
        if (room == null || room.getState() != RoomState.PLAYING) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c猎人死亡TP后的5分钟内，暂时不能捡起物品。");
        playRestrictionSound(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room != null && plugin.getBrickGuardManager().handleItemConsume(player, room, item)) {
            return;
        }
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) {
            return;
        }

        int modelData = 0;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            modelData = item.getItemMeta().getCustomModelData();
        }
        if (modelData != 10009) {
            return;
        }

        stopRandomCompassEating(player);
        room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room != null) {
            plugin.getGameManager().consumeRandomCompass(player, room);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerStopUsingItem(PlayerStopUsingItemEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) {
            return;
        }

        int modelData = 0;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            modelData = item.getItemMeta().getCustomModelData();
        }
        if (modelData != 10009) {
            return;
        }

        stopRandomCompassEating(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) return;

        if (plugin.getBrickGuardManager().handleRespawn(event, player, room)) {
            return;
        }

        // 检查是否有待复活位置（猎物死亡时设置的）
        org.bukkit.Location pendingLoc = room.getPendingRespawnLocation(player.getUniqueId());
        if (pendingLoc != null) {
            if (room.getGameMode() == GameMode.END_FLASH) {
                event.setRespawnLocation(getEndFlashSafeRespawnLocation(room, pendingLoc));
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> plugin.getGameManager().syncEndDimensionBrightness(player, room), 2L);
                return;
            }
            event.setRespawnLocation(pendingLoc);
            applyFlashRespawnProtectionAndCooldown(player, room);
            return;
        }

        // 猎人/猎物复活到游戏世界
        if (room.getState() == RoomState.PLAYING) {
            org.bukkit.Location respawnLoc = player.getRespawnLocation();
            if (respawnLoc != null && respawnLoc.getWorld() != null) {
                String respawnRoomId = plugin.getWorldManager().getRoomIdByWorld(respawnLoc.getWorld());
                if (respawnRoomId != null && respawnRoomId.equals(room.getRoomId())) {
                    event.setRespawnLocation(getSafeRespawnLocation(respawnLoc));
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> plugin.getGameManager().syncEndDimensionBrightness(player, room), 2L);
                    applyFlashRespawnProtectionAndCooldown(player, room);
                    return;
                }
            }
            if (respawnLoc != null && room.getGameWorld() != null && respawnLoc.getWorld() != null
                    && respawnLoc.getWorld().equals(room.getGameWorld())) {
                event.setRespawnLocation(getSafeRespawnLocation(respawnLoc));
            } else if (room.getGameWorld() != null) {
                event.setRespawnLocation(getSafeRespawnLocation(room.getGameWorld().getSpawnLocation()));
            }
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getGameManager().syncEndDimensionBrightness(player, room), 2L);
            applyFlashRespawnProtectionAndCooldown(player, room);
        }
    }

    private void applyFlashRespawnProtectionAndCooldown(Player player, GameRoom room) {
        if (!plugin.getFlashModeManager().isFlashMode(room) || room.getState() != RoomState.PLAYING) {
            return;
        }
        if (room.getGameMode().isFlashTournament()) {
            return;
        }
        if (!room.isHunter(player.getUniqueId())) {
            return;
        }
        if (room.getGameMode() == GameMode.END_FLASH) {
            return;
        }
        addCompassTpCooldown(player.getUniqueId(), 2 * 60 * 1000L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), 20 * 20));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 20 * 20, 4, false, true, true));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.42f, 1.55f);
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§5§5§F§F§A§A✦ §a你获得了20秒复活保护，传送冷却额外增加2分钟。");
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());

        if (room == null || room.getState() != RoomState.PLAYING) return;

        Player earlyKiller = resolveDeathKiller(player, room);
        if (earlyKiller != null && !earlyKiller.getUniqueId().equals(player.getUniqueId())
                && room.getAllPlayerUUIDs().contains(earlyKiller.getUniqueId())) {
            boolean hunterKillsPrey = room.isHunter(earlyKiller.getUniqueId()) && room.isPrey(player.getUniqueId());
            boolean preyKillsHunter = room.isPrey(earlyKiller.getUniqueId()) && room.isHunter(player.getUniqueId());
            if ((hunterKillsPrey || preyKillsHunter) && !room.getGameMode().isLuckyPillars() && room.getGameMode().usesHunterFlowMode()) {
                playHunterGameKillEffect(earlyKiller, player.getLocation().clone());
            }
        }

        // 隐藏全局死亡消息，只把“原版死亡消息内容 + 猎人游戏前缀”发给本房间
        Component vanillaDeathMessage = event.deathMessage();
        if (vanillaDeathMessage == null) {
            String legacyDeathMessage = event.getDeathMessage();
            if (legacyDeathMessage != null && !legacyDeathMessage.isBlank()) {
                vanillaDeathMessage = LegacyComponentSerializer.legacySection().deserialize(legacyDeathMessage);
            }
        }
        event.deathMessage(null);
        if (vanillaDeathMessage != null) {
            Component message = room.getGameMode().isFlashTournament()
                    ? vanillaDeathMessage
                    : room.getGameMode().isLuckyPillars()
                    ? withLuckyPillarsPrefix(vanillaDeathMessage)
                    : withHunterGamePrefix(vanillaDeathMessage);
            broadcastRoomComponent(room, message);
        }

        if (room.getGameMode() == GameMode.END_FLASH) {
            handleEndFlashDeath(event, player, room);
            return;
        }

        if (plugin.getBrickGuardManager().handleDeath(event, player, room)) {
            return;
        }

        if (plugin.getGameManager().handleLuckyPillarsDeath(event, player, room)) {
            return;
        }

        if (plugin.getGameManager().handleStandaloneMiniGameDeath(event, player, room)) {
            return;
        }

        if (room.isPrey(player.getUniqueId())) {
            boolean keepPreyInventory = !room.getGameMode().isFlashTournament()
                    && room.hasModifier("PreyRespawn")
                    && room.canUsePreyRespawn(player.getUniqueId());
            event.setKeepInventory(keepPreyInventory);
            if (keepPreyInventory) {
                event.getDrops().clear();
            }

            // 把死亡位置存起来，在 PlayerRespawnEvent 里直接设置复活点
            room.setPendingRespawnLocation(player.getUniqueId(), player.getLocation().clone());

            boolean tournament = room.getGameMode().isFlashTournament();

            // 猎物死亡扣分 -5
            if (!tournament) {
                plugin.getPlayerDataManager().addPreyPoints(player.getUniqueId(), -5, room.getGameMode());
            }

            // 记录击杀（1v1情况下不记录）
            Player killer = resolveDeathKiller(player, room);
            if (killer != null && room.isHunter(killer.getUniqueId())) {
                boolean isOneVsOne = room.getPlayerCount() == 2 && room.getPreyUUIDs().size() == 1;
                if (!isOneVsOne && !tournament) {
                    plugin.getPlayerDataManager().incrementHunterKills(killer.getUniqueId(), room.getGameMode());
                }
                // 猎人击杀猎物+3积分
                if (!tournament) {
                    plugin.getPlayerDataManager().addHunterPoints(killer.getUniqueId(), 3, room.getGameMode());
                }

                // 发送击杀消息和播放雷击音效
                killer.playSound(killer.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
                if (!tournament) {
                    Map<String, String> killMsg = new HashMap<>();
                    killMsg.put("player", player.getName());
                    killMsg.put("points", "+3");
                    killer.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("kill.hunter_kill_prey", killMsg));
                }
                // 击杀特效已在死亡事件入口统一播放，避免终章闪光/普通分支漏播或重复播。
            }

            if (keepPreyInventory) {
                room.markPreyRespawnUsed(player.getUniqueId());
                // 有复活修饰符且还有一次复活机会，不结束游戏，立即自动复活
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        player.spigot().respawn();
                    } catch (Exception ignored) {}
                }, 1L);
            } else {
                // 立即复活猎物，然后结束游戏
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        player.spigot().respawn();
                    } catch (Exception ignored) {}
                }, 1L);

                if (room.getPreyUUIDs().size() > 1) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> convertDefeatedPreyToSpectator(player, room), 5L);
                } else {
                    if (killer != null && room.isHunter(killer.getUniqueId())) {
                        room.setVictoryEffectTrigger(killer.getUniqueId(), player.getLocation().clone());
                    }
                    // 延迟结束游戏，让复活先完成
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (room.getState() == RoomState.PLAYING) {
                            plugin.getGameManager().endGame(room, false);
                        }
                    }, 5L);
                }
            }
        } else {
            // 猎人默认掉落物品；闪光模式中，猎人距离猎物 200 格外死亡时保留装备，避免远处无意义掉装。
            boolean flashFarFromPrey = !room.getGameMode().isFlashTournament()
                    && plugin.getFlashModeManager().isHunterFarFromAllPrey(room, player, 200.0D);
            event.setKeepInventory(flashFarFromPrey);
            if (flashFarFromPrey) {
                event.getDrops().clear();
                player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§8§8§D§D§F§F✦ §b你距离猎物超过200格，本次死亡不会掉落装备。");
            } else {
                // 追踪指南针是模式工具，不管普通/闪光/赛事都不应该死亡掉落。
                event.getDrops().removeIf(drop -> drop != null
                        && (drop.getType() == Material.COMPASS || drop.getType() == Material.RECOVERY_COMPASS));
            }

            boolean tournament = room.getGameMode().isFlashTournament();

            // 猎人死亡扣分 -2
            if (!tournament) {
                plugin.getPlayerDataManager().addHunterPoints(player.getUniqueId(), -2, room.getGameMode());
            }

            // 如果凶手是猎物，统计猎物击杀猎人次数（每10次+1猎物积分）
            Player hunterKiller = resolveDeathKiller(player, room);
            if (hunterKiller != null && room.isPrey(hunterKiller.getUniqueId())) {
                if (!tournament) {
                    boolean rewarded = plugin.getPlayerDataManager().incrementPreyKillHunter(hunterKiller.getUniqueId(), room.getGameMode());
                    if (rewarded) {
                        hunterKiller.playSound(hunterKiller.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        int total = plugin.getPlayerDataManager().getPlayerData(hunterKiller.getUniqueId()).getPreyKillHunterTotal();
                        Map<String, String> ph = new HashMap<>();
                        ph.put("count", String.valueOf(total));
                        hunterKiller.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("points.prey_kill_hunter_reward", ph));
                    }
                }
                // 击杀特效已在死亡事件入口统一播放，避免分支差异导致漏播。
            }

            // 复活后重新给指南针（延迟等复活完成）
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (room.getState() == org.gamefunxiao.game.RoomState.PLAYING && room.isHunter(player.getUniqueId())) {
                    // 检查背包里是否已有指南针
                    boolean hasCompass = false;
                    for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.COMPASS) {
                            hasCompass = true;
                            break;
                        }
                    }
                    if (!hasCompass) {
                        plugin.getGameManager().giveHunterItems(player, room);
                    }
                }
            }, 40L); // 延迟2秒等复活完成

            // 如果有修饰符，给TP机会
            if (room.hasModifier("HunterTPOnDeath")) {
                int hunterCount = 0;
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    if (room.isHunter(uuid)) {
                        hunterCount++;
                    }
                }

                if (hunterCount >= 2) {
                    forcedHunterTpSelection.add(player.getUniqueId());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.tp_on_death"));
                        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        new org.gamefunxiao.menu.hunter.TeleportTeammateMenu(plugin, player, room, true).open();
                    }, 2L);
                } else {
                    player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c猎人死亡TP修饰符启用失败：当前猎人数量不足2人。");
                }
            }
        }
    }

    private void handleEndFlashDeath(PlayerDeathEvent event, Player player, GameRoom room) {
        Location deathLoc = player.getLocation().clone();
        Location respawnWaitLoc = getEndFlashSpectatorWaitingLocation(room, deathLoc);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        room.setPendingRespawnLocation(player.getUniqueId(), respawnWaitLoc.clone());

        Player killer = resolveDeathKiller(player, room);
        if (room.isHunter(player.getUniqueId()) && isEndFlashHunterRespawnWaiting(player.getUniqueId())) {
            int remainingSeconds = getEndFlashHunterRespawnRemainingSeconds(player.getUniqueId());
            respawnEndFlashDeadPlayerNextTick(player);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> enterEndFlashHunterRespawnWait(player, room, respawnWaitLoc, remainingSeconds, false), 3L);
            return;
        }

        if (room.isPrey(player.getUniqueId())) {
            plugin.getPlayerDataManager().addPreyPoints(player.getUniqueId(), -5, room.getGameMode());
            if (killer != null && room.isHunter(killer.getUniqueId())) {
                plugin.getPlayerDataManager().incrementHunterKills(killer.getUniqueId(), room.getGameMode());
                plugin.getPlayerDataManager().addHunterPoints(killer.getUniqueId(), 3, room.getGameMode());
                room.setVictoryEffectTrigger(killer.getUniqueId(), deathLoc.clone());
                Map<String, String> killMsg = new HashMap<>();
                killMsg.put("player", player.getName());
                killMsg.put("points", "+3");
                killer.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("kill.hunter_kill_prey", killMsg));
                // 击杀特效已在死亡事件入口统一播放，避免终章闪光重复播。
            }

            boolean lastPrey = countAliveEndFlashPrey(room, player.getUniqueId()) <= 0;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    player.spigot().respawn();
                } catch (Exception ignored) {
                }
            }, 1L);

            if (lastPrey) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (room.getState() == RoomState.PLAYING) {
                        launchEndFlashSpectator(player);
                        if (room.getState() == RoomState.PLAYING) {
                            plugin.getGameManager().endGame(room, false);
                        }
                    }
                }, 6L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> convertEndFlashDefeatedPrey(player, room), 6L);
            }
            return;
        }

        plugin.getPlayerDataManager().addHunterPoints(player.getUniqueId(), -2, room.getGameMode());
        if (killer != null && room.isPrey(killer.getUniqueId())) {
            // 击杀特效已在死亡事件入口统一播放，避免终章闪光重复播。
        }
        int respawnSeconds = ThreadLocalRandom.current().nextInt(90, 231);
        endFlashHunterRespawnUntil.put(player.getUniqueId(), System.currentTimeMillis() + respawnSeconds * 1000L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Exception ignored) {
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enterEndFlashHunterRespawnWait(player, room, respawnWaitLoc, respawnSeconds, true);
        }, 3L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> respawnEndFlashHunter(player, room, deathLoc), respawnSeconds * 20L);
    }

    private void respawnEndFlashDeadPlayerNextTick(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Exception ignored) {
            }
        }, 1L);
    }

    private void playHunterGameKillEffect(Player killer, Location deathLocation) {
        if (killer == null || deathLocation == null) {
            return;
        }
        try {
            plugin.getGameManager().playHunterKillEffect(killer, deathLocation);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("猎人游戏击杀特效播放失败: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void enterEndFlashHunterRespawnWait(Player player, GameRoom room, Location waitLoc, int respawnSeconds, boolean announce) {
        if (player == null || !player.isOnline() || room == null || room.getState() != RoomState.PLAYING
                || room.getGameMode() != GameMode.END_FLASH || !room.isHunter(player.getUniqueId())) {
            return;
        }
        Location safeWaitLoc = getEndFlashSpectatorWaitingLocation(room, waitLoc == null ? player.getLocation() : waitLoc);
        if (safeWaitLoc.getWorld() != null && (!player.getWorld().equals(safeWaitLoc.getWorld())
                || player.getLocation().distanceSquared(safeWaitLoc) > 16.0D)) {
            player.teleport(safeWaitLoc);
        }
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), 20));
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        Vector velocity = player.getLocation().getDirection();
        if (velocity.lengthSquared() < 0.0001D) {
            velocity = new Vector(0.0D, 0.0D, 1.0D);
        }
        player.setVelocity(velocity.normalize().multiply(0.82D).setY(0.76D));
        if (announce) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§B§B§8§8§F§F✦ §d终章猎人死亡，§f将在 §e" + formatCompactTime(respawnSeconds) + " §f后复活。");
        }
        startEndFlashHunterRespawnCountdown(player, room, Math.max(1, respawnSeconds));
    }

    private void startEndFlashHunterRespawnCountdown(Player player, GameRoom room, int respawnSeconds) {
        if (player == null || room == null || respawnSeconds <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        cancelEndFlashHunterRespawnCountdown(playerId);
        long respawnAt = System.currentTimeMillis() + respawnSeconds * 1000L;
        endFlashHunterRespawnUntil.put(playerId, respawnAt);

        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            GameRoom currentRoom = online == null ? null : plugin.getRoomManager().getPlayerRoom(playerId);
            if (online == null || !online.isOnline()
                    || currentRoom == null
                    || !currentRoom.getRoomId().equals(room.getRoomId())
                    || currentRoom.getState() != RoomState.PLAYING
                    || currentRoom.getGameMode() != GameMode.END_FLASH
                    || !currentRoom.isHunter(playerId)
                    || online.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                cancelEndFlashHunterRespawnCountdown(playerId);
                return;
            }

            long remainingMillis = respawnAt - System.currentTimeMillis();
            if (remainingMillis <= 0L) {
                online.sendActionBar("§x§5§5§F§F§A§A⏳ §a终章复活 §f0 : 00");
                cancelEndFlashHunterRespawnCountdown(playerId);
                return;
            }

            long remainingSeconds = (remainingMillis + 999L) / 1000L;
            online.sendActionBar("§x§B§B§8§8§F§F⏳ §d终章复活倒计时 §f" + formatActionBarCountdown(remainingSeconds));
        }, 0L, 20L);
        endFlashHunterRespawnCountdownTasks.put(playerId, task);
    }

    public boolean isEndFlashHunterRespawnWaiting(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        Long until = endFlashHunterRespawnUntil.get(playerId);
        return until != null && until > System.currentTimeMillis();
    }

    private int getEndFlashHunterRespawnRemainingSeconds(UUID playerId) {
        if (playerId == null) {
            return 1;
        }
        Long until = endFlashHunterRespawnUntil.get(playerId);
        if (until == null) {
            return 1;
        }
        return (int) Math.max(1L, (until - System.currentTimeMillis() + 999L) / 1000L);
    }

    private void cancelEndFlashHunterRespawnCountdown(UUID playerId) {
        if (playerId == null) {
            return;
        }
        org.bukkit.scheduler.BukkitTask task = endFlashHunterRespawnCountdownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        endFlashHunterRespawnUntil.remove(playerId);
    }

    private String formatActionBarCountdown(long seconds) {
        long minutes = Math.max(0L, seconds) / 60L;
        long rest = Math.max(0L, seconds) % 60L;
        return minutes + " : " + String.format("%02d", rest);
    }

    private int countAliveEndFlashPrey(GameRoom room, UUID excluding) {
        int count = 0;
        for (UUID uuid : room.getPreyUUIDs()) {
            if (uuid.equals(excluding) || room.isSpectator(uuid)) {
                continue;
            }
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null && prey.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private void convertEndFlashDefeatedPrey(Player player, GameRoom room) {
        if (player == null || room == null || !player.isOnline() || room.getState() != RoomState.PLAYING
                || !room.isPrey(player.getUniqueId())) {
            return;
        }
        room.removePlayer(player.getUniqueId());
        room.addSpectator(player.getUniqueId());
        launchEndFlashSpectator(player);
        plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
        plugin.getRoomManager().refreshPlayerVisibility();
        plugin.getGameManager().giveSpectatorItems(player);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§8§8§B§B✦ §d终章中猎物死亡后不会复活，已进入旁观。");
    }

    private void launchEndFlashSpectator(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        Vector velocity = player.getLocation().getDirection();
        if (velocity.lengthSquared() < 0.0001D) {
            velocity = new Vector(0.0D, 0.0D, 1.0D);
        }
        player.setVelocity(velocity.normalize().multiply(0.92D).setY(0.82D));
    }

    private void respawnEndFlashHunter(Player player, GameRoom room, Location deathLoc) {
        if (player == null || !player.isOnline() || room == null || room.getState() != RoomState.PLAYING
                || !room.isHunter(player.getUniqueId())) {
            if (player != null) {
                cancelEndFlashHunterRespawnCountdown(player.getUniqueId());
            }
            return;
        }
        cancelEndFlashHunterRespawnCountdown(player.getUniqueId());
        Location target = getEndFlashSafeRespawnLocation(room, deathLoc);
        player.teleport(target);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setNoDamageTicks(0);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
        plugin.getGameManager().syncEndDimensionBrightness(player, room);
        if (!hasCompass(player)) {
            plugin.getGameManager().giveHunterItems(player, room);
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.92f, 1.18f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.52f, 1.45f);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✦ §a你已在终章闪光复活，本次复活没有无敌时间。");
    }

    private boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return true;
            }
        }
        return false;
    }

    private String formatCompactTime(int seconds) {
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (minutes <= 0) {
            return rest + "秒";
        }
        return minutes + "分" + (rest == 0 ? "" : rest + "秒");
    }

    public void suppressAdvancementMessages(UUID playerId, long ticks) {
        if (playerId == null) {
            return;
        }
        long millis = Math.max(1000L, ticks * 50L);
        advancementMessageSuppressUntil.put(playerId, System.currentTimeMillis() + millis);
    }

    private boolean shouldSuppressAdvancementMessage(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long until = advancementMessageSuppressUntil.getOrDefault(playerId, 0L);
        if (until <= now) {
            advancementMessageSuppressUntil.remove(playerId);
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (shouldSuppressAdvancementMessage(player.getUniqueId())) {
            event.message(null);
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            return;
        }

        // 所有 GameFun 模式里都不显示成就提示：不走全服公告，也不转播到房间。
        event.message(null);
    }

    private Component withHunterGamePrefix(Component message) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getConfigManager().getHunterGamePrefix() + "§r")
                .append(message);
    }

    private Component withLuckyPillarsPrefix(Component message) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getConfigManager().getLuckyPillarsPrefix() + "§r")
                .append(message);
    }

    private void broadcastRoomComponent(GameRoom room, Component message) {
        if (room == null || message == null) {
            return;
        }

        Set<UUID> receivers = new HashSet<>();
        receivers.addAll(room.getAllPlayerUUIDs());
        receivers.addAll(room.getSpectators());
        for (UUID uuid : receivers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    private void convertDefeatedPreyToSpectator(Player player, GameRoom room) {
        if (player == null || room == null || !player.isOnline()) {
            return;
        }

        if (!room.isPrey(player.getUniqueId()) || room.getState() != RoomState.PLAYING) {
            return;
        }

        room.removePlayer(player.getUniqueId());
        room.addSpectator(player.getUniqueId());

        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
        plugin.getRoomManager().refreshPlayerVisibility();
        plugin.getGameManager().giveSpectatorItems(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerStartSpectating(PlayerStartSpectatingEntityEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null || !room.isSpectator(player.getUniqueId())) {
            return;
        }

        if (!(event.getNewSpectatorTarget() instanceof Player target)) {
            return;
        }

        GameRoom targetRoom = plugin.getRoomManager().getPlayerRoom(target.getUniqueId());
        if (targetRoom == null || !room.getRoomId().equals(targetRoom.getRoomId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.spectator_target_room_only"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player viewer)) {
            return;
        }
        event.getCompletions().removeIf(completion -> {
            Player target = Bukkit.getPlayerExact(completion);
            return target != null && !plugin.getRoomManager().canSeeInRoomTab(viewer, target);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChatPlayer(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (InvitePlayerMenu.isWaitingSearchInput(player.getUniqueId())) {
            event.setCancelled(true);
            String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            Bukkit.getScheduler().runTask(plugin, () -> InvitePlayerMenu.handleSearchChatInput(plugin, player, plainContent));
            return;
        }
        if (EndFlashKitDetailMenu.isWaitingRenameInput(player.getUniqueId())) {
            event.setCancelled(true);
            String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            Bukkit.getScheduler().runTask(plugin, () -> EndFlashKitDetailMenu.handleRenameChatInput(plugin, player, plainContent));
            return;
        }
        if (EndFlashKitDetailMenu.isWaitingGuideInput(player.getUniqueId())) {
            event.setCancelled(true);
            String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            Bukkit.getScheduler().runTask(plugin, () -> EndFlashKitDetailMenu.handleGuideChatInput(plugin, player, plainContent));
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());

        if (room != null) {
            if (room.getGameMode().isBrickGuard() && plugin.getBrickGuardManager().handleAsyncChat(event, player, room)) {
                return;
            }
            if (isFlashTournamentStartedRoom(room)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendActionBar("§c赛事正式开始后只能使用 §f/teammsg <内容> §c或简单语音聊天");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.38f, 0.72f);
                });
                return;
            }
            if (plugin.getConfigManager().isRoomChatIsolationEnabled()) {
                event.setCancelled(true);

                String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
                String message;
                if (room.getGameMode().isLuckyPillars()) {
                    String stagePrefix;
                    if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
                        stagePrefix = "§6[幸运大厅] ";
                    } else if (room.isSpectator(player.getUniqueId()) || room.isLuckyPillarsEliminated(player.getUniqueId())) {
                        stagePrefix = "§7[旁观] ";
                    } else {
                        stagePrefix = "";
                    }
                    String roomPrefix = "§8[§x§F§F§D§D§5§5幸§x§F§F§C§C§6§6运§x§F§F§B§B§7§7之§x§F§F§A§A§8§8柱§8] ";
                    message = plugin.getConfigManager().getLuckyPillarsPrefix()
                            + roomPrefix + stagePrefix + "§f" + player.getName() + "§7: §f" + plainContent;
                } else {
                    String rolePrefix;
                    if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
                        rolePrefix = "§x§5§5§F§F§A§A[等待] ";
                    } else if (room.getState() == RoomState.SELECTING) {
                        rolePrefix = "§x§8§8§D§D§F§F[选择] ";
                    } else if (room.isPrey(player.getUniqueId())) {
                        rolePrefix = "§a[猎物] ";
                    } else if (room.isSpectator(player.getUniqueId())) {
                        rolePrefix = "§7[旁观] ";
                    } else {
                        rolePrefix = "§c[猎人] ";
                    }

                    if (room.getGameMode().isFlashTournament()) {
                        message = "§f" + player.getName() + "§7: §f" + plainContent;
                    } else {
                        String gamePrefix = "§8[§x§8§9§F§8§A§B" + room.getModeName() + "§8] ";
                        message = plugin.getConfigManager().getHunterGamePrefix() +
                                gamePrefix + rolePrefix + "§f" + player.getName() + "§7: §f" + plainContent;
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Set<UUID> receivers = new HashSet<>();
                    receivers.addAll(room.getAllPlayerUUIDs());
                    receivers.addAll(room.getSpectators());
                    for (UUID uuid : receivers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.sendMessage(message);
                        }
                    }
                });
            }
            return;
        }

        Set<UUID> endingReceivers = plugin.getRoomManager().getEndingChatReceivers(player.getUniqueId());
        if (!endingReceivers.isEmpty() && plugin.getConfigManager().isRoomChatIsolationEnabled()) {
            event.setCancelled(true);
            String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            String modeName = plugin.getRoomManager().getEndingChatModeName(player.getUniqueId());
            if (modeName == null || modeName.isBlank()) {
                modeName = "结算中";
            }
            boolean luckyEnding = modeName.contains("幸运之柱");
            String gamePrefix = luckyEnding
                    ? "§8[§x§F§F§D§D§5§5幸§x§F§F§C§C§6§6运§x§F§F§B§B§7§7结§x§F§F§A§A§8§8算§8] "
                    : "§8[§x§8§9§F§8§A§B" + modeName + "§8] ";
            String basePrefix = luckyEnding
                    ? plugin.getConfigManager().getLuckyPillarsPrefix()
                    : plugin.getConfigManager().getHunterGamePrefix();
            String settlePrefix = luckyEnding ? "§6[终局] " : "§x§F§F§D§D§7§7[结算] ";
            String message = basePrefix + gamePrefix + settlePrefix + "§f" + player.getName() + "§7: §f" + plainContent;

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : endingReceivers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(message);
                    }
                }
            });
            return;
        }

        event.viewers().removeIf(audience -> {
            if (!(audience instanceof Player target)) {
                return false;
            }
            return plugin.getRoomManager().isChatIsolated(target.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameRoom room = resolveManagedRoom(player, event.getBlock().getWorld());

        if (plugin.getGameManager().handleLuckyPillarBreak(event, player, room)) {
            return;
        }
        if (plugin.getBrickGuardManager().handleBlockBreak(event, player, room)) {
            return;
        }

        if (room != null && room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            cancelBlockBreakAndResync(event, player);
            return;
        }

        if (plugin.getFlashModeManager().handleTmtBlockBreak(event, player, room)) {
            return;
        }

        if (plugin.getFlashModeManager().handleCoalPickaxeFireTrapBreak(event, player, room)) {
            return;
        }

        if (plugin.getFlashModeManager().handlePseudoPoisonPotatoCropBreak(event, player, room)) {
            return;
        }

        if (shouldLockWorldInteraction(player, event.getBlock().getWorld(), room)) {
            cancelBlockBreakAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameRoom room = resolveManagedRoom(player, event.getBlockPlaced().getWorld());

        if (plugin.getBrickGuardManager().handleBlockPlace(event, player, room)) {
            return;
        }

        if (room != null && room.getState() == RoomState.PLAYING && room.getGameMode().isLuckyPillars()
                && isLuckyPillarsForbiddenPlaceMaterial(event.getBlockPlaced().getType())) {
            cancelBlockPlaceAndResync(event, player);
            return;
        }

        if (plugin.getFlashModeManager().handleTmtBlockPlace(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashCoarseDirtPlace(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleFlashWetFarmlandCropPlace(event, player, room)) {
            return;
        }

        if (room != null && room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            cancelBlockPlaceAndResync(event, player);
            return;
        }

        if (shouldLockWorldInteraction(player, event.getBlockPlaced().getWorld(), room)) {
            cancelBlockPlaceAndResync(event, player);
            return;
        }
        plugin.getFlashModeManager().handleFlashPlayerBlockPlace(event, player, room);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        GameRoom room = resolveManagedRoom(player, event.getBlock().getWorld());
        if (room != null && room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            cancelBucketEmptyAndResync(event, player);
            return;
        }
        if (shouldLockWorldInteraction(player, event.getBlock().getWorld(), room)) {
            cancelBucketEmptyAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        GameRoom room = resolveManagedRoom(player, event.getBlock().getWorld());
        if (room != null && room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            cancelBucketFillAndResync(event, player);
            return;
        }
        if (shouldLockWorldInteraction(player, event.getBlock().getWorld(), room)) {
            cancelBucketFillAndResync(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        GameRoom room = resolveManagedRoom(player, event.getBlock().getWorld());
        if (shouldLockWorldInteraction(player, event.getBlock().getWorld(), room)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    private boolean isLuckyPillarsForbiddenPlaceMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.equals("COMMAND_BLOCK")
                || name.equals("CHAIN_COMMAND_BLOCK")
                || name.equals("REPEATING_COMMAND_BLOCK")
                || name.equals("STRUCTURE_BLOCK")
                || name.equals("STRUCTURE_VOID")
                || name.equals("JIGSAW")
                || name.equals("BARRIER")
                || name.equals("LIGHT")
                || name.equals("BEDROCK")
                || name.equals("END_PORTAL_FRAME")
                || name.equals("SPAWNER")
                || name.equals("TRIAL_SPAWNER")
                || name.equals("VAULT");
    }

    private void cancelBlockBreakAndResync(BlockBreakEvent event, Player player) {
        event.setCancelled(true);
        if (player == null) {
            return;
        }
        Location location = event.getBlock().getLocation().clone();
        org.bukkit.block.data.BlockData blockData = event.getBlock().getBlockData();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendBlockChange(location, blockData);
            player.updateInventory();
        });
    }

    private void cancelBlockPlaceAndResync(BlockPlaceEvent event, Player player) {
        event.setCancelled(true);
        if (player == null) {
            return;
        }
        Location location = event.getBlockPlaced().getLocation().clone();
        org.bukkit.block.data.BlockData blockData = event.getBlockReplacedState().getBlockData();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendBlockChange(location, blockData);
            player.updateInventory();
        });
    }

    private void cancelBucketEmptyAndResync(PlayerBucketEmptyEvent event, Player player) {
        event.setCancelled(true);
        if (player == null) {
            return;
        }
        Location placedLocation = event.getBlock().getLocation().clone();
        org.bukkit.block.data.BlockData placedBlockData = event.getBlock().getBlockData();
        Location clickedLocation = event.getBlockClicked().getLocation().clone();
        org.bukkit.block.data.BlockData clickedBlockData = event.getBlockClicked().getBlockData();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendBlockChange(placedLocation, placedBlockData);
            player.sendBlockChange(clickedLocation, clickedBlockData);
            player.updateInventory();
        });
    }

    private void cancelBucketFillAndResync(PlayerBucketFillEvent event, Player player) {
        event.setCancelled(true);
        if (player == null) {
            return;
        }
        Location location = event.getBlock().getLocation().clone();
        org.bukkit.block.data.BlockData blockData = event.getBlock().getBlockData();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendBlockChange(location, blockData);
            player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrePlayerAttackEntity(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        rememberHunterGameQuickSwapAttack(player, room);
        plugin.getFlashModeManager().prepareSpearKineticThreshold(player, room);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityAttemptSmashAttack(EntityAttemptSmashAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        ItemStack weapon = event.getWeapon();
        if (!isHunterGameSpear(weapon)) {
            return;
        }
        int slot = findQuickSwapHotbarSlot(player, weapon);
        if (slot < 0) {
            slot = player.getInventory().getHeldItemSlot();
        }
        rememberHunterGameQuickSwapAttack(player, room, slot, weapon);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (!isActiveHunterGameQuickSwapRoom(player, room)) {
            return;
        }
        if (!isHunterGameLungeVelocity(event.getVelocity())) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!isHunterGameSpear(weapon)) {
            return;
        }
        rememberHunterGameQuickSwapAttack(player, room, player.getInventory().getHeldItemSlot(), weapon);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (!isActiveHunterGameQuickSwapRoom(player, room)) {
            return;
        }

        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        if (isHunterGameSpear(previous) && isHunterGameLungeVelocity(player.getVelocity())) {
            rememberHunterGameQuickSwapAttack(player, room, event.getPreviousSlot(), previous);
        }

        ItemStack next = player.getInventory().getItem(event.getNewSlot());
        if (isHunterGameSpear(next)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                GameRoom activeRoom = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
                plugin.getFlashModeManager().prepareSpearKineticThreshold(player, activeRoom);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttackingPlayer(event.getDamager());
        if (attacker != null && !(event.getEntity() instanceof Player)) {
            if (shouldProtectLobbyEntity(attacker, event.getEntity().getWorld())) {
                event.setCancelled(true);
                attacker.playSound(attacker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.7f, 0.8f);
                return;
            }
        }

        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity livingVictim)) {
            return;
        }

        GameRoom attackerRoom = attacker == null ? null : plugin.getRoomManager().getPlayerRoom(attacker.getUniqueId());
        restoreHunterGameQuickSwapAttackWeapon(attacker, attackerRoom);
        if (!(livingVictim instanceof Player victim)) {
            if (attacker != null && plugin.getFlashModeManager().isFlashCombatAvailable(attacker)) {
                plugin.getFlashModeManager().applyMeleeUpgradeBonus(event, attacker, livingVictim, attackerRoom);
                plugin.getFlashModeManager().rememberOwnerCombatTarget(attacker, livingVictim);
            }
            return;
        }
        plugin.getGameManager().handleLuckyPillarsDamage(event);
        plugin.getBrickGuardManager().handleDamage(event);
        if (attacker == null) return;

        GameRoom room = plugin.getRoomManager().getPlayerRoom(victim.getUniqueId());
        if (room == null) {
            room = attackerRoom;
        }
        restoreHunterGameQuickSwapAttackWeapon(attacker, room);
        if (room == null) {
            if (plugin.getFlashModeManager().isFlashCombatAvailable(attacker)) {
                plugin.getFlashModeManager().applyMeleeUpgradeBonus(event, attacker, victim, null);
                plugin.getFlashModeManager().rememberOwnerCombatTarget(attacker, victim);
            }
            return;
        }

        // 在大厅中禁止PVP
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
            event.setCancelled(true);
            return;
        }

        // 在世界选择阶段禁止PVP
        if (room.getState() == RoomState.SELECTING) {
            event.setCancelled(true);
            return;
        }

        // 游戏已结束或任何非正式游戏阶段都不允许玩家互相造成伤害，避免结算倒计时/下把准备时继承伤害状态。
        if (room.getState() != RoomState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        // 在游戏开始阶段（PLAYING状态但游戏还没正式开始），禁止PVP
        if (room.getState() == RoomState.PLAYING) {
            if (!room.isGameActuallyStarted()) {
                if (room.getGameMode().isFlashTournament()
                        && room.isHunter(attacker.getUniqueId())
                        && room.isPrey(victim.getUniqueId())) {
                    event.setCancelled(false);
                    event.setDamage(0.0D);
                    pushTournamentPreyWithoutDamage(attacker, victim);
                    return;
                }
                event.setCancelled(true);
                return;
            }

            if (room.getGameMode().isLuckyPillars()) {
                if (room.isLuckyPillarsEliminated(attacker.getUniqueId())
                        || room.isLuckyPillarsEliminated(victim.getUniqueId())) {
                    event.setCancelled(true);
                } else if (!attacker.getUniqueId().equals(victim.getUniqueId()) && event.getDamage() > 0.0D) {
                    rememberRecentCombatHit(victim, attacker, room);
                }
                return;
            }

            if (room.getGameMode().isStandaloneMiniGame()) {
                event.setCancelled(true);
                return;
            }

            // 猎人攻击猎人的友伤判断
            if (room.isHunter(attacker.getUniqueId()) && room.isHunter(victim.getUniqueId())) {
                boolean friendlyFire = plugin.getConfigManager().getConfig().getBoolean("hunter_game.pvp.hunter_friendly_fire", false);
                if (!friendlyFire && !room.getGameMode().isFlashTournament()) {
                    plugin.getFlashModeManager().applySwordPotionOnly(event, attacker, victim, room);
                    // 不造成伤害，但保留攻击动作（击退等效果）
                    event.setDamage(0);
                    return;
                }
            }

            // 游戏正式开始后，追踪攻击猎物的统计数据
            if (room.isPrey(victim.getUniqueId()) && room.isHunter(attacker.getUniqueId())) {
                room.addAttack(attacker.getUniqueId());
                room.addDamage(attacker.getUniqueId(), event.getFinalDamage());
            }

            if (room.hasModifier("ThunderStorm") && room.isGameActuallyStarted()) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
            }

            plugin.getFlashModeManager().applyMeleeUpgradeBonus(event, attacker, victim, room);
            plugin.getFlashModeManager().rememberOwnerCombatTarget(attacker, victim);
            if (!event.isCancelled() && event.getDamage() > 0.0D) {
                plugin.getFlashModeManager().rememberOwnerCombatTarget(victim, attacker);
                rememberRecentCombatHit(victim, attacker, room);
            }
        }
    }

    private void rememberRecentCombatHit(Player victim, Player attacker, GameRoom room) {
        if (victim == null || attacker == null || room == null
                || victim.getUniqueId().equals(attacker.getUniqueId())
                || !room.getAllPlayerUUIDs().contains(victim.getUniqueId())
                || !room.getAllPlayerUUIDs().contains(attacker.getUniqueId())) {
            return;
        }
        recentCombatHits.put(victim.getUniqueId(), new RecentCombatHit(attacker.getUniqueId(), room.getRoomId(), System.currentTimeMillis()));
    }

    private Player resolveRecentCombatKiller(Player victim, GameRoom room) {
        if (victim == null || room == null) {
            return null;
        }
        RecentCombatHit hit = recentCombatHits.get(victim.getUniqueId());
        if (hit == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - hit.timeMillis() > RECENT_COMBAT_HIT_EXPIRE_MS || !room.getRoomId().equals(hit.roomId())) {
            recentCombatHits.remove(victim.getUniqueId());
            return null;
        }
        Player attacker = Bukkit.getPlayer(hit.attackerUuid());
        if (attacker == null || !attacker.isOnline() || !room.getAllPlayerUUIDs().contains(attacker.getUniqueId())) {
            return null;
        }
        return attacker;
    }

    private Player resolveDeathKiller(Player victim, GameRoom room) {
        if (victim == null) {
            return null;
        }
        Player killer = victim.getKiller();
        if (killer != null) {
            return killer;
        }
        return resolveRecentCombatKiller(victim, room);
    }

    private Player resolveAttackingPlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean shouldProtectLobbyEntity(Player attacker, World world) {
        if (world == null) {
            return false;
        }

        GameRoom room = resolveManagedRoom(attacker, world);
        return shouldLockWorldInteraction(attacker, world, room);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerVoidDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 检查是否是虚空伤害
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) return;

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) return;

        if (room.getGameMode().isBrickGuard() && plugin.getBrickGuardManager().handleVoidDamage(room, player)) {
            event.setCancelled(true);
            return;
        }

        if (room.getGameMode().isStandaloneMiniGame() && room.getState() == RoomState.PLAYING) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            plugin.getGameManager().eliminateStandaloneMiniGamePlayer(room, player, player.getLocation(), "§c掉出了竞技场");
            return;
        }

        if (room.getGameMode() == GameMode.END_FLASH && room.isHunter(player.getUniqueId())
                && isEndFlashHunterRespawnWaiting(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            Location safe = getEndFlashSpectatorWaitingLocation(room, player.getLocation());
            if (safe.getWorld() != null) {
                player.teleport(safe);
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            return;
        }

        if (room.getState() == RoomState.SELECTING && room.isPrey(player.getUniqueId())) {
            event.setCancelled(true);
            returnWorldSelectionPrey(player, room, player.getLocation(), true);
            return;
        }

        // 在大厅中掉入虚空时，传送回出生点
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
            event.setCancelled(true);

            // 获取大厅世界
            org.bukkit.World lobbyWorld = plugin.getWorldManager().getLobbyWorld(room.getRoomId());
            if (lobbyWorld != null) {
                // 重置摔落距离，防止摔伤
                player.setFallDistance(0);
                player.teleport(lobbyWorld.getSpawnLocation());
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFallDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 检查是否是摔落伤害
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) return;

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        plugin.getFlashModeManager().handleSilentBootFallDamage(event, player, room);
        if (room == null) return;

        if (room.getGameMode().isBrickGuard() && plugin.getBrickGuardManager().shouldCancelFallDamage(room, player)) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }

        // 在大厅中禁止摔落伤害
        if (room.getState() == RoomState.WAITING
                || room.getState() == RoomState.STARTING
                || room.getState() == RoomState.SELECTING
                || (room.getState() == RoomState.PLAYING && !room.isGameActuallyStarted())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }

        // 在世界选择阶段，猎物无敌（禁止所有伤害）
        if (room.getState() == RoomState.SELECTING && room.isPrey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) return;

        if (room.getGameMode() == GameMode.END_FLASH && room.isHunter(player.getUniqueId())
                && isEndFlashHunterRespawnWaiting(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }

        if (room.getGameMode().isStandaloneMiniGame()) {
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) {
                return;
            }
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }

        // 在世界选择阶段，猎物无敌（禁止所有伤害）
        if (room.getState() == RoomState.SELECTING && room.isPrey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (InvitePlayerMenu.isWaitingSearchInput(player.getUniqueId())
                || EndFlashKitDetailMenu.isWaitingRenameInput(player.getUniqueId())
                || EndFlashKitDetailMenu.isWaitingGuideInput(player.getUniqueId())) {
            org.bukkit.Location to = event.getTo();
            if (to != null) {
                org.bukkit.Location from = event.getFrom();
                if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                    to.setX(from.getX());
                    to.setY(from.getY());
                    to.setZ(from.getZ());
                    event.setTo(to);
                }
            }
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());

        org.bukkit.Location to = event.getTo();
        if (to == null) return;
        if (room == null) {
            if (plugin.getFlashModeManager().isFlashCombatAvailable(player)) {
                plugin.getFlashModeManager().handleHappyGhastMountedSpeed(player);
                plugin.getFlashModeManager().handleFlashTamedMountControl(event, player, null);
                plugin.getFlashModeManager().handleUnstableMaceDisplacementLock(event, player, null);
                plugin.getFlashModeManager().handleFlashHoeTrapMove(event, player, null);
                plugin.getFlashModeManager().handleFlashFishingWaterTrapMove(event, player, null);
                plugin.getFlashModeManager().handleSilentBootStep(event, player, null);
                plugin.getFlashModeManager().handleShieldWindChargeAirBounceLanding(player, null);
            }
            return;
        }

        if (isFlashExitPortalMove(room, to)) {
            event.setCancelled(true);
            event.setTo(event.getFrom());
            if (startEndFlashExitPortalAction(player, room)) {
                handleFlashExitPortalCompletion(player, room);
            }
            return;
        }

        if (isSwapHoldingPlayer(room, player)) {
            org.bukkit.Location holding = room.getGameWorld() == null
                    ? event.getFrom()
                    : room.getGameWorld().getSpawnLocation().clone().add(0, 180, 0);
            if (room.getGameWorld() != null) {
                double maxY = room.getGameWorld().getMaxHeight() - 5;
                if (holding.getY() > maxY) {
                    holding.setY(maxY);
                }
                holding.setYaw(to.getYaw());
                holding.setPitch(to.getPitch());
                if (!player.getWorld().equals(holding.getWorld()) || player.getLocation().distanceSquared(holding) > 6.25D) {
                    player.teleport(holding);
                }
            }
            event.setTo(holding);
            return;
        }

        if (handleFlashTournamentWarmupMove(event, player, room)) {
            return;
        }

        if (plugin.getGameManager().handleStandaloneMiniGameMove(event, player, room)) {
            return;
        }

        if (plugin.getBrickGuardManager().handleMove(event, player, room)) {
            return;
        }

        // 只在世界选择阶段限制猎物的XZ移动
        if (room.getState() == RoomState.SELECTING) {
            // 只限制猎物的移动
            if (!room.isPrey(player.getUniqueId())) return;

            // 获取移动前后的位置
            org.bukkit.Location from = event.getFrom();

            // 获取世界出生点（世界可能还在生成中）
            if (room.getGameWorld() == null) return;
            org.bukkit.Location spawnLoc = room.getGameWorld().getSpawnLocation();

            if (shouldReturnWorldSelectionPrey(room, to, spawnLoc)) {
                org.bukkit.Location safe = getWorldSelectionPreySafeLocation(room, to);
                if (safe != null) {
                    player.setFallDistance(0.0F);
                    player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                    if (player.getWorld().equals(safe.getWorld())) {
                        event.setTo(safe);
                    } else {
                        player.teleport(safe);
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.58f, 1.32f);
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.26f, 1.88f);
                    return;
                }
            }

            // 计算XZ距离
            double distanceX = Math.abs(to.getX() - spawnLoc.getX());
            double distanceZ = Math.abs(to.getZ() - spawnLoc.getZ());

            // 如果XZ距离超过0.3格，限制移动
            if (distanceX > 0.3 || distanceZ > 0.3) {
                to.setX(from.getX());
                to.setZ(from.getZ());
                event.setTo(to);
            }

            // 启用禁止Y轴移动修饰符后，猎物不能上下移动
            if (room.hasModifier("NoYChange") && to.getY() != from.getY()) {
                to.setY(from.getY());
                event.setTo(to);
            }
        }

        // 在游戏开始阶段（PLAYING状态但游戏还没正式开始），禁止所有玩家移动
        if (room.getState() == RoomState.PLAYING) {
            if (!room.isGameActuallyStarted()) {
                player.setFallDistance(0.0F);
                // 游戏还没正式开始，禁止所有玩家移动
                org.bukkit.Location from = event.getFrom();

                // 只允许Y轴移动（视角），禁止XZ移动
                if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                    to.setX(from.getX());
                    to.setZ(from.getZ());
                    event.setTo(to);
                }
            } else {
                plugin.getFlashModeManager().handleHappyGhastMountedSpeed(player);
                plugin.getFlashModeManager().handleFlashTamedMountControl(event, player, room);
                plugin.getFlashModeManager().handleUnstableMaceDisplacementLock(event, player, room);
                to = event.getTo();
                plugin.getFlashModeManager().handleFlashHoeTrapMove(event, player, room);
                plugin.getFlashModeManager().handleFlashFishingWaterTrapMove(event, player, room);
                plugin.getFlashModeManager().handleSilentBootStep(event, player, room);
                plugin.getFlashModeManager().handleShieldWindChargeAirBounceLanding(player, room);
                // 游戏正式开始后，追踪所有玩家的奔跑距离
                if (to != null) {
                    room.updateDistance(player.getUniqueId(), to);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null
                || !room.getGameMode().isFlashTournament()
                || room.getState() != RoomState.PLAYING
                || room.isGameActuallyStarted()
                || !room.isPrey(player.getUniqueId())) {
            return;
        }
        org.bukkit.Input input = event.getInput();
        if (input != null && (input.isForward() || input.isBackward() || input.isLeft() || input.isRight() || input.isJump())) {
            flashTournamentPreyInputUntil.put(player.getUniqueId(), System.currentTimeMillis() + 450L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            InvitePlayerMenu.cancelSearchInput(plugin, event.getPlayer(), true);
            EndFlashKitDetailMenu.cancelRenameInput(plugin, event.getPlayer(), true);
            EndFlashKitDetailMenu.cancelGuideInput(plugin, event.getPlayer(), true);
        }
    }

    private void rememberHunterGameQuickSwapAttack(Player player, GameRoom room) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        rememberHunterGameQuickSwapAttack(player, room, slot, weapon);
    }

    private void rememberHunterGameQuickSwapAttack(Player player, GameRoom room, int slot, ItemStack weapon) {
        if (!isActiveHunterGameQuickSwapRoom(player, room)) {
            return;
        }
        if (weapon == null || weapon.getType() == Material.AIR) {
            quickSwapAttackSnapshots.remove(player.getUniqueId());
            return;
        }
        ItemStack copy = weapon.clone();
        copy.setAmount(1);
        quickSwapAttackSnapshots.put(player.getUniqueId(),
                new QuickSwapAttackSnapshot(slot, copy, System.currentTimeMillis()));
    }

    private void restoreHunterGameQuickSwapAttackWeapon(Player player, GameRoom room) {
        if (!isActiveHunterGameQuickSwapRoom(player, room)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        QuickSwapAttackSnapshot snapshot = quickSwapAttackSnapshots.get(uuid);
        if (snapshot == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - snapshot.timeMillis() > 900L) {
            quickSwapAttackSnapshots.remove(uuid);
            return;
        }
        if (snapshot.slot() < 0 || snapshot.slot() > 8) {
            return;
        }
        ItemStack slotItem = player.getInventory().getItem(snapshot.slot());
        if (!isSameQuickSwapItem(slotItem, snapshot.weapon())) {
            return;
        }
        int currentSlot = player.getInventory().getHeldItemSlot();
        ItemStack current = player.getInventory().getItemInMainHand();
        if (currentSlot == snapshot.slot() && isSameQuickSwapItem(current, snapshot.weapon())) {
            return;
        }

        plugin.getFlashModeManager().markQuickSwapAttributeCorrection(player, snapshot.weapon());
        player.getInventory().setHeldItemSlot(snapshot.slot());
        scheduleQuickSwapHeldSlotRestore(player, currentSlot);
    }

    private boolean isSameQuickSwapItem(ItemStack current, ItemStack snapshot) {
        if (current == null || snapshot == null || current.getType() == Material.AIR || snapshot.getType() == Material.AIR) {
            return false;
        }
        ItemStack currentCopy = current.clone();
        currentCopy.setAmount(1);
        ItemStack snapshotCopy = snapshot.clone();
        snapshotCopy.setAmount(1);
        return currentCopy.isSimilar(snapshotCopy);
    }

    private void scheduleQuickSwapHeldSlotRestore(Player player, int desiredSlot) {
        if (player == null || desiredSlot < 0 || desiredSlot > 8) {
            return;
        }
        UUID uuid = player.getUniqueId();
        pendingQuickSwapRestoreSlots.put(uuid, desiredSlot);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Integer restoreSlot = pendingQuickSwapRestoreSlots.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (restoreSlot == null || online == null || !online.isOnline() || restoreSlot < 0 || restoreSlot > 8) {
                return;
            }
            GameRoom activeRoom = plugin.getRoomManager().getPlayerRoom(uuid);
            if (activeRoom == null || activeRoom.getState() != RoomState.PLAYING || !activeRoom.isGameActuallyStarted()) {
                return;
            }
            online.getInventory().setHeldItemSlot(restoreSlot);
        });
    }

    private boolean isActiveHunterGameQuickSwapRoom(Player player, GameRoom room) {
        return player != null
                && room != null
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted()
                && !room.isSpectator(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        GameRoom room = plugin.getRoomManager().getPlayerRoom(event.getPlayer().getUniqueId());
        if (plugin.getBrickGuardManager().handleEntityInteract(event, event.getPlayer(), room)) {
            return;
        }
        if (shouldCancelProtectedEntityInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (shouldCancelProtectedEntityInteract(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    private int findQuickSwapHotbarSlot(Player player, ItemStack snapshot) {
        if (player == null || snapshot == null || snapshot.getType() == Material.AIR) {
            return -1;
        }
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isSameQuickSwapItem(item, snapshot)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isHunterGameSpear(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_SPEAR");
    }

    private boolean isHunterGameLungeVelocity(Vector velocity) {
        if (velocity == null) {
            return false;
        }
        Vector horizontal = velocity.clone();
        horizontal.setY(0.0D);
        return horizontal.lengthSquared() >= 0.18D || velocity.lengthSquared() >= 0.30D;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            return;
        }

        if (room.getState() == RoomState.PLAYING
                && !room.isGameActuallyStarted()
                && room.isDualPreyStackLocked()
                && room.isAnyStackedPreyPassenger(player.getUniqueId())) {
            event.setCancelled(true);
            playRestrictionSound(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        org.bukkit.inventory.Inventory clickedInventory = event.getClickedInventory();

        // 自定义菜单点击统一交给 MenuListener 处理。
        // 这里如果继续套用房间 WAITING / SELECTING / 未开局锁背包逻辑，会把终章 Kit 投票、
        // 猎物 Kit 选择、个人 Kit 编辑等菜单的按钮二次拦截，表现成按钮点了没反应。
        if (event.getView().getTopInventory().getHolder() instanceof org.gamefunxiao.menu.base.BaseMenu
                || (clickedInventory != null && clickedInventory.getHolder() instanceof org.gamefunxiao.menu.base.BaseMenu)) {
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            if (plugin.getFlashModeManager().handleDragonBreathWeaponInfusion(event, player, null)) {
                return;
            }
            if (plugin.getFlashModeManager().handleMaterialUpgradeInfusion(event, player, null)) {
                return;
            }
            return;
        }

        if (isSwapHoldingPlayer(room, player)) {
            event.setCancelled(true);
            return;
        }

        plugin.getBrickGuardManager().handleInventoryClick(event, player, room);

        if (plugin.getFlashModeManager().handleFlashBackpackInventoryClick(event, player, room)) {
            return;
        }

        if (plugin.getFlashModeManager().handleDragonBreathWeaponInfusion(event, player, room)) {
            return;
        }
        if (plugin.getFlashModeManager().handleMaterialUpgradeInfusion(event, player, room)) {
            return;
        }

        if (isMountedActiveGameCraftingClick(event, player, room)) {
            // 坐船时原版玩家背包/工作台合成必须保持放行，避免残留菜单状态或容器保护把结果槽回滚。
            event.setCancelled(false);
            plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
            return;
        }

        if (isHunterTpLootLocked(player.getUniqueId())
                && clickedInventory != null
                && event.getView().getTopInventory().equals(clickedInventory)
                && !isCraftingView(event.getView().getTopInventory())
                && clickedInventory.getHolder() != player
                && !(clickedInventory.getHolder() instanceof org.gamefunxiao.menu.base.BaseMenu)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c猎人死亡TP后的5分钟内，暂时不能从容器中取出物品。");
            playRestrictionSound(player);
            return;
        }

        // 在大厅、世界选择阶段，禁止所有人移动物品
        if (room.getState() == RoomState.WAITING ||
            room.getState() == RoomState.STARTING ||
            room.getState() == RoomState.SELECTING) {
            event.setCancelled(true);
            return;
        }

        // 旁观者点击退出物品即退出
        if (room.isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PAPER && clicked.hasItemMeta()) {
                var meta = clicked.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 10008) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getRoomManager().leaveRoom(player));
                }
            }
            return;
        }

        if (room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            event.setCancelled(true);
            return;
        }

        // 在游戏开始阶段（PLAYING状态但游戏还没正式开始），禁止所有人移动物品
        if (room.getState() == RoomState.PLAYING) {
            if (!room.isGameActuallyStarted()) {
                if (room.getGameMode() == GameMode.END_FLASH && canEditEndFlashStartupInventory(event, player)) {
                    return;
                }
                // 其他模式仍保持锁定；终章·闪光只放行玩家自己的背包装备整理。
                event.setCancelled(true);
            }
            // 游戏正式开始后，允许所有玩家移动物品
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTopInventory().getHolder() instanceof org.gamefunxiao.menu.base.BaseMenu) {
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) return;

        if (room.getState() == RoomState.WAITING
                || room.getState() == RoomState.STARTING
                || room.getState() == RoomState.SELECTING
                || (room.getState() == RoomState.PLAYING && !room.isGameActuallyStarted())) {
            player.setFallDistance(0.0F);
        }

        if (room.getState() == RoomState.PLAYING && room.getGameMode().isStandaloneMiniGame()) {
            event.setCancelled(true);
            return;
        }

        if (isSwapHoldingPlayer(room, player)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getFlashModeManager().handleFlashBackpackInventoryDrag(event, player, room)) {
            return;
        }

        if (isMountedActiveGameCraftingDrag(event, player, room)) {
            // 坐船合成时，右键滑动物品进 2x2/3x3 合成格不能被残留菜单取消。
            event.setCancelled(false);
            plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
            return;
        }

        // 在大厅、世界选择阶段，禁止所有人拖拽物品
        if (room.getState() == RoomState.WAITING ||
            room.getState() == RoomState.STARTING ||
            room.getState() == RoomState.SELECTING) {
            event.setCancelled(true);
            return;
        }

        if (room.getState() == RoomState.PLAYING && !room.isGameActuallyStarted()) {
            if (room.getGameMode() == GameMode.END_FLASH && canDragEndFlashStartupInventory(event, player)) {
                return;
            }
            event.setCancelled(true);
            return;
        }

        // 游戏正式开始后必须放行原版背包/工作台拖拽。
        // 之前这里把猎物的 InventoryDragEvent 全部取消了，会导致右键把物品滑进合成格时，
        // 服务端把一次拖拽分配到多个格子的操作回滚，看起来就像只能一个一个放。
        // 菜单保护由 MenuListener 负责，这里不能拦正常合成。
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPrepareMountedCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (isMountedActiveGame(player, room) && isCraftingView(event.getView().getTopInventory())) {
            plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
        }
    }

    private boolean handleFlashTournamentWarmupMove(PlayerMoveEvent event, Player player, GameRoom room) {
        if (room == null
                || !room.getGameMode().isFlashTournament()
                || room.getState() != RoomState.PLAYING
                || room.isGameActuallyStarted()) {
            return false;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return true;
        }
        if (room.getGameWorld() == null || !player.getWorld().equals(room.getGameWorld())) {
            return false;
        }
        player.setFallDistance(0.0F);

        // 赛事模式进入世界后的前2秒，所有玩家只能转视角，不能位移。
        if (room.isFlashTournamentMovementLocked()) {
            freezeTournamentMove(event, from, to);
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (room.isPrey(uuid)) {
            Location start = room.getFlashTournamentStartLocation(uuid);
            if (start == null) {
                start = from == null ? to.clone() : from.clone();
                room.setFlashTournamentStartLocation(uuid, start);
            }
            Long inputUntil = flashTournamentPreyInputUntil.get(uuid);
            boolean movedBySelfInput = inputUntil != null && inputUntil >= System.currentTimeMillis();
            if (movedBySelfInput && isMovedBeyondFlashTournamentStart(start, to, 0.8D)) {
                flashTournamentPreyInputUntil.remove(uuid);
                plugin.getGameManager().triggerFlashTournamentStartByMovement(room);
            }
            return true;
        }

        if (!room.isHunter(uuid)) {
            return true;
        }

        Player centerPrey = findNearestOnlineTournamentPrey(room, player);
        if (centerPrey == null || !centerPrey.getWorld().equals(player.getWorld())) {
            return true;
        }

        double fromDistanceSquared = horizontalDistanceSquared(from == null ? player.getLocation() : from, centerPrey.getLocation());
        double toDistanceSquared = horizontalDistanceSquared(to, centerPrey.getLocation());

        // 猎人正式开始前必须待在猎物 1~12 格环形范围内。
        // 超出12格时不弹飞，只锁住继续远离的移动，并允许玩家自己走回去。
        if (toDistanceSquared > 144.0D && toDistanceSquared >= fromDistanceSquared - 0.0001D) {
            freezeTournamentMove(event, from, to);
            return true;
        }
        // 不能贴到猎物1格内，避免猎人推猎物导致误触发开局。
        if (toDistanceSquared < 1.0D && toDistanceSquared <= fromDistanceSquared + 0.0001D) {
            freezeTournamentMove(event, from, to);
            return true;
        }
        return true;
    }

    private void freezeTournamentMove(PlayerMoveEvent event, Location from, Location to) {
        if (event == null || from == null || to == null) {
            return;
        }
        Location locked = from.clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        event.setTo(locked);
    }

    private double horizontalDistanceSquared(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return Double.MAX_VALUE;
        }
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private boolean isMovedBeyondFlashTournamentStart(Location start, Location current, double limit) {
        if (start == null || current == null || start.getWorld() == null || current.getWorld() == null) {
            return false;
        }
        if (!start.getWorld().equals(current.getWorld())) {
            return true;
        }
        double dx = current.getX() - start.getX();
        double dz = current.getZ() - start.getZ();
        return dx * dx + dz * dz > limit * limit;
    }

    private Player findNearestOnlineTournamentPrey(GameRoom room, Player player) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (UUID preyUuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null || !prey.isOnline()) {
                continue;
            }
            if (!prey.getWorld().equals(player.getWorld())) {
                if (nearest == null) {
                    nearest = prey;
                }
                continue;
            }
            double distance = prey.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = prey;
            }
        }
        return nearest;
    }

    private void pushTournamentPreyWithoutDamage(Player attacker, Player victim) {
        if (attacker == null || victim == null || !attacker.getWorld().equals(victim.getWorld())) {
            return;
        }
        Vector push = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
        push.setY(0.0D);
        if (push.lengthSquared() < 0.0001D) {
            push = attacker.getLocation().getDirection();
            push.setY(0.0D);
        }
        if (push.lengthSquared() < 0.0001D) {
            return;
        }
        push.normalize().multiply(0.36D);
        push.setY(Math.max(0.12D, victim.getVelocity().getY() * 0.35D + 0.08D));
        final Vector finalPush = push.clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.setVelocity(victim.getVelocity().add(finalPush));
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event instanceof PlayerPortalEvent) {
            return;
        }
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null) {
            return;
        }

        World fromWorld = event.getFrom() == null ? player.getWorld() : event.getFrom().getWorld();
        if (isFlashExitPortalTeleport(room, fromWorld, event.getCause())) {
            event.setCancelled(true);
            if (startEndFlashExitPortalAction(player, room)) {
                handleFlashExitPortalCompletion(player, room);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDimensionTeleportStateCapture(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null || event.getFrom() == null || event.getTo() == null
                || event.getFrom().getWorld() == null || event.getTo().getWorld() == null
                || event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null || room.getState() != RoomState.PLAYING || !isRoomWorld(room, event.getFrom().getWorld())) {
            return;
        }
        dimensionGameModeRestore.put(player.getUniqueId(), player.getGameMode());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room == null || room.getState() != RoomState.PLAYING) {
            dimensionGameModeRestore.remove(player.getUniqueId());
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> restoreRoomStateAfterDimensionChange(player, room, dimensionGameModeRestore.remove(player.getUniqueId())), 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());

        if (room == null || room.getState() != RoomState.PLAYING) return;


        // 获取玩家当前所在的世界
        World fromWorld = event.getFrom() == null ? player.getWorld() : event.getFrom().getWorld();

        if (!isRoomWorld(room, fromWorld)) return;

        if (isFlashExitPortalTeleport(room, fromWorld, event.getCause())) {
            event.setCancelled(true);
            if (startEndFlashExitPortalAction(player, room)) {
                handleFlashExitPortalCompletion(player, room);
            }
            return;
        }

        // 根据传送门类型传送到对应的游戏世界维度
        World targetWorld = null;
        String roomId = room.getRoomId();

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            // 下界传送门
            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                // 从主世界到下界
                targetWorld = plugin.getWorldManager().getNetherWorld(roomId);
                if (targetWorld == null) {
                    targetWorld = plugin.getWorldManager().ensureNetherWorld(roomId);
                }
            } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                // 从下界到主世界
                targetWorld = plugin.getWorldManager().getGameWorld(roomId);
            }
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            // 末地传送门
            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                // 从主世界到末地
                targetWorld = plugin.getWorldManager().getEndWorld(roomId);
                if (targetWorld == null) {
                    targetWorld = plugin.getWorldManager().ensureEndWorld(roomId);
                }
            } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
                // 从末地到主世界
                targetWorld = plugin.getWorldManager().getGameWorld(roomId);
            }
        }

        if (targetWorld != null) {
            // 取消默认传送
            event.setCancelled(true);

            org.bukkit.Location targetLoc;
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                // 末地传送门：传送到目标世界出生点
                targetLoc = targetWorld.getSpawnLocation();
            } else {
                // 下界传送门：用原坐标换算并寻找或生成传送门
                org.bukkit.Location from = event.getFrom();
                if (from == null) return;

                // 计算目标坐标（主世界→下界除以8，下界→主世界乘以8）
                double x, z;
                if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                    // 主世界到下界：坐标除以8
                    x = from.getX() / 8.0;
                    z = from.getZ() / 8.0;
                } else {
                    // 下界到主世界：坐标乘以8
                    x = from.getX() * 8.0;
                    z = from.getZ() * 8.0;
                }

                // 创建目标位置
                targetLoc = new org.bukkit.Location(targetWorld, x, from.getY(), z, from.getYaw(), from.getPitch());

                // 检查是否有缓存的传送门位置
                UUID playerId = player.getUniqueId();
                org.bukkit.Location cachedPortal = portalLocationCache.get(playerId);

                if (cachedPortal != null && cachedPortal.getWorld() != null &&
                    cachedPortal.getWorld().equals(targetWorld)) {
                    // 检查缓存的传送门是否仍然存在
                    if (isPortalAt(cachedPortal)) {
                        // 使用缓存的传送门位置
                        targetLoc = cachedPortal.clone();
                    } else {
                        // 传送门已被破坏，清除缓存并寻找新位置
                        portalLocationCache.remove(playerId);
                        targetLoc = findOrCreatePortal(targetLoc, playerId);
                    }
                } else {
                    // 没有缓存，寻找或创建传送门
                    targetLoc = findOrCreatePortal(targetLoc, playerId);
                }
            }

            // 使用 Bukkit.getScheduler 延迟传送，避免传送门事件冲突
            final org.bukkit.Location finalLoc = targetLoc;
            final org.bukkit.GameMode beforePortalGameMode = player.getGameMode();
            dimensionGameModeRestore.put(player.getUniqueId(), beforePortalGameMode);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(finalLoc);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.0f);
                plugin.getGameManager().syncEndDimensionBrightness(player, room);

                // 跨维度后可能被 Multiverse/世界默认模式覆盖，延迟把游戏内身份模式恢复回来。
                restoreRoomStateAfterDimensionChange(player, room, beforePortalGameMode);
            });
        } else {
            // 如果目标世界不存在，取消传送
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.dimension_not_available"));
        }
    }

    private void handleFlashExitPortalCompletion(Player player, GameRoom room) {
        if (player == null || room == null || room.getState() != RoomState.PLAYING
                || !room.getGameMode().isFlashLike()) {
            return;
        }

        String modeName = room.getGameMode() == GameMode.END_FLASH ? "终章"
                : (room.getGameMode().isFlashTournament() ? "赛事" : "闪光");
        if (!room.isEndFlashDragonDefeated()) {
            bounceEndFlashExitPortalPlayer(player, "§x§B§B§8§8§F§F✦ §d需要先击败末影龙，龙池传送门才会结算" + modeName + "。");
            return;
        }

        if (room.isHunter(player.getUniqueId())) {
            if (room.getGameMode() == GameMode.END_FLASH) {
                bounceEndFlashExitPortalPlayer(player, "§x§F§F§8§8§8§8✦ §c终章龙池传送门只承认猎物通关，猎人会被动量弹开。");
            } else {
                bounceEndFlashExitPortalPlayer(player, "§x§F§F§B§B§6§6✦ §e闪光龙池传送门只承认猎物通关，猎人不能结算。");
            }
            return;
        }

        if (!room.isPrey(player.getUniqueId())) {
            bounceEndFlashExitPortalPlayer(player, "§x§A§A§A§A§F§F✦ §7旁观者不能触发" + modeName + "通关。");
            return;
        }

        launchEndFlashSpectator(player);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.82f, 1.18f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.74f, 1.55f);
        room.broadcast(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§B§B§8§8§F§F✦ §d猎物 §f" + player.getName()
                + " §d进入了击败末影龙后的龙池传送门，" + modeName + "通关！");
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (room.getState() == RoomState.PLAYING) {
                plugin.getGameManager().endGame(room, true);
            }
        });
    }

    private void bounceEndFlashExitPortalPlayer(Player player, String actionBarText) {
        Location location = player.getLocation();
        Vector away = getEndFlashExitPortalAwayVector(location);
        player.setFallDistance(0.0F);
        Vector push = away.clone().multiply(1.28D).setY(0.76D);
        player.setVelocity(push);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.setFallDistance(0.0F);
                player.setVelocity(push.clone());
            }
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.setFallDistance(0.0F);
                player.setVelocity(push.clone().multiply(0.62D).setY(0.38D));
            }
        }, 2L);
        player.sendActionBar(actionBarText);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.62f, 0.72f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.48f, 0.62f);
        player.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, player.getLocation().add(0.0D, 0.8D, 0.0D),
                34, 0.45D, 0.55D, 0.45D, 0.035D);
    }

    private Vector getEndFlashExitPortalAwayVector(Location source) {
        if (source == null || source.getWorld() == null) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        Location portalCenter = findNearbyEndPortalCenter(source, 6);
        Vector away = portalCenter == null
                ? source.getDirection().multiply(-1.0D)
                : source.toVector().subtract(portalCenter.toVector());
        away.setY(0.0D);
        if (away.lengthSquared() < 0.0001D) {
            away = source.getDirection().multiply(-1.0D);
            away.setY(0.0D);
        }
        if (away.lengthSquared() < 0.0001D) {
            away = new Vector(0.0D, 0.0D, 1.0D);
        }
        return away.normalize();
    }

    private Location findNearbyEndPortalCenter(Location source, int radius) {
        if (source == null || source.getWorld() == null) {
            return null;
        }
        World world = source.getWorld();
        int baseX = source.getBlockX();
        int baseY = source.getBlockY();
        int baseZ = source.getBlockZ();
        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int y = baseY + dy;
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                    continue;
                }
                for (int dz = -radius; dz <= radius; dz++) {
                    if (world.getBlockAt(baseX + dx, y, baseZ + dz).getType() == Material.END_PORTAL) {
                        sumX += baseX + dx + 0.5D;
                        sumY += y + 0.5D;
                        sumZ += baseZ + dz + 0.5D;
                        count++;
                    }
                }
            }
        }
        if (count <= 0) {
            return null;
        }
        return new Location(world, sumX / count, sumY / count, sumZ / count);
    }

    private void restoreRoomStateAfterDimensionChange(Player player, GameRoom room, org.bukkit.GameMode previousMode) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            GameRoom currentRoom = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (currentRoom == null || !currentRoom.getRoomId().equals(room.getRoomId())) return;
            if (currentRoom.getState() != RoomState.PLAYING) return;

            String currentRoomId = plugin.getWorldManager().getRoomIdByWorld(player.getWorld());
            if (currentRoomId == null || !currentRoomId.equals(room.getRoomId())) return;

            boolean spectatorLike = currentRoom.isSpectator(player.getUniqueId())
                    || forcedHunterTpSelection.contains(player.getUniqueId())
                    || (currentRoom.getGameMode() == GameMode.END_FLASH
                    && currentRoom.isHunter(player.getUniqueId())
                    && isEndFlashHunterRespawnWaiting(player.getUniqueId()))
                    || previousMode == org.bukkit.GameMode.SPECTATOR;
            if (spectatorLike) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                if (currentRoom.isSpectator(player.getUniqueId())) {
                    plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
                    plugin.getGameManager().giveSpectatorItems(player);
                }
            } else {
                player.setGameMode(previousMode == null || previousMode == org.bukkit.GameMode.SPECTATOR
                        ? org.bukkit.GameMode.SURVIVAL
                        : previousMode);
                if (currentRoom.isPrey(player.getUniqueId()) || currentRoom.isHunter(player.getUniqueId())) {
                    plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(),
                            currentRoom.isPrey(player.getUniqueId()),
                            currentRoom.getGameMode() == GameMode.END_FLASH
                                    ? currentRoom.getAssignedEndFlashKitName(player.getUniqueId())
                                    : null);
                }
            }
            plugin.getGameManager().syncEndDimensionBrightness(player, room);
        }, 2L);
    }

    /**
     * 检查指定位置是否有传送门
     */
    private boolean isPortalAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        // 检查周围3x3x3区域是否有传送门方块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    org.bukkit.Location checkLoc = loc.clone().add(dx, dy, dz);
                    if (checkLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 寻找附近的传送门或创建新传送门
     */
    private org.bukkit.Location findOrCreatePortal(org.bukkit.Location targetLoc, UUID playerId) {
        World world = targetLoc.getWorld();
        if (world == null) return targetLoc;

        int x = targetLoc.getBlockX();
        int y = targetLoc.getBlockY();
        int z = targetLoc.getBlockZ();

        // 先在附近16格范围内搜索已有的传送门
        org.bukkit.Location nearbyPortal = findNearbyPortal(world, x, y, z, 16);
        if (nearbyPortal != null) {
            // 找到附近的传送门，缓存并使用
            portalLocationCache.put(playerId, nearbyPortal);
            return nearbyPortal;
        }

        // 没有找到附近的传送门，创建新的
        org.bukkit.Location newPortal = findSafePortalLocation(targetLoc);
        // 缓存新创建的传送门位置
        portalLocationCache.put(playerId, newPortal);
        return newPortal;
    }

    /**
     * 在指定范围内搜索传送门
     */
    private org.bukkit.Location findNearbyPortal(World world, int centerX, int centerY, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    org.bukkit.Location checkLoc = new org.bukkit.Location(world, centerX + dx, centerY + dy, centerZ + dz);
                    if (checkLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                        // 找到传送门，返回一个安全的传送位置（传送门中心）
                        return checkLoc.clone().add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 寻找安全的传送门位置并生成传送门
     */
    private org.bukkit.Location findSafePortalLocation(org.bukkit.Location targetLoc) {
        World world = targetLoc.getWorld();
        if (world == null) return targetLoc;

        int x = targetLoc.getBlockX();
        int y = targetLoc.getBlockY();
        int z = targetLoc.getBlockZ();

        // 限制Y坐标范围
        if (world.getEnvironment() == World.Environment.NETHER) {
            // 下界：Y坐标限制在10-120之间
            y = Math.max(10, Math.min(120, y));
        } else {
            // 主世界：Y坐标限制在-60到300之间
            y = Math.max(-60, Math.min(300, y));
        }

        // 寻找安全位置（向上搜索）
        org.bukkit.Location safeLoc = new org.bukkit.Location(world, x + 0.5, y, z + 0.5, targetLoc.getYaw(), targetLoc.getPitch());

        for (int i = 0; i < 30; i++) {
            int checkY = y + i;
            if (checkY >= world.getMaxHeight() - 3) break;

            org.bukkit.Location checkLoc = new org.bukkit.Location(world, x, checkY, z);

            // 检查是否是安全位置（脚下是固体方块，身体和头部是空气）
            Material feet = world.getBlockAt(checkLoc).getType();
            Material body = world.getBlockAt(checkLoc.clone().add(0, 1, 0)).getType();
            Material head = world.getBlockAt(checkLoc.clone().add(0, 2, 0)).getType();

            if (feet.isSolid() && !body.isSolid() && !head.isSolid()) {
                safeLoc.setY(checkY + 1);

                // 生成下界门框架（4x5的门框）
                generateNetherPortal(world, x, checkY + 1, z);

                return safeLoc;
            }
        }

        // 如果没找到安全位置，在当前位置生成平台和传送门
        generatePlatformAndPortal(world, x, y, z);
        safeLoc.setY(y + 1);
        return safeLoc;
    }

    /**
     * 生成下界门框架
     */
    private void generateNetherPortal(World world, int x, int y, int z) {
        // 生成4x5的下界门框架（黑曜石）
        Material frame = Material.OBSIDIAN;
        Material portal = Material.NETHER_PORTAL;

        // 底部和顶部
        for (int dx = -1; dx <= 2; dx++) {
            world.getBlockAt(x + dx, y - 1, z).setType(frame);
            world.getBlockAt(x + dx, y + 3, z).setType(frame);
        }

        // 左右两侧
        for (int dy = 0; dy < 3; dy++) {
            world.getBlockAt(x - 1, y + dy, z).setType(frame);
            world.getBlockAt(x + 2, y + dy, z).setType(frame);
        }

        // 中间的传送门方块
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                org.bukkit.block.Block block = world.getBlockAt(x + dx, y + dy, z);
                block.setType(portal);

                // 设置传送门方向
                if (block.getBlockData() instanceof org.bukkit.block.data.Orientable orientable) {
                    orientable.setAxis(org.bukkit.Axis.X);
                    block.setBlockData(orientable);
                }
            }
        }
    }

    /**
     * 生成平台和传送门（当找不到安全位置时）
     */
    private void generatePlatformAndPortal(World world, int x, int y, int z) {
        // 生成5x5的黑曜石平台
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(x + dx, y - 1, dz + z).setType(Material.OBSIDIAN);
            }
        }

        // 清空上方空间
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                world.getBlockAt(x + dx, y + dy, z).setType(Material.AIR);
            }
        }

        // 生成传送门
        generateNetherPortal(world, x, y, z);
    }

    // 检查猎物背包里是否还有开始按钮（CustomModelData=10007）
    private boolean hasStartButton(GameRoom room) {
        for (UUID uuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null) {
                for (org.bukkit.inventory.ItemStack item : prey.getInventory().getContents()) {
                    if (item != null && item.getType() == org.bukkit.Material.PAPER && item.hasItemMeta()) {
                        var meta = item.getItemMeta();
                        if (meta.hasCustomModelData() && meta.getCustomModelData() == 10007) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // 获取安全的重生位置（避免卡在方块里）
    private org.bukkit.Location getEndFlashSpectatorWaitingLocation(GameRoom room, org.bukkit.Location deathLocation) {
        World world = deathLocation == null ? null : deathLocation.getWorld();
        if (world == null && room != null) {
            world = room.getGameWorld();
        }
        if (world == null) {
            return deathLocation == null ? new org.bukkit.Location(null, 0.0D, 80.0D, 0.0D) : deathLocation.clone();
        }

        org.bukkit.Location base = deathLocation == null ? world.getSpawnLocation() : deathLocation.clone();
        if (base.getWorld() == null || !base.getWorld().equals(world)) {
            base.setWorld(world);
        }
        double minSafeY = world.getMinHeight() + 12.0D;
        double maxSafeY = world.getMaxHeight() - 8.0D;
        if (base.getY() < minSafeY || base.getY() > maxSafeY) {
            org.bukkit.Location surface = findEndFlashSurfaceLocation(world, base.getBlockX(), base.getBlockZ(), base.getYaw(), base.getPitch());
            if (surface != null) {
                base = surface.add(0.0D, 4.0D, 0.0D);
            } else {
                org.bukkit.Location spawnSafe = getEndFlashSafeRespawnLocation(room, world.getSpawnLocation());
                base = spawnSafe.add(0.0D, 4.0D, 0.0D);
            }
        }
        base.setY(Math.max(minSafeY, Math.min(maxSafeY, base.getY())));
        base.setX(base.getBlockX() + 0.5D);
        base.setZ(base.getBlockZ() + 0.5D);
        return base;
    }

    private org.bukkit.Location getEndFlashSafeRespawnLocation(GameRoom room, org.bukkit.Location location) {
        World world = location == null ? null : location.getWorld();
        if (world == null && room != null) {
            world = room.getGameWorld();
        }
        if (world == null) {
            return location == null ? new org.bukkit.Location(null, 0.0D, 80.0D, 0.0D) : location.clone();
        }

        org.bukkit.Location base = location == null ? world.getSpawnLocation() : location.clone();
        if (base.getWorld() == null || !base.getWorld().equals(world)) {
            base.setWorld(world);
        }

        org.bukkit.Location surface = findEndFlashSurfaceLocation(world, base.getBlockX(), base.getBlockZ(), base.getYaw(), base.getPitch());
        if (surface != null) {
            return surface;
        }

        org.bukkit.Location spawn = world.getSpawnLocation();
        org.bukkit.Location spawnSurface = findEndFlashSurfaceLocation(world, spawn.getBlockX(), spawn.getBlockZ(), base.getYaw(), base.getPitch());
        if (spawnSurface != null) {
            return spawnSurface;
        }

        org.bukkit.Location safeSpawn = getSafeRespawnLocation(spawn);
        if (safeSpawn.getY() <= world.getMinHeight() + 4.0D) {
            safeSpawn = spawn.clone().add(0.5D, 6.0D, 0.5D);
        }
        safeSpawn.setY(Math.max(world.getMinHeight() + 8.0D, Math.min(world.getMaxHeight() - 4.0D, safeSpawn.getY())));
        return safeSpawn;
    }

    private org.bukkit.Location findEndFlashSurfaceLocation(World world, int x, int z, float yaw, float pitch) {
        if (world == null) {
            return null;
        }
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 3;
        for (int y = maxY; y >= minY; y--) {
            Material ground = world.getBlockAt(x, y, z).getType();
            if (!ground.isSolid()) {
                continue;
            }
            Material feet = world.getBlockAt(x, y + 1, z).getType();
            Material head = world.getBlockAt(x, y + 2, z).getType();
            if (!feet.isSolid() && !head.isSolid()) {
                return new org.bukkit.Location(world, x + 0.5D, y + 1.0D, z + 0.5D, yaw, pitch);
            }
        }
        return null;
    }

    private org.bukkit.Location getSafeRespawnLocation(org.bukkit.Location location) {
        World world = location.getWorld();
        if (world == null) return location;

        org.bukkit.Location safeLoc = location.clone();
        int x = safeLoc.getBlockX();
        int y = safeLoc.getBlockY();
        int z = safeLoc.getBlockZ();

        // 向上搜索安全位置（最多搜索10格）
        for (int i = 0; i < 10; i++) {
            int checkY = y + i;
            if (checkY >= world.getMaxHeight()) break;

            org.bukkit.Location checkLoc = new org.bukkit.Location(world, x + 0.5, checkY, z + 0.5);

            // 检查脚下、身体、头部三个位置
            Material feet = world.getBlockAt(checkLoc).getType();
            Material body = world.getBlockAt(checkLoc.clone().add(0, 1, 0)).getType();
            Material head = world.getBlockAt(checkLoc.clone().add(0, 2, 0)).getType();

            // 如果脚下是固体方块，身体和头部是空气，则是安全位置
            if (feet.isSolid() && !body.isSolid() && !head.isSolid()) {
                safeLoc.setY(checkY + 1); // 站在固体方块上面
                safeLoc.setX(x + 0.5); // 居中
                safeLoc.setZ(z + 0.5); // 居中
                return safeLoc;
            }
        }

        // 如果没找到安全位置，返回原位置并向上偏移1格
        safeLoc.add(0, 1, 0);
        return safeLoc;
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // 检查是否是末影龙死亡
        if (event.getEntityType() != org.bukkit.entity.EntityType.ENDER_DRAGON) {
            return;
        }

        String worldRoomId = plugin.getWorldManager().getRoomIdByWorld(event.getEntity().getWorld());
        GameRoom room = worldRoomId == null ? null : plugin.getRoomManager().getRoom(worldRoomId);

        // 获取击杀者
        Player killer = event.getEntity().getKiller();
        if (room == null && killer != null) {
            room = plugin.getRoomManager().getPlayerRoom(killer.getUniqueId());
        }

        if (room == null || room.getState() != RoomState.PLAYING) {
            return;
        }

        if (worldRoomId != null && !worldRoomId.equals(room.getRoomId())) {
            return;
        }

        if (room.getGameMode().isFlashLike()) {
            String modeName = room.getGameMode() == GameMode.END_FLASH ? "终章"
                    : (room.getGameMode().isFlashTournament() ? "赛事" : "闪光");
            room.setEndFlashDragonDefeated(true);
            if (!room.getGameMode().isFlashTournament()) {
                room.broadcast(plugin.getConfigManager().getHunterGamePrefix()
                        + "§x§B§B§8§8§F§F✦ §d末影龙已被击败！§f猎物进入龙池传送门后才会真正结算" + modeName + "通关。");
            }
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) {
                    online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.82f, 1.05f);
                    online.playSound(online.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.68f, 1.22f);
                    if (!room.getGameMode().isFlashTournament()) {
                        online.sendActionBar("§x§B§B§8§8§F§F✦ §d龙池传送门已解锁 §8| §f猎物进入才算" + modeName + "结束");
                    }
                }
            }
            return;
        }

        // 无论是猎人还是猎物击杀末影龙，都算猎物胜利
        plugin.getGameManager().endGame(room, true);
    }

    /**
     * 设置指南针传送冷却时间
     */
    public void setCompassTpCooldown(UUID uuid) {
        compassTpCooldown.put(uuid, System.currentTimeMillis() + 15 * 60 * 1000L);
    }

    public void setCompassTpCooldown(UUID uuid, GameRoom room) {
        compassTpCooldown.put(uuid, System.currentTimeMillis() + getCompassTpCooldownMillis(room));
    }

    public void addCompassTpCooldown(UUID uuid, long extraMillis) {
        long now = System.currentTimeMillis();
        long currentUntil = Math.max(now, compassTpCooldown.getOrDefault(uuid, now));
        compassTpCooldown.put(uuid, currentUntil + Math.max(0L, extraMillis));
    }

    public void setCompassTpCooldownUntil(UUID uuid, long untilMillis) {
        compassTpCooldown.put(uuid, Math.max(System.currentTimeMillis(), untilMillis));
    }

    public long getCompassTpRemainingMillis(UUID uuid) {
        return Math.max(0L, compassTpCooldown.getOrDefault(uuid, 0L) - System.currentTimeMillis());
    }

    public String getCompassTpRemainingDisplay(UUID uuid) {
        long remaining = getCompassTpRemainingMillis(uuid);
        return remaining <= 0L ? "可传送" : formatDurationZh(remaining);
    }

    public void resetCompassTpState(GameRoom room) {
        if (room == null) {
            return;
        }
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            compassTpCooldown.remove(uuid);
            compassDropCount.remove(uuid);
            lastCompassDrop.remove(uuid);
            hunterTpLootLockUntil.remove(uuid);
            forcedHunterTpSelection.remove(uuid);
        }
        for (UUID uuid : room.getSpectators()) {
            compassTpCooldown.remove(uuid);
            compassDropCount.remove(uuid);
            lastCompassDrop.remove(uuid);
            hunterTpLootLockUntil.remove(uuid);
            forcedHunterTpSelection.remove(uuid);
        }
    }

    public void primeFlashStartCompassCooldown(GameRoom room) {
        if (room == null || !plugin.getFlashModeManager().isFlashMode(room) || room.getGameMode().isFlashTournament()) {
            return;
        }
        long until = System.currentTimeMillis() + 3 * 60 * 1000L;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid) || (room.isPrey(uuid) && room.getPreyUUIDs().size() >= 2)) {
                setCompassTpCooldownUntil(uuid, until);
            }
        }
    }

    private String buildChineseDeathMessage(Player player) {
        Player killer = player.getKiller();
        if (killer != null) {
            return "§x§F§F§7§7§7§7☠ §f" + player.getName() + " §7被 §c" + killer.getName() + " §7击杀了";
        }

        String name = "§f" + player.getName() + " §7";
        org.bukkit.event.entity.EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage == null) {
            return "§x§F§F§7§7§7§7☠ " + name + "倒下了";
        }

        return switch (lastDamage.getCause()) {
            case FALL -> "§x§F§F§7§7§7§7☠ " + name + "摔成了一地方块";
            case FIRE, FIRE_TICK -> "§x§F§F§7§7§7§7☠ " + name + "被火焰吞没了";
            case LAVA -> "§x§F§F§7§7§7§7☠ " + name + "在岩浆里融化了";
            case DROWNING -> "§x§F§F§7§7§7§7☠ " + name + "溺水了";
            case VOID -> "§x§F§F§7§7§7§7☠ " + name + "坠入了虚空";
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> "§x§F§F§7§7§7§7☠ " + name + "被炸飞了";
            case PROJECTILE -> "§x§F§F§7§7§7§7☠ " + name + "被远程攻击击杀了";
            case MAGIC, WITHER, POISON -> "§x§F§F§7§7§7§7☠ " + name + "被异常状态带走了";
            case SUFFOCATION -> "§x§F§F§7§7§7§7☠ " + name + "卡在方块里窒息了";
            case STARVATION -> "§x§F§F§7§7§7§7☠ " + name + "饿倒了";
            default -> "§x§F§F§7§7§7§7☠ " + name + "死亡了";
        };
    }

    public void setHunterTpLootLock(UUID uuid) {
        hunterTpLootLockUntil.put(uuid, System.currentTimeMillis() + 5 * 60 * 1000L);
    }

    public boolean isForcedHunterTpSelection(UUID uuid) {
        return forcedHunterTpSelection.contains(uuid);
    }

    public void clearForcedHunterTpSelection(UUID uuid) {
        forcedHunterTpSelection.remove(uuid);
    }

    private long getCompassTpCooldownMillis(GameRoom room) {
        if (room != null && room.hasModifier("InfiniteTP")) {
            return 60 * 1000L;
        }
        return 15 * 60 * 1000L;
    }

    private String formatDurationZh(long millis) {
        long totalSeconds = Math.max(0L, (long) Math.ceil(millis / 1000.0D));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "时" + minutes + "分" + seconds + "秒";
        }
        if (minutes > 0L) {
            return minutes + "分" + seconds + "秒";
        }
        return seconds + "秒";
    }

    private boolean isTeamTeleportCompassUser(Player player, GameRoom room, ItemStack item) {
        if (player == null || room == null || !isPlainTrackingCompass(item)) {
            return false;
        }
        if (room.getGameMode().isFlashTournament()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (room.isHunter(uuid)) {
            return true;
        }
        return plugin.getFlashModeManager().isFlashMode(room)
                && room.isPrey(uuid)
                && room.getPreyUUIDs().size() >= 2;
    }

    private boolean isPlainTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null || !meta.hasCustomModelData();
    }

    private void startRandomCompassEating(Player player) {
        if (!randomCompassEating.add(player.getUniqueId())) {
            return;
        }

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1, false, false, false));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 60, 0, false, false, false));
    }

    private void stopRandomCompassEating(Player player) {
        if (!randomCompassEating.remove(player.getUniqueId())) {
            return;
        }

        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS);
    }

    private boolean isHunterTpLootLocked(UUID uuid) {
        Long until = hunterTpLootLockUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            hunterTpLootLockUntil.remove(uuid);
            return false;
        }
        return true;
    }

    private void playRestrictionSound(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String rawCommand = event.getMessage().substring(1).trim();
        String[] rawParts = rawCommand.split(" ");
        String baseCommand = rawParts[0].toLowerCase();
        GameRoom senderRoom = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());

        if (isFlashWikiCommand(baseCommand)) {
            event.setCancelled(true);
            giveFlashWikiBook(player);
            return;
        }

        if (isHunterTeamMessageCommand(baseCommand, senderRoom, player)) {
            event.setCancelled(true);
            handleHunterTeamMessage(player, senderRoom, rawCommand);
            return;
        }

        if (isFlashTournamentStartedRoom(senderRoom) && isDirectTextMessageCommand(baseCommand)) {
            event.setCancelled(true);
            player.sendActionBar("§c赛事正式开始后只能使用 §f/teammsg <内容> §c或简单语音聊天");
            playRestrictionSound(player);
            return;
        }

        if (isEmoteCommand(baseCommand)) {
            if (senderRoom != null && senderRoom.getState() == RoomState.PLAYING) {
                event.setCancelled(true);
                String message = plugin.getConfigManager().getConfig().getString("disabled_commands.blocked_message",
                    "§x§F§F§8§8§5§5⚠ §c游戏进行中无法使用此命令！");
                player.sendMessage(message);
                return;
            }

            event.setCancelled(true);
            if (rawParts.length < 2) {
                return;
            }

            String actionText = rawCommand.substring(rawCommand.indexOf(' ') + 1).trim();
            if (actionText.isEmpty()) {
                return;
            }

            String emoteMessage = "§d* §f" + player.getName() + " §d" + actionText;
            for (Player target : Bukkit.getOnlinePlayers()) {
                GameRoom targetRoom = plugin.getRoomManager().getPlayerRoom(target.getUniqueId());
                if (targetRoom != null && targetRoom.getState() == RoomState.PLAYING) {
                    continue;
                }
                target.sendMessage(emoteMessage);
            }
            return;
        }

        // 检查玩家是否在游戏房间中
        GameRoom room = senderRoom;
        if (room == null) {
            return;
        }

        // 只在游戏进行中（PLAYING状态）禁用命令
        if (room.getState() != RoomState.PLAYING) {
            return;
        }

        // 检查是否启用命令禁用功能
        if (!plugin.getConfigManager().getConfig().getBoolean("disabled_commands.enabled", true)) {
            return;
        }

        // 获取命令（去掉开头的/）
        String command = rawCommand.toLowerCase();
        String[] parts = command.split(" ");
        baseCommand = parts[0];

        // 获取禁用的命令列表
        var disabledCommands = plugin.getConfigManager().getConfig().getStringList("disabled_commands.commands");

        // 检查命令是否被禁用
        for (String disabled : disabledCommands) {
            if (baseCommand.equals(disabled.toLowerCase()) || baseCommand.startsWith(disabled.toLowerCase() + ":")) {
                event.setCancelled(true);
                String message = plugin.getConfigManager().getConfig().getString("disabled_commands.blocked_message",
                    "§x§F§F§8§8§5§5⚠ §c游戏进行中无法使用此命令！");
                player.sendMessage(message);
                return;
            }
        }
    }

    private boolean isFlashWikiCommand(String baseCommand) {
        if (baseCommand == null) {
            return false;
        }
        String normalized = baseCommand.startsWith("gamefunxiao:")
                ? baseCommand.substring("gamefunxiao:".length())
                : baseCommand;
        return normalized.equals("flashwiki")
                || normalized.equals("bookwiki")
                || normalized.equals("gamefunwiki")
                || normalized.equals("闪光手册")
                || normalized.equals("书wiki");
    }

    private void giveFlashWikiBook(Player player) {
        ItemStack book = plugin.getFlashModeManager().createFlashGameGuideBook();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(book);
        if (leftovers.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_given"));
        } else {
            Map<Integer, ItemStack> enderLeftovers = player.getEnderChest().addItem(leftovers.values().toArray(new ItemStack[0]));
            if (enderLeftovers.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_ender"));
            } else {
                for (ItemStack item : enderLeftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.wiki_book_dropped"));
            }
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.72f, 1.36f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.48f, 1.62f);
    }

    private void handleHunterTeamMessage(Player player, GameRoom room, String rawCommand) {
        if (room == null) {
            return;
        }

        int firstSpace = rawCommand.indexOf(' ');
        if (firstSpace < 0 || firstSpace >= rawCommand.length() - 1) {
            if (room.getGameMode().isFlashTournament()) {
                player.sendMessage(getFlashTournamentTeamPrefix(room, player) + " §7请输入队伍消息。");
            } else {
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.teammsg_empty"));
            }
            playRestrictionSound(player);
            return;
        }

        String content = rawCommand.substring(firstSpace + 1).trim();
        if (content.isEmpty()) {
            if (room.getGameMode().isFlashTournament()) {
                player.sendMessage(getFlashTournamentTeamPrefix(room, player) + " §7请输入队伍消息。");
            } else {
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.teammsg_empty"));
            }
            playRestrictionSound(player);
            return;
        }

        boolean preySender = room.isPrey(player.getUniqueId());
        if (room.getGameMode().isFlashTournament()) {
            String formatted = getFlashTournamentTeamPrefix(room, player) + " §f" + player.getName() + "§7: §f" + content;
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                if (preySender != room.isPrey(uuid)) {
                    continue;
                }
                Player teammate = Bukkit.getPlayer(uuid);
                if (teammate != null && teammate.isOnline()) {
                    teammate.sendMessage(formatted);
                }
            }
            return;
        }

        String roleName = preySender ? "猎物" : "猎队";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("message", content);
        placeholders.put("role", roleName);
        String formatted = plugin.getMessageManager().getHunterGameMessageWithPrefix("game.teammsg_format", placeholders);
        if (!formatted.contains(roleName) && formatted.contains("[猎队]") && preySender) {
            formatted = formatted.replace("[猎队]", "[猎物]");
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (preySender != room.isPrey(uuid)) {
                continue;
            }
            Player teammate = Bukkit.getPlayer(uuid);
            if (teammate != null && teammate.isOnline()) {
                teammate.sendMessage(formatted);
            }
        }
    }

    private boolean isHunterTeamMessageCommand(String baseCommand, GameRoom room, Player player) {
        if (room == null || player == null || room.getState() != RoomState.PLAYING
                || (!room.isHunter(player.getUniqueId()) && !room.isPrey(player.getUniqueId()))) {
            return false;
        }

        String normalized = baseCommand;
        int namespaceIndex = normalized.lastIndexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < normalized.length() - 1) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        return normalized.equals("teammsg") || normalized.equals("tm");
    }

    private String getFlashTournamentTeamPrefix(GameRoom room, Player player) {
        return room != null && player != null && room.isPrey(player.getUniqueId())
                ? "§a[猎队]"
                : "§c[追队]";
    }

    private boolean isFlashTournamentStartedRoom(GameRoom room) {
        return room != null
                && room.getGameMode().isFlashTournament()
                && room.getState() == RoomState.PLAYING
                && room.isGameActuallyStarted();
    }

    private boolean isDirectTextMessageCommand(String baseCommand) {
        if (baseCommand == null || baseCommand.isBlank()) {
            return false;
        }
        String normalized = baseCommand.toLowerCase();
        int namespaceIndex = normalized.lastIndexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < normalized.length() - 1) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        return normalized.equals("msg")
                || normalized.equals("tell")
                || normalized.equals("w")
                || normalized.equals("whisper")
                || normalized.equals("message")
                || normalized.equals("pm")
                || normalized.equals("dm")
                || normalized.equals("reply")
                || normalized.equals("r")
                || normalized.equals("me")
                || normalized.equals("emote")
                || normalized.equals("action");
    }

    private boolean isEmoteCommand(String baseCommand) {
        return baseCommand.equals("me")
            || baseCommand.equals("minecraft:me")
            || baseCommand.equals("action")
            || baseCommand.equals("emote");
    }

    private record QuickSwapAttackSnapshot(int slot, ItemStack weapon, long timeMillis) {
    }

    private record RecentCombatHit(UUID attackerUuid, String roomId, long timeMillis) {
    }
}
