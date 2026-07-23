package org.gamefunxiao.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EndFlashKitManager {

    public static final int DEFAULT_ENDER_CHEST_SIZE = 27;
    public static final int MIN_ENDER_CHEST_SIZE = 9;
    public static final int MAX_ENDER_CHEST_SIZE = 27;
    private static final DateTimeFormatter EDIT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public enum Role {
        HUNTER("hunter", "猎人"),
        PREY("prey", "猎物");

        private final String id;
        private final String displayName;

        Role(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public static Role fromId(String id) {
            if (id == null) {
                return null;
            }
            String normalized = id.toLowerCase(Locale.ROOT);
            if (normalized.equals("hunter") || normalized.equals("hunt") || normalized.equals("h") || normalized.equals("猎人")) {
                return HUNTER;
            }
            if (normalized.equals("prey") || normalized.equals("p") || normalized.equals("猎物")) {
                return PREY;
            }
            return null;
        }
    }

    public record KitLayout(ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offHandItem) {
        public KitLayout {
            storageContents = normalizeArray(storageContents, 36);
            armorContents = normalizeArray(armorContents, 4);
            offHandItem = cloneOrNull(offHandItem);
        }
    }

    public static final class Kit {
        private final String id;
        private final Role role;
        private String displayName;
        private double chance;
        private final List<ItemStack> items;
        private int enderChestSize;
        private final ItemStack[] enderChestContents;
        private final ItemStack[] storageContents;
        private final ItemStack[] armorContents;
        private ItemStack offHandItem;
        private String guide = "";
        private String creatorName = "";
        private String creatorUuid = "";
        private long createdAt = 0L;
        private String lastEditorName = "";
        private String lastEditorUuid = "";
        private long lastEditedAt = 0L;
        private int startExpLevel = 0;

        private Kit(String id, Role role, String displayName, double chance, List<ItemStack> items, List<ItemStack> enderChestItems) {
            this(id, role, displayName, chance, compactItemsToStorage(items), emptyArray(4), null,
                    compactItemsToEnderContents(enderChestItems), DEFAULT_ENDER_CHEST_SIZE);
        }

        private Kit(String id, Role role, String displayName, double chance, ItemStack[] storageContents, ItemStack[] armorContents,
                    ItemStack offHandItem, List<ItemStack> enderChestItems) {
            this(id, role, displayName, chance, storageContents, armorContents, offHandItem,
                    compactItemsToEnderContents(enderChestItems), DEFAULT_ENDER_CHEST_SIZE);
        }

        private Kit(String id, Role role, String displayName, double chance, ItemStack[] storageContents, ItemStack[] armorContents,
                    ItemStack offHandItem, ItemStack[] enderChestContents, int enderChestSize) {
            this.id = id;
            this.role = role;
            this.displayName = displayName;
            this.chance = clampChance(chance);
            this.storageContents = normalizeArray(storageContents, 36);
            this.armorContents = normalizeArray(armorContents, 4);
            this.offHandItem = cloneOrNull(offHandItem);
            this.items = new ArrayList<>();
            rebuildCompactItems();
            this.enderChestSize = clampEnderChestSize(enderChestSize);
            this.enderChestContents = normalizeArray(enderChestContents, MAX_ENDER_CHEST_SIZE);
        }

        public String id() {
            return id;
        }

        public Role role() {
            return role;
        }

        public String displayName() {
            return displayName;
        }

        public String guide() {
            return guide == null ? "" : guide;
        }

        public boolean hasGuide() {
            return !guide().isBlank();
        }

        public List<String> guideLines() {
            return splitGuideLines(guide());
        }

        public String creatorName() {
            return creatorName == null || creatorName.isBlank() ? "未记录" : creatorName;
        }

        public String creatorUuid() {
            return creatorUuid == null ? "" : creatorUuid;
        }

        public long createdAt() {
            return createdAt;
        }

        public String createdAtText() {
            return formatEditTime(createdAt);
        }

        public String lastEditorName() {
            return lastEditorName == null || lastEditorName.isBlank() ? "未记录" : lastEditorName;
        }

        public String lastEditorUuid() {
            return lastEditorUuid == null ? "" : lastEditorUuid;
        }

        public long lastEditedAt() {
            return lastEditedAt;
        }

        public String lastEditedAtText() {
            return formatEditTime(lastEditedAt);
        }

        public double chance() {
            return chance;
        }

        public int startExpLevel() {
            return startExpLevel;
        }

        public List<ItemStack> items() {
            List<ItemStack> copy = new ArrayList<>();
            for (ItemStack item : items) {
                copy.add(item.clone());
            }
            return copy;
        }

        public ItemStack[] storageContents() {
            return normalizeArray(storageContents, 36);
        }

        public ItemStack[] armorContents() {
            return normalizeArray(armorContents, 4);
        }

        public ItemStack offHandItem() {
            return cloneOrNull(offHandItem);
        }

        public List<ItemStack> enderChestItems() {
            List<ItemStack> copy = new ArrayList<>();
            int limit = Math.min(enderChestSize, enderChestContents.length);
            for (int i = 0; i < limit; i++) {
                ItemStack item = enderChestContents[i];
                if (item != null && item.getType() != Material.AIR) {
                    copy.add(item.clone());
                }
            }
            return copy;
        }

        public ItemStack[] enderChestContents() {
            return normalizeArray(enderChestContents, enderChestSize);
        }

        public int enderChestSize() {
            return enderChestSize;
        }

        private void replaceLayout(ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offHandItem) {
            ItemStack[] nextStorage = normalizeArray(storageContents, 36);
            ItemStack[] nextArmor = normalizeArray(armorContents, 4);
            for (int i = 0; i < this.storageContents.length; i++) {
                this.storageContents[i] = nextStorage[i];
            }
            for (int i = 0; i < this.armorContents.length; i++) {
                this.armorContents[i] = nextArmor[i];
            }
            this.offHandItem = cloneOrNull(offHandItem);
            rebuildCompactItems();
        }

        private void rebuildCompactItems() {
            items.clear();
            addNonAirItems(items, storageContents);
            addNonAirItems(items, armorContents);
            if (offHandItem != null && offHandItem.getType() != Material.AIR) {
                items.add(offHandItem.clone());
            }
        }

        private void replaceEnderChest(ItemStack[] contents, int size) {
            this.enderChestSize = clampEnderChestSize(size);
            ItemStack[] next = normalizeArray(contents, MAX_ENDER_CHEST_SIZE);
            for (int i = 0; i < this.enderChestContents.length; i++) {
                this.enderChestContents[i] = i < this.enderChestSize ? next[i] : null;
            }
        }

        private boolean appendEnderChestItem(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
            int limit = Math.min(enderChestSize, enderChestContents.length);
            for (int i = 0; i < limit; i++) {
                if (enderChestContents[i] == null || enderChestContents[i].getType() == Material.AIR) {
                    enderChestContents[i] = item.clone();
                    return true;
                }
            }
            return false;
        }

        private boolean appendStorageItem(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
            for (int i = 0; i < storageContents.length; i++) {
                if (storageContents[i] == null || storageContents[i].getType() == Material.AIR) {
                    storageContents[i] = item.clone();
                    rebuildCompactItems();
                    return true;
                }
            }
            return false;
        }
    }

    private final GameFunXiao plugin;
    private final File file;
    private final boolean sharedStorageMode;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final Map<UUID, Map<Role, String>> selections = new HashMap<>();
    private long lastLoadedModified = Long.MIN_VALUE;
    private long lastLoadedLength = Long.MIN_VALUE;

    private static String formatEditTime(long millis) {
        if (millis <= 0L) {
            return "未记录";
        }
        return EDIT_TIME_FORMAT.format(Instant.ofEpochMilli(millis));
    }

    public EndFlashKitManager(GameFunXiao plugin) {
        this.plugin = plugin;
        File localFile = new File(plugin.getDataFolder(), "end_flash_kits.yml");
        String sharedRootPath = plugin.getConfigManager().getSharedPlayerDataRootPath();
        // Kit 属于 gamefun/gameing 都要读取的公共配置，只要配置了共享根目录就直接使用。
        // 不能再依赖“当前房间后端模式是否启用”，否则大厅服或调试服某些配置下会落回本地文件，两个服自然不同步。
        boolean shouldUseSharedStorage = sharedRootPath != null && !sharedRootPath.isBlank();
        if (shouldUseSharedStorage) {
            File sharedRoot = new File(sharedRootPath);
            this.file = new File(sharedRoot, "end_flash_kits.yml");
            this.sharedStorageMode = true;
            copyLocalFileToSharedIfMissing(localFile, file);
        } else {
            this.file = localFile;
            this.sharedStorageMode = false;
        }
        plugin.getLogger().info("终章闪光 Kit 存储路径: " + file.getAbsolutePath()
                + (sharedStorageMode ? " (跨服共享)" : " (本地单服)"));
        load();
    }

    public synchronized void load() {
        if (!file.exists()) {
            save(false);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Kit> loadedKits = new LinkedHashMap<>();
        boolean migratedLegacyFormat = false;
        ConfigurationSection kitSection = config.getConfigurationSection("kits");
        if (kitSection != null) {
            for (String id : kitSection.getKeys(false)) {
                String path = "kits." + id + ".";
                Role role = Role.fromId(config.getString(path + "role"));
                if (role == null) {
                    continue;
                }
                String displayName = config.getString(path + "name", id);
                String guide = normalizeGuideText(config.getString(path + "guide", ""));
                double chance = clampChance(config.getDouble(path + "chance", 10.0D));
                int startExpLevel = clampStartExpLevel(config.getInt(path + "start_exp_level", 0));
                List<ItemStack> items = decodeItems(config.getStringList(path + "items"));
                boolean hasStorageKey = config.isList(path + "storage");
                boolean hasArmorKey = config.isList(path + "armor");
                boolean hasOffhandKey = config.isSet(path + "offhand");
                boolean hasEnderSlotsKey = config.isList(path + "ender_chest_slots");
                ItemStack[] storageContents = decodeSlotArray(config.getStringList(path + "storage"), 36);
                ItemStack[] armorContents = decodeSlotArray(config.getStringList(path + "armor"), 4);
                ItemStack offHandItem = decodeItem(config.getString(path + "offhand"));
                if (!hasAnyItem(storageContents) && !hasAnyItem(armorContents) && isEmptyItem(offHandItem)) {
                    storageContents = compactItemsToStorage(items);
                    if (!items.isEmpty()) {
                        migratedLegacyFormat = true;
                    }
                } else if (!hasStorageKey || !hasArmorKey || !hasOffhandKey) {
                    migratedLegacyFormat = true;
                }
                int enderChestSize = clampEnderChestSize(config.getInt(path + "ender_chest_size", DEFAULT_ENDER_CHEST_SIZE));
                ItemStack[] enderChestContents = decodeSlotArray(config.getStringList(path + "ender_chest_slots"), MAX_ENDER_CHEST_SIZE);
                if (!hasAnyItem(enderChestContents)) {
                    List<ItemStack> oldEnderItems = decodeItems(config.getStringList(path + "ender_chest"));
                    enderChestContents = compactItemsToEnderContents(oldEnderItems);
                    if (!oldEnderItems.isEmpty()) {
                        migratedLegacyFormat = true;
                    }
                } else if (!hasEnderSlotsKey) {
                    migratedLegacyFormat = true;
                }
                Kit kit = new Kit(id.toLowerCase(Locale.ROOT), role, displayName, chance,
                        storageContents, armorContents, offHandItem, enderChestContents, enderChestSize);
                kit.guide = guide;
                kit.startExpLevel = startExpLevel;
                loadAuditFields(config, path, kit);
                if (syncKitLore(kit)) {
                    migratedLegacyFormat = true;
                }
                loadedKits.put(id.toLowerCase(Locale.ROOT), kit);
            }
        }

        if (loadedKits.isEmpty() && sharedStorageMode) {
            File localFile = new File(plugin.getDataFolder(), "end_flash_kits.yml");
            if (localFile.exists() && !sameFile(localFile, file)) {
                Map<String, Kit> localKits = loadKitsFromFile(localFile);
                if (!localKits.isEmpty()) {
                    plugin.getLogger().warning("共享终章闪光 Kit 文件没有读取到 Kit，已从本地备份恢复 " + localKits.size() + " 个 Kit。");
                    loadedKits.putAll(localKits);
                }
            }
        }

        if (loadedKits.isEmpty() && !kits.isEmpty()) {
            plugin.getLogger().warning("本次没有读取到任何终章闪光 Kit，为避免游戏中 Kit 突然消失，保留内存中的 " + kits.size() + " 个 Kit。");
            updateLoadedFileState();
            return;
        }
        Map<UUID, Map<Role, String>> loadedSelections = new HashMap<>();
        ConfigurationSection selectionSection = config.getConfigurationSection("selections");
        if (selectionSection != null) {
            for (String rawUuid : selectionSection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException ignored) {
                    migratedLegacyFormat = true;
                    continue;
                }
                Map<Role, String> playerSelections = new HashMap<>();
                for (Role role : Role.values()) {
                    String kitId = config.getString("selections." + rawUuid + "." + role.id());
                    if (kitId == null || kitId.isBlank()) {
                        continue;
                    }
                    String normalizedId = kitId.toLowerCase(Locale.ROOT);
                    Kit selectedKit = loadedKits.get(normalizedId);
                    if (selectedKit != null && selectedKit.role() == role) {
                        playerSelections.put(role, normalizedId);
                    } else {
                        migratedLegacyFormat = true;
                    }
                }
                if (!playerSelections.isEmpty()) {
                    loadedSelections.put(uuid, playerSelections);
                }
            }
        }

        kits.clear();
        kits.putAll(loadedKits);
        selections.clear();
        selections.putAll(loadedSelections);
        updateLoadedFileState();
        if (migratedLegacyFormat) {
            save(false);
            plugin.getLogger().info("已自动升级终章闪光 Kit 文件格式，旧版 items 已补齐为新版槽位结构。");
        }
    }

    public synchronized void save() {
        save(true);
    }

    private synchronized void save(boolean broadcastSync) {
        FileConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            syncKitLore(kit);
            String path = "kits." + kit.id() + ".";
            config.set(path + "role", kit.role().id());
            config.set(path + "name", kit.displayName());
            config.set(path + "guide", kit.guide());
            config.set(path + "creator_name", kit.creatorName());
            config.set(path + "creator_uuid", kit.creatorUuid());
            config.set(path + "created_at", kit.createdAt());
            config.set(path + "last_editor_name", kit.lastEditorName());
            config.set(path + "last_editor_uuid", kit.lastEditorUuid());
            config.set(path + "last_edited_at", kit.lastEditedAt());
            config.set(path + "chance", kit.chance());
            config.set(path + "start_exp_level", kit.startExpLevel());
            config.set(path + "items", encodeItems(kit.items()));
            config.set(path + "storage", encodeSlotArray(kit.storageContents()));
            config.set(path + "armor", encodeSlotArray(kit.armorContents()));
            config.set(path + "offhand", encodeItem(kit.offHandItem()));
            config.set(path + "ender_chest_size", kit.enderChestSize());
            config.set(path + "ender_chest_slots", encodeSlotArray(kit.enderChestContents()));
            config.set(path + "ender_chest", encodeItems(kit.enderChestItems()));
        }
        for (Map.Entry<UUID, Map<Role, String>> playerEntry : selections.entrySet()) {
            for (Map.Entry<Role, String> selectionEntry : playerEntry.getValue().entrySet()) {
                config.set("selections." + playerEntry.getKey() + "." + selectionEntry.getKey().id(),
                        selectionEntry.getValue());
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
            updateLoadedFileState();
            if (broadcastSync && plugin.getChildServerManager() != null) {
                plugin.getChildServerManager().scheduleEndFlashKitSync(file);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("无法保存终章闪光 Kit 数据: " + exception.getMessage());
        }
    }

    public synchronized byte[] exportSyncBytes() {
        try {
            if (!file.exists()) {
                save(false);
            }
            return Files.readAllBytes(file.toPath());
        } catch (IOException exception) {
            plugin.getLogger().warning("导出终章闪光 Kit 同步数据失败: " + exception.getMessage());
            return new byte[0];
        }
    }

    public synchronized void importSyncedBytes(byte[] yamlBytes, String sourceName) {
        if (yamlBytes == null || yamlBytes.length == 0) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File temp = new File(parent == null ? plugin.getDataFolder() : parent,
                    file.getName() + ".sync-" + UUID.randomUUID() + ".tmp");
            Files.write(temp.toPath(), yamlBytes);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            load();
            plugin.getLogger().info("已同步终章闪光 Kit 数据"
                    + (sourceName == null || sourceName.isBlank() ? "。" : "，来源: " + sourceName));
        } catch (IOException exception) {
            plugin.getLogger().warning("写入跨服同步的终章闪光 Kit 数据失败: " + exception.getMessage());
        }
    }

    private Map<String, Kit> loadKitsFromFile(File sourceFile) {
        Map<String, Kit> loaded = new LinkedHashMap<>();
        if (sourceFile == null || !sourceFile.exists()) {
            return loaded;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(sourceFile);
        ConfigurationSection kitSection = config.getConfigurationSection("kits");
        if (kitSection == null) {
            return loaded;
        }
        for (String id : kitSection.getKeys(false)) {
            String path = "kits." + id + ".";
            Role role = Role.fromId(config.getString(path + "role"));
            if (role == null) {
                continue;
            }
            String displayName = config.getString(path + "name", id);
            String guide = normalizeGuideText(config.getString(path + "guide", ""));
            double chance = clampChance(config.getDouble(path + "chance", 10.0D));
            int startExpLevel = clampStartExpLevel(config.getInt(path + "start_exp_level", 0));
            List<ItemStack> items = decodeItems(config.getStringList(path + "items"));
            ItemStack[] storageContents = decodeSlotArray(config.getStringList(path + "storage"), 36);
            ItemStack[] armorContents = decodeSlotArray(config.getStringList(path + "armor"), 4);
            ItemStack offHandItem = decodeItem(config.getString(path + "offhand"));
            if (!hasAnyItem(storageContents) && !hasAnyItem(armorContents) && isEmptyItem(offHandItem)) {
                storageContents = compactItemsToStorage(items);
            }
            int enderChestSize = clampEnderChestSize(config.getInt(path + "ender_chest_size", DEFAULT_ENDER_CHEST_SIZE));
            ItemStack[] enderChestContents = decodeSlotArray(config.getStringList(path + "ender_chest_slots"), MAX_ENDER_CHEST_SIZE);
            if (!hasAnyItem(enderChestContents)) {
                enderChestContents = compactItemsToEnderContents(decodeItems(config.getStringList(path + "ender_chest")));
            }
            Kit kit = new Kit(id.toLowerCase(Locale.ROOT), role, displayName, chance,
                    storageContents, armorContents, offHandItem, enderChestContents, enderChestSize);
            kit.guide = guide;
            kit.startExpLevel = startExpLevel;
            loadAuditFields(config, path, kit);
            syncKitLore(kit);
            loaded.put(id.toLowerCase(Locale.ROOT), kit);
        }
        return loaded;
    }

    private static void loadAuditFields(FileConfiguration config, String path, Kit kit) {
        if (config == null || path == null || kit == null) {
            return;
        }
        kit.creatorName = config.getString(path + "creator_name", "");
        kit.creatorUuid = config.getString(path + "creator_uuid", "");
        kit.createdAt = config.getLong(path + "created_at", 0L);
        kit.lastEditorName = config.getString(path + "last_editor_name", "");
        kit.lastEditorUuid = config.getString(path + "last_editor_uuid", "");
        kit.lastEditedAt = config.getLong(path + "last_edited_at", 0L);
    }

    private boolean sameFile(File first, File second) {
        try {
            return first != null && second != null && first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException ignored) {
            return first != null && second != null && first.getAbsolutePath().equalsIgnoreCase(second.getAbsolutePath());
        }
    }

    public Kit createKit(Role role, String displayName, double chance, List<ItemStack> items) {
        return createKit(role, displayName, chance, items, (Player) null);
    }

    public Kit createKit(Role role, String displayName, double chance, List<ItemStack> items, Player editor) {
        reloadIfChanged();
        String id = makeUniqueId(displayName);
        Kit kit = new Kit(id, role, displayName == null || displayName.isBlank() ? id : displayName, chance, items, Collections.emptyList());
        syncKitLore(kit);
        markCreatedAndEdited(kit, editor);
        kits.put(id, kit);
        save();
        return kit;
    }

    public Kit createKit(Role role, String displayName, double chance, List<ItemStack> items, List<ItemStack> enderChestItems) {
        return createKit(role, displayName, chance, items, enderChestItems, null);
    }

    public Kit createKit(Role role, String displayName, double chance, List<ItemStack> items, List<ItemStack> enderChestItems, Player editor) {
        reloadIfChanged();
        String id = makeUniqueId(displayName);
        Kit kit = new Kit(id, role, displayName == null || displayName.isBlank() ? id : displayName, chance, items, enderChestItems);
        syncKitLore(kit);
        markCreatedAndEdited(kit, editor);
        kits.put(id, kit);
        save();
        return kit;
    }

    public Kit createKit(Role role, String displayName, double chance, KitLayout layout) {
        return createKit(role, displayName, chance, layout, null);
    }

    public Kit createKit(Role role, String displayName, double chance, KitLayout layout, Player editor) {
        reloadIfChanged();
        String id = makeUniqueId(displayName);
        KitLayout safeLayout = layout == null ? new KitLayout(null, null, null) : layout;
        Kit kit = new Kit(id, role, displayName == null || displayName.isBlank() ? id : displayName, chance,
                syncFlashArrayCopy(safeLayout.storageContents(), 36),
                syncFlashArrayCopy(safeLayout.armorContents(), 4),
                syncFlashItemCopy(safeLayout.offHandItem()), Collections.emptyList());
        syncKitLore(kit);
        markCreatedAndEdited(kit, editor);
        kits.put(id, kit);
        save();
        return kit;
    }

    public boolean appendItem(String kitId, ItemStack item) {
        return appendItem(kitId, item, null);
    }

    public boolean appendItem(String kitId, ItemStack item, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!kit.appendStorageItem(syncFlashItemCopy(item))) {
            return false;
        }
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean appendEnderChestItem(String kitId, ItemStack item) {
        return appendEnderChestItem(kitId, item, null);
    }

    public boolean appendEnderChestItem(String kitId, ItemStack item, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!kit.appendEnderChestItem(syncFlashItemCopy(item))) {
            return false;
        }
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean removeKit(String kitId) {
        reloadIfChanged();
        if (kitId == null) {
            return false;
        }
        Kit removed = kits.remove(kitId.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public boolean setChance(String kitId, double chance) {
        return setChance(kitId, chance, null);
    }

    public boolean setChance(String kitId, double chance, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return false;
        }
        kit.chance = clampChance(chance);
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean setStartExpLevel(String kitId, int level, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return false;
        }
        kit.startExpLevel = clampStartExpLevel(level);
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean setDisplayName(String kitId, String displayName) {
        return setDisplayName(kitId, displayName, null);
    }

    public boolean setDisplayName(String kitId, String displayName, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || displayName == null || displayName.isBlank()) {
            return false;
        }
        kit.displayName = displayName.trim();
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean setGuide(String kitId, String guide) {
        return setGuide(kitId, guide, null);
    }

    public boolean setGuide(String kitId, String guide, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return false;
        }
        kit.guide = normalizeGuideText(guide);
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean replaceItems(String kitId, List<ItemStack> items) {
        return replaceItems(kitId, items, null);
    }

    public boolean replaceItems(String kitId, List<ItemStack> items, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || items == null || items.isEmpty()) {
            return false;
        }
        kit.replaceLayout(compactItemsToStorage(syncFlashItemList(items)), emptyArray(4), null);
        if (kit.items.isEmpty()) {
            return false;
        }
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean replaceLayout(String kitId, KitLayout layout) {
        return replaceLayout(kitId, layout, null);
    }

    public boolean replaceLayout(String kitId, KitLayout layout, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || layout == null || !layoutHasAnyItem(layout)) {
            return false;
        }
        kit.replaceLayout(syncFlashArrayCopy(layout.storageContents(), 36),
                syncFlashArrayCopy(layout.armorContents(), 4),
                syncFlashItemCopy(layout.offHandItem()));
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean replaceEnderChestLayout(String kitId, ItemStack[] contents, int size) {
        return replaceEnderChestLayout(kitId, contents, size, null);
    }

    public boolean replaceEnderChestLayout(String kitId, ItemStack[] contents, int size, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null || contents == null) {
            return false;
        }
        kit.replaceEnderChest(syncFlashArrayCopy(contents, MAX_ENDER_CHEST_SIZE), size);
        markEdited(kit, editor);
        save();
        return true;
    }

    public boolean setEnderChestSize(String kitId, int size) {
        return setEnderChestSize(kitId, size, null);
    }

    public boolean setEnderChestSize(String kitId, int size, Player editor) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return false;
        }
        kit.replaceEnderChest(kit.enderChestContents(), size);
        markEdited(kit, editor);
        save();
        return true;
    }

    private void markCreatedAndEdited(Kit kit, Player editor) {
        if (kit == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = editorName(editor);
        String uuid = editor == null ? "" : editor.getUniqueId().toString();
        kit.creatorName = name;
        kit.creatorUuid = uuid;
        kit.createdAt = now;
        kit.lastEditorName = name;
        kit.lastEditorUuid = uuid;
        kit.lastEditedAt = now;
    }

    private void markEdited(Kit kit, Player editor) {
        if (kit == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = editorName(editor);
        String uuid = editor == null ? "" : editor.getUniqueId().toString();
        if (kit.creatorName == null || kit.creatorName.isBlank()) {
            kit.creatorName = name;
            kit.creatorUuid = uuid;
            kit.createdAt = kit.createdAt <= 0L ? now : kit.createdAt;
        }
        kit.lastEditorName = name;
        kit.lastEditorUuid = uuid;
        kit.lastEditedAt = now;
    }

    private String editorName(Player editor) {
        if (editor == null) {
            return "系统";
        }
        String name = editor.getName();
        return name == null || name.isBlank() ? "未知管理员" : name;
    }

    public synchronized void setSelection(UUID uuid, Role role, String kitId) {
        reloadIfChanged();
        if (uuid == null || role == null) {
            return;
        }
        if (kitId == null || kitId.isBlank()) {
            Map<Role, String> selected = selections.get(uuid);
            if (selected != null) {
                selected.remove(role);
                if (selected.isEmpty()) {
                    selections.remove(uuid);
                }
            }
            save();
            return;
        }

        String normalizedId = kitId.toLowerCase(Locale.ROOT);
        Kit kit = kits.get(normalizedId);
        if (kit == null || kit.role() != role) {
            return;
        }
        selections.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(role, normalizedId);
        save();
    }

    public synchronized String getSelection(UUID uuid, Role role) {
        reloadIfChanged();
        if (uuid == null || role == null) {
            return null;
        }
        Map<Role, String> selected = selections.get(uuid);
        if (selected == null) {
            return null;
        }
        String kitId = selected.get(role);
        Kit kit = kitId == null ? null : kits.get(kitId);
        if (kit == null || kit.role() != role) {
            selected.remove(role);
            if (selected.isEmpty()) {
                selections.remove(uuid);
            }
            return null;
        }
        return kitId;
    }

    public Kit getKit(String kitId) {
        reloadIfChanged();
        return kitId == null ? null : kits.get(kitId.toLowerCase(Locale.ROOT));
    }

    public List<Kit> getKits(Role role) {
        reloadIfChanged();
        List<Kit> result = new ArrayList<>();
        for (Kit kit : kits.values()) {
            if (kit.role() == role) {
                result.add(kit);
            }
        }
        result.sort(Comparator.comparing(Kit::id));
        return result;
    }

    public Kit pickKit(UUID playerId, Role role) {
        List<Kit> candidates = getKits(role);
        if (candidates.isEmpty()) {
            return null;
        }
        double total = 0.0D;
        for (Kit kit : candidates) {
            total += Math.max(0, kit.chance());
        }
        if (total <= 0.0D) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0.0D;
        for (Kit kit : candidates) {
            cursor += Math.max(0, kit.chance());
            if (roll < cursor) {
                return kit;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    public Kit applyKit(Player player, Role role) {
        if (player == null || role == null) {
            return null;
        }
        Kit kit = pickKit(player.getUniqueId(), role);
        player.getInventory().clear();
        player.getEnderChest().clear();
        if (kit == null || kit.items().isEmpty()) {
            giveFallbackKit(player, role);
            return null;
        }
        if (syncKitLore(kit)) {
            save(false);
        }
        player.getInventory().setStorageContents(kit.storageContents());
        player.getInventory().setArmorContents(kit.armorContents());
        player.getInventory().setItemInOffHand(kit.offHandItem());
        applyEnderChestKit(player, kit);
        applyPersonalOverrides(player, role, kit);
        applyStartExperience(player, kit);
        return kit;
    }

    public void applyStartExperience(Player player, Kit kit) {
        if (player == null || kit == null) {
            return;
        }
        int level = clampStartExpLevel(kit.startExpLevel());
        player.setTotalExperience(0);
        player.setLevel(level);
        player.setExp(0.0F);
    }

    public void sendKitGuide(Player player, Kit kit) {
        if (player == null || kit == null || !kit.hasGuide()) {
            return;
        }
        List<String> lines = kit.guideLines();
        if (lines.isEmpty()) {
            return;
        }
        String prefix = plugin.getConfigManager().getHunterGamePrefix();
        player.sendMessage(prefix + "§x§B§B§8§8§F§F✦ §dKit玩法提示 §8- §f" + kit.displayName());
        player.sendMessage(prefix + "§x§D§D§A§A§F§F» §d创建者 §f" + kit.creatorName()
                + " §8/ §7" + kit.createdAtText());
        player.sendMessage(prefix + "§x§8§8§D§D§F§F» §b最后编辑 §f" + kit.lastEditorName()
                + " §8/ §e" + kit.lastEditedAtText());
        for (String line : lines) {
            player.sendMessage(prefix + "§x§8§8§D§D§F§F» §f" + line);
        }
        String firstLine = lines.get(0);
        if (firstLine.length() > 36) {
            firstLine = firstLine.substring(0, 36) + "…";
        }
        player.sendTitle("§x§B§B§8§8§F§F✦ §dKit玩法提示", "§f" + firstLine, 5, 90, 10);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.28f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.48f, 1.72f);
    }

    private void applyPersonalOverrides(Player player, Role role, Kit kit) {
        if (player == null || role == null || kit == null) {
            return;
        }
        KitLayout personalLayout = plugin.getPlayerDataManager()
                .getEndFlashPersonalKitLayout(player.getUniqueId(), role, kit.id());
        if (layoutHasAnyItem(personalLayout)) {
            player.getInventory().setStorageContents(personalLayout.storageContents());
            player.getInventory().setArmorContents(personalLayout.armorContents());
            player.getInventory().setItemInOffHand(personalLayout.offHandItem());
        }

        ItemStack[] personalEnderChest = plugin.getPlayerDataManager()
                .getEndFlashPersonalEnderChestLayout(player.getUniqueId(), role, kit.id(), kit.enderChestSize());
        if (hasAnyItem(personalEnderChest)) {
            org.bukkit.inventory.Inventory enderChest = player.getEnderChest();
            enderChest.clear();
            int limit = Math.min(personalEnderChest.length, enderChest.getSize());
            for (int i = 0; i < limit; i++) {
                ItemStack item = personalEnderChest[i];
                if (item != null && item.getType() != Material.AIR) {
                    enderChest.setItem(i, item.clone());
                }
            }
        }
    }

    public void applyEnderChestKit(Player player, Kit kit) {
        if (player == null || kit == null) {
            return;
        }
        if (!hasAnyItem(kit.enderChestContents())) {
            return;
        }
        org.bukkit.inventory.Inventory enderChest = player.getEnderChest();
        enderChest.clear();
        ItemStack[] contents = kit.enderChestContents();
        int limit = Math.min(contents.length, enderChest.getSize());
        for (int i = 0; i < limit; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                enderChest.setItem(i, item.clone());
            }
        }
    }

    public List<ItemStack> snapshotEnderChest(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(syncFlashItemCopy(item));
            }
        }
        return items;
    }

    public List<ItemStack> snapshotInventory(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(syncFlashItemCopy(item));
            }
        }
        return items;
    }

    public KitLayout snapshotInventoryLayout(Player player) {
        if (player == null) {
            return new KitLayout(null, null, null);
        }
        return new KitLayout(syncFlashArrayCopy(player.getInventory().getStorageContents(), 36),
                syncFlashArrayCopy(player.getInventory().getArmorContents(), 4),
                syncFlashItemCopy(player.getInventory().getItemInOffHand()));
    }

    public List<ItemStack> snapshotHand(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return Collections.emptyList();
        }
        return List.of(syncFlashItemCopy(item));
    }

    private void copyLocalFileToSharedIfMissing(File localFile, File sharedFile) {
        if (localFile == null || sharedFile == null || !localFile.exists() || sharedFile.exists()) {
            return;
        }
        try {
            File parent = sharedFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.copy(localFile.toPath(), sharedFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            plugin.getLogger().info("已把本地终章闪光 Kit 数据复制到跨服共享目录。");
        } catch (IOException exception) {
            plugin.getLogger().warning("无法迁移本地终章闪光 Kit 数据到共享目录: " + exception.getMessage());
        }
    }

    private void updateLoadedFileState() {
        lastLoadedModified = file.exists() ? file.lastModified() : -1L;
        lastLoadedLength = file.exists() ? file.length() : -1L;
    }

    private void giveFallbackKit(Player player, Role role) {
        if (role == Role.PREY) {
            player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
            player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
        } else {
            player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
            player.getInventory().addItem(new ItemStack(Material.BOW));
            player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 12));
        }
    }

    private String makeUniqueId(String displayName) {
        String base = displayName == null ? "kit" : displayName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-\\p{IsHan}]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (base.isBlank()) {
            base = "kit";
        }
        String id = base;
        int index = 2;
        while (kits.containsKey(id)) {
            id = base + "_" + index++;
        }
        return id;
    }

    public static String formatChance(double chance) {
        double value = clampChance(chance);
        if (Math.abs(value - Math.rint(value)) < 1.0E-9D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static double clampChance(double chance) {
        if (!Double.isFinite(chance)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(50.0D, chance));
    }

    public static int clampStartExpLevel(int level) {
        return Math.max(0, Math.min(100, level));
    }

    private static String normalizeGuideText(String guide) {
        if (guide == null) {
            return "";
        }
        String normalized = guide.replace('&', '§')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (normalized.equalsIgnoreCase("无")
                || normalized.equalsIgnoreCase("none")
                || normalized.equalsIgnoreCase("clear")
                || normalized.equalsIgnoreCase("清空")) {
            return "";
        }
        if (normalized.length() > 500) {
            normalized = normalized.substring(0, 500).trim();
        }
        return normalized;
    }

    private static List<String> splitGuideLines(String guide) {
        String normalized = normalizeGuideText(guide);
        if (normalized.isBlank()) {
            return Collections.emptyList();
        }
        String[] pieces = normalized.replace("\\n", "\n").split("\\R|\\s*\\|\\s*");
        List<String> lines = new ArrayList<>();
        for (String piece : pieces) {
            String line = piece == null ? "" : piece.trim();
            if (!line.isBlank()) {
                lines.add(line);
            }
            if (lines.size() >= 8) {
                break;
            }
        }
        return lines;
    }

    public static int clampEnderChestSize(int size) {
        int normalized = Math.max(MIN_ENDER_CHEST_SIZE, Math.min(MAX_ENDER_CHEST_SIZE, size));
        return ((normalized + 8) / 9) * 9;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType() == Material.AIR ? null : item.clone();
    }

    private static ItemStack[] emptyArray(int size) {
        return new ItemStack[size];
    }

    private static ItemStack[] normalizeArray(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source == null) {
            return result;
        }
        for (int i = 0; i < result.length && i < source.length; i++) {
            result[i] = cloneOrNull(source[i]);
        }
        return result;
    }

    private ItemStack syncFlashItemCopy(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemStack copy = item.clone();
        if (plugin.getFlashModeManager() == null) {
            return copy;
        }
        return plugin.getFlashModeManager().syncFlashItemLore(copy);
    }

    private ItemStack[] syncFlashArrayCopy(ItemStack[] source, int size) {
        ItemStack[] copy = normalizeArray(source, size);
        if (plugin.getFlashModeManager() != null) {
            plugin.getFlashModeManager().syncFlashItemLore(copy);
        }
        return copy;
    }

    private List<ItemStack> syncFlashItemList(List<ItemStack> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : source) {
            if (item != null && item.getType() != Material.AIR) {
                result.add(syncFlashItemCopy(item));
            }
        }
        return result;
    }

    private boolean syncKitLore(Kit kit) {
        if (kit == null || plugin.getFlashModeManager() == null) {
            return false;
        }
        boolean changed = false;
        changed |= plugin.getFlashModeManager().syncFlashItemLore(kit.storageContents);
        changed |= plugin.getFlashModeManager().syncFlashItemLore(kit.armorContents);
        changed |= plugin.getFlashModeManager().syncFlashItemLore(kit.enderChestContents);

        ItemStack oldOffhand = kit.offHandItem;
        ItemStack newOffhand = syncFlashItemCopy(oldOffhand);
        if (!sameItem(oldOffhand, newOffhand)) {
            kit.offHandItem = newOffhand;
            changed = true;
        }

        if (changed) {
            kit.rebuildCompactItems();
        }
        return changed;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || first.getType() == Material.AIR) {
            return second == null || second.getType() == Material.AIR;
        }
        return first.equals(second);
    }

    public synchronized void reloadIfChanged() {
        long diskLastModified = file.exists() ? file.lastModified() : -1L;
        long diskLength = file.exists() ? file.length() : -1L;
        if (diskLastModified != lastLoadedModified || diskLength != lastLoadedLength) {
            load();
        }
    }

    private static void addNonAirItems(List<ItemStack> target, ItemStack[] source) {
        if (target == null || source == null) {
            return;
        }
        for (ItemStack item : source) {
            if (item != null && item.getType() != Material.AIR) {
                target.add(item.clone());
            }
        }
    }

    private static ItemStack[] compactItemsToStorage(List<ItemStack> items) {
        ItemStack[] storage = new ItemStack[36];
        if (items == null) {
            return storage;
        }
        int slot = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (slot >= storage.length) {
                break;
            }
            storage[slot++] = item.clone();
        }
        return storage;
    }

    private static ItemStack[] compactItemsToEnderContents(List<ItemStack> items) {
        ItemStack[] storage = new ItemStack[MAX_ENDER_CHEST_SIZE];
        if (items == null) {
            return storage;
        }
        int slot = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (slot >= storage.length) {
                break;
            }
            storage[slot++] = item.clone();
        }
        return storage;
    }

    private static boolean isEmptyItem(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private static boolean hasAnyItem(ItemStack[] items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (!isEmptyItem(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean layoutHasAnyItem(KitLayout layout) {
        return layout != null && (hasAnyItem(layout.storageContents()) || hasAnyItem(layout.armorContents()) || !isEmptyItem(layout.offHandItem()));
    }

    private List<String> encodeItems(List<ItemStack> items) {
        List<String> encoded = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            encoded.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        }
        return encoded;
    }

    private List<String> encodeSlotArray(ItemStack[] items) {
        List<String> encoded = new ArrayList<>();
        ItemStack[] normalized = normalizeArray(items, items == null ? 0 : items.length);
        for (ItemStack item : normalized) {
            encoded.add(encodeItem(item));
        }
        return encoded;
    }

    private String encodeItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private List<ItemStack> decodeItems(List<String> encoded) {
        List<ItemStack> items = new ArrayList<>();
        for (String text : encoded) {
            if (text == null || text.isBlank()) {
                continue;
            }
            try {
                items.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(text)));
            } catch (Exception exception) {
                Bukkit.getLogger().warning("[GameFunXiao] 跳过损坏的终章闪光 Kit 物品数据。");
            }
        }
        return items;
    }

    private ItemStack[] decodeSlotArray(List<String> encoded, int size) {
        ItemStack[] items = new ItemStack[size];
        if (encoded == null) {
            return items;
        }
        for (int i = 0; i < items.length && i < encoded.size(); i++) {
            items[i] = decodeItem(encoded.get(i));
        }
        return items;
    }

    private ItemStack decodeItem(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(text));
        } catch (Exception exception) {
            Bukkit.getLogger().warning("[GameFunXiao] 跳过损坏的终章闪光 Kit 槽位数据。");
            return null;
        }
    }
}
