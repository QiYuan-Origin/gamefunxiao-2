package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DeathSwapNavigationMenu extends BaseMenu {

    public DeathSwapNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l⟲ 死亡互换 ⟲", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.ENDER_PEARL,
                "§x§8§8§D§D§F§F⟲ §x§A§A§E§E§F§F死§x§C§C§F§F§F§F亡§x§E§E§F§F§D§D互§x§F§F§D§D§B§B换",
                "§8· · · · · · · · · · · · · ·",
                "§f周期互换位置，只有一次生命",
                "§f前一小时能打人但不造成伤害，之后开启真实 PVP",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(0, createLeaderboardButton());
        inventory.setItem(8, createRoomListButton());
        inventory.setItem(21, createRoomButton("双人局", 2, Material.ENDER_EYE,
                "§f- §b适合快速体验互换陷阱",
                "§f- §e出生点附近会筛非沙漠村庄"));
        inventory.setItem(22, createRoomButton("四人局", 4, Material.CHORUS_FRUIT,
                "§f- §b互换路线更乱，陷阱更好骗",
                "§f- §e默认投票 5 / 10 分钟互换"));
        inventory.setItem(23, createRoomButton("八人局", 8, Material.RECOVERY_COMPASS,
                "§f- §b按出生点围成一圈开局",
                "§f- §e完整死亡互换标准人数"));
        inventory.setItem(36, createBackButton());
        inventory.setItem(44, createCreateRoomButton());
    }

    private ItemStack createLeaderboardButton() {
        return createItem(Material.DIAMOND,
                "   §8[§x§8§8§D§D§F§F🏆 §x§B§B§F§F§F§F互§x§D§D§F§F§E§E换§x§F§F§D§D§B§B积分§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a查看死亡互换小游戏积分",
                "§f- §7与猎人表现值分开统计",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private ItemStack createRoomListButton() {
        return createItem(Material.ENDER_EYE,
                "   §8[§x§8§8§D§D§F§F👁 §x§A§A§E§E§F§F房§x§C§C§F§F§F§F间§x§E§E§F§F§D§D列表§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a只看死亡互换房间",
                "§f- §b等待中可加入，进行中可旁观",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private ItemStack createRoomButton(String sizeName, int maxPlayers, Material material, String... extras) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§8§8§D§D§F§F死亡互换 §8/ §b" + sizeName + " §8/ §e" + maxPlayers + "人§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e模式: §b" + GameMode.DEATH_SWAP.getDisplayName());
            for (String extra : extras) {
                lore.add(extra);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击创建");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCreateRoomButton() {
        return createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A✚ §x§8§8§D§D§F§F创§x§A§A§E§E§F§F建§x§C§C§F§F§F§F房§x§F§F§D§D§B§B间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a进入死亡互换自己的创建房间菜单",
                "§f- §7最多 8 人，开局围圈出生",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击打开");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        switch (slot) {
            case 0 -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.55f, 1.35f);
                plugin.getMenuManager().openDeathSwapLeaderboardMenu(player);
            }
            case 8 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.72f, 1.32f);
                plugin.getMenuManager().openDeathSwapRoomListMenu(player);
            }
            case 21 -> createDedicatedRoom(2);
            case 22 -> createDedicatedRoom(4);
            case 23 -> createDedicatedRoom(8);
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
            case 44 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
                new org.gamefunxiao.menu.hunter.CreateRoomMenu(plugin, player, MenuSection.DEATH_SWAP).open();
            }
            default -> {
            }
        }
    }

    private void createDedicatedRoom(int maxPlayers) {
        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            playErrorSound();
            player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("room.already_in_room"));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
        plugin.getRoomManager().createConfiguredRoom(player, GameMode.DEATH_SWAP, maxPlayers, true, new HashSet<>());
    }
}
