package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.DeathSwapVictoryEffect;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeathSwapVictoryEffectShopMenu extends BaseMenu {

    private static final int[] EFFECT_SLOTS = {20, 22, 24, 29, 31, 33};
    private final boolean returnToNavigation;

    public DeathSwapVictoryEffectShopMenu(GameFunXiao plugin, Player player) {
        this(plugin, player, false);
    }

    public DeathSwapVictoryEffectShopMenu(GameFunXiao plugin, Player player, boolean returnToNavigation) {
        super(plugin, player, "§0§l⟲ 死亡互换胜利样式 ⟲", 54);
        this.returnToNavigation = returnToNavigation;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        int coins = plugin.getPlayerDataManager().getCoins(player.getUniqueId());
        String currency = plugin.getConfigManager().getMiniGameCurrencyName();
        String selected = plugin.getPlayerDataManager().getSelectedDeathSwapVictoryEffect(player.getUniqueId());

        inventory.setItem(4, createTitleItem(Material.REDSTONE,
                "§x§F§F§6§6§0§0⟲ §x§F§F§8§8§2§2死§x§F§F§A§A§4§4亡§x§F§F§C§C§6§6互§x§F§F§E§E§8§8换§x§F§F§F§F§A§A胜§x§F§F§D§D§7§7利§x§F§F§B§B§4§4样§x§F§F§9§9§2§2式",
                "§8· · · · · · · · · · · · · ·",
                "§f- §c这里只出售死亡互换专属胜利样式",
                "§f- §a结算时会在胜者位置播放",
                "§f- §b余额: §e" + coins + " §6" + currency,
                "§f- §d当前选择: §e" + DeathSwapVictoryEffect.byId(selected).getDisplayName(plugin),
                "§8· · · · · · · · · · · · · ·"));

        DeathSwapVictoryEffect[] effects = DeathSwapVictoryEffect.values();
        for (int i = 0; i < effects.length && i < EFFECT_SLOTS.length; i++) {
            inventory.setItem(EFFECT_SLOTS[i], createEffectItem(effects[i], coins, currency, selected));
        }

        inventory.setItem(45, createBackButton());
    }

    private ItemStack createEffectItem(DeathSwapVictoryEffect effect, int coins, String currency, String selected) {
        ItemStack item = new ItemStack(effect.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean owned = plugin.getPlayerDataManager().hasDeathSwapVictoryEffect(
                    player.getUniqueId(), effect.getId());
            boolean isSelected = effect.getId().equalsIgnoreCase(selected);
            int price = effect.getPrice(plugin);
            String state = isSelected ? "§a✔ 已选择"
                    : owned ? "§b已拥有"
                    : coins >= price ? "§e可购买"
                    : "§c余额不足";

            meta.setDisplayName("   §8[§r" + (isSelected ? "§a✔ " : "§c⟲ ")
                    + effect.getDisplayName(plugin) + " §8| " + state + "§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.addAll(effect.getDescription(plugin));
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6价格: §e" + price + " §6" + currency);
            lore.add("§f- §a你的余额: §e" + coins + " §6" + currency);
            lore.add("§f- §b状态: " + state);
            if (isSelected) {
                lore.add("§f- §a这个样式正在死亡互换中使用");
            } else if (owned) {
                lore.add("§f- §b左键切换为这个胜利样式");
            } else {
                lore.add("§f- §e左键购买并自动选择");
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
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        DeathSwapVictoryEffect[] effects = DeathSwapVictoryEffect.values();
        for (int i = 0; i < EFFECT_SLOTS.length && i < effects.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                handleEffectClick(effects[i]);
                return;
            }
        }

        if (slot == 45) {
            playClickSound();
            new DeathSwapShopCategoryMenu(plugin, player, returnToNavigation).open();
        }
    }

    private void handleEffectClick(DeathSwapVictoryEffect effect) {
        String effectId = effect.getId();
        String selected = plugin.getPlayerDataManager().getSelectedDeathSwapVictoryEffect(player.getUniqueId());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("effect", effect.getDisplayName(plugin));
        placeholders.put("currency", plugin.getConfigManager().getMiniGameCurrencyName());
        placeholders.put("price", String.valueOf(effect.getPrice(plugin)));
        placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));

        if (effectId.equalsIgnoreCase(selected)) {
            player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("shop.already_selected", placeholders));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.6f);
            return;
        }

        if (plugin.getPlayerDataManager().hasDeathSwapVictoryEffect(player.getUniqueId(), effectId)) {
            plugin.getPlayerDataManager().setSelectedDeathSwapVictoryEffect(player.getUniqueId(), effectId);
            player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("shop.select_success", placeholders));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.65f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
            setupItems();
            return;
        }

        int price = effect.getPrice(plugin);
        if (!plugin.getPlayerDataManager().takeCoins(player.getUniqueId(), price)) {
            placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));
            player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("shop.not_enough_currency", placeholders));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.9f, 1.0f);
            setupItems();
            return;
        }

        plugin.getPlayerDataManager().unlockDeathSwapVictoryEffect(player.getUniqueId(), effectId);
        plugin.getPlayerDataManager().setSelectedDeathSwapVictoryEffect(player.getUniqueId(), effectId);
        placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));
        player.sendMessage(plugin.getMessageManager().getDeathSwapMessageWithPrefix("shop.purchase_success", placeholders));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.82f, 1.18f);
        setupItems();
    }
}
