package org.brickguard;

import org.bukkit.Material;

enum Team {
    BRICK("brick", "板砖队", "§x§f§f§7§c§0§0", Material.BRICK),
    NETHER("nether", "下界砖队", "§x§6§6§1§9§0§0", Material.NETHER_BRICK);

    final String id;
    final String display;
    final String color;
    final Material icon;

    Team(String id, String display, String color, Material icon) {
        this.id = id;
        this.display = display;
        this.color = color;
        this.icon = icon;
    }

    static Team fromAction(String value) {
        return value != null && value.endsWith("nether") ? NETHER : BRICK;
    }
}
