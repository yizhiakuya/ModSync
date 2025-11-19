package com.mcmodsync.server;

import com.mcmodsync.McModSync;
import com.mcmodsync.network.ModListPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

public class ModSyncConfigurationTask implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(McModSync.MODID + ":mod_sync");
    
    private final List<ModListPayload.ModInfo> mods;

    public ModSyncConfigurationTask(List<ModListPayload.ModInfo> mods) {
        this.mods = mods;
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        McModSync.LOGGER.info("Running mod sync configuration task, sending {} mods", mods.size());
        sender.accept(new ModListPayload(mods));
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
