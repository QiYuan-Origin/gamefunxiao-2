package org.gamefunxiao.menu.flash;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RailgunTargetMenu extends BaseMenu {

    private final int level;
    private final String railgunId;
    private final UUID worldId;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final Map<Integer, ChunkTarget> targets = new HashMap<>();

    public RailgunTargetMenu(GameFunXiao plugin, Player player, int level, String railgunId,
                             UUID worldId, int centerChunkX, int centerChunkZ) {
        super(plugin, player, "§8轨道炮 §7| §c区块锁定", 45);
        this.level = Math.max(1, Math.min(3, level));
        this.railgunId = railgunId;
        this.worldId = worldId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        targets.clear();

        ItemStack filler = createMenuItem(Material.BLACK_STAINED_GLASS_PANE,
                "§8超出当前锁定范围",
                "§7升级轨道炮可以扩大可选区块。 ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        int rows = level == 1 ? 3 : 5;
        int columns = level == 1 ? 3 : level == 2 ? 5 : 9;
        int firstRow = (5 - rows) / 2;
        int firstColumn = (9 - columns) / 2;
        int centerRow = rows / 2;
        int centerColumn = columns / 2;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int offsetX = column - centerColumn;
                int offsetZ = row - centerRow;
                int chunkX = centerChunkX + offsetX;
                int chunkZ = centerChunkZ + offsetZ;
                int slot = (firstRow + row) * 9 + firstColumn + column;
                boolean current = offsetX == 0 && offsetZ == 0;
                targets.put(slot, new ChunkTarget(chunkX, chunkZ));
                inventory.setItem(slot, createTargetItem(current, offsetX, offsetZ, chunkX, chunkZ));
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getWhoClicked() != player || event.getRawSlot() < 0 || event.getRawSlot() >= size) {
            return;
        }
        ChunkTarget target = targets.get(event.getRawSlot());
        if (target == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.48F, 0.72F);
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.78F, 0.62F);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.58F, 1.22F);
        player.closeInventory();
        plugin.getFlashModeManager().fireRailgunAtChunk(
                player, railgunId, level, worldId, target.chunkX(), target.chunkZ());
    }

    @Override
    protected void playOpenSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.72F, 0.52F);
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.46F, 0.74F);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.34F, 1.45F);
    }

    private ItemStack createTargetItem(boolean current, int offsetX, int offsetZ, int chunkX, int chunkZ) {
        String direction = current ? "当前位置" : directionName(offsetX, offsetZ);
        Material material = current ? Material.RECOVERY_COMPASS : Material.TNT;
        String name = current
                ? "§x§5§5§F§F§A§A◆ §a当前位置 §8| §f" + chunkX + "§8, §f" + chunkZ
                : "§x§F§F§5§5§5§5◇ §c" + direction + " §8| §f" + chunkX + "§8, §f" + chunkZ;
        List<String> lore = new ArrayList<>();
        lore.add("§8区块中心：§f" + (chunkX * 16 + 8) + "§8, §f" + (chunkZ * 16 + 8));
        lore.add("§8相对距离：§f" + Math.abs(offsetX) + "格横向 §8/ §f" + Math.abs(offsetZ) + "格纵向");
        lore.add("");
        lore.add("§x§F§F§8§8§5§5左键或右键 §7确认轨道炮落点");
        return createMenuItem(material, name, lore.toArray(String[]::new));
    }

    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String directionName(int offsetX, int offsetZ) {
        String vertical = offsetZ < 0 ? "北" : offsetZ > 0 ? "南" : "";
        String horizontal = offsetX < 0 ? "西" : offsetX > 0 ? "东" : "";
        return vertical + horizontal + " " + Math.max(Math.abs(offsetX), Math.abs(offsetZ)) + "区块";
    }

    private record ChunkTarget(int chunkX, int chunkZ) {
    }
}
