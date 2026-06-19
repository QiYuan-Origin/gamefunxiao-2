package org.gamefunxiao.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.menu.hunter.*;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MenuManager {

    private final GameFunXiao plugin;
    private final Map<UUID, BaseMenu> openMenus = new HashMap<>();
    private final Set<UUID> commandReturnMenus = new HashSet<>();
    private final Map<UUID, String> commandBackCommands = new HashMap<>();
    private final Map<UUID, Class<? extends BaseMenu>> commandReturnRootMenuTypes = new HashMap<>();
    private static final List<String> COMMAND_MENU_IDS = List.of(
            "main", "home", "hunter", "huntergame", "hg",
            "lucky", "luckypillars", "lp",
            "rooms", "roomlist", "luckyrooms", "lprooms",
            "create", "createroom", "luckycreate", "lpcreate",
            "leaderboard", "lb", "luckyleaderboard", "lplb",
            "shop", "settings", "victoryshop", "victorysettings",
            "endflashkit", "endflashkitadmin", "personalkit", "endflashpersonalkit",
            "pass_count", "fastest_time", "play_count", "hunter_points", "prey_points", "minigame_points"
    );

    public MenuManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        MainNavigationMenu menu = new MainNavigationMenu(plugin, player);
        openMenu(player, menu);
    }

    public void openCommandGatewayMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        commandReturnMenus.remove(player.getUniqueId());
        commandBackCommands.remove(player.getUniqueId());
        commandReturnRootMenuTypes.remove(player.getUniqueId());
        openMenu(player, new CommandGatewayMenu(plugin, player));
    }

    public boolean openMenuFromCommand(Player player, String menuId) {
        return openMenuFromCommand(player, menuId, null);
    }

    public boolean openMenuFromCommand(Player player, String menuId, String backCommand) {
        if (!canOpenGameFunMenu(player)) {
            return true;
        }

        BaseMenu menu = createCommandTargetMenu(player, menuId);
        if (menu == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        commandReturnMenus.add(uuid);
        commandReturnRootMenuTypes.put(uuid, menu.getClass());
        String normalizedBackCommand = normalizeBackCommand(backCommand);
        if (normalizedBackCommand == null || normalizedBackCommand.isBlank()) {
            commandBackCommands.remove(uuid);
        } else {
            commandBackCommands.put(uuid, normalizedBackCommand);
        }
        openMenu(player, menu);
        return true;
    }

    private BaseMenu createCommandTargetMenu(Player player, String menuId) {
        String id = normalizeCommandMenuId(menuId);
        return switch (id) {
            case "main", "home", "menu", "主菜单", "主页" -> new MainNavigationMenu(plugin, player);
            case "hunter", "huntergame", "hg", "猎人", "猎人游戏" -> new HunterGameNavigationMenu(plugin, player);
            case "lucky", "luckypillars", "lp", "幸运之柱" -> new LuckyPillarsNavigationMenu(plugin, player);
            case "rooms", "roomlist", "房间", "查看房间" -> new RoomListMenu(plugin, player);
            case "luckyrooms", "lprooms", "幸运房间" -> RoomListMenu.luckyPillarsOnly(plugin, player);
            case "create", "createroom", "创建", "创建房间" -> new CreateRoomMenu(plugin, player);
            case "luckycreate", "lpcreate", "幸运创建" -> new CreateRoomMenu(plugin, player, MenuSection.LUCKY_PILLARS);
            case "leaderboard", "lb", "排行", "排行榜" -> new LeaderboardMenu(plugin, player);
            case "luckyleaderboard", "lplb", "幸运排行" -> new LeaderboardMenu(plugin, player, MenuSection.LUCKY_PILLARS, RoomListMenu.defaultLuckyFilter());
            case "passcount", "pass_count", "通关次数" -> new LeaderboardDetailMenu(plugin, player, "pass_count");
            case "fastesttime", "fastest_time", "最快通关" -> new LeaderboardDetailMenu(plugin, player, "fastest_time");
            case "playcount", "play_count", "游玩次数" -> new LeaderboardDetailMenu(plugin, player, "play_count");
            case "hunterpoints", "hunter_points", "猎人积分" -> new LeaderboardDetailMenu(plugin, player, "hunter_points");
            case "preypoints", "prey_points", "猎物积分" -> new LeaderboardDetailMenu(plugin, player, "prey_points");
            case "minigamepoints", "minigame_points", "小游戏积分" -> new LeaderboardDetailMenu(plugin, player, "minigame_points", MenuSection.LUCKY_PILLARS, RoomListMenu.defaultLuckyFilter());
            case "shop", "商城", "商店" -> new MiniGameShopCategoryMenu(plugin, player);
            case "settings", "setting", "设置", "个人设置" -> new SettingsCategoryMenu(plugin, player);
            case "victoryshop", "effectshop", "特效商城" -> new HunterVictoryEffectShopMenu(plugin, player);
            case "victorysettings", "effectsettings", "特效设置" -> new HunterVictoryEffectSettingsMenu(plugin, player);
            case "luckyvictoryshop", "lpvictoryshop", "幸运特效商城", "幸运之柱特效商城" -> new LuckyPillarsVictoryEffectShopMenu(plugin, player);
            case "luckyvictorysettings", "lpvictorysettings", "幸运特效设置", "幸运之柱特效设置" -> new LuckyPillarsVictoryEffectSettingsMenu(plugin, player);
            case "killshop", "killeffectshop", "击杀特效商城" -> new HunterKillEffectShopMenu(plugin, player);
            case "killsettings", "killeffectsettings", "击杀特效设置" -> new HunterKillEffectSettingsMenu(plugin, player);
            case "endflashkit", "endflashkitadmin", "flashkit", "终章kit", "闪光kit" -> {
                if (!player.hasPermission("gamefunxiao.admin")) {
                    player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    yield null;
                }
                yield new EndFlashKitAdminMenu(plugin, player);
            }
            case "personalkit", "endflashpersonalkit", "个人kit", "个人终章kit" -> new EndFlashPersonalKitMenu(plugin, player);
            default -> null;
        };
    }

    private String normalizeCommandMenuId(String menuId) {
        if (menuId == null) {
            return "";
        }
        return menuId.trim().toLowerCase(java.util.Locale.ROOT).replace("-", "").replace("_", "");
    }

    public List<String> getCommandMenuIds() {
        return Collections.unmodifiableList(COMMAND_MENU_IDS);
    }

    public boolean tryHandleCommandBackButton(Player player, BaseMenu currentMenu, InventoryClickEvent event) {
        if (player == null || currentMenu == null || event == null) {
            return false;
        }
        if (!commandReturnMenus.contains(player.getUniqueId()) || currentMenu instanceof CommandGatewayMenu) {
            return false;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return false;
        }
        if (!isCommandReturnBackItem(event.getCurrentItem())) {
            return false;
        }
        if (!isCommandReturnRootMenu(player, currentMenu)) {
            return false;
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72f, 1.55f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.52f, 1.2f);
        String customBackCommand = commandBackCommands.get(player.getUniqueId());
        if (customBackCommand != null && !customBackCommand.isBlank()) {
            commandReturnMenus.remove(player.getUniqueId());
            commandBackCommands.remove(player.getUniqueId());
            commandReturnRootMenuTypes.remove(player.getUniqueId());
            // 不先关闭当前 GameFun 菜单，让旧菜单保持显示到目标命令打开新菜单。
            // 命令放到下一 tick 执行，避免在 InventoryClickEvent 内直接开关菜单导致状态异常。
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.performCommand(customBackCommand);
                }
            });
            return true;
        }
        openCommandGatewayMenu(player);
        return true;
    }

    private boolean isCommandReturnRootMenu(Player player, BaseMenu currentMenu) {
        Class<? extends BaseMenu> rootMenuType = commandReturnRootMenuTypes.get(player.getUniqueId());
        return rootMenuType != null && rootMenuType.equals(currentMenu.getClass());
    }

    private String normalizeBackCommand(String command) {
        if (command == null) {
            return null;
        }
        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private boolean isCommandReturnBackItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        String display = meta.hasDisplayName() ? meta.getDisplayName() : "";
        if (display.contains("上一页") || display.contains("下一页") || display.contains("返回选择模式")) {
            return false;
        }
        if (display.contains("返回主菜单") || display.contains("§c返回")) {
            return true;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        return display.contains("返回") && lore.stream().anyMatch(line -> line.contains("返回上一级"));
    }

    public void openHunterGameMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        HunterGameNavigationMenu menu = new HunterGameNavigationMenu(plugin, player);
        openMenu(player, menu);
    }


    public void openLuckyPillarsMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LuckyPillarsNavigationMenu(plugin, player));
    }


    public void openLuckyPillarsRoomListMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, RoomListMenu.luckyPillarsOnly(plugin, player));
    }



    public void openRoomListMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        RoomListMenu menu = new RoomListMenu(plugin, player);
        openMenu(player, menu);
    }

    public void openCreateRoomMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        CreateRoomMenu menu = new CreateRoomMenu(plugin, player);
        openMenu(player, menu);
    }

    public void openLeaderboardMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        LeaderboardMenu menu = new LeaderboardMenu(plugin, player);
        openMenu(player, menu);
    }

    public void openLuckyPillarsLeaderboardMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LeaderboardMenu(plugin, player, MenuSection.LUCKY_PILLARS, RoomListMenu.defaultLuckyFilter()));
    }


    public void openShopCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new MiniGameShopCategoryMenu(plugin, player));
    }

    public void openMiniGameShopCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new MiniGameShopCategoryMenu(plugin, player));
    }

    public void openHunterVictoryEffectShopMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterVictoryEffectShopMenu(plugin, player));
    }

    public void openHunterKillEffectShopMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterKillEffectShopMenu(plugin, player));
    }

    public void openHunterGameShopCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterGameShopCategoryMenu(plugin, player));
    }

    public void openLuckyPillarsShopCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LuckyPillarsShopCategoryMenu(plugin, player));
    }


    public void openHunterVictoryEffectSettingsMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterVictoryEffectSettingsMenu(plugin, player));
    }

    public void openLuckyPillarsVictoryEffectShopMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LuckyPillarsVictoryEffectShopMenu(plugin, player));
    }

    public void openLuckyPillarsVictoryEffectSettingsMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LuckyPillarsVictoryEffectSettingsMenu(plugin, player));
    }

    public void openHunterKillEffectSettingsMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterKillEffectSettingsMenu(plugin, player));
    }

    public void openHunterGameEffectCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new HunterGameEffectCategoryMenu(plugin, player));
    }

    public void openLuckyPillarsEffectCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new LuckyPillarsEffectCategoryMenu(plugin, player));
    }

    public void openGameSettingsMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new GameSettingsMenu(plugin, player));
    }

    public void openSettingsCategoryMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        openMenu(player, new SettingsCategoryMenu(plugin, player));
    }

    public void openEndFlashKitAdminMenu(Player player) {
        if (!canOpenGameFunMenu(player)) {
            return;
        }
        if (!player.hasPermission("gamefunxiao.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.no_permission"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        openMenu(player, new EndFlashKitAdminMenu(plugin, player));
    }

    private void openMenu(Player player, BaseMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
        menu.open();
    }

    public void registerMenu(Player player, BaseMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public BaseMenu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }

    public void closeMenu(Player player) {
        openMenus.remove(player.getUniqueId());
        commandReturnMenus.remove(player.getUniqueId());
        commandBackCommands.remove(player.getUniqueId());
        commandReturnRootMenuTypes.remove(player.getUniqueId());
    }

    private boolean canOpenGameFunMenu(Player player) {
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (room != null && room.getState() == RoomState.PLAYING && room.isGameActuallyStarted()) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§5§5⚠ §c游戏正式开始后不能打开 GameFun 菜单。");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        return true;
    }
}
