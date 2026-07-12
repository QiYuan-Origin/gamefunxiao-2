package org.brickguard;

import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

final class Room {
    enum Status { WAITING, RUNNING, ENDED }

    final String id;
    final String runtimeKey;
    final UUID owner;
    final Set<UUID> players = new LinkedHashSet<>();
    final Map<UUID, Team> teams = new HashMap<>();
    final Set<UUID> spectators = new HashSet<>();
    final Set<UUID> finalDead = new HashSet<>();
    final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();
    final List<Entity> shopEntities = new ArrayList<>();
    final List<Entity> guardEntities = new ArrayList<>();
    final Map<UUID, Double> shopHealth = new HashMap<>();
    final Map<UUID, Integer> respawnSeconds = new HashMap<>();
    final Map<UUID, Integer> dyingSeconds = new HashMap<>();
    final Set<UUID> totemUsed = new HashSet<>();
    final Map<UUID, Integer> pickaxeLevel = new HashMap<>();
    final Map<UUID, Integer> kills = new HashMap<>();
    final Map<UUID, Double> originalMaxHealth = new HashMap<>();
    final Set<String> placedBlocks = new HashSet<>();
    final Map<String, ItemStack> placedBlockItems = new HashMap<>();
    final Map<String, Entity> obsidianPoolDisplays = new HashMap<>();
    final Set<String> obsidianChargePoints = new LinkedHashSet<>();
    final Set<String> chargedObsidianPoints = new HashSet<>();
    final List<BukkitTask> portalTasks = new ArrayList<>();
    final List<Location> brickPortalBlocks = new ArrayList<>();
    final List<Location> netherPortalBlocks = new ArrayList<>();
    Axis brickPortalAxis = Axis.X;
    Axis netherPortalAxis = Axis.X;
    BossBar brickBossBar;
    BossBar netherBossBar;
    int brickBossNoticeTicks;
    int netherBossNoticeTicks;
    String brickBossNotice;
    String netherBossNotice;
    String lastFinalKillSoundKey;

    Status status = Status.WAITING;
    World lobbyWorld;
    World brickWorld;
    World netherWorld;
    int waitingLeft;
    int gameLeft;
    int brickCoreHealth;
    int brickCoreMax;
    int obsidianDeposited;
    int obsidianRequired;
    boolean portalOpened;
    UUID corePlayer;
    Location coreStartLocation;
    boolean coreTransferUsed;
    int coreTransferLeft;
    BukkitTask task;

    Room(String id, UUID owner, int waitingLeft, int gameLeft, int brickCoreMax) {
        this.id = id;
        this.runtimeKey = id + "_" + Long.toUnsignedString(System.nanoTime(), 36);
        this.owner = owner;
        this.waitingLeft = waitingLeft;
        this.gameLeft = gameLeft;
        this.brickCoreMax = brickCoreMax;
        this.brickCoreHealth = brickCoreMax;
    }

    int count(Team team) {
        int count = 0;
        for (UUID uuid : players) {
            if (teams.get(uuid) == team) count++;
        }
        return count;
    }

    Team team(UUID uuid) {
        return teams.get(uuid);
    }

    boolean contains(Player player) {
        return players.contains(player.getUniqueId());
    }

    List<Player> onlinePlayers() {
        List<Player> out = new ArrayList<>();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) out.add(player);
        }
        return out;
    }

    List<Player> onlineTeam(Team team) {
        List<Player> out = new ArrayList<>();
        for (Player player : onlinePlayers()) {
            if (teams.get(player.getUniqueId()) == team) out.add(player);
        }
        return out;
    }

    boolean isActive(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid) && !finalDead.contains(uuid);
    }

    boolean canFight(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid) && !spectators.contains(uuid) && !finalDead.contains(uuid);
    }

    int aliveCount(Team team) {
        int count = 0;
        for (Player player : onlineTeam(team)) {
            if (isActive(player)) count++;
        }
        return count;
    }

    void broadcast(String message) {
        for (Player player : onlinePlayers()) {
            player.sendMessage(Text.c(message));
        }
    }

    void sound(Sound sound, float volume, float pitch) {
        for (Player player : onlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    void actionBar(String message) {
        for (Player player : onlinePlayers()) {
            player.sendActionBar(Text.c(message));
        }
    }

    void title(String title, String subtitle) {
        for (Player player : onlinePlayers()) {
            player.showTitle(net.kyori.adventure.title.Title.title(Text.c(title), Text.c(subtitle)));
        }
    }

    ItemStack[] cloneInventory(Player player) {
        return InventorySnapshot.cloneItems(player.getInventory().getContents());
    }
}
