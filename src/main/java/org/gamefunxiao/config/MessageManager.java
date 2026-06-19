package org.gamefunxiao.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MessageManager {

    private final GameFunXiao plugin;
    private FileConfiguration messages;
    private final File messagesFile;

    public MessageManager(GameFunXiao plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadMessages();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // 从 jar 包中读取新版默认值，补齐缺失键并保存，不覆盖服主已修改的内容。
        FileConfiguration defaults = loadDefaultMessages();
        messages.setDefaults(defaults);
        if (mergeMissingKeys(messages, defaults)) {
            saveMessages();
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存消息文件！");
            e.printStackTrace();
        }
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // reload 时也补齐新版缺失消息，避免出现“消息未找到”。
        FileConfiguration defaults = loadDefaultMessages();
        messages.setDefaults(defaults);
        if (mergeMissingKeys(messages, defaults)) {
            saveMessages();
        }
    }

    public String getMessage(String path) {
        if (messages.isSet(path) || messages.getDefaults() != null && messages.getDefaults().isSet(path)) {
            String value = messages.getString(path);
            if (value != null && !value.isBlank()) {
                return value;
            }
            if (messages.getDefaults() != null) {
                String def = messages.getDefaults().getString(path);
                if (def != null && !def.isBlank()) {
                    return def;
                }
            }
        }
        return "§c消息未找到: " + path;
    }

    public String getMessageWithPrefix(String path) {
        return withPrefixIfPresent(plugin.getConfigManager().getPrefix(), path, null);
    }

    public String getHunterGameMessageWithPrefix(String path) {
        return withPrefixIfPresent(plugin.getConfigManager().getHunterGamePrefix(), path, null);
    }

    public String getHunterGameMessageWithPrefix(String path, Map<String, String> placeholders) {
        return withPrefixIfPresent(plugin.getConfigManager().getHunterGamePrefix(), path, placeholders);
    }

    public String getMiniGameMessageWithPrefix(String path) {
        return withPrefixIfPresent(plugin.getConfigManager().getMiniGamePrefix(), path, null);
    }

    public String getMiniGameMessageWithPrefix(String path, Map<String, String> placeholders) {
        return withPrefixIfPresent(plugin.getConfigManager().getMiniGamePrefix(), path, placeholders);
    }

    public String getLuckyPillarsMessageWithPrefix(String path) {
        return withPrefixIfPresent(plugin.getConfigManager().getLuckyPillarsPrefix(), path, null);
    }

    public String getLuckyPillarsMessageWithPrefix(String path, Map<String, String> placeholders) {
        return withPrefixIfPresent(plugin.getConfigManager().getLuckyPillarsPrefix(), path, placeholders);
    }

    public String getBrickGuardMessageWithPrefix(String path) {
        return withPrefixIfPresent(plugin.getConfigManager().getBrickGuardPrefix(), path, null);
    }

    public String getBrickGuardMessageWithPrefix(String path, Map<String, String> placeholders) {
        return withPrefixIfPresent(plugin.getConfigManager().getBrickGuardPrefix(), path, placeholders);
    }

    public String getModeMessageWithPrefix(GameMode mode, String path) {
        if (mode != null && mode.isLuckyPillars()) {
            return getLuckyPillarsMessageWithPrefix(path);
        }
        if (mode != null && mode.isBrickGuard()) {
            return getBrickGuardMessageWithPrefix(path);
        }
        if (mode != null && (mode.isStandaloneMiniGame() || mode.isIndependentMode())) {
            return getMiniGameMessageWithPrefix(path);
        }
        return getHunterGameMessageWithPrefix(path);
    }

    public String getModeMessageWithPrefix(GameMode mode, String path, Map<String, String> placeholders) {
        if (mode != null && mode.isLuckyPillars()) {
            return getLuckyPillarsMessageWithPrefix(path, placeholders);
        }
        if (mode != null && mode.isBrickGuard()) {
            return getBrickGuardMessageWithPrefix(path, placeholders);
        }
        if (mode != null && (mode.isStandaloneMiniGame() || mode.isIndependentMode())) {
            return getMiniGameMessageWithPrefix(path, placeholders);
        }
        return getHunterGameMessageWithPrefix(path, placeholders);
    }

    public String getRoomMessageWithPrefix(GameRoom room, String path) {
        return getModeMessageWithPrefix(room == null ? null : room.getGameMode(), path);
    }

    public String getRoomMessageWithPrefix(GameRoom room, String path, Map<String, String> placeholders) {
        return getModeMessageWithPrefix(room == null ? null : room.getGameMode(), path, placeholders);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public String getMessageWithPrefix(String path, Map<String, String> placeholders) {
        return withPrefixIfPresent(plugin.getConfigManager().getPrefix(), path, placeholders);
    }


    private String withPrefixIfPresent(String prefix, String path, Map<String, String> placeholders) {
        if (!hasMessage(path)) {
            return getMessage(path, placeholders);
        }
        return prefix + getMessage(path, placeholders);
    }

    private boolean hasMessage(String path) {
        return messages.isSet(path) || messages.getDefaults() != null && messages.getDefaults().isSet(path);
    }

    private FileConfiguration loadDefaultMessages() {
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)
        );
    }

    private boolean mergeMissingKeys(FileConfiguration target, FileConfiguration defaults) {
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

    public void sendMessage(Player player, String path) {
        player.sendMessage(getMessageWithPrefix(path));
    }

    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getMessageWithPrefix(path, placeholders));
    }
}
