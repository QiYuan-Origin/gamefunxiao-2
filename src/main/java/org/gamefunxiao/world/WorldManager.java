package org.gamefunxiao.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;

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
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"deprecation", "removal"})
public class WorldManager {

    private static final String TEMPLATE_LOBBY_NAME = "hugamelobby";
    private static final String LOBBY_PREFIX = "gamefun_lobby_";
    private static final String GAME_PREFIX = "gamefun_game_";
    private static final String END_FLASH_TUNING_WORLD_NAME = "gamefun_end_flash_debug_lobby";

    private final GameFunXiao plugin;
    private World templateLobbyWorld;
    private World endFlashTuningWorld;
    private final Map<String, World> lobbyWorlds = new HashMap<>();
    private final Map<String, World> gameWorlds = new HashMap<>();
    private final Map<String, World> netherWorlds = new HashMap<>();
    private final Map<String, World> endWorlds = new HashMap<>();

    public WorldManager(GameFunXiao plugin) {
        this.plugin = plugin;
        initTemplateLobbyWorld();
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
            lobbyWorlds.put(roomId, existingWorld);
            return existingWorld;
        }

        MiniGameMapManager.MapDefinition miniGameMap = null;
        BrickGuardMapManager.MapDefinition brickGuardMap = null;
        World sourceTemplateWorld = null;
        if (mode != null && plugin.getMiniGameMapManager() != null && mode.isMiniGameMapEditableMode()) {
            miniGameMap = plugin.getMiniGameMapManager().findUsableMap(mode, 1);
            if (miniGameMap != null) {
                sourceTemplateWorld = getOrCreateMiniGameTemplateWorld(miniGameMap, MiniGameMapManager.EditWorldKind.LOBBY);
            }
        }
        if (sourceTemplateWorld == null && mode != null && mode.isBrickGuard() && plugin.getBrickGuardMapManager() != null) {
            brickGuardMap = findBrickGuardMapForRoom(roomId);
            if (brickGuardMap != null) {
                sourceTemplateWorld = getOrCreateBrickGuardTemplateWorld(brickGuardMap, BrickGuardMapManager.EditWorldKind.LOBBY);
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
        if (brickGuardMap != null && sourceTemplateWorld.getName().equalsIgnoreCase(brickGuardMap.lobbyTemplateWorld())) {
            Location lobbySpawn = plugin.getBrickGuardMapManager().getLobbySpawn(brickGuardMap, lobbyWorld);
            if (lobbySpawn != null) {
                lobbyWorld.setSpawnLocation(lobbySpawn);
            }
        }
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

    public World getOrCreateBrickGuardTemplateWorld(BrickGuardMapManager.MapDefinition definition,
                                                    BrickGuardMapManager.EditWorldKind kind) {
        if (definition == null || kind == null) {
            return null;
        }
        String worldName = plugin.getBrickGuardMapManager().templateWorldName(definition, kind);
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            if (kind == BrickGuardMapManager.EditWorldKind.LOBBY) {
                applyTemplateLobbyRules(loaded);
            } else {
                setupGameWorld(loaded);
            }
            return loaded;
        }

        File templateFolder = new File(Bukkit.getWorldContainer(), worldName);
        boolean existingWorld = templateFolder.exists();
        if (!existingWorld && !plugin.getBrickGuardMapManager().shouldAutoCreateTemplate(definition)) {
            return null;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(kind == BrickGuardMapManager.EditWorldKind.NETHER_BRICK
                ? World.Environment.NETHER : World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());
        creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);

        World world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().severe("板砖守卫战模板世界加载失败: " + worldName);
            return null;
        }

        world.setKeepSpawnInMemory(false);
        if (kind == BrickGuardMapManager.EditWorldKind.LOBBY) {
            applyTemplateLobbyRules(world);
            world.setSpawnLocation(plugin.getBrickGuardMapManager().getLobbySpawn(definition, world));
        } else {
            setupGameWorld(world);
            if (kind == BrickGuardMapManager.EditWorldKind.BRICK) {
                world.setSpawnLocation(plugin.getBrickGuardMapManager().getBrickSpawn(definition, world));
            } else {
                world.setSpawnLocation(plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, world));
            }
        }
        if (!existingWorld) {
            setupBrickGuardTemplateWorld(world, definition, kind);
            plugin.getLogger().info("板砖守卫战模板世界已创建: " + worldName + " (" + kind.displayName() + ")");
        } else {
            plugin.getLogger().info("板砖守卫战模板世界已从已有文件加载: " + worldName);
        }
        return world;
    }

    private void setupBrickGuardTemplateWorld(World world, BrickGuardMapManager.MapDefinition definition,
                                              BrickGuardMapManager.EditWorldKind kind) {
        if (world == null || definition == null || kind == null) {
            return;
        }
        switch (kind) {
            case LOBBY -> {
                createMiniGameLobbyTemplate(world, null);
                Location spawn = plugin.getBrickGuardMapManager().getLobbySpawn(definition, world);
                if (spawn != null) {
                    world.setSpawnLocation(spawn);
                }
            }
            case BRICK -> {
                Location spawn = plugin.getBrickGuardMapManager().getBrickSpawn(definition, world);
                if (spawn != null) {
                    world.setSpawnLocation(spawn);
                }
                createBrickGuardPlatform(world, Material.BRICKS, Material.ORANGE_STAINED_GLASS, 0);
                Location core = plugin.getBrickGuardMapManager().getBrickCore(definition, world);
                if (core != null) {
                    core.getBlock().setType(Material.RED_GLAZED_TERRACOTTA, false);
                }
            }
            case NETHER_BRICK -> {
                Location spawn = plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, world);
                if (spawn != null) {
                    world.setSpawnLocation(spawn);
                }
                createBrickGuardPlatform(world, Material.NETHER_BRICKS, Material.RED_STAINED_GLASS, 0);
            }
        }
    }

    private void createBrickGuardPlatform(World world, Material floor, Material marker, int centerZ) {
        int baseY = 78;
        for (int x = -24; x <= 24; x++) {
            for (int z = centerZ - 24; z <= centerZ + 24; z++) {
                boolean edge = Math.abs(x) == 24 || Math.abs(z - centerZ) == 24;
                world.getBlockAt(x, baseY, z).setType(edge ? marker : floor, false);
            }
        }
        for (int y = baseY + 1; y <= baseY + 3; y++) {
            for (int x = -24; x <= 24; x++) {
                world.getBlockAt(x, y, centerZ - 24).setType(Material.BARRIER, false);
                world.getBlockAt(x, y, centerZ + 24).setType(Material.BARRIER, false);
            }
            for (int z = centerZ - 24; z <= centerZ + 24; z++) {
                world.getBlockAt(-24, y, z).setType(Material.BARRIER, false);
                world.getBlockAt(24, y, z).setType(Material.BARRIER, false);
            }
        }
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
        String name = world.getName();
        return TEMPLATE_LOBBY_NAME.equalsIgnoreCase(name)
                || END_FLASH_TUNING_WORLD_NAME.equalsIgnoreCase(name)
                || (plugin.getMiniGameMapManager() != null && plugin.getMiniGameMapManager().isTemplateWorldName(name))
                || (plugin.getBrickGuardMapManager() != null && plugin.getBrickGuardMapManager().isTemplateWorldName(name))
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
        return lobbyWorlds.get(roomId);
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
        String worldName = GAME_PREFIX + roomId.toLowerCase();
        long seed = System.currentTimeMillis();

        prepareFreshWorldFolder(worldName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv create " + worldName + " NORMAL --seed " + seed);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.seed(seed);
            creator.type(WorldType.NORMAL);
            creator.environment(World.Environment.NORMAL);
            creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);
            world = creator.createWorld();
            if (world != null) {
                plugin.getLogger().info("游戏世界已创建(Bukkit降级): " + worldName);
            }
        } else {
            plugin.getLogger().info("游戏世界已创建(MV): " + worldName);
        }

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            setupGameWorld(world);
            gameWorlds.put(roomId, world);
        }
        return world;
    }

    public BrickGuardMapManager.RuntimeWorlds createBrickGuardWorlds(String roomId,
                                                                     BrickGuardMapManager.MapDefinition definition) {
        if (roomId == null || roomId.isBlank()) {
            return new BrickGuardMapManager.RuntimeWorlds(null, null);
        }
        BrickGuardMapManager.MapDefinition selected = definition == null ? findBrickGuardMapForRoom(roomId) : definition;
        if (selected == null && plugin.getBrickGuardMapManager() != null) {
            selected = plugin.getBrickGuardMapManager().findUsableMap(2);
        }
        if (selected == null) {
            plugin.getLogger().warning("无法为房间 " + roomId + " 创建板砖守卫战世界：没有可用地图配置");
            return new BrickGuardMapManager.RuntimeWorlds(null, null);
        }

        World brickWorld = createBrickGuardWorld(roomId, selected, BrickGuardMapManager.EditWorldKind.BRICK);
        World netherBrickWorld = createBrickGuardWorld(roomId, selected, BrickGuardMapManager.EditWorldKind.NETHER_BRICK);
        return new BrickGuardMapManager.RuntimeWorlds(brickWorld, netherBrickWorld);
    }

    private BrickGuardMapManager.MapDefinition findBrickGuardMapForRoom(String roomId) {
        if (plugin.getBrickGuardMapManager() == null) {
            return null;
        }
        GameRoom room = plugin.getRoomManager() == null ? null : plugin.getRoomManager().getRoom(roomId);
        if (room != null && room.getGameMode().isBrickGuard()) {
            BrickGuardMapManager.MapDefinition roomMap = plugin.getBrickGuardMapManager().getMapDefinition(room.getBrickGuardMapId());
            if (roomMap != null) {
                return roomMap;
            }
            return plugin.getBrickGuardMapManager().findUsableMap(room.getPlayerCount());
        }
        return plugin.getBrickGuardMapManager().findUsableMap(2);
    }

    private World createBrickGuardWorld(String roomId, BrickGuardMapManager.MapDefinition definition,
                                        BrickGuardMapManager.EditWorldKind kind) {
        String suffix = kind == BrickGuardMapManager.EditWorldKind.NETHER_BRICK ? "_nether_brick" : "_brick";
        String worldName = GAME_PREFIX + roomId.toLowerCase() + suffix;

        prepareFreshWorldFolder(worldName);
        World sourceTemplateWorld = getOrCreateBrickGuardTemplateWorld(definition, kind);
        File targetFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (sourceTemplateWorld != null) {
            sourceTemplateWorld.save();
            try {
                copyWorldFolder(sourceTemplateWorld.getWorldFolder(), targetFolder);
                File uidFile = new File(targetFolder, "uid.dat");
                if (uidFile.exists() && !uidFile.delete()) {
                    plugin.getLogger().warning("无法删除板砖守卫战运行世界 uid.dat: " + uidFile.getAbsolutePath());
                }
                File sessionFile = new File(targetFolder, "session.lock");
                if (sessionFile.exists() && !sessionFile.delete()) {
                    plugin.getLogger().warning("无法删除板砖守卫战运行世界 session.lock: " + sessionFile.getAbsolutePath());
                }
                plugin.getLogger().info("板砖守卫战运行世界已从模板复制: " + sourceTemplateWorld.getName() + " -> " + worldName);
            } catch (IOException exception) {
                plugin.getLogger().warning("复制板砖守卫战模板世界失败，改用临时虚空世界生成: " + exception.getMessage());
                deleteFolder(targetFolder);
            }
        }

        World.Environment environment = kind == BrickGuardMapManager.EditWorldKind.NETHER_BRICK
                ? World.Environment.NETHER : World.Environment.NORMAL;
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(environment);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidWorldGenerator());
        creator.keepSpawnLoaded(net.kyori.adventure.util.TriState.FALSE);

        World world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().severe("板砖守卫战运行世界加载失败: " + worldName);
            return null;
        }

        world.setKeepSpawnInMemory(false);
        setupGameWorld(world);
        world.setStorm(false);
        world.setThundering(false);
        if (kind == BrickGuardMapManager.EditWorldKind.BRICK) {
            Location spawn = plugin.getBrickGuardMapManager().getBrickSpawn(definition, world);
            if (spawn != null) {
                world.setSpawnLocation(spawn);
            }
            Location core = plugin.getBrickGuardMapManager().getBrickCore(definition, world);
            if (core != null) {
                core.getBlock().setType(Material.RED_GLAZED_TERRACOTTA, false);
            }
            gameWorlds.put(roomId, world);
            plugin.getLogger().info("板砖世界已创建: " + worldName);
        } else {
            Location spawn = plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, world);
            if (spawn != null) {
                world.setSpawnLocation(spawn);
            }
            netherWorlds.put(roomId, world);
            plugin.getLogger().info("下界砖世界已创建: " + worldName);
        }
        return world;
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

        long seed = System.currentTimeMillis();

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
        long seed = System.currentTimeMillis();

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
        setRule(world, "announceAdvancements", false);
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

        for (Map.Entry<String, World> entry : lobbyWorlds.entrySet()) {
            if (world.equals(entry.getValue())) {
                return entry.getKey();
            }
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
            deleteWorld(world);
        }
    }

    public void deleteGameWorlds(String roomId) {
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
        if (plugin.getMiniGameMapManager() != null && plugin.getMiniGameMapManager().isTemplateWorldName(world.getName())) {
            plugin.getLogger().warning("不能删除小游戏模板世界: " + world.getName());
            return;
        }
        if (plugin.getBrickGuardMapManager() != null && plugin.getBrickGuardMapManager().isTemplateWorldName(world.getName())) {
            plugin.getLogger().warning("不能删除板砖守卫战模板世界: " + world.getName());
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
                if ((name.startsWith(GAME_PREFIX) || name.startsWith(LOBBY_PREFIX)) && !TEMPLATE_LOBBY_NAME.equals(name)) {
                    leftoverNames.add(name);
                }
            }
        }

        int cleaned = 0;
        for (String worldName : leftoverNames) {
            if (TEMPLATE_LOBBY_NAME.equals(worldName)) {
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
        gameWorlds.clear();
        netherWorlds.clear();
        endWorlds.clear();

        cleanupLeftoverWorlds();
    }
}
