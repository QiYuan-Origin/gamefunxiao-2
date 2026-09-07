package org.gamefunxiao.placeholder;



import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;

import org.gamefunxiao.cosmetics.HunterKillEffect;

import org.gamefunxiao.GameFunXiao;

import org.gamefunxiao.cosmetics.HunterVictoryEffect;

import org.gamefunxiao.cosmetics.LuckyPillarsVictoryEffect;

import org.gamefunxiao.cosmetics.DeathSwapVictoryEffect;

import org.gamefunxiao.data.PlayerData;

import org.gamefunxiao.game.HunterPerformanceRank;
import org.gamefunxiao.game.GameMode;



import java.util.Locale;
import java.util.List;



public class GameFunPlaceholderExpansion extends PlaceholderExpansion {



    private final GameFunXiao plugin;



    public GameFunPlaceholderExpansion(GameFunXiao plugin) {

        this.plugin = plugin;

    }



    @Override

    public String getIdentifier() {

        return "gamefun";

    }



    @Override

    public String getAuthor() {

        return "QiCheng";

    }



    @Override

    public String getVersion() {

        return plugin.getDescription().getVersion();

    }



    @Override

    public boolean persist() {

        return true;

    }



    @Override

    public String onRequest(OfflinePlayer player, String params) {

        if (params == null) {

            return "";

        }



        String key = params.toLowerCase();

        if (key.equals("currency") || key.equals("currency_name")) {

            return plugin.getConfigManager().getMiniGameCurrencyName();

        }



        if (player == null || player.getUniqueId() == null) {

            return "";

        }



        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        String statsValue = resolveStatsPlaceholder(data, key);

        if (statsValue != null) {

            return statsValue;

        }



        return switch (key) {

            case "coins", "minigame_coins", "money", "balance" ->

                    String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId()));

            case "hunter_victory_effect" ->

                    plugin.getPlayerDataManager().getSelectedHunterVictoryEffect(player.getUniqueId());

            case "hunter_victory_effect_name" -> {

                String effectId = plugin.getPlayerDataManager().getSelectedHunterVictoryEffect(player.getUniqueId());

                yield HunterVictoryEffect.byId(effectId).getDisplayName(plugin);

            }

            case "lucky_pillars_victory_effect" ->

                    plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());

            case "lucky_pillars_victory_effect_name" -> {

                String effectId = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());

                yield LuckyPillarsVictoryEffect.byId(effectId).getDisplayName(plugin);

            }

            case "death_swap_victory_effect" ->
                    plugin.getPlayerDataManager().getSelectedDeathSwapVictoryEffect(player.getUniqueId());

            case "death_swap_victory_effect_name" -> {
                String effectId = plugin.getPlayerDataManager().getSelectedDeathSwapVictoryEffect(player.getUniqueId());
                yield DeathSwapVictoryEffect.byId(effectId).getDisplayName(plugin);
            }

            case "hunter_kill_effect" ->

                    plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());

            case "hunter_kill_effect_name" -> {

                String effectId = plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());

                yield HunterKillEffect.byId(effectId).getDisplayName(plugin);

            }

            default -> "";

        };

    }



    private String resolveStatsPlaceholder(PlayerData data, String key) {

        if (data == null || key == null || key.isBlank()) {

            return null;

        }

        String normalized = key.toLowerCase(Locale.ROOT);

        if (normalized.equals("hunter_rank") || normalized.equals("hunter_points_rank")) {

            return HunterPerformanceRank.coloredHunterRank(data.getHunterPoints("total"));

        }

        if (normalized.equals("prey_rank") || normalized.equals("prey_points_rank")) {

            return HunterPerformanceRank.coloredPreyRank(data.getPreyPoints("total"));

        }

        if (normalized.equals("hunter_points")) {

            return String.valueOf(data.getHunterPoints("total"));

        }

        if (normalized.equals("prey_points")) {

            return String.valueOf(data.getPreyPoints("total"));

        }

        if (normalized.equals("minigame_points")) {

            return String.valueOf(data.getMiniGamePoints("total"));

        }

        if (normalized.equals("play_count")) {

            return String.valueOf(data.getPlayCount("total"));

        }

        if (normalized.equals("hunter_wins")) {

            return String.valueOf(data.getHunterWins("total"));

        }

        if (normalized.equals("prey_wins")) {

            return String.valueOf(data.getPreyWins("total"));

        }

        if (normalized.equals("hunter_kills")) {

            return String.valueOf(data.getHunterKills("total"));

        }

        if (normalized.equals("fastest_time")) {

            return formatDuration(data.getFastestTime("total"));

        }

        ParsedStatsPlaceholder parsed = parseStatsPlaceholder(normalized);

        if (parsed == null) {

            return null;

        }

        return resolveParsedStatsPlaceholder(data, parsed.base(), parsed.range(), parsed.modeId());

    }



    private ParsedStatsPlaceholder parseStatsPlaceholder(String normalized) {

        String remaining = normalized;

        String range = "total";

        String modeId = null;

        ModeToken trailingMode = findTrailingModeToken(remaining);

        if (trailingMode != null) {

            modeId = trailingMode.modeId();

            remaining = trailingMode.prefix();

        }

        String trailingRange = trailingToken(remaining);

        String normalizedRange = normalizeRange(trailingRange);

        if (normalizedRange != null && remaining.length() > trailingRange.length()) {

            range = normalizedRange;

            remaining = remaining.substring(0, remaining.length() - trailingRange.length() - 1);

        }

        if (modeId == null) {

            trailingMode = findTrailingModeToken(remaining);

            if (trailingMode != null) {

                modeId = trailingMode.modeId();

                remaining = trailingMode.prefix();

            }

        }

        trailingRange = trailingToken(remaining);

        normalizedRange = normalizeRange(trailingRange);

        if (normalizedRange != null && remaining.length() > trailingRange.length()) {

            range = normalizedRange;

            remaining = remaining.substring(0, remaining.length() - trailingRange.length() - 1);

        }

        if (remaining.isBlank()) {

            return null;

        }

        return new ParsedStatsPlaceholder(remaining, range, modeId);

    }



    private String resolveParsedStatsPlaceholder(PlayerData data, String base, String range, String modeId) {

        return switch (base) {

            case "hunter_rank", "hunter_points_rank" -> HunterPerformanceRank.coloredHunterRank(data.getHunterPoints(range, modeId));

            case "prey_rank", "prey_points_rank" -> HunterPerformanceRank.coloredPreyRank(data.getPreyPoints(range, modeId));

            case "hunter_points" -> String.valueOf(data.getHunterPoints(range, modeId));

            case "prey_points" -> String.valueOf(data.getPreyPoints(range, modeId));

            case "minigame_points", "mini_game_points" -> String.valueOf(data.getMiniGamePoints(range, modeId));

            case "play_count" -> String.valueOf(data.getPlayCount(range, modeId));

            case "hunter_wins" -> String.valueOf(data.getHunterWins(range, modeId));

            case "prey_wins" -> String.valueOf(data.getPreyWins(range, modeId));

            case "hunter_kills" -> String.valueOf(data.getHunterKills(range, modeId));

            case "fastest_time" -> formatDuration(data.getFastestTime(range, modeId));

            default -> null;

        };

    }



    private String trailingToken(String value) {

        if (value == null) {

            return "";

        }

        int index = value.lastIndexOf('_');

        return index < 0 ? value : value.substring(index + 1);

    }



    private ModeToken findTrailingModeToken(String value) {

        if (value == null || value.isBlank()) {

            return null;

        }

        for (String alias : List.of("flash_tournament", "flash_formula", "flashsmp", "flash_smp", "end_flash", "endflash", "tournament", "赛事", "终章", "flash")) {

            if (!value.equals(alias) && value.endsWith("_" + alias)) {

                return new ModeToken(value.substring(0, value.length() - alias.length() - 1), normalizeModeAlias(alias));

            }

        }

        for (GameMode mode : GameMode.values()) {

            String id = mode.getId().toLowerCase(Locale.ROOT);

            if (!value.equals(id) && value.endsWith("_" + id)) {

                return new ModeToken(value.substring(0, value.length() - id.length() - 1), id);

            }

        }

        return null;

    }



    private String normalizeModeAlias(String alias) {

        return switch (alias) {

            case "flash_formula", "flashsmp", "flash_smp" -> GameMode.FLASH.getId();

            case "endflash", "终章" -> GameMode.END_FLASH.getId();

            case "tournament", "赛事" -> GameMode.FLASH_TOURNAMENT.getId();

            default -> alias;

        };

    }



    private String normalizeRange(String range) {

        if (range == null) {

            return null;

        }

        return switch (range.toLowerCase(Locale.ROOT)) {

            case "total", "all", "day", "daily", "today", "week", "weekly", "month", "monthly", "year", "yearly" -> switch (range.toLowerCase(Locale.ROOT)) {

                case "all" -> "total";

                case "daily", "today" -> "day";

                case "weekly" -> "week";

                case "monthly" -> "month";

                case "yearly" -> "year";

                default -> range.toLowerCase(Locale.ROOT);

            };

            default -> null;

        };

    }



    private record ParsedStatsPlaceholder(String base, String range, String modeId) {

    }



    private record ModeToken(String prefix, String modeId) {

    }



    private String formatDuration(long millis) {

        if (millis <= 0L) {

            return "-";

        }

        long totalSeconds = millis / 1000L;

        long hours = totalSeconds / 3600L;

        long minutes = (totalSeconds % 3600L) / 60L;

        long seconds = totalSeconds % 60L;

        if (hours > 0L) {

            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);

        }

        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);

    }

}



