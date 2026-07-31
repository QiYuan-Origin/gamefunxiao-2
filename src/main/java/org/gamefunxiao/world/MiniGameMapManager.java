package org.gamefunxiao.world;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MiniGameMapManager {

    public enum EditWorldKind {
        LOBBY("lobby", "等待大厅"),
        GAME("game", "游戏地图");

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
                return GAME;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.equals("lobby") || lower.equals("wait") || lower.equals("waiting") || lower.equals("大厅") || lower.equals("等待大厅")) {
                return LOBBY;
            }
            return GAME;
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

    public record MapDefinition(org.gamefunxiao.game.GameMode mode,
                                String mapId,
                                String displayName,
                                boolean enabled,
                                int minPlayers,
                                int maxPlayers,
                                String lobbyTemplateWorld,
                                String gameTemplateWorld,
                                LocationSpec lobbySpawn,
                                List<LocationSpec> gameSpawns,
                                LocationSpec arenaCenter,
                                LocationSpec spectatorSpawn,
                                String themeId,
                                int eliminationY,
                                double boundaryRadius,
                                int gameTimeSeconds,
                                int randomItemIntervalSeconds,
                                int randomEventIntervalSeconds,
                                boolean autoCreateTemplate) {
        public String modeKey() {
            return mode.getId();
        }
    }

    public record MapSizeProfile(String id, String displayName, int maxPlayers, double playerSpacing, double boundaryRadius) {
    }

    private record EditorSession(org.gamefunxiao.game.GameMode mode,
                                 String mapId,
                                 EditWorldKind kind,
                                 Location previousLocation,
                                 GameMode previousGameMode,
                                 boolean previousAllowFlight,
                                 boolean previousFlying,
                                 ItemStack[] contents,
                                 ItemStack[] armor,
                                 ItemStack offhand) {
    }

    private enum ToolAction {
        SET_LOBBY_SPAWN,
        SET_PRIMARY_GAME_SPAWN,
        ADD_GAME_SPAWN,
        REMOVE_NEAREST_GAME_SPAWN,
        CLEAR_GAME_SPAWNS,
        SET_ARENA,
        MAX_PLAYERS,
        INFO,
        SAVE,
        EXIT,
        SWITCH_LOBBY,
        SWITCH_GAME,
    }

    private static final String CONFIG_NAME = "minigame-maps";
    private static final String TOOL_KEY_VALUE = "gamefun_minigame_map_tool";
    private static final int DEFAULT_LUCKY_PILLARS_MAX_PLAYERS = 16;
    private static final double DEFAULT_BOUNDARY_RADIUS = 66.0D;

    private final GameFunXiao plugin;
    private final NamespacedKey toolKey;
    private final Map<UUID, EditorSession> editorSessions = new HashMap<>();

    public MiniGameMapManager(GameFunXiao plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, TOOL_KEY_VALUE);
    }

    private FileConfiguration config() {
        return plugin.getConfigManager().getConfig(CONFIG_NAME);
    }

    public boolean isEditing(Player player) {
        return player != null && editorSessions.containsKey(player.getUniqueId());
    }

    public boolean isEditorTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
    }

    public boolean isTemplateWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        FileConfiguration cfg = config();
        if (cfg == null) {
            return false;
        }
        for (org.gamefunxiao.game.GameMode mode : org.gamefunxiao.game.GameMode.getMiniGameMapEditableModes()) {
            if (worldName.equalsIgnoreCase(defaultTemplateWorldName(mode, "default", EditWorldKind.LOBBY))) {
                return true;
            }
            String modePath = "maps." + mode.getId();
            if (!cfg.isConfigurationSection(modePath)) {
                continue;
            }
            for (String mapId : cfg.getConfigurationSection(modePath).getKeys(false)) {
                String path = modePath + "." + mapId;
                if (worldName.equalsIgnoreCase(cfg.getString(path + ".lobby_template_world", ""))
                        || worldName.equalsIgnoreCase(cfg.getString(path + ".game_template_world", ""))) {
                    return true;
                }
            }
        }
        return false;
    }

    public String normalizeMapId(String raw) {
        String value = raw == null || raw.isBlank() ? "default" : raw.trim().toLowerCase(Locale.ROOT);
        value = value.replace(' ', '_');
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-') {
                builder.append(ch);
            }
        }
        return builder.isEmpty() ? "default" : builder.toString();
    }

    public MapDefinition ensureMapDefinition(org.gamefunxiao.game.GameMode mode, String mapId, int maxPlayers) {
        if (mode == null) {
            mode = org.gamefunxiao.game.GameMode.LUCKY_PILLARS;
        }
        String normalizedMapId = normalizeMapId(mapId);
        FileConfiguration cfg = config();
        String path = mapPath(mode, normalizedMapId);
        int safeMaxPlayers = Math.max(2, maxPlayers <= 0 ? DEFAULT_LUCKY_PILLARS_MAX_PLAYERS : maxPlayers);

        boolean changed = false;
        if (!cfg.isConfigurationSection(path)) {
            cfg.set(path + ".display_name", defaultDisplayName(mode, normalizedMapId));
            cfg.set(path + ".enabled", true);
            cfg.set(path + ".max_players", safeMaxPlayers);
            cfg.set(path + ".lobby_template_world", defaultTemplateWorldName(mode, normalizedMapId, EditWorldKind.LOBBY));
            cfg.set(path + ".game_template_world", defaultTemplateWorldName(mode, normalizedMapId, EditWorldKind.GAME));
            cfg.set(path + ".theme", defaultThemeId(normalizedMapId));
            cfg.set(path + ".game_time_seconds", 480);
            cfg.set(path + ".random_item_interval_seconds", 5);
            cfg.set(path + ".random_event_interval_seconds", 30);
            cfg.set(path + ".auto_create_template", true);
            changed = true;
        } else if (maxPlayers > 0 && cfg.getInt(path + ".max_players", 0) != safeMaxPlayers) {
            cfg.set(path + ".max_players", safeMaxPlayers);
            changed = true;
        }
        String sharedLobbyTemplate = defaultTemplateWorldName(mode, normalizedMapId, EditWorldKind.LOBBY);
        if (!sharedLobbyTemplate.equalsIgnoreCase(cfg.getString(path + ".lobby_template_world", ""))) {
            cfg.set(path + ".lobby_template_world", sharedLobbyTemplate);
            changed = true;
        }
        if (cfg.isConfigurationSection(path + ".lobby_spawn")
                && !sharedLobbyTemplate.equalsIgnoreCase(cfg.getString(path + ".lobby_spawn.world", ""))) {
            cfg.set(path + ".lobby_spawn.world", sharedLobbyTemplate);
            changed = true;
        }

        String activePath = "active_maps." + mode.getId();
        if (cfg.getString(activePath, "").isBlank()) {
            cfg.set(activePath, normalizedMapId);
            changed = true;
        }

        if (changed) {
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
        }
        return getMapDefinition(mode, normalizedMapId);
    }

    public MapDefinition getMapDefinition(org.gamefunxiao.game.GameMode mode, String mapId) {
        if (mode == null) {
            return null;
        }
        FileConfiguration cfg = config();
        String normalizedMapId = normalizeMapId(mapId);
        String path = mapPath(mode, normalizedMapId);
        if (!cfg.isConfigurationSection(path)) {
            return null;
        }
        String displayName = cfg.getString(path + ".display_name", defaultDisplayName(mode, normalizedMapId));
        boolean enabled = cfg.getBoolean(path + ".enabled", true);
        int minPlayers = Math.max(2, cfg.getInt(path + ".min_players", 2));
        int maxPlayers = Math.max(minPlayers, cfg.getInt(path + ".max_players", DEFAULT_LUCKY_PILLARS_MAX_PLAYERS));
        String lobbyTemplate = defaultTemplateWorldName(mode, normalizedMapId, EditWorldKind.LOBBY);
        String gameTemplate = cfg.getString(path + ".game_template_world", defaultTemplateWorldName(mode, normalizedMapId, EditWorldKind.GAME));
        LocationSpec lobbySpawn = readLocation(path + ".lobby_spawn");
        List<LocationSpec> gameSpawns = readLocationList(path + ".game_spawns");
        LocationSpec arenaCenter = readLocation(path + ".arena_center");
        LocationSpec spectatorSpawn = readLocation(path + ".spectator_spawn");
        String themeId = cfg.getString(path + ".theme", defaultThemeId(normalizedMapId));
        int eliminationY = cfg.getInt(path + ".elimination_y", 0);
        double boundaryRadius = Math.max(8.0D, cfg.getDouble(path + ".boundary_radius", DEFAULT_BOUNDARY_RADIUS));
        int gameTimeSeconds = Math.max(60, cfg.getInt(path + ".game_time_seconds", 480));
        int randomItemIntervalSeconds = Math.max(2, cfg.getInt(path + ".random_item_interval_seconds", 5));
        int randomEventIntervalSeconds = Math.max(8, cfg.getInt(path + ".random_event_interval_seconds", 30));
        boolean autoCreateTemplate = cfg.getBoolean(path + ".auto_create_template", true);
        return new MapDefinition(mode, normalizedMapId, displayName, enabled, minPlayers, maxPlayers, lobbyTemplate, gameTemplate,
                lobbySpawn, gameSpawns, arenaCenter, spectatorSpawn, themeId, eliminationY, boundaryRadius,
                gameTimeSeconds, randomItemIntervalSeconds, randomEventIntervalSeconds, autoCreateTemplate);
    }

    public MapDefinition getActiveMap(org.gamefunxiao.game.GameMode mode) {
        if (mode == null) {
            return null;
        }
        FileConfiguration cfg = config();
        String activeMapId = cfg.getString("active_maps." + mode.getId(), "");
        if (isRandomActiveMap(activeMapId)) {
            return null;
        }
        if (activeMapId.isBlank()) {
            activeMapId = "default";
        }
        MapDefinition definition = getMapDefinition(mode, activeMapId);
        return definition;
    }

    public MapDefinition findPlayableMap(org.gamefunxiao.game.GameMode mode, int playerCount) {
        if (mode == null || (!mode.isLuckyPillars())) {
            return null;
        }
        int required = Math.max(2, playerCount);
        String activeMapId = config().getString("active_maps." + mode.getId(), "");
        if (isRandomActiveMap(activeMapId)) {
            List<MapDefinition> candidates = getMapDefinitions(mode).stream()
                    .filter(MapDefinition::enabled)
                    .filter(definition -> definition.minPlayers() <= required)
                    .filter(definition -> definition.maxPlayers() >= required)
                    .filter(definition -> definition.gameSpawns().isEmpty() || definition.gameSpawns().size() >= required)
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            }
        }
        MapDefinition active = getActiveMap(mode);
        if (active != null && active.enabled()
                && active.minPlayers() <= required
                && active.maxPlayers() >= required
                && (active.gameSpawns().isEmpty() || active.gameSpawns().size() >= required)) {
            return active;
        }
        MapDefinition strict = getMapDefinitions(mode).stream()
                .filter(MapDefinition::enabled)
                .filter(definition -> definition.minPlayers() <= required)
                .filter(definition -> definition.maxPlayers() >= required)
                .filter(definition -> definition.gameSpawns().isEmpty() || definition.gameSpawns().size() >= required)
                .min(Comparator.comparingInt(MapDefinition::maxPlayers))
                .orElse(null);
        if (strict != null) {
            return strict;
        }
        return getMapDefinitions(mode).stream()
                .filter(MapDefinition::enabled)
                .filter(definition -> definition.minPlayers() <= required)
                .filter(definition -> definition.maxPlayers() >= required)
                .min(Comparator.comparingInt(MapDefinition::maxPlayers))
                .orElse(active);
    }

    public MapDefinition findUsableMap(org.gamefunxiao.game.GameMode mode, int playerCount) {
        if (mode == null) {
            return null;
        }
        if (mode.isLuckyPillars()) {
            return findPlayableMap(mode, playerCount);
        }

        int required = Math.max(2, playerCount);
        MapDefinition active = getActiveMap(mode);
        if (active != null && active.enabled()
                && active.minPlayers() <= required
                && active.maxPlayers() >= required) {
            return active;
        }

        return getMapDefinitions(mode).stream()
                .filter(MapDefinition::enabled)
                .filter(definition -> definition.minPlayers() <= required)
                .filter(definition -> definition.maxPlayers() >= required)
                .min(Comparator.comparingInt(MapDefinition::maxPlayers))
                .orElse(active != null && active.enabled() ? active : null);
    }

    public int getConfiguredMaxPlayers(org.gamefunxiao.game.GameMode mode) {
        MapDefinition active = getActiveMap(mode);
        return active == null ? -1 : active.maxPlayers();
    }

    public MapSizeProfile resolveSizeProfile(org.gamefunxiao.game.GameMode mode, int requestedMaxPlayers, int playerCount) {
        int target = requestedMaxPlayers > 0 ? requestedMaxPlayers : Math.max(2, playerCount);
        List<MapSizeProfile> profiles = getSizeProfiles(mode);
        return profiles.stream()
                .filter(profile -> profile.maxPlayers() >= target)
                .min(Comparator.comparingInt(MapSizeProfile::maxPlayers))
                .orElse(profiles.isEmpty()
                        ? new MapSizeProfile("middle", "中型地图", 16, 18.0D, 66.0D)
                        : profiles.get(profiles.size() - 1));
    }

    public List<MapSizeProfile> getSizeProfiles(org.gamefunxiao.game.GameMode mode) {
        String modeId = mode == null ? org.gamefunxiao.game.GameMode.LUCKY_PILLARS.getId() : mode.getId();
        List<MapSizeProfile> profiles = new ArrayList<>();
        String path = "size_profiles." + modeId;
        if (config().isConfigurationSection(path)) {
            for (String id : config().getConfigurationSection(path).getKeys(false)) {
                String profilePath = path + "." + id;
                profiles.add(new MapSizeProfile(
                        normalizeMapId(id),
                        config().getString(profilePath + ".display_name", id),
                        Math.max(2, config().getInt(profilePath + ".max_players", 16)),
                        Math.max(8.0D, config().getDouble(profilePath + ".player_spacing", 18.0D)),
                        Math.max(40.0D, config().getDouble(profilePath + ".boundary_radius", DEFAULT_BOUNDARY_RADIUS))
                ));
            }
        }
        profiles.sort(Comparator.comparingInt(MapSizeProfile::maxPlayers));
        return profiles;
    }

    public List<MapDefinition> getMapDefinitions(org.gamefunxiao.game.GameMode mode) {
        List<MapDefinition> definitions = new ArrayList<>();
        if (mode == null) {
            return definitions;
        }
        FileConfiguration cfg = config();
        String modePath = "maps." + mode.getId();
        if (!cfg.isConfigurationSection(modePath)) {
            return definitions;
        }
        for (String mapId : cfg.getConfigurationSection(modePath).getKeys(false)) {
            MapDefinition definition = getMapDefinition(mode, mapId);
            if (definition != null) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    public void setActiveMap(org.gamefunxiao.game.GameMode mode, String mapId) {
        if (mode == null) {
            return;
        }
        if (isRandomActiveMap(mapId)) {
            config().set("active_maps." + mode.getId(), "random");
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
            return;
        }
        String normalizedMapId = normalizeMapId(mapId);
        ensureMapDefinition(mode, normalizedMapId, -1);
        config().set("active_maps." + mode.getId(), normalizedMapId);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
    }

    public String getActiveMapId(org.gamefunxiao.game.GameMode mode) {
        if (mode == null) {
            return "";
        }
        return config().getString("active_maps." + mode.getId(), "");
    }

    public boolean isRandomActiveMapId(String mapId) {
        return isRandomActiveMap(mapId);
    }

    public boolean hasMapDefinition(org.gamefunxiao.game.GameMode mode, String mapId) {
        if (mode == null) {
            return false;
        }
        return config().isConfigurationSection(mapPath(mode, normalizeMapId(mapId)));
    }

    public boolean deleteMapDefinition(org.gamefunxiao.game.GameMode mode, String mapId) {
        if (mode == null || isRandomActiveMap(mapId)) {
            return false;
        }
        String normalizedMapId = normalizeMapId(mapId);
        String path = mapPath(mode, normalizedMapId);
        if (!config().isConfigurationSection(path)) {
            return false;
        }
        config().set(path, null);
        String activePath = "active_maps." + mode.getId();
        String active = config().getString(activePath, "");
        if (active.equalsIgnoreCase(normalizedMapId)) {
            config().set(activePath, findFirstRemainingMapId(mode));
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return true;
    }

    public MapDefinition setMapEnabled(org.gamefunxiao.game.GameMode mode, String mapId, boolean enabled) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        config().set(mapPath(definition) + ".enabled", enabled);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapDisplayName(org.gamefunxiao.game.GameMode mode, String mapId, String displayName) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        String safeName = displayName == null || displayName.isBlank()
                ? defaultDisplayName(mode, definition.mapId())
                : displayName.trim();
        config().set(mapPath(definition) + ".display_name", safeName);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapPlayerRange(org.gamefunxiao.game.GameMode mode, String mapId, int minPlayers, int maxPlayers) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, maxPlayers);
        int safeMin = Math.max(2, minPlayers);
        int safeMax = Math.max(safeMin, maxPlayers);
        config().set(mapPath(definition) + ".min_players", safeMin);
        config().set(mapPath(definition) + ".max_players", safeMax);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapTiming(org.gamefunxiao.game.GameMode mode, String mapId,
                                      int gameTimeSeconds, int itemIntervalSeconds, int eventIntervalSeconds) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        String path = mapPath(definition);
        config().set(path + ".game_time_seconds", Math.max(60, gameTimeSeconds));
        config().set(path + ".random_item_interval_seconds", Math.max(2, itemIntervalSeconds));
        config().set(path + ".random_event_interval_seconds", Math.max(8, eventIntervalSeconds));
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapBoundary(org.gamefunxiao.game.GameMode mode, String mapId,
                                        double boundaryRadius, int eliminationY) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        String path = mapPath(definition);
        config().set(path + ".boundary_radius", Math.max(8.0D, boundaryRadius));
        if (eliminationY != Integer.MIN_VALUE) {
            config().set(path + ".elimination_y", eliminationY);
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapTheme(org.gamefunxiao.game.GameMode mode, String mapId, String themeId) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        String safeTheme = themeId == null || themeId.isBlank()
                ? defaultThemeId(definition.mapId())
                : themeId.trim().toUpperCase(Locale.ROOT);
        config().set(mapPath(definition) + ".theme", safeTheme);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    public MapDefinition setMapAutoCreateTemplate(org.gamefunxiao.game.GameMode mode, String mapId, boolean autoCreateTemplate) {
        MapDefinition definition = ensureMapDefinition(mode, mapId, -1);
        config().set(mapPath(definition) + ".auto_create_template", autoCreateTemplate);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return getMapDefinition(definition.mode(), definition.mapId());
    }

    private String findFirstRemainingMapId(org.gamefunxiao.game.GameMode mode) {
        String modePath = "maps." + mode.getId();
        if (!config().isConfigurationSection(modePath)) {
            return "random";
        }
        return config().getConfigurationSection(modePath).getKeys(false).stream()
                .findFirst()
                .orElse("random");
    }

    public void requestEdit(Player player, org.gamefunxiao.game.GameMode mode, String mapId, EditWorldKind kind, int maxPlayers) {
        if (player == null || !player.isOnline()) {
            return;
        }
        ensureMapDefinition(mode, mapId, maxPlayers);
        plugin.getChildServerManager().requestMiniGameMapEdit(player, mode, normalizeMapId(mapId), kind, maxPlayers);
    }

    public boolean enterEditorSession(Player player, org.gamefunxiao.game.GameMode mode, String mapId, EditWorldKind kind, int maxPlayers) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        MapDefinition definition = ensureMapDefinition(mode, mapId, maxPlayers);
        config().set("active_maps." + definition.mode().getId(), definition.mapId());
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        return openEditorWorld(player, definition, kind == null ? EditWorldKind.GAME : kind, false);
    }

    private boolean openEditorWorld(Player player, MapDefinition definition, EditWorldKind kind, boolean keepPrevious) {
        if (player == null || definition == null) {
            return false;
        }
        World world = plugin.getWorldManager().getOrCreateMiniGameTemplateWorld(definition, kind);
        if (world == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("mode", definition.mode().getDisplayName());
            placeholders.put("map", definition.mapId());
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_failed", placeholders));
            return false;
        }

        EditorSession previousSession = editorSessions.get(player.getUniqueId());
        EditorSession session = keepPrevious && previousSession != null
                ? new EditorSession(definition.mode(), definition.mapId(), kind,
                previousSession.previousLocation(), previousSession.previousGameMode(),
                previousSession.previousAllowFlight(), previousSession.previousFlying(),
                previousSession.contents(), previousSession.armor(), previousSession.offhand())
                : snapshotSession(player, definition, kind);
        editorSessions.put(player.getUniqueId(), session);

        Location target;
        if (kind == EditWorldKind.LOBBY) {
            target = getLobbySpawn(definition, world);
        } else {
            target = getFirstGameSpawn(definition, world);
        }
        if (target == null) {
            target = world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        }

        player.teleport(target);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        giveEditorTools(player, definition, kind);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", definition.mode().getDisplayName());
        placeholders.put("map", definition.displayName());
        placeholders.put("id", definition.mapId());
        placeholders.put("kind", kind.displayName());
        placeholders.put("world", world.getName());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_joined", placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.82f, 1.42f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.54f, 1.65f);
            }
        }, 5L);
        return true;
    }

    private EditorSession snapshotSession(Player player, MapDefinition definition, EditWorldKind kind) {
        PlayerInventory inventory = player.getInventory();
        return new EditorSession(definition.mode(), definition.mapId(), kind,
                player.getLocation().clone(), player.getGameMode(), player.getAllowFlight(), player.isFlying(),
                cloneItems(inventory.getContents()), cloneItems(inventory.getArmorContents()),
                inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone());
    }

    public void exitEditorSession(Player player, boolean teleportBack) {
        if (player == null) {
            return;
        }
        EditorSession session = editorSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(cloneItems(session.contents()));
        inventory.setArmorContents(cloneItems(session.armor()));
        inventory.setItemInOffHand(session.offhand() == null ? null : session.offhand().clone());
        player.setGameMode(session.previousGameMode());
        player.setAllowFlight(session.previousAllowFlight());
        player.setFlying(session.previousFlying());
        if (teleportBack && session.previousLocation() != null && session.previousLocation().getWorld() != null) {
            player.teleport(session.previousLocation());
        }
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.exit_success"));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.75f, 1.22f);
    }

    public boolean handleEditorInteract(PlayerInteractEvent event) {
        if (event == null || !(event.getPlayer() instanceof Player player)) {
            return false;
        }
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        ItemStack item = event.getItem();
        ToolAction action = readToolAction(item);
        if (action == null) {
            return false;
        }
        event.setCancelled(true);

        MapDefinition definition = ensureMapDefinition(session.mode(), session.mapId(), -1);
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        switch (action) {
            case SET_LOBBY_SPAWN -> setLobbySpawn(player, definition);
            case SET_PRIMARY_GAME_SPAWN -> setPrimaryGameSpawn(player, definition);
            case ADD_GAME_SPAWN -> addGameSpawn(player, definition);
            case REMOVE_NEAREST_GAME_SPAWN -> removeNearestGameSpawn(player, definition);
            case CLEAR_GAME_SPAWNS -> clearGameSpawns(player, definition);
            case SET_ARENA -> setArena(player, definition);
            case MAX_PLAYERS -> adjustMaxPlayers(player, definition, rightClick);
            case INFO -> sendMapInfo(player, definition);
            case SAVE -> saveEditorWorld(player, definition);
            case EXIT -> {
                saveEditorWorld(player, definition);
                exitEditorSession(player, true);
            }
            case SWITCH_LOBBY -> openEditorWorld(player, definition, EditWorldKind.LOBBY, true);
            case SWITCH_GAME -> openEditorWorld(player, definition, EditWorldKind.GAME, true);
        }
        return true;
    }

    public boolean shouldProtectEditorInventory(Player player, ItemStack currentItem, ItemStack cursorItem) {
        return isEditing(player) && (isEditorTool(currentItem) || isEditorTool(cursorItem));
    }


    private void setLobbySpawn(Player player, MapDefinition definition) {
        Location loc = player.getLocation();
        writeSharedLobbySpawn(definition.mode(), loc);
        World world = loc.getWorld();
        if (world != null) {
            world.setSpawnLocation(loc);
            world.save();
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("world", world == null ? "unknown" : world.getName());
        placeholders.put("x", format(loc.getX()));
        placeholders.put("y", format(loc.getY()));
        placeholders.put("z", format(loc.getZ()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.lobby_spawn_set", placeholders));
        playEditConfirmSound(player);
    }

    public void writeSharedLobbySpawn(org.gamefunxiao.game.GameMode mode, Location loc) {
        if (mode == null || loc == null) {
            return;
        }
        String sharedWorldName = defaultTemplateWorldName(mode, "default", EditWorldKind.LOBBY);
        for (MapDefinition definition : getMapDefinitions(mode)) {
            String path = mapPath(definition);
            config().set(path + ".lobby_template_world", sharedWorldName);
            config().set(path + ".lobby_spawn", writeLocation(loc));
            config().set(path + ".lobby_spawn.world", sharedWorldName);
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
    }

    private void setPrimaryGameSpawn(Player player, MapDefinition definition) {
        List<LocationSpec> spawns = new ArrayList<>(definition.gameSpawns());
        LocationSpec spec = toSpec(player.getLocation());
        if (spawns.isEmpty()) {
            spawns.add(spec);
        } else {
            spawns.set(0, spec);
        }
        writeGameSpawns(definition, spawns);
        sendSpawnChanged(player, definition, "minigame_map.game_primary_spawn_set", spawns.size());
    }

    private void addGameSpawn(Player player, MapDefinition definition) {
        List<LocationSpec> spawns = new ArrayList<>(definition.gameSpawns());
        spawns.add(toSpec(player.getLocation()));
        writeGameSpawns(definition, spawns);
        sendSpawnChanged(player, definition, "minigame_map.game_spawn_added", spawns.size());
    }

    private void removeNearestGameSpawn(Player player, MapDefinition definition) {
        List<LocationSpec> spawns = new ArrayList<>(definition.gameSpawns());
        if (spawns.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.no_spawn_remove", basePlaceholders(definition)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        Location playerLocation = player.getLocation();
        int nearestIndex = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < spawns.size(); i++) {
            Location spawn = spawns.get(i).toLocation(playerLocation.getWorld());
            if (spawn == null) {
                continue;
            }
            double distance = spawn.distanceSquared(playerLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }
        if (nearestIndex < 0) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.no_spawn_remove", basePlaceholders(definition)));
            return;
        }
        spawns.remove(nearestIndex);
        writeGameSpawns(definition, spawns);
        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("count", String.valueOf(spawns.size()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.game_spawn_removed", placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.45f, 1.85f);
    }

    private void clearGameSpawns(Player player, MapDefinition definition) {
        writeGameSpawns(definition, new ArrayList<>());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.game_spawns_cleared", basePlaceholders(definition)));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.55f, 1.5f);
    }

    private void setArena(Player player, MapDefinition definition) {
        Location loc = player.getLocation();
        String path = mapPath(definition);
        config().set(path + ".arena_center", writeLocation(loc));
        int eliminationY = Math.max(loc.getWorld() == null ? -64 : loc.getWorld().getMinHeight(), loc.getBlockY() - 70);
        config().set(path + ".elimination_y", eliminationY);
        if (config().getDouble(path + ".boundary_radius", 0.0D) < 8.0D) {
            config().set(path + ".boundary_radius", DEFAULT_BOUNDARY_RADIUS);
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);

        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("x", format(loc.getX()));
        placeholders.put("y", format(loc.getY()));
        placeholders.put("z", format(loc.getZ()));
        placeholders.put("elimination_y", String.valueOf(eliminationY));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.arena_set", placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.65f, 1.22f);
    }

    private void adjustMaxPlayers(Player player, MapDefinition definition, boolean rightClick) {
        int current = Math.max(2, config().getInt(mapPath(definition) + ".max_players", definition.maxPlayers()));
        int next;
        if (player.isSneaking()) {
            next = Math.max(2, definition.gameSpawns().size());
        } else if (rightClick) {
            next = Math.max(2, current - 1);
        } else {
            next = Math.min(128, current + 1);
        }
        config().set(mapPath(definition) + ".max_players", next);
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("max", String.valueOf(next));
        placeholders.put("spawns", String.valueOf(definition.gameSpawns().size()));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.max_players_changed", placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.65f, 1.25f + Math.min(1.0f, next / 48.0f));
        giveEditorTools(player, getMapDefinition(definition.mode(), definition.mapId()), editorSessions.get(player.getUniqueId()).kind());
    }

    private void saveEditorWorld(Player player, MapDefinition definition) {
        World world = player.getWorld();
        if (world != null) {
            world.save();
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("world", world == null ? "unknown" : world.getName());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.save_success", placeholders));
        playEditConfirmSound(player);
    }

    public void sendMapInfo(Player player, MapDefinition definition) {
        if (player == null || definition == null) {
            return;
        }
        MapDefinition latest = getMapDefinition(definition.mode(), definition.mapId());
        if (latest == null) {
            latest = definition;
        }
        String active = latest.mapId().equalsIgnoreCase(config().getString("active_maps." + latest.mode().getId(), "")) ? "§a是" : "§7否";
        player.sendMessage("§x§7§D§F§F§C§8✦ §x§A§9§F§F§E§4小游戏地图 §8» §f" + latest.displayName() + " §8(" + latest.mapId() + "§8)");
        player.sendMessage("§8· §b模式: §f" + latest.mode().getDisplayName() + " §8/ §b启用: " + (latest.enabled() ? "§a是" : "§c否") + " §8/ §b当前地图: " + active);
        player.sendMessage("§8· §e最大人数: §f" + latest.maxPlayers() + " §8/ §e出生点: §f" + latest.gameSpawns().size());
        player.sendMessage("§8· §d大厅模板: §f" + latest.lobbyTemplateWorld());
        player.sendMessage("§8· §d游戏模板: §f" + latest.gameTemplateWorld());
        player.sendMessage("§8· §6边界半径: §f" + format(latest.boundaryRadius()) + " §8/ §6淘汰高度: §f" + latest.eliminationY());
        player.sendMessage("§8· §e主题: §f" + latest.themeId() + " §8/ §e游戏时间: §f" + latest.gameTimeSeconds() + "秒 §8/ §e物品/事件: §f" + latest.randomItemIntervalSeconds() + "秒/" + latest.randomEventIntervalSeconds() + "秒");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.55f, 1.48f);
    }

    public Map<UUID, Location> assignGameSpawns(MapDefinition definition, World world, List<UUID> participants) {
        Map<UUID, Location> result = new LinkedHashMap<>();
        if (definition == null || world == null || participants == null || participants.isEmpty()) {
            return result;
        }
        List<LocationSpec> specs = definition.gameSpawns();
        if (specs.size() < participants.size()) {
            return result;
        }
        Location center = resolveArenaCenter(definition, world, specs);
        for (int i = 0; i < participants.size(); i++) {
            Location spawn = specs.get(i).toLocation(world);
            if (spawn == null) {
                continue;
            }
            if (center != null) {
                spawn.setYaw((float) Math.toDegrees(Math.atan2(center.getZ() - spawn.getZ(), center.getX() - spawn.getX())) - 90.0F);
            }
            result.put(participants.get(i), spawn);
        }
        return result;
    }

    public void applyLuckyPillarsArena(GameRoom room, MapDefinition definition, World world, Collection<Location> spawns) {
        if (room == null || definition == null || world == null) {
            return;
        }
        Location center = resolveArenaCenter(definition, world, definition.gameSpawns());
        if (center == null) {
            center = world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        }
        double radius = definition.boundaryRadius();
        if (radius < 8.0D && spawns != null && !spawns.isEmpty()) {
            for (Location spawn : spawns) {
                if (spawn != null && spawn.getWorld() != null && spawn.getWorld().equals(world)) {
                    radius = Math.max(radius, spawn.distance(center) + 24.0D);
                }
            }
        }
        radius = Math.max(40.0D, radius);
        int eliminationY = definition.eliminationY() == 0
                ? Math.max(world.getMinHeight(), center.getBlockY() - 70)
                : definition.eliminationY();
        room.setLuckyPillarsArena(center, eliminationY, radius);
        room.clearLuckyPillarBlocks();
        world.setSpawnLocation(center);
        world.getWorldBorder().setCenter(center);
        world.getWorldBorder().setSize(Math.max(80.0D, radius * 2.0D));
        world.getWorldBorder().setWarningDistance(6);
        world.getWorldBorder().setDamageBuffer(2.0D);
        world.getWorldBorder().setDamageAmount(1.0D);
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

    public Location getFirstGameSpawn(MapDefinition definition, World world) {
        if (world == null) {
            return null;
        }
        if (definition != null && !definition.gameSpawns().isEmpty()) {
            return definition.gameSpawns().get(0).toLocation(world);
        }
        return world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
    }

    public Location getSpectatorSpawn(MapDefinition definition, World world) {
        if (world == null) {
            return null;
        }
        if (definition != null && definition.spectatorSpawn() != null) {
            return definition.spectatorSpawn().toLocation(world);
        }
        Location center = resolveArenaCenter(definition, world, definition == null ? List.of() : definition.gameSpawns());
        if (center == null) {
            center = world.getSpawnLocation().clone();
        }
        Location spectator = center.clone().add(0.0D, 18.0D, 0.0D);
        spectator.setPitch(35.0F);
        return spectator;
    }

    public void writeDefaultLobbyTemplateData(MapDefinition definition, Location spawn) {
        if (definition == null || spawn == null) {
            return;
        }
        String path = mapPath(definition);
        if (!config().isConfigurationSection(path + ".lobby_spawn")) {
            writeSharedLobbySpawn(definition.mode(), spawn);
        }
    }

    public void writeDefaultGameTemplateData(MapDefinition definition, Location arenaCenter, int eliminationY,
                                             double boundaryRadius, List<Location> spawns) {
        if (definition == null || arenaCenter == null || spawns == null || spawns.isEmpty()) {
            return;
        }
        String path = mapPath(definition);
        boolean changed = false;
        if (readLocation(path + ".arena_center") == null) {
            config().set(path + ".arena_center", writeLocation(arenaCenter));
            changed = true;
        }
        if (config().getInt(path + ".elimination_y", 0) == 0) {
            config().set(path + ".elimination_y", eliminationY);
            changed = true;
        }
        if (config().getDouble(path + ".boundary_radius", 0.0D) < 8.0D) {
            config().set(path + ".boundary_radius", boundaryRadius);
            changed = true;
        }
        if (!config().isConfigurationSection(path + ".spectator_spawn")) {
            Location spectator = arenaCenter.clone().add(0.0D, 18.0D, 0.0D);
            spectator.setPitch(35.0F);
            config().set(path + ".spectator_spawn", writeLocation(spectator));
            changed = true;
        }
        if (config().getString(path + ".theme", "").isBlank()) {
            config().set(path + ".theme", defaultThemeId(definition.mapId()));
            changed = true;
        }
        if (config().getInt(path + ".game_time_seconds", 0) <= 0) {
            config().set(path + ".game_time_seconds", 480);
            changed = true;
        }
        if (config().getInt(path + ".random_item_interval_seconds", 0) <= 0) {
            config().set(path + ".random_item_interval_seconds", 5);
            changed = true;
        }
        if (config().getInt(path + ".random_event_interval_seconds", 0) <= 0) {
            config().set(path + ".random_event_interval_seconds", 30);
            changed = true;
        }
        if (definition.gameSpawns().isEmpty()) {
            List<LocationSpec> specs = spawns.stream().map(this::toSpec).toList();
            config().set(path + ".game_spawns", specs.stream().map(this::writeLocationSpec).toList());
            changed = true;
        }
        if (changed) {
            plugin.getConfigManager().saveConfig(CONFIG_NAME);
        }
    }

    private Location resolveArenaCenter(MapDefinition definition, World world, List<LocationSpec> specs) {
        if (definition == null || world == null) {
            return null;
        }
        if (definition.arenaCenter() != null) {
            return definition.arenaCenter().toLocation(world);
        }
        if (specs == null || specs.isEmpty()) {
            return world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        }
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;
        for (LocationSpec spec : specs) {
            x += spec.x();
            y += spec.y();
            z += spec.z();
            count++;
        }
        return new Location(world, x / count, y / count, z / count);
    }

    private void giveEditorTools(Player player, MapDefinition definition, EditWorldKind kind) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);

        if (kind == EditWorldKind.LOBBY) {
            inventory.setItem(0, createTool(Material.RECOVERY_COMPASS, "§x§7§D§F§F§C§8✦ §b设置等待大厅出生点",
                    List.of("§f- 右键即可把当前位置保存为大厅出生点", "§8- 房间等待时会传送到这里"), ToolAction.SET_LOBBY_SPAWN));
            inventory.setItem(1, createTool(Material.END_CRYSTAL, "§x§F§F§D§D§5§5➜ §e切到游戏地图编辑",
                    List.of("§f- 右键进入同一地图的游戏世界", "§8- 用来设置玩家出生点和边界"), ToolAction.SWITCH_GAME));
        } else {
            inventory.setItem(0, createTool(Material.EMERALD_BLOCK, "§x§5§5§F§F§A§A✔ §a设置主出生点",
                    List.of("§f- 左键/右键把当前位置设为第一个游戏出生点", "§8- 没有出生点时会自动新增"), ToolAction.SET_PRIMARY_GAME_SPAWN));
            inventory.setItem(1, createTool(Material.SPECTRAL_ARROW, "§x§8§8§D§D§F§F✚ §b添加玩家出生点",
                    List.of("§f- 右键把当前位置追加到出生点列表", "§8- 每个参赛玩家会按顺序分配"), ToolAction.ADD_GAME_SPAWN));
            inventory.setItem(2, createTool(Material.REDSTONE_TORCH, "§x§F§F§B§B§6§6⌫ §e移除最近出生点",
                    List.of("§f- 右键删除离你最近的一个出生点", "§8- 用错了也可以重新添加"), ToolAction.REMOVE_NEAREST_GAME_SPAWN));
            inventory.setItem(3, createTool(Material.TNT, "§x§F§F§6§6§6§6⊗ §c清空全部出生点",
                    List.of("§f- 右键清空这张地图的所有玩家出生点", "§8- 清空后记得重新添加"), ToolAction.CLEAR_GAME_SPAWNS));
            inventory.setItem(4, createTool(Material.BEACON, "§x§D§D§A§A§F§F◎ §d设置竞技场中心",
                    List.of("§f- 右键保存当前位置为地图中心", "§f- 同时自动设置掉落淘汰高度", "§8- 边界会围绕这里收缩"), ToolAction.SET_ARENA));
            inventory.setItem(5, createTool(Material.PLAYER_HEAD, "§x§F§F§D§D§5§5☘ §e调整最大人数 §7(" + definition.maxPlayers() + ")",
                    List.of("§f- 左键最大人数 +1", "§f- 右键最大人数 -1", "§f- 蹲下点击设为出生点数量", "§8- 当前出生点: §b" + definition.gameSpawns().size()), ToolAction.MAX_PLAYERS));
            inventory.setItem(6, createTool(Material.MAP, "§x§7§D§F§F§C§8? §b查看地图信息",
                    List.of("§f- 查看模板世界、人数、出生点数量", "§8- 不会修改地图"), ToolAction.INFO));
            inventory.setItem(7, createTool(Material.LODESTONE, "§x§5§5§F§F§A§A⟳ §a保存地图",
                    List.of("§f- 保存当前世界和地图配置", "§8- 继续留在编辑世界"), ToolAction.SAVE));
            inventory.setItem(8, createTool(Material.BARRIER, "§x§F§F§8§8§8§8↩ §c保存并退出",
                    List.of("§f- 保存后恢复背包并回到进入前位置", "§8- 如果跨服进入，仍会留在当前子服"), ToolAction.EXIT));
            player.updateInventory();
            return;
        }

        inventory.setItem(5, createTool(Material.PLAYER_HEAD, "§x§F§F§D§D§5§5☘ §e调整最大人数 §7(" + definition.maxPlayers() + ")",
                List.of("§f- 左键最大人数 +1", "§f- 右键最大人数 -1", "§f- 蹲下点击设为出生点数量", "§8- 当前出生点: §b" + definition.gameSpawns().size()), ToolAction.MAX_PLAYERS));
        inventory.setItem(6, createTool(Material.MAP, "§x§7§D§F§F§C§8? §b查看地图信息",
                List.of("§f- 查看模板世界、人数、出生点数量", "§8- 不会修改地图"), ToolAction.INFO));
        inventory.setItem(7, createTool(Material.LODESTONE, "§x§5§5§F§F§A§A⟳ §a保存地图",
                List.of("§f- 保存当前世界和地图配置", "§8- 继续留在编辑世界"), ToolAction.SAVE));
        inventory.setItem(8, createTool(Material.BARRIER, "§x§F§F§8§8§8§8↩ §c保存并退出",
                List.of("§f- 保存后恢复背包并回到进入前位置", "§8- 如果跨服进入，仍会留在当前子服"), ToolAction.EXIT));
        player.updateInventory();
    }

    private ItemStack createTool(Material material, String name, List<String> lore, ToolAction action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        meta.lore(lore.stream().map(line -> LegacyComponentSerializer.legacySection().deserialize(line)).toList());
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, action.name());
        item.setItemMeta(meta);
        return item;
    }

    private ToolAction readToolAction(ItemStack item) {
        if (!isEditorTool(item)) {
            return null;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return ToolAction.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void sendSpawnChanged(Player player, MapDefinition definition, String path, int count) {
        Map<String, String> placeholders = basePlaceholders(definition);
        placeholders.put("count", String.valueOf(count));
        placeholders.put("max", String.valueOf(Math.max(count, definition.maxPlayers())));
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix(path, placeholders));
        playEditConfirmSound(player);
    }

    private void writeGameSpawns(MapDefinition definition, List<LocationSpec> spawns) {
        config().set(mapPath(definition) + ".game_spawns", spawns.stream().map(this::writeLocationSpec).toList());
        if (definition.maxPlayers() < spawns.size()) {
            config().set(mapPath(definition) + ".max_players", spawns.size());
        }
        plugin.getConfigManager().saveConfig(CONFIG_NAME);
    }

    private Map<String, String> basePlaceholders(MapDefinition definition) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", definition == null ? "未知模式" : definition.mode().getDisplayName());
        placeholders.put("map", definition == null ? "未知地图" : definition.displayName());
        placeholders.put("id", definition == null ? "unknown" : definition.mapId());
        return placeholders;
    }


    private boolean isRandomActiveMap(String activeMapId) {
        if (activeMapId == null) {
            return false;
        }
        String value = activeMapId.trim().toLowerCase(Locale.ROOT);
        return value.equals("random") || value.equals("随机") || value.equals("all") || value.equals("* ".trim());
    }

    private String defaultThemeId(String mapId) {
        String id = normalizeMapId(mapId);
        return switch (id) {
            case "nether", "hell", "地狱之柱", "下界之柱" -> "NETHER";
            case "glass", "玻璃之柱" -> "GLASS";
            case "void", "虚空之柱" -> "VOID";
            case "tnt", "tnt之柱" -> "TNT";
            case "trapdoor", "活板门之柱" -> "TRAPDOOR";
            case "ocean", "sea", "海洋之柱" -> "OCEAN";
            case "moon", "lunar", "月球之柱" -> "MOON";
            default -> "WOOL";
        };
    }

    private String mapPath(org.gamefunxiao.game.GameMode mode, String mapId) {
        return "maps." + mode.getId() + "." + normalizeMapId(mapId);
    }

    private String mapPath(MapDefinition definition) {
        return mapPath(definition.mode(), definition.mapId());
    }

    private String defaultDisplayName(org.gamefunxiao.game.GameMode mode, String mapId) {
        if (mode != null && mode.isLuckyPillars()) {
            String normalized = normalizeMapId(mapId);
            return switch (normalized) {
                case "small" -> "羊毛圆盘";
                case "middle", "medium" -> "虚空圆盘";
                case "large" -> "海洋圆盘";
                case "default" -> "羊毛圆盘";
                default -> mode.getDisplayName() + "地图-" + mapId;
            };
        }
        if ("default".equalsIgnoreCase(mapId)) {
            return mode.getDisplayName() + "默认地图";
        }
        return mode.getDisplayName() + "地图-" + mapId;
    }

    private String defaultTemplateWorldName(org.gamefunxiao.game.GameMode mode, String mapId, EditWorldKind kind) {
        if (kind == EditWorldKind.LOBBY) {
            return "gamefun_template_" + mode.getId().toLowerCase(Locale.ROOT) + "_lobby";
        }
        return "gamefun_template_" + mode.getId().toLowerCase(Locale.ROOT) + "_" + normalizeMapId(mapId) + "_" + kind.id();
    }

    private LocationSpec readLocation(String path) {
        FileConfiguration cfg = config();
        if (!cfg.isConfigurationSection(path)) {
            return null;
        }
        return new LocationSpec(
                cfg.getString(path + ".world", ""),
                cfg.getDouble(path + ".x", 0.5D),
                cfg.getDouble(path + ".y", 65.0D),
                cfg.getDouble(path + ".z", 0.5D),
                (float) cfg.getDouble(path + ".yaw", 0.0D),
                (float) cfg.getDouble(path + ".pitch", 0.0D)
        );
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

    private Map<String, Object> writeLocation(Location location) {
        return writeLocationSpec(toSpec(location));
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

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void playEditConfirmSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.55f, 1.72f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.35f);
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] cloned = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                cloned[i] = source[i].clone();
            }
        }
        return cloned;
    }
}
