package org.brickguard;

import org.bukkit.Location;
import org.bukkit.World;

record Point(double x, double y, double z, float yaw, float pitch, String world) {
    static Point of(Location location) {
        return new Point(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(),
                location.getWorld() == null ? "" : location.getWorld().getName());
    }

    Location toLocation(World targetWorld) {
        return new Location(targetWorld, x, y, z, yaw, pitch);
    }
}
