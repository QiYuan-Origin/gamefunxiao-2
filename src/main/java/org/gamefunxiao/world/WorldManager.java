package org.gamefunxiao.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"deprecation", "removal"})
public class WorldManager {

    private static final String TEMPLATE_LOBBY_NAME = "hugamelobby";
    private static final String DEATH_SWAP_TEMPLATE_LOBBY_NAME = "gamefun_deathswap_lobby";
    private static final String LOBBY_PREFIX = "gamefun_lobby_";
    private static final String GAME_PREFIX = "gamefun_game_";
    private static final String END_FLASH_TUNING_WORLD_NAME = "gamefun_end_flash_debug_lobby";
    private static final int FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS = 170;
    private static final int FLASH_OUTPOST_VERIFY_RADIUS_CHUNKS = 8;
    private static final int FLASH_OUTPOST_RESCUE_RADIUS_CHUNKS = 192;
    private static final int FLASH_OUTPOST_SPAWN_ANCHOR_BLOCKS = 96;
    private static final int FLASH_OUTPOST_CANDIDATE_SEARCH_RADIUS_BLOCKS = 1536;
    private static final int FLASH_OUTPOST_SEED_MAX_ATTEMPTS = 1_000_000;
    private static final long FLASH_OUTPOST_SEARCH_TIMEOUT_NANOS = 8_000_000_000L;
    private static final long DEATH_SWAP_VILLAGE_SEARCH_TIMEOUT_NANOS = 8_000_000_000L;
    private static final int FLASH_OUTPOST_VERIFY_WORLD_MAX_ATTEMPTS = 20;
    private static final int FLASH_OUTPOST_SPACING = 32;
    private static final int FLASH_OUTPOST_SEPARATION = 8;
    private static final int FLASH_OUTPOST_DEFAULT_SALT = 165745296;
    private static final int FLASH_OUTPOST_FREQUENCY_DIVISOR = 5;
    private static final int FLASH_OUTPOST_VILLAGE_EXCLUSION_CHUNKS = 10;
    private static final int FLASH_VILLAGE_SPACING = 34;
    private static final int FLASH_VILLAGE_SEPARATION = 8;
    private static final int FLASH_VILLAGE_DEFAULT_SALT = 10387312;
    private static final long LARGE_FEATURE_X_MULTIPLIER = 341873128712L;
    private static final long LARGE_FEATURE_Z_MULTIPLIER = 132897987541L;
    private final GameFunXiao plugin;
    private World templateLobbyWorld;
    private World deathSwapTemplateLobbyWorld;
    private World endFlashTuningWorld;
    private final Map<String, World> lobbyWorlds = new HashMap<>();
    private final Map<String, Location> normalizedLobbySpawns = new HashMap<>();
    private final Map<String, World> gameWorlds = new HashMap<>();
    private final Map<String, FlashOutpostSeedStatus> flashOutpostSeedStatuses = new HashMap<>();
    private final Map<String, DeathSwapVillageSeedStatus> deathSwapVillageSeedStatuses = new HashMap<>();
    private final Map<String, World> netherWorlds = new HashMap<>();
    private final Map<String, World> endWorlds = new HashMap<>();
    private final Set<Long> issuedGameSeeds = new LinkedHashSet<>();

    public WorldManager(GameFunXiao plugin) {
        this.plugin = plugin;
        initTemplateLobbyWorld();
        initDeathSwapTemplateLobbyWorld();
    }

    private void initTemplateLobbyWorld() {
        templateLobbyWorld = Bukkit.getWorld(TEMPLATE_LOBBY_NAME);
        if (templateLobbyWorld != null) {
            applyTemplateLobbyRules(templateLobbyWorld);
            plugin.getLogger().info("模板大厅世界已加载: " + TEMPLATE_LOBBY_NAME);
            return;
        }

        File templateFolder = new File(Bukkit.getWorldContainer(), TEMPLATE_LOBBY_NAME);
        boolean existingTemplateWorld = templateFolder.exists();

        WorldCreator creator = new WorldCreator(TEMPLATE_LOBBY_NAME);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());

        templateLobbyWorld = creator.createWorld();
        if (templateLobbyWorld != null) {
            if (existingTemplateWorld) {
                applyTemplateLobbyRules(templateLobbyWorld);
                plugin.getLogger().info("模板大厅世界已从已有文件加载: " + TEMPLATE_LOBBY_NAME);
            } else {
                setupNewTemplateLobbyWorld(templateLobbyWorld);
                plugin.getLogger().info("模板大厅世界已创建: " + TEMPLATE_LOBBY_NAME);
                plugin.getLogger().info("管理员可以在此世界中建筑并设置出生点！");
            }
        }
    }

    private void initDeathSwapTemplateLobbyWorld() {
        deathSwapTemplateLobbyWorld = Bukkit.getWorld(DEATH_SWAP_TEMPLATE_LOBBY_NAME);
        if (deathSwapTemplateLobbyWorld != null) {
            applyTemplateLobbyRules(deathSwapTemplateLobbyWorld);
            plugin.getLogger().info("死亡互换等待大厅模板已加载: " + DEATH_SWAP_TEMPLATE_LOBBY_NAME);
            return;
        }

        File templateFolder = new File(Bukkit.getWorldContainer(), DEATH_SWAP_TEMPLATE_LOBBY_NAME);
        boolean existingTemplateWorld = templateFolder.exists();

        WorldCreator creator = new WorldCreator(DEATH_SWAP_TEMPLATE_LOBBY_NAME);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());

        deathSwapTemplateLobbyWorld = creator.createWorld();
        if (deathSwapTemplateLobbyWorld != null) {
            if (existingTemplateWorld) {
                applyTemplateLobbyRules(deathSwapTemplateLobbyWorld);
                plugin.getLogger().info("死亡互换等待大厅模板已从已有文件加载: " + DEATH_SWAP_TEMPLATE_LOBBY_NAME);
            } else {
                setupNewDeathSwapTemplateLobbyWorld(deathSwapTemplateLobbyWorld);
                plugin.getLogger().info("死亡互换等待大厅模板已创建: " + DEATH_SWAP_TEMPLATE_LOBBY_NAME);
            }
        }
    }

    private void setRule(World world, String name, Object value) {
        if (world == null) {
            return;
        }
        world.setGameRuleValue(name, String.valueOf(value));
    }

    private void applyTemplateLobbyRules(World world) {
        setRule(world, "doDaylightCycle", false);
        setRule(world, "doWeatherCycle", false);
        setRule(world, "doMobSpawning", false);
        setRule(world, "keepInventory", true);
        setRule(world, "announceAdvancements", false);
        setRule(world, "doFireTick", false);
        setRule(world, "mobGriefing", false);

        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void setupNewTemplateLobbyWorld(World world) {
        applyTemplateLobbyRules(world);
        world.setSpawnLocation(new Location(world, 0.5, 65, 0.5));
        createDefaultPlatform(world);
    }

    private void setupNewDeathSwapTemplateLobbyWorld(World world) {
        applyTemplateLobbyRules(world);
        int baseY = 64;
        int radius = 12;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
                boolean axis = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                Material material = edge ? Material.RED_STAINED_GLASS : (axis ? Material.GRAY_STAINED_GLASS : Material.WHITE_STAINED_GLASS);
                world.getBlockAt(x, baseY, z).setType(material, false);
            }
        }
        for (int y = baseY + 1; y <= baseY + 3; y++) {
            for (int x = -radius; x <= radius; x++) {
                world.getBlockAt(x, y, -radius).setType(Material.BARRIER, false);
                world.getBlockAt(x, y, radius).setType(Material.BARRIER, false);
            }
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(-radius, y, z).setType(Material.BARRIER, false);
                world.getBlockAt(radius, y, z).setType(Material.BARRIER, false);
            }
        }
        world.setSpawnLocation(new Location(world, 0.5D, baseY + 1.0D, 0.5D, 0.0F, 0.0F));
    }

    private void createDefaultPlatform(World world) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                world.getBlockAt(x, 64, z).setType(Material.WHITE_STAINED_GLASS);
            }
        }

        for (int y = 65; y <= 67; y++) {
            for (int x = -10; x <= 10; x++) {
                world.getBlockAt(x, y, -10).setType(Material.BARRIER);
                world.getBlockAt(x, y, 10).setType(Material.BARRIER);
            }
            for (int z = -10; z <= 10; z++) {
                world.getBlockAt(-10, y, z).setType(Material.BARRIER);
                world.getBlockAt(10, y, z).setType(Material.BARRIER);
            }
        }
    }

    public World getTemplateLobbyWorld() {
        return templateLobbyWorld;
    }

    public World createLobbyWorld(String roomId) {
        return createLobbyWorld(roomId, null);
    }

    public World createLobbyWorld(String roomId, GameMode mode) {
        String lobbyWorldName = LOBBY_PREFIX + roomId.toLowerCase();
        plugin.getLogger().info("开始创建大厅世界: " + lobbyWorldName + " (房间ID: " + roomId + ")");

        World existingWorld = Bukkit.getWorld(lobbyWorldName);
        if (existingWorld != null) {
            plugin.getLogger().info("大厅世界已存在，直接使用: " + lobbyWorldName);
            applyLobbyWorldRules(existingWorld);
            normalizeLobbySpawn(existingWorld, mode, null);
            lobbyWorlds.put(roomId, existingWorld);
            return existingWorld;
        }

        MiniGameMapManager.MapDefinition miniGameMap = null;
        World sourceTemplateWorld = null;
        if (mode != null && mode.isDeathSwap()) {
            if (deathSwapTemplateLobbyWorld == null) {
                initDeathSwapTemplateLobbyWorld();
            }
            sourceTemplateWorld = deathSwapTemplateLobbyWorld;
        }
        if (sourceTemplateWorld == null && mode != null && plugin.getMiniGameMapManager() != null && mode.isMiniGameMapEditableMode()) {
            miniGameMap = plugin.getMiniGameMapManager().findUsableMap(mode, 1);
            if (miniGameMap != null) {
                sourceTemplateWorld = getOrCreateMiniGameTemplateWorld(miniGameMap, MiniGameMapManager.EditWorldKind.LOBBY);
            }
        }

        if (sourceTemplateWorld == null && templateLobbyWorld == null) {
            plugin.getLogger().severe("模板大厅世界不存在！尝试重新初始化...");
            initTemplateLobbyWorld();
            if (templateLobbyWorld == null) {
                plugin.getLogger().severe("模板大厅世界初始化失败！");
                return null;
            }
        }

        if (sourceTemplateWorld == null) {
            sourceTemplateWorld = templateLobbyWorld;
        }

        sourceTemplateWorld.save();
        File templateFolder = sourceTemplateWorld.getWorldFolder();
        File newWorldFolder = new File(Bukkit.getWorldContainer(), lobbyWorldName);

        plugin.getLogger().info("从模板世界复制: " + sourceTemplateWorld.getName());
        plugin.getLogger().info("复制世界文件夹: " + templateFolder.getAbsolutePath() + " -> " + newWorldFolder.getAbsolutePath());

        try {
            copyWorldFolder(templateFolder, newWorldFolder);
            plugin.getLogger().info("世界文件夹复制成功");
        } catch (IOException e) {
            plugin.getLogger().severe("复制大厅世界失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        File uidFile = new File(newWorldFolder, "uid.dat");
        if (uidFile.exists() && !uidFile.delete()) {
            plugin.getLogger().warning("无法删除大厅世界 uid.dat: " + uidFile.getAbsolutePath());
        }

        plugin.getLogger().info("加载新世界: " + lobbyWorldName);
        WorldCreator creator = new WorldCreator(lobbyWorldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());

        World lobbyWorld = creator.createWorld();
        if (lobbyWorld == null) {
            plugin.getLogger().severe("世界加载失败: " + lobbyWorldName);
            return null;
        }

        applyLobbyWorldRules(lobbyWorld);
        if (miniGameMap != null && sourceTemplateWorld.getName().equalsIgnoreCase(miniGameMap.lobbyTemplateWorld())) {
            Location lobbySpawn = plugin.getMiniGameMapManager().getLobbySpawn(miniGameMap, lobbyWorld);
            if (lobbySpawn != null) {
                lobbyWorld.setSpawnLocation(lobbySpawn);
            }
        }
        normalizeLobbySpawn(lobbyWorld, mode, sourceTemplateWorld);
        lobbyWorlds.put(roomId, lobbyWorld);
        plugin.getLogger().info("世界加载成功: " + lobbyWorld.getName() + " (UUID: " + lobbyWorld.getUID() + ")");
        plugin.getLogger().info("房间大厅世界已创建并注册: " + lobbyWorldName + " -> 房间ID: " + roomId);
        return lobbyWorld;
    }

    public World getOrCreateEndFlashTuningWorld() {
        if (endFlashTuningWorld != null) {
            applyLobbyWorldRules(endFlashTuningWorld);
            return endFlashTuningWorld;
        }

        World loadedWorld = Bukkit.getWorld(END_FLASH_TUNING_WORLD_NAME);
        if (loadedWorld != null) {
            endFlashTuningWorld = loadedWorld;
            applyLobbyWorldRules(endFlashTuningWorld);
            plugin.getLogger().info("终章调试世界已加载: " + END_FLASH_TUNING_WORLD_NAME);
            return endFlashTuningWorld;
        }

        if (templateLobbyWorld == null) {
            initTemplateLobbyWorld();
            if (templateLobbyWorld == null) {
                plugin.getLogger().severe("无法创建终章调试世界：模板大厅 hugamelobby 不存在");
                return null;
            }
        }

        File targetFolder = new File(Bukkit.getWorldContainer(), END_FLASH_TUNING_WORLD_NAME);
        if (!targetFolder.exists()) {
            templateLobbyWorld.save();
            try {
                copyWorldFolder(templateLobbyWorld.getWorldFolder(), targetFolder);
                File uidFile = new File(targetFolder, "uid.dat");
                if (uidFile.exists() && !uidFile.delete()) {
                    plugin.getLogger().warning("无法删除终章调试世界 uid.dat: " + uidFile.getAbsolutePath());
                }
                plugin.getLogger().info("终章调试世界已从等待大厅模板复制: " + END_FLASH_TUNING_WORLD_NAME);
            } catch (IOException exception) {
                plugin.getLogger().severe("复制终章调试世界失败: " + exception.getMessage());
                exception.printStackTrace();
                return null;
            }
        }

        WorldCreator creator = new WorldCreator(END_FLASH_TUNING_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());

        endFlashTuningWorld = creator.createWorld();
        if (endFlashTuningWorld == null) {
            plugin.getLogger().severe("终章调试世界加载失败: " + END_FLASH_TUNING_WORLD_NAME);
            return null;
        }

        applyLobbyWorldRules(endFlashTuningWorld);
        Location templateSpawn = templateLobbyWorld.getSpawnLocation();
        endFlashTuningWorld.setSpawnLocation(new Location(endFlashTuningWorld,
                templateSpawn.getX(), templateSpawn.getY(), templateSpawn.getZ(),
                templateSpawn.getYaw(), templateSpawn.getPitch()));
        plugin.getLogger().info("终章调试世界已就绪: " + END_FLASH_TUNING_WORLD_NAME);
        return endFlashTuningWorld;
    }

    public boolean isEndFlashTuningWorld(World world) {
        return world != null && END_FLASH_TUNING_WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    public World getOrCreateMiniGameTemplateWorld(MiniGameMapManager.MapDefinition definition,
                                                  MiniGameMapManager.EditWorldKind kind) {
        if (definition == null || kind == null) {
            return null;
        }
        String worldName;
        if (kind == MiniGameMapManager.EditWorldKind.LOBBY) {
            worldName = definition.lobbyTemplateWorld();
        } else {
            worldName = definition.gameTemplateWorld();
        }
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            applyTemplateLobbyRules(loaded);
            return loaded;
        }

        File templateFolder = new File(Bukkit.getWorldContainer(), worldName);
        boolean existingWorld = templateFolder.exists();
        if (!existingWorld && !definition.autoCreateTemplate()) {
            return null;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().severe("小游戏模板世界加载失败: " + worldName);
            return null;
        }

        applyTemplateLobbyRules(world);
        if (!existingWorld) {
            setupNewMiniGameTemplateWorld(world, definition, kind);
            plugin.getLogger().info("小游戏模板世界已创建: " + worldName + " (" + definition.mode().getDisplayName() + " / " + kind.displayName() + ")");
        } else {
            plugin.getLogger().info("小游戏模板世界已从已有文件加载: " + worldName);
        }
        return world;
    }

    private void setupNewMiniGameTemplateWorld(World world, MiniGameMapManager.MapDefinition definition,
                                               MiniGameMapManager.EditWorldKind kind) {
        world.setSpawnLocation(new Location(world, 0.5D, kind == MiniGameMapManager.EditWorldKind.LOBBY ? 65.0D : 150.0D, 0.5D));
        if (kind == MiniGameMapManager.EditWorldKind.LOBBY) {
            createMiniGameLobbyTemplate(world, definition);
            plugin.getMiniGameMapManager().writeDefaultLobbyTemplateData(definition, world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
            return;
        }

        if (definition.mode().isLuckyPillars()) {
            createLuckyPillarsTemplate(world, definition);
        } else {
            createDefaultPlatform(world);
        }
    }

    private void createMiniGameLobbyTemplate(World world, MiniGameMapManager.MapDefinition definition) {
        int baseY = 64;
        int radius = 12;
        Material corner = definition != null && definition.mode() != null && definition.mode().isLuckyPillars()
                ? Material.LIME_STAINED_GLASS
                : Material.LIGHT_BLUE_STAINED_GLASS;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
                boolean axis = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                Material material = edge ? Material.GRAY_STAINED_GLASS : (axis ? corner : Material.WHITE_STAINED_GLASS);
                world.getBlockAt(x, baseY, z).setType(material, false);
            }
        }
        for (int y = baseY + 1; y <= baseY + 3; y++) {
            for (int x = -radius; x <= radius; x++) {
                world.getBlockAt(x, y, -radius).setType(Material.BARRIER, false);
                world.getBlockAt(x, y, radius).setType(Material.BARRIER, false);
            }
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(-radius, y, z).setType(Material.BARRIER, false);
                world.getBlockAt(radius, y, z).setType(Material.BARRIER, false);
            }
        }
        world.setSpawnLocation(new Location(world, 0.5D, baseY + 1.0D, 0.5D, 0.0F, 0.0F));
    }

    private void createLuckyPillarsTemplate(World world, MiniGameMapManager.MapDefinition definition) {
        int count = Math.max(2, Math.min(64, definition.maxPlayers() <= 0 ? 16 : definition.maxPlayers()));
        boolean useCenterPillar = count >= 5;
        int outerCount = Math.max(1, count - (useCenterPillar ? 1 : 0));
        double radius = 18.0D + Math.max(0, outerCount - 8) * 2.75D;
        int baseY = 118;
        int topY = baseY + 30;
        int eliminationY = Math.max(world.getMinHeight(), topY - 28);
        double boundaryRadius = Math.max(28.0D, radius + 10.0D);
        Location center = new Location(world, 0.5D, topY + 1.0D, 0.5D, 0.0F, 10.0F);
        List<Location> spawns = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int pillarX;
            int pillarZ;
            boolean centerPillar = useCenterPillar && i == 0;
            if (centerPillar) {
                pillarX = 0;
                pillarZ = 0;
            } else {
                int ringIndex = useCenterPillar ? i - 1 : i;
                double angle = (Math.PI * 2.0D / outerCount) * ringIndex - Math.PI / 2.0D;
                pillarX = (int) Math.round(Math.cos(angle) * radius);
                pillarZ = (int) Math.round(Math.sin(angle) * radius);
            }
            buildTemplateLuckyPillar(world, pillarX, baseY, topY, pillarZ, centerPillar);
            int cagePlayerY = topY + 3;
            buildTemplateLuckyPillarsCage(world, pillarX, cagePlayerY, pillarZ, centerPillar);
            Location spawn = new Location(world, pillarX + 0.5D, cagePlayerY, pillarZ + 0.5D,
                    (float) Math.toDegrees(Math.atan2(center.getZ() - pillarZ, center.getX() - pillarX)) - 90.0F,
                    8.0F);
            spawns.add(spawn);
        }

        Location spectator = center.clone().add(0.0D, 12.0D, 0.0D);
        spectator.setPitch(35.0F);
        world.setSpawnLocation(center);
        world.getWorldBorder().setCenter(center);
        world.getWorldBorder().setSize(Math.max(60.0D, boundaryRadius * 2.0D));
        plugin.getMiniGameMapManager().writeDefaultGameTemplateData(definition, center, eliminationY, boundaryRadius, spawns);
    }

    private void buildTemplateLuckyPillar(World world, int centerX, int baseY, int topY, int centerZ, boolean centerPillar) {
        int pillarRadius = centerPillar ? 2 : 1;
        for (int y = baseY; y <= topY; y++) {
            for (int x = -pillarRadius; x <= pillarRadius; x++) {
                for (int z = -pillarRadius; z <= pillarRadius; z++) {
                    if (Math.abs(x) + Math.abs(z) > pillarRadius + 1) {
                        continue;
                    }
                    world.getBlockAt(centerX + x, y, centerZ + z).setType(Material.BEDROCK, false);
                }
            }
        }
        int platformRadius = centerPillar ? 3 : 2;
        for (int x = -platformRadius; x <= platformRadius; x++) {
            for (int z = -platformRadius; z <= platformRadius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) > platformRadius) {
                    continue;
                }
                world.getBlockAt(centerX + x, topY, centerZ + z).setType(Material.BEDROCK, false);
            }
        }
    }

    private void buildTemplateLuckyPillarsCage(World world, int centerX, int playerY, int centerZ, boolean centerPillar) {
        int[][] offsets = {
                {0, -2, 0},
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {0, 2, 0}
        };
        for (int[] offset : offsets) {
            world.getBlockAt(centerX + offset[0], playerY + offset[1], centerZ + offset[2]).setType(Material.GLASS, false);
        }
    }
    private void applyLobbyWorldRules(World world) {
        setRule(world, "doDaylightCycle", false);
        setRule(world, "doWeatherCycle", false);
        setRule(world, "doMobSpawning", false);
        setRule(world, "keepInventory", true);
        setRule(world, "announceAdvancements", false);
        setRule(world, "doFireTick", false);
        setRule(world, "mobGriefing", false);
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
    }

    public boolean isLobbyLikeWorld(World world) {
        if (world == null) {
            return false;
        }
        if (world.equals(templateLobbyWorld)) {
            return true;
        }
        if (world.equals(deathSwapTemplateLobbyWorld)) {
            return true;
        }
        String name = world.getName();
        return TEMPLATE_LOBBY_NAME.equalsIgnoreCase(name)
                || DEATH_SWAP_TEMPLATE_LOBBY_NAME.equalsIgnoreCase(name)
                || END_FLASH_TUNING_WORLD_NAME.equalsIgnoreCase(name)
                || (plugin.getMiniGameMapManager() != null && plugin.getMiniGameMapManager().isTemplateWorldName(name))
                || name.toLowerCase().startsWith(LOBBY_PREFIX)
                || lobbyWorlds.containsValue(world);
    }

    public boolean isPrimaryGameWorld(String roomId, World world) {
        if (roomId == null || roomId.isBlank() || world == null) {
            return false;
        }
        World tracked = gameWorlds.get(roomId);
        return tracked != null && tracked.equals(world);
    }

    public void keepLobbyWeatherClear() {
        if (templateLobbyWorld != null) {
            applyLobbyWorldRules(templateLobbyWorld);
        }
        if (deathSwapTemplateLobbyWorld != null) {
            applyLobbyWorldRules(deathSwapTemplateLobbyWorld);
        }
        if (endFlashTuningWorld != null) {
            applyLobbyWorldRules(endFlashTuningWorld);
        }
        for (World world : new ArrayList<>(lobbyWorlds.values())) {
            applyLobbyWorldRules(world);
        }
    }

    private void copyWorldFolder(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                throw new IOException("无法创建目录: " + target.getAbsolutePath());
            }

            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    if (child.equals("session.lock") || child.equals("uid.dat")) {
                        continue;
                    }
                    copyWorldFolder(new File(source, child), new File(target, child));
                }
            }
            return;
        }

        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public World getLobbyWorld(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        World tracked = lobbyWorlds.get(roomId);
        if (tracked != null) {
            return tracked;
        }

        World loaded = Bukkit.getWorld(LOBBY_PREFIX + roomId.toLowerCase(Locale.ROOT));
        if (loaded != null) {
            applyLobbyWorldRules(loaded);
            lobbyWorlds.put(roomId, loaded);
        }
        return loaded;
    }

    public String getLobbyRoomIdByWorld(World world) {
        if (world == null) {
            return null;
        }
        for (Map.Entry<String, World> entry : lobbyWorlds.entrySet()) {
            if (world.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        String name = world.getName();
        if (name == null) {
            return null;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lowerName.startsWith(LOBBY_PREFIX)) {
            return null;
        }
        String roomId = name.substring(LOBBY_PREFIX.length());
        if (!roomId.isBlank()) {
            lobbyWorlds.putIfAbsent(roomId, world);
            applyLobbyWorldRules(world);
        }
        return roomId.isBlank() ? null : roomId;
    }

    public boolean isRoomLobbyWorld(String roomId, World world) {
        if (roomId == null || roomId.isBlank() || world == null) {
            return false;
        }
        World tracked = getLobbyWorld(roomId);
        if (tracked != null && tracked.equals(world)) {
            return true;
        }
        String resolvedRoomId = getLobbyRoomIdByWorld(world);
        return resolvedRoomId != null && resolvedRoomId.equalsIgnoreCase(roomId);
    }

    /**
     * 返回房间等待大厅的实际安全出生点。大厅虚空保护和玩家进入大厅必须共用这个方法，
     * 不能分别读取游戏世界出生点或玩家当前坐标。
     */
    public Location getLobbySpawnLocation(String roomId, GameMode mode) {
        World lobbyWorld = getLobbyWorld(roomId);
        if (lobbyWorld == null) {
            return null;
        }
        Location cached = normalizedLobbySpawns.get(lobbyWorld.getName());
        if (cached != null && cached.getWorld() != null && cached.getWorld().equals(lobbyWorld)) {
            return cached.clone();
        }
        Location safe = normalizeLobbySpawn(lobbyWorld, mode, null);
        return safe == null ? null : safe.clone();
    }

    public Location getLobbySpawnLocation(String roomId) {
        return getLobbySpawnLocation(roomId, null);
    }

    public Location getLobbySpawnLocationForWorld(World world, GameMode mode) {
        Location safe = normalizeLobbySpawn(world, mode, null);
        return safe == null ? null : safe.clone();
    }

    private Location normalizeLobbySpawn(World world, GameMode mode, World sourceTemplateWorld) {
        if (world == null) {
            return null;
        }

        String worldName = world.getName();
        Location cached = normalizedLobbySpawns.get(worldName);
        if (cached != null && cached.getWorld() != null && cached.getWorld().equals(world)) {
            return cached.clone();
        }
        normalizedLobbySpawns.remove(worldName);

        Location resolved = null;
        if (mode != null && mode.isMiniGameMapEditableMode() && plugin.getMiniGameMapManager() != null) {
            MiniGameMapManager.MapDefinition map = plugin.getMiniGameMapManager().findUsableMap(mode, 1);
            if (map != null && map.lobbySpawn() != null) {
                resolved = map.lobbySpawn().toLocation(world);
            }
        }
        if (resolved == null && sourceTemplateWorld != null) {
            resolved = copyLocationToWorld(sourceTemplateWorld.getSpawnLocation(), world);
        }
        if (resolved == null && mode != null && mode.isDeathSwap() && deathSwapTemplateLobbyWorld != null) {
            resolved = copyLocationToWorld(deathSwapTemplateLobbyWorld.getSpawnLocation(), world);
        }
        if (resolved == null && templateLobbyWorld != null
                && (mode == null || !mode.isMiniGameMapEditableMode())) {
            resolved = copyLocationToWorld(templateLobbyWorld.getSpawnLocation(), world);
        }
        if (resolved == null) {
            resolved = world.getSpawnLocation().clone();
        }
        normalizedLobbySpawns.put(worldName, resolved.clone());
        return resolved;
    }

    private Location copyLocationToWorld(Location source, World targetWorld) {
        if (source == null || targetWorld == null) {
            return null;
        }
        return new Location(targetWorld, source.getX(), source.getY(), source.getZ(),
                source.getYaw(), source.getPitch());
    }

    public World getGameWorld(String roomId) {
        return gameWorlds.get(roomId);
    }

    private org.mvplugins.multiverse.core.world.WorldManager getMVWorldManager() {
        try {
            if (!MultiverseCoreApi.isLoaded()) {
                return null;
            }
            return MultiverseCoreApi.get().getWorldManager();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public World createGameWorld(String roomId) {
        return createGameWorld(roomId, null);
    }

    public World createGameWorld(String roomId, GameMode mode) {
        flashOutpostSeedStatuses.remove(roomId);
        deathSwapVillageSeedStatuses.remove(roomId);
        if (mode != null && mode.isDeathSwap()) {
            return createDeathSwapGameWorld(roomId);
        }
        if (isFlashOutpostSeededMode(mode)) {
            return createFlashOutpostSeededGameWorld(roomId, mode);
        }
        String worldName = GAME_PREFIX + roomId.toLowerCase();
        long seed = nextUniqueGameSeed(null);
        markGameSeedIssued(seed);

        prepareFreshWorldFolder(worldName);
        World world = createNormalGameWorldWithSeed(worldName, seed, true);

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            setupGameWorld(world);
            gameWorlds.put(roomId, world);
        }
        return world;
    }

    public World createDeathSwapGameWorld(String roomId) {
        deathSwapVillageSeedStatuses.remove(roomId);
        String worldName = GAME_PREFIX + roomId.toLowerCase();
        int radiusBlocks = plugin.getConfigManager().getDeathSwapVillageRadiusBlocks();
        int maxAttempts = plugin.getConfigManager().getDeathSwapVillageMaxAttempts();
        int villageSalt = readSpigotStructureSeed(worldName, "seed-village", FLASH_VILLAGE_DEFAULT_SALT);
        plugin.getLogger().info("开始为死亡互换外部算法筛选村庄种子: " + worldName
                + "，要求出生点附近 " + radiusBlocks + " 格内有村庄候选，筛中前不会创建世界，超过 8 秒就随机世界"
                + "，villageSalt=" + villageSalt);

        long searchStartedAt = System.nanoTime();
        Set<Long> attemptedSeeds = new LinkedHashSet<>();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (isDeathSwapVillageSearchTimedOut(searchStartedAt)) {
                return createDeathSwapRandomFallbackGameWorld(roomId, worldName, attemptedSeeds,
                        "死亡互换外部筛村庄超过 8 秒，随机创建世界", attempt - 1);
            }
            long seed = nextUniqueGameSeed(attemptedSeeds);
            DeathSwapVillageSeedCandidate candidate = findDeathSwapVillageSeedCandidate(seed, attempt, villageSalt, radiusBlocks);
            if (candidate == null) {
                if (attempt == 1 || attempt % 128 == 0) {
                    plugin.getLogger().info("死亡互换外部筛种进度 " + attempt + "/" + maxAttempts
                            + "：还没有命中可用村庄候选，不创建世界，已去重种子数=" + attemptedSeeds.size());
                }
                continue;
            }

            plugin.getLogger().info("死亡互换外部筛种命中候选: " + worldName
                    + " seed=" + seed
                    + " attempt=" + attempt
                    + " chunk=" + candidate.villageChunkX() + "," + candidate.villageChunkZ()
                    + " predictedVillage=" + candidate.villageBlockX() + ",0," + candidate.villageBlockZ()
                    + " originDistance=" + Math.round(candidate.distanceFromOrigin())
                    + "，开始创建最终游戏世界；本次筛种只创建这一个世界");

            markGameSeedIssued(seed);
            prepareFreshWorldFolder(worldName);
            World world = createNormalGameWorldWithSeed(worldName, seed, true);
            if (world == null) {
                recordDeathSwapVillageStatus(roomId, false, true, null, null,
                        "死亡互换命中候选后世界创建失败");
                plugin.getLogger().warning("死亡互换候选世界创建失败: seed=" + seed + " attempt=" + attempt);
                return null;
            }
            world.setKeepSpawnInMemory(false);
            setupGameWorld(world);
            Location villageLocation = candidate.toLocation(world);
            Location flatSpawn = findDeathSwapFlatSpawn(world, villageLocation, radiusBlocks);
            if (flatSpawn != null) {
                world.setSpawnLocation(flatSpawn);
            }
            Location spawn = world.getSpawnLocation();
            gameWorlds.put(roomId, world);
            recordDeathSwapVillageStatus(roomId, true, false, spawn, villageLocation,
                    "外部算法筛中村庄候选");
            plugin.getLogger().info("死亡互换外部筛种最终世界已创建: " + worldName
                    + " seed=" + seed
                    + " attempt=" + attempt
                    + " spawn=" + formatBlockLocation(spawn)
                    + " predictedVillage=" + formatBlockLocation(villageLocation)
                    + " distance=" + Math.round(Math.sqrt(horizontalDistanceSquared(spawn, villageLocation)))
                    + "；按要求不再反复创建/定位验证世界");
            return world;
        }

        return createDeathSwapRandomFallbackGameWorld(roomId, worldName, attemptedSeeds,
                "达到最大外部筛种次数后随机创建世界", maxAttempts);
    }

    private boolean isDeathSwapVillageSearchTimedOut(long searchStartedAt) {
        return System.nanoTime() - searchStartedAt >= DEATH_SWAP_VILLAGE_SEARCH_TIMEOUT_NANOS;
    }

    private World createDeathSwapRandomFallbackGameWorld(String roomId, String worldName, Set<Long> attemptedSeeds,
                                                        String reason, int attempts) {
        long seed = nextUniqueGameSeed(attemptedSeeds);
        markGameSeedIssued(seed);
        prepareFreshWorldFolder(worldName);
        World fallback = createNormalGameWorldWithSeed(worldName, seed, true);
        if (fallback != null) {
            fallback.setKeepSpawnInMemory(false);
            setupGameWorld(fallback);
            Location flatSpawn = findDeathSwapFlatSpawn(fallback);
            if (flatSpawn != null) {
                fallback.setSpawnLocation(flatSpawn);
            }
            gameWorlds.put(roomId, fallback);
            recordDeathSwapVillageStatus(roomId, false, true, fallback.getSpawnLocation(), null, reason);
            plugin.getLogger().warning("死亡互换村庄筛种降级随机世界: " + worldName
                    + " seed=" + seed
                    + " reason=" + reason
                    + " attempts=" + attempts
                    + " uniqueSeeds=" + (attemptedSeeds == null ? 0 : attemptedSeeds.size())
                    + "；按要求不再继续筛或验证村庄");
        } else {
            recordDeathSwapVillageStatus(roomId, false, true, null, null, reason + "，但随机世界创建失败");
            plugin.getLogger().severe("死亡互换村庄筛种降级随机世界失败: " + worldName
                    + " seed=" + seed
                    + " reason=" + reason);
        }
        return fallback;
    }

    private DeathSwapVillageSeedCandidate findDeathSwapVillageSeedCandidate(long seed, int attempt, int villageSalt, int radiusBlocks) {
        int safeRadius = Math.max(96, radiusBlocks);
        int radiusChunks = (safeRadius + 15) / 16 + 1;
        int minRegionX = Math.floorDiv(-radiusChunks, FLASH_VILLAGE_SPACING);
        int maxRegionX = Math.floorDiv(radiusChunks, FLASH_VILLAGE_SPACING);
        int minRegionZ = Math.floorDiv(-radiusChunks, FLASH_VILLAGE_SPACING);
        int maxRegionZ = Math.floorDiv(radiusChunks, FLASH_VILLAGE_SPACING);
        DeathSwapVillageSeedCandidate best = null;
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                ChunkCandidate villageChunk = getRandomSpreadChunkForRegion(
                        seed,
                        regionX,
                        regionZ,
                        FLASH_VILLAGE_SPACING,
                        FLASH_VILLAGE_SEPARATION,
                        villageSalt
                );
                int blockX = chunkLocateBlock(villageChunk.chunkX());
                int blockZ = chunkLocateBlock(villageChunk.chunkZ());
                double distance = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
                if (distance > safeRadius) {
                    continue;
                }
                DeathSwapVillageSeedCandidate candidate = new DeathSwapVillageSeedCandidate(
                        seed,
                        attempt,
                        villageChunk.chunkX(),
                        villageChunk.chunkZ(),
                        blockX,
                        blockZ,
                        distance
                );
                if (best == null || candidate.distanceFromOrigin() < best.distanceFromOrigin()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    public Location findDeathSwapFlatSpawn(World world) {
        if (world == null) {
            return null;
        }
        return findDeathSwapFlatSpawn(world, world.getSpawnLocation(), 128);
    }

    private Location findDeathSwapFlatSpawn(World world, Location preferred, int maxRadiusBlocks) {
        if (world == null) {
            return null;
        }
        Location anchor = preferred == null || preferred.getWorld() == null
                ? world.getSpawnLocation()
                : preferred;
        int centerX = anchor.getBlockX();
        int centerZ = anchor.getBlockZ();
        int safeMaxRadius = Math.max(16, Math.min(192, maxRadiusBlocks));
        int[] radii = {0, 8, 16, 24, 32, 48, 64, 80, 96, 128, 160, 192};
        int[][] directions = {
                {0, 0},
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {2, 1}, {-2, 1}, {2, -1}, {-2, -1},
                {1, 2}, {-1, 2}, {1, -2}, {-1, -2}
        };
        for (int radius : radii) {
            if (radius > safeMaxRadius) {
                break;
            }
            for (int[] direction : directions) {
                if (radius == 0 && (direction[0] != 0 || direction[1] != 0)) {
                    continue;
                }
                if (radius > 0 && direction[0] == 0 && direction[1] == 0) {
                    continue;
                }
                double length = Math.sqrt((double) direction[0] * direction[0] + (double) direction[1] * direction[1]);
                int x = centerX + (radius == 0 ? 0 : (int) Math.round(radius * direction[0] / length));
                int z = centerZ + (radius == 0 ? 0 : (int) Math.round(radius * direction[1] / length));
                Location candidate = getSurfaceSpawnAt(world, x, z);
                if (isDeathSwapFlatSpawnCandidate(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isDeathSwapFlatSpawnCandidate(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int centerX = location.getBlockX();
        int centerZ = location.getBlockZ();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                int standY = Math.min(world.getMaxHeight() - 2,
                        Math.max(world.getMinHeight() + 2, world.getHighestBlockYAt(x, z) + 1));
                Block floor = world.getBlockAt(x, standY - 1, z);
                Block feet = world.getBlockAt(x, standY, z);
                Block head = world.getBlockAt(x, standY + 1, z);
                if (!isDeathSwapSafeFloor(floor)
                        || !isDeathSwapPassableForSpawn(feet)
                        || !isDeathSwapPassableForSpawn(head)) {
                    return false;
                }
                minY = Math.min(minY, standY);
                maxY = Math.max(maxY, standY);
            }
        }
        if (maxY - minY > 1) {
            return false;
        }
        location.setY(world.getHighestBlockYAt(centerX, centerZ) + 1.0D);
        location.setX(centerX + 0.5D);
        location.setZ(centerZ + 0.5D);
        return true;
    }

    private boolean isDeathSwapSafeFloor(Block block) {
        if (block == null || block.isEmpty() || block.isLiquid() || !block.getType().isSolid()) {
            return false;
        }
        Material type = block.getType();
        String name = type.name();
        return type != Material.CACTUS
                && type != Material.MAGMA_BLOCK
                && type != Material.CAMPFIRE
                && type != Material.SOUL_CAMPFIRE
                && type != Material.POWDER_SNOW
                && !name.contains("LEAVES")
                && !name.contains("LOG")
                && !name.contains("WOOD")
                && !name.contains("STEM")
                && !name.contains("HYPHAE")
                && !name.contains("WART_BLOCK");
    }

    private boolean isDeathSwapPassableForSpawn(Block block) {
        return block != null && (block.isEmpty() || block.isPassable()) && !block.isLiquid();
    }

    private StructureSearchResult findNearestNonDesertVillageNearSpawn(World world, int radiusChunks) {
        if (world == null) {
            return null;
        }
        Location spawn = world.getSpawnLocation();
        StructureSearchResult best = null;
        double bestDistance = Double.MAX_VALUE;
        Structure[] candidates = {
                Structure.VILLAGE_PLAINS,
                Structure.VILLAGE_SAVANNA,
                Structure.VILLAGE_SNOWY,
                Structure.VILLAGE_TAIGA
        };
        for (Structure structure : candidates) {
            try {
                StructureSearchResult result = world.locateNearestStructure(spawn, structure, Math.max(1, radiusChunks), true);
                if (result == null || result.getLocation() == null) {
                    continue;
                }
                double distance = horizontalDistanceSquared(spawn, result.getLocation());
                if (distance < bestDistance) {
                    best = result;
                    bestDistance = distance;
                }
            } catch (Throwable throwable) {
                plugin.getLogger().warning("死亡互换定位村庄失败: " + world.getName()
                        + " structure=" + structure.getKey() + " - " + throwable.getMessage());
            }
        }
        return best;
    }

    private boolean isVillageWithinRadius(Location spawn, StructureSearchResult village, int radiusBlocks) {
        return spawn != null
                && spawn.getWorld() != null
                && village != null
                && village.getLocation() != null
                && village.getLocation().getWorld() != null
                && spawn.getWorld().equals(village.getLocation().getWorld())
                && horizontalDistanceSquared(spawn, village.getLocation()) <= (double) radiusBlocks * radiusBlocks;
    }

    private void recordDeathSwapVillageStatus(String roomId, boolean confirmed, boolean fallback,
                                              Location spawn, Location village, String reason) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        int distance = -1;
        if (spawn != null && village != null && spawn.getWorld() != null && village.getWorld() != null
                && spawn.getWorld().equals(village.getWorld())) {
            distance = (int) Math.round(Math.sqrt(horizontalDistanceSquared(spawn, village)));
        }
        deathSwapVillageSeedStatuses.put(roomId, new DeathSwapVillageSeedStatus(
                confirmed,
                fallback,
                distance,
                village == null ? "" : formatBlockLocation(village),
                reason == null || reason.isBlank() ? "未知" : reason
        ));
    }

    public DeathSwapVillageSeedStatus getDeathSwapVillageSeedStatus(String roomId) {
        return roomId == null ? null : deathSwapVillageSeedStatuses.get(roomId);
    }

    public record DeathSwapVillageSeedStatus(boolean confirmed, boolean fallback, int distance,
                                             String villageLocation, String reason) {
    }

    private World createNormalGameWorldWithSeed(String worldName, long seed, boolean verbose) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " NORMAL --seed " + seed);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.seed(seed);
            creator.type(WorldType.NORMAL);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(true);
            creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);
            world = creator.createWorld();
            if (world != null && verbose) {
                plugin.getLogger().info("游戏世界已创建(Bukkit降级): " + worldName);
            }
        } else if (verbose) {
            plugin.getLogger().info("游戏世界已创建(MV): " + worldName);
        }
        return world;
    }

    private boolean isFlashOutpostSeededMode(GameMode mode) {
        return mode == GameMode.FLASH || mode == GameMode.FLASH_TOURNAMENT;
    }

    private World createFlashOutpostSeededGameWorld(String roomId, GameMode mode) {
        String worldName = GAME_PREFIX + roomId.toLowerCase();
        String modeName = mode == null ? "闪光" : mode.getDisplayName().replaceAll("§.", "");
        int outpostSalt = readSpigotStructureSeed(worldName, "seed-outpost", FLASH_OUTPOST_DEFAULT_SALT);
        int villageSalt = readSpigotStructureSeed(worldName, "seed-village", FLASH_VILLAGE_DEFAULT_SALT);
        plugin.getLogger().info("开始为 " + modeName + " 外部算法筛选哨塔种子: " + worldName
                + "，要求出生点 " + FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS
                + " 格内必有掠夺者哨塔，候选搜索半径 " + FLASH_OUTPOST_CANDIDATE_SEARCH_RADIUS_BLOCKS
                + " 格，筛中候选前不会创建世界"
                + "，outpostSalt=" + outpostSalt + "，villageSalt=" + villageSalt);

        Set<Long> attemptedSeeds = new LinkedHashSet<>();
        long searchStartedAt = System.nanoTime();
        int verifiedWorlds = 0;
        for (int attempt = 1; attempt <= FLASH_OUTPOST_SEED_MAX_ATTEMPTS; attempt++) {
            if (isFlashOutpostSearchTimedOut(searchStartedAt)) {
                return createRandomFallbackGameWorld(roomId, worldName, attemptedSeeds,
                        "外部筛种超过 8 秒，已停止搜索并改为随机世界",
                        attempt - 1,
                        verifiedWorlds);
            }

            long seed = nextUniqueGameSeed(attemptedSeeds);
            FlashOutpostSeedCandidate candidate = findFlashOutpostSeedCandidate(seed, attempt, outpostSalt, villageSalt);
            if (candidate == null) {
                if (attempt == 1 || attempt % 512 == 0) {
                    plugin.getLogger().info("闪光外部筛种进度 " + attempt + "/" + FLASH_OUTPOST_SEED_MAX_ATTEMPTS
                            + "：还没有命中可用哨塔候选，不创建世界，已去重种子数=" + attemptedSeeds.size());
                }
                continue;
            }

            if (verifiedWorlds >= FLASH_OUTPOST_VERIFY_WORLD_MAX_ATTEMPTS) {
                return createRandomFallbackGameWorld(roomId, worldName, attemptedSeeds,
                        "候选世界连续 " + verifiedWorlds + " 次未通过服务器哨塔确认，改为随机世界",
                        attempt - 1,
                        verifiedWorlds);
            }

            plugin.getLogger().info("闪光外部筛种命中候选: " + worldName
                    + " seed=" + seed
                    + " attempt=" + attempt
                    + " chunk=" + candidate.outpostChunkX() + "," + candidate.outpostChunkZ()
                    + " predictedOutpost=" + candidate.outpostBlockX() + ",0," + candidate.outpostBlockZ()
                    + " originDistance=" + Math.round(candidate.distanceFromOrigin())
                    + "，开始创建第 " + (verifiedWorlds + 1) + " 个候选验证世界");

            markGameSeedIssued(seed);
            prepareFreshWorldFolder(worldName);
            World world = createNormalGameWorldWithSeed(worldName, seed, false);
            if (world == null) {
                plugin.getLogger().warning("闪光外部候选创建世界失败: seed=" + seed + " attempt=" + attempt + "，继续寻找下一个候选");
                continue;
            }

            world.setKeepSpawnInMemory(false);
            setupGameWorld(world);
            verifiedWorlds++;
            if (confirmFinalOutpostCheck(roomId, world, candidate, seed, attempt, verifiedWorlds)) {
                gameWorlds.put(roomId, world);
                return world;
            }

            plugin.getLogger().warning("闪光候选世界未通过服务器哨塔确认，已删除并继续筛下一个候选: "
                    + worldName + " seed=" + seed + " verifiedWorlds=" + verifiedWorlds);
            deleteWorld(world);
        }

        return createRandomFallbackGameWorld(roomId, worldName, attemptedSeeds,
                "外部筛种达到最大尝试次数，改为随机世界",
                FLASH_OUTPOST_SEED_MAX_ATTEMPTS,
                verifiedWorlds);
    }

    private boolean confirmFinalOutpostCheck(String roomId, World world, FlashOutpostSeedCandidate candidate, long seed, int attempt, int verifiedWorlds) {
        if (world == null || candidate == null) {
            return false;
        }
        StructureSearchResult outpost = findPillagerOutpostNearSpawn(world);
        Location spawn = world.getSpawnLocation();
        if (isOutpostWithinSpawnRadius(spawn, outpost, null)) {
            logConfirmedOutpost(world, seed, attempt, verifiedWorlds, spawn, outpost.getLocation(), "原出生点确认");
            recordFlashOutpostStatus(roomId, true, false, spawn, outpost.getLocation(), "原出生点确认");
            return true;
        }

        StructureSearchResult predictedOutpost = findPredictedPillagerOutpost(world, candidate);
        if (tryAnchorFlashSpawnNearOutpost(world, predictedOutpost)) {
            Location anchoredSpawn = world.getSpawnLocation();
            if (isOutpostWithinSpawnRadius(anchoredSpawn, predictedOutpost, null)) {
                logConfirmedOutpost(world, seed, attempt, verifiedWorlds,
                        anchoredSpawn, predictedOutpost.getLocation(), "预测哨塔校准出生点");
                recordFlashOutpostStatus(roomId, true, false, anchoredSpawn, predictedOutpost.getLocation(), "预测哨塔校准出生点");
                return true;
            }
        }

        StructureSearchResult rescueOutpost = findNearestPillagerOutpostForSpawnRescue(world);
        if (tryAnchorFlashSpawnNearOutpost(world, rescueOutpost)) {
            Location anchoredSpawn = world.getSpawnLocation();
            if (isOutpostWithinSpawnRadius(anchoredSpawn, rescueOutpost, null)) {
                logConfirmedOutpost(world, seed, attempt, verifiedWorlds,
                        anchoredSpawn, rescueOutpost.getLocation(), "服务器定位哨塔校准出生点");
                recordFlashOutpostStatus(roomId, true, false, anchoredSpawn, rescueOutpost.getLocation(), "服务器定位哨塔校准出生点");
                return true;
            }
        }

        plugin.getLogger().warning("闪光外部筛种候选未通过服务器结构定位确认: " + world.getName()
                + " seed=" + seed
                + " attempt=" + attempt
                + " verifiedWorlds=" + verifiedWorlds
                + " spawn=" + formatBlockLocation(spawn)
                + " serverOutpost=" + (outpost == null ? "null" : formatBlockLocation(outpost.getLocation()))
                + " predictedServerOutpost=" + (predictedOutpost == null ? "null" : formatBlockLocation(predictedOutpost.getLocation()))
                + " rescueOutpost=" + (rescueOutpost == null ? "null" : formatBlockLocation(rescueOutpost.getLocation()))
                + " predictedOutpost=" + candidate.outpostBlockX() + ",0," + candidate.outpostBlockZ()
                + "；不会继续使用这个世界");
        return false;
    }

    private void recordFlashOutpostStatus(String roomId, boolean confirmed, boolean fallback,
                                          Location spawn, Location outpost, String reason) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        int distance = -1;
        if (spawn != null && outpost != null && spawn.getWorld() != null && outpost.getWorld() != null
                && spawn.getWorld().equals(outpost.getWorld())) {
            distance = (int) Math.round(Math.sqrt(horizontalDistanceSquared(spawn, outpost)));
        }
        String outpostText = outpost == null ? "" : formatBlockLocation(outpost);
        flashOutpostSeedStatuses.put(roomId, new FlashOutpostSeedStatus(confirmed, fallback, distance, outpostText,
                reason == null || reason.isBlank() ? "未知" : reason));
    }

    public FlashOutpostSeedStatus getFlashOutpostSeedStatus(String roomId) {
        return roomId == null ? null : flashOutpostSeedStatuses.get(roomId);
    }

    public record FlashOutpostSeedStatus(boolean confirmed, boolean fallback, int distance,
                                         String outpostLocation, String reason) {
    }

    private void logConfirmedOutpost(World world, long seed, int attempt, int verifiedWorlds,
                                     Location spawn, Location outpost, String mode) {
        plugin.getLogger().info("闪光外部筛种最终确认: " + world.getName()
                + " seed=" + seed
                + " attempt=" + attempt
                + " verifiedWorlds=" + verifiedWorlds
                + " mode=" + mode
                + " spawn=" + formatBlockLocation(spawn)
                + " outpost=" + formatBlockLocation(outpost)
                + " distance=" + Math.round(Math.sqrt(horizontalDistanceSquared(spawn, outpost))));
    }

    private boolean isFlashOutpostSearchTimedOut(long searchStartedAt) {
        return System.nanoTime() - searchStartedAt >= FLASH_OUTPOST_SEARCH_TIMEOUT_NANOS;
    }

    private World createRandomFallbackGameWorld(String roomId, String worldName, Set<Long> attemptedSeeds,
                                                String reason, int attempts, int verifiedWorlds) {
        long seed = nextUniqueGameSeed(attemptedSeeds);
        markGameSeedIssued(seed);
        prepareFreshWorldFolder(worldName);
        World world = createNormalGameWorldWithSeed(worldName, seed, true);
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            setupGameWorld(world);
            StructureSearchResult fallbackOutpost = findNearestPillagerOutpostForSpawnRescue(world);
            boolean rescued = tryAnchorFlashSpawnNearOutpost(world, fallbackOutpost)
                    && isOutpostWithinSpawnRadius(world.getSpawnLocation(), fallbackOutpost, null);
            gameWorlds.put(roomId, world);
            if (rescued) {
                recordFlashOutpostStatus(roomId, true, true, world.getSpawnLocation(), fallbackOutpost.getLocation(), "随机世界补救定位");
            } else {
                recordFlashOutpostStatus(roomId, false, true, world.getSpawnLocation(), null, reason);
            }
            plugin.getLogger().warning("闪光筛种降级随机世界: " + worldName
                    + " seed=" + seed
                    + " reason=" + reason
                    + " attempts=" + attempts
                    + " verifiedWorlds=" + verifiedWorlds
                    + " uniqueSeeds=" + (attemptedSeeds == null ? 0 : attemptedSeeds.size())
                    + (rescued
                    ? "，已补救校准出生点到哨塔附近 spawn=" + formatBlockLocation(world.getSpawnLocation())
                    + " outpost=" + formatBlockLocation(fallbackOutpost.getLocation())
                    + " distance=" + Math.round(Math.sqrt(horizontalDistanceSquared(world.getSpawnLocation(), fallbackOutpost.getLocation())))
                    : "，补救定位未找到可用哨塔"));
        } else {
            recordFlashOutpostStatus(roomId, false, true, null, null, reason);
            plugin.getLogger().severe("闪光筛种降级随机世界失败: " + worldName
                    + " seed=" + seed
                    + " reason=" + reason);
        }
        return world;
    }

    private long nextGameSeed() {
        return ThreadLocalRandom.current().nextLong()
                ^ Long.rotateLeft(ThreadLocalRandom.current().nextLong(), 21)
                ^ Long.reverse(System.nanoTime());
    }

    private long nextUniqueGameSeed(Set<Long> attemptedSeeds) {
        if (attemptedSeeds == null) {
            for (int retry = 0; retry < 64; retry++) {
                long seed = nextGameSeed();
                if (!isGameSeedIssued(seed)) {
                    return seed;
                }
            }
            long seed;
            do {
                seed = nextGameSeed();
            } while (isGameSeedIssued(seed));
            return seed;
        }
        for (int retry = 0; retry < 64; retry++) {
            long seed = nextGameSeed();
            if (!attemptedSeeds.contains(seed) && !isGameSeedIssued(seed)) {
                attemptedSeeds.add(seed);
                return seed;
            }
        }
        long seed;
        do {
            seed = nextGameSeed();
        } while (attemptedSeeds.contains(seed) || isGameSeedIssued(seed));
        attemptedSeeds.add(seed);
        return seed;
    }

    private boolean isGameSeedIssued(long seed) {
        synchronized (issuedGameSeeds) {
            return issuedGameSeeds.contains(seed);
        }
    }

    private void markGameSeedIssued(long seed) {
        synchronized (issuedGameSeeds) {
            issuedGameSeeds.add(seed);
        }
    }

    private FlashOutpostSeedCandidate findFlashOutpostSeedCandidate(long seed, int attempt, int outpostSalt, int villageSalt) {
        int radiusChunks = (FLASH_OUTPOST_CANDIDATE_SEARCH_RADIUS_BLOCKS + 15) / 16 + 1;
        int minRegionX = Math.floorDiv(-radiusChunks, FLASH_OUTPOST_SPACING);
        int maxRegionX = Math.floorDiv(radiusChunks, FLASH_OUTPOST_SPACING);
        int minRegionZ = Math.floorDiv(-radiusChunks, FLASH_OUTPOST_SPACING);
        int maxRegionZ = Math.floorDiv(radiusChunks, FLASH_OUTPOST_SPACING);
        FlashOutpostSeedCandidate best = null;
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                ChunkCandidate outpostChunk = getRandomSpreadChunkForRegion(
                        seed,
                        regionX,
                        regionZ,
                        FLASH_OUTPOST_SPACING,
                        FLASH_OUTPOST_SEPARATION,
                        outpostSalt
                );
                int blockX = chunkLocateBlock(outpostChunk.chunkX());
                int blockZ = chunkLocateBlock(outpostChunk.chunkZ());
                double distance = Math.sqrt((double) blockX * blockX + (double) blockZ * blockZ);
                if (distance > FLASH_OUTPOST_CANDIDATE_SEARCH_RADIUS_BLOCKS) {
                    continue;
                }
                if (!passesPillagerOutpostLegacyFrequency(seed, outpostChunk.chunkX(), outpostChunk.chunkZ())) {
                    continue;
                }
                if (isForbiddenByNearbyVillage(seed, outpostChunk.chunkX(), outpostChunk.chunkZ(), villageSalt)) {
                    continue;
                }
                FlashOutpostSeedCandidate candidate = new FlashOutpostSeedCandidate(
                        seed,
                        attempt,
                        outpostChunk.chunkX(),
                        outpostChunk.chunkZ(),
                        blockX,
                        blockZ,
                        distance
                );
                if (best == null || candidate.distanceFromOrigin() < best.distanceFromOrigin()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private ChunkCandidate getRandomSpreadChunkForRegion(long seed, int regionX, int regionZ,
                                                         int spacing, int separation, int salt) {
        Random random = new Random();
        setLargeFeatureWithSalt(random, seed, regionX, regionZ, salt);
        int maxOffset = spacing - separation;
        int offsetX = random.nextInt(maxOffset);
        int offsetZ = random.nextInt(maxOffset);
        return new ChunkCandidate(regionX * spacing + offsetX, regionZ * spacing + offsetZ);
    }

    private void setLargeFeatureWithSalt(Random random, long seed, int regionX, int regionZ, int salt) {
        long structureSeed = (long) regionX * LARGE_FEATURE_X_MULTIPLIER
                + (long) regionZ * LARGE_FEATURE_Z_MULTIPLIER
                + seed
                + salt;
        random.setSeed(structureSeed);
    }

    private boolean passesPillagerOutpostLegacyFrequency(long seed, int chunkX, int chunkZ) {
        int reducedChunkX = chunkX >> 4;
        int reducedChunkZ = chunkZ >> 4;
        Random random = new Random(((long) (reducedChunkX ^ (reducedChunkZ << 4))) ^ seed);
        random.nextInt();
        return random.nextInt(FLASH_OUTPOST_FREQUENCY_DIVISOR) == 0;
    }

    private boolean isForbiddenByNearbyVillage(long seed, int outpostChunkX, int outpostChunkZ, int villageSalt) {
        int minChunkX = outpostChunkX - FLASH_OUTPOST_VILLAGE_EXCLUSION_CHUNKS;
        int maxChunkX = outpostChunkX + FLASH_OUTPOST_VILLAGE_EXCLUSION_CHUNKS;
        int minChunkZ = outpostChunkZ - FLASH_OUTPOST_VILLAGE_EXCLUSION_CHUNKS;
        int maxChunkZ = outpostChunkZ + FLASH_OUTPOST_VILLAGE_EXCLUSION_CHUNKS;
        int minRegionX = Math.floorDiv(minChunkX, FLASH_VILLAGE_SPACING);
        int maxRegionX = Math.floorDiv(maxChunkX, FLASH_VILLAGE_SPACING);
        int minRegionZ = Math.floorDiv(minChunkZ, FLASH_VILLAGE_SPACING);
        int maxRegionZ = Math.floorDiv(maxChunkZ, FLASH_VILLAGE_SPACING);
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                ChunkCandidate villageChunk = getRandomSpreadChunkForRegion(
                        seed,
                        regionX,
                        regionZ,
                        FLASH_VILLAGE_SPACING,
                        FLASH_VILLAGE_SEPARATION,
                        villageSalt
                );
                if (villageChunk.chunkX() >= minChunkX
                        && villageChunk.chunkX() <= maxChunkX
                        && villageChunk.chunkZ() >= minChunkZ
                        && villageChunk.chunkZ() <= maxChunkZ) {
                    return true;
                }
            }
        }
        return false;
    }

    private int chunkLocateBlock(int chunk) {
        return chunk * 16;
    }

    private int readSpigotStructureSeed(String worldName, String key, int fallback) {
        File spigotFile = new File("spigot.yml");
        if (!spigotFile.isFile()) {
            return fallback;
        }
        try {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(spigotFile);
            Integer worldValue = readStructureSeedValue(configuration, "world-settings." + worldName + "." + key);
            if (worldValue != null) {
                return worldValue;
            }
            Integer defaultValue = readStructureSeedValue(configuration, "world-settings.default." + key);
            return defaultValue == null ? fallback : defaultValue;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("读取 spigot.yml 结构盐失败，使用默认值 " + key + "=" + fallback + ": " + throwable.getMessage());
            return fallback;
        }
    }

    private Integer readStructureSeedValue(YamlConfiguration configuration, String path) {
        Object value = configuration.get(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.equalsIgnoreCase("default") || trimmed.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private StructureSearchResult findPredictedPillagerOutpost(World world, FlashOutpostSeedCandidate candidate) {
        if (world == null || candidate == null) {
            return null;
        }
        try {
            Location predicted = new Location(
                    world,
                    candidate.outpostBlockX() + 8.0D,
                    Math.max(world.getMinHeight() + 64.0D, world.getSpawnLocation().getY()),
                    candidate.outpostBlockZ() + 8.0D
            );
            return world.locateNearestStructure(
                    predicted,
                    Structure.PILLAGER_OUTPOST,
                    FLASH_OUTPOST_VERIFY_RADIUS_CHUNKS,
                    true
            );
        } catch (Throwable throwable) {
            plugin.getLogger().warning("筛种验证预测掠夺者哨塔失败: " + world.getName() + " - " + throwable.getMessage());
            return null;
        }
    }

    private StructureSearchResult findPillagerOutpostNearSpawn(World world) {
        if (world == null) {
            return null;
        }
        try {
            Location spawn = world.getSpawnLocation();
            int radiusChunks = (FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS + 15) / 16 + 2;
            return world.locateNearestStructure(
                    spawn,
                    Structure.PILLAGER_OUTPOST,
                    radiusChunks,
                    true
            );
        } catch (Throwable throwable) {
            plugin.getLogger().warning("筛种验证出生点附近掠夺者哨塔失败: " + world.getName() + " - " + throwable.getMessage());
            return null;
        }
    }

    private StructureSearchResult findNearestPillagerOutpostForSpawnRescue(World world) {
        if (world == null) {
            return null;
        }
        try {
            return world.locateNearestStructure(
                    world.getSpawnLocation(),
                    Structure.PILLAGER_OUTPOST,
                    FLASH_OUTPOST_RESCUE_RADIUS_CHUNKS,
                    true
            );
        } catch (Throwable throwable) {
            plugin.getLogger().warning("筛种补救定位掠夺者哨塔失败: " + world.getName() + " - " + throwable.getMessage());
            return null;
        }
    }

    private boolean tryAnchorFlashSpawnNearOutpost(World world, StructureSearchResult result) {
        if (world == null || result == null || result.getLocation() == null) {
            return false;
        }
        Location outpost = result.getLocation();
        if (outpost.getWorld() == null || !outpost.getWorld().equals(world)) {
            return false;
        }
        Location safeSpawn = findSafeFlashOutpostSpawn(world, outpost);
        if (safeSpawn == null) {
            return false;
        }
        world.setSpawnLocation(safeSpawn);
        return true;
    }

    private Location findSafeFlashOutpostSpawn(World world, Location outpost) {
        if (world == null || outpost == null) {
            return null;
        }
        int[] radii = {96, 80, 112, 64, 128, 48, 144, 32, 160};
        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1},
                {1, 1},
                {-1, 1},
                {1, -1},
                {-1, -1},
                {2, 1},
                {-2, 1},
                {2, -1},
                {-2, -1},
                {1, 2},
                {-1, 2},
                {1, -2},
                {-1, -2}
        };
        for (int radius : radii) {
            for (int[] direction : directions) {
                double length = Math.sqrt((double) direction[0] * direction[0] + (double) direction[1] * direction[1]);
                int offsetX = (int) Math.round(radius * direction[0] / length);
                int offsetZ = (int) Math.round(radius * direction[1] / length);
                Location candidate = getSurfaceSpawnAt(world,
                        outpost.getBlockX() + offsetX,
                        outpost.getBlockZ() + offsetZ);
                if (isSafeOutpostSpawn(candidate) && horizontalDistanceSquared(candidate, outpost)
                        <= (double) FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS * FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS) {
                    return candidate;
                }
            }
        }
        Location fallback = getSurfaceSpawnAt(world, outpost.getBlockX(), outpost.getBlockZ());
        return isSafeOutpostSpawn(fallback) ? fallback : null;
    }

    private Location getSurfaceSpawnAt(World world, int x, int z) {
        if (world == null) {
            return null;
        }
        int y = Math.min(world.getMaxHeight() - 2, Math.max(world.getMinHeight() + 2, world.getHighestBlockYAt(x, z) + 1));
        return new Location(world, x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
    }

    private boolean isSafeOutpostSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        Block feet = world.getBlockAt(location);
        Block head = world.getBlockAt(location.clone().add(0.0D, 1.0D, 0.0D));
        Block floor = world.getBlockAt(location.clone().add(0.0D, -1.0D, 0.0D));
        return feet.getType().isAir()
                && head.getType().isAir()
                && floor.getType().isSolid()
                && floor.getType() != Material.CACTUS
                && floor.getType() != Material.MAGMA_BLOCK
                && floor.getType() != Material.CAMPFIRE
                && floor.getType() != Material.SOUL_CAMPFIRE;
    }

    private boolean isOutpostWithinSpawnRadius(Location spawn, StructureSearchResult result, FlashOutpostSeedCandidate candidate) {
        if (spawn == null || result == null || result.getLocation() == null) {
            return false;
        }
        Location outpost = result.getLocation();
        if (spawn.getWorld() == null || outpost.getWorld() == null || !spawn.getWorld().equals(outpost.getWorld())) {
            return false;
        }
        double max = FLASH_OUTPOST_REQUIRED_RADIUS_BLOCKS;
        if (horizontalDistanceSquared(spawn, outpost) > max * max) {
            return false;
        }
        if (candidate == null) {
            return true;
        }
        double verifyMax = (FLASH_OUTPOST_VERIFY_RADIUS_CHUNKS + 1.0D) * 16.0D;
        double dx = outpost.getX() - candidate.outpostBlockX();
        double dz = outpost.getZ() - candidate.outpostBlockZ();
        return dx * dx + dz * dz <= verifyMax * verifyMax;
    }

    private double horizontalDistanceSquared(Location first, Location second) {
        if (first == null || second == null) {
            return Double.MAX_VALUE;
        }
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private String formatBlockLocation(Location location) {
        if (location == null) {
            return "null";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private record ChunkCandidate(int chunkX, int chunkZ) {
    }

    private record FlashOutpostSeedCandidate(long seed, int attempt, int outpostChunkX, int outpostChunkZ,
                                             int outpostBlockX, int outpostBlockZ, double distanceFromOrigin) {
    }

    private record DeathSwapVillageSeedCandidate(long seed, int attempt, int villageChunkX, int villageChunkZ,
                                                 int villageBlockX, int villageBlockZ, double distanceFromOrigin) {
        private Location toLocation(World world) {
            if (world == null) {
                return null;
            }
            int centerX = villageBlockX + 8;
            int centerZ = villageBlockZ + 8;
            int y = Math.min(world.getMaxHeight() - 2,
                    Math.max(world.getMinHeight() + 2, world.getHighestBlockYAt(centerX, centerZ) + 1));
            return new Location(world, centerX + 0.5D, y, centerZ + 0.5D, 0.0F, 0.0F);
        }
    }

    public World createLuckyPillarsWorld(String roomId) {
        return createLuckyPillarsWorld(roomId, GameMode.LUCKY_PILLARS, null);
    }

    public World createLuckyPillarsWorld(String roomId, GameMode mode, MiniGameMapManager.MapDefinition definition) {
        GameMode safeMode = mode == null ? GameMode.LUCKY_PILLARS : mode;
        String worldName = GAME_PREFIX + roomId.toLowerCase() + "_" + safeMode.getId();

        prepareFreshWorldFolder(worldName);

        if (definition != null) {
            World sourceTemplateWorld = getOrCreateMiniGameTemplateWorld(definition, MiniGameMapManager.EditWorldKind.GAME);
            if (sourceTemplateWorld != null) {
                sourceTemplateWorld.save();
                File targetFolder = new File(Bukkit.getWorldContainer(), worldName);
                try {
                    copyWorldFolder(sourceTemplateWorld.getWorldFolder(), targetFolder);
                    File uidFile = new File(targetFolder, "uid.dat");
                    if (uidFile.exists() && !uidFile.delete()) {
                        plugin.getLogger().warning("无法删除幸运之柱运行世界 uid.dat: " + uidFile.getAbsolutePath());
                    }
                    File sessionFile = new File(targetFolder, "session.lock");
                    if (sessionFile.exists() && !sessionFile.delete()) {
                        plugin.getLogger().warning("无法删除幸运之柱运行世界 session.lock: " + sessionFile.getAbsolutePath());
                    }
                    plugin.getLogger().info("幸运之柱运行世界已从模板复制: " + sourceTemplateWorld.getName() + " -> " + worldName);
                } catch (IOException exception) {
                    plugin.getLogger().warning("复制幸运之柱模板世界失败，改用临时虚空世界生成: " + exception.getMessage());
                    deleteFolder(targetFolder);
                }

                if (targetFolder.exists()) {
                    WorldCreator creator = new WorldCreator(worldName);
                    creator.environment(World.Environment.NORMAL);
                    creator.type(WorldType.FLAT);
                    creator.generateStructures(false);
                    creator.generator(new VoidWorldGenerator());
                    creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);

                    World world = creator.createWorld();
                    if (world != null) {
                        world.setKeepSpawnInMemory(false);
                        setupGameWorld(world);
                        world.setTime(6000L);
                        world.setStorm(false);
                        world.setThundering(false);
                        clearVoidSpawnPlatform(world);
                        gameWorlds.put(roomId, world);
                        plugin.getLogger().info("幸运之柱模板运行世界已创建: " + worldName);
                        return world;
                    }
                }
            }
        }

        long seed = nextUniqueGameSeed(null);
        markGameSeedIssued(seed);

        WorldCreator creator = new WorldCreator(worldName);
        creator.seed(seed);
        creator.type(WorldType.FLAT);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());
        creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);

        World world = creator.createWorld();
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setSpawnLocation(new Location(world, 0.5D, 130.0D, 0.5D));
            setupGameWorld(world);
            world.setTime(6000L);
            world.setStorm(false);
            world.setThundering(false);
            clearVoidSpawnPlatform(world);
            gameWorlds.put(roomId, world);
            plugin.getLogger().info("幸运之柱虚空世界已创建: " + worldName);
        }
        return world;
    }

    private void clearVoidSpawnPlatform(World world) {
        if (world == null) {
            return;
        }
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                for (int y = 0; y <= 90; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    public World createEndFlashWorld(String roomId) {
        String worldName = GAME_PREFIX + roomId.toLowerCase() + "_end_flash";
        long seed = nextUniqueGameSeed(null);
        markGameSeedIssued(seed);

        prepareFreshWorldFolder(worldName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " THE_END --seed " + seed);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.seed(seed);
            creator.environment(World.Environment.THE_END);
            creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);
            world = creator.createWorld();
            if (world != null) {
                plugin.getLogger().info("终章闪光末地世界已创建(Bukkit降级): " + worldName);
            }
        } else {
            plugin.getLogger().info("终章闪光末地世界已创建(MV): " + worldName);
        }

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            setupDimensionWorld(world);
            gameWorlds.put(roomId, world);
        }
        return world;
    }

    private void setupGameWorld(World world) {
        applyCommonGameRules(world);
        applyDimensionStandbyRules(world);
    }

    public void enableGameWorldRules(World world) {
        if (world == null) {
            return;
        }
        applyDimensionActiveRules(world);
    }

    public void enableGameWorldRules(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        enableGameWorldRules(gameWorlds.get(roomId));
        enableGameWorldRules(netherWorlds.get(roomId));
        enableGameWorldRules(endWorlds.get(roomId));
    }

    public void createGameWorldDimensions(String roomId) {
        ensureNetherWorld(roomId);
        ensureEndWorld(roomId);
    }

    public World ensureNetherWorld(String roomId) {
        return ensureDimensionWorld(roomId, World.Environment.NETHER);
    }

    public World ensureEndWorld(String roomId) {
        return ensureDimensionWorld(roomId, World.Environment.THE_END);
    }

    private World ensureDimensionWorld(String roomId, World.Environment environment) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }

        Map<String, World> targetMap = environment == World.Environment.NETHER ? netherWorlds : endWorlds;
        World cached = targetMap.get(roomId);
        if (cached != null) {
            return cached;
        }

        World overworld = gameWorlds.get(roomId);
        if (overworld == null) {
            plugin.getLogger().warning("无法为房间 " + roomId + " 创建维度：主世界不存在");
            return null;
        }

        String baseName = GAME_PREFIX + roomId.toLowerCase();
        long seed = overworld.getSeed();
        String worldName = environment == World.Environment.NETHER ? baseName + "_nether" : baseName + "_the_end";
        String mvEnvironment = environment == World.Environment.NETHER ? "NETHER" : "THE_END";

        prepareFreshWorldFolder(worldName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " " + mvEnvironment + " --seed " + seed);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.seed(seed);
            creator.environment(environment);
            creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);
            world = creator.createWorld();
        }

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            setupDimensionWorld(world);
            targetMap.put(roomId, world);
            plugin.getLogger().info((environment == World.Environment.NETHER ? "下界维度已创建: " : "末地维度已创建: ") + worldName);
        }

        return world;
    }

    private void setupDimensionWorld(World world) {
        applyCommonGameRules(world);
        applyDimensionStandbyRules(world);
        applyDimensionActiveRules(world);
    }

    private void applyCommonGameRules(World world) {
        if (world == null) {
            return;
        }

        setRule(world, "keepInventory", false);
        setRule(world, "announceAdvancements", true);
        setRule(world, "doImmediateRespawn", true);
        setRule(world, "showDeathMessages", true);
        setLocatorBarEnabled(world, true);

        if (Bukkit.isPrimaryThread()) {
            world.setDifficulty(Difficulty.HARD);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> world.setDifficulty(Difficulty.HARD));
        }
    }

    private void applyDimensionStandbyRules(World world) {
        if (world == null) {
            return;
        }

        setRule(world, "doMobSpawning", false);
        switch (world.getEnvironment()) {
            case NORMAL -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
                world.setTime(6000);
                world.setStorm(false);
                world.setThundering(false);
            }
            case NETHER -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
                world.setStorm(false);
                world.setThundering(false);
            }
            case THE_END -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
                world.setTime(6000);
                world.setStorm(false);
                world.setThundering(false);
            }
            default -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
            }
        }
    }

    private void applyDimensionActiveRules(World world) {
        if (world == null) {
            return;
        }

        setRule(world, "doMobSpawning", true);
        switch (world.getEnvironment()) {
            case NORMAL -> {
                setRule(world, "doDaylightCycle", true);
                setRule(world, "doWeatherCycle", true);
            }
            case NETHER -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
                world.setStorm(false);
                world.setThundering(false);
            }
            case THE_END -> {
                setRule(world, "doDaylightCycle", false);
                setRule(world, "doWeatherCycle", false);
                world.setTime(6000);
                world.setStorm(false);
                world.setThundering(false);
            }
            default -> {
                setRule(world, "doDaylightCycle", true);
                setRule(world, "doWeatherCycle", true);
            }
        }
    }

    public void setLocatorBarEnabled(World world, boolean enabled) {
        if (world == null) {
            return;
        }
        world.setGameRule(GameRule.LOCATOR_BAR, enabled);
    }

    public void setLocatorBarEnabled(String roomId, boolean enabled) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        setLocatorBarEnabled(gameWorlds.get(roomId), enabled);
        setLocatorBarEnabled(netherWorlds.get(roomId), enabled);
        setLocatorBarEnabled(endWorlds.get(roomId), enabled);
    }

    public World getNetherWorld(String roomId) {
        return netherWorlds.get(roomId);
    }

    public World getEndWorld(String roomId) {
        return endWorlds.get(roomId);
    }

    public String getRoomIdByWorld(World world) {
        if (world == null) {
            return null;
        }

        String lobbyRoomId = getLobbyRoomIdByWorld(world);
        if (lobbyRoomId != null) {
            return lobbyRoomId;
        }
        for (Map.Entry<String, World> entry : gameWorlds.entrySet()) {
            if (world.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        for (Map.Entry<String, World> entry : netherWorlds.entrySet()) {
            if (world.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        for (Map.Entry<String, World> entry : endWorlds.entrySet()) {
            if (world.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void deleteLobbyWorld(String roomId) {
        World world = lobbyWorlds.remove(roomId);
        if (world != null) {
            normalizedLobbySpawns.remove(world.getName());
            deleteWorld(world);
        }
    }

    public void deleteGameWorlds(String roomId) {
        flashOutpostSeedStatuses.remove(roomId);
        deathSwapVillageSeedStatuses.remove(roomId);
        World overworld = gameWorlds.remove(roomId);
        if (overworld != null) {
            deleteWorld(overworld);
        }

        World nether = netherWorlds.remove(roomId);
        if (nether != null) {
            deleteWorld(nether);
        }

        World end = endWorlds.remove(roomId);
        if (end != null) {
            deleteWorld(end);
        }
    }

    public void deleteRoomWorldsLater(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        flashOutpostSeedStatuses.remove(roomId);
        deathSwapVillageSeedStatuses.remove(roomId);

        ArrayList<World> worlds = new ArrayList<>();
        World overworld = gameWorlds.remove(roomId);
        if (overworld != null) {
            worlds.add(overworld);
        }
        World nether = netherWorlds.remove(roomId);
        if (nether != null) {
            worlds.add(nether);
        }
        World end = endWorlds.remove(roomId);
        if (end != null) {
            worlds.add(end);
        }
        World lobby = lobbyWorlds.remove(roomId);
        if (lobby != null) {
            normalizedLobbySpawns.remove(lobby.getName());
            worlds.add(lobby);
        }

        long delay = 40L;
        for (World world : worlds) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> deleteWorld(world), delay);
            delay += 60L;
        }
    }

    public void remapGameWorld(String roomId, World newWorld) {
        if (roomId == null || roomId.isBlank() || newWorld == null) {
            return;
        }
        gameWorlds.put(roomId, newWorld);
    }

    public void remapFlashOutpostSeedStatus(String fromRoomId, String toRoomId) {
        if (fromRoomId == null || fromRoomId.isBlank() || toRoomId == null || toRoomId.isBlank()
                || fromRoomId.equals(toRoomId)) {
            return;
        }
        FlashOutpostSeedStatus status = flashOutpostSeedStatuses.remove(fromRoomId);
        if (status != null) {
            flashOutpostSeedStatuses.put(toRoomId, status);
        }
    }

    public void preloadChunks(World world, int centerChunkX, int centerChunkZ, int radius, Runnable callback) {
        if (world == null) {
            if (callback != null) {
                callback.run();
            }
            return;
        }

        if (radius <= 0) {
            if (callback != null) {
                callback.run();
            }
            return;
        }

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                queue.add(new int[]{chunkX, chunkZ});
            }
        }

        int parallelTasks = plugin.getConfigManager().getHunterGamePreloadParallelTasks();
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable[] pumpRef = new Runnable[1];

        pumpRef[0] = () -> {
            while (inFlight.get() < parallelTasks) {
                int[] chunk = queue.poll();
                if (chunk == null) {
                    if (inFlight.get() == 0 && finished.compareAndSet(false, true) && callback != null) {
                        Bukkit.getScheduler().runTask(plugin, callback);
                    }
                    return;
                }

                if (world.isChunkLoaded(chunk[0], chunk[1])) {
                    continue;
                }

                inFlight.incrementAndGet();
                world.getChunkAtAsync(chunk[0], chunk[1], true, true).whenComplete((loadedChunk, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("区块预加载失败: " + world.getName() + " [" + chunk[0] + "," + chunk[1] + "] " + throwable.getMessage());
                    }
                    inFlight.decrementAndGet();
                    Bukkit.getScheduler().runTask(plugin, pumpRef[0]);
                });
            }
        };

        Bukkit.getScheduler().runTask(plugin, pumpRef[0]);
    }

    public void deleteWorld(World world) {
        if (world == null) {
            return;
        }
        if (TEMPLATE_LOBBY_NAME.equals(world.getName())) {
            plugin.getLogger().warning("不能删除模板大厅世界！");
            return;
        }
        if (DEATH_SWAP_TEMPLATE_LOBBY_NAME.equals(world.getName())) {
            plugin.getLogger().warning("不能删除死亡互换等待大厅模板！");
            return;
        }
        if (plugin.getMiniGameMapManager() != null && plugin.getMiniGameMapManager().isTemplateWorldName(world.getName())) {
            plugin.getLogger().warning("不能删除小游戏模板世界: " + world.getName());
            return;
        }

        String worldName = world.getName();
        world.setAutoSave(false);

        World fallbackWorld = Bukkit.getWorlds().stream()
                .filter(other -> other != null && !worldName.equals(other.getName()))
                .findFirst()
                .orElse(null);
        Location fallbackSpawn = fallbackWorld == null ? null : fallbackWorld.getSpawnLocation();

        if (fallbackSpawn != null) {
            for (Player player : new ArrayList<>(world.getPlayers())) {
                player.teleport(fallbackSpawn);
            }
        }

        org.mvplugins.multiverse.core.world.WorldManager mvwm = getMVWorldManager();
        if (mvwm != null) {
            try {
                mvwm.getLoadedWorld(worldName).peek(loadedWorld ->
                        mvwm.unloadWorld(UnloadWorldOptions.world(loadedWorld)
                                .unloadBukkitWorld(true)
                                .saveBukkitWorld(false))
                );
                mvwm.getWorld(worldName).peek(mvwm::removeWorld);
                mvwm.saveWorldsConfig();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Multiverse 卸载世界失败，改用 Bukkit 卸载: " + worldName);
            }
        }

        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            Bukkit.unloadWorld(loaded, false);
        }

        deleteFolder(world.getWorldFolder());
        plugin.getLogger().info("世界已删除: " + worldName);
    }

    private void prepareFreshWorldFolder(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            deleteWorld(loaded);
            return;
        }

        File folder = new File(Bukkit.getWorldContainer(), worldName);
        if (folder.exists()) {
            deleteFolder(folder);
        }
    }

    private boolean deleteFolder(File file) {
        if (file == null || !file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteFolder(child)) {
                        return false;
                    }
                }
            }
        }

        return file.delete();
    }

    public void cleanupLeftoverWorlds() {
        Set<String> leftoverNames = new LinkedHashSet<>();

        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            if (name.startsWith(GAME_PREFIX) || name.startsWith(LOBBY_PREFIX)) {
                leftoverNames.add(name);
            }
        }

        File container = Bukkit.getWorldContainer();
        File[] children = container.listFiles();
        if (children != null) {
            for (File child : children) {
                String name = child.getName();
                if ((name.startsWith(GAME_PREFIX) || name.startsWith(LOBBY_PREFIX))
                        && !TEMPLATE_LOBBY_NAME.equals(name)
                        && !DEATH_SWAP_TEMPLATE_LOBBY_NAME.equals(name)) {
                    leftoverNames.add(name);
                }
            }
        }

        int cleaned = 0;
        for (String worldName : leftoverNames) {
            if (TEMPLATE_LOBBY_NAME.equals(worldName)) {
                continue;
            }
            if (DEATH_SWAP_TEMPLATE_LOBBY_NAME.equals(worldName)) {
                continue;
            }

            World loaded = Bukkit.getWorld(worldName);
            if (loaded != null) {
                deleteWorld(loaded);
                cleaned++;
                continue;
            }

            File folder = new File(Bukkit.getWorldContainer(), worldName);
            if (folder.exists() && deleteFolder(folder)) {
                plugin.getLogger().info("已清理残留世界: " + worldName);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            plugin.getLogger().info("共清理 " + cleaned + " 个残留游戏世界文件夹");
        }
    }

    public void cleanupAllWorlds() {
        Collection<World> managedWorlds = new LinkedHashSet<>();
        managedWorlds.addAll(lobbyWorlds.values());
        managedWorlds.addAll(gameWorlds.values());
        managedWorlds.addAll(netherWorlds.values());
        managedWorlds.addAll(endWorlds.values());

        for (World world : managedWorlds) {
            if (world != null) {
                deleteWorld(world);
            }
        }

        lobbyWorlds.clear();
        normalizedLobbySpawns.clear();
        gameWorlds.clear();
        netherWorlds.clear();
        endWorlds.clear();

        cleanupLeftoverWorlds();
    }
}
