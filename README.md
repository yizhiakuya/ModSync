# MC Mod Sync

NeoForge mod，通过主菜单手动同步服务器 mod。

## 核心功能

**主菜单同步**：
- 主菜单添加"同步 Mod"按钮
- 输入服务器 API 地址
- 对比并下载缺失的 mod
- SHA-256 验证文件完整性
- 下载完成提示重启游戏

## 使用方法

### 1. 服务端安装 Mod

将 `mcmodsync-1.0.0.jar` 放入服务器 `mods` 目录。

服务器启动时会自动启动 HTTP API 服务器（端口 56552）。

### 2. 客户端安装 Mod

将 `mcmodsync-1.0.0.jar` 放入客户端 `mods` 目录：

```
client/mods/mcmodsync-1.0.0.jar
```

### 3. 同步 Mod

1. 启动 Minecraft 客户端
2. 在主菜单点击**"同步 Mod"**按钮
3. 输入服务器 API 地址（默认：`http://localhost:56552`）
4. 点击**"开始同步"**
5. 等待下载完成
6. 重启游戏加载新 mod

## 工作原理

1. **主菜单按钮** → 打开 mod 同步界面
2. **HTTP API 请求** → 获取服务器 mod 列表（JSON）
3. **对比哈希** → 检查本地 mod 的 SHA-256
4. **HTTP 下载** → 下载缺失或版本不同的 mod
5. **文件验证** → 验证下载文件的哈希值
6. **提示重启** → 显示下载结果

## API 说明

### GET /api/mods

返回服务器 mod 列表：

```json
[
  {
    "fileName": "Mekanism-1.21.1-10.7.17.83.jar",
    "sha256": "abc123...",
    "fileSize": 11950439,
    "downloadUrl": "http://localhost:8080/api/download/Mekanism-1.21.1-10.7.17.83.jar"
  }
]
```

### GET /api/download/{filename}

下载指定 mod 文件。

## 开发

### 构建

```bash
cd d:\mcmode\mod
.\gradlew.bat build
```

输出：`mod\build\libs\mcmodsync-1.0.0.jar`

### 测试

**启动服务器：**
```bash
cd d:\mcmode\mod
.\gradlew.bat runServer
```

**启动客户端：**
```bash
cd d:\mcmode\mod
.\gradlew.bat runClient
```

## 技术架构

- **客户端**：NeoForge 1.21 mod
- **GUI**：Minecraft Screen API
- **HTTP 客户端**：Java HttpURLConnection
- **文件验证**：SHA-256 哈希校验
- **异步下载**：多线程，避免阻塞游戏

## 优势

✅ **避免网络通道验证** - 不通过 Minecraft 协议，绕过 mod 验证
✅ **手动控制** - 玩家决定何时同步
✅ **支持大文件** - 没有 Minecraft 网络包大小限制
✅ **独立部署** - API 服务器可独立运行
✅ **跨版本兼容** - 不依赖 Minecraft 服务器
