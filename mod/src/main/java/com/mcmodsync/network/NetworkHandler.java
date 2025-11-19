package com.mcmodsync.network;

import com.mcmodsync.McModSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = McModSync.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // 配置阶段：服务端发送 mod 列表给客户端
        registrar.configurationToClient(
            ModListPayload.TYPE,
            ModListPayload.STREAM_CODEC,
            ModListPayload::handleConfiguration
        );

        // 配置阶段：客户端请求下载 mod
        registrar.configurationToServer(
            ModFileRequestPayload.TYPE,
            ModFileRequestPayload.STREAM_CODEC,
            ModFileRequestPayload::handleConfiguration
        );

        // 配置阶段：服务端发送 mod 文件数据
        registrar.configurationToClient(
            ModFileDataPayload.TYPE,
            ModFileDataPayload.STREAM_CODEC,
            ModFileDataPayload::handleConfiguration
        );

        McModSync.LOGGER.info("Network payloads registered for configuration phase");
    }
}
