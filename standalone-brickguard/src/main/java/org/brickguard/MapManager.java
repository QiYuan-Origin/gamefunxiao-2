package org.brickguard;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"deprecation", "removal"})
final class MapManager {
    private final BrickGuardPlugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, List<Entity>> previews = new HashMap<>();

    MapManager(BrickGuardPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maps.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    FileConfiguration raw() {
        return config;
    }

    void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("地图数据保存失败: " + exception.getMessage());
        }
    }

    void createEditWorlds() {
        createEditWorld(MapSide.LOBBY);
        createEditWorld(MapSide.BRICK);
        createEditWorld(MapSide.NETHER);
    }

    void createEditWorld(MapSide side) {
        createVoidWorld(switch (side) {
            case LOBBY -> plugin.getConfig().getString("lobby_world", "brickguard_lobby");
            case BRICK -> plugin.getConfig().getString("brick_world", "brickguard_brick");
            case NETHER -> plugin.getConfig().getString("nether_world", "brickguard_nether");
        }, Material.STONE);
    }

    World world(MapSide side) {
        return switch (side) {
            case LOBBY -> Bukkit.getWorld(plugin.getConfig().getString("lobby_world", "brickguard_lobby"));
            case BRICK -> Bukkit.getWorld(plugin.getConfig().getString("brick_world", "brickguard_brick"));
            case NETHER -> Bukkit.getWorld(plugin.getConfig().getString("nether_world", "brickguard_nether"));
        };
    }

    World createVoidWorld(String name, Material platform) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldCreator creator = new WorldCreator(name);
            creator.generator(new ChunkGenerator() {
            });
            world = creator.createWorld();
        }
        if (world != null) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setSpawnLocation(0, 80, 0);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(x, 79, z).setType(platform, false);
                }
            }
        }
        return world;
    }

    MapData read() {
        MapData data = new MapData();
        data.enabled = config.getBoolean("enabled", false);
        data.lobbySpawn = loc("lobby.spawn");
        data.brickSpawn = loc("brick.spawn");
        data.netherSpawn = loc("nether.spawn");
        data.brickCore = loc("brick.core");
        data.brickPortal = loc("brick.portal");
        data.netherPortal = loc("nether.portal");
        data.obsidianPool = loc("nether.obsidian_pool");
        data.brickTraders.addAll(locList("brick.traders"));
        data.netherTraders.addAll(locList("nether.traders"));
        data.brickGuards.addAll(locList("brick.guards"));
        data.netherGuards.addAll(locList("nether.guards"));
        data.brickMines.addAll(locList("brick.mines"));
        data.netherMines.addAll(locList("nether.mines"));
        return data;
    }

    void write(String path, Location location) {
        Point p = Point.of(location);
        config.set(path + ".world", p.world());
        config.set(path + ".x", p.x());
        config.set(path + ".y", p.y());
        config.set(path + ".z", p.z());
        config.set(path + ".yaw", (double) p.yaw());
        config.set(path + ".pitch", (double) p.pitch());
        save();
    }

    void append(String path, Location location) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Point point : locList(path)) {
            list.add(map(point));
        }
        list.add(map(Point.of(location)));
        config.set(path, list);
        save();
    }

    void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        save();
    }

    void clear(String path) {
        config.set(path, new ArrayList<>());
        save();
    }

    Location location(Point point, MapSide fallbackSide) {
        if (point == null) return null;
        World pointWorld = point.world() == null || point.world().isBlank() ? null : Bukkit.getWorld(point.world());
        World target = pointWorld != null ? pointWorld : world(fallbackSide);
        return target == null ? null : point.toLocation(target);
    }

    private Point loc(String path) {
        if (!config.isConfigurationSection(path)) return null;
        return new Point(config.getDouble(path + ".x"), config.getDouble(path + ".y"), config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"), (float) config.getDouble(path + ".pitch"),
                config.getString(path + ".world", ""));
    }

    private List<Point> locList(String path) {
        List<Point> points = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList(path)) {
            points.add(new Point(num(raw.get("x")), num(raw.get("y")), num(raw.get("z")),
                    (float) num(raw.get("yaw")), (float) num(raw.get("pitch")),
                    String.valueOf(raw.containsKey("world") ? raw.get("world") : "")));
        }
        return points;
    }

    private double num(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    private Map<String, Object> map(Point point) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("world", point.world());
        out.put("x", point.x());
        out.put("y", point.y());
        out.put("z", point.z());
        out.put("yaw", (double) point.yaw());
        out.put("pitch", (double) point.pitch());
        return out;
    }

    void refreshPreview(Player player, MapSide side) {
        clearPreview(player);
        World world = player.getWorld();
        MapData data = read();
        List<Entity> entities = new ArrayList<>();
        if (side == MapSide.LOBBY) {
            addBlock(entities, world, data.lobbySpawn, Material.BEACON);
        } else if (side == MapSide.BRICK) {
            addBlock(entities, world, data.brickSpawn, Material.RESPAWN_ANCHOR);
            addBlock(entities, world, data.brickCore, Material.RED_GLAZED_TERRACOTTA);
            addPortal(entities, world, data.brickPortal);
            data.brickTraders.forEach(point -> addBlock(entities, world, point, Material.EMERALD_BLOCK));
            data.brickGuards.forEach(point -> addBlock(entities, world, point, Material.IRON_BLOCK));
        } else {
            addBlock(entities, world, data.netherSpawn, Material.RESPAWN_ANCHOR);
            addPortal(entities, world, data.netherPortal);
            addPool(entities, world, data.obsidianPool);
            data.netherTraders.forEach(point -> addBlock(entities, world, point, Material.GOLD_BLOCK));
            data.netherGuards.forEach(point -> addBlock(entities, world, point, Material.NETHERITE_BLOCK));
        }
        previews.put(player.getUniqueId(), entities);
    }

    void clearPreview(Player player) {
        List<Entity> old = previews.remove(player.getUniqueId());
        if (old != null) {
            old.forEach(Entity::remove);
        }
    }

    void clearAllPreviews() {
        previews.values().forEach(list -> list.forEach(Entity::remove));
        previews.clear();
    }

    private void addPortal(List<Entity> entities, World world, Point point) {
        if (point == null) return;
        int x = (int) Math.floor(point.x());
        int y = (int) Math.floor(point.y());
        int z = (int) Math.floor(point.z());
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                boolean frame = dx == -1 || dx == 2 || dy == 0 || dy == 4;
                addBlock(entities, world, new Point(x + dx, y + dy, z, 0, 0, ""), frame ? Material.OBSIDIAN : Material.NETHER_PORTAL);
            }
        }
    }

    private void addPool(List<Entity> entities, World world, Point point) {
        if (point == null) return;
        int x = (int) Math.floor(point.x());
        int y = (int) Math.floor(point.y());
        int z = (int) Math.floor(point.z());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                addBlock(entities, world, new Point(x + dx, y, z + dz, 0, 0, ""), dx == 0 && dz == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
            }
        }
    }

    private void addBlock(List<Entity> entities, World world, Point point, Material material) {
        if (point == null || world == null) return;
        Location loc = new Location(world, Math.floor(point.x()), Math.floor(point.y()), Math.floor(point.z()));
        entities.add(world.spawn(loc, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setGlowing(true);
            display.setPersistent(false);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(64.0F);
            display.setShadowRadius(0.0F);
        }));
    }
}
