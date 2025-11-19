package com.mcmodsync.network;

import com.mcmodsync.McModSync;
import com.mcmodsync.server.ModSyncServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ModFileRequestPayload(String fileName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ModFileRequestPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(McModSync.MODID, "file_request"));

    public static final StreamCodec<ByteBuf, ModFileRequestPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ModFileRequestPayload::fileName,
        ModFileRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.info("Client requested mod file: {}", fileName);
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ModSyncServer.handleFileRequest(fileName, serverPlayer);
            }
        });
    }

    public void handleConfiguration(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.info("[Config Phase] Client requested mod file: {}", fileName);
            // 配置阶段不支持直接传输文件，需要告诉客户端等待登录后再下载
            McModSync.LOGGER.warn("File download in configuration phase not supported yet");
        });
    }
}
