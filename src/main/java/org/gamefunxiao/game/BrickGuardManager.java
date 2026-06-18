package org.gamefunxiao.game;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.world.BrickGuardMapManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"deprecation"})
public class BrickGuardManager {

    public enum TeamSide {
        BRICK,
        NETHER
    }

    private static final String CONFIG_NAME = "brick-guard-maps";
    private static final String DEFAULT_MAP_ID = "default";
    private static final int TEAM_SELECTOR_MODEL = 21001;
    private static final int CORE_TRANSFER_MODEL = 21010;
    private static final int SPECIAL_SNACK_MODEL = 21011;
    private static final int MAX_TEAM_BUTTON_AMOUNT = 99;
    private static final int DEFAULT_CORE_HEALTH = 500;
    private static final int DEFAULT_BOUNDARY_RADIUS = 1500;
    private static final int DEFAULT_TIME_LIMIT_SECONDS = 3600;
    private static final int DEFAULT_RESPAWN_MIN_SECONDS = 10;
    private static final int DEFAULT_RESPAWN_MAX_SECONDS = 35;
    private static final long CORE_TRANSFER_WINDOW_MS = 60_000L;
    private static final long DYING_DURATION_MS = 90_000L;
    private static final double CORE_MAX_HEALTH = 220.0D;
    private static final double NORMAL_MAX_HEALTH = 20.0D;
    private static final double TOTEM_MAX_HEALTH = 10.0D;
    private static final int FOX_PICK_REPAIR_PER_SECOND = 3;
    private static final int WIN_POINTS = 20;
    private static final int PARTICIPATE_POINTS = 2;
    private static final int KILL_POINTS = 3;

    private static final String ACH_FIRST_WIN = "first_win";
    private static final String ACH_CORE_DESTROYER = "core_destroyer";
    private static final String ACH_CORE_KILLER = "core_killer";
    private static final String ACH_DYING_SURVIVOR = "dying_survivor";
    private static final String ACH_FOX_PICK_MASTER = "fox_pick_master";
    private static final String ACH_TRADE_MASTER = "villager_trade_master";
    private static final List<String> ACHIEVEMENT_ORDER = List.of(
            ACH_FIRST_WIN,
            ACH_CORE_DESTROYER,
            ACH_CORE_KILLER,
            ACH_DYING_SURVIVOR,
            ACH_FOX_PICK_MASTER,
            ACH_TRADE_MASTER
    );

    private static final String BRICK_TEAM_NAME = "板砖队";
    private static final String NETHER_TEAM_NAME = "下界砖队";
    private static final String BRICK_TEAM_DISPLAY = "§x§F§F§7§C§0§0板砖队";
    private static final String NETHER_TEAM_DISPLAY = "§x§6§6§1§9§0§0下界砖队";

    private final GameFunXiao plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final NamespacedKey foxPickKey;
    private final NamespacedKey foxPickLevelKey;
    private final NamespacedKey foxPickMaxDurabilityKey;
    private final NamespacedKey foxPickCurrentDurabilityKey;
    private final NamespacedKey specialSnackKey;
    private final NamespacedKey npcRoleKey;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, BukkitTask> roomTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> respawnTasks = new HashMap<>();
    private final Map<UUID, Long> boundaryWarnCooldowns = new HashMap<>();

    public BrickGuardManager(GameFunXiao plugin) {
        this.plugin = plugin;
        this.foxPickKey = new NamespacedKey(plugin, "brick_guard_fox_pick");
        this.foxPickLevelKey = new NamespacedKey(plugin, "brick_guard_fox_pick_level");
        this.foxPickMaxDurabilityKey = new NamespacedKey(plugin, "brick_guard_fox_pick_max_durability");
        this.foxPickCurrentDurabilityKey = new NamespacedKey(plugin, "brick_guard_fox_pick_current_durability");
        this.specialSnackKey = new NamespacedKey(plugin, "brick_guard_special_snack");
        this.npcRoleKey = new NamespacedKey(plugin, "brick_guard_npc_role");
    }

    public boolean isBrickGuardRoom(GameRoom room) {
        return room != null && room.getGameMode().isBrickGuard();
    }

    public void removeSession(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        BukkitTask task = roomTasks.remove(roomId);
        if (task != null) {
            task.cancel();
        }
        Session session = sessions.remove(roomId);
        if (session == null) {
            return;
        }
        for (UUID uuid : new HashSet<>(session.pendingRespawns.keySet())) {
            BukkitTask respawnTask = respawnTasks.remove(uuid);
            if (respawnTask != null) {
                respawnTask.cancel();
            }
        }
    }

    public void handlePlayerRemoved(String roomId, UUID uuid) {
        if (roomId == null || uuid == null) {
            return;
        }
        Session session = sessions.get(roomId);
        if (session == null) {
            return;
        }
        session.brickTeam.remove(uuid);
        session.netherTeam.remove(uuid);
        session.dyingPlayers.remove(uuid);
        session.eliminatedBrickPlayers.remove(uuid);
        session.eliminatedNetherPlayers.remove(uuid);
        session.pendingRespawns.remove(uuid);
        session.kills.remove(uuid);
        if (uuid.equals(session.corePlayer)) {
            session.corePlayer = null;
        }
        BukkitTask task = respawnTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void giveLobbyItems(Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return;
        }
        Session session = getOrCreateSession(room);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0.0F);
        player.getInventory().setItem(0, createTeamSelectorItem(player, room, session));
        if (player.hasPermission("gamefunxiao.admin") && !room.isAdminForceStartUsed()) {
            player.getInventory().setItem(4, createForceStartItem());
        }
        player.getInventory().setItem(7, createAdvertiseItem());
        player.getInventory().setItem(8, createQuitItem());
    }

    public boolean handleLobbyItem(Player player, GameRoom room, ItemStack item, Material type, int modelData) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        if (modelData == TEAM_SELECTOR_MODEL) {
            player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.72f, 0.82f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.42f, 1.55f);
            new org.gamefunxiao.menu.BrickGuardTeamSelectMenu(plugin, player, room).open();
            return true;
        }
        if (modelData == 10002 && player.hasPermission("gamefunxiao.admin")) {
            if (room.isAdminForceStartUsed()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("game.force_start_already_used"));
                return true;
            }
            int requiredPlayers = plugin.getRoomManager().getMinimumPlayersForMode(room.getGameMode());
            if (room.getPlayerCount() >= requiredPlayers) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                new org.gamefunxiao.menu.hunter.ConfirmForceStartMenu(plugin, player, room).open();
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("count", String.valueOf(requiredPlayers));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("room.need_more_players", placeholders));
            }
            return true;
        }
        if (modelData == 10003) {
            player.performCommand("hh");
            return true;
        }
        if (modelData == 10004) {
            plugin.getRoomManager().leaveRoom(player);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("room.you_left"));
            return true;
        }
        return false;
    }

    public boolean handleGameItem(Player player, GameRoom room, ItemStack item, Material type, int modelData) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        if (modelData == CORE_TRANSFER_MODEL) {
            Session session = getOrCreateSession(room);
            if (!isCoreTransferAvailable(session, player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.core_transfer_unavailable"));
                return true;
            }
            new org.gamefunxiao.menu.BrickGuardCoreTransferMenu(plugin, player, room).open();
            return true;
        }
        return false;
    }

    public void start(GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return;
        }
        Session session = getOrCreateSession(room);
        syncTeamsFromRoom(room, session);
        autoBalanceTeams(room, session);
        if (session.brickTeam.isEmpty() || session.netherTeam.isEmpty()) {
            room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("game.brick_guard_cancelled"));
            plugin.getGameManager().endGameWithoutReward(room);
            return;
        }

        BrickGuardMapManager.MapDefinition definition = plugin.getBrickGuardMapManager() == null
                ? null
                : plugin.getBrickGuardMapManager().findUsableMap(Math.max(2, room.getPlayerCount()));
        BrickGuardMapManager.RuntimeWorlds runtimeWorlds = null;
        if (plugin.getWorldManager() != null && definition != null) {
            runtimeWorlds = plugin.getWorldManager().createBrickGuardWorlds(room.getRoomId(), definition);
        }

        World brickWorld = runtimeWorlds != null && runtimeWorlds.brickWorld() != null ? runtimeWorlds.brickWorld() : room.getGameWorld();
        if (brickWorld == null) {
            brickWorld = plugin.getWorldManager().createGameWorld(room.getRoomId());
        }
        if (brickWorld != null) {
            room.setGameWorld(brickWorld);
        }
        World netherWorld = runtimeWorlds != null && runtimeWorlds.netherBrickWorld() != null
                ? runtimeWorlds.netherBrickWorld()
                : plugin.getWorldManager().getNetherWorld(room.getRoomId());
        if (netherWorld == null) {
            netherWorld = plugin.getWorldManager().ensureNetherWorld(room.getRoomId());
        }
        if (brickWorld == null || netherWorld == null) {
            room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("lobby.lobby_creation_failed"));
            plugin.getGameManager().endGameWithoutReward(room);
            return;
        }

        MapConfig mapConfig = loadActiveMap(definition);
        session.coreHealth = mapConfig.coreHealth();
        session.maxCoreHealth = mapConfig.coreHealth();
        session.boundaryRadius = mapConfig.boundaryRadius();
        session.timeLimitSeconds = mapConfig.timeLimitSeconds();
        session.mapId = mapConfig.id();
        session.mapName = mapConfig.displayName();
        applyRuntimeLocations(room, session, definition, brickWorld, netherWorld, mapConfig);

        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(true);
        room.setPreyStarted(true);
        room.setGameStartTime(System.currentTimeMillis());
        session.startTimeMillis = room.getGameStartTime();

        if (session.brickSpawn == null || session.brickCoreLocation == null) {
            prepareBrickWorld(brickWorld, session);
        } else {
            buildCorePad(brickWorld, session.brickCoreLocation, Material.RED_GLAZED_TERRACOTTA, Material.GRAY_CONCRETE);
            placeBrickResources(brickWorld, session);
        }
        if (session.netherSpawn == null) {
            prepareNetherWorld(netherWorld, session);
        } else {
            placeNetherResources(netherWorld, session);
        }
        spawnBrickVillagers(brickWorld, session);
        spawnNetherPiglins(netherWorld, session);
        chooseCorePlayer(room, session);

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            clearBrickGuardPlayerState(player);
            plugin.getRoomManager().resetPlayerForGameStart(room, player);
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0.0F);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFallDistance(0.0F);
            player.setFireTicks(0);
            if (isBrickTeam(room, uuid)) {
                player.teleport(session.brickSpawn.clone().add(randomOffset(1.25D), 0.0D, randomOffset(1.25D)));
                equipBrickPlayer(player);
            } else {
                player.teleport(session.netherSpawn.clone().add(randomOffset(1.25D), 0.0D, randomOffset(1.25D)));
                equipNetherPlayer(player, uuid.equals(session.corePlayer));
            }
            plugin.getPlayerDataManager().incrementPlayCount(uuid, room.getGameMode());
        }

        plugin.getWorldManager().enableGameWorldRules(room.getRoomId());
        refreshRoleDisplays(room);
        room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("game.brick_guard_started"));
        startGuardTask(room, session);
    }

    public boolean handleBlockBreak(BlockBreakEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (room.getState() != RoomState.PLAYING) {
            event.setCancelled(true);
            return true;
        }
        if (room.isSpectator(player.getUniqueId()) || isRespawnWaiting(session, player.getUniqueId())) {
            event.setCancelled(true);
            return true;
        }

        if (sameBlock(event.getBlock().getLocation(), session.brickCoreLocation)) {
            event.setCancelled(true);
            if (!isNetherTeam(room, player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                return true;
            }
            session.coreHealth = Math.max(0, session.coreHealth - getCoreDamageFromTool(player.getInventory().getItemInMainHand()));
            event.getBlock().getWorld().spawnParticle(Particle.BLOCK, event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D), 16, 0.2D, 0.2D, 0.2D, Material.RED_GLAZED_TERRACOTTA.createBlockData());
            event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.BLOCK_NETHER_BRICKS_BREAK, 0.85f, 0.92f);
            broadcastBrickCoreUnderAttack(room);
            if (session.coreHealth <= 0) {
                unlockAchievement(player, ACH_CORE_DESTROYER);
                endBrickGuardGame(room, false, "板砖核心被摧毁");
            }
            return true;
        }

        Material original = session.managedResources.get(key(event.getBlock().getLocation()));
        if (original != null) {
            event.setDropItems(false);
            handleManagedResourceBreak(event.getBlock(), player, original);
            return true;
        }
        return false;
    }

    public boolean handleBlockPlace(BlockPlaceEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (room.getState() != RoomState.PLAYING || room.isSpectator(player.getUniqueId()) || isRespawnWaiting(session, player.getUniqueId())) {
            cancelBlockPlaceAndResync(event, player);
            return true;
        }
        if (session.brickCoreLocation != null && event.getBlockPlaced().getLocation().distanceSquared(session.brickCoreLocation) <= 4.0D) {
            cancelBlockPlaceAndResync(event, player);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return true;
        }
        return false;
    }

    public boolean handleEntityInteract(PlayerInteractEntityEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Integer role = event.getRightClicked().getPersistentDataContainer().get(npcRoleKey, PersistentDataType.INTEGER);
        if (role == null) {
            return false;
        }
        event.setCancelled(true);
        if (role == 1) {
            if (!isBrickTeam(room, player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.enemy_trade_denied"));
                return true;
            }
            if (isFoxPick(player.getInventory().getItemInMainHand())) {
                tryUpgradeFoxPick(player, room, getOrCreateSession(room));
            } else {
                player.openMerchant(createVillagerMerchant(), true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.8f, 1.1f);
            }
            return true;
        }
        if (role == 2) {
            if (!isNetherTeam(room, player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_PIGLIN_ANGRY, 0.8f, 0.85f);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.enemy_trade_denied"));
                return true;
            }
            player.openMerchant(createPiglinMerchant(), true);
            player.playSound(player.getLocation(), Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 0.8f, 1.2f);
            return true;
        }
        return false;
    }

    public void handleDamage(EntityDamageByEntityEvent event) {
        if (event == null || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(victim.getUniqueId());
        if (!isBrickGuardRoom(room) || room.getState() != RoomState.PLAYING) {
            return;
        }
        Player attacker = resolveAttackingPlayer(event.getDamager());
        if (attacker == null || !room.getAllPlayerUUIDs().contains(attacker.getUniqueId())) {
            return;
        }
        Session session = getOrCreateSession(room);
        if (room.isSpectator(attacker.getUniqueId()) || room.isSpectator(victim.getUniqueId())
                || isRespawnWaiting(session, attacker.getUniqueId()) || isRespawnWaiting(session, victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        boolean attackerBrick = isBrickTeam(room, attacker.getUniqueId());
        boolean victimBrick = isBrickTeam(room, victim.getUniqueId());
        if (attackerBrick == victimBrick) {
            event.setCancelled(true);
            return;
        }
        if (session.corePlayer != null && victim.getUniqueId().equals(session.corePlayer)) {
            if (isFoxPick(attacker.getInventory().getItemInMainHand())) {
                event.setDamage(event.getDamage() * 3.0D);
            } else {
                event.setDamage(event.getDamage() * 1.35D);
            }
        }
        if (session.corePlayer != null && attacker.getUniqueId().equals(session.corePlayer)) {
            event.setDamage(event.getDamage() * 1.40D);
            if (isAnyPickaxe(victim.getInventory().getItemInMainHand())) {
                event.setDamage(event.getDamage() * 1.70D);
            }
        }
        if (session.dyingPlayers.containsKey(victim.getUniqueId())) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false, false));
        }
    }

    public boolean handleDeath(PlayerDeathEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room) || room.getState() != RoomState.PLAYING) {
            return false;
        }
        Session session = getOrCreateSession(room);
        UUID uuid = player.getUniqueId();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.deathMessage(null);

        Player killer = player.getKiller();
        if (killer != null && room.getAllPlayerUUIDs().contains(killer.getUniqueId())) {
            session.kills.merge(killer.getUniqueId(), 1, Integer::sum);
            if (isBrickTeam(room, killer.getUniqueId())) {
                restoreFoxPickDurability(killer);
            }
            plugin.getPlayerDataManager().addMiniGamePoints(killer.getUniqueId(), KILL_POINTS, room.getGameMode());
        }

        if (uuid.equals(session.corePlayer)) {
            if (killer != null && isBrickTeam(room, killer.getUniqueId())) {
                unlockAchievement(killer, ACH_CORE_KILLER);
            }
            session.eliminatedNetherPlayers.add(uuid);
            room.setPendingRespawnLocation(uuid, spectatorWait(session.netherSpawn));
            scheduleImmediateRespawn(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> convertToSpectator(player, room, spectatorWait(session.netherSpawn)), 4L);
            triggerCoreDeath(room, session);
            return true;
        }

        DyingState dyingState = session.dyingPlayers.get(uuid);
        if (dyingState != null) {
            if (!dyingState.totemUsed()) {
                session.dyingPlayers.put(uuid, new DyingState(System.currentTimeMillis() + DYING_DURATION_MS, true));
                room.setPendingRespawnLocation(uuid, session.coreOrigin == null ? session.netherSpawn : session.coreOrigin);
                scheduleImmediateRespawn(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> finishTotemRespawn(player, room, session), 4L);
            } else {
                session.dyingPlayers.remove(uuid);
                session.eliminatedNetherPlayers.add(uuid);
                room.setPendingRespawnLocation(uuid, spectatorWait(session.netherSpawn));
                scheduleImmediateRespawn(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> convertToSpectator(player, room, spectatorWait(session.netherSpawn)), 4L);
                checkBrickVictory(room, session);
            }
            return true;
        }

        int seconds = randomRespawnSeconds();
        if (isBrickTeam(room, uuid)) {
            degradeFoxPick(player);
            RespawnState respawnState = new RespawnState(true, System.currentTimeMillis() + seconds * 1000L,
                    session.brickSpawn.clone(), spectatorWait(session.brickSpawn));
            session.pendingRespawns.put(uuid, respawnState);
            room.setPendingRespawnLocation(uuid, respawnState.waitLocation());
            scheduleImmediateRespawn(player);
            scheduleRespawn(player, room, session, respawnState);
            return true;
        }

        RespawnState respawnState = new RespawnState(false, System.currentTimeMillis() + seconds * 1000L,
                resolveNetherRespawn(session), spectatorWait(session.netherSpawn));
        session.pendingRespawns.put(uuid, respawnState);
        room.setPendingRespawnLocation(uuid, respawnState.waitLocation());
        scheduleImmediateRespawn(player);
        scheduleRespawn(player, room, session, respawnState);
        return true;
    }

    public boolean handleRespawn(PlayerRespawnEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        RespawnState state = session.pendingRespawns.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        event.setRespawnLocation(state.waitLocation());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && room.getState() == RoomState.PLAYING) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setInvulnerable(true);
            }
        }, 2L);
        return true;
    }

    public boolean handleMove(PlayerMoveEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (room.getState() != RoomState.PLAYING || !room.isGameActuallyStarted() || room.isSpectator(player.getUniqueId())) {
            return false;
        }
        Location to = event.getTo();
        if (to == null) {
            return false;
        }
        if (isRespawnWaiting(session, player.getUniqueId())) {
            Location locked = event.getFrom().clone();
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            event.setTo(locked);
            return true;
        }
        Location center = to.getWorld() != null && session.netherSpawn != null && to.getWorld().equals(session.netherSpawn.getWorld())
                ? session.netherSpawn : session.brickSpawn;
        if (center == null) {
            return false;
        }
        double dx = to.getX() - center.getX();
        double dz = to.getZ() - center.getZ();
        if (dx * dx + dz * dz > (double) session.boundaryRadius * session.boundaryRadius) {
            Location locked = event.getFrom().clone();
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            event.setTo(locked);
            long now = System.currentTimeMillis();
            Long last = boundaryWarnCooldowns.get(player.getUniqueId());
            if (last == null || now - last > 1200L) {
                boundaryWarnCooldowns.put(player.getUniqueId(), now);
                player.sendActionBar(Component.text("§x§F§F§7§C§0§0伪边界已阻止你继续前进"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.42f, 0.8f);
            }
            return true;
        }
        return false;
    }

    public boolean handleAsyncChat(AsyncChatEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        event.setCancelled(true);
        String plainContent = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
        String rolePrefix;
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
            rolePrefix = "§8[§x§F§F§7§C§0§0板砖大厅§8] ";
        } else if (room.isSpectator(player.getUniqueId()) || isEliminated(session, player.getUniqueId())) {
            rolePrefix = "§8[§7旁观§8] ";
        } else if (player.getUniqueId().equals(session.corePlayer)) {
            rolePrefix = "§8[§x§6§6§1§9§0§0核心§8] ";
        } else if (session.dyingPlayers.containsKey(player.getUniqueId())) {
            rolePrefix = "§8[§x§D§D§8§8§F§F濒死§8] ";
        } else {
            rolePrefix = "§8[" + (isBrickTeam(room, player.getUniqueId()) ? BRICK_TEAM_DISPLAY : NETHER_TEAM_DISPLAY) + "§8] ";
        }
        String message = plugin.getConfigManager().getBrickGuardPrefix() + rolePrefix + "§f" + player.getName() + "§7: §f" + plainContent;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Set<UUID> receivers = new LinkedHashSet<>(room.getAllPlayerUUIDs());
            receivers.addAll(room.getSpectators());
            for (UUID uuid : receivers) {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    online.sendMessage(message);
                }
            }
        });
        return true;
    }

    public boolean handleItemConsume(Player player, GameRoom room, ItemStack item) {
        if (!isBrickGuardRoom(room) || item == null || item.getType() != Material.ROTTEN_FLESH) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(specialSnackKey, PersistentDataType.BYTE)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        DyingState state = session.dyingPlayers.get(player.getUniqueId());
        if (state == null) {
            return true;
        }
        session.dyingPlayers.put(player.getUniqueId(), new DyingState(System.currentTimeMillis() + DYING_DURATION_MS, state.totemUsed()));
        unlockAchievement(player, ACH_DYING_SURVIVOR);
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.dying_reset"));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.9f, 1.0f);
        return true;
    }

    public boolean handleDropItem(GameRoom room, Player player, ItemStack item) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING || room.getState() == RoomState.SELECTING) {
            return true;
        }
        if (room.isSpectator(player.getUniqueId()) || isRespawnWaiting(session, player.getUniqueId())) {
            return true;
        }
        return false;
    }

    public boolean handleVoidDamage(GameRoom room, Player player) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (room.getState() == RoomState.WAITING || room.getState() == RoomState.STARTING) {
            if (session.brickSpawn != null) {
                player.setFallDistance(0.0F);
                player.teleport(session.brickSpawn);
            }
            return true;
        }
        if (room.getState() == RoomState.PLAYING) {
            if (isRespawnWaiting(session, player.getUniqueId())) {
                player.setFallDistance(0.0F);
                player.teleport(spectatorWait(isBrickTeam(room, player.getUniqueId()) ? session.brickSpawn : session.netherSpawn));
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean shouldCancelFallDamage(GameRoom room, Player player) {
        if (!isBrickGuardRoom(room)) {
            return false;
        }
        Session session = getOrCreateSession(room);
        return room.getState() == RoomState.WAITING
                || room.getState() == RoomState.STARTING
                || room.getState() == RoomState.SELECTING
                || isRespawnWaiting(session, player.getUniqueId());
    }

    public int getBrickTeamCount(GameRoom room) {
        Session session = sessionOf(room);
        return session == null ? 0 : (int) session.brickTeam.stream().filter(room.getAllPlayerUUIDs()::contains).count();
    }

    public int getNetherTeamCount(GameRoom room) {
        Session session = sessionOf(room);
        return session == null ? 0 : (int) session.netherTeam.stream().filter(room.getAllPlayerUUIDs()::contains).count();
    }

    public int getCoreHealth(GameRoom room) {
        Session session = sessionOf(room);
        return session == null ? DEFAULT_CORE_HEALTH : session.coreHealth;
    }

    public int getMaxCoreHealth(GameRoom room) {
        Session session = sessionOf(room);
        return session == null ? DEFAULT_CORE_HEALTH : session.maxCoreHealth;
    }

    public String getCorePlayerName(GameRoom room) {
        Session session = sessionOf(room);
        if (session == null || session.corePlayer == null) {
            return "未选定";
        }
        Player online = Bukkit.getPlayer(session.corePlayer);
        return online != null ? online.getName() : Optional.ofNullable(Bukkit.getOfflinePlayer(session.corePlayer).getName()).orElse("未知");
    }

    public int getDyingCount(GameRoom room) {
        Session session = sessionOf(room);
        return session == null ? 0 : session.dyingPlayers.size();
    }

    public long getRemainingTimeSeconds(GameRoom room) {
        Session session = sessionOf(room);
        if (session == null || session.startTimeMillis <= 0L) {
            return DEFAULT_TIME_LIMIT_SECONDS;
        }
        long elapsed = Math.max(0L, (System.currentTimeMillis() - session.startTimeMillis) / 1000L);
        return Math.max(0L, session.timeLimitSeconds - elapsed);
    }

    public String getEndedSummary(GameRoom room) {
        Session session = sessionOf(room);
        return session == null || session.lastSummary == null ? "平局" : session.lastSummary;
    }

    public boolean isBrickTeam(GameRoom room, UUID uuid) {
        Session session = sessionOf(room);
        return session != null && session.brickTeam.contains(uuid);
    }

    public boolean isNetherTeam(GameRoom room, UUID uuid) {
        Session session = sessionOf(room);
        return session != null && session.netherTeam.contains(uuid);
    }

    public boolean isCorePlayer(GameRoom room, UUID uuid) {
        Session session = sessionOf(room);
        return session != null && uuid != null && uuid.equals(session.corePlayer);
    }

    public boolean isDying(GameRoom room, UUID uuid) {
        Session session = sessionOf(room);
        return session != null && session.dyingPlayers.containsKey(uuid);
    }

    public boolean isEliminated(GameRoom room, UUID uuid) {
        Session session = sessionOf(room);
        return session != null && isEliminated(session, uuid);
    }

    public String getRoleLabel(GameRoom room, UUID uuid) {
        if (room == null || uuid == null) {
            return "未选队";
        }
        Session session = getOrCreateSession(room);
        if (room.isSpectator(uuid) || isEliminated(session, uuid)) {
            return "旁观";
        }
        if (uuid.equals(session.corePlayer)) {
            return "下界核心";
        }
        if (session.dyingPlayers.containsKey(uuid)) {
            return "濒死";
        }
        return session.brickTeam.contains(uuid) ? BRICK_TEAM_NAME : session.netherTeam.contains(uuid) ? NETHER_TEAM_NAME : "未选队";
    }

    public String getTabPrefix(GameRoom room, UUID uuid) {
        if (room == null || uuid == null) {
            return "§7[未选队] §r";
        }
        Session session = getOrCreateSession(room);
        if (room.isSpectator(uuid) || isEliminated(session, uuid)) {
            return "§7[旁观] §r";
        }
        if (uuid.equals(session.corePlayer)) {
            return "§x§6§6§1§9§0§0[下界核心] §r";
        }
        if (session.dyingPlayers.containsKey(uuid)) {
            return "§x§D§D§8§8§F§F[濒死] §r";
        }
        if (session.brickTeam.contains(uuid)) {
            return "§x§F§F§7§C§0§0[板砖队] §r";
        }
        if (session.netherTeam.contains(uuid)) {
            return "§x§6§6§1§9§0§0[下界砖队] §r";
        }
        return "§7[未选队] §r";
    }

    public List<String> getTeamPreviewNames(GameRoom room, TeamSide side, int limit) {
        if (room == null || side == null) {
            return Collections.emptyList();
        }
        Session session = getOrCreateSession(room);
        Set<UUID> source = side == TeamSide.BRICK ? session.brickTeam : session.netherTeam;
        List<String> names = new ArrayList<>();
        for (UUID uuid : source) {
            if (!room.getAllPlayerUUIDs().contains(uuid)) {
                continue;
            }
            names.add(Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse("未知"));
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names.size() > limit ? names.subList(0, limit) : names;
    }

    public TeamSide getSelectedTeam(GameRoom room, UUID uuid) {
        if (room == null || uuid == null) {
            return null;
        }
        Session session = getOrCreateSession(room);
        if (session.brickTeam.contains(uuid)) {
            return TeamSide.BRICK;
        }
        if (session.netherTeam.contains(uuid)) {
            return TeamSide.NETHER;
        }
        return null;
    }

    public void selectTeam(GameRoom room, UUID uuid, TeamSide side) {
        if (!isBrickGuardRoom(room) || uuid == null || side == null) {
            return;
        }
        Session session = getOrCreateSession(room);
        session.brickTeam.remove(uuid);
        session.netherTeam.remove(uuid);
        if (side == TeamSide.BRICK) {
            session.brickTeam.add(uuid);
        } else {
            session.netherTeam.add(uuid);
        }
        refreshRoleDisplays(room);
        for (UUID member : room.getAllPlayerUUIDs()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                giveLobbyItems(online, room);
            }
        }
    }

    public boolean canJoinTeam(GameRoom room, UUID uuid, TeamSide side) {
        if (!isBrickGuardRoom(room) || uuid == null || side == null) {
            return false;
        }
        Session session = getOrCreateSession(room);
        int brick = getBrickTeamCount(room);
        int nether = getNetherTeamCount(room);
        TeamSide current = getSelectedTeam(room, uuid);
        if (current == TeamSide.BRICK) {
            brick = Math.max(0, brick - 1);
        } else if (current == TeamSide.NETHER) {
            nether = Math.max(0, nether - 1);
        }
        if (side == TeamSide.BRICK) {
            brick++;
        } else {
            nether++;
        }
        return Math.abs(brick - nether) <= 2;
    }

    public boolean transferCore(GameRoom room, Player actor, UUID newCore) {
        if (!isBrickGuardRoom(room) || actor == null || newCore == null) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (!isCoreTransferAvailable(session, actor.getUniqueId()) || !session.netherTeam.contains(newCore) || newCore.equals(session.corePlayer)) {
            return false;
        }
        Player oldCore = session.corePlayer == null ? null : Bukkit.getPlayer(session.corePlayer);
        Player target = Bukkit.getPlayer(newCore);
        if (target == null || !target.isOnline()) {
            return false;
        }
        clearCorePlayerState(oldCore);
        removeCoreTransferItem(oldCore);
        session.corePlayer = newCore;
        session.coreOrigin = target.getLocation().clone();
        session.coreTransferUsed = true;
        applyCorePlayerState(target);
        giveCoreTransferItem(target, false);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.core_transferred", placeholders));
        refreshRoleDisplays(room);
        return true;
    }

    public boolean forceCore(GameRoom room, UUID newCore) {
        if (!isBrickGuardRoom(room) || newCore == null) {
            return false;
        }
        Session session = getOrCreateSession(room);
        if (!session.netherTeam.contains(newCore)) {
            return false;
        }
        Player target = Bukkit.getPlayer(newCore);
        if (target == null || !target.isOnline()) {
            return false;
        }
        Player oldCore = session.corePlayer == null ? null : Bukkit.getPlayer(session.corePlayer);
        clearCorePlayerState(oldCore);
        removeCoreTransferItem(oldCore);
        session.corePlayer = newCore;
        session.coreOrigin = target.getLocation().clone();
        session.coreTransferUsed = true;
        applyCorePlayerState(target);
        giveCoreTransferItem(target, false);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.core_forced", placeholders));
        refreshRoleDisplays(room);
        return true;
    }

    public void forceEnd(GameRoom room, Boolean brickWin, String summary) {
        if (!isBrickGuardRoom(room)) {
            return;
        }
        endBrickGuardGame(room, brickWin, summary == null || summary.isBlank() ? "管理员强制结束" : summary);
    }

    public List<String> getAchievementLines(UUID uuid) {
        Set<String> unlocked = plugin.getPlayerDataManager().getBrickGuardAchievements(uuid);
        List<String> lines = new ArrayList<>();
        for (String id : ACHIEVEMENT_ORDER) {
            boolean has = unlocked.contains(id);
            lines.add((has ? "§a✔ " : "§7✘ ") + achievementName(id) + " §8- " + achievementDescription(id));
        }
        return lines;
    }

    public void handleInventoryClick(InventoryClickEvent event, Player player, GameRoom room) {
        if (!isBrickGuardRoom(room) || room.getState() != RoomState.PLAYING || player == null || event == null) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.MERCHANT || event.getRawSlot() != 2) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            GameRoom activeRoom = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            if (activeRoom == null || !activeRoom.getRoomId().equals(room.getRoomId()) || activeRoom.getState() != RoomState.PLAYING) {
                return;
            }
            Session session = getOrCreateSession(room);
            int trades = session.tradeCounts.merge(player.getUniqueId(), 1, Integer::sum);
            if (trades >= 3) {
                unlockAchievement(player, ACH_TRADE_MASTER);
            }
        });
    }

    public ItemStack createFoxPick(int level) {
        Material material = switch (level) {
            case 1, 2 -> Material.IRON_PICKAXE;
            case 3 -> Material.GOLDEN_PICKAXE;
            case 4 -> Material.DIAMOND_PICKAXE;
            default -> findMaterial("NETHERITE_PICKAXE", Material.DIAMOND_PICKAXE);
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§E§F§4§D§0§0狐稿 §8[" + level + "级]");
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §6板砖队专属成长稿子",
                    "§f- §c对下界核心玩家额外造成 +200% 伤害",
                    "§f- §7靠近板砖核心每秒恢复 3 点耐久",
                    "§8· · · · · · · · · · · · · ·"));
            meta.getPersistentDataContainer().set(foxPickKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(foxPickLevelKey, PersistentDataType.INTEGER, level);
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        int maxDurability = maxFoxDurability(material);
        setFoxPickMaxDurability(item, maxDurability);
        setFoxPickCurrentDurability(item, maxDurability);
        if (level >= 2) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, Math.min(level, 5));
        }
        return item;
    }

    public ItemStack createSpecialSnack(int amount) {
        ItemStack item = new ItemStack(Material.ROTTEN_FLESH, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§6§6§1§9§0§0下界特色小吃");
            meta.setCustomModelData(SPECIAL_SNACK_MODEL);
            meta.getPersistentDataContainer().set(specialSnackKey, PersistentDataType.BYTE, (byte) 1);
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §e濒死状态下食用可重置死亡倒计时",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(1).saturation(0.2F).canAlwaysEat(true));
        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(1.1F)
                .animation(ItemUseAnimation.EAT)
                .sound(Key.key("minecraft:entity.generic.eat"))
                .hasConsumeParticles(true));
        return item;
    }

    private void unlockAchievement(Player player, String achievementId) {
        if (player == null || achievementId == null || achievementId.isBlank()) {
            return;
        }
        if (!plugin.getPlayerDataManager().unlockBrickGuardAchievement(player.getUniqueId(), achievementId)) {
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", achievementName(achievementId));
        placeholders.put("description", achievementDescription(achievementId));
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.achievement_unlocked", placeholders));
        player.showTitle(Title.title(
                legacy.deserialize("§x§F§F§D§7§0§0成就达成"),
                legacy.deserialize(achievementName(achievementId)),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1800), Duration.ofMillis(350))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.82f, 1.08f);
    }

    private String achievementName(String achievementId) {
        return plugin.getMessageManager().getMessage("brick_guard_achievements." + achievementId + ".name");
    }

    private String achievementDescription(String achievementId) {
        return plugin.getMessageManager().getMessage("brick_guard_achievements." + achievementId + ".description");
    }

    private Session sessionOf(GameRoom room) {
        return room == null ? null : sessions.get(room.getRoomId());
    }

    private Session getOrCreateSession(GameRoom room) {
        return sessions.computeIfAbsent(room.getRoomId(), ignored -> new Session());
    }

    private void syncTeamsFromRoom(GameRoom room, Session session) {
        session.brickTeam.retainAll(room.getAllPlayerUUIDs());
        session.netherTeam.retainAll(room.getAllPlayerUUIDs());
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!session.brickTeam.contains(uuid) && !session.netherTeam.contains(uuid)) {
                if (session.brickTeam.size() <= session.netherTeam.size()) {
                    session.brickTeam.add(uuid);
                } else {
                    session.netherTeam.add(uuid);
                }
            }
        }
    }

    private void autoBalanceTeams(GameRoom room, Session session) {
        List<UUID> candidates = new ArrayList<>(room.getAllPlayerUUIDs());
        candidates.sort(Comparator.comparing(uuid -> Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(""), String.CASE_INSENSITIVE_ORDER));
        while (Math.abs(session.brickTeam.size() - session.netherTeam.size()) > 2 && !candidates.isEmpty()) {
            if (session.brickTeam.size() > session.netherTeam.size()) {
                UUID move = candidates.stream().filter(session.brickTeam::contains).findFirst().orElse(null);
                if (move == null) {
                    break;
                }
                session.brickTeam.remove(move);
                session.netherTeam.add(move);
            } else {
                UUID move = candidates.stream().filter(session.netherTeam::contains).findFirst().orElse(null);
                if (move == null) {
                    break;
                }
                session.netherTeam.remove(move);
                session.brickTeam.add(move);
            }
        }
    }

    private void prepareBrickWorld(World world, Session session) {
        int baseY = Math.max(world.getHighestBlockYAt(0, 0) + 8, 96);
        clearArea(world, 0, baseY - 2, 0, 26, 12);
        buildPlatform(world, 0, baseY - 1, 0, 12, Material.BRICKS, Material.ORANGE_TERRACOTTA);
        buildPortal(world, 5, baseY, 0);
        session.brickSpawn = new Location(world, 0.5D, baseY, 0.5D, -90.0F, 0.0F);
        session.brickCoreLocation = new Location(world, 0.5D, baseY, 10.5D, 180.0F, 0.0F);
        world.setSpawnLocation(session.brickSpawn);
        world.getBlockAt(session.brickCoreLocation).setType(Material.RED_GLAZED_TERRACOTTA, false);
        buildCorePad(world, session.brickCoreLocation, Material.RED_GLAZED_TERRACOTTA, Material.GRAY_CONCRETE);
        placeBrickResources(world, session);
    }

    private void prepareNetherWorld(World world, Session session) {
        int baseY = Math.max(world.getMinHeight() + 70, 90);
        clearArea(world, 0, baseY - 2, 0, 26, 12);
        buildPlatform(world, 0, baseY - 1, 0, 12, Material.NETHER_BRICKS, Material.BLACKSTONE);
        buildPortal(world, 5, baseY, 0);
        session.netherSpawn = new Location(world, 0.5D, baseY, 0.5D, -90.0F, 0.0F);
        world.setSpawnLocation(session.netherSpawn);
        placeNetherResources(world, session);
    }

    private void chooseCorePlayer(GameRoom room, Session session) {
        List<UUID> candidates = session.netherTeam.stream().filter(room.getAllPlayerUUIDs()::contains).toList();
        if (candidates.isEmpty()) {
            session.corePlayer = null;
            return;
        }
        session.corePlayer = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        Player core = Bukkit.getPlayer(session.corePlayer);
        if (core != null) {
            session.coreOrigin = core.getLocation().clone();
            applyCorePlayerState(core);
            giveCoreTransferItem(core, true);
            core.showTitle(Title.title(
                    legacy.deserialize("§x§6§6§1§9§0§0你为本队核心"),
                    legacy.deserialize("§f请小心谨慎"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ofMillis(500))
            ));
        }
    }

    private void refreshRoleDisplays(GameRoom room) {
        plugin.getRoomManager().clearAllRoleNameTags(room);
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (room.isSpectator(uuid) || isEliminated(room, uuid)) {
                plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
                continue;
            }
            String label = getRoleLabel(room, uuid);
            plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(), false, label);
            plugin.getRoomManager().updatePlayerTabNameWithRole(player, room.getRoomId(), false, label);
        }
    }

    private void startGuardTask(GameRoom room, Session session) {
        BukkitTask old = roomTasks.remove(room.getRoomId());
        if (old != null) {
            old.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || !room.getGameMode().isBrickGuard()) {
                    cancel();
                    return;
                }
                long elapsed = Math.max(0L, (System.currentTimeMillis() - session.startTimeMillis) / 1000L);
                if (elapsed >= session.timeLimitSeconds) {
                    endBrickGuardGame(room, null, "一小时到时，双方平局");
                    cancel();
                    return;
                }
                tickDyingPlayers(room, session);
                tickFoxPickRecovery(room, session);
                maintainCoreState(session);
                checkBrickVictory(room, session);
                maintainCoreTransferWindow(session);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        roomTasks.put(room.getRoomId(), task);
    }

    private void tickDyingPlayers(GameRoom room, Session session) {
        long now = System.currentTimeMillis();
        for (UUID uuid : new ArrayList<>(session.dyingPlayers.keySet())) {
            if (room.isSpectator(uuid) || isRespawnWaiting(session, uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            applyDyingState(player);
            DyingState state = session.dyingPlayers.get(uuid);
            if (state != null && now >= state.expiresAtMs()) {
                player.setHealth(0.0D);
            }
        }
    }

    private void tickFoxPickRecovery(GameRoom room, Session session) {
        for (UUID uuid : session.brickTeam) {
            if (!room.getAllPlayerUUIDs().contains(uuid) || room.isSpectator(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (session.brickCoreLocation != null
                    && player.getWorld().equals(session.brickCoreLocation.getWorld())
                    && player.getLocation().distanceSquared(session.brickCoreLocation) <= 64.0D) {
                repairFoxPick(player, FOX_PICK_REPAIR_PER_SECOND);
            }
            applyFoxPickMiningPenalty(player);
        }
    }

    private void maintainCoreState(Session session) {
        if (session.corePlayer == null) {
            return;
        }
        Player player = Bukkit.getPlayer(session.corePlayer);
        if (player != null) {
            applyCorePlayerState(player);
        }
    }

    private void maintainCoreTransferWindow(Session session) {
        if (session.corePlayer == null) {
            return;
        }
        Player player = Bukkit.getPlayer(session.corePlayer);
        if (player == null) {
            return;
        }
        if (session.coreTransferUsed || System.currentTimeMillis() > session.startTimeMillis + CORE_TRANSFER_WINDOW_MS) {
            removeCoreTransferItem(player);
        }
    }

    private void triggerCoreDeath(GameRoom room, Session session) {
        session.coreTransferUsed = true;
        List<UUID> candidates = new ArrayList<>();
        for (UUID uuid : session.netherTeam) {
            if (!room.getAllPlayerUUIDs().contains(uuid) || uuid.equals(session.corePlayer) || room.isSpectator(uuid) || session.eliminatedNetherPlayers.contains(uuid)) {
                continue;
            }
            candidates.add(uuid);
        }
        Collections.shuffle(candidates);
        int dyingCount = candidates.isEmpty() ? 0 : Math.max(1, candidates.size() / 2);
        for (int i = 0; i < dyingCount && i < candidates.size(); i++) {
            UUID uuid = candidates.get(i);
            session.dyingPlayers.put(uuid, new DyingState(System.currentTimeMillis() + DYING_DURATION_MS, false));
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyDyingState(player);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.dying_started"));
            }
        }
        room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.core_player_dead"));
        checkBrickVictory(room, session);
    }

    private void endBrickGuardGame(GameRoom room, Boolean brickWin, String summary) {
        if (room == null || room.getState() == RoomState.ENDED) {
            return;
        }
        Session session = getOrCreateSession(room);
        session.lastSummary = summary;
        BukkitTask task = roomTasks.remove(room.getRoomId());
        if (task != null) {
            task.cancel();
        }
        room.setState(RoomState.ENDED);
        room.setPreyWon(Boolean.TRUE.equals(brickWin));

        if (brickWin == null) {
            room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.draw"));
        } else if (brickWin) {
            room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.brick_win"));
        } else {
            room.broadcast(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.nether_win"));
        }
        if (summary != null && !summary.isBlank()) {
            room.broadcast(plugin.getConfigManager().getBrickGuardPrefix() + "§7原因: §f" + summary);
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            boolean winner = brickWin != null && (brickWin ? isBrickTeam(room, uuid) : isNetherTeam(room, uuid));
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvulnerable(true);
            clearBrickGuardPlayerState(player);
            if (brickWin != null) {
                if (winner) {
                    unlockAchievement(player, ACH_FIRST_WIN);
                }
                plugin.getPlayerDataManager().addMiniGamePoints(uuid, winner ? WIN_POINTS : PARTICIPATE_POINTS, room.getGameMode());
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("points", String.valueOf(winner ? WIN_POINTS : PARTICIPATE_POINTS));
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix(winner ? "points.minigame_win" : "points.minigame_participate", placeholders));
            }
        }

        new BukkitRunnable() {
            int countdown = 10;
            @Override
            public void run() {
                if (countdown <= 0) {
                    Set<UUID> restoreTargets = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    restoreTargets.addAll(room.getSpectators());
                    for (UUID uuid : restoreTargets) {
                        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
                    }
                    plugin.getRoomManager().clearAllRoleNameTags(room);
                    plugin.getRoomManager().deleteRoom(room.getRoomId());
                    cancel();
                    return;
                }
                if (countdown <= 5) {
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(plugin.getConfigManager().getBrickGuardPrefix() + "§c" + countdown + " §7秒后关闭...");
                        }
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkBrickVictory(GameRoom room, Session session) {
        if (session.corePlayer == null || !session.eliminatedNetherPlayers.contains(session.corePlayer)) {
            return;
        }
        for (UUID uuid : session.netherTeam) {
            if (!room.getAllPlayerUUIDs().contains(uuid) || uuid.equals(session.corePlayer)) {
                continue;
            }
            if (!room.isSpectator(uuid) && !session.eliminatedNetherPlayers.contains(uuid)) {
                return;
            }
        }
        endBrickGuardGame(room, true, "下界核心玩家与全部下界队员已被击败");
    }

    private void scheduleRespawn(Player player, GameRoom room, Session session, RespawnState state) {
        UUID uuid = player.getUniqueId();
        BukkitTask old = respawnTasks.remove(uuid);
        if (old != null) {
            old.cancel();
        }
        long delayTicks = Math.max(20L, (state.readyAtMs() - System.currentTimeMillis()) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnTasks.remove(uuid);
            if (!player.isOnline() || room.getState() != RoomState.PLAYING) {
                return;
            }
            session.pendingRespawns.remove(uuid);
            player.teleport(state.respawnLocation());
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setInvulnerable(false);
            player.setNoDamageTicks(40);
            if (!uuid.equals(session.corePlayer)) {
                clearCorePlayerState(player);
            }
            resetAttackSpeed(player);
            if (state.brickTeam()) {
                equipBrickArmor(player);
            } else {
                equipNetherArmor(player, uuid.equals(session.corePlayer));
            }
            refreshRoleDisplays(room);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.respawned"));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.75f, 1.2f);
        }, delayTicks);
        respawnTasks.put(uuid, task);
    }

    private void finishTotemRespawn(Player player, GameRoom room, Session session) {
        UUID uuid = player.getUniqueId();
        session.pendingRespawns.remove(uuid);
        player.teleport(session.coreOrigin == null ? session.netherSpawn : session.coreOrigin);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setNoDamageTicks(60);
        setPlayerMaxHealth(player, TOTEM_MAX_HEALTH);
        player.setHealth(Math.min(TOTEM_MAX_HEALTH, player.getAttribute(Attribute.MAX_HEALTH) == null ? TOTEM_MAX_HEALTH : player.getAttribute(Attribute.MAX_HEALTH).getValue()));
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0D, 1.0D, 0.0D), 28, 0.35D, 0.45D, 0.35D, 0.08D);
        unlockAchievement(player, ACH_DYING_SURVIVOR);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.totem_used"));
        refreshRoleDisplays(room);
    }

    private void convertToSpectator(Player player, GameRoom room, Location location) {
        if (player == null || room == null || !player.isOnline()) {
            return;
        }
        if (!room.isSpectator(player.getUniqueId())) {
            room.addSpectator(player.getUniqueId());
        }
        player.teleport(location == null ? player.getLocation() : location);
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        plugin.getGameManager().giveSpectatorItems(player);
        plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
    }

    private void tryUpgradeFoxPick(Player player, GameRoom room, Session session) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFoxPick(item)) {
            return;
        }
        int level = getFoxPickLevel(item);
        if (level >= 5) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.fox_pick_max"));
            return;
        }
        int needKills = level * 2;
        int needBrick = 24 + level * 12;
        int needDiamond = 2 + level * 2;
        if (session.kills.getOrDefault(player.getUniqueId(), 0) < needKills) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kills", String.valueOf(needKills));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.fox_pick_need_kills", placeholders));
            return;
        }
        if (!hasEnough(player, Material.BRICK, needBrick) || !hasEnough(player, Material.DIAMOND, needDiamond)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("brick", String.valueOf(needBrick));
            placeholders.put("diamond", String.valueOf(needDiamond));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.fox_pick_need_materials", placeholders));
            return;
        }
        take(player, Material.BRICK, needBrick);
        take(player, Material.DIAMOND, needDiamond);
        double ratio = Math.max(0.1D, getFoxPickCurrentDurability(item) / (double) Math.max(1, getFoxPickMaxDurability(item)));
        ItemStack upgraded = createFoxPick(level + 1);
        setFoxPickCurrentDurability(upgraded, Math.max(1, (int) Math.round(getFoxPickMaxDurability(upgraded) * ratio)));
        player.getInventory().setItemInMainHand(upgraded);
        if (level + 1 >= 5) {
            unlockAchievement(player, ACH_FOX_PICK_MASTER);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.fox_pick_upgraded"));
    }

    private Merchant createVillagerMerchant() {
        Merchant merchant = Bukkit.createMerchant("板砖商人");
        merchant.setRecipes(List.of(
                recipe(createNamedItem(Material.IRON_SWORD, "§x§F§F§7§C§0§0板砖短剑"), ingredient(Material.BRICK, 18), ingredient(Material.DIAMOND, 2)),
                recipe(new ItemStack(Material.CROSSBOW), ingredient(Material.BRICK, 22), ingredient(Material.DIAMOND, 2)),
                recipe(new ItemStack(Material.COOKED_BEEF, 12), ingredient(Material.BRICK, 8)),
                recipe(new ItemStack(Material.BRICKS, 32), ingredient(Material.BRICK, 12)),
                recipe(new ItemStack(Material.IRON_CHESTPLATE), ingredient(Material.BRICK, 26), ingredient(Material.DIAMOND, 4))
        ));
        return merchant;
    }

    private Merchant createPiglinMerchant() {
        Merchant merchant = Bukkit.createMerchant("下界商旅");
        merchant.setRecipes(List.of(
                recipe(createNamedItem(Material.IRON_SWORD, "§x§6§6§1§9§0§0下界短剑"), ingredient(Material.GOLD_NUGGET, 18), ingredient(Material.GLOWSTONE_DUST, 8)),
                recipe(new ItemStack(Material.CROSSBOW), ingredient(Material.GOLD_NUGGET, 20), ingredient(Material.GLOWSTONE_DUST, 10)),
                recipe(new ItemStack(findMaterial("NETHERITE_PICKAXE", Material.DIAMOND_PICKAXE)), ingredient(Material.GOLD_NUGGET, 28), ingredient(Material.GLOWSTONE_DUST, 14)),
                recipe(new ItemStack(Material.NETHER_BRICKS, 32), ingredient(Material.GOLD_NUGGET, 10), ingredient(Material.GLOWSTONE_DUST, 6)),
                recipe(createSpecialSnack(4), ingredient(Material.GOLD_NUGGET, 6), ingredient(Material.GLOWSTONE_DUST, 3))
        ));
        return merchant;
    }

    private void spawnBrickVillagers(World world, Session session) {
        clearNpcEntities(session.brickVillagers);
        session.brickVillagers.clear();
        int count = ThreadLocalRandom.current().nextInt(3, 5);
        for (int i = 0; i < count; i++) {
            Location location = session.brickSpawn.clone().add(-5 + i * 3, 0.0D, 5 + (i % 2) * 2);
            Villager villager = world.spawn(location, Villager.class, spawned -> {
                spawned.setAI(false);
                spawned.setAdult();
                spawned.setRemoveWhenFarAway(false);
                spawned.customName(legacy.deserialize("§x§F§F§7§C§0§0板砖商人"));
                spawned.setCustomNameVisible(true);
                spawned.getPersistentDataContainer().set(npcRoleKey, PersistentDataType.INTEGER, 1);
            });
            session.brickVillagers.add(villager.getUniqueId());
        }
    }

    private void spawnNetherPiglins(World world, Session session) {
        clearNpcEntities(session.netherPiglins);
        session.netherPiglins.clear();
        for (int i = 0; i < 3; i++) {
            Location location = session.netherSpawn.clone().add(-4 + i * 4, 0.0D, 5.0D);
            Piglin piglin = world.spawn(location, Piglin.class, spawned -> {
                spawned.setAI(false);
                spawned.setAdult();
                spawned.setImmuneToZombification(true);
                spawned.setRemoveWhenFarAway(false);
                spawned.customName(legacy.deserialize("§x§6§6§1§9§0§0下界商旅猪灵"));
                spawned.setCustomNameVisible(true);
                spawned.getPersistentDataContainer().set(npcRoleKey, PersistentDataType.INTEGER, 2);
            });
            session.netherPiglins.add(piglin.getUniqueId());
        }
    }

    private void placeBrickResources(World world, Session session) {
        for (int x = -9; x <= -3; x++) {
            for (int z = -9; z <= -3; z++) {
                if ((x + z) % 3 == 0) {
                    setManagedResource(world, session, x, session.brickSpawn.getBlockY(), z, Material.BRICKS);
                }
            }
        }
        for (int x = 3; x <= 9; x += 2) {
            for (int z = -9; z <= -3; z += 2) {
                setManagedResource(world, session, x, session.brickSpawn.getBlockY(), z, Material.DIAMOND_ORE);
            }
        }
    }

    private void placeNetherResources(World world, Session session) {
        for (int x = -9; x <= -3; x++) {
            for (int z = -9; z <= -3; z++) {
                if ((x + z) % 2 == 0) {
                    setManagedResource(world, session, x, session.netherSpawn.getBlockY(), z, Material.GLOWSTONE);
                }
            }
        }
        for (int x = 3; x <= 9; x += 2) {
            for (int z = -9; z <= -3; z += 2) {
                setManagedResource(world, session, x, session.netherSpawn.getBlockY(), z, Material.NETHER_GOLD_ORE);
            }
        }
    }

    private void setManagedResource(World world, Session session, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
        session.managedResources.put(key(new Location(world, x, y, z)), material);
    }

    private void handleManagedResourceBreak(Block block, Player player, Material original) {
        Material dropType;
        int minAmount;
        int maxAmount;
        if (original == Material.BRICKS) {
            dropType = Material.BRICK;
            minAmount = 2;
            maxAmount = 4;
        } else if (original == Material.DIAMOND_ORE || original == Material.DEEPSLATE_DIAMOND_ORE) {
            dropType = Material.DIAMOND;
            minAmount = 1;
            maxAmount = 1;
        } else if (original == Material.GLOWSTONE) {
            dropType = Material.GLOWSTONE_DUST;
            minAmount = 3;
            maxAmount = 5;
        } else if (original == Material.NETHER_GOLD_ORE) {
            dropType = Material.GOLD_NUGGET;
            minAmount = 3;
            maxAmount = 6;
        } else {
            dropType = original;
            minAmount = 1;
            maxAmount = 1;
        }
        int amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), new ItemStack(dropType, amount));
        block.setType(Material.AIR, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(original, false),
                original == Material.DIAMOND_ORE || original == Material.NETHER_GOLD_ORE ? 140L : 100L);
        reduceFoxPickDurability(player, 1);
    }

    private void broadcastBrickCoreUnderAttack(GameRoom room) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!isBrickTeam(room, uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.showTitle(Title.title(legacy.deserialize(" "), legacy.deserialize("§x§F§F§5§5§5§5核心受到攻击"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(150))));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.65f, 0.85f);
        }
    }

    private void scheduleImmediateRespawn(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Throwable ignored) {
            }
        }, 1L);
    }

    private boolean isRespawnWaiting(Session session, UUID uuid) {
        RespawnState state = session.pendingRespawns.get(uuid);
        return state != null && state.readyAtMs() > System.currentTimeMillis();
    }

    private void applyCorePlayerState(Player player) {
        if (player == null) {
            return;
        }
        setPlayerMaxHealth(player, CORE_MAX_HEALTH);
        if (player.getHealth() < CORE_MAX_HEALTH) {
            player.setHealth(Math.min(CORE_MAX_HEALTH, player.getAttribute(Attribute.MAX_HEALTH) == null ? CORE_MAX_HEALTH : player.getAttribute(Attribute.MAX_HEALTH).getValue()));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false));
    }

    private void clearCorePlayerState(Player player) {
        if (player == null) {
            return;
        }
        setPlayerMaxHealth(player, NORMAL_MAX_HEALTH);
        if (player.getHealth() > NORMAL_MAX_HEALTH) {
            player.setHealth(NORMAL_MAX_HEALTH);
        }
        player.removePotionEffect(PotionEffectType.GLOWING);
    }

    private void clearBrickGuardPlayerState(Player player) {
        if (player == null) {
            return;
        }
        clearCorePlayerState(player);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        resetAttackSpeed(player);
    }

    private void resetAttackSpeed(Player player) {
        if (player != null && player.getAttribute(Attribute.ATTACK_SPEED) != null) {
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.0D);
        }
    }

    private void applyDyingState(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false, false));
        if (player.getAttribute(Attribute.ATTACK_SPEED) != null) {
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(4.6D);
        }
    }

    private void setPlayerMaxHealth(Player player, double value) {
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(value);
        }
    }

    private void buildPlatform(World world, int centerX, int y, int centerZ, int radius, Material centerMaterial, Material edgeMaterial) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Material material = Math.abs(x) == radius || Math.abs(z) == radius ? edgeMaterial : centerMaterial;
                world.getBlockAt(centerX + x, y, centerZ + z).setType(material, false);
                world.getBlockAt(centerX + x, y + 1, centerZ + z).setType(Material.AIR, false);
                world.getBlockAt(centerX + x, y + 2, centerZ + z).setType(Material.AIR, false);
            }
        }
    }

    private void buildCorePad(World world, Location core, Material coreMaterial, Material surround) {
        int x = core.getBlockX();
        int y = core.getBlockY() - 1;
        int z = core.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(surround, false);
            }
        }
        world.getBlockAt(core).setType(coreMaterial, false);
    }

    private void clearArea(World world, int centerX, int baseY, int centerZ, int radius, int height) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= height; y++) {
                    world.getBlockAt(centerX + x, baseY + y, centerZ + z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void buildPortal(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 2; dx++) {
            world.getBlockAt(x + dx, y - 1, z).setType(Material.OBSIDIAN, false);
            world.getBlockAt(x + dx, y + 3, z).setType(Material.OBSIDIAN, false);
        }
        for (int dy = 0; dy < 3; dy++) {
            world.getBlockAt(x - 1, y + dy, z).setType(Material.OBSIDIAN, false);
            world.getBlockAt(x + 2, y + dy, z).setType(Material.OBSIDIAN, false);
        }
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                Block block = world.getBlockAt(x + dx, y + dy, z);
                block.setType(Material.NETHER_PORTAL, false);
                if (block.getBlockData() instanceof Orientable orientable) {
                    orientable.setAxis(org.bukkit.Axis.Z);
                    block.setBlockData(orientable, false);
                }
            }
        }
    }

    private void equipBrickPlayer(Player player) {
        equipBrickArmor(player);
        player.getInventory().setItem(0, createFoxPick(1));
        player.getInventory().addItem(new ItemStack(Material.BREAD, 8));
        player.getInventory().addItem(new ItemStack(Material.BRICKS, 16));
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        player.getInventory().addItem(new ItemStack(Material.CROSSBOW));
    }

    private void equipBrickArmor(Player player) {
        ItemStack helmet = leather(Material.LEATHER_HELMET, Color.fromRGB(0xEF, 0x4D, 0x00));
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack leggings = leather(Material.LEATHER_LEGGINGS, Color.fromRGB(0xEF, 0x4D, 0x00));
        ItemStack boots = leather(Material.LEATHER_BOOTS, Color.fromRGB(0xEF, 0x4D, 0x00));
        player.getInventory().setArmorContents(new ItemStack[]{boots, leggings, chest, helmet});
    }

    private void equipNetherPlayer(Player player, boolean core) {
        equipNetherArmor(player, core);
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.CROSSBOW));
        player.getInventory().addItem(new ItemStack(findMaterial("SPEAR", Material.TRIDENT)));
        player.getInventory().addItem(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 6));
        player.getInventory().addItem(createSpecialSnack(4));
        if (core) {
            player.getInventory().setItem(8, createCoreTransferItem(true));
        }
    }

    private void equipNetherArmor(Player player, boolean core) {
        ItemStack helmet = core ? new ItemStack(Material.IRON_HELMET) : leather(Material.LEATHER_HELMET, Color.fromRGB(0x66, 0x19, 0x00));
        ItemStack chest = core ? new ItemStack(Material.DIAMOND_CHESTPLATE) : new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack leggings = leather(Material.LEATHER_LEGGINGS, Color.fromRGB(0x66, 0x19, 0x00));
        ItemStack boots = leather(Material.LEATHER_BOOTS, Color.fromRGB(0x66, 0x19, 0x00));
        player.getInventory().setArmorContents(new ItemStack[]{boots, leggings, chest, helmet});
    }

    private ItemStack leather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isFoxPick(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(foxPickKey, PersistentDataType.BYTE);
    }

    private int getFoxPickLevel(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(foxPickLevelKey, PersistentDataType.INTEGER, 1);
    }

    private int getFoxPickMaxDurability(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(foxPickMaxDurabilityKey, PersistentDataType.INTEGER, maxFoxDurability(item.getType()));
    }

    private int getFoxPickCurrentDurability(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(foxPickCurrentDurabilityKey, PersistentDataType.INTEGER, getFoxPickMaxDurability(item));
    }

    private void setFoxPickMaxDurability(ItemStack item, int value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(foxPickMaxDurabilityKey, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    private void setFoxPickCurrentDurability(ItemStack item, int value) {
        ItemMeta meta = item.getItemMeta();
        int max = getFoxPickMaxDurability(item);
        int current = Math.max(0, Math.min(max, value));
        meta.getPersistentDataContainer().set(foxPickCurrentDurabilityKey, PersistentDataType.INTEGER, current);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(Math.max(0, max - current));
        }
        item.setItemMeta(meta);
    }

    private void degradeFoxPick(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFoxPick(item)) {
                int current = getFoxPickCurrentDurability(item);
                int max = getFoxPickMaxDurability(item);
                setFoxPickCurrentDurability(item, Math.max(1, current - Math.max(1, max / 5)));
            }
        }
    }

    private void reduceFoxPickDurability(Player player, int amount) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isFoxPick(item)) {
            setFoxPickCurrentDurability(item, getFoxPickCurrentDurability(item) - amount);
        }
    }

    private void repairFoxPick(Player player, int amount) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isFoxPick(item)) {
            setFoxPickCurrentDurability(item, getFoxPickCurrentDurability(item) + amount);
        }
    }

    private void restoreFoxPickDurability(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFoxPick(item)) {
                setFoxPickCurrentDurability(item, getFoxPickMaxDurability(item));
            }
        }
    }

    private void applyFoxPickMiningPenalty(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFoxPick(item)) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            return;
        }
        double ratio = getFoxPickCurrentDurability(item) / (double) Math.max(1, getFoxPickMaxDurability(item));
        if (ratio <= 0.25D) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 1, false, false, false));
        } else if (ratio <= 0.50D) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 0, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
    }

    private int maxFoxDurability(Material material) {
        return switch (material) {
            case IRON_PICKAXE -> 250;
            case GOLDEN_PICKAXE -> 32;
            case DIAMOND_PICKAXE -> 1561;
            default -> material == findMaterial("NETHERITE_PICKAXE", Material.DIAMOND_PICKAXE) ? 2031 : 250;
        };
    }

    private MerchantRecipe recipe(ItemStack result, ItemStack... ingredients) {
        MerchantRecipe recipe = new MerchantRecipe(result, Integer.MAX_VALUE);
        for (ItemStack ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }

    private ItemStack ingredient(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private ItemStack createNamedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void clearNpcEntities(Set<UUID> uuids) {
        for (UUID uuid : uuids) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private ItemStack createTeamSelectorItem(Player player, GameRoom room, Session session) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            TeamSide side = getSelectedTeam(room, player.getUniqueId());
            meta.setDisplayName("§x§F§F§7§C§0§0队伍指南针");
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §7当前选择: " + (side == TeamSide.BRICK ? BRICK_TEAM_DISPLAY : side == TeamSide.NETHER ? NETHER_TEAM_DISPLAY : "§7未选择"),
                    "§f- §a右键打开队伍选择界面",
                    "§f- §7板砖: §e" + getBrickTeamCount(room) + " §8| §7下界: §e" + getNetherTeamCount(room),
                    "§8· · · · · · · · · · · · · ·"));
            meta.setCustomModelData(TEAM_SELECTOR_MODEL);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createForceStartItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§7§7§5§5⚡ §x§F§F§9§9§7§7强§x§F§F§B§B§9§9制§x§F§F§D§D§B§B开始");
            meta.setCustomModelData(10002);
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c管理员专用",
                    "§f- §e右键强制开始板砖守卫战",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAdvertiseItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§D§7§0§0📢 §x§F§F§B§B§3§3宣传房间");
            meta.setCustomModelData(10003);
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §e右键发送宣传消息",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQuitItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退出游戏");
            meta.setCustomModelData(10004);
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c右键退出房间",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCoreTransferItem(boolean active) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§6§6§1§9§0§0核心转移文书");
            meta.setCustomModelData(CORE_TRANSFER_MODEL);
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §7开局 1 分钟内可转移一次核心身份",
                    active ? "§f- §a右键选择一名队友成为新核心" : "§f- §7该文书已失效",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveCoreTransferItem(Player player, boolean active) {
        if (player != null) {
            player.getInventory().setItem(8, createCoreTransferItem(active));
        }
    }

    private void removeCoreTransferItem(Player player) {
        if (player == null) {
            return;
        }
        ItemStack item = player.getInventory().getItem(8);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()
                && item.getItemMeta().getCustomModelData() == CORE_TRANSFER_MODEL) {
            player.getInventory().setItem(8, null);
        }
    }

    private boolean isCoreTransferAvailable(Session session, UUID uuid) {
        return session != null
                && uuid != null
                && uuid.equals(session.corePlayer)
                && !session.coreTransferUsed
                && System.currentTimeMillis() <= session.startTimeMillis + CORE_TRANSFER_WINDOW_MS;
    }

    private boolean isEliminated(Session session, UUID uuid) {
        return session.eliminatedBrickPlayers.contains(uuid) || session.eliminatedNetherPlayers.contains(uuid);
    }

    private boolean sameBlock(Location a, Location b) {
        return a != null && b != null
                && a.getWorld() != null && b.getWorld() != null
                && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private Material findMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private Player resolveAttackingPlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean isAnyPickaxe(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_PICKAXE");
    }

    private int getCoreDamageFromTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 1;
        }
        return switch (item.getType()) {
            case WOODEN_PICKAXE, STONE_PICKAXE -> 2;
            case IRON_PICKAXE, GOLDEN_PICKAXE -> 3;
            case DIAMOND_PICKAXE -> 4;
            default -> item.getType() == findMaterial("NETHERITE_PICKAXE", Material.DIAMOND_PICKAXE) ? 5 : 1;
        };
    }

    private Location resolveNetherRespawn(Session session) {
        Location base = session.netherSpawn == null ? session.coreOrigin : session.netherSpawn;
        if (base == null) {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return world == null ? new Location(null, 0.5D, 100.0D, 0.5D) : new Location(world, 0.5D, 100.0D, 0.5D);
        }
        if (isSafeRespawn(base)) {
            return centered(base);
        }
        if (session.coreOrigin != null) {
            Location nearby = findNearbySafeRespawn(session.coreOrigin, 2, 4);
            if (nearby != null) {
                return nearby;
            }
            return centered(session.coreOrigin);
        }
        return centered(base);
    }

    private boolean isSafeRespawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.getBlockAt(bx + x, by + y, bz + z).getType().isSolid()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Location findNearbySafeRespawn(Location center, int minRadius, int maxRadius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        for (int radius = minRadius; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) + Math.abs(z) < minRadius) {
                        continue;
                    }
                    for (int y = -2; y <= 2; y++) {
                        Location target = center.clone().add(x, y, z);
                        if (isSafeRespawn(target)) {
                            return centered(target);
                        }
                    }
                }
            }
        }
        return null;
    }

    private Location spectatorWait(Location location) {
        if (location == null) {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return world == null ? new Location(null, 0.5D, 100.0D, 0.5D) : new Location(world, 0.5D, 100.0D, 0.5D);
        }
        return centered(location.clone().add(0.0D, 6.0D, 0.0D));
    }

    private Location centered(Location location) {
        location.setX(location.getBlockX() + 0.5D);
        location.setZ(location.getBlockZ() + 0.5D);
        return location;
    }

    private double randomOffset(double max) {
        return ThreadLocalRandom.current().nextDouble(-max, max);
    }

    private int randomRespawnSeconds() {
        return ThreadLocalRandom.current().nextInt(DEFAULT_RESPAWN_MIN_SECONDS, DEFAULT_RESPAWN_MAX_SECONDS + 1);
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ':' + location.getBlockX() + ':' + location.getBlockY() + ':' + location.getBlockZ();
    }

    private boolean hasEnough(Player player, Material material, int amount) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total >= amount;
    }

    private void take(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }
            int remove = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            remaining -= remove;
            if (item.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    private MapConfig loadActiveMap(BrickGuardMapManager.MapDefinition definition) {
        FileConfiguration config = plugin.getConfigManager().getConfig(CONFIG_NAME);
        String active = definition == null ? DEFAULT_MAP_ID : definition.mapId();
        String displayName = definition == null ? "默认板砖战场" : definition.displayName();
        int boundary = definition == null ? DEFAULT_BOUNDARY_RADIUS : (int) Math.round(definition.fakeBorderRadius());
        if (config == null) {
            return new MapConfig(active, displayName, DEFAULT_CORE_HEALTH, boundary, DEFAULT_TIME_LIMIT_SECONDS);
        }
        String path = "maps." + active;
        if (!config.isConfigurationSection(path)) {
            path = "maps." + config.getString("active_map", DEFAULT_MAP_ID);
        }
        return new MapConfig(
                active,
                config.getString(path + ".display_name", displayName),
                Math.max(100, config.getInt(path + ".core_health", DEFAULT_CORE_HEALTH)),
                Math.max(200, config.getInt(path + ".pseudo_boundary_radius", boundary)),
                Math.max(300, config.getInt(path + ".game_time_limit_seconds", DEFAULT_TIME_LIMIT_SECONDS))
        );
    }

    private void applyRuntimeLocations(GameRoom room, Session session, BrickGuardMapManager.MapDefinition definition,
                                       World brickWorld, World netherWorld, MapConfig mapConfig) {
        Location brickSpawn = room.getBrickGuardBrickSpawn();
        Location netherSpawn = room.getBrickGuardNetherBrickSpawn();
        Location core = room.getBrickGuardCoreLocation();
        Location borderCenter = room.getBrickGuardFakeBorderCenter();
        if (definition != null && plugin.getBrickGuardMapManager() != null) {
            if (brickSpawn == null) {
                brickSpawn = plugin.getBrickGuardMapManager().getBrickSpawn(definition, brickWorld);
            }
            if (netherSpawn == null) {
                netherSpawn = plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, netherWorld);
            }
            if (core == null) {
                core = plugin.getBrickGuardMapManager().getBrickCore(definition, brickWorld);
            }
            if (borderCenter == null) {
                borderCenter = plugin.getBrickGuardMapManager().getFakeBorderCenter(definition, brickWorld);
            }
        }
        session.brickSpawn = brickSpawn == null ? null : centered(brickSpawn.clone());
        session.netherSpawn = netherSpawn == null ? null : centered(netherSpawn.clone());
        session.brickCoreLocation = core == null ? null : centered(core.clone());
        if (definition != null) {
            room.setBrickGuardRuntimeSettings(mapConfig.id(), mapConfig.displayName(), session.brickSpawn,
                    session.netherSpawn, session.brickCoreLocation, borderCenter, mapConfig.boundaryRadius());
        }
    }

    private void cancelBlockPlaceAndResync(BlockPlaceEvent event, Player player) {
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    private record DyingState(long expiresAtMs, boolean totemUsed) {
    }

    private record RespawnState(boolean brickTeam, long readyAtMs, Location respawnLocation, Location waitLocation) {
    }

    private record MapConfig(String id, String displayName, int coreHealth, int boundaryRadius, int timeLimitSeconds) {
    }

    private static class Session {
        private final Set<UUID> brickTeam = new LinkedHashSet<>();
        private final Set<UUID> netherTeam = new LinkedHashSet<>();
        private final Set<UUID> eliminatedBrickPlayers = new HashSet<>();
        private final Set<UUID> eliminatedNetherPlayers = new HashSet<>();
        private final Set<UUID> brickVillagers = new HashSet<>();
        private final Set<UUID> netherPiglins = new HashSet<>();
        private final Map<UUID, DyingState> dyingPlayers = new HashMap<>();
        private final Map<UUID, RespawnState> pendingRespawns = new HashMap<>();
        private final Map<UUID, Integer> kills = new HashMap<>();
        private final Map<UUID, Integer> tradeCounts = new HashMap<>();
        private final Map<String, Material> managedResources = new HashMap<>();
        private UUID corePlayer;
        private Location coreOrigin;
        private Location brickSpawn;
        private Location netherSpawn;
        private Location brickCoreLocation;
        private int coreHealth = DEFAULT_CORE_HEALTH;
        private int maxCoreHealth = DEFAULT_CORE_HEALTH;
        private int boundaryRadius = DEFAULT_BOUNDARY_RADIUS;
        private int timeLimitSeconds = DEFAULT_TIME_LIMIT_SECONDS;
        private long startTimeMillis;
        private boolean coreTransferUsed;
        private String mapId = DEFAULT_MAP_ID;
        private String mapName = "默认板砖战场";
        private String lastSummary = "平局";
    }
}
