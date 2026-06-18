package org.gamefunxiao.data;

import org.bukkit.Bukkit;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.HunterKillEffect;
import org.gamefunxiao.game.EndChapterKit;
import org.gamefunxiao.game.EndChapterKitRole;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.game.GameMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final GameFunXiao plugin;
    private final File dataFolder;
    private final File sessionFolder;
    private final boolean sharedStorageMode;
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerDataLastModified = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    public PlayerDataManager(GameFunXiao plugin) {
        this.plugin = plugin;
        boolean childServerEnabled = plugin.getConfigManager().getConfig().getBoolean("child_server.enabled", false);
        boolean crossServerEnabled = plugin.getConfigManager().getHunterGameBackendMode() == org.gamefunxiao.game.HunterGameBackendMode.CROSS_SERVER;
        boolean fixedCrossServerEnabled = plugin.getConfigManager().getConfig().getBoolean("hunter_game.cross_server.enabled", false);
        String sharedRootPath = plugin.getConfigManager().getSharedPlayerDataRootPath();
        this.sharedStorageMode = !sharedRootPath.isEmpty() && (childServerEnabled || crossServerEnabled || fixedCrossServerEnabled);
        if (sharedStorageMode) {
            File sharedRoot = new File(sharedRootPath);
            this.dataFolder = new File(sharedRoot, "playerdata");
            this.sessionFolder = new File(sharedRoot, "playerdata/sessions");
        } else {
            this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
            this.sessionFolder = new File(plugin.getDataFolder(), "playerdata/sessions");
        }
        if (!dataFolder.exists()) dataFolder.mkdirs();
        if (!sessionFolder.exists()) sessionFolder.mkdirs();
        plugin.getLogger().info("玩家数据存储路径: " + dataFolder.getAbsolutePath() + (sharedStorageMode ? " (跨服共享)" : " (本地单服)"));
        loadAllData();
    }

    private void loadAllData() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String uuidStr = file.getName().replace(".yml", "");
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerData data = loadPlayerData(uuid);
                    if (data != null) {
                        playerDataCache.put(uuid, data);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (!sharedStorageMode) {
            return playerDataCache.computeIfAbsent(uuid, this::loadOrCreatePlayerData);
        }
        return getSynchronizedPlayerData(uuid);
    }

    private PlayerData loadOrCreatePlayerData(UUID uuid) {
        PlayerData data = loadPlayerData(uuid);
        if (data == null) {
            data = new PlayerData(uuid);
        }
        return data;
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            playerDataLastModified.put(uuid, -1L);
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid);

        data.setPlayerName(config.getString("name", "Unknown"));
        data.setCoins(config.getInt("coins", 0));
        data.setOwnedVictoryEffects(new HashSet<>(config.getStringList("cosmetics.victory_effects.owned")));
        data.setOwnedLuckyPillarsVictoryEffects(new HashSet<>(config.getStringList("cosmetics.lucky_pillars_victory_effects.owned")));
        data.setSelectedHunterVictoryEffect(config.getString("cosmetics.victory_effects.selected.hunter_game", "fireworks"));
        data.setSelectedLuckyPillarsVictoryEffect(config.getString("cosmetics.victory_effects.selected.lucky_pillars", "fireworks"));
        data.setOwnedKillEffects(new HashSet<>(config.getStringList("cosmetics.kill_effects.owned")));
        data.setSelectedHunterKillEffect(config.getString("cosmetics.kill_effects.selected.hunter_game", "none"));
        data.setMessageFrequency(config.getString("settings.message_frequency", "chatty"));
        data.setBrickGuardAchievements(new HashSet<>(config.getStringList("achievements.brick_guard")));

        // 加载统计数据
        data.setPreyWinsTotal(config.getInt("stats.prey_wins.total", 0));
        data.setPreyWinsDay(config.getInt("stats.prey_wins.day", 0));
        data.setPreyWinsWeek(config.getInt("stats.prey_wins.week", 0));
        data.setPreyWinsMonth(config.getInt("stats.prey_wins.month", 0));
        data.setPreyWinsYear(config.getInt("stats.prey_wins.year", 0));
        loadModeIntStats(config, "stats.prey_wins_by_mode", data::setPreyWinsForMode);

        data.setHunterWinsTotal(config.getInt("stats.hunter_wins.total", 0));
        data.setHunterWinsDay(config.getInt("stats.hunter_wins.day", 0));
        data.setHunterWinsWeek(config.getInt("stats.hunter_wins.week", 0));
        data.setHunterWinsMonth(config.getInt("stats.hunter_wins.month", 0));
        data.setHunterWinsYear(config.getInt("stats.hunter_wins.year", 0));
        loadModeIntStats(config, "stats.hunter_wins_by_mode", data::setHunterWinsForMode);

        data.setPlayCountTotal(config.getInt("stats.play_count.total", 0));
        data.setPlayCountDay(config.getInt("stats.play_count.day", 0));
        data.setPlayCountWeek(config.getInt("stats.play_count.week", 0));
        data.setPlayCountMonth(config.getInt("stats.play_count.month", 0));
        data.setPlayCountYear(config.getInt("stats.play_count.year", 0));
        loadModeIntStats(config, "stats.play_count_by_mode", data::setPlayCountForMode);

        data.setFastestTimeTotal(config.getLong("stats.fastest_time.total", 0));
        data.setFastestTimeDay(config.getLong("stats.fastest_time.day", 0));
        data.setFastestTimeWeek(config.getLong("stats.fastest_time.week", 0));
        data.setFastestTimeMonth(config.getLong("stats.fastest_time.month", 0));
        data.setFastestTimeYear(config.getLong("stats.fastest_time.year", 0));
        ConfigurationSection fastestByModeSection = config.getConfigurationSection("stats.fastest_time_by_mode");
        if (fastestByModeSection != null) {
            for (String modeId : fastestByModeSection.getKeys(false)) {
                String basePath = "stats.fastest_time_by_mode." + modeId + ".";
                data.setFastestTimeForMode(modeId, "total", config.getLong(basePath + "total", 0L));
                data.setFastestTimeForMode(modeId, "day", config.getLong(basePath + "day", 0L));
                data.setFastestTimeForMode(modeId, "week", config.getLong(basePath + "week", 0L));
                data.setFastestTimeForMode(modeId, "month", config.getLong(basePath + "month", 0L));
                data.setFastestTimeForMode(modeId, "year", config.getLong(basePath + "year", 0L));
            }
        }

        data.setHunterKillsTotal(config.getInt("stats.hunter_kills.total", 0));
        data.setHunterKillsDay(config.getInt("stats.hunter_kills.day", 0));
        data.setHunterKillsWeek(config.getInt("stats.hunter_kills.week", 0));
        data.setHunterKillsMonth(config.getInt("stats.hunter_kills.month", 0));
        data.setHunterKillsYear(config.getInt("stats.hunter_kills.year", 0));
        loadModeIntStats(config, "stats.hunter_kills_by_mode", data::setHunterKillsForMode);

        data.setLastResetDay(config.getLong("last_reset.day", 0));
        data.setLastResetWeek(config.getLong("last_reset.week", 0));
        data.setLastResetMonth(config.getLong("last_reset.month", 0));
        data.setLastResetYear(config.getLong("last_reset.year", 0));

        // 积分
        data.setHunterPointsTotal(config.getInt("stats.hunter_points.total", 0));
        data.setHunterPointsDay(config.getInt("stats.hunter_points.day", 0));
        data.setHunterPointsWeek(config.getInt("stats.hunter_points.week", 0));
        data.setHunterPointsMonth(config.getInt("stats.hunter_points.month", 0));
        data.setHunterPointsYear(config.getInt("stats.hunter_points.year", 0));
        loadModeIntStats(config, "stats.hunter_points_by_mode", data::setHunterPointsForMode);

        data.setPreyPointsTotal(config.getInt("stats.prey_points.total", 0));
        data.setPreyPointsDay(config.getInt("stats.prey_points.day", 0));
        data.setPreyPointsWeek(config.getInt("stats.prey_points.week", 0));
        data.setPreyPointsMonth(config.getInt("stats.prey_points.month", 0));
        data.setPreyPointsYear(config.getInt("stats.prey_points.year", 0));
        loadModeIntStats(config, "stats.prey_points_by_mode", data::setPreyPointsForMode);

        data.setMiniGamePointsTotal(config.getInt("stats.minigame_points.total", 0));
        data.setMiniGamePointsDay(config.getInt("stats.minigame_points.day", 0));
        data.setMiniGamePointsWeek(config.getInt("stats.minigame_points.week", 0));
        data.setMiniGamePointsMonth(config.getInt("stats.minigame_points.month", 0));
        data.setMiniGamePointsYear(config.getInt("stats.minigame_points.year", 0));
        loadModeIntStats(config, "stats.minigame_points_by_mode", data::setMiniGamePointsForMode);

        data.setPreyKillHunterTotal(config.getInt("stats.prey_kill_hunter_total", 0));
        playerDataLastModified.put(uuid, file.lastModified());

        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;
        dirtyPlayers.add(uuid);

        File file = getPlayerDataFile(uuid);
        FileConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        config.set("name", data.getPlayerName());
        config.set("currency.type", "gamefun_minigame");
        config.set("currency.name", plugin.getConfigManager().getMiniGameCurrencyName());
        config.set("coins", data.getCoins());
        config.set("cosmetics.victory_effects.owned", new ArrayList<>(data.getOwnedVictoryEffects()));
        config.set("cosmetics.lucky_pillars_victory_effects.owned", new ArrayList<>(data.getOwnedLuckyPillarsVictoryEffects()));
        config.set("cosmetics.victory_effects.selected.hunter_game", data.getSelectedHunterVictoryEffect());
        config.set("cosmetics.victory_effects.selected.lucky_pillars", data.getSelectedLuckyPillarsVictoryEffect());
        config.set("cosmetics.kill_effects.owned", new ArrayList<>(data.getOwnedKillEffects()));
        config.set("cosmetics.kill_effects.selected.hunter_game", data.getSelectedHunterKillEffect());
        config.set("settings.message_frequency", data.getMessageFrequency());
        config.set("achievements.brick_guard", new ArrayList<>(data.getBrickGuardAchievements()));

        config.set("stats.prey_wins.total", data.getPreyWinsTotal());
        config.set("stats.prey_wins.day", data.getPreyWinsDay());
        config.set("stats.prey_wins.week", data.getPreyWinsWeek());
        config.set("stats.prey_wins.month", data.getPreyWinsMonth());
        config.set("stats.prey_wins.year", data.getPreyWinsYear());
        saveModeIntStats(config, "stats.prey_wins_by_mode", data.getPreyWinsByModeSnapshot());

        config.set("stats.hunter_wins.total", data.getHunterWinsTotal());
        config.set("stats.hunter_wins.day", data.getHunterWinsDay());
        config.set("stats.hunter_wins.week", data.getHunterWinsWeek());
        config.set("stats.hunter_wins.month", data.getHunterWinsMonth());
        config.set("stats.hunter_wins.year", data.getHunterWinsYear());
        saveModeIntStats(config, "stats.hunter_wins_by_mode", data.getHunterWinsByModeSnapshot());

        config.set("stats.play_count.total", data.getPlayCountTotal());
        config.set("stats.play_count.day", data.getPlayCountDay());
        config.set("stats.play_count.week", data.getPlayCountWeek());
        config.set("stats.play_count.month", data.getPlayCountMonth());
        config.set("stats.play_count.year", data.getPlayCountYear());
        saveModeIntStats(config, "stats.play_count_by_mode", data.getPlayCountByModeSnapshot());

        config.set("stats.fastest_time.total", data.getFastestTimeTotal());
        config.set("stats.fastest_time.day", data.getFastestTimeDay());
        config.set("stats.fastest_time.week", data.getFastestTimeWeek());
        config.set("stats.fastest_time.month", data.getFastestTimeMonth());
        config.set("stats.fastest_time.year", data.getFastestTimeYear());
        config.set("stats.fastest_time_by_mode", null);
        Map<String, Map<String, Long>> fastestByMode = data.getFastestTimeByModeSnapshot();
        for (Map.Entry<String, Map<String, Long>> entry : fastestByMode.entrySet()) {
            Map<String, Long> values = entry.getValue();
            boolean hasValue = values.values().stream().anyMatch(value -> value != null && value > 0L);
            if (!hasValue) {
                continue;
            }
            String basePath = "stats.fastest_time_by_mode." + entry.getKey() + ".";
            config.set(basePath + "total", values.getOrDefault("total", 0L));
            config.set(basePath + "day", values.getOrDefault("day", 0L));
            config.set(basePath + "week", values.getOrDefault("week", 0L));
            config.set(basePath + "month", values.getOrDefault("month", 0L));
            config.set(basePath + "year", values.getOrDefault("year", 0L));
        }

        config.set("stats.hunter_kills.total", data.getHunterKillsTotal());
        config.set("stats.hunter_kills.day", data.getHunterKillsDay());
        config.set("stats.hunter_kills.week", data.getHunterKillsWeek());
        config.set("stats.hunter_kills.month", data.getHunterKillsMonth());
        config.set("stats.hunter_kills.year", data.getHunterKillsYear());
        saveModeIntStats(config, "stats.hunter_kills_by_mode", data.getHunterKillsByModeSnapshot());

        config.set("last_reset.day", data.getLastResetDay());
        config.set("last_reset.week", data.getLastResetWeek());
        config.set("last_reset.month", data.getLastResetMonth());
        config.set("last_reset.year", data.getLastResetYear());

        // 积分
        config.set("stats.hunter_points.total", data.getHunterPointsTotal());
        config.set("stats.hunter_points.day", data.getHunterPointsDay());
        config.set("stats.hunter_points.week", data.getHunterPointsWeek());
        config.set("stats.hunter_points.month", data.getHunterPointsMonth());
        config.set("stats.hunter_points.year", data.getHunterPointsYear());
        saveModeIntStats(config, "stats.hunter_points_by_mode", data.getHunterPointsByModeSnapshot());

        config.set("stats.prey_points.total", data.getPreyPointsTotal());
        config.set("stats.prey_points.day", data.getPreyPointsDay());
        config.set("stats.prey_points.week", data.getPreyPointsWeek());
        config.set("stats.prey_points.month", data.getPreyPointsMonth());
        config.set("stats.prey_points.year", data.getPreyPointsYear());
        saveModeIntStats(config, "stats.prey_points_by_mode", data.getPreyPointsByModeSnapshot());

        config.set("stats.minigame_points.total", data.getMiniGamePointsTotal());
        config.set("stats.minigame_points.day", data.getMiniGamePointsDay());
        config.set("stats.minigame_points.week", data.getMiniGamePointsWeek());
        config.set("stats.minigame_points.month", data.getMiniGamePointsMonth());
        config.set("stats.minigame_points.year", data.getMiniGamePointsYear());
        saveModeIntStats(config, "stats.minigame_points_by_mode", data.getMiniGamePointsByModeSnapshot());

        config.set("stats.prey_kill_hunter_total", data.getPreyKillHunterTotal());

        try {
            config.save(file);
            dirtyPlayers.remove(uuid);
            playerDataLastModified.put(uuid, file.lastModified());
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家数据: " + uuid);
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (UUID uuid : new HashSet<>(dirtyPlayers)) {
            savePlayerData(uuid);
        }
    }

    public ItemStack[] getEndChapterKitLayout(UUID uuid, EndChapterKitRole role, EndChapterKit kit) {
        if (uuid == null || role == null || kit == null) {
            return null;
        }

        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = getEndChapterKitLayoutPath(role, kit);
        ItemStack[] contents = new ItemStack[36];
        boolean found = false;

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = config.getItemStack(path + "." + slot);
            if (item != null && item.getType() != Material.AIR) {
                contents[slot] = item.clone();
                found = true;
            }
        }

        return found ? contents : null;
    }

    public void saveEndChapterKitLayout(UUID uuid, EndChapterKitRole role, EndChapterKit kit, ItemStack[] contents) {
        if (uuid == null || role == null || kit == null || contents == null) {
            return;
        }

        File file = getPlayerDataFile(uuid);
        FileConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String path = getEndChapterKitLayoutPath(role, kit);
        config.set(path, null);

        for (int slot = 0; slot < 36 && slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && item.getType() != Material.AIR) {
                config.set(path + "." + slot, item.clone());
            }
        }

        saveYamlConfig(file, config, "无法保存末地篇 Kit 布局: " + uuid);
    }

    public void clearEndChapterKitLayout(UUID uuid, EndChapterKitRole role, EndChapterKit kit) {
        if (uuid == null || role == null || kit == null) {
            return;
        }

        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(getEndChapterKitLayoutPath(role, kit), null);
        saveYamlConfig(file, config, "无法清除末地篇 Kit 布局: " + uuid);
    }

    public EndFlashKitManager.KitLayout getEndFlashPersonalKitLayout(UUID uuid, EndFlashKitManager.Role role, String kitId) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank()) {
            return null;
        }
        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = getEndFlashPersonalKitPath(role, kitId);
        ItemStack[] storage = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        boolean found = false;

        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = config.getItemStack(path + ".storage." + slot);
            if (item != null && item.getType() != Material.AIR) {
                storage[slot] = item.clone();
                found = true;
            }
        }
        for (int slot = 0; slot < armor.length; slot++) {
            ItemStack item = config.getItemStack(path + ".armor." + slot);
            if (item != null && item.getType() != Material.AIR) {
                armor[slot] = item.clone();
                found = true;
            }
        }
        ItemStack offhand = config.getItemStack(path + ".offhand");
        if (offhand != null && offhand.getType() != Material.AIR) {
            found = true;
        }
        if (!found) {
            return null;
        }
        EndFlashKitManager.KitLayout layout = new EndFlashKitManager.KitLayout(storage, armor, syncFlashItemCopy(offhand));
        boolean changed = syncFlashItems(layout.storageContents()) | syncFlashItems(layout.armorContents());
        if (!sameItem(offhand, layout.offHandItem())) {
            changed = true;
        }
        if (changed) {
            saveEndFlashPersonalKitLayout(uuid, role, kitId, layout);
        }
        return layout;
    }

    public void saveEndFlashPersonalKitLayout(UUID uuid, EndFlashKitManager.Role role, String kitId, EndFlashKitManager.KitLayout layout) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank() || layout == null) {
            return;
        }
        File file = getPlayerDataFile(uuid);
        FileConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String path = getEndFlashPersonalKitPath(role, kitId);
        config.set(path, null);

        ItemStack[] storage = syncFlashArrayCopy(layout.storageContents(), 36);
        for (int slot = 0; slot < 36 && slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (item != null && item.getType() != Material.AIR) {
                config.set(path + ".storage." + slot, item.clone());
            }
        }
        ItemStack[] armor = syncFlashArrayCopy(layout.armorContents(), 4);
        for (int slot = 0; slot < 4 && slot < armor.length; slot++) {
            ItemStack item = armor[slot];
            if (item != null && item.getType() != Material.AIR) {
                config.set(path + ".armor." + slot, item.clone());
            }
        }
        ItemStack offhand = syncFlashItemCopy(layout.offHandItem());
        if (offhand != null && offhand.getType() != Material.AIR) {
            config.set(path + ".offhand", offhand.clone());
        }

        saveYamlConfig(file, config, "无法保存终章闪光个人 Kit 布局: " + uuid);
    }

    public void clearEndFlashPersonalKitLayout(UUID uuid, EndFlashKitManager.Role role, String kitId) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank()) {
            return;
        }
        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(getEndFlashPersonalKitPath(role, kitId), null);
        saveYamlConfig(file, config, "无法清除终章闪光个人 Kit 布局: " + uuid);
    }

    public ItemStack[] getEndFlashPersonalEnderChestLayout(UUID uuid, EndFlashKitManager.Role role, String kitId, int size) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank()) {
            return null;
        }
        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return null;
        }
        int normalizedSize = EndFlashKitManager.clampEnderChestSize(size);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = getEndFlashPersonalKitPath(role, kitId) + ".ender_chest";
        ItemStack[] contents = new ItemStack[normalizedSize];
        boolean found = false;
        for (int slot = 0; slot < normalizedSize; slot++) {
            ItemStack item = config.getItemStack(path + "." + slot);
            if (item != null && item.getType() != Material.AIR) {
                contents[slot] = item.clone();
                found = true;
            }
        }
        if (found && syncFlashItems(contents)) {
            saveEndFlashPersonalEnderChestLayout(uuid, role, kitId, contents, normalizedSize);
        }
        return found ? contents : null;
    }

    public void saveEndFlashPersonalEnderChestLayout(UUID uuid, EndFlashKitManager.Role role, String kitId, ItemStack[] contents, int size) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank() || contents == null) {
            return;
        }
        int normalizedSize = EndFlashKitManager.clampEnderChestSize(size);
        File file = getPlayerDataFile(uuid);
        FileConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String path = getEndFlashPersonalKitPath(role, kitId) + ".ender_chest";
        config.set(path, null);
        ItemStack[] syncedContents = syncFlashArrayCopy(contents, normalizedSize);
        for (int slot = 0; slot < normalizedSize && slot < syncedContents.length; slot++) {
            ItemStack item = syncedContents[slot];
            if (item != null && item.getType() != Material.AIR) {
                config.set(path + "." + slot, item.clone());
            }
        }
        saveYamlConfig(file, config, "无法保存终章闪光个人末影箱 Kit: " + uuid);
    }

    public void clearEndFlashPersonalEnderChestLayout(UUID uuid, EndFlashKitManager.Role role, String kitId) {
        if (uuid == null || role == null || kitId == null || kitId.isBlank()) {
            return;
        }
        File file = getPlayerDataFile(uuid);
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(getEndFlashPersonalKitPath(role, kitId) + ".ender_chest", null);
        saveYamlConfig(file, config, "无法清除终章闪光个人末影箱 Kit: " + uuid);
    }

    // 统计操作方法
    public PlayerData reloadPlayerData(UUID uuid) {
        return loadFreshPlayerData(uuid);
    }

    public int getCoins(UUID uuid) {
        if (uuid == null) {
            return 0;
        }
        return reloadPlayerData(uuid).getCoins();
    }

    public void setCoins(UUID uuid, int amount) {
        if (uuid == null) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.setCoins(Math.max(0, amount));
        savePlayerData(uuid);
    }

    public void addCoins(UUID uuid, int amount) {
        if (uuid == null || amount == 0) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.setCoins(Math.max(0, data.getCoins() + amount));
        savePlayerData(uuid);
    }

    public boolean hasCoins(UUID uuid, int amount) {
        return getCoins(uuid) >= Math.max(0, amount);
    }

    public boolean takeCoins(UUID uuid, int amount) {
        if (uuid == null) {
            return false;
        }
        int normalized = Math.max(0, amount);
        PlayerData data = reloadPlayerData(uuid);
        if (data.getCoins() < normalized) {
            return false;
        }
        data.setCoins(data.getCoins() - normalized);
        savePlayerData(uuid);
        return true;
    }

    public boolean hasVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null) {
            return false;
        }
        return reloadPlayerData(uuid).hasVictoryEffect(effectId);
    }

    public void unlockVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.unlockVictoryEffect(effectId);
        savePlayerData(uuid);
    }

    public String getSelectedHunterVictoryEffect(UUID uuid) {
        if (uuid == null) {
            return "fireworks";
        }
        PlayerData data = reloadPlayerData(uuid);
        String selected = data.getSelectedHunterVictoryEffect();
        return data.hasVictoryEffect(selected) ? selected : "fireworks";
    }

    public void setSelectedHunterVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        if (!data.hasVictoryEffect(effectId)) {
            return;
        }
        data.setSelectedHunterVictoryEffect(effectId);
        savePlayerData(uuid);
    }

    public String getSelectedLuckyPillarsVictoryEffect(UUID uuid) {
        if (uuid == null) {
            return "fireworks";
        }
        PlayerData data = reloadPlayerData(uuid);
        if (data == null) {
            return "fireworks";
        }
        String selected = data.getSelectedLuckyPillarsVictoryEffect();
        return data.hasLuckyPillarsVictoryEffect(selected) ? selected : "fireworks";
    }

    public void setSelectedLuckyPillarsVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        if (data == null) {
            return;
        }
        if (!data.hasLuckyPillarsVictoryEffect(effectId)) {
            return;
        }
        data.setSelectedLuckyPillarsVictoryEffect(effectId);
        savePlayerData(uuid);
    }

    public boolean hasLuckyPillarsVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null) {
            return false;
        }
        return reloadPlayerData(uuid).hasLuckyPillarsVictoryEffect(effectId);
    }

    public void unlockLuckyPillarsVictoryEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.unlockLuckyPillarsVictoryEffect(effectId);
        savePlayerData(uuid);
    }

    public boolean hasKillEffect(UUID uuid, String effectId) {
        if (uuid == null) {
            return false;
        }
        return reloadPlayerData(uuid).hasKillEffect(effectId);
    }

    public void unlockKillEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.unlockKillEffect(effectId);
        savePlayerData(uuid);
    }

    public String getSelectedHunterKillEffect(UUID uuid) {
        if (uuid == null) {
            return "none";
        }
        PlayerData data = reloadPlayerData(uuid);
        String selected = data.getSelectedHunterKillEffect();
        if (selected == null || selected.isBlank()) {
            return "none";
        }
        if ("none".equalsIgnoreCase(selected)) {
            return "none";
        }
        if (HunterKillEffect.byId(selected) == HunterKillEffect.NONE) {
            return "none";
        }
        return selected.toLowerCase(Locale.ROOT);
    }

    public void setSelectedHunterKillEffect(UUID uuid, String effectId) {
        if (uuid == null || effectId == null || effectId.isBlank()) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        HunterKillEffect effect = HunterKillEffect.byId(effectId);
        if (effect != HunterKillEffect.NONE && !data.hasKillEffect(effect.getId())) {
            data.unlockKillEffect(effect.getId());
        }
        data.setSelectedHunterKillEffect(effect.getId());
        savePlayerData(uuid);
    }

    public boolean isCompactMessages(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return reloadPlayerData(uuid).isCompactMessages();
    }

    public void setMessageFrequency(UUID uuid, String frequency) {
        if (uuid == null) {
            return;
        }
        PlayerData data = reloadPlayerData(uuid);
        data.setMessageFrequency(frequency);
        savePlayerData(uuid);
    }

    public Set<String> getBrickGuardAchievements(UUID uuid) {
        if (uuid == null) {
            return Collections.emptySet();
        }
        return reloadPlayerData(uuid).getBrickGuardAchievements();
    }

    public boolean unlockBrickGuardAchievement(UUID uuid, String achievementId) {
        if (uuid == null || achievementId == null || achievementId.isBlank()) {
            return false;
        }
        PlayerData data = reloadPlayerData(uuid);
        boolean unlocked = data.unlockBrickGuardAchievement(achievementId);
        if (unlocked) {
            savePlayerData(uuid);
        }
        return unlocked;
    }

    public void incrementPlayCount(UUID uuid) {
        incrementPlayCount(uuid, null);
    }

    public void incrementPlayCount(UUID uuid, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setPlayCountTotal(data.getPlayCountTotal() + 1);
        data.setPlayCountDay(data.getPlayCountDay() + 1);
        data.setPlayCountWeek(data.getPlayCountWeek() + 1);
        data.setPlayCountMonth(data.getPlayCountMonth() + 1);
        data.setPlayCountYear(data.getPlayCountYear() + 1);
        if (mode != null) {
            data.addPlayCountForMode(mode.getId(), 1);
        }
        savePlayerData(uuid);
    }

    public void incrementPreyWins(UUID uuid) {
        incrementPreyWins(uuid, null);
    }

    public void incrementPreyWins(UUID uuid, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setPreyWinsTotal(data.getPreyWinsTotal() + 1);
        data.setPreyWinsDay(data.getPreyWinsDay() + 1);
        data.setPreyWinsWeek(data.getPreyWinsWeek() + 1);
        data.setPreyWinsMonth(data.getPreyWinsMonth() + 1);
        data.setPreyWinsYear(data.getPreyWinsYear() + 1);
        if (mode != null) {
            data.addPreyWinsForMode(mode.getId(), 1);
        }
        savePlayerData(uuid);
    }

    public void incrementHunterWins(UUID uuid) {
        incrementHunterWins(uuid, null);
    }

    public void incrementHunterWins(UUID uuid, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setHunterWinsTotal(data.getHunterWinsTotal() + 1);
        data.setHunterWinsDay(data.getHunterWinsDay() + 1);
        data.setHunterWinsWeek(data.getHunterWinsWeek() + 1);
        data.setHunterWinsMonth(data.getHunterWinsMonth() + 1);
        data.setHunterWinsYear(data.getHunterWinsYear() + 1);
        if (mode != null) {
            data.addHunterWinsForMode(mode.getId(), 1);
        }
        savePlayerData(uuid);
    }

    public void incrementHunterKills(UUID uuid) {
        incrementHunterKills(uuid, null);
    }

    public void incrementHunterKills(UUID uuid, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);

        data.setHunterKillsTotal(data.getHunterKillsTotal() + 1);
        data.setHunterKillsDay(data.getHunterKillsDay() + 1);
        data.setHunterKillsWeek(data.getHunterKillsWeek() + 1);
        data.setHunterKillsMonth(data.getHunterKillsMonth() + 1);
        data.setHunterKillsYear(data.getHunterKillsYear() + 1);
        if (mode != null) {
            data.addHunterKillsForMode(mode.getId(), 1);
        }
        savePlayerData(uuid);
    }

    /**
     * 增加猎人积分（可为负数扣分，但最低为0）
     */
    public void addHunterPoints(UUID uuid, int amount) {
        addHunterPoints(uuid, amount, null);
    }

    public void addHunterPoints(UUID uuid, int amount, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setHunterPointsTotal(Math.max(0, data.getHunterPointsTotal() + amount));
        data.setHunterPointsDay(Math.max(0, data.getHunterPointsDay() + amount));
        data.setHunterPointsWeek(Math.max(0, data.getHunterPointsWeek() + amount));
        data.setHunterPointsMonth(Math.max(0, data.getHunterPointsMonth() + amount));
        data.setHunterPointsYear(Math.max(0, data.getHunterPointsYear() + amount));
        if (mode != null) {
            data.addHunterPointsForMode(mode.getId(), amount);
        }
        savePlayerData(uuid);
    }

    /**
     * 增加猎物积分（可为负数扣分，但最低为0）
     */
    public void addPreyPoints(UUID uuid, int amount) {
        addPreyPoints(uuid, amount, null);
    }

    public void addPreyPoints(UUID uuid, int amount, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setPreyPointsTotal(Math.max(0, data.getPreyPointsTotal() + amount));
        data.setPreyPointsDay(Math.max(0, data.getPreyPointsDay() + amount));
        data.setPreyPointsWeek(Math.max(0, data.getPreyPointsWeek() + amount));
        data.setPreyPointsMonth(Math.max(0, data.getPreyPointsMonth() + amount));
        data.setPreyPointsYear(Math.max(0, data.getPreyPointsYear() + amount));
        if (mode != null) {
            data.addPreyPointsForMode(mode.getId(), amount);
        }
        savePlayerData(uuid);
    }

    /**
     * 增加通用小游戏积分（幸运之柱等模式使用，可为负数扣分，但最低为0）
     */
    public void addMiniGamePoints(UUID uuid, int amount) {
        addMiniGamePoints(uuid, amount, null);
    }

    public void addMiniGamePoints(UUID uuid, int amount, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);
        data.setMiniGamePointsTotal(Math.max(0, data.getMiniGamePointsTotal() + amount));
        data.setMiniGamePointsDay(Math.max(0, data.getMiniGamePointsDay() + amount));
        data.setMiniGamePointsWeek(Math.max(0, data.getMiniGamePointsWeek() + amount));
        data.setMiniGamePointsMonth(Math.max(0, data.getMiniGamePointsMonth() + amount));
        data.setMiniGamePointsYear(Math.max(0, data.getMiniGamePointsYear() + amount));
        if (mode != null) {
            data.addMiniGamePointsForMode(mode.getId(), amount);
        }
        savePlayerData(uuid);
    }

    /**
     * 猎物击杀猎人累计计数，每累计10次给+1猎物积分
     * @return 是否触发了积分奖励（即此次击杀是第10/20/30...次）
     */
    public boolean incrementPreyKillHunter(UUID uuid) {
        return incrementPreyKillHunter(uuid, null);
    }

    public boolean incrementPreyKillHunter(UUID uuid, GameMode mode) {
        PlayerData data = getPlayerData(uuid);
        int newTotal = data.getPreyKillHunterTotal() + 1;
        data.setPreyKillHunterTotal(newTotal);
        if (newTotal % 10 == 0) {
            // 每10次+1猎物积分
            addPreyPoints(uuid, 1, mode);
            return true;
        }
        savePlayerData(uuid);
        return false;
    }

    public void updateFastestTime(UUID uuid, long time) {
        updateFastestTime(uuid, time, null);
    }

    public void updateFastestTime(UUID uuid, long time, GameMode mode) {
        if (uuid == null || time <= 0L) {
            return;
        }
        PlayerData data = getPlayerData(uuid);
        checkAndResetStats(data);

        if (data.getFastestTimeTotal() == 0 || time < data.getFastestTimeTotal()) {
            data.setFastestTimeTotal(time);
        }
        if (data.getFastestTimeDay() == 0 || time < data.getFastestTimeDay()) {
            data.setFastestTimeDay(time);
        }
        if (data.getFastestTimeWeek() == 0 || time < data.getFastestTimeWeek()) {
            data.setFastestTimeWeek(time);
        }
        if (data.getFastestTimeMonth() == 0 || time < data.getFastestTimeMonth()) {
            data.setFastestTimeMonth(time);
        }
        if (data.getFastestTimeYear() == 0 || time < data.getFastestTimeYear()) {
            data.setFastestTimeYear(time);
        }
        if (mode != null && mode != GameMode.CUSTOM && !mode.isLuckyPillars() && !mode.isIndependentMode()) {
            data.updateFastestTimeForMode(mode.getId(), time);
        }
        savePlayerData(uuid);
    }

    private void checkAndResetStats(PlayerData data) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();

        // 检查日重置
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis();

        if (data.getLastResetDay() < dayStart) {
            data.setPreyWinsDay(0);
            data.resetPreyWinsByModeDay();
            data.setHunterWinsDay(0);
            data.resetHunterWinsByModeDay();
            data.setPlayCountDay(0);
            data.resetPlayCountByModeDay();
            data.setFastestTimeDay(0);
            data.resetFastestTimeByModeDay();
            data.setHunterKillsDay(0);
            data.resetHunterKillsByModeDay();
            data.setHunterPointsDay(0);
            data.resetHunterPointsByModeDay();
            data.setPreyPointsDay(0);
            data.resetPreyPointsByModeDay();
            data.setMiniGamePointsDay(0);
            data.resetMiniGamePointsByModeDay();
            data.setLastResetDay(now);
        }

        // 检查周重置
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long weekStart = cal.getTimeInMillis();

        if (data.getLastResetWeek() < weekStart) {
            data.setPreyWinsWeek(0);
            data.resetPreyWinsByModeWeek();
            data.setHunterWinsWeek(0);
            data.resetHunterWinsByModeWeek();
            data.setPlayCountWeek(0);
            data.resetPlayCountByModeWeek();
            data.setFastestTimeWeek(0);
            data.resetFastestTimeByModeWeek();
            data.setHunterKillsWeek(0);
            data.resetHunterKillsByModeWeek();
            data.setHunterPointsWeek(0);
            data.resetHunterPointsByModeWeek();
            data.setPreyPointsWeek(0);
            data.resetPreyPointsByModeWeek();
            data.setMiniGamePointsWeek(0);
            data.resetMiniGamePointsByModeWeek();
            data.setLastResetWeek(now);
        }

        // 检查月重置
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        if (data.getLastResetMonth() < monthStart) {
            data.setPreyWinsMonth(0);
            data.resetPreyWinsByModeMonth();
            data.setHunterWinsMonth(0);
            data.resetHunterWinsByModeMonth();
            data.setPlayCountMonth(0);
            data.resetPlayCountByModeMonth();
            data.setFastestTimeMonth(0);
            data.resetFastestTimeByModeMonth();
            data.setHunterKillsMonth(0);
            data.resetHunterKillsByModeMonth();
            data.setHunterPointsMonth(0);
            data.resetHunterPointsByModeMonth();
            data.setPreyPointsMonth(0);
            data.resetPreyPointsByModeMonth();
            data.setMiniGamePointsMonth(0);
            data.resetMiniGamePointsByModeMonth();
            data.setLastResetMonth(now);
        }

        // 检查年重置
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long yearStart = cal.getTimeInMillis();

        if (data.getLastResetYear() < yearStart) {
            data.setPreyWinsYear(0);
            data.resetPreyWinsByModeYear();
            data.setHunterWinsYear(0);
            data.resetHunterWinsByModeYear();
            data.setPlayCountYear(0);
            data.resetPlayCountByModeYear();
            data.setFastestTimeYear(0);
            data.resetFastestTimeByModeYear();
            data.setHunterKillsYear(0);
            data.resetHunterKillsByModeYear();
            data.setHunterPointsYear(0);
            data.resetHunterPointsByModeYear();
            data.setPreyPointsYear(0);
            data.resetPreyPointsByModeYear();
            data.setMiniGamePointsYear(0);
            data.resetMiniGamePointsByModeYear();
            data.setLastResetYear(now);
        }
    }

    private File getPlayerDataFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    @FunctionalInterface
    private interface ModeStatSetter {
        void accept(String modeId, String timeRange, int value);
    }

    private void loadModeIntStats(FileConfiguration config, String path, ModeStatSetter setter) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String modeId : section.getKeys(false)) {
            String basePath = path + "." + modeId + ".";
            setter.accept(modeId, "total", config.getInt(basePath + "total", 0));
            setter.accept(modeId, "day", config.getInt(basePath + "day", 0));
            setter.accept(modeId, "week", config.getInt(basePath + "week", 0));
            setter.accept(modeId, "month", config.getInt(basePath + "month", 0));
            setter.accept(modeId, "year", config.getInt(basePath + "year", 0));
        }
    }

    private void saveModeIntStats(FileConfiguration config, String path, Map<String, Map<String, Integer>> snapshot) {
        config.set(path, null);
        for (Map.Entry<String, Map<String, Integer>> entry : snapshot.entrySet()) {
            Map<String, Integer> values = entry.getValue();
            boolean hasValue = values.values().stream().anyMatch(value -> value != null && value > 0);
            if (!hasValue) {
                continue;
            }
            String basePath = path + "." + entry.getKey() + ".";
            config.set(basePath + "total", values.getOrDefault("total", 0));
            config.set(basePath + "day", values.getOrDefault("day", 0));
            config.set(basePath + "week", values.getOrDefault("week", 0));
            config.set(basePath + "month", values.getOrDefault("month", 0));
            config.set(basePath + "year", values.getOrDefault("year", 0));
        }
    }

    private PlayerData getSynchronizedPlayerData(UUID uuid) {
        File file = getPlayerDataFile(uuid);
        long diskLastModified = file.exists() ? file.lastModified() : -1L;
        PlayerData cached = playerDataCache.get(uuid);
        Long cachedLastModified = playerDataLastModified.get(uuid);

        boolean shouldReload = cached == null
                || cachedLastModified == null
                || diskLastModified > cachedLastModified
                || (!file.exists() && cachedLastModified != -1L);

        if (!shouldReload) {
            return cached;
        }

        return loadFreshPlayerData(uuid);
    }

    private PlayerData loadFreshPlayerData(UUID uuid) {
        PlayerData data = loadPlayerData(uuid);
        if (data == null) {
            data = new PlayerData(uuid);
            playerDataLastModified.put(uuid, -1L);
        }
        playerDataCache.put(uuid, data);
        return data;
    }

    private String getEndChapterKitLayoutPath(EndChapterKitRole role, EndChapterKit kit) {
        return "kit_layouts.end_chapter." + role.getId() + "." + kit.getId();
    }

    private ItemStack syncFlashItemCopy(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemStack copy = item.clone();
        if (plugin.getFlashModeManager() == null) {
            return copy;
        }
        return plugin.getFlashModeManager().syncFlashItemLore(copy);
    }

    private ItemStack[] syncFlashArrayCopy(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source != null) {
            for (int i = 0; i < result.length && i < source.length; i++) {
                result[i] = syncFlashItemCopy(source[i]);
            }
        }
        return result;
    }

    private boolean syncFlashItems(ItemStack[] contents) {
        if (contents == null || plugin.getFlashModeManager() == null) {
            return false;
        }
        return plugin.getFlashModeManager().syncFlashItemLore(contents);
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || first.getType() == Material.AIR) {
            return second == null || second.getType() == Material.AIR;
        }
        return first.equals(second);
    }

    private String getEndFlashPersonalKitPath(EndFlashKitManager.Role role, String kitId) {
        return "kit_layouts.end_flash." + role.id() + "." + kitId.toLowerCase(Locale.ROOT);
    }

    private void saveYamlConfig(File file, FileConfiguration config, String errorMessage) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(errorMessage);
            e.printStackTrace();
        }
    }

    public Collection<PlayerData> getAllPlayerData() {
        if (sharedStorageMode) {
            refreshAllPlayerDataFromDisk();
        }
        return playerDataCache.values();
    }

    private void refreshAllPlayerDataFromDisk() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        Set<UUID> diskPlayers = new HashSet<>();
        for (File file : files) {
            String uuidStr = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(uuidStr);
                diskPlayers.add(uuid);
                Long cachedLastModified = playerDataLastModified.get(uuid);
                long diskLastModified = file.lastModified();
                if (cachedLastModified == null || diskLastModified > cachedLastModified) {
                    loadFreshPlayerData(uuid);
                } else if (!playerDataCache.containsKey(uuid)) {
                    loadFreshPlayerData(uuid);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        playerDataCache.keySet().removeIf(uuid -> !diskPlayers.contains(uuid) && !dirtyPlayers.contains(uuid));
        playerDataLastModified.keySet().removeIf(uuid -> !diskPlayers.contains(uuid) && !dirtyPlayers.contains(uuid));
    }

    // ===== 游戏会话持久化（防止强制关服丢失数据）=====

    /**
     * 保存玩家进入游戏前的状态到文件（加入房间时调用）
     */
    public void savePlayerSession(Player player, Location previousLocation, ItemStack[] inventory) {
        File file = new File(sessionFolder, player.getUniqueId().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // 保存位置
        if (previousLocation != null && previousLocation.getWorld() != null) {
            config.set("location.world", previousLocation.getWorld().getName());
            config.set("location.x", previousLocation.getX());
            config.set("location.y", previousLocation.getY());
            config.set("location.z", previousLocation.getZ());
            config.set("location.yaw", (double) previousLocation.getYaw());
            config.set("location.pitch", (double) previousLocation.getPitch());
        }

        // 保存背包
        if (inventory != null) {
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null) {
                    config.set("inventory." + i, inventory[i]);
                }
            }
        }

        config.set("player_name", player.getName());
        config.set("saved_at", System.currentTimeMillis());
        config.set("session_type", "local_room");
        writeAdvancementSnapshot(config, "advancements", snapshotPlayerAdvancements(player));
        writeRecipeSnapshot(config, "recipes", snapshotPlayerRecipes(player));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家会话: " + player.getName());
        }
    }

    public void saveTransferSession(Player player, Location previousLocation, ItemStack[] inventory, String roomId, String targetServer) {
        saveTransferSession(player, previousLocation, inventory, roomId, targetServer, false);
    }

    public void saveTransferSession(Player player, Location previousLocation, ItemStack[] inventory,
                                    String roomId, String targetServer, boolean spectateMode) {
        File file = new File(sessionFolder, player.getUniqueId().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        if (previousLocation != null && previousLocation.getWorld() != null) {
            config.set("location.world", previousLocation.getWorld().getName());
            config.set("location.x", previousLocation.getX());
            config.set("location.y", previousLocation.getY());
            config.set("location.z", previousLocation.getZ());
            config.set("location.yaw", (double) previousLocation.getYaw());
            config.set("location.pitch", (double) previousLocation.getPitch());
        }

        if (inventory != null) {
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null) {
                    config.set("inventory." + i, inventory[i]);
                }
            }
        }

        config.set("player_name", player.getName());
        config.set("saved_at", System.currentTimeMillis());
        config.set("session_type", "proxy_transfer");
        config.set("transfer.room_id", roomId);
        config.set("transfer.target_server", targetServer);
        config.set("transfer.spectate", spectateMode);
        writeAdvancementSnapshot(config, "advancements", snapshotPlayerAdvancements(player));
        writeRecipeSnapshot(config, "recipes", snapshotPlayerRecipes(player));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存跨服会话: " + player.getName());
        }
    }

    /**
     * 清除玩家会话文件（离开房间/游戏正常结束时调用）
     */
    public void clearPlayerSession(UUID uuid) {
        File file = new File(sessionFolder, uuid.toString() + ".yml");
        if (file.exists()) file.delete();
    }

    /**
     * 启动时恢复所有未正常退出的玩家（强制关服后重启调用）
     */
    public void recoverCrashedSessions() {
        File[] files = sessionFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return;

        plugin.getLogger().info("检测到 " + files.length + " 个未正常退出的游戏会话，正在恢复...");

        for (File file : files) {
            String uuidStr = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(uuidStr);
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    // 玩家不在线，等玩家上线时恢复（标记待恢复）
                    pendingRecovery.add(uuid);
                    continue;
                }

                recoverPlayer(player, config, file);
            } catch (IllegalArgumentException ignored) {
                file.delete();
            }
        }
    }

    private final Set<UUID> pendingRecovery = new HashSet<>();

    /**
     * 玩家上线时检查是否有待恢复的会话
     */
    public void checkPendingRecovery(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pendingRecovery.contains(uuid)) return;

        File file = new File(sessionFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            pendingRecovery.remove(uuid);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        // 延迟1tick确保玩家完全加载
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            recoverPlayer(player, config, file);
            pendingRecovery.remove(uuid);
        }, 1L);
    }

    public boolean restoreTransferSessionIfPresent(Player player) {
        File file = new File(sessionFolder, player.getUniqueId().toString() + ".yml");
        if (!file.exists()) {
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!"proxy_transfer".equalsIgnoreCase(config.getString("session_type", ""))) {
            return false;
        }

        String targetServer = config.getString("transfer.target_server", "");
        String currentServer = resolveCurrentServerName();
        // 玩家刚被送到小游戏服时，不能恢复原位置/背包；否则会把玩家丢到 gameing 主世界出生点。
        if (!targetServer.isBlank() && (sameServerName(targetServer, currentServer)
                || sameServerName(targetServer, resolveRuntimeDirectoryServerName()))) {
            return false;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> recoverPlayer(player, config, file), 1L);
        return true;
    }

    public String getProxyTransferTargetServer(UUID uuid) {
        File file = new File(sessionFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!"proxy_transfer".equalsIgnoreCase(config.getString("session_type", ""))) {
            return null;
        }
        return config.getString("transfer.target_server");
    }

    private String resolveCurrentServerName() {
        String serverName = null;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                serverName = PlaceholderAPI.setPlaceholders(null, "%qichengmorebungeeapi_server%");
            } catch (RuntimeException ignored) {
                serverName = null;
            }
        }
        if (serverName != null && !serverName.isBlank() && !serverName.contains("%qichengmorebungeeapi_server%")) {
            return serverName.trim();
        }
        try {
            Path path = Path.of(System.getProperty("user.dir", "")).getFileName();
            if (path != null && !path.toString().isBlank()) {
                return path.toString().trim();
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private boolean sameServerName(String expected, String actual) {
        String expectedKey = normalizeComparableServerName(expected);
        String actualKey = normalizeComparableServerName(actual);
        return !expectedKey.isBlank() && expectedKey.equals(actualKey);
    }

    private String normalizeComparableServerName(String serverName) {
        if (serverName == null) {
            return "";
        }
        String stripped = org.bukkit.ChatColor.stripColor(serverName.replace('&', '§'));
        if (stripped == null) {
            stripped = serverName;
        }
        return stripped.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveRuntimeDirectoryServerName() {
        try {
            Path path = Path.of(System.getProperty("user.dir", "")).getFileName();
            if (path != null && !path.toString().isBlank()) {
                return path.toString().trim();
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    public boolean restoreLocalRoomSessionIfPresent(Player player) {
        File file = new File(sessionFolder, player.getUniqueId().toString() + ".yml");
        if (!file.exists()) {
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!"local_room".equalsIgnoreCase(config.getString("session_type", ""))) {
            return false;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> recoverPlayer(player, config, file), 1L);
        return true;
    }

    public String getProxyTransferRoomId(UUID uuid) {
        File file = new File(sessionFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!"proxy_transfer".equalsIgnoreCase(config.getString("session_type", ""))) {
            return null;
        }
        return config.getString("transfer.room_id");
    }

    public boolean isProxyTransferSpectate(UUID uuid) {
        File file = new File(sessionFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return false;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!"proxy_transfer".equalsIgnoreCase(config.getString("session_type", ""))) {
            return false;
        }
        return config.getBoolean("transfer.spectate", false);
    }

    private void recoverPlayer(Player player, FileConfiguration config, File sessionFile) {
        if (plugin.getPlayerListener() != null) {
            plugin.getPlayerListener().suppressAdvancementMessages(player.getUniqueId(), 240L);
        }

        // 恢复背包
        player.getInventory().clear();
        if (config.isConfigurationSection("inventory")) {
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack("inventory." + key);
                    if (item != null) {
                        player.getInventory().setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 恢复位置
        String worldName = config.getString("location.world");
        boolean locationRestored = false;
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("location.x");
                double y = config.getDouble("location.y");
                double z = config.getDouble("location.z");
                float yaw = (float) config.getDouble("location.yaw");
                float pitch = (float) config.getDouble("location.pitch");
                Location loc = new Location(world, x, y, z, yaw, pitch);
                player.teleport(loc);
                locationRestored = true;
            } else {
                plugin.getLogger().warning("无法恢复玩家 " + player.getName() + " 的位置：世界 " + worldName + " 不存在");
            }
        }

        // 如果位置恢复失败，传送到主世界出生点
        if (!locationRestored) {
            World mainWorld = Bukkit.getWorlds().get(0);
            if (mainWorld != null) {
                player.teleport(mainWorld.getSpawnLocation());
                plugin.getLogger().warning("已将玩家 " + player.getName() + " 传送到主世界出生点");
            }
        }

        // 恢复游戏模式
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);

        restoreAdvancementSnapshot(player, readAdvancementSnapshot(config, "advancements"));
        restoreRecipeSnapshot(player, readRecipeSnapshot(config, "recipes"));

        player.sendMessage("§x§5§5§F§F§A§A✓ §a检测到上次游戏异常退出，已自动恢复你的状态！");
        plugin.getLogger().info("已恢复玩家 " + player.getName() + " 的游戏前状态");

        // 删除会话文件
        sessionFile.delete();
    }

    private Map<String, Set<String>> snapshotPlayerAdvancements(Player player) {
        Map<String, Set<String>> snapshot = new HashMap<>();
        if (player == null) {
            return snapshot;
        }
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (!progress.getAwardedCriteria().isEmpty()) {
                snapshot.put(advancement.getKey().toString(), new HashSet<>(progress.getAwardedCriteria()));
            }
        }
        return snapshot;
    }

    private Set<NamespacedKey> snapshotPlayerRecipes(Player player) {
        if (player == null) {
            return new HashSet<>();
        }
        return new HashSet<>(player.getDiscoveredRecipes());
    }

    private void writeAdvancementSnapshot(FileConfiguration config, String path, Map<String, Set<String>> snapshot) {
        config.set(path, null);
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
            config.set(path + "." + entry.getKey().replace(".", "__dot__"), new ArrayList<>(entry.getValue()));
        }
    }

    private Map<String, Set<String>> readAdvancementSnapshot(FileConfiguration config, String path) {
        Map<String, Set<String>> snapshot = new HashMap<>();
        if (config == null || !config.isConfigurationSection(path)) {
            return snapshot;
        }
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return snapshot;
        }
        for (String key : section.getKeys(false)) {
            snapshot.put(key.replace("__dot__", "."), new HashSet<>(section.getStringList(key)));
        }
        return snapshot;
    }

    private void restoreAdvancementSnapshot(Player player, Map<String, Set<String>> snapshot) {
        if (player == null) {
            return;
        }
        Map<String, Set<String>> expectedSnapshot = snapshot == null ? Collections.emptyMap() : snapshot;
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            Set<String> expectedCriteria = expectedSnapshot.getOrDefault(advancement.getKey().toString(), Collections.emptySet());

            for (String awarded : new HashSet<>(progress.getAwardedCriteria())) {
                if (!expectedCriteria.contains(awarded)) {
                    try {
                        progress.revokeCriteria(awarded);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            for (String criterion : expectedCriteria) {
                if (!progress.getAwardedCriteria().contains(criterion)) {
                    try {
                        progress.awardCriteria(criterion);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    private void writeRecipeSnapshot(FileConfiguration config, String path, Set<NamespacedKey> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            config.set(path, new ArrayList<>());
            return;
        }
        List<String> values = new ArrayList<>();
        for (NamespacedKey key : recipes) {
            if (key != null) {
                values.add(key.toString());
            }
        }
        config.set(path, values);
    }

    private Set<NamespacedKey> readRecipeSnapshot(FileConfiguration config, String path) {
        Set<NamespacedKey> recipes = new HashSet<>();
        if (config == null) {
            return recipes;
        }
        for (String raw : config.getStringList(path)) {
            try {
                NamespacedKey key = NamespacedKey.fromString(raw);
                if (key != null) {
                    recipes.add(key);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return recipes;
    }

    private void restoreRecipeSnapshot(Player player, Set<NamespacedKey> recipes) {
        if (player == null || recipes == null || recipes.isEmpty()) {
            return;
        }
        player.discoverRecipes(new ArrayList<>(recipes));
    }
}
