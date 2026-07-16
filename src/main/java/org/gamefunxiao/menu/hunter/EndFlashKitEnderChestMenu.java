package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

public class EndFlashKitEnderChestMenu extends BaseMenu {

    private final EndFlashKitManager.Role role;
    private final String kitId;
    private final int backPage;
    private final EndFlashKitDetailMenu returnMenu;
    private int editSize;
    private ItemStack[] contents;

    public EndFlashKitEnderChestMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId, int backPage) {
        this(plugin, player, role, kitId, backPage, null);
    }

    EndFlashKitEnderChestMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId,
                              int backPage, EndFlashKitDetailMenu returnMenu) {
        super(plugin, player, resolveTitle(plugin, kitId), 54);
        this.role = role;
        this.kitId = kitId;
        this.backPage = Math.max(0, backPage);
        this.returnMenu = returnMenu;
    }

    private static String resolveTitle(GameFunXiao plugin, String kitId) {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        return "§0§l" + (kit == null ? "末影箱Kit" : kit.displayName() + "末影箱");
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
        if (contents == null) {
            editSize = kit.enderChestSize();
            contents = kit.enderChestContents();
        }

        for (int slot = 0; slot < 45; slot++) {
            if (slot < editSize) {
                ItemStack item = slot < contents.length ? contents[slot] : null;
                inventory.setItem(slot, item == null ? null : item.clone());
            } else {
                inventory.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE,
                        "§8你看我干什么",
                        "§8虽然你点我也没有用ewe"));
            }
        }
        renderButtons(kit);
    }

    @Override
    public boolean isEditableTopInventorySlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < editSize;
    }

    @Override
    public boolean handlesBottomInventoryClick(InventoryClickEvent event) {
        return true;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) {
            copyClickedPlayerItem(event);
            return;
        }
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (isEditableTopInventorySlot(slot)) {
            return;
        }

        switch (slot) {
            case 45 -> {
                playPageTurnSound();
                if (returnMenu == null) {
                    new EndFlashKitDetailMenu(plugin, player, role, kitId, backPage).open();
                } else {
                    returnMenu.open();
                }
            }
            case 47 -> changeSize(-9);
            case 49 -> saveContents();
            case 51 -> changeSize(9);
            case 53 -> clearContents();
            default -> {
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
        }
    }

    private void renderButtons(EndFlashKitManager.Kit kit) {
        inventory.setItem(45, createBackButton());
        inventory.setItem(46, createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§x§D§D§A§A§F§F点击自己背包物品会复制到上方",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(47, createItem(Material.REDSTONE,
                "   §8[§x§F§F§8§8§8§8末影箱大小 -9§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前: §d" + editSize + " §7格",
                "§f- §7最小: §c" + EndFlashKitManager.MIN_ENDER_CHEST_SIZE + " §7格",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(48, createItem(Material.PINK_STAINED_GLASS_PANE,
                "§x§F§F§C§C§F§F这不是上传按钮",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(49, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A保存末影箱Kit§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a保存上方格子为公共末影箱 Kit",
                "§f- §dKit: §f" + kit.displayName(),
                "§f- §b大小: §e" + editSize + " §7格",
                "§f- §d最后编辑: §f" + kit.lastEditorName() + " §8/ §e" + kit.lastEditedAtText(),
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(50, createItem(Material.BLUE_STAINED_GLASS_PANE,
                "§x§8§8§D§D§F§F可直接摆放和拿开上方物品",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(51, createItem(Material.GLOWSTONE_DUST,
                "   §8[§x§8§8§D§D§F§F末影箱大小 +9§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前: §d" + editSize + " §7格",
                "§f- §7最大: §b" + EndFlashKitManager.MAX_ENDER_CHEST_SIZE + " §7格",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(52, createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§x§B§B§F§F§E§E关闭不会自动保存",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(53, createItem(Material.BARRIER,
                "   §8[§x§F§F§8§8§8§8清空编辑区§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e清空上方末影箱格子",
                "§f- §7清空后还需要点击保存才会生效",
                "§8· · · · · · · · · · · · · ·"));
    }

    private void copyClickedPlayerItem(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ItemStack copy = clicked.clone();
        if (event.isRightClick()) {
            copy.setAmount(1);
        }
        for (int slot = 0; slot < editSize; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(slot, copy);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BUNDLE_INSERT, 0.65f, 1.45f);
                return;
            }
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§D§D§8§8⚠ §e末影箱编辑区已经满了。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.82f);
    }

    private void changeSize(int delta) {
        collectContents();
        int next = EndFlashKitManager.clampEnderChestSize(editSize + delta);
        if (next == editSize) {
            playWarningSound();
            return;
        }
        boolean expanded = next > editSize;
        ItemStack[] resized = new ItemStack[next];
        for (int i = 0; i < resized.length && i < contents.length; i++) {
            resized[i] = contents[i] == null ? null : contents[i].clone();
        }
        editSize = next;
        contents = resized;
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.68f, expanded ? 1.45f : 0.85f);
        setupItems();
    }

    private void saveContents() {
        collectContents();
        if (!plugin.getEndFlashKitManager().replaceEnderChestLayout(kitId, contents, editSize, player)) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c保存失败，这个 Kit 可能已经不存在。");
            return;
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已保存这个 Kit 的公共末影箱，大小 §e" + editSize + " §a格。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.82f, 1.25f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.42f, 1.25f);
    }

    private void clearContents() {
        contents = new ItemStack[editSize];
        setupItems();
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§D§D§8§8⌑ §e已清空编辑区，点击保存后才会写入。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.72f, 0.88f);
    }

    private void collectContents() {
        ItemStack[] next = new ItemStack[editSize];
        for (int slot = 0; slot < editSize; slot++) {
            ItemStack item = inventory.getItem(slot);
            next[slot] = item == null || item.getType() == Material.AIR ? null : item.clone();
        }
        contents = next;
    }
}
