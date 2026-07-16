package org.gamefunxiao.menu.base;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation"})
public abstract class BaseMenu implements InventoryHolder {

    protected final GameFunXiao plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final String title;
    protected final int size;

    public BaseMenu(GameFunXiao plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.size = size;

        // 如果配置开启，在标题后添加版本号
        String finalTitle = title;
        if (plugin.getConfigManager().isMenuVersionEnabled()) {
            String version = plugin.getConfigManager().getMenuVersion();
            finalTitle = title + " §8- §7" + version;
        }

        this.inventory = Bukkit.createInventory(this, size, finalTitle);
    }

    public void open() {
        setupItems();
        // 注册到MenuManager
        plugin.getMenuManager().registerMenu(player, this);
        player.openInventory(inventory);
        playOpenSound();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected abstract void setupItems();

    public abstract void handleClick(InventoryClickEvent event);

    public boolean isEditableTopInventorySlot(int rawSlot) {
        return false;
    }

    public boolean allowsTopInventoryClick(InventoryClickEvent event) {
        if (event == null || event.getClickedInventory() == null) {
            return false;
        }
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return false;
        }
        if (!isEditableTopInventorySlot(event.getRawSlot())) {
            return false;
        }

        return switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                    PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR,
                    MOVE_TO_OTHER_INVENTORY -> true;
            default -> false;
        };
    }

    public boolean handlesBottomInventoryClick(InventoryClickEvent event) {
        return false;
    }

    public boolean allowsInventoryDrag(InventoryDragEvent event) {
        if (event == null || event.getRawSlots().isEmpty()) {
            return false;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize || !isEditableTopInventorySlot(rawSlot)) {
                return false;
            }
        }
        return true;
    }

    public void onClose(InventoryCloseEvent event) {
    }

    // 米饭风格边框
    protected void fillMiFanBorder() {
        ItemStack border = createBorderItem();

        // 第一行
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }

        // 最后一行
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, border);
        }

        // 左右两列
        for (int i = 9; i < size - 9; i += 9) {
            inventory.setItem(i, border);
            if (i + 8 < size) {
                inventory.setItem(i + 8, border);
            }
        }
    }

    protected ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§7分割板§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§7哎呀,不要随便戳人家啦");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createBackButton() {
        return createItem(Material.BARRIER,
            "   §8[§c返回§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §7点击返回上一级",
            "§8· · · · · · · · · · · · · ·");
    }

    protected ItemStack createCloseButton() {
        if (plugin.getConfigManager().shouldCloseButtonUseReturnMode()) {
            return createItem(Material.OAK_DOOR,
                "   §8[§x§5§5§F§F§D§D↩ §x§7§7§F§F§E§E返§x§9§9§F§F§F§F回§x§B§B§E§E§F§F主§x§D§D§D§D§F§F菜§x§F§F§C§C§F§F单§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击返回服务器主菜单",
                "§f- §7适合从服务器菜单进入时使用",
                "§8· · · · · · · · · · · · · ·");
        }
        return createPlainCloseButton();
    }

    protected ItemStack createPlainCloseButton() {
        return createItem(Material.BARRIER,
            "   §8[§c关闭§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §7点击关闭菜单",
            "§8· · · · · · · · · · · · · ·");
    }

    protected ItemStack createNextPageButton() {
        return createItem(Material.ARROW,
            "   §8[§a下一页 →§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §7查看下一页内容",
            "§8· · · · · · · · · · · · · ·");
    }

    protected ItemStack createPreviousPageButton() {
        return createItem(Material.ARROW,
            "   §8[§a← 上一页§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §7返回上一页",
            "§8· · · · · · · · · · · · · ·");
    }

    protected ItemStack createTitleItem(Material material, String name, String... lore) {
        return createItem(material, name, lore);
    }

    // 音效系统
    protected void playClickSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    protected void playSuccessSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    protected void playErrorSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    protected void playPageTurnSound() {
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    protected void playOpenSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    protected void playCloseSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
    }

    protected void playSelectSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
    }

    protected void playConfirmSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    protected void playCancelSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
    }

    protected void playWarningSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
    }

    protected void playDeleteSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
    }

    protected void playCreateSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public Player getPlayer() {
        return player;
    }

    protected void handleCloseButtonAction() {
        if (plugin.getConfigManager().shouldCloseButtonUseReturnMode()) {
            playClickSound();
            player.closeInventory();
            String command = plugin.getConfigManager().getCloseButtonReturnCommand();
            if (command != null && !command.isBlank()) {
                String normalized = command.startsWith("/") ? command.substring(1) : command;
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(normalized));
            }
            return;
        }

        playCloseSound();
        player.closeInventory();
    }

    protected void handlePlainCloseAction() {
        playCloseSound();
        player.closeInventory();
    }
}
