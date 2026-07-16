package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EndFlashKitDetailMenu extends BaseMenu {

    private static final Map<UUID, EndFlashKitDetailMenu> WAITING_RENAME_INPUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, EndFlashKitDetailMenu> WAITING_GUIDE_INPUTS = new ConcurrentHashMap<>();
    private static final int RENAME_SLOT = 46;
    private static final int SAVE_SLOT = 47;
    private static final int ENDER_SLOT = 48;
    private static final int IMPORT_SLOT = 49;
    private static final int GUIDE_SLOT = 50;
    private static final int CHANCE_SLOT = 51;
    private static final int START_EXP_SLOT = 52;
    private static final int DELETE_SLOT = 53;

    private final EndFlashKitManager.Role role;
    private final String kitId;
    private final int backPage;

    private ItemStack[] storageContents;
    private ItemStack[] armorContents;
    private ItemStack offHandItem;
    private boolean importedButNotSaved;

    public EndFlashKitDetailMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId, int backPage) {
        super(plugin, player, resolveTitle(plugin, kitId), 54);
        this.role = role;
        this.kitId = kitId;
        this.backPage = Math.max(0, backPage);
    }

    EndFlashKitDetailMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId, int backPage,
                          ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offHandItem,
                          boolean importedButNotSaved) {
        this(plugin, player, role, kitId, backPage);
        EndFlashKitManager.KitLayout layout = new EndFlashKitManager.KitLayout(storageContents, armorContents, offHandItem);
        this.storageContents = layout.storageContents();
        this.armorContents = layout.armorContents();
        this.offHandItem = layout.offHandItem();
        this.importedButNotSaved = importedButNotSaved;
    }

    private static String resolveTitle(GameFunXiao plugin, String kitId) {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        return "§0§l" + (kit == null ? "Kit不存在" : kit.displayName());
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        if (kit == null) {
            inventory.setItem(22, createItem(Material.BARRIER,
                    "   §8[§cKit不存在§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c这个 Kit 可能已经被删除",
                    "§8· · · · · · · · · · · · · ·"));
            inventory.setItem(45, createBackButton());
            return;
        }

        if (storageContents == null || armorContents == null) {
            loadLayoutFromKit(kit);
        }

        renderStorageRows();
        renderArmorAndOffhandRow();
        renderButtons(kit);
    }

    private void loadLayoutFromKit(EndFlashKitManager.Kit kit) {
        storageContents = kit.storageContents();
        armorContents = kit.armorContents();
        offHandItem = kit.offHandItem();
        importedButNotSaved = false;
    }

    private void importCurrentPlayerLayout() {
        EndFlashKitManager.KitLayout layout = plugin.getEndFlashKitManager().snapshotInventoryLayout(player);
        storageContents = layout.storageContents();
        armorContents = layout.armorContents();
        offHandItem = layout.offHandItem();
        importedButNotSaved = true;
        playSelectSound();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.65f, 1.35f);
        setupItems();
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§8§8§D§D§F§F✦ §b已导入你当前的快捷栏、背包、盔甲和副手，确认没问题后点保存。");
    }

    private void renderStorageRows() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = slot < storageContents.length ? storageContents[slot] : null;
            inventory.setItem(slot, cloneOrNull(item));
        }
    }

    private void renderArmorAndOffhandRow() {
        inventory.setItem(36, cloneOrNull(getArmorItem(3)));
        inventory.setItem(37, cloneOrNull(getArmorItem(2)));
        inventory.setItem(38, cloneOrNull(getArmorItem(1)));
        inventory.setItem(39, cloneOrNull(getArmorItem(0)));
        inventory.setItem(40, cloneOrNull(offHandItem));
        for (int slot = 41; slot <= 44; slot++) {
            inventory.setItem(slot, createGlassPane());
        }
    }

    private void renderButtons(EndFlashKitManager.Kit kit) {
        inventory.setItem(45, createBackButton());
        inventory.setItem(RENAME_SLOT, createRenameButton(kit));
        inventory.setItem(SAVE_SLOT, createSaveButton(kit));
        inventory.setItem(ENDER_SLOT, createEnderChestButton(kit));
        inventory.setItem(IMPORT_SLOT, createImportButton());
        inventory.setItem(GUIDE_SLOT, createGuideButton(kit));
        inventory.setItem(CHANCE_SLOT, createChanceButton(kit));
        inventory.setItem(START_EXP_SLOT, createStartExpButton(kit));
        inventory.setItem(DELETE_SLOT, createDeleteButton());
    }

    private ItemStack getArmorItem(int index) {
        if (armorContents == null || index < 0 || index >= armorContents.length) {
            return null;
        }
        return armorContents[index];
    }

    private ItemStack cloneOrNull(ItemStack source) {
        return source == null || source.getType() == Material.AIR ? null : source.clone();
    }

    private ItemStack createGlassPane() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE,
                "§8你看我干什么",
                "§8虽然你点我也没有用ewe");
    }

    private ItemStack createRenameButton(EndFlashKitManager.Kit kit) {
        String currentName = kit == null ? "未知Kit" : kit.displayName();
        return createItem(Material.NAME_TAG,
                "   §8[§x§D§2§A§6§F§4修改名字§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §d当前名字: §r" + currentName,
                "§f- §b点击后关闭菜单，在聊天框输入新名字",
                "§f- §7输入 §ecancel §7或蹲下取消",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击修改");
    }

    private ItemStack createSaveButton(EndFlashKitManager.Kit kit) {
        return createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A保存Kit§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a保存当前界面里的快捷栏、背包、盔甲和副手",
                importedButNotSaved ? "§f- §e状态: §6已修改，等待保存" : "§f- §e状态: §a已显示当前 Kit",
                kit == null ? "§f- §b创建者: §7未知" : "§f- §b创建者: §a" + kit.creatorName() + " §8/ §7" + kit.createdAtText(),
                kit == null ? "§f- §d最后编辑: §7未知" : "§f- §d最后编辑: §f" + kit.lastEditorName() + " §8/ §e" + kit.lastEditedAtText(),
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击保存");
    }

    private ItemStack createImportButton() {
        return createItem(Material.CHEST,
                "   §8[§x§8§8§D§D§F§F导入当前背包§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b导入你身上的快捷栏、背包、盔甲和副手",
                "§f- §7导入后不会立刻覆盖 Kit，需要再点保存",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e点击导入");
    }

    private ItemStack createEnderChestButton(EndFlashKitManager.Kit kit) {
        int size = kit == null ? EndFlashKitManager.DEFAULT_ENDER_CHEST_SIZE : kit.enderChestSize();
        int count = kit == null ? 0 : kit.enderChestItems().size();
        return createItem(Material.ENDER_CHEST,
                "   §8[§x§B§B§8§8§F§F末§x§D§D§A§A§F§F影§x§F§F§D§D§A§A箱Kit§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §d当前大小: §e" + size + " §7格",
                "§f- §b当前物品: §a" + count + " §7件",
                "§f- §7点击进入末影箱编辑，不是上传末影箱",
                "§f- §a管理员可以直接把物品放到上方格子",
                "§8· · · · · · · · · · · · · ·",
                "§f- §d点击编辑末影箱");
    }

    private ItemStack createGuideButton(EndFlashKitManager.Kit kit) {
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        if (kit == null || !kit.hasGuide()) {
            lore.add("§f- §d当前教学: §7无");
        } else {
            lore.add("§f- §d当前教学: §a已设置");
            int index = 1;
            for (String line : kit.guideLines()) {
                lore.add("§f- §7" + index++ + ". §f" + trimPreview(line, 28));
                if (index > 3) {
                    break;
                }
            }
        }
        lore.add("§f- §b开局分到这个 Kit 时会自动发给玩家");
        lore.add("§f- §7聊天输入时用 §e| §7可以分成多行");
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §a左键设置  §c右键清空");
        return createItem(Material.WRITABLE_BOOK,
                "   §8[§x§B§B§8§8§F§F开§x§D§D§A§A§F§F局§x§F§F§D§D§A§A教学§8]",
                lore);
    }

    private ItemStack createChanceButton(EndFlashKitManager.Kit kit) {
        double current = kit == null ? 0.0D : kit.chance();
        return createItem(Material.AMETHYST_SHARD,
                "   §8[§x§F§F§D§D§8§8权重§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前: §d" + EndFlashKitManager.formatChance(current) + " §7/ §d50",
                "§f- §b实际: §a" + actualChanceText(kit),
                "§f- §a左键 +0.5  §c右键 -0.5",
                "§f- §bShift左键 +5  §6Shift右键 -5",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createStartExpButton(EndFlashKitManager.Kit kit) {
        int current = kit == null ? 0 : kit.startExpLevel();
        return createItem(Material.EXPERIENCE_BOTTLE,
                "   §8[§x§8§8§F§F§B§B开§x§A§A§F§F§C§C局§x§C§C§F§F§D§D经验§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b当前开局经验: §e" + current + " §7级",
                "§f- §7玩家正式开局后会恢复到这个等级",
                "§f- §a左键 +1  §c右键 -1",
                "§f- §bShift左键 +10  §6Shift右键 -10",
                "§f- §8范围: §70 - 100",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createDeleteButton() {
        return createItem(Material.TNT,
                "   §8[§x§F§F§5§5§5§5删除Kit§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §cShift + 点击 删除这个 Kit",
                "§f- §7普通点击只会提示，不会误删",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public boolean isEditableTopInventorySlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot <= 40;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (slot == 45) {
            playClickSound();
            new EndFlashKitRoleMenu(plugin, player, role, backPage).open();
            return;
        }

        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        if (kit == null) {
            playErrorSound();
            new EndFlashKitRoleMenu(plugin, player, role, backPage).open();
            return;
        }

        collectEditedLayout();

        switch (slot) {
            case RENAME_SLOT -> startRenameInput(kit);
            case SAVE_SLOT -> openSaveConfirmMenu();
            case ENDER_SLOT -> {
                playClickSound();
                new EndFlashKitEnderChestMenu(plugin, player, role, kit.id(), backPage, this).open();
            }
            case IMPORT_SLOT -> importCurrentPlayerLayout();
            case GUIDE_SLOT -> {
                if (event.isRightClick()) {
                    clearGuide(kit);
                } else {
                    startGuideInput(kit);
                }
            }
            case CHANCE_SLOT -> changeChance(kit, event.isShiftClick() ? (event.isRightClick() ? -5.0D : 5.0D) : (event.isRightClick() ? -0.5D : 0.5D));
            case START_EXP_SLOT -> changeStartExp(kit, event.isShiftClick() ? (event.isRightClick() ? -10 : 10) : (event.isRightClick() ? -1 : 1));
            case DELETE_SLOT -> deleteKit(event.isShiftClick());
            default -> {
            }
        }
    }

    private void openSaveConfirmMenu() {
        collectEditedLayout();
        playClickSound();
        new EndFlashKitSaveConfirmMenu(plugin, player, role, kitId, backPage,
                storageContents, armorContents, offHandItem, importedButNotSaved).open();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
        }
    }

    private void collectEditedLayout() {
        ItemStack[] nextStorage = new ItemStack[36];
        for (int slot = 0; slot < nextStorage.length; slot++) {
            nextStorage[slot] = cloneOrNull(inventory.getItem(slot));
        }

        ItemStack[] nextArmor = new ItemStack[4];
        nextArmor[3] = cloneOrNull(inventory.getItem(36));
        nextArmor[2] = cloneOrNull(inventory.getItem(37));
        nextArmor[1] = cloneOrNull(inventory.getItem(38));
        nextArmor[0] = cloneOrNull(inventory.getItem(39));
        ItemStack nextOffHand = cloneOrNull(inventory.getItem(40));

        EndFlashKitManager.KitLayout next = new EndFlashKitManager.KitLayout(nextStorage, nextArmor, nextOffHand);
        if (!Arrays.equals(storageContents, next.storageContents())
                || !Arrays.equals(armorContents, next.armorContents())
                || !java.util.Objects.equals(offHandItem, next.offHandItem())) {
            importedButNotSaved = true;
        }
        storageContents = next.storageContents();
        armorContents = next.armorContents();
        offHandItem = next.offHandItem();
    }

    private void reopenWithCurrentLayout(Player target) {
        new EndFlashKitDetailMenu(plugin, target, role, kitId, backPage,
                storageContents, armorContents, offHandItem, importedButNotSaved).open();
    }

    private void startRenameInput(EndFlashKitManager.Kit kit) {
        WAITING_GUIDE_INPUTS.remove(player.getUniqueId());
        WAITING_RENAME_INPUTS.put(player.getUniqueId(), this);
        player.closeInventory();
        applyInputState(player);
        player.sendTitle("§x§D§2§A§6§F§4✎ 请输入Kit新名字", "§7当前: §r" + kit.displayName() + " §8| §7输入 §ecancel §7或蹲下取消", 5, 100, 10);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§D§2§A§6§F§4✎ §d请在聊天框输入这个终章闪光 Kit 的新名字，§7输入 §ecancel §7取消。");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.68f, 1.55f);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.45f, 1.35f);

        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            EndFlashKitDetailMenu menu = WAITING_RENAME_INPUTS.get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (menu != this || online == null || !online.isOnline()) {
                return;
            }
            WAITING_RENAME_INPUTS.remove(uuid);
            clearInputState(online);
            online.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⌛ §e终章闪光 Kit 改名输入超时，已经取消。");
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.78f);
            reopenWithCurrentLayout(online);
        }, 30 * 20L);
    }

    private void startGuideInput(EndFlashKitManager.Kit kit) {
        WAITING_RENAME_INPUTS.remove(player.getUniqueId());
        WAITING_GUIDE_INPUTS.put(player.getUniqueId(), this);
        player.closeInventory();
        applyInputState(player);
        String current = kit.hasGuide() ? trimPreview(kit.guide(), 34) : "无";
        player.sendTitle("§x§B§B§8§8§F§F✦ 请输入开局教学", "§7当前: §f" + current + " §8| §7输入 §ecancel §7或蹲下取消", 5, 100, 10);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§B§B§8§8§F§F✦ §d请在聊天框输入这个 Kit 的开局教学，§7输入 §e无 §7可清空，输入 §ecancel §7取消。");
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§8§8§D§D§F§F» §b多行提示可以用 §e| §b隔开，例如：§f先蓄力|再用弩剑追击|注意保命");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.62f, 1.28f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.55f, 1.72f);

        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            EndFlashKitDetailMenu menu = WAITING_GUIDE_INPUTS.get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (menu != this || online == null || !online.isOnline()) {
                return;
            }
            WAITING_GUIDE_INPUTS.remove(uuid);
            clearInputState(online);
            online.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⌛ §e终章闪光 Kit 教学输入超时，已经取消。");
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.78f);
            reopenWithCurrentLayout(online);
        }, 30 * 20L);
    }

    private void clearGuide(EndFlashKitManager.Kit kit) {
        if (!plugin.getEndFlashKitManager().setGuide(kit.id(), "", player)) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c清空失败，这个 Kit 可能已经不存在。");
            return;
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§B§B§6§6⌑ §e已把 §f" + kit.displayName() + " §e的开局教学设置为 §7无§e。");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.58f, 0.82f);
        setupItems();
    }

    private void changeChance(EndFlashKitManager.Kit kit, double delta) {
        double next = EndFlashKitManager.clampChance(kit.chance() + delta);
        plugin.getEndFlashKitManager().setChance(kit.id(), next, player);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§D§D§A§A§F§F✦ §d已把 Kit §f" + kit.displayName() + " §d权重调整为 §e" + EndFlashKitManager.formatChance(next) + "§d。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, delta > 0 ? 1.55f : 0.85f);
        setupItems();
    }

    private void changeStartExp(EndFlashKitManager.Kit kit, int delta) {
        int next = EndFlashKitManager.clampStartExpLevel(kit.startExpLevel() + delta);
        if (!plugin.getEndFlashKitManager().setStartExpLevel(kit.id(), next, player)) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c开局经验修改失败，这个 Kit 可能已经不存在。");
            return;
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§8§8§F§F§B§B✦ §b已把 Kit §f" + kit.displayName() + " §b开局经验调整为 §e" + next + " §b级。");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.72f, delta > 0 ? 1.65f : 0.92f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.38f, 1.78f);
        setupItems();
    }

    private void deleteKit(boolean shiftClick) {
        if (!shiftClick) {
            playWarningSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⚠ §e如果确定删除，请按住 Shift 再点击删除按钮。");
            return;
        }
        String removed = kitId;
        if (!plugin.getEndFlashKitManager().removeKit(kitId)) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§8§8§8§8⚠ §cKit 已不存在。");
            new EndFlashKitRoleMenu(plugin, player, role, backPage).open();
            return;
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§F§F§5§5§5§5✘ §c已删除终章闪光 Kit：§e" + removed);
        playDeleteSound();
        new EndFlashKitRoleMenu(plugin, player, role, backPage).open();
    }

    private String actualChanceText(EndFlashKitManager.Kit kit) {
        if (kit == null) {
            return "0.0%";
        }
        List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
        double total = kits.stream().mapToDouble(EndFlashKitManager.Kit::chance).sum();
        double balanced = total <= 0
                ? 100.0D / Math.max(1, kits.size())
                : kit.chance() * 100.0D / total;
        return String.format(Locale.ROOT, "%.1f", balanced) + "%";
    }

    private static String trimPreview(String text, int maxLength) {
        String value = text == null ? "" : text.replace('\n', ' ').replace('\r', ' ').trim();
        int safeMax = Math.max(4, maxLength);
        return value.length() <= safeMax ? value : value.substring(0, safeMax - 1) + "…";
    }

    public static boolean isWaitingRenameInput(UUID uuid) {
        return uuid != null && WAITING_RENAME_INPUTS.containsKey(uuid);
    }

    public static boolean isWaitingGuideInput(UUID uuid) {
        return uuid != null && WAITING_GUIDE_INPUTS.containsKey(uuid);
    }

    public static boolean handleRenameChatInput(GameFunXiao plugin, Player player, String input) {
        EndFlashKitDetailMenu menu = WAITING_RENAME_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }

        clearInputState(player);
        String rawName = input == null ? "" : input.trim();
        if (rawName.equalsIgnoreCase("cancel") || rawName.equalsIgnoreCase("取消")) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⌑ §e已取消修改终章闪光 Kit 名字。");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.82f);
            menu.reopenWithCurrentLayout(player);
            return true;
        }

        String newName = rawName.replace('&', '§').replace('\n', ' ').replace('\r', ' ').trim();
        if (newName.isBlank()) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c名字不能为空，已经取消本次修改。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.72f, 0.82f);
            menu.reopenWithCurrentLayout(player);
            return true;
        }
        if (newName.length() > 48) {
            newName = newName.substring(0, 48);
        }

        if (!plugin.getEndFlashKitManager().setDisplayName(menu.kitId, newName, player)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c修改失败，这个 Kit 可能已经不存在。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.72f, 0.82f);
            new EndFlashKitRoleMenu(plugin, player, menu.role, menu.backPage).open();
            return true;
        }

        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已把终章闪光 Kit 名字修改为：§r" + newName);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72f, 1.65f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.42f, 1.35f);
        menu.reopenWithCurrentLayout(player);
        return true;
    }

    public static boolean handleGuideChatInput(GameFunXiao plugin, Player player, String input) {
        EndFlashKitDetailMenu menu = WAITING_GUIDE_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }

        clearInputState(player);
        String rawGuide = input == null ? "" : input.trim();
        if (rawGuide.equalsIgnoreCase("cancel") || rawGuide.equalsIgnoreCase("取消")) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⌑ §e已取消修改终章闪光 Kit 开局教学。");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.82f);
            menu.reopenWithCurrentLayout(player);
            return true;
        }

        String guide = rawGuide.replace('&', '§')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        boolean clear = guide.equalsIgnoreCase("无")
                || guide.equalsIgnoreCase("none")
                || guide.equalsIgnoreCase("clear")
                || guide.equalsIgnoreCase("清空");
        if (!clear && guide.isBlank()) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c教学内容不能为空，想清空请输入 §e无§c。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.72f, 0.82f);
            menu.reopenWithCurrentLayout(player);
            return true;
        }
        if (!clear && guide.length() > 500) {
            guide = guide.substring(0, 500).trim();
        }

        if (!plugin.getEndFlashKitManager().setGuide(menu.kitId, clear ? "" : guide, player)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c修改失败，这个 Kit 可能已经不存在。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.72f, 0.82f);
            new EndFlashKitRoleMenu(plugin, player, menu.role, menu.backPage).open();
            return true;
        }

        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + (clear
                ? "§x§F§F§B§B§6§6⌑ §e已把终章闪光 Kit 开局教学设置为 §7无§e。"
                : "§x§5§5§F§F§A§A✔ §a已保存这个终章闪光 Kit 的开局教学。"));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.62f, 1.45f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.62f, 1.65f);
        menu.reopenWithCurrentLayout(player);
        return true;
    }

    public static boolean cancelRenameInput(GameFunXiao plugin, Player player, boolean reopenMenu) {
        EndFlashKitDetailMenu menu = WAITING_RENAME_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }
        clearInputState(player);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§B§B§6§6⌑ §e已取消修改终章闪光 Kit 名字。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.82f);
        if (reopenMenu) {
            menu.reopenWithCurrentLayout(player);
        }
        return true;
    }

    public static boolean cancelGuideInput(GameFunXiao plugin, Player player, boolean reopenMenu) {
        EndFlashKitDetailMenu menu = WAITING_GUIDE_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }
        clearInputState(player);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§B§B§6§6⌑ §e已取消修改终章闪光 Kit 开局教学。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.78f, 0.82f);
        if (reopenMenu) {
            menu.reopenWithCurrentLayout(player);
        }
        return true;
    }

    private static void applyInputState(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30 * 20, 6, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30 * 20, 0, false, false, false));
    }

    private static void clearInputState(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.resetTitle();
    }
}
