package org.gamefunxiao.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.HunterGameBackendMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final GameFunXiao plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigManager(GameFunXiao plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // 保存默认配置
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // 从 jar 包中读取默认值，自动补充缺失的键
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8)
        );
        config.setDefaults(defaults);
        if (mergeMissingKeys(config, defaults)) {
            plugin.saveConfig();
        }

        // 创建配置文件夹
        File configFolder = new File(plugin.getDataFolder(), "config");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        // 加载游戏模式配置
        loadConfig("gamemodes");

        // 加载修饰符配置
        loadConfig("modifiers");

        // 加载奖励配置
        loadConfig("rewards");

        // 加载记分板配置
        loadConfig("scoreboard");

        // 加载小游戏独立地图配置
        loadConfig("minigame-maps");

        // 加载自动生成竞技场小游戏配置
        loadConfig("minigames");

    }

    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder() + "/config", name + ".yml");
        if (!file.exists()) {
            plugin.saveResource("config/" + name + ".yml", false);
        }
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defaults = loadDefaultConfig("config/" + name + ".yml");
        loaded.setDefaults(defaults);
        configs.put(name, loaded);
        if (mergeMissingKeys(loaded, defaults)) {
            saveConfig(name);
        }
    }

    public void saveConfig(String name) {
        File file = new File(plugin.getDataFolder() + "/config", name + ".yml");
        try {
            configs.get(name).save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + name);
            e.printStackTrace();
        }
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        // 从 jar 包中读取默认值，自动补充缺失的键
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("config.yml"), StandardCharsets.UTF_8)
        );
        config.setDefaults(defaults);
        if (mergeMissingKeys(config, defaults)) {
            plugin.saveConfig();
        }

        for (String name : configs.keySet()) {
            File file = new File(plugin.getDataFolder() + "/config", name + ".yml");
            FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
            FileConfiguration configDefaults = loadDefaultConfig("config/" + name + ".yml");
            loaded.setDefaults(configDefaults);
            configs.put(name, loaded);
            if (mergeMissingKeys(loaded, configDefaults)) {
                saveConfig(name);
            }
        }
    }


    private FileConfiguration loadDefaultConfig(String resourcePath) {
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource(resourcePath), StandardCharsets.UTF_8)
        );
    }

    private boolean mergeMissingKeys(FileConfiguration target, FileConfiguration defaults) {
        if (target == null || defaults == null) {
            return false;
        }
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                if (!target.isConfigurationSection(key)) {
                    target.createSection(key);
                    changed = true;
                }
                continue;
            }
            if (!target.isSet(key)) {
                target.set(key, defaults.get(key));
                changed = true;
            }
        }
        int defaultVersion = defaults.getInt("config_version", target.getInt("config_version", 0));
        if (defaultVersion > target.getInt("config_version", 0)) {
            target.set("config_version", defaultVersion);
            changed = true;
        }
        return changed;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    public String getPrefix() {
        return config.getString("prefix", "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏 §x§F§F§6§6§0§0» §f");
    }

    public String getHunterGamePrefix() {
        return config.getString("hunter_game_prefix", "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9游§x§C§C§F§F§9§9戏 §x§F§F§6§6§0§0» §f");
    }

    public String getMiniGamePrefix() {
        return config.getString("minigame_prefix", "§x§7§D§F§F§C§8✦ §x§9§3§F§F§D§6小§x§A§9§F§F§E§4游§x§B§F§F§F§F§2戏 §x§7§D§F§F§C§8» §f");
    }

    public String getLuckyPillarsPrefix() {
        return config.getString("lucky_pillars_prefix", "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱 §x§F§F§D§D§5§5» §f");
    }

    public String getMiniGameCurrencyName() {
        return config.getString("minigame_currency.name", "小游戏币");
    }

    public int getNetherHunterVoteTime() {
        return Math.max(1, config.getInt("hunter_game.nether_hunter_vote_time", 45));
    }

    public int getEndChapterPreStartCountdown() {
        return Math.max(1, config.getInt("hunter_game.end_chapter_pre_start_countdown", 10));
    }

    public HunterGameBackendMode getHunterGameBackendMode() {
        return HunterGameBackendMode.fromString(
                config.getString("hunter_game.room_backend", "cross_server")
        );
    }


    public boolean isCrossServerBackendEnabled() {
        return config.getBoolean("hunter_game.cross_server.enabled", true);
    }

    public String getCrossServerGameServerName() {
        return config.getString("hunter_game.cross_server.game_server_name", "gameing");
    }

    public String getCrossServerLobbyServerName() {
        return config.getString("hunter_game.cross_server.lobby_server_name",
                config.getString("child_server.lobby_server_name", "gamefun"));
    }

    public String getCrossServerRequestPath() {
        return config.getString("hunter_game.cross_server.request_path", "D:/plus/GameFunShared/cross-create-requests");
    }

    public String getSharedPlayerDataRootPath() {
        String explicitCrossRoot = normalizePath(config.getString("hunter_game.cross_server.shared_playerdata_root", ""));
        if (!explicitCrossRoot.isEmpty()) {
            return explicitCrossRoot;
        }

        String legacySharedRoot = normalizePath(config.getString("child_server.shared_storage.playerdata_root", ""));
        if (!legacySharedRoot.isEmpty()) {
            return legacySharedRoot;
        }

        String requestPath = normalizePath(getCrossServerRequestPath());
        if (requestPath.isEmpty()) {
            return "";
        }

        File requestDir = new File(requestPath);
        File parent = requestDir.getParentFile();
        return parent == null ? "" : parent.getAbsolutePath();
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }

    public boolean isRoomChatIsolationEnabled() {
        return config.getBoolean("hunter_game.chat.isolate_room_chat",
                config.getBoolean("hunter_game.chat.isolate_game_chat", true));
    }

    public boolean isCrossServerAdvertiseEnabled() {
        return config.getBoolean("hunter_game.cross_server.forward_advertise_to_lobby", true);
    }

    public boolean isScoreboardEnabled() {
        FileConfiguration scoreboard = getConfig("scoreboard");
        return scoreboard == null || scoreboard.getBoolean("enabled", true);
    }

    public boolean shouldShowScoreboardNumbers() {
        FileConfiguration scoreboard = getConfig("scoreboard");
        return scoreboard == null || scoreboard.getBoolean("show_right_numbers", true);
    }

    public boolean shouldScoreboardRespectForeignSidebar() {
        FileConfiguration scoreboard = getConfig("scoreboard");
        return scoreboard != null && scoreboard.getBoolean("compatibility.respect_foreign_sidebar", true);
    }

    public String getScoreboardTitle() {
        FileConfiguration scoreboard = getConfig("scoreboard");
        return scoreboard == null ? "§e§l⚔ 猎人游戏" : scoreboard.getString("title", "§e§l⚔ 猎人游戏");
    }

    public int getHunterGamePreloadRadius() {
        return Math.max(0, config.getInt("hunter_game.performance.preload_radius", 2));
    }

    public int getHunterGamePreloadParallelTasks() {
        return Math.max(1, config.getInt("hunter_game.performance.preload_parallel_tasks", 2));
    }

    public boolean isLazyDimensionCreationEnabled() {
        return config.getBoolean("hunter_game.performance.lazy_dimension_creation", true);
    }

    public int getConfigVersion() {
        return config.getInt("config_version", 1);
    }

    public boolean isMenuVersionEnabled() {
        return config.getBoolean("menu.show_version", true);
    }

    public String getMenuVersion() {
        return config.getString("menu.version", "v1.1.0");
    }

    public boolean shouldCloseButtonUseReturnMode() {
        return config.getBoolean("menu.close_button.display_as_return_button", false);
    }

    public String getCloseButtonReturnCommand() {
        return config.getString("menu.close_button.return_command", "menu");
    }

}

