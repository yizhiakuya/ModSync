package com.mcmodsync.network;

import com.mcmodsync.McModSync;
import com.mcmodsync.client.ModSyncClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ModFileDataPayload(String fileName, byte[] data, int chunkIndex, int totalChunks) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ModFileDataPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(McModSync.MODID, "file_data"));

    public static final StreamCodec<ByteBuf, ModFileDataPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ModFileDataPayload::fileName,
        ByteBufCodecs.BYTE_ARRAY,
        ModFileDataPayload::data,
        ByteBufCodecs.VAR_INT,
        ModFileDataPayload::chunkIndex,
        ByteBufCodecs.VAR_INT,
        ModFileDataPayload::totalChunks,
        ModFileDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.debug("Received mod file chunk: {} ({}/{})",
                fileName, chunkIndex + 1, totalChunks);
            ModSyncClient.handleFileData(fileName, data, chunkIndex, totalChunks);
        });
    }

    public void handleConfiguration(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.debug("[Config Phase] Received mod file chunk: {} ({}/{})",
                fileName, chunkIndex + 1, totalChunks);
            ModSyncClient.handleFileData(fileName, data, chunkIndex, totalChunks);
        });
    }
}
