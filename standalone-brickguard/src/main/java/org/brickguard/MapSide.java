package org.brickguard;

import org.bukkit.Material;

enum MapSide {
    LOBBY("等待大厅", "§x§7§D§F§F§C§8", Material.COMPASS),
    BRICK("板砖地图", "§x§f§f§7§c§0§0", Material.BRICKS),
    NETHER("下界砖地图", "§x§6§6§1§9§0§0", Material.NETHER_BRICKS);

    final String display;
    final String color;
    final Material icon;

    MapSide(String display, String color, Material icon) {
        this.display = display;
        this.color = color;
        this.icon = icon;
    }

    static MapSide parse(String raw) {
        String v = raw == null ? "" : raw.toLowerCase();
        if (v.contains("brick") || v.contains("板砖")) return BRICK;
        if (v.contains("nether") || v.contains("下界")) return NETHER;
        return LOBBY;
    }
}
