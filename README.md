# MC Mod Sync

一个自动同步服务器 mod 的 Minecraft 模组，让玩家无需手动下载服务器所需的 mod。

🔗 **GitHub**: [https://github.com/yizhiakuya/ModSync](https://github.com/yizhiakuya/ModSync)

## ✨ 功能特点

- 🚀 **主菜单自动同步** - 进入游戏主菜单时自动检查并下载所需 mod
- 🔄 **一键手动同步** - 主菜单提供"同步 Mod"按钮，可随时手动同步
- 🔒 **SHA-256 校验** - 确保下载的 mod 文件完整性和安全性
- ⚡ **HTTP API** - 通过高效的 HTTP 协议进行 mod 传输
- 📦 **智能增量更新** - 仅下载缺失或版本不同的 mod
- 💾 **自动保存配置** - 记住服务器地址，下次自动连接

## 📋 使用要求

- **Minecraft 版本**: 1.21.1
- **Mod 加载器**: NeoForge 21.1.206+
- **Java 版本**: Java 21

## 🎮 使用方法

### 客户端

1. 将 `mcmodsync-1.0.0.jar` 放入 `.minecraft/mods` 文件夹
2. 启动游戏，进入主菜单
3. 首次使用：
   - 点击"同步 Mod"按钮
   - 输入服务器地址（格式：`http://服务器IP:56552`，默认端口56552）
   - 勾选"自动同步"选项
   - 点击"开始同步"
4. 等待 mod 下载完成后重启游戏
5. 之后每次启动游戏都会自动检查并同步 mod

### 服务器端

1. 将 `mcmodsync-1.0.0.jar` 放入服务器的 `mods` 文件夹
2. 启动服务器，mod 会自动启动 HTTP API（默认端口 56552）
3. 服务器会自动扫描 `mods` 文件夹中的所有 mod（会排除 mcmodsync 自身）
4. 客户端通过主菜单同步时会自动获取 mod 列表并下载

## ⚙️ 配置

配置文件位于：`.minecraft/config/mcmodsync.json`

```json
{
  "serverAddress": "http://your-server:56552",
  "autoSync": true,
  "serverPort": 56552
}
```

- `serverAddress`: 服务器 HTTP API 地址（客户端配置）
- `autoSync`: 是否启用主菜单自动同步（客户端配置）
- `serverPort`: HTTP API 监听端口（服务器端配置，默认 56552）

## 🔧 服务器配置

服务器端 HTTP API 默认端口为 **56552**。可通过配置文件修改：

**修改端口方法**：
1. 在服务器的 `config/mcmodsync.json` 中添加或修改 `serverPort` 配置项
2. 重启服务器即可生效
3. 端口范围：1-65535

示例配置文件：
```json
{
  "serverAddress": "http://localhost:56552",
  "autoSync": false,
  "serverPort": 8080
}
```

**防火墙设置**：请确保配置的端口已在防火墙中开放。

**服务端类型**：仅在专用服务器（Dedicated Server）上启动 HTTP API，单人游戏或局域网服务器不会启动。

## 📝 工作原理

1. **主菜单同步**：进入主菜单时，客户端通过 HTTP API（GET /api/mods）获取服务器 mod 列表
2. **校验对比**：客户端对比本地 mod 的 SHA-256 哈希值，识别需要下载的 mod
3. **下载更新**：通过 HTTP API（GET /api/download/{filename}）下载缺失或版本不匹配的 mod
4. **完成提示**：所有 mod 下载完成后提示玩家重启游戏以加载新 mod
5. **排除机制**：服务器会自动排除 mcmodsync 自身，避免重复同步

## 🐛 常见问题

**Q: 下载速度慢？**  
A: 取决于服务器网络速度，大型 mod 包可能需要较长时间。

**Q: 连接失败？**  
A: 检查服务器地址是否正确，端口是否开放，服务器 mod 是否正常运行。

**Q: 需要重启游戏？**  
A: 是的，下载新 mod 后必须重启游戏才能加载。

**Q: 支持哪些 Minecraft 版本？**  
A: 当前版本仅支持 1.21.1 + NeoForge。

## 🔨 构建项目

### 环境要求
- Java 21 JDK
- Git（可选）

### 构建步骤

1. **克隆仓库**
```bash
git clone https://github.com/yizhiakuya/ModSync.git
cd ModSync/mod
```

2. **使用 Gradle 构建**

Windows:
```bash
.\gradlew.bat build
```

Linux/Mac:
```bash
./gradlew build
```

3. **获取构建结果**

构建成功后，jar 文件位于：
```
mod/build/libs/mcmodsync-1.0.0.jar
```

### 开发测试

**启动客户端测试：**
```bash
.\gradlew.bat runClient
```

**启动服务器测试：**
```bash
.\gradlew.bat runServer
```

### 清理构建

```bash
.\gradlew.bat clean
```

## 📄 许可证

本项目采用 [MIT License](mod/LICENSE) 开源。

## ⚠️ 免责声明

本软件按"原样"提供，不提供任何明示或暗示的保证，包括但不限于适销性、特定用途适用性和非侵权性的保证。

**使用须知**：
- 本mod为免费开源软件，作者不对使用本mod造成的任何问题负责
- 使用本mod前请备份您的存档和重要文件
- 服务器管理员应评估mod的安全性和稳定性后再部署到生产环境
- mod通过网络传输文件，请确保服务器来源可信
- 作者不对因使用本mod导致的数据丢失、服务器崩溃或其他问题承担责任

**建议**：
- 优先在测试环境中验证mod功能
- 定期备份服务器数据
- 使用防火墙保护服务器端口

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 👨‍💻 作者

本项目由伟大的AI大人开发。

## 📮 联系方式

- **GitHub 仓库**: [https://github.com/yizhiakuya/ModSync](https://github.com/yizhiakuya/ModSync)
- **问题反馈**: 请在 [GitHub Issues](https://github.com/yizhiakuya/ModSync/issues) 提交
- 欢迎 Star ⭐ 和 Fork 🍴
