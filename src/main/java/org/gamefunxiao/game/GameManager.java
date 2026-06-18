package org.gamefunxiao.game;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.HunterKillEffect;
import org.gamefunxiao.cosmetics.HunterVictoryEffect;
import org.gamefunxiao.cosmetics.LuckyPillarsVictoryEffect;
import org.gamefunxiao.world.BrickGuardMapManager;
import org.gamefunxiao.world.MiniGameMapManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"deprecation"})
public class GameManager {

    private final GameFunXiao plugin;
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<String, BukkitTask> gameTasks = new HashMap<>();
    private final Map<String, BukkitTask> preGameCountdownTasks = new HashMap<>();
    private final Map<String, BukkitTask> divisionTasks = new HashMap<>();
    private final Map<String, BukkitTask> dualPreyDecisionTasks = new HashMap<>();
    private final Map<String, BossBar> randomCompassBossBars = new HashMap<>();
    private final Map<String, Integer> randomCompassCountdowns = new HashMap<>();
    private final Map<String, BossBar> survivalBossBars = new HashMap<>();
    private final Map<UUID, UUID> luckyPillarsSummonOwners = new HashMap<>();
    private final Map<String, Map<String, BlockData>> tntRunOriginalBlocks = new HashMap<>();
    private final Map<String, Map<String, BlockData>> blockPartyOriginalBlocks = new HashMap<>();
    private final Map<String, Set<String>> tntRunFadingBlocks = new HashMap<>();
    private final Map<String, TntRunMapConfig> tntRunRuntimes = new HashMap<>();
    private final Map<String, BlockPartyRuntime> blockPartyRuntimes = new HashMap<>();
    private final Set<String> thunderStormAppliedRooms = new HashSet<>();
    private static final int RANDOM_COMPASS_INTERVAL_SECONDS = 5 * 60;
    private static final int RANDOM_COMPASS_GLOW_TICKS = 15 * 20;
    private static final int RANDOM_COMPASS_SLOW_TICKS = 10 * 20;
    private static final int RANDOM_COMPASS_HUNGER_TICKS = 15 * 20;
    private static final int SWAP_INTERVAL_SECONDS = 60;
    private static final int NETHER_SCENARIO_VOTE_COMPASS_MODEL = 10011;
    private static final int NETHER_SCENARIO_VOTE_BONUS = 30;
    private static final int DOUBLE_PREY_VOTE_MODEL = 10012;
    private static final int DOUBLE_PREY_REBUT_MODEL = 10013;
    private static final int DOUBLE_PREY_MIN_HUNTERS = 11;
    private static final int DOUBLE_PREY_WORLD_DECISION_SECONDS = 20;
    private static final int FLASH_TRIPLE_PREY_VOTE_MODEL = 10022;
    private static final int END_PREY_KIT_COMPASS_MODEL = 10018;
    private static final int END_PREY_POSITION_COMPASS_MODEL = 10019;
    private static final int END_HUNTER_KIT_COMPASS_MODEL = 10020;
    private static final int END_HUNTER_POSITION_COMPASS_MODEL = 10021;
    private static final int END_CHAPTER_DIVISION_SECONDS = 45;
    private static final int END_CHAPTER_HUNTER_VOTE_BONUS = 22;
    private static final int END_CHAPTER_POSITION_VOTE_BONUS = 18;
    private static final int END_DIMENSION_BRIGHTNESS_TICKS = 20 * 60 * 30;
    private static final int FLASH_STORM_DURATION_TICKS = 20 * 60 * 60;
    private static final long FLASH_STORM_DELAY_MILLIS = 10L * 60L * 1000L;
    private static final int LUCKY_PILLARS_COUNTDOWN_SECONDS = 10;
    private static final int LUCKY_PILLARS_RANDOM_ITEM_INTERVAL_SECONDS = 5;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_INTERVAL_SECONDS = 30;
    private static final int LUCKY_PILLARS_COLUMN_HEIGHT = 64;
    private static final int LUCKY_PILLARS_PLAYER_SPACING = 18;
    private static final int LUCKY_PILLARS_OUTER_RING_LIMIT = 8;
    private static final int LUCKY_PILLARS_MIN_COLUMN_COUNT = 8;
    private static final double LUCKY_PILLARS_SUPPORT_DISC_CHANCE = 1.0D;
    private static final int LUCKY_PILLARS_DROP_ELIMINATION_DISTANCE = 28;
    private static final int LUCKY_PILLARS_ELIMINATION_INVISIBILITY_TICKS = 20 * 60 * 10;
    private static final int LUCKY_PILLARS_PLATFORM_RADIUS = 2;
    private static final int LUCKY_PILLARS_CAGE_HEIGHT = 3;
    private static final int LUCKY_PILLARS_MAX_CAGE_BUILD_Y = 6;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_SUPPLY_DROP_COUNT = 5;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_THUNDER_TARGETS = 3;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_ARROW_VOLLEYS = 4;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_MOON_TICKS = 20 * 15;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_INVISIBILITY_TICKS = 20 * 12;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_FIRST_DELAY_SECONDS = 60;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_MIN_DELAY_SECONDS = 60;
    private static final int LUCKY_PILLARS_RANDOM_EVENT_MAX_DELAY_SECONDS = 150;
    private static final int LUCKY_PILLARS_WIN_POINTS = 20;
    private static final int LUCKY_PILLARS_PARTICIPATE_POINTS = 2;
    private static final int LUCKY_PILLARS_KILL_POINTS = 3;
    private static final int STANDALONE_MINIGAME_COUNTDOWN_SECONDS = 5;
    private static final int STANDALONE_MINIGAME_WIN_POINTS = 20;
    private static final int STANDALONE_MINIGAME_PARTICIPATE_POINTS = 2;
    private static final Set<String> LUCKY_PILLARS_BLOCKED_RANDOM_MATERIALS = Set.of(
            "AIR", "CAVE_AIR", "VOID_AIR"
    );
    private static final List<Material> LUCKY_PILLARS_EQUAL_RANDOM_MATERIALS = Arrays.stream(Material.values())
            .filter(GameManager::isAllowedLuckyPillarsRandomMaterial)
            .toList();
    private static final String[] LUCKY_PILLARS_WEAPON_MATERIALS = {
            "WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD",
            "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "DIAMOND_AXE", "NETHERITE_AXE",
            "BOW", "CROSSBOW", "TRIDENT", "MACE"
    };
    private static final String[] LUCKY_PILLARS_BLOCK_MATERIALS = {
            "OAK_PLANKS", "SPRUCE_PLANKS", "COBBLESTONE", "STONE", "DEEPSLATE",
            "DIRT", "GLASS", "HAY_BLOCK", "SLIME_BLOCK", "LADDER", "SCAFFOLDING"
    };
    private static final String[] LUCKY_PILLARS_UTILITY_MATERIALS = {
            "ENDER_PEARL", "WATER_BUCKET", "LAVA_BUCKET", "FISHING_ROD", "SNOWBALL",
            "EGG", "WIND_CHARGE", "TNT", "FLINT_AND_STEEL", "COBWEB", "GOLDEN_APPLE"
    };
    private static final String[] LUCKY_PILLARS_SPAWN_EGG_MATERIALS = {
            "ZOMBIE_SPAWN_EGG", "SKELETON_SPAWN_EGG", "SPIDER_SPAWN_EGG", "CREEPER_SPAWN_EGG",
            "BLAZE_SPAWN_EGG", "ENDERMAN_SPAWN_EGG", "VINDICATOR_SPAWN_EGG", "RAVAGER_SPAWN_EGG",
            "WARDEN_SPAWN_EGG", "ENDER_DRAGON_SPAWN_EGG"
    };
    private static final String[] LUCKY_PILLARS_FOOD_MATERIALS = {
            "COOKED_BEEF", "COOKED_PORKCHOP", "BREAD", "BAKED_POTATO", "CARROT",
            "APPLE", "GOLDEN_CARROT", "PUMPKIN_PIE"
    };
    private static final String[] LUCKY_PILLARS_ARMOR_MATERIALS = {
            "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS",
            "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS",
            "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS",
            "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS",
            "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
            "SHIELD"
    };

    private record TntRunMapConfig(String id, String displayName, int minPlayers, int maxPlayers,
                                   String shape, int radius, int width, int length, int layers,
                                   int layerSpacing, int disappearDelayTicks, int eliminationY,
                                   int maxGameTimeSeconds) {
    }

    private record BlockPartyMapConfig(String id, String displayName, int minPlayers, int maxPlayers,
                                       String shape, String floorType, String pattern, int size,
                                       List<DyeColor> colors, int initialCountdownSeconds,
                                       double countdownReduceSeconds, int minCountdownSeconds,
                                       int disappearSeconds, boolean music, int eliminationY,
                                       int maxGameTimeSeconds) {
    }

    private record BlockPartyRuntime(BlockPartyMapConfig config, Material targetMaterial, DyeColor targetColor,
                                     int round, int countdownSeconds, boolean clearing) {
    }

    public GameManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    private boolean isTournamentSilent(GameRoom room) {
        return room != null && room.getGameMode().isFlashTournament();
    }

    private void setTournamentAdvancementAnnouncements(GameRoom room, boolean enabled) {
        if (!isTournamentSilent(room)) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            String worldRoomId = plugin.getWorldManager().getRoomIdByWorld(world);
            if (room.getRoomId().equals(worldRoomId)) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, enabled);
            }
        }
        World mainWorld = room.getGameWorld();
        if (mainWorld != null) {
            mainWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, enabled);
        }
    }

    private int getNetherHunterVoteSeconds() {
        return plugin.getConfigManager().getNetherHunterVoteTime();
    }

    private int getEndChapterPreStartSeconds() {
        return plugin.getConfigManager().getEndChapterPreStartCountdown();
    }

    public void syncEndDimensionBrightness(Player player, GameRoom room) {
        if (player == null) {
            return;
        }

        PotionEffect active = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        boolean shouldApply = false;

        if (room != null && player.getWorld() != null) {
            String roomId = plugin.getWorldManager().getRoomIdByWorld(player.getWorld());
            shouldApply = room.getRoomId().equals(roomId) && player.getWorld().getEnvironment() == World.Environment.THE_END;
        }

        if (shouldApply && room.getGameMode() == GameMode.END_FLASH && room.isHunter(player.getUniqueId())) {
            clearManagedEndDimensionBrightness(player);
            return;
        }

        if (shouldApply) {
            if (active == null || active.getDuration() < END_DIMENSION_BRIGHTNESS_TICKS / 2
                    || active.hasParticles() || active.hasIcon()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                        END_DIMENSION_BRIGHTNESS_TICKS, 0, false, false, false));
            }
            return;
        }

        clearManagedEndDimensionBrightness(player);
    }

    public void clearManagedEndDimensionBrightness(Player player) {
        if (player == null) {
            return;
        }

        PotionEffect active = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (active == null) {
            return;
        }

        if (!active.hasParticles() && !active.hasIcon() && active.getAmplifier() == 0
                && active.getDuration() >= END_DIMENSION_BRIGHTNESS_TICKS / 2) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    private String formatChineseDuration(int seconds) {
        if (seconds <= 0) {
            return "0秒";
        }

        int minutes = seconds / 60;
        int remainSeconds = seconds % 60;
        if (minutes <= 0) {
            return remainSeconds + "秒";
        }
        if (remainSeconds <= 0) {
            return minutes + "分钟";
        }
        return minutes + "分" + remainSeconds + "秒";
    }

    public void giveLobbyItems(Player player, GameRoom room) {
        player.getInventory().clear();

        // 设置经验条为0（人数不够时）
        player.setLevel(0);
        player.setExp(0);

        if (room != null && room.getGameMode().isLuckyPillars()) {
            giveLuckyPillarsLobbyItems(player, room);
            return;
        }
        if (room != null && room.getGameMode().isBrickGuard()) {
            plugin.getBrickGuardManager().giveLobbyItems(player, room);
            return;
        }
        if (room != null && room.getGameMode().isStandaloneMiniGame()) {
            giveStandaloneMiniGameLobbyItems(player, room);
            return;
        }
        if (room != null && !usesLobbyVoteItems(room)) {
            giveIndependentCustomLobbyItems(player, room);
            return;
        }

        // 第一格：投票猎物
        ItemStack voteItem = new ItemStack(Material.PAPER);
        ItemMeta voteMeta = voteItem.getItemMeta();
        if (voteMeta != null) {
            voteMeta.setDisplayName("§x§F§F§A§A§5§5🎯 §x§F§F§C§C§7§7投§x§F§F§E§E§9§9票§x§D§D§F§F§9§9猎§x§B§B§F§F§7§7物");
            voteMeta.setCustomModelData(10001);
            voteMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("compass"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e右键选择猎物");
            lore.add("§f- §a投票决定谁是猎物");
            lore.add("§8· · · · · · · · · · · · · ·");
            voteMeta.setLore(lore);
            voteItem.setItemMeta(voteMeta);
        }
        player.getInventory().setItem(0, voteItem);

        if (shouldOfferDoublePreyVote(room)) {
            player.getInventory().setItem(1, createDoublePreyVoteItem(room, player));
        }
        if (plugin.getFlashModeManager().shouldOfferTriplePreyVote(room)) {
            player.getInventory().setItem(2, createFlashTriplePreyVoteItem(room, player));
        }

        // 中间：管理员强制开始（如果是管理员）
        if (player.hasPermission("gamefunxiao.admin") && !room.isAdminForceStartUsed()) {
            ItemStack forceStart = new ItemStack(Material.PAPER);
            ItemMeta forceMeta = forceStart.getItemMeta();
            if (forceMeta != null) {
                forceMeta.setDisplayName("§x§F§F§5§5§5§5⚡ §x§F§F§7§7§7§7强§x§F§F§9§9§9§9制§x§F§F§B§B§B§B开§x§F§F§D§D§D§D始");
                forceMeta.setCustomModelData(10002);
                forceMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("redstone_torch"));
                List<String> lore = new ArrayList<>();
                lore.add("§8· · · · · · · · · · · · · ·");
                lore.add("§f- §c管理员专用");
                lore.add("§f- §e右键强制开始游戏");
                lore.add("§8· · · · · · · · · · · · · ·");
                forceMeta.setLore(lore);
                forceStart.setItemMeta(forceMeta);
            }
            player.getInventory().setItem(4, forceStart);
        }

        // 猎人玩法第一格有投票物品，宣传房间放倒数第2格。
        player.getInventory().setItem(7, createAdvertiseRoomItem("§f- §e右键发送宣传消息", "§f- §7冷却时间: 30秒"));

        // 最后一格：退出游戏
        ItemStack quit = new ItemStack(Material.PAPER);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退§x§F§F§9§9§9§9出§x§F§F§B§B§B§B游§x§F§F§D§D§D§D戏");
            quitMeta.setCustomModelData(10004);
            quitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c右键退出房间");
            lore.add("§8· · · · · · · · · · · · · ·");
            quitMeta.setLore(lore);
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    private void giveStandaloneMiniGameLobbyItems(Player player, GameRoom room) {
        player.getInventory().setItem(0, createAdvertiseRoomItem("§f- §e右键发送宣传消息"));

        if (player.hasPermission("gamefunxiao.admin") && !room.isAdminForceStartUsed()) {
            ItemStack forceStart = new ItemStack(Material.PAPER);
            ItemMeta forceMeta = forceStart.getItemMeta();
            if (forceMeta != null) {
                forceMeta.setDisplayName("§x§F§F§7§7§5§5⚡ §x§F§F§9§9§7§7强§x§F§F§B§B§9§9制§x§F§F§D§D§B§B开始");
                forceMeta.setCustomModelData(10002);
                forceMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("redstone_torch"));
                forceMeta.setLore(List.of(
                        "§8· · · · · · · · · · · · · ·",
                        "§f- §c管理员专用",
                        "§f- §e右键强制开始 " + room.getGameMode().getDisplayName(),
                        "§8· · · · · · · · · · · · · ·"));
                forceStart.setItemMeta(forceMeta);
            }
            player.getInventory().setItem(4, forceStart);
        }

        ItemStack quit = new ItemStack(Material.PAPER);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§9§9§9§9退出游戏");
            quitMeta.setCustomModelData(10004);
            quitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            quitMeta.setLore(List.of("§8· · · · · · · · · · · · · ·", "§f- §c右键退出房间", "§8· · · · · · · · · · · · · ·"));
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    private void giveIndependentCustomLobbyItems(Player player, GameRoom room) {
        player.getInventory().setItem(0, createAdvertiseRoomItem(
                "§f- §e右键发送房间招募",
                "§f- §7不会出现猎物相关提示"));

        if (player.hasPermission("gamefunxiao.admin") && !room.isAdminForceStartUsed()) {
            ItemStack forceStart = new ItemStack(Material.PAPER);
            ItemMeta forceMeta = forceStart.getItemMeta();
            if (forceMeta != null) {
                forceMeta.setDisplayName("§x§F§F§7§7§5§5⚡ §x§F§F§9§9§7§7强§x§F§F§B§B§9§9制§x§F§F§D§D§B§B开始");
                forceMeta.setCustomModelData(10002);
                forceMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("redstone_torch"));
                forceMeta.setLore(List.of(
                        "§8· · · · · · · · · · · · · ·",
                        "§f- §c管理员专用",
                        "§f- §e右键强制开始 " + room.getGameMode().getDisplayName(),
                        "§8· · · · · · · · · · · · · ·"));
                forceStart.setItemMeta(forceMeta);
            }
            player.getInventory().setItem(4, forceStart);
        }

        ItemStack quit = new ItemStack(Material.PAPER);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退§x§F§F§9§9§9§9出§x§F§F§B§B§B§B游戏");
            quitMeta.setCustomModelData(10004);
            quitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            quitMeta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c右键退出房间",
                    "§8· · · · · · · · · · · · · ·"));
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    private void giveLuckyPillarsLobbyItems(Player player, GameRoom room) {
        player.getInventory().setItem(0, createAdvertiseRoomItem(
                "§f- §e右键发送幸运之柱招募",
                "§f- §7冷却时间: 30秒"));

        if (player.hasPermission("gamefunxiao.admin") && !room.isAdminForceStartUsed()) {
            ItemStack forceStart = new ItemStack(Material.PAPER);
            ItemMeta forceMeta = forceStart.getItemMeta();
            if (forceMeta != null) {
                forceMeta.setDisplayName("§x§F§F§7§7§5§5⚡ §x§F§F§9§9§7§7强§x§F§F§B§B§9§9制§x§F§F§D§D§B§B开始");
                forceMeta.setCustomModelData(10002);
                forceMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("redstone_torch"));
                List<String> lore = new ArrayList<>();
                lore.add("§8· · · · · · · · · · · · · ·");
                lore.add("§f- §c管理员专用");
                lore.add("§f- §e右键强制开始幸运之柱经典模式");
                lore.add("§8· · · · · · · · · · · · · ·");
                forceMeta.setLore(lore);
                forceStart.setItemMeta(forceMeta);
            }
            player.getInventory().setItem(4, forceStart);
        }

        ItemStack quit = new ItemStack(Material.PAPER);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退§x§F§F§9§9§9§9出§x§F§F§B§B§B§B游戏");
            quitMeta.setCustomModelData(10004);
            quitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c右键退出房间");
            lore.add("§8· · · · · · · · · · · · · ·");
            quitMeta.setLore(lore);
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    private ItemStack createAdvertiseRoomItem(String... detailLines) {
        ItemStack advertise = new ItemStack(Material.PAPER);
        ItemMeta adMeta = advertise.getItemMeta();
        if (adMeta != null) {
            adMeta.setDisplayName("§x§F§F§D§7§0§0📢 §x§F§F§B§B§3§3宣§x§F§F§9§9§6§6传§x§F§F§7§7§9§9房§x§F§F§5§5§C§C间");
            adMeta.setCustomModelData(10003);
            adMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("bell"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            if (detailLines == null || detailLines.length == 0) {
                lore.add("§f- §e右键发送宣传消息");
            } else {
                for (String line : detailLines) {
                    if (line != null && !line.isBlank()) {
                        lore.add(line);
                    }
                }
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            adMeta.setLore(lore);
            advertise.setItemMeta(adMeta);
        }
        return advertise;
    }

    public void giveSpectatorItems(Player player) {
        player.getInventory().clear();

        // 退出旁观物品（放在第9格）
        ItemStack exitItem = new ItemStack(Material.PAPER);
        ItemMeta exitMeta = exitItem.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退§x§F§F§9§9§9§9出§x§F§F§B§B§B§B旁§x§F§F§D§D§D§D观");
            exitMeta.setCustomModelData(10008);
            exitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c右键/丢出/点击退出旁观");
            lore.add("§8· · · · · · · · · · · · · ·");
            exitMeta.setLore(lore);
            exitItem.setItemMeta(exitMeta);
        }
        player.getInventory().setItem(8, exitItem);
    }

    public void giveEndChapterDivisionItems(Player player, GameRoom room) {
        player.getInventory().clear();
        player.setLevel(room == null ? 0 : room.getEndChapterDivisionCountdown());
        player.setExp(0);

        if (room != null && room.isPrey(player.getUniqueId())) {
            player.getInventory().setItem(1, createEndChapterPreyKitCompass(room, player));
            player.getInventory().setItem(2, createEndChapterPreyPositionCompass(room, player));
        } else {
            player.getInventory().setItem(1, createEndChapterHunterKitCompass(room, player));
            player.getInventory().setItem(2, createEndChapterHunterPositionCompass(room, player));
        }

        ItemStack quit = new ItemStack(Material.PAPER);
        ItemMeta quitMeta = quit.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName("§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7退§x§F§F§9§9§9§9出§x§F§F§B§B§B§B分§x§F§F§D§D§D§D工");
            quitMeta.setCustomModelData(10004);
            quitMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            quitMeta.setLore(Arrays.asList(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c右键退出当前房间",
                    "§8· · · · · · · · · · · · · ·"));
            quit.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(8, quit);
    }

    public void refreshLobbyItems(GameRoom room) {
        if (room == null) {
            return;
        }

        if (room.getState() != RoomState.WAITING && room.getState() != RoomState.STARTING) {
            return;
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                giveLobbyItems(player, room);
            }
        }
    }

    public boolean shouldOfferDoublePreyVote(GameRoom room) {
        return room != null
                && usesPreySelection(room)
                && room.getGameMode() != GameMode.SWAP
                && !room.isDoublePreyEnabled()
                && getDoublePreyRequiredYesVotes(room) > 0
                && getProspectiveHunterCount(room) >= DOUBLE_PREY_MIN_HUNTERS;
    }

    public int getDoublePreyRequiredYesVotes(GameRoom room) {
        if (room == null || !usesPreySelection(room) || room.getGameMode() == GameMode.SWAP) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(getProspectiveHunterCount(room) / 2.0D));
    }

    private int getProspectiveHunterCount(GameRoom room) {
        if (room == null) {
            return 0;
        }

        if (!room.getPreyUUIDs().isEmpty()) {
            return Math.max(0, room.getAllPlayerUUIDs().size() - room.getPreyUUIDs().size());
        }

        return Math.max(0, room.getPlayerCount() - 1);
    }

    private ItemStack createDoublePreyVoteItem(GameRoom room, Player viewer) {
        ItemStack voteItem = new ItemStack(Material.PAPER);
        ItemMeta meta = voteItem.getItemMeta();
        if (meta != null) {
            int currentVotes = room.getDoublePreyVoteCount();
            int requiredVotes = getDoublePreyRequiredYesVotes(room);
            meta.setDisplayName("§x§F§F§8§8§5§5📜 §x§F§F§B§B§6§6双§x§F§F§D§D§8§8猎§x§D§D§F§F§A§A物§x§B§B§F§F§C§C投§x§9§9§F§F§E§E票");
            meta.setCustomModelData(DOUBLE_PREY_VOTE_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("paper"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e当前同意: §6" + currentVotes + " §7/ §e需要: §6" + requiredVotes);
            lore.add("§f- §a猎人超过10人时可发起双猎物");
            lore.add("§f- §d只提供同意投票，不提供反对投票");
            lore.add(viewer != null && room.hasVotedDoublePrey(viewer.getUniqueId())
                    ? "§f- §6你已经投过同意票了"
                    : "§f- §b右键打开菜单投出同意票");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            voteItem.setItemMeta(meta);
        }
        return voteItem;
    }

    private ItemStack createFlashTriplePreyVoteItem(GameRoom room, Player viewer) {
        ItemStack voteItem = new ItemStack(Material.PAPER);
        ItemMeta meta = voteItem.getItemMeta();
        if (meta != null) {
            int currentVotes = room.getFlashTriplePreyVoteCount();
            int requiredVotes = plugin.getFlashModeManager().getTriplePreyRequiredYesVotes(room);
            meta.setDisplayName("§x§F§F§D§7§0§0📜 §x§F§F§B§B§3§3三§x§F§F§9§9§6§6猎§x§F§F§7§7§9§9物§x§F§F§5§5§C§C投§x§F§F§3§3§F§F票");
            meta.setCustomModelData(FLASH_TRIPLE_PREY_VOTE_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("paper"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e当前同意: §6" + currentVotes + " §7/ §e需要: §6" + requiredVotes);
            lore.add("§f- §a闪光模式人数超过32人后可开启第三位猎物");
            lore.add("§f- §d通过后会在第二轮投票中额外选出一位猎物");
            lore.add(viewer != null && room.hasVotedFlashTriplePrey(viewer.getUniqueId())
                    ? "§f- §6你已经投过同意票了"
                    : "§f- §b右键投出同意票");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            voteItem.setItemMeta(meta);
        }
        return voteItem;
    }

    public void handleDoublePreyVote(Player voter, GameRoom room) {
        if (room == null || voter == null) {
            return;
        }

        if (!shouldOfferDoublePreyVote(room)) {
            voter.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_vote_unavailable"));
            voter.playSound(voter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (room.hasVotedDoublePrey(voter.getUniqueId())) {
            voter.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_vote_same"));
            voter.playSound(voter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        room.voteDoublePrey(voter.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", voter.getName());
        placeholders.put("current", String.valueOf(room.getDoublePreyVoteCount()));
        placeholders.put("required", String.valueOf(getDoublePreyRequiredYesVotes(room)));
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_vote_progress", placeholders));

        refreshLobbyItems(room);

        if (room.getDoublePreyVoteCount() >= getDoublePreyRequiredYesVotes(room)) {
            enableDoublePrey(room);
        }
    }

    public void handleFlashTriplePreyVote(Player voter, GameRoom room) {
        if (room == null || voter == null) {
            return;
        }

        if (!plugin.getFlashModeManager().shouldOfferTriplePreyVote(room)) {
            voter.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.flash_triple_prey_vote_unavailable"));
            voter.playSound(voter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (room.hasVotedFlashTriplePrey(voter.getUniqueId())) {
            voter.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.flash_triple_prey_vote_same"));
            voter.playSound(voter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        room.voteFlashTriplePrey(voter.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", voter.getName());
        placeholders.put("current", String.valueOf(room.getFlashTriplePreyVoteCount()));
        placeholders.put("required", String.valueOf(plugin.getFlashModeManager().getTriplePreyRequiredYesVotes(room)));
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.flash_triple_prey_vote_progress", placeholders));
        refreshLobbyItems(room);

        if (room.getFlashTriplePreyVoteCount() >= plugin.getFlashModeManager().getTriplePreyRequiredYesVotes(room)) {
            plugin.getFlashModeManager().enableTriplePrey(room);
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.flash_triple_prey_enabled"));
            refreshLobbyItems(room);
        }
    }

    private void enableDoublePrey(GameRoom room) {
        if (room == null || room.isDoublePreyEnabled()) {
            return;
        }

        UUID lockedFirstPrey = pickFirstDualPrey(room);
        room.setDoublePreyEnabled(true);
        room.setLockedFirstDualPrey(lockedFirstPrey);
        room.clearDoublePreyVotes();
        room.clearPreyVotes();
        if (plugin.getFlashModeManager().isFlashMode(room)) {
            room.setFlashPreyVoteStage(1);
        }

        Map<String, String> placeholders = new HashMap<>();
        String playerName = lockedFirstPrey == null ? "未知" : Bukkit.getOfflinePlayer(lockedFirstPrey).getName();
        placeholders.put("player", playerName == null ? "未知" : playerName);
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_enabled", placeholders));

        refreshLobbyItems(room);
    }

    private UUID pickFirstDualPrey(GameRoom room) {
        List<UUID> selected = selectPreysFromVotes(room, 1, Collections.emptySet());
        return selected.isEmpty() ? null : selected.get(0);
    }

    public void startCountdown(GameRoom room) {
        BukkitTask oldTask = countdownTasks.remove(room.getRoomId());
        if (oldTask != null) {
            oldTask.cancel();
        }
        if (room.getCountdown() <= 0) {
            room.setCountdown(room.getGameMode().isLuckyPillars() ? LUCKY_PILLARS_COUNTDOWN_SECONDS : 10);
        }
        room.setState(RoomState.STARTING);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                int countdown = room.getCountdown();

                if (room.getGameMode().isLuckyPillars() && countdown == 10) {
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.setLevel(0);
                            p.setExp(0);
                        }
                    }
                    cancel();
                    countdownTasks.remove(room.getRoomId());
                    startLuckyPillars(room);
                    return;
                }

                if (countdown <= 0) {
                    if (usesPreySelection(room)) {
                        ensurePreysSelected(room);
                    }

                    // 清空经验条
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.setLevel(0);
                            p.setExp(0);
                        }
                    }

                    if (room.getGameMode().isLuckyPillars()) {
                        startLuckyPillars(room);
                    } else if (room.getGameMode().isBrickGuard()) {
                        plugin.getBrickGuardManager().start(room);
                    } else if (room.getGameMode() == GameMode.TNT_RUN) {
                        startTntRun(room);
                    } else if (room.getGameMode() == GameMode.BLOCK_PARTY) {
                        startBlockParty(room);
                    } else if (room.getGameMode() == GameMode.END_CHAPTER) {
                        startEndChapterDivisionPhase(room);
                    } else if (room.getGameMode().isDirectFlashStart()) {
                        startFlashMode(room);
                    } else if (usesHunterFlow(room)) {
                        // 倒计时结束，直接传送猎物（世界已在3秒前开始生成）
                        startGame(room);
                    } else {
                        startIndependentMode(room);
                    }
                    cancel();
                    return;
                }

                // 剩下3秒时给猎物显示标题并创建世界
                if (countdown == 3) {
                    if (room.getGameMode().isAutoArenaMiniGame() || room.getGameMode().isBrickGuard()) {
                        for (UUID uuid : room.getAllPlayerUUIDs()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                String titleText = room.getGameMode().isLuckyPillars()
                                        ? "§x§F§F§D§D§5§5⏳ §e幸运之柱地图准备中"
                                        : room.getGameMode().isBrickGuard()
                                        ? "§x§F§F§7§C§0§0⏳ §e板砖守卫战地图准备中"
                                        : room.getGameMode() == GameMode.TNT_RUN
                                        ? "§x§F§F§8§8§5§5⏳ §eTNT跑酷地图准备中"
                                        : "§x§D§D§8§8§F§F⏳ §d方块派对舞台准备中";
                                String subText = room.getGameMode().isLuckyPillars()
                                        ? "§7正在选择主题地图并预加载出生点..."
                                        : room.getGameMode().isBrickGuard()
                                        ? "§7正在创建板砖世界与下界砖世界..."
                                        : "§7正在按配置自动生成本局独立地图...";
                                Component titleComp3 = LegacyComponentSerializer.legacySection().deserialize(titleText);
                                Component subComp3 = LegacyComponentSerializer.legacySection().deserialize(subText);
                                p.showTitle(Title.title(titleComp3, subComp3,
                                        Title.Times.times(Duration.ZERO, Duration.ofMillis(3200), Duration.ofMillis(450))));
                                if (!room.getGameMode().isLuckyPillars()) {
                                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.55f, 1.45f);
                                }
                            }
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (room.getGameMode().isLuckyPillars()) {
                                prepareLuckyPillarsWorldBeforeStart(room);
                            } else if (room.getGameMode().isBrickGuard()) {
                                prepareBrickGuardWorldsBeforeStart(room);
                            } else {
                                prepareStandaloneMiniGameWorldBeforeStart(room);
                            }
                        }, 1L);
                    }
                    if (usesPreySelection(room)) {
                        boolean flashMode = room.getGameMode().isDirectFlashStart();
                        for (UUID uuid : room.getPreyUUIDs()) {
                            Player prey = Bukkit.getPlayer(uuid);
                            if (prey != null) {
                                String titleText = flashMode
                                        ? (room.getGameMode() == GameMode.END_FLASH
                                        ? "§x§B§B§8§8§F§F⏳ §x§D§D§A§A§F§F终章末地准备中"
                                        : "§x§F§F§D§7§0§0⏳ §x§F§F§B§B§3§3闪光世界准备中")
                                        : "§x§F§F§D§7§0§0⏳ §x§F§F§B§B§3§3正在传送至选择世界中";
                                String subtitleText = flashMode ? "§7不会进入世界选择，正在直接开局..." : "§7请稍候...";
                                Component titleComp3 = LegacyComponentSerializer.legacySection().deserialize(titleText);
                                Component subComp3 = LegacyComponentSerializer.legacySection().deserialize(subtitleText);
                                Title title3 = Title.title(titleComp3, subComp3,
                                        Title.Times.times(Duration.ZERO, Duration.ofMillis(3500), Duration.ofMillis(500)));
                                if (!isTournamentSilent(room)) {
                                    prey.showTitle(title3);
                                }
                                prey.playSound(prey.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.2f);
                            }
                        }
                    }
                    // 下一tick创建世界，让当前tick先完成渲染，减少卡顿感知
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (room.getGameMode().isAutoArenaMiniGame() || room.getGameMode().isBrickGuard()) {
                            return;
                        }
                        if (room.getGameWorld() == null) {
                            World gameWorld = room.getGameMode() == GameMode.END_FLASH
                                    ? plugin.getWorldManager().createEndFlashWorld(room.getRoomId())
                                    : plugin.getWorldManager().createGameWorld(room.getRoomId());
                            room.setGameWorld(gameWorld);
                        }
                    }, 1L);
                }

                // 更新所有玩家的经验条
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.setLevel(countdown);
                        p.setExp(0);

                        // 如果倒计时到60秒，清除管理员的强制开始按钮
                        if (countdown == 60 && p.hasPermission("gamefunxiao.admin")) {
                            // 清除强制开始按钮（CustomModelData = 10002）
                            for (int i = 0; i < p.getInventory().getSize(); i++) {
                                ItemStack item = p.getInventory().getItem(i);
                                if (item != null && item.getType() == Material.PAPER) {
                                    if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                                        if (item.getItemMeta().getCustomModelData() == 10002) {
                                            p.getInventory().setItem(i, null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 显示倒计时和音效
                if (countdown <= 10 || countdown % 60 == 0) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", formatTime(countdown));
                    String countdownKey = room.getGameMode().isDirectFlashStart()
                            ? (room.getGameMode() == GameMode.END_FLASH ? "game.countdown_end_flash_direct" : "game.countdown_flash_direct")
                            : room.getGameMode().isLuckyPillars()
                            ? "game.lucky_pillars_countdown"
                            : room.getGameMode() == GameMode.TNT_RUN
                            ? "game.tnt_run_countdown"
                            : room.getGameMode() == GameMode.BLOCK_PARTY
                            ? "game.block_party_countdown"
                            : !usesPreySelection(room)
                            ? "game.independent_countdown"
                            : "game.countdown";
                    if (!isTournamentSilent(room)) {
                        if (room.getGameMode().isLuckyPillars()) {
                            room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix(countdownKey, placeholders));
                        } else if (room.getGameMode().isStandaloneMiniGame() || room.getGameMode().isIndependentMode()) {
                            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix(countdownKey, placeholders));
                        } else {
                            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(countdownKey, placeholders));
                        }
                    }

                    // 播放音效
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && !plugin.getPlayerDataManager().isCompactMessages(uuid)) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                // 剩下10秒时给所有人显示主标题
                if (countdown <= 10 && countdown > 3 && !isTournamentSilent(room)) {
                    boolean flashMode = room.getGameMode().isDirectFlashStart();
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            if (plugin.getPlayerDataManager().isCompactMessages(uuid) && countdown != 10) {
                                continue;
                            }
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + countdown + " §6秒");
                            String subtitle = flashMode
                                    ? (room.getGameMode() == GameMode.END_FLASH ? "§7后终章末地直接开局" : "§7后闪光模式直接开局")
                                    : usesWorldSelection(room)
                                    ? "§7后猎物将开始选择游戏世界"
                                    : "§7后将进入独立模式";
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            if (!isTournamentSilent(room)) {
                                p.showTitle(title);
                            }
                        }
                    }
                }

                // 倒计时结束前，给猎人显示"猎物选择世界中"的标题
                if (countdown <= 3 && countdown > 0 && !isTournamentSilent(room)) {
                    if (usesPreySelection(room)) {
                        boolean flashMode = room.getGameMode().isDirectFlashStart();
                        for (UUID uuid : room.getAllPlayerUUIDs()) {
                            if (!room.isPrey(uuid)) {
                                Player hunter = Bukkit.getPlayer(uuid);
                                if (hunter != null) {
                                    Component htComp = LegacyComponentSerializer.legacySection().deserialize("");
                                    String hunterSubtitle = flashMode
                                            ? (room.getGameMode() == GameMode.END_FLASH ? "§7终章末地准备中..." : "§7闪光模式准备中...")
                                            : "§7猎物选择世界中...";
                                    Component hsubComp = LegacyComponentSerializer.legacySection().deserialize(hunterSubtitle);
                                    Title htitle = Title.title(htComp, hsubComp,
                                            Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(200)));
                                    hunter.showTitle(htitle);
                                }
                            }
                        }
                    }
                }

                room.setCountdown(countdown - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdownTasks.put(room.getRoomId(), task);
    }

    public void speedUpCountdown(GameRoom room) {
        if (room.isAdminForceStartUsed()) {
            return;
        }
        room.setAdminForceStartUsed(true);
        clearForceStartItems(room);
        room.setCountdown(60);
        if (room.getState() == RoomState.WAITING) {
            startCountdown(room);
        }
        if (room.getGameMode().isLuckyPillars()) {
            room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.countdown_speedup"));
        } else if (room.getGameMode().isStandaloneMiniGame() || room.getGameMode().isIndependentMode()) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.countdown_speedup"));
        } else {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.countdown_speedup"));
        }
    }

    public void forceStartNow(GameRoom room) {
        if (room.isAdminForceStartUsed()) {
            return;
        }
        room.setAdminForceStartUsed(true);
        clearForceStartItems(room);
        if (usesPreySelection(room)) {
            ensurePreysSelected(room);
        }
        // 跳至10秒
        room.setCountdown(10);
        if (room.getState() == RoomState.WAITING) {
            startCountdown(room);
        }
        if (room.getGameMode().isLuckyPillars()) {
            room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.countdown_force_start"));
        } else if (room.getGameMode().isStandaloneMiniGame() || room.getGameMode().isIndependentMode()) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.countdown_force_start"));
        } else {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.countdown_force_start"));
        }
    }

    public void clearForceStartItems(GameRoom room) {
        if (room == null) {
            return;
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
                    continue;
                }
                if (item.getItemMeta().getCustomModelData() == 10002) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    public void cancelCountdown(GameRoom room) {
        BukkitTask countdownTask = countdownTasks.remove(room.getRoomId());
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // 清空所有玩家的经验条
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setLevel(0);
                p.setExp(0);
            }
        }
    }

    private void ensurePreysSelected(GameRoom room) {
        if (!usesPreySelection(room)) {
            return;
        }
        int preyCount = plugin.getFlashModeManager().getFlashTargetPreyCount(room);
        if (room.getPreyUUIDs().size() >= preyCount) {
            if (room.getGameMode() == GameMode.SWAP && room.getActiveSwapPrey() == null) {
                initializeSwapPreys(room);
            }
            return;
        }

        LinkedHashSet<UUID> selectedPreys = new LinkedHashSet<>(room.getPreyUUIDs());
        if (room.isDoublePreyEnabled() && room.getLockedFirstDualPrey() != null
                && room.getAllPlayerUUIDs().contains(room.getLockedFirstDualPrey())) {
            selectedPreys.add(room.getLockedFirstDualPrey());
        }

        if (selectedPreys.size() < preyCount) {
            selectedPreys.addAll(selectPreysFromVotes(room, preyCount - selectedPreys.size(), selectedPreys));
        }

        for (UUID preyUuid : selectedPreys) {
            room.setPrey(preyUuid);
        }
        if (plugin.getFlashModeManager().isFlashMode(room)) {
            room.setFlashPreyVoteStage(2);
        }

        if (room.getGameMode() == GameMode.SWAP) {
            initializeSwapPreys(room);
        }

        List<String> preyNames = new ArrayList<>();
        for (UUID preyUuid : selectedPreys) {
            String preyName = Bukkit.getOfflinePlayer(preyUuid).getName();
            preyNames.add(preyName == null ? "未知" : preyName);
        }

        Map<String, String> placeholders = new HashMap<>();
        if (selectedPreys.size() == 1) {
            placeholders.put("player", preyNames.get(0));
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.prey_selected", placeholders));
        } else {
            placeholders.put("players", String.join("、", preyNames));
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.prey_selected_multi", placeholders));
        }
    }

    private List<UUID> selectPreysFromVotes(GameRoom room, int preyCount) {
        return selectPreysFromVotes(room, preyCount, Collections.emptySet());
    }

    private List<UUID> selectPreysFromVotes(GameRoom room, int preyCount, Collection<UUID> excludedPlayers) {
        List<UUID> allPlayers = new ArrayList<>(room.getAllPlayerUUIDs());
        if (allPlayers.isEmpty()) {
            return new ArrayList<>();
        }

        Set<UUID> excluded = excludedPlayers == null ? Collections.emptySet() : new HashSet<>(excludedPlayers);
        allPlayers.removeIf(excluded::contains);
        if (allPlayers.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.shuffle(allPlayers);
        Map<UUID, Integer> voteCounts = room.getAllVoteCounts();
        allPlayers.sort((a, b) -> Integer.compare(voteCounts.getOrDefault(b, 0), voteCounts.getOrDefault(a, 0)));

        List<UUID> selected = new ArrayList<>();
        for (UUID uuid : allPlayers) {
            if (selected.size() >= preyCount) {
                break;
            }
            if (voteCounts.getOrDefault(uuid, 0) > 0) {
                selected.add(uuid);
            }
        }

        List<UUID> shuffledPlayers = new ArrayList<>(room.getAllPlayerUUIDs());
        shuffledPlayers.removeIf(excluded::contains);
        Collections.shuffle(shuffledPlayers);
        for (UUID uuid : shuffledPlayers) {
            if (selected.size() >= preyCount) {
                break;
            }
            if (!selected.contains(uuid)) {
                selected.add(uuid);
            }
        }

        return selected;
    }

    private void initializeSwapPreys(GameRoom room) {
        List<UUID> preyUuids = new ArrayList<>(room.getPreyUUIDs());
        if (preyUuids.size() < 2) {
            return;
        }

        Collections.shuffle(preyUuids);
        room.setActiveSwapPrey(preyUuids.get(0));
        room.setCountdownSwapPrey(preyUuids.get(1));
        room.setSwapCountdownSeconds(SWAP_INTERVAL_SECONDS);
        if (room.getState() == RoomState.PLAYING) {
            updateCompassTracking(room);
        }
    }

    private Collection<UUID> getSelectionPreys(GameRoom room) {
        if (!usesPreySelection(room)) {
            return Collections.emptySet();
        }
        if (room.getGameMode() == GameMode.SWAP && room.getState() == RoomState.SELECTING) {
            return room.getPreyUUIDs();
        }
        if (room.getGameMode() == GameMode.SWAP && room.getActiveSwapPrey() != null) {
            return Collections.singleton(room.getActiveSwapPrey());
        }
        return room.getPreyUUIDs();
    }

    private Collection<UUID> getTrackablePreys(GameRoom room) {
        if (!usesPreySelection(room)) {
            return Collections.emptySet();
        }
        if (room.getGameMode() == GameMode.SWAP && room.getActiveSwapPrey() != null) {
            return Collections.singleton(room.getActiveSwapPrey());
        }
        return room.getPreyUUIDs();
    }

    public EndChapterKit getEndChapterHunterKitVote(GameRoom room, UUID uuid) {
        return room == null ? null : room.getEndHunterKitVote(uuid);
    }

    public EndHunterPosition getEndChapterHunterPositionVote(GameRoom room, UUID uuid) {
        return room == null ? null : room.getEndHunterPositionVote(uuid);
    }

    public double getEndChapterHunterKitProbability(GameRoom room, UUID uuid, EndChapterKit kit) {
        if (room == null || uuid == null || kit == null) {
            return 0.0D;
        }
        EndChapterKit vote = room.getEndHunterKitVote(uuid);
        int totalWeight = 0;
        for (EndChapterKit target : EndChapterKit.values()) {
            totalWeight += getEndChapterHunterKitWeight(target, vote);
        }
        return totalWeight <= 0 ? 0.0D : getEndChapterHunterKitWeight(kit, vote) * 100.0D / totalWeight;
    }

    public double getEndChapterHunterPositionProbability(GameRoom room, UUID uuid, EndHunterPosition position) {
        if (room == null || uuid == null || position == null) {
            return 0.0D;
        }
        EndHunterPosition vote = room.getEndHunterPositionVote(uuid);
        int totalWeight = 0;
        for (EndHunterPosition target : EndHunterPosition.values()) {
            totalWeight += getEndChapterHunterPositionWeight(target, vote);
        }
        return totalWeight <= 0 ? 0.0D : getEndChapterHunterPositionWeight(position, vote) * 100.0D / totalWeight;
    }

    private int getEndChapterHunterKitWeight(EndChapterKit kit, EndChapterKit vote) {
        return Math.max(1, kit.getHunterBaseWeight() + (kit == vote ? END_CHAPTER_HUNTER_VOTE_BONUS : 0));
    }

    private int getEndChapterHunterPositionWeight(EndHunterPosition position, EndHunterPosition vote) {
        return Math.max(1, position.getBaseWeight() + (position == vote ? END_CHAPTER_POSITION_VOTE_BONUS : 0));
    }

    public EndChapterKitPreview createEndChapterKitPreview(EndChapterKitRole role, EndChapterKit kit) {
        if (role == null || kit == null) {
            return new EndChapterKitPreview(new ItemStack[36], new ItemStack[4], null);
        }

        List<ItemStack> items = role == EndChapterKitRole.PREY
                ? createEndChapterPreyPreviewItems(kit)
                : createEndChapterHunterPreviewItems(kit);
        ItemStack offHand = extractPreviewOffHand(items);
        ItemStack[] storageContents = new ItemStack[36];

        int index = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (index >= storageContents.length) {
                break;
            }
            storageContents[index++] = item.clone();
        }

        return new EndChapterKitPreview(storageContents, createEndChapterPreviewArmor(role, kit), offHand);
    }

    private void startEndChapterDivisionPhase(GameRoom room) {
        cancelCountdown(room);
        cancelDivisionTask(room);
        room.clearEndChapterDivisionData();
        room.setState(RoomState.SELECTING);
        room.setEndChapterDivisionActive(true);
        room.setEndChapterDivisionCountdown(END_CHAPTER_DIVISION_SECONDS);

        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_division_start"));
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            giveEndChapterDivisionItems(player, room);
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.75f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.65f, 1.5f);
        }

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = END_CHAPTER_DIVISION_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.SELECTING || !room.isEndChapterDivisionActive()) {
                    cancelDivisionTask(room);
                    cancel();
                    return;
                }

                room.setEndChapterDivisionCountdown(timeLeft);
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        continue;
                    }
                    player.setLevel(timeLeft);
                    player.setExp(Math.max(0, Math.min(1, timeLeft / (float) END_CHAPTER_DIVISION_SECONDS)));
                    Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + formatTime(timeLeft));
                    Component subComp = LegacyComponentSerializer.legacySection().deserialize("§x§D§D§5§5§F§F猎物猎人分工中...");
                    player.showTitle(Title.title(titleComp, subComp,
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))));
                }

                if (timeLeft <= 0) {
                    cancelDivisionTask(room);
                    room.setEndChapterDivisionActive(false);
                    room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_division_end"));
                    startGame(room);
                    cancel();
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        divisionTasks.put(room.getRoomId(), task);
    }

    private void cancelDivisionTask(GameRoom room) {
        if (room == null) {
            return;
        }
        BukkitTask task = divisionTasks.remove(room.getRoomId());
        if (task != null) {
            task.cancel();
        }
    }

    private void startFlashMode(GameRoom room) {
        cancelCountdown(room);
        cancelDivisionTask(room);
        room.setEndChapterDivisionActive(false);
        room.clearAssignedEndFlashKitNames();
        plugin.getRoomManager().applyRoleNameTags(room);

        World gameWorld = room.getGameWorld();
        if (gameWorld == null) {
            gameWorld = room.getGameMode() == GameMode.END_FLASH
                    ? plugin.getWorldManager().createEndFlashWorld(room.getRoomId())
                    : plugin.getWorldManager().createGameWorld(room.getRoomId());
            if (gameWorld == null) {
                room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.lobby_creation_failed"));
                return;
            }
            room.setGameWorld(gameWorld);
        }

        Location spawnLoc = gameWorld.getSpawnLocation();
        plugin.getWorldManager().preloadChunks(
                gameWorld,
                spawnLoc.getBlockX() >> 4,
                spawnLoc.getBlockZ() >> 4,
                plugin.getConfigManager().getHunterGamePreloadRadius(),
                null
        );

        String startMessageKey = room.getGameMode() == GameMode.END_FLASH
                ? "game.end_flash_mode_direct_start"
                : "game.flash_mode_direct_start";
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(startMessageKey));
        confirmWorldAndStart(room);
    }

    public void startLuckyPillars(GameRoom room) {
        cancelCountdown(room);
        cancelDivisionTask(room);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        room.clearLuckyPillarsState();
        plugin.getRoomManager().clearEndingChatIsolation(room);
        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(false);
        room.setGameStartTime(0L);

        List<UUID> participants = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                participants.add(uuid);
            }
        }

        if (participants.size() < 2) {
            room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_cancelled"));
            endGameWithoutReward(room);
            return;
        }

        MiniGameMapManager mapManager = plugin.getMiniGameMapManager();
        int preferredPlayers = Math.max(participants.size(),
                room.getMaxPlayers() > 0 ? room.getMaxPlayers() : participants.size());
        MiniGameMapManager.MapDefinition selectedMap = mapManager == null
                ? null
                : mapManager.findPlayableMap(GameMode.LUCKY_PILLARS, preferredPlayers);
        if (selectedMap == null && mapManager != null) {
            selectedMap = mapManager.getMapDefinition(GameMode.LUCKY_PILLARS, "default");
        }
        if (selectedMap == null && mapManager != null) {
            selectedMap = mapManager.ensureMapDefinition(GameMode.LUCKY_PILLARS, "default",
                    Math.max(LUCKY_PILLARS_MIN_COLUMN_COUNT, preferredPlayers));
        }
        int arenaTargetPlayers = Math.max(participants.size(),
                Math.max(room.getMaxPlayers(), selectedMap != null ? selectedMap.maxPlayers() : LUCKY_PILLARS_MIN_COLUMN_COUNT));
        MiniGameMapManager.MapSizeProfile sizeProfile = mapManager == null
                ? null
                : mapManager.resolveSizeProfile(GameMode.LUCKY_PILLARS, arenaTargetPlayers, participants.size());
        World gameWorld = room.getGameWorld();
        if (gameWorld == null) {
            gameWorld = plugin.getWorldManager().createLuckyPillarsWorld(room.getRoomId(), GameMode.LUCKY_PILLARS, selectedMap);
            if (gameWorld == null) {
                room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.lobby_creation_failed"));
                return;
            }
            room.setGameWorld(gameWorld);
        }

        Location spawnLoc = gameWorld.getSpawnLocation();
        plugin.getWorldManager().preloadChunks(
                gameWorld,
                spawnLoc.getBlockX() >> 4,
                spawnLoc.getBlockZ() >> 4,
                Math.max(4, plugin.getConfigManager().getHunterGamePreloadRadius()),
                null
        );

        if (selectedMap != null) {
            String displayMapName = resolveLuckyPillarsMapDisplayName(selectedMap.themeId(), selectedMap.displayName());
            room.setLuckyPillarsRuntimeSettings(
                    selectedMap.mapId(),
                    displayMapName,
                    selectedMap.themeId(),
                    selectedMap.gameTimeSeconds(),
                    selectedMap.randomItemIntervalSeconds(),
                    selectedMap.randomEventIntervalSeconds(),
                    mapManager.getSpectatorSpawn(selectedMap, gameWorld)
            );
        } else {
            room.setLuckyPillarsRuntimeSettings("default", resolveLuckyPillarsMapDisplayName("WOOL", null), "WOOL", 480, 5, 30, null);
        }

        Map<UUID, Location> spawnLocations = selectedMap == null
                ? null
                : assignLuckyPillarsConfiguredSpawns(room, gameWorld, selectedMap, participants);
        if (spawnLocations == null || spawnLocations.size() < participants.size()
                || !hasUsableLuckyPillarsArena(gameWorld, spawnLocations.values())) {
            spawnLocations = buildLuckyPillarsArena(room, gameWorld, participants, sizeProfile);
            if (selectedMap != null && mapManager != null) {
                mapManager.writeDefaultGameTemplateData(selectedMap,
                        room.getLuckyPillarsArenaCenter(),
                        room.getLuckyPillarsEliminationY(),
                        room.getLuckyPillarsBoundaryRadius(),
                        new ArrayList<>(spawnLocations.values()));
            }
        }
        Map<String, String> sizePlaceholders = new HashMap<>();
        sizePlaceholders.put("size", room.getLuckyPillarsMapName());
        sizePlaceholders.put("max", String.valueOf(selectedMap != null
                ? selectedMap.maxPlayers()
                : sizeProfile != null ? sizeProfile.maxPlayers() : Math.max(room.getMaxPlayers(), participants.size())));
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_size_selected", sizePlaceholders));
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_generating"));

        int index = 0;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }

            Location target = spawnLocations.get(uuid);
            if (target == null) {
                target = gameWorld.getSpawnLocation().clone().add(index * 2.0D, 80.0D, 0.0D);
            }
            plugin.getRoomManager().resetPlayerForGameStart(room, player);
            player.teleport(target);
            prepareLuckyPillarsPlayer(player);
            plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(), false, "");
            plugin.getRoomManager().updatePlayerTabNameWithRole(player, room.getRoomId(), false, "");
            index++;
        }

        new BukkitRunnable() {
            int seconds = LUCKY_PILLARS_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    cancel();
                    return;
                }

                if (seconds <= 0) {
                    room.setGameActuallyStarted(true);
                    room.setGameStartTime(System.currentTimeMillis());
                    room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_start"));
                    breakLuckyPillarsCages(room);
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.setLevel(0);
                            player.setExp(0);
                            Component title = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5🍀 §e开始！");
                            Component subtitle = LegacyComponentSerializer.legacySection().deserialize("§f玻璃笼已打开，活到最后的人获胜");
                            player.showTitle(Title.title(title, subtitle,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1600), Duration.ofMillis(350))));
                        }
                    }
                    startLuckyPillarsGuardTask(room);
                    cancel();
                    return;
                }

                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        continue;
                    }
                    player.setLevel(seconds);
                    player.setExp(seconds / (float) LUCKY_PILLARS_COUNTDOWN_SECONDS);
                    if (plugin.getPlayerDataManager().isCompactMessages(uuid) && seconds > 1) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.65f, 1.0f + Math.max(0, seconds) * 0.035f);
                        continue;
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.65f, 1.0f + Math.max(0, seconds) * 0.035f);
                    Component title = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5" + seconds);
                    Component subtitle = LegacyComponentSerializer.legacySection().deserialize("§7玻璃笼倒计时中，准备开打");
                    player.showTitle(Title.title(title, subtitle,
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(150))));
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        plugin.getChildServerManager().syncRoom(room);
    }

    private boolean hasUsableLuckyPillarsArena(World world, Collection<Location> spawns) {
        if (world == null || spawns == null || spawns.isEmpty()) {
            return false;
        }
        for (Location spawn : spawns) {
            if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().equals(world)) {
                return false;
            }
            int spawnX = spawn.getBlockX();
            int spawnY = spawn.getBlockY();
            int spawnZ = spawn.getBlockZ();
            Material floor = world.getBlockAt(spawnX, spawnY - 1, spawnZ).getType();
            if (floor != Material.BEDROCK) {
                return false;
            }

            int bedrockCount = 0;
            int checkMinY = Math.max(world.getMinHeight(), spawnY - LUCKY_PILLARS_COLUMN_HEIGHT);
            for (int y = spawnY - 1; y >= checkMinY; y--) {
                if (world.getBlockAt(spawnX, y, spawnZ).getType() != Material.BEDROCK) {
                    break;
                }
                bedrockCount++;
            }
            if (bedrockCount < LUCKY_PILLARS_COLUMN_HEIGHT) {
                return false;
            }
        }
        return true;
    }

    private Map<UUID, Location> buildLuckyPillarsArena(GameRoom room, World world, List<UUID> participants) {
        return buildLuckyPillarsArena(room, world, participants,
                plugin.getMiniGameMapManager().resolveSizeProfile(room.getGameMode(), room.getMaxPlayers(), participants.size()));
    }

    private Map<UUID, Location> buildLuckyPillarsArena(GameRoom room, World world, List<UUID> participants,
                                                       MiniGameMapManager.MapSizeProfile sizeProfile) {
        Map<UUID, Location> spawnLocations = new LinkedHashMap<>();
        Location center = new Location(world, 0.5D, world.getSpawnLocation().getY(), 0.5D);
        int count = Math.max(LUCKY_PILLARS_MIN_COLUMN_COUNT, participants.size());
        double baseSpacing = sizeProfile == null ? LUCKY_PILLARS_PLAYER_SPACING : sizeProfile.playerSpacing();
        double ringStep = Math.max(7.0D, baseSpacing * 0.55D);
        List<int[]> pillarPositions = new ArrayList<>();
        pillarPositions.add(new int[]{center.getBlockX(), center.getBlockZ(), 1});
        int remaining = count - 1;
        int ringIndex = 0;
        double outermostRadius = 0.0D;
        while (remaining > 0) {
            int ringCapacity = ringIndex == 0 ? 7 : 8 + ringIndex * 4;
            int placeCount = Math.min(remaining, ringCapacity);
            double ringRadius = baseSpacing + ringIndex * ringStep;
            outermostRadius = Math.max(outermostRadius, ringRadius);
            for (int i = 0; i < placeCount; i++) {
                double angle = (Math.PI * 2.0D / placeCount) * i - Math.PI / 2.0D;
                int pillarX = center.getBlockX() + (int) Math.round(Math.cos(angle) * ringRadius);
                int pillarZ = center.getBlockZ() + (int) Math.round(Math.sin(angle) * ringRadius);
                pillarPositions.add(new int[]{pillarX, pillarZ, 0});
            }
            remaining -= placeCount;
            ringIndex++;
        }
        if (outermostRadius <= 0.0D) {
            outermostRadius = baseSpacing;
        }
        int baseY = getLuckyPillarsBaseY(world, center);
        int topY = baseY + LUCKY_PILLARS_COLUMN_HEIGHT;
        int eliminationY = topY - LUCKY_PILLARS_DROP_ELIMINATION_DISTANCE;
        double boundaryRadius = Math.max(outermostRadius + 8.0D,
                sizeProfile == null ? outermostRadius + 10.0D : sizeProfile.boundaryRadius());
        room.setLuckyPillarsArena(new Location(world, center.getX(), topY + 1.0D, center.getZ()), eliminationY, boundaryRadius);
        room.clearLuckyPillarBlocks();
        room.clearLuckyPillarsCageBlocks();
        setupLuckyPillarsWorldBorder(world, center, boundaryRadius);
        clearLuckyPillarsArenaArea(world, center, baseY, topY, (int) Math.ceil(boundaryRadius) + 6);
        buildLuckyPillarsCenterDisc(room, world, center.getBlockX(), baseY - 1, center.getBlockZ(), (int) Math.ceil(outermostRadius) + 4);

        List<Location> pillarSpawns = new ArrayList<>();
        for (int[] pillarData : pillarPositions) {
            int pillarX = pillarData[0];
            int pillarZ = pillarData[1];
            boolean centerPillar = pillarData[2] == 1;
            buildLuckyPillar(room, world, pillarX, baseY, topY, pillarZ, centerPillar);
            int cagePlayerY = topY + 3;
            buildLuckyPillarsGlassCage(room, world, pillarX, cagePlayerY, pillarZ, centerPillar);

            Location spawn = new Location(world,
                    pillarX + 0.5D,
                    cagePlayerY,
                    pillarZ + 0.5D,
                    (float) Math.toDegrees(Math.atan2(center.getZ() - pillarZ, center.getX() - pillarX)) - 90.0F,
                    8.0F);
            pillarSpawns.add(spawn);
        }

        for (int i = 0; i < participants.size(); i++) {
            spawnLocations.put(participants.get(i), pillarSpawns.get(i));
        }

        Location spectator = new Location(world, center.getX(), topY + 12.0D, center.getZ(), 0.0F, 35.0F);
        room.setLuckyPillarsRuntimeSettings(
                room.getLuckyPillarsMapId(),
                room.getLuckyPillarsMapName(),
                room.getLuckyPillarsThemeId(),
                room.getLuckyPillarsGameTimeSeconds(),
                room.getLuckyPillarsRandomItemIntervalSeconds(),
                room.getLuckyPillarsRandomEventIntervalSeconds(),
                spectator
        );
        return spawnLocations;
    }

    private int getLuckyPillarsBaseY(World world, Location center) {
        int desired = Math.max(world.getMinHeight() + 96, 118);
        int maxAllowed = world.getMaxHeight() - LUCKY_PILLARS_COLUMN_HEIGHT - LUCKY_PILLARS_MAX_CAGE_BUILD_Y - 8;
        int minAllowed = world.getMinHeight() + 72;
        return Math.max(minAllowed, Math.min(desired, maxAllowed));
    }

    private void clearLuckyPillarsArenaArea(World world, Location center, int baseY, int topY, int radius) {
        int minY = world.getMinHeight();
        int maxY = Math.min(world.getMaxHeight() - 1, topY + LUCKY_PILLARS_MAX_CAGE_BUILD_Y + 3);
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void setupLuckyPillarsWorldBorder(World world, Location center, double radius) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(Math.max(1536.0D, radius * 8.0D));
        border.setWarningDistance(4);
        border.setDamageBuffer(64.0D);
        border.setDamageAmount(0.0D);
    }

    private void buildLuckyPillar(GameRoom room, World world, int centerX, int baseY, int topY, int centerZ, boolean centerPillar) {
        for (int y = baseY; y <= topY; y++) {
            Location pillarLoc = new Location(world, centerX, y, centerZ);
            pillarLoc.getBlock().setType(Material.BEDROCK, false);
            room.addLuckyPillarBlock(pillarLoc);
        }
        world.getBlockAt(centerX, topY + 1, centerZ).setType(Material.AIR, false);
    }

    private void buildLuckyPillarsCenterDisc(GameRoom room, World world, int centerX, int y, int centerZ, int radius) {
        int discY = Math.max(world.getMinHeight() + 6, y);
        Material[] palette = chooseLuckyPillarsDiscPalette(room);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius + 0.25D) {
                    continue;
                }
                Location discLoc = new Location(world, centerX + x, discY, centerZ + z);
                Material material = pickLuckyPillarsDiscMaterial(palette, x, z, distance, radius);
                discLoc.getBlock().setType(material, false);
                room.addLuckyPillarBlock(discLoc);
            }
        }
    }

    private Material[] chooseLuckyPillarsDiscPalette(GameRoom room) {
        String themeId = room == null ? "WOOL" : room.getLuckyPillarsThemeId();
        return switch (themeId == null ? "WOOL" : themeId.trim().toUpperCase(Locale.ROOT)) {
            case "NETHER" -> new Material[]{
                    Material.NETHERRACK, Material.NETHER_BRICKS, Material.CRIMSON_NYLIUM, Material.MAGMA_BLOCK, Material.SHROOMLIGHT
            };
            case "GLASS" -> new Material[]{
                    Material.WHITE_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.CYAN_STAINED_GLASS,
                    Material.BLUE_STAINED_GLASS, Material.SEA_LANTERN
            };
            case "VOID" -> new Material[]{
                    Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.PURPLE_CONCRETE, Material.BLACKSTONE, Material.END_STONE
            };
            case "TNT" -> new Material[]{
                    Material.RED_CONCRETE, Material.TNT, Material.RED_SANDSTONE, Material.ORANGE_CONCRETE, Material.GOLD_BLOCK
            };
            case "TRAPDOOR" -> new Material[]{
                    Material.OAK_PLANKS, Material.SPRUCE_TRAPDOOR, Material.DARK_OAK_PLANKS, Material.BARREL, Material.LANTERN
            };
            case "OCEAN" -> new Material[]{
                    Material.PRISMARINE, Material.DARK_PRISMARINE, Material.WARPED_WART_BLOCK, Material.SEA_LANTERN, Material.HEART_OF_THE_SEA
            };
            case "MOON" -> new Material[]{
                    Material.END_STONE, Material.SMOOTH_BASALT, Material.CALCITE, Material.PURPUR_BLOCK, Material.OCHRE_FROGLIGHT
            };
            default -> new Material[]{
                    Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.YELLOW_WOOL, Material.GLOWSTONE
            };
        };
    }

    private String resolveLuckyPillarsMapDisplayName(String themeId, String fallback) {
        String resolved = switch (themeId == null ? "WOOL" : themeId.trim().toUpperCase(Locale.ROOT)) {
            case "NETHER" -> "下界圆盘";
            case "GLASS" -> "玻璃圆盘";
            case "VOID" -> "虚空圆盘";
            case "TNT" -> "爆裂圆盘";
            case "TRAPDOOR" -> "活板圆盘";
            case "OCEAN" -> "海洋圆盘";
            case "MOON" -> "月球圆盘";
            default -> "羊毛圆盘";
        };
        if (fallback != null && !fallback.isBlank() && !fallback.contains("默认地图")) {
            return resolved;
        }
        return resolved;
    }

    private Material pickLuckyPillarsDiscMaterial(Material[] palette, int x, int z, double distance, int radius) {
        if (distance >= radius - 0.65D) {
            return palette[1];
        }
        if (Math.abs(x) == Math.abs(z) || x == 0 || z == 0) {
            return palette[2];
        }
        if (((x * x + z * z) % 17) == 0 || (Math.abs(x) + Math.abs(z)) % 11 == 0) {
            return palette[3];
        }
        if (((x * 31 + z * 17) & 15) == 0) {
            return palette[4];
        }
        return palette[0];
    }

    private void buildLuckyPillarsGlassCage(GameRoom room, World world, int centerX, int playerY, int centerZ, boolean centerPillar) {
        int[][] offsets = {
                {0, -2, 0},
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {0, 2, 0}
        };
        for (int[] offset : offsets) {
            Location blockLoc = new Location(world, centerX + offset[0], playerY + offset[1], centerZ + offset[2]);
            blockLoc.getBlock().setType(Material.GLASS, false);
            room.addLuckyPillarsCageBlock(blockLoc);
        }
    }

    private void breakLuckyPillarsCages(GameRoom room) {
        if (room == null || room.getGameWorld() == null) {
            return;
        }
        World world = room.getGameWorld();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.72f, 1.0f);
            }
        }
        for (Location location : room.getLuckyPillarsCageBlockLocations()) {
            if (location == null || location.getWorld() == null || !location.getWorld().equals(world)) {
                continue;
            }
            Block block = location.getBlock();
            if (block.getType() == Material.AIR) {
                continue;
            }
            world.spawnParticle(Particle.BLOCK, location.clone().add(0.5D, 0.5D, 0.5D), 10, 0.28D, 0.28D, 0.28D,
                    Bukkit.createBlockData(block.getType()));
            block.setType(Material.AIR, false);
        }
        room.clearLuckyPillarsCageBlocks();
    }
    private void prepareLuckyPillarsPlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGravity(true);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(12.0F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (LUCKY_PILLARS_COUNTDOWN_SECONDS + 2) * 20, 4, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (LUCKY_PILLARS_COUNTDOWN_SECONDS + 2) * 20, 0, false, false, false));
        player.updateInventory();
    }

    public void prepareLuckyPillarsArenaBeforeStart(GameRoom room) {
        if (room == null || !room.getGameMode().isLuckyPillars()) {
            return;
        }
        World world = room.getGameWorld();
        if (world == null) {
            MiniGameMapManager mapManager = plugin.getMiniGameMapManager();
            MiniGameMapManager.MapDefinition selectedMap = mapManager == null
                    ? null
                    : mapManager.findPlayableMap(GameMode.LUCKY_PILLARS, Math.max(LUCKY_PILLARS_MIN_COLUMN_COUNT, room.getMaxPlayers()));
            world = plugin.getWorldManager().createLuckyPillarsWorld(room.getRoomId(), room.getGameMode(), selectedMap);
            if (world == null) {
                return;
            }
            room.setGameWorld(world);
        }
        plugin.getWorldManager().preloadChunks(world, 0, 0,
                Math.max(4, plugin.getConfigManager().getHunterGamePreloadRadius()), null);

        int previewCount = Math.max(LUCKY_PILLARS_MIN_COLUMN_COUNT, Math.max(2, room.getMaxPlayers()));
        List<UUID> previewPlayers = new ArrayList<>();
        for (int i = 0; i < previewCount; i++) {
            previewPlayers.add(new UUID(0L, i + 1L));
        }
        MiniGameMapManager mapManager = plugin.getMiniGameMapManager();
        MiniGameMapManager.MapSizeProfile sizeProfile = mapManager == null
                ? null
                : mapManager.resolveSizeProfile(GameMode.LUCKY_PILLARS, Math.max(previewCount, room.getMaxPlayers()), previewCount);
        buildLuckyPillarsArena(room, world, previewPlayers, sizeProfile);
    }

    private void prepareLuckyPillarsWorldBeforeStart(GameRoom room) {
        if (room == null || !room.getGameMode().isLuckyPillars() || room.getGameWorld() != null) {
            return;
        }
        prepareLuckyPillarsArenaBeforeStart(room);
    }

    private void prepareStandaloneMiniGameWorldBeforeStart(GameRoom room) {
        if (room == null || (!room.getGameMode().isStandaloneMiniGame()) || room.getGameWorld() != null) {
            return;
        }
        MiniGameMapManager.MapDefinition definition = null;
        World world = plugin.getWorldManager().createLuckyPillarsWorld(room.getRoomId(), room.getGameMode(), definition);
        if (world == null) {
            return;
        }
        room.setGameWorld(world);
        plugin.getWorldManager().preloadChunks(world, 0, 0,
                Math.max(3, plugin.getConfigManager().getHunterGamePreloadRadius()), null);
    }

    private void prepareBrickGuardWorldsBeforeStart(GameRoom room) {
        if (room == null || !room.getGameMode().isBrickGuard()) {
            return;
        }
        World brickWorld = room.getGameWorld();
        World netherBrickWorld = plugin.getWorldManager().getNetherWorld(room.getRoomId());
        if (brickWorld != null && netherBrickWorld != null) {
            return;
        }

        BrickGuardMapManager.MapDefinition definition = resolveBrickGuardMap(room, Math.max(2, room.getPlayerCount()));
        BrickGuardMapManager.RuntimeWorlds worlds = plugin.getWorldManager().createBrickGuardWorlds(room.getRoomId(), definition);
        if (worlds.brickWorld() != null) {
            room.setGameWorld(worlds.brickWorld());
        }
        applyBrickGuardRuntimeSettings(room, definition, worlds);
    }

    public void startBrickGuard(GameRoom room) {
        if (room == null || !room.getGameMode().isBrickGuard()) {
            return;
        }

        cancelCountdown(room);
        cancelDivisionTask(room);
        room.setEndChapterDivisionActive(false);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();

        List<UUID> participants = onlineRoomParticipants(room);
        if (participants.size() < plugin.getRoomManager().getMinimumPlayersForMode(GameMode.BRICK_GUARD)) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.countdown_cancelled"));
            endGameWithoutReward(room);
            return;
        }

        BrickGuardMapManager.MapDefinition definition = resolveBrickGuardMap(room, participants.size());
        if (definition == null) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix(
                    "room.brick_guard_map_not_found",
                    Map.of("map", room.getBrickGuardMapId())));
            endGameWithoutReward(room);
            return;
        }

        BrickGuardMapManager.RuntimeWorlds worlds = new BrickGuardMapManager.RuntimeWorlds(
                room.getGameWorld(),
                plugin.getWorldManager().getNetherWorld(room.getRoomId()));
        if (!worlds.complete()) {
            worlds = plugin.getWorldManager().createBrickGuardWorlds(room.getRoomId(), definition);
        }
        if (!worlds.complete()) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.brick_guard_world_failed"));
            endGameWithoutReward(room);
            return;
        }

        room.setGameWorld(worlds.brickWorld());
        applyBrickGuardRuntimeSettings(room, definition, worlds);
        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(true);
        room.setPreyStarted(true);
        room.setGameStartTime(System.currentTimeMillis());
        plugin.getRoomManager().clearAllRoleNameTags(room);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("map", definition.displayName());
        placeholders.put("radius", String.valueOf((int) Math.round(definition.fakeBorderRadius())));
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.brick_guard_map_selected", placeholders));

        teleportBrickGuardParticipants(room, participants, worlds);
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.brick_guard_start"));
        startIndependentModeGuardTask(room);
        plugin.getChildServerManager().syncRoom(room);
    }

    private BrickGuardMapManager.MapDefinition resolveBrickGuardMap(GameRoom room, int playerCount) {
        if (room == null || plugin.getBrickGuardMapManager() == null) {
            return null;
        }
        BrickGuardMapManager.MapDefinition current = plugin.getBrickGuardMapManager().getMapDefinition(room.getBrickGuardMapId());
        if (current != null && current.enabled()
                && current.minPlayers() <= playerCount
                && current.maxPlayers() >= playerCount) {
            return current;
        }
        BrickGuardMapManager.MapDefinition usable = plugin.getBrickGuardMapManager().findUsableMap(playerCount);
        return usable == null ? current : usable;
    }

    private void applyBrickGuardRuntimeSettings(GameRoom room, BrickGuardMapManager.MapDefinition definition,
                                                BrickGuardMapManager.RuntimeWorlds worlds) {
        if (room == null || definition == null || worlds == null || worlds.brickWorld() == null) {
            return;
        }
        room.setBrickGuardRuntimeSettings(
                definition.mapId(),
                definition.displayName(),
                plugin.getBrickGuardMapManager().getBrickSpawn(definition, worlds.brickWorld()),
                plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, worlds.netherBrickWorld()),
                plugin.getBrickGuardMapManager().getBrickCore(definition, worlds.brickWorld()),
                plugin.getBrickGuardMapManager().getFakeBorderCenter(definition, worlds.brickWorld()),
                definition.fakeBorderRadius()
        );
    }

    private void teleportBrickGuardParticipants(GameRoom room, List<UUID> participants,
                                                BrickGuardMapManager.RuntimeWorlds worlds) {
        Location brickSpawn = room.getBrickGuardBrickSpawn();
        Location netherSpawn = room.getBrickGuardNetherBrickSpawn();
        if (brickSpawn == null && worlds.brickWorld() != null) {
            brickSpawn = worlds.brickWorld().getSpawnLocation();
        }
        if (netherSpawn == null && worlds.netherBrickWorld() != null) {
            netherSpawn = worlds.netherBrickWorld().getSpawnLocation();
        }

        int index = 0;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            boolean brickTeam = index % 2 == 0;
            Location target = (brickTeam ? brickSpawn : netherSpawn);
            plugin.getRoomManager().resetPlayerForGameStart(room, player);
            player.getInventory().clear();
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setLevel(0);
            player.setExp(0.0F);
            player.setFireTicks(0);
            player.setFallDistance(0.0F);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            if (target != null) {
                player.teleport(target);
            }

            String roleName = brickTeam ? "板砖" : "下界砖";
            plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(), false, roleName);
            plugin.getRoomManager().updatePlayerTabNameWithRole(player, room.getRoomId(), false, roleName);
            player.playSound(player.getLocation(), brickTeam ? Sound.BLOCK_STONE_PLACE : Sound.BLOCK_NETHER_BRICKS_PLACE, 0.75f, 1.15f);
            player.showTitle(Title.title(
                    LegacyComponentSerializer.legacySection().deserialize(brickTeam ? "§x§F§F§7§C§0§0板砖队" : "§x§6§6§1§9§0§0下界砖队"),
                    LegacyComponentSerializer.legacySection().deserialize("§f破坏对面的核心即可获胜"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1800), Duration.ofMillis(300))));
            index++;
        }
    }

    public void startTntRun(GameRoom room) {
        cancelCountdown(room);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        room.clearMiniGameState();
        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(false);
        room.setGameStartTime(0L);

        List<UUID> participants = onlineRoomParticipants(room);
        if (participants.size() < plugin.getRoomManager().getMinimumPlayersForMode(GameMode.TNT_RUN)) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.tnt_run_cancelled"));
            endGameWithoutReward(room);
            return;
        }

        TntRunMapConfig config = resolveTntRunMap(participants.size());
        World world = ensureStandaloneGameWorld(room);
        if (world == null) {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.lobby_creation_failed"));
            return;
        }

        Map<UUID, Location> spawns = buildTntRunArena(room, world, config, participants);
        broadcastMiniGameMapSelected(room, "game.tnt_run_map_selected", config.displayName(), config.maxGameTimeSeconds());
        teleportStandaloneParticipants(room, participants, spawns, "跑酷者");

        new BukkitRunnable() {
            int seconds = STANDALONE_MINIGAME_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    room.setGameActuallyStarted(true);
                    room.setGameStartTime(System.currentTimeMillis());
                    room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.tnt_run_start"));
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.setLevel(0);
                            player.setExp(0);
                            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.35f);
                            player.showTitle(Title.title(
                                    LegacyComponentSerializer.legacySection().deserialize("§x§F§F§8§8§5§5✹ §e开跑！"),
                                    LegacyComponentSerializer.legacySection().deserialize("§f脚下方块会消失，别停下来"),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1600), Duration.ofMillis(300))));
                        }
                    }
                    startTntRunGuardTask(room, config);
                    cancel();
                    return;
                }
                showStandaloneStartCountdown(room, "§x§F§F§8§8§5§5", seconds, "§7站稳，倒计时结束立刻开跑");
                seconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        plugin.getChildServerManager().syncRoom(room);
    }

    private Map<UUID, Location> assignLuckyPillarsConfiguredSpawns(GameRoom room, World world,
                                                                   MiniGameMapManager.MapDefinition definition,
                                                                   List<UUID> participants) {
        if (room == null || world == null || definition == null || participants == null || participants.isEmpty()) {
            return null;
        }
        if (definition.gameSpawns() == null || definition.gameSpawns().size() < participants.size()) {
            return null;
        }

        List<Location> spawns = new ArrayList<>();
        for (MiniGameMapManager.LocationSpec spec : definition.gameSpawns()) {
            if (spec == null) {
                continue;
            }
            Location location = spec.toLocation(world);
            if (location != null) {
                spawns.add(location);
            }
        }
        if (spawns.size() < participants.size()) {
            return null;
        }

        plugin.getMiniGameMapManager().applyLuckyPillarsArena(room, definition, world, spawns);
        Map<UUID, Location> result = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            result.put(participants.get(i), spawns.get(i).clone());
        }
        return result;
    }

    public void startBlockParty(GameRoom room) {
        cancelCountdown(room);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        room.clearMiniGameState();
        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(false);
        room.setGameStartTime(0L);

        List<UUID> participants = onlineRoomParticipants(room);
        if (participants.size() < plugin.getRoomManager().getMinimumPlayersForMode(GameMode.BLOCK_PARTY)) {
            room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.block_party_cancelled"));
            endGameWithoutReward(room);
            return;
        }

        BlockPartyMapConfig config = resolveBlockPartyMap(participants.size());
        World world = ensureStandaloneGameWorld(room);
        if (world == null) {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.lobby_creation_failed"));
            return;
        }

        Map<UUID, Location> spawns = buildBlockPartyArena(room, world, config, participants);
        broadcastMiniGameMapSelected(room, "game.block_party_map_selected", config.displayName(), config.maxGameTimeSeconds());
        teleportStandaloneParticipants(room, participants, spawns, "舞者");

        new BukkitRunnable() {
            int seconds = STANDALONE_MINIGAME_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    room.setGameActuallyStarted(true);
                    room.setGameStartTime(System.currentTimeMillis());
                    room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.block_party_start"));
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.setLevel(0);
                            player.setExp(0);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.95f, 1.4f);
                            player.showTitle(Title.title(
                                    LegacyComponentSerializer.legacySection().deserialize("§x§D§D§8§8§F§F▣ §d派对开始！"),
                                    LegacyComponentSerializer.legacySection().deserialize("§f看清目标颜色，站到正确方块上"),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1800), Duration.ofMillis(350))));
                        }
                    }
                    startBlockPartyLoop(room, config);
                    cancel();
                    return;
                }
                showStandaloneStartCountdown(room, "§x§D§D§8§8§F§F", seconds, "§7等会标题和手中方块会提示目标颜色");
                seconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        plugin.getChildServerManager().syncRoom(room);
    }

    private List<UUID> onlineRoomParticipants(GameRoom room) {
        List<UUID> participants = new ArrayList<>();
        if (room == null) {
            return participants;
        }
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                participants.add(uuid);
            }
        }
        return participants;
    }

    private World ensureStandaloneGameWorld(GameRoom room) {
        World world = room.getGameWorld();
        if (world != null) {
            return world;
        }
        prepareStandaloneMiniGameWorldBeforeStart(room);
        return room.getGameWorld();
    }

    private void broadcastMiniGameMapSelected(GameRoom room, String path, String mapName, int maxSeconds) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("map", mapName);
        placeholders.put("time", formatChineseDuration(maxSeconds));
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix(path, placeholders));
    }

    private void teleportStandaloneParticipants(GameRoom room, List<UUID> participants,
                                                Map<UUID, Location> spawns, String roleName) {
        int index = 0;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Location target = spawns.get(uuid);
            if (target == null && room.getGameWorld() != null) {
                target = room.getGameWorld().getSpawnLocation().clone().add(index * 1.5D, 2.0D, 0.0D);
            }
            plugin.getRoomManager().resetPlayerForGameStart(room, player);
            player.getInventory().clear();
            if (target != null) {
                player.teleport(target);
            }
            prepareStandaloneMiniGamePlayer(player);
            plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(), false, roleName);
            plugin.getRoomManager().updatePlayerTabNameWithRole(player, room.getRoomId(), false, roleName);
            index++;
        }
    }

    private boolean usesPreySelection(GameRoom room) {
        return room != null && room.getGameMode().usesPreySelection();
    }

    private boolean usesWorldSelection(GameRoom room) {
        return room != null && room.getGameMode().usesWorldSelection();
    }

    private boolean usesLobbyVoteItems(GameRoom room) {
        return room != null && room.getGameMode().usesLobbyVoteItems();
    }

    private boolean isIndependentMode(GameRoom room) {
        return room != null && room.getGameMode().isIndependentMode();
    }

    private boolean usesHunterFlow(GameRoom room) {
        return room != null && room.getGameMode().usesHunterFlowMode();
    }

    private void prepareStandaloneMiniGamePlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGravity(true);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (STANDALONE_MINIGAME_COUNTDOWN_SECONDS + 2) * 20, 4, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (STANDALONE_MINIGAME_COUNTDOWN_SECONDS + 2) * 20, 0, false, false, false));
        player.updateInventory();
    }

    private void showStandaloneStartCountdown(GameRoom room, String color, int seconds, String subtitle) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.setLevel(seconds);
            player.setExp(seconds / (float) STANDALONE_MINIGAME_COUNTDOWN_SECONDS);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.78f, 1.0f + seconds * 0.08f);
            player.showTitle(Title.title(
                    LegacyComponentSerializer.legacySection().deserialize(color + seconds),
                    LegacyComponentSerializer.legacySection().deserialize(subtitle),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(150))));
        }
    }

    private TntRunMapConfig resolveTntRunMap(int playerCount) {
        FileConfiguration minigames = plugin.getConfigManager().getConfig("minigames");
        ConfigurationSection mapsSection = minigames == null ? null : minigames.getConfigurationSection("tnt_run.maps");
        String activeId = minigames == null ? "default" : minigames.getString("tnt_run.active_map", "default");
        TntRunMapConfig fallback = new TntRunMapConfig("default", "经典圆形跑酷",
                2, 16, "circle", 22, 42, 42, 4, 7, 8, 70, 420);
        if (mapsSection == null) {
            return fallback;
        }

        TntRunMapConfig selected = null;
        TntRunMapConfig firstEnabled = null;
        for (String key : mapsSection.getKeys(false)) {
            ConfigurationSection section = mapsSection.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            TntRunMapConfig candidate = new TntRunMapConfig(
                    key,
                    section.getString("display_name", key),
                    Math.max(2, section.getInt("min_players", 2)),
                    Math.max(2, section.getInt("max_players", 16)),
                    section.getString("shape", "circle"),
                    Math.max(8, section.getInt("radius", 22)),
                    Math.max(15, section.getInt("width", 42)),
                    Math.max(15, section.getInt("length", 42)),
                    Math.max(2, section.getInt("layers", 4)),
                    Math.max(4, section.getInt("layer_spacing", 7)),
                    Math.max(1, section.getInt("disappear_delay_ticks", 8)),
                    section.getInt("elimination_y", 70),
                    Math.max(60, section.getInt("max_game_time_seconds", 420))
            );
            if (firstEnabled == null) {
                firstEnabled = candidate;
            }
            if (candidate.id().equalsIgnoreCase(activeId)) {
                selected = candidate;
            }
            if (playerCount >= candidate.minPlayers() && playerCount <= candidate.maxPlayers()) {
                if (candidate.id().equalsIgnoreCase(activeId)) {
                    selected = candidate;
                    break;
                }
                if (selected == null) {
                    selected = candidate;
                }
            }
        }
        return selected != null ? selected : (firstEnabled != null ? firstEnabled : fallback);
    }

    private BlockPartyMapConfig resolveBlockPartyMap(int playerCount) {
        FileConfiguration minigames = plugin.getConfigManager().getConfig("minigames");
        ConfigurationSection mapsSection = minigames == null ? null : minigames.getConfigurationSection("block_party.maps");
        String activeId = minigames == null ? "default" : minigames.getString("block_party.active_map", "default");
        BlockPartyMapConfig fallback = new BlockPartyMapConfig("default", "彩虹羊毛舞台",
                2, 20, "square", "wool", "random", 29,
                List.of(DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.WHITE),
                8, 0.45D, 3, 3, true, 72, 420);
        if (mapsSection == null) {
            return fallback;
        }

        BlockPartyMapConfig selected = null;
        BlockPartyMapConfig firstEnabled = null;
        for (String key : mapsSection.getKeys(false)) {
            ConfigurationSection section = mapsSection.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            List<DyeColor> colors = new ArrayList<>();
            for (String raw : section.getStringList("colors")) {
                try {
                    colors.add(DyeColor.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (colors.isEmpty()) {
                colors = new ArrayList<>(fallback.colors());
            }
            BlockPartyMapConfig candidate = new BlockPartyMapConfig(
                    key,
                    section.getString("display_name", key),
                    Math.max(2, section.getInt("min_players", 2)),
                    Math.max(2, section.getInt("max_players", 20)),
                    section.getString("shape", "square"),
                    section.getString("floor_type", "wool"),
                    section.getString("pattern", "random"),
                    Math.max(13, section.getInt("size", 29)),
                    List.copyOf(colors),
                    Math.max(3, section.getInt("initial_countdown_seconds", 8)),
                    Math.max(0.1D, section.getDouble("countdown_reduce_seconds", 0.45D)),
                    Math.max(2, section.getInt("min_countdown_seconds", 3)),
                    Math.max(1, section.getInt("disappear_seconds", 3)),
                    section.getBoolean("music", true),
                    section.getInt("elimination_y", 72),
                    Math.max(60, section.getInt("max_game_time_seconds", 420))
            );
            if (firstEnabled == null) {
                firstEnabled = candidate;
            }
            if (candidate.id().equalsIgnoreCase(activeId)) {
                selected = candidate;
            }
            if (playerCount >= candidate.minPlayers() && playerCount <= candidate.maxPlayers()) {
                if (candidate.id().equalsIgnoreCase(activeId)) {
                    selected = candidate;
                    break;
                }
                if (selected == null) {
                    selected = candidate;
                }
            }
        }
        return selected != null ? selected : (firstEnabled != null ? firstEnabled : fallback);
    }

    private Map<UUID, Location> buildTntRunArena(GameRoom room, World world, TntRunMapConfig config, List<UUID> players) {
        Map<UUID, Location> spawns = new LinkedHashMap<>();
        Map<String, BlockData> originalBlocks = new HashMap<>();
        tntRunOriginalBlocks.put(room.getRoomId(), originalBlocks);
        tntRunFadingBlocks.put(room.getRoomId(), new HashSet<>());
        tntRunRuntimes.put(room.getRoomId(), config);
        room.clearMiniGameState();

        int centerY = 120;
        Location center = new Location(world, 0.5D, centerY, 0.5D);
        Location spectator = center.clone().add(0.0D, 16.0D, 0.0D);
        double boundaryRadius = Math.max(config.radius() + 6.0D, Math.max(config.width(), config.length()) / 2.0D + 4.0D);
        room.setMiniGameArena(config.id(), config.displayName(), center, spectator, config.eliminationY(), boundaryRadius, config.maxGameTimeSeconds());

        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(boundaryRadius * 2.0D);
        border.setWarningDistance(6);
        border.setDamageAmount(0.0D);

        Material[] layerPalette = {Material.TNT, Material.RED_SAND, Material.SAND, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE};
        for (int layer = 0; layer < config.layers(); layer++) {
            int y = centerY - layer * config.layerSpacing();
            Material layerMaterial = layerPalette[layer % layerPalette.length];
            generateTntRunLayer(room, world, originalBlocks, center.getBlockX(), y, center.getBlockZ(), config, layer, layerMaterial);
        }

        double spawnRadius = Math.max(4.5D, Math.min(config.radius() - 3.0D, Math.min(config.width(), config.length()) / 2.0D - 3.0D));
        if (spawnRadius < 2.5D) {
            spawnRadius = Math.max(2.5D, boundaryRadius * 0.5D);
        }
        for (int i = 0; i < players.size(); i++) {
            UUID uuid = players.get(i);
            double angle = Math.PI * 2.0D * i / Math.max(1, players.size());
            double x = center.getX() + Math.cos(angle) * spawnRadius;
            double z = center.getZ() + Math.sin(angle) * spawnRadius;
            Location spawn = new Location(world, Math.floor(x) + 0.5D, centerY + 1.0D, Math.floor(z) + 0.5D);
            spawn.setYaw((float) Math.toDegrees(angle + Math.PI));
            spawn.setPitch(0.0F);
            spawns.put(uuid, spawn);
        }
        return spawns;
    }

    private void generateTntRunLayer(GameRoom room, World world, Map<String, BlockData> originalBlocks,
                                     int centerX, int y, int centerZ, TntRunMapConfig config, int layer, Material material) {
        String shape = config.shape().toLowerCase(Locale.ROOT);
        int halfWidth = Math.max(2, config.width() / 2);
        int halfLength = Math.max(2, config.length() / 2);
        int radius = Math.max(3, config.radius());
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfLength; dz <= halfLength; dz++) {
                if (!isInsideTntRunShape(shape, dx, dz, radius, halfWidth, halfLength, layer)) {
                    continue;
                }
                Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                rememberStandaloneBlock(room, originalBlocks, block);
                block.setType(material, false);
            }
        }
    }

    private boolean isInsideTntRunShape(String shape, int dx, int dz, int radius, int halfWidth, int halfLength, int layer) {
        return switch (shape) {
            case "square" -> Math.abs(dx) <= halfWidth && Math.abs(dz) <= halfLength;
            case "ring", "circle_ring" -> {
                double distance = Math.sqrt(dx * dx + dz * dz);
                yield distance <= radius && distance >= Math.max(3.0D, radius - 3.5D);
            }
            case "honeycomb" -> {
                int adx = Math.abs(dx);
                int adz = Math.abs(dz);
                yield adx + adz / 2.0D <= radius && !(adx % 3 == 0 && adz % 3 == 0 && adx + adz > radius);
            }
            case "spiral" -> {
                double distance = Math.sqrt(dx * dx + dz * dz);
                double angle = Math.atan2(dz, dx);
                double normalized = (angle + Math.PI) / (Math.PI * 2.0D);
                double band = (normalized * radius + layer * 1.7D) % 4.0D;
                yield distance <= radius && (band <= 2.1D || distance <= 4.0D);
            }
            default -> (dx * dx + dz * dz) <= radius * radius;
        };
    }

    private Map<UUID, Location> buildBlockPartyArena(GameRoom room, World world, BlockPartyMapConfig config, List<UUID> players) {
        Map<UUID, Location> spawns = new LinkedHashMap<>();
        Map<String, BlockData> originalBlocks = new HashMap<>();
        blockPartyOriginalBlocks.put(room.getRoomId(), originalBlocks);
        room.clearMiniGameState();

        int centerY = 120;
        Location center = new Location(world, 0.5D, centerY, 0.5D);
        Location spectator = center.clone().add(0.0D, 14.0D, 0.0D);
        double boundaryRadius = Math.max(8.0D, config.size() / 2.0D + 5.0D);
        room.setMiniGameArena(config.id(), config.displayName(), center, spectator, config.eliminationY(), boundaryRadius, config.maxGameTimeSeconds());

        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(boundaryRadius * 2.0D);
        border.setWarningDistance(5);
        border.setDamageAmount(0.0D);

        buildBlockPartyFloor(room, world, originalBlocks, config, config.colors().get(0));

        int radius = Math.max(4, config.size() / 2 - 3);
        for (int i = 0; i < players.size(); i++) {
            UUID uuid = players.get(i);
            double angle = Math.PI * 2.0D * i / Math.max(1, players.size());
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location spawn = new Location(world, Math.floor(x) + 0.5D, centerY + 1.0D, Math.floor(z) + 0.5D);
            spawn.setYaw((float) Math.toDegrees(angle + Math.PI));
            spawn.setPitch(0.0F);
            spawns.put(uuid, spawn);
        }
        return spawns;
    }

    private void buildBlockPartyFloor(GameRoom room, World world, Map<String, BlockData> originalBlocks,
                                      BlockPartyMapConfig config, DyeColor highlightColor) {
        int centerX = 0;
        int centerZ = 0;
        int y = 120;
        int half = Math.max(4, config.size() / 2);
        List<DyeColor> colors = config.colors().isEmpty() ? List.of(DyeColor.WHITE) : config.colors();
        Random random = ThreadLocalRandom.current();
        String pattern = config.pattern().toLowerCase(Locale.ROOT);
        String shape = config.shape().toLowerCase(Locale.ROOT);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if ("circle".equals(shape) && (dx * dx + dz * dz) > half * half) {
                    continue;
                }
                DyeColor color = switch (pattern) {
                    case "checker" -> colors.get(Math.floorMod(dx + dz, colors.size()));
                    case "stripes" -> colors.get(Math.floorMod(dx, colors.size()));
                    case "rings" -> colors.get(Math.floorMod((int) Math.round(Math.sqrt(dx * dx + dz * dz)), colors.size()));
                    default -> colors.get(random.nextInt(colors.size()));
                };
                if (highlightColor != null && dx == 0 && dz == 0) {
                    color = highlightColor;
                }
                Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                rememberStandaloneBlock(room, originalBlocks, block);
                block.setType(resolvePartyMaterial(color, config.floorType()), false);
            }
        }
    }

    private Material resolvePartyMaterial(DyeColor color, String floorType) {
        String suffix = switch (floorType == null ? "wool" : floorType.trim().toLowerCase(Locale.ROOT)) {
            case "concrete" -> "CONCRETE";
            case "glass", "stained_glass" -> "STAINED_GLASS";
            default -> "WOOL";
        };
        Material material = Material.matchMaterial(color.name() + "_" + suffix);
        return material == null ? Material.WHITE_WOOL : material;
    }

    private void rememberStandaloneBlock(GameRoom room, Map<String, BlockData> originalBlocks, Block block) {
        if (block == null) {
            return;
        }
        String key = blockKey(block.getLocation());
        if (!originalBlocks.containsKey(key)) {
            originalBlocks.put(key, block.getBlockData().clone());
        }
        room.addMiniGameProtectedBlock(block.getLocation());
    }

    private String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private Location parseBlockKey(World world, String key) {
        if (world == null || key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.split(":");
        if (parts.length < 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[parts.length - 3]);
            int y = Integer.parseInt(parts[parts.length - 2]);
            int z = Integer.parseInt(parts[parts.length - 1]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void startTntRunGuardTask(GameRoom room, TntRunMapConfig config) {
        BukkitTask existing = gameTasks.remove(room.getRoomId());
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || room.getGameMode() != GameMode.TNT_RUN) {
                    cancel();
                    return;
                }
                elapsedTicks++;
                for (UUID uuid : new ArrayList<>(room.getMiniGameAlivePlayers())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        continue;
                    }
                    room.setMiniGameSurvivalTicks(uuid, elapsedTicks);
                    if (checkStandaloneMiniGameOut(room, player, "§c掉出了跑酷平台")) {
                        continue;
                    }
                    fadeTntRunBlockBelow(room, player, config.disappearDelayTicks());
                }
                if (elapsedTicks >= room.getMiniGameMaxGameTimeSeconds() * 20) {
                    finishStandaloneMiniGameByTime(room);
                    cancel();
                    return;
                }
                checkStandaloneMiniGameWin(room);
            }
        }.runTaskTimer(plugin, 20L, 1L);
        gameTasks.put(room.getRoomId(), task);
    }

    private void fadeTntRunBlockBelow(GameRoom room, Player player, int delayTicks) {
        Location foot = player.getLocation().clone().subtract(0.0D, 1.0D, 0.0D);
        Block block = foot.getBlock();
        if (block.getType().isAir()) {
            return;
        }
        String key = blockKey(block.getLocation());
        Set<String> fading = tntRunFadingBlocks.computeIfAbsent(room.getRoomId(), id -> new HashSet<>());
        if (!fading.add(key)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fading.remove(key);
            if (room.getState() != RoomState.PLAYING || room.getGameMode() != GameMode.TNT_RUN) {
                return;
            }
            Block target = block.getWorld().getBlockAt(block.getLocation());
            if (target.getType().isAir()) {
                return;
            }
            target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0.5D, 0.5D, 0.5D),
                    14, 0.25D, 0.12D, 0.25D, target.getBlockData());
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SAND_BREAK, 0.55f, 1.15f);
            target.setType(Material.AIR, false);
        }, Math.max(1, delayTicks));
    }

    private void startBlockPartyLoop(GameRoom room, BlockPartyMapConfig config) {
        room.setMiniGameRound(0);
        BukkitTask existing = gameTasks.remove(room.getRoomId());
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            int elapsedTicks = 0;
            double nextCountdown = config.initialCountdownSeconds();
            int phase = 0;
            int phaseTicks = 0;
            int currentCountdownSeconds = config.initialCountdownSeconds();
            DyeColor targetColor = config.colors().get(0);
            Material targetMaterial = resolvePartyMaterial(targetColor, config.floorType());

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || room.getGameMode() != GameMode.BLOCK_PARTY) {
                    cancel();
                    return;
                }
                elapsedTicks++;
                for (UUID uuid : new ArrayList<>(room.getMiniGameAlivePlayers())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        continue;
                    }
                    room.setMiniGameSurvivalTicks(uuid, elapsedTicks);
                    checkStandaloneMiniGameOut(room, player, "§c站错位置掉下了舞台");
                }
                if (elapsedTicks >= room.getMiniGameMaxGameTimeSeconds() * 20) {
                    finishStandaloneMiniGameByTime(room);
                    cancel();
                    return;
                }

                if (phase == 0) {
                    room.setMiniGameRound(room.getMiniGameRound() + 1);
                    targetColor = config.colors().get(ThreadLocalRandom.current().nextInt(config.colors().size()));
                    targetMaterial = resolvePartyMaterial(targetColor, config.floorType());
                    buildBlockPartyFloor(room, room.getGameWorld(),
                            blockPartyOriginalBlocks.computeIfAbsent(room.getRoomId(), id -> new HashMap<>()),
                            config, targetColor);
                    currentCountdownSeconds = Math.max(config.minCountdownSeconds(), (int) Math.round(nextCountdown));
                    blockPartyRuntimes.put(room.getRoomId(), new BlockPartyRuntime(config, targetMaterial, targetColor,
                            room.getMiniGameRound(), currentCountdownSeconds, false));
                    announceBlockPartyRound(room, config, targetColor, targetMaterial, currentCountdownSeconds);
                    phaseTicks = currentCountdownSeconds * 20;
                    phase = 1;
                } else if (phase == 1) {
                    if (phaseTicks % 20 == 0) {
                        int secondValue = Math.max(1, phaseTicks / 20);
                        for (UUID uuid : room.getAllPlayerUUIDs()) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player == null) {
                                continue;
                            }
                            player.setLevel(secondValue);
                            player.setExp(secondValue / (float) Math.max(1, currentCountdownSeconds));
                            if (config.music()) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.55f,
                                        1.0f + (currentCountdownSeconds - secondValue) * 0.08f);
                            }
                        }
                    }
                    phaseTicks--;
                    if (phaseTicks <= 0) {
                        clearWrongBlockPartyBlocks(room, targetMaterial);
                        blockPartyRuntimes.put(room.getRoomId(), new BlockPartyRuntime(config, targetMaterial, targetColor,
                                room.getMiniGameRound(), currentCountdownSeconds, true));
                        phaseTicks = Math.max(20, config.disappearSeconds() * 20);
                        phase = 2;
                    }
                } else {
                    if (phaseTicks == Math.max(20, config.disappearSeconds() * 20) - 1) {
                        eliminateWrongBlockPartyPlayers(room, targetMaterial);
                        checkStandaloneMiniGameWin(room);
                    }
                    phaseTicks--;
                    if (phaseTicks <= 0) {
                        nextCountdown = Math.max(config.minCountdownSeconds(), nextCountdown - config.countdownReduceSeconds());
                        phase = 0;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
        gameTasks.put(room.getRoomId(), task);
    }

    private void announceBlockPartyRound(GameRoom room, BlockPartyMapConfig config, DyeColor targetColor,
                                         Material targetMaterial, int countdownSeconds) {
        Map<String, String> roundPlaceholders = new HashMap<>();
        roundPlaceholders.put("round", String.valueOf(room.getMiniGameRound()));
        roundPlaceholders.put("seconds", String.valueOf(countdownSeconds));
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.block_party_round", roundPlaceholders));

        Map<String, String> colorPlaceholders = new HashMap<>();
        colorPlaceholders.put("color", getChineseColorName(targetColor));
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.block_party_target", colorPlaceholders));

        ItemStack hintItem = new ItemStack(targetMaterial);
        ItemMeta meta = hintItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§D§D§8§8▣ §f目标颜色：§e" + getChineseColorName(targetColor));
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a倒计时结束后，只有这个颜色会保留",
                    "§f- §b请立刻站到对应颜色的方块上",
                    "§8· · · · · · · · · · · · · ·"
            ));
            hintItem.setItemMeta(meta);
        }

        String titleColor = getTitleColor(targetColor);
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.getInventory().setItem(4, hintItem);
            player.showTitle(Title.title(
                    LegacyComponentSerializer.legacySection().deserialize(titleColor + "§l" + getChineseColorName(targetColor)),
                    LegacyComponentSerializer.legacySection().deserialize("§f快站上目标颜色方块"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(250))));
            if (config.music()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.7f, 1.2f);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.45f);
            }
        }
    }

    private void clearWrongBlockPartyBlocks(GameRoom room, Material targetMaterial) {
        World world = room.getGameWorld();
        Location center = room.getMiniGameArenaCenter();
        if (world == null || center == null) {
            return;
        }
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int y = center.getBlockY();
        int half = (int) Math.ceil(room.getMiniGameBoundaryRadius());
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                Block block = world.getBlockAt(cx + dx, y, cz + dz);
                if (block.getType().isAir() || block.getType() == targetMaterial || !room.isMiniGameProtectedBlock(block.getLocation())) {
                    continue;
                }
                world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D),
                        10, 0.2D, 0.1D, 0.2D, block.getBlockData());
                block.setType(Material.AIR, false);
            }
        }
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 1.6f);
    }

    private void eliminateWrongBlockPartyPlayers(GameRoom room, Material targetMaterial) {
        for (UUID uuid : new ArrayList<>(room.getMiniGameAlivePlayers())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.getLocation().getY() <= room.getMiniGameEliminationY()) {
                eliminateStandaloneMiniGamePlayer(room, player, player.getLocation(), "§c掉出了彩色舞台");
                continue;
            }
            Block standing = player.getLocation().clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
            if (standing.getType() != targetMaterial) {
                eliminateStandaloneMiniGamePlayer(room, player, player.getLocation(), "§c站错了目标颜色");
            }
        }
    }

    private boolean checkStandaloneMiniGameOut(GameRoom room, Player player, String reason) {
        Location location = player.getLocation();
        World world = room.getGameWorld();
        if (world == null || location.getWorld() == null || !location.getWorld().equals(world)) {
            return eliminateStandaloneMiniGamePlayer(room, player, location, reason);
        }
        if (room.getMiniGameEliminationY() != 0 && location.getY() <= room.getMiniGameEliminationY()) {
            return eliminateStandaloneMiniGamePlayer(room, player, location, reason);
        }
        Location center = room.getMiniGameArenaCenter();
        if (center != null && center.getWorld() != null && center.getWorld().equals(location.getWorld())) {
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            if (dx * dx + dz * dz > Math.pow(room.getMiniGameBoundaryRadius() + 2.0D, 2.0D)) {
                return eliminateStandaloneMiniGamePlayer(room, player, location, "§c飞出了竞技场边界");
            }
        }
        return false;
    }

    public boolean handleStandaloneMiniGameMove(org.bukkit.event.player.PlayerMoveEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || !room.getGameMode().isStandaloneMiniGame()
                || room.getState() != RoomState.PLAYING) {
            return false;
        }
        if (!room.isGameActuallyStarted()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                to.setX(from.getX());
                to.setZ(from.getZ());
                event.setTo(to);
            }
            player.setFallDistance(0.0F);
            return true;
        }
        if (room.isMiniGameEliminated(player.getUniqueId()) || room.isSpectator(player.getUniqueId())) {
            return false;
        }
        if (room.getGameMode() == GameMode.TNT_RUN) {
            TntRunMapConfig runtime = tntRunRuntimes.get(room.getRoomId());
            fadeTntRunBlockBelow(room, player, runtime == null ? 8 : runtime.disappearDelayTicks());
        }
        checkStandaloneMiniGameOut(room, player,
                room.getGameMode() == GameMode.TNT_RUN ? "§c掉出了跑酷平台" : "§c掉出了彩色舞台");
        return false;
    }

    public boolean handleStandaloneMiniGameDeath(PlayerDeathEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || !room.getGameMode().isStandaloneMiniGame()
                || room.getState() != RoomState.PLAYING) {
            return false;
        }
        event.setKeepInventory(true);
        event.getDrops().clear();
        if (room.isMiniGameEliminated(player.getUniqueId())) {
            return true;
        }
        eliminateStandaloneMiniGamePlayer(room, player, player.getLocation(), "§c被淘汰出局");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Exception ignored) {
            }
        }, 1L);
        return true;
    }

    public boolean eliminateStandaloneMiniGamePlayer(GameRoom room, Player player, Location location, String reason) {
        if (room == null || player == null || room.isMiniGameEliminated(player.getUniqueId())) {
            return true;
        }
        room.markMiniGameEliminated(player.getUniqueId());
        room.setPendingRespawnLocation(player.getUniqueId(), getStandaloneMiniGameSpectatorLocation(room, location));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("alive", String.valueOf(room.getMiniGameAlivePlayers().size()));
        String path = room.getGameMode() == GameMode.TNT_RUN ? "game.tnt_run_eliminated" : "game.block_party_eliminated";
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix(path, placeholders));
        player.showTitle(Title.title(
                LegacyComponentSerializer.legacySection().deserialize("§x§F§F§8§8§5§5☠ §c已淘汰"),
                LegacyComponentSerializer.legacySection().deserialize(reason),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1700), Duration.ofMillis(250))));
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        org.bukkit.util.Vector backUp = player.getLocation().getDirection().normalize().multiply(-0.55D).setY(0.82D);
        player.setVelocity(backUp);
        Bukkit.getScheduler().runTaskLater(plugin, () -> convertStandaloneEliminatedToSpectator(player, room, location), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkStandaloneMiniGameWin(room), 6L);
        return true;
    }

    private void convertStandaloneEliminatedToSpectator(Player player, GameRoom room, Location location) {
        if (player == null || !player.isOnline() || room.getState() != RoomState.PLAYING) {
            return;
        }
        room.addSpectator(player.getUniqueId());
        player.teleport(getStandaloneMiniGameSpectatorLocation(room, location));
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setLevel(0);
        player.setExp(0);
        giveSpectatorItems(player);
        plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.62f, 0.92f);
    }

    private Location getStandaloneMiniGameSpectatorLocation(GameRoom room, Location fallback) {
        Location spectator = room.getMiniGameSpectatorSpawn();
        if (spectator != null && spectator.getWorld() != null) {
            return spectator.clone();
        }
        World world = room.getGameWorld();
        if (world == null) {
            return fallback == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : fallback.clone().add(0.0D, 8.0D, 0.0D);
        }
        Location base = fallback != null && fallback.getWorld() != null && fallback.getWorld().equals(world)
                ? fallback.clone()
                : world.getSpawnLocation().clone();
        base.add(0.0D, 8.0D, 0.0D);
        base.setYaw(0.0F);
        base.setPitch(28.0F);
        return base;
    }

    public void checkStandaloneMiniGameWin(GameRoom room) {
        if (room == null || !room.getGameMode().isStandaloneMiniGame() || room.getState() != RoomState.PLAYING) {
            return;
        }
        List<UUID> alive = room.getMiniGameAlivePlayers();
        if (alive.size() <= 1) {
            endStandaloneMiniGame(room, alive.isEmpty() ? null : alive.get(0), false);
        }
    }

    private void finishStandaloneMiniGameByTime(GameRoom room) {
        UUID winner = null;
        long bestTicks = Long.MIN_VALUE;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            long ticks = room.getMiniGameSurvivalTicks(uuid);
            if (ticks > bestTicks) {
                bestTicks = ticks;
                winner = uuid;
            }
        }
        endStandaloneMiniGame(room, winner, true);
    }

    private void endStandaloneMiniGame(GameRoom room, UUID winnerUuid, boolean timeUp) {
        if (room == null || room.getState() == RoomState.ENDED) {
            return;
        }
        room.setState(RoomState.ENDED);
        room.setPreyWon(false);
        BukkitTask gameTask = gameTasks.remove(room.getRoomId());
        if (gameTask != null) {
            gameTask.cancel();
        }

        String winnerName = "无人";
        if (winnerUuid != null) {
            Player winner = Bukkit.getPlayer(winnerUuid);
            winnerName = winner == null ? Optional.ofNullable(Bukkit.getOfflinePlayer(winnerUuid).getName()).orElse("未知玩家") : winner.getName();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("winner", winnerName);
        String messagePath;
        String titleText;
        if (room.getGameMode() == GameMode.TNT_RUN) {
            messagePath = timeUp ? "game.tnt_run_time_up" : "game.tnt_run_win";
            titleText = "§x§F§F§8§8§5§5✹ §eTNT跑酷结束";
        } else {
            messagePath = timeUp ? "game.block_party_time_up" : "game.block_party_win";
            titleText = "§x§D§D§8§8§F§F▣ §d方块派对结束";
        }
        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix(messagePath, placeholders));

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (uuid.equals(winnerUuid)) {
                plugin.getPlayerDataManager().addMiniGamePoints(uuid, STANDALONE_MINIGAME_WIN_POINTS, room.getGameMode());
                Map<String, String> pointPh = new HashMap<>();
                pointPh.put("points", String.valueOf(STANDALONE_MINIGAME_WIN_POINTS));
                player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("points.minigame_win", pointPh));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.15f);
            } else {
                plugin.getPlayerDataManager().addMiniGamePoints(uuid, STANDALONE_MINIGAME_PARTICIPATE_POINTS, room.getGameMode());
                Map<String, String> pointPh = new HashMap<>();
                pointPh.put("points", String.valueOf(STANDALONE_MINIGAME_PARTICIPATE_POINTS));
                player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("points.minigame_participate", pointPh));
            }
            plugin.getPlayerDataManager().incrementPlayCount(uuid, room.getGameMode());
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }

        Title endTitle = Title.title(
                LegacyComponentSerializer.legacySection().deserialize(titleText),
                LegacyComponentSerializer.legacySection().deserialize("§f胜者: §a" + winnerName),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(6500), Duration.ofMillis(800))
        );
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(endTitle);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.25f);
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(endTitle);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.25f);
            }
        }

        restoreStandaloneMiniGameBlocks(room);
        cleanupStandaloneMiniGameState(room);
        scheduleStandaloneMiniGameRoomClose(room);
    }

    private void restoreStandaloneMiniGameBlocks(GameRoom room) {
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }
        Map<String, BlockData> tntBlocks = tntRunOriginalBlocks.get(room.getRoomId());
        if (tntBlocks != null) {
            for (Map.Entry<String, BlockData> entry : tntBlocks.entrySet()) {
                Location location = parseBlockKey(world, entry.getKey());
                if (location != null) {
                    world.getBlockAt(location).setBlockData(entry.getValue(), false);
                }
            }
        }
        Map<String, BlockData> partyBlocks = blockPartyOriginalBlocks.get(room.getRoomId());
        if (partyBlocks != null) {
            for (Map.Entry<String, BlockData> entry : partyBlocks.entrySet()) {
                Location location = parseBlockKey(world, entry.getKey());
                if (location != null) {
                    world.getBlockAt(location).setBlockData(entry.getValue(), false);
                }
            }
        }
    }

    private void cleanupStandaloneMiniGameState(GameRoom room) {
        tntRunOriginalBlocks.remove(room.getRoomId());
        blockPartyOriginalBlocks.remove(room.getRoomId());
        tntRunFadingBlocks.remove(room.getRoomId());
        tntRunRuntimes.remove(room.getRoomId());
        blockPartyRuntimes.remove(room.getRoomId());
        room.clearMiniGameState();
    }

    private void scheduleStandaloneMiniGameRoomClose(GameRoom room) {
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    if (plugin.getChildServerManager().isNodeMode()) {
                        plugin.getLogger().info("[LuckyPillars] 房间结束，节点服准备送回大厅并关闭: room=" + room.getRoomId()
                                + ", managedNode=" + plugin.getChildServerManager().isManagedNodeRoom(room.getRoomId())
                                + ", managedCross=" + plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId()));
                        plugin.getChildServerManager().returnManagedRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }
                    if (plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnCrossServerRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
                    }
                    for (UUID uuid : room.getSpectators()) {
                        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
                    }
                    plugin.getRoomManager().clearAllRoleNameTags(room);
                    plugin.getRoomManager().deleteRoom(room.getRoomId());
                    cancel();
                    return;
                }
                if (countdown <= 5) {
                    room.broadcast("§x§F§F§5§5§5§5⏱ §c" + countdown + " §7秒后关闭...");
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private String getChineseColorName(DyeColor color) {
        return switch (color) {
            case WHITE -> "白色";
            case ORANGE -> "橙色";
            case MAGENTA -> "品红色";
            case LIGHT_BLUE -> "淡蓝色";
            case YELLOW -> "黄色";
            case LIME -> "黄绿色";
            case PINK -> "粉色";
            case GRAY -> "灰色";
            case LIGHT_GRAY -> "浅灰色";
            case CYAN -> "青色";
            case PURPLE -> "紫色";
            case BLUE -> "蓝色";
            case BROWN -> "棕色";
            case GREEN -> "绿色";
            case RED -> "红色";
            case BLACK -> "黑色";
        };
    }

    private String getTitleColor(DyeColor color) {
        return switch (color) {
            case WHITE -> "§f";
            case ORANGE, BROWN -> "§6";
            case MAGENTA, PINK -> "§d";
            case LIGHT_BLUE -> "§b";
            case YELLOW -> "§e";
            case LIME -> "§a";
            case GRAY -> "§8";
            case LIGHT_GRAY -> "§7";
            case CYAN -> "§3";
            case PURPLE -> "§5";
            case BLUE -> "§9";
            case GREEN -> "§2";
            case RED -> "§c";
            case BLACK -> "§0";
        };
    }

    public boolean handleLuckyPillarBreak(org.bukkit.event.block.BlockBreakEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || !room.getGameMode().isLuckyPillars()
                || room.getState() != RoomState.PLAYING) {
            return false;
        }

        if (!room.isGameActuallyStarted() || room.isLuckyPillarsEliminated(player.getUniqueId())) {
            event.setCancelled(true);
            return true;
        }

        if (!room.isLuckyPillarBlock(event.getBlock().getLocation())) {
            return false;
        }

        Location loc = event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D);
        event.setCancelled(true);
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.WAX_ON, loc, 8, 0.25D, 0.18D, 0.25D, 0.01D);
            world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.38f, 0.72f);
        }
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5🍀 §7幸运柱本体不能挖，靠随机物品来搭桥和击退对手"));
        return true;
    }

    private void giveLuckyPillarsSecondItem(GameRoom room, Player player, int secondsElapsed) {
        if (room == null || player == null || !player.isOnline()
                || room.isLuckyPillarsEliminated(player.getUniqueId())) {
            return;
        }

        ItemStack item = createLuckyPillarsRandomItem(room, secondsElapsed);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        item.setAmount(1);
        if (player.getInventory().firstEmpty() == -1) {
            return;
        }
        player.getInventory().addItem(item);
    }

    private ItemStack createLuckyPillarsRandomItem(GameRoom room, int secondsElapsed) {
        return createLuckyPillarsAnyVanillaItem();
    }

    private ItemStack createLuckyPillarsSpawnEggItem(boolean pvpBoss) {
        Material material = randomMaterialFromNames(LUCKY_PILLARS_SPAWN_EGG_MATERIALS);
        if (material == null) {
            return createLuckyPillarsAnyVanillaItem();
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §d幸运之柱召唤物");
            lore.add("§f- §b召唤物造成击杀会算到你头上");
            lore.add(pvpBoss ? "§f- §cPVP大佬模式更容易刷到强生物" : "§f- §7强生物会自动做位置限制");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLuckyPillarsSpecialItem(String[] names, boolean pvpBoss) {
        Material material = randomMaterialFromNames(names);
        if (material == null) {
            return createLuckyPillarsAnyVanillaItem();
        }
        return new ItemStack(material, 1);
    }

    private ItemStack createLuckyPillarsWeaponItem(boolean pvpBoss) {
        Material material = randomMaterialFromNames(LUCKY_PILLARS_WEAPON_MATERIALS);
        if (material == null) {
            return createLuckyPillarsAnyVanillaItem();
        }
        ItemStack item = new ItemStack(material, 1);
        int enchantChance = pvpBoss ? 42 : 12;
        if (ThreadLocalRandom.current().nextInt(100) < enchantChance) {
            enchantLuckyPillarsWeapon(item, pvpBoss);
        }
        return item;
    }

    private ItemStack createLuckyPillarsArmorItem(boolean pvpBoss) {
        Material material = randomMaterialFromNames(LUCKY_PILLARS_ARMOR_MATERIALS);
        if (material == null) {
            return createLuckyPillarsAnyVanillaItem();
        }
        ItemStack item = new ItemStack(material, 1);
        int enchantChance = pvpBoss ? 40 : 10;
        if (isLuckyPillarsArmorPiece(material) && ThreadLocalRandom.current().nextInt(100) < enchantChance) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION, pvpBoss
                    ? ThreadLocalRandom.current().nextInt(1, 4)
                    : 1);
        }
        return item;
    }

    private ItemStack createLuckyPillarsAnyVanillaItem() {
        List<Material> candidates = getLuckyPillarsEqualRandomMaterials();
        if (candidates.isEmpty()) {
            return new ItemStack(Material.OAK_PLANKS, 1);
        }
        Material material = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return new ItemStack(material, 1);
    }

    private List<Material> getLuckyPillarsEqualRandomMaterials() {
        return LUCKY_PILLARS_EQUAL_RANDOM_MATERIALS;
    }

    private Material randomMaterialFromNames(String[] names) {
        List<Material> candidates = new ArrayList<>();
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null && !isBlockedLuckyPillarsRandomMaterial(material)) {
                candidates.add(material);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private boolean isBlockedLuckyPillarsRandomMaterial(Material material) {
        return !isAllowedLuckyPillarsRandomMaterial(material);
    }

    private static boolean isAllowedLuckyPillarsRandomMaterial(Material material) {
        if (material == null || !material.isItem()) {
            return false;
        }
        String name = material.name();
        if (LUCKY_PILLARS_BLOCKED_RANDOM_MATERIALS.contains(name)) {
            return false;
        }
        return true;
    }

    private int getLuckyPillarsSpecialAmount(Material material, boolean pvpBoss) {
        if (material == null) {
            return 1;
        }
        int maxStack = Math.max(1, new ItemStack(material).getMaxStackSize());
        if (maxStack <= 1) {
            return 1;
        }
        String name = material.name();
        if (name.endsWith("_PLANKS") || name.equals("COBBLESTONE") || name.equals("STONE")
                || name.equals("DEEPSLATE") || name.equals("DIRT") || name.equals("GLASS")
                || name.equals("SCAFFOLDING") || name.equals("LADDER")) {
            return ThreadLocalRandom.current().nextInt(8, Math.min(maxStack, pvpBoss ? 20 : 28) + 1);
        }
        if (name.equals("SNOWBALL") || name.equals("EGG")) {
            return ThreadLocalRandom.current().nextInt(4, Math.min(maxStack, 16) + 1);
        }
        if (name.equals("WIND_CHARGE")) {
            return ThreadLocalRandom.current().nextInt(1, Math.min(maxStack, pvpBoss ? 5 : 3) + 1);
        }
        if (name.equals("ENDER_PEARL") || name.equals("TNT") || name.equals("COBWEB")) {
            return ThreadLocalRandom.current().nextInt(1, Math.min(maxStack, pvpBoss ? 4 : 2) + 1);
        }
        if (LUCKY_PILLARS_FOOD_MATERIALS != null && Arrays.asList(LUCKY_PILLARS_FOOD_MATERIALS).contains(name)) {
            return ThreadLocalRandom.current().nextInt(2, Math.min(maxStack, 8) + 1);
        }
        return Math.min(maxStack, pvpBoss ? 4 : 2);
    }

    private int getLuckyPillarsRandomAmount(Material material) {
        if (material == null) {
            return 1;
        }
        int maxStack = Math.max(1, new ItemStack(material).getMaxStackSize());
        if (maxStack <= 1 || isLuckyPillarsCombatMaterial(material)) {
            return 1;
        }
        String name = material.name();
        if (name.endsWith("_PLANKS") || name.endsWith("_LOG") || name.endsWith("_STONE")
                || name.endsWith("_BRICKS") || name.endsWith("_WOOL") || name.endsWith("_CONCRETE")
                || name.endsWith("_TERRACOTTA") || name.endsWith("_DIRT") || name.endsWith("_SAND")) {
            return ThreadLocalRandom.current().nextInt(8, Math.min(maxStack, 32) + 1);
        }
        if (name.contains("ARROW") || name.contains("FIREWORK_ROCKET")) {
            return ThreadLocalRandom.current().nextInt(4, Math.min(maxStack, 20) + 1);
        }
        if (name.contains("BREAD") || name.contains("BEEF") || name.contains("PORKCHOP")
                || name.contains("CHICKEN") || name.contains("CARROT") || name.contains("POTATO")
                || name.contains("APPLE")) {
            return ThreadLocalRandom.current().nextInt(2, Math.min(maxStack, 8) + 1);
        }
        return ThreadLocalRandom.current().nextInt(1, Math.min(maxStack, 8) + 1);
    }

    private boolean isLuckyPillarsCombatMaterial(Material material) {
        return isLuckyPillarsWeaponMaterial(material) || isLuckyPillarsArmorPiece(material) || material == Material.SHIELD;
    }

    private boolean isLuckyPillarsWeaponMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("MACE");
    }

    private boolean isLuckyPillarsRangedWeapon(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }

    private boolean isLuckyPillarsArmorPiece(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private void enchantLuckyPillarsWeapon(ItemStack item, boolean pvpBoss) {
        if (item == null) {
            return;
        }
        Material type = item.getType();
        int maxLevel = pvpBoss ? 3 : 1;
        if (type == Material.BOW) {
            item.addUnsafeEnchantment(Enchantment.POWER, ThreadLocalRandom.current().nextInt(1, maxLevel + 1));
        } else if (type == Material.CROSSBOW) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                item.addUnsafeEnchantment(Enchantment.QUICK_CHARGE, pvpBoss ? 2 : 1);
            } else {
                item.addUnsafeEnchantment(Enchantment.PIERCING, ThreadLocalRandom.current().nextInt(1, Math.min(4, maxLevel + 1) + 1));
            }
        } else if (isLuckyPillarsWeaponMaterial(type)) {
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, ThreadLocalRandom.current().nextInt(1, maxLevel + 1));
        }
    }

    private String formatLuckyPillarsMaterialName(Material material) {
        if (material == null) {
            return "未知物品";
        }
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR) {
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private StartItemGiveResult giveOrEnderChestOrDrop(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR) {
            return new StartItemGiveResult(false, false);
        }
        boolean storedInEnderChest = false;
        boolean dropped = false;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                int before = leftover == null ? 0 : leftover.getAmount();
                Map<Integer, ItemStack> failed = player.getEnderChest().addItem(leftover);
                int failedAmount = countItemAmount(failed.values());
                if (before > failedAmount) {
                    storedInEnderChest = true;
                }
                for (ItemStack failedItem : failed.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), failedItem);
                    dropped = true;
                }
            }
        }
        return new StartItemGiveResult(storedInEnderChest, dropped);
    }

    private int countItemAmount(Collection<ItemStack> items) {
        int amount = 0;
        if (items == null) {
            return amount;
        }
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private record StartItemGiveResult(boolean storedInEnderChest, boolean dropped) {
    }

    public boolean handleLuckyPillarsSpawnEggUse(org.bukkit.event.player.PlayerInteractEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || !room.getGameMode().isLuckyPillars()
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || room.isLuckyPillarsEliminated(player.getUniqueId())) {
            return false;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.getType().name().endsWith("_SPAWN_EGG")) {
            return false;
        }
        event.setCancelled(true);
        Location base = event.getClickedBlock() == null
                ? player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(2.0D))
                : event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5D, 0.0D, 0.5D);
        EntityType type = spawnEggToEntityType(item.getType());
        if (type == null || !type.isSpawnable()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.78f);
            return true;
        }
        Location spawn = adjustLuckyPillarsSummonLocation(room, player, base, type);
        if (spawn == null || spawn.getWorld() == null) {
            return true;
        }
        Entity entity = spawn.getWorld().spawnEntity(spawn, type);
        luckyPillarsSummonOwners.put(entity.getUniqueId(), player.getUniqueId());
        tuneLuckyPillarsSummon(entity, player);
        consumeMainOrOffhandItem(player, item);
        spawn.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, spawn.clone().add(0, 0.8, 0), 24, 0.45D, 0.45D, 0.45D, 0.02D);
        spawn.getWorld().playSound(spawn, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 0.82f, 1.18f);
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5🍀 §f召唤物已归属你，击杀会计入你的淘汰"));
        return true;
    }

    private EntityType spawnEggToEntityType(Material material) {
        if (material == null || !material.name().endsWith("_SPAWN_EGG")) {
            return null;
        }
        String entityName = material.name().substring(0, material.name().length() - "_SPAWN_EGG".length());
        if (entityName.equals("MOOSHROOM")) {
            entityName = "MUSHROOM_COW";
        }
        try {
            return EntityType.valueOf(entityName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Location adjustLuckyPillarsSummonLocation(GameRoom room, Player owner, Location requested, EntityType type) {
        if (requested == null) {
            return null;
        }
        World world = room.getGameWorld();
        if (world == null) {
            return requested;
        }
        Location center = room.getLuckyPillarsArenaCenter();
        Location spawn = requested.clone();
        if (!spawn.getWorld().equals(world)) {
            spawn.setWorld(world);
        }
        int topY = center == null ? spawn.getBlockY() : center.getBlockY();
        if (type == EntityType.ENDER_DRAGON) {
            spawn = center == null ? owner.getLocation().clone() : center.clone();
            spawn.setY(topY + 18.0D);
            return spawn;
        }
        if (type == EntityType.WARDEN) {
            spawn.setY(Math.max(spawn.getY(), topY + 1.0D));
            if (center != null && spawn.distanceSquared(center) < 36.0D) {
                spawn.add(6.5D, 0.0D, 0.0D);
            }
            return spawn;
        }
        spawn.setY(Math.max(spawn.getY(), topY + 1.0D));
        return spawn;
    }

    private void tuneLuckyPillarsSummon(Entity entity, Player owner) {
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
            living.setGlowing(true);
            living.customName(LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5" + owner.getName() + "的召唤物"));
            living.setCustomNameVisible(false);
        }
        if (entity instanceof Mob mob) {
            for (Player candidate : owner.getWorld().getPlayers()) {
                if (!candidate.getUniqueId().equals(owner.getUniqueId())
                        && plugin.getRoomManager().getPlayerRoom(candidate.getUniqueId()) != null) {
                    mob.setTarget(candidate);
                    break;
                }
            }
        }
        if (entity instanceof EnderDragon dragon) {
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
        }
        if (entity instanceof Warden warden) {
            warden.setAnger(owner, 0);
        }
    }

    private void consumeMainOrOffhandItem(Player player, ItemStack source) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == source || (main != null && main.isSimilar(source))) {
            main.setAmount(main.getAmount() - 1);
            player.getInventory().setItemInMainHand(main.getAmount() <= 0 ? null : main);
            return;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == source || (offhand != null && offhand.isSimilar(source))) {
            offhand.setAmount(offhand.getAmount() - 1);
            player.getInventory().setItemInOffHand(offhand.getAmount() <= 0 ? null : offhand);
        }
    }

    public boolean handleLuckyPillarsDeath(PlayerDeathEvent event, Player player, GameRoom room) {
        if (event == null || player == null || room == null
                || !room.getGameMode().isLuckyPillars()
                || room.getState() != RoomState.PLAYING) {
            return false;
        }

        if (room.isLuckyPillarsEliminated(player.getUniqueId())) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            return true;
        }

        room.markLuckyPillarsEliminated(player.getUniqueId());
        Location deathLoc = player.getLocation().clone();
        room.setPendingRespawnLocation(player.getUniqueId(), deathLoc.clone());

        Player killer = player.getKiller();
        boolean summonKill = killer != null && player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent
                && luckyPillarsSummonOwners.get(damageEvent.getDamager().getUniqueId()) != null;
        creditLuckyPillarsKiller(room, player, killer, summonKill);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("alive", String.valueOf(room.getLuckyPillarsAlivePlayers().size()));
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_eliminated", placeholders));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Exception ignored) {
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> convertLuckyPillarsEliminatedToSpectator(player, room, deathLoc), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkLuckyPillarsWin(room), 8L);
        return true;
    }

    private void convertLuckyPillarsEliminatedToSpectator(Player player, GameRoom room, Location deathLoc) {
        if (player == null || !player.isOnline() || room.getState() != RoomState.PLAYING) {
            return;
        }
        room.addSpectator(player.getUniqueId());
        Location target = deathLoc == null ? getLuckyPillarsSpectatorLocation(room, null) : deathLoc.clone();
        player.teleport(target);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGravity(false);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setCanPickupItems(false);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setHealth(Math.max(1.0D, player.getMaxHealth()));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        player.setLevel(0);
        player.setExp(0);
        player.setVelocity(new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, LUCKY_PILLARS_ELIMINATION_INVISIBILITY_TICKS, 1, false, false, false));
        giveSpectatorItems(player);
        plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                online.hidePlayer(plugin, player);
            }
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 0.8f);
    }

    private Location getLuckyPillarsSpectatorLocation(GameRoom room, Location fallback) {
        World world = room.getGameWorld();
        if (world == null) {
            return fallback == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : fallback.clone();
        }
        Location configured = room.getLuckyPillarsSpectatorSpawn();
        if (configured != null && configured.getWorld() != null && configured.getWorld().equals(world)) {
            return configured.clone();
        }
        Location base = fallback != null && fallback.getWorld() != null && fallback.getWorld().equals(world)
                ? fallback.clone()
                : world.getSpawnLocation().clone();
        base.add(0.0D, 8.0D, 0.0D);
        base.setYaw(0.0F);
        base.setPitch(35.0F);
        return base;
    }

    public void checkLuckyPillarsWin(GameRoom room) {
        if (room == null || !room.getGameMode().isLuckyPillars() || room.getState() != RoomState.PLAYING) {
            return;
        }
        List<UUID> alive = room.getLuckyPillarsAlivePlayers();
        if (alive.size() <= 1) {
            UUID winner = alive.isEmpty() ? null : alive.get(0);
            endLuckyPillarsGame(room, winner);
        }
    }

    private void startLuckyPillarsGuardTask(GameRoom room) {
        BukkitTask existing = gameTasks.remove(room.getRoomId());
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            int secondsElapsed = 0;
            int nextRandomEventSeconds = LUCKY_PILLARS_RANDOM_EVENT_FIRST_DELAY_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || !room.getGameMode().isLuckyPillars()) {
                    cancel();
                    return;
                }
                secondsElapsed++;
                if (secondsElapsed >= room.getLuckyPillarsGameTimeSeconds()) {
                    room.broadcast(plugin.getConfigManager().getLuckyPillarsPrefix()
                            + "§x§F§F§D§7§0§0⌛ §e经典模式时间到，§f按当前存活情况结算胜者。");
                    List<UUID> alive = room.getLuckyPillarsAlivePlayers();
                    UUID winner = alive.size() == 1 ? alive.get(0) : null;
                    endLuckyPillarsGame(room, winner);
                    cancel();
                    return;
                }
                updateLuckyPillarsBorder(room, secondsElapsed);
                for (UUID uuid : new ArrayList<>(room.getLuckyPillarsAlivePlayers())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        if (checkLuckyPillarsOut(player, room, secondsElapsed)) {
                            continue;
                        }
                        if (secondsElapsed % room.getLuckyPillarsRandomItemIntervalSeconds() == 0) {
                            giveLuckyPillarsSecondItem(room, player, secondsElapsed);
                        }
                    }
                }
                if (secondsElapsed >= nextRandomEventSeconds) {
                    triggerLuckyPillarsRandomEvent(room, secondsElapsed);
                    nextRandomEventSeconds = secondsElapsed + nextLuckyPillarsRandomEventDelaySeconds();
                }
                checkLuckyPillarsWin(room);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        gameTasks.put(room.getRoomId(), task);
    }

    private int nextLuckyPillarsRandomEventDelaySeconds() {
        return ThreadLocalRandom.current().nextInt(
                LUCKY_PILLARS_RANDOM_EVENT_MIN_DELAY_SECONDS,
                LUCKY_PILLARS_RANDOM_EVENT_MAX_DELAY_SECONDS + 1
        );
    }

    private void triggerLuckyPillarsRandomEvent(GameRoom room, int secondsElapsed) {
        if (room == null || room.getState() != RoomState.PLAYING || !room.getGameMode().isLuckyPillars()) {
            return;
        }
        List<Player> alivePlayers = getLuckyPillarsAlivePlayers(room);
        if (alivePlayers.size() <= 1) {
            return;
        }
        int roll = ThreadLocalRandom.current().nextInt(6);
        switch (roll) {
            case 0 -> triggerLuckyPillarsArrowRainEvent(room, alivePlayers);
            case 1 -> triggerLuckyPillarsSupplyEvent(room, alivePlayers, secondsElapsed);
            case 2 -> triggerLuckyPillarsThunderEvent(room, alivePlayers);
            case 3 -> triggerLuckyPillarsMoonWalkEvent(room, alivePlayers);
            case 4 -> triggerLuckyPillarsChainSwapEvent(room, alivePlayers);
            default -> triggerLuckyPillarsInvisibilityEvent(room, alivePlayers);
        }
    }

    private List<Player> getLuckyPillarsAlivePlayers(GameRoom room) {
        List<Player> players = new ArrayList<>();
        if (room == null) {
            return players;
        }
        for (UUID uuid : room.getLuckyPillarsAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !room.isLuckyPillarsEliminated(uuid)) {
                players.add(player);
            }
        }
        return players;
    }

    private void triggerLuckyPillarsArrowRainEvent(GameRoom room, List<Player> alivePlayers) {
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_arrow_rain"));
        for (Player player : alivePlayers) {
            World world = player.getWorld();
            Location base = player.getLocation();
            for (int i = 0; i < LUCKY_PILLARS_RANDOM_EVENT_ARROW_VOLLEYS; i++) {
                Location spawn = base.clone().add(
                        ThreadLocalRandom.current().nextDouble(-1.4D, 1.4D),
                        10.0D + ThreadLocalRandom.current().nextDouble(0.0D, 3.0D),
                        ThreadLocalRandom.current().nextDouble(-1.4D, 1.4D));
                var arrow = world.spawnArrow(spawn, new org.bukkit.util.Vector(0.0D, -1.7D, 0.0D), 1.7f, 10.0f);
                arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setCritical(false);
                arrow.setDamage(3.0D);
                arrow.setFireTicks(0);
            }
            world.spawnParticle(Particle.CRIT, base.clone().add(0.0D, 2.2D, 0.0D), 14, 0.55D, 1.2D, 0.55D, 0.08D);
            player.playSound(base, Sound.ENTITY_ARROW_SHOOT, 0.7f, 0.85f);
        }
    }

    private void triggerLuckyPillarsSupplyEvent(GameRoom room, List<Player> alivePlayers, int secondsElapsed) {
        Location center = room.getLuckyPillarsArenaCenter();
        World world = room.getGameWorld();
        if (center == null || world == null) {
            return;
        }
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_supply"));
        int dropCount = Math.max(3, Math.min(7, Math.max(1, alivePlayers.size())));
        dropCount = Math.max(dropCount, LUCKY_PILLARS_RANDOM_EVENT_SUPPLY_DROP_COUNT);
        for (int i = 0; i < dropCount; i++) {
            double angle = (Math.PI * 2.0D / dropCount) * i;
            double radius = i % 2 == 0 ? 1.8D : 3.4D;
            Location dropLoc = center.clone().add(Math.cos(angle) * radius, 8.0D + ThreadLocalRandom.current().nextDouble(0.0D, 2.5D),
                    Math.sin(angle) * radius);
            ItemStack item = createLuckyPillarsRandomItem(room, secondsElapsed + i + 1);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            item.setAmount(1);
            world.dropItem(dropLoc, item).setVelocity(new org.bukkit.util.Vector(
                    ThreadLocalRandom.current().nextDouble(-0.06D, 0.06D),
                    ThreadLocalRandom.current().nextDouble(-0.04D, 0.04D),
                    ThreadLocalRandom.current().nextDouble(-0.06D, 0.06D)));
            world.spawnParticle(Particle.END_ROD, dropLoc, 6, 0.08D, 0.18D, 0.08D, 0.02D);
            world.spawnParticle(Particle.GLOW, dropLoc, 5, 0.12D, 0.12D, 0.12D, 0.01D);
        }
        world.playSound(center, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM, 0.85f, 1.12f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0.0D, 1.2D, 0.0D), 14, 0.55D, 0.45D, 0.55D, 0.05D);
    }

    private void triggerLuckyPillarsThunderEvent(GameRoom room, List<Player> alivePlayers) {
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_thunder"));
        List<Player> targets = new ArrayList<>(alivePlayers);
        Collections.shuffle(targets);
        int limit = Math.min(LUCKY_PILLARS_RANDOM_EVENT_THUNDER_TARGETS, targets.size());
        for (int i = 0; i < limit; i++) {
            Player player = targets.get(i);
            Location strike = player.getLocation().clone();
            world.strikeLightningEffect(strike);
            world.spawnParticle(Particle.ELECTRIC_SPARK, strike.clone().add(0.0D, 1.0D, 0.0D), 22, 0.28D, 0.65D, 0.28D, 0.08D);
            world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.52f, 1.26f);
            if (room.isLuckyPillarsEliminated(player.getUniqueId())) {
                continue;
            }
            double damage = Math.min(Math.max(4.0D, player.getHealth() - 1.0D), 6.0D);
            if (damage > 0.0D) {
                player.damage(damage);
            }
            org.bukkit.util.Vector knock = new org.bukkit.util.Vector(
                    ThreadLocalRandom.current().nextDouble(-0.45D, 0.45D),
                    0.45D,
                    ThreadLocalRandom.current().nextDouble(-0.45D, 0.45D));
            player.setVelocity(knock);
            player.setFallDistance(0.0F);
        }
    }

    private void triggerLuckyPillarsMoonWalkEvent(GameRoom room, List<Player> alivePlayers) {
        Location center = room.getLuckyPillarsArenaCenter();
        World world = room.getGameWorld();
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_moon_walk"));
        for (Player player : alivePlayers) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, LUCKY_PILLARS_RANDOM_EVENT_MOON_TICKS, 1, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, LUCKY_PILLARS_RANDOM_EVENT_MOON_TICKS, 0, false, false, false));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.72f, 1.76f);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D), 10, 0.25D, 0.55D, 0.25D, 0.03D);
        }
        if (world != null && center != null) {
            world.spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 1.2D, 0.0D), 22, 0.95D, 0.75D, 0.95D, 0.05D);
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.85f, 1.42f);
        }
    }

    private void triggerLuckyPillarsChainSwapEvent(GameRoom room, List<Player> alivePlayers) {
        if (alivePlayers.size() <= 1) {
            return;
        }
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_chain_swap"));
        List<Location> locations = new ArrayList<>();
        for (Player player : alivePlayers) {
            locations.add(player.getLocation().clone());
        }
        Collections.rotate(locations, 1);
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player player = alivePlayers.get(i);
            Location from = player.getLocation().clone();
            Location to = locations.get(i).clone().add(0.0D, 0.01D, 0.0D);
            player.teleport(to);
            player.setFallDistance(0.0F);
            player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.1f);
            player.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.55D, 0.35D, 0.18D);
            player.getWorld().spawnParticle(Particle.PORTAL, to.clone().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.55D, 0.35D, 0.18D);
        }
    }

    private void triggerLuckyPillarsInvisibilityEvent(GameRoom room, List<Player> alivePlayers) {
        Location center = room.getLuckyPillarsArenaCenter();
        World world = room.getGameWorld();
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_event_invisibility"));
        for (Player player : alivePlayers) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, LUCKY_PILLARS_RANDOM_EVENT_INVISIBILITY_TICKS, 0, false, false, false));
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.65f, 1.18f);
            player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().clone().add(0.0D, 1.0D, 0.0D), 12, 0.3D, 0.45D, 0.3D, 0.02D);
        }
        if (center != null && world != null) {
            world.spawnParticle(Particle.WITCH, center.clone().add(0.0D, 1.1D, 0.0D), 24, 0.85D, 0.55D, 0.85D, 0.03D);
        }
    }

    private void updateLuckyPillarsBorder(GameRoom room, int secondsElapsed) {
        World world = room.getGameWorld();
        Location center = room.getLuckyPillarsArenaCenter();
        if (world == null || center == null || !center.getWorld().equals(world)) {
            return;
        }
        double radius = Math.max(24.0D, room.getLuckyPillarsBoundaryRadius());
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        int shrinkDelaySeconds = Math.min(120, Math.max(45, room.getLuckyPillarsGameTimeSeconds() / 3));
        if (secondsElapsed < shrinkDelaySeconds) {
            border.setSize(Math.max(1536.0D, radius * 8.0D));
            border.setDamageBuffer(64.0D);
            border.setDamageAmount(0.0D);
        } else {
            double progress = Math.min(1.0D,
                    (secondsElapsed - shrinkDelaySeconds) / (double) Math.max(1, room.getLuckyPillarsGameTimeSeconds() - shrinkDelaySeconds));
            double startDiameter = Math.max(1536.0D, radius * 8.0D);
            double endDiameter = Math.max(48.0D, radius * 2.0D);
            double currentDiameter = startDiameter - (startDiameter - endDiameter) * progress;
            border.setSize(Math.max(endDiameter, currentDiameter));
            border.setDamageBuffer(1.0D);
            border.setDamageAmount(1.0D);
        }
        if (secondsElapsed > 0 && secondsElapsed % 60 == 0) {
            int minute = secondsElapsed / 60;
            room.broadcast(plugin.getConfigManager().getLuckyPillarsPrefix()
                    + "§x§F§F§B§B§5§5⌛ §e第 " + minute + " §f分钟，继续守住你的柱子。");
        }
    }

    private boolean checkLuckyPillarsOut(Player player, GameRoom room, int secondsElapsed) {
        Location location = player.getLocation();
        World world = room.getGameWorld();
        if (world == null || !location.getWorld().equals(world)) {
            return eliminateLuckyPillarsPlayer(room, player, location, "§c离开了幸运之柱竞技场");
        }
        Location center = room.getLuckyPillarsArenaCenter();
        if (center != null && center.getWorld() != null && center.getWorld().equals(location.getWorld())) {
            double allowedRadius = Math.max(24.0D, room.getLuckyPillarsBoundaryRadius());
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > Math.pow(allowedRadius + 5.0D, 2.0D)) {
                return eliminateLuckyPillarsPlayer(room, player, location, "§c离柱群太远，被判定出界");
            }
        }
        return false;
    }

    private boolean eliminateLuckyPillarsPlayer(GameRoom room, Player player, Location location, String reason) {
        if (room == null || player == null || room.isLuckyPillarsEliminated(player.getUniqueId())) {
            return true;
        }
        Player killer = null;
        boolean summonKill = false;
        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            UUID ownerId = luckyPillarsSummonOwners.get(damageEvent.getDamager().getUniqueId());
            if (ownerId != null) {
                killer = Bukkit.getPlayer(ownerId);
                summonKill = true;
            } else if (damageEvent.getDamager() instanceof Player directKiller) {
                killer = directKiller;
            }
        }
        room.markLuckyPillarsEliminated(player.getUniqueId());
        creditLuckyPillarsKiller(room, player, killer, summonKill);
        Location deathLoc = location == null ? player.getLocation().clone() : location.clone();
        room.setPendingRespawnLocation(player.getUniqueId(), deathLoc.clone());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("alive", String.valueOf(room.getLuckyPillarsAlivePlayers().size()));
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_eliminated", placeholders));
        player.showTitle(Title.title(
                LegacyComponentSerializer.legacySection().deserialize("§x§F§F§8§8§5§5☠ §c已淘汰"),
                LegacyComponentSerializer.legacySection().deserialize(reason),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1800), Duration.ofMillis(300))));
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        org.bukkit.util.Vector bounce = player.getLocation().getDirection().normalize().multiply(-0.45D).setY(0.72D);
        player.setVelocity(bounce);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (Exception ignored) {
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> convertLuckyPillarsEliminatedToSpectator(player, room, deathLoc), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkLuckyPillarsWin(room), 4L);
        return true;
    }

    public void handleLuckyPillarsDamage(EntityDamageByEntityEvent event) {
        if (event == null || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(victim.getUniqueId());
        if (room == null || !room.getGameMode().isLuckyPillars() || room.getState() != RoomState.PLAYING) {
            return;
        }
        UUID ownerId = luckyPillarsSummonOwners.get(event.getDamager().getUniqueId());
        if (ownerId == null || ownerId.equals(victim.getUniqueId())) {
            return;
        }
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline() && !room.isLuckyPillarsEliminated(ownerId)) {
            victim.setKiller(owner);
        }
    }

    public void handleLuckyPillarsMobTarget(org.bukkit.event.entity.EntityTargetEvent event) {
        if (event == null || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        UUID ownerId = luckyPillarsSummonOwners.get(mob.getUniqueId());
        if (ownerId == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        GameRoom room = plugin.getRoomManager().getPlayerRoom(ownerId);
        if (room == null || !room.getGameMode().isLuckyPillars()) {
            return;
        }
        if (target.getUniqueId().equals(ownerId) || room.isSpectator(target.getUniqueId()) || room.isLuckyPillarsEliminated(target.getUniqueId())) {
            event.setCancelled(true);
            retargetLuckyPillarsMob(mob, ownerId, room);
        }
    }

    private void retargetLuckyPillarsMob(Mob mob, UUID ownerId, GameRoom room) {
        Player best = null;
        double nearest = Double.MAX_VALUE;
        for (UUID uuid : room.getLuckyPillarsAlivePlayers()) {
            if (uuid.equals(ownerId)) {
                continue;
            }
            Player candidate = Bukkit.getPlayer(uuid);
            if (candidate == null || !candidate.isOnline() || !candidate.getWorld().equals(mob.getWorld())) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(mob.getLocation());
            if (distance < nearest) {
                nearest = distance;
                best = candidate;
            }
        }
        if (best != null) {
            mob.setTarget(best);
        }
    }

    private void creditLuckyPillarsKiller(GameRoom room, Player victim, Player killer, boolean summonKill) {
        if (room == null || victim == null || killer == null || killer.getUniqueId().equals(victim.getUniqueId())
                || !room.getAllPlayerUUIDs().contains(killer.getUniqueId())
                || room.isLuckyPillarsEliminated(killer.getUniqueId())) {
            return;
        }
        room.addLuckyPillarsKill(killer.getUniqueId(), summonKill);
        plugin.getPlayerDataManager().addMiniGamePoints(killer.getUniqueId(), LUCKY_PILLARS_KILL_POINTS, room.getGameMode());
        Map<String, String> ph = new HashMap<>();
        ph.put("player", victim.getName());
        ph.put("points", String.valueOf(LUCKY_PILLARS_KILL_POINTS));
        killer.sendMessage(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("points.minigame_kill", ph));
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, summonKill ? 1.65f : 1.4f);
    }

    private void endLuckyPillarsGame(GameRoom room, UUID winnerUuid) {
        if (room.getState() == RoomState.ENDED) {
            return;
        }
        cleanupLuckyPillarsSummons(room);
        room.setState(RoomState.ENDED);
        room.setPreyWon(false);
        room.clearLuckyPillarBlocks();
        cleanupRandomCompassMode(room.getRoomId());
        cleanupSurvivalMode(room.getRoomId());
        cleanupThunderStormWeather(room);
        plugin.getPlayerListener().resetCompassTpState(room);
        plugin.getFlashModeManager().cleanupFlashRoomBackpacks(room);

        BukkitTask gameTask = gameTasks.remove(room.getRoomId());
        if (gameTask != null) {
            gameTask.cancel();
        }

        String winnerName = "无人";
        if (winnerUuid != null) {
            Player winner = Bukkit.getPlayer(winnerUuid);
            winnerName = winner == null ? Optional.ofNullable(Bukkit.getOfflinePlayer(winnerUuid).getName()).orElse("未知玩家") : winner.getName();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("winner", winnerName);
        room.broadcast(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("game.lucky_pillars_win", placeholders));

        safePlayLuckyPillarsVictoryEffect(room, winnerUuid);

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (uuid.equals(winnerUuid)) {
                plugin.getPlayerDataManager().addMiniGamePoints(uuid, LUCKY_PILLARS_WIN_POINTS, room.getGameMode());
                Map<String, String> ph = new HashMap<>();
                ph.put("points", String.valueOf(LUCKY_PILLARS_WIN_POINTS));
                player.sendMessage(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("points.minigame_win", ph));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.95f, 1.2f);
            } else {
                plugin.getPlayerDataManager().addMiniGamePoints(uuid, LUCKY_PILLARS_PARTICIPATE_POINTS, room.getGameMode());
                Map<String, String> ph = new HashMap<>();
                ph.put("points", String.valueOf(LUCKY_PILLARS_PARTICIPATE_POINTS));
                player.sendMessage(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("points.minigame_participate", ph));
            }
            plugin.getPlayerDataManager().incrementPlayCount(uuid, room.getGameMode());
            if (uuid.equals(winnerUuid)) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setFallDistance(0.0F);
            } else {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }

        Component title = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§D§5§5🍀 §e幸运之柱经典结束");
        Component subtitle = LegacyComponentSerializer.legacySection().deserialize("§f地图: §e" + room.getLuckyPillarsMapName() + " §7| §f胜者: §a" + winnerName);
        Title endTitle = Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(7000), Duration.ofMillis(800)));
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(endTitle);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.25f);
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(endTitle);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.25f);
            }
        }

        scheduleLuckyPillarsRoomClose(room);
    }

    private void playLuckyPillarsVictoryEffect(GameRoom room, UUID winnerUuid) {
        if (room == null || winnerUuid == null) {
            return;
        }
        Player winner = Bukkit.getPlayer(winnerUuid);
        Location center = winner != null && winner.isOnline()
                ? winner.getLocation().clone().add(0.0D, 1.0D, 0.0D)
                : room.getLuckyPillarsArenaCenter();
        if (center == null || center.getWorld() == null) {
            return;
        }
        String effectId = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(winnerUuid);
        if (!plugin.getPlayerDataManager().hasLuckyPillarsVictoryEffect(winnerUuid, effectId)) {
            effectId = "fireworks";
        }

        LuckyPillarsVictoryEffect effect = LuckyPillarsVictoryEffect.byId(effectId);
        Location effectCenter = center.clone();
        switch (effect) {
            case GOLDEN_PILLAR -> playGoldenPillarEffect(effectCenter);
            case CLOVER_RING -> playCloverRingEffect(effectCenter);
            case SKY_GIFT -> playSkyGiftEffect(effectCenter);
            case VOID_LOTUS -> playVoidLotusEffect(effectCenter);
            case HONEY_SPLASH -> playHoneySplashEffect(effectCenter);
            case PRISM_COLUMN -> playPrismColumnEffect(effectCenter);
            case TOTEM_GARDEN -> playTotemGardenEffect(effectCenter);
            case FIREWORKS -> spawnInstantVictoryFirework(effectCenter);
        }
    }

    private void spawnInstantVictoryFirework(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        org.bukkit.entity.Firework firework = location.getWorld().spawn(location, org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW)
                .withFade(org.bukkit.Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.BURST)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTask(plugin, firework::detonate);
    }

    private void safePlayLuckyPillarsVictoryEffect(GameRoom room, UUID winnerUuid) {
        try {
            playLuckyPillarsVictoryEffect(room, winnerUuid);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("幸运之柱胜利特效播放失败，已跳过特效并继续正常结束游戏: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void cleanupLuckyPillarsSummons(GameRoom room) {
        if (room == null) {
            return;
        }
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }
        Set<UUID> remove = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : luckyPillarsSummonOwners.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.getWorld().equals(world)) {
                if (entity != null) {
                    entity.remove();
                }
                remove.add(entry.getKey());
            }
        }
        remove.forEach(luckyPillarsSummonOwners::remove);
    }

    private void scheduleLuckyPillarsRoomClose(GameRoom room) {
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    if (plugin.getChildServerManager().isNodeMode()) {
                        plugin.getLogger().info("[LuckyPillars] 房间结束，节点服准备送回大厅并关闭: room=" + room.getRoomId()
                                + ", managedNode=" + plugin.getChildServerManager().isManagedNodeRoom(room.getRoomId())
                                + ", managedCross=" + plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId()));
                        plugin.getChildServerManager().returnManagedRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }

                    if (plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnCrossServerRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }

                    Set<UUID> restoreTargets = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    restoreTargets.addAll(room.getSpectators());
                    for (UUID uuid : restoreTargets) {
                        restoreLuckyPillarsPlayerAfterClose(room, uuid);
                    }
                    plugin.getRoomManager().clearAllRoleNameTags(room);
                    plugin.getRoomManager().deleteRoom(room.getRoomId());
                    cancel();
                    return;
                }

                if (countdown <= 5) {
                    Set<UUID> closeRecipients = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    closeRecipients.addAll(room.getSpectators());
                    for (UUID uuid : closeRecipients) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage("§x§F§F§5§5§5§5⏱ §c" + countdown + " §7秒后关闭...");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void restoreLuckyPillarsPlayerAfterClose(GameRoom room, UUID uuid) {
        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
    }

    public void startGame(GameRoom room) {
        // 取消倒计时任务
        cancelCountdown(room);
        cancelDivisionTask(room);
        room.setEndChapterDivisionActive(false);

        if (!usesHunterFlow(room)) {
            startIndependentMode(room);
            return;
        }

        room.setState(RoomState.SELECTING);

        // 猎物已确定，给所有玩家设置头顶职业前缀
        plugin.getRoomManager().applyRoleNameTags(room);

        // 给猎物黑暗buff（飞行等传送后再给，避免在大厅飞行）
        for (UUID uuid : getSelectionPreys(room)) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null) {
                prey.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 10, 0, false, false, false));
                prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
            }
        }

        // 使用倒计时3秒时已创建好的世界
        World gameWorld = room.getGameWorld();
        if (gameWorld == null) {
            // 如果世界还没创建（极端情况），在主线程创建
            gameWorld = plugin.getWorldManager().createGameWorld(room.getRoomId());
            if (gameWorld == null) {
                room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.lobby_creation_failed"));
                return;
            }
            room.setGameWorld(gameWorld);
        }

        final World gw = gameWorld;

        // 假进度条期间预加载区块
        Location spawnLoc0 = gw.getSpawnLocation();
        int chunkX0 = spawnLoc0.getBlockX() >> 4;
        int chunkZ0 = spawnLoc0.getBlockZ() >> 4;
        plugin.getWorldManager().preloadChunks(
                gw,
                chunkX0,
                chunkZ0,
                plugin.getConfigManager().getHunterGamePreloadRadius(),
                null
        );

        if (room.hasModifier("NoWorld")) {
            room.setCountdown(3);
        }

        // 开始假进度条动画，动画结束后用mvtp传送
        startLoadingAnimation(room, () -> {
            World currentGw = room.getGameWorld();
            if (currentGw == null) return;

            if (room.hasModifier("NoWorld")) {
                if (!isTournamentSilent(room)) {
                    room.broadcast(plugin.getConfigManager().getHunterGamePrefix() + "§x§8§8§D§D§F§F🌍 §b无世界修饰符已生效，正在直接传送到随机世界...");
                }
                confirmWorldAndStart(room);
                return;
            }

            for (UUID uuid : getSelectionPreys(room)) {
                Player prey = Bukkit.getPlayer(uuid);
                if (prey != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "mvtp " + prey.getName() + " " + currentGw.getName());
                    prey.removePotionEffect(PotionEffectType.DARKNESS);
                    prey.playSound(prey.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.5f);
                    // 传送后保持飞行（选择世界阶段）
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        prey.setAllowFlight(true);
                        prey.setFlying(true);
                        // 传送完成后给选择世界的按钮
                        giveWorldSelectionItems(prey);
                    }, 5L);
                }
            }
            startWorldSelection(room);
        });
    }

    private void startLoadingAnimation(GameRoom room, Runnable onComplete) {
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 60; // 3秒

            @Override
            public void run() {
                if (tick >= totalTicks) {
                    for (UUID uuid : getSelectionPreys(room)) {
                        Player prey = Bukkit.getPlayer(uuid);
                        if (prey != null) {
                            Component clear = LegacyComponentSerializer.legacySection().deserialize("");
                            prey.showTitle(Title.title(clear, clear,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1), Duration.ofMillis(200))));
                        }
                    }
                    cancel();
                    onComplete.run();
                    return;
                }

                // 两侧方块慢慢填满3格：左边3格从外到内，右边3格从外到内
                // 总共6格，每10tick填一格
                int filled = Math.min(3, tick / 10); // 0~3
                // 左侧：从左到右填
                String left = "§x§5§5§F§F§A§A".repeat(filled) + "§8▪".repeat(3 - filled);
                // 右侧：从右到左填（镜像）
                String right = "§8▪".repeat(3 - filled) + "§x§5§5§F§F§A§A".repeat(filled);
                // 替换为实际方块字符
                left = left.replace("§x§5§5§F§F§A§A", "§x§5§5§F§F§A§A█").replace("§8▪", "§8░");
                right = right.replace("§x§5§5§F§F§A§A", "§x§5§5§F§F§A§A█").replace("§8▪", "§8░");

                // 重新构建
                StringBuilder leftBar = new StringBuilder();
                StringBuilder rightBar = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    if (i < filled) leftBar.append("§x§5§5§F§F§A§A█");
                    else leftBar.append("§8░");
                }
                for (int i = 0; i < 3; i++) {
                    if (i < filled) rightBar.insert(0, "§x§5§5§F§F§A§A█");
                    else rightBar.insert(0, "§8░");
                }

                String bar = leftBar + " §x§8§8§D§D§F§F传送中 " + rightBar;
                // 标题文字根据进度变化
                String[] titleTexts = {"§x§F§F§D§7§0§0⏳ §x§F§F§B§B§3§3正在生成世界", "§x§F§F§D§7§0§0⌛ §x§F§F§B§B§3§3世界生成中", "§x§F§F§D§7§0§0✦ §x§F§F§B§B§3§3即将传送"};
                String titleText = titleTexts[Math.min(2, tick / 20)];

                Component titleComp = LegacyComponentSerializer.legacySection().deserialize(titleText);
                Component subComp = LegacyComponentSerializer.legacySection().deserialize(bar);
                Title title = Title.title(titleComp, subComp,
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(200)));
                for (UUID uuid : getSelectionPreys(room)) {
                    Player prey = Bukkit.getPlayer(uuid);
                    if (prey != null) {
                        if (!isTournamentSilent(room)) {
                            prey.showTitle(title);
                        }
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void startLoadingAnimationForReroll(GameRoom room, Runnable onComplete) {
        startLoadingAnimation(room, onComplete);
    }

    private void startIndependentMode(GameRoom room) {
        if (room == null) {
            return;
        }

        cancelCountdown(room);
        cancelDivisionTask(room);
        room.setEndChapterDivisionActive(false);

        if (room.getGameMode().isLuckyPillars()) {
            startLuckyPillars(room);
            return;
        }
        if (room.getGameMode().isBrickGuard()) {
            plugin.getBrickGuardManager().start(room);
            return;
        }
        if (room.getGameMode().isStandaloneMiniGame()) {
            if (room.getGameMode() == GameMode.TNT_RUN) {
                startTntRun(room);
            } else if (room.getGameMode() == GameMode.BLOCK_PARTY) {
                startBlockParty(room);
            }
            return;
        }
        room.setState(RoomState.PLAYING);
        room.setGameActuallyStarted(true);
        room.setPreyStarted(true);
        room.setGameStartTime(System.currentTimeMillis());
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        plugin.getRoomManager().clearAllRoleNameTags(room);

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFallDistance(0.0F);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.45f);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.55f, 1.85f);
            plugin.getRoomManager().setRoleNameTag(player, room.getRoomId(), false, "参赛");
            plugin.getRoomManager().updatePlayerTabNameWithRole(player, room.getRoomId(), false, "参赛");
        }

        room.broadcast(plugin.getMessageManager().getMiniGameMessageWithPrefix("game.independent_mode_started"));
        startIndependentModeGuardTask(room);
    }


    private void startIndependentModeGuardTask(GameRoom room) {
        if (room == null) {
            return;
        }
        BukkitTask existing = gameTasks.remove(room.getRoomId());
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        gameTasks.put(room.getRoomId(), task);
    }

    private String buildProgressBar(float progress) {
        int total = 20;
        int filled = (int) (progress * total);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("§x§5§5§F§F§A§A█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8] §f").append((int)(progress * 100)).append("§7%");
        return bar.toString();
    }

    private void startWorldSelection(GameRoom room) {
        // 重置世界切换计数器
        room.resetWorldRerollCount();
        room.clearNetherHunterScenarioVotes();
        room.setWorldSelectionConfirmed(false);
        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();

        if (room.getGameMode() == GameMode.NETHER_CHAPTER) {
            giveNetherHunterScenarioVoteItems(room);
        }

        // 给猎物2分钟选择世界
        if (!isTournamentSilent(room)) {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.world_selection_start"));
        }

        // 世界选择倒计时，显示在经验条上
        int worldSelectionTime = Math.max(1, plugin.getConfigManager().getConfig().getInt("hunter_game.world_selection_time", 120));
        room.setCountdown(worldSelectionTime);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (room.getState() != RoomState.SELECTING) {
                    cancel();
                    return;
                }

                int countdown = room.getCountdown();
                if (countdown <= 0) {
                    if (room.getGameMode() == GameMode.NETHER_CHAPTER && room.isWorldSelectionConfirmed()) {
                        room.setWorldSelectionConfirmed(false);
                        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.nether_position_vote_finished"));
                        confirmWorldAndStart(room);
                    } else if (room.hasPendingDualPreyProposal()) {
                        approveDualPreyWorldProposal(room, null, true);
                    } else {
                        // 时间到，强制开始
                        confirmWorldAndStart(room);
                    }
                    cancel();
                    return;
                }

                // 更新所有玩家的经验条（包括猎人和猎物）
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.setLevel(countdown);
                        // 计算经验条进度（0.0 - 1.0）
                        float progress = countdown / (float) worldSelectionTime;
                        p.setExp(progress);

                        // 保持猎物飞行
                        if (getSelectionPreys(room).contains(uuid) && !p.getAllowFlight()) {
                            p.setAllowFlight(true);
                            p.setFlying(true);
                        }

                        // 持续给猎人显示剩余时间主标题
                        if (!room.isPrey(uuid) && !isTournamentSilent(room)) {
                            String timeStr = formatTime(countdown);
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + timeStr);
                            String subtitle = room.getGameMode() == GameMode.NETHER_CHAPTER && room.isWorldSelectionConfirmed()
                                    ? "§x§8§8§D§D§F§F猎人投票时间中..."
                                    : "§x§8§8§D§D§F§F猎物选择世界中...";
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            p.showTitle(title);
                        } else if (room.getGameMode() == GameMode.NETHER_CHAPTER && room.isWorldSelectionConfirmed() && !isTournamentSilent(room)) {
                            String timeStr = formatTime(countdown);
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§5§5§F§F§D§D世界已锁定");
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7等待猎人投票 §e" + timeStr);
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            p.showTitle(title);
                        }
                    }
                }

                room.setCountdown(countdown - 1);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void lockNetherChapterWorldSelection(GameRoom room) {
        if (room == null) {
            return;
        }

        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();
        room.setWorldSelectionConfirmed(true);
        int voteSeconds = getNetherHunterVoteSeconds();
        if (room.getCountdown() > voteSeconds) {
            room.setCountdown(voteSeconds);
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("vote_time", formatChineseDuration(voteSeconds));
        String lockMessage = plugin.getMessageManager().getHunterGameMessageWithPrefix("game.nether_world_locked_wait", placeholders)
                .replace("1分钟", formatChineseDuration(voteSeconds))
                .replace("60秒", formatChineseDuration(voteSeconds));
        room.broadcast(lockMessage);

        for (UUID uuid : getSelectionPreys(room)) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey == null) {
                continue;
            }
            prey.getInventory().clear();
            prey.playSound(prey.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.2f);
            prey.playSound(prey.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.7f);
        }
    }

    public void giveWorldSelectionItemsPublic(Player player) {
        giveWorldSelectionItems(player);
    }

    public int getNetherHunterScenarioEffectiveWeight(GameRoom room, NetherHunterScenario scenario) {
        if (scenario == null) {
            return 0;
        }
        int votes = room == null ? 0 : room.getNetherHunterScenarioVoteCount(scenario);
        return Math.max(1, scenario.getBaseWeight() + votes * NETHER_SCENARIO_VOTE_BONUS);
    }

    public Map<NetherHunterScenario, Double> getNetherHunterScenarioProbabilities(GameRoom room) {
        Map<NetherHunterScenario, Integer> weights = getNetherHunterScenarioWeights(room);
        int totalWeight = 0;
        for (int weight : weights.values()) {
            totalWeight += weight;
        }

        Map<NetherHunterScenario, Double> probabilities = new EnumMap<>(NetherHunterScenario.class);
        for (NetherHunterScenario scenario : NetherHunterScenario.values()) {
            int weight = weights.getOrDefault(scenario, scenario.getBaseWeight());
            double probability = totalWeight <= 0 ? 0.0D : weight * 100.0D / totalWeight;
            probabilities.put(scenario, probability);
        }
        return probabilities;
    }

    private ItemStack createEndChapterPreyKitCompass(GameRoom room, Player player) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            EndChapterKit selected = room == null ? null : room.getEndPreyKitSelection(player.getUniqueId());
            meta.setDisplayName("§x§D§D§5§5§F§F🧭 §x§C§C§7§7§F§F猎§x§B§B§9§9§F§F物§x§A§A§B§B§F§FK§x§9§9§D§D§F§Fi§x§8§8§F§F§F§Ft选择");
            meta.setCustomModelData(END_PREY_KIT_COMPASS_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("recovery_compass"));
            meta.setLore(Arrays.asList(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a右键打开猎物Kit菜单",
                    "§f- §e当前选择: §b" + (selected == null ? "未选择" : selected.getDisplayName()),
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEndChapterPreyPositionCompass(GameRoom room, Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            EndPreyPosition selected = room == null ? null : room.getEndPreyPositionSelection(player.getUniqueId());
            meta.setDisplayName("§x§F§F§6§6§0§0🧭 §x§F§F§8§8§2§2猎§x§F§F§A§A§4§4物§x§F§F§C§C§6§6位§x§F§F§E§E§8§8置选择");
            meta.setCustomModelData(END_PREY_POSITION_COMPASS_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("compass"));
            meta.setLore(Arrays.asList(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a右键选择末地篇出生位置",
                    "§f- §e当前选择: §b" + (selected == null ? "未选择" : selected.getDisplayName()),
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEndChapterHunterKitCompass(GameRoom room, Player player) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            EndChapterKit vote = room == null ? null : room.getEndHunterKitVote(player.getUniqueId());
            meta.setDisplayName("§x§8§8§D§D§F§F🧭 §x§A§A§E§E§F§F猎§x§C§C§F§F§F§F人§x§D§D§F§F§C§CK§x§E§E§D§D§A§Ai§x§F§F§C§C§8§8t投票");
            meta.setCustomModelData(END_HUNTER_KIT_COMPASS_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("recovery_compass"));
            meta.setLore(Arrays.asList(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a右键打开猎人Kit投票菜单",
                    "§f- §e你的当前倾向: §b" + (vote == null ? "未投票" : vote.getDisplayName()),
                    "§f- §d越稀有的Kit越难被分到",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEndChapterHunterPositionCompass(GameRoom room, Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            EndHunterPosition vote = room == null ? null : room.getEndHunterPositionVote(player.getUniqueId());
            meta.setDisplayName("§x§5§5§F§F§D§D🧭 §x§7§7§F§F§E§E猎§x§9§9§F§F§F§F人§x§B§B§E§E§F§F位§x§D§D§D§D§F§F置投票");
            meta.setCustomModelData(END_HUNTER_POSITION_COMPASS_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("compass"));
            meta.setLore(Arrays.asList(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §a右键打开猎人位置投票菜单",
                    "§f- §e你的当前倾向: §b" + (vote == null ? "未投票" : vote.getDisplayName()),
                    "§f- §d投票会提高你自己抽到该位置的概率",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void setEndChapterPreyKit(GameRoom room, Player player, EndChapterKit kit) {
        if (room == null || player == null || kit == null) {
            return;
        }
        room.setEndPreyKitSelection(player.getUniqueId(), kit);
        giveEndChapterDivisionItems(player, room);
    }

    public void setEndChapterPreyPosition(GameRoom room, Player player, EndPreyPosition position) {
        if (room == null || player == null || position == null) {
            return;
        }
        room.setEndPreyPositionSelection(player.getUniqueId(), position);
        giveEndChapterDivisionItems(player, room);
    }

    public void setEndChapterHunterKitVote(GameRoom room, Player player, EndChapterKit kit) {
        if (room == null || player == null || kit == null) {
            return;
        }
        room.setEndHunterKitVote(player.getUniqueId(), kit);
        giveEndChapterDivisionItems(player, room);
    }

    public void setEndChapterHunterPositionVote(GameRoom room, Player player, EndHunterPosition position) {
        if (room == null || player == null || position == null) {
            return;
        }
        room.setEndHunterPositionVote(player.getUniqueId(), position);
        giveEndChapterDivisionItems(player, room);
    }

    public boolean requiresDualPreyWorldAgreement(GameRoom room) {
        return room != null
                && (room.isDoublePreyEnabled() || room.getGameMode() == GameMode.SWAP)
                && room.getState() == RoomState.SELECTING
                && room.getPreyUUIDs().size() >= 2;
    }

    public void startDualPreyWorldProposal(GameRoom room, Player initiator, DualPreyProposalType proposalType) {
        if (!requiresDualPreyWorldAgreement(room) || initiator == null) {
            return;
        }

        if (room.hasPendingDualPreyProposal()) {
            initiator.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_waiting_other"));
            initiator.playSound(initiator.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        room.beginDualPreyProposal(proposalType, initiator.getUniqueId(), DOUBLE_PREY_WORLD_DECISION_SECONDS);
        refreshWorldSelectionItems(room);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", initiator.getName());
        placeholders.put("time", String.valueOf(DOUBLE_PREY_WORLD_DECISION_SECONDS));
        String key = proposalType == DualPreyProposalType.CONFIRM_WORLD
                ? "game.double_prey_world_pending"
                : "game.double_prey_reroll_pending";
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(key, placeholders));

        cancelDualPreyDecisionTask(room);
        BukkitTask task = new BukkitRunnable() {
            int countdown = DOUBLE_PREY_WORLD_DECISION_SECONDS;

            @Override
            public void run() {
                if (room.getState() != RoomState.SELECTING
                        || !room.hasPendingDualPreyProposal()
                        || room.getDualPreyProposalType() != proposalType
                        || !Objects.equals(room.getDualPreyProposalInitiator(), initiator.getUniqueId())) {
                    cancelDualPreyDecisionTask(room);
                    return;
                }

                room.setDualPreyProposalCountdown(countdown);
                showDualPreyDecisionTitle(room, proposalType, countdown);

                if (countdown <= 0) {
                    approveDualPreyWorldProposal(room, null, true);
                    return;
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        dualPreyDecisionTasks.put(room.getRoomId(), task);
    }

    public void approveDualPreyWorldProposal(GameRoom room, Player responder, boolean autoApproved) {
        if (room == null || !room.hasPendingDualPreyProposal()) {
            return;
        }

        DualPreyProposalType proposalType = room.getDualPreyProposalType();
        UUID initiatorUuid = room.getDualPreyProposalInitiator();

        if (!autoApproved && responder != null && Objects.equals(responder.getUniqueId(), initiatorUuid)) {
            responder.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_waiting_other"));
            responder.playSound(responder.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        cancelDualPreyDecisionTask(room);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", responder == null ? "系统" : responder.getName());
        String key;
        if (proposalType == DualPreyProposalType.CONFIRM_WORLD) {
            key = autoApproved ? "game.double_prey_world_auto_confirmed" : "game.double_prey_world_confirmed";
        } else {
            key = autoApproved ? "game.double_prey_reroll_auto_confirmed" : "game.double_prey_reroll_confirmed";
        }
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(key, placeholders));
        room.clearDualPreyProposal();

        if (proposalType == DualPreyProposalType.CONFIRM_WORLD) {
            if (room.getGameMode() == GameMode.NETHER_CHAPTER && room.getCountdown() > getNetherHunterVoteSeconds()) {
                lockNetherChapterWorldSelection(room);
            } else {
                confirmSelectedWorld(room);
            }
        } else {
            rerollSelectedWorld(room);
        }
    }

    public void rejectDualPreyWorldProposal(GameRoom room, Player responder) {
        if (room == null || responder == null || !room.hasPendingDualPreyProposal()) {
            return;
        }

        if (Objects.equals(room.getDualPreyProposalInitiator(), responder.getUniqueId())) {
            responder.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_waiting_other"));
            responder.playSound(responder.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", responder.getName());
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_world_rejected", placeholders));

        refreshWorldSelectionItems(room);
    }

    private void cancelDualPreyDecisionTask(GameRoom room) {
        if (room == null) {
            return;
        }

        BukkitTask task = dualPreyDecisionTasks.remove(room.getRoomId());
        if (task != null) {
            task.cancel();
        }
    }

    private void refreshWorldSelectionItems(GameRoom room) {
        for (UUID uuid : getSelectionPreys(room)) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null) {
                giveWorldSelectionItems(prey);
            }
        }
    }

    private void showDualPreyDecisionTitle(GameRoom room, DualPreyProposalType proposalType, int secondsLeft) {
        if (isTournamentSilent(room)) {
            return;
        }
        String subtitle = proposalType == DualPreyProposalType.CONFIRM_WORLD
                ? "§7另一位猎物若不反驳，将直接确定当前世界"
                : "§7另一位猎物若不反驳，将直接更换世界";

        for (UUID uuid : getSelectionPreys(room)) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey == null) {
                continue;
            }

            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + secondsLeft + " §6秒");
            Component subComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
            Title title = Title.title(titleComp, subComp,
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
            prey.showTitle(title);
        }
    }

    private void giveWorldSelectionItems(Player player) {
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        player.getInventory().clear();

        if (room != null && requiresDualPreyWorldAgreement(room) && room.hasPendingDualPreyProposal()) {
            player.getInventory().setItem(3, createWorldSelectionConfirmItem(room));
            player.getInventory().setItem(4, createWorldSelectionRebutItem(room));
            return;
        }

        player.getInventory().setItem(3, createWorldSelectionConfirmItem(room));
        player.getInventory().setItem(5, createWorldSelectionRerollItem());
    }

    private ItemStack createWorldSelectionConfirmItem(GameRoom room) {
        ItemStack confirm = new ItemStack(Material.PAPER);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            boolean pendingProposal = room != null && room.hasPendingDualPreyProposal();
            boolean rerollProposal = pendingProposal && room.getDualPreyProposalType() == DualPreyProposalType.REROLL_WORLD;
            confirmMeta.setDisplayName(rerollProposal
                    ? "§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7同§x§9§9§F§F§9§9意§x§B§B§F§F§B§B换§x§D§D§F§F§D§D世§f界"
                    : "§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7选§x§9§9§F§F§9§9择§x§B§B§F§F§B§B当§x§D§D§F§F§D§D前§f世界");
            confirmMeta.setCustomModelData(10005);
            confirmMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("lime_dye"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            if (pendingProposal) {
                lore.add(rerollProposal ? "§f- §a右键同意更换到新世界" : "§f- §a右键同意锁定当前世界");
                lore.add("§f- §d如果你不操作，倒计时结束后也会自动同意");
            } else if (requiresDualPreyWorldAgreement(room)) {
                lore.add("§f- §a右键发起当前世界确认");
                lore.add("§f- §d另一位猎物有20秒可确认或反驳");
            } else {
                lore.add("§f- §a右键确认选择此世界");
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            confirmMeta.setLore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        return confirm;
    }

    private ItemStack createWorldSelectionRerollItem() {
        ItemStack reroll = new ItemStack(Material.PAPER);
        ItemMeta rerollMeta = reroll.getItemMeta();
        if (rerollMeta != null) {
            rerollMeta.setDisplayName("§x§F§F§5§5§5§5🔄 §x§F§F§7§7§7§7换§x§F§F§9§9§9§9一§x§F§F§B§B§B§B个§x§F§F§D§D§D§D世界");
            rerollMeta.setCustomModelData(10006);
            rerollMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("red_dye"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c右键重新生成世界");
            lore.add("§8· · · · · · · · · · · · · ·");
            rerollMeta.setLore(lore);
            reroll.setItemMeta(rerollMeta);
        }
        return reroll;
    }

    private ItemStack createWorldSelectionRebutItem(GameRoom room) {
        ItemStack rebut = new ItemStack(Material.PAPER);
        ItemMeta rebutMeta = rebut.getItemMeta();
        if (rebutMeta != null) {
            boolean rerollProposal = room != null && room.getDualPreyProposalType() == DualPreyProposalType.REROLL_WORLD;
            rebutMeta.setDisplayName(rerollProposal
                    ? "§x§F§F§6§6§0§0✗ §x§F§F§8§8§2§2反§x§F§F§A§A§4§4驳§x§F§F§C§C§6§6换§x§F§F§E§E§8§8世§f界"
                    : "§x§F§F§6§6§0§0✗ §x§F§F§8§8§2§2反§x§F§F§A§A§4§4驳§x§F§F§C§C§6§6当§x§F§F§E§E§8§8前§f世界");
            rebutMeta.setCustomModelData(DOUBLE_PREY_REBUT_MODEL);
            rebutMeta.setItemModel(org.bukkit.NamespacedKey.minecraft("barrier"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §c右键取消这次提议");
            lore.add("§f- §7取消后两位猎物可继续重新选择");
            lore.add("§8· · · · · · · · · · · · · ·");
            rebutMeta.setLore(lore);
            rebut.setItemMeta(rebutMeta);
        }
        return rebut;
    }

    public void confirmSelectedWorld(GameRoom room) {
        if (room == null) {
            return;
        }

        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();

        for (UUID uuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null) {
                prey.getInventory().clear();
                prey.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 20 * 8, 0, false, false, false));
            }
        }

        startLoadingAnimationForReroll(room, () -> confirmWorldAndStart(room));
    }

    public void rerollSelectedWorld(GameRoom room) {
        if (room == null) {
            return;
        }

        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();

        World oldWorld = room.getGameWorld();

        room.incrementWorldRerollCount();

        for (UUID uuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null) {
                prey.getInventory().clear();
                prey.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 20 * 8, 0, false, false, false));
            }
        }

        String newRoomId = room.getRoomId() + "_" + System.currentTimeMillis();
        World newWorld = plugin.getWorldManager().createGameWorld(newRoomId);
        if (newWorld != null) {
            plugin.getWorldManager().remapGameWorld(room.getRoomId(), newWorld);
            Location sp = newWorld.getSpawnLocation();
            plugin.getWorldManager().preloadChunks(
                    newWorld,
                    sp.getBlockX() >> 4,
                    sp.getBlockZ() >> 4,
                    plugin.getConfigManager().getHunterGamePreloadRadius(),
                    null
            );
        }
        final World finalNewWorld = newWorld;

        startLoadingAnimationForReroll(room, () -> {
            if (finalNewWorld == null) {
                return;
            }

            room.setGameWorld(finalNewWorld);
            for (UUID uuid : room.getPreyUUIDs()) {
                Player prey = Bukkit.getPlayer(uuid);
                if (prey == null) {
                    continue;
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "mvtp " + prey.getName() + " " + finalNewWorld.getName());
                prey.removePotionEffect(PotionEffectType.DARKNESS);
                prey.playSound(prey.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.5f);
                prey.playSound(prey.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                prey.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.world_rerolled"));
                giveWorldSelectionItemsPublic(prey);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    prey.setAllowFlight(true);
                    prey.setFlying(true);
                }, 5L);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getWorldManager().deleteWorld(oldWorld), 20L);
        });
    }

    private void giveNetherHunterScenarioVoteItems(GameRoom room) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isHunter(uuid)) {
                continue;
            }
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter == null) {
                continue;
            }

            hunter.getInventory().setItem(1, createNetherHunterVoteCompass());
            hunter.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.nether_position_vote_available"));
            hunter.playSound(hunter.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.35f);
            hunter.playSound(hunter.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.7f);
        }
    }

    private ItemStack createNetherHunterVoteCompass() {
        ItemStack compass = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§8§8§D§D§F§F🧭 §x§A§A§E§E§F§F幽§x§C§C§F§F§F§F匿§x§D§D§F§F§C§C指§x§E§E§D§D§A§A南§x§F§F§C§C§8§8针");
            meta.setCustomModelData(NETHER_SCENARIO_VOTE_COMPASS_MODEL);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("recovery_compass"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b下界篇等待阶段专属道具");
            lore.add("§f- §e右键打开猎人落点投票菜单");
            lore.add("§f- §d票数越多，对应落点概率越高");
            lore.add("§f- §a菜单会实时刷新当前概率");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    public void confirmWorldAndStart(GameRoom room) {
        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();
        room.setWorldSelectionConfirmed(false);
        room.setState(RoomState.PLAYING);
        if (room.getGameMode().isFlashLike()) {
            room.setEndFlashDragonDefeated(false);
        }

        // 只为当前模式提前创建必须用到的维度，其他维度懒加载，避免开局一次性创建 3 个世界
        if (plugin.getConfigManager().isLazyDimensionCreationEnabled()) {
            if (room.getGameMode() == GameMode.NETHER_CHAPTER) {
                plugin.getWorldManager().ensureNetherWorld(room.getRoomId());
            } else if (room.getGameMode() == GameMode.END_CHAPTER) {
                plugin.getWorldManager().ensureEndWorld(room.getRoomId());
            } else if (room.getGameMode() == GameMode.END_FLASH) {
                // 终章 · 闪光 本身就是末地世界，不创建主世界/下界/额外末地。
            }
        } else {
            if (room.getGameMode() != GameMode.END_FLASH) {
                plugin.getWorldManager().createGameWorldDimensions(room.getRoomId());
            }
        }
        plugin.getWorldManager().setLocatorBarEnabled(room.getRoomId(), !room.hasModifier("NoLocatorBar"));

        String startedKey = isPreyManualStartAllowed(room)
                ? "game.started"
                : (room.getGameMode() == GameMode.END_FLASH ? "game.started_auto_end_flash" : "game.started_auto");
        if (!isTournamentSilent(room)) {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(startedKey));
        }

        // 给所有人显示屏幕标题：猎物→游戏即将开始，猎人→正在传送中（不是等待猎物按开始）
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (room.isPrey(uuid)) {
                    Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⚔ §x§F§F§B§B§3§3游戏即将开始");
                    Component subComp = LegacyComponentSerializer.legacySection().deserialize("§73秒后传送至游戏世界...");
                    Title title = Title.title(titleComp, subComp,
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(3500), Duration.ofMillis(500)));
                    if (!isTournamentSilent(room)) {
                        p.showTitle(title);
                    }
                } else {
                    // 猎人：正在传送中（等传送到后再显示倒计时）
                    Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§8§8§D§D§F§F⏳ §b正在传送中...");
                    Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7即将传送到游戏世界...");
                    Title title = Title.title(titleComp, subComp,
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(3500), Duration.ofMillis(500)));
                    if (!isTournamentSilent(room)) {
                        p.showTitle(title);
                    }
                }
                p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);
            }
        }

        // 3秒后传送所有人
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (room.getGameMode() == GameMode.NETHER_CHAPTER) {
                teleportNetherChapterPlayers(room);
            } else if (room.getGameMode() == GameMode.END_CHAPTER) {
                teleportEndChapterPlayers(room);
            } else if (room.getGameMode() == GameMode.FLASH || room.getGameMode() == GameMode.FLASH_TOURNAMENT) {
                teleportFlashModePlayers(room);
            } else if (room.getGameMode() == GameMode.END_FLASH) {
                teleportEndFlashPlayers(room);
            } else {
                Location spawnLoc = getSafeSpawnLocation(room.getGameWorld().getSpawnLocation());

                // 传送猎物
                for (UUID uuid : room.getPreyUUIDs()) {
                    Player prey = Bukkit.getPlayer(uuid);
                    if (prey != null) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + prey.getName() + " " + room.getGameWorld().getName());
                        prey.getInventory().clear();
                        prey.setLevel(0);
                        prey.setExp(0);

                        if (room.getGameMode() == GameMode.SWAP && room.isSwapCountdownPrey(uuid)) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                applySwapCountdownState(prey, room);
                                plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true);
                            }, 10L);
                        } else {
                            prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
                            prey.setAllowFlight(false);
                            prey.setFlying(false);
                            plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true);
                        }
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> stackDualPreysBeforeStart(room), 20L);

                List<UUID> hunters = new ArrayList<>();
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    if (room.isHunter(uuid)) hunters.add(uuid);
                }

                int hunterCount = hunters.size();
                if (hunterCount > 0) {
                    double radius = 1.8 + (hunterCount - 1) * 0.5;
                    double angleStep = 360.0 / hunterCount;

                    for (int i = 0; i < hunterCount; i++) {
                        UUID hunterUUID = hunters.get(i);
                        Player hunter = Bukkit.getPlayer(hunterUUID);
                        if (hunter != null) {
                            double angle = Math.toRadians(angleStep * i);
                            double x = spawnLoc.getX() + radius * Math.cos(angle);
                            double z = spawnLoc.getZ() + radius * Math.sin(angle);
                            Location hunterLoc = getSafeSpawnLocation(new Location(
                                room.getGameWorld(), x, spawnLoc.getY(), z,
                                (float) Math.toDegrees(Math.atan2(spawnLoc.getZ() - z, spawnLoc.getX() - x)) + 90, 0));
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + hunter.getName() + " " + room.getGameWorld().getName());
                            final Location finalLoc = hunterLoc;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                double dx = spawnLoc.getX() - finalLoc.getX();
                                double dz = spawnLoc.getZ() - finalLoc.getZ();
                                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                                Location facingLoc = finalLoc.clone();
                                facingLoc.setYaw(yaw);
                                facingLoc.setPitch(0);
                                hunter.teleport(facingLoc);
                                hunter.getInventory().clear();
                                hunter.setGameMode(org.bukkit.GameMode.SURVIVAL);
                                hunter.setLevel(0);
                                hunter.setExp(0);
                                if (!room.hasModifier("YesHunterSee")) {
                                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
                                }
                                hunter.playSound(hunter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                                plugin.getRoomManager().updatePlayerTabNameWithRole(hunter, room.getRoomId(), false);
                            }, 10L);
                        }
                    }
                }
            }

            // 给猎物开始按钮和音效（延迟40L确保mvtp传送完成后再给按钮），同时开始30秒预开始倒计时
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID uuid : getSelectionPreys(room)) {
                    Player prey = Bukkit.getPlayer(uuid);
                    if (prey != null) {
                        if (isPreyManualStartAllowed(room)) {
                            giveStartButton(prey);
                        }
                        prey.playSound(prey.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        prey.playSound(prey.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                    }
                }
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    if (!room.isPrey(uuid)) {
                        Player hunter = Bukkit.getPlayer(uuid);
                        if (hunter != null) {
                            hunter.playSound(hunter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                            hunter.playSound(hunter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                        }
                    }
                }

                // 开始游戏计时任务（指南针追踪等）
                startGameTask(room);

                if (room.getGameMode().isFlashTournament()) {
                    // 赛事模式不走任何开局倒计时；猎物离开初始点0.8格后才正式开始。
                    Bukkit.getScheduler().runTaskLater(plugin, () -> faceHuntersToNearestPrey(room), 20L);
                    return;
                }

                // 开始30秒预开始倒计时
                startPreGameCountdown(room);

                // 传送完成后精准调整猎人朝向（看向猎物实际位置）
                Bukkit.getScheduler().runTaskLater(plugin, () -> faceHuntersToNearestPrey(room), 20L); // 1秒后精确对准猎物
            }, 40L);

        }, 60L); // 3秒后
    }

    private void faceHuntersToNearestPrey(GameRoom room) {
        if (room == null) {
            return;
        }
        for (UUID hunterUUID : room.getAllPlayerUUIDs()) {
            if (!room.isHunter(hunterUUID)) continue;
            Player hunter = Bukkit.getPlayer(hunterUUID);
            if (hunter == null) continue;
            for (UUID preyUUID : getTrackablePreys(room)) {
                Player prey = Bukkit.getPlayer(preyUUID);
                if (prey != null && prey.getWorld().equals(hunter.getWorld())) {
                    double dx = prey.getX() - hunter.getX();
                    double dy = (prey.getY() + 1.62) - (hunter.getY() + 1.62);
                    double dz = prey.getZ() - hunter.getZ();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
                    Location facingPreyLoc = hunter.getLocation().clone();
                    facingPreyLoc.setYaw(yaw);
                    facingPreyLoc.setPitch(Math.max(-90, Math.min(90, pitch)));
                    // 这里只需要校正视角，不要再 teleport。
                    // 进入世界后的预开始倒计时里重复 teleport 会在玩家身上触发类似末影传送的闪光/抖动。
                    hunter.setRotation(facingPreyLoc.getYaw(), facingPreyLoc.getPitch());
                    break;
                }
            }
        }
    }

    private void teleportFlashModePlayers(GameRoom room) {
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }
        if (room.getGameMode().isFlashTournament()) {
            room.clearFlashTournamentStartLocations();
            // mvtp后还有10tick二次定位；按“进入世界后2秒”锁移动，这里留出传送延迟余量。
            room.setFlashTournamentMovementUnlockMillis(System.currentTimeMillis() + 3000L);
        }

        Location preyCenter = getSafeSpawnLocation(world.getSpawnLocation());
        if (preyCenter == null) {
            preyCenter = world.getSpawnLocation().clone();
        }
        final Location preyCenterFinal = preyCenter.clone();

        List<UUID> preyOrder = new ArrayList<>();
        if (room.getLockedFirstDualPrey() != null && room.getPreyUUIDs().contains(room.getLockedFirstDualPrey())) {
            preyOrder.add(room.getLockedFirstDualPrey());
        }
        for (UUID uuid : room.getPreyUUIDs()) {
            if (!preyOrder.contains(uuid)) {
                preyOrder.add(uuid);
            }
        }

        for (UUID preyUuid : preyOrder) {
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + prey.getName() + " " + world.getName());
            final Location finalSpawn = preyCenterFinal.clone();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                prey.teleport(finalSpawn);
                prey.getInventory().clear();
                prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
                prey.setAllowFlight(false);
                prey.setFlying(false);
                prey.setLevel(0);
                prey.setExp(0);
                if (room.getGameMode().isFlashTournament()) {
                    room.setFlashTournamentStartLocation(preyUuid, prey.getLocation());
                }
                plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true);
                prey.playSound(prey.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.15f);
            }, 10L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> stackDualPreysBeforeStart(room), 25L);

        List<UUID> hunters = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid)) {
                hunters.add(uuid);
            }
        }

        int[] ringCounts = new int[3];
        for (int i = 0; i < hunters.size(); i++) {
            ringCounts[i % 3]++;
        }
        double[] radii = room.getGameMode().isFlashTournament()
                ? new double[]{2.1D, 3.0D, 3.8D}
                : new double[]{2.4D, 5.0D, 7.6D};
        int hunterIndex = 0;
        for (int ring = 0; ring < ringCounts.length; ring++) {
            if (ringCounts[ring] <= 0) {
                continue;
            }
            double angleStep = 360.0D / ringCounts[ring];
            for (int indexInRing = 0; indexInRing < ringCounts[ring]; indexInRing++) {
                UUID hunterUuid = hunters.get(hunterIndex++);
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter == null) {
                    continue;
                }
                double angle = Math.toRadians(angleStep * indexInRing + ring * 17.0D);
                double x = preyCenterFinal.getX() + radii[ring] * Math.cos(angle);
                double z = preyCenterFinal.getZ() + radii[ring] * Math.sin(angle);
                Location hunterLoc = getSafeSpawnLocation(new Location(world, x, preyCenterFinal.getY(), z));
                if (hunterLoc == null) {
                    hunterLoc = new Location(world, x, preyCenterFinal.getY(), z);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + hunter.getName() + " " + world.getName());
                final Location finalLoc = hunterLoc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    double dx = preyCenterFinal.getX() - finalLoc.getX();
                    double dz = preyCenterFinal.getZ() - finalLoc.getZ();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    Location facingLoc = finalLoc.clone();
                    facingLoc.setYaw(yaw);
                    facingLoc.setPitch(0);
                    hunter.teleport(facingLoc);
                    hunter.getInventory().clear();
                    hunter.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    hunter.setLevel(0);
                    hunter.setExp(0);
                    if (!room.getGameMode().isFlashTournament() && !room.hasModifier("YesHunterSee")) {
                        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
                    }
                    hunter.playSound(hunter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    plugin.getRoomManager().updatePlayerTabNameWithRole(hunter, room.getRoomId(), false);
                }, 10L);
            }
        }
    }

    private void teleportEndFlashPlayers(GameRoom room) {
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }

        Location preyCenter = findEndFlashOpeningCenter(world);
        if (preyCenter == null) {
            preyCenter = new Location(world, 100.5D, 50.0D, 0.5D);
            prepareEndFlashObsidianPlatform(preyCenter);
        }
        final Location preyCenterFinal = preyCenter.clone();

        List<UUID> preyOrder = new ArrayList<>();
        if (room.getLockedFirstDualPrey() != null && room.getPreyUUIDs().contains(room.getLockedFirstDualPrey())) {
            preyOrder.add(room.getLockedFirstDualPrey());
        }
        for (UUID uuid : room.getPreyUUIDs()) {
            if (!preyOrder.contains(uuid)) {
                preyOrder.add(uuid);
            }
        }

        for (UUID preyUuid : preyOrder) {
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + prey.getName() + " " + world.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ensureEndEntryObsidianFooting(preyCenterFinal);
                prey.teleport(preyCenterFinal);
                prey.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                prey.setFallDistance(0.0F);
                prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
                prey.setAllowFlight(false);
                prey.setFlying(false);
                prey.setLevel(0);
                prey.setExp(0);
                EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().applyKit(prey, EndFlashKitManager.Role.PREY);
                String kitLabel = kit == null ? null : kit.displayName();
                room.assignEndFlashKitName(prey.getUniqueId(), kitLabel);
                room.assignEndFlashKitStartExpLevel(prey.getUniqueId(), kit == null ? 0 : kit.startExpLevel());
                if (room.getPreyUUIDs().size() >= 2) {
                    giveFlashPreyItems(prey, room);
                }
                prey.setRespawnLocation(preyCenterFinal.clone(), true);
                plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true, kitLabel);
                plugin.getRoomManager().setRoleNameTag(prey, room.getRoomId(), true, kitLabel);
                syncEndDimensionBrightness(prey, room);
                prey.playSound(prey.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.72f, 1.18f);
                prey.playSound(prey.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.52f, 1.55f);
                if (kit != null) {
                    prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                            + "§x§B§B§8§8§F§F✦ §d终章 Kit：§f" + kit.displayName());
                    prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                            + "§x§D§D§A§A§F§F» §dKit创建者: §f" + kit.creatorName()
                            + " §8/ §7" + kit.createdAtText());
                    prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                            + "§x§8§8§D§D§F§F» §bKit最后编辑: §f" + kit.lastEditorName()
                            + " §8/ §e" + kit.lastEditedAtText());
                    plugin.getEndFlashKitManager().sendKitGuide(prey, kit);
                }
            }, 10L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> stackDualPreysBeforeStart(room), 25L);

        List<UUID> hunters = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid)) {
                hunters.add(uuid);
            }
        }

        int[] ringCounts = new int[3];
        for (int i = 0; i < hunters.size(); i++) {
            ringCounts[i % 3]++;
        }
        double[] radii = {3.0D, 5.8D, 8.6D};
        int hunterIndex = 0;
        for (int ring = 0; ring < ringCounts.length; ring++) {
            if (ringCounts[ring] <= 0) {
                continue;
            }
            double angleStep = 360.0D / ringCounts[ring];
            for (int indexInRing = 0; indexInRing < ringCounts[ring]; indexInRing++) {
                UUID hunterUuid = hunters.get(hunterIndex++);
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter == null) {
                    continue;
                }
                double angle = Math.toRadians(angleStep * indexInRing + ring * 18.0D);
                double x = preyCenterFinal.getX() + radii[ring] * Math.cos(angle);
                double z = preyCenterFinal.getZ() + radii[ring] * Math.sin(angle);
                Location hunterLoc = getSafeSpawnLocation(new Location(world, x, preyCenterFinal.getY(), z));
                if (hunterLoc == null) {
                    hunterLoc = new Location(world, x, preyCenterFinal.getY(), z);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + hunter.getName() + " " + world.getName());
                final Location finalLoc = hunterLoc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ensureEndEntryObsidianFooting(finalLoc);
                    double dx = preyCenterFinal.getX() - finalLoc.getX();
                    double dz = preyCenterFinal.getZ() - finalLoc.getZ();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    Location facingLoc = finalLoc.clone();
                    facingLoc.setYaw(yaw);
                    facingLoc.setPitch(0);
                    hunter.teleport(facingLoc);
                    hunter.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    hunter.setFallDistance(0.0F);
                    hunter.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    hunter.setLevel(0);
                    hunter.setExp(0);
                    if (!room.hasModifier("YesHunterSee")) {
                        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
                    }
                    EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().applyKit(hunter, EndFlashKitManager.Role.HUNTER);
                    String kitLabel = kit == null ? null : kit.displayName();
                    room.assignEndFlashKitName(hunter.getUniqueId(), kitLabel);
                    room.assignEndFlashKitStartExpLevel(hunter.getUniqueId(), kit == null ? 0 : kit.startExpLevel());
                    giveHunterItems(hunter, room);
                    hunter.setRespawnLocation(finalLoc.clone(), true);
                    syncEndDimensionBrightness(hunter, room);
                    hunter.playSound(hunter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    hunter.playSound(hunter.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.42f, 1.65f);
                    plugin.getRoomManager().updatePlayerTabNameWithRole(hunter, room.getRoomId(), false, kitLabel);
                    plugin.getRoomManager().setRoleNameTag(hunter, room.getRoomId(), false, kitLabel);
                    if (kit != null) {
                        hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                                + "§x§F§F§D§D§8§8✦ §e终章 Kit：§f" + kit.displayName());
                        hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                                + "§x§D§D§A§A§F§F» §dKit创建者: §f" + kit.creatorName()
                                + " §8/ §7" + kit.createdAtText());
                        hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                                + "§x§8§8§D§D§F§F» §bKit最后编辑: §f" + kit.lastEditorName()
                                + " §8/ §e" + kit.lastEditedAtText());
                        plugin.getEndFlashKitManager().sendKitGuide(hunter, kit);
                    }
                }, 10L);
            }
        }
    }

    private Location findEndFlashOpeningCenter(World world) {
        if (world == null) {
            return null;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.35D) {
            Location platform = new Location(world, 100.5D, 50.0D, 0.5D);
            prepareEndFlashObsidianPlatform(platform);
            return platform;
        }
        Location center = getEndPortalCenter(world);
        for (int attempt = 0; attempt < 80; attempt++) {
            double radius = ThreadLocalRandom.current().nextDouble(46.0D, 155.0D);
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            if (Math.abs(x) < 10 && Math.abs(z) < 10) {
                continue;
            }
            forceLoadChunkArea(world, x - 16, z - 16, x + 16, z + 16);
            for (int y = Math.min(world.getMaxHeight() - 3, world.getHighestBlockYAt(x, z) + 1); y > world.getMinHeight() + 4; y--) {
                Material floor = world.getBlockAt(x, y - 1, z).getType();
                if ((floor == Material.END_STONE || floor == Material.OBSIDIAN)
                        && !world.getBlockAt(x, y, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                    Location loc = new Location(world, x + 0.5D, y, z + 0.5D);
                    carveSafePocket(loc, floor);
                    ensureEndSpawnLighting(loc, floor);
                    return getSafeSpawnLocation(loc);
                }
            }
        }
        Location platform = new Location(world, 100.5D, 50.0D, 0.5D);
        prepareEndFlashObsidianPlatform(platform);
        return platform;
    }

    private void prepareEndFlashObsidianPlatform(Location center) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        int baseY = center.getBlockY() - 1;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        forceLoadChunkArea(world, cx - 8, cz - 8, cx + 8, cz + 8);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(cx + x, baseY, cz + z).setType(Material.OBSIDIAN, false);
                for (int y = 1; y <= 4; y++) {
                    world.getBlockAt(cx + x, baseY + y, cz + z).setType(Material.AIR, false);
                }
            }
        }
        world.getBlockAt(cx, baseY + 1, cz).setType(Material.LIGHT, false);
    }

    private void ensureEndEntryObsidianFooting(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        int footY = location.getBlockY();
        int blockY = footY - 1;
        if (blockY < world.getMinHeight()) {
            blockY = world.getMinHeight();
            location.setY(blockY + 1.0D);
        }
        if (blockY >= world.getMaxHeight()) {
            return;
        }
        world.getBlockAt(location.getBlockX(), blockY, location.getBlockZ()).setType(Material.OBSIDIAN, false);
    }

    private void teleportNetherChapterPlayers(GameRoom room) {
        World world = room.getGameWorld();
        if (world == null) {
            return;
        }

        List<UUID> preyOrder = new ArrayList<>(room.getPreyUUIDs());
        Location primaryPreySpawn = findNetherChapterPreySpawn(world);
        if (primaryPreySpawn == null) {
            primaryPreySpawn = getSafeSpawnLocation(world.getSpawnLocation());
        }
        final Location finalPrimaryPreySpawn = primaryPreySpawn == null ? null : primaryPreySpawn.clone();
        ensureLavaLakeNear(primaryPreySpawn);

        Map<UUID, Location> preySpawns = new LinkedHashMap<>();
        List<Location> occupiedPreySpawns = new ArrayList<>();
        for (UUID preyUuid : preyOrder) {
            Location spawn = prepareNetherPreySpawn(primaryPreySpawn, occupiedPreySpawns);
            if (spawn == null) {
                spawn = primaryPreySpawn == null ? getSafeSpawnLocation(world.getSpawnLocation()) : primaryPreySpawn.clone();
            }
            if (spawn == null) {
                continue;
            }
            preySpawns.put(preyUuid, spawn);
            occupiedPreySpawns.add(spawn.clone());
        }

        room.clearDualPreyStack();
        for (UUID uuid : preyOrder) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey == null) continue;

            Location assignedPreySpawn = preySpawns.getOrDefault(uuid, primaryPreySpawn);
            if (assignedPreySpawn == null) {
                assignedPreySpawn = getSafeSpawnLocation(world.getSpawnLocation());
            }
            if (assignedPreySpawn == null) {
                continue;
            }

            final Location finalPreySpawn = assignedPreySpawn.clone();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + prey.getName() + " " + world.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                prey.teleport(finalPreySpawn);
                prey.getInventory().clear();
                prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
                prey.setAllowFlight(false);
                prey.setFlying(false);
                prey.setLevel(0);
                prey.setExp(0);
                giveNetherChapterPreyLoadout(prey);
                plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true);
                prey.playSound(prey.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
                prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§6§6§0§0📍 §e你的出生位置: §f" +
                        finalPreySpawn.getBlockX() + " §7/ §f" + finalPreySpawn.getBlockY() + " §7/ §f" + finalPreySpawn.getBlockZ());
            }, 10L);
        }

        List<UUID> hunters = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid)) {
                hunters.add(uuid);
            }
        }

        List<Location> occupiedHunterSpawns = new ArrayList<>(occupiedPreySpawns);
        for (UUID hunterUuid : hunters) {
            Player hunter = Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue;

            NetherHunterScenario selectedScenario = room.getPlayerNetherHunterScenarioVote(hunterUuid);
            if (selectedScenario == null) {
                selectedScenario = pickNetherHunterScenario(room);
            }

            World selectedHunterWorld = selectedScenario == NetherHunterScenario.NETHER
                    ? plugin.getWorldManager().getNetherWorld(room.getRoomId())
                    : world;
            if (selectedHunterWorld == null) {
                selectedHunterWorld = world;
            }

            Location rawHunterSpawn = findNetherHunterSpawn(room, selectedScenario, finalPrimaryPreySpawn, selectedHunterWorld);
            Location hunterSpawn = prepareNetherHunterSpawn(rawHunterSpawn, selectedScenario, selectedHunterWorld, occupiedHunterSpawns);
            if (hunterSpawn == null) {
                hunterSpawn = rawHunterSpawn == null ? getSafeSpawnLocation(selectedHunterWorld.getSpawnLocation()) : rawHunterSpawn.clone();
            }
            if (hunterSpawn == null) {
                continue;
            }
            occupiedHunterSpawns.add(hunterSpawn.clone());
            String targetWorldName = hunterSpawn.getWorld() == null ? world.getName() : hunterSpawn.getWorld().getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + hunter.getName() + " " + targetWorldName);

            final Location finalHunterSpawn = hunterSpawn;
            final NetherHunterScenario finalScenario = selectedScenario;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                hunter.teleport(finalHunterSpawn);
                hunter.getInventory().clear();
                hunter.setGameMode(org.bukkit.GameMode.SURVIVAL);
                hunter.setLevel(0);
                hunter.setExp(0);
                if (!room.hasModifier("YesHunterSee")) {
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
                }
                giveNetherChapterHunterLoadout(hunter, finalScenario);
                createHunterRespawnCamp(hunter, finalHunterSpawn, finalScenario);
                faceHunterTowardTarget(hunter, finalPrimaryPreySpawn);
                plugin.getRoomManager().updatePlayerTabNameWithRole(hunter, room.getRoomId(), false);
                hunter.playSound(hunter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§B§B§6§6📌 §e本次落点: §f" + finalScenario.getDisplayName());
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§8§8§D§D§F§F📍 §b你的出生位置: §f" +
                        finalHunterSpawn.getBlockX() + " §7/ §f" + finalHunterSpawn.getBlockY() + " §7/ §f" + finalHunterSpawn.getBlockZ());
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§5§5§5§5🎯 §c猎物位置播报: §f" +
                        finalPrimaryPreySpawn.getBlockX() + " §7/ §f" + finalPrimaryPreySpawn.getBlockY() + " §7/ §f" + finalPrimaryPreySpawn.getBlockZ());
            }, 10L);
        }
    }

    private void teleportEndChapterPlayers(GameRoom room) {
        World baseWorld = room.getGameWorld();
        World endWorld = resolveEndChapterWorld(room);
        if (baseWorld == null && endWorld == null) {
            return;
        }

        List<UUID> preyOrder = new ArrayList<>(room.getPreyUUIDs());
        if (preyOrder.isEmpty()) {
            return;
        }

        Map<UUID, Location> preySpawns = new LinkedHashMap<>();
        UUID anchorPrey = null;
        if (room.isDoublePreyEnabled() && preyOrder.size() >= 2) {
            anchorPrey = room.getLockedFirstDualPrey();
            if (anchorPrey == null || !preyOrder.contains(anchorPrey)) {
                anchorPrey = preyOrder.get(0);
            }
        }

        Location sharedPreySpawn = null;
        if (anchorPrey != null) {
            EndPreyPosition anchorPosition = getEndChapterSelectedPreyPosition(room, anchorPrey);
            World anchorWorld = resolveEndChapterPreyWorld(room, anchorPosition);
            sharedPreySpawn = findEndChapterPreySpawn(anchorWorld, anchorPosition, Collections.emptyList());
        }

        List<Location> occupiedPreySpawns = new ArrayList<>();
        for (UUID preyUuid : preyOrder) {
            EndPreyPosition preyPosition = getEndChapterSelectedPreyPosition(room, preyUuid);
            World preyWorld = sharedPreySpawn != null ? sharedPreySpawn.getWorld() : resolveEndChapterPreyWorld(room, preyPosition);
            Location preySpawn = sharedPreySpawn != null
                    ? sharedPreySpawn.clone()
                    : findEndChapterPreySpawn(preyWorld, preyPosition, occupiedPreySpawns);
            preySpawns.put(preyUuid, preySpawn);
            occupiedPreySpawns.add(preySpawn);
        }

        Location primaryPreySpawn = preySpawns.values().iterator().next();
        room.clearAssignedEndHunterKits();
        room.clearAssignedEndHunterPositions();

        List<UUID> hunters = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid)) {
                hunters.add(uuid);
            }
        }
        Collections.shuffle(hunters);

        Map<UUID, EndChapterKit> assignedKits = assignEndChapterHunterKits(room, hunters);
        Map<UUID, EndHunterPosition> assignedPositions = assignEndChapterHunterPositions(room, hunters);
        List<Location> occupiedHunterSpawns = new ArrayList<>(occupiedPreySpawns);

        int preyCount = preyOrder.size();
        for (UUID uuid : preyOrder) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey == null) {
                continue;
            }

            EndChapterKit kit = getEndChapterSelectedPreyKit(room, uuid);
            EndPreyPosition position = getEndChapterSelectedPreyPosition(room, uuid);
            Location preySpawn = preySpawns.getOrDefault(uuid, primaryPreySpawn).clone();
            World preyTargetWorld = preySpawn.getWorld() == null ? (endWorld != null ? endWorld : baseWorld) : preySpawn.getWorld();

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + prey.getName() + " " + preyTargetWorld.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ensureEndEntryObsidianFooting(preySpawn);
                prey.teleport(preySpawn);
                prey.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                prey.setFallDistance(0.0F);
                prey.getInventory().clear();
                prey.setGameMode(org.bukkit.GameMode.SURVIVAL);
                prey.setAllowFlight(false);
                prey.setFlying(false);
                prey.setLevel(0);
                prey.setExp(0);
                giveEndChapterPreyLoadout(prey, kit, position, preyCount);
                prey.setRespawnLocation(preySpawn.clone(), true);
                plugin.getRoomManager().updatePlayerTabNameWithRole(prey, room.getRoomId(), true);
                syncEndDimensionBrightness(prey, room);
                prey.playSound(prey.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.9f, 1.2f);
                prey.playSound(prey.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.9f, 1.0f);
                prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§B§B§8§8§F§F🌌 §d你的 Kit: §f" + kit.getDisplayName());
                prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§6§6§0§0📍 §e你的出生点: §f" + position.getDisplayName());
                prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§8§8§D§D§F§F📌 §b坐标: §f" +
                        preySpawn.getBlockX() + " §7/ §f" + preySpawn.getBlockY() + " §7/ §f" + preySpawn.getBlockZ());
            }, 10L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> stackDualPreysBeforeStart(room), 25L);

        for (UUID hunterUuid : hunters) {
            Player hunter = Bukkit.getPlayer(hunterUuid);
            if (hunter == null) {
                continue;
            }

            EndChapterKit kit = assignedKits.getOrDefault(hunterUuid, EndChapterKit.UHC);
            EndHunterPosition position = assignedPositions.getOrDefault(hunterUuid, EndHunterPosition.MAIN_ISLAND_EDGE);
            World hunterWorld = resolveEndChapterHunterWorld(room, position);
            Location hunterSpawn = findEndChapterHunterSpawn(hunterWorld, position, occupiedHunterSpawns);
            occupiedHunterSpawns.add(hunterSpawn);
            World hunterTargetWorld = hunterSpawn.getWorld() == null ? (endWorld != null ? endWorld : baseWorld) : hunterSpawn.getWorld();

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + hunter.getName() + " " + hunterTargetWorld.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ensureEndEntryObsidianFooting(hunterSpawn);
                hunter.teleport(hunterSpawn);
                hunter.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                hunter.setFallDistance(0.0F);
                hunter.getInventory().clear();
                hunter.setGameMode(org.bukkit.GameMode.SURVIVAL);
                hunter.setLevel(0);
                hunter.setExp(0);
                if (!room.hasModifier("YesHunterSee")) {
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
                }
                giveEndChapterHunterLoadout(hunter, kit, position);
                hunter.setRespawnLocation(hunterSpawn.clone(), true);
                faceHunterTowardTarget(hunter, primaryPreySpawn);
                plugin.getRoomManager().updatePlayerTabNameWithRole(hunter, room.getRoomId(), false);
                syncEndDimensionBrightness(hunter, room);
                hunter.playSound(hunter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§B§B§8§8§F§F🌌 §d分配 Kit: §f" + kit.getDisplayName());
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§B§B§6§6📌 §e本次落点: §f" + position.getDisplayName());
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§8§8§D§D§F§F📍 §b你的出生位置: §f" +
                        hunterSpawn.getBlockX() + " §7/ §f" + hunterSpawn.getBlockY() + " §7/ §f" + hunterSpawn.getBlockZ());
                hunter.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§5§5§5§5🎯 §c猎物位置播报: §f" +
                        primaryPreySpawn.getBlockX() + " §7/ §f" + primaryPreySpawn.getBlockY() + " §7/ §f" + primaryPreySpawn.getBlockZ());
            }, 10L);
        }
    }

    private World resolveEndChapterWorld(GameRoom room) {
        World endWorld = plugin.getWorldManager().getEndWorld(room.getRoomId());
        return endWorld != null ? endWorld : room.getGameWorld();
    }

    private World resolveEndChapterPreyWorld(GameRoom room, EndPreyPosition position) {
        if (room == null) {
            return null;
        }
        if (position == EndPreyPosition.END_PORTAL) {
            return room.getGameWorld();
        }
        return resolveEndChapterWorld(room);
    }

    private World resolveEndChapterHunterWorld(GameRoom room, EndHunterPosition position) {
        if (room == null) {
            return null;
        }
        if (position == EndHunterPosition.GATEWAY_CHASER || position == EndHunterPosition.GATEWAY_LOGISTICS) {
            return room.getGameWorld();
        }
        return resolveEndChapterWorld(room);
    }

    private Map<UUID, EndChapterKit> assignEndChapterHunterKits(GameRoom room, List<UUID> hunters) {
        Map<UUID, EndChapterKit> result = new LinkedHashMap<>();
        Map<EndChapterKit, Integer> assignedCounts = new EnumMap<>(EndChapterKit.class);
        for (EndChapterKit kit : EndChapterKit.values()) {
            assignedCounts.put(kit, 0);
        }

        for (UUID hunterUuid : hunters) {
            EndChapterKit vote = room.getEndHunterKitVote(hunterUuid);
            EndChapterKit assigned = pickEndChapterHunterKit(vote, assignedCounts);
            assignedCounts.merge(assigned, 1, Integer::sum);
            room.assignEndHunterKit(hunterUuid, assigned);
            result.put(hunterUuid, assigned);
        }
        return result;
    }

    private EndChapterKit pickEndChapterHunterKit(EndChapterKit vote, Map<EndChapterKit, Integer> assignedCounts) {
        List<EndChapterKit> available = new ArrayList<>();
        for (EndChapterKit kit : EndChapterKit.values()) {
            if (assignedCounts.getOrDefault(kit, 0) < kit.getHunterMaxAssignments()) {
                available.add(kit);
            }
        }
        if (available.isEmpty()) {
            available.addAll(Arrays.asList(EndChapterKit.values()));
        }

        int totalWeight = 0;
        for (EndChapterKit kit : available) {
            totalWeight += getEndChapterHunterKitWeight(kit, vote);
        }
        if (totalWeight <= 0) {
            return EndChapterKit.UHC;
        }

        int roll = new Random().nextInt(totalWeight);
        int current = 0;
        for (EndChapterKit kit : available) {
            current += getEndChapterHunterKitWeight(kit, vote);
            if (roll < current) {
                return kit;
            }
        }
        return EndChapterKit.UHC;
    }

    private Map<UUID, EndHunterPosition> assignEndChapterHunterPositions(GameRoom room, List<UUID> hunters) {
        Map<UUID, EndHunterPosition> result = new LinkedHashMap<>();
        Map<EndHunterPosition, Integer> assignedCounts = new EnumMap<>(EndHunterPosition.class);
        for (EndHunterPosition position : EndHunterPosition.values()) {
            assignedCounts.put(position, 0);
        }

        for (UUID hunterUuid : hunters) {
            EndHunterPosition vote = room.getEndHunterPositionVote(hunterUuid);
            EndHunterPosition assigned = pickEndChapterHunterPosition(vote, assignedCounts);
            assignedCounts.merge(assigned, 1, Integer::sum);
            room.assignEndHunterPosition(hunterUuid, assigned);
            result.put(hunterUuid, assigned);
        }
        return result;
    }

    private EndHunterPosition pickEndChapterHunterPosition(EndHunterPosition vote, Map<EndHunterPosition, Integer> assignedCounts) {
        List<EndHunterPosition> available = new ArrayList<>();
        for (EndHunterPosition position : EndHunterPosition.values()) {
            if (assignedCounts.getOrDefault(position, 0) < position.getMaxAssignments()) {
                available.add(position);
            }
        }
        if (available.isEmpty()) {
            available.addAll(Arrays.asList(EndHunterPosition.values()));
        }

        int totalWeight = 0;
        for (EndHunterPosition position : available) {
            totalWeight += getEndChapterHunterPositionWeight(position, vote);
        }
        if (totalWeight <= 0) {
            return EndHunterPosition.MAIN_ISLAND_EDGE;
        }

        int roll = new Random().nextInt(totalWeight);
        int current = 0;
        for (EndHunterPosition position : available) {
            current += getEndChapterHunterPositionWeight(position, vote);
            if (roll < current) {
                return position;
            }
        }
        return EndHunterPosition.MAIN_ISLAND_EDGE;
    }

    private EndChapterKit getEndChapterSelectedPreyKit(GameRoom room, UUID uuid) {
        EndChapterKit selected = room.getEndPreyKitSelection(uuid);
        return selected == null ? EndChapterKit.UHC : selected;
    }

    private EndPreyPosition getEndChapterSelectedPreyPosition(GameRoom room, UUID uuid) {
        EndPreyPosition selected = room.getEndPreyPositionSelection(uuid);
        return selected == null ? EndPreyPosition.FOUNTAIN_FRONTLINE : selected;
    }

    private Location findEndChapterPreySpawn(World world, EndPreyPosition position, Collection<Location> occupied) {
        if (world == null) {
            return null;
        }
        Location center = getEndPortalCenter(world);
        return switch (position) {
            case OBSIDIAN_PLATFORM -> prepareEndSpawnLocation(findEndObsidianPlatformSpawn(world, occupied), Material.OBSIDIAN, occupied);
            case FOUNTAIN_FRONTLINE -> prepareEndSpawnLocation(findEndSurfaceAround(world, center, 10, 26, occupied, false), Material.END_STONE, occupied);
            case END_PORTAL -> prepareStrongholdSpawnLocation(findStrongholdPortalFrameTopSpawn(world, occupied), occupied);
            case CHORUS_FOREST -> prepareEndSpawnLocation(findEndChorusSpawn(world, occupied), Material.END_STONE, occupied);
            case END_CITY_APPROACH -> prepareEndSpawnLocation(findEndCityApproachSpawn(world, occupied), Material.END_STONE_BRICKS, occupied);
            case OUTER_GATEWAY_RING -> prepareEndSpawnLocation(findEndOuterRingSpawn(world, occupied), Material.END_STONE_BRICKS, occupied);
        };
    }

    private Location findEndChapterHunterSpawn(World world, EndHunterPosition position, Collection<Location> occupied) {
        if (world == null) {
            return null;
        }
        Location center = getEndPortalCenter(world);
        return switch (position) {
            case MAIN_ISLAND_EDGE -> prepareEndSpawnLocation(findEndSurfaceAround(world, center, 48, 108, occupied, false), Material.END_STONE, occupied);
            case CHORUS_SCOUT -> prepareEndSpawnLocation(findEndChorusSpawn(world, occupied), Material.END_STONE, occupied);
            case END_CITY_RAIDER -> prepareEndSpawnLocation(findEndCityApproachSpawn(world, occupied), Material.END_STONE_BRICKS, occupied);
            case OUTER_RING_MINER -> prepareEndSpawnLocation(findEndOuterRingSpawn(world, occupied), Material.END_STONE_BRICKS, occupied);
            case GATEWAY_CHASER -> prepareStrongholdSpawnLocation(findStrongholdPortalAreaSpawn(world, occupied, 8, 18), occupied);
            case GATEWAY_LOGISTICS -> prepareStrongholdSpawnLocation(findStrongholdPortalAreaSpawn(world, occupied, 3, 12), occupied);
        };
    }

    private Location getEndPortalCenter(World world) {
        Location guess = new Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 1.0, 0.5);
        if (guess.getBlockY() <= world.getMinHeight() + 2) {
            guess = world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
        }
        return getSafeSpawnLocation(guess);
    }

    private Location findEndObsidianPlatformSpawn(World world, Collection<Location> occupied) {
        if (world == null) {
            return null;
        }

        List<Location> anchors = new ArrayList<>();
        anchors.add(world.getSpawnLocation().clone());
        anchors.add(new Location(world, 100.5D, 50.0D, 0.5D));
        anchors.add(new Location(world, 0.5D, Math.max(world.getMinHeight() + 1, world.getHighestBlockYAt(0, 0)), 0.5D));

        for (Location anchor : anchors) {
            forceLoadChunkArea(world, anchor.getBlockX() - 24, anchor.getBlockZ() - 24,
                    anchor.getBlockX() + 24, anchor.getBlockZ() + 24);
            Location platform = findSpawnOnMaterialCluster(world, anchor, Material.OBSIDIAN, 24, 18, occupied, 6.0D);
            if (platform != null) {
                return platform;
            }
        }

        return world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
    }

    private Location prepareEndSpawnLocation(Location location, Material floorType, Collection<Location> occupied) {
        Location candidate = location == null ? null : location.clone();
        if (candidate == null || candidate.getWorld() == null) {
            return candidate;
        }

        if (isTooCloseToOccupied(candidate, occupied, 20.0D)) {
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30.0D);
                Location shifted = candidate.clone().add(Math.cos(angle) * 14.0D, 0.0D, Math.sin(angle) * 14.0D);
                shifted.setY(Math.max(candidate.getY(), shifted.getWorld().getHighestBlockYAt(shifted.getBlockX(), shifted.getBlockZ()) + 1.0D));
                if (!isTooCloseToOccupied(shifted, occupied, 20.0D)) {
                    candidate = shifted;
                    break;
                }
            }
        }

        carveSafePocket(candidate, floorType);
        ensureEndSpawnLighting(candidate, floorType);
        return getSafeSpawnLocation(candidate);
    }

    private Location prepareStrongholdSpawnLocation(Location location, Collection<Location> occupied) {
        Location candidate = location == null ? null : location.clone();
        if (candidate == null || candidate.getWorld() == null) {
            return candidate;
        }

        if (isTooCloseToOccupied(candidate, occupied, 14.0D)) {
            Location portalCenter = getStrongholdPortalCenter(candidate.getWorld());
            if (portalCenter != null) {
                Location shifted = findStrongholdPortalAreaSpawn(candidate.getWorld(), occupied, 6, 16);
                if (shifted != null) {
                    candidate = shifted;
                } else {
                    candidate = portalCenter;
                }
            }
        }

        ensureSpawnStandingSpace(candidate, Material.STONE_BRICKS);
        ensureEndSpawnLighting(candidate, Material.STONE_BRICKS);
        return getSafeSpawnLocation(candidate);
    }

    private boolean isTooCloseToOccupied(Location location, Collection<Location> occupied, double minDistance) {
        if (location == null || occupied == null) {
            return false;
        }
        for (Location other : occupied) {
            if (other == null || other.getWorld() == null || location.getWorld() == null) {
                continue;
            }
            if (!other.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (other.distance(location) < minDistance) {
                return true;
            }
        }
        return false;
    }

    private Location findEndSurfaceAround(World world, Location center, int minDistance, int maxDistance, Collection<Location> occupied, boolean requireChorus) {
        Random random = new Random();
        Location origin = center == null ? world.getSpawnLocation() : center;

        for (int attempt = 0; attempt < 120; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);
            Material ground = world.getBlockAt(x, Math.max(world.getMinHeight(), y - 1), z).getType();
            if (!ground.isSolid() || ground == Material.AIR || ground == Material.CAVE_AIR || ground == Material.VOID_AIR) {
                continue;
            }

            Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (requireChorus && !hasNearbyMaterial(candidate, Material.CHORUS_PLANT, 18, 10, 18)
                    && !hasNearbyMaterial(candidate, Material.CHORUS_FLOWER, 18, 10, 18)) {
                continue;
            }
            if (isTooCloseToOccupied(candidate, occupied, 20.0D)) {
                continue;
            }
            return candidate;
        }

        return getSafeSpawnLocation(origin.clone());
    }

    private Location findEndChorusSpawn(World world, Collection<Location> occupied) {
        Location center = getEndPortalCenter(world);
        return findEndSurfaceAround(world, center, 850, 2000, occupied, true);
    }

    private Location findEndOuterRingSpawn(World world, Collection<Location> occupied) {
        Location center = getEndPortalCenter(world);
        Location gateway = findEndGatewayLanding(world, center, occupied);
        if (gateway != null) {
            return gateway;
        }
        return findEndSurfaceAround(world, center, 1100, 2600, occupied, false);
    }

    private Location findEndGatewayLanding(World world, Location center, Collection<Location> occupied) {
        if (world == null || center == null) {
            return null;
        }

        int scanRadius = 128;
        forceLoadChunkArea(world, center.getBlockX() - scanRadius, center.getBlockZ() - scanRadius,
                center.getBlockX() + scanRadius, center.getBlockZ() + scanRadius);
        int minY = Math.max(world.getMinHeight() + 2, center.getBlockY() - 48);
        int maxY = Math.min(world.getMaxHeight() - 3, center.getBlockY() + 80);
        List<Location> gateways = new ArrayList<>();
        for (int x = center.getBlockX() - scanRadius; x <= center.getBlockX() + scanRadius; x++) {
            for (int z = center.getBlockZ() - scanRadius; z <= center.getBlockZ() + scanRadius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.END_GATEWAY) {
                        gateways.add(new Location(world, x + 0.5D, y, z + 0.5D));
                    }
                }
            }
        }

        gateways.sort(Comparator.comparingDouble(gateway -> gateway.distanceSquared(center)));
        for (Location gateway : gateways) {
            for (int radius = 3; radius <= 14; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        int x = gateway.getBlockX() + dx;
                        int z = gateway.getBlockZ() + dz;
                        int y = world.getHighestBlockYAt(x, z);
                        Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
                        if (isStrictSpawnCandidate(candidate) && !isTooCloseToOccupied(candidate, occupied, 16.0D)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Location findEndCityApproachSpawn(World world, Collection<Location> occupied) {
        Location center = getEndPortalCenter(world);
        try {
            org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(center, org.bukkit.generator.structure.Structure.END_CITY, 6000, false);
            if (result != null && result.getLocation() != null) {
                Location city = result.getLocation().clone();
                forceLoadChunkArea(world, city.getBlockX() - 64, city.getBlockZ() - 64, city.getBlockX() + 64, city.getBlockZ() + 64);
                Location directCityLanding = findSpawnOnAnyMaterialCluster(world, city,
                        Arrays.asList(Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.END_STONE_BRICKS, Material.END_STONE),
                        56, 64, occupied, 16.0D);
                if (directCityLanding != null) {
                    return directCityLanding;
                }
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30.0D);
                    int offset = 26 + i * 2;
                    int x = city.getBlockX() + (int) Math.round(Math.cos(angle) * offset);
                    int z = city.getBlockZ() + (int) Math.round(Math.sin(angle) * offset);
                    int y = world.getHighestBlockYAt(x, z);
                    Material ground = world.getBlockAt(x, Math.max(world.getMinHeight(), y - 1), z).getType();
                    if (!ground.isSolid() || ground == Material.AIR || ground == Material.CAVE_AIR || ground == Material.VOID_AIR) {
                        continue;
                    }

                    Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
                    if (!isTooCloseToOccupied(candidate, occupied, 20.0D)) {
                        return candidate;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return findEndOuterRingSpawn(world, occupied);
    }

    private Location findStrongholdPortalAreaSpawn(World world, Collection<Location> occupied, int minDistance, int maxDistance) {
        Location portalCenter = getStrongholdPortalCenter(world);
        if (portalCenter == null) {
            return getSafeSpawnLocation(world.getSpawnLocation());
        }

        List<Location> fallback = new ArrayList<>();
        int baseX = portalCenter.getBlockX();
        int baseY = portalCenter.getBlockY();
        int baseZ = portalCenter.getBlockZ();

        for (int radius = Math.max(1, minDistance); radius <= Math.max(minDistance, maxDistance); radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = baseX + dx;
                    int z = baseZ + dz;
                    for (int y = baseY - 4; y <= baseY + 5; y++) {
                        if (!isStrongholdSpawnCandidate(world, x, y, z)) {
                            continue;
                        }

                        Location candidate = new Location(world, x + 0.5D, y, z + 0.5D);
                        if (!isTooCloseToOccupied(candidate, occupied, 14.0D)) {
                            return candidate;
                        }
                        fallback.add(candidate);
                    }
                }
            }
        }

        if (!fallback.isEmpty()) {
            return fallback.get(0);
        }

        return portalCenter.clone();
    }

    private Location findStrongholdPortalFrameTopSpawn(World world, Collection<Location> occupied) {
        Location portalCenter = getStrongholdPortalCenter(world);
        if (portalCenter == null || portalCenter.getWorld() == null) {
            return world == null ? null : getSafeSpawnLocation(world.getSpawnLocation());
        }

        List<Location> frames = findPortalFramesNear(portalCenter.getWorld(), portalCenter, 10, 8);
        frames.sort(Comparator.comparingDouble(frame -> frame.distanceSquared(portalCenter)));
        for (Location frame : frames) {
            Location candidate = frame.clone().add(0.0D, 1.0D, 0.0D);
            candidate.setX(frame.getBlockX() + 0.5D);
            candidate.setZ(frame.getBlockZ() + 0.5D);
            if (isTooCloseToOccupied(candidate, occupied, 4.0D)) {
                continue;
            }
            ensureStrongholdPortalFrameTopSpace(candidate);
            return candidate;
        }

        return findStrongholdPortalAreaSpawn(world, occupied, 1, 8);
    }

    private Location getStrongholdPortalCenter(World world) {
        if (world == null) {
            return null;
        }

        Location searchOrigin = world.getSpawnLocation();
        try {
            org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(
                    searchOrigin, org.bukkit.generator.structure.Structure.STRONGHOLD, 3000, false);
            if (result != null && result.getLocation() != null) {
                Location stronghold = result.getLocation();
                forceLoadChunkArea(world, stronghold.getBlockX() - 128, stronghold.getBlockZ() - 128,
                        stronghold.getBlockX() + 128, stronghold.getBlockZ() + 128);
                Location portalFrames = findPortalFrameClusterCenter(world, stronghold, 128, 48);
                if (portalFrames != null) {
                    return portalFrames;
                }
                plugin.getLogger().warning("末地篇要塞已定位，但未扫描到末地传送门框架，回退到要塞结构点附近。");
                return getSafeSpawnLocation(stronghold.clone().add(0.5D, 1.0D, 0.5D));
            }
        } catch (Exception ignored) {
        }

        return getSafeSpawnLocation(searchOrigin.clone());
    }

    private List<Location> findPortalFramesNear(World world, Location origin, int horizontalRadius, int verticalRadius) {
        List<Location> frames = new ArrayList<>();
        if (world == null || origin == null) {
            return frames;
        }

        int minX = origin.getBlockX() - horizontalRadius;
        int maxX = origin.getBlockX() + horizontalRadius;
        int minZ = origin.getBlockZ() - horizontalRadius;
        int maxZ = origin.getBlockZ() + horizontalRadius;
        forceLoadChunkArea(world, minX, minZ, maxX, maxZ);

        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 2, origin.getBlockY() + verticalRadius);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.END_PORTAL_FRAME) {
                        frames.add(new Location(world, x + 0.5D, y, z + 0.5D));
                    }
                }
            }
        }
        return frames;
    }

    private void ensureStrongholdPortalFrameTopSpace(Location location) {
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Material floor = world.getBlockAt(x, y - 1, z).getType();
        if (floor != Material.END_PORTAL_FRAME && !floor.isSolid()) {
            world.getBlockAt(x, y - 1, z).setType(Material.STONE_BRICKS, false);
        }
        world.getBlockAt(x, y, z).setType(Material.AIR, false);
        world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
        if (world.getBlockAt(x, y + 2, z).getType() != Material.END_PORTAL_FRAME) {
            world.getBlockAt(x, y + 2, z).setType(Material.AIR, false);
        }
    }

    private Location findSpawnOnMaterialCluster(World world, Location origin, Material material, int horizontalRadius,
                                                int verticalRadius, Collection<Location> occupied, double occupiedDistance) {
        return findSpawnOnAnyMaterialCluster(world, origin, Collections.singletonList(material),
                horizontalRadius, verticalRadius, occupied, occupiedDistance);
    }

    private Location findSpawnOnAnyMaterialCluster(World world, Location origin, Collection<Material> materials, int horizontalRadius,
                                                   int verticalRadius, Collection<Location> occupied, double occupiedDistance) {
        if (world == null || origin == null || materials == null || materials.isEmpty()) {
            return null;
        }

        int minX = origin.getBlockX() - horizontalRadius;
        int maxX = origin.getBlockX() + horizontalRadius;
        int minZ = origin.getBlockZ() - horizontalRadius;
        int maxZ = origin.getBlockZ() + horizontalRadius;
        forceLoadChunkArea(world, minX, minZ, maxX, maxZ);

        int minY = Math.max(world.getMinHeight() + 1, origin.getBlockY() - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 3, origin.getBlockY() + verticalRadius);
        Location best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Material floor = world.getBlockAt(x, y, z).getType();
                    if (!materials.contains(floor) || !floor.isSolid()) {
                        continue;
                    }

                    Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
                    if (!isStrictSpawnCandidate(candidate) || isTooCloseToOccupied(candidate, occupied, occupiedDistance)) {
                        continue;
                    }

                    double distance = candidate.distanceSquared(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }

        return best;
    }

    private Location findPortalFrameClusterCenter(World world, Location origin, int horizontalRadius, int verticalRadius) {
        if (world == null || origin == null) {
            return null;
        }

        int minX = origin.getBlockX() - horizontalRadius;
        int maxX = origin.getBlockX() + horizontalRadius;
        int minZ = origin.getBlockZ() - horizontalRadius;
        int maxZ = origin.getBlockZ() + horizontalRadius;
        forceLoadChunkArea(world, minX, minZ, maxX, maxZ);
        if (!isChunkAreaLoaded(world, minX, minZ, maxX, maxZ)) {
            return null;
        }

        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + verticalRadius);
        int totalX = 0;
        int totalY = 0;
        int totalZ = 0;
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.END_PORTAL_FRAME) {
                        totalX += x;
                        totalY += y;
                        totalZ += z;
                        count++;
                    }
                }
            }
        }

        if (count < 6) {
            return null;
        }

        return new Location(world,
                totalX / (double) count + 0.5D,
                totalY / (double) count + 1.0D,
                totalZ / (double) count + 0.5D);
    }

    private boolean isStrongholdSpawnCandidate(World world, int x, int y, int z) {
        if (world == null || y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
            return false;
        }

        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        Material floor = world.getBlockAt(x, y - 1, z).getType();

        if (!isPassableSpawnBlock(feet) || !isPassableSpawnBlock(head)) {
            return false;
        }
        if (!floor.isSolid() || floor == Material.END_PORTAL || floor == Material.LAVA) {
            return false;
        }

        return hasNearbyMaterial(new Location(world, x + 0.5D, y, z + 0.5D), Material.END_PORTAL_FRAME, 8, 6, 8);
    }

    private boolean isPassableSpawnBlock(Material material) {
        return material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.LIGHT;
    }

    private boolean isStrictSpawnCandidate(Location location) {
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
            return false;
        }

        Material floor = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        Material above = world.getBlockAt(x, y + 2, z).getType();
        if (!floor.isSolid() || floor == Material.LAVA || floor == Material.WATER || floor == Material.FIRE || floor == Material.SOUL_FIRE) {
            return false;
        }
        if (!isPassableSpawnBlock(feet) || !isPassableSpawnBlock(head)) {
            return false;
        }
        return above != Material.LAVA && above != Material.WATER && above != Material.FIRE && above != Material.SOUL_FIRE;
    }

    private void ensureSpawnStandingSpace(Location location, Material floorType) {
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (!isChunkAreaLoaded(world, x - 1, z - 1, x + 1, z + 1)) {
            return;
        }

        Material floor = world.getBlockAt(x, y - 1, z).getType();
        if (!floor.isSolid() || floor == Material.LAVA || floor == Material.END_PORTAL) {
        world.getBlockAt(x, y - 1, z).setType(floorType, false);
        }
        world.getBlockAt(x, y, z).setType(Material.AIR, false);
        world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
    }

    private void ensureEndSpawnLighting(Location location, Material floorType) {
        World world = location == null ? null : location.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        if (!isChunkAreaLoaded(world, baseX - 3, baseZ - 3, baseX + 3, baseZ + 3)) {
            return;
        }

        int[][] offsets = {
                {2, 0},
                {-2, 0},
                {0, 2},
                {0, -2}
        };

        for (int[] offset : offsets) {
            int x = baseX + offset[0];
            int z = baseZ + offset[1];
            Material floor = world.getBlockAt(x, baseY - 1, z).getType();
            if (!floor.isSolid() || floor == Material.AIR || floor == Material.CAVE_AIR || floor == Material.VOID_AIR) {
                world.getBlockAt(x, baseY - 1, z).setType(floorType, false);
            }

            Material feet = world.getBlockAt(x, baseY, z).getType();
            Material head = world.getBlockAt(x, baseY + 1, z).getType();
            if (isPassableSpawnBlock(feet) && isPassableSpawnBlock(head)) {
                world.getBlockAt(x, baseY, z).setType(Material.END_ROD, false);
            }
        }
    }

    private void giveEndChapterPreyLoadout(Player prey, EndChapterKit kit, EndPreyPosition position, int preyCount) {
        Random random = new Random();
        org.bukkit.inventory.PlayerInventory inventory = prey.getInventory();

        switch (kit) {
            case UHC -> {
                equipArmorSet(prey, "DIAMOND", true);
                addRandomProtection(prey, random, 3, 0.38D);
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 3));
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_AXE, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createEndChapterBow(random, 4, true));
                inventory.addItem(new ItemStack(Material.ARROW, 52 + random.nextInt(16)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 6 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.WATER_BUCKET));
                inventory.addItem(new ItemStack(Material.LAVA_BUCKET));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.82D, 1);
            }
            case MACE -> {
                equipArmorSet(prey, "DIAMOND", true);
                addRandomProtection(prey, random, 3, 0.36D);
                inventory.addItem(createUsedItem(findMaterial("MACE", Material.BREEZE_ROD), random));
                inventory.addItem(new ItemStack(findMaterial("BREEZE_ROD", Material.BLAZE_ROD), 16));
                ItemStack boots = inventory.getBoots() == null ? createUsedItem(Material.DIAMOND_BOOTS, random) : inventory.getBoots();
                boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4);
                inventory.setBoots(boots);
                inventory.addItem(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 32 + random.nextInt(9)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 2));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 6 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.84D, 1);
            }
            case DEBUFF -> {
                equipArmorSet(prey, "DIAMOND", false);
                addRandomProtection(prey, random, 2, 0.88D);
                addRandomProtection(prey, random, 3, 0.28D);
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.POISON, 2));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.SLOWNESS, 2));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.WEAKNESS, 2));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 2));
                inventory.addItem(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.HEALING, 5));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 3));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.COBWEB, 5 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.55D, 1);
            }
            case DIAMOND_SMP -> {
                equipArmorSet(prey, "DIAMOND", true);
                ItemStack smpChest = createUsedItem(Material.NETHERITE_CHESTPLATE, random);
                smpChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                inventory.setChestplate(smpChest);
                addRandomProtection(prey, random, 3, 0.45D);
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.60D ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 3));
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.40D ? Material.NETHERITE_AXE : Material.DIAMOND_AXE,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(new ItemStack(Material.SHIELD));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 5 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 6 + random.nextInt(4)));
                maybeAddTotem(inventory, random, 0.92D, 1 + (random.nextDouble() < 0.45D ? 1 : 0));
            }
            case TRAPPER -> {
                equipArmorSet(prey, "DIAMOND", true);
                addRandomProtection(prey, random, 3, 0.20D);
                ItemStack trapChest = inventory.getChestplate();
                if (trapChest != null) {
                    trapChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 4);
                    inventory.setChestplate(trapChest);
                }
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_AXE, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(new ItemStack(Material.COBWEB, 18 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.TNT, 12 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.TNT_MINECART, 3 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.RAIL, 16 + random.nextInt(9)));
                inventory.addItem(new ItemStack(Material.ACTIVATOR_RAIL, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.REDSTONE_TORCH, 5 + random.nextInt(3)));
                inventory.addItem(createUsedItem(Material.FLINT_AND_STEEL, random));
                inventory.addItem(new ItemStack(Material.TRIPWIRE_HOOK, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.STRING, 14 + random.nextInt(7)));
                addRespawnAnchorBundle(inventory, 3, 12 + random.nextInt(7));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.SHIELD));
            }
            case CRYSTAL -> {
                equipArmorSet(prey, "DIAMOND", true);
                ItemStack crystalHelmet = createUsedItem(Material.NETHERITE_HELMET, random);
                crystalHelmet.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                inventory.setHelmet(crystalHelmet);
                ItemStack crystalChest = inventory.getChestplate();
                if (crystalChest != null) {
                    crystalChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 4);
                    inventory.setChestplate(crystalChest);
                }
                ItemStack crystalLeggings = inventory.getLeggings();
                if (crystalLeggings != null) {
                    crystalLeggings.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 4);
                    inventory.setLeggings(crystalLeggings);
                }
                ItemStack crystalBoots = inventory.getBoots();
                if (crystalBoots != null) {
                    crystalBoots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4);
                    inventory.setBoots(crystalBoots);
                }
                addRandomProtection(prey, random, 3, 0.45D);
                ItemStack crystalPick = createEnchantedUsedItem(Material.DIAMOND_PICKAXE, random, org.bukkit.enchantments.Enchantment.EFFICIENCY, 3);
                inventory.addItem(crystalPick);
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.45D ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(new ItemStack(Material.END_CRYSTAL, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.OBSIDIAN, 24 + random.nextInt(11)));
                addRespawnAnchorBundle(inventory, 3 + random.nextInt(2), 12 + random.nextInt(7));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 6 + random.nextInt(4)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                maybeAddTotem(inventory, random, 0.94D, 1 + (random.nextDouble() < 0.45D ? 1 : 0));
            }
            case POTION -> {
                equipArmorSet(prey, "DIAMOND", false);
                addRandomProtection(prey, random, 2, 0.90D);
                addRandomProtection(prey, random, 3, 0.26D);
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.STRENGTH, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 2));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HEALING, 5));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 3));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.POISON, 2));
                inventory.addItem(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                inventory.addItem(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.HARMING, 1));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.52D, 1);
            }
            case CROSSBOW -> {
                equipArmorSet(prey, "DIAMOND", false);
                addRandomProtection(prey, random, 2, 0.82D);
                addRandomProtection(prey, random, 3, 0.22D);
                ItemStack crossHelmet = inventory.getHelmet();
                if (crossHelmet != null) {
                    crossHelmet.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROJECTILE_PROTECTION, 4);
                    inventory.setHelmet(crossHelmet);
                }
                ItemStack crossChest = inventory.getChestplate();
                if (crossChest != null) {
                    crossChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROJECTILE_PROTECTION, 4);
                    inventory.setChestplate(crossChest);
                }
                ItemStack crossbow = createEndChapterCrossbow(random, 3, true);
                inventory.addItem(crossbow);
                inventory.addItem(createEndChapterBow(random, 3, false));
                inventory.addItem(new ItemStack(Material.ARROW, 44 + random.nextInt(15)));
                inventory.addItem(createTippedArrow(org.bukkit.potion.PotionType.SLOWNESS, 10 + random.nextInt(5)));
                inventory.addItem(createTippedArrow(org.bukkit.potion.PotionType.HARMING, 8 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.FIREWORK_ROCKET, 14 + random.nextInt(6)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.45D, 1);
            }
            case SPEAR_HAMMER -> {
                equipArmorSet(prey, "DIAMOND", true);
                addRandomProtection(prey, random, 3, 0.30D);
                ItemStack spear = createUsedItem(findMaterial("SPEAR", Material.TRIDENT), random);
                if (spear.getType() == Material.TRIDENT) {
                    spear.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LOYALTY, 1);
                }
                ItemStack spearBoots = inventory.getBoots();
                if (spearBoots != null) {
                    spearBoots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4);
                    inventory.setBoots(spearBoots);
                }
                inventory.addItem(spear);
                inventory.addItem(createUsedItem(findMaterial("MACE", Material.BREEZE_ROD), random));
                inventory.addItem(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 8 + random.nextInt(4)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 2));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 2));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.84D, 1);
            }
        }

        addEndChapterPreyCoreCombatLoadout(prey, random);
        addCommonEndChapterSupplies(inventory, random, true);
        addEndChapterPreyPositionSupplies(inventory, random, position, preyCount);
        equipShieldOffhand(prey);
        applySavedEndChapterKitLayout(prey, EndChapterKitRole.PREY, kit);
    }

    private void giveEndChapterHunterLoadout(Player hunter, EndChapterKit kit, EndHunterPosition position) {
        Random random = new Random();
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();

        switch (kit) {
            case UHC -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 2);
                addRandomProtection(hunter, random, 2, 0.65D);
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.50D ? Material.DIAMOND_SWORD : Material.IRON_SWORD,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createEndChapterBow(random, 3, false));
                inventory.addItem(new ItemStack(Material.ARROW, 36 + random.nextInt(13)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SHIELD));
            }
            case MACE -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 2, 0.36D);
                inventory.addItem(createUsedItem(findMaterial("MACE", Material.BREEZE_ROD), random));
                inventory.addItem(new ItemStack(findMaterial("BREEZE_ROD", Material.BLAZE_ROD), 16));
                ItemStack boots = inventory.getBoots() == null ? createUsedItem(Material.DIAMOND_BOOTS, random) : inventory.getBoots();
                boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4);
                inventory.setBoots(boots);
                inventory.addItem(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 28 + random.nextInt(9)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 5 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.45D, 1);
            }
            case DEBUFF -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 2, 0.42D);
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.28D ? Material.DIAMOND_SWORD : Material.IRON_SWORD,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.POISON, 1));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.SLOWNESS, 1));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.WEAKNESS, 1));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 1));
                inventory.addItem(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.HEALING, 4));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 2));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 3 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.SHIELD));
            }
            case DIAMOND_SMP -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 4, 2);
                addRandomProtection(hunter, random, 2, 0.68D);
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.60D ? Material.DIAMOND_AXE : Material.IRON_AXE,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
                inventory.addItem(new ItemStack(Material.SHIELD));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(3)));
                maybeAddTotem(inventory, random, 0.55D, 1);
            }
            case TRAPPER -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 1, 0.34D);
                ItemStack hunterTrapChest = inventory.getChestplate();
                if (hunterTrapChest != null) {
                    hunterTrapChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 3);
                    inventory.setChestplate(hunterTrapChest);
                }
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.25D ? Material.DIAMOND_AXE : Material.IRON_AXE,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
                inventory.addItem(new ItemStack(Material.COBWEB, 12 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.TNT, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.TNT_MINECART, 2 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.RAIL, 14 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.ACTIVATOR_RAIL, 7 + random.nextInt(4)));
                inventory.addItem(createUsedItem(Material.FLINT_AND_STEEL, random));
                inventory.addItem(new ItemStack(Material.TRIPWIRE_HOOK, 5 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.STRING, 12 + random.nextInt(5)));
                addRespawnAnchorBundle(inventory, 1 + random.nextInt(2), 6 + random.nextInt(5));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
            }
            case CRYSTAL -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 2, 0.50D);
                ItemStack hunterCrystalChest = inventory.getChestplate();
                if (hunterCrystalChest != null) {
                    hunterCrystalChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 3);
                    inventory.setChestplate(hunterCrystalChest);
                }
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.40D ? Material.DIAMOND_SWORD : Material.IRON_SWORD,
                        random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
                inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.42D ? Material.DIAMOND_PICKAXE : Material.IRON_PICKAXE,
                        random, org.bukkit.enchantments.Enchantment.EFFICIENCY, 2));
                inventory.addItem(new ItemStack(Material.END_CRYSTAL, 6 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.OBSIDIAN, 18 + random.nextInt(8)));
                addRespawnAnchorBundle(inventory, 2 + random.nextInt(2), 8 + random.nextInt(5));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
                maybeAddTotem(inventory, random, 0.50D, 1);
            }
            case POTION -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 2, 0.36D);
                inventory.addItem(createEnchantedUsedItem(Material.IRON_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.STRENGTH, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 2));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HEALING, 5));
                inventory.addItem(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 3));
                inventory.addItem(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 3 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.SHIELD));
            }
            case CROSSBOW -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 1, 0.42D);
                ItemStack hunterCrossChest = inventory.getChestplate();
                if (hunterCrossChest != null) {
                    hunterCrossChest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROJECTILE_PROTECTION, 3);
                    inventory.setChestplate(hunterCrossChest);
                }
                ItemStack crossbow = createEndChapterCrossbow(random, 2, true);
                inventory.addItem(crossbow);
                inventory.addItem(createEndChapterBow(random, 3, false));
                inventory.addItem(new ItemStack(Material.ARROW, 34 + random.nextInt(11)));
                inventory.addItem(createTippedArrow(org.bukkit.potion.PotionType.SLOWNESS, 9 + random.nextInt(4)));
                inventory.addItem(createTippedArrow(org.bukkit.potion.PotionType.HARMING, 5 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.FIREWORK_ROCKET, 12 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 3 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
                inventory.addItem(new ItemStack(Material.SHIELD));
            }
            case SPEAR_HAMMER -> {
                equipArmorSet(hunter, "IRON", false);
                upgradeArmorPiecesToDiamond(hunter, random, 3, 1);
                addRandomProtection(hunter, random, 2, 0.32D);
                ItemStack spear = createUsedItem(findMaterial("SPEAR", Material.TRIDENT), random);
                if (spear.getType() == Material.TRIDENT && random.nextDouble() < 0.35D) {
                    spear.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LOYALTY, 1);
                }
                ItemStack hunterSpearBoots = inventory.getBoots();
                if (hunterSpearBoots != null) {
                    hunterSpearBoots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 3);
                    inventory.setBoots(hunterSpearBoots);
                }
                inventory.addItem(spear);
                inventory.addItem(createUsedItem(findMaterial("MACE", Material.BREEZE_ROD), random));
                inventory.addItem(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 8 + random.nextInt(5)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 3 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                maybeAddTotem(inventory, random, 0.42D, 1);
            }
        }

        addEndChapterHunterCoreCombatLoadout(hunter, random);
        addCommonEndChapterSupplies(inventory, random, false);
        addEndChapterHunterPositionSupplies(inventory, random, position);
        equipShieldOffhand(hunter);
        applySavedEndChapterKitLayout(hunter, EndChapterKitRole.HUNTER, kit);
    }

    private List<ItemStack> createEndChapterPreyPreviewItems(EndChapterKit kit) {
        List<ItemStack> items = new ArrayList<>();

        switch (kit) {
            case UHC -> {
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.BOW));
                items.add(new ItemStack(Material.ARROW, 58));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 7));
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.LAVA_BUCKET));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
            case MACE -> {
                items.add(new ItemStack(findMaterial("MACE", Material.BREEZE_ROD)));
                items.add(new ItemStack(findMaterial("BREEZE_ROD", Material.BLAZE_ROD), 16));
                items.add(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 36));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 2));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 7));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
            case DEBUFF -> {
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.POISON, 2));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.SLOWNESS, 2));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.WEAKNESS, 2));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 2));
                items.add(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.HEALING, 5));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 3));
                items.add(new ItemStack(Material.COBWEB, 6));
                items.add(new ItemStack(Material.SHIELD));
            }
            case DIAMOND_SMP -> {
                items.add(new ItemStack(Material.NETHERITE_SWORD));
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 6));
                items.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                items.add(new ItemStack(Material.ENDER_PEARL, 7));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
            }
            case TRAPPER -> {
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(new ItemStack(Material.COBWEB, 20));
                items.add(new ItemStack(Material.TNT, 14));
                items.add(new ItemStack(Material.TNT_MINECART, 4));
                items.add(new ItemStack(Material.RAIL, 20));
                items.add(new ItemStack(Material.ACTIVATOR_RAIL, 10));
                items.add(new ItemStack(Material.FLINT_AND_STEEL));
                items.add(new ItemStack(Material.TRIPWIRE_HOOK, 8));
                items.add(new ItemStack(Material.STRING, 16));
                items.add(new ItemStack(Material.RESPAWN_ANCHOR, 3));
                items.add(new ItemStack(Material.GLOWSTONE, 16));
                items.add(new ItemStack(Material.SHIELD));
            }
            case CRYSTAL -> {
                items.add(new ItemStack(Material.DIAMOND_PICKAXE));
                items.add(new ItemStack(Material.NETHERITE_SWORD));
                items.add(new ItemStack(Material.END_CRYSTAL, 10));
                items.add(new ItemStack(Material.OBSIDIAN, 28));
                items.add(new ItemStack(Material.RESPAWN_ANCHOR, 3));
                items.add(new ItemStack(Material.GLOWSTONE, 16));
                items.add(new ItemStack(Material.ENDER_PEARL, 7));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
            }
            case POTION -> {
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.STRENGTH, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 2));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HEALING, 5));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HARMING, 3));
                items.add(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                items.add(new ItemStack(Material.SHIELD));
            }
            case CROSSBOW -> {
                items.add(new ItemStack(Material.CROSSBOW));
                items.add(new ItemStack(Material.BOW));
                items.add(new ItemStack(Material.ARROW, 48));
                items.add(createTippedArrow(org.bukkit.potion.PotionType.SLOWNESS, 12));
                items.add(createTippedArrow(org.bukkit.potion.PotionType.HARMING, 9));
                items.add(new ItemStack(Material.FIREWORK_ROCKET, 16));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 3));
            }
            case SPEAR_HAMMER -> {
                items.add(new ItemStack(findMaterial("SPEAR", Material.TRIDENT)));
                items.add(new ItemStack(findMaterial("MACE", Material.BREEZE_ROD)));
                items.add(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 9));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 2));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }

        addEndChapterPreyCorePreviewItems(items);
        addPreviewCommonEndChapterSupplies(items, true);
        return items;
    }

    private List<ItemStack> createEndChapterHunterPreviewItems(EndChapterKit kit) {
        List<ItemStack> items = new ArrayList<>();

        switch (kit) {
            case UHC -> {
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(new ItemStack(Material.BOW));
                items.add(new ItemStack(Material.ARROW, 32));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 4));
                items.add(new ItemStack(Material.SHIELD));
            }
            case MACE -> {
                items.add(new ItemStack(findMaterial("MACE", Material.BREEZE_ROD)));
                items.add(new ItemStack(findMaterial("BREEZE_ROD", Material.BLAZE_ROD), 16));
                items.add(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 25));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 5));
                items.add(new ItemStack(Material.SHIELD));
            }
            case DEBUFF -> {
                items.add(new ItemStack(Material.IRON_SWORD));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.POISON, 1));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.SLOWNESS, 1));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.WEAKNESS, 1));
                items.add(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.HEALING, 4));
                items.add(new ItemStack(Material.SHIELD));
            }
            case DIAMOND_SMP -> {
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 4));
                items.add(new ItemStack(Material.ENDER_PEARL, 5));
            }
            case TRAPPER -> {
                items.add(new ItemStack(Material.IRON_AXE));
                items.add(new ItemStack(Material.COBWEB, 12));
                items.add(new ItemStack(Material.TNT, 7));
                items.add(new ItemStack(Material.TNT_MINECART, 3));
                items.add(new ItemStack(Material.RAIL, 12));
                items.add(new ItemStack(Material.ACTIVATOR_RAIL, 6));
                items.add(new ItemStack(Material.FLINT_AND_STEEL));
                items.add(new ItemStack(Material.TRIPWIRE_HOOK, 5));
            }
            case CRYSTAL -> {
                items.add(new ItemStack(Material.IRON_SWORD));
                items.add(new ItemStack(Material.DIAMOND_PICKAXE));
                items.add(new ItemStack(Material.END_CRYSTAL, 5));
                items.add(new ItemStack(Material.OBSIDIAN, 16));
                items.add(new ItemStack(Material.RESPAWN_ANCHOR, 2));
                items.add(new ItemStack(Material.GLOWSTONE, 8));
            }
            case POTION -> {
                items.add(new ItemStack(Material.IRON_SWORD));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.STRENGTH, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.REGENERATION, 1));
                items.add(createPotionItem(Material.SPLASH_POTION, org.bukkit.potion.PotionType.HEALING, 3));
                items.add(createPotionItem(Material.LINGERING_POTION, org.bukkit.potion.PotionType.POISON, 1));
                items.add(new ItemStack(Material.SHIELD));
            }
            case CROSSBOW -> {
                items.add(new ItemStack(Material.CROSSBOW));
                items.add(new ItemStack(Material.BOW));
                items.add(new ItemStack(Material.ARROW, 28));
                items.add(createTippedArrow(org.bukkit.potion.PotionType.SLOWNESS, 7));
                items.add(new ItemStack(Material.FIREWORK_ROCKET, 9));
                items.add(new ItemStack(Material.SHIELD));
            }
            case SPEAR_HAMMER -> {
                items.add(new ItemStack(findMaterial("SPEAR", Material.TRIDENT)));
                items.add(new ItemStack(findMaterial("MACE", Material.BREEZE_ROD)));
                items.add(new ItemStack(findMaterial("WIND_CHARGE", Material.SNOWBALL), 6));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
                items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                items.add(new ItemStack(Material.SHIELD));
            }
        }

        addEndChapterHunterCorePreviewItems(items);
        addPreviewCommonEndChapterSupplies(items, false);
        return items;
    }

    private ItemStack[] createEndChapterPreviewArmor(EndChapterKitRole role, EndChapterKit kit) {
        if (role == EndChapterKitRole.PREY) {
            return switch (kit) {
                case UHC -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
                case MACE -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 4));
                case DEBUFF -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
                case DIAMOND_SMP -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 3, 0, 0, 0),
                        createPreviewArmorPiece(Material.NETHERITE_CHESTPLATE, 4, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 3, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 3, 0, 0, 0));
                case TRAPPER -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 4, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
                case CRYSTAL -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.NETHERITE_HELMET, 4, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 4, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 4, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 4));
                case POTION -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
                case CROSSBOW -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 4, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 4, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
                case SPEAR_HAMMER -> createEndChapterPreviewArmorSet(
                        createPreviewArmorPiece(Material.NETHERITE_HELMET, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                        createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 4));
            };
        }

        return switch (kit) {
            case UHC -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 2, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_BOOTS, 2, 0, 0, 0));
            case MACE -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_BOOTS, 1, 0, 0, 4));
            case DEBUFF -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_BOOTS, 1, 0, 0, 0));
            case DIAMOND_SMP -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.DIAMOND_HELMET, 2, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 2, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 0));
            case TRAPPER -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 3, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 0));
            case CRYSTAL -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.DIAMOND_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 3, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 0));
            case POTION -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 0));
            case CROSSBOW -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.IRON_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 0, 3, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 0));
            case SPEAR_HAMMER -> createEndChapterPreviewArmorSet(
                    createPreviewArmorPiece(Material.DIAMOND_HELMET, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_CHESTPLATE, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.DIAMOND_LEGGINGS, 1, 0, 0, 0),
                    createPreviewArmorPiece(Material.IRON_BOOTS, 1, 0, 0, 3));
        };
    }

    private ItemStack[] createEndChapterPreviewArmorSet(Material helmet, Material chestplate, Material leggings, Material boots) {
        return new ItemStack[]{
                new ItemStack(helmet),
                new ItemStack(chestplate),
                new ItemStack(leggings),
                new ItemStack(boots)
        };
    }

    private ItemStack[] createEndChapterPreviewArmorSet(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        return new ItemStack[]{helmet, chestplate, leggings, boots};
    }

    private ItemStack createPreviewArmorPiece(Material material, int protectionLevel, int blastProtectionLevel,
                                              int projectileProtectionLevel, int featherFallingLevel) {
        ItemStack item = new ItemStack(material);
        if (protectionLevel > 0) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, protectionLevel);
        }
        if (blastProtectionLevel > 0) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, blastProtectionLevel);
        }
        if (projectileProtectionLevel > 0) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROJECTILE_PROTECTION, projectileProtectionLevel);
        }
        if (featherFallingLevel > 0) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FEATHER_FALLING, featherFallingLevel);
        }
        return item;
    }

    private void addPreviewCommonEndChapterSupplies(List<ItemStack> items, boolean prey) {
        items.add(new ItemStack(Material.COOKED_BEEF, prey ? 34 : 24));
        items.add(new ItemStack(Material.END_STONE, prey ? 96 : 68));
        items.add(new ItemStack(Material.CHORUS_FRUIT, prey ? 8 : 6));
        items.add(new ItemStack(Material.ENDER_PEARL, prey ? 8 : 5));
        items.add(new ItemStack(Material.OBSIDIAN, prey ? 4 : 3));
        items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, prey ? 20 : 12));
        if (prey) {
            items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
        items.add(new ItemStack(Material.ENCHANTED_BOOK));
    }

    private ItemStack extractPreviewOffHand(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item != null && item.getType() == Material.SHIELD) {
                items.remove(i);
                return item.clone();
            }
        }
        return null;
    }

    private boolean hasSword(Iterable<ItemStack> items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (item.getType().name().endsWith("_SWORD")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRangedWeapon(Iterable<ItemStack> items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            Material type = item.getType();
            if (type == Material.BOW || type == Material.CROSSBOW) {
                return true;
            }
        }
        return false;
    }

    private boolean hasShield(Iterable<ItemStack> items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item != null && item.getType() == Material.SHIELD) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPotionType(Iterable<ItemStack> items, org.bukkit.potion.PotionType potionType) {
        if (items == null || potionType == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta
                    && potionMeta.getBasePotionType() == potionType) {
                return true;
            }
        }
        return false;
    }

    private void addEndChapterPreyCoreCombatLoadout(Player prey, Random random) {
        if (prey == null) {
            return;
        }

        org.bukkit.inventory.PlayerInventory inventory = prey.getInventory();
        List<ItemStack> items = Arrays.asList(inventory.getStorageContents());
        if (!hasSword(items)) {
            inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.22D ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD,
                    random, org.bukkit.enchantments.Enchantment.SHARPNESS, 2));
        }
        if (!hasRangedWeapon(items)) {
            inventory.addItem(createEndChapterBow(random, 3, false));
            inventory.addItem(new ItemStack(Material.ARROW, 26 + random.nextInt(11)));
        }
        if (!hasShield(items)) {
            inventory.addItem(new ItemStack(Material.SHIELD));
        }
        if (!hasPotionType(items, org.bukkit.potion.PotionType.SLOW_FALLING)) {
            inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
        }
    }

    private void addEndChapterHunterCoreCombatLoadout(Player hunter, Random random) {
        if (hunter == null) {
            return;
        }

        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        List<ItemStack> items = Arrays.asList(inventory.getStorageContents());
        if (!hasSword(items)) {
            inventory.addItem(createEnchantedUsedItem(random.nextDouble() < 0.18D ? Material.DIAMOND_SWORD : Material.IRON_SWORD,
                    random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
        }
        if (!hasRangedWeapon(items)) {
            inventory.addItem(createEndChapterBow(random, 2, false));
            inventory.addItem(new ItemStack(Material.ARROW, 18 + random.nextInt(9)));
        }
        if (!hasShield(items)) {
            inventory.addItem(new ItemStack(Material.SHIELD));
        }
        if (!hasPotionType(items, org.bukkit.potion.PotionType.SLOW_FALLING)) {
            inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
        }
    }

    private void addEndChapterPreyCorePreviewItems(List<ItemStack> items) {
        if (items == null) {
            return;
        }

        if (!hasSword(items)) {
            items.add(new ItemStack(Material.DIAMOND_SWORD));
        }
        if (!hasRangedWeapon(items)) {
            items.add(new ItemStack(Material.BOW));
            items.add(new ItemStack(Material.ARROW, 30));
        }
        if (!hasShield(items)) {
            items.add(new ItemStack(Material.SHIELD));
        }
        if (!hasPotionType(items, org.bukkit.potion.PotionType.SLOW_FALLING)) {
            items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
        }
    }

    private void addEndChapterHunterCorePreviewItems(List<ItemStack> items) {
        if (items == null) {
            return;
        }

        if (!hasSword(items)) {
            items.add(new ItemStack(Material.IRON_SWORD));
        }
        if (!hasRangedWeapon(items)) {
            items.add(new ItemStack(Material.BOW));
            items.add(new ItemStack(Material.ARROW, 22));
        }
        if (!hasShield(items)) {
            items.add(new ItemStack(Material.SHIELD));
        }
        if (!hasPotionType(items, org.bukkit.potion.PotionType.SLOW_FALLING)) {
            items.add(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
        }
    }

    private void applySavedEndChapterKitLayout(Player player, EndChapterKitRole role, EndChapterKit kit) {
        if (player == null || role == null || kit == null) {
            return;
        }

        ItemStack[] savedLayout = plugin.getPlayerDataManager().getEndChapterKitLayout(player.getUniqueId(), role, kit);
        if (!hasAnyEndChapterLayoutItem(savedLayout)) {
            return;
        }

        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        ItemStack[] currentContents = inventory.getStorageContents();
        Map<String, Deque<Integer>> preferredSlots = new HashMap<>();
        ItemStack[] reordered = new ItemStack[36];
        List<ItemStack> remaining = new ArrayList<>();

        for (int slot = 0; slot < 36 && slot < savedLayout.length; slot++) {
            ItemStack layoutItem = savedLayout[slot];
            if (layoutItem == null || layoutItem.getType() == Material.AIR) {
                continue;
            }
            preferredSlots.computeIfAbsent(getEndChapterLayoutSignature(layoutItem), key -> new ArrayDeque<>()).add(slot);
        }

        for (ItemStack current : currentContents) {
            if (current == null || current.getType() == Material.AIR) {
                continue;
            }

            Deque<Integer> slotQueue = preferredSlots.get(getEndChapterLayoutSignature(current));
            Integer targetSlot = slotQueue == null ? null : slotQueue.pollFirst();
            if (targetSlot != null && targetSlot >= 0 && targetSlot < reordered.length && reordered[targetSlot] == null) {
                reordered[targetSlot] = current.clone();
            } else {
                remaining.add(current.clone());
            }
        }

        for (ItemStack item : remaining) {
            for (int slot = 0; slot < reordered.length; slot++) {
                if (reordered[slot] == null || reordered[slot].getType() == Material.AIR) {
                    reordered[slot] = item;
                    break;
                }
            }
        }

        inventory.setStorageContents(reordered);
    }

    private boolean hasAnyEndChapterLayoutItem(ItemStack[] layout) {
        if (layout == null) {
            return false;
        }
        for (ItemStack item : layout) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private String getEndChapterLayoutSignature(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }

        StringBuilder builder = new StringBuilder(item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta && potionMeta.getBasePotionType() != null) {
            builder.append('|').append(potionMeta.getBasePotionType().name());
        }
        return builder.toString();
    }

    private void addCommonEndChapterSupplies(org.bukkit.inventory.PlayerInventory inventory, Random random, boolean prey) {
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, (prey ? 28 : 20) + random.nextInt(prey ? 13 : 9)));
        inventory.addItem(new ItemStack(Material.END_STONE, (prey ? 84 : 60) + random.nextInt(prey ? 29 : 21)));
        inventory.addItem(new ItemStack(Material.CHORUS_FRUIT, (prey ? 7 : 5) + random.nextInt(4)));
        inventory.addItem(new ItemStack(Material.ENDER_PEARL, (prey ? 5 : 4) + random.nextInt(prey ? 4 : 3)));
        inventory.addItem(new ItemStack(Material.OBSIDIAN, (prey ? 3 : 2) + random.nextInt(prey ? 3 : 2)));
        inventory.addItem(new ItemStack(Material.STRING, (prey ? 3 : 2) + random.nextInt(prey ? 3 : 3)));
        inventory.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, (prey ? 12 : 10) + random.nextInt(prey ? 11 : 7)));
        if (random.nextDouble() < (prey ? 0.85D : 0.58D)) {
            inventory.addItem(createNetherChapterEnchantedBook(random));
        }
    }

    private void addEndChapterPreyPositionSupplies(org.bukkit.inventory.PlayerInventory inventory, Random random, EndPreyPosition position, int preyCount) {
        switch (position) {
            case OBSIDIAN_PLATFORM -> {
                inventory.addItem(new ItemStack(Material.OBSIDIAN, 8 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.COBBLESTONE, 24 + random.nextInt(13)));
            }
            case FOUNTAIN_FRONTLINE -> {
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(2)));
                inventory.addItem(createEndChapterBow(random, 3, false));
                inventory.addItem(new ItemStack(Material.ARROW, 16 + random.nextInt(9)));
            }
            case END_PORTAL -> {
                int eyes = 14 + random.nextInt(7);
                if (preyCount >= 2) {
                    eyes = Math.max(1, eyes / 2);
                }
                inventory.addItem(new ItemStack(Material.ENDER_EYE, eyes));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
            }
            case CHORUS_FOREST -> {
                inventory.addItem(new ItemStack(Material.CHORUS_FLOWER, 5 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.CHORUS_FRUIT, 6 + random.nextInt(4)));
            }
            case END_CITY_APPROACH -> {
                inventory.addItem(new ItemStack(Material.PURPUR_BLOCK, 16 + random.nextInt(13)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(3)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SLOW_FALLING, 1));
            }
            case OUTER_GATEWAY_RING -> {
                inventory.addItem(new ItemStack(Material.END_STONE_BRICKS, 20 + random.nextInt(17)));
                inventory.addItem(new ItemStack(Material.LADDER, 10 + random.nextInt(9)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 3 + random.nextInt(2)));
            }
        }
    }

    private void addEndChapterHunterPositionSupplies(org.bukkit.inventory.PlayerInventory inventory, Random random, EndHunterPosition position) {
        switch (position) {
            case MAIN_ISLAND_EDGE -> {
                inventory.addItem(new ItemStack(Material.END_STONE, 26 + random.nextInt(13)));
                inventory.addItem(new ItemStack(Material.COOKED_BEEF, 8 + random.nextInt(4)));
            }
            case CHORUS_SCOUT -> {
                inventory.addItem(new ItemStack(Material.CHORUS_FRUIT, 5 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 3 + random.nextInt(3)));
            }
            case END_CITY_RAIDER -> {
                inventory.addItem(new ItemStack(Material.PURPUR_BLOCK, 12 + random.nextInt(11)));
                inventory.addItem(new ItemStack(Material.IRON_INGOT, 6 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
            }
            case OUTER_RING_MINER -> {
                inventory.addItem(new ItemStack(Material.END_STONE, 28 + random.nextInt(17)));
                inventory.addItem(createEnchantedUsedItem(Material.IRON_PICKAXE, random, org.bukkit.enchantments.Enchantment.EFFICIENCY, 1));
                inventory.addItem(new ItemStack(Material.OAK_PLANKS, 14 + random.nextInt(11)));
            }
            case GATEWAY_CHASER -> {
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 5 + random.nextInt(3)));
                inventory.addItem(createPotionItem(Material.POTION, org.bukkit.potion.PotionType.SWIFTNESS, 1));
                inventory.addItem(new ItemStack(Material.END_STONE, 24 + random.nextInt(13)));
            }
            case GATEWAY_LOGISTICS -> {
                inventory.addItem(new ItemStack(Material.WHITE_WOOL, 28 + random.nextInt(25)));
                inventory.addItem(new ItemStack(Material.OAK_LOG, 20 + random.nextInt(17)));
                inventory.addItem(new ItemStack(Material.OAK_PLANKS, 24 + random.nextInt(17)));
                inventory.addItem(new ItemStack(Material.WHITE_BED, 2 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.CRAFTING_TABLE));
            }
        }
    }

    private ItemStack createPotionItem(Material material, org.bukkit.potion.PotionType potionType, int amount) {
        ItemStack item = new ItemStack(material, amount);
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta meta) {
            meta.setBasePotionType(potionType);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTippedArrow(org.bukkit.potion.PotionType potionType, int amount) {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta meta) {
            meta.setBasePotionType(potionType);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEnchantedUsedItem(Material material, Random random,
                                              org.bukkit.enchantments.Enchantment enchantment, int level) {
        ItemStack item = createUsedItem(material, random);
        if (level > 0) {
            item.addUnsafeEnchantment(enchantment, level);
        }
        return item;
    }

    private ItemStack createEndChapterBow(Random random, int powerLevel, boolean enhanced) {
        ItemStack bow = createUsedItem(Material.BOW, random);
        if (powerLevel > 0) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, Math.min(5, powerLevel));
        }
        if (powerLevel >= 2) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PUNCH, 1);
        }
        if (enhanced) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FLAME, 1);
            if (random.nextDouble() < 0.45D) {
                bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.INFINITY, 1);
            }
        }
        return bow;
    }

    private ItemStack createEndChapterCrossbow(Random random, int piercingLevel, boolean enhanced) {
        ItemStack crossbow = createUsedItem(Material.CROSSBOW, random);
        if (piercingLevel > 0) {
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PIERCING, Math.min(4, piercingLevel));
        }
        if (enhanced) {
            if (random.nextBoolean()) {
                crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.QUICK_CHARGE, 2 + random.nextInt(2));
            } else {
                crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.MULTISHOT, 1);
            }
        } else {
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.QUICK_CHARGE, 1);
        }
        return crossbow;
    }

    private void addRespawnAnchorBundle(org.bukkit.inventory.PlayerInventory inventory, int anchorAmount, int glowstoneAmount) {
        inventory.addItem(new ItemStack(Material.RESPAWN_ANCHOR, Math.max(1, anchorAmount)));
        inventory.addItem(new ItemStack(Material.GLOWSTONE, Math.max(1, glowstoneAmount)));
    }

    private void maybeAddTotem(org.bukkit.inventory.PlayerInventory inventory, Random random, double chance, int amount) {
        if (amount <= 0 || random.nextDouble() >= chance) {
            return;
        }
        inventory.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, amount));
    }

    private Material findMaterial(String materialName, Material fallback) {
        Material material = Material.matchMaterial(materialName);
        return material == null ? fallback : material;
    }

    private void stackDualPreysBeforeStart(GameRoom room) {
        if (room == null || room.getGameMode() == GameMode.SWAP || !room.isDoublePreyEnabled()) {
            if (room != null) {
                room.clearDualPreyStack();
            }
            return;
        }

        List<Player> preys = new ArrayList<>();
        for (UUID uuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(uuid);
            if (prey != null && prey.isOnline()) {
                preys.add(prey);
            }
        }

        if (preys.size() < 2) {
            room.clearDualPreyStack();
            return;
        }

        Player carrier = null;
        if (room.getLockedFirstDualPrey() != null) {
            for (Player prey : preys) {
                if (prey.getUniqueId().equals(room.getLockedFirstDualPrey())) {
                    carrier = prey;
                    break;
                }
            }
        }
        if (carrier == null) {
            carrier = preys.get(0);
        }

        List<Player> orderedPassengers = new ArrayList<>();
        for (Player prey : preys) {
            if (!prey.getUniqueId().equals(carrier.getUniqueId())) {
                orderedPassengers.add(prey);
            }
        }

        if (orderedPassengers.isEmpty()) {
            room.clearDualPreyStack();
            return;
        }

        carrier.leaveVehicle();
        carrier.eject();
        for (Player passenger : orderedPassengers) {
            passenger.leaveVehicle();
            passenger.eject();
        }

        Location baseLocation = carrier.getLocation().clone();
        baseLocation.setYaw(carrier.getLocation().getYaw());
        baseLocation.setPitch(carrier.getLocation().getPitch());

        carrier.teleport(baseLocation);
        Player middlePassenger = orderedPassengers.get(0);
        middlePassenger.teleport(baseLocation.clone().add(0, 0.15, 0));
        carrier.addPassenger(middlePassenger);

        if (room.isFlashTriplePreyEnabled() && orderedPassengers.size() >= 2) {
            Player topPassenger = orderedPassengers.get(1);
            topPassenger.teleport(baseLocation.clone().add(0, 0.35, 0));
            middlePassenger.addPassenger(topPassenger);
            room.setTriplePreyStack(carrier.getUniqueId(), middlePassenger.getUniqueId(), topPassenger.getUniqueId(), true);
            return;
        }

        room.setDualPreyStack(carrier.getUniqueId(), middlePassenger.getUniqueId(), true);
    }

    private Location findNetherChapterPreySpawn(World world) {
        Random random = new Random();
        Location worldSpawn = world.getSpawnLocation();
        int minY = Math.max(world.getMinHeight() + 8, 10);

        for (int attempt = 0; attempt < 64; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int radius = 48 + random.nextInt(209);
            int centerX = worldSpawn.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int centerZ = worldSpawn.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            forceLoadChunkArea(world, centerX - 20, centerZ - 20, centerX + 20, centerZ + 20);
            int maxY = Math.min(48, world.getHighestBlockYAt(centerX, centerZ) - 6);
            Location lavaSpawn = findNearbyNaturalLavaLakeSpawn(world, centerX, centerZ,
                    minY, maxY, 20);
            if (lavaSpawn != null) {
                return lavaSpawn;
            }
        }

        int fallbackMaxY = Math.min(48, world.getHighestBlockYAt(worldSpawn) - 6);
        forceLoadChunkArea(world, worldSpawn.getBlockX() - 32, worldSpawn.getBlockZ() - 32,
                worldSpawn.getBlockX() + 32, worldSpawn.getBlockZ() + 32);
        Location fallbackLava = findNearbyNaturalLavaLakeSpawn(world, worldSpawn.getBlockX(), worldSpawn.getBlockZ(),
                minY, fallbackMaxY, 32);
        if (fallbackLava != null) {
            return fallbackLava;
        }

        plugin.getLogger().warning("下界篇未在已加载区域找到天然岩浆湖，已回退到世界出生点附近生成猎物。");
        return getSafeSpawnLocation(world.getSpawnLocation());
    }

    private Location findNetherHunterSpawn(GameRoom room, NetherHunterScenario scenario, Location preySpawn, World targetWorld) {
        return switch (scenario) {
            case CAVE -> findUndergroundSpawn(preySpawn, targetWorld, 500, 2200);
            case CHASE -> {
                Location loc = findRandomSurfaceLocation(targetWorld, preySpawn, 300, 700, Collections.emptySet());
                yield loc == null ? getSafeSpawnLocation(targetWorld.getSpawnLocation()) : loc;
            }
            case WOOD -> {
                Set<org.bukkit.block.Biome> forestBiomes = new HashSet<>(Arrays.asList(
                        org.bukkit.block.Biome.FOREST,
                        org.bukkit.block.Biome.BIRCH_FOREST,
                        org.bukkit.block.Biome.DARK_FOREST,
                        org.bukkit.block.Biome.OLD_GROWTH_PINE_TAIGA,
                        org.bukkit.block.Biome.OLD_GROWTH_SPRUCE_TAIGA,
                        org.bukkit.block.Biome.TAIGA,
                        org.bukkit.block.Biome.JUNGLE
                ));
                Location loc = findRandomSurfaceLocation(targetWorld, preySpawn, 600, 2200, forestBiomes);
                yield loc == null ? getSafeSpawnLocation(targetWorld.getSpawnLocation()) : loc;
            }
            case TRIAL -> {
                Location loc = findTrialChamberSpawn(targetWorld, preySpawn);
                if (loc == null) {
                    loc = findUndergroundSpawn(preySpawn, targetWorld, 700, 2400);
                }
                yield loc;
            }
            case NETHER -> findNetherSpawn(room, preySpawn);
            case ANCIENT_CITY -> {
                Location loc = findAncientCitySpawn(targetWorld, preySpawn);
                if (loc == null) {
                    loc = findUndergroundSpawn(preySpawn, targetWorld, 900, 2600);
                }
                yield loc;
            }
        };
    }

    private Location prepareNetherPreySpawn(Location anchorSpawn, Collection<Location> occupied) {
        Location fallback = anchorSpawn == null ? null : anchorSpawn.clone();
        if (fallback == null || fallback.getWorld() == null) {
            return fallback;
        }

        Location candidate = fallback.clone();
        if (occupied != null && !occupied.isEmpty()) {
            boolean foundSpreadSpawn = false;
            for (int ring = 1; ring <= 4; ring++) {
                double radius = 2.5D + ring * 2.25D;
                int stepCount = 8 + ring * 4;
                for (int step = 0; step < stepCount; step++) {
                    double angle = (Math.PI * 2.0D / stepCount) * step;
                    Location shifted = fallback.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                    shifted = adaptNetherPreySpawn(shifted);
                    if (shifted == null || shifted.getWorld() == null) {
                        continue;
                    }
                    if (!isTooCloseToOccupied(shifted, occupied, 4.0D)) {
                        candidate = shifted;
                        foundSpreadSpawn = true;
                        break;
                    }
                }
                if (foundSpreadSpawn) {
                    break;
                }
            }
        }

        Location prepared = adaptNetherPreySpawn(candidate);
        return prepared == null ? fallback : prepared;
    }

    private Location adaptNetherPreySpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return location;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        if (!isChunkAreaLoaded(world, x - 1, z - 1, x + 1, z + 1)) {
            return getSafeSpawnLocation(location);
        }

        Location candidate = location.clone();
        carveSafePocket(candidate, Material.COBBLED_DEEPSLATE);
        return getSafeSpawnLocation(candidate);
    }

    private Location prepareNetherHunterSpawn(Location anchorSpawn, NetherHunterScenario scenario, World targetWorld, Collection<Location> occupied) {
        Location fallback = anchorSpawn != null ? anchorSpawn.clone() : null;
        if ((fallback == null || fallback.getWorld() == null) && targetWorld != null) {
            fallback = getSafeSpawnLocation(targetWorld.getSpawnLocation());
        }
        if (fallback == null || fallback.getWorld() == null) {
            return fallback;
        }

        Location candidate = fallback.clone();
        if (occupied != null && !occupied.isEmpty()) {
            boolean foundSpreadSpawn = false;
            for (int ring = 1; ring <= 4; ring++) {
                double radius = 5.0D + ring * 3.5D;
                int stepCount = 8 + ring * 4;
                for (int step = 0; step < stepCount; step++) {
                    double angle = (Math.PI * 2.0D / stepCount) * step;
                    Location shifted = fallback.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                    shifted = adaptNetherHunterSpawnToScenario(shifted, scenario);
                    if (shifted == null || shifted.getWorld() == null) {
                        continue;
                    }
                    if (!isTooCloseToOccupied(shifted, occupied, 8.0D)) {
                        candidate = shifted;
                        foundSpreadSpawn = true;
                        break;
                    }
                }
                if (foundSpreadSpawn) {
                    break;
                }
            }
        }

        Location prepared = adaptNetherHunterSpawnToScenario(candidate, scenario);
        return prepared == null ? fallback : prepared;
    }

    private Location adaptNetherHunterSpawnToScenario(Location location, NetherHunterScenario scenario) {
        if (location == null || location.getWorld() == null) {
            return location;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        if (!isChunkAreaLoaded(world, x - 1, z - 1, x + 1, z + 1)) {
            return getSafeSpawnLocation(location);
        }

        Location candidate = location.clone();
        if (scenario == NetherHunterScenario.CHASE || scenario == NetherHunterScenario.WOOD) {
            int y = world.getHighestBlockYAt(x, z);
            candidate.setY(y + 1.0D);
            ensureSpawnStandingSpace(candidate, getNetherHunterFloorType(scenario, world));
            return getSafeSpawnLocation(candidate);
        }

        carveSafePocket(candidate, getNetherHunterFloorType(scenario, world));
        return getSafeSpawnLocation(candidate);
    }

    private Material getNetherHunterFloorType(NetherHunterScenario scenario, World world) {
        if (scenario == NetherHunterScenario.NETHER || (world != null && world.getEnvironment() == World.Environment.NETHER)) {
            return Material.BLACKSTONE;
        }

        return switch (scenario) {
            case ANCIENT_CITY -> Material.DEEPSLATE_TILES;
            case TRIAL -> Material.TUFF_BRICKS;
            case CAVE -> Material.STONE;
            case CHASE -> Material.STONE_BRICKS;
            case WOOD -> Material.MOSS_BLOCK;
            case NETHER -> Material.BLACKSTONE;
        };
    }

    private Location findRandomSurfaceLocation(World world, Location center, int minDistance, int maxDistance, Set<org.bukkit.block.Biome> preferredBiomes) {
        Random random = new Random();
        Location base = center != null ? center : world.getSpawnLocation();

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = base.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = base.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);

            forceLoadChunkArea(world, x - 2, z - 2, x + 2, z + 2);

            if (!preferredBiomes.isEmpty()) {
                org.bukkit.block.Biome biome = world.getBiome(x, world.getHighestBlockYAt(x, z), z);
                if (!preferredBiomes.contains(biome)) {
                    continue;
                }
            }

            int y = world.getHighestBlockYAt(x, z);
            Material ground = world.getBlockAt(x, Math.max(world.getMinHeight(), y - 1), z).getType();
            if (!ground.isSolid() || ground == Material.WATER || ground == Material.LAVA) {
                continue;
            }

            Location surface = new Location(world, x + 0.5, y + 1.0, z + 0.5);
            return getSafeSpawnLocation(surface);
        }

        return null;
    }

    private void ensureLavaLakeNear(Location baseLocation) {
        if (baseLocation == null || baseLocation.getWorld() == null) {
            return;
        }

        World world = baseLocation.getWorld();
        int baseX = baseLocation.getBlockX();
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ();
        // 只清理天然岩浆湖旁边的出生位，不再人为生成岩浆；保证出生脚下不是岩浆，旁边能直接看见岩浆池。
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (world.getBlockAt(baseX + x, baseY - 1, baseZ + z).getType() == Material.LAVA
                        || world.getBlockAt(baseX + x, baseY, baseZ + z).getType() == Material.LAVA
                        || world.getBlockAt(baseX + x, baseY + 1, baseZ + z).getType() == Material.LAVA) {
                    continue;
                }
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(Material.COBBLED_DEEPSLATE, false);
                world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.AIR, false);
                world.getBlockAt(baseX + x, baseY + 1, baseZ + z).setType(Material.AIR, false);
            }
        }

        // 挖下来的痕迹：地表到天然岩浆湖旁边的竖井 + 底部通道
        createPreyDigTrace(baseLocation);
    }

    private boolean hasNearbyMaterial(Location center, Material material, int radiusX, int radiusY, int radiusZ) {
        World world = center.getWorld();
        if (world == null) return false;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    if (world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).getType() == material) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Location findNearbyNaturalLavaLakeSpawn(World world, int centerX, int centerZ, int minY, int maxY, int radius) {
        if (maxY < minY) {
            return null;
        }

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (!isChunkAreaLoaded(world, x - 2, z - 2, x + 2, z + 2)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.LAVA) {
                        continue;
                    }

                    if (countNearbyMaterial(world, x, y, z, Material.LAVA, 3, 1, 3) < 10) {
                        continue;
                    }

                    Location spawn = createSpawnNearNaturalLava(world, x, y, z);
                    if (spawn != null) {
                        return spawn;
                    }
                }
            }
        }

        return null;
    }

    private Location createSpawnNearNaturalLava(World world, int lavaX, int lavaY, int lavaZ) {
        List<org.bukkit.block.BlockFace> faces = Arrays.asList(
                org.bukkit.block.BlockFace.WEST,
                org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.NORTH_WEST,
                org.bukkit.block.BlockFace.NORTH_EAST,
                org.bukkit.block.BlockFace.SOUTH_WEST,
                org.bukkit.block.BlockFace.SOUTH_EAST
        );

        for (org.bukkit.block.BlockFace face : faces) {
            int spawnX = lavaX + face.getModX();
            int spawnZ = lavaZ + face.getModZ();
            if (!isChunkAreaLoaded(world, spawnX - 1, spawnZ - 1, spawnX + 1, spawnZ + 1)) {
                continue;
            }

            Material feetType = world.getBlockAt(spawnX, lavaY, spawnZ).getType();
            Material headType = world.getBlockAt(spawnX, lavaY + 1, spawnZ).getType();
            Material floorType = world.getBlockAt(spawnX, lavaY - 1, spawnZ).getType();

            if (feetType == Material.LAVA || headType == Material.LAVA || floorType == Material.LAVA) {
                continue;
            }

            if (countNearbyMaterial(world, spawnX, lavaY, spawnZ, Material.LAVA, 2, 1, 2) < 1) {
                continue;
            }

            if (!floorType.isSolid() || floorType == Material.AIR || floorType == Material.CAVE_AIR || floorType == Material.VOID_AIR) {
                world.getBlockAt(spawnX, lavaY - 1, spawnZ).setType(Material.COBBLED_DEEPSLATE, false);
            }

            world.getBlockAt(spawnX, lavaY, spawnZ).setType(Material.AIR, false);
            world.getBlockAt(spawnX, lavaY + 1, spawnZ).setType(Material.AIR, false);
            world.getBlockAt(spawnX, lavaY + 2, spawnZ).setType(Material.AIR, false);

            Location candidate = new Location(world, spawnX + 0.5, lavaY, spawnZ + 0.5);
            if (isStrictSpawnCandidate(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private int countNearbyMaterial(World world, int centerX, int centerY, int centerZ, Material material, int radiusX, int radiusY, int radiusZ) {
        int count = 0;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    if (world.getBlockAt(centerX + x, centerY + y, centerZ + z).getType() == material) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void createPreyDigTrace(Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) {
            return;
        }

        int baseX = baseLocation.getBlockX();
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ();
        int shaftX = baseX - 4;
        int shaftZ = baseZ;
        int surfaceY = world.getHighestBlockYAt(shaftX, shaftZ);

        // 地表入口稍微挖开一点，做出有人挖下去过的痕迹
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 2; y++) {
                    world.getBlockAt(shaftX + x, surfaceY + y, shaftZ + z).setType(Material.AIR, false);
                }
            }
        }

        // 竖井
        for (int y = baseY + 1; y <= surfaceY; y++) {
            world.getBlockAt(shaftX, y, shaftZ).setType(Material.AIR, false);
            world.getBlockAt(shaftX, y + 1, shaftZ).setType(Material.AIR, false);
            world.getBlockAt(shaftX, y, shaftZ + 1).setType(Material.AIR, false);
            world.getBlockAt(shaftX, y + 1, shaftZ + 1).setType(Material.AIR, false);
        }

        // 底部横向通道接到岩浆湖洞穴
        for (int x = shaftX; x <= baseX - 1; x++) {
            for (int y = baseY; y <= baseY + 1; y++) {
                world.getBlockAt(x, y, shaftZ).setType(Material.AIR, false);
                world.getBlockAt(x, y, shaftZ + 1).setType(Material.AIR, false);
            }
            world.getBlockAt(x, baseY - 1, shaftZ).setType(Material.COBBLED_DEEPSLATE, false);
            world.getBlockAt(x, baseY - 1, shaftZ + 1).setType(Material.COBBLED_DEEPSLATE, false);
        }
    }

    private Location findUndergroundSpawn(Location center, World world, int minDistance, int maxDistance) {
        Random random = new Random();
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = 18 + random.nextInt(28);
            forceLoadChunkArea(world, x - 2, z - 2, x + 2, z + 2);
            Location location = new Location(world, x + 0.5, y, z + 0.5);
            carveSafePocket(location, Material.STONE);
            return location;
        }
        return getSafeSpawnLocation(world.getSpawnLocation());
    }

    private Location findTrialChamberSpawn(World world, Location preySpawn) {
        try {
            org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(preySpawn, org.bukkit.generator.structure.Structure.TRIAL_CHAMBERS, 1200, false);
            if (result != null && result.getLocation() != null) {
                Location structure = result.getLocation().clone();
                forceLoadChunkArea(world, structure.getBlockX() - 56, structure.getBlockZ() - 56,
                        structure.getBlockX() + 56, structure.getBlockZ() + 56);
                Location landing = findSpawnOnAnyMaterialCluster(world, structure,
                        Arrays.asList(Material.TUFF_BRICKS, Material.CHISELED_TUFF_BRICKS, Material.POLISHED_TUFF, Material.COPPER_GRATE),
                        48, 36, Collections.emptyList(), 0.0D);
                if (landing != null) {
                    return landing;
                }
                Location location = structure.add(0.5, 1.0, 0.5);
                carveSafePocket(location, Material.TUFF_BRICKS);
                return getSafeSpawnLocation(location);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Location findAncientCitySpawn(World world, Location preySpawn) {
        try {
            org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(preySpawn, org.bukkit.generator.structure.Structure.ANCIENT_CITY, 1500, false);
            if (result != null && result.getLocation() != null) {
                Location structure = result.getLocation().clone();
                forceLoadChunkArea(world, structure.getBlockX() - 72, structure.getBlockZ() - 72,
                        structure.getBlockX() + 72, structure.getBlockZ() + 72);
                Location landing = findSpawnOnAnyMaterialCluster(world, structure,
                        Arrays.asList(Material.DEEPSLATE_TILES, Material.DEEPSLATE_BRICKS, Material.SCULK, Material.REINFORCED_DEEPSLATE),
                        60, 32, Collections.emptyList(), 0.0D);
                if (landing != null) {
                    carveSafePocket(landing, Material.DEEPSLATE_TILES);
                    return landing;
                }
                Location location = getSafeSpawnLocation(structure.add(0.5, 1.0, 0.5));
                carveSafePocket(location, Material.DEEPSLATE_TILES);
                return location;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Location findNetherSpawn(GameRoom room, Location preySpawn) {
        World netherWorld = plugin.getWorldManager().getNetherWorld(room.getRoomId());
        if (netherWorld == null) {
            return getSafeSpawnLocation(room.getGameWorld().getSpawnLocation());
        }

        Location netherAnchor = new Location(netherWorld,
                preySpawn.getX() / 8.0D + 0.5D,
                Math.min(80.0D, Math.max(48.0D, preySpawn.getY())),
                preySpawn.getZ() / 8.0D + 0.5D);
        Location structureSpawn = findNetherStructureSpawn(netherWorld, netherAnchor);
        if (structureSpawn != null) {
            return structureSpawn;
        }

        forceLoadChunkArea(netherWorld, netherAnchor.getBlockX() - 48, netherAnchor.getBlockZ() - 48,
                netherAnchor.getBlockX() + 48, netherAnchor.getBlockZ() + 48);
        Location lavaShelf = findNearbyNaturalLavaLakeSpawn(netherWorld, netherAnchor.getBlockX(), netherAnchor.getBlockZ(),
                Math.max(netherWorld.getMinHeight() + 8, 24), Math.min(netherWorld.getMaxHeight() - 8, 86), 48);
        if (lavaShelf != null) {
            return lavaShelf;
        }

        Random random = new Random();
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = 300 + random.nextInt(2601);
            int x = (int) Math.round(preySpawn.getX() / 8.0 + Math.cos(angle) * (radius / 8.0));
            int z = (int) Math.round(preySpawn.getZ() / 8.0 + Math.sin(angle) * (radius / 8.0));
            int y = 40 + random.nextInt(51);
            forceLoadChunkArea(netherWorld, x - 2, z - 2, x + 2, z + 2);
            Location location = new Location(netherWorld, x + 0.5, y, z + 0.5);
            carveSafePocket(location, Material.NETHERRACK);
            return location;
        }

        return getSafeSpawnLocation(netherWorld.getSpawnLocation());
    }

    private Location findNetherStructureSpawn(World netherWorld, Location anchor) {
        if (netherWorld == null || anchor == null) {
            return null;
        }

        List<org.bukkit.generator.structure.Structure> structures = Arrays.asList(
                org.bukkit.generator.structure.Structure.FORTRESS,
                org.bukkit.generator.structure.Structure.BASTION_REMNANT,
                org.bukkit.generator.structure.Structure.RUINED_PORTAL_NETHER
        );
        for (org.bukkit.generator.structure.Structure structureType : structures) {
            try {
                org.bukkit.util.StructureSearchResult result = netherWorld.locateNearestStructure(anchor, structureType, 1600, false);
                if (result == null || result.getLocation() == null) {
                    continue;
                }

                Location structure = result.getLocation().clone();
                forceLoadChunkArea(netherWorld, structure.getBlockX() - 64, structure.getBlockZ() - 64,
                        structure.getBlockX() + 64, structure.getBlockZ() + 64);
                Location landing;
                if (structureType == org.bukkit.generator.structure.Structure.FORTRESS) {
                    landing = findSpawnOnAnyMaterialCluster(netherWorld, structure,
                            Arrays.asList(Material.NETHER_BRICKS, Material.NETHER_BRICK_FENCE, Material.CRACKED_NETHER_BRICKS),
                            56, 42, Collections.emptyList(), 0.0D);
                } else if (structureType == org.bukkit.generator.structure.Structure.BASTION_REMNANT) {
                    landing = findSpawnOnAnyMaterialCluster(netherWorld, structure,
                            Arrays.asList(Material.BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, Material.GILDED_BLACKSTONE),
                            56, 48, Collections.emptyList(), 0.0D);
                } else {
                    landing = findSpawnOnAnyMaterialCluster(netherWorld, structure,
                            Arrays.asList(Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.NETHERRACK, Material.BLACKSTONE),
                            32, 24, Collections.emptyList(), 0.0D);
                }

                if (landing != null) {
                    ensureSpawnStandingSpace(landing, Material.BLACKSTONE);
                    return landing;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void carveSafePocket(Location location, Material floorType) {
        World world = location.getWorld();
        if (world == null) return;

        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        if (!isChunkAreaLoaded(world, baseX - 1, baseZ - 1, baseX + 1, baseZ + 1)) {
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.AIR);
                }
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(floorType);
            }
        }
    }

    private boolean isChunkLoaded(World world, int blockX, int blockZ) {
        return world != null && world.isChunkLoaded(blockX >> 4, blockZ >> 4);
    }

    private void forceLoadChunkArea(World world, int minX, int minZ, int maxX, int maxZ) {
        if (world == null) {
            return;
        }

        int minChunkX = Math.min(minX, maxX) >> 4;
        int maxChunkX = Math.max(minX, maxX) >> 4;
        int minChunkZ = Math.min(minZ, maxZ) >> 4;
        int maxChunkZ = Math.max(minZ, maxZ) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ, true);
                }
            }
        }
    }

    private boolean isChunkAreaLoaded(World world, int minX, int minZ, int maxX, int maxZ) {
        if (world == null) {
            return false;
        }

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Map<NetherHunterScenario, Integer> getNetherHunterScenarioWeights(GameRoom room) {
        Map<NetherHunterScenario, Integer> weights = new EnumMap<>(NetherHunterScenario.class);
        for (NetherHunterScenario scenario : NetherHunterScenario.values()) {
            weights.put(scenario, getNetherHunterScenarioEffectiveWeight(room, scenario));
        }
        return weights;
    }

    private NetherHunterScenario pickNetherHunterScenario(GameRoom room) {
        Map<NetherHunterScenario, Integer> weights = getNetherHunterScenarioWeights(room);
        int totalWeight = 0;
        for (int weight : weights.values()) {
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return NetherHunterScenario.CAVE;
        }

        int roll = new Random().nextInt(totalWeight);
        int current = 0;
        for (NetherHunterScenario scenario : NetherHunterScenario.values()) {
            current += weights.getOrDefault(scenario, scenario.getBaseWeight());
            if (roll < current) {
                return scenario;
            }
        }
        return NetherHunterScenario.CAVE;
    }

    private void faceHunterTowardTarget(Player hunter, Location target) {
        if (hunter == null || target == null || hunter.getWorld() == null || target.getWorld() == null) {
            return;
        }
        if (!hunter.getWorld().equals(target.getWorld())) {
            return;
        }
        Location hunterLoc = hunter.getLocation();
        double dx = target.getX() - hunterLoc.getX();
        double dy = (target.getY() + 1.62) - (hunterLoc.getY() + 1.62);
        double dz = target.getZ() - hunterLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
        hunterLoc.setYaw(yaw);
        hunterLoc.setPitch(Math.max(-90, Math.min(90, pitch)));
        hunter.teleport(hunterLoc);
    }

    private void giveNetherChapterPreyLoadout(Player prey) {
        org.bukkit.inventory.PlayerInventory inventory = prey.getInventory();
        Random random = new Random();
        double roll = random.nextDouble() * 100.0;

        if (roll < 2.0D) {
            equipArmorSet(prey, "DIAMOND", true);
            addRandomProtection(prey, random, 3, 0.55D);
        } else if (roll < 12.0D) {
            equipArmorSet(prey, "DIAMOND", false);
            addRandomProtection(prey, random, 2, 0.45D);
        } else if (roll < 28.0D) {
            equipMixedArmor(prey, "DIAMOND", "IRON");
            addRandomProtection(prey, random, 2, 0.35D);
        } else if (roll < 58.0D) {
            equipMixedArmor(prey, "COPPER", "DIAMOND");
            addRandomProtection(prey, random, 1, 0.28D);
        } else if (roll < 92.0D) {
            equipArmorSet(prey, "IRON", false);
            addRandomProtection(prey, random, 1, 0.22D);
        } else {
            equipArmorSet(prey, "COPPER", false);
            addRandomProtection(prey, random, 1, 0.14D);
        }

        inventory.addItem(createNetherChapterPreyWeapon(Material.IRON_SWORD, Material.DIAMOND_SWORD, 0.35D, random, true));
        inventory.addItem(createNetherChapterPreyWeapon(Material.IRON_AXE, Material.DIAMOND_AXE, 0.30D, random, true));
        inventory.addItem(createNetherChapterPreyWeapon(Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, 0.28D, random, false));
        if (random.nextDouble() < 0.70D) {
            inventory.addItem(createUsedItem(Material.IRON_SHOVEL, random));
        }
        inventory.addItem(new ItemStack(Material.SHIELD));
        equipShieldOffhand(prey);
        if (random.nextDouble() < 0.84D) {
            inventory.addItem(createNetherChapterPreyBow(random));
            inventory.addItem(new ItemStack(Material.ARROW, 24 + random.nextInt(25)));
        }
        inventory.addItem(new ItemStack(Material.WATER_BUCKET, 2 + random.nextInt(2)));
        inventory.addItem(new ItemStack(Material.BUCKET, 1 + random.nextInt(2)));
        inventory.addItem(new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(6)));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 22 + random.nextInt(21)));
        inventory.addItem(new ItemStack(Material.STONE, 56 + random.nextInt(57)));
        inventory.addItem(new ItemStack(Material.DIRT, 16 + random.nextInt(21)));
        inventory.addItem(new ItemStack(Material.BONE, 8 + random.nextInt(19)));
        inventory.addItem(new ItemStack(Material.TORCH, 24 + random.nextInt(25)));
        inventory.addItem(new ItemStack(Material.COBBLED_DEEPSLATE, 32 + random.nextInt(33)));
        inventory.addItem(new ItemStack(Material.COAL, 12 + random.nextInt(17)));
        inventory.addItem(new ItemStack(Material.IRON_INGOT, 8 + random.nextInt(9)));
        inventory.addItem(new ItemStack(Material.COPPER_INGOT, 8 + random.nextInt(13)));
        inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(3)));

        if (random.nextDouble() < 0.65D) {
            inventory.addItem(createUsedItem(Material.FLINT_AND_STEEL, random));
        }
        if (random.nextDouble() < 0.65D) {
            inventory.addItem(new ItemStack(Material.OBSIDIAN, 4 + random.nextInt(5)));
        }
        if (random.nextDouble() < 0.48D) {
            inventory.addItem(new ItemStack(Material.STRING, 4 + random.nextInt(7)));
        }
        if (random.nextDouble() < 0.40D) {
            inventory.addItem(new ItemStack(Material.FEATHER, 4 + random.nextInt(6)));
        }

        inventory.addItem(createNetherChapterEnchantedBook(random));
        if (random.nextDouble() < 0.55D) {
            inventory.addItem(createNetherChapterEnchantedBook(random));
        }
        if (random.nextDouble() < 0.12D) {
            inventory.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
        }
    }

    private void giveNetherChapterHunterLoadout(Player hunter, NetherHunterScenario scenario) {
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        Random random = new Random();
        inventory.addItem(createTrialWeapon(Material.STONE_SWORD, Material.IRON_SWORD, 0.58D, random));
        inventory.addItem(createTrialWeapon(Material.STONE_PICKAXE, Material.IRON_PICKAXE, 0.72D, random));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 14 + random.nextInt(13)));
        inventory.addItem(new ItemStack(Material.COBBLESTONE, 48 + random.nextInt(49)));
        inventory.addItem(new ItemStack(Material.TORCH, 20 + random.nextInt(25)));
        inventory.addItem(new ItemStack(Material.IRON_INGOT, 5 + random.nextInt(9)));
        inventory.addItem(new ItemStack(Material.COAL, 10 + random.nextInt(15)));
        inventory.addItem(new ItemStack(Material.BREAD, 5 + random.nextInt(7)));
        inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
        if (random.nextDouble() < 0.65D) {
            inventory.addItem(new ItemStack(Material.ENDER_PEARL, 1 + random.nextInt(3)));
        }

        switch (scenario) {
            case CAVE -> {
                applyCaveHunterLoadout(hunter, random);
            }
            case CHASE -> {
                equipArmorSet(hunter, "IRON", false);
                addRandomProtection(hunter, random, 1, 0.30D);
                if (random.nextDouble() < 0.25D) {
                    replaceRandomArmorWithDiamond(hunter, random, 1);
                }
                inventory.addItem(createTrialWeapon(Material.IRON_SWORD, Material.DIAMOND_SWORD, 0.18D, random));
                inventory.addItem(createTrialWeapon(Material.IRON_AXE, Material.DIAMOND_AXE, 0.16D, random));
                inventory.addItem(new ItemStack(Material.SHIELD));
                inventory.addItem(createNetherChapterPreyBow(random));
                inventory.addItem(new ItemStack(Material.ARROW, 20 + random.nextInt(17)));
                equipShieldOffhand(hunter);
                if (random.nextDouble() < 0.75D) {
                    inventory.addItem(new ItemStack(Material.ENDER_PEARL, 2 + random.nextInt(3)));
                }
                inventory.addItem(new ItemStack(Material.GOLDEN_CARROT, 6 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.OAK_PLANKS, 24 + random.nextInt(25)));
            }
            case WOOD -> {
                inventory.setHelmet(createUsedItem(Material.IRON_HELMET, random));
                inventory.setChestplate(createCopperArmorPiece("CHESTPLATE", random, true));
                inventory.setLeggings(createCopperArmorPiece("LEGGINGS", random, true));
                inventory.setBoots(createUsedItem(Material.IRON_BOOTS, random));
                inventory.addItem(createUsedItem(Material.IRON_AXE, random));
                inventory.addItem(createUsedItem(Material.IRON_SWORD, random));
                inventory.addItem(createUsedItem(Material.IRON_PICKAXE, random));
                inventory.addItem(createUsedItem(Material.STONE_SHOVEL, random));
                inventory.addItem(new ItemStack(Material.SHIELD));
                equipShieldOffhand(hunter);
                if (random.nextDouble() < 0.55D) {
                    inventory.addItem(createUsedItem(Material.BOW, random));
                    inventory.addItem(new ItemStack(Material.ARROW, 12 + random.nextInt(13)));
                }
                inventory.addItem(new ItemStack(Material.OAK_LOG, 28 + random.nextInt(29)));
                inventory.addItem(new ItemStack(Material.OAK_PLANKS, 32 + random.nextInt(33)));
                inventory.addItem(new ItemStack(Material.STICK, 12 + random.nextInt(13)));
                inventory.addItem(new ItemStack(Material.APPLE, 4 + random.nextInt(5)));
                if (random.nextBoolean()) {
                    inventory.addItem(new ItemStack(Material.GOLDEN_APPLE));
                }
            }
            case TRIAL -> {
                equipArmorSet(hunter, "IRON", false);
                if (random.nextDouble() < 0.55D) {
                    replaceRandomArmorWithDiamond(hunter, random, 1);
                }
                inventory.addItem(createTrialWeapon(Material.IRON_SWORD, Material.DIAMOND_SWORD, 0.28D, random));
                inventory.addItem(createTrialWeapon(Material.IRON_AXE, Material.DIAMOND_AXE, 0.42D, random));
                inventory.addItem(createTrialWeapon(Material.STONE_PICKAXE, Material.IRON_PICKAXE, 0.55D, random));
                inventory.addItem(new ItemStack(Material.SHIELD));
                equipShieldOffhand(hunter);
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(2)));
                inventory.addItem(createUsedItem(Material.BOW, random));
                inventory.addItem(new ItemStack(Material.ARROW, 18 + random.nextInt(15)));
                inventory.addItem(createOminousBottle(random));
                if (random.nextBoolean()) {
                    inventory.addItem(createOminousBottle(random));
                }
                inventory.addItem(new ItemStack(Material.IRON_INGOT, 6 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.MOSSY_COBBLESTONE, 20 + random.nextInt(17)));
            }
            case NETHER -> {
                equipMixedEnchantedNetherArmor(hunter, random);
                if (random.nextDouble() < 0.45D) {
                    replaceRandomArmorWithDiamond(hunter, random, 1);
                }
                inventory.addItem(createEnchantedUsedItem(Material.DIAMOND_SWORD, random, org.bukkit.enchantments.Enchantment.SHARPNESS, 1));
                inventory.addItem(createTrialWeapon(Material.IRON_AXE, Material.DIAMOND_AXE, 0.30D, random));
                inventory.addItem(new ItemStack(Material.SHIELD));
                equipShieldOffhand(hunter);
                inventory.addItem(new ItemStack(Material.BLACKSTONE, 40 + random.nextInt(41)));
                inventory.addItem(new ItemStack(Material.FIRE_CHARGE, 4 + random.nextInt(5)));
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(2)));
                inventory.addItem(new ItemStack(Material.OBSIDIAN, 6 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.COOKED_PORKCHOP, 10 + random.nextInt(9)));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 2 + random.nextInt(3)));
            }
            case ANCIENT_CITY -> {
                equipArmorSet(hunter, "IRON", false);
                addRandomProtection(hunter, random, 1, 0.35D);
                if (random.nextDouble() < 0.55D) {
                    replaceRandomArmorWithDiamond(hunter, random, 1);
                }
                inventory.addItem(createTrialWeapon(Material.IRON_SWORD, Material.DIAMOND_SWORD, 0.20D, random));
                inventory.addItem(createTrialWeapon(Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, 0.18D, random));
                inventory.addItem(createUsedItem(Material.CROSSBOW, random));
                inventory.addItem(new ItemStack(Material.ARROW, 18 + random.nextInt(15)));
                inventory.addItem(new ItemStack(Material.SHIELD));
                equipShieldOffhand(hunter);
                inventory.addItem(new ItemStack(Material.ENCHANTING_TABLE));
                inventory.addItem(new ItemStack(Material.BOOKSHELF, 4 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.SUGAR_CANE, 8 + random.nextInt(17)));
                inventory.addItem(new ItemStack(Material.LEATHER, 6 + random.nextInt(13)));
                for (int i = 0; i < 3; i++) {
                    inventory.addItem(createAncientCityHoe(random));
                }
                if (random.nextDouble() < 0.75D) {
                    inventory.addItem(createNetherChapterEnchantedBook(random));
                }
                inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
                inventory.addItem(new ItemStack(Material.ENDER_PEARL, 2 + random.nextInt(3)));
                inventory.addItem(new ItemStack(Material.ECHO_SHARD, 2 + random.nextInt(4)));
                inventory.addItem(new ItemStack(Material.SCULK, 12 + random.nextInt(13)));
                inventory.addItem(new ItemStack(Material.GLOW_BERRIES, 6 + random.nextInt(7)));
                inventory.addItem(new ItemStack(Material.CANDLE, 4 + random.nextInt(5)));
            }
        }

        inventory.addItem(new ItemStack(Material.STRING, 4 + random.nextInt(7)));
        inventory.addItem(new ItemStack(Material.ROTTEN_FLESH, 3 + random.nextInt(7)));
        inventory.addItem(new ItemStack(Material.GRAVEL, 12 + random.nextInt(13)));
        equipShieldOffhand(hunter);
    }

    private ItemStack createAncientCityHoe(Random random) {
        ItemStack hoe = createUsedItem(Material.DIAMOND_HOE, random);
        if (random.nextDouble() < 0.20D) {
            hoe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
        }
        return hoe;
    }

    private void equipArmorSet(Player player, String tier, boolean protectionTwo) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(createArmorPiece(tier + "_HELMET", Material.IRON_HELMET, protectionTwo));
        inventory.setChestplate(createArmorPiece(tier + "_CHESTPLATE", Material.IRON_CHESTPLATE, protectionTwo));
        inventory.setLeggings(createArmorPiece(tier + "_LEGGINGS", Material.IRON_LEGGINGS, protectionTwo));
        inventory.setBoots(createArmorPiece(tier + "_BOOTS", Material.IRON_BOOTS, protectionTwo));
    }

    private void equipMixedArmor(Player player, String primaryTier, String secondaryTier) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(createArmorPiece(primaryTier + "_HELMET", Material.IRON_HELMET, false));
        inventory.setChestplate(createArmorPiece(secondaryTier + "_CHESTPLATE", Material.DIAMOND_CHESTPLATE, false));
        inventory.setLeggings(createArmorPiece(primaryTier + "_LEGGINGS", Material.IRON_LEGGINGS, false));
        inventory.setBoots(createArmorPiece(primaryTier + "_BOOTS", Material.IRON_BOOTS, false));
    }

    private ItemStack createArmorPiece(String materialName, Material fallback, boolean protectionTwo) {
        Random random = new Random();
        ItemStack item;
        if (materialName.startsWith("COPPER_")) {
            item = createCopperArmorPiece(materialName.substring("COPPER_".length()), random, protectionTwo);
        } else {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = fallback;
            }
            item = new ItemStack(material);
        }
        if (protectionTwo) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 2);
        }
        return applyUsedLook(item, random, 0.12D, 0.55D);
    }

    private ItemStack createNetherChapterEnchantedBook(Random random) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        if (random.nextBoolean()) {
            meta.addStoredEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, rollProtectionLevel(random), true);
        } else {
            meta.addStoredEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, rollSharpnessLevel(random), true);
        }
        book.setItemMeta(meta);
        return book;
    }

    private int rollProtectionLevel(Random random) {
        int roll = random.nextInt(38);
        if (roll < 20) return 1;
        if (roll < 30) return 2;
        if (roll < 35) return 3;
        return 4;
    }

    private int rollSharpnessLevel(Random random) {
        int roll = random.nextInt(62);
        if (roll < 40) return 1;
        if (roll < 52) return 2;
        if (roll < 58) return 3;
        if (roll < 61) return 4;
        return 5;
    }

    private void applyCaveHunterLoadout(Player hunter, Random random) {
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        List<java.util.function.Consumer<ItemStack>> armorSetters = Arrays.asList(
                inventory::setHelmet,
                inventory::setChestplate,
                inventory::setLeggings,
                inventory::setBoots
        );
        List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(slots, random);

        int ironArmorCount = 2 + random.nextInt(2);
        int copperArmorCount = 1 + random.nextInt(3);
        int used = 0;
        for (int i = 0; i < ironArmorCount && used < slots.size(); i++, used++) {
            ItemStack piece = createRandomIronArmorPiece(slots.get(used), random);
            if (random.nextDouble() < 0.35D) {
                piece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 1);
            }
            armorSetters.get(slots.get(used)).accept(piece);
        }
        for (int i = 0; i < copperArmorCount && used < slots.size(); i++, used++) {
            armorSetters.get(slots.get(used)).accept(createCopperArmorPiece(getArmorSuffixBySlot(slots.get(used)), random, false));
        }

        if (random.nextDouble() < 0.20D) {
            replaceRandomArmorWithDiamond(hunter, random, 1);
        }

        int ironToolCount = 2 + random.nextInt(3);
        inventory.addItem(createUsedItem(Material.IRON_PICKAXE, random));
        if (ironToolCount >= 2) {
            inventory.addItem(createUsedItem(Material.IRON_SWORD, random));
        }
        if (ironToolCount >= 3) {
            inventory.addItem(createUsedItem(random.nextBoolean() ? Material.IRON_AXE : Material.IRON_SHOVEL, random));
        }

        inventory.addItem(createUsedItem(Material.STONE_SHOVEL, random));
        inventory.addItem(new ItemStack(Material.SHIELD));
        equipShieldOffhand(hunter);
        if (random.nextDouble() < 0.55D) {
            inventory.addItem(createUsedItem(Material.BOW, random));
            inventory.addItem(new ItemStack(Material.ARROW, 12 + random.nextInt(13)));
        }
        inventory.addItem(new ItemStack(Material.OAK_PLANKS, 20 + random.nextInt(21)));
        inventory.addItem(new ItemStack(Material.RAW_IRON, 6 + random.nextInt(7)));
        inventory.addItem(new ItemStack(Material.RAW_COPPER, 8 + random.nextInt(9)));
        if (random.nextDouble() < 0.60D) {
            inventory.addItem(new ItemStack(Material.GOLDEN_APPLE));
        }
    }

    private ItemStack createRandomIronArmorPiece(int slot, Random random) {
        return switch (slot) {
            case 0 -> createUsedItem(Material.IRON_HELMET, random);
            case 1 -> createUsedItem(Material.IRON_CHESTPLATE, random);
            case 2 -> createUsedItem(Material.IRON_LEGGINGS, random);
            default -> createUsedItem(Material.IRON_BOOTS, random);
        };
    }

    private String getArmorSuffixBySlot(int slot) {
        return switch (slot) {
            case 0 -> "HELMET";
            case 1 -> "CHESTPLATE";
            case 2 -> "LEGGINGS";
            default -> "BOOTS";
        };
    }

    private ItemStack createCopperArmorPiece(String suffix, Random random, boolean protectionTwo) {
        Material copperMaterial = Material.matchMaterial("COPPER_" + suffix);
        ItemStack item;
        if (copperMaterial != null) {
            item = new ItemStack(copperMaterial);
        } else {
            Material leatherType = switch (suffix) {
                case "HELMET" -> Material.LEATHER_HELMET;
                case "CHESTPLATE" -> Material.LEATHER_CHESTPLATE;
                case "LEGGINGS" -> Material.LEATHER_LEGGINGS;
                default -> Material.LEATHER_BOOTS;
            };
            item = new ItemStack(leatherType);
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(Color.fromRGB(184, 115, 51));
                item.setItemMeta(leatherMeta);
            }
        }
        if (protectionTwo) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 2);
        }
        return applyUsedLook(item, random, 0.08D, 0.40D);
    }

    private ItemStack createTrialWeapon(Material base, Material upgrade, double upgradeChance, Random random) {
        return createUsedItem(random.nextDouble() < upgradeChance ? upgrade : base, random);
    }

    private ItemStack createNetherChapterPreyWeapon(Material base, Material upgrade, double upgradeChance, Random random, boolean combatWeapon) {
        ItemStack item = createTrialWeapon(base, upgrade, upgradeChance, random);
        double enchantRoll = random.nextDouble();
        if (combatWeapon) {
            if (enchantRoll < 0.18D) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 2);
            } else if (enchantRoll < 0.48D) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
            }
        } else {
            if (enchantRoll < 0.14D) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
            } else if (enchantRoll < 0.40D) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 1);
            }
        }
        return item;
    }

    private ItemStack createNetherChapterPreyBow(Random random) {
        ItemStack bow = createUsedItem(Material.BOW, random);
        double roll = random.nextDouble();
        if (roll < 0.14D) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 3);
        } else if (roll < 0.38D) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 2);
        } else if (roll < 0.68D) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 1);
        }
        if (random.nextDouble() < 0.18D) {
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FLAME, 1);
        }
        return bow;
    }

    private void addRandomProtection(Player player, Random random, int level, double chancePerPiece) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        ItemStack[] armorPieces = {
                inventory.getHelmet(),
                inventory.getChestplate(),
                inventory.getLeggings(),
                inventory.getBoots()
        };
        for (ItemStack armorPiece : armorPieces) {
            if (armorPiece != null && random.nextDouble() < chancePerPiece) {
                armorPiece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, level);
            }
        }
    }

    private ItemStack createOminousBottle(Random random) {
        ItemStack bottle = new ItemStack(Material.OMINOUS_BOTTLE, 1);
        if (bottle.getItemMeta() instanceof org.bukkit.inventory.meta.OminousBottleMeta meta) {
            meta.setAmplifier(1 + random.nextInt(5));
            bottle.setItemMeta(meta);
        }
        return bottle;
    }

    private void equipMixedEnchantedNetherArmor(Player hunter, Random random) {
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(slots, random);

        for (int i = 0; i < slots.size(); i++) {
            ItemStack piece;
            if (i < 3) {
                piece = createRandomIronArmorPiece(slots.get(i), random);
            } else {
                piece = createGoldArmorPiece(getArmorSuffixBySlot(slots.get(i)), random);
            }
            piece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 3 + random.nextInt(2));
            switch (slots.get(i)) {
                case 0 -> inventory.setHelmet(piece);
                case 1 -> inventory.setChestplate(piece);
                case 2 -> inventory.setLeggings(piece);
                case 3 -> inventory.setBoots(piece);
            }
        }
    }

    private ItemStack createGoldArmorPiece(String suffix, Random random) {
        Material material = switch (suffix) {
            case "HELMET" -> Material.GOLDEN_HELMET;
            case "CHESTPLATE" -> Material.GOLDEN_CHESTPLATE;
            case "LEGGINGS" -> Material.GOLDEN_LEGGINGS;
            default -> Material.GOLDEN_BOOTS;
        };
        return createUsedItem(material, random);
    }

    private void replaceRandomArmorWithDiamond(Player hunter, Random random, int protectionLevel) {
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        int slot = random.nextInt(4);
        ItemStack diamondPiece = switch (slot) {
            case 0 -> createUsedItem(Material.DIAMOND_HELMET, random);
            case 1 -> createUsedItem(Material.DIAMOND_CHESTPLATE, random);
            case 2 -> createUsedItem(Material.DIAMOND_LEGGINGS, random);
            default -> createUsedItem(Material.DIAMOND_BOOTS, random);
        };
        if (protectionLevel > 0) {
            diamondPiece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, protectionLevel);
        }
        switch (slot) {
            case 0 -> inventory.setHelmet(diamondPiece);
            case 1 -> inventory.setChestplate(diamondPiece);
            case 2 -> inventory.setLeggings(diamondPiece);
            case 3 -> inventory.setBoots(diamondPiece);
        }
    }

    private void upgradeArmorPiecesToDiamond(Player player, Random random, int count, int protectionLevel) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(slots, random);

        int upgradeCount = Math.max(0, Math.min(4, count));
        for (int i = 0; i < upgradeCount; i++) {
            int slot = slots.get(i);
            ItemStack diamondPiece = switch (slot) {
                case 0 -> createUsedItem(Material.DIAMOND_HELMET, random);
                case 1 -> createUsedItem(Material.DIAMOND_CHESTPLATE, random);
                case 2 -> createUsedItem(Material.DIAMOND_LEGGINGS, random);
                default -> createUsedItem(Material.DIAMOND_BOOTS, random);
            };
            if (protectionLevel > 0) {
                diamondPiece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, protectionLevel);
            }
            switch (slot) {
                case 0 -> inventory.setHelmet(diamondPiece);
                case 1 -> inventory.setChestplate(diamondPiece);
                case 2 -> inventory.setLeggings(diamondPiece);
                case 3 -> inventory.setBoots(diamondPiece);
            }
        }
    }

    private ItemStack createUsedItem(Material material, Random random) {
        return applyUsedLook(new ItemStack(material), random, 0.10D, 0.55D);
    }

    private ItemStack applyUsedLook(ItemStack item, Random random, double minPercent, double maxPercent) {
        if (item == null || item.getItemMeta() == null) {
            return item;
        }
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable && item.getType().getMaxDurability() > 0) {
            int maxDurability = item.getType().getMaxDurability();
            double percent = minPercent + (maxPercent - minPercent) * random.nextDouble();
            int damage = Math.min(maxDurability - 1, Math.max(1, (int) Math.round(maxDurability * percent)));
            damageable.setDamage(damage);
            item.setItemMeta((ItemMeta) damageable);
        }
        return item;
    }

    private void equipShieldOffhand(Player hunter) {
        org.bukkit.inventory.PlayerInventory inventory = hunter.getInventory();
        if (inventory.getItemInOffHand().getType() == Material.AIR) {
            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && item.getType() == Material.SHIELD) {
                    inventory.setItemInOffHand(item.clone());
                    inventory.setItem(i, null);
                    break;
                }
            }
        }
    }

    private void createHunterRespawnCamp(Player hunter, Location spawn, NetherHunterScenario scenario) {
        if (hunter == null || spawn == null || spawn.getWorld() == null) {
            return;
        }

        Location bedBase = spawn.clone();
        if (scenario == NetherHunterScenario.TRIAL) {
            bedBase = createTrialRespawnCamp(spawn);
        } else if (scenario == NetherHunterScenario.ANCIENT_CITY) {
            createSimpleRespawnCamp(spawn, Material.DEEPSLATE_TILES);
        } else {
            createSimpleRespawnCamp(spawn, spawn.getWorld().getEnvironment() == World.Environment.NETHER ? Material.BLACKSTONE : Material.STONE_BRICKS);
        }

        hunter.setRespawnLocation(bedBase.clone().add(0.5, 0.6, 0.5), true);
    }

    private Location createTrialRespawnCamp(Location center) {
        World world = center.getWorld();
        if (world == null) return center;

        int shaftX = center.getBlockX();
        int shaftZ = center.getBlockZ();
        int topY = world.getHighestBlockYAt(shaftX, shaftZ);

        for (int y = center.getBlockY(); y <= topY; y++) {
            world.getBlockAt(shaftX, y, shaftZ).setType(Material.AIR);
            world.getBlockAt(shaftX, y, shaftZ + 1).setType(Material.AIR);
        }

        Location roomBase = new Location(world, shaftX, center.getBlockY(), shaftZ);
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    world.getBlockAt(roomBase.getBlockX() + x, roomBase.getBlockY() + y, roomBase.getBlockZ() + z).setType(Material.AIR);
                    if (y == 0) {
                        world.getBlockAt(roomBase.getBlockX() + x, roomBase.getBlockY() - 1, roomBase.getBlockZ() + z).setType(Material.STONE_BRICKS);
                    }
                }
            }
        }

        return placeBed(roomBase.clone().add(0, 0, 0), Material.RED_BED, org.bukkit.block.BlockFace.SOUTH);
    }

    private void createSimpleRespawnCamp(Location center, Material floorMaterial) {
        World world = center.getWorld();
        if (world == null) return;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(center.getBlockX() + x, center.getBlockY() - 1, center.getBlockZ() + z).setType(floorMaterial);
                for (int y = 0; y <= 2; y++) {
                    world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).setType(Material.AIR);
                }
            }
        }
        placeBed(center.clone(), Material.RED_BED, org.bukkit.block.BlockFace.SOUTH);
    }

    private Location placeBed(Location footLocation, Material bedMaterial, org.bukkit.block.BlockFace facing) {
        World world = footLocation.getWorld();
        if (world == null) return footLocation;

        Location foot = footLocation.clone();
        Location head = foot.clone().add(facing.getModX(), 0, facing.getModZ());

        foot.getBlock().setType(bedMaterial, false);
        head.getBlock().setType(bedMaterial, false);

        if (foot.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Bed footData) {
            footData.setFacing(facing);
            footData.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
            foot.getBlock().setBlockData(footData, false);
        }
        if (head.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Bed headData) {
            headData.setFacing(facing);
            headData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            head.getBlock().setBlockData(headData, false);
        }

        return foot;
    }

    private void giveStartButton(Player player) {
        ItemStack startBtn = new ItemStack(Material.PAPER);
        ItemMeta meta = startBtn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§D§7§0§0⚡ §x§F§F§B§B§3§3开§x§F§F§9§9§6§6始§x§F§F§7§7§9§9游§x§F§F§5§5§C§C戏");
            meta.setCustomModelData(10007);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("nether_star"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e右键提前进入个人开局倒计时");
            lore.add("§f- §a不点也会按模式时间自动开始");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            startBtn.setItemMeta(meta);
        }
        player.getInventory().setItem(4, startBtn);
    }

    public void triggerGameStart(GameRoom room, Player prey) {
        if (!isPreyManualStartAllowed(room)) {
            return;
        }

        for (UUID uuid : getSelectionPreys(room)) {
            Player selectionPrey = Bukkit.getPlayer(uuid);
            if (selectionPrey != null) {
                selectionPrey.getInventory().remove(Material.PAPER);
            }
        }

        // 标记猎物已按下开始，冻结所有玩家移动
        room.setGameStartCountdown(true);
        room.setPreyStarted(true);

        int randomCount = 2 + new Random().nextInt(9); // 随机2~10秒
        room.setPreyStartCountdownSeconds(randomCount);
        final int totalPreyCount = randomCount;

        new BukkitRunnable() {
            int count = totalPreyCount;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || room.isGameActuallyStarted()) {
                    room.setPreyStartCountdownSeconds(0);
                    cancel();
                    return;
                }
                room.setPreyStartCountdownSeconds(Math.max(0, count));
                if (count <= 0) {
                    // 倒计时结束，解除冻结
                    room.setGameStartCountdown(false);
                    room.setPreyStartCountdownSeconds(0);

                    // 取消猎人的30秒预开始倒计时（如果还在运行）
                    BukkitTask preGameTask = preGameCountdownTasks.remove(room.getRoomId());
                    if (preGameTask != null) {
                        preGameTask.cancel();
                    }

                    // 游戏正式开始
                    doActualGameStart(room);
                    cancel();
                    return;
                }

                // 显示倒计时给猎物
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        if (room.isPrey(uuid)) {
                            // 猎物：显示自己的倒计时，经验条跟随
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + count + " §6秒");
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7准备开始...");
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            p.showTitle(title);
                            p.setLevel(count);
                            p.setExp(count / (float) totalPreyCount);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                        // 猎人保持他们自己的30秒倒计时（由 startPreGameCountdown 负责更新）
                        // ShowCountdownToHunter 修饰符的猎人也显示
                        if (!room.isPrey(uuid) && room.hasModifier("ShowCountdownToHunter")) {
                            Component showTitleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + count + " §6秒");
                            Component showSubComp = LegacyComponentSerializer.legacySection().deserialize("§7准备开始...");
                            Title showTitle = Title.title(showTitleComp, showSubComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            if (!isTournamentSilent(room)) {
                                p.showTitle(showTitle);
                            }
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void triggerFlashTournamentStartByMovement(GameRoom room) {
        if (room == null
                || !room.getGameMode().isFlashTournament()
                || room.getState() != RoomState.PLAYING
                || room.isGameActuallyStarted()) {
            return;
        }
        room.setGameStartCountdown(false);
        room.setPreyStarted(true);
        BukkitTask preGameTask = preGameCountdownTasks.remove(room.getRoomId());
        if (preGameTask != null) {
            preGameTask.cancel();
        }
        doActualGameStart(room);
    }

    /**
     * 30秒预开始倒计时
     * 猎人和猎物都看到倒计时，猎物提前按下开始后：
     * - 猎物切换为自己的随机倒计时
     * - 猎人继续看30秒倒计时（到0显示"稍等"）
     */
    private void startPreGameCountdown(GameRoom room) {
        final boolean manualStartAllowed = isPreyManualStartAllowed(room);
        final boolean silentTournament = isTournamentSilent(room);
        final int totalSeconds = room.getGameMode() == GameMode.NETHER_CHAPTER
                ? 15
                : room.getGameMode() == GameMode.END_CHAPTER ? getEndChapterPreStartSeconds() : 30;

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = totalSeconds;
            int waitAfterHunterCountdownEnded = 0;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    preGameCountdownTasks.remove(room.getRoomId());
                    cancel();
                    return;
                }
                if (room.isGameActuallyStarted()) {
                    preGameCountdownTasks.remove(room.getRoomId());
                    cancel();
                    return;
                }

                // 猎物已按下开始，猎人继续倒计时直至0
                if (manualStartAllowed && room.isPreyStarted()) {
                    if (timeLeft <= 0) {
                        int preyCountdown = Math.max(0, room.getPreyStartCountdownSeconds());
                        if (!room.isGameStartCountdown() || preyCountdown <= 0 || waitAfterHunterCountdownEnded >= 12) {
                            room.setGameStartCountdown(false);
                            room.setPreyStartCountdownSeconds(0);
                            preGameCountdownTasks.remove(room.getRoomId());
                            doActualGameStart(room);
                            cancel();
                            return;
                        }

                        // 猎人30秒结束，猎物倒计时还在跑 → 临时显示"稍等"，但不再把任务取消到永久卡住
                        for (UUID uuid : room.getAllPlayerUUIDs()) {
                            if (!room.isPrey(uuid)) {
                                Player hunter = Bukkit.getPlayer(uuid);
                                if (hunter != null) {
                                    Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⌛ §e稍等");
                                    Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7等待猎物倒计时结束...");
                                    Title title = Title.title(titleComp, subComp,
                                            Title.Times.times(Duration.ZERO, Duration.ofMillis(999999), Duration.ofMillis(200)));
                                    if (!silentTournament) {
                                        hunter.showTitle(title);
                                    }
                                    hunter.setLevel(0);
                                    hunter.setExp(0);
                                }
                            }
                        }
                        waitAfterHunterCountdownEnded++;
                        return;
                    }

                    // 更新猎人经验条和标题（继续倒计时）
                    float progress = timeLeft / (float) totalSeconds;
                    for (UUID uuid : room.getAllPlayerUUIDs()) {
                        if (!room.isPrey(uuid)) {
                            Player hunter = Bukkit.getPlayer(uuid);
                            if (hunter != null) {
                                hunter.setLevel(timeLeft);
                                hunter.setExp(Math.max(0, Math.min(1, progress)));
                                Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + timeLeft + " §6秒后开始游戏");
                                Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7或等待猎物提前开始游戏");
                                Title title = Title.title(titleComp, subComp,
                                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                                if (!silentTournament) {
                                    hunter.showTitle(title);
                                }
                            }
                        }
                    }
                    timeLeft--;
                    return;
                }

                // 猎物还没按开始，30秒到了 → 自动开始
                if (timeLeft <= 0) {
                    if (manualStartAllowed) {
                        // 强制移除猎物的开始按钮
                        for (UUID uuid : getSelectionPreys(room)) {
                            Player prey = Bukkit.getPlayer(uuid);
                            if (prey != null) {
                                prey.getInventory().remove(Material.PAPER);
                            }
                        }
                        preGameCountdownTasks.remove(room.getRoomId());
                        cancel();
                        triggerGameStartAuto(room);
                    } else {
                        preGameCountdownTasks.remove(room.getRoomId());
                        cancel();
                        doActualGameStart(room);
                    }
                    return;
                }

                // 正常倒计时，展示给所有人（包括旁观者）
                float progress = timeLeft / (float) totalSeconds;
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.setLevel(timeLeft);
                        p.setExp(Math.max(0, Math.min(1, progress)));

                        if (!room.isPrey(uuid)) {
                            // 猎人
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + timeLeft + " §6秒后开始游戏");
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize(
                                    manualStartAllowed ? "§7或等待猎物提前开始游戏" : getNoManualStartCountdownSubtitle(room, false));
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            if (!silentTournament) {
                                p.showTitle(title);
                            }
                        } else {
                            // 猎物
                            Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + timeLeft + " §6秒后开始游戏");
                            Component subComp = LegacyComponentSerializer.legacySection().deserialize(
                                    manualStartAllowed ? "§a右键手中按钮可提前开始！" : getNoManualStartCountdownSubtitle(room, true));
                            Title title = Title.title(titleComp, subComp,
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                            if (!silentTournament) {
                                p.showTitle(title);
                            }
                        }
                    }
                }
                // 旁观者也看到倒计时
                for (UUID uuid : room.getSpectators()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.setLevel(timeLeft);
                        p.setExp(Math.max(0, Math.min(1, progress)));
                        Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + timeLeft + " §6秒后开始游戏");
                        Component subComp = LegacyComponentSerializer.legacySection().deserialize(
                                manualStartAllowed ? "§7或等待猎物提前开始游戏" : getNoManualStartCountdownSubtitle(room, false));
                        Title title = Title.title(titleComp, subComp,
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                        if (!silentTournament) {
                            p.showTitle(title);
                        }
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        preGameCountdownTasks.put(room.getRoomId(), task);
    }

    private boolean isPreyManualStartAllowed(GameRoom room) {
        return room.getGameMode() != GameMode.NETHER_CHAPTER
                && room.getGameMode() != GameMode.END_CHAPTER
                && room.getGameMode() != GameMode.END_FLASH
                && !room.getGameMode().isFlashTournament();
    }

    private String getNoManualStartCountdownSubtitle(GameRoom room, boolean prey) {
        if (room.getGameMode() == GameMode.END_FLASH) {
            return prey ? "§x§B§B§8§8§F§F终章 · 闪光将自动开始" : "§7终章 · 闪光即将正式开始";
        }
        if (room.getGameMode() == GameMode.END_CHAPTER) {
            return prey ? "§x§B§B§8§8§F§F末地篇将自动开始" : "§7末地篇即将正式开始";
        }
        if (room.getGameMode() == GameMode.NETHER_CHAPTER) {
            return prey ? "§x§F§F§6§6§0§0下界篇将在15秒后自动开始" : "§7下界篇即将正式开始";
        }
        return prey ? "§a右键手中按钮可提前开始！" : "§7或等待猎物提前开始游戏";
    }

    /** 30秒自动到期时触发（等价于猎物按下开始，但无人按） */
    private void triggerGameStartAuto(GameRoom room) {
        room.setGameStartCountdown(true);
        room.setPreyStarted(true);

        int randomCount = 2 + new Random().nextInt(9);
        room.setPreyStartCountdownSeconds(randomCount);
        final int totalCount = randomCount;

        new BukkitRunnable() {
            int count = totalCount;

            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING || room.isGameActuallyStarted()) {
                    room.setPreyStartCountdownSeconds(0);
                    cancel();
                    return;
                }
                room.setPreyStartCountdownSeconds(Math.max(0, count));
                if (count <= 0) {
                    room.setGameStartCountdown(false);
                    room.setPreyStartCountdownSeconds(0);
                    // 取消预游戏任务（已在调用前cancel，保险起见再清一次）
                    BukkitTask t = preGameCountdownTasks.remove(room.getRoomId());
                    if (t != null) t.cancel();
                    doActualGameStart(room);
                    cancel();
                    return;
                }

                // 所有人看到自动开始倒计时（包括旁观者）
                for (UUID uuid : room.getAllPlayerUUIDs()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + count + " §6秒");
                        Component subComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⚡ §e自动开始...");
                        Title title = Title.title(titleComp, subComp,
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                        if (!isTournamentSilent(room)) {
                            p.showTitle(title);
                        }
                        p.setLevel(count);
                        p.setExp(count / (float) totalCount);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
                // 旁观者也看到倒计时
                for (UUID uuid : room.getSpectators()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e" + count + " §6秒");
                        Component subComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⚡ §e自动开始...");
                        Title title = Title.title(titleComp, subComp,
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
                        if (!isTournamentSilent(room)) {
                            p.showTitle(title);
                        }
                        p.setLevel(count);
                        p.setExp(count / (float) totalCount);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /** 游戏正式开始的核心逻辑（从倒计时结束后调用） */
    private void doActualGameStart(GameRoom room) {
        if (room == null || room.isGameActuallyStarted()) {
            return;
        }
        // 设置游戏开始时间（从这里开始计时）
        room.setGameStartTime(System.currentTimeMillis());
        // 标记游戏正式开始
        room.setGameActuallyStarted(true);
        room.setGameStartCountdown(false);
        room.setPreyStartCountdownSeconds(0);
        setTournamentAdvancementAnnouncements(room, true);
        room.setDualPreyStackLocked(false);
        plugin.getPlayerListener().resetCompassTpState(room);
        plugin.getPlayerListener().primeFlashStartCompassCooldown(room);

        // 游戏正式开始
        if (!isTournamentSilent(room)) {
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.go"));
        }

        // 启用房间所有游戏维度的时间流逝和生物生成
        plugin.getWorldManager().enableGameWorldRules(room.getRoomId());
        applyStartModifiers(room);
        boolean flashMode = plugin.getFlashModeManager().isFlashMode(room);
        if (flashMode) {
            clearFlashModeStormUntilDelay(room);
        }

        // 获取猎物名字
        String preyNames = room.getPreyNames().isEmpty() ? "未知" : String.join("、", room.getPreyNames());

        // 解除所有限制，清空经验条
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (room.isSpectator(uuid)) {
                    keepSpectatorModeForGameStart(room, p);
                    continue;
                }
                plugin.getRoomManager().resetPlayerForGameStart(room, p);
                // 末影龙音效
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

                // 游戏开始后清空经验条
                p.setLevel(0);
                p.setExp(0);
                if (room.getGameMode() == GameMode.END_FLASH) {
                    int startExpLevel = room.getAssignedEndFlashKitStartExpLevel(uuid);
                    p.setTotalExperience(0);
                    p.setLevel(startExpLevel);
                    p.setExp(0.0F);
                }

                if (flashMode && !isTournamentSilent(room)) {
                    giveFlashStartGuideBook(p, room);
                }

                // 猎物解除飞行限制，正常生存模式
                if (room.isPrey(uuid)) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    p.resetCooldown();
                    if (flashMode) {
                        giveFlashPreyStartCondensedEnderPearl(p, room);
                        if (room.getPreyUUIDs().size() >= 2 && !isTournamentSilent(room)) {
                            giveFlashPreyItems(p, room);
                        }
                    }
                }

                // 给猎人标题：游戏开始！副标题：本次猎物是...
                if (room.isHunter(uuid)) {
                    Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⚔ §6§l游戏开始！");
                    Component subComp = LegacyComponentSerializer.legacySection().deserialize("§f本次猎物是：§c" + preyNames);
                    Title title = Title.title(titleComp, subComp,
                            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(500)));
                    if (!isTournamentSilent(room)) {
                        p.showTitle(title);
                    }
                    giveHunterItems(p, room);
                }
            }
        }

        if (room.getGameMode().isFlashTournament()) {
            giveFlashTournamentStartPseudoPoisonPotatoes(room);
        }

        if (room.getGameMode() == GameMode.RANDOM_COMPASS) {
            startRandomCompassMode(room);
        }
        if (room.getGameMode() == GameMode.SWAP) {
            startSwapMode(room);
        }
        if (room.getGameMode() == GameMode.SURVIVAL) {
            startSurvivalMode(room);
        }
    }

    private void giveFlashTournamentStartPseudoPoisonPotatoes(GameRoom room) {
        if (room == null || !room.getGameMode().isFlashTournament()) {
            return;
        }

        List<Player> players = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isSpectator(uuid) || (!room.isHunter(uuid) && !room.isPrey(uuid))) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        if (players.size() < 2) {
            return;
        }

        Collections.shuffle(players, ThreadLocalRandom.current());
        for (int i = 0; i + 1 < players.size(); i += 2) {
            if (ThreadLocalRandom.current().nextDouble() >= 0.25D) {
                continue;
            }
            Player target = ThreadLocalRandom.current().nextBoolean() ? players.get(i) : players.get(i + 1);
            ItemStack potato = plugin.getFlashModeManager().createPseudoPoisonPotato();
            potato.setAmount(1);
            Map<Integer, ItemStack> leftovers = target.getInventory().addItem(potato);
            if (!leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    if (leftover != null && leftover.getType() != Material.AIR) {
                        target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                    }
                }
            }
        }
    }

    private void keepSpectatorModeForGameStart(GameRoom room, Player player) {
        if (player == null) {
            return;
        }
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        giveSpectatorItems(player);
        if (room != null) {
            syncEndDimensionBrightness(player, room);
            plugin.getRoomManager().setSpectatorNameTag(player, room.getRoomId());
        }
    }

    private void applyStartModifiers(GameRoom room) {
        if (room.hasModifier("ThunderStorm")) {
            applyThunderStormWeather(room);
        }

        if (room.hasModifier("NoLocatorBar")) {
            plugin.getWorldManager().setLocatorBarEnabled(room.getRoomId(), false);
        }

        if (room.hasModifier("RewardChest")) {
            spawnRewardChest(room);
            giveRewardChestCompass(room);
        }

        if (room.hasModifier("HunterTPOnDeath") && getHunterCount(room) < 2) {
            room.broadcast(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c猎人死亡TP修饰符启用失败：当前猎人数量不足2人。");
        }
    }

    private void applyFlashModeStorm(GameRoom room) {
        if (room == null || !plugin.getFlashModeManager().isFlashMode(room)) {
            return;
        }
        World world = room.getGameWorld();
        if (world == null
                || plugin.getWorldManager().isLobbyLikeWorld(world)
                || !plugin.getWorldManager().isPrimaryGameWorld(room.getRoomId(), world)) {
            plugin.getWorldManager().keepLobbyWeatherClear();
            return;
        }
        applyStorm(world, true);
        plugin.getWorldManager().keepLobbyWeatherClear();
    }

    private void updateFlashModeStorm(GameRoom room) {
        if (room == null || !plugin.getFlashModeManager().isFlashMode(room) || !room.isGameActuallyStarted()) {
            return;
        }
        if (System.currentTimeMillis() - room.getGameStartTime() < FLASH_STORM_DELAY_MILLIS) {
            clearFlashModeStormUntilDelay(room);
            return;
        }
        applyFlashModeStorm(room);
    }

    private void clearFlashModeStormUntilDelay(GameRoom room) {
        if (room == null || !plugin.getFlashModeManager().isFlashMode(room)) {
            return;
        }
        World world = room.getGameWorld();
        if (world != null
                && !plugin.getWorldManager().isLobbyLikeWorld(world)
                && plugin.getWorldManager().isPrimaryGameWorld(room.getRoomId(), world)) {
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            clearStorm(world);
        }
        plugin.getWorldManager().keepLobbyWeatherClear();
    }

    public void giveHunterItems(Player hunter, GameRoom room) {
        GameMode mode = room.getGameMode();

        // 经典模式、随机指南针模式、互换模式、存活模式给追踪指南针
        if (mode == GameMode.CLASSIC || mode == GameMode.RANDOM_COMPASS || mode == GameMode.SWAP || mode == GameMode.SURVIVAL || mode == GameMode.NETHER_CHAPTER || mode == GameMode.END_CHAPTER || mode.isFlashLike()) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                if (mode.isFlashTournament()) {
                    meta.setDisplayName("§fCompass");
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    compass.setItemMeta(meta);
                    hunter.getInventory().addItem(compass);
                    return;
                }
                meta.setDisplayName("§x§F§F§A§A§5§5🧭 §x§F§F§C§C§7§7追§x§F§F§E§E§9§9踪§x§D§D§F§F§9§9指§x§B§B§F§F§7§7南§x§9§9§F§F§5§5针");
                List<String> lore = new ArrayList<>();
                lore.add("§8· · · · · · · · · · · · · ·");
                lore.add("§f- §e主手或副手持有");
                lore.add("§f- §a显示最近猎物距离");
                lore.add("§f- §d连续扔掉两次获得TP");
                if (mode.isFlashLike()) {
                    lore.add("§f- §b右键打开6行共享背包");
                    lore.add("§f- §d左键打开3行个人背包");
                    lore.add("§f- §c距离猎物70格内不能传送队友");
                }
                lore.add("§8· · · · · · · · · · · · · ·");
                meta.setLore(lore);
                compass.setItemMeta(meta);
            }
            hunter.getInventory().addItem(compass);
        }
    }

    public void giveFlashPreyItems(Player prey, GameRoom room) {
        if (room == null || !plugin.getFlashModeManager().isFlashMode(room) || !room.isPrey(prey.getUniqueId()) || room.getPreyUUIDs().size() < 2) {
            return;
        }
        boolean hasCompass = false;
        for (ItemStack item : prey.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                hasCompass = true;
                break;
            }
        }
        if (hasCompass) {
            return;
        }
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§A§A§D§D🧭 §x§F§F§C§C§E§E猎§x§F§F§E§E§F§F物§x§D§D§F§F§F§F共§x§B§B§F§F§E§E鸣§x§9§9§F§F§D§D指§x§7§7§F§F§C§C南§x§5§5§F§F§B§B针");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e主手或副手持有");
            lore.add("§f- §a显示最近队友猎物距离");
            lore.add("§f- §d连续扔掉两次传送到猎物队友");
            lore.add("§f- §b右键打开6行共享背包");
            lore.add("§f- §d左键打开3行个人背包");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        prey.getInventory().addItem(compass);
    }

    private void giveFlashStartGuideBook(Player player, GameRoom room) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (room == null
                || !plugin.getFlashModeManager().isFlashMode(room)
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || !room.getAllPlayerUUIDs().contains(player.getUniqueId())
                || room.isSpectator(player.getUniqueId())) {
            return;
        }
        ItemStack guide = plugin.getFlashModeManager().createFlashGameGuideBook();
        StartItemGiveResult giveResult = giveOrEnderChestOrDrop(player, guide);
        if (!isTournamentSilent(room)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§9§8§D§D§F§F✦ §b已收到 §f主 · 闪光书§7，§f右键§7打开主目录，点击目录可打开对应内容，§8可直接丢弃。"
                    + (giveResult.storedInEnderChest() ? "§8（§e背包已满，已放入末影箱§8）" : "")
                    + (giveResult.dropped() ? "§8（§c末影箱也满了，已掉落在脚下§8）" : ""));
        }
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.58f, 1.18f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.34f, 1.58f);
    }

    private void giveFlashPreyStartCondensedEnderPearl(Player prey, GameRoom room) {
        if (prey == null || !prey.isOnline()) {
            return;
        }
        if (room == null
                || !plugin.getFlashModeManager().isFlashMode(room)
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || !room.isPrey(prey.getUniqueId())
                || room.isHunter(prey.getUniqueId())) {
            return;
        }
        ItemStack pearl = plugin.getFlashModeManager().createCondensedEnderPearl();
        StartItemGiveResult giveResult = giveOrEnderChestOrDrop(prey, pearl);
        if (!isTournamentSilent(room)) {
            prey.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§9§8§D§D§F§F✦ §d开局获得浓缩末影珍珠§7，§f右键§7可随机跃迁 §f50~200格§7。"
                    + (giveResult.storedInEnderChest() ? "§8（§e背包已满，已放入末影箱§8）" : "")
                    + (giveResult.dropped() ? "§8（§c末影箱也满了，已掉落在脚下§8）" : ""));
        }
        prey.playSound(prey.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 0.58f, 1.46f);
        prey.playSound(prey.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.32f, 1.86f);
        if (room.getGameMode().isFlashTournament()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> ensureFlashTournamentCondensedEnderPearl(prey.getUniqueId(), room.getRoomId()), 10L);
        }
    }

    private void ensureFlashTournamentCondensedEnderPearl(UUID preyUuid, String roomId) {
        Player prey = Bukkit.getPlayer(preyUuid);
        GameRoom room = plugin.getRoomManager().getRoom(roomId);
        if (prey == null || room == null || !room.getGameMode().isFlashTournament()
                || room.getState() != RoomState.PLAYING
                || !room.isGameActuallyStarted()
                || !room.isPrey(preyUuid)) {
            return;
        }
        if (hasCondensedEnderPearl(prey.getInventory()) || hasCondensedEnderPearl(prey.getEnderChest())) {
            return;
        }
        ItemStack pearl = plugin.getFlashModeManager().createCondensedEnderPearl();
        HashMap<Integer, ItemStack> leftover = prey.getInventory().addItem(pearl);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> prey.getWorld().dropItemNaturally(prey.getLocation(), item));
        }
    }

    private boolean hasCondensedEnderPearl(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack item : inventory.getContents()) {
            if (plugin.getFlashModeManager().isCondensedEnderPearlItem(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean consumeRandomCompass(Player player, GameRoom room) {
        if (room.getGameMode() != GameMode.RANDOM_COMPASS || !room.isGameActuallyStarted()) {
            return false;
        }

        if (!room.isHunter(player.getUniqueId()) && !room.isPrey(player.getUniqueId())) {
            return false;
        }

        List<Player> targets = new ArrayList<>();
        if (room.isHunter(player.getUniqueId())) {
            for (UUID preyUuid : getTrackablePreys(room)) {
                Player prey = Bukkit.getPlayer(preyUuid);
                if (prey != null && prey.isOnline()) {
                    targets.add(prey);
                }
            }
        } else {
            for (UUID hunterUuid : room.getAllPlayerUUIDs()) {
                if (!room.isHunter(hunterUuid)) continue;
                Player hunter = Bukkit.getPlayer(hunterUuid);
                if (hunter != null && hunter.isOnline()) {
                    targets.add(hunter);
                }
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.random_compass_no_target"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        int useCount = room.incrementRandomCompassUseCount(player.getUniqueId());
        int commonAmplifier = useCount == 1 ? 1 : 2; // 1次=II，2次及以后=III
        boolean givePoison = useCount >= 3;

        for (Player target : targets) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, RANDOM_COMPASS_GLOW_TICKS, commonAmplifier, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, RANDOM_COMPASS_SLOW_TICKS, commonAmplifier, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, RANDOM_COMPASS_HUNGER_TICKS, commonAmplifier, false, true, true));
            if (givePoison) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 8 * 20, 1, false, true, true));
            }
            target.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.random_compass_affected"));
            target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.9f, 1.2f);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.random_compass_used", placeholders));

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.4f);
        return true;
    }

    private void startGameTask(GameRoom room) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (room.getState() != RoomState.PLAYING) {
                    cleanupRandomCompassMode(room.getRoomId());
                    cleanupSurvivalMode(room.getRoomId());
                    cleanupThunderStormWeather(room);
                    cancel();
                    return;
                }

                // 更新指南针追踪
                updateCompassTracking(room);
                updateRewardChestCompass(room);
                updateFlashModeStorm(room);
                updateThunderStormEffects(room);

                if (room.getGameMode() == GameMode.RANDOM_COMPASS && room.isGameActuallyStarted()) {
                    updateRandomCompassCycle(room);
                }

                if (room.getGameMode() == GameMode.SWAP && room.isGameActuallyStarted()) {
                    updateSwapMode(room);
                }

                if (room.getGameMode() == GameMode.SURVIVAL && room.isGameActuallyStarted()) {
                    updateSurvivalBossBar(room);
                }

                // 检查存活模式时间
                if (room.getGameMode() == GameMode.SURVIVAL) {
                    long duration = room.getGameDuration();
                    int survivalTime = plugin.getConfigManager().getConfig().getInt("hunter_game.survival_time", 30) * 60 * 1000;
                    if (duration >= survivalTime) {
                        endGame(room, true);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        gameTasks.put(room.getRoomId(), task);
    }

    private void startRandomCompassMode(GameRoom room) {
        cleanupRandomCompassMode(room.getRoomId());

        BossBar bossBar = Bukkit.createBossBar(
                buildRandomCompassBossBarTitle(RANDOM_COMPASS_INTERVAL_SECONDS),
                BarColor.YELLOW,
                BarStyle.SEGMENTED_10
        );
        bossBar.setVisible(true);

        randomCompassBossBars.put(room.getRoomId(), bossBar);
        randomCompassCountdowns.put(room.getRoomId(), RANDOM_COMPASS_INTERVAL_SECONDS);

        refreshRandomCompassBossBarPlayers(room);
        updateRandomCompassBossBar(room, RANDOM_COMPASS_INTERVAL_SECONDS);
    }

    private void updateRandomCompassCycle(GameRoom room) {
        BossBar bossBar = randomCompassBossBars.get(room.getRoomId());
        if (bossBar == null) {
            startRandomCompassMode(room);
            return;
        }

        refreshRandomCompassBossBarPlayers(room);

        int timeLeft = randomCompassCountdowns.getOrDefault(room.getRoomId(), RANDOM_COMPASS_INTERVAL_SECONDS);
        if (timeLeft <= 0) {
            giveRandomCompassToRandomPlayer(room);
            timeLeft = RANDOM_COMPASS_INTERVAL_SECONDS;
        }

        updateRandomCompassBossBar(room, timeLeft);
        randomCompassCountdowns.put(room.getRoomId(), timeLeft - 1);
    }

    private void updateRandomCompassBossBar(GameRoom room, int timeLeft) {
        BossBar bossBar = randomCompassBossBars.get(room.getRoomId());
        if (bossBar == null) return;

        double progress = Math.max(0.0D, Math.min(1.0D, timeLeft / (double) RANDOM_COMPASS_INTERVAL_SECONDS));
        bossBar.setProgress(progress);
        bossBar.setTitle(buildRandomCompassBossBarTitle(timeLeft));

        if (timeLeft <= 60) {
            bossBar.setColor(BarColor.RED);
        } else if (timeLeft <= 180) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
    }

    private String buildRandomCompassBossBarTitle(int timeLeft) {
        return "§x§F§F§A§A§5§5🧭 §x§F§F§C§C§7§7幽§x§F§F§E§E§9§9匿§x§D§D§F§F§9§9指§x§B§B§F§F§7§7南§x§9§9§F§F§5§5针 §f将在 §e"
                + formatBossBarTime(timeLeft) + " §f后降临";
    }

    private String formatBossBarTime(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void refreshRandomCompassBossBarPlayers(GameRoom room) {
        BossBar bossBar = randomCompassBossBars.get(room.getRoomId());
        if (bossBar == null) return;

        Set<Player> viewers = new HashSet<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                viewers.add(player);
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player spectator = Bukkit.getPlayer(uuid);
            if (spectator != null && spectator.isOnline()) {
                viewers.add(spectator);
            }
        }

        for (Player existing : new ArrayList<>(bossBar.getPlayers())) {
            if (!viewers.contains(existing)) {
                bossBar.removePlayer(existing);
            }
        }

        for (Player viewer : viewers) {
            if (!bossBar.getPlayers().contains(viewer)) {
                bossBar.addPlayer(viewer);
            }
        }
    }

    private void giveRandomCompassToRandomPlayer(GameRoom room) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isSpectator(uuid)) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }

        if (onlinePlayers.isEmpty()) {
            return;
        }

        Player luckyPlayer = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
        ItemStack randomCompass = createRandomCompassItem();

        if (luckyPlayer.getInventory().firstEmpty() != -1) {
            luckyPlayer.getInventory().addItem(randomCompass);
        } else {
            luckyPlayer.getWorld().dropItemNaturally(luckyPlayer.getLocation(), randomCompass);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", luckyPlayer.getName());

        room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.random_compass_given", placeholders));
        luckyPlayer.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.random_compass_received"));

        luckyPlayer.playSound(luckyPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
        luckyPlayer.playSound(luckyPlayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.2f);
    }

    private ItemStack createRandomCompassItem() {
        ItemStack compass = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§A§A§5§5🧭 §x§F§F§C§C§7§7幽§x§F§F§E§E§9§9匿§x§D§D§F§F§9§9指§x§B§B§F§F§7§7南§x§9§9§F§F§5§5针");
            meta.setCustomModelData(10009);
            meta.setItemModel(NamespacedKey.minecraft("recovery_compass"));

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e长按右键 2.5 秒吃掉它");
            lore.add("§f- §7吃的时候会获得 §8缓慢II §7和 §0黑暗");
            lore.add("§f- §a敌方获得 §e发光15秒");
            lore.add("§f- §7敌方获得 §8缓慢I 10秒");
            lore.add("§f- §6敌方获得 §e饥饿I 15秒");
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §d随机指南针模式专属道具");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }

        compass.setData(DataComponentTypes.FOOD, FoodProperties.food()
                .nutrition(0)
                .saturation(0.0f)
                .canAlwaysEat(true));
        compass.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(2.5f)
                .animation(ItemUseAnimation.EAT)
                .sound(Key.key("minecraft", "entity.generic.eat"))
                .hasConsumeParticles(true));
        return compass;
    }

    private boolean isRandomCompassItem(ItemStack item) {
        if (item == null || item.getType() != Material.RECOVERY_COMPASS || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 10009;
    }

    private void cleanupRandomCompassMode(String roomId) {
        randomCompassCountdowns.remove(roomId);

        BossBar bossBar = randomCompassBossBars.remove(roomId);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    private void startSurvivalMode(GameRoom room) {
        cleanupSurvivalMode(room.getRoomId());

        BossBar bossBar = Bukkit.createBossBar(
                buildSurvivalBossBarTitle(plugin.getConfigManager().getConfig().getInt("hunter_game.survival_time", 30) * 60),
                BarColor.BLUE,
                BarStyle.SEGMENTED_10
        );
        bossBar.setVisible(true);
        survivalBossBars.put(room.getRoomId(), bossBar);
        refreshSurvivalBossBarPlayers(room);
        updateSurvivalBossBar(room);
    }

    private void updateSurvivalBossBar(GameRoom room) {
        BossBar bossBar = survivalBossBars.get(room.getRoomId());
        if (bossBar == null) {
            startSurvivalMode(room);
            return;
        }

        refreshSurvivalBossBarPlayers(room);

        int totalSeconds = plugin.getConfigManager().getConfig().getInt("hunter_game.survival_time", 30) * 60;
        int elapsedSeconds = (int) (room.getGameDuration() / 1000L);
        int remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
        bossBar.setTitle(buildSurvivalBossBarTitle(remainingSeconds));
        bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, remainingSeconds / (double) totalSeconds)));

        if (remainingSeconds <= 300) {
            bossBar.setColor(BarColor.RED);
        } else if (remainingSeconds <= 900) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.BLUE);
        }
    }

    private String buildSurvivalBossBarTitle(int seconds) {
        return "§x§5§5§F§F§D§D⏳ §x§7§7§F§F§E§E存§x§9§9§F§F§F§F活§x§B§B§E§E§F§F倒§x§D§D§D§D§F§F计§x§F§F§C§C§F§F时 §f剩余 §e" + formatBossBarTime(seconds);
    }

    private void refreshSurvivalBossBarPlayers(GameRoom room) {
        BossBar bossBar = survivalBossBars.get(room.getRoomId());
        if (bossBar == null) return;

        Set<Player> viewers = new HashSet<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                viewers.add(player);
            }
        }
        for (UUID uuid : room.getSpectators()) {
            Player spectator = Bukkit.getPlayer(uuid);
            if (spectator != null && spectator.isOnline()) {
                viewers.add(spectator);
            }
        }

        for (Player existing : new ArrayList<>(bossBar.getPlayers())) {
            if (!viewers.contains(existing)) {
                bossBar.removePlayer(existing);
            }
        }

        for (Player viewer : viewers) {
            if (!bossBar.getPlayers().contains(viewer)) {
                bossBar.addPlayer(viewer);
            }
        }
    }

    private void cleanupSurvivalMode(String roomId) {
        BossBar bossBar = survivalBossBars.remove(roomId);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    private void updateRewardChestCompass(GameRoom room) {
        if (!room.hasModifier("RewardChest") || room.getRewardChestLocation() == null || room.isRewardChestOpened()) {
            return;
        }

        for (UUID preyUuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null || !prey.isOnline()) continue;

            ItemStack mainHand = prey.getInventory().getItemInMainHand();
            ItemStack offHand = prey.getInventory().getItemInOffHand();
            if (isRewardChestCompass(mainHand) || isRewardChestCompass(offHand)) {
                prey.setCompassTarget(room.getRewardChestLocation());
                double distance = prey.getLocation().distance(room.getRewardChestLocation());
                prey.sendActionBar("§x§F§F§D§7§0§0🎁 §e奖励箱距离: §b" + String.format("%.1f", distance) + " §e格");
            }
        }
    }

    private void spawnRewardChest(GameRoom room) {
        if (room.getGameWorld() == null || room.getRewardChestLocation() != null) {
            return;
        }

        World world = room.getGameWorld();
        Location spawn = world.getSpawnLocation();
        Random random = new Random();
        Location chestLocation = null;

        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = 50 + random.nextInt(51);
            int x = spawn.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = spawn.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);

            Material ground = world.getBlockAt(x, Math.max(world.getMinHeight(), y - 1), z).getType();
            if (!ground.isSolid() || ground == Material.WATER || ground == Material.LAVA) {
                continue;
            }

            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (candidate.getBlock().getType().isAir() || candidate.getBlock().isPassable()) {
                chestLocation = candidate;
                break;
            }
        }

        if (chestLocation == null) {
            chestLocation = spawn.clone().add(8, 0, 8);
            chestLocation.setY(world.getHighestBlockYAt(chestLocation.getBlockX(), chestLocation.getBlockZ()));
        }

        chestLocation.getBlock().setType(Material.CHEST);
        if (chestLocation.getBlock().getState() instanceof Chest chest) {
            fillRewardChest(chest.getBlockInventory());
            chest.update();
        }

        room.setRewardChestLocation(chestLocation.clone());
        room.setRewardChestOpened(false);
    }

    private void fillRewardChest(org.bukkit.inventory.Inventory inventory) {
        inventory.clear();
        FileConfiguration rewardsConfig = plugin.getConfigManager().getConfig("rewards");
        if (rewardsConfig != null) {
            List<Map<?, ?>> items = rewardsConfig.getMapList("reward_chest_items");
            Random random = new Random();
            for (Map<?, ?> itemMap : items) {
                Object materialValue = itemMap.containsKey("material") ? itemMap.get("material") : "STONE";
                String materialName = String.valueOf(materialValue);
                Material material = Material.matchMaterial(materialName);
                if (material == null) continue;
                String upperName = material.name();
                if (upperName.endsWith("_SWORD") || upperName.endsWith("_AXE") || upperName.endsWith("_BOW")) {
                    continue;
                }

                int amount = parseInt(itemMap.get("amount"), 1);
                double chance = parseDouble(itemMap.get("chance"), 1.0D);
                if (random.nextDouble() <= chance) {
                    inventory.addItem(new ItemStack(material, Math.max(1, amount)));
                }
            }
        }

        inventory.addItem(new ItemStack(selectRewardWeapon(), 1));
    }

    private Material selectRewardWeapon() {
        int roll = new Random().nextInt(110);
        if (roll < 80) return Material.WOODEN_SWORD;
        if (roll < 100) return Material.STONE_AXE;
        if (roll < 108) return Material.IRON_SWORD;
        return Material.IRON_AXE;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void giveRewardChestCompass(GameRoom room) {
        for (UUID preyUuid : room.getPreyUUIDs()) {
            Player prey = Bukkit.getPlayer(preyUuid);
            if (prey == null || !prey.isOnline()) continue;
            prey.getInventory().addItem(createRewardChestCompass());
            prey.playSound(prey.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
    }

    private ItemStack createRewardChestCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§F§F§D§7§0§0🎁 §x§F§F§B§B§3§3奖§x§F§F§9§9§6§6励§x§F§F§7§7§9§9箱§x§F§F§5§5§C§C指§x§F§F§3§3§F§F南针");
            meta.setCustomModelData(10010);
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e会持续指向奖励箱");
            lore.add("§f- §a奖励箱生成在路地上");
            lore.add("§f- §b里面有矿物和随机武器");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    private boolean isRewardChestCompass(ItemStack item) {
        return item != null
                && item.getType() == Material.COMPASS
                && item.hasItemMeta()
                && item.getItemMeta().hasCustomModelData()
                && item.getItemMeta().getCustomModelData() == 10010;
    }

    private void applyThunderStormWeather(GameRoom room) {
        if (room == null || !thunderStormAppliedRooms.add(room.getRoomId())) {
            return;
        }

        // 雷魔修饰符只控制当前房间的主游戏世界，不主动创建/修改下界、末地或其他房间世界，避免天气包刷屏和影响全服世界。
        applyStorm(room.getGameWorld(), false);
    }

    private void applyStorm(World world, boolean lockWeatherCycle) {
        if (world == null) return;
        if (lockWeatherCycle) {
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }
        if (!world.hasStorm()) {
            world.setStorm(true);
        }
        if (!world.isThundering()) {
            world.setThundering(true);
        }
        world.setWeatherDuration(FLASH_STORM_DURATION_TICKS);
        world.setThunderDuration(FLASH_STORM_DURATION_TICKS);
    }

    private void cleanupThunderStormWeather(GameRoom room) {
        if (room == null || !thunderStormAppliedRooms.remove(room.getRoomId())) {
            return;
        }

        clearStorm(room.getGameWorld());
        plugin.getWorldManager().keepLobbyWeatherClear();
    }

    private void clearStorm(World world) {
        if (world == null) return;
        if (world.hasStorm()) {
            world.setStorm(false);
        }
        if (world.isThundering()) {
            world.setThundering(false);
        }
        world.setWeatherDuration(0);
        world.setThunderDuration(0);
    }

    private void updateThunderStormEffects(GameRoom room) {
        if (!room.hasModifier("ThunderStorm") || !room.isGameActuallyStarted()) {
            return;
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            World world = player.getWorld();
            if (!world.hasStorm()) continue;
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            if (!player.getLocation().getBlock().getBiome().toString().contains("DESERT")) {
                if (world.getHighestBlockYAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ()) <= player.getLocation().getBlockY()
                        && !player.getLocation().getBlock().isLiquid()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true, true));
                }
            }
        }
    }

    private int getHunterCount(GameRoom room) {
        int hunterCount = 0;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (room.isHunter(uuid)) {
                hunterCount++;
            }
        }
        return hunterCount;
    }

    private void startSwapMode(GameRoom room) {
        if (room.getActiveSwapPrey() == null || room.getCountdownSwapPrey() == null) {
            initializeSwapPreys(room);
        }

        UUID activeUuid = room.getActiveSwapPrey();
        UUID countdownUuid = room.getCountdownSwapPrey();
        if (activeUuid == null || countdownUuid == null) {
            return;
        }

        Player activePrey = Bukkit.getPlayer(activeUuid);
        Player countdownPrey = Bukkit.getPlayer(countdownUuid);

        if (activePrey != null) {
            releaseSwapActiveState(activePrey);
        }
        if (countdownPrey != null) {
            applySwapCountdownState(countdownPrey, room);
        }

        room.setSwapCountdownSeconds(SWAP_INTERVAL_SECONDS);
    }

    private void updateSwapMode(GameRoom room) {
        UUID activeUuid = room.getActiveSwapPrey();
        UUID countdownUuid = room.getCountdownSwapPrey();
        if (activeUuid == null || countdownUuid == null) {
            startSwapMode(room);
            return;
        }

        Player countdownPrey = Bukkit.getPlayer(countdownUuid);
        if (countdownPrey == null || !countdownPrey.isOnline()) {
            return;
        }
        keepSwapCountdownState(countdownPrey, room);

        int timeLeft = room.getSwapCountdownSeconds();
        if (timeLeft <= 0) {
            swapActivePrey(room);
            return;
        }

        countdownPrey.setLevel(timeLeft);
        countdownPrey.setExp(Math.max(0.0f, Math.min(1.0f, timeLeft / (float) SWAP_INTERVAL_SECONDS)));

        String activeName = Optional.ofNullable(Bukkit.getPlayer(activeUuid)).map(Player::getName).orElse("未知猎物");
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§x§F§F§D§7§0§0⌛ §e" + timeLeft + " §6秒");
        Component subComp = LegacyComponentSerializer.legacySection().deserialize("§7倒计时结束后接管 §a" + activeName + " §7的状态");
        countdownPrey.showTitle(Title.title(titleComp, subComp,
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))));
        countdownPrey.sendActionBar("§7你正在待命，剩余 §e" + timeLeft + " §7秒接管猎物状态");

        room.setSwapCountdownSeconds(timeLeft - 1);
    }

    private void keepSwapCountdownState(Player player, GameRoom room) {
        if (player == null || room == null || room.getGameWorld() == null) {
            return;
        }
        Location holding = getSwapHoldingLocation(room);
        if (!player.getWorld().equals(holding.getWorld()) || player.getLocation().distanceSquared(holding) > 2.25D) {
            player.teleport(holding);
        }
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGravity(false);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
        }
        if (!player.hasPotionEffect(PotionEffectType.DARKNESS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false, false));
        }
        if (!player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false, false));
        }
    }

    private void swapActivePrey(GameRoom room) {
        UUID activeUuid = room.getActiveSwapPrey();
        UUID countdownUuid = room.getCountdownSwapPrey();
        if (activeUuid == null || countdownUuid == null) {
            return;
        }

        Player activePrey = Bukkit.getPlayer(activeUuid);
        Player countdownPrey = Bukkit.getPlayer(countdownUuid);
        if (activePrey == null || countdownPrey == null) {
            return;
        }

        copyPlayerState(activePrey, countdownPrey);
        releaseSwapActiveState(countdownPrey);
        applySwapCountdownState(activePrey, room);

        room.setActiveSwapPrey(countdownUuid);
        room.setCountdownSwapPrey(activeUuid);
        room.setSwapCountdownSeconds(SWAP_INTERVAL_SECONDS);
        updateCompassTracking(room);

        String activeName = countdownPrey.getName();
        room.broadcast(plugin.getConfigManager().getHunterGamePrefix() + "§x§D§D§5§5§F§F🔄 §b互换完成！当前接力猎物：§a" + activeName);
        countdownPrey.playSound(countdownPrey.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        activePrey.playSound(activePrey.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.2f);
    }

    private void applySwapCountdownState(Player player, GameRoom room) {
        if (player == null || room.getGameWorld() == null) {
            return;
        }

        player.getInventory().clear();
        // 互换模式待命猎物不能使用旁观模式，否则会打开原版旁观传送物品栏。
        // 改为生存下的隐身定点待命：玩家看不见、动不了、不会掉落，但也不会获得旁观传送能力。
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGravity(false);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
        player.teleport(getSwapHoldingLocation(room));
    }

    private void releaseSwapActiveState(Player player) {
        if (player == null) {
            return;
        }

        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGravity(true);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.resetCooldown();
    }

    private Location getSwapHoldingLocation(GameRoom room) {
        World world = room.getGameWorld();
        Location base = world.getSpawnLocation().clone().add(0, 180, 0);
        double maxY = world.getMaxHeight() - 5;
        if (base.getY() > maxY) {
            base.setY(maxY);
        }
        return base;
    }

    private void copyPlayerState(Player from, Player to) {
        ItemStack[] contents = from.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                copy[i] = contents[i].clone();
            }
        }

        to.getInventory().clear();
        to.getInventory().setContents(copy);
        to.teleport(from.getLocation().clone());
        to.setVelocity(from.getVelocity().clone());
        to.setHealth(Math.min(from.getHealth(), to.getMaxHealth()));
        to.setFoodLevel(from.getFoodLevel());
        to.setSaturation(from.getSaturation());
        to.setLevel(from.getLevel());
        to.setExp(from.getExp());
        to.setFireTicks(from.getFireTicks());
        to.setRemainingAir(from.getRemainingAir());
        to.setFallDistance(from.getFallDistance());

        for (PotionEffect effect : new ArrayList<>(to.getActivePotionEffects())) {
            to.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : from.getActivePotionEffects()) {
            to.addPotionEffect(effect);
        }

        copyAdvancements(from, to);
    }

    private void copyAdvancements(Player from, Player to) {
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress fromProgress = from.getAdvancementProgress(advancement);
            AdvancementProgress toProgress = to.getAdvancementProgress(advancement);

            Set<String> awardedCriteria = new HashSet<>(fromProgress.getAwardedCriteria());
            for (String criterion : new HashSet<>(toProgress.getAwardedCriteria())) {
                if (!awardedCriteria.contains(criterion)) {
                    toProgress.revokeCriteria(criterion);
                }
            }
            for (String criterion : awardedCriteria) {
                if (!toProgress.getAwardedCriteria().contains(criterion)) {
                    toProgress.awardCriteria(criterion);
                }
            }
        }
    }

    private void updateCompassTracking(GameRoom room) {
        boolean tournamentCompass = room.getGameMode().isFlashTournament();
        for (UUID hunterUuid : room.getAllPlayerUUIDs()) {
            if (!room.isHunter(hunterUuid)) continue;

            Player hunter = Bukkit.getPlayer(hunterUuid);
            if (hunter == null) continue;
            if (room.getGameMode() == GameMode.END_FLASH && plugin.getPlayerListener().isEndFlashHunterRespawnWaiting(hunterUuid)) {
                continue;
            }

            ItemStack mainHand = hunter.getInventory().getItemInMainHand();
            ItemStack offHand = hunter.getInventory().getItemInOffHand();

            if (isTrackingCompass(mainHand) || isTrackingCompass(offHand)) {
                // 找到最近的猎物
                Player nearestPrey = null;
                boolean hasDifferentDimensionPrey = false;
                double nearestDistance = Double.MAX_VALUE;

                for (UUID preyUuid : getTrackablePreys(room)) {
                    Player prey = Bukkit.getPlayer(preyUuid);
                    if (prey != null && prey.getWorld().equals(hunter.getWorld())) {
                        double distance = calculateTrackingDistance(room, hunter.getLocation(), prey.getLocation());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestPrey = prey;
                        }
                    } else if (prey != null && prey.isOnline()) {
                        hasDifferentDimensionPrey = true;
                    }
                }

                if (nearestPrey != null) {
                    double displayDistance = calculateTrackingDistance(room, hunter.getLocation(), nearestPrey.getLocation());
                    Location target = nearestPrey.getLocation().clone();

                    if (!tournamentCompass) {
                        String cooldownText = plugin.getPlayerListener().getCompassTpRemainingDisplay(hunter.getUniqueId());
                        hunter.sendActionBar("§e🧭 距离最近猎物: §b" + String.format("%.1f", displayDistance) + " §e格"
                                + " §8| §d传送: §f" + cooldownText);
                    }
                    updateHeldTrackingCompasses(hunter, mainHand, offHand, target, tournamentCompass);
                } else if (hasDifferentDimensionPrey) {
                    makeHeldTrackingCompassesSpin(hunter, mainHand, offHand);
                } else {
                    clearHeldTrackingCompasses(hunter, mainHand, offHand);
                }
            }
        }

        if (!tournamentCompass && plugin.getFlashModeManager().isFlashMode(room) && room.getPreyUUIDs().size() >= 2) {
            for (UUID preyUuid : room.getPreyUUIDs()) {
                Player preyPlayer = Bukkit.getPlayer(preyUuid);
                if (preyPlayer == null || room.isSpectator(preyUuid)) {
                    continue;
                }
                ItemStack mainHand = preyPlayer.getInventory().getItemInMainHand();
                ItemStack offHand = preyPlayer.getInventory().getItemInOffHand();
                if (!isTrackingCompass(mainHand) && !isTrackingCompass(offHand)) {
                    continue;
                }
                Player nearestTeammate = null;
                double nearestDistance = Double.MAX_VALUE;
                for (UUID otherPreyUuid : room.getPreyUUIDs()) {
                    if (otherPreyUuid.equals(preyUuid) || room.isSpectator(otherPreyUuid)) {
                        continue;
                    }
                    Player other = Bukkit.getPlayer(otherPreyUuid);
                    if (other != null && other.getWorld().equals(preyPlayer.getWorld())) {
                        double distance = calculateTrackingDistance(room, preyPlayer.getLocation(), other.getLocation());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestTeammate = other;
                        }
                    }
                }
                if (nearestTeammate != null) {
                    double displayDistance = calculateTrackingDistance(room, preyPlayer.getLocation(), nearestTeammate.getLocation());
                    Location target = nearestTeammate.getLocation().clone();
                    String cooldownText = plugin.getPlayerListener().getCompassTpRemainingDisplay(preyUuid);
                    preyPlayer.sendActionBar("§x§F§F§A§A§D§D🧭 最近猎物队友: §b" + String.format("%.1f", displayDistance) + " §e格"
                            + " §8| §d传送: §f" + cooldownText);
                    updateHeldTrackingCompasses(preyPlayer, mainHand, offHand, target, false);
                } else {
                    makeHeldTrackingCompassesSpin(preyPlayer, mainHand, offHand);
                }
            }
        }
    }

    private boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null || !meta.hasCustomModelData();
    }

    private void updateHeldTrackingCompasses(Player player, ItemStack mainHand, ItemStack offHand, Location target, boolean tournamentCompass) {
        if (player == null || target == null || target.getWorld() == null) {
            return;
        }
        if (player.getWorld().equals(target.getWorld())) {
            player.setCompassTarget(target);
        }
        updateTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.HAND, target, false, tournamentCompass);
        updateTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.OFF_HAND, target, false, tournamentCompass);
    }

    private void makeHeldTrackingCompassesSpin(Player player, ItemStack mainHand, ItemStack offHand) {
        if (player == null) {
            return;
        }
        updateTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.HAND, null, true, false);
        updateTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.OFF_HAND, null, true, false);
    }

    private void clearHeldTrackingCompasses(Player player, ItemStack mainHand, ItemStack offHand) {
        if (player == null) {
            return;
        }
        clearTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.HAND);
        clearTrackingCompassInHand(player, org.bukkit.inventory.EquipmentSlot.OFF_HAND);
    }

    private void updateTrackingCompassInHand(Player player, org.bukkit.inventory.EquipmentSlot hand, Location target, boolean spin, boolean tournamentCompass) {
        if (player == null || hand == null) {
            return;
        }
        ItemStack compass = hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!isTrackingCompass(compass)) {
            return;
        }
        ItemStack updated = compass.clone();
        if (spin) {
            updateTrackingCompassSpinMeta(updated, player);
        } else if (tournamentCompass) {
            // 赛事指南针只用玩家个人 compass target，不写每秒变化的 lodestone NBT，避免近距离追踪时前后跳舞。
            clearTrackingCompassMeta(updated);
        } else {
            updateTrackingCompassMeta(updated, target);
        }
        if (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(updated);
        } else {
            player.getInventory().setItemInMainHand(updated);
        }
    }

    private void clearTrackingCompassInHand(Player player, org.bukkit.inventory.EquipmentSlot hand) {
        if (player == null || hand == null) {
            return;
        }
        ItemStack compass = hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!isTrackingCompass(compass)) {
            return;
        }
        ItemStack updated = compass.clone();
        clearTrackingCompassMeta(updated);
        if (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(updated);
        } else {
            player.getInventory().setItemInMainHand(updated);
        }
    }

    private void updateTrackingCompassMeta(ItemStack compass, Location target) {
        if (!isTrackingCompass(compass) || target == null || target.getWorld() == null) {
            return;
        }
        ItemMeta meta = compass.getItemMeta();
        if (meta instanceof CompassMeta compassMeta) {
            Location lodestoneTarget = target.clone();
            lodestoneTarget.setX(lodestoneTarget.getBlockX() + 0.5D);
            lodestoneTarget.setY(lodestoneTarget.getBlockY());
            lodestoneTarget.setZ(lodestoneTarget.getBlockZ() + 0.5D);
            compassMeta.setLodestone(lodestoneTarget);
            compassMeta.setLodestoneTracked(false);
            compass.setItemMeta(compassMeta);
        }
    }

    private void updateTrackingCompassSpinMeta(ItemStack compass, Player player) {
        if (!isTrackingCompass(compass) || player == null || player.getWorld() == null) {
            return;
        }
        ItemMeta meta = compass.getItemMeta();
        if (meta instanceof CompassMeta compassMeta) {
            Location lostTarget = findLostCompassTarget(player);
            compassMeta.setLodestone(lostTarget);
            compassMeta.setLodestoneTracked(true);
            compass.setItemMeta(compassMeta);
        }
    }

    private Location findLostCompassTarget(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation();
        int baseX = base.getBlockX();
        int baseY = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, base.getBlockY() + 2));
        int baseZ = base.getBlockZ();
        for (int dy = 0; dy <= 3; dy++) {
            int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, baseY + dy));
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Location candidate = new Location(world, baseX + dx + 0.5D, y, baseZ + dz + 0.5D);
                    if (candidate.getBlock().getType() != Material.LODESTONE) {
                        return candidate;
                    }
                }
            }
        }
        return new Location(world, baseX + 0.5D, baseY, baseZ + 0.5D);
    }

    private void clearTrackingCompassMeta(ItemStack compass) {
        if (!isTrackingCompass(compass)) {
            return;
        }
        ItemMeta meta = compass.getItemMeta();
        if (meta instanceof CompassMeta compassMeta) {
            compassMeta.setLodestone(null);
            compassMeta.setLodestoneTracked(false);
            compass.setItemMeta(compassMeta);
        }
    }

    private double calculateTrackingDistance(GameRoom room, Location hunterLoc, Location preyLoc) {
        if (room.hasModifier("IncludeY")) {
            return hunterLoc.distance(preyLoc);
        }
        return Math.sqrt(
                Math.pow(hunterLoc.getX() - preyLoc.getX(), 2) +
                Math.pow(hunterLoc.getZ() - preyLoc.getZ(), 2)
        );
    }

    public void endGame(GameRoom room, boolean preyWin) {
        if (room == null || room.getState() == RoomState.ENDED) {
            return;
        }
        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        room.clearFlashTournamentStartLocations();
        room.setState(RoomState.ENDED);
        room.setPreyWon(preyWin);
        setTournamentAdvancementAnnouncements(room, false);
        cleanupRandomCompassMode(room.getRoomId());
        cleanupSurvivalMode(room.getRoomId());
        cleanupThunderStormWeather(room);
        plugin.getPlayerListener().resetCompassTpState(room);
        plugin.getFlashModeManager().cleanupFlashRoomBackpacks(room);

        // 取消游戏任务
        BukkitTask gameTask = gameTasks.remove(room.getRoomId());
        if (gameTask != null) {
            gameTask.cancel();
        }

        // 发送结束消息
        if (!isTournamentSilent(room)) {
            String resultKey = preyWin ? "game.prey_win" : "game.hunter_win";
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix(resultKey));
        }

        // 显示游戏结束标题和音效
        showGameEndTitle(room, preyWin);

        // 发放奖励
        if (!isTournamentSilent(room)) {
            giveRewards(room, preyWin);
        }

        // 发放积分奖励
        if (!isTournamentSilent(room)) {
            givePoints(room, preyWin);
        }

        // 记录数据
        recordGameData(room, preyWin);

        // 显示本局排行榜
        if (!isTournamentSilent(room)) {
            showEndGameLeaderboard(room, preyWin);
        }

        // 猎物胜利时全部人变旁观模式，猎人胜利时只有猎物变旁观
        if (preyWin) {
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                }
            }
        } else {
            // 只有猎物变为旁观模式，并给予向前上方的动量
            for (UUID uuid : room.getPreyUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    org.bukkit.util.Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(1.0);
                    player.setVelocity(velocity);

                    safePlayHunterVictoryEffect(room, player.getLocation());
                }
            }
        }

        // 猎物胜利20秒，猎人胜利10秒
        int closingTime = preyWin ? 20 : 10;

        new BukkitRunnable() {
            int countdown = closingTime;

            @Override
            public void run() {
                if (countdown <= 0) {
                    if (plugin.getChildServerManager().isManagedNodeRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnManagedRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }

                    if (plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnCrossServerRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        cancel();
                        return;
                    }

                    // 踢出所有玩家并恢复状态
                    Set<UUID> restoreTargets = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    restoreTargets.addAll(room.getSpectators());
                    for (UUID uuid : restoreTargets) {
                        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
                    }

                    // 清除头顶职业前缀
                    plugin.getRoomManager().clearAllRoleNameTags(room);
                    // 删除房间
                    plugin.getRoomManager().deleteRoom(room.getRoomId());
                    cancel();
                    return;
                }

                // 显示倒计时
                if (isTournamentSilent(room)) {
                    countdown--;
                    return;
                }

                if (countdown <= 5) {
                    // 5秒及以下时显示倒计时
                    Set<UUID> closeRecipients = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    closeRecipients.addAll(room.getSpectators());
                    for (UUID uuid : closeRecipients) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage("§x§F§F§5§5§5§5⏱ §c" + countdown + " §7秒后关闭...");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endGameWithoutReward(GameRoom room) {
        if (room == null || room.getState() == RoomState.ENDED) {
            return;
        }
        cancelDualPreyDecisionTask(room);
        room.clearDualPreyProposal();
        room.clearDualPreyStack();
        room.setState(RoomState.ENDED);
        room.setPreyWon(!room.isPreyQuit());
        setTournamentAdvancementAnnouncements(room, false);
        cleanupRandomCompassMode(room.getRoomId());
        cleanupSurvivalMode(room.getRoomId());
        cleanupThunderStormWeather(room);
        plugin.getPlayerListener().resetCompassTpState(room);
        plugin.getFlashModeManager().cleanupFlashRoomBackpacks(room);

        // 取消游戏任务
        BukkitTask gameTask = gameTasks.remove(room.getRoomId());
        if (gameTask != null) {
            gameTask.cancel();
        }

        // 发送结束消息
        if (!isTournamentSilent(room)) {
            if (room.getGameMode().isLuckyPillars()) {
                room.broadcast(plugin.getMessageManager().getMessage("game.ended_no_reward"));
            } else if (room.getGameMode().isStandaloneMiniGame() || room.getGameMode().isIndependentMode()) {
                room.broadcast(plugin.getMessageManager().getMessage("game.ended_no_reward"));
            } else {
                room.broadcast(plugin.getMessageManager().getMessage("game.ended_no_reward"));
            }
        } else {
            showTournamentVictoryTitle(room);
        }

        // 不发放奖励，不记录数据

        // 将所有玩家变为旁观模式，并给予向前上方的动量
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);

                // 给予向前上方的动量
                org.bukkit.util.Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(1.0);
                player.setVelocity(velocity);
            }
        }

        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);

                // 给予向前上方的动量
                org.bukkit.util.Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(1.0);
                player.setVelocity(velocity);
            }
        }

        // 10秒倒计时
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    if (plugin.getChildServerManager().isManagedNodeRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnManagedRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        plugin.getChildServerManager().scheduleNodeShutdown();
                        cancel();
                        return;
                    }

                    if (plugin.getChildServerManager().isManagedCrossServerRoom(room.getRoomId())) {
                        plugin.getChildServerManager().returnCrossServerRoomPlayersToLobby(room);
                        plugin.getRoomManager().clearAllRoleNameTags(room);
                        plugin.getRoomManager().deleteRoom(room.getRoomId());
                        cancel();
                        return;
                    }

                    // 踢出所有玩家并恢复状态
                    Set<UUID> restoreTargets = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    restoreTargets.addAll(room.getSpectators());
                    for (UUID uuid : restoreTargets) {
                        plugin.getRoomManager().restorePlayerAfterRoom(room, uuid, true);
                    }

                    // 清除头顶职业前缀
                    plugin.getRoomManager().clearAllRoleNameTags(room);
                    // 删除房间
                    plugin.getRoomManager().deleteRoom(room.getRoomId());
                    cancel();
                    return;
                }

                // 显示倒计时
                if (isTournamentSilent(room)) {
                    countdown--;
                    return;
                }

                if (countdown <= 5) {
                    // 5秒及以下时显示倒计时
                    Set<UUID> closeRecipients = new LinkedHashSet<>(room.getAllPlayerUUIDs());
                    closeRecipients.addAll(room.getSpectators());
                    for (UUID uuid : closeRecipients) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage("§x§F§F§5§5§5§5⏱ §c" + countdown + " §7秒后关闭...");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void showEndGameLeaderboard(GameRoom room, boolean preyWin) {
        if (room.getGameMode() == GameMode.NO_ITEM) {
            return;
        }

        // 收集所有玩家
        List<UUID> allPlayers = new ArrayList<>(room.getAllPlayerUUIDs());

        // 分离猎物和猎人
        List<UUID> preyList = new ArrayList<>();
        List<UUID> hunterList = new ArrayList<>();
        for (UUID uuid : allPlayers) {
            if (room.isPrey(uuid)) {
                preyList.add(uuid);
            } else {
                hunterList.add(uuid);
            }
        }

        // 猎人按贡献值排序
        hunterList.sort((a, b) -> Double.compare(room.getContribution(b), room.getContribution(a)));
        // 猎物按贡献值排序（多猎物时）
        preyList.sort((a, b) -> Double.compare(room.getContribution(b), room.getContribution(a)));

        // 猎物赢了，猎物排在最前面；猎人赢了，猎人排在最前面
        List<UUID> ranked = new ArrayList<>();
        if (preyWin) {
            ranked.addAll(preyList);
            ranked.addAll(hunterList);
        } else {
            ranked.addAll(hunterList);
            ranked.addAll(preyList);
        }

        // 限制前8名
        if (ranked.size() > 8) {
            ranked = ranked.subList(0, 8);
        }

        // 构建排行榜消息（从messages读取）
        StringBuilder sb = new StringBuilder();
        sb.append(plugin.getMessageManager().getMessage("game.leaderboard_header"));

        String[] rankIcons = {"§x§F§F§D§7§0§0🥇", "§x§C§0§C§0§C§0🥈", "§x§C§D§7§F§3§2🥉"};

        for (int i = 0; i < ranked.size(); i++) {
            UUID uuid = ranked.get(i);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = "未知玩家";

            boolean isPrey = room.isPrey(uuid);
            String rankStr = i < 3 ? rankIcons[i] : "§7#" + (i + 1);

            int attacks = room.getAttackCount(uuid);
            double damage = room.getDamageDealt(uuid);
            double distance = room.getDistanceRun(uuid);

            Map<String, String> ph = new HashMap<>();
            ph.put("rank", rankStr);
            ph.put("name", name);
            ph.put("attacks", String.valueOf(attacks));
            ph.put("damage", String.format("%.1f", damage));
            ph.put("distance", String.format("%.0f", distance));

            String rowKey = isPrey ? "game.leaderboard_prey_row" : "game.leaderboard_hunter_row";
            sb.append(plugin.getMessageManager().getMessage(rowKey, ph)).append("\n");
        }

        sb.append(plugin.getMessageManager().getMessage("game.leaderboard_footer"));

        String leaderboard = sb.toString();
        room.broadcast(leaderboard);
    }

    private void giveRewards(GameRoom room, boolean preyWin) {
        // 自定义房间不给奖励（除非是管理员房间）
        if (room.isCustomRoom()) {
            Player owner = Bukkit.getPlayer(room.getOwnerUuid());
            if (owner == null || !owner.hasPermission("gamefunxiao.admin")) {
                return;
            }
        }

        GameMode mode = room.getGameMode();
        // 如果猎物完成任务，给予正常奖励；否则按config里的fail_reward发放
        int failReward = plugin.getConfigManager().getConfig().getInt("hunter_game.rewards.fail_reward", 25);
        int hunterReward = preyWin ? mode.getHunterReward() : failReward + 15;
        int preyReward = preyWin ? mode.getPreyReward() : failReward;

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            int reward = room.isPrey(uuid) ? preyReward : hunterReward;

            // 给予金币
            plugin.getPlayerDataManager().addCoins(uuid, reward);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(reward));
            placeholders.put("currency", plugin.getConfigManager().getMiniGameCurrencyName());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.reward_received", placeholders));
        }
    }

    private void givePoints(GameRoom room, boolean preyWin) {
        // 自定义房间不给积分（除非是管理员房间）
        if (room.isCustomRoom()) {
            Player owner = Bukkit.getPlayer(room.getOwnerUuid());
            if (owner == null || !owner.hasPermission("gamefunxiao.admin")) {
                return;
            }
        }

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            if (room.isPrey(uuid)) {
                if (preyWin) {
                    int preyPoints = room.getGameMode() == GameMode.SURVIVAL ? 10 : 25;
                    plugin.getPlayerDataManager().addPreyPoints(uuid, preyPoints, room.getGameMode());
                    Map<String, String> ph = new HashMap<>();
                    ph.put("points", String.valueOf(preyPoints));
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("points.prey_win", ph));
                }
            } else {
                // 猎人胜利 +1（但猎物退出不算）
                if (!preyWin && !room.isPreyQuit()) {
                    plugin.getPlayerDataManager().addHunterPoints(uuid, 1, room.getGameMode());
                    Map<String, String> ph = new HashMap<>();
                    ph.put("points", "1");
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("points.hunter_win", ph));
                }
            }
        }
    }

    private void recordGameData(GameRoom room, boolean preyWin) {
        // 自定义房间不记录（除非是管理员房间）
        if (room.isCustomRoom()) {
            Player owner = Bukkit.getPlayer(room.getOwnerUuid());
            if (owner == null || !owner.hasPermission("gamefunxiao.admin")) {
                return;
            }
        }

        long gameTime = room.getGameDuration();
        boolean recordFastestTime = preyWin && shouldRecordFastestTime(room);

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            plugin.getPlayerDataManager().incrementPlayCount(uuid, room.getGameMode());

            if (room.isPrey(uuid)) {
                if (preyWin) {
                    plugin.getPlayerDataManager().incrementPreyWins(uuid, room.getGameMode());
                    if (recordFastestTime) {
                        plugin.getPlayerDataManager().updateFastestTime(uuid, gameTime, room.getGameMode());
                    }
                }
            } else {
                if (!preyWin) {
                    plugin.getPlayerDataManager().incrementHunterWins(uuid, room.getGameMode());
                }
            }
        }
    }

    private boolean shouldRecordFastestTime(GameRoom room) {
        if (room == null || room.getGameMode().isLuckyPillars() || room.getGameMode().isIndependentMode()) {
            return false;
        }
        if (room.getGameMode() == GameMode.END_FLASH) {
            return room.isDoublePreyEnabled();
        }
        return true;
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "分" + secs + "秒";
        }
        return secs + "秒";
    }

    // 显示游戏结束标题和音效
    private void showGameEndTitle(GameRoom room, boolean preyWin) {
        if (isTournamentSilent(room)) {
            showTournamentVictoryTitle(room);
            return;
        }

        String title;
        String subtitle;
        Sound sound;

        if (preyWin) {
            // 猎物胜利 - 显示通关时间
            long gameTime = room.getGameDuration();
            String timeStr = formatGameTime(gameTime);

            title = "§e§l恭喜通关！";
            subtitle = "§f最终时间 §e(" + timeStr + ")";
            sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
        } else {
            // 猎人胜利
            title = "§x§F§F§5§5§5§5💀 §c§l游戏结束";
            subtitle = "§x§F§F§7§7§7§7猎人击杀了猎物！";
            sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
        }

        // 给所有玩家（包括旁观者）显示标题和音效，显示10秒（200 ticks）
        Component endTitleComp = LegacyComponentSerializer.legacySection().deserialize(title);
        Component endSubComp = LegacyComponentSerializer.legacySection().deserialize(subtitle);
        Title endTitle = Title.title(endTitleComp, endSubComp,
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(10000), Duration.ofMillis(1000)));

        for (UUID uuid : room.getAllPlayerUUIDs()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!isTournamentSilent(room)) {
                    player.showTitle(endTitle);
                }
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }

        for (UUID uuid : room.getSpectators()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!isTournamentSilent(room)) {
                    player.showTitle(endTitle);
                }
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }

        // 猎物胜利时，在物品栏上方显示通关时间
        if (preyWin) {
            long gameTime = room.getGameDuration();
            String timeStr = formatGameTime(gameTime);
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendActionBar("§e(" + timeStr + ")");
                }
            }
        }
    }

    private void showTournamentVictoryTitle(GameRoom room) {
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize("§e§l胜利");
        Component subComp = LegacyComponentSerializer.legacySection().deserialize("");
        Title title = Title.title(titleComp, subComp,
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(5000), Duration.ofMillis(700)));

        Set<UUID> receivers = new HashSet<>();
        receivers.addAll(room.getAllPlayerUUIDs());
        receivers.addAll(room.getSpectators());
        for (UUID uuid : receivers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.75f, 1.18f);
            }
        }
    }

    // 格式化游戏时间（毫秒转换为 "02:05:20:359" 格式）
    private String formatGameTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        long ms = millis % 1000;
        return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, ms);
    }

    public void shutdown() {
        // 取消所有任务
        for (BukkitTask task : countdownTasks.values()) {
            task.cancel();
        }
        for (BukkitTask task : gameTasks.values()) {
            task.cancel();
        }
        for (BukkitTask task : preGameCountdownTasks.values()) {
            task.cancel();
        }
        countdownTasks.clear();
        gameTasks.clear();
        preGameCountdownTasks.clear();

        for (BossBar bossBar : randomCompassBossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        randomCompassBossBars.clear();
        randomCompassCountdowns.clear();

        for (BossBar bossBar : survivalBossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        survivalBossBars.clear();
    }

    // 获取安全的出生位置（避免卡在方块里）
    private Location getSafeSpawnLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return location;

        Location safeLoc = location.clone();
        int x = safeLoc.getBlockX();
        int y = safeLoc.getBlockY();
        int z = safeLoc.getBlockZ();

        // 兼容“传入脚下方块”和“传入玩家站立点”两种坐标，优先保留指定结构点。
        for (int i = -2; i < 12; i++) {
            int standY = y + i;
            if (standY <= world.getMinHeight() + 1 || standY >= world.getMaxHeight() - 2) continue;

            Location standLoc = new Location(world, x + 0.5, standY, z + 0.5);
            if (isStrictSpawnCandidate(standLoc)) {
                return standLoc;
            }

            Material feetBlock = world.getBlockAt(x, standY, z).getType();
            Material body = world.getBlockAt(x, standY + 1, z).getType();
            Material head = world.getBlockAt(x, standY + 2, z).getType();

            // 如果传入的是脚下方块坐标，则返回其上一格作为站立点。
            if (feetBlock.isSolid() && !body.isSolid() && !head.isSolid()
                    && feetBlock != Material.LAVA && feetBlock != Material.WATER) {
                safeLoc.setY(standY + 1);
                safeLoc.setX(x + 0.5);
                safeLoc.setZ(z + 0.5);
                return safeLoc;
            }
        }

        // 如果没找到安全位置，返回原位置并向上偏移1格
        safeLoc.add(0, 1, 0);
        return safeLoc;
    }

    // 在指定位置附近生成烟花
    private void spawnFireworksNearLocation(World world, Location center, int radius) {
        java.util.Random random = new java.util.Random();

        // 生成5-8个烟花
        int fireworkCount = 5 + random.nextInt(4);

        for (int i = 0; i < fireworkCount; i++) {
            // 在15格范围内随机位置
            double offsetX = (random.nextDouble() - 0.5) * 2 * radius;
            double offsetZ = (random.nextDouble() - 0.5) * 2 * radius;
            double offsetY = random.nextDouble() * 5; // 0-5格高度

            Location fireworkLoc = center.clone().add(offsetX, offsetY, offsetZ);

            // 延迟生成（0-40 tick，即0-2秒）
            long delay = random.nextInt(41);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.entity.Firework firework = world.spawn(fireworkLoc, org.bukkit.entity.Firework.class);
                org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();

                // 随机颜色
                org.bukkit.Color[] colors = {
                    org.bukkit.Color.RED,
                    org.bukkit.Color.ORANGE,
                    org.bukkit.Color.YELLOW,
                    org.bukkit.Color.LIME,
                    org.bukkit.Color.GREEN,
                    org.bukkit.Color.AQUA,
                    org.bukkit.Color.BLUE,
                    org.bukkit.Color.PURPLE,
                    org.bukkit.Color.FUCHSIA,
                    org.bukkit.Color.WHITE
                };

                org.bukkit.Color color1 = colors[random.nextInt(colors.length)];
                org.bukkit.Color color2 = colors[random.nextInt(colors.length)];

                // 随机效果类型
                org.bukkit.FireworkEffect.Type[] types = {
                    org.bukkit.FireworkEffect.Type.BALL,
                    org.bukkit.FireworkEffect.Type.BALL_LARGE,
                    org.bukkit.FireworkEffect.Type.STAR,
                    org.bukkit.FireworkEffect.Type.BURST,
                    org.bukkit.FireworkEffect.Type.CREEPER
                };

                org.bukkit.FireworkEffect.Type type = types[random.nextInt(types.length)];

                // 创建烟花效果
                org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                    .withColor(color1)
                    .withFade(color2)
                    .with(type)
                    .flicker(random.nextBoolean())
                    .trail(random.nextBoolean())
                    .build();

                meta.addEffect(effect);
                meta.setPower(random.nextInt(2)); // 0-1，飞行高度
                firework.setFireworkMeta(meta);

                // 立即引爆（1-2秒后）
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    firework.detonate();
                }, 20L + random.nextInt(21)); // 20-40 tick = 1-2秒
            }, delay);
        }
    }

    private void playHunterVictoryEffect(GameRoom room, Location fallbackLocation) {
        Location center = room.getVictoryEffectLocation();
        if (center == null) {
            center = fallbackLocation == null ? null : fallbackLocation.clone();
        }
        if (center == null || center.getWorld() == null) {
            return;
        }

        UUID trigger = room.getVictoryEffectTriggerUuid();
        String effectId = "fireworks";
        if (trigger != null && room.isHunter(trigger)) {
            String selected = plugin.getPlayerDataManager().getSelectedHunterVictoryEffect(trigger);
            if (plugin.getPlayerDataManager().hasVictoryEffect(trigger, selected)) {
                effectId = selected;
            }
        }

        HunterVictoryEffect effect = HunterVictoryEffect.byId(effectId);
        switch (effect) {
            case BLACK_HOLE -> playBlackHoleEffect(center);
            case STAR_RAIN -> playStarRainEffect(center);
            case DRAGON_BREATH_BLOOM -> playDragonBreathBloomEffect(center);
            case THUNDER_CROWN -> playThunderCrownEffect(center);
            case SOUL_VORTEX -> playSoulVortexEffect(center);
            case AURORA_SPIRAL -> playAuroraSpiralEffect(center);
            case CRYSTAL_BLOOM -> playCrystalBloomEffect(center);
            case FIREWORKS -> spawnFireworksNearLocation(center.getWorld(), center, 15);
        }
        room.clearVictoryEffectTrigger();
    }

    private void safePlayHunterVictoryEffect(GameRoom room, Location fallbackLocation) {
        try {
            playHunterVictoryEffect(room, fallbackLocation);
        } catch (Throwable throwable) {
            if (room != null) {
                room.clearVictoryEffectTrigger();
            }
            plugin.getLogger().warning("猎人胜利特效播放失败，已跳过特效并继续正常结束游戏: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    public void playHunterKillEffect(Player killer, Location deathLocation) {
        if (killer == null || deathLocation == null || deathLocation.getWorld() == null) {
            return;
        }
        String effectId = plugin.getPlayerDataManager().getSelectedHunterKillEffect(killer.getUniqueId());
        HunterKillEffect effect = HunterKillEffect.byId(effectId);
        if (effect == HunterKillEffect.NONE) {
            return;
        }

        World world = deathLocation.getWorld();
        Location center = deathLocation.clone().add(0.0D, 0.9D, 0.0D);
        switch (effect) {
            case FIREWORK_BURST -> {
                world.spawnParticle(Particle.FIREWORK, center, 24, 0.45D, 0.45D, 0.45D, 0.12D);
                world.spawnParticle(Particle.END_ROD, center, 12, 0.3D, 0.25D, 0.3D, 0.02D);
                playKillEffectNearbySound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.85f, 1.18f, 18.0D);
            }
            case SOUL_BREAK -> {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 28, 0.4D, 0.4D, 0.4D, 0.03D);
                world.spawnParticle(Particle.SCULK_SOUL, center, 16, 0.35D, 0.3D, 0.35D, 0.03D);
                playKillEffectNearbySound(center, Sound.PARTICLE_SOUL_ESCAPE, 0.95f, 0.92f, 18.0D);
            }
            case THUNDER_MARK -> {
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 36, 0.45D, 0.5D, 0.45D, 0.08D);
                world.spawnParticle(Particle.FLASH, center, 1);
                playKillEffectNearbySound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.45f, 1.35f, 20.0D);
            }
            case BLOOD_BLOOM -> {
                world.spawnParticle(Particle.DUST, center, 32, 0.45D, 0.35D, 0.45D,
                        new Particle.DustOptions(Color.fromRGB(196, 42, 42), 1.65f));
                world.spawnParticle(Particle.CRIT, center, 12, 0.35D, 0.2D, 0.35D, 0.08D);
                playKillEffectNearbySound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.95f, 0.86f, 18.0D);
            }
            case VOID_CRACK -> {
                world.spawnParticle(Particle.PORTAL, center, 48, 0.48D, 0.4D, 0.48D, 0.55D);
                world.spawnParticle(Particle.REVERSE_PORTAL, center, 24, 0.32D, 0.2D, 0.32D, 0.08D);
                world.spawnParticle(Particle.SQUID_INK, center, 12, 0.24D, 0.18D, 0.24D, 0.02D);
                playKillEffectNearbySound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.62f, 18.0D);
            }
            case FROST_BITE -> {
                world.spawnParticle(Particle.SNOWFLAKE, center, 42, 0.42D, 0.42D, 0.42D, 0.04D);
                world.spawnParticle(Particle.ITEM_SNOWBALL, center, 22, 0.26D, 0.22D, 0.26D, 0.03D);
                world.spawnParticle(Particle.CLOUD, center, 12, 0.24D, 0.16D, 0.24D, 0.01D);
                playKillEffectNearbySound(center, Sound.BLOCK_GLASS_BREAK, 0.78f, 1.72f, 18.0D);
            }
            case SOLAR_FLARE -> {
                world.spawnParticle(Particle.FLAME, center, 40, 0.4D, 0.35D, 0.4D, 0.02D);
                world.spawnParticle(Particle.DUST, center, 28, 0.36D, 0.24D, 0.36D,
                        new Particle.DustOptions(Color.fromRGB(255, 184, 52), 1.75f));
                world.spawnParticle(Particle.FLASH, center, 1);
                playKillEffectNearbySound(center, Sound.ITEM_FIRECHARGE_USE, 0.82f, 1.28f, 18.0D);
            }
            case WITCH_CURSE -> {
                world.spawnParticle(Particle.WITCH, center, 36, 0.38D, 0.36D, 0.38D, 0.02D);
                world.spawnParticle(Particle.ENTITY_EFFECT, center, 24, 0.3D, 0.3D, 0.3D, 0.0D);
                playKillEffectNearbySound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.62f, 1.08f, 18.0D);
            }
            case CHORUS_SHATTER -> {
                world.spawnParticle(Particle.PORTAL, center, 30, 0.35D, 0.35D, 0.35D, 0.22D);
                world.spawnParticle(Particle.END_ROD, center, 18, 0.28D, 0.28D, 0.28D, 0.03D);
                world.spawnParticle(Particle.DUST, center, 16, 0.3D, 0.22D, 0.3D,
                        new Particle.DustOptions(Color.fromRGB(204, 102, 255), 1.45f));
                playKillEffectNearbySound(center, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.88f, 1.12f, 18.0D);
            }
            case TOTEM_COLLAPSE -> {
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 32, 0.34D, 0.42D, 0.34D, 0.12D);
                world.spawnParticle(Particle.GLOW, center, 18, 0.26D, 0.32D, 0.26D, 0.03D);
                world.spawnParticle(Particle.CRIT, center, 10, 0.26D, 0.18D, 0.26D, 0.08D);
                playKillEffectNearbySound(center, Sound.ITEM_TOTEM_USE, 0.68f, 0.74f, 18.0D);
            }
            case ECHO_PULSE -> {
                world.spawnParticle(Particle.SCULK_SOUL, center, 20, 0.34D, 0.32D, 0.34D, 0.02D);
                world.spawnParticle(Particle.TRIAL_OMEN, center, 16, 0.22D, 0.16D, 0.22D, 0.01D);
                world.spawnParticle(Particle.DUST, center, 18, 0.32D, 0.18D, 0.32D,
                        new Particle.DustOptions(Color.fromRGB(74, 196, 212), 1.55f));
                playKillEffectNearbySound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.62f, 0.86f, 18.0D);
            }
            case ROSE_FUNERAL -> {
                world.spawnParticle(Particle.SMOKE, center, 24, 0.34D, 0.28D, 0.34D, 0.02D);
                world.spawnParticle(Particle.FALLING_DUST, center, 20, 0.26D, 0.22D, 0.26D,
                        Bukkit.createBlockData(Material.RED_CONCRETE_POWDER));
                world.spawnParticle(Particle.DUST, center, 14, 0.26D, 0.18D, 0.26D,
                        new Particle.DustOptions(Color.fromRGB(112, 12, 24), 1.45f));
                playKillEffectNearbySound(center, Sound.ENTITY_WITHER_HURT, 0.55f, 1.45f, 18.0D);
            }
            case NONE -> {
            }
        }
    }

    private void playKillEffectNearbySound(Location center, Sound sound, float volume, float pitch, double radius) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        double radiusSquared = radius * radius;
        for (Player nearby : world.getPlayers()) {
            if (nearby.getLocation().distanceSquared(center) <= radiusSquared) {
                nearby.playSound(center, sound, volume, pitch);
            }
        }
    }

    private void playBlackHoleEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.3f, 0.55f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.65f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 100) {
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                    world.spawnParticle(Particle.FLASH, center, 1);
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.DUST, center, 30, 1.2, 1.2, 1.2,
                        new Particle.DustOptions(Color.BLACK, 3.5f));
                for (int i = 0; i < 28; i++) {
                    double angle = Math.toRadians((tick * 18 + i * 360.0 / 28));
                    double radius = Math.max(0.3, 7.0 - tick * 0.06);
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.4 + Math.sin(tick * 0.13 + i) * 1.8, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.REVERSE_PORTAL, p, 2, 0.05, 0.05, 0.05, 0.02);
                    world.spawnParticle(Particle.SQUID_INK, p, 1, 0.03, 0.03, 0.03, 0.01);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playStarRainEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.1f, 1.8f);
        Random random = new Random();
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 80) {
                    spawnFireworksNearLocation(world, center, 8);
                    cancel();
                    return;
                }
                for (int i = 0; i < 10; i++) {
                    Location p = center.clone().add((random.nextDouble() - 0.5) * 14, 7 - random.nextDouble() * 4, (random.nextDouble() - 0.5) * 14);
                    world.spawnParticle(Particle.END_ROD, p, 3, 0.12, 0.6, 0.12, 0.04);
                    world.spawnParticle(Particle.FIREWORK, p, 2, 0.08, 0.2, 0.08, 0.03);
                }
                tick += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void playDragonBreathBloomEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.45f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 90) {
                    cancel();
                    return;
                }
                double radius = 1.0 + tick * 0.055;
                for (int i = 0; i < 36; i++) {
                    double angle = Math.toRadians(i * 10 + tick * 4);
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.6 + Math.sin(angle * 2) * 0.35, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.DRAGON_BREATH, p, 2, 0.05, 0.05, 0.05, 0.01);
                }
                world.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 12, 1.0, 0.6, 1.0,
                        new Particle.DustOptions(Color.PURPLE, 1.8f));
                tick += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void playThunderCrownEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.strikeLightningEffect(center);
        world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.15f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 70) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + tick * 7);
                    Location p = center.clone().add(Math.cos(angle) * 3.0, 2.2 + Math.sin(i) * 0.35, Math.sin(angle) * 3.0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticle(Particle.DUST, p, 2, 0.04, 0.04, 0.04,
                            new Particle.DustOptions(Color.YELLOW, 1.6f));
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playSoulVortexEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_SOUL_SAND_PLACE, 1.2f, 0.8f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 100) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 26; i++) {
                    double y = i * 0.11;
                    double angle = Math.toRadians(tick * 8 + i * 24);
                    double radius = 3.2 - i * 0.07;
                    Location p = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.04, 0.04, 0.04, 0.01);
                    world.spawnParticle(Particle.SOUL, p, 1, 0.04, 0.04, 0.04, 0.01);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playAuroraSpiralEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.7f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 90) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 32; i++) {
                    double angle = Math.toRadians(tick * 6 + i * 18);
                    double radius = 2.0 + Math.sin((tick + i) * 0.12);
                    Location p = center.clone().add(Math.cos(angle) * radius, i * 0.09, Math.sin(angle) * radius);
                    Color color = i % 2 == 0 ? Color.AQUA : Color.FUCHSIA;
                    world.spawnParticle(Particle.DUST, p, 2, 0.06, 0.06, 0.06,
                            new Particle.DustOptions(color, 1.4f));
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playCrystalBloomEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.4f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 70) {
                    cancel();
                    return;
                }
                double radius = 0.5 + tick * 0.07;
                for (int i = 0; i < 40; i++) {
                    double angle = Math.toRadians(i * 9);
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.7 + Math.sin(tick * 0.12) * 0.6, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.END_ROD, p, 1, 0.03, 0.03, 0.03, 0.02);
                    world.spawnParticle(Particle.DUST, p, 1, 0.03, 0.03, 0.03,
                            new Particle.DustOptions(Color.fromRGB(190, 130, 255), 1.2f));
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playGoldenPillarEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.45f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 60) {
                    cancel();
                    return;
                }
                for (int y = 0; y < 8; y++) {
                    Location p = center.clone().add(0.0D, y * 0.45D, 0.0D);
                    world.spawnParticle(Particle.END_ROD, p, 2, 0.12D, 0.05D, 0.12D, 0.02D);
                    world.spawnParticle(Particle.DUST, p, 3, 0.18D, 0.08D, 0.18D,
                            new Particle.DustOptions(Color.fromRGB(255, 214, 64), 1.7f));
                }
                tick += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void playCloverRingEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.7f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 72) {
                    cancel();
                    return;
                }
                double radius = 2.0D + Math.sin(tick * 0.08D) * 0.5D;
                for (int i = 0; i < 28; i++) {
                    double angle = Math.toRadians(i * (360.0D / 28.0D) + tick * 5.0D);
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.55D, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, p, 1, 0.04D, 0.04D, 0.04D, 0.01D);
                    world.spawnParticle(Particle.DUST, p, 1, 0.03D, 0.03D, 0.03D,
                            new Particle.DustOptions(Color.fromRGB(92, 255, 120), 1.45f));
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playSkyGiftEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.ENTITY_ITEM_PICKUP, 0.85f, 0.8f);
        world.playSound(center, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
        Random random = new Random();
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 50) {
                    spawnFireworksNearLocation(world, center, 6);
                    cancel();
                    return;
                }
                for (int i = 0; i < 8; i++) {
                    Location p = center.clone().add((random.nextDouble() - 0.5D) * 6.0D, 5.5D - tick * 0.08D, (random.nextDouble() - 0.5D) * 6.0D);
                    world.spawnParticle(Particle.GLOW, p, 2, 0.05D, 0.05D, 0.05D, 0.02D);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, p, 1, 0.04D, 0.04D, 0.04D, 0.01D);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playVoidLotusEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.95f, 0.7f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 68) {
                    cancel();
                    return;
                }
                double radius = 0.8D + tick * 0.05D;
                for (int petal = 0; petal < 6; petal++) {
                    double base = Math.toRadians(petal * 60.0D + tick * 3.0D);
                    for (int i = 0; i < 8; i++) {
                        double angle = base + Math.toRadians(i * 6.0D);
                        Location p = center.clone().add(Math.cos(angle) * radius, 0.45D + Math.sin(i * 0.55D) * 0.25D, Math.sin(angle) * radius);
                        world.spawnParticle(Particle.REVERSE_PORTAL, p, 1, 0.03D, 0.03D, 0.03D, 0.02D);
                        world.spawnParticle(Particle.DUST, p, 1, 0.03D, 0.03D, 0.03D,
                                new Particle.DustOptions(Color.fromRGB(150, 78, 255), 1.35f));
                    }
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playHoneySplashEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_HONEY_BLOCK_BREAK, 0.9f, 1.15f);
        world.spawnParticle(Particle.WAX_ON, center, 18, 0.45D, 0.3D, 0.45D, 0.02D);
        world.spawnParticle(Particle.ITEM_SLIME, center, 24, 0.4D, 0.25D, 0.4D, 0.03D);
        world.spawnParticle(Particle.DUST, center, 16, 0.32D, 0.22D, 0.32D,
                new Particle.DustOptions(Color.fromRGB(255, 196, 72), 1.6f));
    }

    private void playPrismColumnEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.82f, 1.52f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 70) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18.0D + tick * 8.0D);
                    double radius = 1.25D + Math.sin((tick + i) * 0.15D) * 0.35D;
                    Location p = center.clone().add(Math.cos(angle) * radius, i * 0.12D, Math.sin(angle) * radius);
                    Color color = switch (i % 4) {
                        case 0 -> Color.AQUA;
                        case 1 -> Color.FUCHSIA;
                        case 2 -> Color.YELLOW;
                        default -> Color.WHITE;
                    };
                    world.spawnParticle(Particle.DUST, p, 2, 0.03D, 0.03D, 0.03D,
                            new Particle.DustOptions(color, 1.35f));
                    world.spawnParticle(Particle.END_ROD, p, 1, 0.02D, 0.02D, 0.02D, 0.01D);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void playTotemGardenEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        world.playSound(center, Sound.ITEM_TOTEM_USE, 0.72f, 1.05f);
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick > 56) {
                    cancel();
                    return;
                }
                double radius = 1.0D + tick * 0.06D;
                for (int i = 0; i < 18; i++) {
                    double angle = Math.toRadians(i * 20.0D);
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.35D + Math.sin(tick * 0.12D) * 0.2D, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, p, 1, 0.03D, 0.03D, 0.03D, 0.01D);
                    world.spawnParticle(Particle.GLOW, p, 1, 0.04D, 0.04D, 0.04D, 0.01D);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}

