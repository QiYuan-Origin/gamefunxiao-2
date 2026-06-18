package org.gamefunxiao;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.gamefunxiao.commands.GameFunCommand;
import org.gamefunxiao.config.ConfigManager;
import org.gamefunxiao.config.MessageManager;
import org.gamefunxiao.data.PlayerDataManager;
import org.gamefunxiao.flash.FlashModeManager;
import org.gamefunxiao.game.BrickGuardManager;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.game.GameManager;
import org.gamefunxiao.game.RoomManager;
import org.gamefunxiao.leaderboard.LeaderboardManager;
import org.gamefunxiao.listeners.FlashModeListener;
import org.gamefunxiao.listeners.MenuListener;
import org.gamefunxiao.listeners.MiniGameMapEditListener;
import org.gamefunxiao.listeners.PlayerListener;
import org.gamefunxiao.menu.MenuManager;
import org.gamefunxiao.placeholder.GameFunPlaceholderExpansion;
import org.gamefunxiao.scoreboard.ScoreboardManager;
import org.gamefunxiao.server.ChildServerManager;
import org.gamefunxiao.tab.TabHeaderFooterManager;
import org.gamefunxiao.world.BrickGuardMapManager;
import org.gamefunxiao.world.MiniGameMapManager;
import org.gamefunxiao.world.WorldManager;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GameFunXiao extends JavaPlugin {

    private static GameFunXiao instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private MenuManager menuManager;
    private GameManager gameManager;
    private RoomManager roomManager;
    private PlayerDataManager playerDataManager;
    private LeaderboardManager leaderboardManager;
    private WorldManager worldManager;
    private ScoreboardManager scoreboardManager;
    private PlayerListener playerListener;
    private ChildServerManager childServerManager;
    private TabHeaderFooterManager tabHeaderFooterManager;
    private FlashModeManager flashModeManager;
    private EndFlashKitManager endFlashKitManager;
    private MiniGameMapManager miniGameMapManager;
    private BrickGuardManager brickGuardManager;
    private BrickGuardMapManager brickGuardMapManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("§x§5§5§F§F§A§A✓ GameFun 正在启动...");

        // 初始化配置
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        childServerManager = new ChildServerManager(this);

        // 初始化管理器
        miniGameMapManager = new MiniGameMapManager(this);
        brickGuardMapManager = new BrickGuardMapManager(this);
        worldManager = new WorldManager(this);
        playerDataManager = new PlayerDataManager(this);
        leaderboardManager = new LeaderboardManager(this);
        flashModeManager = new FlashModeManager(this);
        flashModeManager.installBundledFlashNoteSongs();
        endFlashKitManager = new EndFlashKitManager(this);
        brickGuardManager = new BrickGuardManager(this);
        roomManager = new RoomManager(this);
        gameManager = new GameManager(this);
        menuManager = new MenuManager(this);
        scoreboardManager = new ScoreboardManager(this);
        tabHeaderFooterManager = new TabHeaderFooterManager(this);

        // 注册命令
        registerCommands();

        // 注册监听器
        registerListeners();
        childServerManager.registerMessaging();
        flashModeManager.registerRecipe();
        registerPlaceholderExpansion();
        int cleanedGhostRooms = childServerManager.cleanupStaleRegistryEntriesOnStartup();
        if (cleanedGhostRooms > 0) {
            getLogger().info("§x§5§5§F§F§A§A✓ 已清理 " + cleanedGhostRooms + " 个幽灵跨服房间注册。");
        }

        // 启动记分板更新任务
        scoreboardManager.startUpdateTask();
        tabHeaderFooterManager.start();

        // 清理上次强制关服残留的世界文件夹
        worldManager.cleanupLeftoverWorlds();

        // 恢复强制关服时未正常退出的玩家会话
        if (!childServerManager.isNodeMode()) {
            playerDataManager.recoverCrashedSessions();
        }

        Bukkit.getScheduler().runTask(this, () -> Bukkit.getOnlinePlayers().forEach(roomManager::ensurePlayerRecipesAvailable));
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            if (flashModeManager != null) {
                flashModeManager.syncFlashItemLore(player);
            }
        }), 60L);

        getLogger().info("§x§5§5§F§F§A§A✓ GameFun 启动完成！");
    }

    @Override
    public void onDisable() {
        getLogger().info("§x§F§F§8§8§5§5⊗ GameFun 正在关闭...");

        // 停止记分板更新任务
        if (tabHeaderFooterManager != null) {
            tabHeaderFooterManager.stop();
        }
        if (scoreboardManager != null) {
            scoreboardManager.stopUpdateTask();
            scoreboardManager.clearAll();
        }

        if (flashModeManager != null) {
            flashModeManager.stopAllFlashNoteMusic();
            flashModeManager.clearTurtleShellSpeedModifiers();
        }

        // 结束所有游戏
        if (gameManager != null) {
            gameManager.shutdown();
        }

        // 删除所有房间和世界
        if (roomManager != null) {
            roomManager.cleanupAllRooms();
        }

        // 清理所有创建的世界
        if (worldManager != null) {
            worldManager.cleanupAllWorlds();
        }

        if (childServerManager != null) {
            childServerManager.shutdown();
        }

        // 保存数据
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        getLogger().info("§x§F§F§8§8§5§5⊗ GameFun 已关闭！");
    }

    private void registerCommands() {
        GameFunCommand gameFunCommand = new GameFunCommand(this);
        if (registerPaperCommands(gameFunCommand)) {
            return;
        }
        bindLegacyCommand("gamefunxiao", gameFunCommand);
        bindLegacyCommand("hh", gameFunCommand);
        bindLegacyCommand("ec", gameFunCommand);
        bindLegacyCommand("enderchest", gameFunCommand);
        bindLegacyCommand("flashwiki", gameFunCommand);
    }

    private boolean registerPaperCommands(GameFunCommand executor) {
        Class<?> basicCommandClass;
        Method registerMethod;
        try {
            basicCommandClass = Class.forName("io.papermc.paper.command.brigadier.BasicCommand");
            registerMethod = JavaPlugin.class.getMethod(
                    "registerCommand",
                    String.class,
                    String.class,
                    Collection.class,
                    basicCommandClass
            );
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return false;
        }

        try {
            registerPaperCommand(registerMethod, basicCommandClass, "gamefunxiao",
                    "GameFun 小游戏主命令", List.of("gfx", "gamefun"), "gamefunxiao.use", executor);
            registerPaperCommand(registerMethod, basicCommandClass, "hh",
                    "宣传当前房间", List.of(), "gamefunxiao.use", executor);
            registerPaperCommand(registerMethod, basicCommandClass, "ec",
                    "终章中距离猎物100~170格打开末影箱", List.of(), "gamefunxiao.use", executor);
            registerPaperCommand(registerMethod, basicCommandClass, "enderchest",
                    "终章末影箱完整命令", List.of("endflashender"), "gamefunxiao.use", executor);
            registerPaperCommand(registerMethod, basicCommandClass, "flashwiki",
                    "获取闪光模式局内书 Wiki", List.of("bookwiki", "gamefunwiki", "闪光手册", "书wiki"),
                    "gamefunxiao.use", executor);
            getLogger().info("§x§5§5§F§F§A§A✓ 已使用 Paper 指令注册。");
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Paper 指令注册失败: " + exception.getMessage(), exception);
        }
    }

    private void registerPaperCommand(Method registerMethod, Class<?> basicCommandClass, String name,
                                      String description, Collection<String> aliases, String permission,
                                      GameFunCommand executor) throws ReflectiveOperationException {
        Object command = Proxy.newProxyInstance(
                basicCommandClass.getClassLoader(),
                new Class<?>[]{basicCommandClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "GameFunPaperCommand{" + name + "}";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }

                    return switch (method.getName()) {
                        case "execute" -> {
                            CommandSender sender = extractPaperCommandSender(args != null && args.length > 0 ? args[0] : null);
                            String[] commandArgs = args != null && args.length > 1 && args[1] instanceof String[] strings
                                    ? strings : new String[0];
                            executor.executeRegisteredCommand(sender, name, commandArgs);
                            yield null;
                        }
                        case "suggest" -> {
                            CommandSender sender = extractPaperCommandSender(args != null && args.length > 0 ? args[0] : null);
                            String[] commandArgs = args != null && args.length > 1 && args[1] instanceof String[] strings
                                    ? strings : new String[0];
                            yield executor.tabCompleteRegisteredCommand(sender, name, commandArgs);
                        }
                        case "canUse" -> {
                            CommandSender sender = args != null && args.length > 0 && args[0] instanceof CommandSender commandSender
                                    ? commandSender : null;
                            yield permission == null || permission.isBlank() || sender == null || sender.hasPermission(permission);
                        }
                        case "permission" -> permission;
                        default -> Collections.emptyList();
                    };
                }
        );
        registerMethod.invoke(this, name, description, aliases, command);
    }

    private CommandSender extractPaperCommandSender(Object commandSourceStack) throws ReflectiveOperationException {
        if (commandSourceStack == null) {
            return getServer().getConsoleSender();
        }
        Method getSenderMethod = commandSourceStack.getClass().getMethod("getSender");
        Object sender = getSenderMethod.invoke(commandSourceStack);
        if (sender instanceof CommandSender commandSender) {
            return commandSender;
        }
        return getServer().getConsoleSender();
    }

    private void bindLegacyCommand(String name, GameFunCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("未在 plugin.yml 中找到命令: " + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new MiniGameMapEditListener(this), this);
        getServer().getPluginManager().registerEvents(new FlashModeListener(this), this);
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new GameFunPlaceholderExpansion(this).register();
        getLogger().info("§x§5§5§F§F§A§A✓ 已注册 PlaceholderAPI 变量: %gamefun_coins%");
    }

    public static GameFunXiao getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ChildServerManager getChildServerManager() {
        return childServerManager;
    }

    public TabHeaderFooterManager getTabHeaderFooterManager() {
        return tabHeaderFooterManager;
    }

    public FlashModeManager getFlashModeManager() {
        return flashModeManager;
    }

    public EndFlashKitManager getEndFlashKitManager() {
        return endFlashKitManager;
    }

    public MiniGameMapManager getMiniGameMapManager() {
        return miniGameMapManager;
    }

    public BrickGuardManager getBrickGuardManager() {
        return brickGuardManager;
    }

    public BrickGuardMapManager getBrickGuardMapManager() {
        return brickGuardMapManager;
    }
}
