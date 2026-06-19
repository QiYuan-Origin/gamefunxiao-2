package org.gamefunxiao.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.Comparator;
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

        public static EditWorldKind fromString(String value) {
            if (value == null || value.isBlank()) {
                return BRICK;
            }
            String lower = value.toLowerCase(Locale.ROOT).replace('-', '_');
            return switch (lower) {
                case "lobby", "wait", "waiting", "大厅", "等待大厅" -> LOBBY;
                case "nether", "nether_brick", "下界", "下界砖" -> NETHER_BRICK;
                default -> BRICK;
            };
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

    public record MapDefinition(String mapId,
                                String displayName,
                                boolean enabled,
                                boolean allowInGame,
                                int minPlayers,
                                int maxPlayers,
                                int coreHealth,
                                int gameTimeLimitSeconds,
                                String lobbyTemplateWorld,
                                String brickTemplateWorld,
                                String netherBrickTemplateWorld,
                                LocationSpec lobbySpawn,
                                LocationSpec brickSpawn,
                                LocationSpec netherBrickSpawn,
                                LocationSpec brickCore,
                                List<LocationSpec> brickVillagerSpawns,
                                List<LocationSpec> netherPiglinSpawns,
                                List<LocationSpec> brickResourceBlocks,
                                List<LocationSpec> netherResourceBlocks,
                                LocationSpec fakeBorderCenter,
                                double fakeBorderRadius,
                                boolean autoCreateTemplate) {

        public boolean playableEnabled() {
            return enabled && allowInGame;
        }
    }

    public record ValidationResult(boolean complete, List<String> missingElements) {
    }

    public record RuntimeWorlds(World brickWorld, World netherBrickWorld) {
        public boolean complete() {
            return brickWorld != null && netherBrickWorld != null;
        }
    }

    private static final String CONFIG_NAME = "brick-guard-maps";
    private static final double DEFAULT_FAKE_BORDER_RADIUS = 1500.0D;
    private static final int DEFAULT_MAX_PLAYERS = 16;
    private static final int DEFAULT_CORE_HEALTH = 500;
    private static final int DEFAULT_TIME_LIMIT_SECONDS = 3600;

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
        boolean isDefault = "default".equalsIgnoreCase(normalizedMapId);

        if (!config().isConfigurationSection(path)) {
            config().set(path + ".display_name", defaultDisplayName(normalizedMapId));
            config().set(path + ".enabled", true);
            config().set(path + ".allow_in_game", isDefault);
            config().set(path + ".min_players", 2);
            config().set(path + ".max_players", safeMaxPlayers);
            config().set(path + ".core_health", DEFAULT_CORE_HEALTH);
            config().set(path + ".game_time_limit_seconds", DEFAULT_TIME_LIMIT_SECONDS);
            config().set(path + ".pseudo_boundary_radius", DEFAULT_FAKE_BORDER_RADIUS);
            config().set(path + ".lobby_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.LOBBY));
            config().set(path + ".brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.BRICK));
            config().set(path + ".nether_brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.NETHER_BRICK));
            config().set(path + ".lobby_spawn", writeLocationSpec(new LocationSpec("", 0.5D, 65.0D, 0.5D, 0.0F, 0.0F)));
            config().set(path + ".brick_spawn", writeLocationSpec(new LocationSpec("", -8.5D, 80.0D, 0.5D, -90.0F, 0.0F)));
            config().set(path + ".nether_brick_spawn", writeLocationSpec(new LocationSpec("", 8.5D, 80.0D, 0.5D, 90.0F, 0.0F)));
            config().set(path + ".brick_core", writeLocationSpec(new LocationSpec("", -18.0D, 79.0D, 0.0D, 0.0F, 0.0F)));
            config().set(path + ".fake_border.center", writeLocationSpec(new LocationSpec("", 0.0D, 80.0D, 0.0D, 0.0F, 0.0F)));
            config().set(path + ".fake_border.radius", DEFAULT_FAKE_BORDER_RADIUS);
            config().set(path + ".auto_create_template", true);
            config().set(path + ".brick_villager_spawns", isDefault
                    ? writeLocationList(List.of(
                    new LocationSpec("", -7.5D, 80.0D, 5.5D, 0.0F, 0.0F),
                    new LocationSpec("", -4.5D, 80.0D, 7.5D, 0.0F, 0.0F),
                    new LocationSpec("", -1.5D, 80.0D, 5.5D, 0.0F, 0.0F)
            )) : new ArrayList<>());
            config().set(path + ".nether_piglin_spawns", isDefault
                    ? writeLocationList(List.of(
                    new LocationSpec("", -4.5D, 80.0D, 5.5D, 0.0F, 0.0F),
                    new LocationSpec("", 0.5D, 80.0D, 5.5D, 0.0F, 0.0F),
                    new LocationSpec("", 4.5D, 80.0D, 5.5D, 0.0F, 0.0F)
            )) : new ArrayList<>());
            config().set(path + ".brick_resource_blocks", isDefault
                    ? writeLocationList(List.of(
                    new LocationSpec("", -9.0D, 80.0D, -9.0D, 0.0F, 0.0F),
                    new LocationSpec("", -7.0D, 80.0D, -7.0D, 0.0F, 0.0F),
                    new LocationSpec("", -5.0D, 80.0D, -5.0D, 0.0F, 0.0F),
                    new LocationSpec("", 3.0D, 80.0D, -9.0D, 0.0F, 0.0F),
                    new LocationSpec("", 5.0D, 80.0D, -7.0D, 0.0F, 0.0F),
                    new LocationSpec("", 7.0D, 80.0D, -5.0D, 0.0F, 0.0F)
            )) : new ArrayList<>());
            config().set(path + ".nether_resource_blocks", isDefault
                    ? writeLocationList(List.of(
                    new LocationSpec("", -9.0D, 80.0D, -9.0D, 0.0F, 0.0F),
                    new LocationSpec("", -7.0D, 80.0D, -7.0D, 0.0F, 0.0F),
                    new LocationSpec("", -5.0D, 80.0D, -5.0D, 0.0F, 0.0F),
                    new LocationSpec("", 3.0D, 80.0D, -9.0D, 0.0F, 0.0F),
                    new LocationSpec("", 5.0D, 80.0D, -7.0D, 0.0F, 0.0F),
                    new LocationSpec("", 7.0D, 80.0D, -5.0D, 0.0F, 0.0F)
            )) : new ArrayList<>());
            changed = true;
        }

        changed |= ensureValue(path + ".allow_in_game", isDefault);
        changed |= ensureValue(path + ".core_health", DEFAULT_CORE_HEALTH);
        changed |= ensureValue(path + ".game_time_limit_seconds", DEFAULT_TIME_LIMIT_SECONDS);
        changed |= ensureValue(path + ".pseudo_boundary_radius", DEFAULT_FAKE_BORDER_RADIUS);
        changed |= ensureValue(path + ".brick_villager_spawns", new ArrayList<>());
        changed |= ensureValue(path + ".nether_piglin_spawns", new ArrayList<>());
        changed |= ensureValue(path + ".brick_resource_blocks", new ArrayList<>());
        changed |= ensureValue(path + ".nether_resource_blocks", new ArrayList<>());

        if (maxPlayers > 0 && config().getInt(path + ".max_players", 0) != safeMaxPlayers) {
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

    private boolean ensureValue(String path, Object value) {
        if (config().isSet(path)) {
            return false;
        }
        config().set(path, value);
        return true;
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
        int coreHealth = Math.max(100, config().getInt(path + ".core_health", DEFAULT_CORE_HEALTH));
        int gameTime = Math.max(300, config().getInt(path + ".game_time_limit_seconds", DEFAULT_TIME_LIMIT_SECONDS));
        double radius = Math.max(8.0D, config().getDouble(path + ".fake_border.radius",
                config().getDouble(path + ".pseudo_boundary_radius", DEFAULT_FAKE_BORDER_RADIUS)));

        return new MapDefinition(
                normalizedMapId,
                config().getString(path + ".display_name", defaultDisplayName(normalizedMapId)),
                config().getBoolean(path + ".enabled", true),
                config().getBoolean(path + ".allow_in_game", "default".equalsIgnoreCase(normalizedMapId)),
                minPlayers,
                maxPlayers,
                coreHealth,
                gameTime,
                config().getString(path + ".lobby_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.LOBBY)),
                config().getString(path + ".brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.BRICK)),
                config().getString(path + ".nether_brick_template_world", defaultTemplateWorldName(normalizedMapId, EditWorldKind.NETHER_BRICK)),
                readLocation(path + ".lobby_spawn"),
                readLocation(path + ".brick_spawn"),
                readLocation(path + ".nether_brick_spawn"),
                readLocation(path + ".brick_core"),
                readLocationList(path + ".brick_villager_spawns"),
                readLocationList(path + ".nether_piglin_spawns"),
                readLocationList(path + ".brick_resource_blocks"),
                readLocationList(path + ".nether_resource_blocks"),
                readLocation(path + ".fake_border.center"),
                radius,
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
        definitions.sort(Comparator.comparing(MapDefinition::mapId, String.CASE_INSENSITIVE_ORDER));
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
                    .filter(MapDefinition::playableEnabled)
                    .filter(definition -> definition.minPlayers() <= required)
                    .filter(definition -> definition.maxPlayers() >= required)
                    .filter(this::hasAllRequiredElements)
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            }
        }

        MapDefinition activeMap = getMapDefinition(active);
        if (activeMap != null && activeMap.playableEnabled()
                && activeMap.minPlayers() <= required
                && activeMap.maxPlayers() >= required
                && hasAllRequiredElements(activeMap)) {
            return activeMap;
        }

        return getMapDefinitions().stream()
                .filter(MapDefinition::playableEnabled)
                .filter(definition -> definition.minPlayers() <= required)
                .filter(definition -> definition.maxPlayers() >= required)
                .filter(this::hasAllRequiredElements)
                .findFirst()
                .orElse(null);
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

    public MapDefinition setAllowInGame(String mapId, boolean allowInGame) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + ".allow_in_game", allowInGame);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setCoreHealth(String mapId, int health) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + ".core_health", Math.max(100, health));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setTimeLimitSeconds(String mapId, int seconds) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + ".game_time_limit_seconds", Math.max(300, seconds));
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

    public MapDefinition setLocationList(String mapId, String key, List<Location> locations) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + "." + key, writeLocationListFromLocations(locations));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition addLocationEntry(String mapId, String key, Location location) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        List<LocationSpec> specs = readLocationList(mapPath(normalizedMapId) + "." + key);
        specs.add(toSpec(location));
        config().set(mapPath(normalizedMapId) + "." + key, writeLocationList(specs));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition removeNearestLocationEntry(String mapId, String key, Location reference) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        List<LocationSpec> specs = readLocationList(mapPath(normalizedMapId) + "." + key);
        if (reference == null || reference.getWorld() == null || specs.isEmpty()) {
            return getMapDefinition(normalizedMapId);
        }
        int nearestIndex = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < specs.size(); i++) {
            Location target = specs.get(i).toLocation(reference.getWorld());
            if (target == null) {
                continue;
            }
            double distance = target.distanceSquared(reference);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }
        if (nearestIndex >= 0) {
            specs.remove(nearestIndex);
            config().set(mapPath(normalizedMapId) + "." + key, writeLocationList(specs));
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
        }
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition clearLocationList(String mapId, String key) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        config().set(mapPath(normalizedMapId) + "." + key, new ArrayList<>());
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition setFakeBorder(String mapId, Location center, double radius) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        if (center != null) {
            config().set(mapPath(normalizedMapId) + ".fake_border.center", writeLocation(center));
        }
        double safeRadius = Math.max(8.0D, radius);
        config().set(mapPath(normalizedMapId) + ".fake_border.radius", safeRadius);
        config().set(mapPath(normalizedMapId) + ".pseudo_boundary_radius", safeRadius);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public MapDefinition bindTemplateWorld(String mapId, EditWorldKind kind, String worldName) {
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(normalizedMapId, -1);
        String path = switch (kind == null ? EditWorldKind.BRICK : kind) {
            case LOBBY -> ".lobby_template_world";
            case BRICK -> ".brick_template_world";
            case NETHER_BRICK -> ".nether_brick_template_world";
        };
        config().set(mapPath(normalizedMapId) + path, worldName == null ? "" : worldName.trim());
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(normalizedMapId);
    }

    public ValidationResult validateMap(MapDefinition definition) {
        if (definition == null) {
            return new ValidationResult(false, List.of("地图不存在"));
        }
        List<String> missing = new ArrayList<>();
        if (definition.lobbySpawn() == null) {
            missing.add("等待大厅出生点");
        }
        if (definition.brickSpawn() == null) {
            missing.add("板砖出生点");
        }
        if (definition.netherBrickSpawn() == null) {
            missing.add("下界砖出生点");
        }
        if (definition.brickCore() == null) {
            missing.add("板砖核心");
        }
        if (definition.fakeBorderCenter() == null) {
            missing.add("伪边界中心");
        }
        if (definition.brickVillagerSpawns().isEmpty()) {
            missing.add("板砖村民点位");
        }
        if (definition.netherPiglinSpawns().isEmpty()) {
            missing.add("下界猪灵点位");
        }
        if (definition.brickResourceBlocks().isEmpty()) {
            missing.add("板砖矿物点位");
        }
        if (definition.netherResourceBlocks().isEmpty()) {
            missing.add("下界矿物点位");
        }
        return new ValidationResult(missing.isEmpty(), List.copyOf(missing));
    }

    public List<String> getUnavailableReasons(MapDefinition definition, int playerCount, boolean requireAllowInGame) {
        if (definition == null) {
            return List.of("地图不存在");
        }
        List<String> reasons = new ArrayList<>();
        if (!definition.enabled()) {
            reasons.add("地图未启用");
        }
        if (requireAllowInGame && !definition.allowInGame()) {
            reasons.add("地图未允许加入");
        }
        int requiredPlayers = Math.max(1, playerCount);
        if (requiredPlayers < definition.minPlayers()) {
            reasons.add("至少需要 " + definition.minPlayers() + " 人");
        }
        if (requiredPlayers > definition.maxPlayers()) {
            reasons.add("最多支持 " + definition.maxPlayers() + " 人");
        }
        reasons.addAll(validateMap(definition).missingElements());
        return List.copyOf(reasons);
    }

    public boolean isPlayableForPlayerCount(MapDefinition definition, int playerCount, boolean requireAllowInGame) {
        return getUnavailableReasons(definition, playerCount, requireAllowInGame).isEmpty();
    }

    public boolean hasAllRequiredElements(MapDefinition definition) {
        return validateMap(definition).complete();
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

    public List<Location> getBrickVillagerSpawns(MapDefinition definition, World world) {
        return toLocations(definition == null ? List.of() : definition.brickVillagerSpawns(), world);
    }

    public List<Location> getNetherPiglinSpawns(MapDefinition definition, World world) {
        return toLocations(definition == null ? List.of() : definition.netherPiglinSpawns(), world);
    }

    public List<Location> getBrickResourceBlocks(MapDefinition definition, World world) {
        return toLocations(definition == null ? List.of() : definition.brickResourceBlocks(), world);
    }

    public List<Location> getNetherResourceBlocks(MapDefinition definition, World world) {
        return toLocations(definition == null ? List.of() : definition.netherResourceBlocks(), world);
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

    private List<Location> toLocations(List<LocationSpec> specs, World world) {
        List<Location> locations = new ArrayList<>();
        if (world == null || specs == null) {
            return locations;
        }
        for (LocationSpec spec : specs) {
            Location location = spec == null ? null : spec.toLocation(world);
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
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

    private List<LocationSpec> readLocationList(String path) {
        List<LocationSpec> locations = new ArrayList<>();
        List<Map<?, ?>> rawList = config().getMapList(path);
        for (Map<?, ?> raw : rawList) {
            Object rawWorld = raw.containsKey("world") ? raw.get("world") : "";
            String worldName = String.valueOf(rawWorld);
            double x = toDouble(raw.get("x"), 0.5D);
            double y = toDouble(raw.get("y"), 65.0D);
            double z = toDouble(raw.get("z"), 0.5D);
            float yaw = (float) toDouble(raw.get("yaw"), 0.0D);
            float pitch = (float) toDouble(raw.get("pitch"), 0.0D);
            locations.add(new LocationSpec(worldName, x, y, z, yaw, pitch));
        }
        return locations;
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
        return writeLocationSpec(toSpec(location));
    }

    private List<Map<String, Object>> writeLocationListFromLocations(List<Location> locations) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (locations == null) {
            return result;
        }
        for (Location location : locations) {
            if (location != null) {
                result.add(writeLocation(location));
            }
        }
        return result;
    }

    private List<Map<String, Object>> writeLocationList(List<LocationSpec> specs) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (specs == null) {
            return result;
        }
        for (LocationSpec spec : specs) {
            if (spec != null) {
                result.add(writeLocationSpec(spec));
            }
        }
        return result;
    }

    private LocationSpec toSpec(Location location) {
        return new LocationSpec(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
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

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
