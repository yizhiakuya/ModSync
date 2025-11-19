package com.mcmodsync.client.gui;

import com.mcmodsync.McModSync;
import com.mcmodsync.client.ModDownloader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModSyncScreen extends Screen {
    private final Screen lastScreen;
    private EditBox serverAddressBox;
    private Button syncButton;
    private String statusMessage;
    private int statusColor;
    private final List<String> downloadLog;
    private boolean syncing;

    public ModSyncScreen(Screen lastScreen) {
        super(Component.literal("Mod 同步"));
        this.lastScreen = lastScreen;
        this.statusMessage = "输入服务器地址并点击开始同步";
        this.statusColor = 0xAAAAAA;
        this.downloadLog = new ArrayList<>();
        this.syncing = false;
    }

    @Override
    protected void init() {
        super.init();

        // 服务器地址输入框
        this.serverAddressBox = new EditBox(
            this.font,
            this.width / 2 - 100,
            this.height / 2 - 30,
            200,
            20,
            Component.literal("服务器地址")
        );
        this.serverAddressBox.setMaxLength(128);
        this.serverAddressBox.setValue("http://localhost:56552");
        this.addRenderableWidget(this.serverAddressBox);

        // 同步按钮
        this.syncButton = Button.builder(
            Component.literal("开始同步"),
            button -> this.startSync()
        ).bounds(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build();
        this.addRenderableWidget(this.syncButton);

        // 返回按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("返回"),
            button -> this.minecraft.setScreen(this.lastScreen)
        ).bounds(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build());
    }

    private void startSync() {
        String serverAddress = this.serverAddressBox.getValue().trim();
        if (serverAddress.isEmpty()) {
            this.statusMessage = "请输入服务器地址";
            this.statusColor = 0xFF5555;
            return;
        }

        this.syncButton.active = false;
        this.syncing = true;
        this.downloadLog.clear();
        this.statusMessage = "正在连接服务器...";
        this.statusColor = 0xFFFF55;

        // 异步下载
        new Thread(() -> {
            try {
                ModDownloader downloader = new ModDownloader(serverAddress);
                int downloadedCount = downloader.syncMods(
                    (current, total, fileName) -> {
                        this.minecraft.execute(() -> {
                            this.statusMessage = String.format("下载进度: %d/%d", current, total);
                            this.statusColor = 0xFFFF55;
                            
                            String logMsg = String.format("[%d/%d] %s", current, total, fileName);
                            if (this.downloadLog.size() >= 10) {
                                this.downloadLog.remove(0);
                            }
                            this.downloadLog.add(logMsg);
                        });
                    }
                );

                this.minecraft.execute(() -> {
                    this.syncing = false;
                    if (downloadedCount > 0) {
                        this.statusMessage = String.format("成功下载 %d 个 mod！请重启游戏", downloadedCount);
                        this.statusColor = 0x55FF55;
                    } else {
                        this.statusMessage = "所有 mod 已是最新";
                        this.statusColor = 0x55FF55;
                    }
                    this.syncButton.active = true;
                });

            } catch (Exception e) {
                McModSync.LOGGER.error("Mod sync failed", e);
                this.minecraft.execute(() -> {
                    this.syncing = false;
                    this.statusMessage = "同步失败: " + e.getMessage();
                    this.statusColor = 0xFF5555;
                    this.syncButton.active = true;
                    this.downloadLog.add("错误: " + e.getMessage());
                });
            }
        }, "ModSync-Downloader").start();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染完整的背景（像设置页面一样）
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // 标题
        graphics.drawCenteredString(
            this.font,
            this.title,
            this.width / 2,
            20,
            0xFFFFFF
        );

        // 服务器地址标签
        graphics.drawCenteredString(
            this.font,
            "服务器 API 地址:",
            this.width / 2,
            this.height / 2 - 50,
            0xAAAAAA
        );

        // 状态框背景
        int statusBoxX = this.width / 2 - 150;
        int statusBoxY = this.height / 2 + 60;
        int statusBoxWidth = 300;
        int statusBoxHeight = 100;
        
        // 半透明黑色背景
        graphics.fill(statusBoxX, statusBoxY, statusBoxX + statusBoxWidth, statusBoxY + statusBoxHeight, 0xDD000000);
        // 边框
        graphics.fill(statusBoxX, statusBoxY, statusBoxX + statusBoxWidth, statusBoxY + 1, this.statusColor);
        graphics.fill(statusBoxX, statusBoxY + statusBoxHeight - 1, statusBoxX + statusBoxWidth, statusBoxY + statusBoxHeight, this.statusColor);
        graphics.fill(statusBoxX, statusBoxY, statusBoxX + 1, statusBoxY + statusBoxHeight, this.statusColor);
        graphics.fill(statusBoxX + statusBoxWidth - 1, statusBoxY, statusBoxX + statusBoxWidth, statusBoxY + statusBoxHeight, this.statusColor);

        // 状态消息
        graphics.drawCenteredString(
            this.font,
            "状态",
            this.width / 2,
            statusBoxY + 6,
            0xFFFFFF
        );
        
        graphics.drawCenteredString(
            this.font,
            this.statusMessage,
            this.width / 2,
            statusBoxY + 20,
            this.statusColor
        );

        // 下载日志
        if (!this.downloadLog.isEmpty()) {
            int logY = statusBoxY + 35;
            for (int i = Math.max(0, this.downloadLog.size() - 5); i < this.downloadLog.size(); i++) {
                String log = this.downloadLog.get(i);
                graphics.drawString(
                    this.font,
                    log,
                    statusBoxX + 5,
                    logY,
                    0xAAAAAA
                );
                logY += 10;
            }
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染完全不透明的渐变背景
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
