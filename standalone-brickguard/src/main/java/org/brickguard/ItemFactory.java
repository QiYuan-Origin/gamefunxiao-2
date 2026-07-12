package org.brickguard;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ItemFactory {
    private final NamespacedKey actionKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey teamKey;
    private final NamespacedKey levelKey;

    ItemFactory(NamespacedKey actionKey, NamespacedKey typeKey) {
        this.actionKey = actionKey;
        this.typeKey = typeKey;
        this.teamKey = new NamespacedKey(actionKey.getNamespace(), "team");
        this.levelKey = new NamespacedKey(actionKey.getNamespace(), "level");
    }

    ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        return named(item, name, lore);
    }

    ItemStack item(Material material, int amount, String name, List<String> lore) {
        ItemStack item = item(material, name, lore);
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return item;
    }

    ItemStack named(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.c(name));
            meta.lore((lore == null ? List.<String>of() : lore).stream().map(Text::c).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    ItemStack action(Material material, String name, List<String> lore, String action) {
        ItemStack item = item(material, name, lore);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action));
        return item;
    }

    ItemStack action(Material material, int amount, String name, List<String> lore, String action) {
        ItemStack item = item(material, amount, name, lore);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action));
        return item;
    }

    ItemStack actionModel(Material material, String name, List<String> lore, String action, Material model) {
        ItemStack item = action(material, name, lore, action);
        item.editMeta(meta -> {
            if (model != null && model != Material.AIR) {
                meta.setItemModel(NamespacedKey.minecraft(model.getKey().getKey()));
            }
        });
        return item;
    }

    ItemStack tool(Material material, String name, List<String> lore, String action, Material model) {
        ItemStack item = action(material, name, lore, action);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "tool");
            if (model != null && model != Material.AIR) {
                meta.setItemModel(NamespacedKey.minecraft(model.getKey().getKey()));
            }
        });
        return item;
    }

    ItemStack gameItem(Material material, int amount, String name, List<String> lore, Team team, String type) {
        ItemStack item = item(material, amount, name, lore);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
            if (team != null) meta.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, team.id);
        });
        return item;
    }

    ItemStack pickaxe(int level) {
        return pickaxe(level, Team.BRICK);
    }

    ItemStack pickaxe(int level, Team team) {
        Material material = switch (Math.max(1, Math.min(5, level))) {
            case 2 -> Material.IRON_PICKAXE;
            case 3 -> Material.GOLDEN_PICKAXE;
            case 4 -> Material.DIAMOND_PICKAXE;
            case 5 -> Material.NETHERITE_PICKAXE;
            default -> Material.IRON_PICKAXE;
        };
        String color = team == Team.NETHER ? "§x§6§6§1§9§0§0" : "§x§e§f§4§d§0§0";
        String title = team == Team.NETHER ? "下界矿稿" : "狐稿";
        String name = color + title + " §8[" + Math.max(1, Math.min(5, level)) + "级]";
        List<String> lore = new ArrayList<>();
        lore.add(team == Team.NETHER ? "§f- §a下界砖队矿稿" : "§f- §a板砖队专用矿稿");
        lore.add("§f- §7靠近核心会慢慢修复耐久");
        lore.add("§8[ 板砖工坊 ]");
        ItemStack item = gameItem(material, 1, name, lore, team, "pickaxe");
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, Math.max(1, Math.min(5, level)));
            if (level >= 2) meta.addEnchant(Enchantment.EFFICIENCY, Math.min(level, 4), true);
            if (level >= 3) meta.addEnchant(Enchantment.UNBREAKING, Math.min(level, 3), true);
            if (level >= 5) meta.addEnchant(Enchantment.FORTUNE, 1, true);
        });
        return item;
    }

    int pickaxeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    String typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    void tag(ItemStack item, String type) {
        if (item == null) return;
        item.editMeta(meta -> meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type));
    }

    ItemStack enchanted(Material material, int amount, String name, List<String> lore, Map<Enchantment, Integer> enchants, Team team, String type) {
        ItemStack item = gameItem(material, amount, name, lore, team, type);
        item.editMeta(meta -> enchants.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true)));
        return item;
    }

    String actionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    boolean isTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "tool".equals(item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING));
    }
}
