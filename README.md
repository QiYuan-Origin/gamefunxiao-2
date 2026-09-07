# GameFunXiao

GameFunXiao 是一个基于 Paper 的 Minecraft 小游戏插件，面向 QiYuan-Origin 服务器网络，核心玩法为猎人游戏及其扩展模式。

## 功能

- 猎人游戏房间：创建、加入、离开、旁观、重连和快速匹配
- 经典、随机指南针、互换、无物品、闪光公式等猎人游戏模式
- 死亡互换、幸运之柱等独立小游戏模式
- 房间状态管理、跨服房间同步和独立游戏世界
- 猎物选择、世界选择、双猎物、队友传送和房间邀请
- 猎人追踪指南针、表现值、段位、排行榜和游戏奖励
- 闪光公式物品、闪光难度、闪光书籍 Wiki 与音符盒音乐
- 终章闪光 Kit、末影箱和局内专属功能
- 可配置的菜单、记分板、消息、模式、地图和奖励
- PlaceholderAPI、Vault、Multiverse-Core 与 LuckPerms 兼容

## 运行环境

- Java 21 或更高版本
- Paper 1.21.x，推荐使用项目当前依赖的 Paper API 版本
- Maven 3.9 或更高版本

可选依赖：

- PlaceholderAPI
- Vault
- Multiverse-Core
- LuckPerms

## 构建

```bash
mvn clean package
```

构建产物位于 `target/`。将生成的插件 JAR 放入 Paper 服务端的 `plugins/` 目录，然后按服务器流程加载插件。

## 常用命令

```text
/gamefunxiao help
/gamefunxiao menu
/gamefunxiao huntergame
/gamefunxiao deathswap
/gamefunxiao hg list
/gamefunxiao hg join <房间号>
/gamefunxiao leave
/gamefunxiao rejoin
/gamefunxiao rank
/gamefunxiao wiki
/flashwiki
```

管理员命令包括配置重载、房间清理、地图管理、终章闪光 Kit、闪光音乐、大厅模板和管理入口，完整列表使用 `/gamefunxiao help` 查看。

## 配置

默认配置位于 `src/main/resources/`，包括：

- `config.yml`：插件总配置
- `messages.yml`：聊天消息
- `config/`：游戏模式、地图、奖励和记分板配置
- `flash/`：闪光公式相关数据

插件支持在运行时重载配置，并保留玩家数据。

## 项目结构

```text
src/main/java/org/gamefunxiao/
  commands/       命令与补全
  config/         配置和消息管理
  data/           玩家数据
  flash/          闪光公式玩法
  game/           房间、模式和游戏流程
  listeners/      Bukkit/Paper 事件监听
  menu/           游戏菜单
  scoreboard/     记分板
  server/         跨服通信
  world/          世界和地图管理
src/main/resources/
  config/         默认玩法配置
  flash/          闪光公式资源
```

## 许可

本项目基于 BSD 3-Clause License，详见 `LICENSE`。
