package org.gamefunxiao.cosmetics;

import org.bukkit.Material;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 死亡互换专属胜利样式。
 *
 * 这些样式与猎人游戏、幸运之柱分别保存，避免跨玩法串用外观。
 */
public enum DeathSwapVictoryEffect {
    FIREWORKS("fireworks", "默认烟花", Material.FIREWORK_ROCKET, 0,
            "§f- §a经典烟花庆祝，默认拥有",
            "§f- §b适合死亡互换的最后胜利"),
    RED_RIFT("red_rift", "猩红裂隙", Material.REDSTONE, 900,
            "§f- §c红色裂隙从胜者脚下扩散",
            "§f- §7像空间被互换撕开了一道口子"),
    CLOCKWORK("clockwork", "逆时钟", Material.CLOCK, 1050,
            "§f- §6金红环流围绕胜者旋转",
            "§f- §7时间停在最后一次互换之后"),
    END_PORTAL("end_portal", "错位终点", Material.ENDER_EYE, 980,
            "§f- §5终界粒子向四周散开",
            "§f- §7留下短暂的传送残影"),
    SOUL_EXCHANGE("soul_exchange", "灵魂交接", Material.ECHO_SHARD, 1100,
            "§f- §3灵魂火焰盘旋升起",
            "§f- §7像最后一次状态交接"),
    BLOOD_MOON("blood_moon", "猩红月环", Material.RED_DYE, 1250,
            "§f- §c猩红月环在场地中央展开",
            "§f- §7胜利者成为最后的坐标");

    private final String id;
    private final String defaultName;
    private final Material material;
    private final int defaultPrice;
    private final List<String> defaultLore;

    DeathSwapVictoryEffect(String id, String defaultName, Material material, int defaultPrice, String... defaultLore) {
        this.id = id;
        this.defaultName = defaultName;
        this.material = material;
        this.defaultPrice = defaultPrice;
        this.defaultLore = Arrays.asList(defaultLore);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName(GameFunXiao plugin) {
        return plugin.getConfigManager().getConfig().getString(
                "shop.death_swap_victory_effects." + id + ".name", defaultName);
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice(GameFunXiao plugin) {
        return Math.max(0, plugin.getConfigManager().getConfig().getInt(
                "shop.death_swap_victory_effects." + id + ".price", defaultPrice));
    }

    public List<String> getDescription(GameFunXiao plugin) {
        List<String> configured = plugin.getConfigManager().getConfig().getStringList(
                "shop.death_swap_victory_effects." + id + ".lore");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return new ArrayList<>(defaultLore);
    }

    public static DeathSwapVictoryEffect byId(String id) {
        if (id == null || id.isBlank()) {
            return FIREWORKS;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (DeathSwapVictoryEffect effect : values()) {
            if (effect.id.equals(normalized)) {
                return effect;
            }
        }
        return FIREWORKS;
    }
}
