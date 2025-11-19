package com.mcmodsync;

import com.mcmodsync.client.ModDownloader;
import com.mcmodsync.client.gui.ModSyncScreen;
import com.mcmodsync.config.ModSyncConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = McModSync.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean autoSyncDone = false;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            // 在主菜单添加按钮
            int buttonWidth = 200;
            int buttonHeight = 20;
            int x = event.getScreen().width / 2 - 100;
            int y = event.getScreen().height / 4 + 48 + 72; // 单人游戏下方

            Button syncButton = Button.builder(
                Component.literal("同步 Mod"),
                button -> Minecraft.getInstance().setScreen(new ModSyncScreen(event.getScreen()))
            ).bounds(x, y, buttonWidth, buttonHeight).build();

            event.addListener(syncButton);

            // 自动同步 - 仅在第一次显示主菜单时执行
            if (!autoSyncDone) {
                autoSyncDone = true;
                ModSyncConfig config = ModSyncConfig.getInstance();
                
                if (config.isAutoSync()) {
                    performAutoSync(config.getServerAddress());
                }
            }
        }
    }

    private static void performAutoSync(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            McModSync.LOGGER.warn("Auto-sync enabled but no server address configured");
            return;
        }

        McModSync.LOGGER.info("Starting auto-sync from: {}", serverAddress);
        showMessage("§e[Mod Sync] 正在自动同步 mod...");

        // 异步执行同步
        new Thread(() -> {
            try {
                ModDownloader downloader = new ModDownloader(serverAddress);
                int downloadedCount = downloader.syncMods((current, total, fileName) -> {
                    if (current % 5 == 0 || current == total) {
                        McModSync.LOGGER.info("Auto-sync progress: {}/{} - {}", current, total, fileName);
                    }
                });

                if (downloadedCount > 0) {
                    showMessage(String.format("§a[Mod Sync] 自动同步完成！下载了 %d 个 mod，请重启游戏", downloadedCount));
                    McModSync.LOGGER.info("Auto-sync completed: {} mods downloaded", downloadedCount);
                } else {
                    showMessage("§a[Mod Sync] 所有 mod 已是最新");
                    McModSync.LOGGER.info("Auto-sync completed: all mods up to date");
                }

            } catch (Exception e) {
                showMessage("§c[Mod Sync] 自动同步失败: " + e.getMessage());
                McModSync.LOGGER.error("Auto-sync failed", e);
            }
        }, "ModSync-AutoSync").start();
    }

    private static void showMessage(String message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
            } else {
                // 如果玩家还未加载，记录到日志
                McModSync.LOGGER.info(message);
            }
        });
    }
}
