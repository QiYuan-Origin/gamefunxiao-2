package org.gamefunxiao.game;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * GameFun 的模式总分类中心。
 *
 * 以后要完整下架/删除一个模式，不要再到菜单、房间、记分板、监听器里面到处挖 Set：
 * 1. 把对应枚举的 enabledInBuild 改成 false；
 * 2. 把 category 改成 REMOVED；
 * 3. 再慢慢删除它自己的包/菜单/玩法实现代码即可。
 *
 * 这样旧数据、旧跨服注册、旧配置里残留的 id 不会把插件炸掉，但玩家也创建不了、菜单也不会显示。
 */
public enum GameMode {
    CLASSIC("classic", "经典模式", ModeCategory.HUNTER, true, 150, 300),
    RANDOM_COMPASS("random_compass", "随机指南针", ModeCategory.HUNTER, true, 150, 300),
    SWAP("swap", "互换模式", ModeCategory.HUNTER, true, 300, 400),
    NO_ITEM("no_item", "无有模式", ModeCategory.HUNTER, true, 500, 600),
    SURVIVAL("survival", "存活模式", ModeCategory.HUNTER, true, 50, 100),
    FLASH("flash", "闪光模式", ModeCategory.HUNTER, true, 150, 300),
    FLASH_TOURNAMENT("flash_tournament", "闪光 · §c§l赛事", ModeCategory.HUNTER, true, 150, 300),
    END_FLASH("end_flash", "终章 · 闪光", ModeCategory.HUNTER, true, 220, 420),

    LUCKY_PILLARS("lucky_pillars", "幸运之柱", ModeCategory.LUCKY_PILLARS, true, 120, 120),
    BRICK_GUARD("brick_guard", "雨云 · 板砖守卫战", ModeCategory.BRICK_GUARD, true, 150, 150),
    // 已下架模式：保留枚举只是为了兼容旧房间/旧数据/旧配置，不能创建，不能出现在菜单和补全里。
    LUCKY_PILLARS_PVP("lucky_pillars_pvp", "幸运之柱 · PVP大佬", ModeCategory.REMOVED, false, 120, 120),
    TNT_RUN("tnt_run", "TNT跑酷", ModeCategory.REMOVED, false, 120, 120),
    BLOCK_PARTY("block_party", "方块派对", ModeCategory.REMOVED, false, 120, 120),

    NETHER_CHAPTER("nether_chapter", "下界篇", ModeCategory.HUNTER_INTERNAL, true, 150, 300),
    END_CHAPTER("end_chapter", "末地篇", ModeCategory.HUNTER_INTERNAL, true, 180, 320),
    CUSTOM("custom", "自定义模式", ModeCategory.HUNTER, true, 100, 200);

    public enum ModeCategory {
        HUNTER,
        HUNTER_INTERNAL,
        LUCKY_PILLARS,
        BRICK_GUARD,
        REMOVED
    }

    private static final EnumSet<GameMode> FLASH_LIKE_MODES = EnumSet.of(
            FLASH,
            FLASH_TOURNAMENT,
            END_FLASH
    );

    private static final EnumSet<GameMode> DIRECT_FLASH_START_MODES = EnumSet.of(
            FLASH,
            END_FLASH
    );

    private static final EnumSet<GameMode> WORLD_SELECTION_MODES = EnumSet.of(
            CLASSIC,
            RANDOM_COMPASS,
            SWAP,
            NO_ITEM,
            SURVIVAL,
            FLASH_TOURNAMENT,
            NETHER_CHAPTER,
            END_CHAPTER,
            CUSTOM
    );

    private static final EnumSet<GameMode> HUNTER_CREATE_MENU_MODES = EnumSet.of(
            CLASSIC,
            RANDOM_COMPASS,
            SWAP,
            NO_ITEM,
            SURVIVAL,
            FLASH_TOURNAMENT,
            END_FLASH,
            CUSTOM
    );

    private static final EnumSet<GameMode> HUNTER_ADMIN_CREATE_MODES = EnumSet.of(
            CLASSIC,
            RANDOM_COMPASS,
            SWAP,
            NO_ITEM,
            SURVIVAL,
            FLASH_TOURNAMENT,
            LUCKY_PILLARS,
            CUSTOM
    );

    private final String id;
    private final String displayName;
    private final ModeCategory category;
    private final boolean enabledInBuild;
    private final int hunterReward;
    private final int preyReward;

    GameMode(String id, String displayName, ModeCategory category, boolean enabledInBuild, int hunterReward, int preyReward) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.enabledInBuild = enabledInBuild;
        this.hunterReward = hunterReward;
        this.preyReward = preyReward;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ModeCategory getCategory() {
        return category;
    }

    public boolean isEnabledInBuild() {
        return enabledInBuild && category != ModeCategory.REMOVED;
    }

    public int getHunterReward() {
        return hunterReward;
    }

    public int getPreyReward() {
        return preyReward;
    }

    public boolean isLuckyPillars() {
        return isEnabledInBuild() && category == ModeCategory.LUCKY_PILLARS;
    }

    public boolean isBrickGuard() {
        return isEnabledInBuild() && category == ModeCategory.BRICK_GUARD;
    }

    public boolean isTntRun() {
        return isEnabledInBuild() && this == TNT_RUN;
    }

    public boolean isBlockParty() {
        return isEnabledInBuild() && this == BLOCK_PARTY;
    }

    public boolean isAutoArenaMiniGame() {
        return isLuckyPillars() || isStandaloneMiniGame();
    }

    public boolean isStandaloneMiniGame() {
        return isEnabledInBuild() && (this == TNT_RUN || this == BLOCK_PARTY);
    }

    public boolean isIndependentMode() {
        return isEnabledInBuild() && (category == ModeCategory.LUCKY_PILLARS || category == ModeCategory.BRICK_GUARD || isStandaloneMiniGame());
    }

    public boolean usesHunterFlowMode() {
        return isEnabledInBuild() && (category == ModeCategory.HUNTER || category == ModeCategory.HUNTER_INTERNAL);
    }

    public boolean usesPreySelection() {
        return usesHunterFlowMode();
    }

    public boolean usesLobbyVoteItems() {
        return usesPreySelection();
    }

    public boolean usesWorldSelection() {
        return isEnabledInBuild() && WORLD_SELECTION_MODES.contains(this);
    }

    public boolean usesHunterReconnectSnapshot() {
        return usesHunterFlowMode();
    }

    public boolean isFlashLike() {
        return isEnabledInBuild() && FLASH_LIKE_MODES.contains(this);
    }

    public boolean isDirectFlashStart() {
        return isEnabledInBuild() && DIRECT_FLASH_START_MODES.contains(this);
    }

    public boolean isFlashTournament() {
        return isEnabledInBuild() && this == FLASH_TOURNAMENT;
    }

    public boolean isLegacyRemovedMode() {
        return !isEnabledInBuild();
    }

    public boolean isCommandMode() {
        return isEnabledInBuild() && (category == ModeCategory.HUNTER || category == ModeCategory.LUCKY_PILLARS || category == ModeCategory.BRICK_GUARD);
    }

    public boolean isHunterSectionMode() {
        return isEnabledInBuild() && category == ModeCategory.HUNTER;
    }


    public boolean isHunterCreateMenuMode() {
        return isEnabledInBuild() && HUNTER_CREATE_MENU_MODES.contains(this);
    }

    public boolean isHunterAdminCreateMode() {
        return isEnabledInBuild() && HUNTER_ADMIN_CREATE_MODES.contains(this);
    }

    public boolean isLuckyPillarsSectionMode() {
        return isLuckyPillars();
    }

    public boolean isBrickGuardSectionMode() {
        return isBrickGuard();
    }

    public boolean isMiniGameMapEditableMode() {
        return isLuckyPillars();
    }

    public boolean supportsFastestTimeLeaderboard() {
        return isHunterSectionMode() && this != CUSTOM;
    }

    public static List<GameMode> getCommandModes() {
        return list(GameMode::isCommandMode);
    }

    public static Set<GameMode> getHunterSectionModes() {
        return set(GameMode::isHunterSectionMode);
    }

    public static List<GameMode> getHunterCreateMenuModes() {
        return list(GameMode::isHunterCreateMenuMode);
    }

    public static List<GameMode> getHunterAdminCreateModes() {
        return list(GameMode::isHunterAdminCreateMode);
    }

    public static Set<GameMode> getLuckyPillarsSectionModes() {
        return set(GameMode::isLuckyPillarsSectionMode);
    }

    public static Set<GameMode> getBrickGuardSectionModes() {
        return set(GameMode::isBrickGuardSectionMode);
    }


    public static List<GameMode> getMiniGameMapEditableModes() {
        return list(GameMode::isMiniGameMapEditableMode);
    }

    public static List<GameMode> getEnabledModes() {
        return list(GameMode::isEnabledInBuild);
    }

    public static Optional<GameMode> findByIdStrict(String id) {
        return findByIdIncludingRemoved(id).filter(GameMode::isEnabledInBuild);
    }

    public static Optional<GameMode> findByIdIncludingRemoved(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (GameMode mode : values()) {
            if (mode.id.equalsIgnoreCase(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    public static GameMode fromId(String id) {
        return findByIdStrict(id).orElse(CLASSIC);
    }

    private static List<GameMode> list(Predicate<GameMode> predicate) {
        return Arrays.stream(values()).filter(predicate).toList();
    }

    private static EnumSet<GameMode> set(Predicate<GameMode> predicate) {
        EnumSet<GameMode> result = EnumSet.noneOf(GameMode.class);
        Arrays.stream(values()).filter(predicate).forEach(result::add);
        return result;
    }
}
