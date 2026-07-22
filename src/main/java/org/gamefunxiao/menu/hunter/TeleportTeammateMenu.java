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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TeleportTeammateMenu extends BaseMenu {

    private final GameRoom room;
    private final boolean mandatoryRespawnSelection;

    public TeleportTeammateMenu(GameFunXiao plugin, Player player, GameRoom room) {
        this(plugin, player, room, false);
    }

    public TeleportTeammateMenu(GameFunXiao plugin, Player player, GameRoom room, boolean mandatoryRespawnSelection) {
        super(plugin, player, "§0§l🌀 传送到队友 🌀", 27);
        this.room = room;
        this.mandatoryRespawnSelection = mandatoryRespawnSelection;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.ENDER_PEARL,
            "§x§D§D§5§5§F§F🌀 §x§C§C§7§7§F§F传§x§B§B§9§9§F§F送§x§A§A§B§B§F§F到§x§9§9§D§D§F§F队§x§8§8§F§F§F§F友 §x§D§D§5§5§F§F🌀",
            "§8· · · · · · · · · · · · · ·",
            "§f选择要传送到的队友",
            "§8· · · · · · · · · · · · · ·"));

        // 显示队友：普通模式显示猎人，闪光多猎物时猎物也能互相传送。
        List<UUID> teammates = new ArrayList<>();
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (isTeleportTeammate(uuid)) {
                teammates.add(uuid);
            }
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < slots.length && i < teammates.size(); i++) {
            UUID uuid = teammates.get(i);
            inventory.setItem(slots[i], createTeammateHead(uuid));
        }

        // 如果没有队友
        if (teammates.isEmpty()) {
            inventory.setItem(13, createItem(Material.STRUCTURE_VOID,
                "   §8[§7没有队友§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §c没有可传送的队友",
                "§8· · · · · · · · · · · · · ·"));
        }

        if (!mandatoryRespawnSelection) {
            inventory.setItem(22, createPlainCloseButton());
        } else {
            inventory.setItem(22, createItem(Material.BARRIER,
                "   §8[§c必须选择队友§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §c你死亡后必须选择一位队友",
                "§f- §c否则无法继续复活",
                "§8· · · · · · · · · · · · · ·"));
        }
    }

    private ItemStack createTeammateHead(UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
            String playerName = PlayerHeadUtil.getBestPlayerName(uuid, data == null ? null : data.getPlayerName());
            PlayerHeadUtil.applyPlayerSkin(meta, uuid, playerName);

            String role = room.isPrey(uuid) ? "猎物" : "猎人";
            String roleColor = room.isPrey(uuid) ? "§x§F§F§A§A§D§D" : "§x§5§5§A§A§F§F";
            meta.setDisplayName(roleColor + "⚔ §f" + playerName + " " + roleColor + "[" + role + "]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            Player target = Bukkit.getPlayer(uuid);
            if (!mandatoryRespawnSelection && room.isHunter(uuid)) {
                double preyDistance = target == null ? -1.0D
                        : plugin.getFlashModeManager().getNearestPreyDistance(room, target);
                if (preyDistance >= 0.0D) {
                    boolean blocked = preyDistance <= plugin.getFlashModeManager().getHunterCompassPreyLimit();
                    lore.add("§f- §e距离最近猎物: "
                            + (blocked ? "§c" : "§a")
                            + String.format(Locale.ROOT, "%.1f", preyDistance) + " §e格");
                    lore.add(blocked
                            ? "§f- §c目标处于猎物100格内，暂时不能传送"
                            : "§f- §a目标距离安全，可以传送");
                } else {
                    lore.add("§f- §7目标与在线猎物不在同一世界");
                    lore.add("§f- §a当前可以传送");
                }
            } else {
                lore.add("§f- §a点击传送到此队友身边");
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

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        // 检查是否点击了队友
        int slotIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            List<UUID> teammates = new ArrayList<>();
            for (UUID uuid : room.getAllPlayerUUIDs()) {
                if (isTeleportTeammate(uuid)) {
                    teammates.add(uuid);
                }
            }

            if (slotIndex < teammates.size()) {
                UUID targetUuid = teammates.get(slotIndex);
                Player target = Bukkit.getPlayer(targetUuid);

                if (target != null && target.isOnline()) {
                    if (!mandatoryRespawnSelection && room.isHunter(player.getUniqueId())
                            && plugin.getFlashModeManager().denyHunterCompassUseNearPrey(player, room)) {
                        playErrorSound();
                        return;
                    }
                    if (!mandatoryRespawnSelection && room.isHunter(targetUuid)
                            && plugin.getFlashModeManager().isHunterCompassUseBlockedNearPrey(target, room)) {
                        double distance = plugin.getFlashModeManager().getNearestPreyDistance(room, target);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("target", target.getName());
                        placeholders.put("distance", String.format(Locale.ROOT, "%.1f", Math.max(0.0D, distance)));
                        placeholders.put("limit", String.format(Locale.ROOT, "%.0f",
                                plugin.getFlashModeManager().getHunterCompassPreyLimit()));
                        player.sendMessage(plugin.getMessageManager()
                                .getHunterGameMessageWithPrefix("game.compass_target_prey_too_close", placeholders));
                        playErrorSound();
                        setupItems();
                        return;
                    }
                    playConfirmSound();

                    if (mandatoryRespawnSelection) {
                        plugin.getPlayerListener().clearForcedHunterTpSelection(player.getUniqueId());
                    }

                    player.closeInventory();

                    // 传送
                    player.teleport(target.getLocation());
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                    if (mandatoryRespawnSelection) {
                        plugin.getPlayerListener().setHunterTpLootLock(player.getUniqueId());
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    } else {
                        plugin.getPlayerListener().setCompassTpCooldown(player.getUniqueId(), room);
                    }

                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("player", player.getName());
                    placeholders.put("target", target.getName());
                    room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.teleported", placeholders));
                } else {
                    playErrorSound();
                    player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.player_not_found"));
                }
            }
            return;
        }

        if (slot == 22) {
            if (!mandatoryRespawnSelection) {
                handlePlainCloseAction();
            } else {
                playErrorSound();
            }
        }
    }

    private boolean isTeleportTeammate(UUID uuid) {
        if (uuid == null || uuid.equals(player.getUniqueId()) || room.isSpectator(uuid)) {
            return false;
        }
        if (plugin.getFlashModeManager().isFlashMode(room) && room.isPrey(player.getUniqueId())) {
            return room.isPrey(uuid);
        }
        return room.isHunter(player.getUniqueId()) && room.isHunter(uuid);
    }
}
