package org.gamefunxiao.data;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerData {

    private final UUID uuid;
    private String playerName;
    private int coins;
    private final Set<String> ownedVictoryEffects = new HashSet<>();
    private final Set<String> ownedLuckyPillarsVictoryEffects = new HashSet<>();
    private String selectedHunterVictoryEffect = "fireworks";
    private String selectedLuckyPillarsVictoryEffect = "fireworks";
    private final Set<String> ownedKillEffects = new HashSet<>();
    private String selectedHunterKillEffect = "none";
    private String messageFrequency = "chatty";
    private final Set<String> brickGuardAchievements = new HashSet<>();

    // 猎物通关次数
    private int preyWinsTotal;
    private int preyWinsDay;
    private int preyWinsWeek;
    private int preyWinsMonth;
    private int preyWinsYear;
    private final Map<String, TimedIntRecord> preyWinsByMode = new HashMap<>();

    // 猎人胜利次数
    private int hunterWinsTotal;
    private int hunterWinsDay;
    private int hunterWinsWeek;
    private int hunterWinsMonth;
    private int hunterWinsYear;
    private final Map<String, TimedIntRecord> hunterWinsByMode = new HashMap<>();

    // 游玩次数
    private int playCountTotal;
    private int playCountDay;
    private int playCountWeek;
    private int playCountMonth;
    private int playCountYear;
    private final Map<String, TimedIntRecord> playCountByMode = new HashMap<>();

    // 最快通关时间
    private long fastestTimeTotal;
    private long fastestTimeDay;
    private long fastestTimeWeek;
    private long fastestTimeMonth;
    private long fastestTimeYear;
    private final Map<String, FastestTimeRecord> fastestTimeByMode = new HashMap<>();

    // 猎人击杀次数
    private int hunterKillsTotal;
    private int hunterKillsDay;
    private int hunterKillsWeek;
    private int hunterKillsMonth;
    private int hunterKillsYear;
    private final Map<String, TimedIntRecord> hunterKillsByMode = new HashMap<>();

    // 猎人积分
    private int hunterPointsTotal;
    private int hunterPointsDay;
    private int hunterPointsWeek;
    private int hunterPointsMonth;
    private int hunterPointsYear;
    private final Map<String, TimedIntRecord> hunterPointsByMode = new HashMap<>();

    // 猎物积分
    private int preyPointsTotal;
    private int preyPointsDay;
    private int preyPointsWeek;
    private int preyPointsMonth;
    private int preyPointsYear;
    private final Map<String, TimedIntRecord> preyPointsByMode = new HashMap<>();

    // 小游戏积分（幸运之柱等非猎人/猎物队伍玩法使用）
    private int miniGamePointsTotal;
    private int miniGamePointsDay;
    private int miniGamePointsWeek;
    private int miniGamePointsMonth;
    private int miniGamePointsYear;
    private final Map<String, TimedIntRecord> miniGamePointsByMode = new HashMap<>();

    // 猎物击杀猎人累计次数（用于每10次+1积分判断）
    private int preyKillHunterTotal;

    // 重置时间记录
    private long lastResetDay;
    private long lastResetWeek;
    private long lastResetMonth;
    private long lastResetYear;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.playerName = Bukkit.getOfflinePlayer(uuid).getName();
        if (this.playerName == null) {
            this.playerName = "Unknown";
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public Set<String> getOwnedVictoryEffects() {
        ownedVictoryEffects.add("fireworks");
        return new HashSet<>(ownedVictoryEffects);
    }

    public void setOwnedVictoryEffects(Set<String> effects) {
        ownedVictoryEffects.clear();
        if (effects != null) {
            for (String effect : effects) {
                if (effect != null && !effect.isBlank()) {
                    ownedVictoryEffects.add(effect.toLowerCase());
                }
            }
        }
        ownedVictoryEffects.add("fireworks");
    }

    public boolean hasVictoryEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }
        return "fireworks".equalsIgnoreCase(effectId) || ownedVictoryEffects.contains(effectId.toLowerCase());
    }

    public void unlockVictoryEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return;
        }
        ownedVictoryEffects.add(effectId.toLowerCase());
    }

    public Set<String> getOwnedLuckyPillarsVictoryEffects() {
        ownedLuckyPillarsVictoryEffects.add("fireworks");
        return new HashSet<>(ownedLuckyPillarsVictoryEffects);
    }

    public void setOwnedLuckyPillarsVictoryEffects(Set<String> effects) {
        ownedLuckyPillarsVictoryEffects.clear();
        if (effects != null) {
            for (String effect : effects) {
                if (effect != null && !effect.isBlank()) {
                    ownedLuckyPillarsVictoryEffects.add(effect.toLowerCase());
                }
            }
        }
        ownedLuckyPillarsVictoryEffects.add("fireworks");
    }

    public boolean hasLuckyPillarsVictoryEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }
        return "fireworks".equalsIgnoreCase(effectId) || ownedLuckyPillarsVictoryEffects.contains(effectId.toLowerCase());
    }

    public void unlockLuckyPillarsVictoryEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return;
        }
        ownedLuckyPillarsVictoryEffects.add(effectId.toLowerCase());
    }

    public String getSelectedHunterVictoryEffect() {
        if (selectedHunterVictoryEffect == null || selectedHunterVictoryEffect.isBlank()) {
            return "fireworks";
        }
        return selectedHunterVictoryEffect;
    }

    public void setSelectedHunterVictoryEffect(String selectedHunterVictoryEffect) {
        this.selectedHunterVictoryEffect = selectedHunterVictoryEffect == null || selectedHunterVictoryEffect.isBlank()
                ? "fireworks"
                : selectedHunterVictoryEffect.toLowerCase();
    }

    public String getSelectedLuckyPillarsVictoryEffect() {
        if (selectedLuckyPillarsVictoryEffect == null || selectedLuckyPillarsVictoryEffect.isBlank()) {
            return "fireworks";
        }
        return selectedLuckyPillarsVictoryEffect;
    }

    public void setSelectedLuckyPillarsVictoryEffect(String selectedLuckyPillarsVictoryEffect) {
        this.selectedLuckyPillarsVictoryEffect = selectedLuckyPillarsVictoryEffect == null || selectedLuckyPillarsVictoryEffect.isBlank()
                ? "fireworks"
                : selectedLuckyPillarsVictoryEffect.toLowerCase();
    }

    public Set<String> getOwnedKillEffects() {
        ownedKillEffects.add("none");
        return new HashSet<>(ownedKillEffects);
    }

    public void setOwnedKillEffects(Set<String> effects) {
        ownedKillEffects.clear();
        if (effects != null) {
            for (String effect : effects) {
                if (effect != null && !effect.isBlank()) {
                    ownedKillEffects.add(effect.toLowerCase());
                }
            }
        }
        ownedKillEffects.add("none");
    }

    public boolean hasKillEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }
        return "none".equalsIgnoreCase(effectId) || ownedKillEffects.contains(effectId.toLowerCase());
    }

    public void unlockKillEffect(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return;
        }
        ownedKillEffects.add(effectId.toLowerCase());
    }

    public String getSelectedHunterKillEffect() {
        if (selectedHunterKillEffect == null || selectedHunterKillEffect.isBlank()) {
            return "none";
        }
        return selectedHunterKillEffect;
    }

    public void setSelectedHunterKillEffect(String selectedHunterKillEffect) {
        this.selectedHunterKillEffect = selectedHunterKillEffect == null || selectedHunterKillEffect.isBlank()
                ? "none"
                : selectedHunterKillEffect.toLowerCase();
    }

    public String getMessageFrequency() {
        return messageFrequency == null || messageFrequency.isBlank() ? "chatty" : messageFrequency;
    }

    public boolean isCompactMessages() {
        return "normal".equalsIgnoreCase(getMessageFrequency());
    }

    public void setMessageFrequency(String messageFrequency) {
        this.messageFrequency = messageFrequency == null || messageFrequency.isBlank()
                ? "chatty"
                : messageFrequency.toLowerCase();
    }

    public Set<String> getBrickGuardAchievements() {
        return new HashSet<>(brickGuardAchievements);
    }

    public void setBrickGuardAchievements(Set<String> achievements) {
        brickGuardAchievements.clear();
        if (achievements == null) {
            return;
        }
        for (String achievement : achievements) {
            if (achievement != null && !achievement.isBlank()) {
                brickGuardAchievements.add(achievement.toLowerCase(java.util.Locale.ROOT).trim());
            }
        }
    }

    public boolean hasBrickGuardAchievement(String achievementId) {
        return achievementId != null
                && !achievementId.isBlank()
                && brickGuardAchievements.contains(achievementId.toLowerCase(java.util.Locale.ROOT).trim());
    }

    public boolean unlockBrickGuardAchievement(String achievementId) {
        if (achievementId == null || achievementId.isBlank()) {
            return false;
        }
        return brickGuardAchievements.add(achievementId.toLowerCase(java.util.Locale.ROOT).trim());
    }

    // 猎物通关次数
    public int getPreyWins(String timeRange) {
        return switch (timeRange) {
            case "day" -> preyWinsDay;
            case "week" -> preyWinsWeek;
            case "month" -> preyWinsMonth;
            case "year" -> preyWinsYear;
            default -> preyWinsTotal;
        };
    }

    public int getPreyWins(String timeRange, String modeId) {
        return getModeValue(preyWinsByMode, timeRange, modeId, () -> getPreyWins(timeRange));
    }

    public int getPreyWinsTotal() { return preyWinsTotal; }
    public void setPreyWinsTotal(int value) { this.preyWinsTotal = value; }
    public int getPreyWinsDay() { return preyWinsDay; }
    public void setPreyWinsDay(int value) { this.preyWinsDay = value; }
    public int getPreyWinsWeek() { return preyWinsWeek; }
    public void setPreyWinsWeek(int value) { this.preyWinsWeek = value; }
    public int getPreyWinsMonth() { return preyWinsMonth; }
    public void setPreyWinsMonth(int value) { this.preyWinsMonth = value; }
    public int getPreyWinsYear() { return preyWinsYear; }
    public void setPreyWinsYear(int value) { this.preyWinsYear = value; }
    public void setPreyWinsForMode(String modeId, String timeRange, int value) { setModeValue(preyWinsByMode, modeId, timeRange, value); }
    public void addPreyWinsForMode(String modeId, int amount) { addModeValue(preyWinsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getPreyWinsByModeSnapshot() { return getModeSnapshot(preyWinsByMode); }
    public void resetPreyWinsByModeDay() { resetModeDay(preyWinsByMode); }
    public void resetPreyWinsByModeWeek() { resetModeWeek(preyWinsByMode); }
    public void resetPreyWinsByModeMonth() { resetModeMonth(preyWinsByMode); }
    public void resetPreyWinsByModeYear() { resetModeYear(preyWinsByMode); }

    // 猎人胜利次数
    public int getHunterWins(String timeRange) {
        return switch (timeRange) {
            case "day" -> hunterWinsDay;
            case "week" -> hunterWinsWeek;
            case "month" -> hunterWinsMonth;
            case "year" -> hunterWinsYear;
            default -> hunterWinsTotal;
        };
    }

    public int getHunterWins(String timeRange, String modeId) {
        return getModeValue(hunterWinsByMode, timeRange, modeId, () -> getHunterWins(timeRange));
    }

    public int getHunterWinsTotal() { return hunterWinsTotal; }
    public void setHunterWinsTotal(int value) { this.hunterWinsTotal = value; }
    public int getHunterWinsDay() { return hunterWinsDay; }
    public void setHunterWinsDay(int value) { this.hunterWinsDay = value; }
    public int getHunterWinsWeek() { return hunterWinsWeek; }
    public void setHunterWinsWeek(int value) { this.hunterWinsWeek = value; }
    public int getHunterWinsMonth() { return hunterWinsMonth; }
    public void setHunterWinsMonth(int value) { this.hunterWinsMonth = value; }
    public int getHunterWinsYear() { return hunterWinsYear; }
    public void setHunterWinsYear(int value) { this.hunterWinsYear = value; }
    public void setHunterWinsForMode(String modeId, String timeRange, int value) { setModeValue(hunterWinsByMode, modeId, timeRange, value); }
    public void addHunterWinsForMode(String modeId, int amount) { addModeValue(hunterWinsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getHunterWinsByModeSnapshot() { return getModeSnapshot(hunterWinsByMode); }
    public void resetHunterWinsByModeDay() { resetModeDay(hunterWinsByMode); }
    public void resetHunterWinsByModeWeek() { resetModeWeek(hunterWinsByMode); }
    public void resetHunterWinsByModeMonth() { resetModeMonth(hunterWinsByMode); }
    public void resetHunterWinsByModeYear() { resetModeYear(hunterWinsByMode); }

    // 游玩次数
    public int getPlayCount(String timeRange) {
        return switch (timeRange) {
            case "day" -> playCountDay;
            case "week" -> playCountWeek;
            case "month" -> playCountMonth;
            case "year" -> playCountYear;
            default -> playCountTotal;
        };
    }

    public int getPlayCount(String timeRange, String modeId) {
        return getModeValue(playCountByMode, timeRange, modeId, () -> getPlayCount(timeRange));
    }

    public int getPlayCountTotal() { return playCountTotal; }
    public void setPlayCountTotal(int value) { this.playCountTotal = value; }
    public int getPlayCountDay() { return playCountDay; }
    public void setPlayCountDay(int value) { this.playCountDay = value; }
    public int getPlayCountWeek() { return playCountWeek; }
    public void setPlayCountWeek(int value) { this.playCountWeek = value; }
    public int getPlayCountMonth() { return playCountMonth; }
    public void setPlayCountMonth(int value) { this.playCountMonth = value; }
    public int getPlayCountYear() { return playCountYear; }
    public void setPlayCountYear(int value) { this.playCountYear = value; }
    public void setPlayCountForMode(String modeId, String timeRange, int value) { setModeValue(playCountByMode, modeId, timeRange, value); }
    public void addPlayCountForMode(String modeId, int amount) { addModeValue(playCountByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getPlayCountByModeSnapshot() { return getModeSnapshot(playCountByMode); }
    public void resetPlayCountByModeDay() { resetModeDay(playCountByMode); }
    public void resetPlayCountByModeWeek() { resetModeWeek(playCountByMode); }
    public void resetPlayCountByModeMonth() { resetModeMonth(playCountByMode); }
    public void resetPlayCountByModeYear() { resetModeYear(playCountByMode); }

    // 最快通关时间
    public long getFastestTime(String timeRange) {
        return switch (timeRange) {
            case "day" -> fastestTimeDay;
            case "week" -> fastestTimeWeek;
            case "month" -> fastestTimeMonth;
            case "year" -> fastestTimeYear;
            default -> fastestTimeTotal;
        };
    }

    public long getFastestTimeTotal() { return fastestTimeTotal; }
    public void setFastestTimeTotal(long value) { this.fastestTimeTotal = value; }
    public long getFastestTimeDay() { return fastestTimeDay; }
    public void setFastestTimeDay(long value) { this.fastestTimeDay = value; }
    public long getFastestTimeWeek() { return fastestTimeWeek; }
    public void setFastestTimeWeek(long value) { this.fastestTimeWeek = value; }
    public long getFastestTimeMonth() { return fastestTimeMonth; }
    public void setFastestTimeMonth(long value) { this.fastestTimeMonth = value; }
    public long getFastestTimeYear() { return fastestTimeYear; }
    public void setFastestTimeYear(long value) { this.fastestTimeYear = value; }

    public long getFastestTime(String timeRange, String modeId) {
        if (modeId == null || modeId.isBlank() || modeId.equalsIgnoreCase("all")) {
            return getFastestTime(timeRange);
        }
        FastestTimeRecord record = fastestTimeByMode.get(normalizeModeId(modeId));
        return record == null ? 0L : record.get(timeRange);
    }

    public void setFastestTimeForMode(String modeId, String timeRange, long value) {
        if (modeId == null || modeId.isBlank()) {
            return;
        }
        fastestTimeByMode.computeIfAbsent(normalizeModeId(modeId), ignored -> new FastestTimeRecord()).set(timeRange, Math.max(0L, value));
    }

    public void updateFastestTimeForMode(String modeId, long time) {
        if (modeId == null || modeId.isBlank() || time <= 0L) {
            return;
        }
        fastestTimeByMode.computeIfAbsent(normalizeModeId(modeId), ignored -> new FastestTimeRecord()).update(time);
    }

    public Map<String, Map<String, Long>> getFastestTimeByModeSnapshot() {
        Map<String, Map<String, Long>> snapshot = new HashMap<>();
        for (Map.Entry<String, FastestTimeRecord> entry : fastestTimeByMode.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().toMap());
        }
        return snapshot;
    }

    public void resetFastestTimeByModeDay() {
        fastestTimeByMode.values().forEach(record -> record.day = 0L);
    }

    public void resetFastestTimeByModeWeek() {
        fastestTimeByMode.values().forEach(record -> record.week = 0L);
    }

    public void resetFastestTimeByModeMonth() {
        fastestTimeByMode.values().forEach(record -> record.month = 0L);
    }

    public void resetFastestTimeByModeYear() {
        fastestTimeByMode.values().forEach(record -> record.year = 0L);
    }

    // 猎人击杀
    public int getHunterKills(String timeRange) {
        return switch (timeRange) {
            case "day" -> hunterKillsDay;
            case "week" -> hunterKillsWeek;
            case "month" -> hunterKillsMonth;
            case "year" -> hunterKillsYear;
            default -> hunterKillsTotal;
        };
    }

    public int getHunterKills(String timeRange, String modeId) {
        return getModeValue(hunterKillsByMode, timeRange, modeId, () -> getHunterKills(timeRange));
    }

    public int getHunterKillsTotal() { return hunterKillsTotal; }
    public void setHunterKillsTotal(int value) { this.hunterKillsTotal = value; }
    public int getHunterKillsDay() { return hunterKillsDay; }
    public void setHunterKillsDay(int value) { this.hunterKillsDay = value; }
    public int getHunterKillsWeek() { return hunterKillsWeek; }
    public void setHunterKillsWeek(int value) { this.hunterKillsWeek = value; }
    public int getHunterKillsMonth() { return hunterKillsMonth; }
    public void setHunterKillsMonth(int value) { this.hunterKillsMonth = value; }
    public int getHunterKillsYear() { return hunterKillsYear; }
    public void setHunterKillsYear(int value) { this.hunterKillsYear = value; }
    public void setHunterKillsForMode(String modeId, String timeRange, int value) { setModeValue(hunterKillsByMode, modeId, timeRange, value); }
    public void addHunterKillsForMode(String modeId, int amount) { addModeValue(hunterKillsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getHunterKillsByModeSnapshot() { return getModeSnapshot(hunterKillsByMode); }
    public void resetHunterKillsByModeDay() { resetModeDay(hunterKillsByMode); }
    public void resetHunterKillsByModeWeek() { resetModeWeek(hunterKillsByMode); }
    public void resetHunterKillsByModeMonth() { resetModeMonth(hunterKillsByMode); }
    public void resetHunterKillsByModeYear() { resetModeYear(hunterKillsByMode); }

    // 重置时间
    public long getLastResetDay() { return lastResetDay; }
    public void setLastResetDay(long value) { this.lastResetDay = value; }
    public long getLastResetWeek() { return lastResetWeek; }
    public void setLastResetWeek(long value) { this.lastResetWeek = value; }
    public long getLastResetMonth() { return lastResetMonth; }
    public void setLastResetMonth(long value) { this.lastResetMonth = value; }
    public long getLastResetYear() { return lastResetYear; }
    public void setLastResetYear(long value) { this.lastResetYear = value; }

    // 猎人积分
    public int getHunterPoints(String timeRange) {
        return switch (timeRange) {
            case "day" -> hunterPointsDay;
            case "week" -> hunterPointsWeek;
            case "month" -> hunterPointsMonth;
            case "year" -> hunterPointsYear;
            default -> hunterPointsTotal;
        };
    }

    public int getHunterPoints(String timeRange, String modeId) {
        return getModeValue(hunterPointsByMode, timeRange, modeId, () -> getHunterPoints(timeRange));
    }

    public int getHunterPointsTotal() { return hunterPointsTotal; }
    public void setHunterPointsTotal(int value) { this.hunterPointsTotal = value; }
    public int getHunterPointsDay() { return hunterPointsDay; }
    public void setHunterPointsDay(int value) { this.hunterPointsDay = value; }
    public int getHunterPointsWeek() { return hunterPointsWeek; }
    public void setHunterPointsWeek(int value) { this.hunterPointsWeek = value; }
    public int getHunterPointsMonth() { return hunterPointsMonth; }
    public void setHunterPointsMonth(int value) { this.hunterPointsMonth = value; }
    public int getHunterPointsYear() { return hunterPointsYear; }
    public void setHunterPointsYear(int value) { this.hunterPointsYear = value; }
    public void setHunterPointsForMode(String modeId, String timeRange, int value) { setModeValue(hunterPointsByMode, modeId, timeRange, value); }
    public void addHunterPointsForMode(String modeId, int amount) { addModeValue(hunterPointsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getHunterPointsByModeSnapshot() { return getModeSnapshot(hunterPointsByMode); }
    public void resetHunterPointsByModeDay() { resetModeDay(hunterPointsByMode); }
    public void resetHunterPointsByModeWeek() { resetModeWeek(hunterPointsByMode); }
    public void resetHunterPointsByModeMonth() { resetModeMonth(hunterPointsByMode); }
    public void resetHunterPointsByModeYear() { resetModeYear(hunterPointsByMode); }

    // 猎物积分
    public int getPreyPoints(String timeRange) {
        return switch (timeRange) {
            case "day" -> preyPointsDay;
            case "week" -> preyPointsWeek;
            case "month" -> preyPointsMonth;
            case "year" -> preyPointsYear;
            default -> preyPointsTotal;
        };
    }

    public int getPreyPoints(String timeRange, String modeId) {
        return getModeValue(preyPointsByMode, timeRange, modeId, () -> getPreyPoints(timeRange));
    }

    public int getPreyPointsTotal() { return preyPointsTotal; }
    public void setPreyPointsTotal(int value) { this.preyPointsTotal = value; }
    public int getPreyPointsDay() { return preyPointsDay; }
    public void setPreyPointsDay(int value) { this.preyPointsDay = value; }
    public int getPreyPointsWeek() { return preyPointsWeek; }
    public void setPreyPointsWeek(int value) { this.preyPointsWeek = value; }
    public int getPreyPointsMonth() { return preyPointsMonth; }
    public void setPreyPointsMonth(int value) { this.preyPointsMonth = value; }
    public int getPreyPointsYear() { return preyPointsYear; }
    public void setPreyPointsYear(int value) { this.preyPointsYear = value; }
    public void setPreyPointsForMode(String modeId, String timeRange, int value) { setModeValue(preyPointsByMode, modeId, timeRange, value); }
    public void addPreyPointsForMode(String modeId, int amount) { addModeValue(preyPointsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getPreyPointsByModeSnapshot() { return getModeSnapshot(preyPointsByMode); }
    public void resetPreyPointsByModeDay() { resetModeDay(preyPointsByMode); }
    public void resetPreyPointsByModeWeek() { resetModeWeek(preyPointsByMode); }
    public void resetPreyPointsByModeMonth() { resetModeMonth(preyPointsByMode); }
    public void resetPreyPointsByModeYear() { resetModeYear(preyPointsByMode); }

    // 小游戏积分
    public int getMiniGamePoints(String timeRange) {
        return switch (timeRange) {
            case "day" -> miniGamePointsDay;
            case "week" -> miniGamePointsWeek;
            case "month" -> miniGamePointsMonth;
            case "year" -> miniGamePointsYear;
            default -> miniGamePointsTotal;
        };
    }

    public int getMiniGamePoints(String timeRange, String modeId) {
        return getModeValue(miniGamePointsByMode, timeRange, modeId, () -> getMiniGamePoints(timeRange));
    }

    public int getMiniGamePointsTotal() { return miniGamePointsTotal; }
    public void setMiniGamePointsTotal(int value) { this.miniGamePointsTotal = value; }
    public int getMiniGamePointsDay() { return miniGamePointsDay; }
    public void setMiniGamePointsDay(int value) { this.miniGamePointsDay = value; }
    public int getMiniGamePointsWeek() { return miniGamePointsWeek; }
    public void setMiniGamePointsWeek(int value) { this.miniGamePointsWeek = value; }
    public int getMiniGamePointsMonth() { return miniGamePointsMonth; }
    public void setMiniGamePointsMonth(int value) { this.miniGamePointsMonth = value; }
    public int getMiniGamePointsYear() { return miniGamePointsYear; }
    public void setMiniGamePointsYear(int value) { this.miniGamePointsYear = value; }
    public void setMiniGamePointsForMode(String modeId, String timeRange, int value) { setModeValue(miniGamePointsByMode, modeId, timeRange, value); }
    public void addMiniGamePointsForMode(String modeId, int amount) { addModeValue(miniGamePointsByMode, modeId, amount); }
    public Map<String, Map<String, Integer>> getMiniGamePointsByModeSnapshot() { return getModeSnapshot(miniGamePointsByMode); }
    public void resetMiniGamePointsByModeDay() { resetModeDay(miniGamePointsByMode); }
    public void resetMiniGamePointsByModeWeek() { resetModeWeek(miniGamePointsByMode); }
    public void resetMiniGamePointsByModeMonth() { resetModeMonth(miniGamePointsByMode); }
    public void resetMiniGamePointsByModeYear() { resetModeYear(miniGamePointsByMode); }

    // 猎物击杀猎人累计次数
    public int getPreyKillHunterTotal() { return preyKillHunterTotal; }
    public void setPreyKillHunterTotal(int value) { this.preyKillHunterTotal = value; }

    private int getModeValue(Map<String, TimedIntRecord> source, String timeRange, String modeId, Supplier<Integer> fallback) {
        if (modeId == null || modeId.isBlank() || modeId.equalsIgnoreCase("all")) {
            return fallback.get();
        }
        TimedIntRecord record = source.get(normalizeModeId(modeId));
        return record == null ? 0 : record.get(timeRange);
    }

    private void setModeValue(Map<String, TimedIntRecord> source, String modeId, String timeRange, int value) {
        if (modeId == null || modeId.isBlank()) {
            return;
        }
        source.computeIfAbsent(normalizeModeId(modeId), ignored -> new TimedIntRecord()).set(timeRange, Math.max(0, value));
    }

    private void addModeValue(Map<String, TimedIntRecord> source, String modeId, int amount) {
        if (modeId == null || modeId.isBlank()) {
            return;
        }
        source.computeIfAbsent(normalizeModeId(modeId), ignored -> new TimedIntRecord()).add(amount);
    }

    private Map<String, Map<String, Integer>> getModeSnapshot(Map<String, TimedIntRecord> source) {
        Map<String, Map<String, Integer>> snapshot = new HashMap<>();
        for (Map.Entry<String, TimedIntRecord> entry : source.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().toMap());
        }
        return snapshot;
    }

    private void resetModeDay(Map<String, TimedIntRecord> source) {
        source.values().forEach(record -> record.day = 0);
    }

    private void resetModeWeek(Map<String, TimedIntRecord> source) {
        source.values().forEach(record -> record.week = 0);
    }

    private void resetModeMonth(Map<String, TimedIntRecord> source) {
        source.values().forEach(record -> record.month = 0);
    }

    private void resetModeYear(Map<String, TimedIntRecord> source) {
        source.values().forEach(record -> record.year = 0);
    }

    private String normalizeModeId(String modeId) {
        return modeId.toLowerCase(java.util.Locale.ROOT).trim();
    }

    private static class TimedIntRecord {
        private int total;
        private int day;
        private int week;
        private int month;
        private int year;

        private int get(String timeRange) {
            return switch (timeRange) {
                case "day" -> day;
                case "week" -> week;
                case "month" -> month;
                case "year" -> year;
                default -> total;
            };
        }

        private void set(String timeRange, int value) {
            switch (timeRange) {
                case "day" -> day = value;
                case "week" -> week = value;
                case "month" -> month = value;
                case "year" -> year = value;
                default -> total = value;
            }
        }

        private void add(int amount) {
            total = Math.max(0, total + amount);
            day = Math.max(0, day + amount);
            week = Math.max(0, week + amount);
            month = Math.max(0, month + amount);
            year = Math.max(0, year + amount);
        }

        private Map<String, Integer> toMap() {
            Map<String, Integer> map = new HashMap<>();
            map.put("total", total);
            map.put("day", day);
            map.put("week", week);
            map.put("month", month);
            map.put("year", year);
            return map;
        }
    }

    private static class FastestTimeRecord {
        private long total;
        private long day;
        private long week;
        private long month;
        private long year;

        private long get(String timeRange) {
            return switch (timeRange) {
                case "day" -> day;
                case "week" -> week;
                case "month" -> month;
                case "year" -> year;
                default -> total;
            };
        }

        private void set(String timeRange, long value) {
            switch (timeRange) {
                case "day" -> day = value;
                case "week" -> week = value;
                case "month" -> month = value;
                case "year" -> year = value;
                default -> total = value;
            }
        }

        private void update(long time) {
            if (total == 0L || time < total) total = time;
            if (day == 0L || time < day) day = time;
            if (week == 0L || time < week) week = time;
            if (month == 0L || time < month) month = time;
            if (year == 0L || time < year) year = time;
        }

        private Map<String, Long> toMap() {
            Map<String, Long> map = new HashMap<>();
            map.put("total", total);
            map.put("day", day);
            map.put("week", week);
            map.put("month", month);
            map.put("year", year);
            return map;
        }
    }
}
