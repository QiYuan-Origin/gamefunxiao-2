package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateRoomMenu extends BaseMenu {

    private static final int[] MODE_SLOTS = {19, 20, 21, 22, 23, 24, 25, 31};

    private final MenuSection menuSection;
    private final List<GameMode> availableModes;
    private GameMode selectedMode = null;
    private int maxPlayers = 16;
    private boolean isPublic = true;
    private final Set<String> selectedModifiers = new HashSet<>();
    private int step = 1;

    public CreateRoomMenu(GameFunXiao plugin, Player player) {
        this(plugin, player, MenuSection.HUNTER);
    }

    public CreateRoomMenu(GameFunXiao plugin, Player player, MenuSection menuSection) {
        super(plugin, player, resolveTitle(menuSection), 54);
        this.menuSection = menuSection == null ? MenuSection.HUNTER : menuSection;
        this.availableModes = resolveModes(this.menuSection);
    }

    private static String resolveTitle(MenuSection menuSection) {
        return switch (menuSection) {
            case LUCKY_PILLARS -> "§0§l🍀 幸运之柱 - 创建房间 🍀";
            case BRICK_GUARD -> "§0§l▣ 板砖守卫战 - 创建房间 ▣";
            case GENERIC, HUNTER -> "§0§l⚔ 猎人游戏 - 创建房间 ⚔";
        };
    }

    private static List<GameMode> resolveModes(MenuSection menuSection) {
        return switch (menuSection) {
            case LUCKY_PILLARS -> new ArrayList<>(GameMode.getLuckyPillarsSectionModes());
            case BRICK_GUARD -> new ArrayList<>(GameMode.getBrickGuardSectionModes());
            case GENERIC, HUNTER -> GameMode.getHunterCreateMenuModes();
        };
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        if (step == 1) {
            setupModeSelection();
        } else {
            setupRoomOptions();
        }
    }

    private void setupModeSelection() {
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(getSectionTitleMaterial(),
                getSectionTitleText(),
                "§8· · · · · · · · · · · · · ·",
                switch (menuSection) {
                    case LUCKY_PILLARS -> "§f先选你要开的幸运之柱玩法";
                    case BRICK_GUARD -> "§f当前分区只创建板砖守卫战房间";
                    case GENERIC, HUNTER -> "§f先选你要开的猎人玩法";
                },
                "§f这个界面里的模式都只属于当前玩法分区",
                "§8· · · · · · · · · · · · · ·"));

        for (int i = 0; i < availableModes.size() && i < MODE_SLOTS.length; i++) {
            inventory.setItem(MODE_SLOTS[i], createModeButton(availableModes.get(i)));
        }

        inventory.setItem(45, createBackButton());
    }

    private void setupRoomOptions() {
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(getSectionTitleMaterial(),
                getSectionTitleText(),
                "§8· · · · · · · · · · · · · ·",
                "§f当前模式: §e" + selectedMode.getDisplayName(),
                "§f继续设置这个玩法房间的基础参数",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(19, createMaxPlayersButton());
        inventory.setItem(10, createItem(Material.RED_STAINED_GLASS_PANE,
                "   §8[§c- §7减少人数§8]",
                "§f- §7点击减少最大人数"));
        inventory.setItem(28, createItem(Material.LIME_STAINED_GLASS_PANE,
                "   §8[§a+ §7增加人数§8]",
                "§f- §7点击增加最大人数"));

        inventory.setItem(22, createVisibilityButton());

        if (!isPublic) {
            inventory.setItem(31, createInviteHintButton());
        }

        if (selectedMode == GameMode.CUSTOM) {
            inventory.setItem(25, createModifiersButton());
        }

        inventory.setItem(40, createCreateButton());
        inventory.setItem(45, createItem(Material.ARROW,
                "   §8[§c← §7返回选择模式§8]",
                "§f- §7点击回到当前玩法的模式列表"));
    }

    private Material getSectionTitleMaterial() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> Material.GOLD_BLOCK;
            case BRICK_GUARD -> Material.BRICK;
            case GENERIC, HUNTER -> Material.IRON_SWORD;
        };
    }

    private String getSectionTitleText() {
        return switch (menuSection) {
            case LUCKY_PILLARS -> "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱房间";
            case BRICK_GUARD -> "§x§F§F§7§C§0§0▣ §x§F§F§8§8§1§1板§x§F§F§9§4§2§2砖§x§D§D§6§6§1§1守§x§B§B§4§4§0§0卫§x§6§6§1§9§0§0战房间";
            case GENERIC, HUNTER -> "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9房§x§C§C§F§F§9§9间";
        };
    }

    private ItemStack createModeButton(GameMode mode) {
        ItemStack item = new ItemStack(getModeMaterial(mode));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[" + getModeDisplay(mode) + "§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            for (String line : getModeLore(mode)) {
                lore.add(line);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击选择这个模式");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getModeMaterial(GameMode mode) {
        return switch (mode) {
            case CLASSIC -> Material.COMPASS;
            case RANDOM_COMPASS -> Material.RECOVERY_COMPASS;
            case SWAP -> Material.ENDER_PEARL;
            case NO_ITEM -> Material.BARRIER;
            case SURVIVAL -> Material.CLOCK;
            case FLASH_TOURNAMENT -> Material.RED_BANNER;
            case END_FLASH -> Material.END_CRYSTAL;
            case CUSTOM -> Material.COMMAND_BLOCK;
            case LUCKY_PILLARS -> Material.GOLD_BLOCK;
            case BRICK_GUARD -> Material.BRICK;
            default -> Material.PAPER;
        };
    }

    private String getModeDisplay(GameMode mode) {
        return switch (mode) {
            case CLASSIC -> "§x§5§5§F§F§5§5经§x§7§7§F§F§7§7典§x§9§9§F§F§9§9模§x§B§B§F§F§B§B式";
            case RANDOM_COMPASS -> "§x§F§F§A§A§5§5随§x§F§F§C§C§7§7机§x§F§F§E§E§9§9指§x§D§D§F§F§9§9南§x§B§B§F§F§7§7针";
            case SWAP -> "§x§D§D§5§5§F§F互§x§C§C§7§7§F§F换§x§B§B§9§9§F§F模§x§A§A§B§B§F§F式";
            case NO_ITEM -> "§x§F§F§5§5§5§5无§x§F§F§7§7§7§7有§x§F§F§9§9§9§9模§x§F§F§B§B§B§B式";
            case SURVIVAL -> "§x§F§F§E§E§5§5存§x§E§E§F§F§7§7活§x§D§D§F§F§9§9模§x§C§C§F§F§B§B式";
            case FLASH_TOURNAMENT -> "§x§F§F§F§F§9§9闪§x§F§F§E§E§6§6光 §c§l· 赛事";
            case END_FLASH -> "§x§B§B§8§8§F§F终§x§D§D§A§A§F§F章 §x§F§F§D§D§A§A· §x§D§D§F§F§C§C闪§x§B§B§F§F§E§E光";
            case CUSTOM -> "§x§F§F§D§7§0§0自§x§F§F§B§B§3§3定§x§F§F§9§9§6§6义§x§F§F§7§7§9§9模§x§F§F§5§5§C§C式";
            case LUCKY_PILLARS -> "§x§F§F§D§D§5§5幸§x§F§F§C§C§6§6运§x§F§F§B§B§7§7之§x§F§F§A§A§8§8柱";
            case BRICK_GUARD -> "§x§F§F§7§C§0§0板§x§F§F§8§8§1§1砖 §x§D§D§5§5§1§1· §x§9§9§3§3§0§0守§x§6§6§1§9§0§0卫战";
            default -> mode.getDisplayName();
        };
    }

    private List<String> getModeLore(GameMode mode) {
        List<String> lore = new ArrayList<>();
        switch (mode) {
            case CLASSIC -> {
                lore.add("§f- §e目标: §a击败末影龙");
                lore.add("§f- §b猎人拥有追踪指南针");
            }
            case RANDOM_COMPASS -> {
                lore.add("§f- §e目标: §a击败末影龙");
                lore.add("§f- §b每5分钟随机发一次特殊指南针");
            }
            case SWAP -> {
                lore.add("§f- §e目标: §a击败末影龙");
                lore.add("§f- §d双猎物定时互换位置和背包");
            }
            case NO_ITEM -> {
                lore.add("§f- §e目标: §a击败末影龙");
                lore.add("§f- §c没有指南针和传送机会");
            }
            case SURVIVAL -> {
                lore.add("§f- §e目标: §a猎物存活到时间结束");
                lore.add("§f- §b更适合长时追逐局");
            }
            case FLASH_TOURNAMENT -> {
                lore.add("§f- §e赛事版闪光玩法");
                lore.add("§f- §c无职业前缀，猎人互伤，死亡必掉");
            }
            case END_FLASH -> {
                lore.add("§f- §d只在末地开局");
                lore.add("§f- §b猎人和猎物会使用终章 Kit 池");
            }
            case CUSTOM -> {
                lore.add("§f- §a自由选择修饰符");
                lore.add("§f- §c默认不计入排行榜");
            }
            case LUCKY_PILLARS -> {
                lore.add("§f- §a高空柱台出生");
                lore.add("§f- §e按配置间隔随机发原版物品");
                lore.add("§f- §b最后存活者获胜");
            }
            case BRICK_GUARD -> {
                lore.add("§f- §6两队夺旗升级对抗");
                lore.add("§f- §c板砖核心方块与下界核心玩家同时存在");
                lore.add("§f- §7围绕核心方块和核心玩家推进胜负");
            }
            default -> lore.add("§f- §7暂无说明");
        }
        return lore;
    }

    private ItemStack createMaxPlayersButton() {
        String maxText = maxPlayers == -1 ? "无限" : String.valueOf(maxPlayers);
        return createItem(Material.PLAYER_HEAD,
                "   §8[§x§5§5§F§F§A§A👥 §x§7§7§F§F§B§B最§x§9§9§F§F§C§C大§x§B§B§F§F§D§D人§x§D§D§F§F§E§E数§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前: §b" + maxText,
                "§f- §7左上减少，左下增加",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createVisibilityButton() {
        Material material = isPublic ? Material.ENDER_EYE : Material.ENDER_PEARL;
        String current = isPublic ? "§a公开加入" : "§c仅邀请可进";

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §e当前: " + current);
        lore.add("§f- §7点击切换房间可见性");
        lore.add(isPublic
                ? "§f- §7任何玩家都可以直接加入"
                : "§f- §7只有被邀请的玩家可以进来");
        lore.add("§8· · · · · · · · · · · · · ·");

        return createItem(material,
                "   §8[§x§F§F§E§E§5§5🔐 §x§F§F§D§D§7§7进§x§F§F§C§C§9§9入§x§F§F§B§B§B§B权§x§F§F§A§A§D§D限§8]",
                lore);
    }

    private ItemStack createInviteHintButton() {
        return createItem(Material.WRITABLE_BOOK,
                "   §8[§x§5§5§F§F§D§D✉ §x§7§7§F§F§C§C邀§x§9§9§F§F§B§B请§x§B§B§F§F§A§A提§x§D§D§F§F§9§9示§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §7创建完成后可用下面命令邀请",
                "§f- §e/gamefunxiao hg invite <玩家>",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createModifiersButton() {
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §e已选择: §b" + selectedModifiers.size() + " §e个修饰符");
        if (!selectedModifiers.isEmpty()) {
            for (String modifier : selectedModifiers) {
                lore.add("§f  - §b" + modifier);
            }
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §a点击管理修饰符");
        return createItem(Material.BLAZE_POWDER,
                "   §8[§x§F§F§A§A§5§5⚡ §x§F§F§C§C§7§7修§x§F§F§E§E§9§9饰§x§D§D§F§F§9§9符§8]",
                lore);
    }

    private ItemStack createCreateButton() {
        return createItem(Material.EMERALD_BLOCK,
                "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7创§x§9§9§F§F§9§9建§x§B§B§F§F§B§B房§x§D§D§F§F§D§D间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e模式: §b" + selectedMode.getDisplayName(),
                "§f- §e人数: §b" + (maxPlayers == -1 ? "无限" : maxPlayers),
                "§f- §e可见: §b" + (isPublic ? "公开" : "仅邀请"),
                selectedMode == GameMode.CUSTOM
                        ? "§f- §e修饰符: §b" + selectedModifiers.size() + " 个"
                        : "§f- §e修饰符: §7当前模式不启用",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击确认创建");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (step == 1) {
            handleModeSelection(slot);
        } else {
            handleOptionsSelection(slot);
        }
    }

    private void handleModeSelection(int slot) {
        for (int i = 0; i < MODE_SLOTS.length && i < availableModes.size(); i++) {
            if (MODE_SLOTS[i] == slot) {
                selectMode(availableModes.get(i));
                return;
            }
        }

        switch (slot) {
            case 45 -> {
                playClickSound();
                openSectionRoot();
            }
            case 49 -> handleCloseButtonAction();
            default -> {
            }
        }
    }

    private void selectMode(GameMode mode) {
        playSelectSound();
        selectedMode = mode;
        if (mode.isFlashTournament()) {
            maxPlayers = 67;
        } else if (mode.isDirectFlashStart()) {
            maxPlayers = 64;
        } else if (mode == GameMode.LUCKY_PILLARS) {
            maxPlayers = 16;
        } else if (mode == GameMode.BRICK_GUARD) {
            maxPlayers = 16;
        } else {
            maxPlayers = 16;
        }

        int minPlayers = plugin.getRoomManager().getMinimumPlayersForMode(mode);
        if (maxPlayers != -1 && maxPlayers < minPlayers) {
            maxPlayers = minPlayers;
        }
        step = 2;
        setupItems();
    }

    private void handleOptionsSelection(int slot) {
        switch (slot) {
            case 10 -> decreasePlayers();
            case 22 -> {
                playClickSound();
                isPublic = !isPublic;
                setupItems();
            }
            case 25 -> {
                if (selectedMode == GameMode.CUSTOM) {
                    playClickSound();
                    new ModifierSelectMenu(plugin, player, selectedMode, selectedModifiers, this).open();
                }
            }
            case 28 -> increasePlayers();
            case 31 -> {
                if (!isPublic) {
                    playClickSound();
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.invite_command_hint"));
                }
            }
            case 40 -> {
                playConfirmSound();
                player.closeInventory();
                plugin.getRoomManager().createConfiguredRoom(player, selectedMode, maxPlayers, isPublic, selectedModifiers);
            }
            case 45 -> {
                playClickSound();
                step = 1;
                setupItems();
            }
            case 49 -> handleCloseButtonAction();
            default -> {
            }
        }
    }

    private void decreasePlayers() {
        if (selectedMode != null && selectedMode.isFlashTournament()) {
            playErrorSound();
            return;
        }
        int minPlayers = plugin.getRoomManager().getMinimumPlayersForMode(selectedMode);
        if (maxPlayers > minPlayers) {
            playClickSound();
            maxPlayers--;
            setupItems();
        } else {
            playErrorSound();
        }
    }

    private void increasePlayers() {
        int hardMax;
        if (selectedMode != null && selectedMode.isFlashTournament()) {
            hardMax = 67;
        } else if (selectedMode != null && selectedMode.isLuckyPillars()) {
            hardMax = 32;
        } else if (selectedMode != null && selectedMode.isBrickGuard()) {
            hardMax = 32;
        } else {
            hardMax = 20;
        }

        if (maxPlayers < hardMax) {
            playClickSound();
            maxPlayers++;
            setupItems();
        } else if (selectedMode != null && !selectedMode.isFlashTournament() && !selectedMode.isLuckyPillars() && maxPlayers == 20) {
            playClickSound();
            maxPlayers = -1;
            setupItems();
        } else {
            playErrorSound();
        }
    }

    private void openSectionRoot() {
        switch (menuSection) {
            case LUCKY_PILLARS -> plugin.getMenuManager().openLuckyPillarsMenu(player);
            case BRICK_GUARD -> plugin.getMenuManager().openBrickGuardMenu(player);
            case GENERIC, HUNTER -> plugin.getMenuManager().openHunterGameMenu(player);
        }
    }

    public Set<String> getSelectedModifiers() {
        return selectedModifiers;
    }
}
