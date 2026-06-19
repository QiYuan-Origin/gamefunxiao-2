package org.gamefunxiao.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.HunterGameBackendMode;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.world.BrickGuardMapManager;
import org.gamefunxiao.world.MiniGameMapManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChildServerManager implements PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String END_FLASH_KIT_SYNC_CHANNEL = "GameFunEndFlashKitSync";
    private static final String END_FLASH_KIT_SYNC_REQUEST_CHANNEL = "GameFunEndFlashKitRequest";
    private static final int END_FLASH_KIT_SYNC_CHUNK_SIZE = 24_000;
    private static final long END_FLASH_KIT_SYNC_REQUEST_COOLDOWN = 30_000L;
    private static final long END_FLASH_KIT_SYNC_BUFFER_TTL = 120_000L;
    private static final long REGISTRY_STALE_MILLIS = 90_000L;
    private static final long EMPTY_REGISTRY_STALE_MILLIS = 20_000L;

    private final GameFunXiao plugin;
    private final String syncInstanceId = UUID.randomUUID().toString();
    private final boolean enabled;
    private final ChildServerRole role;
    private final File registryFolder;
    private final File crossRequestFolder;
    private final File templateEditRequestFolder;
    private final File endFlashTuningRequestFolder;
    private final File miniGameMapEditRequestFolder;
    private final File brickGuardMapEditRequestFolder;
    private final File bootstrapFile;
    private final String lobbyServerName;
    private final String crossLobbyServerName;
    private final String crossGameServerName;
    private final Map<String, NodeDefinition> lobbyNodes = new LinkedHashMap<>();
    private final Set<UUID> pendingCrossRoomCreations = ConcurrentHashMap.newKeySet();
    private final Set<UUID> transferringPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, BukkitTask> startTasks = new ConcurrentHashMap<>();
    private final Map<String, InetSocketAddress> crossServerEndpoints = new ConcurrentHashMap<>();
    private final Map<String, Long> recentAdvertiseIds = new ConcurrentHashMap<>();

    private ChildRoomBootstrap pendingBootstrap;
    private String activeManagedRoomId;
    private BukkitTask nodeSyncTask;
    private BukkitTask crossSyncTask;
    private BukkitTask pendingEndFlashKitSyncTask;
    private long lastEndFlashKitSyncRequestAt;
    private final Set<String> managedCrossServerRooms = ConcurrentHashMap.newKeySet();
    private final Map<String, KitSyncBuffer> pendingEndFlashKitSyncs = new ConcurrentHashMap<>();

    public ChildServerManager(GameFunXiao plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfigManager().getConfig();
        this.enabled = config.getBoolean("child_server.enabled", false);
        this.role = enabled
                ? ChildServerRole.fromString(config.getString("child_server.role", "standalone"))
                : ChildServerRole.STANDALONE;
        this.lobbyServerName = config.getString("child_server.lobby_server_name", "lobby");
        this.crossLobbyServerName = plugin.getConfigManager().getCrossServerLobbyServerName();
        this.crossGameServerName = plugin.getConfigManager().getCrossServerGameServerName();

        String registryPath = config.getString("child_server.shared_storage.room_registry_path",
                new File(plugin.getDataFolder(), "child-room-registry").getAbsolutePath());
        this.registryFolder = new File(registryPath);
        if (!registryFolder.exists()) {
            registryFolder.mkdirs();
        }

        this.crossRequestFolder = new File(plugin.getConfigManager().getCrossServerRequestPath());
        if (!crossRequestFolder.exists()) {
            crossRequestFolder.mkdirs();
        }
        this.templateEditRequestFolder = new File(crossRequestFolder.getParentFile() == null
                ? plugin.getDataFolder()
                : crossRequestFolder.getParentFile(), "template-edit-requests");
        if (!templateEditRequestFolder.exists()) {
            templateEditRequestFolder.mkdirs();
        }
        this.endFlashTuningRequestFolder = new File(crossRequestFolder.getParentFile() == null
                ? plugin.getDataFolder()
                : crossRequestFolder.getParentFile(), "end-flash-tuning-requests");
        if (!endFlashTuningRequestFolder.exists()) {
            endFlashTuningRequestFolder.mkdirs();
        }
        this.miniGameMapEditRequestFolder = new File(crossRequestFolder.getParentFile() == null
                ? plugin.getDataFolder()
                : crossRequestFolder.getParentFile(), "minigame-map-edit-requests");
        if (!miniGameMapEditRequestFolder.exists()) {
            miniGameMapEditRequestFolder.mkdirs();
        }
        this.brickGuardMapEditRequestFolder = new File(crossRequestFolder.getParentFile() == null
                ? plugin.getDataFolder()
                : crossRequestFolder.getParentFile(), "brickguard-map-edit-requests");
        if (!brickGuardMapEditRequestFolder.exists()) {
            brickGuardMapEditRequestFolder.mkdirs();
        }

        this.bootstrapFile = new File(plugin.getDataFolder(), "child-bootstrap.yml");
        if (role == ChildServerRole.NODE) {
            this.pendingBootstrap = ChildRoomBootstrap.load(bootstrapFile);
        }

        if (role == ChildServerRole.LOBBY) {
            loadLobbyNodes(config.getConfigurationSection("child_server.lobby.nodes"));
        }
    }

    public void registerMessaging() {
        // 这里必须常驻注册，配置 reload 后可能从当前服模式切到 cross_server。
        // 如果只按启动时配置注册，后续点击菜单发送 ServerIP 就会抛 ChannelNotRegisteredException。
        if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, BUNGEE_CHANNEL)) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        }
        if (!plugin.getServer().getMessenger().isIncomingChannelRegistered(plugin, BUNGEE_CHANNEL)) {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
        }
    }

    public void unregisterMessaging() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLobbyMode() {
        return enabled && role == ChildServerRole.LOBBY;
    }

    public boolean isNodeMode() {
        return enabled && role == ChildServerRole.NODE;
    }

    public boolean isCrossServerBackendConfigured() {
        return plugin.getConfigManager().getHunterGameBackendMode() == HunterGameBackendMode.CROSS_SERVER
                && plugin.getConfigManager().isCrossServerBackendEnabled();
    }

    public boolean isManagedCrossServerRoom(String roomId) {
        return roomId != null && managedCrossServerRooms.contains(roomId);
    }

    public Collection<ChildRoomRegistryEntry> getLobbyRoomEntries() {
        if (!isLobbyMode() && !isCrossServerBackendConfigured()) {
            return Collections.emptyList();
        }
        return loadAllRegistryEntries();
    }

    public ChildRoomRegistryEntry getLobbyRoomEntry(String roomId) {
        if ((!isLobbyMode() && !isCrossServerBackendConfigured()) || roomId == null || roomId.isBlank()) {
            return null;
        }
        return loadRegistryEntry(roomId);
    }

    public String getActiveManagedRoomId() {
        return activeManagedRoomId;
    }

    public boolean isManagedNodeRoom(String roomId) {
        return isNodeMode() && roomId != null && roomId.equals(activeManagedRoomId);
    }

    public boolean isTransferInProgress(UUID uuid) {
        return uuid != null && transferringPlayers.contains(uuid);
    }

    public void markTransferHandled(UUID uuid) {
        if (uuid != null) {
            transferringPlayers.remove(uuid);
        }
    }

    public void shutdown() {
        unregisterMessaging();
        for (BukkitTask task : startTasks.values()) {
            task.cancel();
        }
        startTasks.clear();

        if (nodeSyncTask != null) {
            nodeSyncTask.cancel();
            nodeSyncTask = null;
        }
        if (crossSyncTask != null) {
            crossSyncTask.cancel();
            crossSyncTask = null;
        }
        if (pendingEndFlashKitSyncTask != null) {
            pendingEndFlashKitSyncTask.cancel();
            pendingEndFlashKitSyncTask = null;
        }
        pendingEndFlashKitSyncs.clear();
    }

    public int cleanupStaleRegistryEntriesOnStartup() {
        return cleanupStaleRegistryEntries(true);
    }

    public int cleanupAllRegistryEntries() {
        int removed = 0;
        File[] files = registryFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (deleteRegistryFile(file, "管理员手动清理跨服房间注册")) {
                removed++;
            }
        }
        managedCrossServerRooms.clear();
        return removed;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!BUNGEE_CHANNEL.equals(channel) || message == null || message.length == 0) {
            return;
        }

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();
            if ("GameFunAdvertise".equals(subChannel)) {
                handleForwardedAdvertise(input);
                return;
            }
            if (END_FLASH_KIT_SYNC_CHANNEL.equals(subChannel)) {
                handleEndFlashKitSync(input);
                return;
            }
            if (END_FLASH_KIT_SYNC_REQUEST_CHANNEL.equals(subChannel)) {
                handleEndFlashKitSyncRequest(input);
                return;
            }
            if (!"ServerIP".equals(subChannel)) {
                return;
            }

            String serverName = input.readUTF();
            String host = input.readUTF();
            int port = input.readUnsignedShort();
            crossServerEndpoints.put(normalizeServerKey(serverName), InetSocketAddress.createUnresolved(host, port));
        } catch (Exception exception) {
            plugin.getLogger().warning("解析 Bungee ServerIP 消息失败: " + exception.getMessage());
        }
    }

    private void handleForwardedAdvertise(ByteArrayDataInput input) {
        short length = input.readShort();
        byte[] payload = new byte[length];
        input.readFully(payload);
        ByteArrayDataInput payloadInput = ByteStreams.newDataInput(payload);
        String roomId = payloadInput.readUTF();
        String messageText = payloadInput.readUTF();
        String hoverText = payloadInput.readUTF();
        String advertiseId = payloadInput.readUTF();

        long now = System.currentTimeMillis();
        recentAdvertiseIds.entrySet().removeIf(entry -> now - entry.getValue() > 120000L);
        Long previous = recentAdvertiseIds.putIfAbsent(advertiseId, now);
        if (previous != null && now - previous <= 120000L) {
            return;
        }

        net.kyori.adventure.text.Component message = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .deserialize(messageText)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/gamefunxiao hg join " + roomId))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(hoverText)));

        for (Player target : Bukkit.getOnlinePlayers()) {
            GameRoom targetRoom = plugin.getRoomManager().getPlayerRoom(target.getUniqueId());
            if (targetRoom != null) {
                continue;
            }
            target.sendMessage(message);
            target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
    }

    public void scheduleEndFlashKitSync(File sourceFile) {
        if (pendingEndFlashKitSyncTask != null) {
            return;
        }
        pendingEndFlashKitSyncTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingEndFlashKitSyncTask = null;
            broadcastEndFlashKitSync(sourceFile);
        }, 12L);
    }

    public void requestEndFlashKitSync(Player carrier) {
        if (carrier == null || !carrier.isOnline()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastEndFlashKitSyncRequestAt < END_FLASH_KIT_SYNC_REQUEST_COOLDOWN) {
            return;
        }
        lastEndFlashKitSyncRequestAt = now;

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeUTF("KIT_SYNC_REQUEST_V1");
        payload.writeUTF(syncInstanceId);
        payload.writeUTF(resolveCurrentServerName());
        sendForwardPluginMessage(carrier, "ALL", END_FLASH_KIT_SYNC_REQUEST_CHANNEL, payload.toByteArray());
    }

    private void handleEndFlashKitSyncRequest(ByteArrayDataInput input) {
        int length = input.readUnsignedShort();
        if (length <= 0) {
            return;
        }
        byte[] payload = new byte[length];
        input.readFully(payload);
        ByteArrayDataInput payloadInput = ByteStreams.newDataInput(payload);
        String marker = payloadInput.readUTF();
        if (!"KIT_SYNC_REQUEST_V1".equals(marker)) {
            return;
        }
        String originId = payloadInput.readUTF();
        if (syncInstanceId.equals(originId)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastEndFlashKitSync(null), 10L);
    }

    private void broadcastEndFlashKitSync(File sourceFile) {
        if (plugin.getEndFlashKitManager() == null) {
            return;
        }
        Player carrier = findPluginMessageCarrier();
        if (carrier == null) {
            return;
        }
        try {
            byte[] yamlBytes = plugin.getEndFlashKitManager().exportSyncBytes();
            if (yamlBytes.length == 0) {
                return;
            }
            byte[] compressed = gzip(yamlBytes);
            CRC32 crc32 = new CRC32();
            crc32.update(compressed);
            long checksum = crc32.getValue();

            String syncId = UUID.randomUUID().toString();
            String sourceName = resolveCurrentServerName();
            int totalChunks = Math.max(1, (compressed.length + END_FLASH_KIT_SYNC_CHUNK_SIZE - 1) / END_FLASH_KIT_SYNC_CHUNK_SIZE);
            for (int index = 0; index < totalChunks; index++) {
                int offset = index * END_FLASH_KIT_SYNC_CHUNK_SIZE;
                int length = Math.min(END_FLASH_KIT_SYNC_CHUNK_SIZE, compressed.length - offset);

                ByteArrayDataOutput payload = ByteStreams.newDataOutput();
                payload.writeUTF("KIT_SYNC_V1");
                payload.writeUTF(syncInstanceId);
                payload.writeUTF(sourceName);
                payload.writeUTF(syncId);
                payload.writeInt(index);
                payload.writeInt(totalChunks);
                payload.writeInt(compressed.length);
                payload.writeLong(checksum);
                payload.writeInt(length);
                payload.write(compressed, offset, length);

                sendForwardPluginMessage(carrier, "ALL", END_FLASH_KIT_SYNC_CHANNEL, payload.toByteArray());
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("同步终章闪光 Kit 到其他服务端失败: " + exception.getMessage());
        }
    }

    private void handleEndFlashKitSync(ByteArrayDataInput input) {
        cleanupExpiredEndFlashKitSyncBuffers();
        int length = input.readUnsignedShort();
        if (length <= 0) {
            return;
        }
        byte[] payload = new byte[length];
        input.readFully(payload);
        ByteArrayDataInput payloadInput = ByteStreams.newDataInput(payload);
        String marker = payloadInput.readUTF();
        if (!"KIT_SYNC_V1".equals(marker)) {
            return;
        }

        String originId = payloadInput.readUTF();
        if (syncInstanceId.equals(originId)) {
            return;
        }
        String sourceName = payloadInput.readUTF();
        String syncId = payloadInput.readUTF();
        int index = payloadInput.readInt();
        int totalChunks = payloadInput.readInt();
        int totalLength = payloadInput.readInt();
        long checksum = payloadInput.readLong();
        int chunkLength = payloadInput.readInt();
        if (totalChunks <= 0 || totalChunks > 1024 || totalLength <= 0 || chunkLength <= 0
                || index < 0 || index >= totalChunks || chunkLength > END_FLASH_KIT_SYNC_CHUNK_SIZE + 512) {
            return;
        }
        byte[] chunk = new byte[chunkLength];
        payloadInput.readFully(chunk);

        String key = originId + ":" + syncId;
        KitSyncBuffer buffer = pendingEndFlashKitSyncs.computeIfAbsent(key,
                ignored -> new KitSyncBuffer(sourceName, totalChunks, totalLength, checksum));
        if (!buffer.accept(index, chunk, totalChunks, totalLength, checksum)) {
            pendingEndFlashKitSyncs.remove(key);
            return;
        }
        if (!buffer.isComplete()) {
            return;
        }
        pendingEndFlashKitSyncs.remove(key);

        try {
            byte[] compressed = buffer.join();
            CRC32 crc32 = new CRC32();
            crc32.update(compressed);
            if (crc32.getValue() != checksum) {
                plugin.getLogger().warning("终章闪光 Kit 跨服同步校验失败，已丢弃本次数据。");
                return;
            }
            byte[] yamlBytes = gunzip(compressed);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getEndFlashKitManager() != null) {
                    plugin.getEndFlashKitManager().importSyncedBytes(yamlBytes, buffer.sourceName);
                }
            });
        } catch (Exception exception) {
            plugin.getLogger().warning("读取终章闪光 Kit 跨服同步数据失败: " + exception.getMessage());
        }
    }

    private void cleanupExpiredEndFlashKitSyncBuffers() {
        long now = System.currentTimeMillis();
        pendingEndFlashKitSyncs.entrySet().removeIf(entry -> now - entry.getValue().createdAt > END_FLASH_KIT_SYNC_BUFFER_TTL);
    }

    private void sendForwardPluginMessage(Player carrier, String targetServer, String subChannel, byte[] payloadBytes) {
        if (carrier == null || !carrier.isOnline() || payloadBytes == null || payloadBytes.length == 0) {
            return;
        }
        if (payloadBytes.length > 30_000) {
            plugin.getLogger().warning("跨服插件消息过大，已取消发送: " + subChannel + " / " + payloadBytes.length + " bytes");
            return;
        }

        registerMessaging();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Forward");
        output.writeUTF(targetServer == null || targetServer.isBlank() ? "ALL" : targetServer);
        output.writeUTF(subChannel);
        output.writeShort(payloadBytes.length);
        output.write(payloadBytes);
        try {
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, output.toByteArray());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("发送跨服插件消息失败: " + exception.getMessage());
        }
    }

    private Player findPluginMessageCarrier() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    private String resolveCurrentServerName() {
        String serverName = applyPlaceholders(null, "%qichengmorebungeeapi_server%");
        if (serverName != null && !serverName.isBlank() && !serverName.contains("%qichengmorebungeeapi_server%")) {
            return serverName.trim();
        }
        try {
            Path path = Path.of(System.getProperty("user.dir", "")).getFileName();
            if (path != null && !path.toString().isBlank()) {
                return path.toString().trim();
            }
        } catch (RuntimeException ignored) {
        }
        return role == null ? "unknown" : role.name().toLowerCase(Locale.ROOT);
    }

    private byte[] gzip(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(source);
        }
        return output.toByteArray();
    }

    private byte[] gunzip(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(source))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    public void forwardRoomAdvertiseToLobby(Player carrier, GameRoom room, String messageText) {
        if (carrier == null || !carrier.isOnline() || room == null || messageText == null || messageText.isBlank()) {
            return;
        }
        if (!plugin.getConfigManager().isCrossServerAdvertiseEnabled()) {
            return;
        }
        if (crossLobbyServerName == null || crossLobbyServerName.isBlank()) {
            return;
        }

        registerMessaging();

        String max = room.getMaxPlayers() == -1 ? "∞" : String.valueOf(room.getMaxPlayers());
        String hoverText = "§a§l点击加入房间\n\n§7房间ID: §e" + room.getRoomId()
                + "\n§7模式: §b" + room.getModeName()
                + "\n§7人数: §a" + room.getPlayerCount() + "§7/§a" + max;
        String advertiseId = room.getRoomId() + ":" + carrier.getUniqueId() + ":" + System.currentTimeMillis();

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeUTF(room.getRoomId());
        payload.writeUTF(messageText);
        payload.writeUTF(hoverText);
        payload.writeUTF(advertiseId);
        byte[] payloadBytes = payload.toByteArray();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Forward");
        output.writeUTF(crossLobbyServerName);
        output.writeUTF("GameFunAdvertise");
        output.writeShort(payloadBytes.length);
        output.write(payloadBytes);

        try {
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, output.toByteArray());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("转发房间宣传到 " + crossLobbyServerName + " 失败: " + exception.getMessage());
        }
    }

    public void requestCrossServerEndpoint(Player carrier) {
        if (!isCrossServerBackendConfigured() || carrier == null || !carrier.isOnline()) {
            return;
        }
        requestServerEndpoint(carrier, crossGameServerName);
    }

    public boolean createCrossServerRoom(Player owner, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        if (!isCrossServerBackendConfigured()) {
            return false;
        }

        if (owner == null || !owner.isOnline()) {
            return true;
        }

        UUID ownerId = owner.getUniqueId();
        if (isTransferInProgress(ownerId) || !pendingCrossRoomCreations.add(ownerId)) {
            owner.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(mode, "room.cross_server_transferring"));
            return true;
        }

        Set<String> modifierSnapshot = modifiers == null ? new HashSet<>() : new HashSet<>(modifiers);
        if (!isCrossServerAvailableNow(owner, crossGameServerName, false)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (owner == null || !owner.isOnline()) {
                        return;
                    }
                    if (!isCrossServerAvailableNow(owner, crossGameServerName, true)) {
                        return;
                    }
                    createCrossServerRoomNow(owner, mode, maxPlayers, isPublic, modifierSnapshot);
                } finally {
                    pendingCrossRoomCreations.remove(ownerId);
                }
            }, 20L);
            return true;
        }

        try {
            createCrossServerRoomNow(owner, mode, maxPlayers, isPublic, modifierSnapshot);
        } finally {
            pendingCrossRoomCreations.remove(ownerId);
        }
        return true;
    }

    private void createCrossServerRoomNow(Player owner, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        String roomId = plugin.getRoomManager().generateExternalRoomId();
        ItemStack[] snapshot = cloneInventory(owner.getInventory().getContents());
        Location previousLocation = owner.getLocation().clone();

        try {
            CrossServerRoomRequest request = new CrossServerRoomRequest(
                    owner.getUniqueId(),
                    owner.getName(),
                    roomId,
                    mode,
                    maxPlayers,
                    isPublic,
                    modifiers,
                    crossLobbyServerName,
                    System.currentTimeMillis()
            );
            request.save(getCrossRequestFile(owner.getUniqueId()));
        } catch (IOException exception) {
            plugin.getLogger().severe("保存跨服创建房间请求失败: " + exception.getMessage());
            owner.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(mode, "room.cross_server_start_failed"));
            return;
        }

        plugin.getPlayerDataManager().saveTransferSession(owner, previousLocation, snapshot, roomId, crossGameServerName);
        owner.getInventory().clear();

        ChildRoomRegistryEntry entry = new ChildRoomRegistryEntry();
        entry.setRoomId(roomId);
        entry.setOwnerUuid(owner.getUniqueId());
        entry.setOwnerName(owner.getName());
        entry.setMode(mode);
        entry.setMaxPlayers(maxPlayers);
        entry.setPublic(isPublic);
        entry.setModifiers(modifiers);
        entry.setNodeId("cross-server");
        entry.setProxyServerName(crossGameServerName);
        entry.setHost("proxy");
        entry.setPort(0);
        entry.setState(RoomState.WAITING);
        entry.setCurrentPlayers(0);
        entry.setCreatedAt(System.currentTimeMillis());
        saveRegistryEntry(entry);

        owner.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(mode, "room.cross_server_transferring"));
        connectPlayer(owner, crossGameServerName);
    }

    public boolean joinCrossServerRoom(Player player, String roomId) {
        if (!isCrossServerBackendConfigured()) {
            return false;
        }

        ChildRoomRegistryEntry entry = loadRegistryEntry(roomId);
        if (entry == null) {
            return false;
        }

        if (entry.getState() != RoomState.WAITING && entry.getState() != RoomState.STARTING) {
            if ((entry.getMode() == GameMode.FLASH || entry.getMode() == GameMode.END_FLASH
                    || entry.getMode().isLuckyPillars())
                    && entry.getState() == RoomState.PLAYING) {
                return spectateCrossServerRoom(player, roomId);
            }
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.game_started"));
            return true;
        }

        if (!entry.isPublic()) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.invite_only"));
            return true;
        }

        if (entry.getMaxPlayers() != -1 && entry.getCurrentPlayers() >= entry.getMaxPlayers()) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.join_failed"));
            return true;
        }

        if (!ensureCrossServerAvailable(player, entry.getProxyServerName())) {
            return true;
        }

        ItemStack[] snapshot = cloneInventory(player.getInventory().getContents());
        plugin.getPlayerDataManager().saveTransferSession(player, player.getLocation().clone(), snapshot, roomId, entry.getProxyServerName());
        player.getInventory().clear();

        entry.setCurrentPlayers(entry.getCurrentPlayers() + 1);
        saveRegistryEntry(entry);

        player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.cross_server_join_loading"));
        connectPlayer(player, entry.getProxyServerName());
        return true;
    }

    public boolean spectateCrossServerRoom(Player player, String roomId) {
        if (!isCrossServerBackendConfigured()) {
            return false;
        }

        ChildRoomRegistryEntry entry = loadRegistryEntry(roomId);
        if (entry == null) {
            return false;
        }

        if (entry.getState() != RoomState.PLAYING && entry.getState() != RoomState.SELECTING) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.not_playing"));
            return true;
        }

        if (!ensureCrossServerAvailable(player, entry.getProxyServerName())) {
            return true;
        }

        ItemStack[] snapshot = cloneInventory(player.getInventory().getContents());
        plugin.getPlayerDataManager().saveTransferSession(player, player.getLocation().clone(), snapshot, roomId, entry.getProxyServerName(), true);
        player.getInventory().clear();
        player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.cross_server_join_loading"));
        connectPlayer(player, entry.getProxyServerName());
        return true;
    }

    public boolean handleCrossServerPlayerJoin(Player player) {
        if (!isCrossServerBackendConfigured()) {
            return false;
        }

        File requestFile = getCrossRequestFile(player.getUniqueId());
        CrossServerRoomRequest request = CrossServerRoomRequest.load(requestFile);
        if (request != null) {
            if (!isCurrentProxyServer(crossGameServerName)) {
                plugin.getLogger().warning("检测到跨服创建请求，但当前服不是目标小游戏服: current="
                        + resolveCurrentServerName() + ", target=" + crossGameServerName);
                return false;
            }
            requestFile.delete();
            GameRoom room = plugin.getRoomManager().createRoom(player, request.getMode(),
                    request.getMaxPlayers(), request.isPublicRoom(), request.getModifiers(),
                    request.getRoomId(), false);
            managedCrossServerRooms.add(room.getRoomId());
            saveCrossRoomSnapshot(room);
            startCrossSyncTask();
            player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.cross_server_ready"));
            return true;
        }

        String roomId = plugin.getPlayerDataManager().getProxyTransferRoomId(player.getUniqueId());
        if (roomId == null) {
            return false;
        }

        GameRoom room = plugin.getRoomManager().getRoom(roomId);
        if (room == null) {
            ChildRoomRegistryEntry entry = loadRegistryEntry(roomId);
            if (entry != null && player.getUniqueId().equals(entry.getOwnerUuid()) && isCurrentProxyServer(entry.getProxyServerName())) {
                room = plugin.getRoomManager().createRoom(player, entry.getMode(), entry.getMaxPlayers(),
                        entry.isPublic(), entry.getModifiers(), entry.getRoomId(), false);
                managedCrossServerRooms.add(room.getRoomId());
                saveCrossRoomSnapshot(room);
                startCrossSyncTask();
                player.sendMessage(plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.cross_server_ready"));
                return true;
            }
            if (isCurrentProxyServer(plugin.getPlayerDataManager().getProxyTransferTargetServer(player.getUniqueId()))) {
                player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("room.cross_server_wait_room"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        handleCrossServerPlayerJoin(player);
                    }
                }, 20L);
                return true;
            }
            return false;
        }

        boolean spectate = plugin.getPlayerDataManager().isProxyTransferSpectate(player.getUniqueId());
        if (spectate) {
            if (!room.isSpectator(player.getUniqueId())) {
                plugin.getRoomManager().spectateRoom(player, room);
                saveCrossRoomSnapshot(room);
            }
            return true;
        }

        if (!plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            plugin.getRoomManager().joinRoom(player, room, false);
            saveCrossRoomSnapshot(room);
        }
        return true;
    }

    public boolean requestTemplateLobbyEdit(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (!ensureCrossServerAvailable(player, crossGameServerName)) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.template_edit_failed"));
            return false;
        }

        File file = getTemplateEditRequestFile(player.getUniqueId());
        FileConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("uuid", player.getUniqueId().toString());
        config.set("name", player.getName());
        config.set("created_at", System.currentTimeMillis());
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存等待大厅模板编辑请求失败: " + exception.getMessage());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.template_edit_failed"));
            return false;
        }

        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.template_edit_request_sent"));
        connectPlayer(player, crossGameServerName);
        return true;
    }

    public boolean requestEndFlashTuning(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.already_in_room"));
            return true;
        }

        if (isCurrentProxyServer(crossGameServerName)) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_local"));
            return teleportToEndFlashTuningWorld(player);
        }

        if (!isCrossServerBackendConfigured()) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_cross_required"));
            return false;
        }

        if (!ensureCrossServerAvailable(player, crossGameServerName)) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_failed"));
            return false;
        }

        File file = getEndFlashTuningRequestFile(player.getUniqueId());
        FileConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("uuid", player.getUniqueId().toString());
        config.set("name", player.getName());
        config.set("created_at", System.currentTimeMillis());
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存终章调试世界传送请求失败: " + exception.getMessage());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_failed"));
            return false;
        }

        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_transferring"));
        playEndFlashTuningSound(player);
        connectPlayer(player, crossGameServerName);
        return true;
    }

    public boolean handleEndFlashTuningJoin(Player player) {
        if (player == null) {
            return false;
        }
        File file = getEndFlashTuningRequestFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        file.delete();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                teleportToEndFlashTuningWorld(player);
            }
        }, 10L);
        return true;
    }

    private boolean teleportToEndFlashTuningWorld(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        World world = plugin.getWorldManager().getOrCreateEndFlashTuningWorld();
        if (world == null) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_failed"));
            return false;
        }

        Location spawn = world.getSpawnLocation().clone();
        player.teleport(spawn);
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        plugin.getRoomManager().ensurePlayerRecipesAvailable(player);
        player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.end_flash_tune_joined"));
        playEndFlashTuningSound(player);
        return true;
    }

    public boolean handleTemplateLobbyEditJoin(Player player) {
        if (player == null) {
            return false;
        }
        File file = getTemplateEditRequestFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        file.delete();

        World templateWorld = plugin.getWorldManager().getTemplateLobbyWorld();
        if (templateWorld == null) {
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.template_not_found"));
            return true;
        }

        Location spawn = templateWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleport(spawn);
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("lobby.template_edit_joined"));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.25f);
        }, 10L);
        return true;
    }

    public boolean requestMiniGameMapEdit(Player player, GameMode mode, String mapId,
                                          MiniGameMapManager.EditWorldKind kind, int maxPlayers) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (mode == null) {
            mode = GameMode.LUCKY_PILLARS;
        }
        if (kind == null) {
            kind = MiniGameMapManager.EditWorldKind.GAME;
        }
        String normalizedMapId = plugin.getMiniGameMapManager().normalizeMapId(mapId);

        if (isCurrentProxyServer(crossGameServerName) || !isCrossServerBackendConfigured()) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_local"));
            return plugin.getMiniGameMapManager().enterEditorSession(player, mode, normalizedMapId, kind, maxPlayers);
        }

        if (!ensureCrossServerAvailable(player, crossGameServerName)) {
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_failed"));
            return false;
        }

        File file = getMiniGameMapEditRequestFile(player.getUniqueId());
        FileConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("uuid", player.getUniqueId().toString());
        config.set("name", player.getName());
        config.set("mode", mode.getId());
        config.set("map_id", normalizedMapId);
        config.set("kind", kind.id());
        config.set("max_players", maxPlayers);
        config.set("created_at", System.currentTimeMillis());
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存小游戏地图编辑请求失败: " + exception.getMessage());
            player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_failed"));
            return false;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", mode.getDisplayName());
        placeholders.put("id", normalizedMapId);
        placeholders.put("kind", kind.displayName());
        player.sendMessage(plugin.getMessageManager().getMiniGameMessageWithPrefix("minigame_map.edit_request_sent", placeholders));
        playMiniGameMapEditSound(player);
        connectPlayer(player, crossGameServerName);
        return true;
    }

    public boolean handleMiniGameMapEditJoin(Player player) {
        if (player == null) {
            return false;
        }
        File file = getMiniGameMapEditRequestFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        file.delete();

        GameMode mode = GameMode.fromId(config.getString("mode", GameMode.LUCKY_PILLARS.getId()));
        String mapId = config.getString("map_id", "default");
        MiniGameMapManager.EditWorldKind kind = MiniGameMapManager.EditWorldKind.fromString(config.getString("kind", "game"));
        int maxPlayers = config.getInt("max_players", -1);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getMiniGameMapManager().enterEditorSession(player, mode, mapId, kind, maxPlayers);
            }
        }, 10L);
        return true;
    }

    public boolean requestBrickGuardMapEdit(Player player, String mapId,
                                            BrickGuardMapManager.EditWorldKind kind, int maxPlayers) {
        if (player == null || !player.isOnline() || plugin.getBrickGuardMapManager() == null) {
            return false;
        }
        if (kind == null) {
            kind = BrickGuardMapManager.EditWorldKind.BRICK;
        }
        String normalizedMapId = plugin.getBrickGuardMapManager().normalizeMapId(mapId);

        if (isCurrentProxyServer(crossGameServerName) || !isCrossServerBackendConfigured()) {
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_local"));
            return plugin.getBrickGuardMapEditorManager().requestEdit(player, normalizedMapId, kind, maxPlayers);
        }

        if (!ensureCrossServerAvailable(player, crossGameServerName)) {
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_failed"));
            return false;
        }

        File file = getBrickGuardMapEditRequestFile(player.getUniqueId());
        FileConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("uuid", player.getUniqueId().toString());
        config.set("name", player.getName());
        config.set("map_id", normalizedMapId);
        config.set("kind", kind.id());
        config.set("max_players", maxPlayers);
        config.set("created_at", System.currentTimeMillis());
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存板砖守卫战地图编辑请求失败: " + exception.getMessage());
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_failed"));
            return false;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("map", normalizedMapId);
        placeholders.put("kind", kind.displayName());
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.map_edit_request_sent", placeholders));
        playMiniGameMapEditSound(player);
        connectPlayer(player, crossGameServerName);
        return true;
    }

    public boolean handleBrickGuardMapEditJoin(Player player) {
        if (player == null || plugin.getBrickGuardMapManager() == null) {
            return false;
        }
        File file = getBrickGuardMapEditRequestFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        file.delete();

        String mapId = config.getString("map_id", "default");
        BrickGuardMapManager.EditWorldKind kind = BrickGuardMapManager.EditWorldKind.fromString(config.getString("kind", "brick"));
        int maxPlayers = config.getInt("max_players", -1);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getBrickGuardMapEditorManager().requestEdit(player, mapId, kind, maxPlayers);
            }
        }, 10L);
        return true;
    }

    public void returnCrossServerRoomPlayerToLobby(Player player) {
        if (player == null) {
            return;
        }

        player.getInventory().clear();
        plugin.getRoomManager().resetPlayerForServerReturn(null, player);
        player.sendMessage(plugin.getMessageManager().getMessage("room.cross_server_returning_lobby"));
        connectPlayer(player, crossLobbyServerName);
    }

    public void returnCrossServerRoomPlayersToLobby(GameRoom room) {
        if (room == null) {
            return;
        }
        Set<UUID> everyone = new HashSet<>(room.getAllPlayerUUIDs());
        everyone.addAll(room.getSpectators());
        for (UUID uuid : everyone) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getRoomManager().restorePlayerAdvancements(player, room);
                returnCrossServerRoomPlayerToLobby(player);
            }
        }
    }

    public boolean createLobbyRoom(Player owner, GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) {
        if (!isLobbyMode()) {
            return false;
        }

        NodeDefinition node = findAvailableNode();
        if (node == null) {
            owner.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.child_server_no_idle_node"));
            return true;
        }

        String roomId = plugin.getRoomManager().generateExternalRoomId();
        ItemStack[] snapshot = cloneInventory(owner.getInventory().getContents());
        Location previousLocation = owner.getLocation().clone();

        plugin.getPlayerDataManager().saveTransferSession(owner, previousLocation, snapshot, roomId, node.proxyServerName);
        owner.getInventory().clear();
        owner.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.child_server_starting"));

        ChildRoomRegistryEntry entry = new ChildRoomRegistryEntry();
        entry.setRoomId(roomId);
        entry.setOwnerUuid(owner.getUniqueId());
        entry.setOwnerName(owner.getName());
        entry.setMode(mode);
        entry.setMaxPlayers(maxPlayers);
        entry.setPublic(isPublic);
        entry.setModifiers(modifiers);
        entry.setNodeId(node.id);
        entry.setProxyServerName(node.proxyServerName);
        entry.setHost(node.host);
        entry.setPort(node.port);
        entry.setState(RoomState.WAITING);
        entry.setCurrentPlayers(1);
        entry.setCreatedAt(System.currentTimeMillis());
        saveRegistryEntry(entry);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File runtimeDir = prepareRuntimeDirectory(node, roomId);
                writeBootstrap(runtimeDir, node, roomId, owner.getUniqueId(), mode, maxPlayers, isPublic, modifiers);
                updateServerProperties(runtimeDir, node, roomId);
                startNodeProcess(runtimeDir, node);

                Bukkit.getScheduler().runTask(plugin, () -> waitAndConnectOwner(owner, node.proxyServerName, node, roomId, snapshot));
            } catch (Exception exception) {
                plugin.getLogger().severe("启动子服失败: " + exception.getMessage());
                exception.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getPlayerDataManager().clearPlayerSession(owner.getUniqueId());
                    owner.getInventory().setContents(snapshot);
                    owner.teleport(previousLocation);
                    owner.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.child_server_start_failed"));
                    deleteRegistryEntry(roomId);
                });
            }
        });
        return true;
    }

    public boolean joinLobbyRoom(Player player, String roomId) {
        if (!isLobbyMode()) {
            return false;
        }

        ChildRoomRegistryEntry entry = loadRegistryEntry(roomId);
        if (entry == null) {
            return false;
        }

        if (entry.getState() != RoomState.WAITING && entry.getState() != RoomState.STARTING) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.game_started"));
            return true;
        }

        if (!entry.isPublic()) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.invite_only"));
            return true;
        }

        if (entry.getMaxPlayers() != -1 && entry.getCurrentPlayers() >= entry.getMaxPlayers()) {
            player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.join_failed"));
            return true;
        }

        ItemStack[] snapshot = cloneInventory(player.getInventory().getContents());
        plugin.getPlayerDataManager().saveTransferSession(player, player.getLocation().clone(), snapshot, roomId, entry.getProxyServerName());
        player.getInventory().clear();

        entry.setCurrentPlayers(entry.getCurrentPlayers() + 1);
        saveRegistryEntry(entry);

        player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(entry.getMode(), "room.child_server_join_loading"));
        connectPlayer(player, entry.getProxyServerName());
        return true;
    }

    public void handleLobbyJoinRestore(Player player) {
        if (!isLobbyMode() && !isCrossServerBackendConfigured()) {
            return;
        }
        plugin.getPlayerDataManager().restoreTransferSessionIfPresent(player);
    }

    public void handleNodePlayerJoin(Player player) {
        if (!isNodeMode() || pendingBootstrap == null) {
            return;
        }

        if (activeManagedRoomId == null) {
            if (!player.getUniqueId().equals(pendingBootstrap.getOwnerUuid())) {
                player.sendMessage(plugin.getMessageManager().getModeMessageWithPrefix(pendingBootstrap.getMode(), "room.child_server_wait_owner"));
                connectPlayer(player, lobbyServerName);
                return;
            }

            GameRoom room = plugin.getRoomManager().createRoom(player, pendingBootstrap.getMode(),
                    pendingBootstrap.getMaxPlayers(), pendingBootstrap.isPublic(), pendingBootstrap.getModifiers(),
                    pendingBootstrap.getRoomId(), false);
            activeManagedRoomId = room.getRoomId();
            saveNodeRoomSnapshot(room);
            startNodeSyncTask();
            return;
        }

        GameRoom room = plugin.getRoomManager().getRoom(activeManagedRoomId);
        if (room == null) {
            connectPlayer(player, lobbyServerName);
            return;
        }

        if (!plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            plugin.getRoomManager().joinRoom(player, room, false);
            saveNodeRoomSnapshot(room);
        }
    }

    public void syncRoom(GameRoom room) {
        if (room == null) {
            return;
        }
        if (isNodeMode() && room.getRoomId().equals(activeManagedRoomId)) {
            saveNodeRoomSnapshot(room);
            return;
        }
        if (managedCrossServerRooms.contains(room.getRoomId())) {
            saveCrossRoomSnapshot(room);
        }
    }

    public void onManagedRoomDeleted(String roomId) {
        if (roomId != null && managedCrossServerRooms.remove(roomId)) {
            deleteRegistryEntry(roomId);
        }

        if (!isNodeMode() || roomId == null || !roomId.equals(activeManagedRoomId)) {
            return;
        }
        deleteRegistryEntry(roomId);
        if (bootstrapFile.exists()) {
            bootstrapFile.delete();
        }
        activeManagedRoomId = null;
        pendingBootstrap = null;
        if (nodeSyncTask != null) {
            nodeSyncTask.cancel();
            nodeSyncTask = null;
        }
    }

    public void returnManagedRoomPlayerToLobby(Player player) {
        if (player == null) {
            return;
        }

        player.getInventory().clear();
        plugin.getRoomManager().resetPlayerForServerReturn(null, player);
        player.sendMessage(plugin.getMessageManager().getMessage("room.child_server_returning_lobby"));
        connectPlayer(player, lobbyServerName);
    }

    public void returnManagedRoomPlayersToLobby(GameRoom room) {
        if (!isNodeMode() || room == null) {
            return;
        }

        Set<UUID> everyone = new HashSet<>(room.getAllPlayerUUIDs());
        everyone.addAll(room.getSpectators());
        for (UUID uuid : everyone) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getRoomManager().restorePlayerAdvancements(player, room);
                returnManagedRoomPlayerToLobby(player);
            }
        }
    }

    public void scheduleNodeShutdown() {
        if (!isNodeMode()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 40L);
    }

    private void startNodeSyncTask() {
        if (nodeSyncTask != null) {
            nodeSyncTask.cancel();
        }
        nodeSyncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (activeManagedRoomId == null) {
                return;
            }
            GameRoom room = plugin.getRoomManager().getRoom(activeManagedRoomId);
            if (room != null) {
                saveNodeRoomSnapshot(room);
            }
        }, 40L, 40L);
    }

    private void startCrossSyncTask() {
        if (crossSyncTask != null) {
            return;
        }
        crossSyncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (managedCrossServerRooms.isEmpty()) {
                return;
            }
            for (String roomId : new HashSet<>(managedCrossServerRooms)) {
                GameRoom room = plugin.getRoomManager().getRoom(roomId);
                if (room != null) {
                    saveCrossRoomSnapshot(room);
                } else {
                    managedCrossServerRooms.remove(roomId);
                    deleteRegistryEntry(roomId);
                }
            }
        }, 40L, 40L);
    }

    private void saveNodeRoomSnapshot(GameRoom room) {
        ChildRoomRegistryEntry entry = loadRegistryEntry(room.getRoomId());
        if (entry == null) {
            entry = new ChildRoomRegistryEntry();
            entry.setRoomId(room.getRoomId());
            entry.setOwnerUuid(room.getOwnerUuid());
            entry.setOwnerName(room.getOwnerName());
            entry.setMode(room.getGameMode());
            entry.setMaxPlayers(room.getMaxPlayers());
            entry.setPublic(room.isPublic());
            entry.setModifiers(room.getModifiers());
            if (pendingBootstrap != null) {
                entry.setNodeId(pendingBootstrap.getNodeId());
                entry.setProxyServerName(pendingBootstrap.getProxyServerName());
                entry.setPort(pendingBootstrap.getPort());
                entry.setHost("127.0.0.1");
                entry.setCreatedAt(pendingBootstrap.getCreatedAt());
            }
        }
        entry.setState(room.getState());
        entry.setCurrentPlayers(room.getPlayerCount());
        saveRegistryEntry(entry);
    }

    private void saveCrossRoomSnapshot(GameRoom room) {
        ChildRoomRegistryEntry entry = loadRegistryEntry(room.getRoomId());
        if (entry == null) {
            entry = new ChildRoomRegistryEntry();
            entry.setRoomId(room.getRoomId());
            entry.setOwnerUuid(room.getOwnerUuid());
            entry.setOwnerName(room.getOwnerName());
            entry.setMode(room.getGameMode());
            entry.setMaxPlayers(room.getMaxPlayers());
            entry.setPublic(room.isPublic());
            entry.setModifiers(room.getModifiers());
            entry.setNodeId("cross-server");
            entry.setProxyServerName(crossGameServerName);
            entry.setHost("proxy");
            entry.setPort(0);
            entry.setCreatedAt(System.currentTimeMillis());
        }
        entry.setState(room.getState());
        entry.setCurrentPlayers(room.getPlayerCount());
        saveRegistryEntry(entry);
    }

    private void waitAndConnectOwner(Player owner, String targetServer, NodeDefinition node, String roomId, ItemStack[] snapshot) {
        BukkitTask task = new BukkitRunnable() {
            private int attempts = 0;

            @Override
            public void run() {
                attempts++;
                if (isPortOpen(node.host, node.port, 1000)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (owner.isOnline()) {
                            owner.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.child_server_ready"));
                            connectPlayer(owner, targetServer);
                        }
                    });
                    startTasks.remove(roomId);
                    cancel();
                    return;
                }

                int maxAttempts = Math.max(10, plugin.getConfigManager().getConfig().getInt("child_server.lobby.start_timeout_seconds", 60) * 2);
                if (attempts >= maxAttempts) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getPlayerDataManager().clearPlayerSession(owner.getUniqueId());
                        owner.getInventory().setContents(snapshot);
                        owner.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.child_server_start_failed"));
                        deleteRegistryEntry(roomId);
                    });
                    startTasks.remove(roomId);
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 10L);
        startTasks.put(roomId, task);
    }

    private void connectPlayer(Player player, String targetServer) {
        if (player == null || !player.isOnline() || targetServer == null || targetServer.isBlank()) {
            return;
        }

        transferringPlayers.add(player.getUniqueId());
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(targetServer);
        player.sendPluginMessage(plugin, BUNGEE_CHANNEL, output.toByteArray());

        Bukkit.getScheduler().runTaskLater(plugin, () -> transferringPlayers.remove(player.getUniqueId()), 200L);
    }

    private void loadLobbyNodes(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection nodeSection = section.getConfigurationSection(key);
            if (nodeSection == null) {
                continue;
            }

            NodeDefinition definition = new NodeDefinition();
            definition.id = key;
            definition.proxyServerName = nodeSection.getString("proxy_server_name", key);
            definition.host = nodeSection.getString("host", "127.0.0.1");
            definition.port = nodeSection.getInt("port", 25580);
            definition.templateDir = new File(nodeSection.getString("template_dir", ""));
            definition.runtimeRoot = new File(nodeSection.getString("runtime_root",
                    new File(plugin.getDataFolder(), "generated-nodes").getAbsolutePath()));
            definition.javaCommand = nodeSection.getString("java_command", "java");
            definition.serverJar = nodeSection.getString("server_jar", "paper.jar");
            definition.startupCommand = nodeSection.getString("startup_command", "");
            definition.jvmArgs = new ArrayList<>(nodeSection.getStringList("jvm_args"));
            lobbyNodes.put(key, definition);
        }
    }

    private NodeDefinition findAvailableNode() {
        Collection<ChildRoomRegistryEntry> activeRooms = loadAllRegistryEntries();
        Set<String> busyNodes = new HashSet<>();
        for (ChildRoomRegistryEntry entry : activeRooms) {
            if (entry.getNodeId() != null) {
                busyNodes.add(entry.getNodeId());
            }
        }

        for (NodeDefinition definition : lobbyNodes.values()) {
            if (!busyNodes.contains(definition.id)) {
                return definition;
            }
        }
        return null;
    }

    private Collection<ChildRoomRegistryEntry> loadAllRegistryEntries() {
        List<ChildRoomRegistryEntry> entries = new ArrayList<>();
        File[] files = registryFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return entries;
        }

        for (File file : files) {
            ChildRoomRegistryEntry entry = ChildRoomRegistryEntry.load(file);
            if (entry == null) {
                deleteRegistryFile(file, "删除损坏的跨服房间注册文件");
                continue;
            }
            if (isStaleRegistryEntry(entry)) {
                deleteRegistryFile(file, "删除过期的幽灵房间注册");
                continue;
            }
            entries.add(entry);
        }
        return entries;
    }

    private int cleanupStaleRegistryEntries(boolean startup) {
        int removed = 0;
        File[] files = registryFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            ChildRoomRegistryEntry entry = ChildRoomRegistryEntry.load(file);
            if (entry == null || entry.getState() == RoomState.ENDED || isStaleRegistryEntry(entry)) {
                if (deleteRegistryFile(file, startup ? "启动清理幽灵房间注册" : "清理幽灵房间注册")) {
                    removed++;
                }
            }
        }
        return removed;
    }

    private File prepareRuntimeDirectory(NodeDefinition node, String roomId) throws IOException {
        if (!node.runtimeRoot.exists()) {
            node.runtimeRoot.mkdirs();
        }

        String folderName = node.id + "_" + roomId + "_" + System.currentTimeMillis();
        File runtimeDir = new File(node.runtimeRoot, folderName);
        copyDirectory(node.templateDir.toPath(), runtimeDir.toPath());
        return runtimeDir;
    }

    private void writeBootstrap(File runtimeDir, NodeDefinition node, String roomId, UUID ownerUuid,
                                GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers) throws IOException {
        File pluginFolder = new File(runtimeDir, "plugins/GameFunXiao");
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        ChildRoomBootstrap bootstrap = new ChildRoomBootstrap(node.id, node.proxyServerName, node.port, roomId,
                ownerUuid, mode, maxPlayers, isPublic, modifiers, System.currentTimeMillis());
        bootstrap.save(new File(pluginFolder, "child-bootstrap.yml"));
    }

    private void updateServerProperties(File runtimeDir, NodeDefinition node, String roomId) throws IOException {
        File serverProperties = new File(runtimeDir, "server.properties");
        if (!serverProperties.exists()) {
            return;
        }

        List<String> lines = Files.readAllLines(serverProperties.toPath());
        boolean foundPort = false;
        boolean foundMotd = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("server-port=")) {
                lines.set(i, "server-port=" + node.port);
                foundPort = true;
            } else if (line.startsWith("motd=")) {
                lines.set(i, "motd=GameFun Node " + roomId);
                foundMotd = true;
            }
        }
        if (!foundPort) {
            lines.add("server-port=" + node.port);
        }
        if (!foundMotd) {
            lines.add("motd=GameFun Node " + roomId);
        }
        Files.write(serverProperties.toPath(), lines);
    }

    private void startNodeProcess(File runtimeDir, NodeDefinition node) throws IOException {
        List<String> command = new ArrayList<>();
        if (node.startupCommand != null && !node.startupCommand.isBlank()) {
            command.add("powershell");
            command.add("-NoProfile");
            command.add("-Command");
            command.add(node.startupCommand);
        } else {
            command.add(node.javaCommand);
            command.addAll(node.jvmArgs);
            command.add("-jar");
            command.add(node.serverJar);
            command.add("nogui");
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(runtimeDir);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(runtimeDir, "child-server.log")));
        builder.start();
    }

    private File getRegistryFile(String roomId) {
        return new File(registryFolder, roomId + ".yml");
    }

    private File getCrossRequestFile(UUID uuid) {
        return new File(crossRequestFolder, uuid.toString() + ".yml");
    }

    private File getTemplateEditRequestFile(UUID uuid) {
        return new File(templateEditRequestFolder, uuid.toString() + ".yml");
    }

    private File getEndFlashTuningRequestFile(UUID uuid) {
        return new File(endFlashTuningRequestFolder, uuid.toString() + ".yml");
    }

    private File getMiniGameMapEditRequestFile(UUID uuid) {
        return new File(miniGameMapEditRequestFolder, uuid.toString() + ".yml");
    }

    private File getBrickGuardMapEditRequestFile(UUID uuid) {
        return new File(brickGuardMapEditRequestFolder, uuid.toString() + ".yml");
    }

    private ChildRoomRegistryEntry loadRegistryEntry(String roomId) {
        File file = getRegistryFile(roomId);
        ChildRoomRegistryEntry entry = ChildRoomRegistryEntry.load(file);
        if (entry == null) {
            if (file.exists()) {
                deleteRegistryFile(file, "删除损坏的跨服房间注册文件");
            }
            return null;
        }
        if (isStaleRegistryEntry(entry)) {
            deleteRegistryFile(file, "删除过期的幽灵房间注册");
            return null;
        }
        return entry;
    }

    private void saveRegistryEntry(ChildRoomRegistryEntry entry) {
        if (entry == null || entry.getRoomId() == null) {
            return;
        }
        entry.setUpdatedAt(System.currentTimeMillis());
        try {
            entry.save(getRegistryFile(entry.getRoomId()));
        } catch (IOException exception) {
            plugin.getLogger().warning("保存子服房间注册信息失败: " + exception.getMessage());
        }
    }

    private void deleteRegistryEntry(String roomId) {
        File file = getRegistryFile(roomId);
        if (file.exists()) {
            deleteRegistryFile(file, "删除跨服房间注册");
        }
    }

    private boolean deleteRegistryFile(File file, String reason) {
        if (file == null || !file.exists()) {
            return false;
        }
        boolean deleted = file.delete();
        if (!deleted) {
            plugin.getLogger().warning(reason + "失败: " + file.getAbsolutePath());
        }
        return deleted;
    }

    private boolean isStaleRegistryEntry(ChildRoomRegistryEntry entry) {
        if (entry == null) {
            return true;
        }
        if (entry.getState() == RoomState.ENDED) {
            return true;
        }

        long now = System.currentTimeMillis();
        long lastUpdate = Math.max(entry.getUpdatedAt(), entry.getCreatedAt());
        if (lastUpdate <= 0L) {
            return true;
        }

        long age = now - lastUpdate;
        if (entry.getCurrentPlayers() <= 0 && age > EMPTY_REGISTRY_STALE_MILLIS) {
            return true;
        }

        if ("cross-server".equalsIgnoreCase(entry.getNodeId())) {
            return age > REGISTRY_STALE_MILLIS;
        }

        if (entry.getHost() == null || entry.getHost().isBlank() || entry.getPort() <= 0) {
            return age > REGISTRY_STALE_MILLIS;
        }

        return age > REGISTRY_STALE_MILLIS && !isPortOpen(entry.getHost(), entry.getPort(), 500);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("模板目录不存在: " + source);
        }

        Files.walk(source)
                .sorted(Comparator.naturalOrder())
                .forEach(path -> {
                    try {
                        Path relative = source.relativize(path);
                        Path destination = target.resolve(relative);
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(destination);
                        } else {
                            Files.createDirectories(destination.getParent());
                            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
    }

    private boolean isPortOpen(String host, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean ensureCrossServerAvailable(Player player, String targetServer) {
        return isCrossServerAvailableNow(player, targetServer, true);
    }

    private boolean isCrossServerAvailableNow(Player player, String targetServer, boolean notifyPlayer) {
        if (player == null || targetServer == null || targetServer.isBlank()) {
            return false;
        }

        requestServerEndpoint(player, targetServer);
        InetSocketAddress endpoint = crossServerEndpoints.get(normalizeServerKey(targetServer));
        if (endpoint == null || endpoint.getHostString().isBlank() || endpoint.getPort() <= 0) {
            if (notifyPlayer) {
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.cross_server_open_failed"));
            }
            return false;
        }

        if (!isPortOpen(endpoint.getHostString(), endpoint.getPort(), 800)) {
            if (notifyPlayer) {
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.cross_server_open_failed"));
            }
            return false;
        }
        return true;
    }

    private void requestServerEndpoint(Player carrier, String serverName) {
        if (carrier == null || !carrier.isOnline() || serverName == null || serverName.isBlank()) {
            return;
        }

        registerMessaging();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("ServerIP");
        output.writeUTF(serverName);
        try {
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, output.toByteArray());
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("发送 BungeeCord ServerIP 请求失败: " + exception.getMessage());
        }
    }

    private boolean isCurrentProxyServer(String targetServer) {
        if (targetServer == null || targetServer.isBlank()) {
            return false;
        }
        String serverName = resolveCurrentServerName();
        if (sameServerName(targetServer, serverName)) {
            return true;
        }
        // %qichengmorebungeeapi_server% 有时会返回带颜色的展示名，比如“小游戏 - 游戏中”，不能拿它硬等于代理服ID。
        // 跨服创建这种关键判断必须再用服务器目录名兜底，否则第二次进 gameing 会被误判成不是 gameing。
        return sameServerName(targetServer, resolveRuntimeDirectoryServerName());
    }

    private String applyPlaceholders(OfflinePlayer player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method method = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (ReflectiveOperationException exception) {
            return text;
        }
    }

    private void playEndFlashTuningSound(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.75f, 1.35f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.45f, 1.65f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.55f, 1.2f);
            }
        }, 5L);
    }

    private void playMiniGameMapEditSound(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.55f, 1.45f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72f, 1.68f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.58f, 1.28f);
            }
        }, 6L);
    }

    private String normalizeServerKey(String serverName) {
        return serverName == null ? "" : serverName.toLowerCase(Locale.ROOT);
    }

    private boolean sameServerName(String expected, String actual) {
        String expectedKey = normalizeComparableServerName(expected);
        String actualKey = normalizeComparableServerName(actual);
        return !expectedKey.isBlank() && expectedKey.equals(actualKey);
    }

    private String normalizeComparableServerName(String serverName) {
        if (serverName == null) {
            return "";
        }
        String stripped = org.bukkit.ChatColor.stripColor(serverName.replace('&', '§'));
        if (stripped == null) {
            stripped = serverName;
        }
        return stripped.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveRuntimeDirectoryServerName() {
        try {
            Path path = Path.of(System.getProperty("user.dir", "")).getFileName();
            if (path != null && !path.toString().isBlank()) {
                return path.toString().trim();
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private ItemStack[] cloneInventory(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                cloned[i] = contents[i].clone();
            }
        }
        return cloned;
    }

    private static class NodeDefinition {
        private String id;
        private String proxyServerName;
        private String host;
        private int port;
        private File templateDir;
        private File runtimeRoot;
        private String javaCommand;
        private String serverJar;
        private String startupCommand;
        private List<String> jvmArgs = new ArrayList<>();
    }

    private static class KitSyncBuffer {
        private final String sourceName;
        private final int totalChunks;
        private final int totalLength;
        private final long checksum;
        private final byte[][] chunks;
        private final long createdAt = System.currentTimeMillis();
        private int receivedChunks;
        private int receivedLength;

        private KitSyncBuffer(String sourceName, int totalChunks, int totalLength, long checksum) {
            this.sourceName = sourceName;
            this.totalChunks = totalChunks;
            this.totalLength = totalLength;
            this.checksum = checksum;
            this.chunks = new byte[totalChunks][];
        }

        private boolean accept(int index, byte[] chunk, int totalChunks, int totalLength, long checksum) {
            if (totalChunks != this.totalChunks || totalLength != this.totalLength || checksum != this.checksum
                    || index < 0 || index >= this.totalChunks || chunk == null || chunk.length == 0) {
                return false;
            }
            if (chunks[index] != null) {
                return true;
            }
            chunks[index] = chunk;
            receivedChunks++;
            receivedLength += chunk.length;
            return receivedLength <= this.totalLength;
        }

        private boolean isComplete() {
            return receivedChunks == totalChunks && receivedLength == totalLength;
        }

        private byte[] join() {
            byte[] joined = new byte[totalLength];
            int offset = 0;
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    throw new IllegalStateException("missing kit sync chunk");
                }
                System.arraycopy(chunk, 0, joined, offset, chunk.length);
                offset += chunk.length;
            }
            return joined;
        }
    }
}
