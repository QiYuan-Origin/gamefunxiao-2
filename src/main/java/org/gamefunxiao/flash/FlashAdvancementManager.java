package org.gamefunxiao.flash;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.gamefunxiao.GameFunXiao;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class FlashAdvancementManager {

    private static final String CRITERION = "trigger";
    private final GameFunXiao plugin;
    private final NamespacedKey rootKey;
    private final Map<FlashAdvancement, NamespacedKey> keys = new EnumMap<>(FlashAdvancement.class);

    public FlashAdvancementManager(GameFunXiao plugin) {
        this.plugin = plugin;
        this.rootKey = new NamespacedKey(plugin, "flash/root");
        for (FlashAdvancement advancement : FlashAdvancement.values()) {
            keys.put(advancement, new NamespacedKey(plugin, "flash/" + advancement.key()));
        }
    }

    public void loadAdvancements() {
        removeIfLoaded(rootKey);
        for (NamespacedKey key : keys.values()) {
            removeIfLoaded(key);
        }
        load(rootKey, rootJson());
        for (FlashAdvancement advancement : FlashAdvancement.values()) {
            load(keys.get(advancement), advancementJson(advancement));
        }
    }

    public void unloadAdvancements() {
        for (NamespacedKey key : keys.values()) {
            removeIfLoaded(key);
        }
        removeIfLoaded(rootKey);
    }

    public void award(Player player, FlashAdvancement advancement) {
        if (player == null || advancement == null || !player.isOnline()) {
            return;
        }
        awardKey(player, rootKey);
        awardKey(player, keys.get(advancement));
    }

    public void reset(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        for (NamespacedKey key : keys.values()) {
            revokeKey(player, key);
        }
        revokeKey(player, rootKey);
    }

    private boolean awardKey(Player player, NamespacedKey key) {
        Advancement advancement = key == null ? null : Bukkit.getAdvancement(key);
        if (advancement == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone() && progress.getRemainingCriteria().contains(CRITERION)) {
            progress.awardCriteria(CRITERION);
            return true;
        }
        return false;
    }

    private void revokeKey(Player player, NamespacedKey key) {
        Advancement advancement = key == null ? null : Bukkit.getAdvancement(key);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getAwardedCriteria()) {
            progress.revokeCriteria(criterion);
        }
    }

    private void load(NamespacedKey key, String json) {
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);
        } catch (IllegalArgumentException ex) {
            try {
                Bukkit.getUnsafe().loadAdvancement(key, json.replace("\"id\":", "\"item\":"));
            } catch (IllegalArgumentException fallback) {
                plugin.getLogger().warning("闪光成就加载失败 " + key + ": " + fallback.getMessage());
            }
        }
    }

    private void removeIfLoaded(NamespacedKey key) {
        try {
            if (Bukkit.getAdvancement(key) != null) {
                Bukkit.getUnsafe().removeAdvancement(key);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String rootJson() {
        return "{"
                + "\"display\":{"
                + "\"icon\":{\"id\":\"minecraft:oak_planks\"},"
                + "\"title\":{\"text\":\"闪光公式\"},"
                + "\"description\":{\"text\":\"每把闪光对局都会重新点亮的挑战记录\"},"
                + "\"background\":\"minecraft:textures/block/oak_planks.png\","
                + "\"show_toast\":false,"
                + "\"announce_to_chat\":false,"
                + "\"hidden\":false"
                + "},"
                + "\"criteria\":{\"" + CRITERION + "\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"requirements\":[[\"" + CRITERION + "\"]]"
                + "}";
    }

    private String advancementJson(FlashAdvancement advancement) {
        return "{"
                + "\"parent\":\"" + rootKey + "\","
                + "\"display\":{"
                + "\"icon\":{\"id\":\"minecraft:" + advancement.icon().name().toLowerCase(Locale.ROOT) + "\"},"
                + "\"title\":{\"text\":\"" + escape(advancement.title()) + "\"},"
                + "\"description\":{\"text\":\"" + escape(advancement.description()) + "\"},"
                + "\"frame\":\"" + advancement.frame() + "\","
                + "\"show_toast\":true,"
                + "\"announce_to_chat\":true,"
                + "\"hidden\":false"
                + "},"
                + "\"criteria\":{\"" + CRITERION + "\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"requirements\":[[\"" + CRITERION + "\"]]"
                + "}";
    }

    private String escape(String text) {
        return text == null ? "" : text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    public enum FlashAdvancement {
        FIRST_UPGRADE("first_upgrade", "强化物品！", "给任意物品完成一次闪光强化", Material.SMITHING_TABLE, "goal"),
        STABLE_SWORD("stable_sword", "稳定锻造", "在锻造台给剑打上稳定强化", Material.NETHERITE_SWORD, "challenge"),
        TMT_NEARBY("tmt_nearby", "这也算烟花?", "近距离见证一次 TMT 爆炸", Material.TNT, "challenge"),
        CROSSBOW_CHILD("crossbow_child", "版本之子?", "获得一把弩", Material.CROSSBOW, "task"),
        COAL_PICKAXE_SMELT("coal_pickaxe_smelt", "随身高炉", "副手煤炭、主手镐子直接熔炼矿物", Material.BLAST_FURNACE, "goal"),
        SPYGLASS_BURN("spyglass_burn", "视线点火", "用火焰附加望远镜点燃目标", Material.SPYGLASS, "goal"),
        JUKEBOX_DOMAIN("jukebox_domain", "领域展开", "受到一次唱片领域效果", Material.JUKEBOX, "task"),
        SUPER_HAPPY_GHAST("super_happy_ghast", "超级乐魂！", "把失水乐魂养成乐魂", Material.DRIED_GHAST, "challenge"),
        TNT_SKELETON_DEATH("tnt_skeleton_death", "!?TNT?!", "被 TNT 弩小白炸死", Material.TNT_MINECART, "challenge"),
        GOLDEN_APPLE_ARMOR("golden_apple_armor", "血量增加", "给盔甲打上金苹果生命强化", Material.ENCHANTED_GOLDEN_APPLE, "goal"),
        SWORD_WAVE("sword_wave", "按Q释放剑气", "把剑丢出去化成剑气", Material.DIAMOND_SWORD, "goal");

        private final String key;
        private final String title;
        private final String description;
        private final Material icon;
        private final String frame;

        FlashAdvancement(String key, String title, String description, Material icon, String frame) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.frame = frame;
        }

        public String key() {
            return key;
        }

        public String title() {
            return title;
        }

        public String description() {
            return description;
        }

        public Material icon() {
            return icon;
        }

        public String frame() {
            return frame;
        }
    }
}
