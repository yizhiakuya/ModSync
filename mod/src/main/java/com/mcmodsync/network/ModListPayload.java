package com.mcmodsync.network;

import com.mcmodsync.McModSync;
import com.mcmodsync.client.ModSyncClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ModListPayload(List<ModInfo> mods) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ModListPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(McModSync.MODID, "mod_list"));

    public static final StreamCodec<ByteBuf, ModListPayload> STREAM_CODEC = StreamCodec.composite(
        ModInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ModListPayload::mods,
        ModListPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.info("Received mod list from server: {} mods", mods.size());
            ModSyncClient.handleModList(mods, context);
        });
    }

    public void handleConfiguration(IPayloadContext context) {
        context.enqueueWork(() -> {
            McModSync.LOGGER.info("[Config Phase] Received mod list from server: {} mods", mods.size());
            ModSyncClient.handleModList(mods, context);
        });
    }

    public record ModInfo(String fileName, String sha256, long fileSize) {
        public static final StreamCodec<ByteBuf, ModInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ModInfo::fileName,
            ByteBufCodecs.STRING_UTF8,
            ModInfo::sha256,
            ByteBufCodecs.VAR_LONG,
            ModInfo::fileSize,
            ModInfo::new
        );
    }
}
