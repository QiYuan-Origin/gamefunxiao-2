package org.gamefunxiao.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BrickGuardListener implements Listener {

    private static final long BORDER_WARNING_INTERVAL_MILLIS = 1800L;

    private final GameFunXiao plugin;
    private final Map<UUID, Long> borderWarnings = new HashMap<>();

    public BrickGuardListener(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Location from = event.getFrom();
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (!shouldCheckBorder(player, room, to.getWorld())) {
            return;
        }

        Location center = room.getBrickGuardFakeBorderCenter();
        if (center == null) {
            World world = to.getWorld();
            center = world == null ? null : world.getSpawnLocation();
        }
        if (center == null || isInsideFakeBorder(to, center, room.getBrickGuardFakeBorderRadius())) {
            return;
        }

        Location blocked = from.clone();
        blocked.setYaw(to.getYaw());
        blocked.setPitch(to.getPitch());
        event.setTo(blocked);
        warnBorder(player, room.getBrickGuardFakeBorderRadius());
    }

    private boolean shouldCheckBorder(Player player, GameRoom room, World world) {
        if (player == null || room == null || world == null) {
            return false;
        }
        if (!room.getGameMode().isBrickGuard()
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || room.isSpectator(player.getUniqueId())) {
            return false;
        }

        String worldRoomId = plugin.getWorldManager().getRoomIdByWorld(world);
        if (!room.getRoomId().equals(worldRoomId)) {
            return false;
        }

        World brickWorld = room.getGameWorld();
        World netherBrickWorld = plugin.getWorldManager().getNetherWorld(room.getRoomId());
        return world.equals(brickWorld) || world.equals(netherBrickWorld);
    }

    private boolean isInsideFakeBorder(Location location, Location center, double radius) {
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    private void warnBorder(Player player, double radius) {
        long now = System.currentTimeMillis();
        long last = borderWarnings.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < BORDER_WARNING_INTERVAL_MILLIS) {
            return;
        }
        borderWarnings.put(player.getUniqueId(), now);
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix(
                "game.brick_guard_fake_border_warning",
                Map.of("radius", String.valueOf((int) Math.round(radius)))));
    }
}
