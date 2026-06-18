# GameFunXiao

GameFunXiao 是一个基于 Paper 1.21.x 的 Minecraft 小游戏插件，当前主要提供“猎人游戏”玩法。

玩家可以创建或加入房间，分为猎人和猎物两个阵营进行追逐对抗。猎物需要完成目标或存活到规定时间，猎人则需要在此之前击杀猎物。

## 主要功能

- 多种猎人游戏模式
- 房间创建、加入、离开和旁观
- GUI 菜单操作
- 猎人追踪指南针
- 游戏排行榜与奖励
- 独立游戏世界管理
- 配置文件和消息自定义
- 闪光模式扩展玩法

## 环境要求

- Java 21+
- Paper 1.21.x
- Maven 3.9+

可选依赖：

- Vault
- PlaceholderAPI
- Multiverse-Core

## 构建

```bash
mvn clean package
```

构建完成后，将 `target/` 目录中的插件 jar 放入 Paper 服务端的 `plugins/` 目录即可。

## 基础命令

```text
/gamefunxiao
/gamefunxiao menu
/gamefunxiao hg
/gamefunxiao leave
/gamefunxiao rejoin
/flashwiki
```

管理员可使用：

```text
/gamefunxiao reload
/gamefunxiao hg create <模式> <人数>
```

## 文档

仓库内提供了更多说明文档：

- `插件介绍.md`
- `开发指南.md`
- `闪光模式.md`

## License

暂未指定开源许可证。
