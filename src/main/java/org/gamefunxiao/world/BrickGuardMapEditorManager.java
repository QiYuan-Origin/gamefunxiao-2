package org.gamefunxiao.world;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

public class BrickGuardMapEditorManager {

    private static final String TOOLKIT_MODEL = "paper";
    private static final String CATEGORY_VILLAGER = "villager";
    private static final String CATEGORY_PIGLIN = "piglin";
    private static final String CATEGORY_RESOURCE = "resource";
    private static final String CATEGORY_SETTINGS = "settings";
    private static final int CATEGORY_PAGE_SIZE = 7;

    private final GameFunXiao plugin;
    private final NamespacedKey toolkitKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey mapKey;
    private final NamespacedKey worldKindKey;
    private final NamespacedKey pageKey;
    private final Map<UUID, EditorSession> editorSessions = new HashMap<>();

    public BrickGuardMapEditorManager(GameFunXiao plugin) {
        this.plugin = plugin;
        this.toolkitKey = new NamespacedKey(plugin, "brick_guard_map_toolkit");
        this.actionKey = new NamespacedKey(plugin, "brick_guard_map_editor_action");
        this.mapKey = new NamespacedKey(plugin, "brick_guard_map_editor_map");
        this.worldKindKey = new NamespacedKey(plugin, "brick_guard_map_editor_kind");
        this.pageKey = new NamespacedKey(plugin, "brick_guard_map_editor_page");
    }

    public boolean isEditing(Player player) {
        return player != null && editorSessions.containsKey(player.getUniqueId());
    }

    public boolean isEditorWorld(World world) {
        if (world == null || plugin.getBrickGuardMapManager() == null) {
            return false;
        }
        return plugin.getBrickGuardMapManager().isTemplateWorldName(world.getName());
    }

    public boolean requestEdit(Player player, String mapId, BrickGuardMapManager.EditWorldKind kind, int maxPlayers) {
        if (player == null || !player.isOnline() || plugin.getBrickGuardMapManager() == null) {
            return false;
        }
        BrickGuardMapManager.MapDefinition definition = plugin.getBrickGuardMapManager().ensureMapDefinition(mapId, maxPlayers);
        return enterEditorSession(player, definition, kind == null ? BrickGuardMapManager.EditWorldKind.BRICK : kind);
    }

    public boolean enterEditorSession(Player player, BrickGuardMapManager.MapDefinition definition,
                                      BrickGuardMapManager.EditWorldKind kind) {
        if (player == null || definition == null) {
            return false;
        }
        World world = plugin.getWorldManager().getOrCreateBrickGuardTemplateWorld(definition, kind);
        if (world == null) {
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_failed",
                    Map.of("map", definition.displayName(), "kind", kind.displayName())));
            return false;
        }

        EditorSession old = editorSessions.get(player.getUniqueId());
        EditorSession session = old != null
                ? new EditorSession(definition.mapId(), kind, old.previousLocation(), old.previousGameMode(),
                old.previousAllowFlight(), old.previousFlying(), cloneItems(old.contents()), cloneItems(old.armor()),
                old.offhand() == null ? null : old.offhand().clone(), old.heldSlot())
                : snapshot(player, definition.mapId(), kind);
        editorSessions.put(player.getUniqueId(), session);

        Location target = switch (kind) {
            case LOBBY -> plugin.getBrickGuardMapManager().getLobbySpawn(definition, world);
            case BRICK -> plugin.getBrickGuardMapManager().getBrickSpawn(definition, world);
            case NETHER_BRICK -> plugin.getBrickGuardMapManager().getNetherBrickSpawn(definition, world);
        };
        if (target == null) {
            target = world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        }

        player.teleport(target);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        giveToolkit(player, definition.mapId(), kind);
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_joined",
                Map.of("map", definition.displayName(), "kind", kind.displayName(), "world", world.getName())));
        playOpenEditSound(player);
        return true;
    }

    public void exitEditorSession(Player player, boolean teleportBack) {
        if (player == null) {
            return;
        }
        EditorSession session = editorSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(cloneItems(session.contents()));
        inventory.setArmorContents(cloneItems(session.armor()));
        inventory.setItemInOffHand(session.offhand() == null ? null : session.offhand().clone());
        inventory.setHeldItemSlot(Math.max(0, Math.min(8, session.heldSlot())));
        player.setGameMode(session.previousGameMode());
        player.setAllowFlight(session.previousAllowFlight());
        player.setFlying(session.previousFlying());
        if (teleportBack && session.previousLocation() != null && session.previousLocation().getWorld() != null) {
            player.teleport(session.previousLocation());
        }
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_exit"));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.75f, 1.18f);
    }

    public boolean handleToolkitInteract(PlayerInteractEvent event) {
        if (event == null) {
            return false;
        }
        Player player = event.getPlayer();
        if (!isEditing(player)) {
            return false;
        }
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return false;
        }
        ItemStack item = event.getItem();
        if (!isToolkit(item)) {
            return false;
        }
        event.setCancelled(true);
        openRootMenu(player);
        return true;
    }

    public boolean handleMenuClick(InventoryClickEvent event, Player player) {
        if (!(event.getInventory().getHolder() instanceof BrickGuardEditorMenu menu)) {
            return false;
        }
        menu.handleClick(event);
        return true;
    }

    public boolean shouldProtectEditorInventory(Player player, ItemStack currentItem, ItemStack cursorItem) {
        return isEditing(player) && (isToolkit(currentItem) || isToolkit(cursorItem) || isEditorButton(currentItem) || isEditorButton(cursorItem));
    }

    public boolean handleDrop(Player player, ItemStack item) {
        return isEditing(player) && (isToolkit(item) || isEditorButton(item));
    }

    public void refreshToolkit(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        giveToolkit(player, session.mapId(), session.kind());
    }

    private EditorSession snapshot(Player player, String mapId, BrickGuardMapManager.EditWorldKind kind) {
        PlayerInventory inventory = player.getInventory();
        return new EditorSession(
                mapId,
                kind,
                player.getLocation().clone(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying(),
                cloneItems(inventory.getContents()),
                cloneItems(inventory.getArmorContents()),
                inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone(),
                inventory.getHeldItemSlot()
        );
    }

    private void giveToolkit(Player player, String mapId, BrickGuardMapManager.EditWorldKind kind) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        inventory.setHeldItemSlot(0);
        inventory.setItem(0, createToolkitItem(mapId, kind));
        player.updateInventory();
    }

    private ItemStack createToolkitItem(String mapId, BrickGuardMapManager.EditWorldKind kind) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§x§F§F§7§C§0§0[ 工具包 ]"));
            meta.lore(List.of(
                    LegacyComponentSerializer.legacySection().deserialize("§f- 右键打开地图编辑工具"),
                    LegacyComponentSerializer.legacySection().deserialize("§7- 当前地图: §f" + mapId),
                    LegacyComponentSerializer.legacySection().deserialize("§7- 当前维度: §f" + kind.displayName())
            ));
            meta.getPersistentDataContainer().set(toolkitKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(mapKey, PersistentDataType.STRING, mapId);
            meta.getPersistentDataContainer().set(worldKindKey, PersistentDataType.STRING, kind.id());
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", TOOLKIT_MODEL));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    private boolean isToolkit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(toolkitKey, PersistentDataType.BYTE);
    }

    private boolean isEditorButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING);
    }

    private void openRootMenu(Player player) {
        BrickGuardMapManager.MapDefinition definition = currentDefinition(player);
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (definition == null || session == null) {
            return;
        }
        new BrickGuardEditorMenu(plugin, player, definition, session.kind(), null, 0).open();
    }

    private BrickGuardMapManager.MapDefinition currentDefinition(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null || plugin.getBrickGuardMapManager() == null) {
            return null;
        }
        return plugin.getBrickGuardMapManager().getMapDefinition(session.mapId());
    }

    private ItemStack createActionItem(Material material, String name, List<String> lore,
                                       String action, String mapId, BrickGuardMapManager.EditWorldKind kind, int page) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            meta.lore(lore.stream().map(line -> LegacyComponentSerializer.legacySection().deserialize(line)).toList());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(mapKey, PersistentDataType.STRING, mapId);
            meta.getPersistentDataContainer().set(worldKindKey, PersistentDataType.STRING, kind.id());
            meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String readAction(ItemStack item) {
        if (!isEditorButton(item)) {
            return "";
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(actionKey, PersistentDataType.STRING, "");
    }

    private int readPage(ItemStack item) {
        if (!isEditorButton(item)) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(pageKey, PersistentDataType.INTEGER, 0);
    }

    private void playOpenEditSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.72f, 1.8f);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.52f, 1.55f);
    }

    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.45f, 1.3f);
    }

    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.34f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 1.84f);
    }

    private void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.75f, 1.0f);
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] cloned = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                cloned[i] = source[i].clone();
            }
        }
        return cloned;
    }

    private record EditorSession(String mapId,
                                 BrickGuardMapManager.EditWorldKind kind,
                                 Location previousLocation,
                                 GameMode previousGameMode,
                                 boolean previousAllowFlight,
                                 boolean previousFlying,
                                 ItemStack[] contents,
                                 ItemStack[] armor,
                                 ItemStack offhand,
                                 int heldSlot) {
    }

    private final class BrickGuardEditorMenu extends BaseMenu {

        private final BrickGuardMapManager.MapDefinition definition;
        private final BrickGuardMapManager.EditWorldKind kind;
        private final String parentCategory;
        private final int page;

        private BrickGuardEditorMenu(GameFunXiao plugin, Player player, BrickGuardMapManager.MapDefinition definition,
                                     BrickGuardMapManager.EditWorldKind kind, String parentCategory, int page) {
            super(plugin, player, "§0§l板砖地图编辑", 54);
            this.definition = definition;
            this.kind = kind;
            this.parentCategory = parentCategory;
            this.page = page;
        }

        @Override
        protected void setupItems() {
            fillMenuFrame();
            inventory.setItem(4, createTitleItem(Material.PAPER,
                    "§x§F§F§7§C§0§0板砖守卫战",
                    "§7- 当前地图: §f" + definition.displayName(),
                    "§7- 当前维度: §f" + kind.displayName()));
            if (parentCategory == null) {
                setupRoot();
                inventory.setItem(49, createActionItem(Material.BARRIER, "§x§F§F§8§8§8§8[ 关闭 ]",
                        List.of("§f- 恢复原背包并退出编辑"), "close", definition.mapId(), kind, 0));
            } else {
                setupCategory(parentCategory, page);
                inventory.setItem(49, createActionItem(Material.ARROW, "§x§F§F§D§D§5§5[ 返回 ]",
                        List.of("§f- 返回上一层分类"), "back", definition.mapId(), kind, 0));
            }
        }

        private void fillMenuFrame() {
            ItemStack gray = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8你看我干什么", "§8虽然你点我也没有用ewe");
            ItemStack red = createItem(Material.RED_STAINED_GLASS_PANE, "§x§F§F§7§C§0§0你看我干什么", "§8虽然你点我也没有用ewe");
            for (int slot = 0; slot < 54; slot++) {
                int row = slot / 9;
                int col = slot % 9;
                if (row == 0 || row == 5 || col == 0 || col == 8) {
                    inventory.setItem(slot, gray);
                }
            }
            inventory.setItem(0, red);
            inventory.setItem(8, red);
            inventory.setItem(45, red);
            inventory.setItem(53, red);
        }

        private void setupRoot() {
            List<ItemStack> buttons = new ArrayList<>();
            buttons.add(createActionItem(Material.VILLAGER_SPAWN_EGG, "§x§F§F§7§C§0§0[ 板砖商人 ]",
                    List.of("§f- 编辑板砖村民点位"), "open:" + CATEGORY_VILLAGER, definition.mapId(), kind, 0));
            buttons.add(createActionItem(Material.PIGLIN_SPAWN_EGG, "§x§6§6§1§9§0§0[ 下界商旅 ]",
                    List.of("§f- 编辑下界猪灵点位"), "open:" + CATEGORY_PIGLIN, definition.mapId(), kind, 0));
            buttons.add(createActionItem(Material.DIAMOND_ORE, "§x§5§5§F§F§A§A[ 矿物点位 ]",
                    List.of("§f- 编辑板砖矿物和下界矿物"), "open:" + CATEGORY_RESOURCE, definition.mapId(), kind, 0));
            buttons.add(createActionItem(Material.COMPASS, "§x§F§F§D§D§5§5[ 基础设置 ]",
                    List.of("§f- 出生点、核心、边界、启用状态"), "open:" + CATEGORY_SETTINGS, definition.mapId(), kind, 0));
            placeCentered(buttons, 20, 24);
        }

        private void setupCategory(String category, int pageIndex) {
            List<ItemStack> buttons = switch (category) {
                case CATEGORY_VILLAGER -> villagerButtons();
                case CATEGORY_PIGLIN -> piglinButtons();
                case CATEGORY_RESOURCE -> resourceButtons();
                case CATEGORY_SETTINGS -> settingButtons();
                default -> List.of(createItem(Material.BARRIER, "§c啥也没有", "§8这里还没有可用按钮"));
            };
            placePagedCentered(buttons, pageIndex, 19, 34);
            setupPageButtons(category, buttons.size(), pageIndex);
        }

        private List<ItemStack> villagerButtons() {
            List<String> preview = previewLocations(definition.brickVillagerSpawns(), kind);
            return List.of(
                    createActionItem(Material.EMERALD, "§x§F§F§7§C§0§0[ 添加商人点 ]",
                            List.of("§f- 把当前位置加入板砖商人点位", "§7- 当前数量: §f" + definition.brickVillagerSpawns().size()), "add:brick_villager_spawns", definition.mapId(), kind, 0),
                    createActionItem(Material.IRON_AXE, "§x§F§F§7§C§0§0[ 移除最近点位 ]",
                            mergeLore(List.of("§f- 删除离你最近的板砖商人点位"), preview), "remove:brick_villager_spawns", definition.mapId(), kind, 0),
                    createActionItem(Material.BOOK, "§x§F§F§7§C§0§0[ 点位预览 ]",
                            mergeLore(List.of("§f- 当前已记录 §e" + definition.brickVillagerSpawns().size() + " §f个商人点"), preview), "noop", definition.mapId(), kind, 0),
                    createActionItem(Material.BARRIER, "§x§F§F§7§C§0§0[ 清空商人点 ]",
                            List.of("§f- 清空全部板砖商人点位"), "clear:brick_villager_spawns", definition.mapId(), kind, 0)
            );
        }

        private List<ItemStack> piglinButtons() {
            List<String> preview = previewLocations(definition.netherPiglinSpawns(), kind);
            return List.of(
                    createActionItem(Material.GOLD_INGOT, "§x§6§6§1§9§0§0[ 添加猪灵点 ]",
                            List.of("§f- 把当前位置加入下界猪灵点位", "§7- 当前数量: §f" + definition.netherPiglinSpawns().size()), "add:nether_piglin_spawns", definition.mapId(), kind, 0),
                    createActionItem(Material.NETHERITE_AXE, "§x§6§6§1§9§0§0[ 移除最近点位 ]",
                            mergeLore(List.of("§f- 删除离你最近的下界猪灵点位"), preview), "remove:nether_piglin_spawns", definition.mapId(), kind, 0),
                    createActionItem(Material.BOOK, "§x§6§6§1§9§0§0[ 点位预览 ]",
                            mergeLore(List.of("§f- 当前已记录 §e" + definition.netherPiglinSpawns().size() + " §f个猪灵点"), preview), "noop", definition.mapId(), kind, 0),
                    createActionItem(Material.BARRIER, "§x§6§6§1§9§0§0[ 清空猪灵点 ]",
                            List.of("§f- 清空全部下界猪灵点位"), "clear:nether_piglin_spawns", definition.mapId(), kind, 0)
            );
        }

        private List<ItemStack> resourceButtons() {
            List<String> brickPreview = previewLocations(definition.brickResourceBlocks(), BrickGuardMapManager.EditWorldKind.BRICK);
            List<String> netherPreview = previewLocations(definition.netherResourceBlocks(), BrickGuardMapManager.EditWorldKind.NETHER_BRICK);
            return List.of(
                    createActionItem(Material.BRICKS, "§x§F§F§7§C§0§0[ 板砖矿点 ]",
                            mergeLore(List.of("§f- 左键添加点位", "§f- 右键移除最近点位", "§7- 当前数量: §f" + definition.brickResourceBlocks().size()), brickPreview), "cycle:brick_resource_blocks", definition.mapId(), kind, 0),
                    createActionItem(Material.GLOWSTONE, "§x§6§6§1§9§0§0[ 下界矿点 ]",
                            mergeLore(List.of("§f- 左键添加点位", "§f- 右键移除最近点位", "§7- 当前数量: §f" + definition.netherResourceBlocks().size()), netherPreview), "cycle:nether_resource_blocks", definition.mapId(), kind, 0),
                    createActionItem(Material.BOOK, "§x§F§F§D§D§5§5[ 矿点总览 ]",
                            List.of(
                                    "§f- 板砖矿点: §e" + definition.brickResourceBlocks().size(),
                                    "§f- 下界矿点: §e" + definition.netherResourceBlocks().size(),
                                    "§7- 左右键都在对应按钮上操作"
                            ), "noop", definition.mapId(), kind, 0),
                    createActionItem(Material.BARRIER, "§x§F§F§D§D§5§5[ 清空全部矿点 ]",
                            List.of("§f- 左键清空板砖矿点", "§f- 右键清空下界矿点"), "clear_dual_resource", definition.mapId(), kind, 0)
            );
        }

        private List<ItemStack> settingButtons() {
            BrickGuardMapManager.ValidationResult validation = plugin.getBrickGuardMapManager().validateMap(definition);
            return List.of(
                    createActionItem(Material.RECOVERY_COMPASS, "§x§F§F§D§D§5§5[ 设置出生点 ]",
                            List.of("§f- 当前维度直接写入本维度出生点"), "set_spawn", definition.mapId(), kind, 0),
                    createActionItem(Material.RED_GLAZED_TERRACOTTA, "§x§F§F§7§C§0§0[ 设置板砖核心 ]",
                            List.of("§f- 只在板砖世界可用"), "set_core", definition.mapId(), kind, 0),
                    createActionItem(Material.BEACON, "§x§5§5§F§F§A§A[ 设置伪边界 ]",
                            List.of("§f- 把当前位置设为伪边界中心", "§f- 左键半径 +100", "§f- 右键半径 -100", "§7- 当前半径: §f" + (int) Math.round(definition.fakeBorderRadius())), "border", definition.mapId(), kind, 0),
                    createActionItem(Material.LODESTONE, "§x§5§5§F§F§A§A[ 保存地图 ]",
                            List.of("§f- 保存当前模板世界与配置"), "save", definition.mapId(), kind, 0),
                    createActionItem(definition.allowInGame() ? Material.LIME_DYE : Material.GRAY_DYE,
                            "§x§F§F§D§D§5§5[ 切换允许加入 ]",
                            List.of("§f- 当前: " + (definition.allowInGame() ? "§a允许" : "§c禁止"),
                                    "§f- 若元素不完整会阻止启用"), "toggle_allow", definition.mapId(), kind, 0),
                    createActionItem(Material.MAP, "§x§F§F§D§D§5§5[ 查看校验 ]",
                            validationLore(definition), "validate", definition.mapId(), kind, 0),
                    createActionItem(validation.complete() ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS,
                            "§x§F§F§D§D§5§5[ 当前状态 ]",
                            List.of(
                                    "§f- 允许加入: " + (definition.allowInGame() ? "§a是" : "§c否"),
                                    "§f- 地图完整: " + (validation.complete() ? "§a是" : "§c否"),
                                    "§f- 核心血量: §e" + definition.coreHealth(),
                                    "§f- 时长上限: §e" + definition.gameTimeLimitSeconds() + "s"
                            ), "noop", definition.mapId(), kind, 0),
                    createActionItem(Material.OAK_SIGN, "§x§F§F§D§D§5§5[ 切换维度 ]",
                            List.of("§f- 左键大厅", "§f- 右键板砖", "§f- 掉落键下界砖"), "switch_kind", definition.mapId(), kind, 0)
            );
        }

        private List<String> validationLore(BrickGuardMapManager.MapDefinition definition) {
            BrickGuardMapManager.ValidationResult validation = plugin.getBrickGuardMapManager().validateMap(definition);
            List<String> lore = new ArrayList<>();
            lore.add("§f- 当前状态: " + (validation.complete() ? "§a完整" : "§c缺失"));
            if (validation.complete()) {
                lore.add("§7- 这张地图已经满足开局要求");
            } else {
                for (String missing : validation.missingElements()) {
                    lore.add("§7- 缺少: §f" + missing);
                }
            }
            return lore;
        }

        private void placeCentered(List<ItemStack> items, int start, int end) {
            List<Integer> slots = new ArrayList<>();
            for (int slot = start; slot <= end; slot++) {
                if (slot % 9 != 0 && slot % 9 != 8) {
                    slots.add(slot);
                }
            }
            int size = items.size();
            if (size <= 0 || slots.isEmpty()) {
                return;
            }
            int offset = Math.max(0, (slots.size() - size) / 2);
            for (int i = 0; i < size && i + offset < slots.size(); i++) {
                inventory.setItem(slots.get(i + offset), items.get(i));
            }
        }

        private void placePagedCentered(List<ItemStack> items, int pageIndex, int start, int end) {
            int safePage = Math.max(0, pageIndex);
            int fromIndex = safePage * CATEGORY_PAGE_SIZE;
            if (fromIndex >= items.size()) {
                fromIndex = 0;
            }
            int toIndex = Math.min(items.size(), fromIndex + CATEGORY_PAGE_SIZE);
            placeCentered(items.subList(fromIndex, toIndex), start, end);
        }

        private void setupPageButtons(String category, int totalSize, int pageIndex) {
            int maxPage = Math.max(0, (totalSize - 1) / CATEGORY_PAGE_SIZE);
            if (pageIndex > 0) {
                inventory.setItem(46, createActionItem(Material.ARROW, "§x§F§F§D§D§5§5[ 上一页 ]",
                        List.of("§f- 返回上一页内容"), "page:" + category + ":" + (pageIndex - 1), definition.mapId(), kind, pageIndex - 1));
            }
            if (pageIndex < maxPage) {
                inventory.setItem(52, createActionItem(Material.ARROW, "§x§F§F§D§D§5§5[ 下一页 ]",
                        List.of("§f- 查看下一页内容"), "page:" + category + ":" + (pageIndex + 1), definition.mapId(), kind, pageIndex + 1));
            }
        }

        private List<String> previewLocations(List<BrickGuardMapManager.LocationSpec> specs, BrickGuardMapManager.EditWorldKind expectedKind) {
            if (specs == null || specs.isEmpty()) {
                return List.of("§7- 当前还没有记录点位");
            }
            List<String> lines = new ArrayList<>();
            int count = 0;
            for (BrickGuardMapManager.LocationSpec spec : specs) {
                if (spec == null) {
                    continue;
                }
                boolean sameWorld = expectedKind == null
                        || player.getWorld() == null
                        || spec.worldName() == null
                        || spec.worldName().isBlank()
                        || spec.worldName().equalsIgnoreCase(player.getWorld().getName());
                String color = sameWorld ? "§f" : "§7";
                lines.add(color + "- " + shortLocation(spec));
                count++;
                if (count >= 4) {
                    break;
                }
            }
            if (specs.size() > 4) {
                lines.add("§7- 其余 §f" + (specs.size() - 4) + " §7个点位已省略");
            }
            return lines;
        }

        private String shortLocation(BrickGuardMapManager.LocationSpec spec) {
            return (int) Math.round(spec.x()) + ", "
                    + (int) Math.round(spec.y()) + ", "
                    + (int) Math.round(spec.z());
        }

        private List<String> mergeLore(List<String> header, List<String> preview) {
            List<String> merged = new ArrayList<>(header);
            merged.addAll(preview);
            return merged;
        }

        @Override
        public void handleClick(InventoryClickEvent event) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }
            BrickGuardMapEditorManager.this.playClickSound(player);
            String action = readAction(clicked);
            if (action.isBlank()) {
                return;
            }
            BrickGuardMapManager.MapDefinition latest = plugin.getBrickGuardMapManager().getMapDefinition(definition.mapId());
            if (latest == null) {
                BrickGuardMapEditorManager.this.playErrorSound(player);
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_not_found",
                        Map.of("map", definition.mapId())));
                return;
            }

            if (action.startsWith("open:")) {
                new BrickGuardEditorMenu(plugin, player, latest, kind, action.substring("open:".length()), 0).open();
                return;
            }
            if (action.startsWith("page:")) {
                String[] split = action.split(":", 3);
                String category = split.length >= 2 ? split[1] : CATEGORY_SETTINGS;
                int targetPage = split.length >= 3 ? parsePage(split[2]) : 0;
                new BrickGuardEditorMenu(plugin, player, latest, kind, category, targetPage).open();
                return;
            }
            switch (action) {
                case "back" -> new BrickGuardEditorMenu(plugin, player, latest, kind, null, 0).open();
                case "close" -> {
                    player.closeInventory();
                    exitEditorSession(player, true);
                }
                case "noop" -> {
                }
                case "save" -> saveCurrentWorld(latest);
                case "validate" -> sendValidation(latest);
                case "toggle_allow" -> toggleAllow(latest);
                case "set_spawn" -> setSpawn(latest);
                case "set_core" -> setCore(latest);
                case "border" -> adjustBorder(latest, event.getClick());
                case "switch_kind" -> switchKind(latest, event.getClick());
                case "clear_dual_resource" -> clearDualResource(latest, event.getClick());
                default -> {
                    if (action.startsWith("add:")) {
                        addLocation(latest, action.substring(4));
                    } else if (action.startsWith("remove:")) {
                        removeLocation(latest, action.substring(7));
                    } else if (action.startsWith("clear:")) {
                        clearLocation(latest, action.substring(6));
                    } else if (action.startsWith("cycle:")) {
                        cycleLocation(latest, action.substring(6), event.getClick());
                    }
                }
            }
        }

        private void saveCurrentWorld(BrickGuardMapManager.MapDefinition latest) {
            if (player.getWorld() != null) {
                player.getWorld().save();
            }
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_saved",
                    Map.of("map", latest.displayName(), "world", player.getWorld() == null ? "unknown" : player.getWorld().getName())));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
        }

        private void sendValidation(BrickGuardMapManager.MapDefinition latest) {
            BrickGuardMapManager.ValidationResult validation = plugin.getBrickGuardMapManager().validateMap(latest);
            if (validation.complete()) {
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_validate_ok",
                        Map.of("map", latest.displayName())));
                BrickGuardMapEditorManager.this.playSuccessSound(player);
                return;
            }
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_validate_failed",
                    Map.of("map", latest.displayName(), "missing", String.join("、", validation.missingElements()))));
            BrickGuardMapEditorManager.this.playErrorSound(player);
        }

        private void toggleAllow(BrickGuardMapManager.MapDefinition latest) {
            BrickGuardMapManager.ValidationResult validation = plugin.getBrickGuardMapManager().validateMap(latest);
            if (!latest.allowInGame() && !validation.complete()) {
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_validate_failed",
                        Map.of("map", latest.displayName(), "missing", String.join("、", validation.missingElements()))));
                BrickGuardMapEditorManager.this.playErrorSound(player);
                return;
            }
            plugin.getBrickGuardMapManager().setAllowInGame(latest.mapId(), !latest.allowInGame());
            BrickGuardMapManager.MapDefinition refreshed = plugin.getBrickGuardMapManager().getMapDefinition(latest.mapId());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_allow_in_game_changed",
                    Map.of("map", refreshed.displayName(), "state", refreshed.allowInGame() ? "允许" : "禁止")));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
            new BrickGuardEditorMenu(plugin, player, refreshed, kind, CATEGORY_SETTINGS, 0).open();
        }

        private void setSpawn(BrickGuardMapManager.MapDefinition latest) {
            String key = switch (kind) {
                case LOBBY -> "lobby_spawn";
                case BRICK -> "brick_spawn";
                case NETHER_BRICK -> "nether_brick_spawn";
            };
            plugin.getBrickGuardMapManager().setLocation(latest.mapId(), key, player.getLocation());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_spawn_set",
                    Map.of("map", latest.displayName(), "kind", kind.displayName())));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
        }

        private void setCore(BrickGuardMapManager.MapDefinition latest) {
            if (kind != BrickGuardMapManager.EditWorldKind.BRICK) {
                player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_core_only_brick"));
                BrickGuardMapEditorManager.this.playErrorSound(player);
                return;
            }
            plugin.getBrickGuardMapManager().setLocation(latest.mapId(), "brick_core", player.getLocation());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_core_set",
                    Map.of("map", latest.displayName())));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
        }

        private void adjustBorder(BrickGuardMapManager.MapDefinition latest, ClickType click) {
            double current = latest.fakeBorderRadius();
            if (click.isLeftClick()) {
                current += 100.0D;
            } else if (click.isRightClick()) {
                current = Math.max(100.0D, current - 100.0D);
            }
            plugin.getBrickGuardMapManager().setFakeBorder(latest.mapId(), player.getLocation(), current);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_border_set",
                    Map.of("map", latest.displayName(), "radius", String.valueOf((int) current))));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
        }

        private void switchKind(BrickGuardMapManager.MapDefinition latest, ClickType click) {
            BrickGuardMapManager.EditWorldKind target;
            if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                target = BrickGuardMapManager.EditWorldKind.NETHER_BRICK;
            } else if (click.isRightClick()) {
                target = BrickGuardMapManager.EditWorldKind.BRICK;
            } else {
                target = BrickGuardMapManager.EditWorldKind.LOBBY;
            }
            player.closeInventory();
            enterEditorSession(player, latest, target);
        }

        private void addLocation(BrickGuardMapManager.MapDefinition latest, String key) {
            plugin.getBrickGuardMapManager().addLocationEntry(latest.mapId(), key, player.getLocation());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_point_added",
                    Map.of("map", latest.displayName(), "target", targetName(key))));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
            reopenCategory(latest, key);
        }

        private void removeLocation(BrickGuardMapManager.MapDefinition latest, String key) {
            plugin.getBrickGuardMapManager().removeNearestLocationEntry(latest.mapId(), key, player.getLocation());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_point_removed",
                    Map.of("map", latest.displayName(), "target", targetName(key))));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
            reopenCategory(latest, key);
        }

        private void clearLocation(BrickGuardMapManager.MapDefinition latest, String key) {
            plugin.getBrickGuardMapManager().clearLocationList(latest.mapId(), key);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_point_cleared",
                    Map.of("map", latest.displayName(), "target", targetName(key))));
            BrickGuardMapEditorManager.this.playSuccessSound(player);
            reopenCategory(latest, key);
        }

        private void cycleLocation(BrickGuardMapManager.MapDefinition latest, String key, ClickType click) {
            if (click.isRightClick()) {
                removeLocation(latest, key);
            } else {
                addLocation(latest, key);
            }
        }

        private void clearDualResource(BrickGuardMapManager.MapDefinition latest, ClickType click) {
            String key = click.isRightClick() ? "nether_resource_blocks" : "brick_resource_blocks";
            clearLocation(latest, key);
        }

        private String targetName(String key) {
            return switch (key.toLowerCase(Locale.ROOT)) {
                case "brick_villager_spawns" -> "板砖商人点位";
                case "nether_piglin_spawns" -> "下界猪灵点位";
                case "brick_resource_blocks" -> "板砖矿点";
                case "nether_resource_blocks" -> "下界矿点";
                default -> "点位";
            };
        }

        private void reopenCategory(BrickGuardMapManager.MapDefinition latest, String key) {
            String category = switch (key) {
                case "brick_villager_spawns" -> CATEGORY_VILLAGER;
                case "nether_piglin_spawns" -> CATEGORY_PIGLIN;
                default -> CATEGORY_RESOURCE;
            };
            BrickGuardMapManager.MapDefinition refreshed = plugin.getBrickGuardMapManager().getMapDefinition(latest.mapId());
            new BrickGuardEditorMenu(plugin, player, refreshed, kind, category, 0).open();
        }

        private int parsePage(String value) {
            try {
                return Math.max(0, Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
