package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.data.PlayerData;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.util.PlayerHeadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VotePreyMenu extends BaseMenu {

    private final GameRoom room;

    public VotePreyMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l🎯 投票选择猎物 🎯", 54);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.COMPASS,
            "§x§F§F§A§A§5§5🎯 §x§F§F§C§C§7§7投§x§F§F§E§E§9§9票§x§D§D§F§F§9§9猎§x§B§B§F§F§7§7物 §x§F§F§A§A§5§5🎯",
            "§8· · · · · · · · · · · · · ·",
            getVoteStageLine(),
            room.isDoublePreyEnabled() && room.getLockedFirstDualPrey() != null
                    ? "§f第一位猎物已锁定: §c" + getPlayerName(room.getLockedFirstDualPrey())
                    : "§f票数最多的将成为猎物",
            "§8· · · · · · · · · · · · · ·"));

        // 显示所有玩家（按投票数排序）
        List<UUID> players = getSortedPlayersByVotes();
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < slots.length && i < players.size(); i++) {
            UUID uuid = players.get(i);
            inventory.setItem(slots[i], createPlayerHead(uuid));
        }

        // 刷新按钮
        inventory.setItem(49, createRefreshButton());

        // 返回按钮
        inventory.setItem(45, createBackButton());
    }

    private List<UUID> getSortedPlayersByVotes() {
        List<UUID> players = new ArrayList<>(room.getAllPlayerUUIDs());
        Map<UUID, Integer> voteCounts = room.getAllVoteCounts();

        // 按投票数排序（多的在前）
        players.sort((a, b) -> {
            if (room.isDoublePreyEnabled()) {
                UUID locked = room.getLockedFirstDualPrey();
                if (locked != null) {
                    if (a.equals(locked) && !b.equals(locked)) return -1;
                    if (!a.equals(locked) && b.equals(locked)) return 1;
                }
            }
            int votesA = voteCounts.getOrDefault(a, 0);
            int votesB = voteCounts.getOrDefault(b, 0);
            return Integer.compare(votesB, votesA);
        });

        return players;
    }

    private String getPlayerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "未知玩家" : name;
    }

    private String getVoteStageLine() {
        if (room.getGameMode() == org.gamefunxiao.game.GameMode.FLASH && room.isFlashTriplePreyEnabled()) {
            return "§f点击玩家头颅投票第二、第三位猎物";
        }
        if (room.isDoublePreyEnabled()) {
            return "§f点击玩家头颅投票第二位猎物";
        }
        return "§f点击玩家头颅投票";
    }

    private ItemStack createRefreshButton() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§a刷新投票§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击刷新投票显示");
            lore.add("§f- §7更新玩家排序和票数");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
            String playerName = PlayerHeadUtil.getBestPlayerName(uuid, data == null ? getPlayerName(uuid) : data.getPlayerName());
            PlayerHeadUtil.applyPlayerSkin(meta, uuid, playerName);

            boolean isPrey = room.isPrey(uuid);
            boolean isLockedFirstPrey = room.isDoublePreyEnabled() && uuid.equals(room.getLockedFirstDualPrey());
            boolean isSelf = uuid.equals(player.getUniqueId());
            int voteCount = room.getVoteCount(uuid);

            String status;
            if (isPrey) {
                status = "§c[已是猎物]";
            } else if (isLockedFirstPrey) {
                status = "§6[第一猎物]";
            } else if (isSelf) {
                status = "§7[自己]";
            } else {
                status = "§a[可投票]";
            }

            meta.setDisplayName("§e" + playerName + " " + status);

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");

            if (isPrey) {
                lore.add("§f- §c该玩家已经是猎物");
            } else if (isLockedFirstPrey) {
                lore.add("§f- §6该玩家已锁定为第一位猎物");
            } else if (isSelf) {
                lore.add("§f- §7不能投票给自己");
            } else {
                if (room.getGameMode() == org.gamefunxiao.game.GameMode.FLASH && room.isFlashTriplePreyEnabled()) {
                    lore.add("§f- §a点击投票此玩家进入额外猎物名额");
                } else if (room.isDoublePreyEnabled()) {
                    lore.add("§f- §a点击投票此玩家成为下一位猎物");
                } else {
                    lore.add("§f- §a点击投票此玩家为猎物");
                }
            }

            lore.add("§f- §b当前票数: §e" + voteCount + " §b票");

            // 显示游玩次数和表现值
            org.gamefunxiao.data.PlayerData playerData = data;
            if (playerData != null) {
                int playCount = playerData.getPlayCount("total");
                lore.add("§f- §d游玩次数: §e" + playCount + " §d次");
                // 显示猎人表现和猎物表现
                int hunterPts = playerData.getHunterPoints("total");
                int preyPts = playerData.getPreyPoints("total");
                String hColor = hunterPts >= 0 ? "§6" : "§c";
                String pColor = preyPts >= 0 ? "§6" : "§c";
                lore.add("§f- §c猎人表现: " + hColor + hunterPts + " §8| §a猎物表现: " + pColor + preyPts);
            }

            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        // 检查是否点击了玩家
        int slotIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            List<UUID> players = getSortedPlayersByVotes();
            if (slotIndex < players.size()) {
                UUID targetUuid = players.get(slotIndex);

                // 防止投票给自己
                if (targetUuid.equals(player.getUniqueId())) {
                    playErrorSound();
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.cannot_vote_self"));
                    return;
                }

                if (room.isPrey(targetUuid) || (room.isDoublePreyEnabled() && targetUuid.equals(room.getLockedFirstDualPrey()))) {
                    playErrorSound();
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.already_prey"));
                } else {
                    // 检查是否已经投过票给这个人
                    UUID currentVote = room.getPlayerVote(player.getUniqueId());
                    if (currentVote != null && currentVote.equals(targetUuid)) {
                        playErrorSound();
                        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.already_voted_this_player"));
                        return;
                    }

                    playConfirmSound();
                    room.voteForPrey(player.getUniqueId(), targetUuid);

                    String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("player", player.getName());
                    placeholders.put("target", targetName);
                    room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.voted_prey", placeholders));

                    player.closeInventory();
                }
            }
            return;
        }

        switch (slot) {
            case 45 -> {
                playClickSound();
                player.closeInventory();
            }
            case 49 -> {
                playClickSound();
                setupItems();
            }
        }
    }
}
