package org.gamefunxiao.menu.hunter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.FlashDifficulty;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class FlashDifficultyVoteMenu extends BaseMenu {

    private static final int NORMAL_SLOT = 11;
    private static final int EASY_SLOT = 13;
    private static final int FLASH_SMP_SLOT = 15;
    private final GameRoom room;

    public FlashDifficultyVoteMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0闪光难度投票", 27);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        inventory.setItem(4, createTitleItem(Material.COMPARATOR,
                "§x§F§F§D§7§0§0✦ §x§F§F§B§B§4§4闪§x§F§F§9§9§8§8光§x§F§F§7§7§C§C难§x§F§F§5§5§F§F度§x§D§D§8§8§F§F投§x§B§B§B§B§F§F票 §x§8§8§D§D§F§F✦",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b三种难度按中轴整齐排列",
                "§f- §e票数最多的难度会被选中",
                "§f- §7平票或没人投票时默认 §c正常",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(NORMAL_SLOT, createDifficultyItem(FlashDifficulty.NORMAL));
        inventory.setItem(EASY_SLOT, createDifficultyItem(FlashDifficulty.EASY));
        inventory.setItem(FLASH_SMP_SLOT, createDifficultyItem(FlashDifficulty.FLASH_SMP));
    }

    private ItemStack createDifficultyItem(FlashDifficulty difficulty) {
        boolean normal = difficulty == FlashDifficulty.NORMAL;
        boolean easy = difficulty == FlashDifficulty.EASY;
        boolean flashSmp = difficulty == FlashDifficulty.FLASH_SMP;
        int votes = room.getFlashDifficultyVoteCount(difficulty);
        boolean selected = difficulty == room.getFlashDifficultyVote(player.getUniqueId());
        ItemStack item = new ItemStack(normal ? Material.IRON_SWORD : easy ? Material.FEATHER : Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(text(normal
                ? "§x§F§F§8§8§5§5正常难度 §7<§e" + votes + "票§7>"
                : easy
                ? "§x§5§5§F§F§A§A简单难度 §7<§e" + votes + "票§7>"
                : "§x§8§8§D§D§F§FFlashSMP难度 §7<§e" + votes + "票§7>"));
        List<Component> lore = new ArrayList<>();
        lore.add(text("§8· · · · · · · · · · · · · ·"));
        if (normal) {
            lore.add(text("§f- §c保留当前完整闪光规则"));
            lore.add(text("§f- §e怪物会获得闪光特供装备"));
            lore.add(text("§f- §b重锤与主世界雷雨保持正常数值"));
            lore.add(text("§f- §d每件材料护甲抵消17.5%强化斧增伤"));
        } else if (easy) {
            lore.add(text("§f- §a怪物使用原版强度与原版装备"));
            lore.add(text("§f- §b普通猛击 +5% §7/ §b破盾猛击 +25%"));
            lore.add(text("§f- §d碎盾 +30% §7/ §d一件材料护甲全免强化斧增伤"));
            lore.add(text("§8- 独立计算，不继承正常闪光的重锤倍率"));
            lore.add(text("§f- §e主世界不会锁定为永久雷雨"));
        } else if (flashSmp) {
            lore.add(text("§f- §d同步 FlashSMP 插件的完整闪光机制"));
            lore.add(text("§f- §bFlashSMP 有的机制在本难度全部启用"));
            lore.add(text("§f- §e不套用 GameFun 正常难度的小白弩减伤"));
            lore.add(text("§f- §a材料护甲按 FlashSMP 每件抵消20%强化斧增伤"));
            lore.add(text("§f- §9开局 §b3分钟 §9后主世界永久雷雨"));
        }
        lore.add(text(selected ? "§f- §a你当前已选择这个难度" : "§f- §7点击把本票投给这个难度"));
        lore.add(text("§8· · · · · · · · · · · · · ·"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        FlashDifficulty difficulty = slot == NORMAL_SLOT
                ? FlashDifficulty.NORMAL
                : slot == EASY_SLOT
                ? FlashDifficulty.EASY
                : slot == FLASH_SMP_SLOT ? FlashDifficulty.FLASH_SMP : null;
        if (difficulty == null) {
            return;
        }
        plugin.getGameManager().handleFlashDifficultyVote(player, room, difficulty);
        if (plugin.getGameManager().canVoteFlashDifficulty(room)) {
            setupItems();
        } else {
            player.closeInventory();
        }
    }

    @Override
    protected void playOpenSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.48f, 1.42f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.62f, 1.72f);
            }
        }, 5L);
    }
}
