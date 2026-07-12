package org.gamefunxiao.menu.flash;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
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
    private final BlockFace facing;
    private final Map<Integer, ChunkTarget> targets = new HashMap<>();

    public RailgunTargetMenu(GameFunXiao plugin, Player player, int level, String railgunId,
                             UUID worldId, int centerChunkX, int centerChunkZ, BlockFace facing) {
        super(plugin, player, "§8轨道炮 §7| §c区块锁定", 45);
        this.level = Math.max(1, Math.min(3, level));
        this.railgunId = railgunId;
        this.worldId = worldId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.facing = normalizeFacing(facing);
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
                int relativeRight = column - centerColumn;
                int relativeForward = centerRow - row;
                int[] worldOffset = rotateOffset(relativeRight, relativeForward);
                int offsetX = worldOffset[0];
                int offsetZ = worldOffset[1];
                int chunkX = centerChunkX + offsetX;
                int chunkZ = centerChunkZ + offsetZ;
                int slot = (firstRow + row) * 9 + firstColumn + column;
                boolean current = relativeRight == 0 && relativeForward == 0;
                targets.put(slot, new ChunkTarget(chunkX, chunkZ));
                inventory.setItem(slot, createTargetItem(
                        current, relativeRight, relativeForward, chunkX, chunkZ));
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

    private ItemStack createTargetItem(boolean current, int relativeRight, int relativeForward,
                                       int chunkX, int chunkZ) {
        String direction = current ? "当前位置" : directionName(relativeRight, relativeForward);
        Material material = current ? Material.YELLOW_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE;
        String name = current
                ? "§x§F§F§E§0§5§5◆ §e当前位置 §8| §f" + chunkX + "§8, §f" + chunkZ
                : "§x§5§5§F§F§8§8◇ §a" + direction + " §8| §f" + chunkX + "§8, §f" + chunkZ;
        List<String> lore = new ArrayList<>();
        lore.add("§8菜单上方：§f" + facingName() + " §7(你的正前方)");
        lore.add("§8区块中心：§f" + (chunkX * 16 + 8) + "§8, §f" + (chunkZ * 16 + 8));
        lore.add("§8相对距离：§f" + Math.abs(relativeRight) + "格左右 §8/ §f"
                + Math.abs(relativeForward) + "格前后");
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

    private int[] rotateOffset(int right, int forward) {
        return switch (facing) {
            case EAST -> new int[]{forward, right};
            case SOUTH -> new int[]{-right, forward};
            case WEST -> new int[]{-forward, -right};
            default -> new int[]{right, -forward};
        };
    }

    private BlockFace normalizeFacing(BlockFace direction) {
        return switch (direction) {
            case EAST, SOUTH, WEST -> direction;
            default -> BlockFace.NORTH;
        };
    }

    private String facingName() {
        return switch (facing) {
            case EAST -> "东";
            case SOUTH -> "南";
            case WEST -> "西";
            default -> "北";
        };
    }

    private String directionName(int right, int forward) {
        String side = right < 0 ? "左" : right > 0 ? "右" : "";
        String depth = forward > 0 ? "前方" : forward < 0 ? "后方" : "侧";
        return side + depth + " " + Math.max(Math.abs(right), Math.abs(forward)) + "区块";
    }

    private record ChunkTarget(int chunkX, int chunkZ) {
    }
}
