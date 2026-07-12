package org.gamefunxiao.game;

import java.util.List;

public final class HunterPerformanceRank {

    private static final List<RankTier> HUNTER_TIERS = List.of(
            new RankTier(3000, "龙王猎人", "§x§C§C§5§5§F§F"),
            new RankTier(2000, "末影猎人", "§x§8§8§5§5§F§F"),
            new RankTier(1400, "合金猎人", "§x§D§D§D§D§D§D"),
            new RankTier(900, "钻石猎人", "§x§5§5§F§F§F§F"),
            new RankTier(500, "金猎人", "§x§F§F§D§D§5§5"),
            new RankTier(250, "铁猎人", "§x§D§D§D§D§D§D"),
            new RankTier(100, "石猎人", "§x§A§A§A§A§A§A"),
            new RankTier(0, "木猎人", "§x§C§C§8§8§5§5")
    );

    private static final List<RankTier> PREY_TIERS = List.of(
            new RankTier(3000, "龙王猎物", "§x§C§C§5§5§F§F"),
            new RankTier(2000, "末影猎物", "§x§8§8§5§5§F§F"),
            new RankTier(1400, "合金猎物", "§x§D§D§D§D§D§D"),
            new RankTier(900, "钻石猎物", "§x§5§5§F§F§F§F"),
            new RankTier(500, "金猎物", "§x§F§F§D§D§5§5"),
            new RankTier(250, "铁猎物", "§x§D§D§D§D§D§D"),
            new RankTier(100, "石猎物", "§x§A§A§A§A§A§A"),
            new RankTier(0, "木猎物", "§x§C§C§8§8§5§5")
    );

    private HunterPerformanceRank() {
    }

    public static RankTier hunter(int value) {
        return resolve(HUNTER_TIERS, value);
    }

    public static RankTier prey(int value) {
        return resolve(PREY_TIERS, value);
    }

    public static List<RankTier> hunterTiers() {
        return HUNTER_TIERS;
    }

    public static List<RankTier> preyTiers() {
        return PREY_TIERS;
    }

    public static String coloredHunterRank(int value) {
        RankTier tier = hunter(value);
        return tier.color() + tier.name();
    }

    public static String coloredPreyRank(int value) {
        RankTier tier = prey(value);
        return tier.color() + tier.name();
    }

    private static RankTier resolve(List<RankTier> tiers, int value) {
        int safeValue = Math.max(0, value);
        for (RankTier tier : tiers) {
            if (safeValue >= tier.threshold()) {
                return tier;
            }
        }
        return tiers.get(tiers.size() - 1);
    }

    public record RankTier(int threshold, String name, String color) {
    }
}
