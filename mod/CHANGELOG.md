# Changelog

## [1.0.0] - 2025-11-20

### 首次发布 🎉

#### 新增功能
- ✨ 主菜单自动 mod 同步功能
- 🔄 主菜单手动同步按钮
- 🔒 SHA-256 文件完整性校验
- ⚡ 基于 HTTP API 的 mod 传输
- 📦 智能增量更新（仅下载需要的 mod）
- 💾 配置文件自动保存
- 📊 实时下载进度显示
- 🎯 GUI 界面配置服务器地址
- 🔔 下载完成后的提示消息
- ⚙️ 服务器端口可通过配置文件自定义（默认 56552）

#### 技术特性
- 支持 Minecraft 1.21.1
- 兼容 NeoForge 21.1.206+
- 客户端和服务器端双端 mod
- HTTP API 服务器（默认端口 56552，可配置）
- JSON 配置文件支持

#### 工作流程
- 移除了加入世界时的强制同步检查
- 仅在主菜单进行 mod 同步
- 提供手动和自动两种同步模式

---

**完整更新日志**: https://github.com/yizhiakuya/mcmod/releases/tag/v1.0.0
**GitHub 仓库**: https://github.com/yizhiakuya/mcmod
