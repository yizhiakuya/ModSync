package com.mcmodsync.server;

import com.mcmodsync.McModSync;
import com.mcmodsync.network.ModListPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;

import java.util.List;

@EventBusSubscriber(modid = McModSync.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        try {
            // 扫描服务器 mods
            List<ModListPayload.ModInfo> mods = ModSyncServer.scanServerMods();
            
            if (!mods.isEmpty()) {
                McModSync.LOGGER.info("Registering mod sync configuration task with {} mods", mods.size());
                event.register(new ModSyncConfigurationTask(mods));
            } else {
                McModSync.LOGGER.info("No mods to sync");
            }
        } catch (Exception e) {
            McModSync.LOGGER.error("Failed to register configuration task", e);
        }
    }
}

@EventBusSubscriber(modid = McModSync.MODID, bus = EventBusSubscriber.Bus.GAME)
class ServerGameEventHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 只在专用服务器端启动 HTTP API（不在客户端或集成服务器）
        if (event.getServer().isDedicatedServer()) {
            HttpApiServer.start();
        } else {
            McModSync.LOGGER.info("Not a dedicated server, HTTP API disabled");
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // 停止 HTTP API 服务器
        if (event.getServer().isDedicatedServer()) {
            HttpApiServer.stop();
        }
    }
}
