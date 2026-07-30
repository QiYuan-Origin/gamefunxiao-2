package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class DeathSwapTimeVoteMenu extends BaseMenu {

    private final GameRoom room;

    public DeathSwapTimeVoteMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l⟲ 死亡互换 - 时间投票 ⟲", 27);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();
        inventory.setItem(4, createTitleItem(Material.CLOCK,
                "§x§F§F§6§6§0§0⏱ §x§F§F§9§9§3§3互§x§F§F§D§D§5§5换时间",
                "§8· · · · · · · · · · · · · ·",
                "§f左键投票 / 取消投票",
                "§f右键切换你要投的分钟数",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(13, createVoteButton());
        inventory.setItem(22, createPlainCloseButton());
    }

    private ItemStack createVoteButton() {
        List<Integer> intervals = plugin.getConfigManager().getDeathSwapVoteIntervalMinutes();
        int cursor = room.getDeathSwapVoteCursor(player.getUniqueId(), intervals);
        Integer ownVote = room.getDeathSwapVote(player.getUniqueId());

        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§F§F§6§6§0§0⟲ §x§F§F§D§D§5§5当前选择 §e" + cursor + "分钟§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            for (int minute : intervals) {
                String color = ownVote != null && ownVote == minute ? "§a" : "§7";
                lore.add(color + "- " + minute + "分钟 （" + room.getDeathSwapVoteCount(minute) + "票）");
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a左键投票 / 取消当前分钟");
            lore.add("§f- §e右键切换投票分钟");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (slot == 22) {
            handlePlainCloseAction();
            return;
        }
        if (slot != 13) {
            return;
        }

        List<Integer> intervals = plugin.getConfigManager().getDeathSwapVoteIntervalMinutes();
        if (event.isRightClick()) {
            int minute = room.cycleDeathSwapVoteCursor(player.getUniqueId(), intervals);
            room.setDeathSwapVote(player.getUniqueId(), minute, intervals);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.85f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("death_swap.vote_changed",
                    java.util.Map.of("minutes", String.valueOf(minute))));
            setupItems();
            plugin.getGameManager().refreshLobbyItems(room);
            return;
        }

        boolean voted = room.toggleDeathSwapVote(player.getUniqueId(), intervals);
        int minute = room.getDeathSwapVoteCursor(player.getUniqueId(), intervals);
        player.playSound(player.getLocation(), voted ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_NOTE_BLOCK_BASS,
                0.85f, 1.0f);
        player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix(
                voted ? "death_swap.vote_changed" : "death_swap.vote_removed",
                java.util.Map.of("minutes", String.valueOf(minute))));
        setupItems();
        plugin.getGameManager().refreshLobbyItems(room);
    }
}
