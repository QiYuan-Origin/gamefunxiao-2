package org.gamefunxiao.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BrickGuardMapManager {

    public enum EditWorldKind {
        LOBBY("lobby", "等待大厅"),
        BRICK("brick", "板砖世界"),
        NETHER_BRICK("nether_brick", "下界砖世界");

        private final String id;
        private final String displayName;

        EditWorldKind(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record LocationSpec(String worldName, double x, double y, double z, float yaw, float pitch) {
        public Location toLocation(World world) {
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    public record AreaSpec(LocationSpec min, LocationSpec max) {
        public boolean complete() {
            return min != null && max != null;
        }
    }

    public record MapDefinition(String mapId,
                                String displayName,
                                boolean enabled,
                                int minPlayers,
                                int maxPlayers,
                                String lobbyTemplateWorld,
                                String brickTemplateWorld,
                                String netherBrickTemplateWorld,
                                LocationSpec lobbySpawn,
                                LocationSpec brickSpawn,
                                LocationSpec netherBrickSpawn,
                                LocationSpec brickCore,
                                AreaSpec villagerArea,
                                AreaSpec mineArea,
                                LocationSpec fakeBorderCenter,
                                double fakeBorderRadius,
                                boolean autoCreateTemplate) {
    }

    public record RuntimeWorlds(World brickWorld, World netherBrickWorld) {
        public boolean complete() {
            return brickWorld != null && netherBrickWorld != null;
        }
    }

    private static final String CONFIG_NAME = "brick-guard-maps";
    private static final double DEFAULT_FAKE_BORDER_RADIUS = 1500.0D;
    private static final int DEFAULT_MAX_PLAYERS = 16;

    private final GameFunXiao plugin;

    public BrickGuardMapManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfigManager().getConfig(CONFIG_NAME);
    }

    public String normalizeMapId(String raw) {
        String value = raw == null || raw.isBlank() ? "default" : raw.trim().toLowerCase(Locale.ROOT);
        value = value.replace(' ', '_').replace('-', '_');
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_') {
                builder.append(ch);
            }
        }
        return builder.isEmpty() ? "default" : builder.toString();
    }

    public MapDefinition ensureMapDefinition(String mapId, int maxPlayers) {
        String normalizedMapId = normalizeMapId(mapId);
        int safeMaxPlayers = Math.max(2, maxPlayers <= 0 ? DEFAULT_MAX_PLAYERS : maxPlayers);
        String path = mapPath(normalizedMapId);
        boolean changed = false;

        if (!config().isConfigurationSection(path)) {
            config().set(path + ".display_name", defaultDisplayName(normalizedMapId));
            config().set(path + ".enabled", true);
            config().set(path + ".min_players", 2);
            config().set(path + ".max_players", safeMaxPlayers);
            config().set(path + ".lobby_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.LOBBY));
            config().set(path + ".brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.BRICK));
            config().set(path + ".nether_brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.NETHER_BRICK));
            config().set(path + ".lobby_spawn", writeLocationSpec(new LocationSpec("", 0.5D, 65.0D, 0.5D, 0.0F, 0.0F)));
            config().set(path + ".brick_spawn", writeLocationSpec(new LocationSpec("", -8.5D, 80.0D, 0.5D, -90.0F, 0.0F)));
            config().set(path + ".nether_brick_spawn", writeLocationSpec(new LocationSpec("", 8.5D, 80.0D, 0.5D, 90.0F, 0.0F)));
            config().set(path + ".brick_core", writeLocationSpec(new LocationSpec("", -18.0D, 79.0D, 0.0D, 0.0F, 0.0F)));
            writeDefaultArea(path + ".villager_area", -10.0D, 79.0D, -6.0D, -4.0D, 84.0D, 6.0D);
            writeDefaultArea(path + ".mine_area", 4.0D, 70.0D, -10.0D, 18.0D, 85.0D, 10.0D);
            config().set(path + ".fake_border.center", writeLocationSpec(new LocationSpec("", 0.0D, 80.0D, 0.0D, 0.0F, 0.0F)));
            config().set(path + ".fake_border.radius", DEFAULT_FAKE_BORDER_RADIUS);
            config().set(path + ".auto_create_template", true);
            changed = true;
        } else if (maxPlayers > 0 && config().getInt(path + ".max_players", 0) != safeMaxPlayers) {
            config().set(path + ".max_players", safeMaxPlayers);
            changed = true;
        }

        if (config().getString("active_map", "").isBlank()) {
            config().set("active_map", normalizedMapId);
            changed = true;
        }

        if (changed) {
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
        }
        return getMapDefinition(normalizedMapId);
    }

    public boolean hasMapDefinition(String mapId) {
        return config().isConfigurationSection(mapPath(normalizeMapId(mapId)));
    }

    public MapDefinition getMapDefinition(String mapId) {
        String normalizedMapId = normalizeMapId(mapId);
        String path = mapPath(normalizedMapId);
        if (!config().isConfigurationSection(path)) {
            return null;
        }

        int minPlayers = Math.max(1, config().getInt(path + ".min_players", 2));
        int maxPlayers = Math.max(minPlayers, config().getInt(path + ".max_players", DEFAULT_MAX_PLAYERS));
        return new MapDefinition(
                normalizedMapId,
                config().getString(path + ".display_name", defaultDisplayName(normalizedMapId)),
                config().getBoolean(path + ".enabled", true),
                minPlayers,
                maxPlayers,
                config().getString(path + ".lobby_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.LOBBY)),
                config().getString(path + ".brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.BRICK)),
                config().getString(path + ".nether_brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.NETHER_BRICK)),
                readLocation(path + ".lobby_spawn"),
                readLocation(path + ".brick_spawn"),
                readLocation(path + ".nether_brick_spawn"),
                readLocation(path + ".brick_core"),
                readArea(path + ".villager_area"),
                readArea(path + ".mine_area"),
                readLocation(path + ".fake_border.center"),
                Math.max(8.0D, config().getDouble(path + ".fake_border.radius", DEFAULT_FAKE_BORDER_RADIUS)),
                config().getBoolean(path + ".auto_create_template", true)
        );
    }

    public List<MapDefinition> getMapDefinitions() {
        List<MapDefinition> definitions = new ArrayList<>();
        ConfigurationSection section = config().getConfigurationSection("maps");
        if (section == null) {
            return definitions;
        }
        for (String mapId : section.getKeys(false)) {
            MapDefinition definition = getMapDefinition(mapId);
            if (definition != null) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    public String getActiveMapId() {
        String active = config().getString("active_map", "default");
        return active == null || active.isBlank() ? "default" : active;
    }

    public boolean isRandomActiveMapId(String mapId) {
        if (mapId == null) {
            return false;
        }
        String value = mapId.trim().toLowerCase(Locale.ROOT);
        return value.equals("random") || value.equals("随机") || value.equals("all") || value.equals("*");
    }

    public void setActiveMap(String mapId) {
        if (isRandomActiveMapId(mapId)) {
            config().set("active_map", "random");
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
            return;
        }
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set("active_map", normalizedMapId);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
    }

    public MapDefinition findUsableMap(int playerCount) {
        int required = Math.max(1, playerCount);
        String active = getActiveMapId();
        if (isRandomActiveMapId(active)) {
            List<MapDefinition> candidates = getMapDefinitions().stream()
                    .filter(MapDefinition::enabled)
                    .filter(definition -> definition.minPlayers() <= required)
                    .filter(definition -> definition.maxPlayers() >= required)
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            }
        }

        MapDefinition activeMap = getMapDefinition(active);
        if (activeMap != null && activeMap.enabled()
                && activeMap.minPlayers() <= required
                && activeMap.maxPlayers() >= required) {
            return activeMap;
        }

        return getMapDefinitions().stream()
                .filter(MapDefinition::enabled)
                .filter(definition -> definition.minPlayers() <= required)
                .filter(definition -> definition.maxPlayers() >= required)
                .findFirst()
                .orElse(activeMap);
    }

    public MapDefinition setMapDisplayName(String mapId, String displayName) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + ".display_name",
                displayName == null || displayName.isBlank() ? defaultDisplayName(normalizedMapId) : displayName);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setMapEnabled(String mapId, boolean enabled) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + ".enabled", enabled);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public boolean deleteMapDefinition(String mapId) {
        String normalizedMapId = normalizeMapId(mapId);
        String path = mapPath(normalizedMapId);
        if (!config().isConfigurationSection(path)) {
            return false;
        }
        config().set(path, null);
        if (normalizedMapId.equalsIgnoreCase(getActiveMapId())) {
            config().set("active_map", "default");
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return true;
    }

    public MapDefinition setLocation(String mapId, String key, Location location) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + "." + key, writeLocation(location));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setAreaCorner(String mapId, String areaKey, String corner, Location location) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        String normalizedCorner = "pos2".equalsIgnoreCase(corner) || "max".equalsIgnoreCase(corner) || "2".equals(corner)
                ? "max" : "min";
        config().set(mapPath(normalizedMapId) + "." + areaKey + "." + normalizedCorner, writeLocation(location));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setFakeBorder(String mapId, Location center, double radius) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        if (center != null) {
            config().set(mapPath(normalizedMapId) + ".fake_border.center", writeLocation(center));
        }
        config().set(mapPath(normalizedMapId) + ".fake_border.radius", Math.max(8.0D, radius));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public Location getLobbySpawn(MapDefinition definition, World world) {
        if (world == null) {
            return null;
        }
        if (definition != null && definition.lobbySpawn() != null) {
            return definition.lobbySpawn().toLocation(world);
        }
        return world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
    }

    public Location getBrickSpawn(MapDefinition definition, World world) {
        return getLocationOrSpawn(definition == null ? null : definition.brickSpawn(), world);
    }

    public Location getNetherBrickSpawn(MapDefinition definition, World world) {
        return getLocationOrSpawn(definition == null ? null : definition.netherBrickSpawn(), world);
    }

    public Location getBrickCore(MapDefinition definition, World world) {
        return getLocationOrSpawn(definition == null ? null : definition.brickCore(), world);
    }

    public Location getFakeBorderCenter(MapDefinition definition, World world) {
        if (world == null) {
            return null;
        }
        if (definition != null && definition.fakeBorderCenter() != null) {
            return definition.fakeBorderCenter().toLocation(world);
        }
        return new Location(world, 0.0D, world.getSpawnLocation().getY(), 0.0D);
    }

    public Location areaMin(AreaSpec area, World world) {
        return area != null && area.min() != null ? area.min().toLocation(world) : null;
    }

    public Location areaMax(AreaSpec area, World world) {
        return area != null && area.max() != null ? area.max().toLocation(world) : null;
    }

    public boolean isTemplateWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        for (MapDefinition definition : getMapDefinitions()) {
            if (worldName.equalsIgnoreCase(definition.lobbyTemplateWorld())
                    || worldName.equalsIgnoreCase(definition.brickTemplateWorld())
                    || worldName.equalsIgnoreCase(definition.netherBrickTemplateWorld())) {
                return true;
            }
        }
        return false;
    }

    public String templateWorldName(MapDefinition definition, EditWorldKind kind) {
        if (definition == null || kind == null) {
            return "";
        }
        return switch (kind) {
            case LOBBY -> definition.lobbyTemplateWorld();
            case BRICK -> definition.brickTemplateWorld();
            case NETHER_BRICK -> definition.netherBrickTemplateWorld();
        };
    }

    public boolean shouldAutoCreateTemplate(MapDefinition definition) {
        return definition == null || definition.autoCreateTemplate();
    }

    private Location getLocationOrSpawn(LocationSpec spec, World world) {
        if (world == null) {
            return null;
        }
        if (spec != null) {
            return spec.toLocation(world);
        }
        return world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
    }

    private void writeDefaultArea(String path, double x1, double y1, double z1, double x2, double y2, double z2) {
        config().set(path + ".min", writeLocationSpec(new LocationSpec("", x1, y1, z1, 0.0F, 0.0F)));
        config().set(path + ".max", writeLocationSpec(new LocationSpec("", x2, y2, z2, 0.0F, 0.0F)));
    }

    private AreaSpec readArea(String path) {
        return new AreaSpec(readLocation(path + ".min"), readLocation(path + ".max"));
    }

    private LocationSpec readLocation(String path) {
        if (!config().isConfigurationSection(path)) {
            return null;
        }
        return new LocationSpec(
                config().getString(path + ".world", ""),
                config().getDouble(path + ".x", 0.5D),
                config().getDouble(path + ".y", 65.0D),
                config().getDouble(path + ".z", 0.5D),
                (float) config().getDouble(path + ".yaw", 0.0D),
                (float) config().getDouble(path + ".pitch", 0.0D)
        );
    }

    private Map<String, Object> writeLocation(Location location) {
        if (location == null) {
            return null;
        }
        return writeLocationSpec(new LocationSpec(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        ));
    }

    private Map<String, Object> writeLocationSpec(LocationSpec spec) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", spec.worldName());
        map.put("x", spec.x());
        map.put("y", spec.y());
        map.put("z", spec.z());
        map.put("yaw", (double) spec.yaw());
        map.put("pitch", (double) spec.pitch());
        return map;
    }

    private String mapPath(String mapId) {
        return "maps." + normalizeMapId(mapId);
    }

    private String defaultDisplayName(String mapId) {
        return "default".equalsIgnoreCase(mapId) ? "板砖守卫战默认地图" : "板砖守卫战地图-" + mapId;
    }

    private String defaultTemplateWorldName(String mapId, EditWorldKind kind) {
        return "gamefun_template_brick_guard_" + normalizeMapId(mapId) + "_" + kind.id();
    }
}
