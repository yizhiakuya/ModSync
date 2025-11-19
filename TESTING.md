# MC Mod Sync 测试指南

## 当前状态

✅ Mod 同步服务器已启动: `http://localhost:8080`
🔄 Minecraft 测试服务器启动中...

## 测试步骤

### 1. 准备测试 Mod

将要测试的 mod `.jar` 文件放入：
```
d:\mcmode\test-server\mods\
```

例如下载一个轻量级 mod 用于测试（如 JEI、Sodium 等）。

### 2. 验证 Mod 同步服务器

打开浏览器访问：
```
http://localhost:8080/api/mods
```

应该能看到 JSON 格式的 mod 列表。

### 3. 启动客户端测试

在新终端中运行：
```bash
cd d:\mcmode\mod
.\gradlew.bat runClient
```

### 4. 测试流程

1. **客户端启动** - 不要先放入任何测试 mod 到客户端 mods 目录
2. **登录游戏** - 进入单人游戏或多人服务器
3. **自动检测** - mod 会自动检查服务器所需的 mod
4. **自动下载** - 缺失的 mod 会自动下载到客户端
5. **提示重启** - 游戏会提示你重启以加载新 mod

### 5. 验证下载

检查客户端的 mods 目录：
```
%APPDATA%\.minecraft\mods\
或
.\mod\run\mods\  (开发环境)
```

应该能看到自动下载的 mod 文件。

## 配置

客户端配置文件位置：
```
.\mod\run\config\mcmodsync-client.toml
```

可配置项：
- `serverApiUrl`: API 地址（默认 http://localhost:8080/api/mods）
- `enableAutoSync`: 启用/禁用自动同步
- `connectionTimeout`: 连接超时时间（毫秒）

## 日志

查看日志输出以了解同步过程：
- 客户端日志: `.\mod\run\logs\latest.log`
- Mod 日志关键字: `[mcmodsync]`, `MC Mod Sync`

## 常见测试场景

### 场景 1: 首次同步
1. 服务器有 3 个 mod
2. 客户端没有这些 mod
3. 预期: 自动下载 3 个 mod

### 场景 2: 部分同步
1. 服务器有 3 个 mod
2. 客户端已有 1 个 mod
3. 预期: 自动下载缺失的 2 个 mod

### 场景 3: 已同步
1. 服务器有 3 个 mod
2. 客户端已有全部 3 个 mod（相同哈希值）
3. 预期: 不下载，显示"已安装"消息
